export const MAX_HOLD_MILLIS = 30 * 60_000;
export const MAX_RECOVERY_MILLIS = 30 * 60_000;
export const MAX_SYNC_BATCH = 100;

export interface SyncRecord {
  clientRecordId: string;
  sessionId: string;
  attemptNumber: number;
  holdDurationMillis: number;
  recoveryDurationMillis: number;
  timestamp: number;
  comfortRating?: number;
}

export interface StatsRow {
  attempts: number;
  total_hold_millis: number;
  max_hold_millis: number;
  latest_hold_millis: number;
  latest_timestamp: number;
}

export function validateRecord(value: unknown, now = Date.now()): SyncRecord {
  if (!value || typeof value !== "object") throw new Error("record must be an object");
  const source = value as Record<string, unknown>;
  const comfortRating = source.comfortRating === undefined
    ? undefined
    : integer(source, "comfortRating", 0, 5);
  return {
    clientRecordId: text(source, "clientRecordId", 80),
    sessionId: text(source, "sessionId", 80),
    attemptNumber: integer(source, "attemptNumber", 1, 100),
    holdDurationMillis: integer(source, "holdDurationMillis", 1, MAX_HOLD_MILLIS),
    recoveryDurationMillis: integer(source, "recoveryDurationMillis", 0, MAX_RECOVERY_MILLIS),
    timestamp: integer(source, "timestamp", 1, now + 5 * 60_000),
    ...(comfortRating === undefined ? {} : {comfortRating}),
  };
}

export function periodFromTimestamp(timestamp: number): string {
  return new Date(timestamp).toISOString().slice(0, 7);
}

export function formatDuration(milliseconds: number): string {
  const seconds = Math.max(0, Math.round(milliseconds / 1_000));
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
}

export function parseCommand(text: string): {command: string; argument: string} {
  const [rawCommand = "", argument = ""] = text.trim().split(/\s+/, 2);
  return {command: rawCommand.toLowerCase().split("@")[0] ?? "", argument};
}

function text(source: Record<string, unknown>, key: string, max: number): string {
  const value = source[key];
  if (typeof value !== "string" || value.length < 1 || value.length > max) throw new Error(`${key} is invalid`);
  return value;
}

function integer(source: Record<string, unknown>, key: string, min: number, max: number): number {
  const value = source[key];
  if (typeof value !== "number" || !Number.isSafeInteger(value) || value < min || value > max) {
    throw new Error(`${key} is invalid`);
  }
  return value;
}
