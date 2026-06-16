import { Link } from 'react-router-dom';
import { login, logout, useAuth } from '../auth/useAuth';

export default function Header() {
  const { status, user } = useAuth();

  return (
    <header>
      <nav>
        <Link to="/">
          <strong>job-search</strong>
        </Link>
      </nav>
      <div>
        {status === 'unknown' && <span>…</span>}
        {status === 'anonymous' && (
          <button type="button" onClick={login}>
            Log in
          </button>
        )}
        {status === 'authenticated' && user && (
          <>
            <span>{user.displayName ?? user.username}</span>{' '}
            <button type="button" onClick={() => void logout()}>
              Log out
            </button>
          </>
        )}
      </div>
    </header>
  );
}
