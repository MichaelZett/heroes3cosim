import type { SecondarySkill, SkillLevel, Spell, WarMachine } from '../../shared/api/types';

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
  'WISDOM',
  'INTELLIGENCE',
  'SORCERY',
  'RESISTANCE',
  'AIR_MAGIC',
  'EARTH_MAGIC',
  'FIRE_MAGIC',
  'WATER_MAGIC',
];

/** Zauber, die ein Held im Simulator wirken kann, mit ihrer Stufe (Manual, Spell Reference). */
export const HERO_SPELLS: [Spell, number][] = [
  ['MAGIC_ARROW', 1],
  ['BLESS', 1],
  ['BLOODLUST', 1],
  ['CURE', 1],
  ['CURSE', 1],
  ['DISPEL', 1],
  ['HASTE', 1],
  ['PROTECTION_FROM_FIRE', 1],
  ['PROTECTION_FROM_WATER', 1],
  ['SHIELD', 1],
  ['SLOW', 1],
  ['STONE_SKIN', 1],
  ['BLIND', 2],
  ['DEATH_RIPPLE', 2],
  ['DISRUPTING_RAY', 2],
  ['FORTUNE', 2],
  ['ICE_BOLT', 2],
  ['LIGHTNING_BOLT', 2],
  ['PRECISION', 2],
  ['PROTECTION_FROM_AIR', 2],
  ['WEAKNESS', 2],
  ['AIR_SHIELD', 3],
  ['ANIMATE_DEAD', 3],
  ['ANTI_MAGIC', 3],
  ['DESTROY_UNDEAD', 3],
  ['FIREBALL', 3],
  ['FORGETFULNESS', 3],
  ['FROST_RING', 3],
  ['MIRTH', 3],
  ['MISFORTUNE', 3],
  ['PROTECTION_FROM_EARTH', 3],
  ['ARMAGEDDON', 4],
  ['CHAIN_LIGHTNING', 4],
  ['COUNTERSTRIKE', 4],
  ['FIRE_SHIELD', 4],
  ['INFERNO', 4],
  ['METEOR_SHOWER', 4],
  ['PRAYER', 4],
  ['RESURRECTION', 4],
  ['SLAYER', 4],
  ['SORROW', 4],
  ['IMPLOSION', 5],
];

const SPELL_LEVEL = new Map<Spell, number>(HERO_SPELLS);

/** Hoechste lernbare Zauberstufe (Manual S. 39): ohne Wisdom 2, Basic/Advanced/Expert 3/4/5. */
export function maxSpellLevel(wisdom: SkillLevel | undefined): number {
  return { NONE: 2, BASIC: 3, ADVANCED: 4, EXPERT: 5 }[wisdom ?? 'NONE'];
}

/** Hoeher von zwei Stufen — Startfertigkeit und Aufbau zusammen. */
export function higherLevel(a: SkillLevel | undefined, b: SkillLevel | undefined): SkillLevel {
  const order: SkillLevel[] = ['NONE', 'BASIC', 'ADVANCED', 'EXPERT'];
  return order[Math.max(order.indexOf(a ?? 'NONE'), order.indexOf(b ?? 'NONE'))];
}

/** Die Zauber des Buchs, die der Held mit dieser Wisdom lernen kann. */
export function learnableSpells(spells: Spell[], wisdom: SkillLevel | undefined): Spell[] {
  const max = maxSpellLevel(wisdom);
  return spells.filter((spell) => (SPELL_LEVEL.get(spell) ?? 99) <= max);
}

/** Mana zu Kampfbeginn (Manual S. 33, 37): 10 je Knowledge, Intelligence +25/50/100 %. */
export function maxMana(knowledge: number, intelligence: SkillLevel | undefined): number {
  const percent = { NONE: 0, BASIC: 25, ADVANCED: 50, EXPERT: 100 }[intelligence ?? 'NONE'];
  return Math.floor((knowledge * 10 * (100 + percent)) / 100);
}

/** Kriegsmaschinen, die ein Held mitfuehren kann (ohne Katapult, das nur bei Belagerung zaehlt). */
export const WAR_MACHINES: WarMachine[] = ['BALLISTA', 'AMMO_CART', 'FIRST_AID_TENT'];

/** Was der Held ausserhalb der Schlacht erreicht hat: Level, Punkte, Fertigkeiten, Boni. */
export interface HeroBuild {
  level: number;
  attack: number;
  defense: number;
  power: number;
  knowledge: number;
  spells: Spell[];
  skills: Partial<Record<SecondarySkill, SkillLevel>>;
  luck: number;
  morale: number;
  warMachines: WarMachine[];
}

export const DEFAULT_BUILD: HeroBuild = {
  level: 1,
  attack: 0,
  defense: 0,
  power: 0,
  knowledge: 0,
  spells: [],
  skills: {},
  luck: 0,
  morale: 0,
  warMachines: [],
};
