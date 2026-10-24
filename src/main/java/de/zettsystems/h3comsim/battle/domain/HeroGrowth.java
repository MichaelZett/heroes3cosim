package de.zettsystems.h3comsim.battle.domain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Ein Held, der nach den Regeln des Spiels zufällig bis zu einem Level aufsteigt
 * (heroes.thelazy.net, <em>Hero class</em>, <em>Secondary skill</em>, <em>Mage Guild</em>).
 *
 * <ul>
 *   <li><strong>Primärwert</strong>: je Aufstieg +1 auf einen der vier, nach den Chancen der
 *       Klasse für Level 2–9 bzw. ab Level 10.</li>
 *   <li><strong>Sekundärfertigkeit</strong>: zwei Angebote. Hat der Held Fertigkeiten auf Basic
 *       oder Advanced und noch einen freien Platz, ist das linke ein Ausbau, das rechte eine neue;
 *       sonst kommen beide aus der einen nicht leeren Gruppe. Gezogen wird nach den Gewichten der
 *       Klasse (je 112 Lose; eine Fertigkeit mit Gewicht 0, die der Held trotzdem hat, zählt 1).
 *       Wisdom ist Magiern alle 3 Level garantiert, Kämpfern alle 6; eine Magie-Schule Magiern
 *       alle 3, Kämpfern alle 4 — jedes Angebot setzt den Zähler zurück, Wisdom hat Vorrang.
 *       Elementalists steigen in SoD wie Kämpfer auf. Gewählt wird immer das linke Angebot: so
 *       behandelt das Spiel Helden, die über Level 1 beginnen („assumed to have always selected
 *       the left-hand skill option").</li>
 *   <li><strong>Zauber</strong>: nur Helden mit Zauberbuch — die magischen Klassen und wer mit
 *       einem Zauber beginnt. Sie lernen in der Gilde ihrer Fraktion ({@link MageGuild}) Stufe 1,
 *       ab Level 2 auch Stufe 2, darüber jede Stufe, die Wisdom erlaubt und die Stadt bauen kann;
 *       jede Stufe zufällig nach den Gewichten der Stadt, Abenteuerzauber eingeschlossen.</li>
 * </ul>
 */
public final class HeroGrowth {

    private static final HeroClass[] CLASSES = HeroClass.values();
    private static final Map<HeroClass, int[]> PRIMARY = new EnumMap<>(HeroClass.class);
    private static final Map<HeroClass, Map<SecondarySkill, Integer>> SKILL_WEIGHTS = new EnumMap<>(HeroClass.class);
    private static final Set<SecondarySkill> SCHOOLS = EnumSet.of(SecondarySkill.AIR_MAGIC,
            SecondarySkill.EARTH_MAGIC, SecondarySkill.FIRE_MAGIC, SecondarySkill.WATER_MAGIC);
    private static final int LATE_LEVEL = 10;

    static {
        // Attack, Defense, Power, Knowledge für Level 2–9, dann ab Level 10, in Prozent.
        primary(HeroClass.KNIGHT, 35, 45, 10, 10, 30, 30, 20, 20);
        primary(HeroClass.CLERIC, 20, 15, 30, 35, 20, 20, 30, 30);
        primary(HeroClass.RANGER, 35, 45, 10, 10, 30, 30, 20, 20);
        primary(HeroClass.DRUID, 10, 20, 35, 35, 20, 20, 30, 30);
        primary(HeroClass.ALCHEMIST, 30, 30, 20, 20, 30, 30, 20, 20);
        primary(HeroClass.WIZARD, 10, 10, 40, 40, 30, 20, 20, 30);
        primary(HeroClass.DEMONIAC, 35, 35, 15, 15, 30, 30, 20, 20);
        primary(HeroClass.HERETIC, 15, 15, 35, 35, 20, 20, 30, 30);
        primary(HeroClass.DEATH_KNIGHT, 30, 25, 20, 25, 25, 25, 25, 25);
        primary(HeroClass.NECROMANCER, 15, 15, 35, 35, 25, 25, 25, 25);
        primary(HeroClass.OVERLORD, 35, 35, 15, 15, 30, 30, 20, 20);
        primary(HeroClass.WARLOCK, 10, 10, 50, 30, 20, 20, 30, 30);
        primary(HeroClass.BARBARIAN, 55, 35, 5, 5, 30, 30, 20, 20);
        primary(HeroClass.BATTLE_MAGE, 30, 20, 25, 25, 25, 25, 25, 25);
        primary(HeroClass.BEASTMASTER, 30, 50, 10, 10, 30, 30, 20, 20);
        primary(HeroClass.WITCH, 5, 15, 40, 40, 20, 20, 30, 30);
        primary(HeroClass.PLANESWALKER, 45, 25, 15, 15, 30, 30, 20, 20);
        primary(HeroClass.ELEMENTALIST, 15, 15, 35, 35, 25, 25, 25, 25);

        // Gewichte je Klasse in der Reihenfolge von HeroClass (Knight … Elementalist).
        row(SecondarySkill.AIR_MAGIC,  3,  4,  1,  2,  4,  6,  2,  3,  2,  3,  1,  2,  3,  3,  1,  3,  2,  6);
        row(SecondarySkill.ARCHERY,  5,  3,  8,  5,  5,  2,  6,  4,  5,  2,  6,  2,  7,  4,  7,  3,  8,  2);
        row(SecondarySkill.ARMORER,  5,  3,  8,  3,  8,  1,  7,  4,  5,  2,  6,  1,  6,  4, 10,  4,  5,  1);
        row(SecondarySkill.ARTILLERY,  5,  2,  6,  1,  4,  1,  5,  4,  5,  3,  8,  1,  8,  4,  8,  1,  8,  1);
        row(SecondarySkill.BALLISTICS,  8,  4,  4,  4,  6,  4,  7,  6,  7,  5,  7,  6,  8,  6,  7,  8,  8,  4);
        row(SecondarySkill.DIPLOMACY,  4,  7,  4,  4,  3,  4,  4,  3,  2,  4,  3,  4,  1,  3,  1,  2,  2,  4);
        row(SecondarySkill.EAGLE_EYE,  2,  6,  2,  7,  3,  8,  3,  4,  4,  7,  2,  8,  2,  5,  1, 10,  2,  8);
        row(SecondarySkill.EARTH_MAGIC,  2,  3,  3,  4,  3,  3,  3,  4,  4,  8,  3,  5,  3,  3,  3,  3,  3,  6);
        row(SecondarySkill.ESTATES,  6,  3,  2,  3,  4,  5,  3,  2,  0,  3,  4,  5,  2,  1,  1,  1,  3,  3);
        row(SecondarySkill.FIRE_MAGIC,  1,  2,  0,  1,  1,  2,  4,  5,  1,  2,  2,  5,  2,  3,  0,  3,  3,  6);
        row(SecondarySkill.FIRST_AID,  2, 10,  3,  7,  2,  7,  2,  5,  0,  0,  1,  6,  1,  4,  6,  8,  1,  4);
        row(SecondarySkill.INTELLIGENCE,  1,  6,  2,  7,  4, 10,  2,  6,  5,  6,  1,  8,  1,  5,  1,  7,  1,  8);
        row(SecondarySkill.LEADERSHIP, 10,  2,  6,  2,  3,  4,  3,  2,  0,  0,  8,  3,  5,  4,  5,  1,  3,  3);
        row(SecondarySkill.LEARNING,  4,  4,  4,  4, 10,  4,  4,  4,  4,  4,  4,  4,  4,  4,  4,  4,  8,  4);
        row(SecondarySkill.LOGISTICS,  5,  4,  5,  5,  6,  2, 10,  3,  5,  4,  8,  2,  7,  9,  8,  3,  8,  2);
        row(SecondarySkill.LUCK,  3,  5,  6,  9,  2,  4,  2,  2,  1,  1,  1,  2,  3,  2,  2,  4,  2,  2);
        row(SecondarySkill.MYSTICISM,  2,  4,  3,  6,  4,  8,  2, 10,  4,  6,  3,  8,  3,  4,  2,  8,  3,  8);
        row(SecondarySkill.NAVIGATION,  8,  5,  3,  2,  3,  1,  4,  2,  8,  5,  4,  4,  2,  0,  8,  6,  5,  4);
        row(SecondarySkill.NECROMANCY,  0,  0,  0,  0,  0,  0,  0,  0, 10, 10,  0,  0,  0,  0,  0,  0,  0,  0);
        row(SecondarySkill.OFFENSE,  7,  4,  5,  1,  6,  1,  8,  4,  7,  3,  8,  1, 10,  8,  5,  2,  9,  1);
        row(SecondarySkill.PATHFINDING,  4,  2,  7,  5,  4,  2,  4,  4,  4,  6,  5,  2,  8,  4,  8,  2,  6,  2);
        row(SecondarySkill.RESISTANCE,  5,  2,  9,  1,  5,  0,  6,  3,  5,  1,  6,  0,  6,  4,  5,  0,  2,  0);
        row(SecondarySkill.SCHOLAR,  1,  6,  1,  8,  3,  9,  2,  5,  2,  6,  1,  8,  1,  4,  1,  7,  1,  8);
        row(SecondarySkill.SCOUTING,  4,  3,  7,  2,  4,  2,  5,  3,  4,  2,  5,  2,  8,  4,  7,  2,  6,  2);
        row(SecondarySkill.SORCERY,  1,  5,  2,  6,  3,  8,  3,  6,  4,  6,  2, 10,  1,  6,  1,  8,  1,  8);
        row(SecondarySkill.TACTICS,  7,  2,  5,  1,  4,  1,  6,  4,  5,  2, 10,  1,  8,  5,  6,  1,  8,  1);
        row(SecondarySkill.WATER_MAGIC,  4,  4,  3,  4,  2,  3,  1,  2,  3,  3,  0,  2,  0,  3,  2,  3,  2,  6);
        row(SecondarySkill.WISDOM,  3,  7,  3,  8,  6, 10,  4,  8,  6,  8,  3, 10,  2,  6,  2,  8,  2,  8);
    }

    private HeroGrowth() {
    }

    private static void primary(HeroClass heroClass, int... percents) {
        PRIMARY.put(heroClass, percents);
    }

    private static void row(SecondarySkill skill, int... perClass) {
        for (int i = 0; i < CLASSES.length; i++) {
            SKILL_WEIGHTS.computeIfAbsent(CLASSES[i], c -> new EnumMap<>(SecondarySkill.class)).put(skill, perClass[i]);
        }
    }

    /** Gewicht der Fertigkeit für die Klasse (von 112). */
    static int skillWeight(HeroClass heroClass, SecondarySkill skill) {
        return SKILL_WEIGHTS.getOrDefault(heroClass, Map.of()).getOrDefault(skill, 0);
    }

    /**
     * Der Held auf {@code level}, vom Katalog-Stand aus zufällig aufgestiegen. Das feste Startbuch
     * des Katalogs weicht den gezogenen Gildenzaubern; der Startzauber eines Spezialisten bleibt.
     */
    public static Hero grow(Hero base, int level, RandomGenerator rng) {
        State state = new State(base);
        for (int next = base.level() + 1; next <= level; next++) {
            state.levelUp(next, rng);
        }
        Set<Spell> spells = EnumSet.noneOf(Spell.class);
        spells.addAll(base.specialty().startingSpells());
        Hero grown = new Hero(base.name(), base.heroClass(), base.faction(), state.primary[0], state.primary[1],
                state.primary[2], state.primary[3], state.skills, base.specialty(), Math.max(level, base.level()),
                Set.of());
        if (hasSpellbook(base)) {
            for (int guildLevel = 1; guildLevel <= 5; guildLevel++) {
                if (guildLevel <= grown.maxSpellLevel() && (guildLevel == 1 || grown.level() >= 2)) {
                    spells.addAll(MageGuild.draw(base.faction(), guildLevel, rng));
                }
            }
        }
        return new Hero(grown.name(), grown.heroClass(), grown.faction(), grown.attack(), grown.defense(),
                grown.power(), grown.knowledge(), grown.skills(), grown.specialty(), grown.level(), spells);
    }

    /** Ein Zauberbuch haben die magischen Klassen und wer mit einem Zauber beginnt. */
    static boolean hasSpellbook(Hero hero) {
        return hero.heroClass().magic() || !hero.specialty().startingSpells().isEmpty();
    }

    /** Was sich beim Aufsteigen ändert. */
    private static final class State {
        private final HeroClass heroClass;
        private final int[] chances;
        private final int[] primary;
        private final Map<SecondarySkill, SkillLevel> skills = new EnumMap<>(SecondarySkill.class);
        private final int wisdomEvery;
        private final int schoolEvery;
        private int lastWisdomOffer;
        private int lastSchoolOffer;

        State(Hero base) {
            heroClass = base.heroClass();
            chances = Objects.requireNonNull(PRIMARY.get(heroClass), "primary chances");
            primary = new int[] {base.attack(), base.defense(), base.power(), base.knowledge()};
            skills.putAll(base.skills());
            boolean magicLeveling = heroClass.magic() && heroClass != HeroClass.ELEMENTALIST;
            wisdomEvery = magicLeveling ? 3 : 6;
            schoolEvery = magicLeveling ? 3 : 4;
        }

        void levelUp(int level, RandomGenerator rng) {
            int offset = level < LATE_LEVEL ? 0 : 4;
            primary[pick(rng, chances[offset], chances[offset + 1], chances[offset + 2], chances[offset + 3])]++;
            levelUpSkill(level, rng);
        }

        private void levelUpSkill(int level, RandomGenerator rng) {
            List<SecondarySkill> upgradable = new ArrayList<>();
            List<SecondarySkill> learnable = new ArrayList<>();
            sortSkills(upgradable, learnable);
            if (upgradable.isEmpty() && learnable.isEmpty()) {
                return;
            }
            List<SecondarySkill> leftPool = upgradable.isEmpty() ? learnable : upgradable;
            List<SecondarySkill> rightPool = learnable.isEmpty() || upgradable.isEmpty()
                    ? new ArrayList<>(leftPool) : learnable;
            boolean wisdomDue = level - lastWisdomOffer >= wisdomEvery;
            boolean schoolDue = level - lastSchoolOffer >= schoolEvery;
            SecondarySkill left = offer(leftPool, wisdomDue, schoolDue, rng);
            rightPool.remove(left);
            noteOffers(level, rightPool.isEmpty()
                    ? List.of(left) : List.of(left, offer(rightPool, wisdomDue, schoolDue, rng)));
            // Helden über Level 1 nehmen immer das linke Angebot.
            skills.merge(left, SkillLevel.BASIC, (old, basic) -> old == SkillLevel.BASIC
                    ? SkillLevel.ADVANCED : SkillLevel.EXPERT);
        }

        /** Teilt die Fertigkeiten in verbesserbare und neu lernbare. */
        private void sortSkills(List<SecondarySkill> upgradable, List<SecondarySkill> learnable) {
            for (SecondarySkill skill : SecondarySkill.values()) {
                SkillLevel current = skills.get(skill);
                if (current == SkillLevel.BASIC || current == SkillLevel.ADVANCED) {
                    upgradable.add(skill);
                } else if (current == null && skills.size() < Hero.MAX_SKILLS && skillWeight(heroClass, skill) > 0) {
                    learnable.add(skill);
                }
            }
        }

        /** Merkt sich, wann zuletzt Wisdom und eine Magieschule angeboten wurden. */
        private void noteOffers(int level, List<SecondarySkill> offered) {
            if (offered.contains(SecondarySkill.WISDOM)) {
                lastWisdomOffer = level;
            }
            if (offered.stream().anyMatch(SCHOOLS::contains)) {
                lastSchoolOffer = level;
            }
        }

        private SecondarySkill offer(List<SecondarySkill> pool, boolean wisdomDue, boolean schoolDue,
                                     RandomGenerator rng) {
            if (wisdomDue && pool.contains(SecondarySkill.WISDOM)) {
                return SecondarySkill.WISDOM;
            }
            List<SecondarySkill> schools = pool.stream().filter(SCHOOLS::contains).toList();
            return weighted(schoolDue && !schools.isEmpty() ? schools : pool, rng);
        }

        private SecondarySkill weighted(List<SecondarySkill> pool, RandomGenerator rng) {
            int[] weights = pool.stream().mapToInt(this::weight).toArray();
            return pool.get(pick(rng, weights));
        }

        /** Gewicht der Klasse; eine verbotene Fertigkeit, die der Held trotzdem hat, zählt 1. */
        private int weight(SecondarySkill skill) {
            int weight = skillWeight(heroClass, skill);
            return weight == 0 && skills.containsKey(skill) ? 1 : weight;
        }

        /** Index nach Gewichten. */
        private static int pick(RandomGenerator rng, int... weights) {
            int total = 0;
            for (int w : weights) {
                total += w;
            }
            int roll = rng.nextInt(total);
            for (int i = 0; i < weights.length; i++) {
                roll -= weights[i];
                if (roll < 0) {
                    return i;
                }
            }
            throw new IllegalStateException("no weight left");
        }
    }
}
