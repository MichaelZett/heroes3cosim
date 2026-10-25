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

  it('hides a destroyed war machine', () => {
    const { container } = renderToken({ ...base, slot: 21, unitName: 'Ammo Cart', count: 0 });
    expect(container.querySelector('g')).toBeNull();
  });
});
