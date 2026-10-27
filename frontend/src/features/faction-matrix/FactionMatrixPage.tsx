import type { SyntheticEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useRunFactionMatrix } from '../../shared/api/hooks';
import { MAGIC_TERRAINS, TERRAINS, TOWNS } from '../../shared/api/catalog';
import type { Faction, MagicTerrain, SolverKind, Terrain } from '../../shared/api/types';
import LanguageSwitcher from '../../shared/ui/LanguageSwitcher';
import ModeSwitcher from '../../shared/ui/ModeSwitcher';
import SolverMatrixView from './SolverMatrixView';
import { useFactionMatrixStore } from './factionMatrixStore';

const SOLVERS: SolverKind[] = ['GREEDY', 'STRATEGIC'];
const FIELD_CLASS =
  'mt-1 w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 focus:border-amber-500 focus:outline-none disabled:text-slate-500';
const CHOICE_CLASS =
  'flex items-center gap-2 rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-200 hover:border-slate-700';

function toggled<T>(list: T[], value: T): T[] {
  return list.includes(value) ? list.filter((v) => v !== value) : [...list, value];
}

/**
 * Faction-Matrix: die Wochenproduktion jeder Stadt gegen jede, in beiden Rollen, je Solver.
 * Einstellungen und Ergebnis auf einer Seite; das Ergebnis bleibt bis zum nächsten Lauf stehen.
 */
export default function FactionMatrixPage() {
  const { t } = useTranslation();
  const form = useFactionMatrixStore((s) => s.form);
  const setForm = useFactionMatrixStore((s) => s.setForm);
  const report = useFactionMatrixStore((s) => s.report);
  const loadReport = useFactionMatrixStore((s) => s.loadReport);
  const run = useRunFactionMatrix((result) => loadReport(result));

  const factions = TOWNS.filter((f) => form.factions.includes(f));
  const solvers = SOLVERS.filter((s) => form.solvers.includes(s));
  const sims = factions.length * factions.length * form.seedsPerPair * solvers.length;
  const valid = factions.length >= 2 && solvers.length > 0;

  function handleSubmit(e: SyntheticEvent) {
    e.preventDefault();
    run.mutate({
      factions,
      solvers,
      seedsPerPair: form.seedsPerPair,
      weeks: form.weeks,
      heroes: form.heroes,
      terrain: form.terrain,
      magicTerrain: form.terrain === 'WATER' ? null : form.magicTerrain,
    });
  }

  return (
    <main className="mx-auto max-w-6xl space-y-6 p-4 md:p-8">
      <header className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-slate-100">{t('factionMatrix.title')}</h1>
          <p className="mt-2 text-slate-400">{t('factionMatrix.subtitle')}</p>
        </div>
        <LanguageSwitcher />
      </header>

      <ModeSwitcher current="factionMatrix" />

      <form onSubmit={handleSubmit} className="space-y-6">
        <section className="space-y-4 rounded-lg border border-slate-800 bg-slate-900 p-6">
          <h2 className="text-lg font-semibold text-slate-100">{t('matrix.parameters')}</h2>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="block">
              <span className="text-sm text-slate-400">
                {t('factionMatrix.seedsLabel')}:{' '}
                <span className="font-mono">{form.seedsPerPair}</span>
              </span>
              <input
                type="range"
                min={1}
                max={100}
                value={form.seedsPerPair}
                onChange={(e) => setForm({ seedsPerPair: Number(e.target.value) })}
                className="mt-1 w-full accent-amber-500"
              />
              <span className="text-xs text-slate-500">{t('factionMatrix.seedsHint')}</span>
            </label>
            <label className="block">
              <span className="text-sm text-slate-400">{t('factionMatrix.weeksLabel')}</span>
              <input
                type="number"
                min={1}
                max={20}
                value={form.weeks}
                onChange={(e) =>
                  setForm({
                    weeks: Math.min(20, Math.max(1, Math.round(Number(e.target.value) || 1))),
                  })
                }
                className={FIELD_CLASS}
              />
              <span className="text-xs text-slate-500">{t('factionMatrix.weeksHint')}</span>
            </label>
            <select
              aria-label={t('army.terrainTitle')}
              className={FIELD_CLASS}
              value={form.terrain ?? ''}
              onChange={(e) =>
                setForm({ terrain: e.target.value === '' ? null : (e.target.value as Terrain) })
              }
            >
              <option value="">{t('army.terrainNone')}</option>
              {TERRAINS.map((terrain) => (
                <option key={terrain} value={terrain}>
                  {t(`terrain.${terrain}`)}
                </option>
              ))}
            </select>
            <select
              aria-label={t('army.magicTerrain')}
              className={FIELD_CLASS}
              value={form.terrain === 'WATER' ? '' : (form.magicTerrain ?? '')}
              disabled={form.terrain === 'WATER'}
              onChange={(e) =>
                setForm({
                  magicTerrain: e.target.value === '' ? null : (e.target.value as MagicTerrain),
                })
              }
            >
              <option value="">{t('army.magicTerrainNone')}</option>
              {MAGIC_TERRAINS.map((magic) => (
                <option key={magic} value={magic}>
                  {t(`magicTerrain.${magic}`)}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-wrap gap-2">
            {SOLVERS.map((solver) => (
              <label key={solver} className={CHOICE_CLASS}>
                <input
                  type="checkbox"
                  checked={form.solvers.includes(solver)}
                  onChange={() => setForm({ solvers: toggled(form.solvers, solver) })}
                  className="accent-amber-500"
                />
                {t(`solver.${solver}`)}
              </label>
            ))}
            <label className={CHOICE_CLASS}>
              <input
                type="checkbox"
                checked={form.heroes}
                onChange={() => setForm({ heroes: !form.heroes })}
                className="accent-amber-500"
              />
              {t('factionMatrix.heroes')}
            </label>
          </div>
          <p className="text-sm text-slate-400">
            {t('factionMatrix.scopeSummary', { factions: factions.length, sims })}
          </p>
        </section>

        <section className="space-y-3 rounded-lg border border-slate-800 bg-slate-900 p-6">
          <h2 className="text-lg font-semibold text-slate-100">
            {t('matrix.excludeFactionsTitle')}
          </h2>
          <div className="grid grid-cols-2 gap-2 md:grid-cols-5">
            {TOWNS.map((faction: Faction) => (
              <label key={faction} className={CHOICE_CLASS}>
                <input
                  type="checkbox"
                  checked={form.factions.includes(faction)}
                  onChange={() => setForm({ factions: toggled(form.factions, faction) })}
                  className="accent-amber-500"
                />
                {t(`faction.${faction}`)}
              </label>
            ))}
          </div>
        </section>

        {run.isError && (
          <p className="text-sm text-red-400">
            {t('matrix.failed', { message: run.error.message })}
          </p>
        )}

        {run.isPending && run.progress && run.progress.total > 0 && (
          <div
            role="status"
            aria-live="polite"
            className="rounded-md border border-amber-500/40 bg-amber-500/5 p-4"
          >
            <p className="text-sm text-amber-200">
              {t('matrix.progress', {
                completed: run.progress.completed,
                total: run.progress.total,
              })}
            </p>
            <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-slate-800">
              <div
                className="h-full bg-amber-500 transition-[width] duration-300"
                style={{
                  width: `${Math.min(100, (run.progress.completed / run.progress.total) * 100)}%`,
                }}
              />
            </div>
          </div>
        )}

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={run.isPending || !valid}
            className="rounded-md bg-amber-500 px-6 py-3 text-base font-semibold text-slate-950 transition hover:bg-amber-400 disabled:cursor-not-allowed disabled:bg-slate-700 disabled:text-slate-400"
          >
            {run.isPending ? t('matrix.running') : t('matrix.start')}
          </button>
        </div>
      </form>

      {report && (
        <div className="space-y-4">
          <p className="text-sm text-slate-400">
            {t('factionMatrix.resultSummary', {
              factions: report.factions.length,
              seeds: report.seedsPerPair,
              weeks: report.weeks,
              heroes: report.heroes ? t('factionMatrix.withHeroes') : t('factionMatrix.noHeroes'),
              terrain: report.terrain
                ? t(`terrain.${report.terrain}`)
                : t('factionMatrix.noTerrain'),
              elapsed: (report.elapsedMs / 1000).toFixed(1),
            })}
            {report.magicTerrain && ` · ${t(`magicTerrain.${report.magicTerrain}`)}`}
          </p>
          <div className="grid gap-4 lg:grid-cols-2">
            {report.matrices.map((matrix) => (
              <SolverMatrixView key={matrix.solver} matrix={matrix} factions={report.factions} />
            ))}
          </div>
        </div>
      )}
    </main>
  );
}
