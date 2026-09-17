# SOC 2 — Control Mapping

> Draft. To be reviewed by the security lead before the external
> audit (post-MVP).

## Common Criteria

| CC | Control | Implementation | Evidence |
|----|---------|----------------|----------|
| CC1.1 | Code of conduct + accountability | `AGENTS.md`, team rotation in `ops/oncall/rotation.yaml` | Updated on hire; reviewed quarterly |
| CC2.1 | Information & communication | `docs/architecture/`, weekly architecture review | ADRs in `docs/architecture/adr/` |
| CC3.1 | Risk assessment | `docs/architecture/adr/INDEX.md` deferred list | Updated when an item is opened/closed |
| CC4.1 | Monitoring | `infra/monitoring/`, `observability-design.md` | Alertmanager config, Grafana dashboards |
| CC5.1 | Control activities | `security-baseline.md` | nftables rules, Keycloak realm |
| CC6.1 | Logical access | `services/auth-realm/`, `docs/architecture/security-baseline.md` | Realm export, role list |
| CC6.6 | External boundaries | `infra/firewall/rules.template` | nftables + Jira tickets for every change |
| CC6.7 | Data in transit | HAProxy + internal CA | Cert inventory, `CertExpiringSoon` alert |
| CC6.8 | Malicious software | Trivy + OWASP Dependency-Check | CI logs, dep-check report |
| CC7.1 | Detection | `infra/monitoring/prometheus/rules/base-alerts.yml` | Alertmanager routing |
| CC7.2 | Anomaly response | `docs/runbooks/`, `ops/incident-response/` | Runbook + postmortem files |
| CC7.3 | Incident response | `ops/incident-response/comms.md` | PagerDuty schedule, escalation matrix |
| CC7.4 | Incident recovery | `docs/runbooks/prod-rollback.md` | Tested in every change window |
| CC8.1 | Change management | `ci/jenkins/shared-library/`, ADRs | PR + ADR for every change |
| CC9.1 | Risk mitigation | ADRs in `docs/architecture/adr/` | Accepted status + date |
| CC9.2 | Vendor management | `ADR-005` (vendor neutrality) | LiteLLM config, provider list |
