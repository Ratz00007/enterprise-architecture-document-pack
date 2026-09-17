@acme-claims
Feature: Claim happy path — FNOL through payout
  A customer reports a loss, an adjuster adjudicates, finance pays out.

  Background:
    Given an active policy P-QA-2026-00000001
    And an authenticated adjuster

  Scenario: Submit FNOL, triage, adjudicate, pay out
    When the customer submits an FNOL for a water loss
    Then the claim is in state NEW
    And the system suggests severity "medium"
    When the adjuster completes triage
    Then the claim is in state TRIAGE
    And an adjuster is auto-assigned
    When the adjuster completes investigation with rationale
    Then the claim is in state DECISION_PENDING
    When the adjuster approves with amount 5000 USD
    Then the claim is in state APPROVED
    And a reserve of 5000 USD is set
    When finance instructs the payout
    Then the claim is in state PAYOUT_PENDING
    And the payout is reconciled
    When the payout settles
    Then the claim is in state PAID
    And the audit log has one row per transition
