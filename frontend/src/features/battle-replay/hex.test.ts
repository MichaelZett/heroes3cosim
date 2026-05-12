import {describe, expect, it} from 'vitest';
import {gridDimensions, HEX_SIZE, hexToPixel} from './hex';

describe('hexToPixel', () => {
    it('puts (0,0) at the origin', () => {
        expect(hexToPixel(0, 0)).toEqual({x: 0, y: 0});
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

describe('gridDimensions', () => {
    it('returns enough room for the last cell plus padding', () => {
        const {w, h} = gridDimensions(15, 11);
        expect(w).toBeGreaterThan(0);
        expect(h).toBeGreaterThan(0);
        const last = hexToPixel(14, 10);
        expect(w).toBeGreaterThanOrEqual(last.x);
        expect(h).toBeGreaterThanOrEqual(last.y);
    });
});
