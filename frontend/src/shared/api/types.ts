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
    | { type: 'Poisoning'; actor: Side; actorSlot: number; target: Side; targetSlot: number }
    | { type: 'Diseasing'; actor: Side; actorSlot: number; target: Side; targetSlot: number }
    | { type: 'Aging'; actor: Side; actorSlot: number; target: Side; targetSlot: number }
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
}

export interface ArmyBattleRequest {
    attacker: ArmySpec;
    defender: ArmySpec;
    seed?: number | null;
}

export interface ArmyBattleSimulation {
    result: BattleResult;
    events: BattleEvent[];
}

export interface FactionPresetDto {
    faction: Faction;
    stacks: StackSpec[];
}

export interface ArmyPresetsResponse {
    presets: FactionPresetDto[];
}

// Matrix-Experiment DTOs — Spiegel von application.experiment.*
export type StackSizingMode =
    | 'EQUAL_COUNT'
    | 'EQUAL_GOLD'
    | 'WEEKLY_PRODUCTION'
    | 'EQUAL_GOLD_WEEKLY';

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
