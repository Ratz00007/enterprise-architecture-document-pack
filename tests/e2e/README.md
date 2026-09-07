# tests/e2e — Playwright end-to-end suite

Runs the adjuster journey (OIDC sign-in → FNOL → triage → adjudication →
approval) against the compose stack.

## Prerequisites

```bash
docker compose up --build -d   # stack on http://localhost:8081
```

## Run

```bash
npm ci
npx playwright install chromium
npm test          # or: npx playwright test
```

`E2E_BASE_URL` overrides the target for QA/UAT environments.

CI: the Jenkinsfile's `E2E Tests` stage performs the same steps on a
Docker-capable agent.
