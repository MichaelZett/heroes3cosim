import type { ReactNode } from 'react';
import type { BattleEvent, Side } from '../api/types';

export interface SideNames {
  attacker: string;
  defender: string;
}

const SIDE_CLASS: Record<Side, string> = {
  ATTACKER: 'font-semibold text-amber-300',
  DEFENDER: 'font-semibold text-blue-300',
};

function actor(side: Side, names: SideNames): ReactNode {
  return (
    <span className={SIDE_CLASS[side]}>
      {side === 'ATTACKER' ? names.attacker : names.defender}
    </span>
  );
}

function winnerLabel(
  winner: 'ATTACKER' | 'DEFENDER' | 'DRAW',
  names: SideNames,
): ReactNode {
  if (winner === 'DRAW') return <span className="font-semibold text-slate-300">Unentschieden</span>;
  return actor(winner, names);
}

export function eventToNode(event: BattleEvent, names: SideNames): ReactNode {
  switch (event.type) {
    case 'BattleStart':
      return (
        <>
          Kampf beginnt: {actor('ATTACKER', names)} ({event.attacker.count}×) gegen{' '}
          {actor('DEFENDER', names)} ({event.defender.count}×).
        </>
      );
    case 'Move':
      return (
        <>
          {actor(event.actor, names)} bewegt sich von ({event.fromQ},{event.fromR}) nach (
          {event.toQ},{event.toR}).
        </>
      );
    case 'MoveBack':
      return (
        <>
          {actor(event.actor, names)} fliegt zurück nach ({event.toQ},{event.toR}).
        </>
      );
    case 'Wait':
      return <>{actor(event.actor, names)} wartet.</>;
    case 'Shoot':
      return (
        <>
          {actor(event.actor, names)} schießt auf {actor(event.target, names)} aus Distanz{' '}
          {event.distance} — {event.damage} Schaden, {event.killed} getötet.
        </>
      );
    case 'Melee':
      return (
        <>
          {actor(event.actor, names)} greift {actor(event.target, names)} im Nahkampf an —{' '}
          {event.damage} Schaden, {event.killed} getötet.
        </>
      );
    case 'Retaliation':
      return (
        <>
          {actor(event.retaliator, names)} schlägt zurück — {event.damage} Schaden,{' '}
          {event.killed} getötet.
        </>
      );
    case 'TwoBlows':
      return <>{actor(event.actor, names)} schlägt ein zweites Mal.</>;
    case 'TwoShots':
      return <>{actor(event.actor, names)} schießt ein zweites Mal.</>;
    case 'GoodMorale':
      return <>{actor(event.actor, names)} hat gute Moral und greift erneut an.</>;
    case 'DeathStare':
      return (
        <>
          {actor(event.actor, names)} tötet {event.kills} Einheit(en) von{' '}
          {actor(event.target, names)} mit Death Stare.
        </>
      );
    case 'Thunderbolts':
      return (
        <>
          {actor(event.actor, names)} verursacht {event.damage} Blitzschaden bei{' '}
          {actor(event.target, names)}.
        </>
      );
    case 'Petrifying':
      return (
        <>
          {actor(event.actor, names)} versteinert {actor(event.target, names)}.
        </>
      );
    case 'Cursing':
      return (
        <>
          {actor(event.actor, names)} verflucht {actor(event.target, names)}.
        </>
      );
    case 'Poisoning':
      return (
        <>
          {actor(event.actor, names)} vergiftet {actor(event.target, names)}.
        </>
      );
    case 'Diseasing':
      return (
        <>
          {actor(event.actor, names)} infiziert {actor(event.target, names)} mit Krankheit.
        </>
      );
    case 'Aging':
      return (
        <>
          {actor(event.actor, names)} lässt {actor(event.target, names)} altern — halbe HP.
        </>
      );
    case 'FireShield':
      return (
        <>
          {actor(event.shielded, names)} reflektiert {event.damage} Schaden durch Feuerschild auf{' '}
          {actor(event.attacker, names)}.
        </>
      );
    case 'Rebirth':
      return (
        <>
          {actor(event.actor, names)} wird durch Wiedergeburt mit {event.restoredCount} Einheiten
          zurückgebracht.
        </>
      );
    case 'BattleEnd':
      return (
        <>
          Kampf vorbei — Sieger: {winnerLabel(event.winner, names)} nach {event.turns} Runden.
          Überlebende: {event.attackerSurvivors} vs. {event.defenderSurvivors}.
        </>
      );
  }
}
