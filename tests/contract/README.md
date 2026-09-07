# tests/contract — Pact consumer-driven contracts (ADR-013)

## Consumer side (here)

`claims-api.pact.spec.mts` defines what `claims-web` needs from
`claims-api` and writes the contract JSON to `pacts/`:

```bash
npm install
npm run test:contract
```

## Provider side (claims-api)

Verify the generated contract against a running provider:

```bash
cd apps/claims-api
./mvnw verify -Ppact-verify \
  -Dpact.filePath=../../tests/contract/pacts/claimsweb-claimsapi.json \
  -Dpact.provider.version=1.0.0
```

The `pact-verify` profile activates `pact-jvm-provider-spring6`; wire the
provider state `a Keycloak adjuster token` in
`ClaimProviderStateTest` (needs a running provider — start it with
`./mvnw spring-boot:run` or against the compose stack).

## Status

- Consumer contract: **implemented and runnable**.
- Provider verification: **documented, profile not yet added to the pom**
  (it needs a running provider + pact-jvm dependency; tracked in the README
  status table as pending work for the QA workstream).

The provider verification must be on the promotion path before the first QA
release (AGENTS.md test pyramid).
