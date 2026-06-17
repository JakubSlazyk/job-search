/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The gateway (BFF) is the single backend origin. Proxying these paths through the Vite dev
// server makes the browser treat every gateway call as same-origin, so the httpOnly `SESSION`
// cookie and the `XSRF-TOKEN`/`X-XSRF-TOKEN` CSRF handshake work with no CORS config. See
// docs/phase-2-plan.md §2.5 and ADR 0004.
//
// 127.0.0.1, not `localhost`: since Node 18 `localhost` resolves to IPv6 `::1` first, and if the
// gateway listens only on IPv4 the dev proxy stalls ~2s on a connect timeout before falling back.
const GATEWAY = 'http://127.0.0.1:8082';

export default defineConfig({
  plugins: [react()],
  server: {
    // Bind IPv4 loopback explicitly. Unset, Vite defaults to `localhost`, which on this machine
    // resolves to IPv6 `[::1]` only — so the browser's IPv4 attempt to 127.0.0.1:5173 has nothing
    // to connect to and stalls ~2s on a SYN timeout before falling back to ::1. Access the app at
    // http://127.0.0.1:5173 to match.
    host: '127.0.0.1',
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
