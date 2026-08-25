# Observability Design

> Two distinct things the pack asks for, by name:
> 1. **Continuous logging** — 24×7 generation, 24-hour retention/reset.
> 2. **Issue / service monitoring** — separate, real-time alerting on
>    errors and service health.
> We refuse to conflate them. This document is how they're wired.

## The two functions, side by side

| | Logging | Issue monitoring |
|--|---------|------------------|
| **Purpose** | Record what happened, for forensics, audit, and RCA | Tell someone *right now* that something is wrong |
| **Retention** | 24h raw, then archived (see "Archive" below) | No retention; only active state + alert history |
| **Volume expectation** | 100–500 MB/day per env, peaks 1 GB/day | Tiny — just metrics + alert state |
| **Schema** | Structured JSON, OpenTelemetry resource attributes | Prometheus metrics + Alertmanager alerts |
| **Consumer** | Loki, searched via Grafana | Prometheus, Alertmanager, PagerDuty-equivalent |
| **Who reads it** | SRE, security, GenAI RCA | SRE oncall, QA, release operator |
| **Latency tolerance** | Seconds (batch) | Seconds (push) — alert must fire before user notices |
| **Pack reference** | REQ-17, ADR-009 | REQ-18 |

## Logging pipeline

```
[app] ── logback JSON ──▶ stdout ── journald ──▶ Promtail ──▶ Loki ──▶ Grafana
                                  │                               │
                                  └─ 24h local rotation (LOGROTATE)─┘
```

- **Application logs** are emitted as one JSON object per line to
  stdout, with at minimum: `ts`, `level`, `service`, `env`, `trace_id`,
  `span_id`, `msg`, plus domain fields.
- **journald** is the local transport. `LOGROTATE` rotates at 24h
  with a copy-truncate so the boundary never drops a line.
- **Promtail** ships to Loki, labelled by `env` and `service`.
- **Loki retention** is 24h for hot storage, 30d for cold (S3-compatible
  block storage) — see "Archive" below for the boundary semantics.

### Log format

```json
{
  "ts": "2026-08-25T10:14:01.234Z",
  "level": "INFO",
  "service": "claims-api",
  "env": "prod",
  "trace_id": "8c1d2e3a4b5c6d7e8f9a0b1c",
  "span_id": "1d2e3f4a5b6c7d8e9f0a1b2c",
  "logger": "com.acme.claims.application.SubmitClaim",
  "msg": "claim submitted",
  "claim_id": "CLM-2026-00012345",
  "user_id": "u_8a7b6c5d",
  "decision": "accepted",
  "duration_ms": 142
}
```

### PII handling

- **No raw PII in logs by default.** The encoder has a deny-list:
  `ssn`, `dob`, `policy_holder_name`, `email`, `phone`, `address`,
  `card_number`, `bank_account`.
- The `claim_id` and `user_id` are opaque IDs, not the underlying
  record. To look up the record, an operator uses the audit log
  (which is access-controlled and time-bounded).
- If a PII field is *required* for a specific log line, it must go
  through a `pii(...)` wrapper that records the access in the audit
  log.

### Archive

- The 24h retention is the **hot** retention (Loki + local journal).
- A nightly job (driven by the same Ansible role that rotates logs)
  ships the previous day's journal to the S3-compatible block
  storage for 30 days (WORM — write once read many, for compliance).
- After 30 days, the archive is purged unless a legal hold is open.
- 24h is for *raw* logs; **derived events** (e.g. aggregated counters)
  follow a separate retention policy documented in the SRE runbook.

## Issue / service monitoring pipeline

```
[exporter] ── /metrics ──▶ Prometheus (mon-01) ──▶ Alertmanager ──▶ Pager (operator)
                                                       │
                                                       └─▶ GenAI RCA (advisory, async)
```

- Every host runs **node_exporter** (system metrics) and
  **postgres_exporter** on the DB hosts.
- The Spring Boot app exposes **/actuator/prometheus** with Micrometer.
- The React web app emits **Real User Monitoring (RUM)** via the
  OpenTelemetry browser SDK to a separate Tempo instance.
- Alert rules are in `infra/monitoring/prometheus/rules/`.
- Alertmanager routes by severity, hours-of-day, and oncall rotation.
- A copy of every `firing` alert is also delivered to the GenAI
  gateway for **optional** RCA summarisation. GenAI never gates the
  alert itself.

### What we alert on (default set)

| Alert | Severity | Window | Notes |
|-------|----------|--------|-------|
| `HostDown` | Sev-2 | 1 min | node_exporter unreachable |
| `DbDown` | Sev-1 | 30 sec | postgres_exporter unreachable |
| `HighErrorRate` | Sev-2 | 5 min, > 1% 5xx on app | Per env, per service |
| `LatencyP95Breach` | Sev-3 | 15 min, p95 > 1s | No NFR target set, so threshold is "user-noticeable" |
| `LogRotationStuck` | Sev-2 | 25h | Rotation not completed in 24h window |
| `PromotionStalled` | Sev-3 | 4h | Candidate waiting for approval > 4h |
| `GenAIGatewayDegraded` | Sev-3 | 10 min | Provider 5xx > 10% or p95 > 10s |
| `DiskPressure` | Sev-2 | 80% in 1h, 90% in 15m | Per host |
| `CertExpiringSoon` | Sev-3 | 14 days | TLS cert renewal |

> No invented NFRs. Thresholds are "user-noticeable" defaults; tune
> after we have a real data set in UAT.

## Dashboards (Grafana)

- **Per-env overview** — host health, app health, DB health, error
  rate, latency, throughput.
- **Pipeline view** — current promotion state, last N releases,
  rollback frequency.
- **GenAI view** — request volume, provider mix, p50/p95 latency,
  cost (if metering on), audit-trail completeness.
- **Audit view** — promotion approvals, role assertions, PII
  accesses.

JSON dashboards are in `infra/monitoring/grafana/dashboards/` and
loaded by the Ansible role.

## Runbook hooks

Every alert links to a runbook in `docs/runbooks/`. If a runbook is
missing, the alert is **not** allowed to be Sev-1 — that's a
contract.

## What we are *not* doing (yet)

- **No full distributed tracing across the GenAI gateway.** The
  gateway logs its own trace context, but we do not (yet) pipe its
  spans into Tempo. Tracked in `multi-agent-plan.md`.
- **No AIOps auto-remediation.** The pack is explicit: AI recommends,
  humans act.
- **No synthetic monitoring from outside the data center.** The pack
  does not call for it; flagged for hardening phase.
