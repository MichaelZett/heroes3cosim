import {describe, expect, it} from 'vitest';
import {reduceEvents} from './state';
import type {BattleEvent, StackSnapshot} from '../../shared/api/types';

const attackerStart: StackSnapshot = {
    side: 'ATTACKER',
    unitName: 'Pikeman',
    count: 10,
    topHp: 10,
    q: 0,
    r: 5,
};
const defenderStart: StackSnapshot = {
    side: 'DEFENDER',
    unitName: 'Goblin',
    count: 8,
    topHp: 5,
    q: 14,
    r: 5,
};
const start: BattleEvent = {
    type: 'BattleStart',
    battlefieldWidth: 15,
    battlefieldHeight: 11,
    obstacles: [],
    attacker: attackerStart,
    defender: defenderStart,
};

describe('reduceEvents', () => {
    it('returns null when the event list is empty or does not start with BattleStart', () => {
        expect(reduceEvents([], 0)).toBeNull();
        expect(reduceEvents([{type: 'Wait', actor: 'ATTACKER'}], 1)).toBeNull();
    });

    it('initializes both sides from the BattleStart snapshot', () => {
        const state = reduceEvents([start], 1);
        expect(state).not.toBeNull();
        expect(state!.width).toBe(15);
        expect(state!.height).toBe(11);
        expect(state!.attacker.unitName).toBe('Pikeman');
        expect(state!.attacker.startCount).toBe(10);
        expect(state!.defender.unitName).toBe('Goblin');
        expect(state!.defender.maxHp).toBe(5);
    });

    it('applies Move events to the actor position', () => {
        const events: BattleEvent[] = [
            start,
            {type: 'Move', actor: 'ATTACKER', fromQ: 0, fromR: 5, toQ: 6, toR: 5, path: []},
        ];
        const state = reduceEvents(events, 2);
        expect(state!.attacker.q).toBe(6);
        expect(state!.attacker.r).toBe(5);
        expect(state!.defender.q).toBe(14);
    });

    it('applies MoveBack events to the actor position', () => {
        const events: BattleEvent[] = [
            start,
            {type: 'Move', actor: 'ATTACKER', fromQ: 0, fromR: 5, toQ: 6, toR: 5, path: []},
            {type: 'MoveBack', actor: 'ATTACKER', toQ: 0, toR: 5, path: []},
        ];
        const state = reduceEvents(events, 3);
        expect(state!.attacker.q).toBe(0);
    });

    it('applies Melee post-state to the target side', () => {
        const events: BattleEvent[] = [
            start,
            {
                type: 'Melee',
                actor: 'ATTACKER',
                target: 'DEFENDER',
                hexesMoved: 0,
                damage: 30,
                killed: 6,
                targetAfter: {...defenderStart, count: 2, topHp: 5},
            },
        ];
        const state = reduceEvents(events, 2);
        expect(state!.defender.count).toBe(2);
        expect(state!.attacker.count).toBe(10);
    });

    it('applies Retaliation post-state to the retaliation target (the original attacker)', () => {
        const events: BattleEvent[] = [
            start,
            {
                type: 'Retaliation',
                retaliator: 'DEFENDER',
                target: 'ATTACKER',
                damage: 4,
                killed: 1,
                targetAfter: {...attackerStart, count: 9, topHp: 10},
            },
        ];
        const state = reduceEvents(events, 2);
        expect(state!.attacker.count).toBe(9);
    });

    it('applies FireShield reverse damage to the attacker side', () => {
        const events: BattleEvent[] = [
            start,
            {
                type: 'FireShield',
                shielded: 'DEFENDER',
                attacker: 'ATTACKER',
                damage: 6,
                attackerAfter: {...attackerStart, count: 9, topHp: 4},
            },
        ];
        const state = reduceEvents(events, 2);
        expect(state!.attacker.count).toBe(9);
        expect(state!.attacker.topHp).toBe(4);
    });

    it('ignores marker events that carry no state mutation', () => {
        const events: BattleEvent[] = [
            start,
            {type: 'TwoBlows', actor: 'ATTACKER'},
            {type: 'GoodMorale', actor: 'DEFENDER'},
            {type: 'Petrifying', actor: 'ATTACKER', target: 'DEFENDER'},
        ];
        const state = reduceEvents(events, 4);
        expect(state!.attacker).toMatchObject({count: 10, q: 0, r: 5});
        expect(state!.defender).toMatchObject({count: 8, q: 14, r: 5});
    });

    it('respects the cursor — only events 0..cursor-1 are applied', () => {
        const events: BattleEvent[] = [
            start,
            {type: 'Move', actor: 'ATTACKER', fromQ: 0, fromR: 5, toQ: 6, toR: 5, path: []},
            {type: 'Move', actor: 'ATTACKER', fromQ: 6, fromR: 5, toQ: 12, toR: 5, path: []},
        ];
        expect(reduceEvents(events, 2)!.attacker.q).toBe(6);
        expect(reduceEvents(events, 3)!.attacker.q).toBe(12);
    });
});
