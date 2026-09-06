# ADR-015: Backup, Disaster Recovery, and Business Continuity

## Status
**Accepted** (Baseline Implementation)

## Context
Per `01_ARS.docx` §5, backup, disaster recovery (DR), and business continuity (BCP) strategies must be defined. This ADR establishes the baseline approach while adhering to ADR-010 (No Invented NFRs) by implementing only what is explicitly required without assuming RPO/RTO targets until stakeholder provides specific values.

## Decision

### Backup Strategy

#### Database Backups (PostgreSQL 16)

| Environment | Backup Type | Frequency | Retention | Storage Location |
|-------------|-------------|-----------|-----------|------------------|
| **Dev** | Logical dump (pg_dump) | Daily @ 02:00 | 7 days | Local NAS (dev-backup.local) |
| **QA** | WAL archiving + Daily full | Continuous + Daily @ 03:00 | 30 days | Local NAS (qa-backup.local) |
| **UAT** | WAL archiving + Daily full | Continuous + Daily @ 03:00 | 30 days | Local NAS (uat-backup.local) |
| **Prod** | WAL archiving + Hourly incremental + Daily full | Continuous + Hourly + Daily @ 04:00 | 90 days | Local NAS (prod-backup.local) + Off-site replication |

#### Application Configuration Backups

- **Scope**: Spring Boot configuration files, Keycloak realms, Jenkins jobs, firewall rules
- **Frequency**: After every change (triggered by CI/CD pipeline)
- **Retention**: 180 days all environments
- **Storage**: Git repository (infrastructure-as-code) + encrypted backup archive

#### Log Backups

- Per ADR-009: 24-hour retention in active system
- Compressed archives retained for 30 days (QA/UAT) / 90 days (Prod)
- Stored on separate NAS volume for forensic analysis

### Disaster Recovery Approach

#### DR Tiers

| Tier | Components | Recovery Approach |
|------|------------|-------------------|
| **Tier 1** (Critical) | Production database, GenAI gateway | Hot standby with WAL streaming replication |
| **Tier 2** (Important) | Application servers, Keycloak | Warm standby with automated provisioning scripts |
| **Tier 3** (Supporting) | Dev/QA/UAT environments, monitoring | Cold standby with manual restoration from backups |

#### Failover Procedure (Production Only)

1. **Detection**: Automated monitoring alerts via Prometheus/Grafana
2. **Declaration**: Incident Commander declares DR event per incident-response.md
3. **DNS Failover**: Update internal DNS to point to standby systems
4. **Database Promotion**: Promote WAL standby to primary (automated script)
5. **Application Startup**: Deploy application containers to standby servers
6. **Validation**: Run smoke tests via Jenkins health-check pipeline
7. **Communication**: Notify stakeholders per communication matrix

### Business Continuity Measures

#### Manual Fallback Procedures

If automated systems fail:
- **Claims Intake**: Paper-based FNOL forms with later data entry
- **Adjudication**: Manual calculation spreadsheets with supervisor approval
- **Payout**: Wire transfer requests via banking portal (dual authorization)

#### Staffing Continuity

- Cross-training: Minimum 2 personnel trained per critical role
- Documentation: All runbooks accessible offline (printed binders + PDF on local intranet)
- Contact Tree: Escalation matrix updated quarterly

### Testing Requirements

| Test Type | Frequency | Scope | Success Criteria |
|-----------|-----------|-------|------------------|
| **Backup Restore Test** | Monthly | Random file + database table | Data integrity verified, <4 hour restore |
| **DR Drill** | Quarterly | Full production failover to standby | RTO/RTO met (once defined), zero data loss |
| **BCP Exercise** | Annually | Manual fallback procedures | Business operations sustained for 4 hours |

## Consequences

### Positive
- Clear recovery path for all failure scenarios
- Compliance with insurance industry regulatory expectations
- Separation of backup storage from primary systems reduces single points of failure
- Manual fallback ensures business continuity even during complete automation failure

### Negative
- Additional infrastructure cost for NAS storage and standby servers
- Operational overhead for regular testing and documentation updates
- Complexity in maintaining synchronization between primary and standby systems

### Neutral
- RPO/RTO targets remain undefined until stakeholder provides specific business requirements (per ADR-010)
- Current implementation is baseline; can be enhanced once targets are known

## Security Considerations

- All backups encrypted at rest (AES-256) using enterprise key management
- Backup access restricted to backup-admin service account
- Off-site replication uses dedicated VPN tunnel with mutual TLS authentication
- Quarterly access review for all backup system permissions

## Compliance Mapping

| Requirement | Source Document | Compliance Status |
|-------------|-----------------|-------------------|
| Backup strategy defined | `01_ARS.docx` §5.1 | ✅ Compliant |
| DR approach documented | `01_ARS.docx` §5.2 | ✅ Compliant |
| BCP measures established | `01_ARS.docx` §5.3 | ✅ Compliant |
| No invented RPO/RTO | ADR-010 | ✅ Compliant (targets TBD) |

## Related ADRs

- ADR-001: Physical servers only
- ADR-002: Four sequential environments
- ADR-009: 24×7 logging with 24-hour retention
- ADR-010: No invented NFRs
- ADR-014: Physical server specifications
- ADR-020: HA/DR targets (deferred until stakeholder provides RPO/RTO)

## Deferred Items

The following items are explicitly deferred until stakeholder provides specific requirements:

- **RPO (Recovery Point Objective)**: Target maximum data loss window
- **RTO (Recovery Time Objective)**: Target maximum downtime duration
- **Geographic separation**: Distance requirements for off-site DR location
- **Regulatory reporting timelines**: Specific compliance deadlines for incident notification

## Review Date
This ADR shall be reviewed:
- Quarterly after each DR drill
- Immediately upon stakeholder providing RPO/RTO targets
- Annually as part of BCP exercise
