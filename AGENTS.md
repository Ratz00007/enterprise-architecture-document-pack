# AGENTS.md

> Root agent-facing project definition. This file is the single source of truth that
> every AI coding agent (and every human engineer) reads first. Keep it tight,
> accurate, and current. If a section drifts, fix it here in the same PR.

## What this project is

**Acme Claims** — a Fortune 500–grade insurance claims processing platform.
A realistic enterprise application, **plus** the on-premises DevOps / GenAI
platform that hosts it, implemented end-to-end against the architecture pack
in `00_Document_Control_Map.docx` … `11_Operations_Runbooks.docx`.

**Domain:** insurance claims (FNOL → adjudication → payout).
**Pack:** on-premises, physical servers, four sequential environments
(Dev → QA → UAT → Prod), same VLAN/subnet, two DB instances (QA + Prod),
pipeline-mediated Prod→QA image sync, central brand-agnostic GenAI, 24×7
logging with 24-hour retention, basic firewall/IP/port security baseline,
human approval gates between every environment.

## The pack is the spec — read it

The 13 Word documents at the repo root (`00_…` through `11_…`) are
**authoritative requirements**. Every requirement is tagged `REQ-NN` in
`01_ARS.docx` and tracked in `10_RTM_ADR.docx`. Every architecture decision
lives in `docs/architecture/adr/`. **Do not invent scope, vendors, or
performance targets the pack does not state.**

## Setup commands

> PowerShell 5.1+ on Windows is the supported host shell. The repo also works
> in WSL2 / Linux CI runners. `bash` here is shorthand for whichever POSIX
> shell is available.

| Step | Command (PowerShell) | Notes |
|------|----------------------|-------|
| Bootstrap | `pwsh -File scripts/setup-dev.ps1` | installs JDK 21, Node 20, Maven wrapper, Ansible, Helm, k6, Playwright |
| Build API | `cd apps\claims-api; .\mvnw.cmd -B verify` | full build + unit + integration tests |
| Build Web | `cd apps\claims-web; pnpm install; pnpm build` | Vite build, type-check, unit tests |
| Run API locally | `cd apps\claims-api; .\mvnw.cmd spring-boot:run` | binds `0.0.0.0:8080` |
| Run Web locally | `cd apps\claims-web; pnpm dev` | Vite dev server on `:5173` |
| Lint all | `pwsh -File scripts/lint.ps1` | runs Checkstyle, SpotBugs, ESLint, Prettier, Markdownlint |
| Typecheck | `cd apps\claims-web; pnpm typecheck` | `tsc --noEmit` |
| Unit tests | `pwsh -File scripts/test-unit.ps1` | JUnit 5 + Vitest |
| E2E tests | `pwsh -File scripts/test-e2e.ps1` | Playwright (headed in CI requires Xvfb) |
| Load tests | `pwsh -File scripts/test-load.ps1` | k6 against QA |
| Promote build | `pwsh -File ci\scripts\promote.ps1 -Env qa` | gates through Jenkins + human approval |

## Project layout

```
.
├── 00_…docx … 11_…docx      Source-of-truth architecture pack (READ, do not edit)
├── README.txt                 Pack README (legacy, kept for traceability)
├── AGENTS.md                  ← you are here
├── README.md                  Project vision & quick links
├── CHANGELOG.md               Release notes
├── .editorconfig              Whitespace & encoding rules
├── .gitattributes             Line endings & linguist hints
├── docs/
│   ├── architecture/          System context, stack, topology, pipeline, observability, security
│   │   ├── adr/               ADR-000…ADR-NNN (every decision has a record)
│   │   └── diagrams/          Mermaid + PlantUML sources, rendered to PNG/SVG
│   ├── domain/                Claims domain: entities, workflows, regulatory context
│   ├── api/                   OpenAPI specs, event schemas
│   ├── compliance/            SOC2 / GDPR / NAIC control mapping
│   └── runbooks/              Operational runbooks (failure & recovery)
├── infra/
│   ├── ansible/               Bare-metal provisioning: inventories, playbooks, roles
│   ├── firewall/              nftables rules per environment
│   ├── monitoring/            Prometheus, Grafana, Loki, Alertmanager config
│   ├── systemd/               Unit files for all services
│   └── timezone/              NTP + chrony per host
├── ci/
│   ├── Jenkinsfile            Root pipeline (shared library: ci\jenkins\shared-library)
│   ├── jenkins/               JCasC, shared library, agent config
│   └── scripts/               Promotion, rollback, evidence capture
├── apps/
│   ├── claims-api/            Spring Boot 3.3 (Java 21) — the main product API
│   └── claims-web/            React 18 + TypeScript + Vite — adjuster + customer UI
├── services/
│   ├── genai-gateway/         LiteLLM-based vendor-neutral GenAI gateway
│   ├── db-sync/               Pipeline-mediated Prod→QA image refresh
│   ├── audit-log/             Append-only audit trail
│   ├── auth-realm/            Keycloak realm export
│   └── log-aggregator/        Promtail + Loki shipper config
├── data/
│   ├── schemas/qa/            Per-env DB migrations
│   ├── schemas/prod/
│   ├── seed/                  Idempotent test-data loaders
│   └── masking/               Data-masking rules for non-prod refresh
├── tests/
│   ├── e2e/                   Playwright (UI), Cucumber (BDD)
│   ├── contract/              Pact consumer/provider
│   ├── load/                  k6 scripts
│   └── fixtures/              Reusable test data
├── ops/
│   ├── dashboards/            Grafana dashboard JSONs
│   ├── incident-response/     Severity matrix, comms templates
│   └── oncall/                Rotation & escalation policy
├── scripts/                   Local-dev, lint, ADR generation
└── .github/ISSUE_TEMPLATE/    Bug report, feature request, ADR request
```

## Code style

- **Java 21, Spring Boot 3.3.** Strict `final` by default, sealed types where
  appropriate, no `var` for public APIs. Lombok is forbidden — records and
  explicit constructors preferred. SpotBugs + Checkstyle enforced in CI.
- **TypeScript strict mode, ES2022, React 18 functional components only.**
  ESLint + Prettier enforced; `any` is denied by ESLint rule.
- **Database naming:** `snake_case`, plural table names, UUIDv7 PKs, every
  table has `created_at` + `updated_at` + `created_by` + `updated_by`.
- **All logs:** structured JSON, OpenTelemetry resource attributes, never
  raw `System.out`. PII fields are masked at the log encoder.
- **All time:** UTC everywhere; convert to user TZ at the edge.
- **All money:** `BIGINT` minor units, ISO 4217 currency code column.
- **Idempotency keys** required on every state-mutating endpoint.
- **One ADR per non-trivial decision.** Run `pwsh -File scripts\gen-adr.ps1`
  to scaffold a new one.

## Testing instructions

- **Unit:** JUnit 5 + AssertJ on the API; Vitest + Testing Library on the web.
- **Integration:** Testcontainers (Postgres 16, Keycloak, MinIO) in
  `apps/claims-api/src/test/java/.../integration`.
- **Contract:** Pact, provider-driven, gates release to UAT.
- **E2E:** Playwright + Cucumber. Critical user journeys in `tests/e2e/`.
- **Load:** k6 in `tests/load/`. Default scenario: 100 RPS, p95 < 400ms, < 1% errors.
- **Coverage gate:** API 80% line / 70% branch, web 70% line. Below the gate = red build.
- **Security:** OWASP ZAP baseline scan in QA gate, before UAT promotion.

All tests must pass before opening a PR.

## Branch, commit & PR conventions

- Default branch: `main`. Never push directly.
- Branch names: `feat/<ticket>-short-name`, `fix/<ticket>-short-name`,
  `chore/<short-name>`, `docs/<short-name>`.
- Commits: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`,
  `refactor:`, `test:`, `perf:`, `revert:`). Subject ≤ 72 chars, body wraps
  at 100. Reference REQ-NN in the footer when the commit closes a requirement.
- PRs: title ≤ 72 chars, description uses `.github/PULL_REQUEST_TEMPLATE.md`
  (TBD). At least one approver from a different team. CI green = required.
- Squash-merge. The squash commit message becomes the canonical changelog entry.

## Security & secrets

- **Never commit secrets.** `.env`, `*.pem`, `*.key`, `application-local.yml`
  are all git-ignored. Use the platform's secret store (Keycloak + Ansible Vault
  for now; upgrade to HashiCorp Vault post-MVP).
- **All production data classified as PII by default.** Logging and
  observability are PII-aware (see `infra/monitoring/loki/`).
- **GenAI data boundary:** the central GenAI gateway never receives raw
  production data unless explicitly allow-listed by the data-classification
  policy. See `services/genai-gateway/` and ADR-011.
- **Vulnerability scanning:** Trivy on every container image, OWASP
  Dependency-Check on every API build.

## Definition of done

A change is "done" only when all of these are true:

1. The requirement(s) it implements are listed in the PR description and
   traced in `docs/architecture/adr/` if a decision was made.
2. All unit, integration, contract, and E2E tests pass on the CI runner.
3. Coverage gates are met.
4. The Jenkins promotion pipeline can move the artifact Dev → QA without
   human intervention, and stops at the QA approval gate.
5. Logs are structured, traces are sampled, and the change appears in the
   Grafana service map.
6. No new SAST/DAST findings, no new license-policy violations.

## When you (an agent) get stuck

1. Re-read the relevant doc in the pack (00–11). The answer is almost always there.
2. Re-read the matching ADR in `docs/architecture/adr/`.
3. Check `docs/architecture/adr/INDEX.md` for the decision register status.
4. If still stuck, open a `?` PR or an ADR-Request issue. Do **not** invent
   a decision and ship it.

## Multi-agent workstream plan

See `docs/architecture/multi-agent-plan.md` for the active workstreams,
their owners, their acceptance criteria, and how they hand off to each other.
Update it whenever a workstream starts, finishes, or changes scope.
