import {describe, expect, it} from "vitest";
import {addRecord, EMPTY_STATS, formatDuration, periodFromTimestamp, validateRecord} from "./domain.js";

describe("AQUAFLOW cloud domain", () => {
  it("aggregates records without losing the latest value", () => {
    const first = validateRecord({
      clientRecordId: "1", sessionId: "s", attemptNumber: 1,
      holdDurationMillis: 65_000, recoveryDurationMillis: 120_000, timestamp: 1_700_000_000_000,
    });
    const second = validateRecord({
      clientRecordId: "2", sessionId: "s", attemptNumber: 2,
      holdDurationMillis: 80_000, recoveryDurationMillis: 120_000, timestamp: 1_700_000_001_000,
    });
    const result = addRecord(addRecord(EMPTY_STATS, first), second);
    expect(result).toMatchObject({attempts: 2, totalHoldMillis: 145_000, maxHoldMillis: 80_000, latestHoldMillis: 80_000});
  });

  it("rejects impossible hold durations", () => {
    expect(() => validateRecord({
      clientRecordId: "1", sessionId: "s", attemptNumber: 1,
      holdDurationMillis: 31 * 60_000, recoveryDurationMillis: 0, timestamp: Date.now(),
    })).toThrow("holdDurationMillis");
  });

  it("formats durations and UTC periods", () => {
    expect(formatDuration(65_400)).toBe("1:05");
    expect(periodFromTimestamp(Date.UTC(2026, 7, 20))).toBe("2026-08");
  });
});
