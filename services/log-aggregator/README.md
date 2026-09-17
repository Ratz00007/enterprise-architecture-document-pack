# Log Aggregator

The log shipping stack. Lives on every host (deployed by the
`common` Ansible role) and on the monitoring host (where Loki +
Promtail run).

## Stack

| Layer | Tool | Notes |
|-------|------|-------|
| Source (apps) | logback → stdout → journald | One JSON object per line |
| Source (system) | rsyslog + journald | One structured record per line |
| Rotation | logrotate (24h, copy-truncate) | Per `ADR-009` |
| Ship | Promtail | Labelled by `env`, `service`, `host` |
| Store | Loki (TSDB chunks + filesystem index) | 24h hot retention |
| Archive | S3-compatible block (WORM) | 30d cold retention |
| Query | Grafana → LogQL | Same Grafana instance as metrics |

## PII handling

The application logger encodes PII fields through a custom
`masked` conversion word (see `apps/claims-api/src/main/resources/logback-spring.xml`).
Any field on the deny-list is replaced with `[REDACTED]` before
the line is written.

## Status

| Component | Status | Notes |
|-----------|--------|-------|
| Promtail config | Drafted | See `promtail.yml` |
| Loki config | Drafted | See `infra/monitoring/loki/loki.yml` |
| Archive | Open | Block storage target TBD |
| Log-based alerts | Open | Mirror of `base-alerts.yml` |

## References

- `ADR-009` 24h log retention
- `observability-design.md`
- `infra/monitoring/`
