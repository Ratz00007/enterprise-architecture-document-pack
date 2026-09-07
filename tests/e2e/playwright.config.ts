import { defineConfig } from "@playwright/test";

// Runs against the compose stack (docker compose up --build):
//   web      http://localhost:8081  (same-origin OIDC + /api proxy)
export default defineConfig({
  testDir: "./specs",
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: 1,
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:8081",
    screenshot: "only-on-failure",
  },
  reporter: [["list"]],
});
