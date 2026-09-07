import axios from "axios";
import { useAuth } from "react-oidc-context";

export const IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

export function newIdempotencyKey(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `idem-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

/** Attaches the access token and, for mutating verbs, a fresh idempotency key (ADR-023). */
export function attachClaimsApiHeaders(config: {
  method?: string;
  headers: Record<string, string>;
  getAccessToken: () => string | undefined;
}): Record<string, string> {
  const token = config.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (config.method && config.method.toUpperCase() !== "GET") {
    config.headers[IDEMPOTENCY_KEY_HEADER] = newIdempotencyKey();
  }
  return config.headers;
}

export function useClaimsApi() {
  const auth = useAuth();
  const client = axios.create({ baseURL: "/api/v1" });
  client.interceptors.request.use((config) => {
    attachClaimsApiHeaders({
      method: config.method,
      headers: config.headers as Record<string, string>,
      getAccessToken: () => auth.user?.access_token,
    });
    return config;
  });
  return client;
}
