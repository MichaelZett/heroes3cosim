import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import HeroSummary from './HeroSummary';
import type { HeroDto } from '../../shared/api/types';

const HERO: HeroDto = {
  name: 'Solmyr',
  heroClass: 'WIZARD',
  faction: 'TOWER',
  attack: 1,
  defense: 2,
  power: 7,
  knowledge: 9,
  skills: { WISDOM: 'EXPERT', SORCERY: 'ADVANCED' },
  specialty: {
    kind: 'SPELL',
    creatureTier: 0,
    attack: 0,
    defense: 0,
    damage: 0,
    description: 'CHAIN_LIGHTNING',
    effective: true,
  },
  level: 15,
  spells: ['CHAIN_LIGHTNING', 'SUMMON_BOAT'],
  artifacts: ['Orb of the Firmament', 'Ring of the Magi'],
};

describe('HeroSummary', () => {
  it('shows level, primary skills, skills and the whole spellbook', () => {
    render(<HeroSummary hero={HERO} />);

    expect(screen.getByText('Solmyr, Level 15 — 1/2/7/9')).toBeInTheDocument();
    expect(screen.getByText(/Wisdom Expert, Sorcery Advanced/)).toBeInTheDocument();
    expect(screen.getByText(/Chain Lightning, Summon Boat/)).toBeInTheDocument();
    expect(
      screen.getByText('Artefakte: Orb of the Firmament, Ring of the Magi'),
    ).toBeInTheDocument();
  });
});
