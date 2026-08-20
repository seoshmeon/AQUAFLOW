import type {Firestore} from "firebase-admin/firestore";
import {createHash} from "node:crypto";
import {formatDuration, type AggregateStats} from "./domain.js";

interface TelegramUser {
  chatId: number;
  username?: string;
  firstName?: string;
}

interface TelegramMessage {
  chat: {id: number};
  from?: {id: number; username?: string; first_name?: string};
  text?: string;
}

export interface TelegramUpdate {
  update_id: number;
  message?: TelegramMessage;
}

export class TelegramBot {
  constructor(
    private readonly db: Firestore,
    private readonly token: string,
  ) {}

  async handle(update: TelegramUpdate): Promise<void> {
    const message = update.message;
    const text = message?.text?.trim();
    if (!message || !text) return;

    const user: TelegramUser = {
      chatId: message.chat.id,
      ...(message.from?.username ? {username: message.from.username} : {}),
      ...(message.from?.first_name ? {firstName: message.from.first_name} : {}),
    };
    const [rawCommand = "", rawArgument = ""] = text.split(/\s+/, 2);
    const command = rawCommand.toLowerCase().split("@")[0];

    switch (command) {
      case "/start":
        await this.start(user);
        break;
      case "/link":
        await this.link(user, rawArgument);
        break;
      case "/unlink":
        await this.unlink(user);
        break;
      case "/progress":
        await this.progress(user);
        break;
      case "/record":
        await this.record(user);
        break;
      case "/month":
        await this.month(user);
        break;
      case "/leaderboard":
        await this.leaderboard(user, rawArgument.toLowerCase());
        break;
      case "/privacy":
        await this.send(user.chatId, PRIVACY_TEXT);
        break;
      default:
        await this.send(user.chatId, HELP_TEXT);
    }
  }

  private async start(user: TelegramUser): Promise<void> {
    const uid = await this.uidFor(user.chatId);
    await this.send(user.chatId, uid
      ? "AQUAFLOW подключён.\n\n" + HELP_TEXT
      : "Это личный кабинет AQUAFLOW. Создайте код подключения в приложении и отправьте:\n/link КОД\n\n" + HELP_TEXT);
  }

  private async link(user: TelegramUser, code: string): Promise<void> {
    if (!/^[A-Z2-9]{8}$/.test(code.toUpperCase())) {
      await this.send(user.chatId, "Код должен состоять из 8 символов. Получите новый код в AQUAFLOW.");
      return;
    }
    const hash = hashCode(code.toUpperCase());
    const codeRef = this.db.collection("linkCodes").doc(hash);
    const telegramRef = this.db.collection("telegramUsers").doc(String(user.chatId));
    let linked = false;
    await this.db.runTransaction(async transaction => {
      const snapshot = await transaction.get(codeRef);
      const data = snapshot.data();
      if (!snapshot.exists || !data || data.claimed === true || Number(data.expiresAt) < Date.now()) return;
      transaction.set(telegramRef, {
        uid: String(data.uid),
        chatId: user.chatId,
        username: user.username ?? null,
        firstName: user.firstName ?? null,
        linkedAt: Date.now(),
        leaderboardOptIn: false,
      });
      transaction.update(codeRef, {claimed: true, claimedAt: Date.now()});
      linked = true;
    });
    await this.send(user.chatId, linked
      ? "Готово — профиль AQUAFLOW подключён. Попробуйте /progress"
      : "Код неверный, уже использован или истёк. Создайте новый код в приложении.");
  }

  private async unlink(user: TelegramUser): Promise<void> {
    await this.db.collection("telegramUsers").doc(String(user.chatId)).delete();
    await this.send(user.chatId, "Telegram отключён. История тренировок AQUAFLOW не удалена.");
  }

  private async progress(user: TelegramUser): Promise<void> {
    const uid = await this.requireUid(user.chatId);
    if (!uid) return;
    const stats = await this.readStats(uid);
    if (!stats || stats.attempts === 0) {
      await this.send(user.chatId, "Синхронизированных подходов пока нет.");
      return;
    }
    await this.send(user.chatId,
      `Ваш прогресс AQUAFLOW\n\nПодходов: ${stats.attempts}\n` +
      `Среднее: ${formatDuration(stats.totalHoldMillis / stats.attempts)}\n` +
      `Рекорд: ${formatDuration(stats.maxHoldMillis)}\n` +
      `Последний: ${formatDuration(stats.latestHoldMillis)}`,
    );
  }

  private async record(user: TelegramUser): Promise<void> {
    const uid = await this.requireUid(user.chatId);
    if (!uid) return;
    const stats = await this.readStats(uid);
    await this.send(user.chatId, stats?.attempts
      ? `Ваш личный рекорд: ${formatDuration(stats.maxHoldMillis)}`
      : "Синхронизированных результатов пока нет.");
  }

  private async month(user: TelegramUser): Promise<void> {
    const uid = await this.requireUid(user.chatId);
    if (!uid) return;
    const period = new Date().toISOString().slice(0, 7);
    const snapshot = await this.db.doc(`users/${uid}/monthly/${period}`).get();
    const stats = snapshot.data() as AggregateStats | undefined;
    await this.send(user.chatId, stats?.attempts
      ? `${period}\n\nПодходов: ${stats.attempts}\nСреднее: ${formatDuration(stats.totalHoldMillis / stats.attempts)}\nЛучшее: ${formatDuration(stats.maxHoldMillis)}`
      : `За ${period} синхронизированных подходов пока нет.`);
  }

  private async leaderboard(user: TelegramUser, argument: string): Promise<void> {
    const linkRef = this.db.collection("telegramUsers").doc(String(user.chatId));
    const link = await linkRef.get();
    const uid = link.data()?.uid as string | undefined;
    if (!uid) {
      await this.send(user.chatId, "Сначала подключите AQUAFLOW командой /link КОД");
      return;
    }
    if (argument === "on" || argument === "off") {
      const optIn = argument === "on";
      await linkRef.update({leaderboardOptIn: optIn});
      const entryRef = this.db.doc(`leaderboards/allTime/entries/${uid}`);
      if (optIn) {
        const stats = await this.readStats(uid);
        await entryRef.set({
          alias: safeAlias(link.data()?.username, user.chatId),
          maxHoldMillis: stats?.maxHoldMillis ?? 0,
          attempts: stats?.attempts ?? 0,
          updatedAt: Date.now(),
        });
      } else {
        await entryRef.delete();
      }
      await this.send(user.chatId, optIn
        ? "Вы включили публичный рейтинг. Видны только псевдоним и сводные показатели."
        : "Вы исключены из публичного рейтинга.");
      return;
    }
    const rows = await this.db.collection("leaderboards/allTime/entries")
      .orderBy("maxHoldMillis", "desc").limit(10).get();
    if (rows.empty) {
      await this.send(user.chatId, "Рейтинг пока пуст. Включить участие: /leaderboard on");
      return;
    }
    const list = rows.docs.map((doc, index) => {
      const data = doc.data();
      return `${index + 1}. ${String(data.alias)} — ${formatDuration(Number(data.maxHoldMillis))}`;
    });
    await this.send(user.chatId,
      `Рейтинг сообщества*\n\n${list.join("\n")}\n\n*Результаты пользователей не являются медицински подтверждёнными.\nУчастие: /leaderboard on или /leaderboard off`,
    );
  }

  private async requireUid(chatId: number): Promise<string | undefined> {
    const uid = await this.uidFor(chatId);
    if (!uid) await this.send(chatId, "Сначала подключите AQUAFLOW командой /link КОД");
    return uid;
  }

  private async uidFor(chatId: number): Promise<string | undefined> {
    const link = await this.db.collection("telegramUsers").doc(String(chatId)).get();
    return link.data()?.uid as string | undefined;
  }

  private async readStats(uid: string): Promise<AggregateStats | undefined> {
    const snapshot = await this.db.collection("stats").doc(uid).get();
    return snapshot.data() as AggregateStats | undefined;
  }

  private async send(chatId: number, text: string): Promise<void> {
    const response = await fetch(`https://api.telegram.org/bot${this.token}/sendMessage`, {
      method: "POST",
      headers: {"content-type": "application/json"},
      body: JSON.stringify({chat_id: chatId, text}),
    });
    if (!response.ok) throw new Error(`Telegram sendMessage failed: ${response.status}`);
  }
}

export function hashCode(code: string): string {
  return createHash("sha256").update(code).digest("hex");
}

function safeAlias(username: unknown, chatId: number): string {
  if (typeof username === "string" && /^[A-Za-z0-9_]{3,32}$/.test(username)) return `@${username}`;
  return `aquaflow_${String(chatId).slice(-4)}`;
}

const HELP_TEXT = [
  "/progress — общая статистика",
  "/record — личный рекорд",
  "/month — текущий месяц",
  "/leaderboard — рейтинг сообщества",
  "/privacy — приватность",
  "/unlink — отключить Telegram",
].join("\n");

const PRIVACY_TEXT = "История задержек приватна. Бот получает доступ только после одноразовой привязки. Публичный рейтинг выключен по умолчанию и показывает только псевдоним и агрегаты. Команда /unlink удаляет связь с Telegram.";
