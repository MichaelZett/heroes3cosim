import { useEffect } from 'react';
import { useBattleStore } from '../store/battleStore';

/**
 * Treibt den Replay-Cursor automatisch hoch — solange nicht pausiert und das Ende
 * noch nicht erreicht ist. Tempo aus dem Store, abbrechbar via setPaused / reset.
 */
export function usePlayer() {
  const simulation = useBattleStore((s) => s.simulation);
  const cursor = useBattleStore((s) => s.cursor);
  const speedMs = useBattleStore((s) => s.speedMs);
  const paused = useBattleStore((s) => s.paused);
  const step = useBattleStore((s) => s.step);

  const totalEvents = simulation?.events.length ?? 0;
  const finished = totalEvents > 0 && cursor >= totalEvents;

  useEffect(() => {
    if (!simulation || paused || finished) return;
    const handle = window.setTimeout(step, speedMs);
    return () => window.clearTimeout(handle);
  }, [simulation, cursor, paused, finished, speedMs, step]);

  return { finished, totalEvents };
}
