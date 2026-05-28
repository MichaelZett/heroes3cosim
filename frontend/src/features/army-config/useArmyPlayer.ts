import {useEffect} from 'react';
import {useArmyBattleStore} from './armyBattleStore';

export function useArmyPlayer() {
    const simulation = useArmyBattleStore((s) => s.simulation);
    const cursor = useArmyBattleStore((s) => s.cursor);
    const speedMs = useArmyBattleStore((s) => s.speedMs);
    const paused = useArmyBattleStore((s) => s.paused);
    const step = useArmyBattleStore((s) => s.step);

    const totalEvents = simulation?.events.length ?? 0;
    const finished = totalEvents > 0 && cursor >= totalEvents;

    useEffect(() => {
        if (!simulation || paused || finished) return;
        const handle = globalThis.setTimeout(step, speedMs);
        return () => globalThis.clearTimeout(handle);
    }, [simulation, cursor, paused, finished, speedMs, step]);

    return {finished, totalEvents};
}
