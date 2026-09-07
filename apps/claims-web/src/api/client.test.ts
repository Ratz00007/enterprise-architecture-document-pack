import { describe, expect, it } from "vitest";
import { IDEMPOTENCY_KEY_HEADER, attachClaimsApiHeaders } from "./client";

describe("claims API headers", () => {
  const token = "test-token";

  it("attaches the bearer token when authenticated", () => {
    const headers = attachClaimsApiHeaders({
      method: "get",
      headers: {},
      getAccessToken: () => token,
    });
    expect(headers.Authorization).toBe(`Bearer ${token}`);
    expect(headers[IDEMPOTENCY_KEY_HEADER]).toBeUndefined();
  });

  it("attaches a fresh idempotency key on mutating verbs (ADR-023)", () => {
    for (const method of ["post", "put", "delete"]) {
      const headers = attachClaimsApiHeaders({ method, headers: {}, getAccessToken: () => token });
      expect(headers[IDEMPOTENCY_KEY_HEADER]).toMatch(/^idem-|[0-9a-f-]{36}/);
    }
  });

  it("generates distinct keys per request", () => {
    const first = attachClaimsApiHeaders({ method: "post", headers: {}, getAccessToken: () => token });
    const second = attachClaimsApiHeaders({ method: "post", headers: {}, getAccessToken: () => token });
    expect(first[IDEMPOTENCY_KEY_HEADER]).not.toBe(second[IDEMPOTENCY_KEY_HEADER]);
  });

  it("omits the key on GET", () => {
    const headers = attachClaimsApiHeaders({ method: "get", headers: {}, getAccessToken: () => token });
    expect(headers[IDEMPOTENCY_KEY_HEADER]).toBeUndefined();
  });
});
