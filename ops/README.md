# Operations

Operational artifacts that the SRE / oncall team uses day-to-day.

```
ops/
├── README.md                       # this file
├── dashboards/                     # Grafana dashboard JSON exports
├── incident-response/              # severity matrix, comms templates
└── oncall/                         # rotation, escalation policy
```

## Status

| Component | Status | Notes |
|-----------|--------|-------|
| Grafana dashboard JSONs | Open | Provisioned by `infra/ansible/roles/monitoring/` |
| Incident severity matrix | Drafted | `incident-response/severity.md` |
| Comms templates | Drafted | `incident-response/comms.md` |
| Oncall rotation | Open | Driven by PagerDuty; exported in `oncall/rotation.yaml` |

## References

- `docs/architecture/observability-design.md`
- `docs/runbooks/` (operational runbooks)
- `infra/monitoring/` (alerting)
