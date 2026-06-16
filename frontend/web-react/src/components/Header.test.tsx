import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import Header from './Header';
import authReducer, { type AuthState } from '../auth/authSlice';

function renderHeader(auth: AuthState) {
  const store = configureStore({ reducer: { auth: authReducer }, preloadedState: { auth } });
  return render(
    <Provider store={store}>
      <MemoryRouter>
        <Header />
      </MemoryRouter>
    </Provider>,
  );
}

describe('Header', () => {
  it('shows Log in when anonymous', () => {
    renderHeader({ status: 'anonymous', user: null });
    expect(screen.getByRole('button', { name: /log in/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /log out/i })).not.toBeInTheDocument();
  });

  it('shows the display name and Log out when authenticated', () => {
    renderHeader({
      status: 'authenticated',
      user: { subject: 'sub-1', username: 'jakub', email: null, displayName: 'Jakub S' },
    });
    expect(screen.getByText('Jakub S')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /log out/i })).toBeInTheDocument();
  });
});
