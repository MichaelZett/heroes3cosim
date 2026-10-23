import { describe, expect, it } from 'vitest';
import { fieldCells, gridDimensions, HEX_SIZE, hexToPixel } from './hex';

describe('hexToPixel', () => {
  it('puts (0,0) at the origin', () => {
    expect(hexToPixel(0, 0)).toEqual({ x: 0, y: 0 });
  });

  it('moves horizontally with q at r=0', () => {
    const a = hexToPixel(0, 0);
    const b = hexToPixel(1, 0);
    expect(b.x).toBeGreaterThan(a.x);
    expect(b.y).toBe(a.y);
  });

  it('moves vertically and slightly diagonally with r', () => {
    const a = hexToPixel(0, 0);
    const b = hexToPixel(0, 1);
    expect(b.y).toBeGreaterThan(a.y);
    // Pointy-top: jede zweite Reihe ist um eine halbe Breite verschoben.
    expect(b.x).toBeCloseTo(HEX_SIZE * Math.sqrt(3) * 0.5, 3);
  });
});

describe('fieldCells', () => {
  it('builds a rectangle with offset rows like the backend battlefield', () => {
    const cells = fieldCells(15, 11);
    // Fünf lange Reihen à 15 Zellen (Parität der Mittelreihe), sechs kurze à 14.
    expect(cells).toHaveLength(5 * 15 + 6 * 14);
    const centerRow = cells.filter((c) => c.r === 5).map((c) => c.q);
    expect(Math.min(...centerRow)).toBe(0);
    expect(Math.max(...centerRow)).toBe(14);
    // Jede Reihe liegt in derselben waagerechten Spanne — kein Parallelogramm.
    for (let r = 0; r < 11; r++) {
      const xs = cells.filter((c) => c.r === r).map((c) => hexToPixel(c.q, c.r).x);
      expect(Math.min(...xs)).toBeGreaterThanOrEqual(hexToPixel(0, 5).x - 1e-9);
      expect(Math.max(...xs)).toBeLessThanOrEqual(hexToPixel(14, 5).x + 1e-9);
    }
  });
});

describe('gridDimensions', () => {
  it('spans every cell of the field plus padding', () => {
    const { x0, w, h } = gridDimensions(15, 11);
    for (const { q, r } of fieldCells(15, 11)) {
      const { x, y } = hexToPixel(q, r);
      expect(x).toBeGreaterThanOrEqual(x0 - 1e-9);
      expect(x).toBeLessThanOrEqual(x0 + w);
      expect(y).toBeLessThanOrEqual(h);
    }
  });
});
