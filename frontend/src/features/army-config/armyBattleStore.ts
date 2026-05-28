import {create} from 'zustand';
import type {ArmyBattleRequest, ArmyBattleSimulation, BattleEvent} from '../../shared/api/types';
import {explodeMoves} from '../battle-replay/explodeMoves';

export interface ArmyBattleStore {
    simulation: ArmyBattleSimulation | null;
    /** Letzte Sim-Konfiguration — Grundlage für Rematch mit getauschten Armeen. */
    lastRequest: ArmyBattleRequest | null;
    cursor: number;
    speedMs: number;
    paused: boolean;

    loadSimulation: (sim: ArmyBattleSimulation, request: ArmyBattleRequest) => void;
    reset: () => void;
    step: () => void;
    setSpeedMs: (ms: number) => void;
    setPaused: (paused: boolean) => void;
}

export const useArmyBattleStore = create<ArmyBattleStore>((set) => ({
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

export function selectArmyCurrentEvent(state: ArmyBattleStore): BattleEvent | null {
    if (!state.simulation || state.cursor === 0) return null;
    return state.simulation.events[state.cursor - 1] ?? null;
}

export function selectArmyPlayedEvents(state: ArmyBattleStore): BattleEvent[] {
    if (!state.simulation) return [];
    return state.simulation.events.slice(0, state.cursor);
}
