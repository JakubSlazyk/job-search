import { useEffect, useMemo, useState } from 'react';
import { type Offer } from './queries';
import { initials, seniorityLabel, SPIN } from '../utils/format';
import { accentFill, theme } from '../theme';
import MatrixRain from '../components/MatrixRain';

export type SortKey = 'title' | 'company' | 'location' | 'seniority' | 'source';
export interface SortState {
  key: SortKey;
  dir: 'asc' | 'desc';
}

interface ResultsTableProps {
  offers: Offer[];
  loading: boolean;
  error?: string;
  sort: SortState;
  onSort: (key: SortKey) => void;
  selectedId?: string;
  onSelect: (offerId: string) => void;
  page: number;
  pageSize: number;
  onPrev: () => void;
  onNext: () => void;
}

const COLS = '2.4fr 1.5fr 1.5fr 1fr 0.9fr';

const HEADERS: { key: SortKey; label: string }[] = [
  { key: 'title', label: 'role' },
  { key: 'company', label: 'company' },
  { key: 'location', label: 'location' },
  { key: 'seniority', label: 'seniority' },
  { key: 'source', label: 'source' },
];

// Right-hand results pane: themed status line, sortable header row, and the offer rows. Sorting is
// client-side over the current page (the offers API has no sort argument). The Matrix rain fills the
// body while a query is in flight.
export default function ResultsTable({
  offers,
  loading,
  error,
  sort,
  onSort,
  selectedId,
  onSelect,
  page,
  pageSize,
  onPrev,
  onNext,
}: ResultsTableProps) {
  const [spin, setSpin] = useState(0);

  // Cycle the braille spinner only while loading.
  useEffect(() => {
    if (!loading) return undefined;
    const t = setInterval(() => setSpin((s) => (s + 1) % SPIN.length), 80);
    return () => clearInterval(t);
  }, [loading]);

  const sorted = useMemo(() => {
    const dir = sort.dir === 'asc' ? 1 : -1;
    return [...offers].sort((a, b) =>
      String(a[sort.key] ?? '').localeCompare(String(b[sort.key] ?? '')) * dir,
    );
  }, [offers, sort]);

  const arrow = (key: SortKey) => (sort.key !== key ? '·' : sort.dir === 'asc' ? '▲' : '▼');

  return (
    <section style={styles.section}>
      <div style={styles.subHeader}>
        <div>
          <h1 style={styles.h1}>open_roles</h1>
          {loading ? (
            <div style={styles.statusLoading}>
              <span style={styles.spinner}>{SPIN[spin]}</span>
              <span>querying database</span>
              <span style={styles.bar}>
                <span style={styles.barFill} />
              </span>
            </div>
          ) : error ? (
            <div style={styles.statusError} role="alert">
              ! query failed: {error}
            </div>
          ) : (
            <div style={styles.statusReady}>
              &gt; <span style={{ color: theme.text }}>{offers.length}</span>{' '}
              {offers.length === 1 ? 'role' : 'roles'} on this page
            </div>
          )}
        </div>
        <div style={styles.pager}>
          <button type="button" onClick={onPrev} disabled={page === 0} style={styles.pagerBtn}>
            [prev]
          </button>
          <span style={styles.pageNum}>pg {page + 1}</span>
          <button
            type="button"
            onClick={onNext}
            disabled={offers.length < pageSize}
            style={styles.pagerBtn}
          >
            [next]
          </button>
        </div>
      </div>

      <div style={styles.scroll}>
        <div style={{ ...styles.headGrid, gridTemplateColumns: COLS }}>
          {HEADERS.map((h) => (
            <div key={h.key} onClick={() => onSort(h.key)} style={styles.headCell}>
              {h.label} <span style={{ color: theme.accent }}>{arrow(h.key)}</span>
            </div>
          ))}
        </div>

        {loading && <MatrixRain />}

        {!loading &&
          !error &&
          sorted.map((o) => {
            const isSel = selectedId === o.offerId;
            return (
              <div
                key={o.offerId}
                onClick={() => onSelect(o.offerId)}
                style={{
                  ...styles.row,
                  gridTemplateColumns: COLS,
                  background: isSel ? accentFill(10) : 'transparent',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#11160d';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = isSel ? accentFill(10) : 'transparent';
                }}
              >
                <div style={styles.roleCell}>
                  <div style={styles.logo}>{initials(o.company)}</div>
                  <div style={{ minWidth: 0 }}>
                    <div style={styles.rowTitle}>{o.title}</div>
                    {o.employmentType && <div style={styles.rowSub}>{o.employmentType}</div>}
                  </div>
                </div>
                <div style={styles.company}>{o.company}</div>
                <div style={styles.location}>
                  <span style={styles.ellipsis}>{o.location || '—'}</span>
                  {o.remote && <span style={styles.rmt}>rmt</span>}
                </div>
                <div style={styles.seniority}>{seniorityLabel(o.seniority) || '—'}</div>
                <div style={styles.source}>{o.source}</div>
              </div>
            );
          })}

        {!loading && !error && offers.length === 0 && (
          <div style={styles.empty}>
            <div style={styles.emptyHead}>&gt; no_results</div>
            <div style={styles.emptySub}>widen your search or clear the filters</div>
          </div>
        )}
      </div>
    </section>
  );
}

const styles: Record<string, React.CSSProperties> = {
  section: {
    flex: '1 1 auto',
    display: 'flex',
    flexDirection: 'column',
    minWidth: 0,
    minHeight: 0,
  },
  subHeader: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 16,
    padding: '15px 22px',
    flex: '0 0 auto',
    borderBottom: `1px solid ${theme.borderDim}`,
  },
  h1: {
    margin: 0,
    fontSize: 16,
    fontWeight: 700,
    letterSpacing: '0.04em',
    textTransform: 'uppercase',
    color: theme.accentBright,
    textShadow: '0 0 8px rgba(126,231,135,0.3)',
  },
  statusLoading: {
    fontSize: 12,
    color: theme.accentBright,
    marginTop: 3,
    textTransform: 'uppercase',
    display: 'flex',
    alignItems: 'center',
    gap: 9,
  },
  spinner: {
    color: theme.accent,
    width: 12,
    display: 'inline-block',
    textShadow: '0 0 6px rgba(95,211,95,0.6)',
  },
  bar: {
    position: 'relative',
    width: 90,
    height: 8,
    background: theme.borderDim,
    overflow: 'hidden',
    display: 'inline-block',
  },
  barFill: {
    position: 'absolute',
    top: 0,
    left: 0,
    width: '25%',
    height: '100%',
    background: theme.accent,
    animation: 'scan 1s linear infinite',
  },
  statusReady: { fontSize: 12, color: theme.textMuted, marginTop: 3, textTransform: 'uppercase' },
  statusError: { fontSize: 12, color: theme.danger, marginTop: 3, textTransform: 'uppercase' },
  pager: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    fontSize: 11,
    textTransform: 'uppercase',
    color: theme.textMuted,
  },
  pagerBtn: {
    background: 'transparent',
    border: `1px solid ${theme.borderMid}`,
    padding: '7px 10px',
    fontFamily: 'inherit',
    fontSize: 11,
    textTransform: 'uppercase',
    color: theme.textSoft,
    cursor: 'pointer',
  },
  pageNum: { color: theme.text },
  scroll: { flex: '1 1 auto', overflow: 'auto', display: 'flex', flexDirection: 'column' },
  headGrid: {
    display: 'grid',
    alignItems: 'center',
    gap: 12,
    padding: '11px 22px',
    background: theme.bgPanel,
    borderBottom: `1px solid ${theme.borderMid}`,
    fontSize: 10,
    fontWeight: 700,
    textTransform: 'uppercase',
    letterSpacing: '0.06em',
    color: theme.textMuted,
    position: 'sticky',
    top: 0,
    zIndex: 1,
  },
  headCell: { cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 5 },
  row: {
    display: 'grid',
    alignItems: 'center',
    gap: 12,
    padding: '13px 22px',
    borderBottom: `1px solid ${theme.border}`,
    cursor: 'pointer',
  },
  roleCell: { display: 'flex', alignItems: 'center', gap: 11, minWidth: 0 },
  logo: {
    width: 36,
    height: 36,
    flex: '0 0 auto',
    background: theme.bgLogo,
    border: `1px solid ${theme.borderMid}`,
    color: theme.accentBright,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 700,
    fontSize: 12,
  },
  rowTitle: {
    fontSize: 13,
    fontWeight: 700,
    color: theme.textBright,
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
  rowSub: { fontSize: 11, color: theme.textMuted, marginTop: 2, textTransform: 'uppercase' },
  company: {
    fontSize: 12,
    color: theme.textSoft,
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
  location: {
    fontSize: 12,
    color: theme.textSubtle,
    display: 'flex',
    alignItems: 'center',
    gap: 6,
    minWidth: 0,
  },
  ellipsis: { whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' },
  rmt: {
    flex: '0 0 auto',
    fontSize: 9,
    fontWeight: 700,
    textTransform: 'uppercase',
    border: `1px solid ${theme.borderBright}`,
    padding: '2px 5px',
    color: theme.accent,
  },
  seniority: {
    fontSize: 11,
    color: theme.textSoft,
    textTransform: 'uppercase',
    whiteSpace: 'nowrap',
  },
  source: {
    fontSize: 11,
    color: theme.textMuted,
    textTransform: 'uppercase',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
  empty: { padding: '60px 24px', textAlign: 'center' },
  emptyHead: {
    fontSize: 13,
    fontWeight: 700,
    textTransform: 'uppercase',
    color: theme.textSoft,
  },
  emptySub: { fontSize: 12, color: theme.textMuted, marginTop: 8 },
};
