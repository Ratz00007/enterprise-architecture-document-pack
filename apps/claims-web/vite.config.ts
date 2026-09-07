/// <reference types="vitest" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Dev proxy keeps everything same-origin: the OIDC issuer and the API both
// resolve through this Vite server, mirroring the nginx layout in compose.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": { target: "http://localhost:8080", changeOrigin: true },
      "/realms": { target: "http://localhost:8180", changeOrigin: true },
      "/resources": { target: "http://localhost:8180", changeOrigin: true },
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: "./src/test/setup.ts",
  },
});
