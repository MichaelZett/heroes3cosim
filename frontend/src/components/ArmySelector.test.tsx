import {describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ArmySelector from './ArmySelector';
import {TEST_FACTIONS, TEST_UNITS} from '../test/fixtures';
import type {Faction} from '../api/types';

interface Overrides {
    selectedFaction?: Faction | 'ALL';
    selectedTier?: number | 'ALL';
    selectedUnit?: string;
    count?: number;
}

function renderSelector(overrides: Overrides = {}) {
    const onFactionChange = vi.fn();
    const onTierChange = vi.fn();
    const onUnitChange = vi.fn();
    const onCountChange = vi.fn();
    render(
        <ArmySelector
            title="Truppe"
            factions={TEST_FACTIONS}
            units={TEST_UNITS}
            selectedFaction={overrides.selectedFaction ?? 'ALL'}
            selectedTier={overrides.selectedTier ?? 'ALL'}
            selectedUnit={overrides.selectedUnit ?? ''}
            count={overrides.count ?? 50}
            onFactionChange={onFactionChange}
            onTierChange={onTierChange}
            onUnitChange={onUnitChange}
            onCountChange={onCountChange}
        />,
    );
    const unitSelect = screen.getByLabelText('Einheit') as HTMLSelectElement;
    const optionLabels = () =>
        Array.from(unitSelect.options)
            .map((o) => o.textContent ?? '')
            .filter((label) => !label.startsWith('—'));
    return {unitSelect, optionLabels, onFactionChange, onTierChange, onUnitChange, onCountChange};
}

describe('ArmySelector', () => {
    it('lists every unit alphabetically when no filter is set', () => {
        const {optionLabels} = renderSelector();
        expect(optionLabels()).toEqual([
            'Archer — T2',
            'Centaur — T1',
            'Dwarf — T2',
            'Gremlin — T1',
            'Halberdier — T1 ★',
            'Pikeman — T1',
        ]);
    });

    it('shows only units of the selected faction', () => {
        const {optionLabels} = renderSelector({selectedFaction: 'CASTLE'});
        expect(optionLabels()).toEqual(['Pikeman — T1', 'Halberdier — T1 ★', 'Archer — T2']);
    });

    it('shows only units of the selected tier', () => {
        const {optionLabels} = renderSelector({selectedTier: 2});
        expect(optionLabels()).toEqual(['Archer — T2', 'Dwarf — T2']);
    });

    it('combines faction and tier filters', () => {
        const {optionLabels} = renderSelector({selectedFaction: 'CASTLE', selectedTier: 1});
        expect(optionLabels()).toEqual(['Pikeman — T1', 'Halberdier — T1 ★']);
    });

    it('clears the selected unit when the faction changes', async () => {
        const user = userEvent.setup();
        const {onFactionChange, onUnitChange} = renderSelector({
            selectedFaction: 'ALL',
            selectedUnit: 'Pikeman',
        });
        await user.selectOptions(screen.getByLabelText('Faktion'), 'CASTLE');
        expect(onFactionChange).toHaveBeenCalledWith('CASTLE');
        expect(onUnitChange).toHaveBeenCalledWith('');
    });

    it('clears the selected unit when the tier changes', async () => {
        const user = userEvent.setup();
        const {onTierChange, onUnitChange} = renderSelector({selectedUnit: 'Pikeman'});
        await user.selectOptions(screen.getByLabelText('Tier'), 'Tier 2');
        expect(onTierChange).toHaveBeenCalledWith(2);
        expect(onUnitChange).toHaveBeenCalledWith('');
    });

    it('coerces non-positive counts to at least 1', () => {
        // fireEvent statt user.type, weil count controlled ist und der Test-Wrapper
        // den Parent-State nicht zurückschreibt — user.type würde nur am 10er-Wert
        // anhängen statt zu ersetzen.
        const {onCountChange} = renderSelector({count: 10});
        const input = screen.getByLabelText('Anzahl') as HTMLInputElement;
        fireEvent.change(input, {target: {value: '0'}});
        expect(onCountChange).toHaveBeenLastCalledWith(1);
    });

    it('renders stat details only when a unit is selected', () => {
        const {rerender} = render(
            <ArmySelector
                title="Truppe"
                factions={TEST_FACTIONS}
                units={TEST_UNITS}
                selectedFaction="ALL"
                selectedTier="ALL"
                selectedUnit=""
                count={50}
                onFactionChange={vi.fn()}
                onTierChange={vi.fn()}
                onUnitChange={vi.fn()}
                onCountChange={vi.fn()}
            />,
        );
        expect(screen.queryByText('Angriff / Verteidigung')).not.toBeInTheDocument();
        rerender(
            <ArmySelector
                title="Truppe"
                factions={TEST_FACTIONS}
                units={TEST_UNITS}
                selectedFaction="ALL"
                selectedTier="ALL"
                selectedUnit="Pikeman"
                count={50}
                onFactionChange={vi.fn()}
                onTierChange={vi.fn()}
                onUnitChange={vi.fn()}
                onCountChange={vi.fn()}
            />,
        );
        expect(screen.getByText('Angriff / Verteidigung')).toBeInTheDocument();
    });
});
