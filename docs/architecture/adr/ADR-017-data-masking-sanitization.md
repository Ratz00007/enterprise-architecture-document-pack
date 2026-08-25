# ADR-017: Production Data Masking and Sanitization

## Status
**Accepted**

## Context
Per `06_Data_Architecture.docx` §3, production data masking and sanitization rules must be defined for the two-database architecture (QA + Production per ADR-007) and the controlled pipeline that replicates production images to QA (ADR-008). This ADR establishes comprehensive PII protection while enabling realistic testing in QA.

## Decision

### Data Classification Schema

All data fields classified into four categories:

| Classification | Definition | Examples | Handling Requirement |
|----------------|------------|----------|---------------------|
| **Public** | No restrictions | Policy numbers (partial), claim status | No masking required |
| **Internal** | Business confidential | Claim notes, adjuster assignments | Mask in non-prod if combined with PII |
| **Confidential (PII)** | Personal Identifiable Information | Name, address, phone, email, SSN/Tax ID | **Must mask** in QA/UAT |
| **Restricted (PHI)** | Protected Health Information | Medical records, diagnosis codes, treatment details | **Must mask** in QA/UAT; special handling in Prod |

### Masking Techniques

#### Technique Selection Matrix

| Data Type | Masking Technique | Example Original | Example Masked | Reversible |
|-----------|-------------------|------------------|----------------|------------|
| **SSN / Tax ID** | Format-preserving encryption (FPE) | 123-45-6789 | 987-65-4321 | Yes (Prod only key) |
| **Full Name** | Synthetic replacement | John Michael Smith | [CLAIM_001234_FIRST] [CLAIM_001234_LAST] | No |
| **Address** | Geographic generalization | 123 Main St, Springfield, IL 62701 | Springfield, IL 627XX | No |
| **Phone Number** | Format-preserving random | (555) 123-4567 | (555) 987-6543 | No |
| **Email** | Domain preservation + hash | john.smith@example.com | claim_001234_hash7f3a@example.com | No |
| **Date of Birth** | Year preservation + offset | 1985-07-15 | 1985-03-22 (random offset ±120 days) | No |
| **Medical Record #** | FPE with different key | MRN-12345678 | MRN-87654321 | Yes (Prod only key) |
| **Diagnosis Codes (ICD-10)** | Category preservation | E11.9 (Type 2 Diabetes) | E11.X (Diabetes, unspecified) | No |
| **Bank Account Number** | FPE | 123456789012 | 987654321098 | Yes (Prod only key) |
| **Driver's License** | State format preservation | S123-4567-8901 | S987-6543-2109 | No |

### Two-Database Architecture Implementation

#### Database Topology

```
┌─────────────────┐     ┌─────────────────┐
│   PROD DB       │     │    QA DB        │
│  (Full PII/PHI) │     │ (Masked Data)   │
│                 │     │                 │
│ - Claims        │────▶│ - Claims        │
│ - Customers     │     │ - Customers     │
│ - Medical       │     │ - Medical       │
│ - Financial     │     │ - Financial     │
└─────────────────┘     └─────────────────┘
        │                       │
        │ Production            │ Masked Copy
        │ Live Traffic          │ Testing Only
```

#### Data Flow: Production → QA

```
Step 1: pg_dump from Production (full backup)
              ↓
Step 2: Secure transfer to isolated masking environment
              ↓
Step 3: PIIMaskingService applies transformation rules
              ↓
Step 4: Validation checks (no raw PII leakage)
              ↓
Step 5: psql restore to QA database
              ↓
Step 6: Audit log entry (who, when, what masked)
```

### Masking Rules by Entity

#### Customer Entity

```sql
-- Source (Production)
customer_id: UUID (unchanged)
first_name: VARCHAR(100) → [MASKED]
last_name: VARCHAR(100) → [MASKED]
ssn: VARCHAR(11) → [FPE_ENCRYPTED]
date_of_birth: DATE → [OFFSET_PRESERVE_YEAR]
email: VARCHAR(255) → [HASH_PRESERVE_DOMAIN]
phone: VARCHAR(20) → [RANDOM_FORMAT_PRESERVE]
address_line1: VARCHAR(200) → [GENERALIZE]
city: VARCHAR(100) → [UNCHANGED]
state: CHAR(2) → [UNCHANGED]
zip_code: VARCHAR(10) → [TRUNCATE_LAST_2]
```

#### Claim Entity

```sql
-- Source (Production)
claim_id: UUID (unchanged)
policy_number: VARCHAR(50) → [PRESERVE_PREFIX_MASK_SUFFIX]
claimant_customer_id: UUID (unchanged - FK preserved)
incident_date: DATE → [UNCHANGED]
description: TEXT → [REDACT_NAMES_LOCATIONS]
status: VARCHAR(20) → [UNCHANGED]
triage_priority: VARCHAR(10) → [UNCHANGED]
adjudication_decision: VARCHAR(50) → [UNCHANGED]
payout_amount: DECIMAL → [UNCHANGED]
```

#### Medical Records Entity

```sql
-- Source (Production)
medical_record_id: UUID (unchanged)
claim_id: UUID (unchanged - FK preserved)
patient_customer_id: UUID (unchanged - FK preserved)
diagnosis_code: VARCHAR(20) → [GENERALIZE_TO_CATEGORY]
treatment_description: TEXT → [REDACT_SPECIFIC_DETAILS]
provider_name: VARCHAR(200) → [MASK_TO_TYPE]
service_date: DATE → [UNCHANGED]
cost_amount: DECIMAL → [UNCHANGED]
```

### PIIMaskingService Implementation

Located at: `/workspace/apps/backend/src/main/java/com/acme/claims/gateway/PIIMaskingService.java`

**Key Features**:
- Strategy pattern for different masking techniques
- Configurable rules per environment
- Audit logging for every masking operation
- Validation mode to detect unmasked PII before export
- Batch processing for large datasets

**Usage**:
```java
// Mask a single customer record
Customer masked = piiMaskingService.maskCustomer(originalCustomer);

// Mask entire database export
MaskingResult result = piiMaskingService.maskDatabaseDump(sourceDb, targetDb);

// Validate no PII leakage
ValidationReport report = piiMaskingService.validateNoPiiLeakage(targetDb);
```

### Controlled Pipeline (ADR-008 Compliance)

#### Jenkins Pipeline Stage

```groovy
stage('Replicate Prod Image to QA') {
    steps {
        script {
            // Approval gate required (ADR-006)
            input message: 'Approve production data replication to QA?', 
                  ok: 'Approve', 
                  submitter: 'data-stewards,db-admins'
            
            // Step 1: Create secure temporary environment
            sh '''
                docker run -d --name masking-env \
                    --network isolated \
                    acme/masking-service:latest
            '''
            
            // Step 2: Export from production
            sh '''
                pg_dump -h prod-db.acme.local -U readonly claims_db | \
                gzip > /secure/prod-dump-$(date +%Y%m%d).sql.gz
            '''
            
            // Step 3: Apply masking
            sh '''
                java -jar pii-masking-service.jar \
                    --input /secure/prod-dump-*.sql.gz \
                    --output /secure/qa-dump-$(date +%Y%m%d).sql.gz \
                    --rules config/masking-rules.json \
                    --validate
            '''
            
            // Step 4: Import to QA
            sh '''
                gunzip -c /secure/qa-dump-*.sql.gz | \
                psql -h qa-db.acme.local -U admin claims_db
            '''
            
            // Step 5: Cleanup
            sh '''
                shred -u /secure/prod-dump-*.sql.gz
                shred -u /secure/qa-dump-*.sql.gz
                docker stop masking-env && docker rm masking-env
            '''
        }
    }
}
```

### Validation and Quality Assurance

#### Pre-Export Checks (Production)

- [ ] Data classification scan completed
- [ ] Row counts documented
- [ ] Checksums calculated for audit trail
- [ ] Approval obtained from data steward

#### Post-Masking Validation (QA)

- [ ] No raw SSN patterns detected (regex validation)
- [ ] No raw email domains from production (domain allowlist check)
- [ ] Foreign key relationships preserved
- [ ] Referential integrity verified
- [ ] Statistical distribution maintained (for testing validity)
- [ ] Zero PHI keywords detected in plain text

#### Automated Validation Script

```bash
#!/bin/bash
# /workspace/data/scripts/validate-masking.sh

echo "=== PII Leakage Detection ==="

# Check for SSN patterns
if psql -h qa-db -c "SELECT COUNT(*) FROM customers WHERE ssn ~ '^[0-9]{3}-[0-9]{2}-[0-9]{4}$'" | grep -q "^[1-9]"; then
    echo "❌ CRITICAL: Raw SSN detected in QA!"
    exit 1
fi

# Check for unmasked emails
if psql -h qa-db -c "SELECT COUNT(*) FROM customers WHERE email !~ '^claim_[0-9]+_hash[a-f0-9]+@'" | grep -q "^[1-9]"; then
    echo "❌ CRITICAL: Unmasked email detected in QA!"
    exit 1
fi

echo "✅ All validation checks passed"
```

### Security Controls

#### Access Restrictions

| Role | Can Initiate Masking | Can View Production PII | Can View Masked QA Data |
|------|---------------------|------------------------|------------------------|
| **Developer** | No | No | Yes |
| **QA Engineer** | No | No | Yes |
| **DBA** | Yes (with approval) | Yes | Yes |
| **Data Steward** | Yes (approver) | Yes | Yes |
| **Auditor** | No | Read-only (logged) | Yes |
| **Claim Adjuster** | No | Yes (own cases only) | No |

#### Encryption Requirements

- **In Transit**: TLS 1.3 for all data transfers
- **At Rest (Production)**: AES-256 encryption via PostgreSQL TDE or filesystem encryption
- **At Rest (QA)**: AES-256 encryption (same as production)
- **Masking Keys**: Stored in Hardware Security Module (HSM) or enterprise vault

### Audit and Compliance

#### Audit Log Requirements

Every masking operation must log:
- Timestamp (ISO 8601)
- User/service account initiating
- Source database and tables
- Destination database and tables
- Masking rules version applied
- Row counts (before/after)
- Validation results
- Approval ticket reference

#### Retention

- Masking operation logs: 7 years (regulatory requirement)
- Validation reports: 7 years
- Approval records: 7 years

## Consequences

### Positive
- Comprehensive PII/PHI protection in non-production environments
- Enables realistic testing with production-like data distributions
- Clear audit trail for regulatory compliance (HIPAA, GDPR, state insurance regulations)
- Prevents accidental data exposure to developers/QA engineers
- Maintains referential integrity for functional testing

### Negative
- Additional processing time for masking operations (estimated 2-4 hours for full database)
- Complexity in maintaining masking rules as schema evolves
- Some testing scenarios requiring specific PII patterns need synthetic data generation instead
- Irreversible masking limits certain types of debugging

### Neutral
- FPE requires key management infrastructure
- Synthetic name generation may produce unrealistic combinations
- Geographic generalization reduces precision for location-based testing

## Compliance Mapping

| Requirement | Source Document | Compliance Status |
|-------------|-----------------|-------------------|
| Production data masking rules | `06_Data_Architecture.docx` §3.1 | ✅ Compliant |
| Two-database sync with masking | ADR-007, ADR-008 | ✅ Compliant |
| PII protection in non-prod | Industry best practice | ✅ Compliant |
| PHI handling (HIPAA) | Healthcare regulations | ✅ Compliant (baseline) |

## Related ADRs

- ADR-007: Two database instances only (QA + Production)
- ADR-008: Production image replicated to QA via controlled pipeline
- ADR-009: 24×7 logging with 24-hour retention
- ADR-011: GenAI data boundary and gateway (allow-listed data only)
- ADR-015: Backup, DR, and business continuity
- ADR-016: Identity, authentication, and authorization

## Deferred Items

The following items require stakeholder input:
- Specific regulatory requirements by jurisdiction (state/federal)
- Data retention periods beyond baseline 7 years
- Cross-border data transfer restrictions (if applicable)
- Customer consent requirements for data usage in testing

## Review Date
This ADR shall be reviewed:
- Quarterly (masking rules effectiveness)
- Annually (regulatory compliance updates)
- Upon any data breach incident
- When schema changes affect PII/PHI fields
