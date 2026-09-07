import { expect, test } from "@playwright/test";

// Dev credentials from services/auth-realm/acme-claims-realm.json.
// This spec exercises the core adjuster flow end to end: OIDC sign-in ->
// FNOL submission -> claim appears in the list -> lifecycle transitions.
const POLICY_NUMBER = `POL-E2E-${Date.now()}`;

test("adjuster signs in, files an FNOL and works it to approval", async ({ page }) => {
  await page.goto("/");

  // Sign in through Keycloak (same origin via the nginx /realms proxy)
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.fill("#username", "adjuster1");
  await page.fill("#password", "adjuster-dev-only");
  await page.click("input[name='login'], #kc-login");
  await expect(page.getByText("adjuster1")).toBeVisible();

  // FNOL
  await page.goto("/fnol");
  await page.fill("#policyNumber", POLICY_NUMBER);
  await page.selectOption("#claimType", "AUTO");
  const incident = new Date(Date.now() - 60 * 60 * 1000);
  const local = new Date(incident.getTime() - incident.getTimezoneOffset() * 60_000)
    .toISOString()
    .slice(0, 16);
  await page.fill("#incidentDate", local);
  await page.fill("#estimatedAmount", "1500.50");
  await page.fill("#description", "E2E: rear-end collision, no injuries");
  await page.getByRole("button", { name: "Submit FNOL" }).click();

  // Land on the claim detail, in FNOL state
  await expect(page.getByText("FNOL", { exact: true })).toBeVisible();

  // Work it forward: triage -> adjudication -> approve
  await page.getByRole("button", { name: "Start triage" }).click();
  await expect(page.getByText("TRIAGE", { exact: true })).toBeVisible();

  await page.getByRole("button", { name: "Begin adjudication" }).click();
  await expect(page.getByText("ADJUDICATION", { exact: true })).toBeVisible();

  await page.fill("input[placeholder='Approved amount (major units)']", "1200");
  await page.getByRole("button", { name: "Approve" }).click();
  await expect(page.getByText("APPROVED", { exact: true })).toBeVisible();

  // Back on the list, the claim is there with its status
  await page.goto("/");
  const row = page.locator("tr", { hasText: POLICY_NUMBER });
  await expect(row).toBeVisible();
  await expect(row.getByText("APPROVED")).toBeVisible();
});
