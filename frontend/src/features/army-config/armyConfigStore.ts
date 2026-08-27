import {create} from 'zustand';
import {persist} from 'zustand/middleware';
import type {Faction, StackSpec} from '../../shared/api/types';

/**
 * Konfigurations-State für Army-vs-Army. Hält 7 Slots pro Seite (Default-leer) plus Seed
 * und die jeweils zuletzt angewählten Preset-Faktionen für die Dropdowns.
 */
export interface ArmyConfigForm {
    attackerFaction: Faction | null;
    attackerStacks: StackSpec[];
    /** Held der Angreifer-Armee; null = fuehrerlos. */
    attackerHeroName: string | null;
    defenderFaction: Faction | null;
    defenderStacks: StackSpec[];
    defenderHeroName: string | null;
    seedText: string;
}

export interface ArmyConfigStore {
    form: ArmyConfigForm;
    setForm: (patch: Partial<ArmyConfigForm>) => void;
    updateStack: (side: 'attacker' | 'defender', slot: number, patch: Partial<StackSpec>) => void;
    reset: () => void;
}

const EMPTY_SLOTS: StackSpec[] = Array.from({length: 7}, () => ({unitName: '', count: 1}));

const DEFAULT_FORM: ArmyConfigForm = {
    attackerFaction: null,
    attackerStacks: EMPTY_SLOTS,
    attackerHeroName: null,
    defenderFaction: null,
    defenderStacks: EMPTY_SLOTS,
    defenderHeroName: null,
    seedText: '',
};

function withUpdatedSlot(stacks: StackSpec[], slot: number, patch: Partial<StackSpec>): StackSpec[] {
    return stacks.map((s, i) => (i === slot ? {...s, ...patch} : s));
}

export const useArmyConfigStore = create<ArmyConfigStore>()(
    persist(
        (set) => ({
            form: DEFAULT_FORM,
            setForm: (patch) => set((state) => ({form: {...state.form, ...patch}})),
            updateStack: (side, slot, patch) =>
                set((state) => {
                    const key = side === 'attacker' ? 'attackerStacks' : 'defenderStacks';
                    return {form: {...state.form, [key]: withUpdatedSlot(state.form[key], slot, patch)}};
                }),
            reset: () => set({form: DEFAULT_FORM}),
        }),
        {name: 'heroes3cosim.army-config-form'},
    ),
);
