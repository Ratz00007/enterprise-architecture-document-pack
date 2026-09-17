# Data

DB schemas, seed data, and masking rules. Source of truth for the
shape of the data; the API is just a façade over it.

```
data/
├── README.md         # this file
├── schemas/
│   ├── qa/           # Flyway migrations for the qa-family DBs
│   │   └── V*.sql
│   └── prod/         # Flyway migrations for the prod DB
│       └── V*.sql
├── seed/             # idempotent test data loaders
│   ├── policies.sql
│   ├── parties.sql
│   └── users.sql
└── masking/
    └── rules.yaml    # per-PII-column sanitization strategy
```

## Schemas

`schemas/qa/` and `schemas/prod/` are **identical in shape**, not
in data. Migrations are append-only. Both directories are tracked
in git.

## Migrations

We use Flyway. Migrations are versioned: `V1__init.sql`,
`V2__add_claims_table.sql`, etc. A migration is committed in the
same PR as the code that requires it. A bad migration is fixed by
a new migration, never by editing the old one.

## Seed data

The `seed/` directory contains idempotent loaders for the QA
environment. They are safe to run multiple times. They never run
in production.

## Masking

The `masking/rules.yaml` file is the data-class-aware masking
config. The `db-sync` job reads it, applies it, and records the
version it used in the audit log. The schema of the file lives in
`services/db-sync/README.md`.
