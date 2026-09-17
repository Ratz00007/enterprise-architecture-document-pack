# Claims Domain — Entities

> Every entity, its attributes, its invariants, and the table it
> maps to. This is the data model reference; the actual DDL is in
> `data/schemas/{qa,prod}/V*.sql` (Flyway migrations).

> **Convention:** `snake_case` table and column names, plural table
> names, UUIDv7 primary keys, every mutable table has
> `created_at`, `updated_at`, `created_by`, `updated_by`, and a
> monotonic `version` for optimistic locking.

## Core entities

### `policies`

The insurance policy under which a claim is filed.

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid PK | UUIDv7 |
| `policy_number` | text unique not null | Human-readable, e.g. `P-2026-00012345` |
| `product_line` | enum(`home`, `auto`, `commercial_property`, `liability`, `other`) | MVP = `home` + `auto` |
| `holder_party_id` | uuid FK → `parties` | The policyholder |
| `effective_date` | date not null | |
| `expiration_date` | date not null | |
| `status` | enum(`active`, `lapsed`, `cancelled`, `pending`) | |
| `coverage_limits` | jsonb not null | Per-coverage cap map |
| `deductibles` | jsonb not null | Per-coverage deductible map |
| `metadata` | jsonb | Free-form extensions |
| `version` | bigint not null | Optimistic lock |

Invariants:

- A policy has at least one active coverage.
- `expiration_date > effective_date`.

### `parties`

A person or organization. Reused for policyholders, claimants,
witnesses, adjusters, payees.

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid PK | |
| `kind` | enum(`person`, `organization`) | |
| `display_name` | text not null | May be a synthetic name for pseudonymized records |
| `email` | text | Encrypted at rest (column-level) |
| `phone` | text | Encrypted at rest |
| `address_id` | uuid FK → `addresses` | |
| `tax_id_hash` | text | One-way hash; never store raw SSN/EIN |
| `metadata` | jsonb | |
| `version` | bigint not null | |

### `addresses`

A postal address. Reused.

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid PK | |
| `line1` | text not null | |
| `line2` | text | |
| `city` | text not null | |
| `region` | text | State / province |
| `postal_code` | text not null | |
| `country_code` | char(2) not null | ISO 3166-1 alpha-2 |
| `latitude`, `longitude` | numeric | Optional, for routing |
| `version` | bigint not null | |

### `claims`

The heart of the system.

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid PK | |
| `claim_number` | text unique not null | Human-readable, e.g. `CLM-2026-00012345` |
| `policy_id` | uuid FK → `policies` | |
| `claimant_party_id` | uuid FK → `parties` | May differ from policyholder |
| `loss_type` | enum(`fire`, `water`, `theft`, `wind`, `liability`, `auto_collision`, `other`) | |
| `loss_date` | timestamp not null | When the loss happened |
| `reported_date` | timestamp not null default now() | When FNOL was received |
| `severity` | enum(`low`, `medium`, `high`, `catastrophe`) | Triage output |
| `state` | enum(`NEW`, `TRIAGE`, `INVESTIGATING`, `DECISION_PENDING`, `APPROVED`, `RESERVED`, `PAYOUT_PENDING`, `PAID`, `DENIED`, `WITHDRAWN`, `CLOSED`, `ARCHIVED`, `REOPENED`) | See state machine in `claims-overview.md` |
| `assigned_adjuster_id` | uuid FK → `users` | |
| `description` | text not null | The customer's account |
| `estimated_amount_cents` | bigint | Minor units, see ADR-000 |
| `currency` | char(3) not null | ISO 4217 |
| `fraud_score` | numeric(5,4) | 0.0000 – 1.0000 |
| `metadata` | jsonb | |
| `version` | bigint not null | |

Invariants:

- `state` transitions are append-only; every change is a row in
  `claim_state_history`.
- The pair (`policy_id`, `loss_date`, `loss_type`) is unique —
  no duplicate FNOLs.

### `claim_state_history`

Append-only state-machine log. Every `claims.state` change writes
one row here, in the same transaction.

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid PK | |
| `claim_id` | uuid FK → `claims` | |
| `from_state` | enum null | null on initial creation |
| `to_state` | enum not null | |
| `changed_by_user_id` | uuid FK → `users` | |
| `changed_at` | timestamp not null default now() | |
| `reason` | text | Human-readable |
| `evidence_ref` | uuid FK → `evidence_items` | Optional |
| `genai_summary` | text | Optional, when GenAI assisted |

### `documents`

Files attached to a claim. Stored in object storage (MinIO for dev,
on-prem S3-compatible for prod); the DB holds metadata only.

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid PK | |
| `claim_id` | uuid FK → `claims` | |
| `kind` | enum(`photo`, `estimate`, `police_report`, `medical`, `invoice`, `correspondence`, `other`) | |
| `original_filename` | text not null | |
| `mime_type` | text not null | |
| `byte_size` | bigint not null | |
| `sha256` | text not null | Integrity check |
| `storage_uri` | text not null | e.g. `s3://claims-prod/documents/CLM-…/photo.jpg` |
| `uploaded_by_user_id` | uuid FK → `users` | |
| `uploaded_at` | timestamp not null default now() | |
| `pii_classification` | enum(`none`, `name`, `address`, `id_number`, `financial`, `medical`) | |
| `pii_redacted_uri` | text | If a redacted version was generated |
| `version` | bigint not null | |

### `evidence_items`

Anything used to make a decision. Links documents, statements,
external data, and GenAI outputs together.

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid PK | |
| `claim_id` | uuid FK → `claims` | |
| `kind` | enum(`document_ref`, `statement`, `site_visit`, `genai_output`, `external_lookup`, `other`) | |
| `source` | text not null | Free-text origin (e.g. "site visit by adjuster 12345") |
| `payload` | jsonb not null | The actual evidence |
| `captured_at` | timestamp not null default now() | |
| `captured_by_user_id` | uuid FK → `users` | |
| `version` | bigint not null | |

### `decisions`

The adjudication outcome.

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid PK | |
| `claim_id` | uuid FK → `claims` | |
| `outcome` | enum(`approved`, `approved_partial`, `denied`, `withdrawn`) | |
| `approved_amount_cents` | bigint | |
| `denial_reason` | text | When outcome = `denied` |
| `approved_by_user_id` | uuid FK → `users` | |
| `decided_at` | timestamp not null default now() | |
| `rationale` | text not null | The adjuster's reasoning |
| `genai_assisted` | boolean default false | |
| `genai_summary` | text | When `genai_assisted = true` |
| `version` | bigint not null | |

### `reserves`

A claim's financial reserve.

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid PK | |
| `claim_id` | uuid FK → `claims` | |
| `amount_cents` | bigint not null | |
| `currency` | char(3) not null | |
| `set_by_user_id` | uuid FK → `users` | |
| `set_at` | timestamp not null default now() | |
| `reason` | text | |
| `superseded_by_id` | uuid FK → `reserves` | The previous reserve this replaces |
| `version` | bigint not null | |

Invariants:

- A claim has at most one *active* reserve at a time (latest
  non-superseded).
- The total of all paid payouts may not exceed the total
  approved amount.

### `payouts`

Money out.

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid PK | |
| `claim_id` | uuid FK → `claims` | |
| `decision_id` | uuid FK → `decisions` | |
| `amount_cents` | bigint not null | |
| `currency` | char(3) not null | |
| `payee_party_id` | uuid FK → `parties` | |
| `method` | enum(`ach`, `wire`, `check`, `credit_on_account`) | |
| `status` | enum(`pending`, `instructed`, `settled`, `reversed`, `failed`) | |
| `instructed_at` | timestamp | |
| `settled_at` | timestamp | |
| `external_reference` | text | Bank ref, etc. |
| `version` | bigint not null | |

### `audit_log`

Append-only, write-once. Every state-mutating action writes a row
in the same transaction as the action itself.

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid PK | |
| `occurred_at` | timestamp not null default now() | |
| `actor_subject` | text not null | Keycloak subject |
| `actor_role` | text | |
| `action` | text not null | e.g. `claim.approve` |
| `resource_type` | text not null | e.g. `claim` |
| `resource_id` | uuid not null | |
| `correlation_id` | uuid | One per request |
| `payload` | jsonb | Action-specific details |
| `prev_hash` | text | Hash of the previous row → tamper-evident chain |
| `hash` | text not null | Hash of this row including `prev_hash` |

Invariants:

- Rows are never updated or deleted. A delete attempt is rejected
  by a `BEFORE DELETE` trigger.
- The hash chain makes any tampering detectable.

## Reference entities

### `users`

The mirror of Keycloak users. Not a source of truth — Keycloak is.

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid PK | Same as Keycloak subject (UUID) |
| `email` | text unique not null | |
| `display_name` | text not null | |
| `role` | enum(`csr`, `adjuster`, `senior_adjuster`, `finance_operator`, `operations`, `auditor`, `devops`, `qa`, `developer`) | |
| `active` | boolean not null default true | |
| `version` | bigint not null | |

### `sla_breaches`

A denormalized table of SLA breaches. Materialized view, refreshed
by a job.

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid PK | |
| `claim_id` | uuid FK | |
| `sla_kind` | text | e.g. `fnol_to_triage` |
| `due_at` | timestamp | |
| `breached_at` | timestamp | |
| `notified_at` | timestamp | |

## Indexes

- `claims(claim_number)`, `claims(policy_id)`, `claims(state)`,
  `claims(assigned_adjuster_id, state)`
- `claim_state_history(claim_id, changed_at desc)`
- `documents(claim_id)`
- `decisions(claim_id)`
- `reserves(claim_id) where superseded_by_id is null` (partial
  unique index — "at most one active reserve per claim")
- `payouts(claim_id)`, `payouts(status)`
- `audit_log(correlation_id)`, `audit_log(resource_id)`,
  `audit_log(actor_subject, occurred_at desc)`

## Schema versioning

- All schema changes are Flyway migrations
  (`data/schemas/qa/V{n}__{description}.sql`).
- A migration is committed in the same PR as the code that
  requires it.
- Migrations are append-only. A bad migration is fixed by a new
  migration, never by editing the old one.
