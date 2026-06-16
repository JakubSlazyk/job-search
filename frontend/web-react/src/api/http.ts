// CSRF-aware fetch wrapper for the BFF. Every call sends the httpOnly `SESSION` cookie
// (`credentials: 'include'`); mutating calls additionally echo the `XSRF-TOKEN` cookie as the
// `X-XSRF-TOKEN` header, which the gateway's CookieServerCsrfTokenRepository expects. See
// api-gateway SecurityConfiguration.kt and docs/phase-2-plan.md §2.5.

const CSRF_COOKIE = 'XSRF-TOKEN';
const CSRF_HEADER = 'X-XSRF-TOKEN';
const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

export function readCookie(name: string): string | null {
  const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = document.cookie.match(new RegExp('(?:^|;\\s*)' + escaped + '=([^;]*)'));
  return match ? decodeURIComponent(match[1]) : null;
}

export async function apiFetch(input: string, init: RequestInit = {}): Promise<Response> {
  const method = (init.method ?? 'GET').toUpperCase();
  const headers = new Headers(init.headers);

  if (MUTATING_METHODS.has(method)) {
    const token = readCookie(CSRF_COOKIE);
    if (token) {
      headers.set(CSRF_HEADER, token);
    }
  }

  return fetch(input, { ...init, headers, credentials: 'include' });
}
