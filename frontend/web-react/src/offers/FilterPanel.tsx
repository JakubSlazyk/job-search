import { type FormEvent } from 'react';
import { theme } from '../theme';

export interface FilterForm {
  query: string;
  location: string;
  source: string;
  seniority: string;
}

interface FilterPanelProps {
  form: FilterForm;
  onChange: (next: FilterForm) => void;
  onApply: () => void;
  onClear: () => void;
}

// Left sidebar of real, server-honoured filters (full-text query + exact-match source/location/
// seniority). Submitting applies them as GraphQL variables; the prototype's salary/type/skill
// facets are intentionally absent because the offers API can't filter on them yet.
export default function FilterPanel({ form, onChange, onApply, onClear }: FilterPanelProps) {
  const submit = (e: FormEvent) => {
    e.preventDefault();
    onApply();
  };

  const field = (key: keyof FilterForm, value: string) => onChange({ ...form, [key]: value });

  return (
    <aside style={styles.aside}>
      <form onSubmit={submit} aria-label="Search offers">
        <div style={styles.headRow}>
          <span style={styles.sectionLabel}>// filters</span>
          <button type="button" onClick={onClear} style={styles.clearBtn}>
            [clear]
          </button>
        </div>

        <div style={styles.group}>
          <label style={styles.fieldLabel} htmlFor="f-query">
            grep keyword
          </label>
          <input
            id="f-query"
            aria-label="Search"
            value={form.query}
            onChange={(e) => field('query', e.target.value)}
            placeholder="title / company / description"
            style={styles.input}
          />
        </div>

        <div style={styles.group}>
          <label style={styles.fieldLabel} htmlFor="f-location">
            location
          </label>
          <input
            id="f-location"
            aria-label="Location"
            value={form.location}
            onChange={(e) => field('location', e.target.value)}
            placeholder="e.g. Remote, Berlin"
            style={styles.input}
          />
        </div>

        <div style={styles.group}>
          <label style={styles.fieldLabel} htmlFor="f-source">
            source
          </label>
          <input
            id="f-source"
            aria-label="Source"
            value={form.source}
            onChange={(e) => field('source', e.target.value)}
            placeholder="e.g. himalayas"
            style={styles.input}
          />
        </div>

        <div style={styles.group}>
          <label style={styles.fieldLabel} htmlFor="f-seniority">
            seniority
          </label>
          <input
            id="f-seniority"
            aria-label="Seniority"
            value={form.seniority}
            onChange={(e) => field('seniority', e.target.value)}
            placeholder="e.g. SENIOR"
            style={styles.input}
          />
        </div>

        <button type="submit" style={styles.runBtn}>
          &gt; run query
        </button>
      </form>
    </aside>
  );
}

const styles: Record<string, React.CSSProperties> = {
  aside: {
    width: 284,
    flex: '0 0 284px',
    background: theme.bgPanel,
    borderRight: `1px solid ${theme.borderMid}`,
    overflowY: 'auto',
    padding: 18,
  },
  headRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  sectionLabel: {
    fontSize: 12,
    fontWeight: 700,
    letterSpacing: '0.08em',
    textTransform: 'uppercase',
    color: theme.textMuted,
  },
  clearBtn: {
    background: 'transparent',
    border: 'none',
    fontSize: 11,
    fontWeight: 700,
    textTransform: 'uppercase',
    cursor: 'pointer',
    padding: 0,
    fontFamily: 'inherit',
    color: theme.accent,
  },
  group: { marginBottom: 18 },
  fieldLabel: {
    display: 'block',
    fontSize: 11,
    fontWeight: 700,
    textTransform: 'uppercase',
    color: theme.textSubtle,
    marginBottom: 6,
  },
  input: {
    width: '100%',
    padding: '9px 11px',
    background: theme.bg,
    border: `1px solid ${theme.borderMid}`,
    color: theme.textBright,
    fontFamily: 'inherit',
    fontSize: 12,
    outline: 'none',
    caretColor: theme.accentBright,
  },
  runBtn: {
    width: '100%',
    marginTop: 4,
    padding: '11px 12px',
    background: theme.accent,
    border: `1px solid ${theme.accent}`,
    color: theme.bg,
    fontFamily: 'inherit',
    fontSize: 12,
    fontWeight: 700,
    textTransform: 'uppercase',
    letterSpacing: '0.04em',
    cursor: 'pointer',
  },
};
