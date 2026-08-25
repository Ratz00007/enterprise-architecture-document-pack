# Network Topology

> The physical & logical network design, faithful to `03_DINA.docx`.
> Compensating controls for the "same VLAN / subnet" requirement are
> spelled out, not assumed.

## Physical baseline

One physical server per environment, plus dedicated boxes for shared
services (DBs, GenAI, observability stack). The pack calls out the
basic model as one server per environment; we extend that with the
**minimum** additional physical hosts needed to satisfy the two-DB
and central-GenAI requirements.

| Host | Role | CPU | RAM | Disk | NICs | Notes |
|------|------|-----|-----|------|------|-------|
| `dev-app-01` | Dev app server | 8c | 32 GB | 200 GB SSD | 1×10G | developer-facing, no PII |
| `qa-app-01` | QA app server | 8c | 32 GB | 200 GB SSD | 1×10G | runs Testcontainers offload too |
| `qa-db-01` | QA database (Postgres 16) | 8c | 64 GB | 500 GB SSD | 1×10G | QA-data only; see masking rules |
| `uat-app-01` | UAT app server | 8c | 32 GB | 200 GB SSD | 1×10G | isolated user cohort |
| `uat-db-01` | UAT database (Prod-image derived) | 8c | 64 GB | 500 GB SSD | 1×10G | sanitized snapshot of prod |
| `prod-app-01` | Prod app server | 16c | 64 GB | 500 GB SSD | 2×10G (bond) | hot-standby rules TBD |
| `prod-db-01` | Prod database (Postgres 16 primary) | 16c | 128 GB | 1 TB SSD | 2×10G (bond) | backup target: `prod-db-bkp-01` |
| `prod-db-bkp-01` | Prod backup / WAL archive | 4c | 16 GB | 4 TB HDD | 1×10G | pgBackRest repo |
| `genai-01` | Central GenAI gateway (LiteLLM) | 8c | 32 GB | 100 GB SSD | 1×10G | no app code, only inference + audit |
| `mon-01` | Prometheus + Grafana + Loki + Alertmanager | 16c | 64 GB | 2 TB HDD | 1×10G | scrape targets across all envs (read-only creds) |
| `ci-01` | Jenkins controller + agents (2×) | 8c | 32 GB | 500 GB SSD | 1×10G | promotion only, no runtime data |

> **CPU/RAM/disk numbers are TBD** — captured in
> `docs/architecture/adr/ADR-014-host-sizing.md` (TBD). Treat the
> table as a *shape*, not a quote.

## VLAN / subnet model

All hosts live on the same `/24` (e.g. `10.50.0.0/24`). Logical
separation is enforced by:

1. **nftables INPUT chains** — each host accepts traffic only from the
   allow-listed sources defined in `infra/firewall/`.
2. **Per-host IP binding** — services bind to specific IPs, never
   `0.0.0.0` unless explicitly required (e.g. the load balancer).
3. **Per-service port allow-list** — only the ports listed in
   `infra/firewall/rules.template` are open; everything else is
   `DROP` with a `LOG` prefix.
4. **PostgreSQL `pg_hba.conf`** — DBs authenticate by source IP +
   role, not just role.
5. **NTP + chrony** — every host uses the same NTP source so log
   timestamps align (see `infra/timezone/`).

> This is **compensating control**, not real isolation. The
> architecture decision (ADR-003) flags this explicitly. A future
> hardening phase should re-introduce a proper DMZ / segmented VLANs.

## Allowed traffic matrix

| Source | Destination | Port(s) | Purpose | Decision |
|--------|-------------|---------|---------|----------|
| Dev workstation | `dev-app-01` | 22, 8080 | SSH + dev app | Allowed |
| Jenkins (`ci-01`) | `*-app-01` | 22, 8080 | deploy + health check | Allowed |
| `qa-app-01` | `qa-db-01` | 5432 | app → DB | Allowed |
| `uat-app-01` | `uat-db-01` | 5432 | app → DB | Allowed |
| `prod-app-01` | `prod-db-01` | 5432 | app → DB | Allowed |
| `db-sync` (Jenkins job) | `prod-db-01` | 5432 (read-only role) | take sanitized snapshot | Allowed, time-bounded |
| `db-sync` (Jenkins job) | `uat-db-01` | 5432 (write role) | restore sanitized snapshot | Allowed, time-bounded |
| `db-sync` (Jenkins job) | `qa-db-01` | 5432 (write role) | restore sanitized snapshot | Allowed, time-bounded |
| `genai-01` | `*-app-01` | 8443 (HTTPS, allow-listed endpoints) | advisory queries | Allowed per ADR-011 |
| `mon-01` | all hosts | 9100, 9115, 9187, 9090, 8080 (`/actuator/prometheus`) | scrape | Allowed (read-only) |
| `mon-01` | all hosts | syslog 5140/tcp | log ship | Allowed |
| `prod-app-01` | internet | 443 (only) | outbound to GenAI provider if on-prem model not available | **DECISION REQUIRED** (see ADR-011) |
| UAT user subnet | `uat-app-01` | 443 | business validation | Allowed |
| Customer (any) | `prod-app-01` | 443 | customer portal | Allowed (TLS terminated at HAProxy) |
| Anyone | any other source-dest pair | any | n/a | **DENY** + LOG |

The `infra/firewall/rules.template` file is the single source of truth
for these rules; the per-environment files in the same directory
are the rendered outputs.

## DNS

- Internal-only DNS for service discovery (e.g. `claims-api.dev.internal`).
- No public DNS records for non-Prod hosts.
- DNSSEC where the resolver supports it.

## Time

- All hosts run `chrony` against an internal NTP server (eventually
  pool-backed; for now a single on-prem NTP appliance).
- `timedatectl` enforces UTC.
- Drift alerts at > 250 ms to `mon-01`.

## What we are *not* doing (and why)

- **No zero-trust microsegmentation.** Out of scope for the current
  baseline; revisit at the security hardening phase.
- **No WAF in front of the prod app yet.** Will be added in hardening
  phase if stakeholder approves.
- **No IDS/IPS.** Same — flagged for future hardening, not in the
  confirmed baseline.
- **No IPv6 on the internal network.** All internal traffic is IPv4.
  Public-facing prod uses IPv4 + IPv6 dual stack at the edge router.
