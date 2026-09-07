# tests/load — k6

`claims-lifecycle.js` implements the load profile required by AGENTS.md
(ADR-013): **100 RPS constant arrival rate, thresholds p95 < 400 ms and error
rate < 5%.**

```bash
# liveness only (no credentials needed)
k6 run tests/load/claims-lifecycle.js -e BASE_URL=http://localhost:8080/api/v1

# with an authenticated claims-list call
TOKEN=$(curl -s http://localhost:8180/realms/acme-claims/protocol/openid-connect/token \
  -d grant_type=password -d client_id=claims-web \
  -d username=adjuster1 -d password=adjuster-dev-only \
  -d scope=openid | jq -r .access_token)
k6 run tests/load/claims-lifecycle.js -e BASE_URL=http://localhost:8080/api/v1 -e ACCESS_TOKEN=$TOKEN
```

Note: the RPS/p95 figures are the CI gate profile from AGENTS.md, not an
invented SLA (ADR-010 governs: capacity targets for production hardware stay
TBD until the stakeholder supplies them, per ADR-014/ADR-020).
