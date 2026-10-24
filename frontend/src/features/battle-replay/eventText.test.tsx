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
