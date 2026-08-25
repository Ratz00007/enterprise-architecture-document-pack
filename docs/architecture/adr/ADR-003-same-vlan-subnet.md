# ADR-003: Same VLAN / subnet

> **Transcription of pack `10_RTM_ADR.docx` §2 ADR-003**, with the
> compensating-control detail from `03_DINA.docx` §5 captured under
> "Operationalisation".

- **Status:** Accepted (with security review)
- **Date:** 2026-08-25
- **Origin:** Pack ADR-003
- **Supersedes:** —

## Context

The stakeholder confirmed that Dev, QA, UAT, and Production all live
on the same VLAN / subnet. Logical separation is achieved through
firewall rules, IP configuration, and port configuration. This is
explicitly called out in the pack as weaker than network-level
isolation; the architecture therefore treats logical separation,
firewall rules, host controls, and strict promotion policy as
compensating controls.

## Decision

- All four environments are on the same `/24` VLAN.
- Logical separation is enforced by:
  1. Per-host nftables INPUT chains (allow-list only).
  2. Per-service port allow-list.
  3. PostgreSQL `pg_hba.conf` IP + role authentication.
  4. Dedicated service accounts per service per host.
  5. Default-deny egress from every host except the GenAI gateway.

## Consequences

**Positive**

- Matches stakeholder direction exactly. No surprise.
- Cross-environment tooling (Prometheus, Loki) is dramatically
  simpler when there is no router hop.

**Negative**

- A compromise of any host can probe any other host on the same
  subnet. The compensating controls must be perfect for this to be
  defensible.
- This ADR is explicitly **not** equivalent to a real DMZ. Anyone
  reviewing the security baseline must understand that.
- A future hardening phase will re-introduce VLAN segmentation; that
  will require its own ADR and stakeholder sign-off.

**Neutral**

- The architecture review gate in `10_RTM_ADR.docx` §3 includes a
  security review step. This ADR is "Accepted with security review"
  to reflect that.

## Operationalisation

The full compensating-control list is in
[`../security-baseline.md`](../security-baseline.md). The traffic
allow-list is in `infra/firewall/`.

- The `infra/firewall/rules.template` is the **only** source of
  truth for what is allowed between hosts. Per-environment files
  are generated from it.
- Every nftables rule change is a PR. CI verifies that the diff
  does not introduce a rule that is too permissive (regex check
  against the known allow-list).
- Pen-test (when stakeholder approves one) is expected to flag
  this ADR as a finding. We will accept the finding and link to
  this ADR as the response.

## References

- `01_ARS.docx` §2, §5
- `02_HLA.docx` §3
- `03_DINA.docx` §3, §5
- `09_Security_Network.docx` §1, §4
- `10_RTM_ADR.docx` §2 ADR-003
- `network-topology.md`
- `security-baseline.md`
