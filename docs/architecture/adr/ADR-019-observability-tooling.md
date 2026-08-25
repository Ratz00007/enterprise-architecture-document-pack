# ADR-019: Observability Tooling Selection

## Status
**Accepted**

## Context
Per `01_ARS.docx` §5 and ADR-000 (Tech Stack Selection), the observability tooling stack has been selected as Prometheus + Grafana + Loki. This ADR formally documents this selection, provides justification, and establishes the governance process for any future changes to the observability stack.

## Decision

### Selected Observability Stack

The Acme Claims platform will use the following open-source observability tools:

| Component | Tool | Version | Purpose | Deployment Model |
|-----------|------|---------|---------|------------------|
| **Metrics** | Prometheus | 2.50+ | Time-series metrics collection and storage | Physical servers (same VLAN) |
| **Visualization** | Grafana | 10.4+ | Dashboards, alerting, data exploration | Physical servers (same VLAN) |
| **Logs** | Loki | 2.9+ | Log aggregation and querying | Physical servers (same VLAN) |
| **Tracing** | Tempo | 2.4+ | Distributed tracing (optional enhancement) | Physical servers (same VLAN) |
| **Alerting** | Alertmanager | 0.26+ | Alert routing, deduplication, silencing | Bundled with Prometheus |

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Application Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ Spring Boot │  │   React     │  │  Keycloak   │             │
│  │   Apps      │  │  Frontend   │  │   Auth      │             │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘             │
│         │                │                │                     │
│         ▼                ▼                ▼                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              OpenTelemetry Instrumentation               │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   Prometheus    │  │      Loki       │  │     Tempo       │
│    (Metrics)    │  │     (Logs)      │  │   (Traces)      │
│                 │  │                 │  │                 │
│ - Scrape every  │  │ - Push via      │  │ - OTLP          │
│   15 seconds    │  │   Promtail      │  │   receiver      │
│ - 24h retention │  │ - 24h retention │  │ - 24h retention │
│   (per ADR-009) │  │   (per ADR-009) │  │   (per ADR-009) │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
                              │
                              ▼
                     ┌─────────────────┐
                     │     Grafana     │
                     │  (Visualization)│
                     │                 │
                     │ - Unified UI    │
                     │ - Alerting      │
                     │ - Annotations   │
                     └────────┬────────┘
                              │
                              ▼
                     ┌─────────────────┐
                     │   Alertmanager  │
                     │                 │
                     │ - Email to SOC  │
                     │ - PagerDuty     │
                     │ - Slack/Teams   │
                     └─────────────────┘
```

### Justification for Selection

#### Why Prometheus?

**Pros**:
- Industry standard for Kubernetes and cloud-native metrics (even on physical servers)
- Pull-based model fits our static infrastructure well
- Powerful PromQL query language
- Native integration with Spring Boot Actuator
- Large ecosystem of exporters (PostgreSQL, Keycloak, Jenkins, etc.)
- No external dependencies; single binary deployment
- Strong community support and documentation

**Cons**:
- Single-server design (requires federation or Thanos for HA)
- 24-hour retention limit requires careful capacity planning
- Learning curve for PromQL

**Mitigation**:
- Deploy Prometheus in high-availability pair with identical scraping
- Use remote write to long-term storage if retention needs increase
- Provide PromQL training to operations team

#### Why Grafana?

**Pros**:
- Unified visualization for metrics (Prometheus), logs (Loki), and traces (Tempo)
- Rich dashboard library with pre-built templates
- Flexible alerting with multiple notification channels
- Role-based access control integrated with Keycloak (OIDC)
- Active development and enterprise support available
- Runs on physical servers without modification

**Cons**:
- Complex configuration for advanced features
- Performance can degrade with many concurrent users
- Some features require Enterprise license

**Mitigation**:
- Use open-source features only (no Enterprise dependency)
- Implement dashboard performance best practices
- Limit concurrent dashboard refreshes

#### Why Loki?

**Pros**:
- Designed to work seamlessly with Prometheus and Grafana
- Cost-effective log storage (indexes labels, not content)
- LogQL query language similar to PromQL
- Low resource footprint compared to ELK stack
- Multi-tenancy support for environment separation

**Cons**:
- Not suitable for full-text search across large log volumes
- Query performance degrades with high cardinality labels
- Less mature than established solutions (ELK, Splunk)

**Mitigation**:
- Careful label design to avoid high cardinality
- Use Promtail for efficient log shipping
- Implement log retention policies per ADR-009

### Deployment Specifications

#### Hardware Requirements (Production)

| Component | CPU | RAM | Storage | Network |
|-----------|-----|-----|---------|---------|
| **Prometheus** | 8 cores | 32GB | 500GB SSD | 1GbE |
| **Grafana** | 4 cores | 16GB | 100GB SSD | 1GbE |
| **Loki** | 8 cores | 32GB | 1TB SSD | 1GbE |
| **Tempo** | 4 cores | 16GB | 500GB SSD | 1GbE |
| **Alertmanager** | 2 cores | 4GB | 50GB SSD | 1GbE |

**Total**: 26 cores, 100GB RAM, 2.15TB SSD storage

#### Placement

All observability components deployed on dedicated physical servers within the same VLAN/subnet as application servers (per ADR-003).

**Server Allocation**:
- **Option A**: 2× servers (Prometheus+Alertmanager on Server 1; Grafana+Loki+Tempo on Server 2)
- **Option B**: 3× servers (Prometheus+Alertmanager; Grafana; Loki+Tempo)
- **Recommended**: Option B for better isolation and performance

### Integration Points

#### Spring Boot Applications

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
  prometheus:
    metrics:
      export:
        enabled: true
        step: 15s
  tracing:
    sampling:
      probability: 0.1  # 10% of traces
```

#### PostgreSQL Exporter

```bash
# Run as systemd service
DATA_SOURCE_NAME="postgresql://exporter:password@prod-db.acme.local:5432/claims_db" \
/prometheus-postgres-exporter
```

#### Keycloak Metrics

Enable MicroProfile Metrics in Keycloak:
```bash
KEYCLOAK_OPTS="-Dmicroprofile.metrics.enabled=true"
```

#### Jenkins Metrics

Install Prometheus Metrics plugin in Jenkins:
- Navigate to `/prometheus` endpoint
- Scrape from Prometheus configuration

### Alerting Strategy

#### Alert Severity Levels

| Severity | Response Time | Notification Channel | Example |
|----------|---------------|---------------------|---------|
| **Critical (P1)** | Immediate (24/7) | PagerDuty + Phone Call | Production down, data loss |
| **High (P2)** | 15 minutes | PagerDuty + Slack | Service degradation, high error rate |
| **Medium (P3)** | 1 hour | Slack + Email | Elevated latency, disk space warning |
| **Low (P4)** | Next business day | Email only | Non-critical warnings, informational |

#### Sample Alert Rules

```yaml
# prometheus/alert-rules.yml
groups:
  - name: acme-claims-alerts
    rules:
      - alert: HighErrorRate
        expr: sum(rate(http_requests_total{status=~"5.."}[5m])) / sum(rate(http_requests_total[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High HTTP 5xx error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }} over the last 5 minutes"
      
      - alert: DatabaseConnectionPoolExhausted
        expr: pg_stat_activity_count{datname="claims_db"} > pg_settings_max_connections * 0.9
        for: 2m
        labels:
          severity: high
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "{{ $value }} active connections out of maximum"
      
      - alert: DiskSpaceLow
        expr: (node_filesystem_avail_bytes / node_filesystem_size_bytes) * 100 < 10
        for: 10m
        labels:
          severity: medium
        annotations:
          summary: "Disk space below 10%"
          description: "Filesystem {{ $labels.mountpoint }} has {{ $value | humanize }}% available"
```

### Retention Policy (per ADR-009)

| Data Type | Active Retention | Archive | Deletion |
|-----------|-----------------|---------|----------|
| **Metrics** | 24 hours (Prometheus) | None | Automatic rollover |
| **Logs** | 24 hours (Loki) | 30 days compressed | Automatic deletion |
| **Traces** | 24 hours (Tempo) | None | Automatic deletion |
| **Alerts** | 90 days (in Alertmanager) | 7 years (exported) | Per compliance |

### Security Considerations

- All observability endpoints accessible only within VLAN (no external exposure)
- Grafana authentication via Keycloak OIDC
- RBAC configured in Grafana (Viewer, Editor, Admin roles)
- TLS encryption for all internal communication
- Audit logging enabled for Grafana access
- No PII logged (enforced via application logging configuration)

## Consequences

### Positive
- Unified observability stack with single pane of glass (Grafana)
- Open-source tools with no licensing costs
- Strong integration between components (Prometheus + Grafana + Loki)
- Community support and extensive documentation
- Runs on physical servers without modification
- Complies with 24-hour retention requirement (ADR-009)

### Negative
- Multiple tools to operate and maintain
- Learning curve for PromQL and LogQL
- Limited long-term storage without additional components (Thanos, Cortex)
- Self-managed infrastructure overhead

### Neutral
- Tool selection can be revisited if requirements change
- Vendor-neutral approach avoids lock-in
- Skills transferable to other organizations

## Compliance Mapping

| Requirement | Source Document | Compliance Status |
|-------------|-----------------|-------------------|
| Observability tooling defined | `01_ARS.docx` §5.5 | ✅ Compliant |
| 24-hour log retention | ADR-009 | ✅ Compliant |
| Physical server deployment | ADR-001 | ✅ Compliant |
| Same VLAN/subnet | ADR-003 | ✅ Compliant |

## Related ADRs

- ADR-000: Tech stack selection (Prometheus/Grafana)
- ADR-001: Physical servers only
- ADR-003: Same VLAN / subnet
- ADR-009: 24×7 logging with 24-hour retention
- ADR-020: HA/DR targets (observability DR strategy)

## Change Governance

Any proposal to replace or significantly modify the observability stack must:

1. Submit a new ADR proposing the change
2. Demonstrate clear benefits over current stack
3. Include migration plan and timeline
4. Obtain approval from Architecture Review Board
5. Update all relevant documentation and runbooks

## Review Date
This ADR shall be reviewed:
- Annually (tool evaluation and updates)
- Upon any major observability incident
- When new tools emerge that provide significant advantages
