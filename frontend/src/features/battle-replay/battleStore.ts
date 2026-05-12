import {create} from 'zustand';
import type {BattleConfigRequest, BattleEvent, BattleSimulationDto} from '../../shared/api/types';
import {explodeMoves} from './explodeMoves';

export interface BattleStore {
    simulation: BattleSimulationDto | null;
    /** Letzte Sim-Konfiguration — Grundlage für das Rückspiel mit getauschten Seiten. */
    lastRequest: BattleConfigRequest | null;
    /** Index des nächsten noch nicht abgespielten Events. */
    cursor: number;
    /** Millisekunden zwischen zwei Events; default 400 ms. */
    speedMs: number;
    paused: boolean;

    loadSimulation: (sim: BattleSimulationDto, request: BattleConfigRequest) => void;
    reset: () => void;
    step: () => void;
    setSpeedMs: (ms: number) => void;
    setPaused: (paused: boolean) => void;
}

export const useBattleStore = create<BattleStore>((set) => ({
    simulation: null,
    lastRequest: null,
    cursor: 0,
    speedMs: 400,
    paused: false,

    loadSimulation: (sim, request) =>
        set({
            simulation: {...sim, events: explodeMoves(sim.events)},
            lastRequest: request,
            cursor: 0,
            paused: false,
        }),
    reset: () => set({cursor: 0, paused: false}),
    step: () =>
        set((state) => {
            if (!state.simulation) return state;
            const next = state.cursor + 1;
            if (next > state.simulation.events.length) return state;
            return {cursor: next};
        }),
    setSpeedMs: (ms) => set({speedMs: ms}),
    setPaused: (paused) => set({paused}),
}));

/** Liefert das gerade aktuelle Event (das letzte abgespielte). */
export function selectCurrentEvent(state: BattleStore): BattleEvent | null {
    if (!state.simulation || state.cursor === 0) return null;
    return state.simulation.events[state.cursor - 1] ?? null;
}

/** Liefert die bisher abgespielten Events. */
export function selectPlayedEvents(state: BattleStore): BattleEvent[] {
    if (!state.simulation) return [];
    return state.simulation.events.slice(0, state.cursor);
}
