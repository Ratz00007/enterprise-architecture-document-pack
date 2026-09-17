# Compliance

Mapping from regulatory and contractual controls to the parts of
the system that satisfy them. This directory is *not* a substitute
for the formal controls themselves — it is the engineering trace
that says "REQ-X is implemented by component Y in file Z".

| Control | Owner | Status | Mapping |
|---------|-------|--------|---------|
| **SOC 2 Type II** | security | Drafted | `soc2.md` |
| **GDPR / CCPA** | security + data | Drafted | `privacy.md` |
| **NAIC (state insurance regs)** | product + legal | Open | `naic.md` |
| **PCI-DSS** (only if we touch card data — we don't in MVP) | n/a | n/a | n/a |

## Cadence

- **Quarterly:** the control owners review the mappings and update
  the references to reflect the current code.
- **Annually:** external audit. The mapping in this directory is
  the input.

## References

- `security-baseline.md`
- `services/audit-log/`
- `docs/domain/entities.md` (PII classification)
