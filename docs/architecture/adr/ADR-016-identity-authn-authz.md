# ADR-016: Identity, Authentication, and Authorization

## Status
**Accepted**

## Context
Per `09_Security_Network.docx` §3, identity management, authentication (authn), and authorization (authz) must be defined beyond basic OIDC + RBAC. This ADR establishes the complete identity architecture using Keycloak as the central identity provider while adhering to physical server constraints and same-VLAN requirements.

## Decision

### Identity Provider Architecture

**Keycloak 24.x** deployed as the centralized Identity and Access Management (IAM) solution for all four environments.

#### Deployment Model

| Environment | Keycloak Instances | Database | Load Balancing | High Availability |
|-------------|-------------------|----------|----------------|-------------------|
| **Dev** | 1 standalone | Embedded PostgreSQL | None | No HA (dev only) |
| **QA** | 2 instances (clustered) | External PostgreSQL | HAProxy (active-passive) | Basic HA |
| **UAT** | 2 instances (clustered) | External PostgreSQL | HAProxy (active-passive) | Basic HA |
| **Prod** | 3 instances (clustered) | External PostgreSQL cluster | HAProxy (active-active) | Full HA with failover |

### Authentication Flows

#### Primary Authentication (OIDC)

```
User Browser → HAProxy → Keycloak Login → [Credentials] → Keycloak validates → 
Issue JWT Token → Redirect to Application → App validates token → Grant access
```

#### Supported Authentication Methods

| Method | Environment | Use Case | MFA Required |
|--------|-------------|----------|--------------|
| Username/Password + TOTP | All | Internal employees | Yes (Prod/UAT) |
| Certificate-based (mTLS) | Prod only | Service accounts | No (cert is sufficient) |
| LDAP/AD Integration | All | Corporate directory sync | Per corporate policy |
| Emergency Break-glass | Prod only | Disaster recovery | Yes (hardware token) |

### Authorization Model

#### Role-Based Access Control (RBAC)

**Realm**: `acme-claims`

**Roles Hierarchy**:

```
claim-admin (superuser - full system access)
├── claim-manager
│   ├── claims-adjuster
│   │   ├── claims-viewer
│   │   └── claims-data-entry
│   └── adjudicator
├── finance-ops
│   └── payout-processor
├── compliance-officer
├── auditor (read-only all)
└── genai-operator (GenAI gateway management)
```

#### Permission Matrix

| Resource | FNOL | Triage | Adjudication | Payout | Audit Log |
|----------|------|--------|--------------|--------|-----------|
| **claims-data-entry** | Create, Read | — | — | — | — |
| **claims-viewer** | Read | Read | Read | Read | — |
| **claims-adjuster** | Read, Update | Read, Update | Read | — | Read (own) |
| **adjudicator** | Read | Read | Read, Update, Decide | Read | Read (own) |
| **payout-processor** | Read | Read | Read | Execute, Approve | Read (own) |
| **claim-manager** | All | All | All | All | Read (team) |
| **claim-admin** | All | All | All | All | All |
| **compliance-officer** | Read | Read | Read | Read | All |
| **auditor** | Read | Read | Read | Read | All |

### Session Management

| Setting | Dev | QA | UAT | Prod |
|---------|-----|-----|-----|------|
| Session Timeout | 8 hours | 4 hours | 2 hours | 30 minutes |
| Idle Timeout | 2 hours | 1 hour | 30 minutes | 10 minutes |
| Max Concurrent Sessions | Unlimited | 5 | 3 | 2 |
| Token Validity (JWT) | 24 hours | 8 hours | 4 hours | 1 hour |
| Refresh Token Validity | 7 days | 3 days | 1 day | 8 hours |

### User Federation

#### LDAP/Active Directory Integration

- **Sync Mode**: Periodic (every 15 minutes) + On-demand trigger
- **User Storage**: Keycloak local cache + LDAP backend
- **Group Mapping**: AD Security Groups → Keycloak Roles
- **Attribute Mapping**:
  - `sAMAccountName` → Keycloak username
  - `mail` → email
  - `displayName` → full name
  - `memberOf` → role assignments

#### Service Accounts

For machine-to-machine communication:
- **Authentication**: Client credentials grant (OAuth 2.0)
- **Storage**: Keycloak internal database (not LDAP)
- **Rotation**: Credentials rotated every 90 days (automated via Jenkins)
- **Audit**: All service account actions logged with client_id

### Security Hardening

#### Password Policy

| Requirement | Setting |
|-------------|---------|
| Minimum Length | 14 characters |
| Complexity | Upper + Lower + Number + Special |
| History | Last 12 passwords blocked |
| Expiration | 90 days (Prod/UAT), 180 days (QA), Never (Dev) |
| Lockout | 5 failed attempts → 30 minute lockout |
| Breach Detection | Check against HaveIBeenPwned API (QA/UAT/Prod only) |

#### Token Security

- **Signing Algorithm**: RS256 (RSA 2048-bit minimum)
- **Encryption**: Optional JWE for sensitive claims (Prod only)
- **Audience Validation**: Strict `aud` claim verification
- **Issuer Validation**: Strict `iss` claim verification
- **Clock Skew Tolerance**: ±30 seconds

### Audit Logging

All authentication and authorization events logged to:
- **Immediate**: SIEM integration (Splunk/ELK via syslog)
- **Retention**: 90 days online, 7 years archived (compliance requirement)
- **Events Captured**:
  - Login success/failure
  - Logout
  - Token refresh
  - Role assignment changes
  - Permission denials
  - Password changes/resets
  - MFA enrollment/modification

## Consequences

### Positive
- Centralized identity management across all environments
- Single sign-on (SSO) experience for users
- Fine-grained access control matching insurance claims workflow
- Comprehensive audit trail for regulatory compliance
- Support for both human users and service accounts

### Negative
- Keycloak clustering adds operational complexity
- LDAP sync failures can block user access (mitigated by local cache)
- Additional infrastructure overhead (3 prod instances + load balancer)
- Learning curve for claims staff on MFA procedures

### Neutral
- Keycloak version upgrades require careful migration planning
- Custom themes needed for corporate branding
- Service account credential rotation requires automation

## Compliance Mapping

| Requirement | Source Document | Compliance Status |
|-------------|-----------------|-------------------|
| AuthN/AuthZ beyond OIDC+RBAC | `09_Security_Network.docx` §3.1 | ✅ Compliant |
| MFA for production | `09_Security_Network.docx` §3.2 | ✅ Compliant |
| Audit logging | `09_Security_Network.docx` §3.3 | ✅ Compliant |
| LDAP integration | `09_Security_Network.docx` §3.4 | ✅ Compliant |

## Related ADRs

- ADR-000: Tech stack selection (Keycloak)
- ADR-001: Physical servers only
- ADR-003: Same VLAN / subnet
- ADR-011: GenAI data boundary and gateway (service accounts)
- ADR-014: Physical server specifications

## Implementation Notes

### Keycloak Realm Export/Import

Realms exported as JSON for version control:
```bash
# Export realm
kcadm.sh export-realm --realm acme-claims --output /backup/acme-claims-realm.json

# Import realm (CI/CD pipeline)
kcadm.sh import-realm --input-file acme-claims-realm.json
```

### Emergency Access Procedure

1. Contact Security Operations Center (SOC)
2. SOC verifies identity via phone callback to known number
3. SOC activates break-glass account (hardware token required)
4. All break-glass usage triggers immediate alert to CISO
5. Post-incident review mandatory within 24 hours

## Review Date
This ADR shall be reviewed:
- Quarterly (password policy, session timeouts)
- Annually (full security assessment)
- Upon any security incident involving authentication/authorization
