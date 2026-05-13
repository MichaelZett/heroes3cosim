package de.zettsystems.h3comsim.battle.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.zettsystems.h3comsim.battle.domain.AttackType.HAND_TO_HAND;
import static de.zettsystems.h3comsim.battle.domain.AttackType.LONG_RANGE;
import static de.zettsystems.h3comsim.battle.domain.Faction.CASTLE;
import static de.zettsystems.h3comsim.battle.domain.Faction.CONFLUX;
import static de.zettsystems.h3comsim.battle.domain.Faction.DUNGEON;
import static de.zettsystems.h3comsim.battle.domain.Faction.FORTRESS;
import static de.zettsystems.h3comsim.battle.domain.Faction.INFERNO;
import static de.zettsystems.h3comsim.battle.domain.Faction.NECROPOLIS;
import static de.zettsystems.h3comsim.battle.domain.Faction.NEUTRAL;
import static de.zettsystems.h3comsim.battle.domain.Faction.RAMPART;
import static de.zettsystems.h3comsim.battle.domain.Faction.STRONGHOLD;
import static de.zettsystems.h3comsim.battle.domain.Faction.TOWER;
import static de.zettsystems.h3comsim.battle.domain.Movement.FLYING;
import static de.zettsystems.h3comsim.battle.domain.Movement.GROUND;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.AGING;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.ANGEL_HATE;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.ANGEL_RACE;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.ATTACKS_WALLS;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.CASTS_BLOODLUST;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.COUNTERSTRIKE_TWICE;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.COUNTERSTRIKE_UNLIMITED;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.CURSING;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.DEATH_BLOW;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.DEATH_STARE;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.DEFENSE_REDUCTION_40;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.DEFENSE_REDUCTION_80;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.DEVIL_HATE;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.DEVIL_RACE;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.DISEASES;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.FIRE_SHIELD;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.GOOD_ARMY_MORALE;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.GOOD_MORALE;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.IMMUNE_TO_BLIND;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.IMMUNE_TO_SPELLS;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.IMMUNE_TO_SPELLS_BELOW_4;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.IMPACT_DAMAGE;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.MOVE_BACK;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.NO_HAND_TO_HAND_PENALTY;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.NO_OBSTACLE_PENALTY;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.NO_RETALIATION;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.PETRYFYING;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.POISONOUS;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.REBIRTH;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.REGENERATION;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.RESURRECTION;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.SPELL_COST_REDUCTION;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.THUNDERBOLTS;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.TITAN_HATE;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.TITAN_RACE;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.TWO_BLOWS;
import static de.zettsystems.h3comsim.battle.domain.UnitSpeciality.TWO_SHOTS;

public final class UnitCatalog {

    public static final Unit AIR_ELEMENTAL = basic("Air Elemental", new Stats(9, 9, 25, 7), melee(2, 8), GROUND, CONFLUX, 2, 250);
    public static final Unit ANCIENT_BEHEMOTH = upgrade("Ancient Behemoth", new Stats(19, 19, 300, 9), melee(30, 50), GROUND, STRONGHOLD, 7, 3000, DEFENSE_REDUCTION_80);
    public static final Unit ANGEL = basic("Angel", new Stats(20, 20, 200, 12), melee(50, 50), FLYING, CASTLE, 7, 3000, GOOD_ARMY_MORALE, DEVIL_HATE, ANGEL_RACE);
    public static final Unit ARCHER = basic("Archer", new Stats(6, 3, 10, 4), ranged(2, 3, 12), GROUND, CASTLE, 2, 100);
    public static final Unit ARCH_ANGEL = upgrade("Arch Angel", new Stats(30, 30, 250, 18), melee(50, 50), FLYING, CASTLE, 7, 5000, RESURRECTION, GOOD_ARMY_MORALE, DEVIL_HATE, ANGEL_RACE);
    public static final Unit ARCH_DEVIL = upgrade("Arch Devil", new Stats(26, 28, 200, 17), melee(30, 40), FLYING, INFERNO, 7, 4500, NO_RETALIATION, ANGEL_HATE, DEVIL_RACE);
    public static final Unit ARCH_MAGI = upgrade("Arch Magi", new Stats(12, 9, 30, 7), ranged(7, 9, 24), GROUND, TOWER, 4, 350, NO_OBSTACLE_PENALTY, SPELL_COST_REDUCTION, NO_HAND_TO_HAND_PENALTY);
    public static final Unit BASILISK = basic("Basilisk", new Stats(11, 11, 35, 5), melee(6, 10), GROUND, FORTRESS, 4, 325, PETRYFYING);
    public static final Unit BATTLE_DWARF = upgrade("Battle Dwarf", new Stats(7, 7, 20, 5), melee(2, 4), GROUND, RAMPART, 2, 150);
    public static final Unit BEHEMOTH = basic("Behemoth", new Stats(17, 17, 160, 6), melee(30, 50), GROUND, STRONGHOLD, 7, 1500, DEFENSE_REDUCTION_40);
    public static final Unit BEHOLDER = basic("Beholder", new Stats(9, 7, 22, 5), ranged(3, 5, 12), GROUND, DUNGEON, 3, 250, NO_HAND_TO_HAND_PENALTY);
    public static final Unit BLACK_DRAGON = upgrade("Black Dragon", new Stats(25, 25, 300, 15), melee(40, 50), FLYING, DUNGEON, 7, 4000, IMMUNE_TO_SPELLS, TITAN_HATE);
    public static final Unit BLACK_KNIGHT = basic("Black Knight", new Stats(16, 16, 120, 7), melee(15, 30), GROUND, NECROPOLIS, 6, 1200, CURSING);
    public static final Unit BONE_DRAGON = basic("Bone Dragon", new Stats(17, 15, 150, 9), melee(25, 50), FLYING, NECROPOLIS, 7, 1800);
    public static final Unit CAVALIER = basic("Cavalier", new Stats(15, 15, 100, 7), melee(15, 25), GROUND, CASTLE, 6, 1000, IMPACT_DAMAGE);
    public static final Unit CENTAUR = basic("Centaur", new Stats(5, 3, 8, 6), melee(2, 3), GROUND, RAMPART, 1, 70);
    public static final Unit CENTAUR_CAPTAIN = upgrade("Centaur Captain", new Stats(6, 3, 10, 8), melee(2, 3), GROUND, RAMPART, 1, 90);
    public static final Unit CERBERUS = upgrade("Cerberus", new Stats(10, 8, 25, 8), melee(2, 5), GROUND, INFERNO, 3, 250, NO_RETALIATION);
    public static final Unit CHAMPION = upgrade("Champion", new Stats(16, 16, 100, 9), melee(20, 25), GROUND, CASTLE, 6, 1200, IMPACT_DAMAGE);
    public static final Unit CHAOS_HYDRA = upgrade("Chaos Hydra", new Stats(18, 20, 250, 7), melee(25, 45), GROUND, FORTRESS, 7, 3500, NO_RETALIATION);
    public static final Unit CRUSADER = upgrade("Crusader", new Stats(12, 12, 35, 6), melee(7, 10), GROUND, CASTLE, 4, 400, TWO_BLOWS);
    public static final Unit CYCLOPS = basic("Cyclops", new Stats(15, 12, 70, 6), ranged(16, 20, 16), GROUND, STRONGHOLD, 6, 750, ATTACKS_WALLS);
    public static final Unit CYCLOPS_KING = upgrade("Cyclops King", new Stats(17, 13, 70, 8), ranged(16, 20, 24), GROUND, STRONGHOLD, 6, 1100, ATTACKS_WALLS);
    public static final Unit DEMON = basic("Demon", new Stats(10, 10, 35, 5), melee(7, 9), GROUND, INFERNO, 4, 250);
    public static final Unit DENDROID_GUARD = basic("Dendroid Guard", new Stats(9, 12, 55, 3), melee(10, 14), GROUND, RAMPART, 5, 350);
    public static final Unit DENDROID_SOLDIER = upgrade("Dendroid Soldier", new Stats(9, 12, 65, 4), melee(10, 14), GROUND, RAMPART, 5, 425);
    public static final Unit DEVIL = basic("Devil", new Stats(19, 21, 160, 11), melee(30, 40), FLYING, INFERNO, 7, 2700, NO_RETALIATION, ANGEL_HATE, DEVIL_RACE);
    public static final Unit DRAGON_FLY = upgrade("Dragon Fly", new Stats(6, 8, 20, 13), melee(2, 5), FLYING, FORTRESS, 3, 240);
    public static final Unit DREAD_KNIGHT = upgrade("Dread Knight", new Stats(18, 18, 120, 9), melee(15, 30), GROUND, NECROPOLIS, 6, 1500, DEATH_BLOW, CURSING);
    public static final Unit DWARF = basic("Dwarf", new Stats(6, 7, 20, 3), melee(2, 4), GROUND, RAMPART, 2, 120);
    public static final Unit EARTH_ELEMENTAL = basic("Earth Elemental", new Stats(10, 10, 40, 4), melee(4, 8), GROUND, CONFLUX, 5, 400);
    public static final Unit EFREET = basic("Efreet", new Stats(16, 12, 90, 9), melee(16, 24), FLYING, INFERNO, 6, 900);
    public static final Unit EFREET_SULTAN = upgrade("Efreet Sultan", new Stats(16, 14, 90, 13), melee(16, 24), FLYING, INFERNO, 6, 1100, FIRE_SHIELD);
    public static final Unit ENERGY_ELEMENTAL = upgrade("Energy Elemental", new Stats(12, 8, 35, 8), melee(4, 6), FLYING, CONFLUX, 4, 400);
    public static final Unit EVIL_EYE = upgrade("Evil Eye", new Stats(10, 8, 22, 7), ranged(3, 5, 24), GROUND, DUNGEON, 3, 280, NO_HAND_TO_HAND_PENALTY);
    public static final Unit FAMILIAR = upgrade("Familiar", new Stats(4, 4, 4, 7), melee(1, 2), GROUND, INFERNO, 1, 60);
    public static final Unit FIREBIRD = basic("Firebird", new Stats(18, 18, 150, 15), melee(30, 40), FLYING, CONFLUX, 7, 1500);
    public static final Unit FIRE_ELEMENTAL = basic("Fire Elemental", new Stats(10, 8, 35, 6), melee(4, 6), GROUND, CONFLUX, 4, 350);
    public static final Unit GENIE = basic("Genie", new Stats(12, 12, 40, 7), melee(13, 16), FLYING, TOWER, 5, 550);
    public static final Unit GHOST_DRAGON = upgrade("Ghost Dragon", new Stats(19, 17, 200, 14), melee(25, 50), FLYING, NECROPOLIS, 7, 3000, AGING);
    public static final Unit GIANT = basic("Giant", new Stats(19, 16, 150, 7), melee(40, 60), GROUND, TOWER, 7, 2000);
    public static final Unit GNOLL = basic("Gnoll", new Stats(3, 5, 6, 4), melee(2, 3), GROUND, FORTRESS, 1, 50);
    public static final Unit GNOLL_MARAUDER = upgrade("Gnoll Marauder", new Stats(4, 6, 6, 5), melee(2, 3), GROUND, FORTRESS, 1, 70);
    public static final Unit GOBLIN = basic("Goblin", new Stats(4, 2, 5, 5), melee(1, 2), GROUND, STRONGHOLD, 1, 40);
    public static final Unit GOG = basic("Gog", new Stats(6, 4, 13, 4), ranged(2, 4, 12), GROUND, INFERNO, 2, 125);
    public static final Unit GOLD_DRAGON = upgrade("Gold Dragon", new Stats(27, 27, 250, 16), melee(40, 50), FLYING, RAMPART, 7, 4000, IMMUNE_TO_SPELLS_BELOW_4);
    public static final Unit GORGON = basic("Gorgon", new Stats(10, 14, 70, 5), melee(12, 16), GROUND, FORTRESS, 5, 525);
    public static final Unit GRAND_ELF = upgrade("Grand Elf", new Stats(9, 5, 15, 7), ranged(3, 5, 24), GROUND, RAMPART, 3, 250, TWO_SHOTS);
    public static final Unit GREATER_BASILISK = upgrade("Greater Basilisk", new Stats(12, 12, 40, 7), melee(6, 10), GROUND, FORTRESS, 4, 400, PETRYFYING);
    public static final Unit GREEN_DRAGON = basic("Green Dragon", new Stats(18, 18, 180, 10), melee(40, 50), FLYING, RAMPART, 7, 2400);
    public static final Unit GREMLIN = basic("Gremlin", new Stats(3, 3, 4, 4), melee(1, 2), GROUND, TOWER, 1, 30);
    public static final Unit GRIFFIN = basic("Griffin", new Stats(8, 8, 25, 6), melee(3, 6), FLYING, CASTLE, 3, 200, COUNTERSTRIKE_TWICE);
    public static final Unit HALBERDIER = upgrade("Halberdier", new Stats(6, 5, 10, 5), melee(2, 3), GROUND, CASTLE, 1, 75);
    public static final Unit HARPY = basic("Harpy", new Stats(6, 5, 14, 6), melee(1, 4), FLYING, DUNGEON, 2, 130, MOVE_BACK);
    public static final Unit HARPY_HAG = upgrade("Harpy Hag", new Stats(6, 6, 14, 9), melee(1, 4), FLYING, DUNGEON, 2, 170, MOVE_BACK, NO_RETALIATION);
    public static final Unit HELL_HOUND = basic("Hell Hound", new Stats(10, 6, 25, 7), melee(2, 7), GROUND, INFERNO, 3, 200);
    public static final Unit HOBGOBLIN = upgrade("Hobgoblin", new Stats(5, 3, 5, 7), melee(1, 2), GROUND, STRONGHOLD, 1, 50);
    public static final Unit HORNED_DEMON = upgrade("Horned Demon", new Stats(10, 10, 40, 6), melee(7, 9), GROUND, INFERNO, 4, 270);
    public static final Unit HYDRA = basic("Hydra", new Stats(16, 18, 175, 5), melee(25, 45), GROUND, FORTRESS, 7, 2200, NO_RETALIATION);
    public static final Unit ICE_ELEMENTAL = upgrade("Ice Elemental", new Stats(8, 10, 30, 6), ranged(3, 7, 24), GROUND, CONFLUX, 3, 375);
    public static final Unit IMP = basic("Imp", new Stats(2, 3, 4, 5), melee(1, 2), GROUND, INFERNO, 1, 50);
    public static final Unit INFERNAL_TROGLODYTE = upgrade("Infernal Troglodyte", new Stats(5, 4, 6, 5), melee(1, 3), GROUND, DUNGEON, 1, 65, IMMUNE_TO_BLIND);
    public static final Unit IRON_GOLEM = upgrade("Iron Golem", new Stats(9, 10, 35, 5), melee(4, 5), GROUND, TOWER, 3, 200);
    public static final Unit LICH = basic("Lich", new Stats(13, 10, 30, 6), ranged(11, 13, 12), GROUND, NECROPOLIS, 5, 550);
    public static final Unit LIZARDMAN = basic("Lizardman", new Stats(5, 6, 12, 4), ranged(1, 3, 12), GROUND, FORTRESS, 2, 110);
    public static final Unit LIZARD_WARRIOR = upgrade("Lizard Warrior", new Stats(5, 7, 12, 5), ranged(2, 3, 24), GROUND, FORTRESS, 2, 130);
    public static final Unit MAGI = basic("Magi", new Stats(11, 8, 25, 5), ranged(7, 9, 24), GROUND, TOWER, 4, 350, NO_HAND_TO_HAND_PENALTY, NO_OBSTACLE_PENALTY);
    public static final Unit MAGIC_ELEMENTAL = upgrade("Magic Elemental", new Stats(15, 13, 80, 9), melee(15, 25), GROUND, CONFLUX, 6, 800, IMMUNE_TO_SPELLS);
    public static final Unit MAGMA_ELEMENTAL = upgrade("Magma Elemental", new Stats(11, 11, 40, 6), melee(6, 10), GROUND, CONFLUX, 5, 500);
    public static final Unit MAGOG = upgrade("Magog", new Stats(7, 4, 13, 6), ranged(2, 4, 24), GROUND, INFERNO, 2, 175);
    public static final Unit MANTICORE = basic("Manticore", new Stats(15, 13, 80, 7), melee(14, 20), FLYING, DUNGEON, 6, 850);
    public static final Unit MARKSMAN = upgrade("Marksman", new Stats(6, 3, 10, 6), ranged(2, 3, 24), GROUND, CASTLE, 2, 150, TWO_SHOTS);
    public static final Unit MASTER_GENIE = upgrade("Master Genie", new Stats(12, 12, 40, 11), melee(13, 16), FLYING, TOWER, 5, 600);
    public static final Unit MASTER_GREMLIN = upgrade("Master Gremlin", new Stats(4, 4, 4, 5), ranged(1, 2, 8), GROUND, TOWER, 1, 40);
    public static final Unit MEDUSA = basic("Medusa", new Stats(9, 9, 25, 5), ranged(6, 8, 4), GROUND, DUNGEON, 4, 300, PETRYFYING, NO_HAND_TO_HAND_PENALTY);
    public static final Unit MEDUSA_QUEEN = upgrade("Medusa Queen", new Stats(10, 10, 30, 6), ranged(6, 8, 8), GROUND, DUNGEON, 4, 330, PETRYFYING, NO_HAND_TO_HAND_PENALTY);
    public static final Unit MIGHTY_GORGON = upgrade("Mighty Gorgon", new Stats(11, 16, 70, 6), melee(12, 16), GROUND, FORTRESS, 5, 600, DEATH_STARE);
    public static final Unit MINOTAUR = basic("Minotaur", new Stats(14, 12, 50, 6), melee(12, 20), GROUND, DUNGEON, 5, 500, GOOD_MORALE);
    public static final Unit MINOTAUR_KING = upgrade("Minotaur King", new Stats(15, 15, 50, 8), melee(12, 20), GROUND, DUNGEON, 5, 575, GOOD_MORALE);
    public static final Unit MONK = basic("Monk", new Stats(12, 7, 30, 5), ranged(10, 12, 12), GROUND, CASTLE, 5, 400);
    public static final Unit NAGA = basic("Naga", new Stats(16, 13, 110, 5), melee(20, 20), GROUND, TOWER, 6, 1100, NO_RETALIATION);
    public static final Unit NAGA_QUEEN = upgrade("Naga Queen", new Stats(16, 13, 110, 7), melee(30, 30), GROUND, TOWER, 6, 1600, NO_RETALIATION);
    public static final Unit OBSIDIAN_GARGOYLE = upgrade("Obsidian Gargoyle", new Stats(7, 7, 16, 9), melee(2, 3), FLYING, TOWER, 2, 160);
    public static final Unit OGRE = basic("Ogre", new Stats(13, 7, 40, 4), melee(6, 12), GROUND, STRONGHOLD, 4, 300);
    public static final Unit OGRE_MAGI = upgrade("Ogre Magi", new Stats(13, 7, 60, 5), melee(6, 12), GROUND, STRONGHOLD, 4, 400, CASTS_BLOODLUST);
    public static final Unit ORC = basic("Orc", new Stats(8, 4, 15, 4), ranged(2, 5, 12), GROUND, STRONGHOLD, 3, 150);
    public static final Unit ORC_CHIEFTAIN = upgrade("Orc Chieftain", new Stats(8, 4, 20, 5), ranged(2, 5, 24), GROUND, STRONGHOLD, 3, 165);
    public static final Unit PEASANT = basic("Peasant", new Stats(1, 1, 1, 3), melee(1, 1), GROUND, NEUTRAL, 1, 10);
    public static final Unit PEGASUS = basic("Pegasus", new Stats(9, 8, 30, 8), melee(5, 9), FLYING, RAMPART, 4, 250);
    public static final Unit PHOENIX = upgrade("Phoenix", new Stats(21, 18, 200, 21), melee(30, 40), FLYING, CONFLUX, 7, 2000, REBIRTH);
    public static final Unit PIKEMAN = basic("Pikeman", new Stats(4, 5, 10, 4), melee(1, 3), GROUND, CASTLE, 1, 60);
    public static final Unit PIT_FIEND = basic("Pit Fiend", new Stats(13, 13, 45, 6), melee(13, 17), GROUND, INFERNO, 5, 500);
    public static final Unit PIXIE = basic("Pixie", new Stats(2, 2, 3, 7), melee(1, 2), FLYING, CONFLUX, 1, 25);
    public static final Unit POWER_LICH = upgrade("Power Lich", new Stats(13, 10, 40, 7), ranged(11, 15, 24), GROUND, NECROPOLIS, 5, 600);
    public static final Unit PSYCHIC_ELEMENTAL = basic("Psychic Elemental", new Stats(15, 13, 75, 7), melee(10, 20), GROUND, CONFLUX, 6, 750);
    public static final Unit RED_DRAGON = basic("Red Dragon", new Stats(19, 19, 180, 11), melee(40, 50), FLYING, DUNGEON, 7, 2500, IMMUNE_TO_SPELLS_BELOW_4);
    public static final Unit ROC = basic("Roc", new Stats(13, 11, 60, 7), melee(11, 15), FLYING, STRONGHOLD, 5, 600);
    public static final Unit ROYAL_GRIFFIN = upgrade("Royal Griffin", new Stats(9, 9, 25, 9), melee(3, 6), FLYING, CASTLE, 3, 240, COUNTERSTRIKE_UNLIMITED);
    public static final Unit SCORPICORE = upgrade("Scorpicore", new Stats(16, 14, 80, 11), melee(14, 20), FLYING, DUNGEON, 6, 1050, PETRYFYING);
    public static final Unit SERPENT_FLY = basic("Serpent Fly", new Stats(6, 8, 20, 9), melee(2, 5), FLYING, FORTRESS, 3, 220);
    public static final Unit SILVER_PEGASUS = upgrade("Silver Pegasus", new Stats(9, 10, 30, 12), melee(5, 9), FLYING, RAMPART, 4, 275);
    public static final Unit SKELETON = basic("Skeleton", new Stats(5, 4, 6, 4), melee(1, 3), GROUND, NECROPOLIS, 1, 60);
    public static final Unit SKELETON_WARRIOR = upgrade("Skeleton Warrior", new Stats(6, 6, 6, 5), melee(1, 3), GROUND, NECROPOLIS, 1, 70);
    public static final Unit SPRITE = upgrade("Sprite", new Stats(2, 2, 3, 9), melee(1, 3), FLYING, CONFLUX, 1, 30, NO_RETALIATION);
    public static final Unit STONE_GARGOYLE = basic("Stone Gargoyle", new Stats(6, 6, 16, 6), melee(2, 3), FLYING, TOWER, 2, 130);
    public static final Unit STONE_GOLEM = basic("Stone Golem", new Stats(7, 10, 30, 3), melee(4, 5), GROUND, TOWER, 3, 150);
    public static final Unit STORM_ELEMENTAL = upgrade("Storm Elemental", new Stats(9, 9, 25, 8), ranged(2, 8, 24), GROUND, CONFLUX, 2, 275);
    public static final Unit SWORDSMAN = basic("Swordsman", new Stats(10, 12, 35, 5), melee(6, 9), GROUND, CASTLE, 4, 300);
    public static final Unit THUNDERBIRD = upgrade("Thunderbird", new Stats(13, 11, 60, 11), melee(11, 15), FLYING, STRONGHOLD, 5, 700, THUNDERBOLTS);
    public static final Unit TITAN = upgrade("Titan", new Stats(24, 24, 300, 11), ranged(40, 60, 24), GROUND, TOWER, 7, 5000, NO_HAND_TO_HAND_PENALTY, TITAN_RACE);
    public static final Unit TROGLODYTE = basic("Troglodyte", new Stats(4, 3, 5, 4), melee(1, 3), GROUND, DUNGEON, 1, 50, IMMUNE_TO_BLIND);
    public static final Unit UNICORN = basic("Unicorn", new Stats(15, 14, 90, 7), melee(18, 22), GROUND, RAMPART, 6, 850);
    public static final Unit VAMPIRE = basic("Vampire", new Stats(10, 9, 30, 6), melee(5, 8), FLYING, NECROPOLIS, 4, 360, NO_RETALIATION);
    public static final Unit VAMPIRE_LORD = upgrade("Vampire Lord", new Stats(10, 10, 40, 9), melee(5, 8), FLYING, NECROPOLIS, 4, 500, NO_RETALIATION);
    public static final Unit WALKING_DEAD = basic("Walking Dead", new Stats(5, 5, 15, 3), melee(2, 3), GROUND, NECROPOLIS, 2, 100);
    public static final Unit WAR_UNICORN = upgrade("War Unicorn", new Stats(15, 14, 110, 9), melee(18, 22), GROUND, RAMPART, 6, 950);
    public static final Unit WATER_ELEMENTAL = basic("Water Elemental", new Stats(8, 10, 30, 5), melee(3, 7), GROUND, CONFLUX, 3, 300);
    public static final Unit WIGHT = basic("Wight", new Stats(7, 7, 18, 5), melee(3, 5), FLYING, NECROPOLIS, 3, 200, REGENERATION);
    public static final Unit WOLF_RAIDER = upgrade("Wolf Raider", new Stats(8, 5, 10, 8), melee(3, 4), GROUND, STRONGHOLD, 2, 140, TWO_BLOWS);
    public static final Unit WOLF_RIDER = basic("Wolf Rider", new Stats(7, 5, 10, 6), melee(2, 4), GROUND, STRONGHOLD, 2, 100);
    public static final Unit WOOD_ELF = basic("Wood Elf", new Stats(9, 5, 15, 6), ranged(3, 5, 24), GROUND, RAMPART, 3, 200);
    public static final Unit WRAITH = upgrade("Wraith", new Stats(7, 7, 18, 7), melee(3, 5), FLYING, NECROPOLIS, 3, 230, REGENERATION);
    public static final Unit WYVERN = basic("Wyvern", new Stats(14, 14, 70, 7), melee(14, 18), FLYING, FORTRESS, 6, 800);
    public static final Unit WYVERN_MONARCH = upgrade("Wyvern Monarch", new Stats(14, 14, 70, 11), melee(18, 22), FLYING, FORTRESS, 6, 1100, POISONOUS);
    public static final Unit ZEALOT = upgrade("Zealot", new Stats(12, 10, 30, 7), ranged(10, 12, 24), GROUND, CASTLE, 5, 450, NO_HAND_TO_HAND_PENALTY);
    public static final Unit ZOMBIE = upgrade("Zombie", new Stats(5, 5, 20, 4), melee(2, 3), GROUND, NECROPOLIS, 2, 125, DISEASES);

    private static final List<Unit> ALL = List.of(
            AIR_ELEMENTAL, ANCIENT_BEHEMOTH, ANGEL, ARCHER, ARCH_ANGEL, ARCH_DEVIL, ARCH_MAGI,
            BASILISK, BATTLE_DWARF, BEHEMOTH, BEHOLDER, BLACK_DRAGON, BLACK_KNIGHT, BONE_DRAGON,
            CAVALIER, CENTAUR, CENTAUR_CAPTAIN, CERBERUS, CHAMPION, CHAOS_HYDRA, CRUSADER,
            CYCLOPS, CYCLOPS_KING,
            DEMON, DENDROID_GUARD, DENDROID_SOLDIER, DEVIL, DRAGON_FLY, DREAD_KNIGHT, DWARF,
            EARTH_ELEMENTAL, EFREET, EFREET_SULTAN, ENERGY_ELEMENTAL, EVIL_EYE,
            FAMILIAR, FIREBIRD, FIRE_ELEMENTAL,
            GENIE, GHOST_DRAGON, GIANT, GNOLL, GNOLL_MARAUDER, GOBLIN, GOG, GOLD_DRAGON, GORGON,
            GRAND_ELF, GREATER_BASILISK, GREEN_DRAGON, GREMLIN, GRIFFIN,
            HALBERDIER, HARPY, HARPY_HAG, HELL_HOUND, HOBGOBLIN, HORNED_DEMON, HYDRA,
            ICE_ELEMENTAL, IMP, INFERNAL_TROGLODYTE, IRON_GOLEM,
            LICH, LIZARDMAN, LIZARD_WARRIOR,
            MAGI, MAGIC_ELEMENTAL, MAGMA_ELEMENTAL, MAGOG, MANTICORE, MARKSMAN,
            MASTER_GENIE, MASTER_GREMLIN, MEDUSA, MEDUSA_QUEEN,
            MIGHTY_GORGON, MINOTAUR, MINOTAUR_KING, MONK,
            NAGA, NAGA_QUEEN,
            OBSIDIAN_GARGOYLE, OGRE, OGRE_MAGI, ORC, ORC_CHIEFTAIN,
            PEASANT, PEGASUS, PHOENIX, PIKEMAN, PIT_FIEND, PIXIE, POWER_LICH, PSYCHIC_ELEMENTAL,
            RED_DRAGON, ROC, ROYAL_GRIFFIN,
            SCORPICORE, SERPENT_FLY, SILVER_PEGASUS, SKELETON, SKELETON_WARRIOR, SPRITE,
            STONE_GARGOYLE, STONE_GOLEM, STORM_ELEMENTAL, SWORDSMAN,
            THUNDERBIRD, TITAN, TROGLODYTE,
            UNICORN, VAMPIRE, VAMPIRE_LORD,
            WALKING_DEAD, WAR_UNICORN, WATER_ELEMENTAL, WIGHT, WOLF_RAIDER, WOLF_RIDER, WOOD_ELF,
            WRAITH, WYVERN, WYVERN_MONARCH,
            ZEALOT, ZOMBIE);

    private static final Map<String, Unit> BY_NAME = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(Unit::name, u -> u));

    private UnitCatalog() {
    }

    public static List<Unit> all() {
        return ALL;
    }

    public static Optional<Unit> byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }

    public static List<Unit> byFaction(Faction faction) {
        return ALL.stream()
                .filter(u -> u.faction() == faction)
                .toList();
    }

    private static Unit basic(String name, Stats stats, Combat combat, Movement movement,
                              Faction faction, int tier, int cost, UnitSpeciality... specialities) {
        return new Unit(name, stats, combat, movement, cost, faction, tier, false, Set.of(specialities));
    }

    private static Unit upgrade(String name, Stats stats, Combat combat, Movement movement,
                                Faction faction, int tier, int cost, UnitSpeciality... specialities) {
        return new Unit(name, stats, combat, movement, cost, faction, tier, true, Set.of(specialities));
    }

    private static Combat melee(int min, int max) {
        return new Combat(min, max, 0, HAND_TO_HAND);
    }

    private static Combat ranged(int min, int max, int shots) {
        return new Combat(min, max, shots, LONG_RANGE);
    }
}
