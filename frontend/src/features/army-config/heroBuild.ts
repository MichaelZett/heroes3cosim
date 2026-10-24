import type { SecondarySkill, SkillLevel, WarMachine } from '../../shared/api/types';

export const MAX_HERO_LEVEL = 99;

/** Die Fertigkeiten, die im Kampf wirken und sich deshalb hier lernen lassen. */
export const BUILD_SKILLS: SecondarySkill[] = [
  'OFFENSE',
  'ARCHERY',
  'ARMORER',
  'LEADERSHIP',
  'LUCK',
  'TACTICS',
  'ARTILLERY',
  'FIRST_AID',
];

/** Kriegsmaschinen, die ein Held mitfuehren kann (ohne Katapult, das nur bei Belagerung zaehlt). */
export const WAR_MACHINES: WarMachine[] = ['BALLISTA', 'AMMO_CART', 'FIRST_AID_TENT'];

/** Was der Held ausserhalb der Schlacht erreicht hat: Level, Punkte, Fertigkeiten, Boni. */
export interface HeroBuild {
  level: number;
  attack: number;
  defense: number;
  skills: Partial<Record<SecondarySkill, SkillLevel>>;
  luck: number;
  morale: number;
  warMachines: WarMachine[];
}

export const DEFAULT_BUILD: HeroBuild = {
  level: 1,
  attack: 0,
  defense: 0,
  skills: {},
  luck: 0,
  morale: 0,
  warMachines: [],
};
