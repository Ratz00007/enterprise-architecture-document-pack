# Tests

The test suites that gate the pipeline. Per `ADR-013` we run four
levels: unit, integration, contract, and E2E — plus load.

```
tests/
├── README.md
├── e2e/                    # Playwright + Cucumber
│   ├── features/
│   └── step-definitions/
├── contract/               # Pact provider/consumer
│   ├── claims-api/
│   └── genai-gateway/
├── load/                   # k6
│   └── claims-api.js
└── fixtures/               # Reusable test data
```

## Commands

See `AGENTS.md` Setup commands for the canonical list. Short form:

```powershell
pwsh -File scripts\test-unit.ps1          # unit (JUnit + Vitest)
pwsh -File scripts\test-integration.ps1   # Testcontainers
pwsh -File scripts\test-contract.ps1      # Pact
pwsh -File scripts\test-e2e.ps1           # Playwright
pwsh -File scripts\test-load.ps1          # k6
```

## Coverage gates

| Component | Line | Branch |
|-----------|------|--------|
| `apps/claims-api` | 80% | 70% |
| `apps/claims-web` | 70% | n/a |
| `services/genai-gateway` | 75% | 65% |
| `services/db-sync` | 80% | 70% |

## Test data

The E2E suite uses the QA environment, which holds a sanitized
image of prod (`ADR-008`). It never touches raw PII. The
Testcontainers-based integration tests use fresh Postgres +
Keycloak + MinIO containers per class.
