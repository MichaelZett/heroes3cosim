package de.zettsystems.h3comsim.battle.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Die Magiergilde einer Fraktion: welche Zauber sie mit welcher Wahrscheinlichkeit lehrt.
 *
 * <p>Je Stufe zieht die Gilde zufällig Zauber dieser Stufe nach den Gewichten der Stadt — fünf
 * auf Stufe 1, vier, drei, zwei, einen auf Stufe 5 (heroes.thelazy.net, <em>Mage Guild</em>).
 * Die Gewichte stammen von den Seiten der einzelnen Zauber; 0 heißt, der Zauber erscheint in
 * dieser Stadt nie, und wo die Gilde nicht so hoch ausgebaut werden kann (Castle bis Stufe 4,
 * Stronghold und Fortress bis Stufe 3), stehen alle Gewichte der Stufe auf 0. Die Library von
 * Tower (ein Zauber mehr je Stufe) ist nicht abgebildet.
 *
 * <p>{@link #firstLevel} ist das feste Startbuch der Magier-Helden im Katalog: die fünf
 * wahrscheinlichsten Kampfzauber der Stufe 1, bei gleichem Gewicht nach Namen. {@link #draw} zieht
 * wie das Spiel, Abenteuerzauber eingeschlossen.
 */
public final class MageGuild {

    /** Zauber je Gildenstufe 1 bis 5. */
    private static final int[] SLOTS = {5, 4, 3, 2, 1};
    private static final Faction[] TOWNS = {Faction.CASTLE, Faction.RAMPART, Faction.TOWER, Faction.INFERNO,
            Faction.NECROPOLIS, Faction.DUNGEON, Faction.STRONGHOLD, Faction.FORTRESS, Faction.CONFLUX};
    private static final Map<Faction, Map<Spell, Integer>> WEIGHTS = new EnumMap<>(Faction.class);
    private static final Map<Faction, Set<Spell>> FIRST_LEVEL = new EnumMap<>(Faction.class);

    static {
        for (Faction town : TOWNS) {
            WEIGHTS.put(town, new EnumMap<>(Spell.class));
        }
        // Castle, Rampart, Tower, Inferno, Necropolis, Dungeon, Stronghold, Fortress, Conflux
        weights(Spell.AIR_SHIELD, 13, 24, 32, 9, 13, 13, 24, 17, 13);
        weights(Spell.ANIMATE_DEAD, 0, 0, 0, 0, 44, 0, 0, 0, 0);
        weights(Spell.ANTI_MAGIC, 35, 24, 32, 17, 25, 24, 35, 40, 35);
        weights(Spell.ARMAGEDDON, 0, 0, 0, 10, 10, 20, 0, 0, 0);
        weights(Spell.BERSERK, 10, 10, 16, 10, 20, 10, 0, 0, 10);
        weights(Spell.BLESS, 54, 32, 39, 0, 0, 19, 20, 31, 31);
        weights(Spell.BLIND, 25, 35, 18, 47, 24, 25, 25, 35, 37);
        weights(Spell.BLOODLUST, 0, 0, 0, 53, 32, 30, 53, 31, 31);
        weights(Spell.CHAIN_LIGHTNING, 20, 20, 29, 20, 20, 20, 0, 0, 20);
        weights(Spell.CLONE, 10, 10, 29, 10, 10, 10, 0, 0, 20);
        weights(Spell.COUNTERSTRIKE, 10, 20, 16, 10, 10, 10, 0, 0, 10);
        weights(Spell.CURE, 54, 54, 39, 31, 0, 30, 31, 53, 31);
        weights(Spell.CURSE, 0, 0, 25, 31, 54, 30, 0, 0, 0);
        weights(Spell.DEATH_RIPPLE, 0, 0, 0, 0, 95, 0, 0, 0, 0);
        weights(Spell.DESTROY_UNDEAD, 35, 24, 32, 9, 0, 13, 13, 9, 13);
        weights(Spell.DIMENSION_DOOR, 0, 20, 28, 16, 16, 16, 0, 0, 13);
        weights(Spell.DISGUISE, 13, 13, 18, 18, 13, 13, 13, 0, 11);
        weights(Spell.DISPEL, 54, 54, 63, 53, 54, 52, 53, 53, 53);
        weights(Spell.DISRUPTING_RAY, 35, 25, 46, 47, 24, 35, 35, 35, 37);
        weights(Spell.EARTHQUAKE, 13, 24, 17, 25, 13, 13, 13, 90, 13);
        weights(Spell.FIRE_SHIELD, 10, 10, 16, 10, 10, 10, 0, 0, 10);
        weights(Spell.FIRE_WALL, 25, 25, 33, 47, 24, 25, 25, 25, 37);
        weights(Spell.FIREBALL, 24, 24, 32, 90, 35, 44, 35, 9, 35);
        weights(Spell.FLY, 0, 16, 32, 16, 16, 12, 0, 0, 13);
        weights(Spell.FORCE_FIELD, 13, 13, 32, 9, 25, 24, 13, 17, 24);
        weights(Spell.FORGETFULNESS, 24, 24, 17, 17, 13, 13, 13, 17, 24);
        weights(Spell.FORTUNE, 25, 35, 33, 18, 13, 13, 25, 25, 11);
        weights(Spell.FRENZY, 10, 10, 16, 10, 10, 10, 0, 0, 10);
        weights(Spell.FROST_RING, 44, 43, 55, 0, 35, 35, 35, 17, 35);
        weights(Spell.HASTE, 32, 54, 39, 31, 32, 52, 31, 53, 53);
        weights(Spell.HYPNOTIZE, 13, 13, 32, 25, 0, 24, 13, 17, 35);
        weights(Spell.ICE_BOLT, 95, 95, 46, 0, 35, 35, 25, 96, 77);
        weights(Spell.IMPLOSION, 0, 16, 28, 16, 20, 12, 0, 0, 13);
        weights(Spell.INFERNO, 10, 10, 16, 20, 10, 10, 0, 0, 20);
        weights(Spell.LAND_MINE, 24, 24, 32, 25, 25, 35, 24, 9, 13);
        weights(Spell.LIGHTNING_BOLT, 25, 25, 98, 65, 35, 95, 95, 25, 77);
        weights(Spell.MAGIC_ARROW, 89, 89, 94, 89, 89, 88, 89, 89, 89);
        weights(Spell.MAGIC_MIRROR, 0, 16, 28, 0, 0, 12, 0, 0, 13);
        weights(Spell.METEOR_SHOWER, 20, 20, 29, 20, 20, 20, 0, 0, 20);
        weights(Spell.MIRTH, 13, 13, 17, 9, 13, 13, 35, 9, 13);
        weights(Spell.MISFORTUNE, 13, 13, 17, 25, 25, 13, 13, 9, 13);
        weights(Spell.PRAYER, 20, 10, 16, 0, 0, 0, 0, 0, 10);
        weights(Spell.PRECISION, 35, 35, 33, 34, 13, 35, 25, 25, 11);
        weights(Spell.PROTECTION_FROM_AIR, 13, 13, 18, 18, 13, 13, 13, 13, 11);
        weights(Spell.PROTECTION_FROM_EARTH, 13, 13, 17, 9, 13, 13, 13, 0, 13);
        weights(Spell.PROTECTION_FROM_FIRE, 20, 20, 25, 20, 20, 19, 20, 20, 20);
        weights(Spell.PROTECTION_FROM_WATER, 20, 20, 25, 20, 20, 19, 20, 20, 20);
        weights(Spell.QUICKSAND, 25, 25, 46, 18, 24, 25, 35, 35, 20);
        weights(Spell.REMOVE_OBSTACLE, 25, 25, 46, 18, 24, 25, 25, 25, 20);
        weights(Spell.RESURRECTION, 20, 20, 29, 20, 20, 20, 0, 0, 10);
        weights(Spell.SACRIFICE, 0, 0, 0, 20, 16, 12, 0, 0, 0);
        weights(Spell.SCUTTLE_BOAT, 35, 25, 33, 18, 24, 25, 25, 25, 20);
        weights(Spell.SHIELD, 54, 20, 39, 20, 54, 30, 31, 31, 31);
        weights(Spell.SLAYER, 10, 10, 16, 10, 10, 10, 0, 0, 10);
        weights(Spell.SLOW, 32, 54, 63, 53, 32, 30, 31, 31, 53);
        weights(Spell.SORROW, 10, 10, 16, 10, 10, 10, 0, 0, 10);
        weights(Spell.STONE_SKIN, 32, 32, 63, 31, 54, 30, 53, 31, 31);
        weights(Spell.SUMMON_AIR_ELEMENTAL, 0, 0, 28, 0, 16, 0, 0, 0, 12);
        weights(Spell.SUMMON_BOAT, 20, 32, 39, 31, 20, 30, 31, 20, 20);
        weights(Spell.SUMMON_EARTH_ELEMENTAL, 0, 16, 28, 16, 16, 12, 0, 0, 12);
        weights(Spell.SUMMON_FIRE_ELEMENTAL, 0, 0, 0, 16, 0, 12, 0, 0, 12);
        weights(Spell.SUMMON_WATER_ELEMENTAL, 0, 16, 28, 0, 0, 12, 0, 0, 12);
        weights(Spell.TELEPORT, 24, 24, 32, 32, 25, 24, 24, 40, 24);
        weights(Spell.TOWN_PORTAL, 20, 20, 29, 20, 20, 20, 0, 0, 20);
        weights(Spell.VIEW_AIR, 20, 20, 25, 20, 20, 19, 20, 20, 20);
        weights(Spell.VIEW_EARTH, 20, 20, 25, 20, 20, 19, 20, 20, 20);
        weights(Spell.VISIONS, 13, 13, 18, 18, 13, 13, 13, 13, 11);
        weights(Spell.WATER_WALK, 20, 20, 29, 20, 20, 20, 0, 0, 20);
        weights(Spell.WEAKNESS, 13, 13, 18, 34, 24, 25, 25, 25, 20);
        for (Faction town : TOWNS) {
            FIRST_LEVEL.put(town, Set.copyOf(spellsOfLevel(town, 1).stream()
                    .filter(spell -> spell.kind() != Spell.Kind.OTHER)
                    .sorted(Comparator.comparingInt((Spell spell) -> -weight(town, spell))
                            .thenComparing(Spell::name))
                    .limit(SLOTS[0])
                    .toList()));
        }
    }

    private MageGuild() {
    }

    private static void weights(Spell spell, int... perTown) {
        for (int i = 0; i < TOWNS.length; i++) {
            WEIGHTS.computeIfAbsent(TOWNS[i], town -> new EnumMap<>(Spell.class)).put(spell, perTown[i]);
        }
    }

    /** Gewicht des Zaubers in der Gilde dieser Fraktion; 0, wenn er dort nie erscheint. */
    public static int weight(Faction faction, Spell spell) {
        Map<Spell, Integer> town = WEIGHTS.get(faction);
        return town == null ? 0 : town.getOrDefault(spell, 0);
    }

    /** Die fünf Stufe-1-Kampfzauber des festen Startbuchs; leer für neutrale. */
    public static Set<Spell> firstLevel(Faction faction) {
        return FIRST_LEVEL.getOrDefault(faction, Set.of());
    }

    /** Höchste Gildenstufe der Stadt: Castle 4, Stronghold und Fortress 3, sonst 5. */
    static int maxLevel(Faction faction) {
        return switch (faction) {
            case CASTLE -> 4;
            case STRONGHOLD, FORTRESS -> 3;
            case NEUTRAL -> 0;
            default -> 5;
        };
    }

    /**
     * Die Zauber einer Gildenstufe, zufällig nach den Gewichten der Stadt gezogen, ohne
     * Zurücklegen; leer, wenn die Stadt die Stufe nicht bauen kann.
     */
    static Set<Spell> draw(Faction faction, int level, RandomGenerator rng) {
        Set<Spell> drawn = EnumSet.noneOf(Spell.class);
        if (level > maxLevel(faction)) {
            return drawn;
        }
        List<Spell> pool = new ArrayList<>(spellsOfLevel(faction, level));
        while (drawn.size() < SLOTS[level - 1] && !pool.isEmpty()) {
            int total = pool.stream().mapToInt(spell -> weight(faction, spell)).sum();
            int roll = rng.nextInt(total);
            for (Spell spell : pool) {
                roll -= weight(faction, spell);
                if (roll < 0) {
                    drawn.add(spell);
                    pool.remove(spell);
                    break;
                }
            }
        }
        return drawn;
    }

    private static List<Spell> spellsOfLevel(Faction faction, int level) {
        return WEIGHTS.getOrDefault(faction, Map.of()).keySet().stream()
                .filter(spell -> spell.level() == level && weight(faction, spell) > 0)
                .toList();
    }
}
