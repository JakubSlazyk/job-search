import { useEffect } from 'react';
import { apiFetch, readCookie } from '../api/http';
import { useAppDispatch, useAppSelector } from '../store';
import { anonymous, authenticated, type AuthUser } from './authSlice';

// Kicks off login by redirecting the browser to the gateway's OAuth2 entry point (full navigation,
// not fetch — the BFF runs Authorization Code + PKCE against Keycloak).
export function login(): void {
  window.location.assign('/oauth2/authorization/keycloak');
}

// RP-initiated logout must be a top-level navigation, not a fetch: the gateway responds with a
// redirect chain through Keycloak's end-session endpoint (cross-origin), which fetch cannot follow
// under CORS. We POST a form (Spring's /logout requires POST + CSRF) carrying the CSRF token as the
// `_csrf` field; the gateway then bounces the browser back to the SPA (app.frontend-base-url).
export function logout(): void {
  const form = document.createElement('form');
  form.method = 'POST';
  form.action = '/logout';

  const token = readCookie('XSRF-TOKEN');
  if (token) {
    const csrf = document.createElement('input');
    csrf.type = 'hidden';
    csrf.name = '_csrf';
    csrf.value = token;
    form.appendChild(csrf);
  }

  document.body.appendChild(form);
  form.submit();
}

// Bootstraps auth state once on mount by probing GET /api/v1/users/me: 200 → authenticated,
// anything else (notably 401) → anonymous.
export function useAuthBootstrap(): void {
  const dispatch = useAppDispatch();

  useEffect(() => {
    let cancelled = false;

    void (async () => {
      try {
        const res = await apiFetch('/api/v1/users/me');
        if (cancelled) return;
        if (res.ok) {
          const user = (await res.json()) as AuthUser;
          dispatch(authenticated(user));
        } else {
          dispatch(anonymous());
        }
      } catch {
        if (!cancelled) dispatch(anonymous());
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [dispatch]);
}

export function useAuth() {
  return useAppSelector((state) => state.auth);
}
