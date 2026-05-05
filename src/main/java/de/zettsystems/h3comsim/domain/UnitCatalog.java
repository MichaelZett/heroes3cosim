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
import static de.zettsystems.h3comsim.domain.UnitSpeciality.ANGEL_RACE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.COUNERSTRIKE_UNLIMITED;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.COUNTERSTRIKE_TWICE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.DEVIL_HATE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.GOOD_ARMY_MORALE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.GOOD_MORALE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.IMMUNE_TO_BLIND;
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
import static de.zettsystems.h3comsim.domain.UnitSpeciality.TITAN_HATE;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.TWO_BLOWS;
import static de.zettsystems.h3comsim.domain.UnitSpeciality.TWO_SHOTS;

public final class UnitCatalog {

    public static final Unit ANGEL = unit("Angel", 20, 20, 200, 12, 50, 50, FLYING, 0, 3000, HAND_TO_HAND, RESURRECTION, GOOD_ARMY_MORALE, DEVIL_HATE, ANGEL_RACE);
    public static final Unit ARCHER = unit("Archer", 6, 3, 10, 4, 2, 3, GROUND, 12, 100, LONG_RANGE);
    public static final Unit ARCH_ANGEL = unit("Arch Angel", 30, 30, 250, 18, 50, 50, FLYING, 0, 5000, HAND_TO_HAND, RESURRECTION, GOOD_ARMY_MORALE, DEVIL_HATE, ANGEL_RACE);
    public static final Unit ARCH_MAGI = unit("Arch Magi", 12, 9, 30, 7, 7, 9, GROUND, 24, 350, LONG_RANGE, NO_OBSTACLE_PENALTY, SPELL_COST_REDUCTION, NO_HAND_TO_HAND_PENALTY);
    public static final Unit BATTLE_DWARF = unit("Battle Dwarf", 7, 7, 20, 5, 2, 4, GROUND, 0, 150, HAND_TO_HAND);
    public static final Unit BEHOLDER = unit("Beholder", 9, 7, 22, 5, 3, 5, GROUND, 12, 250, LONG_RANGE, NO_HAND_TO_HAND_PENALTY);
    public static final Unit BLACK_DRAGON = unit("Black Dragon", 25, 25, 300, 15, 40, 50, FLYING, 0, 4000, HAND_TO_HAND, IMMUNE_TO_SPELLS_BELOW_4, TITAN_HATE);
    public static final Unit CAVALIER = unit("Cavalier", 15, 15, 100, 7, 15, 25, GROUND, 0, 1000, HAND_TO_HAND, IMPACT_DAMAGE);
    public static final Unit CENTAUR = unit("Centaur", 5, 3, 8, 6, 2, 3, GROUND, 0, 70, HAND_TO_HAND);
    public static final Unit CENTAUR_CAPTAIN = unit("Centaur Captain", 6, 3, 10, 8, 2, 3, GROUND, 0, 90, HAND_TO_HAND);
    public static final Unit CERBERUS = unit("Cerberus", 10, 8, 25, 8, 2, 5, GROUND, 0, 250, HAND_TO_HAND);
    public static final Unit CHAMPION = unit("Champion", 16, 16, 100, 9, 20, 25, GROUND, 0, 1200, HAND_TO_HAND, IMPACT_DAMAGE);
    public static final Unit CRUSADER = unit("Crusader", 12, 12, 35, 6, 7, 10, GROUND, 0, 400, HAND_TO_HAND, TWO_BLOWS);
    public static final Unit DEMON = unit("Demon", 10, 10, 35, 5, 7, 9, GROUND, 0, 250, HAND_TO_HAND);
    public static final Unit DENDROID_GUARD = unit("Dendroid Guard", 9, 12, 55, 3, 10, 14, GROUND, 0, 350, HAND_TO_HAND);
    public static final Unit DENDROID_SOLDIER = unit("Dendroid Soldier", 9, 12, 65, 4, 10, 14, GROUND, 0, 425, HAND_TO_HAND);
    public static final Unit DREAD_KNIGHT = unit("Dread Knight", 18, 18, 120, 9, 15, 30, GROUND, 0, 1500, HAND_TO_HAND);
    public static final Unit DWARF = unit("Dwarf", 6, 7, 20, 3, 2, 4, GROUND, 0, 120, HAND_TO_HAND);
    public static final Unit EVIL_EYE = unit("Evil Eye", 10, 8, 22, 7, 3, 5, GROUND, 24, 280, LONG_RANGE, NO_HAND_TO_HAND_PENALTY);
    public static final Unit FAMILIAR = unit("Familiar", 4, 4, 4, 7, 1, 2, GROUND, 0, 60, HAND_TO_HAND);
    public static final Unit GOG = unit("Gog", 6, 4, 13, 4, 2, 4, GROUND, 12, 125, LONG_RANGE);
    public static final Unit GOLD_DRAGON = unit("Gold Dragon", 27, 27, 250, 16, 40, 50, FLYING, 0, 4000, HAND_TO_HAND);
    public static final Unit GRAND_ELF = unit("Grand Elf", 9, 5, 15, 7, 3, 5, GROUND, 24, 250, LONG_RANGE);
    public static final Unit GREEN_DRAGON = unit("Green Dragon", 18, 18, 180, 10, 40, 50, FLYING, 0, 2400, HAND_TO_HAND);
    public static final Unit GRIFFIN = unit("Griffin", 8, 8, 25, 6, 3, 6, FLYING, 0, 200, HAND_TO_HAND, COUNTERSTRIKE_TWICE);
    public static final Unit HALBERDIER = unit("Halberdier", 6, 5, 10, 5, 2, 3, GROUND, 0, 75, HAND_TO_HAND);
    public static final Unit HARPY = unit("Harpy", 6, 5, 14, 6, 1, 4, FLYING, 0, 130, HAND_TO_HAND, MOVE_BACK, NO_RETALIATION);
    public static final Unit HARPY_HAG = unit("Harpy Hag", 6, 6, 14, 9, 1, 4, FLYING, 0, 170, HAND_TO_HAND, MOVE_BACK, NO_RETALIATION);
    public static final Unit HELL_HOUND = unit("Hell Hound", 10, 6, 25, 7, 2, 7, GROUND, 0, 200, HAND_TO_HAND);
    public static final Unit HORNED_DEMON = unit("Horned Demon", 10, 10, 40, 6, 7, 9, GROUND, 0, 270, HAND_TO_HAND);
    public static final Unit IMP = unit("Imp", 2, 3, 4, 5, 1, 2, GROUND, 0, 50, HAND_TO_HAND);
    public static final Unit INFERNAL_TROGLODYTE = unit("Infernal Troglodyte", 5, 4, 6, 5, 1, 3, GROUND, 0, 65, HAND_TO_HAND, IMMUNE_TO_BLIND);
    public static final Unit MAGOG = unit("Magog", 7, 4, 13, 6, 2, 4, GROUND, 24, 175, LONG_RANGE);
    public static final Unit MANTICORE = unit("Manticore", 15, 13, 80, 7, 14, 20, FLYING, 0, 850, HAND_TO_HAND, PARALYZING_VENOM);
    public static final Unit MARKSMAN = unit("Marksman", 6, 3, 10, 6, 2, 3, GROUND, 24, 150, LONG_RANGE, TWO_SHOTS);
    public static final Unit MEDUSA = unit("Medusa", 9, 9, 25, 5, 6, 8, GROUND, 4, 300, LONG_RANGE, PETRYFYING, NO_HAND_TO_HAND_PENALTY);
    public static final Unit MEDUSA_QUEEN = unit("Medusa Queen", 10, 10, 30, 6, 6, 8, GROUND, 8, 330, LONG_RANGE, PETRYFYING, NO_HAND_TO_HAND_PENALTY);
    public static final Unit MIGHTY_GORGON = unit("Mighty Gorgon", 11, 16, 70, 6, 12, 16, GROUND, 0, 600, HAND_TO_HAND);
    public static final Unit MINOTAUR = unit("Minotaur", 14, 12, 50, 6, 12, 20, GROUND, 0, 500, HAND_TO_HAND, GOOD_MORALE);
    public static final Unit MINOTAUR_KING = unit("Minotaur King", 15, 15, 50, 8, 12, 20, GROUND, 0, 575, HAND_TO_HAND, GOOD_MORALE);
    public static final Unit MONK = unit("Monk", 12, 7, 30, 5, 10, 12, GROUND, 12, 400, LONG_RANGE);
    public static final Unit NAGA_QUEEN = unit("Naga Queen", 16, 13, 110, 7, 30, 30, GROUND, 0, 1600, HAND_TO_HAND, NO_RETALIATION);
    public static final Unit PEASANT = unit("Peasant", 1, 1, 1, 3, 1, 1, GROUND, 0, 10, HAND_TO_HAND);
    public static final Unit PEGASUS = unit("Pegasus", 9, 8, 30, 8, 5, 9, FLYING, 0, 250, HAND_TO_HAND);
    public static final Unit PIKEMAN = unit("Pikeman", 4, 5, 10, 4, 1, 3, GROUND, 0, 60, HAND_TO_HAND);
    public static final Unit PIT_FIEND = unit("Pit Fiend", 13, 13, 45, 6, 13, 17, GROUND, 0, 500, HAND_TO_HAND);
    public static final Unit RED_DRAGON = unit("Red Dragon", 19, 19, 180, 11, 40, 50, FLYING, 0, 2500, HAND_TO_HAND, IMMUNE_TO_SPELLS_BELOW_4);
    public static final Unit ROYAL_GRIFFIN = unit("Royal Griffin", 9, 9, 25, 9, 3, 6, FLYING, 0, 240, HAND_TO_HAND, COUNERSTRIKE_UNLIMITED);
    public static final Unit SCORPICORE = unit("Scorpicore", 16, 14, 80, 11, 14, 20, FLYING, 0, 1050, HAND_TO_HAND, PARALYZING_VENOM);
    public static final Unit SILVER_PEGASUS = unit("Silver Pegasus", 9, 10, 30, 12, 5, 9, FLYING, 0, 275, HAND_TO_HAND);
    public static final Unit SWORDSMAN = unit("Swordsman", 10, 12, 35, 5, 6, 9, GROUND, 0, 300, HAND_TO_HAND);
    public static final Unit THUNDERBIRD = unit("Thunderbird", 13, 11, 60, 11, 11, 15, FLYING, 0, 700, HAND_TO_HAND);
    public static final Unit TITAN = unit("Titan", 24, 24, 300, 11, 40, 60, GROUND, 24, 5000, LONG_RANGE, NO_HAND_TO_HAND_PENALTY);
    public static final Unit TROGLODYTE = unit("Troglodyte", 4, 3, 5, 4, 1, 3, GROUND, 0, 50, HAND_TO_HAND, IMMUNE_TO_BLIND);
    public static final Unit UNICORN = unit("Unicorn", 15, 14, 90, 7, 18, 22, GROUND, 0, 850, HAND_TO_HAND);
    public static final Unit WAR_UNICORN = unit("War Unicorn", 15, 14, 110, 9, 18, 22, GROUND, 0, 950, HAND_TO_HAND);
    public static final Unit WOOD_ELF = unit("Wood Elf", 9, 5, 15, 6, 3, 5, GROUND, 24, 200, LONG_RANGE);
    public static final Unit WYVERN_MONARCH = unit("Wyvern Monarch", 14, 14, 70, 11, 18, 22, GROUND, 0, 600, HAND_TO_HAND);
    public static final Unit ZEALOT = unit("Zealot", 12, 10, 30, 7, 10, 12, GROUND, 24, 450, LONG_RANGE, NO_HAND_TO_HAND_PENALTY);

    private static final List<Unit> ALL = List.of(
            ANGEL, ARCHER, ARCH_ANGEL, ARCH_MAGI, BATTLE_DWARF, BEHOLDER, BLACK_DRAGON,
            CAVALIER, CENTAUR, CENTAUR_CAPTAIN, CERBERUS, CHAMPION, CRUSADER,
            DEMON, DENDROID_GUARD, DENDROID_SOLDIER, DREAD_KNIGHT, DWARF,
            EVIL_EYE, FAMILIAR, GOG, GOLD_DRAGON, GRAND_ELF, GREEN_DRAGON, GRIFFIN,
            HALBERDIER, HARPY, HARPY_HAG, HELL_HOUND, HORNED_DEMON,
            IMP, INFERNAL_TROGLODYTE, MAGOG, MANTICORE, MARKSMAN, MEDUSA, MEDUSA_QUEEN,
            MIGHTY_GORGON, MINOTAUR, MINOTAUR_KING, MONK, NAGA_QUEEN,
            PEASANT, PEGASUS, PIKEMAN, PIT_FIEND, RED_DRAGON, ROYAL_GRIFFIN,
            SCORPICORE, SILVER_PEGASUS, SWORDSMAN, THUNDERBIRD, TITAN, TROGLODYTE,
            UNICORN, WAR_UNICORN, WOOD_ELF, WYVERN_MONARCH, ZEALOT);

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

    private static Unit unit(String name, int attack, int defense, int health, int speed,
                             int minDamage, int maxDamage, Movement movement, int shots,
                             int cost, AttackType attackType, UnitSpeciality... specialities) {
        return new Unit(name, attack, defense, health, speed, minDamage, maxDamage,
                movement, shots, cost, attackType, Set.of(specialities));
    }
}
