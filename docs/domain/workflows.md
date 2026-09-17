# Claims Domain — Workflows

> The state machines. Every state, every transition, every
> approver, every GenAI touchpoint. The backend code must
> implement these exactly.

## 1. Claim lifecycle (master state machine)

```
            ┌────────┐
            │  NEW   │  claim created from FNOL
            └────┬───┘
                 │ triage_complete
                 ▼
            ┌────────┐
            │ TRIAGE │  severity assigned, adjuster queued
            └────┬───┘
                 │ adjuster_assigned
                 ▼
       ┌─────────────────┐
       │ INVESTIGATING   │  evidence collection, statements
       └────────┬────────┘
                │ evidence_complete
                ▼
       ┌──────────────────┐
       │ DECISION_PENDING │  awaiting adjudication
       └─┬──────┬──────┬──┘
         │      │      │
approved  │      │      │ denied
         │      │      │ withdrawn
         ▼      ▼      ▼
   ┌─────────┐  ┌───────┐  ┌───────────┐
   │APPROVED │  │DENIED │  │WITHDRAWN  │
   └────┬────┘  └───┬───┘  └─────┬─────┘
        │            │            │
reserve_set          close       close
        ▼            ▼            ▼
   ┌─────────┐  ┌────────┐  ┌────────┐
   │RESERVED │  │ CLOSED │  │ CLOSED │
   └────┬────┘  └────┬───┘  └────┬───┘
        │            │            │
payout_instructed   archive      archive
        ▼            ▼            ▼
  ┌──────────────┐  ┌────────┐
  │PAYOUT_PENDING│  │ARCHIVED│
  └──────┬───────┘  └────────┘
         │ payout_settled
         ▼
       ┌──────┐
       │ PAID │
       └──┬───┘
          │ close
          ▼
      ┌───────┐
      │CLOSED │
      └───┬───┘
          │ archive
          ▼
      ┌─────────┐
      │ARCHIVED │
      └─────────┘
```

`REOPENED` is reachable from `CLOSED` within 90 days, by Senior
Adjuster approval. From `REOPENED`, the state follows the normal
`DECISION_PENDING` branch.

## 2. Per-workflow detail

### 2.1 FNOL intake

| Field | Value |
|-------|-------|
| **Trigger** | Customer (web form / mobile) or CSR (phone) submits FNOL |
| **Actor** | Customer or CSR |
| **Pre-conditions** | Policy is active at loss date; claimant is a party on the policy |
| **Post-conditions** | Claim created, severity suggested, adjuster queue notified |
| **Approvers** | None (intake) |
| **GenAI touchpoints** | Form-fill assist, policy lookup, duplicate-claim detection |
| **SLA** | Triage assigned within 4 business hours |
| **Audit events** | `claim.create`, `claim.fnol.received` |
| **Failure modes** | Policy not found → return 422; duplicate FNOL → link to existing claim; loss date outside policy → return 422 with `loss_date_outside_coverage` |

### 2.2 Document intake

| Field | Value |
|-------|-------|
| **Trigger** | Document uploaded against a claim |
| **Actor** | CSR, Adjuster, Customer (if portal allows) |
| **Pre-conditions** | Claim is in a state that accepts documents (`TRIAGE`, `INVESTIGATING`, `DECISION_PENDING`, `REOPENED`) |
| **Post-conditions** | Document stored with PII classification, evidence link created |
| **Approvers** | None |
| **GenAI touchpoints** | OCR, classification suggestion, PII detection |
| **SLA** | n/a (instant) |
| **Audit events** | `document.upload`, `document.classify` |
| **Failure modes** | Unsupported mime type → 415; PII detected and reviewer not assigned → require manual review |

### 2.3 Triage

| Field | Value |
|-------|-------|
| **Trigger** | New claim enters triage |
| **Actor** | System (auto) + Adjuster (override) |
| **Pre-conditions** | Claim is `NEW`; basic policy + loss data present |
| **Post-conditions** | Severity assigned, adjuster auto-suggested, claim moves to `TRIAGE` |
| **Approvers** | None (auto) or Adjuster (manual override) |
| **GenAI touchpoints** | Severity suggestion, fraud score |
| **SLA** | 4 business hours from `NEW` |
| **Audit events** | `claim.triage.run`, `claim.triage.override` |
| **Failure modes** | Missing policy data → block; fraud score > 0.8 → flag for senior review |

### 2.4 Investigation

| Field | Value |
|-------|-------|
| **Trigger** | Adjuster begins investigation |
| **Actor** | Adjuster |
| **Pre-conditions** | Claim is `TRIAGE` or `REOPENED`; adjuster is assigned |
| **Post-conditions** | Evidence items collected, investigation notes recorded |
| **Approvers** | None (within scope); Senior Adjuster if scope changes |
| **GenAI touchpoints** | Statement summarisation, evidence clustering, similar-claim retrieval |
| **SLA** | 5 business days (low) / 30 (high) |
| **Audit events** | `claim.investigation.start`, `claim.investigation.note`, `claim.investigation.complete` |
| **Failure modes** | Evidence item missing required fields → block; SLA breach → notify Senior Adjuster |

### 2.5 Adjudication

| Field | Value |
|-------|-------|
| **Trigger** | Investigation complete |
| **Actor** | Adjuster |
| **Pre-conditions** | Claim is `INVESTIGATING`; minimum required evidence present |
| **Post-conditions** | Decision recorded, claim moves to `APPROVED` / `DENIED` / `WITHDRAWN` / `REOPENED` |
| **Approvers** | Senior Adjuster if `approved_amount_cents > $25,000`; UAT lead in UAT env |
| **GenAI touchpoints** | Decision rationale assist, similar-claim retrieval, payout sanity check |
| **SLA** | 2 business days from `DECISION_PENDING` |
| **Audit events** | `decision.create`, `decision.override` |
| **Failure modes** | Missing rationale → block; high-value approval missing → block; contradictory evidence → escalate |

### 2.6 Reserve management

| Field | Value |
|-------|-------|
| **Trigger** | Adjuster sets or adjusts a reserve |
| **Actor** | Adjuster |
| **Pre-conditions** | Claim is `APPROVED` or later (excluding `CLOSED`, `ARCHIVED`) |
| **Post-conditions** | Reserve row created, prior reserve marked superseded |
| **Approvers** | Senior Adjuster if delta > $10,000 |
| **GenAI touchpoints** | Reserve recommendation based on similar claims |
| **SLA** | 1 business day from `APPROVED` |
| **Audit events** | `reserve.create`, `reserve.adjust`, `reserve.release` |
| **Failure modes** | New reserve < already-paid total → block; reserve currency mismatch → block |

### 2.7 Payout

| Field | Value |
|-------|-------|
| **Trigger** | Reserve set + decision approved |
| **Actor** | Finance Operator (or auto on low-value) |
| **Pre-conditions** | Claim is `RESERVED`; decision exists and is `approved` or `approved_partial` |
| **Post-conditions** | Payout instruction issued, claim moves to `PAYOUT_PENDING`, then `PAID` |
| **Approvers** | Finance Operator (always) |
| **GenAI touchpoints** | Reconciliation check, anomaly flag |
| **SLA** | 3 business days from `PAYOUT_PENDING` |
| **Audit events** | `payout.instruct`, `payout.settle`, `payout.reverse` |
| **Failure modes** | Payout amount > approved amount → block; payee party inactive → block; duplicate instruction → block |

### 2.8 Reporting

| Field | Value |
|-------|-------|
| **Trigger** | Scheduled (daily) or on-demand |
| **Actor** | Operations, Auditor |
| **Pre-conditions** | n/a |
| **Post-conditions** | Report row created in the reporting schema |
| **Approvers** | None |
| **GenAI touchpoints** | Narrative summaries, trend detection, anomaly flagging |
| **SLA** | n/a |
| **Audit events** | `report.generate` |
| **Failure modes** | Source data missing → partial report with explicit gap |

## 3. Cross-cutting invariants

- **State transitions are append-only.** Every transition writes a
  row in `claim_state_history` in the same DB transaction as the
  state change.
- **Audit-log entries are write-once.** No `UPDATE`, no `DELETE` on
  `audit_log` (database trigger enforces).
- **Idempotency.** Every state-mutating endpoint requires an
  `Idempotency-Key` header. The server stores the result keyed on
  `(actor_subject, idempotency_key)` for 24 hours.
- **Time-boxed reopens.** `REOPENED` is only reachable from
  `CLOSED` within 90 days. After that, the claim can only be
  referenced — a new claim must be opened for any further action.
- **No silent state changes.** A state change without a
  `claim_state_history` row is a code defect, not a feature.

## 4. GenAI usage matrix (summary)

| Workflow | GenAI action | Data class | Authority |
|----------|--------------|------------|-----------|
| FNOL | Form assist, policy lookup | `internal`, `synthetic_test` (qa) | Advisory |
| Triage | Severity suggestion, fraud score | `operational` | Advisory |
| Investigation | Statement summary, evidence cluster | `operational` | Advisory |
| Adjudication | Rationale assist, similar claims | `operational`, `synthetic_test` | Advisory |
| Reserve | Reserve recommendation | `operational` | Advisory |
| Payout | Anomaly flag | `operational` | Advisory |
| Reporting | Narrative summary | `operational` | Advisory |

`production_pii` is never sent to GenAI. Decisions are made by
humans; GenAI assists the human with summarisation, clustering, and
retrieval, and writes its output to `claim_state_history.genai_summary`
or `decisions.genai_summary` for traceability.
