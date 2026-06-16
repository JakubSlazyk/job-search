import { useState, type FormEvent } from 'react';
import { useQuery } from '@apollo/client/react';
import { Link } from 'react-router-dom';
import { OFFERS, type Offer, type OffersVars } from './queries';

const PAGE_SIZE = 20;

interface OffersData {
  offers: Offer[];
}

export default function OffersPage() {
  // `filters` is what we query; `form` is the live form state, applied on submit.
  const [filters, setFilters] = useState<OffersVars>({ page: 0, size: PAGE_SIZE });
  const [form, setForm] = useState({ query: '', source: '', location: '', seniority: '' });

  const { data, loading, error } = useQuery<OffersData, OffersVars>(OFFERS, {
    variables: filters,
  });

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    setFilters({
      query: form.query || undefined,
      source: form.source || undefined,
      location: form.location || undefined,
      seniority: form.seniority || undefined,
      page: 0,
      size: PAGE_SIZE,
    });
  };

  const page = filters.page ?? 0;
  const offers = data?.offers ?? [];

  return (
    <section>
      <h2>Offers</h2>

      <form onSubmit={onSubmit} aria-label="Search offers">
        <input
          aria-label="Search"
          placeholder="Search title, company, description"
          value={form.query}
          onChange={(e) => setForm({ ...form, query: e.target.value })}
        />
        <input
          aria-label="Source"
          placeholder="Source"
          value={form.source}
          onChange={(e) => setForm({ ...form, source: e.target.value })}
        />
        <input
          aria-label="Location"
          placeholder="Location"
          value={form.location}
          onChange={(e) => setForm({ ...form, location: e.target.value })}
        />
        <input
          aria-label="Seniority"
          placeholder="Seniority"
          value={form.seniority}
          onChange={(e) => setForm({ ...form, seniority: e.target.value })}
        />
        <button type="submit">Search</button>
      </form>

      {loading && <p>Loading…</p>}
      {error && <p role="alert">Could not load offers: {error.message}</p>}

      {!loading && !error && (
        <ul>
          {offers.map((offer) => (
            <li key={offer.offerId}>
              <Link to={`/offers/${offer.offerId}`}>{offer.title}</Link> — {offer.company}
              {offer.location ? ` · ${offer.location}` : ''}
              {offer.seniority ? ` · ${offer.seniority}` : ''}
            </li>
          ))}
          {offers.length === 0 && <li>No offers found.</li>}
        </ul>
      )}

      <nav aria-label="Pagination">
        <button
          type="button"
          disabled={page === 0}
          onClick={() => setFilters({ ...filters, page: page - 1 })}
        >
          Previous
        </button>
        <span> Page {page + 1} </span>
        <button
          type="button"
          disabled={offers.length < PAGE_SIZE}
          onClick={() => setFilters({ ...filters, page: page + 1 })}
        >
          Next
        </button>
      </nav>
    </section>
  );
}
