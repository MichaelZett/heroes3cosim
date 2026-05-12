import type {de} from './de';

type Dictionary = typeof de;

export const en: Dictionary = {
    common: {
        factionLabel: 'Faction',
        factionAll: 'All factions',
        tierLabel: 'Tier',
        tierAll: 'All tiers',
        tierN: 'Tier {{tier}}',
        unitLabel: 'Unit',
        unitChoose: '— pick one —',
        countLabel: 'Count',
        upgradeBadge: 'Upgrade',
        statAttackDefense: 'Attack / Defense',
        statHealth: 'HP',
        statDamage: 'Damage',
        statDamageRange: '{{min}}–{{max}}',
        statShotsSuffix: ' ({{shots}} shots)',
        statSpeed: 'Speed',
        statSpeedFlying: 'flies',
        statSpeedGround: 'walks',
        statCost: 'Cost',
        statCostValue: '{{cost}} G',
    },
    faction: {
        CASTLE: 'Castle',
        RAMPART: 'Rampart',
        TOWER: 'Tower',
        INFERNO: 'Inferno',
        NECROPOLIS: 'Necropolis',
        DUNGEON: 'Dungeon',
        STRONGHOLD: 'Stronghold',
        FORTRESS: 'Fortress',
        CONFLUX: 'Conflux',
        NEUTRAL: 'Neutral',
    },
    config: {
        title: 'Heroes 3 Combat Simulator',
        subtitle: 'Pick two armies, optionally set a seed, and start the battle.',
        loading: 'Loading catalog…',
        apiDown: 'API unreachable — is the backend running on {{url}}?',
        attackerTitle: 'Army 1 (Attacker)',
        defenderTitle: 'Army 2 (Defender)',
        seedTitle: 'Seed',
        seedHint: 'Optional — leave blank for a random seed',
        seedPlaceholder: 'e.g. 42',
        rollSeed: 'Roll',
        simulationFailed: 'Simulation failed: {{message}}',
        simulating: 'Simulating…',
        startBattle: 'Start battle',
    },
    battle: {
        title: 'Battle replay',
        backToConfig: '← New configuration',
        combatLog: 'Combat log',
        of: 'of {{total}}',
        fieldAria: 'Hex battlefield',
        waitingForReplay: 'Replay is about to start…',
    },
    playback: {
        speedLabel: 'Speed',
        play: '▶ Play',
        pause: '⏸ Pause',
        step: '⏭ Step',
        restart: '↻ Restart',
    },
    events: {
        battleStart:
            'Battle begins: <actor>{{attacker}}</actor> ({{attackerCount}}×) vs. <target>{{defender}}</target> ({{defenderCount}}×).',
        move: '<actor>{{actor}}</actor> moves from ({{fromQ}},{{fromR}}) to ({{toQ}},{{toR}}).',
        moveBack: '<actor>{{actor}}</actor> flies back to ({{toQ}},{{toR}}).',
        wait: '<actor>{{actor}}</actor> waits.',
        shoot:
            '<actor>{{actor}}</actor> shoots <target>{{target}}</target> from distance {{distance}} — {{damage}} damage, {{killed}} killed.',
        melee:
            '<actor>{{actor}}</actor> attacks <target>{{target}}</target> in melee — {{damage}} damage, {{killed}} killed.',
        retaliation:
            '<retaliator>{{retaliator}}</retaliator> retaliates — {{damage}} damage, {{killed}} killed.',
        twoBlows: '<actor>{{actor}}</actor> strikes a second time.',
        twoShots: '<actor>{{actor}}</actor> shoots a second time.',
        goodMorale: '<actor>{{actor}}</actor> has good morale and attacks again.',
        deathStare:
            '<actor>{{actor}}</actor> kills {{kills}} of <target>{{target}}</target> with Death Stare.',
        thunderbolts:
            '<actor>{{actor}}</actor> deals {{damage}} thunderbolt damage to <target>{{target}}</target>.',
        petrifying: '<actor>{{actor}}</actor> petrifies <target>{{target}}</target>.',
        cursing: '<actor>{{actor}}</actor> curses <target>{{target}}</target>.',
        poisoning: '<actor>{{actor}}</actor> poisons <target>{{target}}</target>.',
        diseasing: '<actor>{{actor}}</actor> diseases <target>{{target}}</target>.',
        aging: '<actor>{{actor}}</actor> ages <target>{{target}}</target> — half HP.',
        fireShield:
            '<shielded>{{shielded}}</shielded> reflects {{damage}} damage via fire shield onto <attacker>{{attacker}}</attacker>.',
        rebirth: '<actor>{{actor}}</actor> is reborn with {{count}} creatures.',
        battleEnd:
            'Battle over — winner: <winner>{{winner}}</winner> after {{turns}} turns. Survivors: {{attackerSurvivors}} vs. {{defenderSurvivors}}.',
        draw: 'Draw',
    },
    langSwitcher: {
        label: 'Language',
        de: 'Deutsch',
        en: 'English',
    },
};
