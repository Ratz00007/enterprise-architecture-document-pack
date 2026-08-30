# System Design Document (SDD)
## Acme Claims Processing Platform
**Version:** 1.0  
**Status:** Approved  
**Date:** October 26, 2023  
**Owner:** Enterprise Architecture Team  
**Related Documents:** TRD-v1.0.md, ADR-000 through ADR-020  

---

## 1. Introduction

### 1.1 Purpose
This System Design Document (SDD) provides a detailed technical blueprint for implementing the Acme Claims Processing Platform. It translates requirements from the PRD and TRD into actionable design specifications for development teams.

### 1.2 Scope
This document covers:
- High-level architecture diagrams
- Component design and interfaces
- Database schema and data flow
- Security architecture
- Integration patterns
- Deployment topology

### 1.3 Definitions & Acronyms
| Term | Definition |
|------|------------|
| FNOL | First Notice of Loss |
| PII | Personally Identifiable Information |
| PHI | Protected Health Information |
| OIDC | OpenID Connect |
| OAuth2 | Open Authorization 2.0 |
| RBAC | Role-Based Access Control |
| API | Application Programming Interface |
| ETL | Extract, Transform, Load |
| WAL | Write-Ahead Logging |
| RPO | Recovery Point Objective |
| RTO | Recovery Time Objective |

---

## 2. Architecture Overview

### 2.1 High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         EXTERNAL USERS                              │
│    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐            │
│    │ Policyholder │  │   Adjuster   │  │   Admin      │            │
│    │   (Web/Mob)  │  │  (Web Portal)│  │  (Dashboard) │            │
│    └──────┬───────┘  └──────┬───────┘  └──────┬───────┘            │
└───────────┼─────────────────┼─────────────────┼─────────────────────┘
            │                 │                 │
            ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      FIREWALL (ACL Rules)                           │
│         Ports: 443 (HTTPS), 8443 (Internal), 9090 (Monitoring)      │
└─────────────────────────────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    LOAD BALANCER (Hardware - F5)                    │
│                  SSL Termination, Health Checks                     │
└─────────────────────────────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      APPLICATION TIER (VLAN 100)                    │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐   │
│  │  App Node 1 │ │  App Node 2 │ │  App Node 3 │ │  App Node N │   │
│  │  (Tomcat)   │ │  (Tomcat)   │ │  (Tomcat)   │ │  (Tomcat)   │   │
│  │  Spring Boot│ │  Spring Boot│ │  Spring Boot│ │  Spring Boot│   │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └──────┬──────┘   │
└─────────┼───────────────┼───────────────┼───────────────┼──────────┘
          │               │               │               │
          └───────────────┴───────┬───────┴───────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────────────┐
│   DATABASE TIER │   │   AUTH TIER     │   │   MONITORING TIER       │
│  ┌───────────┐  │   │  ┌───────────┐  │   │  ┌───────────────────┐  │
│  │ Primary   │  │   │  │ Keycloak  │  │   │  │ Prometheus        │  │
│  │ PostgreSQL│◄─┼───┼──│ Cluster   │  │   │  │ Grafana           │  │
│  │ (Full PII)│  │   │  │ (HA)      │  │   │  │ Loki              │  │
│  └─────┬─────┘  │   │  └───────────┘  │   │  │ Tempo             │  │
│        │        │   │                 │   │  └───────────────────┘  │
│  ┌─────▼─────┐  │   └─────────────────┘   └─────────────────────────┘
│  │ Analytics │  │
│  │ PostgreSQL│  │
│  │ (Masked)  │  │
│  └───────────┘  │
└─────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    GENAI GATEWAY (Internal Service)                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────┐ │
│  │ PII Masking │  │  Prompt     │  │  Response Cache             │ │
│  │  Service    │─►│  Manager    │─►│  (Redis)                    │ │
│  └─────────────┘  └──────┬──────┘  └─────────────────────────────┘ │
│                          │                                        │
│                          ▼                                        │
│                ┌──────────────────┐                               │
│                │  LLM Provider    │ (On-prem or Approved External)│
│                │  (via Firewall)  │                               │
│                └──────────────────┘                               │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 Architecture Patterns

| Pattern | Usage | Justification |
|---------|-------|---------------|
| **Layered Architecture** | Backend services | Clear separation of concerns, testability |
| **Repository Pattern** | Data access | Abstraction over persistence, easy mocking |
| **Service Layer Pattern** | Business logic | Transaction management, validation |
| **API Gateway** | External access | Single entry point, rate limiting, auth |
| **Circuit Breaker** | GenAI calls | Fault tolerance, graceful degradation |
| **CQRS (Limited)** | Read vs Write DBs | Optimized queries, PII masking boundary |
| **Event-Driven** | Notifications | Async processing, decoupling |

---

## 3. Component Design

### 3.1 Backend Services

#### 3.1.1 Claim Service
**Responsibility:** Core claim lifecycle management (FNOL → Payout)

```
┌─────────────────────────────────────────────────────────────┐
│                    ClaimService                             │
├─────────────────────────────────────────────────────────────┤
│ + submitClaim(fnolRequest): ClaimResponse                   │
│ + getClaim(claimId): ClaimDetails                           │
│ + updateClaim(claimId, updates): ClaimDetails               │
│ + assignAdjuster(claimId, adjusterId): void                 │
│ + triageClaim(claimId, triageDecision): void                │
│ + adjudicateClaim(claimId, decision): void                  │
│ + approvePayout(claimId, amount): void                      │
│ + rejectClaim(claimId, reason): void                        │
│ + getClaimHistory(claimId): List<ClaimActivity>             │
└─────────────────────────────────────────────────────────────┘
            ▲
            │ implements
            │
┌─────────────────────────────────────────────────────────────┐
│                 ClaimController (REST)                      │
├─────────────────────────────────────────────────────────────┤
│ POST   /api/v1/claims/fnol                                  │
│ GET    /api/v1/claims/{claimId}                             │
│ PUT    /api/v1/claims/{claimId}                             │
│ POST   /api/v1/claims/{claimId}/triage                      │
│ POST   /api/v1/claims/{claimId}/adjudicate                  │
│ GET    /api/v1/claims/{claimId}/activities                  │
└─────────────────────────────────────────────────────────────┘
```

**Dependencies:**
- `ClaimRepository` (Data access)
- `GenAIGatewayClient` (AI assistance)
- `PolicyService` (Policy validation)
- `NotificationService` (Event publishing)
- `PIIMaskingService` (Data sanitization)

#### 3.1.2 GenAI Gateway Client
**Responsibility:** Secure communication with GenAI gateway

```java
@Component
public class GenAIGatewayClient {
    
    @Value("${genai.gateway.url}")
    private String gatewayUrl;
    
    @Value("${genai.gateway.api-key}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final PII maskingService;
    
    public TriageResponse classifyClaim(Claim claim) {
        // 1. Mask PII
        String maskedDescription = maskingService.mask(claim.getDescription());
        
        // 2. Build request
        GenAIRequest request = GenAIRequest.builder()
            .prompt("fnol_classification")
            .input(Map.of("description", maskedDescription))
            .build();
        
        // 3. Execute with circuit breaker
        return circuitBreaker.run(
            () -> restTemplate.postForObject(
                gatewayUrl + "/classify",
                request,
                TriageResponse.class
            ),
            throwable -> handleFallback(claim)
        );
    }
    
    private TriageResponse handleFallback(Throwable t) {
        log.warn("GenAI gateway unavailable, using default triage", t);
        return TriageResponse.defaultManualReview();
    }
}
```

#### 3.1.3 PIIMaskingService
**Responsibility:** Real-time PII detection and masking

```java
@Service
public class PIIMaskingService {
    
    private final List<MaskingRule> rules;
    private final Pattern ssnPattern = Pattern.compile("\\d{3}-\\d{2}-\\d{4}");
    private final Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    
    public String mask(String input) {
        String result = input;
        
        // Mask SSN
        result = ssnPattern.matcher(result)
            .replaceAll(match -> "XXX-XX-" + match.group().substring(7));
        
        // Mask Email
        result = emailPattern.matcher(result)
            .replaceAll(match -> maskEmail(match.group()));
        
        // Mask Phone Numbers
        result = maskPhoneNumbers(result);
        
        // Mask Addresses (using NLP entity recognition)
        result = maskAddresses(result);
        
        return result;
    }
    
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
```

### 3.2 Frontend Components

#### 3.2.1 Component Hierarchy

```
App
├── AuthProvider (Keycloak OIDC)
├── Layout
│   ├── Header (User info, notifications)
│   ├── Sidebar (Navigation)
│   └── Footer
├── Routes
│   ├── Public
│   │   └── FNOLWizard
│   │       ├── Step1: PolicyValidation
│   │       ├── Step2: IncidentDetails
│   │       ├── Step3: DocumentUpload
│   │       └── Step4: Confirmation
│   │
│   ├── Protected (Adjuster)
│   │   ├── Dashboard
│   │   │   ├── ClaimQueue
│   │   │   ├── MetricsCards
│   │   │   └── AlertsPanel
│   │   │
│   │   ├── ClaimDetail
│   │   │   ├── Timeline
│   │   │   ├── DocumentsViewer
│   │   │   ├── AISummaryPanel
│   │   │   ├── DecisionForm
│   │   │   └── ActivityLog
│   │   │
│   │   └── Reports
│   │
│   └── Admin
│       ├── UserManagement
│       ├── SystemConfig
│       └── AuditLogs
│
└── Shared Components
    ├── DataTable
    ├── Modal
    ├── FileUploader
    ├── AIChatWidget
    └── StatusBadge
```

#### 3.2.2 State Management (Redux Toolkit)

```typescript
// store.ts
export const store = configureStore({
  reducer: {
    claims: claimsReducer,
    user: userReducer,
    ui: uiReducer,
    notifications: notificationsReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: ['auth/logout'],
      },
    }),
});

// slices/claimsSlice.ts
export const claimsSlice = createSlice({
  name: 'claims',
  initialState: {
    list: [],
    selected: null,
    loading: false,
    error: null,
  },
  reducers: {
    setClaims: (state, action) => {
      state.list = action.payload;
    },
    setSelectedClaim: (state, action) => {
      state.selected = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchClaims.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchClaims.fulfilled, (state, action) => {
        state.loading = false;
        state.list = action.payload;
      })
      .addCase(fetchClaims.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message;
      });
  },
});
```

---

## 4. Database Design

### 4.1 Entity Relationship Diagram

```
┌─────────────────────┐       ┌─────────────────────┐
│      POLICY         │       │      ADJUSTER       │
├─────────────────────┤       ├─────────────────────┤
│ policy_id (PK)      │       │ adjuster_id (PK)    │
│ policy_number       │       │ user_id (UK)        │
│ holder_name         │       │ first_name          │
│ type                │       │ last_name           │
│ status              │       │ level               │
│ effective_date      │       │ email               │
│ expiration_date     │       │ active              │
│ coverage_limits     │       │ created_at          │
└─────────┬───────────┘       └──────────┬──────────┘
          │                              │
          │ 1                            │ 1
          │                              │
          │ N                            │ N
┌─────────▼──────────────────────────────▼──────────┐
│                    CLAIM                          │
├───────────────────────────────────────────────────┤
│ claim_id (PK)                                     │
│ policy_number (FK → POLICY)                       │
│ assigned_adjuster_id (FK → ADJUSTER)              │
│ date_of_loss                                      │
│ reported_date                                     │
│ status (NEW, TRIAGE, ADJUDICATION, PAID, etc.)    │
│ category (AUTO, PROPERTY, LIABILITY)              │
│ risk_score                                        │
│ total_amount                                      │
│ payout_amount                                     │
│ created_at                                        │
│ updated_at                                        │
│ version                                           │
└─────────┬─────────────────────────────────────────┘
          │
          │ 1
          │
          │ N
          │
    ┌─────┴─────────────────────────────────────┐
    │                                           │
┌───▼──────────────┐                  ┌────────▼──────────┐
│ CLAIM_DOCUMENT   │                  │  CLAIM_ACTIVITY   │
├──────────────────┤                  ├───────────────────┤
│ document_id (PK) │                  │ activity_id (PK)  │
│ claim_id (FK)    │                  │ claim_id (FK)     │
│ document_type    │                  │ activity_type     │
│ file_path        │                  │ description       │
│ file_size_bytes  │                  │ performed_by      │
│ mime_type        │                  │ performed_at      │
│ uploaded_at      │                  │ metadata (JSONB)  │
│ uploaded_by      │                  │                   │
└──────────────────┘                  └───────────────────┘

┌─────────────────────┐
│   AUDIT_LOG         │
├─────────────────────┤
│ log_id (PK)         │
│ entity_type         │
│ entity_id           │
│ action              │
│ old_value (JSONB)   │
│ new_value (JSONB)   │
│ changed_by          │
│ changed_at          │
│ ip_address          │
└─────────────────────┘
```

### 4.2 Two-Database Sync Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  OPERATIONAL DB (Full PII)                  │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ claim (claim_id, policy_holder_ssn, full_address)   │    │
│  │ policy (policy_id, holder_name, full_contact_info)  │    │
│  └─────────────────────────────────────────────────────┘    │
│                          │                                   │
│                          │ CDC (Change Data Capture)         │
│                          ▼                                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │           SYNC SERVICE (Java Application)            │    │
│  │  1. Listen to WAL changes (pgoutput plugin)          │    │
│  │  2. Apply masking rules per field                    │    │
│  │  3. Transform and validate                           │    │
│  │  4. Batch insert to Analytics DB                     │    │
│  └─────────────────────────────────────────────────────┘    │
│                          │                                   │
│                          ▼                                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │            ANALYTICS DB (Masked PII)                │    │
│  │  ┌─────────────────────────────────────────────┐    │    │
│  │  │ claim_masked                                 │    │    │
│  │  │ - ssn_last4 only                            │    │    │
│  │  │ - name_initial + last                       │    │    │
│  │  │ - city, state only (no street)              │    │    │
│  │  │ - phone_masked (XXX-XXX-1234)               │    │    │
│  │  └─────────────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 Masking Rules Configuration

```json
{
  "version": "1.0",
  "rules": [
    {
      "id": "SSN_MASK",
      "field_pattern": ".*ssn.*",
      "transformation": "REGEX_REPLACE",
      "regex": "(\\d{3})-(\\d{2})-(\\d{4})",
      "replacement": "XXX-XX-$3",
      "applies_to": ["analytics_db"]
    },
    {
      "id": "EMAIL_MASK",
      "field_pattern": ".*email.*",
      "transformation": "CUSTOM_FUNCTION",
      "function": "maskEmail",
      "applies_to": ["analytics_db", "logs"]
    },
    {
      "id": "ADDRESS_MASK",
      "field_pattern": ".*street.*|.*address.*",
      "transformation": "NULLIFY",
      "keep_fields": ["city", "state", "zip"],
      "applies_to": ["analytics_db"]
    },
    {
      "id": "PHONE_MASK",
      "field_pattern": ".*phone.*",
      "transformation": "REGEX_REPLACE",
      "regex": "(\\d{3})-(\\d{3})-(\\d{4})",
      "replacement": "XXX-XXX-$3",
      "applies_to": ["analytics_db", "logs"]
    }
  ]
}
```

---

## 5. Security Design

### 5.1 Authentication Flow (OIDC)

```
┌──────────┐      ┌──────────┐      ┌──────────┐      ┌──────────┐
│  User    │      │  React   │      │ Keycloak │      │  Backend │
│ Browser  │      │   App    │      │  Server  │      │  (Spring)│
└────┬─────┘      └────┬─────┘      └────┬─────┘      └────┬─────┘
     │                 │                 │                 │
     │ 1. Navigate     │                 │                 │
     │────────────────>│                 │                 │
     │                 │                 │                 │
     │ 2. Redirect to  │                 │                 │
     │    Keycloak     │                 │                 │
     │<────────────────│                 │                 │
     │                 │                 │                 │
     │ 3. Login Page   │                 │                 │
     │─────────────────────────────────>│                 │
     │                 │                 │                 │
     │ 4. Authenticate │                 │                 │
     │    (MFA if req) │                 │                 │
     │─────────────────────────────────>│                 │
     │                 │                 │                 │
     │ 5. Auth Code    │                 │                 │
     │<─────────────────────────────────│                 │
     │                 │                 │                 │
     │ 6. POST Code    │                 │                 │
     │────────────────>│                 │                 │
     │                 │                 │                 │
     │ 7. Exchange     │                 │                 │
     │    Code→Tokens  │────────────────>│                 │
     │                 │                 │                 │
     │ 8. Tokens       │                 │                 │
     │<────────────────│                 │                 │
     │                 │                 │                 │
     │ 9. API Call     │                 │                 │
     │    + Access Token                 │                 │
     │────────────────────────────────────────────────────>│
     │                 │                 │                 │
     │ 10. Validate    │                 │                 │
     │     Token       │                 │<────────────────│
     │                 │                 │                 │
     │ 11. Response    │                 │                 │
     │<────────────────────────────────────────────────────│
     │                 │                 │                 │
```

### 5.2 Authorization Matrix

| Endpoint | ROLE_POLICYHOLDER | ROLE_ADJUSTER_JUNIOR | ROLE_ADJUSTER_SENIOR | ROLE_ADJUDICATOR | ROLE_ADMIN |
|----------|-------------------|----------------------|----------------------|------------------|------------|
| POST /claims/fnol | ✅ Own | ❌ | ❌ | ❌ | ❌ |
| GET /claims/{id} | ✅ Own | ✅ Assigned | ✅ All | ✅ All | ✅ All |
| POST /claims/{id}/triage | ❌ | ✅ Assigned | ✅ All | ✅ All | ❌ |
| POST /claims/{id}/adjudicate | ❌ | ❌ | ✅ (<$50k) | ✅ (Unlimited) | ❌ |
| GET /reports/* | ❌ | ❌ | ✅ Limited | ✅ Full | ✅ Full |
| GET /admin/users | ❌ | ❌ | ❌ | ❌ | ✅ |
| POST /admin/config | ❌ | ❌ | ❌ | ❌ | ✅ |

### 5.3 Audit Logging Strategy

```java
@Aspect
@Component
public class AuditLogAspect {
    
    @Autowired
    private AuditLogRepository auditLogRepo;
    
    @AfterReturning(
        pointcut = "@annotation(auditable)",
        returning = "result",
        argNames = "joinPoint,result,auditable"
    )
    public void logAudit(JoinPoint joinPoint, Object result, Auditable auditable) {
        AuditLog log = new AuditLog();
        log.setEntityType(auditable.entityType());
        log.setEntityId(extractId(joinPoint));
        log.setAction(auditable.action());
        log.setChangedBy(SecurityContextHolder.getCurrentUser().getName());
        log.setChangedAt(Instant.now());
        log.setIpAddress(getRequestIP());
        log.setNewValue(serialize(result));
        
        auditLogRepo.save(log);
    }
}

// Usage in service
@Auditable(entityType = "CLAIM", action = "ADJUDICATE")
public Claim adjudicateClaim(UUID claimId, AdjudicationDecision decision) {
    // ... business logic
}
```

---

## 6. Integration Design

### 6.1 Core Insurance System Integration

```
┌─────────────────┐         ┌─────────────────────────────────┐
│  Acme Claims    │         │   Core Insurance System         │
│  Platform       │         │   (Legacy Mainframe)            │
└────────┬────────┘         └───────────────┬─────────────────┘
         │                                   │
         │ REST/SOAP Adapter                 │
         │ (Async where possible)            │
         │                                   │
         ▼                                   ▼
┌─────────────────┐                 ┌─────────────────┐
│ Policy Validation                │
│ Request:                        │
│ {                               │
│   "policyNumber": "POL-123",    │
│   "checkDate": "2023-10-26"     │
│ }                               │
│                                 │
│ Response:                       │
│ {                               │
│   "valid": true,                │
│   "type": "AUTO",               │
│   "status": "ACTIVE",           │
│   "limits": {...}               │
│ }                               │
└─────────────────┘                 └─────────────────┘
```

### 6.2 Banking/Payout Integration

```
┌─────────────────┐         ┌─────────────────────────────────┐
│  Acme Claims    │         │   Internal Banking Gateway      │
│  Platform       │         │   (Secure Payment Processor)    │
└────────┬────────┘         └───────────────┬─────────────────┘
         │                                   │
         │ SFTP Batch + Real-time API        │
         │                                   │
         ▼                                   ▼
┌─────────────────┐                 ┌─────────────────┐
│ Payout Request:                  │
│ {                                 │
│   "claimId": "CLM-xxx",           │
│   "payeeName": "...",             │
│   "accountNumber": "XXX-X-1234",  │
│   "amount": 4500.00,              │
│   "effectiveDate": "2023-10-27"   │
│ }                                 │
│                                   │
│ Payout Confirmation:              │
│ {                                 │
│   "transactionId": "TXN-xxx",     │
│   "status": "COMPLETED",          │
│   "processedAt": "..."            │
│ }                                 │
└─────────────────┘                 └─────────────────┘
```

---

## 7. Deployment Topology

### 7.1 Production Environment

```
                    ┌─────────────────────────────────────────┐
                    │              PHYSICAL RACK A            │
                    │                                         │
┌───────────────────┼─────────────────────────────────────────┼───────────────────┐
│                   │                                         │                   │
│  ┌─────────────┐  │  ┌─────────────┐  ┌─────────────┐       │  ┌─────────────┐  │
│  │   App       │  │  │   App       │  │   App       │       │  │   App       │  │
│  │   Server 1  │  │  │   Server 2  │  │   Server 3  │  ...  │  │   Server 5  │  │
│  │  32-core    │  │  │  32-core    │  │  32-core    │       │  │  32-core    │  │
│  │  128GB RAM  │  │  │  128GB RAM  │  │  128GB RAM  │       │  │  128GB RAM  │  │
│  └─────────────┘  │  └─────────────┘  └─────────────┘       │  └─────────────┘  │
│                   │                                         │                   │
│  ┌─────────────┐  │  ┌─────────────┐  ┌─────────────┐       │                   │
│  │   DB        │  │  │   DB        │  │   DB        │       │                   │
│  │   Primary   │◄─┼──│   Replica 1 │◄─┼──│   Replica 2 │       │                   │
│  │  32-core    │  │  │  32-core    │  │  32-core    │       │                   │
│  │  128GB RAM  │  │  │  128GB RAM  │  │  128GB RAM  │       │                   │
│  │  2TB SSD    │  │  │  2TB SSD    │  │  2TB SSD    │       │                   │
│  └─────────────┘  │  └─────────────┘  └─────────────┘       │                   │
│                   │                                         │                   │
│  ┌─────────────┐  │  ┌─────────────┐                        │                   │
│  │  Keycloak 1 │◄─┼──│ Keycloak 2  │                        │                   │
│  │  (Active)   │  │  │  (Standby)  │                        │                   │
│  └─────────────┘  │  └─────────────┘                        │                   │
│                   │                                         │                   │
│  ┌─────────────────────────────────────────────────────┐   │                   │
│  │              Monitoring Stack                        │   │                   │
│  │  Prometheus │ Grafana │ Loki │ Tempo │ Alertmanager │   │                   │
│  └─────────────────────────────────────────────────────┘   │                   │
│                   │                                         │                   │
└───────────────────┴─────────────────────────────────────────┴───────────────────┘
                    │
                    │ Replication (Async)
                    ▼
                    ┌─────────────────────────────────────────┐
                    │           OFFSITE BACKUP FACILITY       │
                    │           (Daily pg_basebackup)         │
                    └─────────────────────────────────────────┘
```

### 7.2 Network Segmentation

| VLAN ID | Subnet | Purpose | Allowed Ports |
|---------|--------|---------|---------------|
| 100 | 10.0.100.0/24 | Application Tier | 443, 8080, 8443 |
| 101 | 10.0.101.0/24 | Database Tier | 5432 |
| 102 | 10.0.102.0/24 | Management | 22 (SSH), 9090 (Prometheus) |
| 103 | 10.0.103.0/24 | GenAI Gateway | 8081, 6379 (Redis) |

**Firewall Rules:**
- External → LB: 443 (HTTPS) only
- LB → App: 8080, 8443
- App → DB: 5432 (PostgreSQL)
- App → Keycloak: 443
- App → GenAI Gateway: 8081
- No direct internet from App/DB servers

---

## 8. Observability Design

### 8.1 Metrics Collection

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'acme-claims-app'
    static_configs:
      - targets: ['app1:8080', 'app2:8080', 'app3:8080']
    metrics_path: '/actuator/prometheus'
    
  - job_name: 'postgresql'
    static_configs:
      - targets: ['db-primary:9187']
      
  - job_name: 'keycloak'
    static_configs:
      - targets: ['keycloak:9000']
```

### 8.2 Key Dashboards

1. **Executive Dashboard**
   - Claims submitted (today/week/month)
   - Average processing time
   - Approval rate
   - Customer satisfaction score

2. **Operations Dashboard**
   - System health (all services green/red)
   - Request latency (p50, p95, p99)
   - Error rates by endpoint
   - Database connection pool usage

3. **Claims Adjuster Dashboard**
   - Queue length (my claims / team claims)
   - SLA breaches approaching
   - Pending approvals

4. **GenAI Gateway Dashboard**
   - Requests per minute
   - Average latency
   - Token usage
   - Circuit breaker status

### 8.3 Alerting Rules

```yaml
# alerting-rules.yml
groups:
  - name: acme-claims-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }}% on {{ $labels.instance }}"
          
      - alert: DatabaseConnectionPoolExhausted
        expr: db_connection_pool_active / db_connection_pool_max > 0.9
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "DB connection pool nearly exhausted"
          
      - alert: GenAIGatewayCircuitOpen
        expr: genai_circuit_breaker_state == 1
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "GenAI Gateway circuit breaker is OPEN"
          
      - alert: BackupFailed
        expr: backup_success == 0
        labels:
          severity: critical
        annotations:
          summary: "Daily backup failed"
```

---

## 9. Appendix

### Appendix A: API Versioning Strategy
- URL versioning: `/api/v1/...`
- Backward compatibility: 2 previous versions supported
- Deprecation notice: 6 months minimum
- Sunset: Automated response headers with deprecation dates

### Appendix B: Caching Strategy
| Layer | Technology | TTL | Invalidation |
|-------|------------|-----|--------------|
| L1 (In-memory) | Caffeine | 5 min | Time-based |
| L2 (Distributed) | Redis | 30 min | Event-based (claim update) |
| L3 (CDN) | N/A (On-prem) | N/A | N/A |

### Appendix C: Rate Limiting
- Anonymous users: 10 requests/minute
- Authenticated users: 100 requests/minute
- Adjusters: 500 requests/minute
- GenAI Gateway: 50 requests/minute per user

### Appendix D: Document Storage
- On-premises NAS (Network Attached Storage)
- File naming: `{claim_id}/{document_type}/{timestamp}_{uuid}.pdf`
- Encryption: AES-256 at rest
- Retention: 7 years post-claim-closure

---

**Document Control**
| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2023-10-26 | Architecture Team | Initial release |
