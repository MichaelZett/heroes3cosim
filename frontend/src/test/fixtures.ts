import type {
  FactionMatrixReport,
  ArtifactDto,
  BattleEvent,
  CreatureBankDto,
  BattleSimulationDto,
  Faction,
  HeroDto,
  HeroSpecialtyDto,
  MatrixReport,
  StackSnapshot,
  UnitDto,
} from '../shared/api/types';

export const TEST_FACTIONS: Faction[] = ['CASTLE', 'RAMPART', 'TOWER'];

function artifact(
  overrides: Partial<ArtifactDto> & Pick<ArtifactDto, 'id' | 'name' | 'kind'>,
): ArtifactDto {
  return {
    rarity: 'TREASURE',
    value: 1000,
    attack: 0,
    defense: 0,
    power: 0,
    knowledge: 0,
    components: [],
    blocks: [],
    effects: [],
    ...overrides,
  };
}

export const TEST_BANKS: CreatureBankDto[] = [
  {
    id: 'CYCLOPS_STOCKPILE',
    name: 'Cyclops Stockpile',
    levels: [1, 2, 3, 4].map((level) => ({
      level,
      chance: level === 4 ? 10 : 30,
      guards: [0, 1, 2, 3, 4].map((i) => ({
        unitName: 'Cyclops',
        count: 2 + 2 * level,
        upgradeName: i === 2 ? 'Cyclops King' : null,
      })),
    })),
  },
  {
    id: 'CRYPT',
    name: 'Crypt',
    levels: [
      {
        level: 1,
        chance: 100,
        guards: [{ unitName: 'Skeleton', count: 10, upgradeName: null }],
      },
    ],
  },
];

export const TEST_ARTIFACTS: ArtifactDto[] = [
  artifact({
    id: 'SWORD_OF_HELLFIRE',
    name: 'Sword of Hellfire',
    kind: 'WEAPON',
    value: 6000,
    attack: 6,
  }),
  artifact({
    id: 'HELM_OF_CHAOS',
    name: 'Helm of Chaos',
    kind: 'HELMET',
    value: 4000,
    knowledge: 3,
  }),
  artifact({
    id: 'CLOVER_OF_FORTUNE',
    name: 'Clover of Fortune',
    kind: 'MISC',
    effects: [{ type: 'LUCK', value: 1, school: null, spells: [], level: null }],
  }),
  artifact({ id: 'RING_OF_VITALITY', name: 'Ring of Vitality', kind: 'RING', value: 5000 }),
  artifact({ id: 'RING_OF_LIFE', name: 'Ring of Life', kind: 'RING', value: 5000 }),
  artifact({ id: 'VIAL_OF_LIFEBLOOD', name: 'Vial of Lifeblood', kind: 'MISC', value: 10000 }),
  artifact({
    id: 'ELIXIR_OF_LIFE',
    name: 'Elixir of Life',
    kind: 'MISC',
    rarity: 'RELIC',
    value: 20000,
    components: ['VIAL_OF_LIFEBLOOD', 'RING_OF_VITALITY', 'RING_OF_LIFE'],
    blocks: ['RING', 'RING'],
    effects: [
      { type: 'HEALTH', value: 4, school: null, spells: [], level: null },
      { type: 'SPELL_IMMUNITY', value: null, school: null, spells: ['CURSE'], level: null },
    ],
  }),
];

function specialty(overrides: Partial<HeroSpecialtyDto>): HeroSpecialtyDto {
  return {
    kind: 'OTHER',
    creatureTier: 0,
    attack: 0,
    defense: 0,
    damage: 0,
    effective: true,
    ...overrides,
  };
}

export const TEST_HEROES: HeroDto[] = [
  {
    name: 'Crag Hack',
    heroClass: 'BARBARIAN',
    faction: 'STRONGHOLD',
    attack: 4,
    defense: 0,
    power: 1,
    knowledge: 1,
    skills: { OFFENSE: 'ADVANCED' },
    specialty: specialty({ kind: 'SKILL', skill: 'OFFENSE' }),
  },
  {
    name: 'Tazar',
    heroClass: 'BEASTMASTER',
    faction: 'FORTRESS',
    attack: 0,
    defense: 4,
    power: 1,
    knowledge: 1,
    skills: { ARMORER: 'ADVANCED' },
    specialty: specialty({ kind: 'SKILL', skill: 'ARMORER' }),
  },
  {
    name: 'Sorsha',
    heroClass: 'KNIGHT',
    faction: 'CASTLE',
    attack: 2,
    defense: 2,
    power: 1,
    knowledge: 1,
    skills: { LEADERSHIP: 'BASIC', OFFENSE: 'BASIC' },
    specialty: specialty({ kind: 'CREATURE', unitName: 'Swordsman', creatureTier: 4 }),
  },
];

export function unit(overrides: Partial<UnitDto> & { name: string }): UnitDto {
  return {
    id: overrides.name.toLowerCase().replace(/\s+/g, '-'),
    faction: 'CASTLE',
    tier: 1,
    upgrade: false,
    attack: 4,
    defense: 5,
    health: 10,
    speed: 4,
    minDamage: 1,
    maxDamage: 3,
    shots: 0,
    movement: 'GROUND',
    cost: 60,
    specialities: [],
    ...overrides,
  };
}

export const TEST_UNITS: UnitDto[] = [
  unit({ name: 'Pikeman', faction: 'CASTLE', tier: 1 }),
  unit({ name: 'Halberdier', faction: 'CASTLE', tier: 1, upgrade: true }),
  unit({ name: 'Archer', faction: 'CASTLE', tier: 2, shots: 12 }),
  unit({ name: 'Centaur', faction: 'RAMPART', tier: 1 }),
  unit({ name: 'Dwarf', faction: 'RAMPART', tier: 2 }),
  unit({ name: 'Gremlin', faction: 'TOWER', tier: 1 }),
];

function snapshot(
  side: StackSnapshot['side'],
  unitName: string,
  q: number,
  r: number,
  slot = 0,
): StackSnapshot {
  return { side, slot, unitName, count: 10, topHp: 10, q, r };
}

export function simulationFixture(): BattleSimulationDto {
  const attackerSnap = snapshot('ATTACKER', 'Pikeman', 0, 5);
  const defenderSnap = snapshot('DEFENDER', 'Centaur', 14, 5);
  const events: BattleEvent[] = [
    {
      type: 'BattleStart',
      battlefieldWidth: 15,
      battlefieldHeight: 11,
      obstacles: [],
      attacker: attackerSnap,
      defender: defenderSnap,
      stacks: [attackerSnap, defenderSnap],
      attackerMorale: 0,
      defenderMorale: 0,
    },
    {
      type: 'Move',
      actor: 'ATTACKER',
      actorSlot: 0,
      fromQ: 0,
      fromR: 5,
      toQ: 4,
      toR: 5,
      path: [
        { q: 1, r: 5 },
        { q: 2, r: 5 },
        { q: 3, r: 5 },
        { q: 4, r: 5 },
      ],
    },
    {
      type: 'BattleEnd',
      winner: 'ATTACKER',
      attackerSurvivors: 10,
      defenderSurvivors: 0,
      turns: 1,
      finalStacks: [attackerSnap, defenderSnap],
    },
  ];
  return {
    result: {
      winner: 'ATTACKER',
      attackerCountStart: 10,
      attackerSurvivors: 10,
      defenderCountStart: 10,
      defenderSurvivors: 0,
      turnsTaken: 1,
    },
    events,
  };
}

export function matrixReportFixture(): MatrixReport {
  return {
    totalMatchups: 3,
    seedsPerMatchup: 2,
    unitCount: 20,
    elapsedMs: 1234,
    stats: [
      {
        unitName: 'Halberdier',
        faction: 'CASTLE',
        tier: 1,
        upgrade: true,
        totalSims: 4,
        wins: 3,
        losses: 1,
        draws: 0,
        winRate: 0.75,
        avgSurvivorRatio: 0.6,
      },
      {
        unitName: 'Pikeman',
        faction: 'CASTLE',
        tier: 1,
        upgrade: false,
        totalSims: 4,
        wins: 2,
        losses: 2,
        draws: 0,
        winRate: 0.5,
        avgSurvivorRatio: 0.4,
      },
      {
        unitName: 'Archer',
        faction: 'CASTLE',
        tier: 2,
        upgrade: false,
        totalSims: 4,
        wins: 1,
        losses: 3,
        draws: 0,
        winRate: 0.25,
        avgSurvivorRatio: 0.2,
      },
    ],
    factionStats: [
      {
        faction: 'CASTLE',
        unitCount: 3,
        totalSims: 12,
        wins: 6,
        losses: 6,
        draws: 0,
        winRate: 0.5,
        avgSurvivorRatio: 0.4,
      },
    ],
    anomalies: [
      {
        unitName: 'Archer',
        tier: 2,
        againstTier: 1,
        winRate: 0.25,
        sampleSize: 4,
      },
    ],
  };
}

export function factionMatrixReportFixture(): FactionMatrixReport {
  const pairing = (
    faction: 'CASTLE' | 'TOWER',
    opponent: 'CASTLE' | 'TOWER',
    wins: number,
    losses: number,
  ) => ({
    faction,
    opponent,
    sims: wins + losses,
    wins,
    losses,
    draws: 0,
    winRate: wins / (wins + losses),
    avgRounds: 6.5,
  });
  return {
    factions: ['CASTLE', 'TOWER'],
    seedsPerPair: 2,
    weeks: 1,
    heroes: true,
    terrain: 'GRASS',
    magicTerrain: null,
    elapsedMs: 1200,
    matrices: [
      {
        solver: 'STRATEGIC',
        mirrorSideBias: 0.25,
        elapsedMs: 1100,
        standings: [
          {
            faction: 'CASTLE',
            sims: 4,
            wins: 3,
            losses: 1,
            draws: 0,
            winRate: 0.75,
            avgSurvivorRatio: 0.4,
          },
          {
            faction: 'TOWER',
            sims: 4,
            wins: 1,
            losses: 3,
            draws: 0,
            winRate: 0.25,
            avgSurvivorRatio: 0.2,
          },
        ],
        pairings: [
          pairing('CASTLE', 'CASTLE', 1, 1),
          pairing('CASTLE', 'TOWER', 3, 1),
          pairing('TOWER', 'CASTLE', 1, 3),
          pairing('TOWER', 'TOWER', 2, 0),
        ],
      },
    ],
  };
}
