/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The gateway (BFF) is the single backend origin. Proxying these paths through the Vite dev
// server makes the browser treat every gateway call as same-origin, so the httpOnly `SESSION`
// cookie and the `XSRF-TOKEN`/`X-XSRF-TOKEN` CSRF handshake work with no CORS config. See
// docs/phase-2-plan.md §2.5 and ADR 0004.
const GATEWAY = 'http://localhost:8082';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: GATEWAY, changeOrigin: true },
      '/graphql': { target: GATEWAY, changeOrigin: true },
      '/oauth2': { target: GATEWAY, changeOrigin: true },
      '/login': { target: GATEWAY, changeOrigin: true },
      '/logout': { target: GATEWAY, changeOrigin: true },
      // WebSocket route reserved for the next 2.5 subphase (live notifications).
      '/ws': { target: GATEWAY, changeOrigin: true, ws: true },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
});
