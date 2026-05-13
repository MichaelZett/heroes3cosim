import type {BattleEvent, HexCoord, Side, StackSnapshot} from '../../shared/api/types';

export interface SideState {
    side: Side;
    unitName: string;
    count: number;
    topHp: number;
    maxHp: number;
    startCount: number;
    q: number;
    r: number;
}

export interface BattleState {
    width: number;
    height: number;
    obstacles: readonly HexCoord[];
    attacker: SideState;
    defender: SideState;
}

function snapshotToSideState(snap: StackSnapshot, maxHp: number, startCount: number): SideState {
    return {
        side: snap.side,
        unitName: snap.unitName,
        count: snap.count,
        topHp: snap.topHp,
        maxHp,
        startCount,
        q: snap.q,
        r: snap.r,
    };
}

function applySnapshot(side: SideState, snap: StackSnapshot): SideState {
    return {
        ...side,
        count: snap.count,
        topHp: snap.topHp,
        q: snap.q,
        r: snap.r,
    };
}

interface SidesState {
    attacker: SideState;
    defender: SideState;
}

function updateSide(sides: SidesState, side: Side, next: SideState): SidesState {
    return side === 'ATTACKER' ? {...sides, attacker: next} : {...sides, defender: next};
}

function movePosition(sides: SidesState, side: Side, q: number, r: number): SidesState {
    const current = side === 'ATTACKER' ? sides.attacker : sides.defender;
    return updateSide(sides, side, {...current, q, r});
}

function snapshotInto(sides: SidesState, side: Side, snap: StackSnapshot): SidesState {
    const current = side === 'ATTACKER' ? sides.attacker : sides.defender;
    return updateSide(sides, side, applySnapshot(current, snap));
}

function applyEvent(sides: SidesState, event: BattleEvent): SidesState {
    switch (event.type) {
        case 'Move':
        case 'MoveBack':
            return movePosition(sides, event.actor, event.toQ, event.toR);
        case 'Shoot':
        case 'Melee':
        case 'DeathStare':
        case 'Thunderbolts':
        case 'Retaliation':
            return snapshotInto(sides, event.target, event.targetAfter);
        case 'FireShield':
            return snapshotInto(sides, event.attacker, event.attackerAfter);
        case 'Rebirth':
            return snapshotInto(sides, event.actor, event.actorAfter);
        default:
            // Marker-only events (Wait, TwoBlows, TwoShots, GoodMorale, Petrifying, Cursing,
            // Poisoning, Diseasing, Aging, BattleStart, BattleEnd) — kein Grid-State.
            return sides;
    }
}

/**
 * Pure reducer: takes the event list and a cursor (number of events played) and
 * derives the resulting battlefield snapshot. The frontend renders strictly from
 * this — no engine logic is duplicated client-side.
 */
export function reduceEvents(events: readonly BattleEvent[], cursor: number): BattleState | null {
    const start = events[0];
    if (start?.type !== 'BattleStart') return null;
    // The initial snapshot's topHp == max HP of the top creature, snapshot.count == start count.
    let sides: SidesState = {
        attacker: snapshotToSideState(start.attacker, start.attacker.topHp, start.attacker.count),
        defender: snapshotToSideState(start.defender, start.defender.topHp, start.defender.count),
    };
    const end = Math.min(cursor, events.length);
    for (let i = 1; i < end; i++) {
        sides = applyEvent(sides, events[i]);
    }
    return {
        width: start.battlefieldWidth,
        height: start.battlefieldHeight,
        obstacles: start.obstacles ?? [],
        ...sides,
    };
}
