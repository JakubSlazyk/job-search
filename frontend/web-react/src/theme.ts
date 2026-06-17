// Matrix / phosphor terminal theme tokens, ported from the northpost prototype. Centralised here
// so components share one palette. Accent is a constant for now; a future settings slice could make
// it dynamic again (the prototype themed the whole UI off `accentColor`).
export const theme = {
  // backgrounds
  bg: '#0b0e0a',
  bgPanel: '#0e120b',
  bgPanelAlt: '#0c100a',
  bgLogo: '#16200f',

  // borders (dark → light)
  border: '#141c0f',
  borderDim: '#1c2614',
  borderMid: '#243018',
  borderBright: '#2e3d22',
  borderAccent: '#3a5a2c',

  // text (dim → bright)
  textDim: '#3d4a31',
  textMuted: '#5a6b4d',
  textSubtle: '#7e9670',
  textSoft: '#9bbf8f',
  text: '#cfe3c7',
  textBright: '#d6e9cf',

  // accent (phosphor green)
  accent: '#5fd35f',
  accentBright: '#7ee787',
  accentGlow: 'rgba(126,231,135,0.45)',

  // misc
  danger: '#d36b5f',

  font: "'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, monospace",
} as const;

// A faint accent-tinted fill (used for selected rows, active chips). Mirrors the prototype's
// inline `color-mix(in srgb, accent 10%, bg)`.
export function accentFill(pct: number): string {
  return `color-mix(in srgb, ${theme.accent} ${pct}%, ${theme.bg})`;
}
