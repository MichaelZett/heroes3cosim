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

/**
 * Pure reducer: takes the event list and a cursor (number of events played) and
 * derives the resulting battlefield snapshot. The frontend renders strictly from
 * this — no engine logic is duplicated client-side.
 */
export function reduceEvents(events: readonly BattleEvent[], cursor: number): BattleState | null {
    const start = events[0];
    if (!start || start.type !== 'BattleStart') return null;
    // The initial snapshot's topHp == max HP of the top creature, snapshot.count == start count.
    let attacker = snapshotToSideState(start.attacker, start.attacker.topHp, start.attacker.count);
    let defender = snapshotToSideState(start.defender, start.defender.topHp, start.defender.count);

    for (let i = 1; i < cursor && i < events.length; i++) {
        const event = events[i];
        switch (event.type) {
            case 'Move':
            case 'MoveBack': {
                const toQ = event.type === 'Move' ? event.toQ : event.toQ;
                const toR = event.type === 'Move' ? event.toR : event.toR;
                if (event.actor === 'ATTACKER') attacker = {...attacker, q: toQ, r: toR};
                else defender = {...defender, q: toQ, r: toR};
                break;
            }
            case 'Shoot':
            case 'Melee':
            case 'DeathStare':
            case 'Thunderbolts': {
                if (event.target === 'ATTACKER') attacker = applySnapshot(attacker, event.targetAfter);
                else defender = applySnapshot(defender, event.targetAfter);
                break;
            }
            case 'Retaliation': {
                if (event.target === 'ATTACKER') attacker = applySnapshot(attacker, event.targetAfter);
                else defender = applySnapshot(defender, event.targetAfter);
                break;
            }
            case 'FireShield': {
                if (event.attacker === 'ATTACKER') attacker = applySnapshot(attacker, event.attackerAfter);
                else defender = applySnapshot(defender, event.attackerAfter);
                break;
            }
            case 'Rebirth': {
                if (event.actor === 'ATTACKER') attacker = applySnapshot(attacker, event.actorAfter);
                else defender = applySnapshot(defender, event.actorAfter);
                break;
            }
            // Marker-only events — keine State-Mutation am Grid:
            case 'Wait':
            case 'TwoBlows':
            case 'TwoShots':
            case 'GoodMorale':
            case 'Petrifying':
            case 'Cursing':
            case 'Poisoning':
            case 'Diseasing':
            case 'Aging':
            case 'BattleStart':
            case 'BattleEnd':
                break;
        }
    }

    return {
        width: start.battlefieldWidth,
        height: start.battlefieldHeight,
        obstacles: start.obstacles ?? [],
        attacker,
        defender,
    };
}
