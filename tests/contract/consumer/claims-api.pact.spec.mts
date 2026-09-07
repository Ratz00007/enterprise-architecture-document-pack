import { Matcher, PactV3, likeEachInAnyOrder } from "@pact-foundation/pact-js/v3";
import { afterEach, beforeEach, describe, it } from "vitest";

/**
 * Consumer-driven contract for the Claims API (ADR-013, Pact consumer test).
 * Runs the provider against an in-memory mock and writes the contract to
 * ./pacts — the provider side verifies it with the `-Ppact-verify` Maven
 * profile documented in the sibling README.
 */
const provider = new PactV3({
  consumer: "claims-web",
  provider: "claims-api",
  dir: new URL("./pacts", import.meta.url).pathname,
});

const FNOL_REQUEST = {
  policyNumber: "POL-CT-1",
  claimType: "AUTO",
  incidentDate: "2026-09-01T10:00:00Z",
  description: "Contract test FNOL",
  estimatedAmountMinor: 150000,
};

const CLAIM_RESPONSE_MATCHER = Matcher.like({
  id: Matcher.string("018f2c1e-7d3a-7000-8000-000000000000"),
  claimNumber: Matcher.string("CLM-20260906-ABC123"),
  policyNumber: "POL-CT-1",
  status: "FNOL",
  claimType: "AUTO",
  incidentDate: "2026-09-01T10:00:00Z",
  reportedDate: Matcher.string("2026-09-06T00:00:00Z"),
  description: "Contract test FNOL",
  estimatedAmountMinor: Matcher.integer(150000),
  approvedAmountMinor: Matcher.nullValue(),
  paidAmountMinor: Matcher.nullValue(),
  assignedTo: Matcher.nullValue(),
  triagePriority: Matcher.nullValue(),
  triageScore: Matcher.nullValue(),
  adjudicationNotes: Matcher.nullValue(),
  rejectionReason: Matcher.nullValue(),
  payoutReference: Matcher.nullValue(),
  payoutDate: Matcher.nullValue(),
  version: Matcher.integer(0),
  createdAt: Matcher.string("2026-09-06T00:00:00Z"),
  updatedAt: Matcher.nullValue(),
  createdBy: Matcher.string("adjuster-1"),
  updatedBy: Matcher.nullValue(),
});

beforeEach(() => provider.setup());
afterEach(() => provider.finalize());

describe("POST /claims (FNOL)", () => {
  it("creates a claim with an Idempotency-Key and returns 201", async () => {
    provider
      .given("a Keycloak adjuster token")
      .uponReceiving("an FNOL submission with an idempotency key")
      .withRequest({
        method: "POST",
        path: "/claims",
        headers: {
          "Content-Type": "application/json",
          "Idempotency-Key": Matcher.string("11111111-1111-4111-8111-111111111111"),
          Authorization: Matcher.string("Bearer a.b.c"),
        },
        body: FNOL_REQUEST,
      })
      .willRespondWith({
        status: 201,
        headers: { "Content-Type": "application/json" },
        body: CLAIM_RESPONSE_MATCHER,
      });

    await provider.executeTest(async (mockServer) => {
      const response = await fetch(`${mockServer.url}/claims`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Idempotency-Key": "11111111-1111-4111-8111-111111111111",
          Authorization: "Bearer a.b.c",
        },
        body: JSON.stringify(FNOL_REQUEST),
      });
      if (response.status !== 201) {
        throw new Error(`expected 201, got ${response.status}: ${await response.text()}`);
      }
      const claim = (await response.json()) as Record<string, unknown>;
      if (claim.status !== "FNOL") {
        throw new Error(`expected FNOL status, got ${claim.status}`);
      }
    });
  });
});

void likeEachInAnyOrder; // reserved for list-endpoint interactions
