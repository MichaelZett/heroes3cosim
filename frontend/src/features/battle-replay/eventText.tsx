import {Trans, useTranslation} from 'react-i18next';
import type {BattleEvent, Side} from '../../shared/api/types';

export interface SideNames {
    attacker: string;
    defender: string;
}

const SIDE_CLASS: Record<Side, string> = {
    ATTACKER: 'font-semibold text-amber-300',
    DEFENDER: 'font-semibold text-blue-300',
};

function sideName(side: Side, names: SideNames): string {
    return side === 'ATTACKER' ? names.attacker : names.defender;
}

function sideSpan(side: Side) {
    return <span className={SIDE_CLASS[side]}/>;
}

interface EventTextProps {
    event: BattleEvent;
    names: SideNames;
}

export function EventText({event, names}: EventTextProps) {
    const {t} = useTranslation();
    switch (event.type) {
        case 'BattleStart':
            return (
                <Trans
                    i18nKey="events.battleStart"
                    values={{
                        attacker: names.attacker,
                        attackerCount: event.attacker.count,
                        defender: names.defender,
                        defenderCount: event.defender.count,
                    }}
                    components={{actor: sideSpan('ATTACKER'), target: sideSpan('DEFENDER')}}
                />
            );
        case 'Move':
            return (
                <Trans
                    i18nKey="events.move"
                    values={{
                        actor: sideName(event.actor, names),
                        fromQ: event.fromQ,
                        fromR: event.fromR,
                        toQ: event.toQ,
                        toR: event.toR,
                    }}
                    components={{actor: sideSpan(event.actor)}}
                />
            );
        case 'MoveBack':
            return (
                <Trans
                    i18nKey="events.moveBack"
                    values={{actor: sideName(event.actor, names), toQ: event.toQ, toR: event.toR}}
                    components={{actor: sideSpan(event.actor)}}
                />
            );
        case 'Wait':
            return (
                <Trans
                    i18nKey="events.wait"
                    values={{actor: sideName(event.actor, names)}}
                    components={{actor: sideSpan(event.actor)}}
                />
            );
        case 'Shoot':
            return (
                <Trans
                    i18nKey="events.shoot"
                    values={{
                        actor: sideName(event.actor, names),
                        target: sideName(event.target, names),
                        distance: event.distance,
                        damage: event.damage,
                        killed: event.killed,
                    }}
                    components={{actor: sideSpan(event.actor), target: sideSpan(event.target)}}
                />
            );
        case 'Melee':
            return (
                <Trans
                    i18nKey="events.melee"
                    values={{
                        actor: sideName(event.actor, names),
                        target: sideName(event.target, names),
                        damage: event.damage,
                        killed: event.killed,
                    }}
                    components={{actor: sideSpan(event.actor), target: sideSpan(event.target)}}
                />
            );
        case 'Retaliation':
            return (
                <Trans
                    i18nKey="events.retaliation"
                    values={{
                        retaliator: sideName(event.retaliator, names),
                        damage: event.damage,
                        killed: event.killed,
                    }}
                    components={{retaliator: sideSpan(event.retaliator)}}
                />
            );
        case 'TwoBlows':
            return (
                <Trans
                    i18nKey="events.twoBlows"
                    values={{actor: sideName(event.actor, names)}}
                    components={{actor: sideSpan(event.actor)}}
                />
            );
        case 'TwoShots':
            return (
                <Trans
                    i18nKey="events.twoShots"
                    values={{actor: sideName(event.actor, names)}}
                    components={{actor: sideSpan(event.actor)}}
                />
            );
        case 'GoodMorale':
            return (
                <Trans
                    i18nKey="events.goodMorale"
                    values={{actor: sideName(event.actor, names)}}
                    components={{actor: sideSpan(event.actor)}}
                />
            );
        case 'DeathStare':
            return (
                <Trans
                    i18nKey="events.deathStare"
                    values={{
                        actor: sideName(event.actor, names),
                        target: sideName(event.target, names),
                        kills: event.kills,
                    }}
                    components={{actor: sideSpan(event.actor), target: sideSpan(event.target)}}
                />
            );
        case 'Thunderbolts':
            return (
                <Trans
                    i18nKey="events.thunderbolts"
                    values={{
                        actor: sideName(event.actor, names),
                        target: sideName(event.target, names),
                        damage: event.damage,
                    }}
                    components={{actor: sideSpan(event.actor), target: sideSpan(event.target)}}
                />
            );
        case 'Petrifying':
            return (
                <Trans
                    i18nKey="events.petrifying"
                    values={{
                        actor: sideName(event.actor, names),
                        target: sideName(event.target, names),
                    }}
                    components={{actor: sideSpan(event.actor), target: sideSpan(event.target)}}
                />
            );
        case 'Cursing':
            return (
                <Trans
                    i18nKey="events.cursing"
                    values={{
                        actor: sideName(event.actor, names),
                        target: sideName(event.target, names),
                    }}
                    components={{actor: sideSpan(event.actor), target: sideSpan(event.target)}}
                />
            );
        case 'Poisoning':
            return (
                <Trans
                    i18nKey="events.poisoning"
                    values={{
                        actor: sideName(event.actor, names),
                        target: sideName(event.target, names),
                    }}
                    components={{actor: sideSpan(event.actor), target: sideSpan(event.target)}}
                />
            );
        case 'Diseasing':
            return (
                <Trans
                    i18nKey="events.diseasing"
                    values={{
                        actor: sideName(event.actor, names),
                        target: sideName(event.target, names),
                    }}
                    components={{actor: sideSpan(event.actor), target: sideSpan(event.target)}}
                />
            );
        case 'Aging':
            return (
                <Trans
                    i18nKey="events.aging"
                    values={{
                        actor: sideName(event.actor, names),
                        target: sideName(event.target, names),
                    }}
                    components={{actor: sideSpan(event.actor), target: sideSpan(event.target)}}
                />
            );
        case 'FireShield':
            return (
                <Trans
                    i18nKey="events.fireShield"
                    values={{
                        shielded: sideName(event.shielded, names),
                        attacker: sideName(event.attacker, names),
                        damage: event.damage,
                    }}
                    components={{
                        shielded: sideSpan(event.shielded),
                        attacker: sideSpan(event.attacker),
                    }}
                />
            );
        case 'Rebirth':
            return (
                <Trans
                    i18nKey="events.rebirth"
                    values={{actor: sideName(event.actor, names), count: event.restoredCount}}
                    components={{actor: sideSpan(event.actor)}}
                />
            );
        case 'BattleEnd': {
            const winnerName =
                event.winner === 'DRAW' ? t('events.draw') : sideName(event.winner, names);
            const winnerClass =
                event.winner === 'DRAW' ? 'font-semibold text-slate-300' : SIDE_CLASS[event.winner];
            return (
                <Trans
                    i18nKey="events.battleEnd"
                    values={{
                        winner: winnerName,
                        turns: event.turns,
                        attackerSurvivors: event.attackerSurvivors,
                        defenderSurvivors: event.defenderSurvivors,
                    }}
                    components={{winner: <span className={winnerClass}/>}}
                />
            );
        }
    }
}

