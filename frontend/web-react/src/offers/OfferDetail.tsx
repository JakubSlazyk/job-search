import { useQuery } from '@apollo/client/react';
import { Link, useParams } from 'react-router-dom';
import { OFFER, type Offer } from './queries';

interface OfferData {
  offer: Offer | null;
}

export default function OfferDetail() {
  const { offerId = '' } = useParams();
  const { data, loading, error } = useQuery<OfferData, { offerId: string }>(OFFER, {
    variables: { offerId },
  });

  if (loading) return <p>Loading…</p>;
  if (error) return <p role="alert">Could not load offer: {error.message}</p>;

  const offer = data?.offer;
  if (!offer) return <p>Offer not found. <Link to="/">Back to offers</Link></p>;

  return (
    <article>
      <p>
        <Link to="/">← Back to offers</Link>
      </p>
      <h2>{offer.title}</h2>
      <p>
        <strong>{offer.company}</strong>
        {offer.location ? ` · ${offer.location}` : ''}
        {offer.seniority ? ` · ${offer.seniority}` : ''}
      </p>
      <p>Source: {offer.source}</p>
      {offer.url && (
        <p>
          <a href={offer.url} target="_blank" rel="noreferrer">
            View original posting
          </a>
        </p>
      )}
      <p>{offer.description}</p>
    </article>
  );
}
