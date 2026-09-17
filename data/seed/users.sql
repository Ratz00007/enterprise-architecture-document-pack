-- Idempotent seed data for users (mirrors Keycloak).
-- Run after Keycloak realm is up so the UUIDs match.
-- In dev/qa, we use a single known UUID for the seed user
-- so the seed/policies.sql loader works out of the box.

BEGIN;

INSERT INTO users (id, email, display_name, role, active)
VALUES
    ('00000000-0000-0000-0000-000000000000', 'seed@internal.acme', 'Seed User', 'developer', true),
    ('00000000-0000-0000-0000-00000000a101', 'adjuster1@internal.acme', 'Adjuster One', 'adjuster', true),
    ('00000000-0000-0000-0000-00000000a102', 'adjuster2@internal.acme', 'Adjuster Two', 'senior_adjuster', true),
    ('00000000-0000-0000-0000-00000000a103', 'finance@internal.acme', 'Finance Operator', 'finance_operator', true)
ON CONFLICT (id) DO NOTHING;

COMMIT;
