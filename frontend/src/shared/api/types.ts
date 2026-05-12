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
export type BattleEvent =
    | {
    type: 'BattleStart';
    battlefieldWidth: number;
    battlefieldHeight: number;
    obstacles: HexCoord[];
    attacker: StackSnapshot;
    defender: StackSnapshot;
}
    | {
    type: 'Move';
    actor: Side;
    fromQ: number;
    fromR: number;
    toQ: number;
    toR: number;
    path: HexCoord[];
}
    | { type: 'Wait'; actor: Side }
    | {
    type: 'Shoot';
    actor: Side;
    target: Side;
    distance: number;
    damage: number;
    killed: number;
    targetAfter: StackSnapshot;
}
    | {
    type: 'Melee';
    actor: Side;
    target: Side;
    hexesMoved: number;
    damage: number;
    killed: number;
    targetAfter: StackSnapshot;
}
    | {
    type: 'Retaliation';
    retaliator: Side;
    target: Side;
    damage: number;
    killed: number;
    targetAfter: StackSnapshot;
}
    | { type: 'TwoBlows'; actor: Side }
    | { type: 'TwoShots'; actor: Side }
    | { type: 'GoodMorale'; actor: Side }
    | { type: 'MoveBack'; actor: Side; toQ: number; toR: number; path: HexCoord[] }
    | { type: 'DeathStare'; actor: Side; target: Side; kills: number; targetAfter: StackSnapshot }
    | { type: 'Thunderbolts'; actor: Side; target: Side; damage: number; targetAfter: StackSnapshot }
    | { type: 'Petrifying'; actor: Side; target: Side }
    | { type: 'Cursing'; actor: Side; target: Side }
    | { type: 'Poisoning'; actor: Side; target: Side }
    | { type: 'Diseasing'; actor: Side; target: Side }
    | { type: 'Aging'; actor: Side; target: Side }
    | {
    type: 'FireShield';
    shielded: Side;
    attacker: Side;
    damage: number;
    attackerAfter: StackSnapshot;
}
    | { type: 'Rebirth'; actor: Side; restoredCount: number; actorAfter: StackSnapshot }
    | {
    type: 'BattleEnd';
    winner: Winner;
    attackerSurvivors: number;
    defenderSurvivors: number;
    turns: number;
};

export interface BattleSimulationDto {
    result: BattleResult;
    events: BattleEvent[];
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
