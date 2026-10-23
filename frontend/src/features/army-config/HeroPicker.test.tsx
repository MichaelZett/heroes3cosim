import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import HeroPicker from './HeroPicker';
import { TEST_HEROES } from '../../test/fixtures';

describe('HeroPicker', () => {
  it('offers every hero plus the default-hero option', () => {
    render(<HeroPicker heroes={TEST_HEROES} selectedName={null} onChange={vi.fn()} />);

    const options = screen.getAllByRole('option');
    expect(options).toHaveLength(TEST_HEROES.length + 1);
    expect(options[0]).toHaveValue('');
  });

  it('groups the heroes by faction and names their class', () => {
    render(<HeroPicker heroes={TEST_HEROES} selectedName={null} onChange={vi.fn()} />);

    expect(screen.getByRole('group', { name: 'Stronghold' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /Crag Hack \(Barbarian\)/ })).toBeInTheDocument();
  });

  it('reports the selected hero by name', async () => {
    const onChange = vi.fn();
    render(<HeroPicker heroes={TEST_HEROES} selectedName={null} onChange={onChange} />);

    await userEvent.selectOptions(screen.getByRole('combobox'), 'Tazar');

    expect(onChange).toHaveBeenCalledWith('Tazar');
  });

  it('reports null when switching back to the default hero', async () => {
    const onChange = vi.fn();
    render(<HeroPicker heroes={TEST_HEROES} selectedName="Tazar" onChange={onChange} />);

    await userEvent.selectOptions(screen.getByRole('combobox'), '');

    expect(onChange).toHaveBeenCalledWith(null);
  });

  it('shows the effect of the selected hero on the whole army', () => {
    render(<HeroPicker heroes={TEST_HEROES} selectedName="Crag Hack" onChange={vi.fn()} />);

    // Crag Hack: Attack 4, Defense 0 - der Hinweis nennt beide Werte.
    expect(screen.getByText(/\+4/)).toBeInTheDocument();
  });

  it('stays silent while no hero is chosen', () => {
    render(<HeroPicker heroes={TEST_HEROES} selectedName={null} onChange={vi.fn()} />);

    expect(screen.queryByText(/\+4/)).not.toBeInTheDocument();
  });

  it('lists the skills that actually affect combat, with their percentage', () => {
    // Crag Hack hat Advanced Offense -> +20 % (Manual S. 38).
    render(<HeroPicker heroes={TEST_HEROES} selectedName="Crag Hack" onChange={vi.fn()} />);

    expect(screen.getByText(/\+20%/)).toBeInTheDocument();
  });

  it('shows Armorer as a reduction, not as a bonus', () => {
    // Tazar hat Advanced Armorer -> -10 % erlittener Schaden (Manual S. 35).
    render(<HeroPicker heroes={TEST_HEROES} selectedName="Tazar" onChange={vi.fn()} />);

    expect(screen.getByText(/-10%/)).toBeInTheDocument();
  });

  it('does not advertise skills the engine ignores', () => {
    // Scholar wirkt ausserhalb des Kampfes, Tactics braucht eine freie Aufstellung - beide
    // duerfen nicht als Kampfwirkung erscheinen.
    const scholar = {
      ...TEST_HEROES[0],
      name: 'Neela',
      skills: { SCHOLAR: 'BASIC', TACTICS: 'EXPERT' } as const,
    };
    render(<HeroPicker heroes={[scholar]} selectedName="Neela" onChange={vi.fn()} />);

    expect(screen.queryByText(/wirkt im Kampf|effective in combat/)).not.toBeInTheDocument();
  });

  it('shows Leadership as morale points, not as a percentage', () => {
    // Sorsha hat Basic Leadership -> +1 Moral (Manual S. 37). Die Zeile steht getrennt von
    // den Prozent-Fertigkeiten, weil Moral keine Schadensgroesse ist.
    render(<HeroPicker heroes={TEST_HEROES} selectedName="Sorsha" onChange={vi.fn()} />);

    expect(screen.getByText(/\+1 Moral|\+1 morale/)).toBeInTheDocument();
  });

  it('shows the Luck skill as luck points for the army', () => {
    const melodia = {
      ...TEST_HEROES[0],
      name: 'Melodia',
      skills: { WISDOM: 'BASIC', LUCK: 'BASIC' } as const,
    };
    render(<HeroPicker heroes={[melodia]} selectedName="Melodia" onChange={vi.fn()} />);

    expect(screen.getByText(/\+1 Glück|\+1 luck/)).toBeInTheDocument();
  });

  it('describes a creature specialty with its creature and level span', () => {
    render(<HeroPicker heroes={TEST_HEROES} selectedName="Sorsha" onChange={vi.fn()} />);

    expect(screen.getByText(/Spezialität: Swordsman/)).toBeInTheDocument();
    expect(screen.getByText(/je angefangene 4 Level/)).toBeInTheDocument();
  });

  it('marks a specialty without combat effect as such', () => {
    const jenova = {
      ...TEST_HEROES[0],
      name: 'Jenova',
      specialty: {
        ...TEST_HEROES[0].specialty,
        kind: 'OTHER',
        description: '+350 gold per day',
        effective: false,
      },
    } as const;
    render(<HeroPicker heroes={[jenova]} selectedName="Jenova" onChange={vi.fn()} />);

    expect(screen.getByText(/\+350 gold per day — ohne Kampfwirkung/)).toBeInTheDocument();
  });

  it('shows the Tactics zone of the hero', () => {
    const tyris = { ...TEST_HEROES[0], name: 'Tyris', skills: { TACTICS: 'BASIC' } as const };
    render(<HeroPicker heroes={[tyris]} selectedName="Tyris" onChange={vi.fn()} />);

    expect(screen.getByText(/ersten 3 Spalten/)).toBeInTheDocument();
  });

  it('stays silent about morale when the hero has no Leadership', () => {
    render(<HeroPicker heroes={TEST_HEROES} selectedName="Crag Hack" onChange={vi.fn()} />);

    expect(screen.queryByText(/Moral|morale/)).not.toBeInTheDocument();
  });
});
