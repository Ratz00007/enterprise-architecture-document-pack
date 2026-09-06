# ADR-020: High Availability and Disaster Recovery Targets

## Status
**Accepted** (Baseline - Targets TBD)

## Context
Per `01_ARS.docx` §4 and ADR-010 (No Invented NFRs), specific High Availability (HA) and Disaster Recovery (DR) targets including RPO (Recovery Point Objective) and RTO (Recovery Time Objective) must be defined by stakeholders. This ADR establishes the baseline HA/DR architecture while explicitly deferring specific numeric targets until stakeholder provides business requirements.

## Decision

### Baseline HA/DR Architecture

The Acme Claims platform implements a tiered HA/DR approach with different strategies per component and environment. Specific RPO/RTO targets are **explicitly deferred** pending stakeholder input.

---

## High Availability Strategy

### Component Tier Classification

| Tier | Components | HA Approach | Redundancy Level | Failover Type |
|------|------------|-------------|------------------|---------------|
| **Tier 1 (Critical)** | Production Database, GenAI Gateway | Active-Standby with replication | 2× minimum | Automatic (database), Manual (gateway) |
| **Tier 2 (Important)** | Application Servers, Keycloak, Load Balancers | Active-Active cluster | N+1 | Automatic (load balancer directed) |
| **Tier 3 (Supporting)** | CI/CD (Jenkins), Monitoring, Logging | Active-Passive | 1× primary + cold standby | Manual |
| **Tier 4 (Non-Critical)** | Dev/QA/UAT Environments | No HA | Single instance | Manual restoration from backup |

### Production Environment HA Design

#### Database High Availability

```
┌─────────────────┐         ┌─────────────────┐
│   Primary DB    │◄───────►│   Standby DB    │
│   (Read/Write)  │  WAL    │   (Read-Only)   │
│                 │ Streaming│                 │
│ - Active        │◄───────►│ - Hot Standby   │
│ - Accepts writes│         │ - Ready to promote│
└────────┬────────┘         └─────────────────┘
         │
         ▼
┌─────────────────┐
│   PgBouncer     │
│ (Connection Pool)│
└─────────────────┘
```

**Mechanism**: PostgreSQL 16 native streaming replication with synchronous commit option (configurable based on final RPO target).

**Failover Process**:
1. Automatic detection of primary failure (via Patroni or manual monitoring)
2. Standby promotion triggered (automatic or manual per final RTO target)
3. Application connection pool reconfiguration
4. DNS update (if using DNS-based failover)
5. Validation of promoted primary

**Current Configuration**: Asynchronous replication (baseline); can be upgraded to synchronous once RPO is defined.

#### Application Server High Availability

```
                    ┌───────────────┐
                    │   HAProxy     │
                    │ (Load Balancer)│
                    └───────┬───────┘
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
         ▼                  ▼                  ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   App Svr 1 │    │   App Svr 2 │    │   App Svr 3 │
│   (Active)  │    │   (Active)  │    │   (Active)  │
└─────────────┘    └─────────────┘    └─────────────┘
```

**Mechanism**: HAProxy load balancing across multiple application server instances with health checks.

**Health Check Configuration**:
- Interval: 5 seconds
- Unhealthy threshold: 3 consecutive failures
- Healthy threshold: 2 consecutive successes
- Timeout: 3 seconds

**Scaling**: Minimum 3 instances for production; can handle loss of 1 instance without capacity impact.

#### Keycloak High Availability

```
                    ┌───────────────┐
                    │   HAProxy     │
                    │ (Load Balancer)│
                    └───────┬───────┘
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
         ▼                  ▼                  ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Keycloak 1 │    │  Keycloak 2 │    │  Keycloak 3│
│   (Active)  │    │   (Active)  │    │   (Active) │
│             │    │             │    │            │
│ ┌─────────┐ │    │ ┌─────────┐ │    │ ┌────────┐ │
│ │ Infinispan│◄─┼───►│ Infinispan│◄─┼───►│Infinispan││
│ │ (Cache)  │ │    │ │ (Cache)  │ │    │ │(Cache) │ │
│ └─────────┘ │    │ └─────────┘ │    │ └────────┘ │
└─────────────┘    └─────────────┘    └─────────────┘
         │                  │                  │
         └──────────────────┼──────────────────┘
                            │
                            ▼
                   ┌─────────────────┐
                   │ External DB     │
                   │ (PostgreSQL HA) │
                   └─────────────────┘
```

**Mechanism**: Keycloak clustering with Infinispan for session distribution and external database for persistence.

---

## Disaster Recovery Strategy

### DR Site Options

| Option | Description | Cost | Complexity | Recommended For |
|--------|-------------|------|------------|-----------------|
| **Option A: Cold Site** | Empty rack space with power/network; hardware procured on-demand | Low | Low | RTO > 72 hours |
| **Option B: Warm Site** | Hardware pre-positioned; data restored from backups | Medium | Medium | RTO 4-72 hours |
| **Option C: Hot Site** | Fully synchronized standby environment; near-instant failover | High | High | RTO < 4 hours |
| **Option D: Active-Active** | Both sites serving traffic simultaneously | Very High | Very High | RTO < 15 minutes |

**Baseline Selection**: **Option B (Warm Site)** - Can be upgraded based on final RTO target.

### DR Architecture (Warm Site Baseline)

```
┌─────────────────────────────────┐      ┌─────────────────────────────────┐
│       PRIMARY SITE              │      │        DR SITE                  │
│                                 │      │                                 │
│  ┌─────────────────────────┐   │      │   ┌─────────────────────────┐   │
│  │   Production Servers    │   │      │   │   Standby Servers       │   │
│  │   - App (4 instances)   │   │      │   │   - App (4 instances)   │   │
│  │   - DB (Primary + Stdby)│   │      │   │   - DB (Cold Standby)   │   │
│  │   - Keycloak (3 nodes)  │   │      │   │   - Keycloak (3 nodes)  │   │
│  │   - HAProxy (2 nodes)   │   │      │   │   - HAProxy (2 nodes)   │   │
│  └─────────────────────────┘   │      │   └─────────────────────────┘   │
│              │                  │      │              │                  │
│              ▼                  │      │              ▼                  │
│  ┌─────────────────────────┐   │      │   ┌─────────────────────────┐   │
│  │   Local Backup NAS      │   │      │   │   DR Backup NAS         │   │
│  │   - Hourly DB backups   │───┼──────┼───►│   - Replicated backups  │   │
│  │   - Daily full backups  │   │      │   │   - Restore capability  │   │
│  └─────────────────────────┘   │      │   └─────────────────────────┘   │
└─────────────────────────────────┘      └─────────────────────────────────┘
                    │                                         │
                    │            Dedicated VPN Tunnel         │
                    │         (Encrypted, 1Gbps minimum)      │
                    └─────────────────────────────────────────┘
```

### Data Replication for DR

| Data Type | Replication Method | Frequency | Lag (Baseline) |
|-----------|-------------------|-----------|----------------|
| **Database** | WAL archiving to off-site | Continuous | 5-15 minutes |
| **Backups** | Encrypted transfer to DR NAS | Hourly + Daily | 1-2 hours |
| **Configuration** | Git repository (off-site mirror) | Every commit | Near-zero |
| **Logs** | Not replicated (per ADR-009) | N/A | N/A |

---

## Deferred Targets (Awaiting Stakeholder Input)

### RPO (Recovery Point Objective)

**Definition**: Maximum acceptable amount of data loss measured in time.

**Options for Stakeholder Decision**:

| RPO Target | Technical Requirement | Infrastructure Impact | Estimated Cost Impact |
|------------|----------------------|----------------------|----------------------|
| **< 1 minute** | Synchronous database replication to DR site | Fiber connection, DR site within 100km | +150-200% |
| **< 15 minutes** | Asynchronous replication with frequent WAL shipping | Standard internet connection acceptable | +50-75% |
| **< 1 hour** | Hourly backup replication to DR | Any reliable connection | +25-35% |
| **< 24 hours** | Daily backup replication to DR | Minimal connectivity requirement | +10-15% |
| **> 24 hours** | Weekly/monthly backup to DR | Tape or offline transfer acceptable | +5-10% |

**Baseline Assumption**: RPO < 1 hour (can be adjusted upon stakeholder decision)

---

### RTO (Recovery Time Objective)

**Definition**: Maximum acceptable downtime duration.

**Options for Stakeholder Decision**:

| RTO Target | DR Site Type | Automation Required | Staffing Requirement | Estimated Cost Impact |
|------------|--------------|---------------------|---------------------|----------------------|
| **< 15 minutes** | Active-Active | Fully automated failover | 24/7 NOC | +200-300% |
| **< 1 hour** | Hot Site | Automated with manual approval | 24/7 on-call | +100-150% |
| **< 4 hours** | Warm Site | Semi-automated scripts | Business hours + on-call | +50-75% |
| **< 24 hours** | Warm Site | Manual procedures | Business hours | +25-35% |
| **< 72 hours** | Cold Site | Manual procedures | Next business day | +10-20% |

**Baseline Assumption**: RTO < 4 hours (can be adjusted upon stakeholder decision)

---

## Testing Requirements

### HA Testing

| Test | Frequency | Success Criteria | Owner |
|------|-----------|------------------|-------|
| **Database failover** | Monthly | Standby promotes within [RTO], zero/some data loss per [RPO] | DBA Lead |
| **App server failure** | Weekly | Load balancer redirects traffic within 30 seconds | DevOps Lead |
| **Keycloak node failure** | Monthly | Sessions preserved, no authentication failures | Security Lead |
| **Load balancer failover** | Quarterly | Backup LB takes over within 60 seconds | Network Lead |

### DR Testing

| Test | Frequency | Success Criteria | Owner |
|------|-----------|------------------|-------|
| **Backup restore drill** | Monthly | Random table restored within 1 hour, integrity verified | DBA Lead |
| **Full DR failover** | Quarterly (minimum) | All services operational at DR site within [RTO] | DR Coordinator |
| **DR failback** | After each DR test | Services returned to primary site without data loss | DR Coordinator |
| **Communication drill** | Semi-annually | All stakeholders notified within 15 minutes of DR declaration | Communications Lead |

---

## DR Declaration and Escalation

### Authority to Declare DR Event

| Role | Can Declare DR | Conditions |
|------|---------------|------------|
| **CISO** | Yes | Security incident, data breach, ransomware |
| **CTO** | Yes | Extended outage, infrastructure failure |
| **VP Operations** | Yes | Natural disaster, facility inaccessible |
| **Incident Commander** | Yes (with CISO/CTO approval) | P1 incident exceeding 30 minutes |
| **On-Call Engineer** | No | Can recommend escalation |

### DR Declaration Process

1. **Detection**: Monitoring alerts or user reports indicate potential DR-level incident
2. **Assessment**: Incident Commander evaluates severity against DR criteria
3. **Recommendation**: Incident Commander recommends DR declaration to CISO/CTO
4. **Decision**: CISO/CTO approves or rejects DR declaration
5. **Notification**: DR team activated via PagerDuty + phone tree
6. **Execution**: DR runbook executed per incident-response.md
7. **Communication**: Stakeholders, customers, regulators notified as required
8. **Documentation**: All actions logged for post-incident review

---

## Consequences

### Positive
- Clear tiered approach matches component criticality
- Baseline architecture supports range of RPO/RTO targets
- Explicit stakeholder decision points prevent assumptions
- Regular testing ensures DR readiness
- Compliance with insurance industry regulatory expectations

### Negative
- Final costs unknown until RPO/RTO targets defined
- Potential rework if stakeholder selects aggressive targets
- DR site maintenance ongoing cost regardless of usage
- Testing requires dedicated resources and maintenance windows

### Neutral
- Baseline (warm site, RPO<1h, RTO<4h) is reasonable starting point
- Architecture designed for flexibility to accommodate various targets
- DR strategy can evolve as business requirements mature

---

## Compliance Mapping

| Requirement | Source Document | Compliance Status |
|-------------|-----------------|-------------------|
| HA/DR targets defined | `01_ARS.docx` §4.3 | ⚠️ Pending stakeholder input |
| DR testing documented | Industry best practice | ✅ Compliant (baseline) |
| No invented NFRs | ADR-010 | ✅ Compliant (targets explicitly deferred) |
| Physical server constraints | ADR-001 | ✅ Compliant |

---

## Related ADRs

- ADR-001: Physical servers only
- ADR-002: Four sequential environments
- ADR-009: 24×7 logging with 24-hour retention
- ADR-010: No invented NFRs
- ADR-014: Physical server specifications
- ADR-015: Backup, DR, and business continuity
- ADR-019: Observability tooling selection

---

## Action Items for Stakeholders

1. **Define RPO Target**: Select maximum acceptable data loss window
2. **Define RTO Target**: Select maximum acceptable downtime duration
3. **Approve DR Site Type**: Based on RTO selection and budget
4. **Identify DR Location**: Geographic requirements, distance from primary
5. **Assign DR Budget**: Capital and operational expenditure approval
6. **Designate DR Team**: Roles, responsibilities, contact information
7. **Schedule First DR Test**: Timeline for initial full failover exercise

---

## Review Date

This ADR shall be reviewed:
- **Immediately** upon stakeholder providing RPO/RTO targets (requires update with specific numbers)
- Quarterly (DR test results review)
- Annually (full DR strategy assessment)
- After any DR event or failed DR test (lessons learned)

---

## Appendix: RPO/RTO Decision Matrix

| Business Impact | Recommended RPO | Recommended RTO | Approximate Cost Multiplier |
|-----------------|-----------------|-----------------|----------------------------|
| **Critical** (Business cannot function) | < 15 minutes | < 1 hour | 2.5-3.0× |
| **High** (Severe business degradation) | < 1 hour | < 4 hours | 1.5-2.0× |
| **Medium** (Noticeable impact, workarounds exist) | < 4 hours | < 24 hours | 1.2-1.5× |
| **Low** (Minor inconvenience) | < 24 hours | < 72 hours | 1.1-1.2× |

**Note**: Final selection requires stakeholder business impact analysis and budget approval.
