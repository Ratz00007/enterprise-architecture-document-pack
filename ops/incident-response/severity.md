# Incident Severity Matrix

> Every Sev-1 / Sev-2 / Sev-3 has a definition, a notification
> path, and a default response time. Sev-1 is the only severity
> that pages outside business hours; the rest wait until the
> oncall acknowledges.

| Severity | Definition | Examples | Notify | Response | Resolution target |
|----------|------------|----------|--------|----------|-------------------|
| **Sev-1** | Customer-impacting outage; data loss; security breach | Prod app down; DB unreachable; PII exposure | Page oncall, devops, security, eng manager | 15 min | 4 h |
| **Sev-2** | Degraded service; partial outage; risk of escalation | Error rate > 1% for 15 min; GenAI gateway 5xx > 10% for 30 min | Page oncall, post in #incidents | 1 h | 1 business day |
| **Sev-3** | Latent risk; degradation within SLO but not user-visible | Disk 80%; cert expiring in 14d; promotion stalled 4h | Slack #oncall | 4 h | 5 business days |
| **Sev-4** | Hygiene; track for next sprint | Code smell; outdated dependency; doc drift | Backlog | next sprint | next sprint |

## Escalation

1. **Primary oncall** (always paged first for Sev-1/Sev-2).
2. **Secondary oncall** (paged if primary doesn't ack in 5 min).
3. **Eng manager** (paged if Sev-1 isn't acknowledged in 10 min).
4. **VP Engineering** (paged if Sev-1 is unmitigated in 30 min).

## Status updates

- Sev-1: every 15 min, in #incidents.
- Sev-2: every 30 min, in #incidents.
- Sev-3: at ack + at resolution, in #oncall.

## Postmortem

- Sev-1 always gets a postmortem within 5 business days.
- Sev-2 gets a postmortem if customer impact > 30 min.
- Postmortem template in `comms.md`.
