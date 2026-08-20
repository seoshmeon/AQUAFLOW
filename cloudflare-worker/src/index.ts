import {formatDuration, MAX_SYNC_BATCH, parseCommand, periodFromTimestamp, validateRecord, type StatsRow, type SyncRecord} from "./domain";

interface Env {
  DB: D1Database;
  TELEGRAM_BOT_TOKEN: string;
  TELEGRAM_WEBHOOK_SECRET: string;
}

interface TelegramUpdate {
  update_id: number;
  message?: {
    chat: {id: number};
    from?: {id: number; username?: string; first_name?: string};
    text?: string;
  };
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      const url = new URL(request.url);
      if (request.method === "GET" && url.pathname === "/health") return json({status: "ok", service: "aquaflow"});
      if (request.method === "POST" && url.pathname === "/admin/setup-webhook") return setupWebhook(request, env);
      if (request.method === "POST" && url.pathname === "/telegram/webhook") return telegramWebhook(request, env);
      if (request.method === "POST" && url.pathname === "/v1/auth/anonymous") return createAnonymousUser(env);
      if (request.method === "POST" && url.pathname === "/v1/link/start") {
        return createLinkCode(request, env);
      }
      if (request.method === "POST" && url.pathname === "/v1/link/status") return linkStatus(request, env);
      if (request.method === "POST" && url.pathname === "/v1/records/sync") return syncRecords(request, env);
      return json({error: "not_found"}, 404);
    } catch (error) {
      console.error("request failed", error);
      return json({error: error instanceof Error ? error.message : "internal_error"}, 400);
    }
  },
} satisfies ExportedHandler<Env>;

async function setupWebhook(request: Request, env: Env): Promise<Response> {
  if (request.headers.get("x-aquaflow-admin-secret") !== env.TELEGRAM_WEBHOOK_SECRET) {
    return json({error: "unauthorized"}, 401);
  }
  const origin = new URL(request.url).origin;
  const identity = await telegramApi(env, "getMe");
  await telegramApi(env, "setWebhook", {
    url: `${origin}/telegram/webhook`,
    secret_token: env.TELEGRAM_WEBHOOK_SECRET,
    allowed_updates: ["message"],
    drop_pending_updates: true,
  });
  const webhook = await telegramApi(env, "getWebhookInfo");
  const me = identity.result as {username?: string};
  const info = webhook.result as {url?: string; pending_update_count?: number; last_error_message?: string};
  return json({
    botUsername: me.username ?? null,
    webhookUrl: info.url ?? null,
    pendingUpdates: info.pending_update_count ?? 0,
    lastError: info.last_error_message ?? null,
  });
}

async function createAnonymousUser(env: Env): Promise<Response> {
  const userId = crypto.randomUUID();
  const token = randomToken(32);
  const now = Date.now();
  await env.DB.batch([
    env.DB.prepare("INSERT INTO users (id, created_at) VALUES (?, ?)").bind(userId, now),
    env.DB.prepare("INSERT INTO install_tokens (token_hash, user_id, created_at, last_used_at) VALUES (?, ?, ?, ?)")
      .bind(await sha256(token), userId, now, now),
  ]);
  return json({userId, token}, 201);
}

async function createLinkCode(request: Request, env: Env): Promise<Response> {
  const userId = await authenticate(request, env);
  const code = randomCode();
  const now = Date.now();
  await env.DB.prepare("INSERT INTO link_codes (code_hash, user_id, expires_at) VALUES (?, ?, ?)")
    .bind(await sha256(code), userId, now + 10 * 60_000).run();
  return json({code, expiresInSeconds: 600}, 201);
}

async function linkStatus(request: Request, env: Env): Promise<Response> {
  const userId = await authenticate(request, env);
  const link = await env.DB.prepare("SELECT username, first_name FROM telegram_links WHERE user_id = ?")
    .bind(userId).first<{username: string | null; first_name: string | null}>();
  return json({
    linked: link !== null,
    telegramUsername: link?.username ?? null,
    telegramFirstName: link?.first_name ?? null,
  });
}

async function syncRecords(request: Request, env: Env): Promise<Response> {
  const userId = await authenticate(request, env);
  const body = await request.json<{records?: unknown}>();
  if (!Array.isArray(body.records) || body.records.length > MAX_SYNC_BATCH) return json({error: "invalid_batch"}, 400);
  const records = body.records.map(value => validateRecord(value));
  let accepted = 0;
  for (const record of records) accepted += await syncRecord(env.DB, userId, record);
  return json({accepted, duplicates: records.length - accepted});
}

async function syncRecord(db: D1Database, userId: string, record: SyncRecord): Promise<number> {
  const now = Date.now();
  const inserted = await db.prepare(
    `INSERT OR IGNORE INTO records
     (user_id, client_record_id, session_id, attempt_number, hold_duration_millis,
      recovery_duration_millis, recorded_at, comfort_rating, synced_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
  ).bind(
    userId, record.clientRecordId, record.sessionId, record.attemptNumber,
    record.holdDurationMillis, record.recoveryDurationMillis, record.timestamp,
    record.comfortRating ?? null, now,
  ).run();
  if ((inserted.meta.changes ?? 0) === 0) return 0;
  const period = periodFromTimestamp(record.timestamp);
  await db.batch([
    aggregateStatement(db, "stats", userId, undefined, record, now),
    aggregateStatement(db, "monthly_stats", userId, period, record, now),
  ]);
  const link = await db.prepare("SELECT leaderboard_opt_in, username, telegram_user_id FROM telegram_links WHERE user_id = ?")
    .bind(userId).first<{leaderboard_opt_in: number; username: string | null; telegram_user_id: string}>();
  if (link?.leaderboard_opt_in === 1) await refreshLeaderboard(db, userId, link.username, link.telegram_user_id);
  return 1;
}

function aggregateStatement(
  db: D1Database,
  table: "stats" | "monthly_stats",
  userId: string,
  period: string | undefined,
  record: SyncRecord,
  now: number,
): D1PreparedStatement {
  const periodColumn = period === undefined ? "" : ", period";
  const periodValue = period === undefined ? "" : ", ?";
  const conflict = period === undefined ? "user_id" : "user_id, period";
  const sql = `INSERT INTO ${table}
    (user_id${periodColumn}, attempts, total_hold_millis, max_hold_millis, latest_hold_millis, latest_timestamp, updated_at)
    VALUES (?${periodValue}, 1, ?, ?, ?, ?, ?)
    ON CONFLICT(${conflict}) DO UPDATE SET
      attempts = attempts + 1,
      total_hold_millis = total_hold_millis + excluded.total_hold_millis,
      max_hold_millis = MAX(max_hold_millis, excluded.max_hold_millis),
      latest_hold_millis = CASE WHEN excluded.latest_timestamp >= latest_timestamp THEN excluded.latest_hold_millis ELSE latest_hold_millis END,
      latest_timestamp = MAX(latest_timestamp, excluded.latest_timestamp),
      updated_at = excluded.updated_at`;
  const values = period === undefined
    ? [userId, record.holdDurationMillis, record.holdDurationMillis, record.holdDurationMillis, record.timestamp, now]
    : [userId, period, record.holdDurationMillis, record.holdDurationMillis, record.holdDurationMillis, record.timestamp, now];
  return db.prepare(sql).bind(...values);
}

async function telegramWebhook(request: Request, env: Env): Promise<Response> {
  if (request.headers.get("x-telegram-bot-api-secret-token") !== env.TELEGRAM_WEBHOOK_SECRET) {
    return new Response("Unauthorized", {status: 401});
  }
  const update = await request.json<TelegramUpdate>();
  const message = update.message;
  if (!message?.text || !message.from) return new Response(null, {status: 204});
  const {command, argument} = parseCommand(message.text);
  const telegramId = String(message.from.id);
  const chatId = String(message.chat.id);
  switch (command) {
    case "/start": await start(env, telegramId, chatId); break;
    case "/link": await link(env, telegramId, chatId, message.from.username, message.from.first_name, argument); break;
    case "/unlink": await unlink(env, telegramId, chatId); break;
    case "/progress": await progress(env, telegramId, chatId); break;
    case "/record": await record(env, telegramId, chatId); break;
    case "/month": await month(env, telegramId, chatId); break;
    case "/leaderboard": await leaderboard(env, telegramId, chatId, argument.toLowerCase()); break;
    case "/deleteaccount": await deleteAccount(env, telegramId, chatId, argument); break;
    case "/privacy": await send(env, chatId, PRIVACY_TEXT); break;
    default: await send(env, chatId, HELP_TEXT);
  }
  return new Response(null, {status: 204});
}

async function start(env: Env, telegramId: string, chatId: string): Promise<void> {
  const userId = await userIdFor(env.DB, telegramId);
  await send(env, chatId, userId
    ? `AQUAFLOW подключён.\n\n${HELP_TEXT}`
    : `Личный кабинет AQUAFLOW. Создайте код в приложении и отправьте:\n/link КОД\n\n${HELP_TEXT}`);
}

async function link(
  env: Env, telegramId: string, chatId: string, username: string | undefined,
  firstName: string | undefined, rawCode: string,
): Promise<void> {
  const code = rawCode.toUpperCase();
  if (!/^[A-Z2-9]{8}$/.test(code)) {
    await send(env, chatId, "Код должен состоять из 8 символов. Получите новый код в AQUAFLOW.");
    return;
  }
  const row = await env.DB.prepare(
    "SELECT user_id FROM link_codes WHERE code_hash = ? AND claimed_at IS NULL AND expires_at >= ?",
  ).bind(await sha256(code), Date.now()).first<{user_id: string}>();
  if (!row) {
    await send(env, chatId, "Код неверный, использован или истёк. Создайте новый код в приложении.");
    return;
  }
  const now = Date.now();
  await env.DB.batch([
    env.DB.prepare("DELETE FROM telegram_links WHERE user_id = ? OR telegram_user_id = ?").bind(row.user_id, telegramId),
    env.DB.prepare(
      `INSERT INTO telegram_links
       (telegram_user_id, chat_id, user_id, username, first_name, leaderboard_opt_in, linked_at)
       VALUES (?, ?, ?, ?, ?, 0, ?)`,
    ).bind(telegramId, chatId, row.user_id, username ?? null, firstName ?? null, now),
    env.DB.prepare("UPDATE link_codes SET claimed_at = ? WHERE code_hash = ?").bind(now, await sha256(code)),
  ]);
  await send(env, chatId, "Готово — профиль AQUAFLOW подключён. Попробуйте /progress");
}

async function unlink(env: Env, telegramId: string, chatId: string): Promise<void> {
  const userId = await userIdFor(env.DB, telegramId);
  if (userId) await env.DB.batch([
    env.DB.prepare("DELETE FROM leaderboard_entries WHERE user_id = ?").bind(userId),
    env.DB.prepare("DELETE FROM telegram_links WHERE telegram_user_id = ?").bind(telegramId),
  ]);
  await send(env, chatId, "Telegram отключён. История AQUAFLOW не удалена.");
}

async function deleteAccount(env: Env, telegramId: string, chatId: string, confirmation: string): Promise<void> {
  const userId = await userIdFor(env.DB, telegramId);
  if (!userId) { await send(env, chatId, "Подключённый профиль AQUAFLOW не найден."); return; }
  if (confirmation !== "ПОДТВЕРЖДАЮ") {
    await send(env, chatId, "Это безвозвратно удалит облачную историю, агрегаты и рейтинг. Для подтверждения отправьте:\n/deleteaccount ПОДТВЕРЖДАЮ");
    return;
  }
  await env.DB.batch([
    env.DB.prepare("DELETE FROM leaderboard_entries WHERE user_id = ?").bind(userId),
    env.DB.prepare("DELETE FROM records WHERE user_id = ?").bind(userId),
    env.DB.prepare("DELETE FROM monthly_stats WHERE user_id = ?").bind(userId),
    env.DB.prepare("DELETE FROM stats WHERE user_id = ?").bind(userId),
    env.DB.prepare("DELETE FROM link_codes WHERE user_id = ?").bind(userId),
    env.DB.prepare("DELETE FROM install_tokens WHERE user_id = ?").bind(userId),
    env.DB.prepare("DELETE FROM telegram_links WHERE user_id = ?").bind(userId),
    env.DB.prepare("DELETE FROM users WHERE id = ?").bind(userId),
  ]);
  await send(env, chatId, "Облачный профиль AQUAFLOW и его данные полностью удалены.");
}

async function progress(env: Env, telegramId: string, chatId: string): Promise<void> {
  const userId = await requireUser(env, telegramId, chatId); if (!userId) return;
  const stats = await statsFor(env.DB, userId);
  await send(env, chatId, stats?.attempts
    ? `Ваш прогресс AQUAFLOW\n\nПодходов: ${stats.attempts}\nСреднее: ${formatDuration(stats.total_hold_millis / stats.attempts)}\nРекорд: ${formatDuration(stats.max_hold_millis)}\nПоследний: ${formatDuration(stats.latest_hold_millis)}`
    : "Синхронизированных подходов пока нет.");
}

async function record(env: Env, telegramId: string, chatId: string): Promise<void> {
  const userId = await requireUser(env, telegramId, chatId); if (!userId) return;
  const stats = await statsFor(env.DB, userId);
  await send(env, chatId, stats?.attempts ? `Ваш личный рекорд: ${formatDuration(stats.max_hold_millis)}` : "Результатов пока нет.");
}

async function month(env: Env, telegramId: string, chatId: string): Promise<void> {
  const userId = await requireUser(env, telegramId, chatId); if (!userId) return;
  const period = new Date().toISOString().slice(0, 7);
  const stats = await env.DB.prepare("SELECT * FROM monthly_stats WHERE user_id = ? AND period = ?")
    .bind(userId, period).first<StatsRow>();
  await send(env, chatId, stats?.attempts
    ? `${period}\n\nПодходов: ${stats.attempts}\nСреднее: ${formatDuration(stats.total_hold_millis / stats.attempts)}\nЛучшее: ${formatDuration(stats.max_hold_millis)}`
    : `За ${period} результатов пока нет.`);
}

async function leaderboard(env: Env, telegramId: string, chatId: string, argument: string): Promise<void> {
  const link = await env.DB.prepare("SELECT * FROM telegram_links WHERE telegram_user_id = ?")
    .bind(telegramId).first<{user_id: string; username: string | null}>();
  if (!link) { await send(env, chatId, "Сначала подключите AQUAFLOW командой /link КОД"); return; }
  if (argument === "on" || argument === "off") {
    const enabled = argument === "on";
    await env.DB.prepare("UPDATE telegram_links SET leaderboard_opt_in = ? WHERE telegram_user_id = ?")
      .bind(enabled ? 1 : 0, telegramId).run();
    if (enabled) await refreshLeaderboard(env.DB, link.user_id, link.username, telegramId);
    else await env.DB.prepare("DELETE FROM leaderboard_entries WHERE user_id = ?").bind(link.user_id).run();
    await send(env, chatId, enabled ? "Участие в рейтинге включено." : "Участие в рейтинге выключено.");
    return;
  }
  const rows = await env.DB.prepare("SELECT alias, max_hold_millis FROM leaderboard_entries ORDER BY max_hold_millis DESC LIMIT 10")
    .all<{alias: string; max_hold_millis: number}>();
  if (!rows.results.length) { await send(env, chatId, "Рейтинг пока пуст. Включить: /leaderboard on"); return; }
  const lines = rows.results.map((row, index) => `${index + 1}. ${row.alias} — ${formatDuration(row.max_hold_millis)}`);
  await send(env, chatId, `Рейтинг сообщества*\n\n${lines.join("\n")}\n\n*Результаты пользователей не являются медицински подтверждёнными.`);
}

async function refreshLeaderboard(db: D1Database, userId: string, username: string | null, telegramId: string): Promise<void> {
  const stats = await statsFor(db, userId);
  const alias = username && /^[A-Za-z0-9_]{3,32}$/.test(username) ? `@${username}` : `aquaflow_${telegramId.slice(-4)}`;
  await db.prepare(
    `INSERT INTO leaderboard_entries (user_id, alias, max_hold_millis, attempts, updated_at)
     VALUES (?, ?, ?, ?, ?) ON CONFLICT(user_id) DO UPDATE SET
     alias = excluded.alias, max_hold_millis = excluded.max_hold_millis,
     attempts = excluded.attempts, updated_at = excluded.updated_at`,
  ).bind(userId, alias, stats?.max_hold_millis ?? 0, stats?.attempts ?? 0, Date.now()).run();
}

async function authenticate(request: Request, env: Env): Promise<string> {
  const authorization = request.headers.get("authorization");
  if (!authorization?.startsWith("Bearer ")) throw new Error("unauthorized");
  const tokenHash = await sha256(authorization.slice(7));
  const row = await env.DB.prepare("SELECT user_id FROM install_tokens WHERE token_hash = ?").bind(tokenHash).first<{user_id: string}>();
  if (!row) throw new Error("unauthorized");
  await env.DB.prepare("UPDATE install_tokens SET last_used_at = ? WHERE token_hash = ?").bind(Date.now(), tokenHash).run();
  return row.user_id;
}

async function requireUser(env: Env, telegramId: string, chatId: string): Promise<string | undefined> {
  const userId = await userIdFor(env.DB, telegramId);
  if (!userId) await send(env, chatId, "Сначала подключите AQUAFLOW командой /link КОД");
  return userId;
}

async function userIdFor(db: D1Database, telegramId: string): Promise<string | undefined> {
  return (await db.prepare("SELECT user_id FROM telegram_links WHERE telegram_user_id = ?")
    .bind(telegramId).first<{user_id: string}>())?.user_id;
}

async function statsFor(db: D1Database, userId: string): Promise<StatsRow | null> {
  return db.prepare("SELECT * FROM stats WHERE user_id = ?").bind(userId).first<StatsRow>();
}

async function send(env: Env, chatId: string, text: string): Promise<void> {
  await telegramApi(env, "sendMessage", {chat_id: chatId, text});
}

async function telegramApi(env: Env, method: string, body?: unknown): Promise<{ok: boolean; result: unknown}> {
  const response = await fetch(`https://api.telegram.org/bot${env.TELEGRAM_BOT_TOKEN}/${method}`, {
    method: body === undefined ? "GET" : "POST",
    headers: body === undefined ? undefined : {"content-type": "application/json"},
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const result = await response.json<{ok: boolean; result: unknown}>();
  if (!response.ok || !result.ok) throw new Error(`telegram_${method}_error_${response.status}`);
  return result;
}

async function sha256(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return [...new Uint8Array(digest)].map(byte => byte.toString(16).padStart(2, "0")).join("");
}

function randomCode(): string {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const bytes = crypto.getRandomValues(new Uint8Array(8));
  return [...bytes].map(byte => alphabet[byte % alphabet.length]).join("");
}

function randomToken(bytes: number): string {
  const data = crypto.getRandomValues(new Uint8Array(bytes));
  return btoa(String.fromCharCode(...data)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function json(value: unknown, status = 200): Response {
  return Response.json(value, {status, headers: {"cache-control": "no-store"}});
}

const HELP_TEXT = [
  "/progress — общая статистика", "/record — личный рекорд", "/month — текущий месяц",
  "/leaderboard — рейтинг", "/privacy — приватность", "/unlink — отключить Telegram",
  "/deleteaccount — удалить облачный профиль",
].join("\n");

const PRIVACY_TEXT = "История задержек приватна. Публичный рейтинг выключен по умолчанию и показывает только псевдоним и агрегаты. /unlink удаляет связь с Telegram. /deleteaccount позволяет полностью удалить облачный профиль.";
