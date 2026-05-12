import {create} from 'zustand';
import {persist} from 'zustand/middleware';
import type {Faction, MatrixReport, MatrixRequestDto, StackSizingMode} from '../../shared/api/types';

/**
 * Persistente Form-Eingaben, damit „zurück zur Konfiguration" die letzte Auswahl wiederherstellt.
 * Werden bei jedem Tippen aktualisiert. {@code report} und {@code lastRequest} sind das Ergebnis
 * des letzten Laufs — Bewusst nicht persistiert (zu groß für localStorage, kurzlebige Daten).
 */
export interface MatrixFormState {
    unitCount: number;
    seedsPerMatchup: number;
    mode: StackSizingMode;
    excludedFactions: Faction[];
    excludedTiers: number[];
    excludedUnits: string[];
}

export interface MatrixStore {
    report: MatrixReport | null;
    lastRequest: MatrixRequestDto | null;
    form: MatrixFormState;
    loadReport: (report: MatrixReport, request: MatrixRequestDto) => void;
    setForm: (patch: Partial<MatrixFormState>) => void;
    reset: () => void;
}

const DEFAULT_FORM: MatrixFormState = {
    unitCount: 20,
    seedsPerMatchup: 20,
    mode: 'EQUAL_COUNT',
    excludedFactions: [],
    excludedTiers: [],
    excludedUnits: [],
};

export const useMatrixStore = create<MatrixStore>()(
    persist(
        (set) => ({
            report: null,
            lastRequest: null,
            form: DEFAULT_FORM,
            loadReport: (report, request) => set({report, lastRequest: request}),
            setForm: (patch) => set((state) => ({form: {...state.form, ...patch}})),
            reset: () => set({report: null, lastRequest: null, form: DEFAULT_FORM}),
        }),
        {
            name: 'heroes3cosim.matrix-form',
            // Nur die Form-Eingaben persistieren — Report wäre zu groß für localStorage.
            partialize: (state) => ({form: state.form}),
        },
    ),
);
