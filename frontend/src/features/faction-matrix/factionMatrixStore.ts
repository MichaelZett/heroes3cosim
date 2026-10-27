import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type {
  Faction,
  FactionMatrixReport,
  MagicTerrain,
  SolverKind,
  Terrain,
} from '../../shared/api/types';
import { TOWNS } from '../../shared/api/catalog';

/** Persistente Eingaben; das Ergebnis des letzten Laufs bleibt nur im Speicher. */
export interface FactionMatrixForm {
  factions: Faction[];
  solvers: SolverKind[];
  seedsPerPair: number;
  weeks: number;
  heroes: boolean;
  terrain: Terrain | null;
  magicTerrain: MagicTerrain | null;
}

export interface FactionMatrixStore {
  report: FactionMatrixReport | null;
  form: FactionMatrixForm;
  loadReport: (report: FactionMatrixReport) => void;
  setForm: (patch: Partial<FactionMatrixForm>) => void;
  reset: () => void;
}

const DEFAULT_FORM: FactionMatrixForm = {
  factions: [...TOWNS],
  solvers: ['GREEDY', 'STRATEGIC'],
  seedsPerPair: 10,
  weeks: 1,
  heroes: true,
  terrain: null,
  magicTerrain: null,
};

export const useFactionMatrixStore = create<FactionMatrixStore>()(
  persist(
    (set) => ({
      report: null,
      form: DEFAULT_FORM,
      loadReport: (report) => set({ report }),
      setForm: (patch) => set((state) => ({ form: { ...state.form, ...patch } })),
      reset: () => set({ report: null, form: DEFAULT_FORM }),
    }),
    {
      name: 'heroes3cosim.faction-matrix-form',
      partialize: (state) => ({ form: state.form }),
    },
  ),
);
