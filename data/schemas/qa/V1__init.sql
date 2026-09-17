-- V1__init.sql
-- Initial schema for the claims system. See
-- docs/domain/entities.md for the entity reference and
-- docs/domain/workflows.md for the state machine.
--
-- Conventions:
--   * snake_case table and column names
--   * plural table names
--   * UUIDv7 primary keys
--   * every mutable table has created_at, updated_at, created_by, updated_by
--   * every mutable table has a monotonic version column for optimistic locking
--   * UTC everywhere

BEGIN;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";  -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgvector";   -- ADR-011 RAG

-- ---------------------------------------------------------------------------
-- parties, addresses
-- ---------------------------------------------------------------------------
CREATE TABLE addresses (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    line1         text NOT NULL,
    line2         text,
    city          text NOT NULL,
    region        text,
    postal_code   text NOT NULL,
    country_code  char(2) NOT NULL,
    latitude      numeric(9, 6),
    longitude     numeric(9, 6),
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    created_by    uuid NOT NULL,
    updated_by    uuid NOT NULL,
    version       bigint NOT NULL DEFAULT 1
);

CREATE TABLE parties (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    kind            text NOT NULL CHECK (kind IN ('person','organization')),
    display_name    text NOT NULL,
    email           text,
    phone           text,
    address_id      uuid REFERENCES addresses(id),
    tax_id_hash     text,                -- one-way hash; never store raw
    metadata        jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    created_by      uuid NOT NULL,
    updated_by      uuid NOT NULL,
    version         bigint NOT NULL DEFAULT 1
);

CREATE INDEX parties_email_idx ON parties (email) WHERE email IS NOT NULL;

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id              uuid PRIMARY KEY,                  -- matches Keycloak sub
    email           text UNIQUE NOT NULL,
    display_name    text NOT NULL,
    role            text NOT NULL CHECK (role IN (
        'csr','adjuster','senior_adjuster','finance_operator',
        'operations','auditor','devops','qa','developer'
    )),
    active          boolean NOT NULL DEFAULT true,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint NOT NULL DEFAULT 1
);

-- ---------------------------------------------------------------------------
-- policies
-- ---------------------------------------------------------------------------
CREATE TABLE policies (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_number     text UNIQUE NOT NULL,
    product_line      text NOT NULL CHECK (product_line IN (
        'home','auto','commercial_property','liability','other'
    )),
    holder_party_id   uuid NOT NULL REFERENCES parties(id),
    effective_date    date NOT NULL,
    expiration_date   date NOT NULL,
    status            text NOT NULL CHECK (status IN (
        'active','lapsed','cancelled','pending'
    )),
    coverage_limits   jsonb NOT NULL,
    deductibles       jsonb NOT NULL,
    metadata          jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    created_by        uuid NOT NULL,
    updated_by        uuid NOT NULL,
    version           bigint NOT NULL DEFAULT 1,
    CHECK (expiration_date > effective_date)
);

CREATE INDEX policies_holder_idx ON policies (holder_party_id);

-- ---------------------------------------------------------------------------
-- claims
-- ---------------------------------------------------------------------------
CREATE TABLE claims (
    id                       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_number             text UNIQUE NOT NULL,
    policy_id                uuid NOT NULL REFERENCES policies(id),
    claimant_party_id        uuid NOT NULL REFERENCES parties(id),
    loss_type                text NOT NULL CHECK (loss_type IN (
        'fire','water','theft','wind','liability','auto_collision','other'
    )),
    loss_date                timestamptz NOT NULL,
    reported_date            timestamptz NOT NULL DEFAULT now(),
    severity                 text NOT NULL CHECK (severity IN (
        'low','medium','high','catastrophe'
    )),
    state                    text NOT NULL CHECK (state IN (
        'NEW','TRIAGE','INVESTIGATING','DECISION_PENDING',
        'APPROVED','RESERVED','PAYOUT_PENDING','PAID',
        'DENIED','WITHDRAWN','CLOSED','ARCHIVED','REOPENED'
    )),
    assigned_adjuster_id     uuid REFERENCES users(id),
    description              text NOT NULL,
    estimated_amount_cents   bigint NOT NULL DEFAULT 0,
    currency                 char(3) NOT NULL,
    fraud_score              numeric(5, 4),
    metadata                 jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at               timestamptz NOT NULL DEFAULT now(),
    updated_at               timestamptz NOT NULL DEFAULT now(),
    created_by               uuid NOT NULL,
    updated_by               uuid NOT NULL,
    version                  bigint NOT NULL DEFAULT 1
);

CREATE INDEX claims_state_idx            ON claims (state);
CREATE INDEX claims_assigned_idx         ON claims (assigned_adjuster_id, state);
CREATE INDEX claims_policy_idx           ON claims (policy_id);
CREATE UNIQUE INDEX claims_uniq_loss     ON claims (policy_id, loss_date, loss_type);

-- ---------------------------------------------------------------------------
-- claim_state_history (append-only)
-- ---------------------------------------------------------------------------
CREATE TABLE claim_state_history (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id            uuid NOT NULL REFERENCES claims(id),
    from_state          text,
    to_state            text NOT NULL,
    changed_by_user_id  uuid NOT NULL REFERENCES users(id),
    changed_at          timestamptz NOT NULL DEFAULT now(),
    reason              text NOT NULL,
    evidence_ref        uuid,
    genai_summary       text
);

CREATE INDEX csh_claim_idx ON claim_state_history (claim_id, changed_at DESC);

-- Write-once enforcement: reject UPDATE and DELETE.
CREATE OR REPLACE FUNCTION reject_audit_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'claim_state_history is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER csh_no_update
BEFORE UPDATE OR DELETE ON claim_state_history
FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();

-- ---------------------------------------------------------------------------
-- documents
-- ---------------------------------------------------------------------------
CREATE TABLE documents (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id            uuid NOT NULL REFERENCES claims(id),
    kind                text NOT NULL CHECK (kind IN (
        'photo','estimate','police_report','medical',
        'invoice','correspondence','other'
    )),
    original_filename   text NOT NULL,
    mime_type           text NOT NULL,
    byte_size           bigint NOT NULL,
    sha256              text NOT NULL,
    storage_uri         text NOT NULL,
    uploaded_by_user_id uuid NOT NULL REFERENCES users(id),
    uploaded_at         timestamptz NOT NULL DEFAULT now(),
    pii_classification  text NOT NULL DEFAULT 'none' CHECK (pii_classification IN (
        'none','name','address','id_number','financial','medical'
    )),
    pii_redacted_uri    text,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    created_by          uuid NOT NULL,
    updated_by          uuid NOT NULL,
    version             bigint NOT NULL DEFAULT 1
);

CREATE INDEX documents_claim_idx ON documents (claim_id);

-- ---------------------------------------------------------------------------
-- evidence_items
-- ---------------------------------------------------------------------------
CREATE TABLE evidence_items (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id            uuid NOT NULL REFERENCES claims(id),
    kind                text NOT NULL CHECK (kind IN (
        'document_ref','statement','site_visit','genai_output',
        'external_lookup','other'
    )),
    source              text NOT NULL,
    payload             jsonb NOT NULL,
    captured_at         timestamptz NOT NULL DEFAULT now(),
    captured_by_user_id uuid NOT NULL REFERENCES users(id),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    version             bigint NOT NULL DEFAULT 1
);

-- ---------------------------------------------------------------------------
-- decisions
-- ---------------------------------------------------------------------------
CREATE TABLE decisions (
    id                       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id                 uuid NOT NULL REFERENCES claims(id),
    outcome                  text NOT NULL CHECK (outcome IN (
        'approved','approved_partial','denied','withdrawn'
    )),
    approved_amount_cents    bigint,
    denial_reason            text,
    approved_by_user_id      uuid NOT NULL REFERENCES users(id),
    decided_at               timestamptz NOT NULL DEFAULT now(),
    rationale                text NOT NULL,
    genai_assisted           boolean NOT NULL DEFAULT false,
    genai_summary            text,
    created_at               timestamptz NOT NULL DEFAULT now(),
    updated_at               timestamptz NOT NULL DEFAULT now(),
    version                  bigint NOT NULL DEFAULT 1
);

CREATE INDEX decisions_claim_idx ON decisions (claim_id);

-- ---------------------------------------------------------------------------
-- reserves
-- ---------------------------------------------------------------------------
CREATE TABLE reserves (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id            uuid NOT NULL REFERENCES claims(id),
    amount_cents        bigint NOT NULL,
    currency            char(3) NOT NULL,
    set_by_user_id      uuid NOT NULL REFERENCES users(id),
    set_at              timestamptz NOT NULL DEFAULT now(),
    reason              text NOT NULL,
    superseded_by_id    uuid REFERENCES reserves(id),
    created_at          timestamptz NOT NULL DEFAULT now(),
    version             bigint NOT NULL DEFAULT 1
);

-- At most one active reserve per claim (the one not yet superseded).
CREATE UNIQUE INDEX reserves_active_uniq
    ON reserves (claim_id)
    WHERE superseded_by_id IS NULL;

-- ---------------------------------------------------------------------------
-- payouts
-- ---------------------------------------------------------------------------
CREATE TABLE payouts (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id             uuid NOT NULL REFERENCES claims(id),
    decision_id          uuid NOT NULL REFERENCES decisions(id),
    amount_cents         bigint NOT NULL,
    currency             char(3) NOT NULL,
    payee_party_id       uuid NOT NULL REFERENCES parties(id),
    method               text NOT NULL CHECK (method IN (
        'ach','wire','check','credit_on_account'
    )),
    status               text NOT NULL CHECK (status IN (
        'pending','instructed','settled','reversed','failed'
    )),
    instructed_at        timestamptz,
    settled_at           timestamptz,
    external_reference   text,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    version              bigint NOT NULL DEFAULT 1
);

CREATE INDEX payouts_claim_idx ON payouts (claim_id);
CREATE INDEX payouts_status_idx ON payouts (status);

-- ---------------------------------------------------------------------------
-- audit_log (append-only, hash-chained)
-- ---------------------------------------------------------------------------
CREATE TABLE audit_log (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at     timestamptz NOT NULL DEFAULT now(),
    actor_subject   text NOT NULL,
    actor_role      text,
    action          text NOT NULL,
    resource_type   text NOT NULL,
    resource_id     uuid NOT NULL,
    correlation_id  uuid,
    payload         jsonb,
    prev_hash       text,
    hash            text NOT NULL
);

CREATE INDEX audit_correlation_idx   ON audit_log (correlation_id);
CREATE INDEX audit_resource_idx      ON audit_log (resource_id);
CREATE INDEX audit_actor_time_idx    ON audit_log (actor_subject, occurred_at DESC);

CREATE TRIGGER audit_no_update
BEFORE UPDATE OR DELETE ON audit_log
FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();

COMMIT;
