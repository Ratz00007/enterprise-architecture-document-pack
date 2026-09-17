-- Idempotent seed data for the QA environment.
-- Run via Flyway callback or `psql -v ON_ERROR_STOP=1 -f seed/policies.sql`.
-- Safe to re-run.

BEGIN;

INSERT INTO parties (id, kind, display_name, email, created_by, updated_by)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'person',     'Acme QA Holder',     'holder@example.com',   :'seed_user', :'seed_user'),
    ('00000000-0000-0000-0000-000000000002', 'organization','Acme QA Garage',     'garage@example.com',   :'seed_user', :'seed_user'),
    ('00000000-0000-0000-0000-000000000003', 'person',     'Acme QA Witness',    'witness@example.com',  :'seed_user', :'seed_user')
ON CONFLICT (id) DO NOTHING;

INSERT INTO policies (id, policy_number, product_line, holder_party_id, effective_date, expiration_date, status, coverage_limits, deductibles, created_by, updated_by)
VALUES
    ('00000000-0000-0000-0000-00000000a001', 'P-QA-2026-00000001', 'home', '00000000-0000-0000-0000-000000000001', DATE '2026-01-01', DATE '2027-01-01', 'active',
     '{"dwelling": 250000, "personal_property": 100000, "liability": 100000}'::jsonb,
     '{"dwelling": 1000, "personal_property": 500, "liability": 0}'::jsonb,
     :'seed_user', :'seed_user'),
    ('00000000-0000-0000-0000-00000000a002', 'P-QA-2026-00000002', 'auto', '00000000-0000-0000-0000-000000000001', DATE '2026-01-01', DATE '2027-01-01', 'active',
     '{"collision": 50000, "comprehensive": 50000, "liability": 100000}'::jsonb,
     '{"collision": 500, "comprehensive": 250, "liability": 0}'::jsonb,
     :'seed_user', :'seed_user')
ON CONFLICT (id) DO NOTHING;

COMMIT;
