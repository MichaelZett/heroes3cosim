import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type {
  Faction,
  Fortification,
  MagicTerrain,
  StackSpec,
  Terrain,
} from '../../shared/api/types';
import { DEFAULT_BUILD, type HeroBuild } from './heroBuild';

/**
 * Konfigurations-State für Army-vs-Army. Hält 7 Slots pro Seite (Default-leer) plus Seed
 * und die jeweils zuletzt angewählten Preset-Faktionen für die Dropdowns.
 */
export interface ArmyConfigForm {
  attackerFaction: Faction | null;
  attackerStacks: StackSpec[];
  /** Held der Angreifer-Armee; null = Standardheld der Hauptfraktion. */
  attackerHeroName: string | null;
  /** Armee ohne Helden (neutrale Monster, Garnison). */
  attackerWithoutHero?: boolean;
  /** Level, Punkte, Fertigkeiten und gesammelte Boni des Angreifer-Helden. */
  attackerBuild: HeroBuild;
  defenderFaction: Faction | null;
  defenderStacks: StackSpec[];
  defenderHeroName: string | null;
  defenderWithoutHero?: boolean;
  defenderBuild: HeroBuild;
  seedText: string;
  /** Belagerung: Befestigung der Stadt des Verteidigers; null = Feldschlacht. */
  fortification?: Fortification | null;
  /** Stadt des Verteidigers; null = Backend-Default (Held, sonst Hauptfraktion der Garnison). */
  town?: Faction | null;
  /** Grundgelände der Feldschlacht; null = ohne Gelände. */
  terrain?: Terrain | null;
  /** Magisches Gelände der Feldschlacht. */
  magicTerrain?: MagicTerrain | null;
  /** Zauberkosten nach Shadow of Death statt nach dem RoE-Manual. */
  sodSpellCosts?: boolean;
}

export interface ArmyConfigStore {
  form: ArmyConfigForm;
  setForm: (patch: Partial<ArmyConfigForm>) => void;
  updateStack: (side: 'attacker' | 'defender', slot: number, patch: Partial<StackSpec>) => void;
  reset: () => void;
}

const EMPTY_SLOTS: StackSpec[] = Array.from({ length: 7 }, () => ({ unitName: '', count: 1 }));

const DEFAULT_FORM: ArmyConfigForm = {
  attackerFaction: null,
  attackerStacks: EMPTY_SLOTS,
  attackerHeroName: null,
  attackerWithoutHero: false,
  attackerBuild: DEFAULT_BUILD,
  defenderFaction: null,
  defenderStacks: EMPTY_SLOTS,
  defenderHeroName: null,
  defenderWithoutHero: false,
  defenderBuild: DEFAULT_BUILD,
  seedText: '',
  fortification: null,
  town: null,
  terrain: null,
  magicTerrain: null,
  sodSpellCosts: false,
};

function withUpdatedSlot(
  stacks: StackSpec[],
  slot: number,
  patch: Partial<StackSpec>,
): StackSpec[] {
  return stacks.map((s, i) => (i === slot ? { ...s, ...patch } : s));
}

export const useArmyConfigStore = create<ArmyConfigStore>()(
  persist(
    (set) => ({
      form: DEFAULT_FORM,
      setForm: (patch) => set((state) => ({ form: { ...state.form, ...patch } })),
      updateStack: (side, slot, patch) =>
        set((state) => {
          const key = side === 'attacker' ? 'attackerStacks' : 'defenderStacks';
          return { form: { ...state.form, [key]: withUpdatedSlot(state.form[key], slot, patch) } };
        }),
      reset: () => set({ form: DEFAULT_FORM }),
    }),
    { name: 'heroes3cosim.army-config-form' },
  ),
);
