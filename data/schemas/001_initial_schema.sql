-- Acme Claims Processing Platform - Initial Operational Schema
-- PostgreSQL 16.
-- Conventions (AGENTS.md, ADR-007, ADR-009): snake_case names, UUIDv7 primary
-- keys generated application-side (gen_random_uuid() is only a safety
-- fallback), money as BIGINT minor units, all timestamps TIMESTAMPTZ (UTC).

-- Claims table
CREATE TABLE claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_number VARCHAR(50) UNIQUE NOT NULL,
    policy_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('FNOL', 'TRIAGE', 'ADJUDICATION', 'APPROVED', 'REJECTED', 'PAYOUT', 'PAID', 'CLOSED')),
    claim_type VARCHAR(20) NOT NULL CHECK (claim_type IN ('AUTO', 'PROPERTY', 'HEALTH', 'LIFE', 'LIABILITY', 'WORKERS_COMP', 'OTHER')),
    incident_date TIMESTAMPTZ NOT NULL,
    reported_date TIMESTAMPTZ NOT NULL,
    description TEXT,
    estimated_amount BIGINT,
    approved_amount BIGINT,
    paid_amount BIGINT,
    assigned_to VARCHAR(100),
    triage_priority INTEGER CHECK (triage_priority BETWEEN 1 AND 5),
    triage_score DOUBLE PRECISION,
    adjudication_notes TEXT,
    rejection_reason TEXT,
    payout_reference VARCHAR(100),
    payout_date TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_claim_status ON claims(status);
CREATE INDEX idx_claim_policy_number ON claims(policy_number);
CREATE INDEX idx_claim_created_at ON claims(created_at);
CREATE INDEX idx_claim_assigned_to ON claims(assigned_to);
CREATE INDEX idx_claim_triage_priority ON claims(triage_priority DESC);

-- Claim documents table
CREATE TABLE claim_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    uploaded_by VARCHAR(100) NOT NULL,
    is_pii BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_claim_doc_claim_id ON claim_documents(claim_id);
CREATE INDEX idx_claim_doc_type ON claim_documents(document_type);

-- Claim activity log table
CREATE TABLE claim_activity_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    actor VARCHAR(100) NOT NULL,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_claim_activity_claim_id ON claim_activity_log(claim_id);
CREATE INDEX idx_claim_activity_created_at ON claim_activity_log(created_at);

-- Idempotency store (ADR-008)
CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    endpoint VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
