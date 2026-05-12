import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {act, renderHook} from '@testing-library/react';
import {usePlayer} from './usePlayer';
import {useBattleStore} from './battleStore';
import {simulationFixture} from '../../test/fixtures';

beforeEach(() => {
    vi.useFakeTimers();
    useBattleStore.setState({simulation: null, cursor: 0, speedMs: 400, paused: false});
});

afterEach(() => {
    vi.useRealTimers();
});

describe('usePlayer', () => {
    it('advances the cursor on every tick while playing', () => {
        useBattleStore.setState({simulation: simulationFixture(), cursor: 0, paused: false});
        renderHook(() => usePlayer());

        expect(useBattleStore.getState().cursor).toBe(0);
        act(() => {
            vi.advanceTimersByTime(400);
        });
        expect(useBattleStore.getState().cursor).toBe(1);
        act(() => {
            vi.advanceTimersByTime(400);
        });
        expect(useBattleStore.getState().cursor).toBe(2);
    });

    it('stops scheduling steps when paused', () => {
        useBattleStore.setState({simulation: simulationFixture(), cursor: 0, paused: true});
        renderHook(() => usePlayer());

        act(() => {
            vi.advanceTimersByTime(10_000);
        });
        expect(useBattleStore.getState().cursor).toBe(0);
    });

    it('reports finished once the cursor reaches the end and stops stepping', () => {
        const sim = simulationFixture();
        useBattleStore.setState({simulation: sim, cursor: sim.events.length, paused: false});
        const {result} = renderHook(() => usePlayer());

        expect(result.current.finished).toBe(true);
        act(() => {
            vi.advanceTimersByTime(10_000);
        });
        expect(useBattleStore.getState().cursor).toBe(sim.events.length);
    });

    it('uses the current speedMs from the store', () => {
        useBattleStore.setState({simulation: simulationFixture(), cursor: 0, paused: false, speedMs: 1500});
        renderHook(() => usePlayer());

        act(() => {
            vi.advanceTimersByTime(800);
        });
        expect(useBattleStore.getState().cursor).toBe(0);
        act(() => {
            vi.advanceTimersByTime(700);
        });
        expect(useBattleStore.getState().cursor).toBe(1);
    });
});
