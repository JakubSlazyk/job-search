// Presentational helpers, ported/typed from the northpost prototype.

// Two-letter monogram for a company logo tile.
export function initials(name: string): string {
  return name
    .split(' ')
    .slice(0, 2)
    .map((w) => w[0] ?? '')
    .join('')
    .toUpperCase();
}

// Seniority comes from the backend as a free-ish string (e.g. "SENIOR", "Mid"); present it
// consistently without assuming a fixed vocabulary.
export function seniorityLabel(seniority: string): string {
  const s = seniority.trim();
  if (!s) return '';
  return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase();
}

// Braille spinner frames for the "querying database" status line.
export const SPIN = ['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'] as const;
