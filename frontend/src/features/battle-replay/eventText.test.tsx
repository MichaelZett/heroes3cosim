import {describe, expect, it} from 'vitest';
import {render} from '@testing-library/react';
import {EventText} from './eventText';
import type {BattleEvent} from '../../shared/api/types';

const NAMES = {attacker: 'Pikeman', defender: 'Goblin'};

function renderEvent(event: BattleEvent) {
    return render(<EventText event={event} names={NAMES}/>);
}

describe('eventToNode', () => {
    it('renders BattleStart with both unit names', () => {
        const {getByText, container} = renderEvent({
            type: 'BattleStart',
            battlefieldWidth: 15,
            battlefieldHeight: 11,
            obstacles: [],
            attacker: {side: 'ATTACKER', slot: 0, unitName: 'Pikeman', count: 10, topHp: 10, q: 0, r: 5},
            defender: {side: 'DEFENDER', slot: 0, unitName: 'Goblin', count: 8, topHp: 5, q: 14, r: 5},
            stacks: [],
        });
        expect(getByText('Pikeman')).toBeInTheDocument();
        expect(getByText('Goblin')).toBeInTheDocument();
        expect(container.textContent).toContain('Kampf beginnt');
    });

    it('paints the attacker name in amber and the defender name in blue', () => {
        const {getByText} = renderEvent({
            type: 'Melee',
            actor: 'ATTACKER',
            actorSlot: 0,
            target: 'DEFENDER',
            targetSlot: 0,
            hexesMoved: 0,
            damage: 5,
            killed: 1,
            targetAfter: {side: 'DEFENDER', slot: 0, unitName: 'Goblin', count: 7, topHp: 5, q: 1, r: 5},
        });
        expect(getByText('Pikeman')).toHaveClass('text-amber-300');
        expect(getByText('Goblin')).toHaveClass('text-blue-300');
    });

    it('formats Move with origin and destination coordinates', () => {
        const {container} = renderEvent({
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
        const {getByText, container} = renderEvent({
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
        const {getByText} = render(
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
                    targetAfter: {side: 'DEFENDER', slot: 3, unitName: 'Medusa Queen', count: 3, topHp: 10, q: 14, r: 6},
                }}/>,
        );
        expect(getByText('Halberdier')).toBeInTheDocument();
        expect(getByText('Medusa Queen')).toBeInTheDocument();
    });
});
