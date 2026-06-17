import { Route, Routes } from 'react-router-dom';
import Header from './components/Header';
import { useAuthBootstrap } from './auth/useAuth';
import OffersPage from './offers/OffersPage';

// Terminal-shell layout: fixed header, flexible body, and a CRT scanline overlay. Both routes render
// OffersPage; the offer route additionally drives the detail slide-over (via the :offerId param).
export default function App() {
  useAuthBootstrap();

  return (
    <div style={styles.root}>
      <Header />
      <Routes>
        <Route path="/" element={<OffersPage />} />
        <Route path="/offers/:offerId" element={<OffersPage />} />
      </Routes>
      <div style={styles.scanlines} />
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  root: {
    position: 'relative',
    height: '100vh',
    display: 'flex',
    flexDirection: 'column',
    background: '#0b0e0a',
    color: '#cfe3c7',
    overflow: 'hidden',
  },
  scanlines: {
    position: 'fixed',
    inset: 0,
    pointerEvents: 'none',
    zIndex: 9999,
    background:
      'repeating-linear-gradient(rgba(0,0,0,0) 0px, rgba(0,0,0,0) 2px, rgba(0,0,0,0.16) 3px)',
    mixBlendMode: 'multiply',
  },
};
