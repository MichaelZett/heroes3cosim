import type { Faction, MagicTerrain, Terrain } from './types';

/** Grundgelände in der Reihenfolge der Auswahllisten. */
export const TERRAINS: Terrain[] = [
  'GRASS',
  'DIRT',
  'LAVA',
  'SUBTERRANEAN',
  'ROUGH',
  'SAND',
  'SNOW',
  'SWAMP',
  'WATER',
];
/** Magische Schlachtfelder. */
export const MAGIC_TERRAINS: MagicTerrain[] = [
  'MAGIC_PLAINS',
  'CURSED_GROUND',
  'ROCKLANDS',
  'FIERY_FIELDS',
  'LUCID_POOLS',
  'MAGIC_CLOUDS',
  'HOLY_GROUND',
  'EVIL_FOG',
  'CLOVER_FIELD',
];
/** Die neun Städte, ohne NEUTRAL. */
export const TOWNS: Faction[] = [
  'CASTLE',
  'RAMPART',
  'TOWER',
  'INFERNO',
  'NECROPOLIS',
  'DUNGEON',
  'STRONGHOLD',
  'FORTRESS',
  'CONFLUX',
];
