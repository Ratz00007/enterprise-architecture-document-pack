# Security Baseline

> The minimum confirmed security controls, faithfully transcribed from
> `09_Security_Network.docx`, plus the **compensating controls** we
> layer on top of the "same VLAN / subnet" requirement so the baseline
> is defensible, not aspirational.

## Confirmed controls (from the pack)

1. **Basic firewall.** nftables INPUT chains on every host.
2. **IP configuration.** Each host has a documented, fixed IP.
3. **Port configuration.** Only documented ports are open; everything
   else is `DROP` + `LOG`.
4. **Logical environment separation despite common VLAN/subnet.** See
   "Compensating controls" below.

> This is what the stakeholder confirmed. Anything beyond this is a
> recommendation that needs a new ADR + sign-off.

## Compensating controls (because the network is one VLAN)

| Risk | Mitigation |
|------|------------|
| Any compromised host can probe any other | nftables INPUT drops everything not on the allow-list |
| Lateral movement via DB port | `pg_hba.conf` restricts by source IP + role; app credentials are role-scoped (no `postgres` superuser) |
| Log tampering | journald is append-only; Promtail ships to a separate Loki tenant per env; archive bucket is WORM |
| Credential theft | No long-lived SSH keys — all admin access via TOTP + short-lived cert from a bastion (see `infra/firewall/bastion.md`, TBD) |
| Privilege escalation | All processes run as dedicated service accounts; `systemd` `NoNewPrivileges=yes`, `ProtectSystem=strict`, `PrivateTmp=yes` |
| Insider exfiltration | Audit log is write-once, replicated off-host, retention 7 years |
| Compromised GenAI gateway | `genai-01` has no DB credentials, no deploy credentials, no admin access to any other host |
| Outbound exfiltration to internet | Default-deny egress; only the GenAI gateway has an allow-list for outbound 443 to the LLM provider endpoint (if not on-prem) |

## Identity & access

- **Authentication:** OIDC via Keycloak. Every human, every service,
  every CI agent has a Keycloak subject.
- **Authorization model:** RBAC, with a small number of fixed roles
  matching the operating roles in `11_Operations_Runbooks.docx`:
  `developer`, `qa_engineer`, `uat_user`, `devops_operator`,
  `prod_operator`, `genai_service_owner`, `auditor` (read-only).
- **Service-to-service:** OAuth2 client credentials with mTLS at the
  load balancer. No shared API keys.
- **MFA:** TOTP for every human, mandatory for any role that can
  approve a promotion or read raw audit logs.

> **Not yet specified** (per `09_Security_Network.docx` §3): PAM,
> secrets manager (post-MVP), encryption-at-rest for DBs beyond what
> the OS provides, vulnerability management cadence, audit retention
> specifics beyond "long enough". Each of these gets its own ADR
> before implementation.

## Secrets management

- **MVP:** Ansible Vault. Vault password lives in Jenkins credentials
  store. Never in git.
- **Post-MVP:** HashiCorp Vault, with auto-rotation. ADR-015 (TBD).
- **Database credentials** are rotated every 90 days, automated by a
  Jenkins job.
- **API client secrets** are rotated every 180 days.

## Encryption

- **In transit:** TLS 1.3 minimum. Internal services use an internal
  CA rooted in Keycloak's PKI. Public-facing uses Let's Encrypt (or
  enterprise CA for prod, TBD).
- **At rest:** OS-level LUKS on the DB hosts. Application-level column
  encryption for `policy_holder_pii` (deterministic, AES-GCM, key in
  Vault — post-MVP).
- **Backups:** Encrypted at rest with a separate key from the live
  DB. Restoration is logged as a Sev-2 change.

## Vulnerability management

- **Dependencies:** OWASP Dependency-Check (every build) +
  Trivy (every container) gates promotion.
- **Code:** SpotBugs + Checkstyle + PMD + Semgrep (post-MVP).
- **DAST:** OWASP ZAP baseline scan in the QA gate.
- **Cadence:** monthly review of all open findings; Sev-1 / Sev-2
  must have a remediation plan within 7 days.

## What this document is not

It is not a complete enterprise security architecture. The pack
explicitly flags the topics we are not yet covering (identity
provider specifics, RBAC depth, secrets management depth, encryption
depth, certificate management, endpoint protection, vulnerability
management, privileged access management, audit retention). Each of
those gets its own ADR before implementation. The architecture
warning in `09_Security_Network.docx` §4 stands.
