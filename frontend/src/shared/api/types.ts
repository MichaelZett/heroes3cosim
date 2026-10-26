// Mirror der Backend-DTOs unter de.zettsystems.h3comsim.adapter.web.dto.
// Hand-getippt; sobald die API wächst, durch openapi-typescript-codegen ersetzen.

export type Faction =
  | 'CASTLE'
  | 'RAMPART'
  | 'TOWER'
  | 'INFERNO'
  | 'NECROPOLIS'
  | 'DUNGEON'
  | 'STRONGHOLD'
  | 'FORTRESS'
  | 'CONFLUX'
  | 'NEUTRAL';

export type Movement = 'GROUND' | 'FLYING';

export interface UnitDto {
  id: string;
  name: string;
  faction: Faction;
  tier: number;
  upgrade: boolean;
  attack: number;
  defense: number;
  health: number;
  speed: number;
  minDamage: number;
  maxDamage: number;
  shots: number;
  movement: Movement;
  cost: number;
  specialities: string[];
}

export interface BattleConfigRequest {
  attackerUnit: string;
  attackerCount: number;
  defenderUnit: string;
  defenderCount: number;
  seed?: number | null;
}

export type Winner = 'ATTACKER' | 'DEFENDER' | 'DRAW';

export type Side = 'ATTACKER' | 'DEFENDER';

export interface BattleResult {
  winner: Winner;
  attackerCountStart: number;
  attackerSurvivors: number;
  defenderCountStart: number;
  defenderSurvivors: number;
  turnsTaken: number;
}

export interface StackSnapshot {
  side: Side;
  slot: number;
  unitName: string;
  count: number;
  topHp: number;
  q: number;
  r: number;
  /** Zweifeldrig: das zweite Feld liegt beim Angreifer bei q − 1, beim Verteidiger bei q + 1. */
  doubleWide?: boolean;
}

export interface HexCoord {
  q: number;
  r: number;
}

// Discriminated Union — mirror BattleEvent sealed interface mit `type`-Discriminator.
// `actorSlot`/`targetSlot` ist im Single-Battle-Pfad immer 0; im Army-Battle 0..6.
export type BattleEvent =
  | {
      type: 'BattleStart';
      battlefieldWidth: number;
      battlefieldHeight: number;
      obstacles: HexCoord[];
      attacker: StackSnapshot;
      defender: StackSnapshot;
      stacks: StackSnapshot[];
      /** Moral-Rating der Armee (Manual S. 43), -3..+3. */
      attackerMorale: number;
      defenderMorale: number;
      /** Held der Armee; im Army-Battle immer gesetzt, im Single-Battle null. */
      attackerHero?: string | null;
      defenderHero?: string | null;
      /** Glücks-Rating der Armee (Manual S. 44), 0..3; ohne Held 0. */
      attackerLuck?: number;
      defenderLuck?: number;
      /** Kriegsmaschinen beider Seiten. */
      attackerWarMachines?: WarMachine[];
      defenderWarMachines?: WarMachine[];
      /** Die Kriegsmaschinen auf dem Feld, als Stacks ab Slot 20; angreifbar. */
      warMachineStacks?: StackSnapshot[];
      /** Die Befestigung bei einer Belagerung. */
      siege?: SiegeSnapshot | null;
    }
  | {
      type: 'CatapultShot';
      aimed: WallPart;
      hit: WallPart;
      damage: number;
      healthAfter: number;
      /** Slot des schießenden Zyklopen; fehlt beim Katapult. */
      shooterSlot?: number | null;
    }
  | {
      type: 'TowerShot';
      tower: WallPart;
      targetSlot: number;
      damage: number;
      killed: number;
      targetAfter: StackSnapshot;
    }
  | {
      type: 'QuakeHit';
      hit: WallPart;
      healthAfter: number;
    }
  | {
      type: 'Drawbridge';
      state: Gate;
    }
  | {
      type: 'MoatDamage';
      side: Side;
      slot: number;
      damage: number;
      killed: number;
      mine: boolean;
      targetAfter: StackSnapshot;
    }
  | {
      type: 'Move';
      actor: Side;
      actorSlot: number;
      fromQ: number;
      fromR: number;
      toQ: number;
      toR: number;
      path: HexCoord[];
    }
  | { type: 'Wait'; actor: Side; actorSlot: number }
  | {
      type: 'Shoot';
      actor: Side;
      actorSlot: number;
      target: Side;
      targetSlot: number;
      distance: number;
      damage: number;
      killed: number;
      targetAfter: StackSnapshot;
    }
  | {
      type: 'Melee';
      actor: Side;
      actorSlot: number;
      target: Side;
      targetSlot: number;
      hexesMoved: number;
      damage: number;
      killed: number;
      targetAfter: StackSnapshot;
    }
  | {
      type: 'Retaliation';
      retaliator: Side;
      retaliatorSlot: number;
      target: Side;
      targetSlot: number;
      damage: number;
      killed: number;
      targetAfter: StackSnapshot;
    }
  | { type: 'TwoBlows'; actor: Side; actorSlot: number }
  | { type: 'TwoShots'; actor: Side; actorSlot: number }
  | { type: 'GoodMorale'; actor: Side; actorSlot: number }
  | { type: 'BadMorale'; actor: Side; actorSlot: number }
  /** Der unmittelbar folgende Treffer dieses Stacks richtet doppelten Grundschaden an. */
  | { type: 'LuckyStrike'; actor: Side; actorSlot: number }
  /** Schuss der Ballista am Rundenende; doubled = Artillery, lucky = Glueckstreffer. */
  | {
      type: 'BallistaShot';
      actor: Side;
      target: Side;
      targetSlot: number;
      damage: number;
      killed: number;
      lucky: boolean;
      doubled: boolean;
      targetAfter: StackSnapshot;
    }
  /** Eine Kreatur wirkt einen Zauber; resisted = vom Ziel abgewehrt. */
  | {
      type: 'SpellCast';
      actor: Side;
      actorSlot: number;
      spell: Spell;
      target: Side;
      targetSlot: number;
      resisted: boolean;
      targetAfter: StackSnapshot;
    }
  /** Der Held einer Seite wirkt einen Zauber; die Wirkung je Stack folgt als SpellHit. */
  | { type: 'HeroSpell'; actor: Side; spell: Spell; level: SkillLevel; manaLeft: number }
  /** Wirkung eines Heldenzaubers auf einen Stack; resisted = immun oder abgewehrt. */
  | {
      type: 'SpellHit';
      actor: Side;
      spell: Spell;
      target: Side;
      targetSlot: number;
      damage: number;
      killed: number;
      healed: number;
      resisted: boolean;
      /** Von Magic Mirror auf einen Stack des Zaubernden umgelenkt. */
      reflected: boolean;
      targetAfter: StackSnapshot;
    }
  /** Beschworene Elementare: neuer Stack ab Slot 7 oder Verstaerkung des beschworenen. */
  | { type: 'Summon'; actor: Side; spell: Spell; count: number; stack: StackSnapshot }
  /** Serpent oder Dragon Fly hebt die nuetzlichen Zauber des Ziels auf. */
  | {
      type: 'Dispel';
      actor: Side;
      actorSlot: number;
      target: Side;
      targetSlot: number;
      removed: number;
      targetAfter: StackSnapshot;
    }
  /** Das Sanitaetszelt heilt die oberste Kreatur eines eigenen Stacks. */
  | {
      type: 'FirstAid';
      actor: Side;
      targetSlot: number;
      healed: number;
      targetAfter: StackSnapshot;
    }
  /** Tactics: diese Seite hat vor Kampfbeginn in den ersten `columns` Spalten aufgestellt. */
  | { type: 'TacticsPhase'; actor: Side; columns: number }
  | {
      type: 'MoveBack';
      actor: Side;
      actorSlot: number;
      toQ: number;
      toR: number;
      path: HexCoord[];
    }
  | {
      type: 'DeathStare';
      actor: Side;
      actorSlot: number;
      target: Side;
      targetSlot: number;
      kills: number;
      targetAfter: StackSnapshot;
    }
  | {
      type: 'Thunderbolts';
      actor: Side;
      actorSlot: number;
      target: Side;
      targetSlot: number;
      damage: number;
      targetAfter: StackSnapshot;
    }
  | { type: 'Petrifying'; actor: Side; actorSlot: number; target: Side; targetSlot: number }
  | { type: 'Cursing'; actor: Side; actorSlot: number; target: Side; targetSlot: number }
  | {
      type: 'AcidBreath';
      actor: Side;
      actorSlot: number;
      target: Side;
      targetSlot: number;
      damage: number;
      killed: number;
      targetAfter: StackSnapshot;
    }
  | { type: 'Fear'; actor: Side; actorSlot: number }
  | { type: 'Poisoning'; actor: Side; actorSlot: number; target: Side; targetSlot: number }
  | { type: 'Diseasing'; actor: Side; actorSlot: number; target: Side; targetSlot: number }
  | { type: 'Aging'; actor: Side; actorSlot: number; target: Side; targetSlot: number }
  | { type: 'Bind'; actor: Side; actorSlot: number; target: Side; targetSlot: number }
  | {
      type: 'FireShield';
      shielded: Side;
      shieldedSlot: number;
      attacker: Side;
      attackerSlot: number;
      damage: number;
      attackerAfter: StackSnapshot;
    }
  | {
      type: 'Rebirth';
      actor: Side;
      actorSlot: number;
      restoredCount: number;
      actorAfter: StackSnapshot;
    }
  | {
      type: 'Resurrection';
      actor: Side;
      actorSlot: number;
      target: Side;
      targetSlot: number;
      restoredCount: number;
      targetAfter: StackSnapshot;
    }
  | {
      type: 'RaiseDemons';
      actor: Side;
      actorSlot: number;
      target: Side;
      targetSlot: number;
      raisedCount: number;
      targetAfter: StackSnapshot;
    }
  | {
      type: 'BattleEnd';
      winner: Winner;
      attackerSurvivors: number;
      defenderSurvivors: number;
      turns: number;
      finalStacks: StackSnapshot[];
    };

export interface BattleSimulationDto {
  result: BattleResult;
  events: BattleEvent[];
}

// Army-Battle DTOs — mirror von armybattle.values.*
export interface StackSpec {
  unitName: string;
  count: number;
}

export interface ArmySpec {
  stacks: StackSpec[];
  /** Name aus /api/heroes. Null bedeutet: Standardheld der Fraktion mit dem groessten Goldwert. */
  heroName?: string | null;
  /** Gesammeltes Glueck des Helden (Artefakte, Gebaeude, Orte), 0..3. */
  luckBonus?: number;
  /** Gesammelte Moral des Helden, -3..+3. */
  moraleBonus?: number;
  /** Level des Helden, 1..99; wirkt ueber die Spezialitaet. */
  heroLevel?: number;
  /** Zusaetzliche Attack-/Defense-Punkte des Helden, 0..99. */
  attackBonus?: number;
  defenseBonus?: number;
  /** Gelernte oder verbesserte Fertigkeiten; Startfertigkeiten lassen sich nur heben. */
  skills?: Partial<Record<SecondarySkill, SkillLevel>>;
  /** Kriegsmaschinen des Helden. */
  warMachines?: WarMachine[];
  /** Zusaetzliche Power-/Knowledge-Punkte des Helden, 0..99. */
  powerBonus?: number;
  knowledgeBonus?: number;
  /** Zauberbuch: ohne Wisdom bis Stufe 2, mit Basic/Advanced/Expert bis 3/4/5. */
  spells?: Spell[];
  /** Zufaelliger Aufstieg bis heroLevel nach den Regeln des Spiels. */
  randomGrowth?: boolean;
  /** Armee ohne Helden (neutrale Monster, Garnison); schliesst heroName aus. */
  withoutHero?: boolean;
}

export type Spell =
  | 'BLESS'
  | 'BLOODLUST'
  | 'HASTE'
  | 'SHIELD'
  | 'STONE_SKIN'
  | 'PROTECTION_FROM_FIRE'
  | 'PROTECTION_FROM_WATER'
  | 'FORTUNE'
  | 'PRECISION'
  | 'PROTECTION_FROM_AIR'
  | 'AIR_SHIELD'
  | 'ANTI_MAGIC'
  | 'MIRTH'
  | 'PROTECTION_FROM_EARTH'
  | 'COUNTERSTRIKE'
  | 'FIRE_SHIELD'
  | 'FRENZY'
  | 'PRAYER'
  | 'SLAYER'
  | 'CURSE'
  | 'SLOW'
  | 'BLIND'
  | 'DISRUPTING_RAY'
  | 'WEAKNESS'
  | 'FORGETFULNESS'
  | 'MISFORTUNE'
  | 'SORROW'
  | 'CURE'
  | 'DISPEL'
  | 'ANIMATE_DEAD'
  | 'RESURRECTION'
  | 'MAGIC_ARROW'
  | 'DEATH_RIPPLE'
  | 'ICE_BOLT'
  | 'LIGHTNING_BOLT'
  | 'DESTROY_UNDEAD'
  | 'FIREBALL'
  | 'FROST_RING'
  | 'ARMAGEDDON'
  | 'CHAIN_LIGHTNING'
  | 'INFERNO'
  | 'METEOR_SHOWER'
  | 'IMPLOSION'
  | 'MAGIC_MIRROR'
  | 'SUMMON_AIR_ELEMENTAL'
  | 'SUMMON_EARTH_ELEMENTAL'
  | 'SUMMON_FIRE_ELEMENTAL'
  | 'SUMMON_WATER_ELEMENTAL'
  | 'BERSERK'
  | 'CLONE'
  | 'DIMENSION_DOOR'
  | 'DISGUISE'
  | 'EARTHQUAKE'
  | 'FIRE_WALL'
  | 'FLY'
  | 'FORCE_FIELD'
  | 'HYPNOTIZE'
  | 'LAND_MINE'
  | 'QUICKSAND'
  | 'REMOVE_OBSTACLE'
  | 'SACRIFICE'
  | 'SCUTTLE_BOAT'
  | 'SUMMON_BOAT'
  | 'TELEPORT'
  | 'TOWN_PORTAL'
  | 'VIEW_AIR'
  | 'VIEW_EARTH'
  | 'VISIONS'
  | 'WATER_WALK';

export type WarMachine = 'BALLISTA' | 'AMMO_CART' | 'FIRST_AID_TENT' | 'CATAPULT';

export type Fortification = 'FORT' | 'CITADEL' | 'CASTLE';

/** Zustand der Zugbrücke. */
export type Gate = 'CLOSED' | 'OPEN' | 'BLOCKED' | 'DESTROYED';

export type WallPart =
  | 'UPPER_WALL'
  | 'MID_UPPER_WALL'
  | 'MID_LOWER_WALL'
  | 'LOWER_WALL'
  | 'GATE'
  | 'KEEP'
  | 'UPPER_TOWER'
  | 'LOWER_TOWER';

export interface SiegePart {
  part: WallPart;
  /** Feld des Teils; null für den Hauptturm außerhalb des Felds. */
  hex?: HexCoord | null;
  health: number;
}

export interface SiegeSnapshot {
  fortification: Fortification;
  town: Faction;
  permanentWall: HexCoord[];
  parts: SiegePart[];
  /** Grabenfelder, bei Tower die Landminen. */
  moat: HexCoord[];
}

export type HeroClass =
  | 'KNIGHT'
  | 'CLERIC'
  | 'RANGER'
  | 'DRUID'
  | 'ALCHEMIST'
  | 'WIZARD'
  | 'DEMONIAC'
  | 'HERETIC'
  | 'DEATH_KNIGHT'
  | 'NECROMANCER'
  | 'OVERLORD'
  | 'WARLOCK'
  | 'BARBARIAN'
  | 'BATTLE_MAGE'
  | 'BEASTMASTER'
  | 'WITCH'
  | 'PLANESWALKER'
  | 'ELEMENTALIST';

export type SecondarySkill =
  | 'AIR_MAGIC'
  | 'ARCHERY'
  | 'ARMORER'
  | 'ARTILLERY'
  | 'BALLISTICS'
  | 'DIPLOMACY'
  | 'EAGLE_EYE'
  | 'EARTH_MAGIC'
  | 'ESTATES'
  | 'FIRE_MAGIC'
  | 'FIRST_AID'
  | 'INTELLIGENCE'
  | 'LEADERSHIP'
  | 'LEARNING'
  | 'LOGISTICS'
  | 'LUCK'
  | 'MYSTICISM'
  | 'NAVIGATION'
  | 'NECROMANCY'
  | 'OFFENSE'
  | 'PATHFINDING'
  | 'RESISTANCE'
  | 'SCHOLAR'
  | 'SCOUTING'
  | 'SORCERY'
  | 'TACTICS'
  | 'WATER_MAGIC'
  | 'WISDOM';

export type SkillLevel = 'NONE' | 'BASIC' | 'ADVANCED' | 'EXPERT';

/**
 * Held aus /api/heroes. attack und defense werden auf jede Kreatur der gefuehrten Armee addiert,
 * power und knowledge bestimmen Spell Power und Mana seiner Zauber.
 */
export interface HeroDto {
  name: string;
  heroClass: HeroClass;
  faction: Faction;
  attack: number;
  defense: number;
  power: number;
  knowledge: number;
  skills: Partial<Record<SecondarySkill, SkillLevel>>;
  specialty: HeroSpecialtyDto;
  /** Level des Helden; im Katalog 1. */
  level?: number;
  /** Startzauber bzw. Zauberbuch des Helden. */
  spells?: Spell[];
}

/** Spezialfaehigkeit eines Helden; kind bestimmt, welche Felder belegt sind. */
export interface HeroSpecialtyDto {
  kind: 'CREATURE' | 'SKILL' | 'FIXED_CREATURE' | 'BALLISTA' | 'SPELL' | 'SPELL_UNIQUE' | 'OTHER';
  unitName?: string | null;
  creatureTier: number;
  skill?: SecondarySkill | null;
  attack: number;
  defense: number;
  damage: number;
  description?: string | null;
  effective: boolean;
}

export interface ArmyBattleRequest {
  attacker: ArmySpec;
  defender: ArmySpec;
  seed?: number | null;
  /** Belagerung: Befestigung der Stadt des Verteidigers; null = Feldschlacht. */
  fortification?: Fortification | null;
  /** Stadt des Verteidigers; null = Fraktion seines Helden, ohne Helden Hauptfraktion der Garnison. */
  town?: Faction | null;
  /** Zauberkosten nach Shadow of Death: beherrschte Schule macht Zauber billiger. */
  sodSpellCosts?: boolean | null;
}

export interface ArmyBattleSimulation {
  result: BattleResult;
  events: BattleEvent[];
  /** Die Helden, wie sie gekaempft haben (nach Aufbau und Aufstieg); null ohne Helden. */
  attackerHero?: HeroDto | null;
  defenderHero?: HeroDto | null;
}

export interface FactionPresetDto {
  faction: Faction;
  stacks: StackSpec[];
  /** Vorgeschlagener Held der Faktion; wird beim Preset-Laden als Default uebernommen. */
  heroName?: string | null;
}

export interface ArmyPresetsResponse {
  presets: FactionPresetDto[];
}

// Matrix-Experiment DTOs — Spiegel von application.experiment.*
export type StackSizingMode =
  'EQUAL_COUNT' | 'EQUAL_GOLD' | 'WEEKLY_PRODUCTION' | 'EQUAL_GOLD_WEEKLY';

export interface MatrixRequestDto {
  unitCount?: number | null;
  excludeUnits?: string[] | null;
  excludeFactions?: Faction[] | null;
  excludeTiers?: number[] | null;
  mode?: StackSizingMode | null;
  seedsPerMatchup?: number | null;
}

export interface UnitMatchupStats {
  unitName: string;
  faction: Faction;
  tier: number;
  upgrade: boolean;
  totalSims: number;
  wins: number;
  losses: number;
  draws: number;
  winRate: number;
  avgSurvivorRatio: number;
}

export interface TierAnomaly {
  unitName: string;
  tier: number;
  againstTier: number;
  winRate: number;
  sampleSize: number;
}

export interface FactionMatchupStats {
  faction: Faction;
  unitCount: number;
  totalSims: number;
  wins: number;
  losses: number;
  draws: number;
  winRate: number;
  avgSurvivorRatio: number;
}

export interface MatrixReport {
  totalMatchups: number;
  seedsPerMatchup: number;
  unitCount: number;
  elapsedMs: number;
  stats: UnitMatchupStats[];
  factionStats: FactionMatchupStats[];
  anomalies: TierAnomaly[];
}

export type MatrixJobStatus = 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface MatrixJobSnapshot {
  jobId: string;
  status: MatrixJobStatus;
  completed: number;
  total: number;
  report: MatrixReport | null;
  error: string | null;
}

export interface MatrixProgress {
  completed: number;
  total: number;
}
