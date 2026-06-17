import { useEffect, useRef } from 'react';
import { theme } from '../theme';

const RAIN_CHARS =
  'アァカサタナハマヤラワガザダバパイィキシチニヒミリヰギジヂビピウゥクスツヌフムユルグズヅブプエェケセテネヘメレヱゲゼデベペオォコソトノホモヨロヲゴゾドボポ0123456789ABCDEFZ:.=*+-<>'.split(
    '',
  );

// Matrix digital rain drawn on a <canvas>, shown while a query is in flight. Ported from the
// northpost prototype; accent is the constant phosphor green.
export default function MatrixRain() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return undefined;

    const font = 14;
    let ctx: CanvasRenderingContext2D | null = null;
    let drops: number[] = [];
    let w = 0;
    let h = 0;

    const init = (): boolean => {
      const rect = canvas.getBoundingClientRect();
      if (rect.width < 2 || rect.height < 2) return false;
      w = rect.width;
      h = rect.height;
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      canvas.width = Math.floor(w * dpr);
      canvas.height = Math.floor(h * dpr);
      ctx = canvas.getContext('2d');
      if (!ctx) return false;
      ctx.scale(dpr, dpr);
      const cols = Math.floor(w / font);
      drops = Array.from({ length: cols }, () => Math.floor(Math.random() * (h / font)));
      return true;
    };

    const tick = () => {
      if (!ctx && !init()) return;
      if (!ctx) return;
      ctx.fillStyle = 'rgba(11,14,10,0.16)';
      ctx.fillRect(0, 0, w, h);
      ctx.font = `${font}px "JetBrains Mono", monospace`;
      ctx.textAlign = 'center';
      for (let i = 0; i < drops.length; i += 1) {
        const x = i * font + font / 2;
        const y = drops[i] * font;
        ctx.fillStyle = '#dfffcf'; // bright leading glyph
        ctx.fillText(RAIN_CHARS[Math.floor(Math.random() * RAIN_CHARS.length)], x, y);
        ctx.fillStyle = theme.accent; // trailing glyph in accent green
        ctx.fillText(RAIN_CHARS[Math.floor(Math.random() * RAIN_CHARS.length)], x, y - font);
        if (y > h && Math.random() > 0.975) drops[i] = Math.floor(Math.random() * -20);
        drops[i] += 1;
      }
    };

    const timer = setInterval(tick, 55);
    return () => clearInterval(timer);
  }, []);

  return (
    <div style={styles.wrap}>
      <canvas ref={canvasRef} style={{ display: 'block', width: '100%', height: '100%' }} />
      <div style={styles.vignette} />
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrap: {
    position: 'relative',
    width: '100%',
    flex: '1 1 auto',
    minHeight: 440,
    overflow: 'hidden',
    background: theme.bg,
  },
  vignette: {
    position: 'absolute',
    inset: 0,
    pointerEvents: 'none',
    background:
      'radial-gradient(ellipse at center, rgba(11,14,10,0) 35%, rgba(11,14,10,0.85) 100%)',
  },
};
