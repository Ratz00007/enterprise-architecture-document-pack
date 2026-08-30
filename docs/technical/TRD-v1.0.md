# Technical Requirements Document (TRD)
## Acme Claims Processing Platform
**Version:** 1.0  
**Status:** Approved  
**Date:** October 26, 2023  
**Owner:** Enterprise Architecture Team  
**Related Documents:** PRD-v1.0.md, ADR-000 through ADR-020  

---

## 1. System Overview
This document translates the Product Requirements (PRD) into detailed technical specifications for the Acme Claims Processing Platform. It defines the architecture, technology stack, data models, interfaces, and infrastructure constraints.

## 2. Architecture Principles
- **Physical Only:** No virtualization, containers, or cloud services (ADR-014).
- **Sequential Promotion:** Dev → QA → UAT → Prod with manual gates (ADR-003).
- **Network Isolation:** Single VLAN/Subnet per environment with firewall ACLs (ADR-004).
- **Centralized AI:** All GenAI calls route through internal gateway (ADR-005).
- **Data Sovereignty:** Two-database sync with PII masking (ADR-017).

## 3. Technology Stack

### 3.1 Backend
| Component | Technology | Version | Justification |
|-----------|------------|---------|---------------|
| Language | Java | 21 (LTS) | Long-term support, performance, enterprise ecosystem |
| Framework | Spring Boot | 3.3.x | Rapid development, security, observability integration |
| Build Tool | Maven | 3.9.x | Dependency management, reproducible builds |
| ORM | Hibernate | 6.5.x | JPA implementation, caching, lazy loading |

### 3.2 Frontend
| Component | Technology | Version | Justification |
|-----------|------------|---------|---------------|
| Framework | React | 18.3.x | Component-based, large ecosystem, performance |
| Language | TypeScript | 5.4.x | Type safety, IDE support, maintainability |
| State Management | Redux Toolkit | 2.2.x | Predictable state, dev tools |
| UI Library | Material UI | 5.15.x | Accessibility, theming, enterprise components |
| Build Tool | Vite | 5.2.x | Fast builds, HMR, optimized production bundles |

### 3.3 Database
| Component | Technology | Version | Justification |
|-----------|------------|---------|---------------|
| Primary DB | PostgreSQL | 16.x | ACID compliance, JSONB support, on-prem friendly |
| Analytics DB | PostgreSQL | 16.x | Masked data for reporting/BI |
| Sync Tool | Custom Java Service | 1.0 | Real-time PII masking during replication |

### 3.4 Infrastructure
| Component | Technology | Version | Justification |
|-----------|------------|---------|---------------|
| OS | Rocky Linux | 9.4 | RHEL-compatible, stable, long support cycle |
| Web Server | Apache httpd | 2.4.x | Reverse proxy, SSL termination |
| App Server | Embedded Tomcat | 10.1.x | Spring Boot default, production-ready |
| CI/CD | Jenkins | 2.440.x | On-prem automation, plugin ecosystem |
| Auth | Keycloak | 24.0.x | OIDC/OAuth2, RBAC, MFA, on-prem |
| Monitoring | Prometheus | 2.50.x | Metrics collection, alerting |
| Visualization | Grafana | 10.3.x | Dashboards, alerting rules |
| Logging | Loki | 2.9.x | Log aggregation, cost-effective |
| Tracing | Tempo | 2.4.x | Distributed tracing, OpenTelemetry |

## 4. Data Model

### 4.1 Core Entities

#### Claim
```sql
CREATE TABLE claim (
    claim_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_number VARCHAR(20) NOT NULL,
    date_of_loss DATE NOT NULL,
    reported_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL, -- NEW, TRIAGE, ADJUDICATION, APPROVED, PAID, REJECTED
    category VARCHAR(20), -- AUTO, PROPERTY, LIABILITY
    risk_score INTEGER, -- 1-100
    assigned_adjuster_id UUID,
    total_amount DECIMAL(15,2),
    payout_amount DECIMAL(15,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0
);
```

#### ClaimDocument
```sql
CREATE TABLE claim_document (
    document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claim(claim_id),
    document_type VARCHAR(50), -- POLICY, PHOTO, RECEIPT, MEDICAL_RECORD
    file_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT,
    mime_type VARCHAR(100),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by VARCHAR(100)
);
```

#### ClaimActivity
```sql
CREATE TABLE claim_activity (
    activity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claim(claim_id),
    activity_type VARCHAR(50), -- SUBMITTED, ASSIGNED, TRIAGED, DECISION, PAYOUT
    description TEXT,
    performed_by VARCHAR(100),
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);
```

#### Adjuster
```sql
CREATE TABLE adjuster (
    adjuster_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(100) UNIQUE NOT NULL, -- Keycloak ID
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255),
    level VARCHAR(20), -- JUNIOR, SENIOR, MANAGER
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 4.2 Indexes
```sql
CREATE INDEX idx_claim_status ON claim(status);
CREATE INDEX idx_claim_policy ON claim(policy_number);
CREATE INDEX idx_claim_date_loss ON claim(date_of_loss);
CREATE INDEX idx_claim_adjuster ON claim(assigned_adjuster_id);
CREATE INDEX idx_document_claim ON claim_document(claim_id);
CREATE INDEX idx_activity_claim ON claim_activity(claim_id);
```

## 5. API Specifications

### 5.1 REST Endpoints

#### FNOL Submission
```
POST /api/v1/claims/fnol
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "policyNumber": "POL-123456",
  "dateOfLoss": "2023-10-25",
  "description": "Rear-end collision at intersection...",
  "location": {
    "address": "123 Main St",
    "city": "Springfield",
    "state": "IL",
    "zipCode": "62701"
  },
  "involvedParties": [...],
  "documents": [...]
}

Response (201 Created):
{
  "claimId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "NEW",
  "submittedAt": "2023-10-26T10:30:00Z"
}
```

#### Get Claim Details
```
GET /api/v1/claims/{claimId}
Authorization: Bearer {token}

Response (200 OK):
{
  "claimId": "...",
  "policyNumber": "...",
  "status": "TRIAGE",
  "category": "AUTO",
  "riskScore": 45,
  "assignedAdjuster": {...},
  "timeline": [...],
  "documents": [...],
  "aiSummary": "Claim involves minor vehicle damage..."
}
```

#### Triage Decision
```
POST /api/v1/claims/{claimId}/triage
Authorization: Bearer {token}

Request Body:
{
  "category": "AUTO",
  "riskScore": 45,
  "recommendedAction": "FAST_TRACK",
  "notes": "Clear liability, low damage estimate"
}

Response (200 OK):
{
  "claimId": "...",
  "newStatus": "ADJUDICATION",
  "assignedTo": "adjuster-uuid"
}
```

#### Adjudication Decision
```
POST /api/v1/claims/{claimId}/adjudicate
Authorization: Bearer {token}

Request Body:
{
  "decision": "APPROVE",
  "payoutAmount": 4500.00,
  "reasoning": "Damage assessment aligns with policy coverage...",
  "requiresSeniorApproval": false
}

Response (200 OK):
{
  "claimId": "...",
  "newStatus": "APPROVED",
  "nextStep": "PAYOUT_PROCESSING"
}
```

### 5.2 Error Responses
```json
{
  "timestamp": "2023-10-26T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Policy number is inactive",
  "path": "/api/v1/claims/fnol",
  "traceId": "abc123xyz"
}
```

## 6. Security Architecture

### 6.1 Authentication Flow
1. User navigates to application.
2. Redirect to Keycloak login page (OIDC Authorization Code Flow).
3. User authenticates (MFA if required).
4. Keycloak returns authorization code.
5. Backend exchanges code for access token + ID token.
6. Access token used for all subsequent API calls.

### 6.2 Role-Based Access Control (RBAC)
| Role | Permissions |
|------|-------------|
| ROLE_POLICYHOLDER | Submit FNOL, View own claims |
| ROLE_ADJUSTER_JUNIOR | View assigned claims, Draft decisions (<$10k) |
| ROLE_ADJUSTER_SENIOR | View all claims, Approve decisions (<$50k) |
| ROLE_ADJUDICATOR | Approve decisions (>$50k), Override flags |
| ROLE_FINANCE | Process payouts, View financial reports |
| ROLE_COMPLIANCE | View audit logs, Run compliance reports |
| ROLE_ADMIN | User management, System configuration |

### 6.3 PII Masking Rules (Two-DB Sync)
| Field | Operational DB | Analytics DB |
|-------|----------------|--------------|
| SSN | Full (XXX-XX-XXXX) | Last 4 only (XXXX) |
| Name | Full | Initial + Last (J. Doe) |
| Address | Full | City, State only |
| Phone | Full | Area code + XXXX |
| Email | Full | Domain only (@example.com) |
| Bank Account | Full | Last 4 digits |

## 7. GenAI Gateway Integration

### 7.1 Request Flow
```
[Claim Service] → [PII Masking] → [GenAI Gateway] → [LLM Provider]
                                              ↓
                                    [Response Cache]
                                              ↓
[Claim Service] ← [Unmask References] ← [Gateway Response]
```

### 7.2 Prompt Templates
```yaml
triage_classification: |
  You are an insurance claims expert. Analyze the following claim description 
  and categorize it. Return JSON with category, riskScore (1-100), and reasoning.
  
  Claim Description: {{maskedDescription}}
  Policy Type: {{policyType}}
  Historical Claims: {{historicalCount}}
  
  Output Format: {"category": "...", "riskScore": N, "reasoning": "..."}

decision_draft: |
  Summarize the evidence for claim {{claimId}} and draft a decision recommendation.
  Consider: policy coverage, damage assessment, liability determination.
  
  Evidence Summary: {{maskedEvidence}}
  Policy Limits: {{policyLimits}}
  
  Output: Decision draft with approve/reject recommendation and reasoning.
```

### 7.3 Retry & Fallback Strategy
- **Retry Count:** 3 attempts with exponential backoff (1s, 2s, 4s).
- **Timeout:** 30 seconds per request.
- **Fallback:** Queue for async processing if gateway unavailable.
- **Circuit Breaker:** Open after 5 consecutive failures (5-minute recovery).

## 8. Observability

### 8.1 Metrics (Prometheus)
- `http_requests_total` - Counter by endpoint, status, method
- `http_request_duration_seconds` - Histogram of latency
- `claim_processing_time_seconds` - Time from FNOL to payout
- `genai_gateway_latency_seconds` - AI call duration
- `db_connection_pool_active` - Active DB connections
- `jvm_memory_used_bytes` - Heap usage

### 8.2 Logging (Loki)
- **Format:** JSON structured logging
- **Levels:** ERROR, WARN, INFO, DEBUG
- **Correlation:** Trace ID injected in all logs (MDC)
- **Retention:** 30 days (Prod), 7 days (Dev/QA/UAT)

### 8.3 Tracing (Tempo/OpenTelemetry)
- Trace FNOL → Triage → Adjudication → Payout workflow
- Capture GenAI gateway calls as separate spans
- Sample rate: 10% (Prod), 100% (Dev/QA/UAT)

## 9. Deployment Architecture

### 9.1 Server Specifications (Per Environment)
| Environment | App Servers | DB Servers | Jenkins | Keycloak | Monitoring |
|-------------|-------------|------------|---------|----------|------------|
| Dev | 2x (8-core, 32GB) | 1x (8-core, 32GB) | Shared | Shared | Shared |
| QA | 2x (8-core, 32GB) | 1x (8-core, 32GB) | Shared | Shared | Shared |
| UAT | 3x (16-core, 64GB) | 2x (16-core, 64GB) | Dedicated | Dedicated | Dedicated |
| Prod | 5x (32-core, 128GB) | 3x (32-core, 128GB, HA) | Dedicated | Dedicated (HA) | Dedicated (HA) |

### 9.2 Network Topology
```
[External Users] → [Firewall] → [Load Balancer] → [App Servers (VLAN 100)]
                                              ↓
                                       [DB Servers (VLAN 100)]
                                              ↓
                                       [Monitoring Stack (VLAN 100)]
```
- All servers on same subnet (e.g., 10.0.100.0/24)
- Firewall rules restrict ports: 443 (HTTPS), 5432 (PostgreSQL), 8080 (Internal)
- No direct internet access from app/db servers

## 10. Testing Strategy

### 10.1 Automated Testing Pyramid
- **Unit Tests:** 70% coverage minimum (JUnit 5, Mockito)
- **Integration Tests:** API contract testing (Testcontainers for PostgreSQL)
- **E2E Tests:** Critical user journeys (Playwright/Selenium)
- **Performance Tests:** Load testing (Gatling/JMeter)

### 10.2 Manual Testing (UAT & ORT)
- **UAT:** Business users validate functional requirements
- **ORT:** Operations team validates backup, restore, monitoring, failover

## 11. Backup & Disaster Recovery

### 11.1 Backup Schedule
| Environment | Frequency | Retention | Type |
|-------------|-----------|-----------|------|
| Dev | Daily | 7 days | Logical (pg_dump) |
| QA | Daily | 14 days | Logical (pg_dump) |
| UAT | Daily + Weekly | 30 days | Logical + Physical |
| Prod | Hourly (WAL) + Daily | 90 days | Physical (pg_basebackup) + Offsite |

### 11.2 DR Scenarios
- **Server Failure:** Automatic failover to standby (Prod DB HA)
- **Data Center Loss:** Restore from offsite backups (RTO < 4 hours)
- **Data Corruption:** Point-in-time recovery using WAL archives

## 12. Compliance & Audit
- **Audit Trail:** All claim state changes logged with user, timestamp, old/new values
- **Data Retention:** 7 years for closed claims (regulatory requirement)
- **Access Reviews:** Quarterly review of user roles and permissions
- **Penetration Testing:** Annual third-party security assessment

## 13. Appendices
- Appendix A: Complete ER Diagram
- Appendix B: API Swagger/OpenAPI Specification
- Appendix C: Keycloak Realm Configuration JSON
- Appendix D: Prometheus Alert Rules YAML
- Appendix E: Jenkins Pipeline Script Reference

---

**Document Control**
| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2023-10-26 | Architecture Team | Initial release |
