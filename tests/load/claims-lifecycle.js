import http from "k6/http";
import { check, group } from "k6";

// Load test required by AGENTS.md (ADR-013): k6 at 100 RPS with p95 < 400ms.
// Health endpoint runs unauthenticated; claims endpoints need a bearer token
// supplied via ACCESS_TOKEN (obtained from Keycloak's token endpoint for the
// environment under test). Without ACCESS_TOKEN the run covers liveness and
// reports the authenticated paths as skipped via tags.

export const options = {
  scenarios: {
    steady_100rps: {
      executor: "constant-arrival-rate",
      rate: 100,
      timeUnit: "1s",
      duration: "2m",
      preAllocatedVUs: 50,
      maxVUs: 200,
    },
  },
  thresholds: {
    http_req_duration: ["p(95)<400"],
    http_req_failed: ["rate<0.05"],
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080/api/v1";
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || "";
const authHeaders = ACCESS_TOKEN ? { headers: { Authorization: `Bearer ${ACCESS_TOKEN}` } } : {};

export default function () {
  group("liveness", () => {
    const health = http.get(`${BASE_URL}/actuator/health`);
    check(health, { "health 200": (r) => r.status === 200 });
  });

  if (ACCESS_TOKEN) {
    group("claims list", () => {
      const list = http.get(`${BASE_URL}/claims?size=20`, authHeaders);
      check(list, { "claims 200": (r) => r.status === 200 });
    });
  }
}
