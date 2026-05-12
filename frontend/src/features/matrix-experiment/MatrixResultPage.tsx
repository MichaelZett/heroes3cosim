import {useMemo, useState} from 'react';
import {Navigate, useNavigate} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import LanguageSwitcher from '../../shared/ui/LanguageSwitcher';
import ModeSwitcher from '../../shared/ui/ModeSwitcher';
import type {UnitMatchupStats} from '../../shared/api/types';
import {useMatrixStore} from './matrixStore';

type SortKey = 'winRate' | 'avgSurvivorRatio' | 'tier' | 'unitName';

export default function MatrixResultPage() {
    const navigate = useNavigate();
    const {t} = useTranslation();
    const report = useMatrixStore((s) => s.report);
    const [sortKey, setSortKey] = useState<SortKey>('winRate');
    const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');
    const [filterText, setFilterText] = useState('');

    const sortedStats = useMemo(() => {
        if (!report) return [];
        const filtered = filterText
            ? report.stats.filter((s) => s.unitName.toLowerCase().includes(filterText.toLowerCase()))
            : report.stats;
        const dir = sortDir === 'desc' ? -1 : 1;
        return [...filtered].sort((a, b) => {
            const av = a[sortKey];
            const bv = b[sortKey];
            if (typeof av === 'string' && typeof bv === 'string') return av.localeCompare(bv) * dir;
            return ((av as number) - (bv as number)) * dir;
        });
    }, [report, sortKey, sortDir, filterText]);

    if (!report) {
        return <Navigate to="/matrix" replace/>;
    }

    function toggleSort(key: SortKey) {
        if (sortKey === key) {
            setSortDir(sortDir === 'asc' ? 'desc' : 'asc');
        } else {
            setSortKey(key);
            setSortDir(key === 'unitName' || key === 'tier' ? 'asc' : 'desc');
        }
    }

    function arrow(key: SortKey) {
        if (sortKey !== key) return '';
        return sortDir === 'asc' ? ' ↑' : ' ↓';
    }

    return (
        <main className="mx-auto max-w-6xl space-y-6 p-4 md:p-8">
            <header className="flex items-start justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-slate-100">{t('matrix.resultTitle')}</h1>
                    <p className="mt-2 text-sm text-slate-400">
                        {t('matrix.resultSummary', {
                            units: report.stats.length,
                            matchups: report.totalMatchups,
                            seeds: report.seedsPerMatchup,
                            elapsed: (report.elapsedMs / 1000).toFixed(1),
                        })}
                    </p>
                </div>
                <div className="flex items-center gap-4">
                    <LanguageSwitcher/>
                    <button
                        type="button"
                        onClick={() => navigate('/matrix')}
                        className="text-sm text-slate-400 hover:text-amber-400"
                    >
                        {t('matrix.backToConfig')}
                    </button>
                </div>
            </header>

            <ModeSwitcher current="matrix"/>

            {report.anomalies.length > 0 && (
                <section className="rounded-lg border border-amber-500/40 bg-amber-500/5 p-6">
                    <h2 className="text-lg font-semibold text-amber-300">{t('matrix.anomaliesTitle')}</h2>
                    <p className="mt-1 text-sm text-amber-200/80">{t('matrix.anomaliesHint')}</p>
                    <ul className="mt-3 space-y-1 text-sm">
                        {report.anomalies.map((a) => (
                            <li key={a.unitName} className="text-slate-200">
                                <span className="font-semibold text-amber-300">{a.unitName}</span>{' '}
                                <span className="text-slate-400">
                  {t('matrix.anomalyLine', {
                      tier: a.tier,
                      againstTier: a.againstTier,
                      winRate: (a.winRate * 100).toFixed(1),
                      sample: a.sampleSize,
                  })}
                </span>
                            </li>
                        ))}
                    </ul>
                </section>
            )}

            {report.factionStats.length > 0 && (
                <section className="rounded-lg border border-slate-800 bg-slate-900 p-4">
                    <h2 className="mb-3 text-lg font-semibold text-slate-100">{t('matrix.factionStatsTitle')}</h2>
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                            <thead>
                            <tr className="border-b border-slate-800 text-left text-xs uppercase tracking-wide text-slate-500">
                                <th className="px-2 py-2">{t('matrix.thFaction')}</th>
                                <th className="px-2 py-2 text-right">{t('matrix.thFactionUnits')}</th>
                                <th className="px-2 py-2 text-right">{t('matrix.thWlD')}</th>
                                <th className="px-2 py-2 text-right">{t('matrix.thWinRate')}</th>
                                <th className="px-2 py-2 text-right">{t('matrix.thAvgSurvivors')}</th>
                            </tr>
                            </thead>
                            <tbody>
                            {report.factionStats.map((row) => (
                                <tr key={row.faction} className="border-b border-slate-900 text-slate-200">
                                    <td className="px-2 py-1.5">{t(`faction.${row.faction}`)}</td>
                                    <td className="px-2 py-1.5 text-right font-mono">{row.unitCount}</td>
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
                </section>
            )}

            <section className="rounded-lg border border-slate-800 bg-slate-900 p-4">
                <div className="mb-3 flex items-center justify-between gap-3">
                    <h2 className="text-lg font-semibold text-slate-100">{t('matrix.statsTitle')}</h2>
                    <input
                        type="search"
                        value={filterText}
                        onChange={(e) => setFilterText(e.target.value)}
                        placeholder={t('matrix.filterPlaceholder')}
                        className="rounded-md border border-slate-700 bg-slate-950 px-3 py-1.5 text-sm text-slate-100 focus:border-amber-500 focus:outline-none"
                    />
                </div>
                <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                        <thead>
                        <tr className="border-b border-slate-800 text-left text-xs uppercase tracking-wide text-slate-500">
                            <th className="cursor-pointer px-2 py-2 hover:text-slate-300"
                                onClick={() => toggleSort('unitName')}>
                                {t('matrix.thUnit')}{arrow('unitName')}
                            </th>
                            <th className="px-2 py-2">{t('matrix.thFaction')}</th>
                            <th className="cursor-pointer px-2 py-2 hover:text-slate-300"
                                onClick={() => toggleSort('tier')}>
                                {t('matrix.thTier')}{arrow('tier')}
                            </th>
                            <th className="px-2 py-2 text-right">{t('matrix.thWlD')}</th>
                            <th className="cursor-pointer px-2 py-2 text-right hover:text-slate-300"
                                onClick={() => toggleSort('winRate')}>
                                {t('matrix.thWinRate')}{arrow('winRate')}
                            </th>
                            <th className="cursor-pointer px-2 py-2 text-right hover:text-slate-300"
                                onClick={() => toggleSort('avgSurvivorRatio')}>
                                {t('matrix.thAvgSurvivors')}{arrow('avgSurvivorRatio')}
                            </th>
                        </tr>
                        </thead>
                        <tbody>
                        {sortedStats.map((row) => (
                            <StatsRow key={row.unitName} row={row}/>
                        ))}
                        </tbody>
                    </table>
                </div>
            </section>
        </main>
    );
}

function StatsRow({row}: { row: UnitMatchupStats }) {
    const {t} = useTranslation();
    return (
        <tr className="border-b border-slate-900 text-slate-200 hover:bg-slate-800/40">
            <td className="px-2 py-1.5">
                {row.unitName}
                {row.upgrade && <span className="ml-1 text-emerald-400">★</span>}
            </td>
            <td className="px-2 py-1.5 text-slate-400">{t(`faction.${row.faction}`)}</td>
            <td className="px-2 py-1.5">{row.tier}</td>
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
    );
}
