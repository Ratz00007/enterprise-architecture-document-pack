# services/auth-realm — Keycloak realm for Acme Claims

`acme-claims-realm.json` is the realm export that `docker-compose.yml`
imports into Keycloak at startup (`--import-realm`).

## Contents

- **Roles:** `claims-adjuster` (FNOL + triage), `claims-approver`
  (adjudication decisions — the human-approval role from ADR-006),
  `claims-admin`.
- **Clients:** `claims-api` (confidential resource server; tokens carry an
  `aud: claims-api` claim), `claims-web` (public, OIDC authorization-code
  flow for the React UI).
- **Dev users:** `adjuster1` / `approver1` with temporary passwords
  (`adjuster-dev-only` / `approver-dev-only`). **Dev only — never promote
  this realm export to an environment without replacing users and forcing
  the password policy.** Secrets belong in Ansible Vault (AGENTS.md), not in
  this file; this file deliberately contains only throwaway dev credentials.

## Environment promotion

The realm definition is the source of truth for role/client topology, but
per-environment secrets and user onboarding are applied by the platform team
via the Keycloak admin console or `kc.sh import` with an
environment-specific overlay. ADR-016 governs identity decisions.
