import { describe, expect, it } from "vitest";
import { formatMinorUnits, toMinorUnits } from "./money";

describe("money (ADR-022 minor units)", () => {
  it("converts major units to minor units without float drift", () => {
    expect(toMinorUnits(12.34)).toBe(1234);
    expect(toMinorUnits(0.1 + 0.2)).toBe(30);
    expect(toMinorUnits(2500)).toBe(250000);
  });

  it("formats minor units for display", () => {
    expect(formatMinorUnits(1234)).toBe("12.34");
    expect(formatMinorUnits(null)).toBe("—");
    expect(formatMinorUnits(undefined)).toBe("—");
  });
});
