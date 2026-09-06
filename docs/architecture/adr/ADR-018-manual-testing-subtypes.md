# ADR-018: Manual Testing Sub-Types

## Status
**Accepted**

## Context
Per `01_ARS.docx` §5, manual testing sub-types must be defined and confirmed by stakeholders. This ADR establishes the two primary manual testing categories for the Acme Claims platform, complementing the automated test pyramid defined in ADR-013.

## Decision

Two manual testing sub-types are established for the Acme Claims platform:

### 1. User Acceptance Testing (UAT) - Business Process Validation

**Purpose**: Validate that the system supports end-to-end business processes as performed by actual claims processing staff.

**Scope**:
- Complete claim lifecycle workflows (FNOL → Triage → Adjudication → Payout)
- Role-based access and permissions verification
- Business rule application in real-world scenarios
- Integration with external systems (banking, medical providers)
- Exception handling and edge cases

**Participants**:
- Claims adjusters
- Adjudicators
- Finance operations staff
- Compliance officers
- Business analysts

**Environment**: UAT environment with masked production data (per ADR-017)

**Duration**: 2-3 weeks per major release; 3-5 days for minor releases

**Entry Criteria**:
- All automated tests passing (unit, integration, e2e)
- Performance benchmarks met
- Security scan results acceptable
- UAT test cases reviewed and approved

**Exit Criteria**:
- All critical business processes validated
- Zero P1/P2 defects open
- P3 defects documented with workarounds
- Business sign-off from department heads

**Deliverables**:
- UAT test execution logs
- Defect reports with business impact assessment
- UAT Sign-off Certificate
- Go/No-Go recommendation

---

### 2. Operational Readiness Testing (ORT) - Production Deployment Validation

**Purpose**: Validate that the system can be successfully deployed, operated, and supported in the production environment.

**Scope**:
- Deployment procedure validation (Jenkins pipeline execution)
- Rollback procedure testing
- Monitoring and alerting verification
- Backup and restore procedures
- Incident response runbook validation
- Performance under production load
- Security configuration verification
- Disaster recovery failover (quarterly)

**Participants**:
- DevOps engineers
- Database administrators
- Security operations center (SOC)
- Network operations center (NOC)
- Application support team
- Incident commanders

**Environment**: Production environment (during maintenance window) or Production-like staging

**Duration**: 4-8 hours (typically during scheduled maintenance window)

**Entry Criteria**:
- UAT sign-off obtained
- Deployment runbook updated
- Rollback procedure documented
- Monitoring dashboards configured
- On-call staff briefed

**Exit Criteria**:
- Successful deployment verified
- All health checks passing
- Monitoring alerts functional
- Backup completed successfully
- Rollback tested (if applicable)
- ORT Sign-off from Operations Lead

**Deliverables**:
- ORT checklist completion record
- Deployment time metrics
- Rollback time metrics (if tested)
- Monitoring verification log
- ORT Sign-off Certificate

---

## Manual Testing vs. Automated Testing Boundary

| Test Category | Manual | Automated | Rationale |
|---------------|--------|-----------|-----------|
| **Unit Tests** | No | Yes | Fast, repeatable, code-level validation |
| **Integration Tests** | No | Yes | API contract validation, database interactions |
| **E2E Tests** | Partial | Yes (core flows) | Automated for regression; manual for exploratory |
| **UAT (Business Process)** | **Yes** | No | Requires human judgment on business fitness |
| **ORT (Operations)** | **Yes** | Partial | Human verification of operational procedures |
| **Accessibility** | Yes | Partial | Screen reader testing requires human evaluation |
| **Usability** | Yes | No | Subjective user experience assessment |
| **Exploratory** | Yes | No | Unscripted discovery of edge cases |

## UAT Test Case Structure

```markdown
# UAT Test Case: UC-FNOL-001

## Title
Submit First Notice of Loss (FNOL) for Auto Insurance Claim

## Business Process
New claim intake via web portal by policyholder

## Preconditions
- Valid policy exists in system
- User has policyholder account credentials
- Test vehicle data available

## Test Steps
1. Navigate to claims.acme.local
2. Authenticate with policyholder credentials
3. Click "File New Claim"
4. Select policy POL-12345678
5. Enter incident date: [DATE]
6. Enter incident location: [LOCATION]
7. Describe incident: [DESCRIPTION]
8. Upload photos (3 images)
9. Submit FNOL
10. Verify claim number received
11. Verify email confirmation sent
12. Verify claim appears in "My Claims" dashboard

## Expected Results
- Claim created with status "Submitted"
- Claim number format: CLM-YYYYMMDD-####
- Email received within 2 minutes
- Dashboard shows claim in "Pending Triage"

## Pass/Fail Criteria
All expected results must be observed

## Business Sign-off
[ ] Approved by Claims Operations
[ ] Approved by IT Operations
Date: ___________
```

## ORT Checklist Structure

```markdown
# ORT Checklist: ORT-DEPLOY-001

## Deployment Procedure Validation
[ ] Jenkins pipeline triggered successfully
[ ] Build artifact downloaded from Nexus
[ ] Database migration scripts executed
[ ] Application servers restarted in rolling fashion
[ ] Load balancer health checks passing
[ ] Total deployment time < 45 minutes

## Rollback Procedure Validation
[ ] Rollback trigger identified
[ ] Previous version restored from backup
[ ] Database rollback executed (if needed)
[ ] Services restored to previous state
[ ] Total rollback time < 30 minutes

## Monitoring Verification
[ ] Prometheus scraping all endpoints
[ ] Grafana dashboards displaying data
[ ] Alert rules firing for test alerts
[ ] Log aggregation (Loki) receiving logs
[ ] Distributed tracing (Jaeger) capturing spans

## Backup Verification
[ ] Full database backup completed
[ ] Backup integrity check passed
[ ] Backup stored on NAS and off-site
[ ] Backup encryption verified

## Security Verification
[ ] Firewall rules applied correctly
[ ] SSL certificates valid
[ ] Keycloak authentication functional
[ ] Audit logging enabled
[ ] No security scanner warnings

## Sign-off
DevOps Lead: _________________ Date: _______
Security Lead: ________________ Date: _______
Operations Lead: _____________ Date: _______
```

## Scheduling and Coordination

### UAT Schedule

| Release Type | UAT Start | UAT Duration | UAT End | Go-Live Window |
|--------------|-----------|--------------|---------|----------------|
| **Major** (vX.0) | Week 1 Monday | 3 weeks | Week 3 Friday | Week 4 Monday |
| **Minor** (vX.Y) | Week 1 Monday | 1 week | Week 1 Friday | Week 2 Monday |
| **Patch** (vX.Y.Z) | As needed | 2-3 days | Same week | Following Monday |

### ORT Schedule

| Activity | Frequency | Timing | Duration |
|----------|-----------|--------|----------|
| **Deployment ORT** | Every production deployment | Maintenance window (Sat 2-6 AM) | 4 hours |
| **DR Failover ORT** | Quarterly | Scheduled maintenance window | 8 hours |
| **Backup Restore ORT** | Monthly | Off-peak hours | 2 hours |
| **Security Drill ORT** | Semi-annually | Scheduled maintenance window | 4 hours |

## Defect Classification

| Severity | Definition | Response Time | Resolution Target |
|----------|------------|---------------|-------------------|
| **P1 (Critical)** | System unusable; business process blocked | Immediate | 4 hours |
| **P2 (High)** | Major feature broken; workaround difficult | 1 hour | 24 hours |
| **P3 (Medium)** | Minor feature issue; workaround available | 4 hours | 1 week |
| **P4 (Low)** | Cosmetic issue; no business impact | Next business day | Next release |

## Consequences

### Positive
- Clear separation between business validation (UAT) and operational validation (ORT)
- Stakeholder involvement at appropriate stages
- Reduced production incidents through operational readiness verification
- Business confidence through formal UAT sign-off
- Operational confidence through formal ORT sign-off

### Negative
- Manual testing requires significant human resources
- UAT scheduling dependent on business stakeholder availability
- ORT requires production maintenance windows
- Longer release cycles compared to fully automated approaches

### Neutral
- Two sub-types align with industry best practices for regulated industries
- Manual testing complements (not replaces) automated testing
- Documentation overhead balanced by reduced production risk

## Compliance Mapping

| Requirement | Source Document | Compliance Status |
|-------------|-----------------|-------------------|
| Manual testing sub-types defined | `01_ARS.docx` §5.4 | ✅ Compliant |
| UAT process documented | Industry best practice | ✅ Compliant |
| Operational readiness validation | Industry best practice | ✅ Compliant |
| Defect classification | Industry best practice | ✅ Compliant |

## Related ADRs

- ADR-002: Four sequential environments (UAT environment)
- ADR-006: Human approval mandatory for every promotion
- ADR-013: Test pyramid, coverage gates, and required promotion checks
- ADR-015: Backup, DR, and business continuity (DR testing)
- ADR-017: Production data masking and sanitization (UAT data)

## Review Date
This ADR shall be reviewed:
- Annually (testing process improvements)
- After any major production incident (lessons learned)
- Upon stakeholder request for additional testing sub-types
