/**
 * Money is stored and transported as integer minor units (ADR-022); the UI
 * converts to major units only at the presentation edge.
 */
export function toMinorUnits(majorUnits: number): number {
  return Math.round(majorUnits * 100);
}

export function formatMinorUnits(minor: number | null | undefined): string {
  if (minor === null || minor === undefined) {
    return "—";
  }
  return (minor / 100).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
