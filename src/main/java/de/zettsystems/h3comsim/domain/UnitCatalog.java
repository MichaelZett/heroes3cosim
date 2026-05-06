package de.zettsystems.h3comsim.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.zettsystems.h3comsim.domain.AttackType.HAND_TO_HAND;
import static de.zettsystems.h3comsim.domain.AttackType.LONG_RANGE;
import static de.zettsystems.h3comsim.domain.Movement.FLYING;
import static de.zettsystems.h3comsim.domain.Movement.GROUND;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.ANGEL_HATE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.ANGEL_RACE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.COUNERSTRIKE_UNLIMITED;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.COUNTERSTRIKE_TWICE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.CURSING;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.DEATH_BLOW;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.DEATH_STARE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.DEVIL_HATE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.DEVIL_RACE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.GOOD_ARMY_MORALE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.GOOD_MORALE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.IMMUNE_TO_BLIND;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.IMMUNE_TO_SPELLS;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.IMMUNE_TO_SPELLS_BELOW_4;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.IMPACT_DAMAGE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.MOVE_BACK;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.NO_HAND_TO_HAND_PENALTY;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.NO_OBSTACLE_PENALTY;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.NO_RETALIATION;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.PARALYZING_VENOM;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.PETRYFYING;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.RESURRECTION;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.SPELL_COST_REDUCTION;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.THUNDERBOLTS;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.TITAN_HATE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.TWO_BLOWS;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.TWO_SHOTS;

public final class UnitCatalog {

    public static final Unit ANGEL = unit("Angel", new Stats(20, 20, 200, 12), melee(50, 50), FLYING, 3000, GOOD_ARMY_MORALE, DEVIL_HATE, ANGEL_RACE);
    public static final Unit ARCHER = unit("Archer", new Stats(6, 3, 10, 4), ranged(2, 3, 12), GROUND, 100);
    public static final Unit ARCH_ANGEL = unit("Arch Angel", new Stats(30, 30, 250, 18), melee(50, 50), FLYING, 5000, RESURRECTION, GOOD_ARMY_MORALE, DEVIL_HATE, ANGEL_RACE);
    public static final Unit ARCH_DEVIL = unit("Arch Devil", new Stats(26, 28, 200, 17), melee(30, 40), FLYING, 4500, NO_RETALIATION, ANGEL_HATE, DEVIL_RACE);
    public static final Unit ARCH_MAGI = unit("Arch Magi", new Stats(12, 9, 30, 7), ranged(7, 9, 24), GROUND, 350, NO_OBSTACLE_PENALTY, SPELL_COST_REDUCTION, NO_HAND_TO_HAND_PENALTY);
    public static final Unit BASILISK = unit("Basilisk", new Stats(11, 11, 35, 5), melee(6, 10), GROUND, 325, PETRYFYING);
    public static final Unit BATTLE_DWARF = unit("Battle Dwarf", new Stats(7, 7, 20, 5), melee(2, 4), GROUND, 150);
    public static final Unit BEHOLDER = unit("Beholder", new Stats(9, 7, 22, 5), ranged(3, 5, 12), GROUND, 250, NO_HAND_TO_HAND_PENALTY);
    public static final Unit BLACK_DRAGON = unit("Black Dragon", new Stats(25, 25, 300, 15), melee(40, 50), FLYING, 4000, IMMUNE_TO_SPELLS, TITAN_HATE);
    public static final Unit BLACK_KNIGHT = unit("Black Knight", new Stats(16, 16, 120, 7), melee(15, 30), GROUND, 1200, CURSING);
    public static final Unit CAVALIER = unit("Cavalier", new Stats(15, 15, 100, 7), melee(15, 25), GROUND, 1000, IMPACT_DAMAGE);
    public static final Unit CENTAUR = unit("Centaur", new Stats(5, 3, 8, 6), melee(2, 3), GROUND, 70);
    public static final Unit CENTAUR_CAPTAIN = unit("Centaur Captain", new Stats(6, 3, 10, 8), melee(2, 3), GROUND, 90);
    public static final Unit CERBERUS = unit("Cerberus", new Stats(10, 8, 25, 8), melee(2, 5), GROUND, 250, NO_RETALIATION);
    public static final Unit CHAMPION = unit("Champion", new Stats(16, 16, 100, 9), melee(20, 25), GROUND, 1200, IMPACT_DAMAGE);
    public static final Unit CHAOS_HYDRA = unit("Chaos Hydra", new Stats(18, 20, 250, 7), melee(25, 45), GROUND, 3500, NO_RETALIATION);
    public static final Unit CRUSADER = unit("Crusader", new Stats(12, 12, 35, 6), melee(7, 10), GROUND, 400, TWO_BLOWS);
    public static final Unit DEMON = unit("Demon", new Stats(10, 10, 35, 5), melee(7, 9), GROUND, 250);
    public static final Unit DENDROID_GUARD = unit("Dendroid Guard", new Stats(9, 12, 55, 3), melee(10, 14), GROUND, 350);
    public static final Unit DENDROID_SOLDIER = unit("Dendroid Soldier", new Stats(9, 12, 65, 4), melee(10, 14), GROUND, 425);
    public static final Unit DEVIL = unit("Devil", new Stats(19, 21, 160, 11), melee(30, 40), FLYING, 2700, NO_RETALIATION, ANGEL_HATE, DEVIL_RACE);
    public static final Unit DREAD_KNIGHT = unit("Dread Knight", new Stats(18, 18, 120, 9), melee(15, 30), GROUND, 1500, DEATH_BLOW, CURSING);
    public static final Unit DWARF = unit("Dwarf", new Stats(6, 7, 20, 3), melee(2, 4), GROUND, 120);
    public static final Unit EVIL_EYE = unit("Evil Eye", new Stats(10, 8, 22, 7), ranged(3, 5, 24), GROUND, 280, NO_HAND_TO_HAND_PENALTY);
    public static final Unit FAMILIAR = unit("Familiar", new Stats(4, 4, 4, 7), melee(1, 2), GROUND, 60);
    public static final Unit GOG = unit("Gog", new Stats(6, 4, 13, 4), ranged(2, 4, 12), GROUND, 125);
    public static final Unit GOLD_DRAGON = unit("Gold Dragon", new Stats(27, 27, 250, 16), melee(40, 50), FLYING, 4000, IMMUNE_TO_SPELLS_BELOW_4);
    public static final Unit GRAND_ELF = unit("Grand Elf", new Stats(9, 5, 15, 7), ranged(3, 5, 24), GROUND, 250, TWO_SHOTS);
    public static final Unit GREATER_BASILISK = unit("Greater Basilisk", new Stats(12, 12, 40, 7), melee(6, 10), GROUND, 400, PETRYFYING);
    public static final Unit GREEN_DRAGON = unit("Green Dragon", new Stats(18, 18, 180, 10), melee(40, 50), FLYING, 2400);
    public static final Unit GRIFFIN = unit("Griffin", new Stats(8, 8, 25, 6), melee(3, 6), FLYING, 200, COUNTERSTRIKE_TWICE);
    public static final Unit HALBERDIER = unit("Halberdier", new Stats(6, 5, 10, 5), melee(2, 3), GROUND, 75);
    public static final Unit HARPY = unit("Harpy", new Stats(6, 5, 14, 6), melee(1, 4), FLYING, 130, MOVE_BACK);
    public static final Unit HARPY_HAG = unit("Harpy Hag", new Stats(6, 6, 14, 9), melee(1, 4), FLYING, 170, MOVE_BACK, NO_RETALIATION);
    public static final Unit HELL_HOUND = unit("Hell Hound", new Stats(10, 6, 25, 7), melee(2, 7), GROUND, 200);
    public static final Unit HORNED_DEMON = unit("Horned Demon", new Stats(10, 10, 40, 6), melee(7, 9), GROUND, 270);
    public static final Unit HYDRA = unit("Hydra", new Stats(16, 18, 175, 5), melee(25, 45), GROUND, 2200, NO_RETALIATION);
    public static final Unit IMP = unit("Imp", new Stats(2, 3, 4, 5), melee(1, 2), GROUND, 50);
    public static final Unit INFERNAL_TROGLODYTE = unit("Infernal Troglodyte", new Stats(5, 4, 6, 5), melee(1, 3), GROUND, 65, IMMUNE_TO_BLIND);
    public static final Unit MAGI = unit("Magi", new Stats(11, 8, 25, 5), ranged(7, 9, 24), GROUND, 350, NO_HAND_TO_HAND_PENALTY, NO_OBSTACLE_PENALTY);
    public static final Unit MAGOG = unit("Magog", new Stats(7, 4, 13, 6), ranged(2, 4, 24), GROUND, 175);
    public static final Unit MANTICORE = unit("Manticore", new Stats(15, 13, 80, 7), melee(14, 20), FLYING, 850);
    public static final Unit MARKSMAN = unit("Marksman", new Stats(6, 3, 10, 6), ranged(2, 3, 24), GROUND, 150, TWO_SHOTS);
    public static final Unit MEDUSA = unit("Medusa", new Stats(9, 9, 25, 5), ranged(6, 8, 4), GROUND, 300, PETRYFYING, NO_HAND_TO_HAND_PENALTY);
    public static final Unit MEDUSA_QUEEN = unit("Medusa Queen", new Stats(10, 10, 30, 6), ranged(6, 8, 8), GROUND, 330, PETRYFYING, NO_HAND_TO_HAND_PENALTY);
    public static final Unit MIGHTY_GORGON = unit("Mighty Gorgon", new Stats(11, 16, 70, 6), melee(12, 16), GROUND, 600, DEATH_STARE);
    public static final Unit MINOTAUR = unit("Minotaur", new Stats(14, 12, 50, 6), melee(12, 20), GROUND, 500, GOOD_MORALE);
    public static final Unit MINOTAUR_KING = unit("Minotaur King", new Stats(15, 15, 50, 8), melee(12, 20), GROUND, 575, GOOD_MORALE);
    public static final Unit MONK = unit("Monk", new Stats(12, 7, 30, 5), ranged(10, 12, 12), GROUND, 400);
    public static final Unit NAGA = unit("Naga", new Stats(16, 13, 110, 5), melee(20, 20), GROUND, 1100, NO_RETALIATION);
    public static final Unit NAGA_QUEEN = unit("Naga Queen", new Stats(16, 13, 110, 7), melee(30, 30), GROUND, 1600, NO_RETALIATION);
    public static final Unit PEASANT = unit("Peasant", new Stats(1, 1, 1, 3), melee(1, 1), GROUND, 10);
    public static final Unit PEGASUS = unit("Pegasus", new Stats(9, 8, 30, 8), melee(5, 9), FLYING, 250);
    public static final Unit PIKEMAN = unit("Pikeman", new Stats(4, 5, 10, 4), melee(1, 3), GROUND, 60);
    public static final Unit PIT_FIEND = unit("Pit Fiend", new Stats(13, 13, 45, 6), melee(13, 17), GROUND, 500);
    public static final Unit RED_DRAGON = unit("Red Dragon", new Stats(19, 19, 180, 11), melee(40, 50), FLYING, 2500, IMMUNE_TO_SPELLS_BELOW_4);
    public static final Unit ROYAL_GRIFFIN = unit("Royal Griffin", new Stats(9, 9, 25, 9), melee(3, 6), FLYING, 240, COUNERSTRIKE_UNLIMITED);
    public static final Unit SCORPICORE = unit("Scorpicore", new Stats(16, 14, 80, 11), melee(14, 20), FLYING, 1050, PARALYZING_VENOM);
    public static final Unit SILVER_PEGASUS = unit("Silver Pegasus", new Stats(9, 10, 30, 12), melee(5, 9), FLYING, 275);
    public static final Unit SWORDSMAN = unit("Swordsman", new Stats(10, 12, 35, 5), melee(6, 9), GROUND, 300);
    public static final Unit THUNDERBIRD = unit("Thunderbird", new Stats(13, 11, 60, 11), melee(11, 15), FLYING, 700, THUNDERBOLTS);
    public static final Unit TITAN = unit("Titan", new Stats(24, 24, 300, 11), ranged(40, 60, 24), GROUND, 5000, NO_HAND_TO_HAND_PENALTY);
    public static final Unit TROGLODYTE = unit("Troglodyte", new Stats(4, 3, 5, 4), melee(1, 3), GROUND, 50, IMMUNE_TO_BLIND);
    public static final Unit UNICORN = unit("Unicorn", new Stats(15, 14, 90, 7), melee(18, 22), GROUND, 850);
    public static final Unit VAMPIRE = unit("Vampire", new Stats(10, 9, 30, 6), melee(5, 8), FLYING, 360, NO_RETALIATION);
    public static final Unit VAMPIRE_LORD = unit("Vampire Lord", new Stats(10, 10, 40, 9), melee(5, 8), FLYING, 500, NO_RETALIATION);
    public static final Unit WAR_UNICORN = unit("War Unicorn", new Stats(15, 14, 110, 9), melee(18, 22), GROUND, 950);
    public static final Unit WOLF_RAIDER = unit("Wolf Raider", new Stats(8, 5, 10, 8), melee(3, 4), GROUND, 140, TWO_BLOWS);
    public static final Unit WOOD_ELF = unit("Wood Elf", new Stats(9, 5, 15, 6), ranged(3, 5, 24), GROUND, 200);
    public static final Unit WYVERN_MONARCH = unit("Wyvern Monarch", new Stats(14, 14, 70, 11), melee(18, 22), FLYING, 1100, PARALYZING_VENOM);
    public static final Unit ZEALOT = unit("Zealot", new Stats(12, 10, 30, 7), ranged(10, 12, 24), GROUND, 450, NO_HAND_TO_HAND_PENALTY);

    private static final List<Unit> ALL = List.of(
            ANGEL, ARCHER, ARCH_ANGEL, ARCH_DEVIL, ARCH_MAGI,
            BASILISK, BATTLE_DWARF, BEHOLDER, BLACK_DRAGON, BLACK_KNIGHT,
            CAVALIER, CENTAUR, CENTAUR_CAPTAIN, CERBERUS, CHAMPION, CHAOS_HYDRA, CRUSADER,
            DEMON, DENDROID_GUARD, DENDROID_SOLDIER, DEVIL, DREAD_KNIGHT, DWARF,
            EVIL_EYE, FAMILIAR, GOG, GOLD_DRAGON, GRAND_ELF, GREATER_BASILISK, GREEN_DRAGON, GRIFFIN,
            HALBERDIER, HARPY, HARPY_HAG, HELL_HOUND, HORNED_DEMON, HYDRA,
            IMP, INFERNAL_TROGLODYTE, MAGI, MAGOG, MANTICORE, MARKSMAN, MEDUSA, MEDUSA_QUEEN,
            MIGHTY_GORGON, MINOTAUR, MINOTAUR_KING, MONK, NAGA, NAGA_QUEEN,
            PEASANT, PEGASUS, PIKEMAN, PIT_FIEND, RED_DRAGON, ROYAL_GRIFFIN,
            SCORPICORE, SILVER_PEGASUS, SWORDSMAN, THUNDERBIRD, TITAN, TROGLODYTE,
            UNICORN, VAMPIRE, VAMPIRE_LORD, WAR_UNICORN, WOLF_RAIDER, WOOD_ELF, WYVERN_MONARCH, ZEALOT);

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

    private static Unit unit(String name, Stats stats, Combat combat, Movement movement,
                             int cost, UnitSpeciality... specialities) {
        return new Unit(name, stats, combat, movement, cost, Set.of(specialities));
    }

    private static Combat melee(int min, int max) {
        return new Combat(min, max, 0, HAND_TO_HAND);
    }

    private static Combat ranged(int min, int max, int shots) {
        return new Combat(min, max, shots, LONG_RANGE);
    }
}
