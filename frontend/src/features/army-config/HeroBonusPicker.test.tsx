import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import HeroBonusPicker from './HeroBonusPicker';

const BUILD = { level: 1, luck: 0, morale: 0 };

describe('HeroBonusPicker', () => {
  it('offers luck 0..+3 only, because SoD knows no negative luck', () => {
    render(<HeroBonusPicker value={BUILD} onChange={vi.fn()} />);

    const luck = screen.getByRole('combobox', { name: /Glück|luck/i });
    expect(Array.from((luck as HTMLSelectElement).options).map((o) => o.value)).toEqual([
      '0',
      '1',
      '2',
      '3',
    ]);
  });

  it('reports the changed value only', async () => {
    const onChange = vi.fn();
    render(<HeroBonusPicker value={{ ...BUILD, luck: 1 }} onChange={onChange} />);

    await userEvent.selectOptions(screen.getByRole('combobox', { name: /Moral|morale/i }), '-2');

    expect(onChange).toHaveBeenCalledWith({ morale: -2 });
  });

  it('keeps the hero level within 1..99', () => {
    const onChange = vi.fn();
    render(<HeroBonusPicker value={BUILD} onChange={onChange} />);

    fireEvent.change(screen.getByRole('spinbutton', { name: /Level/ }), {
      target: { value: '150' },
    });

    expect(onChange).toHaveBeenCalledWith({ level: 99 });
  });
});
