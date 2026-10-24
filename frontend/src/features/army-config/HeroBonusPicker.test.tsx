import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import HeroBonusPicker from './HeroBonusPicker';
import type { HeroBuild } from './heroBuild';

const BUILD: HeroBuild = {
  level: 1,
  attack: 0,
  defense: 0,
  power: 0,
  knowledge: 0,
  spells: [],
  skills: {},
  luck: 0,
  morale: 0,
  warMachines: [],
};

describe('HeroBonusPicker', () => {
  it('offers luck 0..+3 only, because SoD knows no negative luck', () => {
    render(<HeroBonusPicker value={BUILD} startSkills={{}} onChange={vi.fn()} />);

    const luck = screen.getByRole('combobox', { name: 'Gesammeltes Glück' });
    expect(Array.from((luck as HTMLSelectElement).options).map((o) => o.value)).toEqual([
      '0',
      '1',
      '2',
      '3',
    ]);
  });

  it('reports the changed value only', async () => {
    const onChange = vi.fn();
    render(<HeroBonusPicker value={{ ...BUILD, luck: 1 }} startSkills={{}} onChange={onChange} />);

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Gesammelte Moral' }), '-2');

    expect(onChange).toHaveBeenCalledWith({ morale: -2 });
  });

  it('keeps the hero level within 1..99', () => {
    const onChange = vi.fn();
    render(<HeroBonusPicker value={BUILD} startSkills={{}} onChange={onChange} />);

    fireEvent.change(screen.getByRole('spinbutton', { name: /Level/ }), {
      target: { value: '150' },
    });

    expect(onChange).toHaveBeenCalledWith({ level: 99 });
  });

  it('never offers a skill level below the hero start skill', () => {
    render(
      <HeroBonusPicker value={BUILD} startSkills={{ OFFENSE: 'ADVANCED' }} onChange={vi.fn()} />,
    );

    const offense = screen.getByRole('combobox', { name: 'Offense' }) as HTMLSelectElement;
    expect(offense.value).toBe('ADVANCED');
    expect(Array.from(offense.options).map((o) => o.value)).toEqual(['ADVANCED', 'EXPERT']);
  });

  it('reports a learned skill together with the ones learned before', async () => {
    const onChange = vi.fn();
    render(
      <HeroBonusPicker
        value={{ ...BUILD, skills: { LUCK: 'BASIC' } }}
        startSkills={{}}
        onChange={onChange}
      />,
    );

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Archery' }), 'EXPERT');

    expect(onChange).toHaveBeenCalledWith({ skills: { LUCK: 'BASIC', ARCHERY: 'EXPERT' } });
  });

  it('toggles war machines', async () => {
    const onChange = vi.fn();
    render(
      <HeroBonusPicker
        value={{ ...BUILD, warMachines: ['AMMO_CART'] }}
        startSkills={{}}
        onChange={onChange}
      />,
    );

    await userEvent.click(screen.getByRole('checkbox', { name: 'Ballista' }));
    expect(onChange).toHaveBeenCalledWith({ warMachines: ['AMMO_CART', 'BALLISTA'] });

    await userEvent.click(screen.getByRole('checkbox', { name: 'Munitionswagen' }));
    expect(onChange).toHaveBeenCalledWith({ warMachines: [] });
  });

  it('opens spell levels with Wisdom and shows spell power and mana', () => {
    render(
      <HeroBonusPicker
        value={{ ...BUILD, knowledge: 1, skills: { INTELLIGENCE: 'BASIC' } }}
        startSkills={{ WISDOM: 'BASIC' }}
        basePower={2}
        baseKnowledge={3}
        onChange={vi.fn()}
      />,
    );

    expect(screen.getByRole('checkbox', { name: 'Fireball' })).toBeEnabled();
    expect(screen.getByRole('checkbox', { name: 'Chain Lightning' })).toBeDisabled();
    // (3 + 1) Knowledge × 10, Basic Intelligence +25 %.
    expect(screen.getByText(/Spell Power 2, 50 Mana/)).toBeInTheDocument();
  });

  it('drops spells the hero can no longer learn when Wisdom is lowered', async () => {
    const onChange = vi.fn();
    render(
      <HeroBonusPicker
        value={{
          ...BUILD,
          skills: { WISDOM: 'ADVANCED' },
          spells: ['MAGIC_ARROW', 'FIREBALL', 'PRAYER'],
        }}
        startSkills={{}}
        onChange={onChange}
      />,
    );

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Wisdom' }), 'BASIC');

    expect(onChange).toHaveBeenCalledWith({
      skills: { WISDOM: 'BASIC' },
      spells: ['MAGIC_ARROW', 'FIREBALL'],
    });
  });

  it('toggles spells in the spellbook', async () => {
    const onChange = vi.fn();
    render(<HeroBonusPicker value={BUILD} startSkills={{}} onChange={onChange} />);

    await userEvent.click(screen.getByRole('checkbox', { name: 'Magic Arrow' }));

    expect(onChange).toHaveBeenCalledWith({ spells: ['MAGIC_ARROW'] });
  });

  it('keeps the starting spell of a specialist in the book, even above the Wisdom gate', () => {
    render(
      <HeroBonusPicker
        value={BUILD}
        startSkills={{}}
        startSpells={['RESURRECTION']}
        onChange={vi.fn()}
      />,
    );

    const resurrection = screen.getByRole('checkbox', { name: /Resurrection/ });
    expect(resurrection).toBeChecked();
    expect(resurrection).toBeDisabled();
    expect(screen.getByRole('checkbox', { name: 'Prayer' })).toBeDisabled();
  });
});
