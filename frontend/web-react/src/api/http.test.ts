import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';
import { apiFetch, readCookie } from './http';

describe('apiFetch (CSRF + credentials)', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=tok-123; path=/';
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response(null, { status: 200 })),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('reads a cookie value', () => {
    expect(readCookie('XSRF-TOKEN')).toBe('tok-123');
  });

  it('attaches X-XSRF-TOKEN and credentials on mutating requests', async () => {
    await apiFetch('/logout', { method: 'POST' });
    const [, init] = (fetch as Mock).mock.calls[0];
    const headers = new Headers(init.headers);
    expect(headers.get('X-XSRF-TOKEN')).toBe('tok-123');
    expect(init.credentials).toBe('include');
  });

  it('does not attach the CSRF header on GET but still sends credentials', async () => {
    await apiFetch('/api/v1/users/me');
    const [, init] = (fetch as Mock).mock.calls[0];
    const headers = new Headers(init.headers);
    expect(headers.get('X-XSRF-TOKEN')).toBeNull();
    expect(init.credentials).toBe('include');
  });
});
