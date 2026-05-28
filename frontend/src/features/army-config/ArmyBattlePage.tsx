import {useMemo} from 'react';
import {Navigate, useNavigate} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import {useArmyBattleStore} from './armyBattleStore';
import {useArmyPlayer} from './useArmyPlayer';
import {reduceEvents} from '../battle-replay/state';
import HexGrid from '../battle-replay/HexGrid';
import EventLog from '../battle-replay/EventLog';
import PlaybackControls from '../battle-replay/PlaybackControls';
import LanguageSwitcher from '../../shared/ui/LanguageSwitcher';
import type {SideNames} from '../battle-replay/eventText';
import {useSimulateArmyBattle} from '../../shared/api/hooks';

export default function ArmyBattlePage() {
    const navigate = useNavigate();
    const {t} = useTranslation();
    const simulation = useArmyBattleStore((s) => s.simulation);
    const lastRequest = useArmyBattleStore((s) => s.lastRequest);
    const loadSimulation = useArmyBattleStore((s) => s.loadSimulation);
    const cursor = useArmyBattleStore((s) => s.cursor);
    const speedMs = useArmyBattleStore((s) => s.speedMs);
    const paused = useArmyBattleStore((s) => s.paused);
    const setSpeedMs = useArmyBattleStore((s) => s.setSpeedMs);
    const setPaused = useArmyBattleStore((s) => s.setPaused);
    const reset = useArmyBattleStore((s) => s.reset);
    const step = useArmyBattleStore((s) => s.step);

    const rematch = useSimulateArmyBattle((sim, request) => loadSimulation(sim, request));

    const {finished} = useArmyPlayer();

    const state = useMemo(
        () => (simulation ? reduceEvents(simulation.events, cursor) : null),
        [simulation, cursor],
    );

    const names = useMemo<SideNames>(() => {
        const bySlot = new Map<string, string>();
        let attackerFirst = t('army.attacker');
        let defenderFirst = t('army.defender');
        if (state) {
            for (const stack of state.stacks.values()) {
                bySlot.set(`${stack.side}-${stack.slot}`, stack.unitName);
                if (stack.side === 'ATTACKER' && stack.slot === 0) attackerFirst = stack.unitName;
                if (stack.side === 'DEFENDER' && stack.slot === 0) defenderFirst = stack.unitName;
            }
        }
        return {attacker: attackerFirst, defender: defenderFirst, bySlot};
    }, [state, t]);

    function startRematch() {
        if (!lastRequest) return;
        rematch.mutate({
            attacker: lastRequest.defender,
            defender: lastRequest.attacker,
            seed: lastRequest.seed ?? null,
        });
    }

    if (!simulation) {
        return <Navigate to="/army" replace/>;
    }
    if (!state) {
        return null;
    }

    return (
        <main className="mx-auto max-w-6xl space-y-4 p-4 md:p-8">
            <header className="flex items-center justify-between gap-4">
                <h1 className="text-2xl font-semibold text-slate-100">{t('army.battleTitle')}</h1>
                <div className="flex items-center gap-4">
                    <LanguageSwitcher/>
                    <button
                        type="button"
                        onClick={() => navigate('/army')}
                        className="text-sm text-slate-400 hover:text-amber-400"
                    >
                        {t('battle.backToConfig')}
                    </button>
                </div>
            </header>

            <PlaybackControls
                speedMs={speedMs}
                onSpeedChange={setSpeedMs}
                paused={paused}
                onTogglePaused={() => setPaused(!paused)}
                onRestart={reset}
                onStep={step}
                onRematch={lastRequest ? startRematch : undefined}
                rematchPending={rematch.isPending}
                finished={finished}
            />

            <section className="rounded-lg border border-slate-800 bg-slate-900 p-4">
                <HexGrid state={state} transitionMs={speedMs}/>
                <div className="mt-4 grid grid-cols-2 gap-4 text-sm">
                    <ArmySummary side="ATTACKER" state={state} color="amber"/>
                    <ArmySummary side="DEFENDER" state={state} color="blue"/>
                </div>
            </section>

            <section className="rounded-lg border border-slate-800 bg-slate-900 p-4">
                <h2 className="mb-2 text-lg font-semibold text-slate-100">{t('battle.combatLog')}</h2>
                <EventLog events={simulation.events} cursor={cursor} names={names}/>
            </section>
        </main>
    );
}

interface ArmySummaryProps {
    side: 'ATTACKER' | 'DEFENDER';
    state: import('../battle-replay/state').BattleState;
    color: 'amber' | 'blue';
}

function ArmySummary({side, state, color}: Readonly<ArmySummaryProps>) {
    const {t} = useTranslation();
    const stacks = [...state.stacks.values()].filter((s) => s.side === side);
    const headerText = color === 'amber' ? 'text-amber-300' : 'text-blue-300';
    const dot = color === 'amber' ? 'bg-amber-500' : 'bg-blue-500';
    return (
        <div className="rounded-md border border-slate-800 bg-slate-950 p-3">
            <div className="flex items-center gap-2">
                <span className={`inline-block h-3 w-3 rounded-full ${dot}`}/>
                <span className={`font-semibold ${headerText}`}>
                    {side === 'ATTACKER' ? t('army.attacker') : t('army.defender')}
                </span>
            </div>
            <ul className="mt-2 space-y-0.5 text-xs text-slate-300">
                {stacks.map((s) => {
                    const ratio = s.startCount === 0 ? 0 : Math.max(0, Math.min(1, s.count / s.startCount));
                    return (
                        <li key={`${s.side}-${s.slot}`} className="flex items-center gap-2">
                            <span className="w-32 truncate">{s.unitName}</span>
                            <span className="font-mono">{s.count}</span>
                            <span className="text-slate-500">/ {s.startCount}</span>
                            <div className="ml-auto h-1.5 w-16 overflow-hidden rounded-full bg-slate-800">
                                <div
                                    className={`h-full ${dot}`}
                                    style={{width: `${ratio * 100}%`}}
                                />
                            </div>
                        </li>
                    );
                })}
            </ul>
        </div>
    );
}
