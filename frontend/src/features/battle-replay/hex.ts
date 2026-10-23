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

// Das Feld ist ein Rechteck mit versetzten Reihen (wie Battlefield im Backend): eine Zelle gehört
// dazu, wenn ihre waagerechte Lage q + r/2 innerhalb der Mittelreihe liegt,
// c <= 2q + r <= c + 2(width - 1) mit c = floor(height / 2).
export function fieldCells(width: number, height: number): { q: number; r: number }[] {
  const center = Math.floor(height / 2);
  const cells: { q: number; r: number }[] = [];
  for (let r = 0; r < height; r++) {
    const first = Math.ceil((center - r) / 2);
    const last = Math.floor((center + 2 * (width - 1) - r) / 2);
    for (let q = first; q <= last; q++) {
      cells.push({ q, r });
    }
  }
  return cells;
}

// Ausschnitt fürs SVG: x0 ist der linke Rand, die Zelle (0, c) der Mittelreihe — wegen x = q + r/2
// liegt sie c/2 Breiten rechts vom Ursprung. w/h sind die Ausdehnung samt Rand.
export function gridDimensions(
  width: number,
  height: number,
): { x0: number; w: number; h: number } {
  const center = Math.floor(height / 2);
  const left = hexToPixel(0, center).x;
  const right = hexToPixel(width - 1, center).x;
  const bottom = hexToPixel(0, height - 1).y;
  return {
    x0: left,
    w: right - left + HEX_SIZE * 2,
    h: bottom + HEX_SIZE * 2,
  };
}
