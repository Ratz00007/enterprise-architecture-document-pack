# Incident Response Runbook - Acme Claims Platform

## Overview
This runbook provides step-by-step procedures for responding to incidents in the Acme Claims Processing Platform.

## Severity Levels

| Level | Description | Response Time | Escalation |
|-------|-------------|---------------|------------|
| P1 | Critical - System down, data loss | 15 minutes | Immediate |
| P2 | High - Major functionality impaired | 1 hour | 2 hours |
| P3 | Medium - Minor functionality impaired | 4 hours | Next business day |
| P4 | Low - Cosmetic issues, minor bugs | 24 hours | As scheduled |

## Alert Channels
- **Prometheus/Grafana**: Primary monitoring
- **PagerDuty**: On-call escalation
- **Slack**: #incidents channel
- **Email**: ops-team@acme.com

---

## Incident: Backend Service Down

### Symptoms
- Health check failures on `/api/v1/actuator/health`
- Increased 5xx errors in Grafana
- Prometheus alerts firing

### Diagnosis Steps
```bash
# Check service status
ssh <backend-server> "systemctl status acme-claims-backend"

# Check recent logs
ssh <backend-server> "journalctl -u acme-claims-backend --since '10 minutes ago'"

# Check port binding
ssh <backend-server> "netstat -tlnp | grep 8080"

# Check database connectivity
ssh <backend-server> "curl -f http://localhost:5432 || echo 'DB connection failed'"
```

### Resolution Steps
1. **Restart service**:
   ```bash
   ssh <backend-server> "systemctl restart acme-claims-backend"
   ```

2. **Verify health**:
   ```bash
   curl -f http://<backend-server>:8080/api/v1/actuator/health
   ```

3. **Check logs for root cause**:
   ```bash
   ssh <backend-server> "tail -100 /var/log/acme-claims/backend.log"
   ```

### Escalation
If service fails to restart after 2 attempts, escalate to:
- Primary: Backend Team Lead
- Secondary: Infrastructure Team

---

## Incident: Database Connection Failures

### Symptoms
- Increased connection timeouts
- Application logs showing JDBC errors
- PostgreSQL metrics showing connection pool exhaustion

### Diagnosis Steps
```bash
# Check PostgreSQL status
ssh <db-server> "systemctl status postgresql"

# Check active connections
ssh <db-server> "psql -c 'SELECT count(*) FROM pg_stat_activity;'"

# Check connection limits
ssh <db-server> "psql -c 'SHOW max_connections;'"

# Check disk space
ssh <db-server> "df -h /var/lib/postgresql"
```

### Resolution Steps
1. **Restart PostgreSQL** (if safe):
   ```bash
   ssh <db-server> "systemctl restart postgresql"
   ```

2. **Clear idle connections**:
   ```bash
   ssh <db-server> "psql -c \"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle' AND query_start < NOW() - INTERVAL '30 minutes';\""
   ```

3. **Increase connection pool** (temporary):
   ```bash
   ssh <db-server> "psql -c \"ALTER SYSTEM SET max_connections = 200;\" && systemctl reload postgresql"
   ```

### Escalation
Contact DBA team immediately for persistent issues.

---

## Incident: GenAI Gateway Timeout

### Symptoms
- Triage assessments failing
- Increased latency in claim processing
- GenAI request log showing timeouts

### Diagnosis Steps
```bash
# Check gateway health
curl -f https://genai-gateway.acme.internal/health

# Check recent GenAI requests
psql -d acme_claims -c "SELECT * FROM genai_request_log WHERE status = 'ERROR' ORDER BY created_at DESC LIMIT 10;"

# Check provider status
curl -f https://status.<provider>.com
```

### Resolution Steps
1. **Enable fallback provider** (if configured):
   ```bash
   curl -X POST https://genai-gateway.acme.internal/admin/failover
   ```

2. **Disable GenAI features temporarily**:
   Update application configuration to set `genai.gateway.enabled=false`

3. **Contact GenAI Gateway team**

---

## Post-Incident Actions

1. **Document incident** in incident management system
2. **Update runbook** if new learnings discovered
3. **Schedule post-mortem** for P1/P2 incidents
4. **Create JIRA tickets** for any follow-up work
5. **Update monitoring** if gaps identified

## Contact Information

| Role | Name | Phone | Email |
|------|------|-------|-------|
| On-Call Engineer | PagerDuty | - | - |
| Backend Lead | [TBD] | [TBD] | [TBD] |
| DBA Lead | [TBD] | [TBD] | [TBD] |
| Security Lead | [TBD] | [TBD] | [TBD] |
