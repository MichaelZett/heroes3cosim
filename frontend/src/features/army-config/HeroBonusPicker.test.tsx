import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import HeroBonusPicker from './HeroBonusPicker';

describe('HeroBonusPicker', () => {
  it('offers luck 0..+3 only, because SoD knows no negative luck', () => {
    render(<HeroBonusPicker luck={0} morale={0} onChange={vi.fn()} />);

    const luck = screen.getByRole('combobox', { name: /Glück|luck/i });
    expect(Array.from((luck as HTMLSelectElement).options).map((o) => o.value)).toEqual([
      '0',
      '1',
      '2',
      '3',
    ]);
  });

  it('reports both values when one of them changes', async () => {
    const onChange = vi.fn();
    render(<HeroBonusPicker luck={1} morale={0} onChange={onChange} />);

    await userEvent.selectOptions(screen.getByRole('combobox', { name: /Moral|morale/i }), '-2');

    expect(onChange).toHaveBeenCalledWith(1, -2);
  });
});
