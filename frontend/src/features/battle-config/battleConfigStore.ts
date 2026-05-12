import {create} from 'zustand';
import {persist} from 'zustand/middleware';
import type {Faction} from '../../shared/api/types';

/**
 * Form-Zustand der Single-Battle-Konfiguration. Wird über Navigation hinweg persistiert (per
 * Zustand-Middleware in localStorage), damit „zurück zur Konfiguration" die letzten
 * Eingaben wiederherstellt.
 */
export interface BattleConfigForm {
    attackerFaction: Faction | 'ALL';
    attackerTier: number | 'ALL';
    attackerUnit: string;
    attackerCount: number;
    defenderFaction: Faction | 'ALL';
    defenderTier: number | 'ALL';
    defenderUnit: string;
    defenderCount: number;
    seedText: string;
}

export interface BattleConfigStore {
    form: BattleConfigForm;
    setForm: (patch: Partial<BattleConfigForm>) => void;
    reset: () => void;
}

const DEFAULT_FORM: BattleConfigForm = {
    attackerFaction: 'ALL',
    attackerTier: 'ALL',
    attackerUnit: '',
    attackerCount: 50,
    defenderFaction: 'ALL',
    defenderTier: 'ALL',
    defenderUnit: '',
    defenderCount: 50,
    seedText: '',
};

export const useBattleConfigStore = create<BattleConfigStore>()(
    persist(
        (set) => ({
            form: DEFAULT_FORM,
            setForm: (patch) => set((state) => ({form: {...state.form, ...patch}})),
            reset: () => set({form: DEFAULT_FORM}),
        }),
        {name: 'heroes3cosim.battle-config-form'},
    ),
);
