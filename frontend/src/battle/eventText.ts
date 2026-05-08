import type { BattleEvent, Side } from '../api/types';

const SIDE_LABEL: Record<Side, string> = {
  ATTACKER: 'Truppe 1',
  DEFENDER: 'Truppe 2',
};

const WINNER_LABEL: Record<'ATTACKER' | 'DEFENDER' | 'DRAW', string> = {
  ATTACKER: 'Truppe 1',
  DEFENDER: 'Truppe 2',
  DRAW: 'Unentschieden',
};

export function eventToText(event: BattleEvent): string {
  switch (event.type) {
    case 'BattleStart':
      return `Kampf beginnt: ${SIDE_LABEL.ATTACKER} (${event.attacker.count}× ${event.attacker.unitName}) gegen ${SIDE_LABEL.DEFENDER} (${event.defender.count}× ${event.defender.unitName}).`;
    case 'Move':
      return `${SIDE_LABEL[event.actor]} bewegt sich von (${event.fromQ},${event.fromR}) nach (${event.toQ},${event.toR}).`;
    case 'MoveBack':
      return `${SIDE_LABEL[event.actor]} fliegt zurück nach (${event.toQ},${event.toR}).`;
    case 'Wait':
      return `${SIDE_LABEL[event.actor]} wartet.`;
    case 'Shoot':
      return `${SIDE_LABEL[event.actor]} schießt auf ${SIDE_LABEL[event.target]} aus Distanz ${event.distance} — ${event.damage} Schaden, ${event.killed} getötet.`;
    case 'Melee':
      return `${SIDE_LABEL[event.actor]} greift ${SIDE_LABEL[event.target]} im Nahkampf an — ${event.damage} Schaden, ${event.killed} getötet.`;
    case 'Retaliation':
      return `${SIDE_LABEL[event.retaliator]} schlägt zurück — ${event.damage} Schaden, ${event.killed} getötet.`;
    case 'TwoBlows':
      return `${SIDE_LABEL[event.actor]} schlägt ein zweites Mal.`;
    case 'TwoShots':
      return `${SIDE_LABEL[event.actor]} schießt ein zweites Mal.`;
    case 'GoodMorale':
      return `${SIDE_LABEL[event.actor]} hat gute Moral und greift erneut an.`;
    case 'DeathStare':
      return `${SIDE_LABEL[event.actor]} tötet ${event.kills} Einheit(en) von ${SIDE_LABEL[event.target]} mit Death Stare.`;
    case 'Thunderbolts':
      return `${SIDE_LABEL[event.actor]} verursacht ${event.damage} Blitzschaden bei ${SIDE_LABEL[event.target]}.`;
    case 'Petrifying':
      return `${SIDE_LABEL[event.actor]} versteinert ${SIDE_LABEL[event.target]}.`;
    case 'Cursing':
      return `${SIDE_LABEL[event.actor]} verflucht ${SIDE_LABEL[event.target]}.`;
    case 'Poisoning':
      return `${SIDE_LABEL[event.actor]} vergiftet ${SIDE_LABEL[event.target]}.`;
    case 'Diseasing':
      return `${SIDE_LABEL[event.actor]} infiziert ${SIDE_LABEL[event.target]} mit Krankheit.`;
    case 'Aging':
      return `${SIDE_LABEL[event.actor]} lässt ${SIDE_LABEL[event.target]} altern — halbe HP.`;
    case 'FireShield':
      return `${SIDE_LABEL[event.shielded]} reflektiert ${event.damage} Schaden durch Feuerschild auf ${SIDE_LABEL[event.attacker]}.`;
    case 'Rebirth':
      return `${SIDE_LABEL[event.actor]} wird durch Wiedergeburt mit ${event.restoredCount} Einheiten zurückgebracht.`;
    case 'BattleEnd':
      return `Kampf vorbei — Sieger: ${WINNER_LABEL[event.winner]} nach ${event.turns} Runden. Überlebende: ${event.attackerSurvivors} vs. ${event.defenderSurvivors}.`;
  }
}
