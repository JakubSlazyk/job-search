import { createSlice, type PayloadAction } from '@reduxjs/toolkit';

// Mirror of user-service UserResponse (the subset the SPA needs). The SPA never holds tokens —
// authentication state is derived purely from whether GET /api/v1/users/me succeeds (BFF, ADR 0004).
export interface AuthUser {
  subject: string;
  username: string;
  email: string | null;
  displayName: string | null;
}

export type AuthStatus = 'unknown' | 'authenticated' | 'anonymous';

export interface AuthState {
  status: AuthStatus;
  user: AuthUser | null;
}

const initialState: AuthState = {
  status: 'unknown',
  user: null,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    authenticated(state, action: PayloadAction<AuthUser>) {
      state.status = 'authenticated';
      state.user = action.payload;
    },
    anonymous(state) {
      state.status = 'anonymous';
      state.user = null;
    },
  },
});

export const { authenticated, anonymous } = authSlice.actions;
export default authSlice.reducer;
