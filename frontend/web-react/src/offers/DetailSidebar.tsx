import { useQuery } from '@apollo/client/react';
import { useNavigate, useParams } from 'react-router-dom';
import DOMPurify from 'dompurify';
import { OFFER, type Offer } from './queries';
import { initials, seniorityLabel } from '../utils/format';
import { theme } from '../theme';

// Scraped descriptions can be HTML (e.g. Himalayas). Force external links opened from a description
// to open safely in a new tab — runs once at module load.
DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  if (node.tagName === 'A' && node.hasAttribute('href')) {
    node.setAttribute('target', '_blank');
    node.setAttribute('rel', 'noopener noreferrer');
  }
});

interface OfferData {
  offer: Offer | null;
}

// Route-driven slide-over (open at /offers/:offerId, closed at /). Fetches the full offer and shows
// the rich terminal detail layout. Enrichment sections (salary, stack, responsibilities, …) render
// only when the backend supplies them, so the panel degrades cleanly on today's lean schema.
export default function DetailSidebar() {
  const { offerId } = useParams();
  const navigate = useNavigate();
  const open = Boolean(offerId);

  const { data } = useQuery<OfferData, { offerId: string }>(OFFER, {
    variables: { offerId: offerId ?? '' },
    skip: !offerId,
  });

  const offer = data?.offer ?? null;
  const close = () => navigate('/');

  const salary =
    offer?.salaryMin != null && offer?.salaryMax != null
      ? `$${offer.salaryMin}-${offer.salaryMax}k`
      : null;

  return (
    <>
      <div
        onClick={close}
        style={{
          ...styles.scrim,
          opacity: open ? 1 : 0,
          pointerEvents: open ? 'auto' : 'none',
        }}
      />

      <aside
        style={{ ...styles.panel, transform: open ? 'translateX(0)' : 'translateX(101%)' }}
        aria-hidden={!open}
      >
        {offer && (
          <>
            <div style={styles.head}>
              <div style={styles.headTop}>
                <div style={styles.headId}>
                  <div style={styles.logo}>{initials(offer.company)}</div>
                  <div style={{ minWidth: 0 }}>
                    <div style={styles.companyKicker}>{offer.company}</div>
                    <h2 style={styles.title}>{offer.title}</h2>
                  </div>
                </div>
                <button type="button" onClick={close} style={styles.closeBtn} aria-label="Close">
                  [x]
                </button>
              </div>
              <div style={styles.badges}>
                {offer.location && <span style={styles.metaBadge}>⌖ {offer.location}</span>}
                {offer.seniority && (
                  <span style={styles.metaBadge}>{seniorityLabel(offer.seniority)}</span>
                )}
                {offer.employmentType && <span style={styles.metaBadge}>{offer.employmentType}</span>}
                <span style={styles.sourceBadge}>{offer.source}</span>
                {salary && <span style={styles.salaryBadge}>{salary}</span>}
              </div>
            </div>

            <div style={styles.body}>
              {offer.description?.trim() ? (
                <div
                  className="offer-description"
                  // Sanitised with DOMPurify before injection — scraped HTML is untrusted.
                  dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(offer.description) }}
                />
              ) : (
                <div style={styles.descEmpty}>// no description provided by this source</div>
              )}

              {offer.responsibilities?.length ? (
                <>
                  <div style={styles.sectionLabel}>// what_you_do</div>
                  <ul style={styles.list}>
                    {offer.responsibilities.map((r) => (
                      <li key={r} style={styles.li}>
                        <span style={styles.bullet}>&gt;</span>
                        <span>{r}</span>
                      </li>
                    ))}
                  </ul>
                </>
              ) : null}

              {offer.requirements?.length ? (
                <>
                  <div style={styles.sectionLabel}>// requirements</div>
                  <ul style={styles.list}>
                    {offer.requirements.map((r) => (
                      <li key={r} style={styles.li}>
                        <span style={styles.bullet}>&gt;</span>
                        <span>{r}</span>
                      </li>
                    ))}
                  </ul>
                </>
              ) : null}

              {offer.skills?.length ? (
                <>
                  <div style={styles.sectionLabel}>// stack</div>
                  <div style={styles.chips}>
                    {offer.skills.map((s) => (
                      <span key={s} style={styles.skill}>
                        {s}
                      </span>
                    ))}
                  </div>
                </>
              ) : null}

              {offer.companyBlurb && (
                <div style={styles.about}>
                  <div style={styles.aboutLabel}>// about {offer.company}</div>
                  <div style={styles.aboutText}>{offer.companyBlurb}</div>
                </div>
              )}
            </div>

            <div style={styles.footer}>
              {offer.url ? (
                <a
                  href={offer.url}
                  target="_blank"
                  rel="noreferrer"
                  style={styles.applyBtn}
                >
                  &gt; view original posting
                </a>
              ) : (
                <span style={{ ...styles.applyBtn, opacity: 0.5, cursor: 'default' }}>
                  no link available
                </span>
              )}
            </div>
          </>
        )}
      </aside>
    </>
  );
}

const styles: Record<string, React.CSSProperties> = {
  scrim: {
    position: 'fixed',
    inset: 0,
    background: 'rgba(0,0,0,0.55)',
    transition: 'opacity .26s ease',
    zIndex: 40,
  },
  panel: {
    position: 'fixed',
    top: 0,
    right: 0,
    bottom: 0,
    width: 480,
    maxWidth: '92vw',
    background: theme.bgPanelAlt,
    borderLeft: `1px solid ${theme.borderBright}`,
    boxShadow: '-12px 0 50px rgba(0,0,0,0.6), -1px 0 30px rgba(95,211,95,0.06)',
    transition: 'transform .3s cubic-bezier(.4,0,.2,1)',
    zIndex: 50,
    display: 'flex',
    flexDirection: 'column',
  },
  head: {
    flex: '0 0 auto',
    padding: '20px 24px',
    borderBottom: `1px solid ${theme.borderDim}`,
    background: theme.bgPanel,
  },
  headTop: {
    display: 'flex',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 14,
  },
  headId: { display: 'flex', alignItems: 'center', gap: 13, minWidth: 0 },
  logo: {
    width: 50,
    height: 50,
    flex: '0 0 auto',
    background: theme.bgLogo,
    border: `1px solid ${theme.borderBright}`,
    color: theme.accentBright,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 700,
    fontSize: 17,
  },
  companyKicker: { fontSize: 11, color: theme.textMuted, textTransform: 'uppercase' },
  title: {
    margin: '3px 0 0',
    fontSize: 18,
    fontWeight: 700,
    color: theme.textBright,
    lineHeight: 1.2,
    textShadow: '0 0 8px rgba(126,231,135,0.18)',
  },
  closeBtn: {
    flex: '0 0 auto',
    width: 32,
    height: 32,
    border: `1px solid ${theme.borderBright}`,
    background: 'transparent',
    color: theme.textSoft,
    fontSize: 14,
    cursor: 'pointer',
    fontFamily: 'inherit',
  },
  badges: { display: 'flex', flexWrap: 'wrap', gap: 7, marginTop: 15 },
  metaBadge: {
    fontSize: 11,
    textTransform: 'uppercase',
    color: theme.textSoft,
    background: theme.bgLogo,
    padding: '5px 10px',
  },
  sourceBadge: {
    fontSize: 11,
    textTransform: 'uppercase',
    color: theme.textMuted,
    border: `1px solid ${theme.borderBright}`,
    padding: '5px 10px',
  },
  salaryBadge: {
    fontSize: 11,
    fontWeight: 700,
    color: theme.bg,
    background: theme.accent,
    padding: '5px 10px',
  },
  body: { flex: '1 1 auto', overflowY: 'auto', padding: '22px 24px' },
  descEmpty: {
    fontSize: 12,
    color: theme.textMuted,
    textTransform: 'uppercase',
    letterSpacing: '0.04em',
  },
  sectionLabel: {
    fontSize: 11,
    fontWeight: 700,
    textTransform: 'uppercase',
    letterSpacing: '0.06em',
    color: theme.textMuted,
    margin: '24px 0 11px',
  },
  list: { margin: 0, padding: 0, listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 9 },
  li: { fontSize: 12.5, lineHeight: 1.55, color: '#bcd2b3', display: 'flex', gap: 9 },
  bullet: { color: theme.accent, flex: '0 0 auto' },
  chips: { display: 'flex', flexWrap: 'wrap', gap: 7 },
  skill: {
    fontSize: 11,
    color: theme.textSoft,
    border: `1px solid ${theme.borderBright}`,
    padding: '4px 10px',
  },
  about: { marginTop: 24, padding: 15, background: theme.bgPanel, border: `1px solid ${theme.borderDim}` },
  aboutLabel: {
    fontSize: 11,
    fontWeight: 700,
    textTransform: 'uppercase',
    color: theme.textSubtle,
    marginBottom: 6,
  },
  aboutText: { fontSize: 12.5, lineHeight: 1.6, color: '#8ba37f' },
  footer: {
    flex: '0 0 auto',
    padding: '15px 24px',
    borderTop: `1px solid ${theme.borderDim}`,
    display: 'flex',
    gap: 9,
    background: theme.bgPanel,
  },
  applyBtn: {
    flex: 1,
    textAlign: 'center',
    padding: 13,
    fontSize: 12,
    fontWeight: 700,
    textTransform: 'uppercase',
    letterSpacing: '0.04em',
    cursor: 'pointer',
    fontFamily: 'inherit',
    border: `1px solid ${theme.accent}`,
    background: theme.accent,
    color: theme.bg,
  },
};
