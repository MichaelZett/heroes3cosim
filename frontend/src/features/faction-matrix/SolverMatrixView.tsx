import { useTranslation } from 'react-i18next';
import type { Faction, FactionPairing, SolverMatrix } from '../../shared/api/types';

const ABBREVIATIONS: Partial<Record<Faction, string>> = {
  CASTLE: 'CAS',
  RAMPART: 'RAM',
  TOWER: 'TOW',
  INFERNO: 'INF',
  NECROPOLIS: 'NEC',
  DUNGEON: 'DUN',
  STRONGHOLD: 'STR',
  FORTRESS: 'FOR',
  CONFLUX: 'CON',
};

function percent(value: number): string {
  return `${(value * 100).toFixed(0)}%`;
}

/** Hintergrund je Siegquote: rot unter, grün über der Hälfte; der Spiegel neutral. */
function cellClass(cell: FactionPairing): string {
  if (cell.faction === cell.opponent) return 'bg-slate-800 text-slate-300 italic';
  if (cell.winRate >= 0.75) return 'bg-emerald-700/60 text-emerald-50';
  if (cell.winRate >= 0.55) return 'bg-emerald-900/50 text-emerald-100';
  if (cell.winRate > 0.45) return 'bg-slate-900 text-slate-200';
  if (cell.winRate > 0.25) return 'bg-red-900/50 text-red-100';
  return 'bg-red-700/60 text-red-50';
}

/** Rangliste und Matrix eines Solvers. */
export default function SolverMatrixView({
  matrix,
  factions,
}: Readonly<{ matrix: SolverMatrix; factions: Faction[] }>) {
  const { t } = useTranslation();
  const byKey = new Map(matrix.pairings.map((p) => [`${p.faction}|${p.opponent}`, p]));
  return (
    <section className="space-y-4 rounded-lg border border-slate-800 bg-slate-900 p-4">
      <header className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-lg font-semibold text-slate-100">{t(`solver.${matrix.solver}`)}</h2>
        <p className="text-xs text-slate-400">
          {t('factionMatrix.solverSummary', {
            bias: matrix.mirrorSideBias.toFixed(3),
            elapsed: (matrix.elapsedMs / 1000).toFixed(1),
          })}
        </p>
      </header>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-800 text-left text-xs uppercase tracking-wide text-slate-500">
              <th className="px-2 py-2">#</th>
              <th className="px-2 py-2">{t('matrix.thFaction')}</th>
              <th className="px-2 py-2 text-right">{t('matrix.thWlD')}</th>
              <th className="px-2 py-2 text-right">{t('matrix.thWinRate')}</th>
              <th className="px-2 py-2 text-right">{t('matrix.thAvgSurvivors')}</th>
            </tr>
          </thead>
          <tbody>
            {matrix.standings.map((row, index) => (
              <tr key={row.faction} className="border-b border-slate-900 text-slate-200">
                <td className="px-2 py-1.5 font-mono text-slate-500">{index + 1}</td>
                <td className="px-2 py-1.5">{t(`faction.${row.faction}`)}</td>
                <td className="px-2 py-1.5 text-right font-mono text-slate-300">
                  {row.wins}/{row.losses}/{row.draws}
                </td>
                <td className="px-2 py-1.5 text-right font-mono">
                  {(row.winRate * 100).toFixed(1)}%
                </td>
                <td className="px-2 py-1.5 text-right font-mono">
                  {(row.avgSurvivorRatio * 100).toFixed(1)}%
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="overflow-x-auto">
        <table
          className="text-xs"
          aria-label={t('factionMatrix.matrixLabel', { solver: t(`solver.${matrix.solver}`) })}
        >
          <thead>
            <tr>
              <th className="px-1 py-1" />
              {factions.map((f) => (
                <th
                  key={f}
                  className="px-1 py-1 font-mono text-slate-400"
                  title={t(`faction.${f}`)}
                >
                  {ABBREVIATIONS[f] ?? f}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {factions.map((row) => (
              <tr key={row}>
                <th
                  className="px-1 py-1 text-left font-mono text-slate-400"
                  title={t(`faction.${row}`)}
                >
                  {ABBREVIATIONS[row] ?? row}
                </th>
                {factions.map((column) => {
                  const cell = byKey.get(`${row}|${column}`);
                  return cell ? (
                    <td
                      key={column}
                      className={`min-w-11 px-1.5 py-1 text-center font-mono ${cellClass(cell)}`}
                      title={t('factionMatrix.cellTitle', {
                        faction: t(`faction.${row}`),
                        opponent: t(`faction.${column}`),
                        wins: cell.wins,
                        losses: cell.losses,
                        draws: cell.draws,
                        rounds: cell.avgRounds.toFixed(1),
                      })}
                    >
                      {percent(cell.winRate)}
                    </td>
                  ) : (
                    <td key={column} />
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="text-xs text-slate-500">{t('factionMatrix.matrixHint')}</p>
    </section>
  );
}
