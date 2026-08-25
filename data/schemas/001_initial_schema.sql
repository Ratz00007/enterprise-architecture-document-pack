-- Acme Claims Processing Platform - Initial Database Schema
-- PostgreSQL 16 compatible
-- As per ADR-013: Two-database architecture (Operational + Analytics)

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Claims table
CREATE TABLE claims (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    claim_number VARCHAR(50) UNIQUE NOT NULL,
    policy_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    claim_type VARCHAR(20) NOT NULL,
    incident_date TIMESTAMP NOT NULL,
    reported_date TIMESTAMP NOT NULL,
    description TEXT,
    estimated_amount DECIMAL(15, 2),
    approved_amount DECIMAL(15, 2),
    paid_amount DECIMAL(15, 2),
    assigned_to VARCHAR(100),
    triage_priority INTEGER,
    triage_score DOUBLE PRECISION,
    adjudication_notes TEXT,
    payout_reference VARCHAR(100),
    payout_date TIMESTAMP,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Indexes for claims table
CREATE INDEX idx_claim_status ON claims(status);
CREATE INDEX idx_claim_policy_number ON claims(policy_number);
CREATE INDEX idx_claim_created_at ON claims(created_at);
CREATE INDEX idx_claim_assigned_to ON claims(assigned_to);
CREATE INDEX idx_claim_triage_priority ON claims(triage_priority DESC);

-- Claim documents table
CREATE TABLE claim_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    claim_id UUID NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_by VARCHAR(100) NOT NULL,
    is_pii BOOLEAN DEFAULT false
);

CREATE INDEX idx_claim_doc_claim_id ON claim_documents(claim_id);
CREATE INDEX idx_claim_doc_type ON claim_documents(document_type);

-- Claim activity log table
CREATE TABLE claim_activity_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    claim_id UUID NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20),
    description TEXT,
    performed_by VARCHAR(100) NOT NULL,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX idx_claim_activity_claim_id ON claim_activity_log(claim_id);
CREATE INDEX idx_claim_activity_performed_at ON claim_activity_log(performed_at DESC);

-- Users table (for assignment tracking)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    role VARCHAR(50) NOT NULL,
    department VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    keycloak_id VARCHAR(100)
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- Audit log table
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT
);

CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_changed_at ON audit_log(changed_at DESC);
CREATE INDEX idx_audit_changed_by ON audit_log(changed_by);

-- GenAI request log table
CREATE TABLE genai_request_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    claim_id UUID REFERENCES claims(id),
    request_type VARCHAR(50) NOT NULL,
    request_payload JSONB NOT NULL,
    response_payload JSONB,
    provider VARCHAR(50),
    model VARCHAR(100),
    tokens_used INTEGER,
    latency_ms INTEGER,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_genai_claim_id ON genai_request_log(claim_id);
CREATE INDEX idx_genai_created_at ON genai_request_log(created_at DESC);
CREATE INDEX idx_genai_status ON genai_request_log(status);

-- Comments on tables
COMMENT ON TABLE claims IS 'Core claims table storing all insurance claims';
COMMENT ON TABLE claim_documents IS 'Documents associated with claims';
COMMENT ON TABLE claim_activity_log IS 'Activity log for claim workflow transitions';
COMMENT ON TABLE users IS 'System users for claim assignment';
COMMENT ON TABLE audit_log IS 'Comprehensive audit trail for all entities';
COMMENT ON TABLE genai_request_log IS 'Log of all GenAI gateway requests';
