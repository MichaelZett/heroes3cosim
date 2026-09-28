// Pointy-top axiale Hex-Geometrie. q läuft horizontal, r diagonal nach unten-rechts.
// Quelle: https://www.redblobgames.com/grids/hexagons/#hex-to-pixel

export const HEX_SIZE = 22;

export interface Pixel {
  x: number;
  y: number;
}

export function hexToPixel(q: number, r: number): Pixel {
  const x = HEX_SIZE * Math.sqrt(3) * (q + r / 2);
  const y = HEX_SIZE * 1.5 * r;
  return { x, y };
}

export function gridDimensions(width: number, height: number): { w: number; h: number } {
  // Letzte Zelle bei (width-1, height-1) — Pixel-Position berechnen und etwas Padding addieren.
  const last = hexToPixel(width - 1, height - 1);
  return {
    w: last.x + HEX_SIZE * 2,
    h: last.y + HEX_SIZE * 2,
  };
}
