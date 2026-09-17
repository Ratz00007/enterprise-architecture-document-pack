# Runbook — Production Rollback

## Trigger

- Sev-1 alert: error rate > 5% for 2 min in prod, OR
- Sev-2 alert: GenAI advisory says "rollback recommended" with high
  confidence, OR
- Manual: release operator decides to roll back.

## Triage

1. Acknowledge the page in PagerDuty.
2. Open the Jenkins pipeline for the prod rollback job:
   `https://ci.internal.acme/job/acme-claims/job/prod/job/rollback`.
3. Find the previous good digest in
   `https://grafana.internal.acme/d/release-history` (last 10 prod releases).
4. Open #incidents and post the initial notification (see
   `ops/incident-response/comms.md`).

## Mitigation

> **Always roll back first, debug later.** A known-good state is
> better than an unknown-broken one.

1. In Jenkins, click "Build with Parameters" on the rollback job.
2. Enter the target digest. Double-check it.
3. Click "Build". The job will:
   - Verify the cosign signature of the rollback target.
   - Stop the current prod-app-01.
   - Start the rollback artifact.
   - Run the smoke test.
4. Watch the smoke test. If it fails, **stop here and call the
   secondary oncall**.

## Recovery

1. After the smoke test passes, the rollback is live.
2. Re-run the integration tests against prod (read-only checks).
3. Post a status update in #incidents every 15 min until the
   incident is closed.
4. Once the system is stable, capture the digest of the bad
   release in the incident record and disable its promotion in
   Jenkins.

## Postmortem hooks

- The exact digests involved (good and bad).
- The detection time vs. the rollback time.
- The customer impact window.
- The decision that was made (and what the alternative was).
- Action items with owners and dates.
