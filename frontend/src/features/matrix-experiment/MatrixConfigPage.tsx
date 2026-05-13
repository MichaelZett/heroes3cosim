import type {SyntheticEvent} from 'react';
import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import {useFactions, useRunMatrix, useUnits} from '../../shared/api/hooks';
import type {Faction, StackSizingMode} from '../../shared/api/types';
import LanguageSwitcher from '../../shared/ui/LanguageSwitcher';
import ModeSwitcher from '../../shared/ui/ModeSwitcher';
import {useMatrixStore} from './matrixStore';

const TIERS = [1, 2, 3, 4, 5, 6, 7] as const;
const MODES: StackSizingMode[] = ['EQUAL_COUNT', 'EQUAL_GOLD', 'WEEKLY_PRODUCTION', 'EQUAL_GOLD_WEEKLY'];

export default function MatrixConfigPage() {
    const navigate = useNavigate();
    const {t} = useTranslation();
    const loadReport = useMatrixStore((s) => s.loadReport);
    const form = useMatrixStore((s) => s.form);
    const setForm = useMatrixStore((s) => s.setForm);

    const unitsQuery = useUnits();
    const factionsQuery = useFactions();

    const excludedFactions = useMemo(() => new Set(form.excludedFactions), [form.excludedFactions]);
    const excludedTiers = useMemo(() => new Set(form.excludedTiers), [form.excludedTiers]);
    const excludedUnits = useMemo(() => new Set(form.excludedUnits), [form.excludedUnits]);

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
        setForm({
            excludedFactions: excludedFactions.has(faction)
                ? form.excludedFactions.filter((f) => f !== faction)
                : [...form.excludedFactions, faction],
        });
    }

    function toggleTier(tier: number) {
        setForm({
            excludedTiers: excludedTiers.has(tier)
                ? form.excludedTiers.filter((x) => x !== tier)
                : [...form.excludedTiers, tier],
        });
    }

    function toggleUnit(name: string) {
        setForm({
            excludedUnits: excludedUnits.has(name)
                ? form.excludedUnits.filter((u) => u !== name)
                : [...form.excludedUnits, name],
        });
    }

    function handleSubmit(e: SyntheticEvent) {
        e.preventDefault();
        runMatrix.mutate({
            unitCount: form.unitCount,
            seedsPerMatchup: form.seedsPerMatchup,
            excludeFactions: form.excludedFactions,
            excludeTiers: form.excludedTiers,
            excludeUnits: form.excludedUnits,
            mode: form.mode,
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
    const totalSims = totalMatchups * form.seedsPerMatchup * 2;

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
                {t('matrix.unitCountLabel')}: <span className="font-mono">{form.unitCount}</span>
              </span>
                            <input
                                type="range"
                                min={1}
                                max={200}
                                value={form.unitCount}
                                onChange={(e) => setForm({unitCount: Number(e.target.value)})}
                                className="mt-1 w-full accent-amber-500"
                            />
                            <span className="text-xs text-slate-500">{t('matrix.unitCountHint')}</span>
                        </label>
                        <label className="block">
              <span className="text-sm text-slate-400">
                {t('matrix.seedsLabel')}: <span className="font-mono">{form.seedsPerMatchup}</span>
              </span>
                            <input
                                type="range"
                                min={1}
                                max={100}
                                value={form.seedsPerMatchup}
                                onChange={(e) => setForm({seedsPerMatchup: Number(e.target.value)})}
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
                                        form.mode === m
                                            ? 'border-amber-500 bg-amber-500/10 text-amber-100'
                                            : 'border-slate-800 bg-slate-950 text-slate-200 hover:border-slate-700'
                                    }`}
                                >
                                    <input
                                        type="radio"
                                        name="sizing-mode"
                                        value={m}
                                        checked={form.mode === m}
                                        onChange={() => setForm({mode: m})}
                                        className="mt-1 accent-amber-500"
                                    />
                                    <span className="block font-medium">{t(`matrix.mode.${m}.label`)}</span>
                                    <span
                                        className="block text-xs text-slate-500">{t(`matrix.mode.${m}.hint`)}</span>
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
                                setForm({
                                    excludedUnits: visibleUnits.filter((u) => u.upgrade).map((u) => u.name),
                                })
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
                                setForm({excludedUnits: basicsWithUpgradePeer.map((u) => u.name)});
                            }}
                            className="rounded-md border border-slate-700 px-3 py-1.5 text-xs text-slate-200 hover:border-amber-500 hover:text-amber-400"
                        >
                            {t('matrix.bulkExcludeBasics')}
                        </button>
                        <button
                            type="button"
                            onClick={() => setForm({excludedUnits: []})}
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
                        {t('matrix.failed', {message: runMatrix.error.message})}
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
