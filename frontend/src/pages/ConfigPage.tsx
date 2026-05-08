import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import ArmySelector from '../components/ArmySelector';
import { useFactions, useSimulateBattle, useUnits } from '../api/hooks';
import type { Faction } from '../api/types';
import { useBattleStore } from '../store/battleStore';

export default function ConfigPage() {
  const navigate = useNavigate();
  const loadSimulation = useBattleStore((s) => s.loadSimulation);

  const unitsQuery = useUnits();
  const factionsQuery = useFactions();

  const [attackerFaction, setAttackerFaction] = useState<Faction | 'ALL'>('ALL');
  const [attackerUnit, setAttackerUnit] = useState('');
  const [attackerCount, setAttackerCount] = useState(50);
  const [defenderFaction, setDefenderFaction] = useState<Faction | 'ALL'>('ALL');
  const [defenderUnit, setDefenderUnit] = useState('');
  const [defenderCount, setDefenderCount] = useState(50);
  const [seedText, setSeedText] = useState('');

  const simulate = useSimulateBattle((sim) => {
    loadSimulation(sim);
    navigate('/battle');
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!attackerUnit || !defenderUnit) return;
    const seed = seedText.trim() === '' ? null : Number(seedText);
    simulate.mutate({
      attackerUnit,
      attackerCount,
      defenderUnit,
      defenderCount,
      seed: Number.isFinite(seed as number) ? (seed as number) : null,
    });
  }

  function rollSeed() {
    setSeedText(String(Math.floor(Math.random() * 1_000_000)));
  }

  if (unitsQuery.isPending || factionsQuery.isPending) {
    return <CenteredMessage>Lade Catalog…</CenteredMessage>;
  }
  if (unitsQuery.isError || factionsQuery.isError) {
    return (
      <CenteredMessage tone="error">
        API nicht erreichbar — läuft das Backend auf <code>localhost:8080</code>?
      </CenteredMessage>
    );
  }

  const submitDisabled = !attackerUnit || !defenderUnit || simulate.isPending;

  return (
    <main className="mx-auto max-w-5xl p-8">
      <header className="mb-8">
        <h1 className="text-3xl font-bold text-slate-100">Heroes 3 Combat Simulator</h1>
        <p className="mt-2 text-slate-400">
          Wähle zwei Truppen, lege optional einen Seed fest und starte den Kampf.
        </p>
      </header>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="grid gap-6 md:grid-cols-2">
          <ArmySelector
            title="Truppe 1 (Angreifer)"
            factions={factionsQuery.data}
            units={unitsQuery.data}
            selectedFaction={attackerFaction}
            selectedUnit={attackerUnit}
            count={attackerCount}
            onFactionChange={setAttackerFaction}
            onUnitChange={setAttackerUnit}
            onCountChange={setAttackerCount}
          />
          <ArmySelector
            title="Truppe 2 (Verteidiger)"
            factions={factionsQuery.data}
            units={unitsQuery.data}
            selectedFaction={defenderFaction}
            selectedUnit={defenderUnit}
            count={defenderCount}
            onFactionChange={setDefenderFaction}
            onUnitChange={setDefenderUnit}
            onCountChange={setDefenderCount}
          />
        </div>

        <section className="rounded-lg border border-slate-800 bg-slate-900 p-6">
          <h2 className="text-lg font-semibold text-slate-100">Seed</h2>
          <div className="mt-4 flex items-end gap-3">
            <label className="flex-1">
              <span className="text-sm text-slate-400">
                Optional — leer lassen für Zufalls-Seed
              </span>
              <input
                type="number"
                inputMode="numeric"
                placeholder="z. B. 42"
                className="mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none"
                value={seedText}
                onChange={(e) => setSeedText(e.target.value)}
              />
            </label>
            <button
              type="button"
              onClick={rollSeed}
              className="rounded-md border border-slate-700 px-3 py-2 text-sm text-slate-300 hover:border-amber-500 hover:text-amber-400"
            >
              Würfeln
            </button>
          </div>
        </section>

        {simulate.isError && (
          <p className="text-sm text-red-400">
            Simulation fehlgeschlagen: {(simulate.error as Error).message}
          </p>
        )}

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={submitDisabled}
            className="rounded-md bg-amber-500 px-6 py-3 text-base font-semibold text-slate-950 transition hover:bg-amber-400 disabled:cursor-not-allowed disabled:bg-slate-700 disabled:text-slate-400"
          >
            {simulate.isPending ? 'Simuliere…' : 'Kampf starten'}
          </button>
        </div>
      </form>
    </main>
  );
}

function CenteredMessage({
  children,
  tone = 'info',
}: {
  children: React.ReactNode;
  tone?: 'info' | 'error';
}) {
  return (
    <main className="flex min-h-screen items-center justify-center p-8">
      <p className={tone === 'error' ? 'text-red-400' : 'text-slate-400'}>{children}</p>
    </main>
  );
}
