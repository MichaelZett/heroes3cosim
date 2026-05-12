import type {FormEvent} from 'react';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import ArmySelector from './ArmySelector';
import LanguageSwitcher from '../../shared/ui/LanguageSwitcher';
import ModeSwitcher from '../../shared/ui/ModeSwitcher';
import {useFactions, useSimulateBattle, useUnits} from '../../shared/api/hooks';
import type {Faction} from '../../shared/api/types';
import {useBattleStore} from '../battle-replay/battleStore';

export default function ConfigPage() {
    const navigate = useNavigate();
    const {t} = useTranslation();
    const loadSimulation = useBattleStore((s) => s.loadSimulation);

    const unitsQuery = useUnits();
    const factionsQuery = useFactions();

    const [attackerFaction, setAttackerFaction] = useState<Faction | 'ALL'>('ALL');
    const [attackerTier, setAttackerTier] = useState<number | 'ALL'>('ALL');
    const [attackerUnit, setAttackerUnit] = useState('');
    const [attackerCount, setAttackerCount] = useState(50);
    const [defenderFaction, setDefenderFaction] = useState<Faction | 'ALL'>('ALL');
    const [defenderTier, setDefenderTier] = useState<number | 'ALL'>('ALL');
    const [defenderUnit, setDefenderUnit] = useState('');
    const [defenderCount, setDefenderCount] = useState(50);
    const [seedText, setSeedText] = useState('');

    const simulate = useSimulateBattle((sim, request) => {
        loadSimulation(sim, request);
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
        return <CenteredMessage>{t('config.loading')}</CenteredMessage>;
    }
    if (unitsQuery.isError || factionsQuery.isError) {
        return (
            <CenteredMessage tone="error">
                {t('config.apiDown', {url: 'localhost:8080'})}
            </CenteredMessage>
        );
    }

    const submitDisabled = !attackerUnit || !defenderUnit || simulate.isPending;

    return (
        <main className="mx-auto max-w-5xl p-8">
            <header className="mb-6 flex items-start justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-slate-100">{t('config.title')}</h1>
                    <p className="mt-2 text-slate-400">{t('config.subtitle')}</p>
                </div>
                <LanguageSwitcher/>
            </header>

            <div className="mb-6">
                <ModeSwitcher current="single"/>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
                <div className="grid gap-6 md:grid-cols-2">
                    <ArmySelector
                        title={t('config.attackerTitle')}
                        factions={factionsQuery.data}
                        units={unitsQuery.data}
                        selectedFaction={attackerFaction}
                        selectedTier={attackerTier}
                        selectedUnit={attackerUnit}
                        count={attackerCount}
                        onFactionChange={setAttackerFaction}
                        onTierChange={setAttackerTier}
                        onUnitChange={setAttackerUnit}
                        onCountChange={setAttackerCount}
                    />
                    <ArmySelector
                        title={t('config.defenderTitle')}
                        factions={factionsQuery.data}
                        units={unitsQuery.data}
                        selectedFaction={defenderFaction}
                        selectedTier={defenderTier}
                        selectedUnit={defenderUnit}
                        count={defenderCount}
                        onFactionChange={setDefenderFaction}
                        onTierChange={setDefenderTier}
                        onUnitChange={setDefenderUnit}
                        onCountChange={setDefenderCount}
                    />
                </div>

                <section className="rounded-lg border border-slate-800 bg-slate-900 p-6">
                    <h2 className="text-lg font-semibold text-slate-100">{t('config.seedTitle')}</h2>
                    <div className="mt-4 flex items-end gap-3">
                        <label className="flex-1">
                            <span className="text-sm text-slate-400">{t('config.seedHint')}</span>
                            <input
                                type="number"
                                inputMode="numeric"
                                placeholder={t('config.seedPlaceholder')}
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
                            {t('config.rollSeed')}
                        </button>
                    </div>
                </section>

                {simulate.isError && (
                    <p className="text-sm text-red-400">
                        {t('config.simulationFailed', {message: (simulate.error as Error).message})}
                    </p>
                )}

                <div className="flex justify-end">
                    <button
                        type="submit"
                        disabled={submitDisabled}
                        className="rounded-md bg-amber-500 px-6 py-3 text-base font-semibold text-slate-950 transition hover:bg-amber-400 disabled:cursor-not-allowed disabled:bg-slate-700 disabled:text-slate-400"
                    >
                        {simulate.isPending ? t('config.simulating') : t('config.startBattle')}
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
