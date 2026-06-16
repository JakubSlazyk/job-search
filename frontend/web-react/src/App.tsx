import { Route, Routes } from 'react-router-dom';
import Header from './components/Header';
import { useAuthBootstrap } from './auth/useAuth';
import OffersPage from './offers/OffersPage';
import OfferDetail from './offers/OfferDetail';

export default function App() {
  useAuthBootstrap();

  return (
    <>
      <Header />
      <main>
        <Routes>
          <Route path="/" element={<OffersPage />} />
          <Route path="/offers/:offerId" element={<OfferDetail />} />
        </Routes>
      </main>
    </>
  );
}
