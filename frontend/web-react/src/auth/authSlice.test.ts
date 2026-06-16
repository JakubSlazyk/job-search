import { describe, expect, it } from 'vitest';
import reducer, { anonymous, authenticated, type AuthState, type AuthUser } from './authSlice';

const user: AuthUser = { subject: 'sub-1', username: 'jakub', email: null, displayName: 'Jakub S' };

describe('authSlice', () => {
  it('starts in the unknown state', () => {
    const state = reducer(undefined, { type: '@@INIT' });
    expect(state).toEqual<AuthState>({ status: 'unknown', user: null });
  });

  it('authenticated() stores the user and flips status', () => {
    const state = reducer(undefined, authenticated(user));
    expect(state.status).toBe('authenticated');
    expect(state.user).toEqual(user);
  });

  it('anonymous() clears the user', () => {
    const state = reducer({ status: 'authenticated', user }, anonymous());
    expect(state.status).toBe('anonymous');
    expect(state.user).toBeNull();
  });
});
