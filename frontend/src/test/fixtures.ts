import type {BattleEvent, BattleSimulationDto, Faction, StackSnapshot, UnitDto,} from '../api/types';

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

function snapshot(side: StackSnapshot['side'], unitName: string, q: number, r: number): StackSnapshot {
    return {side, unitName, count: 10, topHp: 10, q, r};
}

export function simulationFixture(): BattleSimulationDto {
    const events: BattleEvent[] = [
        {
            type: 'BattleStart',
            battlefieldWidth: 15,
            battlefieldHeight: 11,
            attacker: snapshot('ATTACKER', 'Pikeman', 0, 5),
            defender: snapshot('DEFENDER', 'Centaur', 14, 5),
        },
        {type: 'Move', actor: 'ATTACKER', fromQ: 0, fromR: 5, toQ: 4, toR: 5},
        {
            type: 'BattleEnd',
            winner: 'ATTACKER',
            attackerSurvivors: 10,
            defenderSurvivors: 0,
            turns: 1,
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
