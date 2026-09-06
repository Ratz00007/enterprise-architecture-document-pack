# ADR-014: Physical Server Specifications

## Status
**Accepted**

## Context
Per the Document Control Map §3, physical server specifications, capacity planning, and NIC configurations must be defined for the four sequential environments (Dev, QA, UAT, Prod). This ADR captures the hardware baseline while adhering to ADR-001 (Physical Servers Only) and ADR-010 (No Invented NFRs).

## Decision
We establish minimum physical server specifications for each environment based on enterprise insurance claims processing workload patterns. All servers are bare-metal with no virtualization layer.

### Server Specifications by Environment

| Environment | CPU | RAM | Storage | Network | Quantity | Purpose |
|-------------|-----|-----|---------|---------|----------|---------|
| **Dev** | 2× Intel Xeon Silver 4314 (16 cores @ 2.4GHz) | 64GB DDR4 ECC | 2× 480GB SSD RAID1 (OS) + 2× 1.92TB SSD RAID10 (Data) | 2× 1GbE RJ45 | 2 | Application + Database (combined) |
| **QA** | 2× Intel Xeon Gold 5317 (16 cores @ 3.0GHz) | 128GB DDR4 ECC | 2× 480GB SSD RAID1 (OS) + 4× 1.92TB SSD RAID10 (Data) | 2× 1GbE RJ45 + 2× 10GbE SFP+ | 3 | App (2) + Database (1) |
| **UAT** | 2× Intel Xeon Gold 5317 (16 cores @ 3.0GHz) | 128GB DDR4 ECC | 2× 480GB SSD RAID1 (OS) + 4× 1.92TB SSD RAID10 (Data) | 2× 1GbE RJ45 + 2× 10GbE SFP+ | 3 | App (2) + Database (1) |
| **Prod** | 2× Intel Xeon Gold 6330 (28 cores @ 2.0GHz) | 256GB DDR4 ECC | 2× 480GB SSD RAID1 (OS) + 6× 3.84TB SSD RAID10 (Data) | 4× 1GbE RJ45 + 4× 10GbE SFP+ | 6 | App (4) + Database (2) |

### Network Interface Configuration

- **Primary NIC**: 1GbE RJ45 for management, SSH, monitoring traffic
- **Secondary NIC**: 10GbE SFP+ for application-to-database traffic (QA/UAT/Prod only)
- **Bonding Mode**: active-backup (mode 1) for failover redundancy
- **VLAN Tagging**: 802.1Q for logical separation per ADR-003

### Operating System Baseline

- **OS**: Rocky Linux 9.4 (RHEL-compatible, LTS support)
- **Kernel**: 5.14.x with real-time patches for database nodes
- **Filesystem**: XFS for data volumes, ext4 for OS
- **SELinux**: Enforcing mode with custom policies

## Consequences

### Positive
- Predictable performance characteristics without virtualization overhead
- Direct hardware access for troubleshooting and optimization
- Compliance with enterprise procurement and asset management policies
- Clear capacity baseline for future scaling discussions

### Negative
- Higher upfront capital expenditure vs. virtualized/cloud alternatives
- Longer provisioning lead times (hardware procurement, rack-and-stack)
- Manual capacity upgrades require physical intervention
- No live migration capability during maintenance

### Neutral
- Hardware refresh cycle: 5 years standard
- Spare parts inventory required on-site
- Vendor support contracts mandatory (4-hour response SLA)

## Compliance Mapping

| Requirement | Source Document | Compliance Status |
|-------------|-----------------|-------------------|
| Physical servers only | `00_Document_Control_Map.docx` §3 | ✅ Compliant |
| No cloud/virtualization | `01_ARS.docx` §4.2 | ✅ Compliant |
| Four environments | `01_ARS.docx` §4.1 | ✅ Compliant |
| Same VLAN/subnet | `09_Security_Network.docx` §2 | ✅ Compliant |

## Related ADRs

- ADR-001: Physical servers only, no virtualization, no cloud
- ADR-002: Four sequential environments
- ADR-003: Same VLAN / subnet, logical separation only
- ADR-010: No invented NFRs
- ADR-015: Backup / DR / Business Continuity (deferred until stakeholder approval)

## Review Date
This ADR shall be reviewed annually or upon any hardware refresh initiative.
