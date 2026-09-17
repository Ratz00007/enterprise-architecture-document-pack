# Acme Claims — Jenkins Shared Library

This directory is a Jenkins Shared Library. It exposes the
`acmePipeline` step that every component's `Jenkinsfile` calls.

## Layout

```
shared-library/
├── src/
│   └── org/acme/claims/
│       ├── AcmePipeline.groovy       # the main step
│       ├── Promotion.groovy          # promoteToQA / UAT / Prod
│       ├── Rollback.groovy
│       ├── Evidence.groovy           # builds the per-stage evidence bundle
│       └── GenaiAdvisory.groovy      # calls the GenAI gateway for advice
├── test/
│   └── org/acme/claims/              # unit tests for the library
├── vars/
│   └── acmePipeline.groovy           # the global step entry point
└── resources/                        # shared templates, e.g. evidence.json.j2
```

## How a component uses it

```groovy
@Library('acme-claims@main') _

acmePipeline {
    name            = 'claims-api'
    language        = 'java'
    buildTool       = 'maven'
    containerImage  = 'registry.internal.acme/claims-api'
    promotionChain  = ['qa', 'uat', 'prod']
    approvalRoles   = [qa: 'qa_lead', uat: 'uat_lead', prod: 'release_operator']
}
```

## Versioning

- `@main` is the bleeding edge; promoted builds use `@main` once
  the build is green on `main`.
- Backports to release branches use a tag like `@v1.4.x`.
- A breaking change to `acmePipeline {}` requires a major bump of
  the library and a coordinated PR across every consumer.

## Promotion mechanics

- **Artifact identity** is by digest, not tag. The library
  verifies the cosign signature before promoting.
- **Approval** is a Jenkins `input` step with `submitter` set to
  the Keycloak group for the role (e.g. `qa_lead`).
- **Audit** is written via the audit-log service on every
  approval, every promote, and every rollback.
- **GenAI** is called only via `GenaiAdvisory.groovy`, which
  enforces the data-class rules from `ADR-011`.

## Runbook links

- `docs/runbooks/jenkins-down.md` (TBD)
- `docs/runbooks/promotion-stalled.md` (TBD)
- `docs/runbooks/rollback-prod.md` (TBD)
