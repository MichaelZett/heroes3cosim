import { useMemo } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useBattleStore } from '../store/battleStore';
import { reduceEvents } from '../battle/state';
import HexGrid from '../battle/HexGrid';
import EventLog from '../battle/EventLog';
import PlaybackControls from '../battle/PlaybackControls';
import { usePlayer } from '../battle/usePlayer';

export default function BattlePage() {
  const navigate = useNavigate();
  const simulation = useBattleStore((s) => s.simulation);
  const cursor = useBattleStore((s) => s.cursor);
  const speedMs = useBattleStore((s) => s.speedMs);
  const paused = useBattleStore((s) => s.paused);
  const setSpeedMs = useBattleStore((s) => s.setSpeedMs);
  const setPaused = useBattleStore((s) => s.setPaused);
  const reset = useBattleStore((s) => s.reset);
  const step = useBattleStore((s) => s.step);

  const { finished } = usePlayer();

  const state = useMemo(
    () => (simulation ? reduceEvents(simulation.events, cursor) : null),
    [simulation, cursor],
  );

  if (!simulation) {
    return <Navigate to="/" replace />;
  }
  if (!state) {
    return null;
  }

  return (
    <main className="mx-auto max-w-5xl space-y-4 p-4 md:p-8">
      <header className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-slate-100">Battle Replay</h1>
        <button
          type="button"
          onClick={() => navigate('/')}
          className="text-sm text-slate-400 hover:text-amber-400"
        >
          ← Neue Konfiguration
        </button>
      </header>

      <PlaybackControls
        speedMs={speedMs}
        onSpeedChange={setSpeedMs}
        paused={paused}
        onTogglePaused={() => setPaused(!paused)}
        onRestart={reset}
        onStep={step}
        finished={finished}
      />

      <section className="rounded-lg border border-slate-800 bg-slate-900 p-4">
        <HexGrid state={state} />
        <div className="mt-4 grid grid-cols-2 gap-4 text-sm">
          <SideCard label="Truppe 1" sub={`${state.attacker.unitName}`} count={state.attacker.count} max={state.attacker.startCount} color="amber" />
          <SideCard label="Truppe 2" sub={`${state.defender.unitName}`} count={state.defender.count} max={state.defender.startCount} color="blue" />
        </div>
      </section>

      <section className="rounded-lg border border-slate-800 bg-slate-900 p-4">
        <h2 className="mb-2 text-lg font-semibold text-slate-100">Combat-Log</h2>
        <EventLog events={simulation.events} cursor={cursor} />
      </section>
    </main>
  );
}

function SideCard({
  label,
  sub,
  count,
  max,
  color,
}: {
  label: string;
  sub: string;
  count: number;
  max: number;
  color: 'amber' | 'blue';
}) {
  const dot = color === 'amber' ? 'bg-amber-500' : 'bg-blue-500';
  const ratio = max === 0 ? 0 : Math.max(0, Math.min(1, count / max));
  return (
    <div className="rounded-md border border-slate-800 bg-slate-950 p-3">
      <div className="flex items-center gap-2 text-slate-200">
        <span className={`inline-block h-3 w-3 rounded-full ${dot}`} />
        <span className="font-semibold">{label}</span>
        <span className="text-slate-500">— {sub}</span>
      </div>
      <div className="mt-2 flex items-center gap-2">
        <span className="font-mono text-base text-slate-100">{count}</span>
        <span className="text-xs text-slate-500">von {max}</span>
        <div className="ml-auto h-1.5 w-24 overflow-hidden rounded-full bg-slate-800">
          <div
            className={`h-full ${color === 'amber' ? 'bg-amber-500' : 'bg-blue-500'}`}
            style={{ width: `${ratio * 100}%` }}
          />
        </div>
      </div>
    </div>
  );
}
