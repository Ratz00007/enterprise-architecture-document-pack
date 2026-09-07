import { describe, expect, it } from "vitest";
import { allowedActions } from "./transitions";

describe("claim lifecycle actions (ADR-021)", () => {
  it("follows the happy path", () => {
    expect(allowedActions("FNOL").map((a) => a.action)).toEqual(["triage"]);
    expect(allowedActions("TRIAGE").map((a) => a.action)).toEqual(["adjudication"]);
    expect(allowedActions("ADJUDICATION").map((a) => a.action)).toEqual(["approve", "reject"]);
    expect(allowedActions("APPROVED").map((a) => a.action)).toEqual(["payout"]);
    expect(allowedActions("PAYOUT").map((a) => a.action)).toEqual(["payout/complete"]);
    expect(allowedActions("PAID").map((a) => a.action)).toEqual(["close"]);
    expect(allowedActions("REJECTED").map((a) => a.action)).toEqual(["close"]);
  });

  it("makes CLOSED terminal", () => {
    expect(allowedActions("CLOSED")).toEqual([]);
  });
});
