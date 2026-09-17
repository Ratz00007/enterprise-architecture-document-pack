// k6 load test for claims-api.
// Default scenario: 100 RPS, p95 < 400ms, < 1% errors.
// Run against UAT only.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const errors = new Counter('errors');
const latency = new Trend('latency_ms');

export const options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 10,
      duration: '30s',
      tags: { scenario: 'smoke' },
    },
    load: {
      executor: 'constant-arrival-rate',
      rate: 100,
      timeUnit: '1s',
      duration: '5m',
      preAllocatedVUs: 50,
      maxVUs: 200,
      startTime: '30s',
      tags: { scenario: 'load' },
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.01'],
    http_req_duration: ['p(95)<400'],
    errors:            ['count<100'],
  },
};

const BASE = __ENV.BASE_URL || 'https://uat-app.internal.acme';
const TOKEN = __ENV.TOKEN;

export default function () {
  const url = `${BASE}/api/v1/claims?assignedToMe=true`;
  const params = {
    headers: {
      Authorization: `Bearer ${TOKEN}`,
      'Content-Type': 'application/json',
    },
  };
  const r = http.get(url, params);
  latency.add(r.timings.duration);
  const ok = check(r, {
    'status is 200': (x) => x.status === 200,
    'p95 < 400ms (this req)': (x) => x.timings.duration < 400,
  });
  if (!ok) errors.add(1);
  sleep(0.1);
}
