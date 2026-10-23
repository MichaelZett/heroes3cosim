package de.zettsystems.h3comsim.battle.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.zettsystems.h3comsim.battle.domain.HeroClass.ALCHEMIST;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.BARBARIAN;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.BATTLE_MAGE;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.BEASTMASTER;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.CLERIC;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.DEATH_KNIGHT;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.DEMONIAC;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.DRUID;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.ELEMENTALIST;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.HERETIC;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.KNIGHT;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.NECROMANCER;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.OVERLORD;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.PLANESWALKER;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.RANGER;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.WARLOCK;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.WITCH;
import static de.zettsystems.h3comsim.battle.domain.HeroClass.WIZARD;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.AIR_MAGIC;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.ARCHERY;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.ARMORER;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.ARTILLERY;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.BALLISTICS;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.DIPLOMACY;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.EAGLE_EYE;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.EARTH_MAGIC;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.ESTATES;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.FIRE_MAGIC;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.FIRST_AID;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.INTELLIGENCE;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.LEADERSHIP;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.LEARNING;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.LOGISTICS;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.LUCK;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.MYSTICISM;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.NAVIGATION;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.NECROMANCY;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.OFFENSE;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.PATHFINDING;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.RESISTANCE;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.SCHOLAR;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.SCOUTING;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.SORCERY;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.TACTICS;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.WATER_MAGIC;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.WISDOM;
import static de.zettsystems.h3comsim.battle.domain.SkillLevel.ADVANCED;
import static de.zettsystems.h3comsim.battle.domain.SkillLevel.BASIC;

/**
 * Alle Helden aus Restoration of Erathia (Manual, Abschnitt <em>Individual Heroes</em>) und die
 * Conflux-Helden aus Armageddon's Blade — 144, je Fraktion zwei Klassen zu acht Helden.
 * Primärwerte hängen an der Klasse ({@link HeroClass}), je Held stehen hier nur Name und
 * Startfertigkeiten. Spezialfähigkeiten (z. B. Fionas Bonus für Hell Hounds) sind nicht
 * abgebildet.
 *
 * <p>Zwei Abweichungen vom Manualtext: Fionas „Advance Scouting" ist als Advanced Scouting und
 * Thorgrims „Magic Resistance" als Resistance übernommen; bei Brissa vertauscht das AB-Manual die
 * Zeilen Attack und Defense, ihre Werte sind die aller Elementalisten.
 *
 * <p>Je Fraktion gibt es einen <strong>Standardhelden</strong> ({@link #defaultFor}), der eine
 * Armee führt, wenn niemand ausgewählt ist ({@link #defaultForArmy}). Gewählt ist jeweils ein Held der martialischen
 * Klasse — Power und Knowledge wirken ohne Zaubersystem nicht, ein Magier wäre ein Held fast
 * ohne Wirkung. Innerhalb der Klasse entschied die Startfertigkeit: bevorzugt eine
 * kampfrelevante auf {@code ADVANCED}. Bei Tower und Necropolis bringt kein Held der
 * martialischen Klasse ausschließlich kampfrelevante Fertigkeiten mit, gewählt ist der mit der
 * besten davon.
 */
public final class HeroCatalog {

    // Standardhelden, Begründung je Fraktion in DEFAULTS.
    public static final Hero SORSHA = hero("Sorsha", KNIGHT, Map.of(LEADERSHIP, BASIC, OFFENSE, BASIC));
    public static final Hero JENOVA = hero("Jenova", RANGER, Map.of(ARCHERY, ADVANCED));
    public static final Hero NEELA = hero("Neela", ALCHEMIST, Map.of(SCHOLAR, BASIC, ARMORER, BASIC));
    public static final Hero NYMUS = hero("Nymus", DEMONIAC, Map.of(OFFENSE, ADVANCED));
    public static final Hero TAMIKA = hero("Tamika", DEATH_KNIGHT, Map.of(NECROMANCY, BASIC, OFFENSE, BASIC));
    public static final Hero DAMACON = hero("Damacon", OVERLORD, Map.of(OFFENSE, ADVANCED));
    public static final Hero CRAG_HACK = hero("Crag Hack", BARBARIAN, Map.of(OFFENSE, ADVANCED));
    public static final Hero TAZAR = hero("Tazar", BEASTMASTER, Map.of(ARMORER, ADVANCED));
    public static final Hero FIUR = hero("Fiur", PLANESWALKER, Map.of(OFFENSE, ADVANCED));

    private static final List<Hero> ALL = List.of(
            // Castle — Knight
            hero("Christian", KNIGHT, Map.of(LEADERSHIP, BASIC, ARTILLERY, BASIC)),
            hero("Edric", KNIGHT, Map.of(LEADERSHIP, BASIC, ARMORER, BASIC)),
            hero("Lord Haart", KNIGHT, Map.of(LEADERSHIP, BASIC, ESTATES, BASIC)),
            hero("Orrin", KNIGHT, Map.of(LEADERSHIP, BASIC, ARCHERY, BASIC)),
            SORSHA,
            hero("Sylvia", KNIGHT, Map.of(LEADERSHIP, BASIC, NAVIGATION, BASIC)),
            hero("Tyris", KNIGHT, Map.of(LEADERSHIP, BASIC, TACTICS, BASIC)),
            hero("Valeska", KNIGHT, Map.of(LEADERSHIP, BASIC, ARCHERY, BASIC)),
            // Castle — Cleric
            hero("Adela", CLERIC, Map.of(WISDOM, BASIC, DIPLOMACY, BASIC)),
            hero("Adelaide", CLERIC, Map.of(WISDOM, ADVANCED)),
            hero("Caitlin", CLERIC, Map.of(WISDOM, BASIC, INTELLIGENCE, BASIC)),
            hero("Cuthbert", CLERIC, Map.of(WISDOM, BASIC, ESTATES, BASIC)),
            hero("Ingham", CLERIC, Map.of(WISDOM, BASIC, MYSTICISM, BASIC)),
            hero("Loynis", CLERIC, Map.of(WISDOM, BASIC, LEARNING, BASIC)),
            hero("Rion", CLERIC, Map.of(WISDOM, BASIC, FIRST_AID, BASIC)),
            hero("Sanya", CLERIC, Map.of(WISDOM, BASIC, EAGLE_EYE, BASIC)),
            // Rampart — Ranger
            hero("Clancy", RANGER, Map.of(RESISTANCE, BASIC, PATHFINDING, BASIC)),
            hero("Ivor", RANGER, Map.of(ARCHERY, BASIC, OFFENSE, BASIC)),
            JENOVA,
            hero("Kyrre", RANGER, Map.of(ARCHERY, BASIC, LOGISTICS, BASIC)),
            hero("Mephala", RANGER, Map.of(LEADERSHIP, BASIC, ARMORER, BASIC)),
            hero("Ryland", RANGER, Map.of(LEADERSHIP, BASIC, DIPLOMACY, BASIC)),
            hero("Thorgrim", RANGER, Map.of(RESISTANCE, ADVANCED)),
            hero("Ufretin", RANGER, Map.of(RESISTANCE, BASIC, LUCK, BASIC)),
            // Rampart — Druid
            hero("Aeris", DRUID, Map.of(WISDOM, BASIC, SCOUTING, BASIC)),
            hero("Alagar", DRUID, Map.of(WISDOM, BASIC, SORCERY, BASIC)),
            hero("Coronius", DRUID, Map.of(WISDOM, BASIC, SCHOLAR, BASIC)),
            hero("Elleshar", DRUID, Map.of(WISDOM, BASIC, INTELLIGENCE, BASIC)),
            hero("Gem", DRUID, Map.of(WISDOM, BASIC, FIRST_AID, BASIC)),
            hero("Malcom", DRUID, Map.of(WISDOM, BASIC, EAGLE_EYE, BASIC)),
            hero("Melodia", DRUID, Map.of(WISDOM, BASIC, LUCK, BASIC)),
            hero("Uland", DRUID, Map.of(WISDOM, ADVANCED, BALLISTICS, BASIC)),
            // Tower — Alchemist
            hero("Fafner", ALCHEMIST, Map.of(SCHOLAR, BASIC, RESISTANCE, BASIC)),
            hero("Iona", ALCHEMIST, Map.of(SCHOLAR, BASIC, INTELLIGENCE, BASIC)),
            hero("Josephine", ALCHEMIST, Map.of(MYSTICISM, BASIC, SORCERY, BASIC)),
            NEELA,
            hero("Piquedram", ALCHEMIST, Map.of(MYSTICISM, BASIC, SCOUTING, BASIC)),
            hero("Rissa", ALCHEMIST, Map.of(MYSTICISM, BASIC, OFFENSE, BASIC)),
            hero("Thane", ALCHEMIST, Map.of(SCHOLAR, ADVANCED)),
            hero("Torosar", ALCHEMIST, Map.of(MYSTICISM, BASIC, TACTICS, BASIC)),
            // Tower — Wizard
            hero("Aine", WIZARD, Map.of(WISDOM, BASIC, SCHOLAR, BASIC)),
            hero("Astral", WIZARD, Map.of(WISDOM, ADVANCED)),
            hero("Cyra", WIZARD, Map.of(WISDOM, BASIC, DIPLOMACY, BASIC)),
            hero("Daremyth", WIZARD, Map.of(WISDOM, BASIC, INTELLIGENCE, BASIC)),
            hero("Halon", WIZARD, Map.of(WISDOM, BASIC, MYSTICISM, BASIC)),
            hero("Serena", WIZARD, Map.of(WISDOM, BASIC, EAGLE_EYE, BASIC)),
            hero("Solmyr", WIZARD, Map.of(WISDOM, BASIC, SORCERY, BASIC)),
            hero("Theodorus", WIZARD, Map.of(WISDOM, BASIC, BALLISTICS, BASIC)),
            // Inferno — Demoniac
            hero("Calh", DEMONIAC, Map.of(ARCHERY, BASIC, SCOUTING, BASIC)),
            hero("Fiona", DEMONIAC, Map.of(SCOUTING, ADVANCED)),
            hero("Ignatius", DEMONIAC, Map.of(TACTICS, BASIC, RESISTANCE, BASIC)),
            hero("Marius", DEMONIAC, Map.of(ARMORER, ADVANCED)),
            NYMUS,
            hero("Octavia", DEMONIAC, Map.of(SCHOLAR, BASIC, OFFENSE, BASIC)),
            hero("Pyre", DEMONIAC, Map.of(LOGISTICS, BASIC, ARTILLERY, BASIC)),
            hero("Rashka", DEMONIAC, Map.of(SCHOLAR, BASIC, WISDOM, BASIC)),
            // Inferno — Heretic
            hero("Ash", HERETIC, Map.of(WISDOM, BASIC, EAGLE_EYE, BASIC)),
            hero("Axsis", HERETIC, Map.of(WISDOM, BASIC, MYSTICISM, BASIC)),
            hero("Ayden", HERETIC, Map.of(WISDOM, BASIC, INTELLIGENCE, BASIC)),
            hero("Calid", HERETIC, Map.of(WISDOM, BASIC, LEARNING, BASIC)),
            hero("Olema", HERETIC, Map.of(WISDOM, BASIC, BALLISTICS, BASIC)),
            hero("Xarfax", HERETIC, Map.of(WISDOM, BASIC, LEADERSHIP, BASIC)),
            hero("Xyron", HERETIC, Map.of(WISDOM, BASIC, SCHOLAR, BASIC)),
            hero("Zydar", HERETIC, Map.of(WISDOM, BASIC, SORCERY, BASIC)),
            // Necropolis — Death Knight
            hero("Charna", DEATH_KNIGHT, Map.of(NECROMANCY, BASIC, TACTICS, BASIC)),
            hero("Clavius", DEATH_KNIGHT, Map.of(NECROMANCY, BASIC, OFFENSE, BASIC)),
            hero("Galthran", DEATH_KNIGHT, Map.of(NECROMANCY, BASIC, ARMORER, BASIC)),
            hero("Isra", DEATH_KNIGHT, Map.of(NECROMANCY, ADVANCED)),
            hero("Moandor", DEATH_KNIGHT, Map.of(NECROMANCY, BASIC, LEARNING, BASIC)),
            hero("Straker", DEATH_KNIGHT, Map.of(NECROMANCY, BASIC, RESISTANCE, BASIC)),
            TAMIKA,
            hero("Vokial", DEATH_KNIGHT, Map.of(NECROMANCY, BASIC, ARTILLERY, BASIC)),
            // Necropolis — Necromancer
            hero("Aislinn", NECROMANCER, Map.of(NECROMANCY, BASIC, WISDOM, BASIC)),
            hero("Nagash", NECROMANCER, Map.of(NECROMANCY, BASIC, INTELLIGENCE, BASIC)),
            hero("Nimbus", NECROMANCER, Map.of(NECROMANCY, BASIC, EAGLE_EYE, BASIC)),
            hero("Sandro", NECROMANCER, Map.of(NECROMANCY, BASIC, SORCERY, BASIC)),
            hero("Septienna", NECROMANCER, Map.of(NECROMANCY, BASIC, SCHOLAR, BASIC)),
            hero("Thant", NECROMANCER, Map.of(NECROMANCY, BASIC, MYSTICISM, BASIC)),
            hero("Vidomina", NECROMANCER, Map.of(NECROMANCY, ADVANCED)),
            hero("Xsi", NECROMANCER, Map.of(NECROMANCY, BASIC, LEARNING, BASIC)),
            // Dungeon — Overlord
            hero("Ajit", OVERLORD, Map.of(LEADERSHIP, BASIC, RESISTANCE, BASIC)),
            hero("Arlach", OVERLORD, Map.of(OFFENSE, BASIC, ARTILLERY, BASIC)),
            hero("Dace", OVERLORD, Map.of(TACTICS, BASIC, OFFENSE, BASIC)),
            DAMACON,
            hero("Gunnar", OVERLORD, Map.of(TACTICS, BASIC, LOGISTICS, BASIC)),
            hero("Lorelei", OVERLORD, Map.of(LEADERSHIP, BASIC, SCOUTING, BASIC)),
            hero("Shakti", OVERLORD, Map.of(OFFENSE, BASIC, TACTICS, BASIC)),
            hero("Synca", OVERLORD, Map.of(LEADERSHIP, BASIC, SCHOLAR, BASIC)),
            // Dungeon — Warlock
            hero("Alamar", WARLOCK, Map.of(WISDOM, BASIC, SCHOLAR, BASIC)),
            hero("Darkstorn", WARLOCK, Map.of(WISDOM, BASIC, LEARNING, BASIC)),
            hero("Deemer", WARLOCK, Map.of(WISDOM, BASIC, SCOUTING, ADVANCED)),
            hero("Geon", WARLOCK, Map.of(WISDOM, BASIC, EAGLE_EYE, BASIC)),
            hero("Jaegar", WARLOCK, Map.of(WISDOM, BASIC, MYSTICISM, BASIC)),
            hero("Jeddite", WARLOCK, Map.of(WISDOM, ADVANCED)),
            hero("Malekith", WARLOCK, Map.of(WISDOM, BASIC, SORCERY, BASIC)),
            hero("Sephinroth", WARLOCK, Map.of(WISDOM, BASIC, INTELLIGENCE, BASIC)),
            // Stronghold — Barbarian
            CRAG_HACK,
            hero("Gretchin", BARBARIAN, Map.of(OFFENSE, BASIC, PATHFINDING, BASIC)),
            hero("Gurnisson", BARBARIAN, Map.of(OFFENSE, BASIC, ARTILLERY, BASIC)),
            hero("Jabarkas", BARBARIAN, Map.of(OFFENSE, BASIC, ARCHERY, BASIC)),
            hero("Krellion", BARBARIAN, Map.of(OFFENSE, BASIC, RESISTANCE, BASIC)),
            hero("Shiva", BARBARIAN, Map.of(OFFENSE, BASIC, SCOUTING, BASIC)),
            hero("Tyraxor", BARBARIAN, Map.of(OFFENSE, BASIC, TACTICS, BASIC)),
            hero("Yog", BARBARIAN, Map.of(OFFENSE, BASIC, BALLISTICS, BASIC)),
            // Stronghold — Battle Mage
            hero("Dessa", BATTLE_MAGE, Map.of(WISDOM, BASIC, LOGISTICS, BASIC)),
            hero("Gird", BATTLE_MAGE, Map.of(WISDOM, BASIC, SORCERY, BASIC)),
            hero("Gundula", BATTLE_MAGE, Map.of(WISDOM, BASIC, OFFENSE, BASIC)),
            hero("Oris", BATTLE_MAGE, Map.of(WISDOM, BASIC, EAGLE_EYE, BASIC)),
            hero("Saurug", BATTLE_MAGE, Map.of(WISDOM, BASIC, RESISTANCE, BASIC)),
            hero("Terek", BATTLE_MAGE, Map.of(WISDOM, BASIC, TACTICS, BASIC)),
            hero("Vey", BATTLE_MAGE, Map.of(WISDOM, BASIC, LEADERSHIP, BASIC)),
            hero("Zubin", BATTLE_MAGE, Map.of(WISDOM, BASIC, ARTILLERY, BASIC)),
            // Fortress — Beastmaster
            hero("Alkin", BEASTMASTER, Map.of(ARMORER, BASIC, OFFENSE, BASIC)),
            hero("Broghild", BEASTMASTER, Map.of(ARMORER, BASIC, SCOUTING, BASIC)),
            hero("Bron", BEASTMASTER, Map.of(ARMORER, BASIC, RESISTANCE, BASIC)),
            hero("Drakon", BEASTMASTER, Map.of(ARMORER, BASIC, LEADERSHIP, BASIC)),
            hero("Gerwulf", BEASTMASTER, Map.of(ARMORER, BASIC, ARTILLERY, BASIC)),
            hero("Korbac", BEASTMASTER, Map.of(ARMORER, BASIC, PATHFINDING, BASIC)),
            TAZAR,
            hero("Wystan", BEASTMASTER, Map.of(ARMORER, BASIC, ARCHERY, BASIC)),
            // Fortress — Witch
            hero("Andra", WITCH, Map.of(WISDOM, BASIC, INTELLIGENCE, BASIC)),
            hero("Merist", WITCH, Map.of(WISDOM, BASIC, LEARNING, BASIC)),
            hero("Mirlanda", WITCH, Map.of(WISDOM, ADVANCED)),
            hero("Rosic", WITCH, Map.of(WISDOM, BASIC, MYSTICISM, BASIC)),
            hero("Styg", WITCH, Map.of(WISDOM, BASIC, SORCERY, BASIC)),
            hero("Tiva", WITCH, Map.of(WISDOM, BASIC, EAGLE_EYE, BASIC)),
            hero("Verdish", WITCH, Map.of(WISDOM, BASIC, FIRST_AID, BASIC)),
            hero("Voy", WITCH, Map.of(WISDOM, BASIC, NAVIGATION, BASIC)),
            // Conflux — Planeswalker
            hero("Erdamon", PLANESWALKER, Map.of(TACTICS, BASIC, ESTATES, BASIC)),
            FIUR,
            hero("Ignissa", PLANESWALKER, Map.of(OFFENSE, BASIC, ARTILLERY, BASIC)),
            hero("Kalt", PLANESWALKER, Map.of(TACTICS, BASIC, LEARNING, BASIC)),
            hero("Lacus", PLANESWALKER, Map.of(TACTICS, ADVANCED)),
            hero("Monere", PLANESWALKER, Map.of(OFFENSE, BASIC, LOGISTICS, BASIC)),
            hero("Pasis", PLANESWALKER, Map.of(OFFENSE, BASIC, ARTILLERY, BASIC)),
            hero("Thunar", PLANESWALKER, Map.of(TACTICS, BASIC, ESTATES, BASIC)),
            // Conflux — Elementalist
            hero("Aenain", ELEMENTALIST, Map.of(WISDOM, BASIC, AIR_MAGIC, BASIC)),
            hero("Brissa", ELEMENTALIST, Map.of(WISDOM, BASIC, AIR_MAGIC, BASIC)),
            hero("Ciele", ELEMENTALIST, Map.of(WISDOM, BASIC, WATER_MAGIC, BASIC)),
            hero("Gelare", ELEMENTALIST, Map.of(WISDOM, BASIC, WATER_MAGIC, BASIC)),
            hero("Grindan", ELEMENTALIST, Map.of(WISDOM, BASIC, EARTH_MAGIC, BASIC)),
            hero("Inteus", ELEMENTALIST, Map.of(WISDOM, BASIC, FIRE_MAGIC, BASIC)),
            hero("Labetha", ELEMENTALIST, Map.of(WISDOM, BASIC, EARTH_MAGIC, BASIC)),
            hero("Luna", ELEMENTALIST, Map.of(WISDOM, BASIC, FIRE_MAGIC, BASIC)));

    /**
     * Castle Sorsha (Leadership und Offense), Rampart Jenova (Advanced Archery passt zu den
     * Grand Elves des Presets), Tower Neela (Armorer; kein Alchemist hat rein kampfrelevante
     * Startfertigkeiten), Inferno Nymus, Dungeon Damacon, Stronghold Crag Hack und Conflux Fiur
     * (je Advanced Offense), Necropolis Tamika (Offense; Necromancy wirkt erst nach der
     * Schlacht), Fortress Tazar (Advanced Armorer).
     */
    private static final Map<Faction, Hero> DEFAULTS = defaults();

    private static final Map<String, Hero> BY_NAME = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(Hero::name, h -> h));

    private HeroCatalog() {
    }

    private static Hero hero(String name, HeroClass heroClass, Map<SecondarySkill, SkillLevel> skills) {
        return new Hero(name, heroClass, heroClass.faction(), heroClass.attack(), heroClass.defense(),
                heroClass.power(), heroClass.knowledge(), skills);
    }

    private static Map<Faction, Hero> defaults() {
        Map<Faction, Hero> map = new EnumMap<>(Faction.class);
        map.put(Faction.CASTLE, SORSHA);
        map.put(Faction.RAMPART, JENOVA);
        map.put(Faction.TOWER, NEELA);
        map.put(Faction.INFERNO, NYMUS);
        map.put(Faction.NECROPOLIS, TAMIKA);
        map.put(Faction.DUNGEON, DAMACON);
        map.put(Faction.STRONGHOLD, CRAG_HACK);
        map.put(Faction.FORTRESS, TAZAR);
        map.put(Faction.CONFLUX, FIUR);
        return map;
    }

    /** Alle Helden, nach Fraktion, Klasse (martialisch zuerst) und Name sortiert. */
    public static List<Hero> all() {
        return ALL;
    }

    public static Optional<Hero> byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }

    /** Der Standardheld dieser Fraktion; leer für {@link Faction#NEUTRAL}. */
    public static Optional<Hero> defaultFor(Faction faction) {
        return Optional.ofNullable(DEFAULTS.get(faction));
    }

    /**
     * Der Standardheld für eine Armee ohne ausgewählten Helden: der der Fraktion, die den größten
     * Goldwert (Anzahl × Kosten) stellt. Bei Gleichstand gewinnt die Fraktion, die in
     * {@link Faction} zuerst steht. Neutrale Kreaturen zählen nicht; eine rein neutrale Armee
     * führt {@link #SORSHA} — die Wahl ist willkürlich, aber fest, damit jede Armee einen Helden
     * hat.
     */
    public static Hero defaultForArmy(List<Stack> stacks) {
        Map<Faction, Long> value = new EnumMap<>(Faction.class);
        for (Stack stack : stacks) {
            Faction faction = stack.unit().faction();
            if (faction != Faction.NEUTRAL) {
                value.merge(faction, (long) stack.getCount() * stack.unit().cost(), Long::sum);
            }
        }
        Faction dominant = null;
        long best = -1;
        for (Map.Entry<Faction, Long> entry : value.entrySet()) {
            if (entry.getValue() > best) {
                dominant = entry.getKey();
                best = entry.getValue();
            }
        }
        return dominant == null ? SORSHA : defaultFor(dominant).orElseThrow();
    }
}
