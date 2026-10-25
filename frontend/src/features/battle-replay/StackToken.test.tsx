import { describe, expect, it } from 'vitest';
import { render } from '@testing-library/react';
import StackToken from './StackToken';
import type { SideState } from './state';

const base: SideState = {
  side: 'DEFENDER',
  slot: 0,
  unitName: 'Pikeman',
  count: 12,
  topHp: 10,
  maxHp: 10,
  startCount: 12,
  q: 3,
  r: 5,
  doubleWide: false,
};

function renderToken(side: SideState) {
  return render(
    <svg>
      <StackToken side={side} color="blue" transitionMs={0} />
    </svg>,
  );
}

describe('StackToken', () => {
  it('shows a creature stack as a circle with its count', () => {
    const { container } = renderToken(base);
    expect(container.querySelector('circle')).not.toBeNull();
    expect(container.textContent).toContain('12');
  });

  it('shows a war machine as a square with its initial', () => {
    const { container } = renderToken({
      ...base,
      slot: 20,
      unitName: 'Ballista',
      count: 1,
      topHp: 250,
      maxHp: 250,
      startCount: 1,
    });
    expect(container.querySelector('circle')).toBeNull();
    expect(container.querySelector('rect')).not.toBeNull();
    expect(container.querySelector('title')?.textContent).toBe('Ballista');
    expect(container.textContent).toContain('B');
  });

  it('stretches a two-hex stack over the hex behind it, towards its own edge', () => {
    const { container } = renderToken({ ...base, unitName: 'Black Dragon', doubleWide: true });
    const token = container.querySelector('[data-testid="wide-token"]');
    expect(container.querySelector('circle')).toBeNull();
    expect(token).not.toBeNull();
    // Der Verteidiger reicht nach rechts: das Rechteck beginnt links vom Kopf und ist breiter als hoch.
    expect(Number(token?.getAttribute('width'))).toBeGreaterThan(
      Number(token?.getAttribute('height')),
    );
    expect(Number(token?.getAttribute('x'))).toBeLessThan(0);
    const attacker = renderToken({ ...base, side: 'ATTACKER', doubleWide: true });
    const attackerToken = attacker.container.querySelector('[data-testid="wide-token"]');
    expect(Number(attackerToken?.getAttribute('x'))).toBeLessThan(Number(token?.getAttribute('x')));
  });

  it('hides a destroyed war machine', () => {
    const { container } = renderToken({ ...base, slot: 21, unitName: 'Ammo Cart', count: 0 });
    expect(container.querySelector('g')).toBeNull();
  });
});
