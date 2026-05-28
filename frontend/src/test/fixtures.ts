import type {
    BattleEvent,
    BattleSimulationDto,
    Faction,
    MatrixReport,
    StackSnapshot,
    UnitDto,
} from '../shared/api/types';

export const TEST_FACTIONS: Faction[] = ['CASTLE', 'RAMPART', 'TOWER'];

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
    unit({name: 'Pikeman', faction: 'CASTLE', tier: 1}),
    unit({name: 'Halberdier', faction: 'CASTLE', tier: 1, upgrade: true}),
    unit({name: 'Archer', faction: 'CASTLE', tier: 2, shots: 12}),
    unit({name: 'Centaur', faction: 'RAMPART', tier: 1}),
    unit({name: 'Dwarf', faction: 'RAMPART', tier: 2}),
    unit({name: 'Gremlin', faction: 'TOWER', tier: 1}),
];

function snapshot(side: StackSnapshot['side'], unitName: string, q: number, r: number, slot = 0): StackSnapshot {
    return {side, slot, unitName, count: 10, topHp: 10, q, r};
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
        },
        {
            type: 'Move',
            actor: 'ATTACKER',
            actorSlot: 0,
            fromQ: 0,
            fromR: 5,
            toQ: 4,
            toR: 5,
            path: [{q: 1, r: 5}, {q: 2, r: 5}, {q: 3, r: 5}, {q: 4, r: 5}]
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
