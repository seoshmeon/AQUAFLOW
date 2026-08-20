export const MAX_HOLD_MILLIS = 30 * 60 * 1_000;
export const MAX_RECOVERY_MILLIS = 30 * 60 * 1_000;
export const MAX_SYNC_BATCH = 200;

export interface SyncRecord {
  clientRecordId: string;
  sessionId: string;
  attemptNumber: number;
  holdDurationMillis: number;
  recoveryDurationMillis: number;
  timestamp: number;
  comfortRating?: number;
}

export interface AggregateStats {
  attempts: number;
  totalHoldMillis: number;
  maxHoldMillis: number;
  latestHoldMillis: number;
  latestTimestamp: number;
}

export const EMPTY_STATS: AggregateStats = {
  attempts: 0,
  totalHoldMillis: 0,
  maxHoldMillis: 0,
  latestHoldMillis: 0,
  latestTimestamp: 0,
};

export function validateRecord(value: unknown): SyncRecord {
  if (!value || typeof value !== "object") throw new Error("record must be an object");
  const record = value as Record<string, unknown>;
  const clientRecordId = stringField(record, "clientRecordId", 80);
  const sessionId = stringField(record, "sessionId", 80);
  const attemptNumber = integerField(record, "attemptNumber", 1, 100);
  const holdDurationMillis = integerField(record, "holdDurationMillis", 1, MAX_HOLD_MILLIS);
  const recoveryDurationMillis = integerField(record, "recoveryDurationMillis", 0, MAX_RECOVERY_MILLIS);
  const timestamp = integerField(record, "timestamp", 1, Date.now() + 5 * 60_000);
  const comfortRating = record.comfortRating === undefined
    ? undefined
    : integerField(record, "comfortRating", 0, 5);
  return {
    clientRecordId,
    sessionId,
    attemptNumber,
    holdDurationMillis,
    recoveryDurationMillis,
    timestamp,
    ...(comfortRating === undefined ? {} : { comfortRating }),
  };
}

export function addRecord(stats: AggregateStats, record: SyncRecord): AggregateStats {
  return {
    attempts: stats.attempts + 1,
    totalHoldMillis: stats.totalHoldMillis + record.holdDurationMillis,
    maxHoldMillis: Math.max(stats.maxHoldMillis, record.holdDurationMillis),
    latestHoldMillis: record.timestamp >= stats.latestTimestamp
      ? record.holdDurationMillis
      : stats.latestHoldMillis,
    latestTimestamp: Math.max(stats.latestTimestamp, record.timestamp),
  };
}

export function periodFromTimestamp(timestamp: number): string {
  const date = new Date(timestamp);
  return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, "0")}`;
}

export function formatDuration(milliseconds: number): string {
  const seconds = Math.max(0, Math.round(milliseconds / 1_000));
  const minutes = Math.floor(seconds / 60);
  return `${minutes}:${String(seconds % 60).padStart(2, "0")}`;
}

function stringField(record: Record<string, unknown>, key: string, maxLength: number): string {
  const value = record[key];
  if (typeof value !== "string" || value.length < 1 || value.length > maxLength) {
    throw new Error(`${key} is invalid`);
  }
  return value;
}

function integerField(
  record: Record<string, unknown>,
  key: string,
  min: number,
  max: number,
): number {
  const value = record[key];
  if (typeof value !== "number" || !Number.isSafeInteger(value) || value < min || value > max) {
    throw new Error(`${key} is invalid`);
  }
  return value;
}
