/**
 * Frontend observability stub.
 * Real implementation wires up OpenTelemetry browser SDK to Tempo
 * and structured console logs to Promtail via the Loki push API.
 * For MVP we use console; the integration is added in the
 * observability workstream.
 */
export interface ObservabilityConfig {
  service: string;
  env: string;
  release: string;
}

export function initObservability(cfg: ObservabilityConfig): void {
  // eslint-disable-next-line no-console
  console.info('[observability] init', cfg);
}

export function logEvent(name: string, attrs: Record<string, unknown>): void {
  // eslint-disable-next-line no-console
  console.info(`[event] ${name}`, attrs);
}
