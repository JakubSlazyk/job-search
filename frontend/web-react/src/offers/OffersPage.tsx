import { useState } from 'react';
import { useQuery } from '@apollo/client/react';
import { useNavigate, useParams } from 'react-router-dom';
import { OFFERS, type Offer, type OffersVars } from './queries';
import FilterPanel, { type FilterForm } from './FilterPanel';
import ResultsTable, { type SortKey, type SortState } from './ResultsTable';
import DetailSidebar from './DetailSidebar';
import { theme } from '../theme';

const PAGE_SIZE = 20;

const EMPTY_FORM: FilterForm = { query: '', location: '', source: '', seniority: '' };

interface OffersData {
  offers: Offer[];
}

// Browse page: themed filter sidebar + results table, with a route-driven detail slide-over.
// `form` is the live filter inputs; `vars` is what we actually query (applied on submit). Sorting is
// client-side over the fetched page.
export default function OffersPage() {
  const { offerId } = useParams();
  const navigate = useNavigate();

  const [form, setForm] = useState<FilterForm>(EMPTY_FORM);
  const [vars, setVars] = useState<OffersVars>({ page: 0, size: PAGE_SIZE });
  const [sort, setSort] = useState<SortState>({ key: 'title', dir: 'asc' });

  const { data, loading, error } = useQuery<OffersData, OffersVars>(OFFERS, { variables: vars });

  const apply = () =>
    setVars({
      query: form.query || undefined,
      location: form.location || undefined,
      source: form.source || undefined,
      seniority: form.seniority || undefined,
      page: 0,
      size: PAGE_SIZE,
    });

  const clear = () => {
    setForm(EMPTY_FORM);
    setVars({ page: 0, size: PAGE_SIZE });
  };

  const onSort = (key: SortKey) =>
    setSort((s) => (s.key === key ? { key, dir: s.dir === 'asc' ? 'desc' : 'asc' } : { key, dir: 'asc' }));

  const page = vars.page ?? 0;
  const offers = data?.offers ?? [];

  return (
    <>
      <div style={styles.main}>
        <FilterPanel form={form} onChange={setForm} onApply={apply} onClear={clear} />
        <ResultsTable
          offers={offers}
          loading={loading}
          error={error?.message}
          sort={sort}
          onSort={onSort}
          selectedId={offerId}
          onSelect={(id) => navigate(`/offers/${id}`)}
          page={page}
          pageSize={PAGE_SIZE}
          onPrev={() => setVars((v) => ({ ...v, page: Math.max(0, (v.page ?? 0) - 1) }))}
          onNext={() => setVars((v) => ({ ...v, page: (v.page ?? 0) + 1 }))}
        />
      </div>
      <DetailSidebar />
    </>
  );
}

const styles: Record<string, React.CSSProperties> = {
  main: {
    flex: '1 1 auto',
    display: 'flex',
    minHeight: 0,
    background: theme.bg,
  },
};
