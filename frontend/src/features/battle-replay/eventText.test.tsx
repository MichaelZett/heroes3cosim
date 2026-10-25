import { describe, expect, it } from 'vitest';
import { render } from '@testing-library/react';
import { EventText } from './eventText';
import type { BattleEvent } from '../../shared/api/types';

const NAMES = { attacker: 'Pikeman', defender: 'Goblin' };

function renderEvent(event: BattleEvent) {
  return render(<EventText event={event} names={NAMES} />);
}

describe('eventToNode', () => {
  it('renders BattleStart with both unit names', () => {
    const { getByText, container } = renderEvent({
      type: 'BattleStart',
      battlefieldWidth: 15,
      battlefieldHeight: 11,
      obstacles: [],
      attacker: {
        side: 'ATTACKER',
        slot: 0,
        unitName: 'Pikeman',
        count: 10,
        topHp: 10,
        q: 0,
        r: 5,
      },
      defender: { side: 'DEFENDER', slot: 0, unitName: 'Goblin', count: 8, topHp: 5, q: 14, r: 5 },
      stacks: [],
      attackerMorale: 0,
      defenderMorale: 0,
    });
    expect(getByText('Pikeman')).toBeInTheDocument();
    expect(getByText('Goblin')).toBeInTheDocument();
    expect(container.textContent).toContain('Kampf beginnt');
  });

  it('adds the morale rating of both armies when at least one side has one', () => {
    const { container } = renderEvent({
      type: 'BattleStart',
      battlefieldWidth: 15,
      battlefieldHeight: 11,
      obstacles: [],
      attacker: {
        side: 'ATTACKER',
        slot: 0,
        unitName: 'Pikeman',
        count: 10,
        topHp: 10,
        q: 0,
        r: 5,
      },
      defender: { side: 'DEFENDER', slot: 0, unitName: 'Goblin', count: 8, topHp: 5, q: 14, r: 5 },
      stacks: [],
      attackerMorale: 2,
      defenderMorale: -1,
    });
    // Das Vorzeichen ist die Information - +2 wirkt anders als -1.
    expect(container.textContent).toContain('+2');
    expect(container.textContent).toContain('-1');
  });

  it('stays silent about morale when neither side has any', () => {
    const { container } = renderEvent({
      type: 'BattleStart',
      battlefieldWidth: 15,
      battlefieldHeight: 11,
      obstacles: [],
      attacker: {
        side: 'ATTACKER',
        slot: 0,
        unitName: 'Pikeman',
        count: 10,
        topHp: 10,
        q: 0,
        r: 5,
      },
      defender: { side: 'DEFENDER', slot: 0, unitName: 'Goblin', count: 8, topHp: 5, q: 14, r: 5 },
      stacks: [],
      attackerMorale: 0,
      defenderMorale: 0,
    });
    expect(container.textContent).not.toContain('Moral');
  });

  it('names the heroes of both armies when the battle has them', () => {
    const { container } = renderEvent({
      type: 'BattleStart',
      battlefieldWidth: 15,
      battlefieldHeight: 11,
      obstacles: [],
      attacker: {
        side: 'ATTACKER',
        slot: 0,
        unitName: 'Pikeman',
        count: 10,
        topHp: 10,
        q: 0,
        r: 5,
      },
      defender: { side: 'DEFENDER', slot: 0, unitName: 'Goblin', count: 8, topHp: 5, q: 14, r: 5 },
      stacks: [],
      attackerMorale: 0,
      defenderMorale: 0,
      attackerHero: 'Sorsha',
      defenderHero: 'Crag Hack',
    });
    expect(container.textContent).toContain('Helden: Sorsha gegen Crag Hack');
  });

  it('renders the tactics phase with the zone width', () => {
    const { container } = renderEvent({ type: 'TacticsPhase', actor: 'DEFENDER', columns: 5 });

    expect(container.textContent).toContain(
      'Verteidiger stellt mit Tactics in den ersten 5 Spalten auf',
    );
  });

  it('renders a ballista shot with its extras', () => {
    const { container } = renderEvent({
      type: 'BallistaShot',
      actor: 'ATTACKER',
      target: 'DEFENDER',
      targetSlot: 0,
      damage: 22,
      killed: 2,
      lucky: false,
      doubled: true,
      targetAfter: {
        side: 'DEFENDER',
        slot: 0,
        unitName: 'Goblin',
        count: 6,
        topHp: 5,
        q: 14,
        r: 5,
      },
    });

    expect(container.textContent).toContain('Ballista des Angreifers trifft Goblin: 22 Schaden');
    expect(container.textContent).toContain('doppelter Schaden durch Artillery');
  });

  it('renders the first aid tent with the healed points', () => {
    const { container } = renderEvent({
      type: 'FirstAid',
      actor: 'DEFENDER',
      targetSlot: 0,
      healed: 17,
      targetAfter: {
        side: 'DEFENDER',
        slot: 0,
        unitName: 'Goblin',
        count: 8,
        topHp: 5,
        q: 14,
        r: 5,
      },
    });

    expect(container.textContent).toContain(
      'Sanitätszelt des Verteidigers heilt Goblin um 17 Lebenspunkte',
    );
  });

  it('renders a creature spell and marks a resisted one', () => {
    const snap = {
      side: 'DEFENDER',
      slot: 0,
      unitName: 'Goblin',
      count: 8,
      topHp: 5,
      q: 14,
      r: 5,
    } as const;
    const cast = renderEvent({
      type: 'SpellCast',
      actor: 'ATTACKER',
      actorSlot: 0,
      spell: 'BLIND',
      target: 'DEFENDER',
      targetSlot: 0,
      resisted: true,
      targetAfter: snap,
    });
    expect(cast.container.textContent).toContain('Pikeman wirkt Blind auf Goblin — abgewehrt');

    const dispel = renderEvent({
      type: 'Dispel',
      actor: 'ATTACKER',
      actorSlot: 0,
      target: 'DEFENDER',
      targetSlot: 0,
      removed: 2,
      targetAfter: snap,
    });
    expect(dispel.container.textContent).toContain('hebt 2 Zauber von Goblin auf');
  });

  it('renders a hero spell and its hits', () => {
    const snap = {
      side: 'DEFENDER',
      slot: 0,
      unitName: 'Goblin',
      count: 8,
      topHp: 5,
      q: 14,
      r: 5,
    } as const;
    const cast = renderEvent({
      type: 'HeroSpell',
      actor: 'DEFENDER',
      spell: 'FIREBALL',
      level: 'ADVANCED',
      manaLeft: 15,
    });
    expect(cast.container.textContent).toContain(
      'Held des Verteidigers wirkt Fireball (Advanced), 15 Mana übrig',
    );

    const hit = {
      type: 'SpellHit',
      actor: 'ATTACKER',
      spell: 'LIGHTNING_BOLT',
      target: 'DEFENDER',
      targetSlot: 0,
      damage: 35,
      killed: 7,
      healed: 0,
      resisted: false,
      reflected: false,
      targetAfter: snap,
    } as const;
    expect(renderEvent(hit).container.textContent).toContain(
      'Lightning Bolt trifft Goblin: 35 Schaden, 7 getötet',
    );
    expect(
      renderEvent({ ...hit, resisted: true, damage: 0, killed: 0 }).container.textContent,
    ).toContain('Lightning Bolt prallt an Goblin ab');
    expect(
      renderEvent({ ...hit, spell: 'CURE', damage: 0, killed: 0, healed: 12 }).container
        .textContent,
    ).toContain('Cure heilt Goblin um 12 Lebenspunkte');
    expect(
      renderEvent({ ...hit, spell: 'SLOW', damage: 0, killed: 0 }).container.textContent,
    ).toContain('Slow wirkt auf Goblin');
    expect(renderEvent({ ...hit, reflected: true }).container.textContent).toContain(
      'Magic Mirror lenkt den Zauber um: Lightning Bolt trifft Goblin',
    );
    expect(
      renderEvent({
        type: 'Summon',
        actor: 'ATTACKER',
        spell: 'SUMMON_FIRE_ELEMENTAL',
        count: 6,
        stack: { ...snap, side: 'ATTACKER', slot: 7, unitName: 'Fire Elemental' },
      }).container.textContent,
    ).toContain('Der Held des Angreifers beschwört 6 Fire Elemental');
  });

  it('renders LuckyStrike as double damage', () => {
    const { container } = renderEvent({ type: 'LuckyStrike', actor: 'ATTACKER', actorSlot: 0 });

    expect(container.textContent).toContain('doppelten Schaden');
  });

  it('adds the luck rating when one army has luck', () => {
    const { container } = renderEvent({
      type: 'BattleStart',
      battlefieldWidth: 15,
      battlefieldHeight: 11,
      obstacles: [],
      attacker: {
        side: 'ATTACKER',
        slot: 0,
        unitName: 'Pikeman',
        count: 10,
        topHp: 10,
        q: 0,
        r: 5,
      },
      defender: { side: 'DEFENDER', slot: 0, unitName: 'Goblin', count: 8, topHp: 5, q: 14, r: 5 },
      stacks: [],
      attackerMorale: 0,
      defenderMorale: 0,
      attackerLuck: 2,
      defenderLuck: 0,
    });
    expect(container.textContent).toContain('Glück: Angreifer +2');
  });

  it('renders BadMorale as a lost action', () => {
    const { container } = renderEvent({ type: 'BadMorale', actor: 'DEFENDER', actorSlot: 0 });

    expect(container.textContent).toContain('schlechte Moral');
  });

  it('renders Resurrection with the number of restored creatures', () => {
    const { container } = renderEvent({
      type: 'Resurrection',
      actor: 'ATTACKER',
      actorSlot: 0,
      target: 'ATTACKER',
      targetSlot: 0,
      restoredCount: 3,
      targetAfter: {
        side: 'ATTACKER',
        slot: 0,
        unitName: 'Pikeman',
        count: 7,
        topHp: 10,
        q: 0,
        r: 5,
      },
    });

    expect(container.textContent).toContain('belebt 3 Einheiten');
  });

  it('renders RaiseDemons with the number of demons', () => {
    const { container } = renderEvent({
      type: 'RaiseDemons',
      actor: 'ATTACKER',
      actorSlot: 0,
      target: 'ATTACKER',
      targetSlot: 1,
      raisedCount: 4,
      targetAfter: {
        side: 'ATTACKER',
        slot: 1,
        unitName: 'Demon',
        count: 4,
        topHp: 35,
        q: 0,
        r: 5,
      },
    });

    expect(container.textContent).toContain('4 Dämonen');
  });

  it('paints the attacker name in amber and the defender name in blue', () => {
    const { getByText } = renderEvent({
      type: 'Melee',
      actor: 'ATTACKER',
      actorSlot: 0,
      target: 'DEFENDER',
      targetSlot: 0,
      hexesMoved: 0,
      damage: 5,
      killed: 1,
      targetAfter: {
        side: 'DEFENDER',
        slot: 0,
        unitName: 'Goblin',
        count: 7,
        topHp: 5,
        q: 1,
        r: 5,
      },
    });
    expect(getByText('Pikeman')).toHaveClass('text-amber-300');
    expect(getByText('Goblin')).toHaveClass('text-blue-300');
  });

  it('describes catapult, tower and moat events of a siege', () => {
    const snap = {
      side: 'ATTACKER' as const,
      slot: 0,
      unitName: 'Pikeman',
      count: 8,
      topHp: 10,
      q: 9,
      r: 4,
    };
    expect(
      renderEvent({
        type: 'CatapultShot',
        aimed: 'GATE',
        hit: 'MID_UPPER_WALL',
        damage: 2,
        healthAfter: 0,
      }).container.textContent,
    ).toContain('Tor');
    expect(
      renderEvent({
        type: 'TowerShot',
        tower: 'KEEP',
        targetSlot: 0,
        damage: 60,
        killed: 2,
        targetAfter: snap,
      }).container.textContent,
    ).toContain('Hauptturm');
    expect(
      renderEvent({
        type: 'MoatDamage',
        side: 'ATTACKER',
        slot: 0,
        damage: 70,
        killed: 0,
        mine: false,
        targetAfter: snap,
      }).container.textContent,
    ).toContain('Graben');
    expect(
      renderEvent({
        type: 'MoatDamage',
        side: 'ATTACKER',
        slot: 0,
        damage: 150,
        killed: 3,
        mine: true,
        targetAfter: snap,
      }).container.textContent,
    ).toContain('Landmine');
    expect(
      renderEvent({
        type: 'CatapultShot',
        aimed: 'GATE',
        hit: 'GATE',
        damage: 1,
        healthAfter: 1,
        shooterSlot: 0,
      }).container.textContent,
    ).toMatch(/zielt auf Tor und trifft Tor: 1 Treffer, noch 1/);
    expect(
      renderEvent({ type: 'QuakeHit', hit: 'LOWER_WALL', healthAfter: 1 }).container.textContent,
    ).toBe('Das Erdbeben trifft untere Mauer, noch 1.');
    expect(
      renderEvent({
        type: 'Bind',
        actor: 'ATTACKER',
        actorSlot: 0,
        target: 'DEFENDER',
        targetSlot: 0,
      }).container.textContent,
    ).toContain('fesseln');
    expect(renderEvent({ type: 'Drawbridge', state: 'OPEN' }).container.textContent).toContain(
      'Zugbrücke senkt sich',
    );
  });

  it('formats Move with origin and destination coordinates', () => {
    const { container } = renderEvent({
      type: 'Move',
      actor: 'DEFENDER',
      actorSlot: 0,
      fromQ: 14,
      fromR: 5,
      toQ: 10,
      toR: 5,
      path: [],
    });
    expect(container.textContent).toContain('(14,5)');
    expect(container.textContent).toContain('(10,5)');
  });

  it('uses the localized DRAW label when neither side wins', () => {
    const { getByText, container } = renderEvent({
      type: 'BattleEnd',
      winner: 'DRAW',
      attackerSurvivors: 0,
      defenderSurvivors: 0,
      turns: 7,
      finalStacks: [],
    });
    expect(getByText('Unentschieden')).toBeInTheDocument();
    expect(container.textContent).toContain('7 Runden');
  });

  it('uses per-slot names when bySlot map is provided (multi-stack)', () => {
    const names = {
      attacker: 'Attacker',
      defender: 'Defender',
      bySlot: new Map([
        ['ATTACKER-0', 'Halberdier'],
        ['DEFENDER-3', 'Medusa Queen'],
      ]),
    };
    const { getByText } = render(
      <EventText
        names={names}
        event={{
          type: 'Shoot',
          actor: 'ATTACKER',
          actorSlot: 0,
          target: 'DEFENDER',
          targetSlot: 3,
          distance: 5,
          damage: 12,
          killed: 1,
          targetAfter: {
            side: 'DEFENDER',
            slot: 3,
            unitName: 'Medusa Queen',
            count: 3,
            topHp: 10,
            q: 14,
            r: 6,
          },
        }}
      />,
    );
    expect(getByText('Halberdier')).toBeInTheDocument();
    expect(getByText('Medusa Queen')).toBeInTheDocument();
  });
});
