import type {FormEvent} from 'react';
import {useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import {useFactions, useRunMatrix, useUnits} from '../../shared/api/hooks';
import type {Faction, StackSizingMode} from '../../shared/api/types';
import LanguageSwitcher from '../../shared/ui/LanguageSwitcher';
import ModeSwitcher from '../../shared/ui/ModeSwitcher';
import {useMatrixStore} from './matrixStore';

export default function MatrixConfigPage() {
    const navigate = useNavigate();
    const {t} = useTranslation();
    const loadReport = useMatrixStore((s) => s.loadReport);

    const unitsQuery = useUnits();
    const factionsQuery = useFactions();

    const [unitCount, setUnitCount] = useState(20);
    const [seedsPerMatchup, setSeedsPerMatchup] = useState(20);
    const [excludedFactions, setExcludedFactions] = useState<Set<Faction>>(new Set());
    const [excludedTiers, setExcludedTiers] = useState<Set<number>>(new Set());
    const [excludedUnits, setExcludedUnits] = useState<Set<string>>(new Set());
    const [mode, setMode] = useState<StackSizingMode>('EQUAL_COUNT');
    const TIERS = [1, 2, 3, 4, 5, 6, 7] as const;
    const MODES: StackSizingMode[] = ['EQUAL_COUNT', 'EQUAL_GOLD', 'WEEKLY_PRODUCTION', 'EQUAL_GOLD_WEEKLY'];

    const runMatrix = useRunMatrix((report, request) => {
        loadReport(report, request);
        navigate('/matrix/result');
    });

    const visibleUnits = useMemo(() => {
        if (!unitsQuery.data) return [];
        return unitsQuery.data
            .filter((u) => !excludedFactions.has(u.faction))
            .filter((u) => !excludedTiers.has(u.tier))
            .sort((a, b) => a.name.localeCompare(b.name));
    }, [unitsQuery.data, excludedFactions, excludedTiers]);

    function toggleFaction(faction: Faction) {
        setExcludedFactions((prev) => {
            const next = new Set(prev);
            if (next.has(faction)) next.delete(faction);
            else next.add(faction);
            return next;
        });
    }

    function toggleTier(tier: number) {
        setExcludedTiers((prev) => {
            const next = new Set(prev);
            if (next.has(tier)) next.delete(tier);
            else next.add(tier);
            return next;
        });
    }

    function toggleUnit(name: string) {
        setExcludedUnits((prev) => {
            const next = new Set(prev);
            if (next.has(name)) next.delete(name);
            else next.add(name);
            return next;
        });
    }

    function handleSubmit(e: FormEvent) {
        e.preventDefault();
        runMatrix.mutate({
            unitCount,
            seedsPerMatchup,
            excludeFactions: Array.from(excludedFactions),
            excludeTiers: Array.from(excludedTiers),
            excludeUnits: Array.from(excludedUnits),
            mode,
        });
    }

    if (unitsQuery.isPending || factionsQuery.isPending) {
        return (
            <main className="flex min-h-screen items-center justify-center p-8">
                <p className="text-slate-400">{t('config.loading')}</p>
            </main>
        );
    }
    if (unitsQuery.isError || factionsQuery.isError) {
        return (
            <main className="flex min-h-screen items-center justify-center p-8">
                <p className="text-red-400">{t('config.apiDown', {url: 'localhost:8080'})}</p>
            </main>
        );
    }

    const eligibleUnitCount = visibleUnits.length - excludedUnits.size;
    const totalMatchups = eligibleUnitCount > 1 ? (eligibleUnitCount * (eligibleUnitCount - 1)) / 2 : 0;
    const totalSims = totalMatchups * seedsPerMatchup * 2;

    return (
        <main className="mx-auto max-w-5xl space-y-6 p-8">
            <header className="flex items-start justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-slate-100">{t('matrix.title')}</h1>
                    <p className="mt-2 text-slate-400">{t('matrix.subtitle')}</p>
                </div>
                <LanguageSwitcher/>
            </header>

            <ModeSwitcher current="matrix"/>

            <form onSubmit={handleSubmit} className="space-y-6">
                <section className="rounded-lg border border-slate-800 bg-slate-900 p-6 space-y-4">
                    <h2 className="text-lg font-semibold text-slate-100">{t('matrix.parameters')}</h2>
                    <div className="grid gap-4 md:grid-cols-2">
                        <label className="block">
              <span className="text-sm text-slate-400">
                {t('matrix.unitCountLabel')}: <span className="font-mono">{unitCount}</span>
              </span>
                            <input
                                type="range"
                                min={1}
                                max={200}
                                value={unitCount}
                                onChange={(e) => setUnitCount(Number(e.target.value))}
                                className="mt-1 w-full accent-amber-500"
                            />
                            <span className="text-xs text-slate-500">{t('matrix.unitCountHint')}</span>
                        </label>
                        <label className="block">
              <span className="text-sm text-slate-400">
                {t('matrix.seedsLabel')}: <span className="font-mono">{seedsPerMatchup}</span>
              </span>
                            <input
                                type="range"
                                min={1}
                                max={100}
                                value={seedsPerMatchup}
                                onChange={(e) => setSeedsPerMatchup(Number(e.target.value))}
                                className="mt-1 w-full accent-amber-500"
                            />
                            <span className="text-xs text-slate-500">{t('matrix.seedsHint')}</span>
                        </label>
                    </div>
                    <p className="text-sm text-slate-400">
                        {t('matrix.scopeSummary', {
                            units: eligibleUnitCount,
                            matchups: totalMatchups,
                            sims: totalSims,
                        })}
                    </p>
                    <fieldset className="space-y-2">
                        <legend className="text-sm font-medium text-slate-300">{t('matrix.modeTitle')}</legend>
                        <div className="grid gap-2 md:grid-cols-2">
                            {MODES.map((m) => (
                                <label
                                    key={m}
                                    className={`flex cursor-pointer items-start gap-2 rounded-md border px-3 py-2 text-sm ${
                                        mode === m
                                            ? 'border-amber-500 bg-amber-500/10 text-amber-100'
                                            : 'border-slate-800 bg-slate-950 text-slate-200 hover:border-slate-700'
                                    }`}
                                >
                                    <input
                                        type="radio"
                                        name="sizing-mode"
                                        value={m}
                                        checked={mode === m}
                                        onChange={() => setMode(m)}
                                        className="mt-1 accent-amber-500"
                                    />
                                    <span>
                                        <span className="block font-medium">{t(`matrix.mode.${m}.label`)}</span>
                                        <span
                                            className="block text-xs text-slate-500">{t(`matrix.mode.${m}.hint`)}</span>
                                    </span>
                                </label>
                            ))}
                        </div>
                    </fieldset>
                </section>

                <section className="rounded-lg border border-slate-800 bg-slate-900 p-6 space-y-3">
                    <h2 className="text-lg font-semibold text-slate-100">{t('matrix.excludeFactionsTitle')}</h2>
                    <div className="grid grid-cols-2 gap-2 md:grid-cols-4">
                        {factionsQuery.data.map((faction) => (
                            <label
                                key={faction}
                                className="flex items-center gap-2 rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-200 hover:border-slate-700"
                            >
                                <input
                                    type="checkbox"
                                    checked={!excludedFactions.has(faction)}
                                    onChange={() => toggleFaction(faction)}
                                    className="accent-amber-500"
                                />
                                {t(`faction.${faction}`)}
                            </label>
                        ))}
                    </div>
                </section>

                <section className="rounded-lg border border-slate-800 bg-slate-900 p-6 space-y-3">
                    <h2 className="text-lg font-semibold text-slate-100">{t('matrix.excludeTiersTitle')}</h2>
                    <p className="text-xs text-slate-500">{t('matrix.excludeTiersHint')}</p>
                    <div className="grid grid-cols-4 gap-2 md:grid-cols-7">
                        {TIERS.map((tier) => (
                            <label
                                key={tier}
                                className="flex items-center gap-2 rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-200 hover:border-slate-700"
                            >
                                <input
                                    type="checkbox"
                                    checked={!excludedTiers.has(tier)}
                                    onChange={() => toggleTier(tier)}
                                    className="accent-amber-500"
                                />
                                {t('common.tierN', {tier})}
                            </label>
                        ))}
                    </div>
                </section>

                <section className="rounded-lg border border-slate-800 bg-slate-900 p-6 space-y-3">
                    <h2 className="text-lg font-semibold text-slate-100">{t('matrix.excludeUnitsTitle')}</h2>
                    <p className="text-xs text-slate-500">{t('matrix.excludeUnitsHint')}</p>
                    <div className="flex flex-wrap gap-2">
                        <button
                            type="button"
                            onClick={() =>
                                setExcludedUnits(
                                    new Set(visibleUnits.filter((u) => u.upgrade).map((u) => u.name)),
                                )
                            }
                            className="rounded-md border border-slate-700 px-3 py-1.5 text-xs text-slate-200 hover:border-amber-500 hover:text-amber-400"
                        >
                            {t('matrix.bulkExcludeUpgrades')}
                        </button>
                        <button
                            type="button"
                            onClick={() => {
                                const basicsWithUpgradePeer = visibleUnits.filter(
                                    (u) =>
                                        !u.upgrade &&
                                        visibleUnits.some(
                                            (other) =>
                                                other.faction === u.faction && other.tier === u.tier && other.upgrade,
                                        ),
                                );
                                setExcludedUnits(new Set(basicsWithUpgradePeer.map((u) => u.name)));
                            }}
                            className="rounded-md border border-slate-700 px-3 py-1.5 text-xs text-slate-200 hover:border-amber-500 hover:text-amber-400"
                        >
                            {t('matrix.bulkExcludeBasics')}
                        </button>
                        <button
                            type="button"
                            onClick={() => setExcludedUnits(new Set())}
                            className="rounded-md border border-slate-700 px-3 py-1.5 text-xs text-slate-200 hover:border-amber-500 hover:text-amber-400"
                        >
                            {t('matrix.bulkClear')}
                        </button>
                    </div>
                    <div
                        className="grid max-h-64 grid-cols-2 gap-1 overflow-y-auto rounded-md border border-slate-800 bg-slate-950 p-3 md:grid-cols-3 lg:grid-cols-4">
                        {visibleUnits.map((unit) => (
                            <label
                                key={unit.name}
                                className="flex items-center gap-2 px-2 py-1 text-xs text-slate-200 hover:bg-slate-900"
                            >
                                <input
                                    type="checkbox"
                                    checked={!excludedUnits.has(unit.name)}
                                    onChange={() => toggleUnit(unit.name)}
                                    className="accent-amber-500"
                                />
                                {unit.name}
                                {unit.upgrade && <span className="text-emerald-400">★</span>}
                            </label>
                        ))}
                    </div>
                </section>

                {runMatrix.isError && (
                    <p className="text-sm text-red-400">
                        {t('matrix.failed', {message: (runMatrix.error as Error).message})}
                    </p>
                )}

                {runMatrix.isPending && runMatrix.progress && runMatrix.progress.total > 0 && (
                    <div
                        role="status"
                        aria-live="polite"
                        className="rounded-md border border-amber-500/40 bg-amber-500/5 p-4"
                    >
                        <p className="text-sm text-amber-200">
                            {t('matrix.progress', {
                                completed: runMatrix.progress.completed,
                                total: runMatrix.progress.total,
                            })}
                        </p>
                        <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-slate-800">
                            <div
                                className="h-full bg-amber-500 transition-[width] duration-300"
                                style={{
                                    width: `${Math.min(100, (runMatrix.progress.completed / runMatrix.progress.total) * 100)}%`,
                                }}
                            />
                        </div>
                    </div>
                )}

                <div className="flex justify-end">
                    <button
                        type="submit"
                        disabled={runMatrix.isPending || totalMatchups === 0}
                        className="rounded-md bg-amber-500 px-6 py-3 text-base font-semibold text-slate-950 transition hover:bg-amber-400 disabled:cursor-not-allowed disabled:bg-slate-700 disabled:text-slate-400"
                    >
                        {runMatrix.isPending ? t('matrix.running') : t('matrix.start')}
                    </button>
                </div>
            </form>
        </main>
    );
}
