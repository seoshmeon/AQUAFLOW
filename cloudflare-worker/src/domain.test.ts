import {describe, expect, it} from "vitest";
import {formatDuration, parseCommand, periodFromTimestamp, validateRecord} from "./domain";

describe("AQUAFLOW Worker domain", () => {
  it("validates a normal record", () => {
    expect(validateRecord({
      clientRecordId: "r1", sessionId: "s1", attemptNumber: 1,
      holdDurationMillis: 65_000, recoveryDurationMillis: 120_000,
      timestamp: Date.UTC(2026, 7, 20), comfortRating: 4,
    }, Date.UTC(2026, 7, 21))).toMatchObject({holdDurationMillis: 65_000, comfortRating: 4});
  });

  it("rejects impossible values", () => {
    expect(() => validateRecord({
      clientRecordId: "r1", sessionId: "s1", attemptNumber: 1,
      holdDurationMillis: 31 * 60_000, recoveryDurationMillis: 0, timestamp: Date.now(),
    })).toThrow("holdDurationMillis");
  });

  it("formats durations, periods and commands", () => {
    expect(formatDuration(65_400)).toBe("1:05");
    expect(periodFromTimestamp(Date.UTC(2026, 7, 20))).toBe("2026-08");
    expect(parseCommand("/Progress@AquaFlowBot now")).toEqual({command: "/progress", argument: "now"});
  });
});
