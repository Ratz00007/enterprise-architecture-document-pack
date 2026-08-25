# ADR-001: Physical servers only

> **Transcription of pack `10_RTM_ADR.docx` §2 ADR-001**, with project-side
> implementation notes added under "Operationalisation".

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** Pack ADR-001
- **Supersedes:** —

## Context

The stakeholder explicitly excluded cloud infrastructure,
virtualization, Citrix, and VDI from the current scope. The basic
infrastructure model is one physical server per environment (Dev,
QA, UAT, Prod). This is a confirmed constraint, not a recommendation.

## Decision

The current baseline runs on physical servers only. No cloud
infrastructure. No virtualization (no VMware ESXi, no KVM, no
Hyper-V, no Proxmox). No container-orchestration platforms that
require a virtualized substrate. No VDI, no Citrix.

The minimum extension to that model is the addition of dedicated
physical hosts for the shared services the pack requires
(central GenAI, observability stack, DB backups, CI controller).
This is documented in `network-topology.md`.

## Consequences

**Positive**

- Eliminates an entire class of "is the hypervisor patched" risk.
- Procurement conversation is straightforward — one quote per box.
- Aligns with the stakeholder's confirmed scope.

**Negative**

- We do not get cloud-style elasticity, but `ADR-010` forbids
  inventing that requirement.
- Hardware refresh cycles become visible operational projects.
- The "one server per environment" model is a single point of
  failure for each environment; this is acknowledged in
  `ADR-010` and explicitly *not* mitigated until the stakeholder
  supplies RPO/RTO targets.

**Neutral**

- Future hardening phase may add virtualization for non-prod, but
  that requires a new ADR and stakeholder sign-off.

## Operationalisation

- **Ansible** is the only tool that touches the hosts. No
  configuration drift outside of Git.
- **Buildah** + **skopeo** are used for container image build/push
  because they do not require a Docker daemon (no daemon = no
  always-on service that could be confused with a hypervisor).
- **systemd** is the init / service manager. Unit files live in
  `infra/systemd/`.
- **NTP** is via `chrony` against an internal server.

## References

- `01_ARS.docx` §2, §4
- `03_DINA.docx` §1
- `10_RTM_ADR.docx` §2 ADR-001
- `network-topology.md`
