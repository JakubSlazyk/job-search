import { useState } from 'react';
import { Link } from 'react-router-dom';
import { login, logout, useAuth } from '../auth/useAuth';
import { theme } from '../theme';

// Terminal-style top bar: brand prompt on the left, real BFF auth on the right. Ported visuals
// from the northpost prototype, wired to the actual auth slice (status + user).
export default function Header() {
  const { status, user } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <header style={styles.header}>
      <Link to="/" style={styles.brandWrap} onClick={() => setMenuOpen(false)}>
        <span style={{ fontSize: 13, color: theme.accent }}>~/northpost</span>
        <span style={{ fontSize: 13, color: theme.textDim }}>$</span>
        <span style={styles.brand}>northpost</span>
        <span style={styles.caret}>▮</span>
      </Link>

      <div style={styles.right}>
        {status === 'unknown' && <span style={{ color: theme.textMuted, fontSize: 12 }}>…</span>}

        {status === 'anonymous' && (
          <button type="button" onClick={login} style={styles.loginBtn}>
            [ log in ]
          </button>
        )}

        {status === 'authenticated' && user && (
          <>
            <button
              type="button"
              onClick={() => setMenuOpen((o) => !o)}
              style={styles.profileBtn}
              aria-haspopup="menu"
              aria-expanded={menuOpen}
            >
              <span style={{ color: theme.accent }}>[</span>
              <span>{user.displayName ?? user.username}</span>
              <span style={{ color: theme.accent }}>]</span>
              <span style={{ color: theme.textMuted }}>▾</span>
            </button>

            <button type="button" onClick={() => void logout()} style={styles.logoutBtn}>
              &gt; log out
            </button>

            {menuOpen && (
              <div style={styles.menu} role="menu">
                <div style={{ padding: '9px 11px' }}>
                  <div style={{ color: theme.textBright, fontWeight: 700 }}>{user.username}</div>
                  {user.email && (
                    <div style={{ color: theme.textMuted, fontSize: 11, marginTop: 2 }}>
                      {user.email}
                    </div>
                  )}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </header>
  );
}

const styles: Record<string, React.CSSProperties> = {
  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 24,
    padding: '0 20px',
    height: 56,
    background: theme.bgPanel,
    borderBottom: `1px solid ${theme.borderMid}`,
    flex: '0 0 auto',
    zIndex: 5,
  },
  brandWrap: { display: 'flex', alignItems: 'center', gap: 10 },
  brand: {
    fontWeight: 700,
    fontSize: 15,
    letterSpacing: '0.04em',
    textTransform: 'uppercase',
    color: theme.accentBright,
    textShadow: `0 0 9px ${theme.accentGlow}`,
  },
  caret: { color: theme.accentBright, fontSize: 15, animation: 'blink 1.1s step-end infinite' },
  right: { display: 'flex', alignItems: 'center', gap: 9, position: 'relative' },
  loginBtn: {
    background: 'transparent',
    border: `1px solid ${theme.borderBright}`,
    padding: '8px 14px',
    fontFamily: 'inherit',
    fontSize: 11,
    fontWeight: 700,
    textTransform: 'uppercase',
    color: theme.textSoft,
    cursor: 'pointer',
  },
  profileBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    background: 'transparent',
    border: `1px solid ${theme.borderBright}`,
    padding: '6px 12px',
    cursor: 'pointer',
    color: theme.textSoft,
    fontFamily: 'inherit',
    fontSize: 12,
    textTransform: 'uppercase',
  },
  logoutBtn: {
    background: 'transparent',
    border: `1px solid ${theme.borderBright}`,
    padding: '8px 12px',
    fontFamily: 'inherit',
    fontSize: 11,
    fontWeight: 700,
    textTransform: 'uppercase',
    color: theme.danger,
    cursor: 'pointer',
  },
  menu: {
    position: 'absolute',
    top: 44,
    right: 0,
    width: 220,
    background: theme.bgPanel,
    border: `1px solid ${theme.borderBright}`,
    boxShadow: '0 0 24px rgba(95,211,95,0.12)',
    padding: 5,
    zIndex: 20,
    fontSize: 12,
  },
};
