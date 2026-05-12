import {describe, expect, it} from 'vitest';
import {render} from '@testing-library/react';
import {EventText} from './eventText';
import type {BattleEvent} from '../api/types';

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
            attacker: {side: 'ATTACKER', unitName: 'Pikeman', count: 10, topHp: 10, q: 0, r: 5},
            defender: {side: 'DEFENDER', unitName: 'Goblin', count: 8, topHp: 5, q: 14, r: 5},
        });
        expect(getByText('Pikeman')).toBeInTheDocument();
        expect(getByText('Goblin')).toBeInTheDocument();
        expect(container.textContent).toContain('Kampf beginnt');
    });

    it('paints the attacker name in amber and the defender name in blue', () => {
        const {getByText} = renderEvent({
            type: 'Melee',
            actor: 'ATTACKER',
            target: 'DEFENDER',
            hexesMoved: 0,
            damage: 5,
            killed: 1,
            targetAfter: {side: 'DEFENDER', unitName: 'Goblin', count: 7, topHp: 5, q: 1, r: 5},
        });
        expect(getByText('Pikeman')).toHaveClass('text-amber-300');
        expect(getByText('Goblin')).toHaveClass('text-blue-300');
    });

    it('formats Move with origin and destination coordinates', () => {
        const {container} = renderEvent({
            type: 'Move',
            actor: 'DEFENDER',
            fromQ: 14,
            fromR: 5,
            toQ: 10,
            toR: 5,
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
        });
        expect(getByText('Unentschieden')).toBeInTheDocument();
        expect(container.textContent).toContain('7 Runden');
    });
});
