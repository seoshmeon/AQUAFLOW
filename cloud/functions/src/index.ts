import {initializeApp} from "firebase-admin/app";
import {getAuth} from "firebase-admin/auth";
import {FieldValue, getFirestore, type DocumentData} from "firebase-admin/firestore";
import {defineSecret} from "firebase-functions/params";
import {onRequest} from "firebase-functions/v2/https";
import {randomInt} from "node:crypto";
import {
  addRecord,
  EMPTY_STATS,
  MAX_SYNC_BATCH,
  periodFromTimestamp,
  validateRecord,
  type AggregateStats,
  type SyncRecord,
} from "./domain.js";
import {hashCode, TelegramBot, type TelegramUpdate} from "./telegram.js";

initializeApp();
const db = getFirestore();
const botToken = defineSecret("TELEGRAM_BOT_TOKEN");
const webhookSecret = defineSecret("TELEGRAM_WEBHOOK_SECRET");

export const telegramWebhook = onRequest(
  {region: "europe-west1", secrets: [botToken, webhookSecret], timeoutSeconds: 30},
  async (request, response) => {
    if (request.method !== "POST") {
      response.status(405).send("Method Not Allowed");
      return;
    }
    if (request.header("x-telegram-bot-api-secret-token") !== webhookSecret.value()) {
      response.status(401).send("Unauthorized");
      return;
    }
    try {
      await new TelegramBot(db, botToken.value()).handle(request.body as TelegramUpdate);
      response.status(204).send();
    } catch (error) {
      console.error("Telegram update failed", error);
      response.status(500).send("Internal Server Error");
    }
  },
);

export const api = onRequest(
  {region: "europe-west1", timeoutSeconds: 60},
  async (request, response) => {
    response.set("Cache-Control", "no-store");
    if (request.method !== "POST") {
      response.status(405).json({error: "method_not_allowed"});
      return;
    }
    try {
      const uid = await authenticatedUid(request.header("authorization"));
      if (request.path === "/link/start") {
        const code = createLinkCode();
        await db.collection("linkCodes").doc(hashCode(code)).set({
          uid,
          expiresAt: Date.now() + 10 * 60_000,
          claimed: false,
          createdAt: Date.now(),
        });
        response.status(201).json({code, expiresInSeconds: 600});
        return;
      }
      if (request.path === "/records/sync") {
        const rawRecords = (request.body as {records?: unknown})?.records;
        if (!Array.isArray(rawRecords) || rawRecords.length > MAX_SYNC_BATCH) {
          response.status(400).json({error: "invalid_batch"});
          return;
        }
        const records = rawRecords.map(validateRecord);
        let inserted = 0;
        for (const record of records) inserted += await syncRecord(uid, record);
        response.status(200).json({accepted: inserted, duplicates: records.length - inserted});
        return;
      }
      response.status(404).json({error: "not_found"});
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown_error";
      const status = message === "unauthorized" ? 401 : 400;
      response.status(status).json({error: message});
    }
  },
);

async function authenticatedUid(authorization: string | undefined): Promise<string> {
  if (!authorization?.startsWith("Bearer ")) throw new Error("unauthorized");
  const token = authorization.slice("Bearer ".length);
  try {
    return (await getAuth().verifyIdToken(token, true)).uid;
  } catch {
    throw new Error("unauthorized");
  }
}

async function syncRecord(uid: string, record: SyncRecord): Promise<number> {
  const recordRef = db.doc(`users/${uid}/records/${record.clientRecordId}`);
  const statsRef = db.doc(`stats/${uid}`);
  const monthlyRef = db.doc(`users/${uid}/monthly/${periodFromTimestamp(record.timestamp)}`);
  return db.runTransaction(async transaction => {
    const existing = await transaction.get(recordRef);
    if (existing.exists) return 0;
    const [statsSnapshot, monthlySnapshot] = await Promise.all([
      transaction.get(statsRef),
      transaction.get(monthlyRef),
    ]);
    const stats = statsFrom(statsSnapshot.data());
    const monthly = statsFrom(monthlySnapshot.data());
    transaction.create(recordRef, {...record, syncedAt: FieldValue.serverTimestamp()});
    transaction.set(statsRef, {...addRecord(stats, record), updatedAt: FieldValue.serverTimestamp()});
    transaction.set(monthlyRef, {...addRecord(monthly, record), updatedAt: FieldValue.serverTimestamp()});
    return 1;
  });
}

function statsFrom(data: DocumentData | undefined): AggregateStats {
  if (!data) return EMPTY_STATS;
  return {
    attempts: Number(data.attempts) || 0,
    totalHoldMillis: Number(data.totalHoldMillis) || 0,
    maxHoldMillis: Number(data.maxHoldMillis) || 0,
    latestHoldMillis: Number(data.latestHoldMillis) || 0,
    latestTimestamp: Number(data.latestTimestamp) || 0,
  };
}

function createLinkCode(): string {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  return Array.from({length: 8}, () => alphabet[randomInt(alphabet.length)]).join("");
}
