package de.zettsystems.h3comsim.battle.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
 * Primärwerte hängen an der Klasse ({@link HeroClass}), je Held stehen hier Name,
 * Spezialfähigkeit ({@link HeroSpecialty}) und Startfertigkeiten. Zauber-Spezialisten beginnen
 * mit ihrem Zauber; ohne Wirkung bleiben nur Hypnotize und Fire Wall, die der Simulator nicht
 * kennt. Helden der magischen Klassen kennen dazu die Stufe 1 der Magiergilde ihrer Fraktion
 * ({@link MageGuild}). Bei Ajit nennt das Manual
 * „after the 1st level" für Beholder (Stufe 3); maßgeblich ist wie überall die Stufe der
 * Kreatur.
 *
 * <p>Zwei Abweichungen vom Manualtext: Fionas „Advance Scouting" ist als Advanced Scouting und
 * Thorgrims „Magic Resistance" als Resistance übernommen; bei Brissa vertauscht das AB-Manual die
 * Zeilen Attack und Defense, ihre Werte sind die aller Elementalisten.
 *
 * <p>Je Fraktion gibt es einen <strong>Standardhelden</strong> ({@link #defaultFor}), der eine
 * Armee führt, wenn niemand ausgewählt ist ({@link #defaultForArmy}). Gewählt ist jeweils ein Held der martialischen
 * Klasse, ohne Startzauber — so bleiben Messungen ohne Zauber vergleichbar. Innerhalb der Klasse entschied die Startfertigkeit: bevorzugt eine
 * kampfrelevante auf {@code ADVANCED}. Bei Tower und Necropolis bringt kein Held der
 * martialischen Klasse ausschließlich kampfrelevante Fertigkeiten mit, gewählt ist der mit der
 * besten davon.
 */
public final class HeroCatalog {

    // Mehrfach vergebene Spezialitäten. Vor den Helden, weil statische Felder
    // in Reihenfolge initialisiert werden.
    private static final HeroSpecialty GOLD = other("+350 gold per day");
    private static final HeroSpecialty BALLISTA = new HeroSpecialty.Ballista();
    private static final HeroSpecialty HASTE = unique(Spell.HASTE);
    private static final HeroSpecialty WEAKNESS = unique(Spell.WEAKNESS);
    private static final HeroSpecialty STONE_SKIN = unique(Spell.STONE_SKIN);

    // Standardhelden, Begründung je Fraktion in DEFAULTS.
    public static final Hero SORSHA = hero("Sorsha", KNIGHT, creature(UnitCatalog.SWORDSMAN),
            Map.of(LEADERSHIP, BASIC, OFFENSE, BASIC));
    public static final Hero JENOVA = hero("Jenova", RANGER, GOLD,
            Map.of(ARCHERY, ADVANCED));
    public static final Hero NEELA = hero("Neela", ALCHEMIST, skill(ARMORER),
            Map.of(SCHOLAR, BASIC, ARMORER, BASIC));
    public static final Hero NYMUS = hero("Nymus", DEMONIAC, creature(UnitCatalog.PIT_FIEND),
            Map.of(OFFENSE, ADVANCED));
    public static final Hero TAMIKA = hero("Tamika", DEATH_KNIGHT, creature(UnitCatalog.BLACK_KNIGHT),
            Map.of(NECROMANCY, BASIC, OFFENSE, BASIC));
    public static final Hero DAMACON = hero("Damacon", OVERLORD, GOLD,
            Map.of(OFFENSE, ADVANCED));
    public static final Hero CRAG_HACK = hero("Crag Hack", BARBARIAN, skill(OFFENSE),
            Map.of(OFFENSE, ADVANCED));
    public static final Hero TAZAR = hero("Tazar", BEASTMASTER, skill(ARMORER), Map.of(ARMORER, ADVANCED));
    public static final Hero FIUR = hero("Fiur", PLANESWALKER, fixed(UnitCatalog.FIRE_ELEMENTAL, 1, 2, 2),
            Map.of(OFFENSE, ADVANCED));

    private static final List<Hero> ALL = List.of(
            // Castle — Knight
            hero("Christian", KNIGHT, BALLISTA, Map.of(LEADERSHIP, BASIC, ARTILLERY, BASIC)),
            hero("Edric", KNIGHT, creature(UnitCatalog.GRIFFIN), Map.of(LEADERSHIP, BASIC, ARMORER, BASIC)),
            hero("Lord Haart", KNIGHT, skill(ESTATES), Map.of(LEADERSHIP, BASIC, ESTATES, BASIC)),
            hero("Orrin", KNIGHT, skill(ARCHERY), Map.of(LEADERSHIP, BASIC, ARCHERY, BASIC)),
            SORSHA,
            hero("Sylvia", KNIGHT, skill(NAVIGATION), Map.of(LEADERSHIP, BASIC, NAVIGATION, BASIC)),
            hero("Tyris", KNIGHT, creature(UnitCatalog.CAVALIER), Map.of(LEADERSHIP, BASIC, TACTICS, BASIC)),
            hero("Valeska", KNIGHT, creature(UnitCatalog.ARCHER), Map.of(LEADERSHIP, BASIC, ARCHERY, BASIC)),
            // Castle — Cleric
            hero("Adela", CLERIC, unique(Spell.BLESS), Map.of(WISDOM, BASIC, DIPLOMACY, BASIC)),
            hero("Adelaide", CLERIC, commonSpell(Spell.FROST_RING), Map.of(WISDOM, ADVANCED)),
            hero("Caitlin", CLERIC, GOLD, Map.of(WISDOM, BASIC, INTELLIGENCE, BASIC)),
            hero("Cuthbert", CLERIC, WEAKNESS, Map.of(WISDOM, BASIC, ESTATES, BASIC)),
            hero("Ingham", CLERIC, creature(UnitCatalog.MONK), Map.of(WISDOM, BASIC, MYSTICISM, BASIC)),
            hero("Loynis", CLERIC, unique(Spell.PRAYER), Map.of(WISDOM, BASIC, LEARNING, BASIC)),
            hero("Rion", CLERIC, skill(FIRST_AID), Map.of(WISDOM, BASIC, FIRST_AID, BASIC)),
            hero("Sanya", CLERIC, skill(EAGLE_EYE), Map.of(WISDOM, BASIC, EAGLE_EYE, BASIC)),
            // Rampart — Ranger
            hero("Clancy", RANGER, creature(UnitCatalog.UNICORN),
                    Map.of(RESISTANCE, BASIC, PATHFINDING, BASIC)),
            hero("Ivor", RANGER, creature(UnitCatalog.WOOD_ELF), Map.of(ARCHERY, BASIC, OFFENSE, BASIC)),
            JENOVA,
            hero("Kyrre", RANGER, skill(LOGISTICS), Map.of(ARCHERY, BASIC, LOGISTICS, BASIC)),
            hero("Mephala", RANGER, skill(ARMORER), Map.of(LEADERSHIP, BASIC, ARMORER, BASIC)),
            hero("Ryland", RANGER, creature(UnitCatalog.DENDROID_GUARD),
                    Map.of(LEADERSHIP, BASIC, DIPLOMACY, BASIC)),
            hero("Thorgrim", RANGER, skill(RESISTANCE), Map.of(RESISTANCE, ADVANCED)),
            hero("Ufretin", RANGER, creature(UnitCatalog.DWARF), Map.of(RESISTANCE, BASIC, LUCK, BASIC)),
            // Rampart — Druid
            hero("Aeris", DRUID, creature(UnitCatalog.PEGASUS), Map.of(WISDOM, BASIC, SCOUTING, BASIC)),
            hero("Alagar", DRUID, commonSpell(Spell.ICE_BOLT), Map.of(WISDOM, BASIC, SORCERY, BASIC)),
            hero("Coronius", DRUID, unique(Spell.SLAYER), Map.of(WISDOM, BASIC, SCHOLAR, BASIC)),
            hero("Elleshar", DRUID, skill(INTELLIGENCE), Map.of(WISDOM, BASIC, INTELLIGENCE, BASIC)),
            hero("Gem", DRUID, skill(FIRST_AID), Map.of(WISDOM, BASIC, FIRST_AID, BASIC)),
            hero("Malcom", DRUID, skill(EAGLE_EYE), Map.of(WISDOM, BASIC, EAGLE_EYE, BASIC)),
            hero("Melodia", DRUID, unique(Spell.FORTUNE), Map.of(WISDOM, BASIC, LUCK, BASIC)),
            hero("Uland", DRUID, commonSpell(Spell.CURE), Map.of(WISDOM, ADVANCED, BALLISTICS, BASIC)),
            // Tower — Alchemist
            hero("Fafner", ALCHEMIST, creature(UnitCatalog.NAGA), Map.of(SCHOLAR, BASIC, RESISTANCE, BASIC)),
            hero("Iona", ALCHEMIST, creature(UnitCatalog.GENIE), Map.of(SCHOLAR, BASIC, INTELLIGENCE, BASIC)),
            hero("Josephine", ALCHEMIST, creature(UnitCatalog.STONE_GOLEM),
                    Map.of(MYSTICISM, BASIC, SORCERY, BASIC)),
            NEELA,
            hero("Piquedram", ALCHEMIST, creature(UnitCatalog.STONE_GARGOYLE),
                    Map.of(MYSTICISM, BASIC, SCOUTING, BASIC)),
            hero("Rissa", ALCHEMIST, other("+1 mercury per day"), Map.of(MYSTICISM, BASIC, OFFENSE, BASIC)),
            hero("Thane", ALCHEMIST, creature(UnitCatalog.GENIE), Map.of(SCHOLAR, ADVANCED)),
            hero("Torosar", ALCHEMIST, BALLISTA, Map.of(MYSTICISM, BASIC, TACTICS, BASIC)),
            // Tower — Wizard
            hero("Aine", WIZARD, GOLD, Map.of(WISDOM, BASIC, SCHOLAR, BASIC)),
            hero("Astral", WIZARD, spell("Hypnotize"), Map.of(WISDOM, ADVANCED)),
            hero("Cyra", WIZARD, HASTE, Map.of(WISDOM, BASIC, DIPLOMACY, BASIC)),
            hero("Daremyth", WIZARD, unique(Spell.FORTUNE), Map.of(WISDOM, BASIC, INTELLIGENCE, BASIC)),
            hero("Halon", WIZARD, skill(MYSTICISM), Map.of(WISDOM, BASIC, MYSTICISM, BASIC)),
            hero("Serena", WIZARD, skill(EAGLE_EYE), Map.of(WISDOM, BASIC, EAGLE_EYE, BASIC)),
            hero("Solmyr", WIZARD, commonSpell(Spell.CHAIN_LIGHTNING), Map.of(WISDOM, BASIC, SORCERY, BASIC)),
            hero("Theodorus", WIZARD, creature(UnitCatalog.MAGI), Map.of(WISDOM, BASIC, BALLISTICS, BASIC)),
            // Inferno — Demoniac
            hero("Calh", DEMONIAC, creature(UnitCatalog.GOG), Map.of(ARCHERY, BASIC, SCOUTING, BASIC)),
            hero("Fiona", DEMONIAC, creature(UnitCatalog.HELL_HOUND), Map.of(SCOUTING, ADVANCED)),
            hero("Ignatius", DEMONIAC, creature(UnitCatalog.IMP), Map.of(TACTICS, BASIC, RESISTANCE, BASIC)),
            hero("Marius", DEMONIAC, creature(UnitCatalog.DEMON), Map.of(ARMORER, ADVANCED)),
            NYMUS,
            hero("Octavia", DEMONIAC, GOLD, Map.of(SCHOLAR, BASIC, OFFENSE, BASIC)),
            hero("Pyre", DEMONIAC, BALLISTA, Map.of(LOGISTICS, BASIC, ARTILLERY, BASIC)),
            hero("Rashka", DEMONIAC, creature(UnitCatalog.EFREET), Map.of(SCHOLAR, BASIC, WISDOM, BASIC)),
            // Inferno — Heretic
            hero("Ash", HERETIC, unique(Spell.BLOODLUST), Map.of(WISDOM, BASIC, EAGLE_EYE, BASIC)),
            hero("Axsis", HERETIC, skill(MYSTICISM), Map.of(WISDOM, BASIC, MYSTICISM, BASIC)),
            hero("Ayden", HERETIC, skill(INTELLIGENCE), Map.of(WISDOM, BASIC, INTELLIGENCE, BASIC)),
            hero("Calid", HERETIC, other("+1 sulfur per day"), Map.of(WISDOM, BASIC, LEARNING, BASIC)),
            hero("Olema", HERETIC, WEAKNESS, Map.of(WISDOM, BASIC, BALLISTICS, BASIC)),
            hero("Xarfax", HERETIC, commonSpell(Spell.FIREBALL), Map.of(WISDOM, BASIC, LEADERSHIP, BASIC)),
            hero("Xyron", HERETIC, commonSpell(Spell.INFERNO), Map.of(WISDOM, BASIC, SCHOLAR, BASIC)),
            hero("Zydar", HERETIC, skill(SORCERY), Map.of(WISDOM, BASIC, SORCERY, BASIC)),
            // Necropolis — Death Knight
            hero("Charna", DEATH_KNIGHT, creature(UnitCatalog.WIGHT),
                    Map.of(NECROMANCY, BASIC, TACTICS, BASIC)),
            hero("Clavius", DEATH_KNIGHT, GOLD,
                    Map.of(NECROMANCY, BASIC, OFFENSE, BASIC)),
            hero("Galthran", DEATH_KNIGHT, creature(UnitCatalog.SKELETON),
                    Map.of(NECROMANCY, BASIC, ARMORER, BASIC)),
            hero("Isra", DEATH_KNIGHT, skill(NECROMANCY), Map.of(NECROMANCY, ADVANCED)),
            hero("Moandor", DEATH_KNIGHT, creature(UnitCatalog.LICH),
                    Map.of(NECROMANCY, BASIC, LEARNING, BASIC)),
            hero("Straker", DEATH_KNIGHT, creature(UnitCatalog.WALKING_DEAD),
                    Map.of(NECROMANCY, BASIC, RESISTANCE, BASIC)),
            TAMIKA,
            hero("Vokial", DEATH_KNIGHT, creature(UnitCatalog.VAMPIRE),
                    Map.of(NECROMANCY, BASIC, ARTILLERY, BASIC)),
            // Necropolis — Necromancer
            hero("Aislinn", NECROMANCER, commonSpell(Spell.METEOR_SHOWER),
                    Map.of(NECROMANCY, BASIC, WISDOM, BASIC)),
            hero("Nagash", NECROMANCER, GOLD,
                    Map.of(NECROMANCY, BASIC, INTELLIGENCE, BASIC)),
            hero("Nimbus", NECROMANCER, skill(EAGLE_EYE), Map.of(NECROMANCY, BASIC, EAGLE_EYE, BASIC)),
            hero("Sandro", NECROMANCER, skill(SORCERY), Map.of(NECROMANCY, BASIC, SORCERY, BASIC)),
            hero("Septienna", NECROMANCER, commonSpell(Spell.DEATH_RIPPLE),
                    Map.of(NECROMANCY, BASIC, SCHOLAR, BASIC)),
            hero("Thant", NECROMANCER, commonSpell(Spell.ANIMATE_DEAD),
                    Map.of(NECROMANCY, BASIC, MYSTICISM, BASIC)),
            hero("Vidomina", NECROMANCER, skill(NECROMANCY), Map.of(NECROMANCY, ADVANCED)),
            hero("Xsi", NECROMANCER, STONE_SKIN, Map.of(NECROMANCY, BASIC, LEARNING, BASIC)),
            // Dungeon — Overlord
            hero("Ajit", OVERLORD, creature(UnitCatalog.BEHOLDER),
                    Map.of(LEADERSHIP, BASIC, RESISTANCE, BASIC)),
            hero("Arlach", OVERLORD, BALLISTA, Map.of(OFFENSE, BASIC, ARTILLERY, BASIC)),
            hero("Dace", OVERLORD, creature(UnitCatalog.MINOTAUR), Map.of(TACTICS, BASIC, OFFENSE, BASIC)),
            DAMACON,
            hero("Gunnar", OVERLORD, skill(LOGISTICS), Map.of(TACTICS, BASIC, LOGISTICS, BASIC)),
            hero("Lorelei", OVERLORD, creature(UnitCatalog.HARPY),
                    Map.of(LEADERSHIP, BASIC, SCOUTING, BASIC)),
            hero("Shakti", OVERLORD, creature(UnitCatalog.TROGLODYTE),
                    Map.of(OFFENSE, BASIC, TACTICS, BASIC)),
            hero("Synca", OVERLORD, creature(UnitCatalog.MANTICORE),
                    Map.of(LEADERSHIP, BASIC, SCHOLAR, BASIC)),
            // Dungeon — Warlock
            hero("Alamar", WARLOCK, commonSpell(Spell.RESURRECTION), Map.of(WISDOM, BASIC, SCHOLAR, BASIC)),
            hero("Darkstorn", WARLOCK, STONE_SKIN, Map.of(WISDOM, BASIC, LEARNING, BASIC)),
            hero("Deemer", WARLOCK, commonSpell(Spell.METEOR_SHOWER), Map.of(WISDOM, BASIC, SCOUTING, ADVANCED)),
            hero("Geon", WARLOCK, skill(EAGLE_EYE), Map.of(WISDOM, BASIC, EAGLE_EYE, BASIC)),
            hero("Jaegar", WARLOCK, skill(MYSTICISM), Map.of(WISDOM, BASIC, MYSTICISM, BASIC)),
            hero("Jeddite", WARLOCK, commonSpell(Spell.RESURRECTION), Map.of(WISDOM, ADVANCED)),
            hero("Malekith", WARLOCK, skill(SORCERY), Map.of(WISDOM, BASIC, SORCERY, BASIC)),
            hero("Sephinroth", WARLOCK, other("+1 crystal per day"),
                    Map.of(WISDOM, BASIC, INTELLIGENCE, BASIC)),
            // Stronghold — Barbarian
            CRAG_HACK,
            hero("Gretchin", BARBARIAN, creature(UnitCatalog.GOBLIN),
                    Map.of(OFFENSE, BASIC, PATHFINDING, BASIC)),
            hero("Gurnisson", BARBARIAN, BALLISTA, Map.of(OFFENSE, BASIC, ARTILLERY, BASIC)),
            hero("Jabarkas", BARBARIAN, creature(UnitCatalog.ORC), Map.of(OFFENSE, BASIC, ARCHERY, BASIC)),
            hero("Krellion", BARBARIAN, creature(UnitCatalog.OGRE),
                    Map.of(OFFENSE, BASIC, RESISTANCE, BASIC)),
            hero("Shiva", BARBARIAN, creature(UnitCatalog.ROC), Map.of(OFFENSE, BASIC, SCOUTING, BASIC)),
            hero("Tyraxor", BARBARIAN, creature(UnitCatalog.WOLF_RIDER),
                    Map.of(OFFENSE, BASIC, TACTICS, BASIC)),
            hero("Yog", BARBARIAN, creature(UnitCatalog.CYCLOPS), Map.of(OFFENSE, BASIC, BALLISTICS, BASIC)),
            // Stronghold — Battle Mage
            hero("Dessa", BATTLE_MAGE, skill(LOGISTICS), Map.of(WISDOM, BASIC, LOGISTICS, BASIC)),
            hero("Gird", BATTLE_MAGE, skill(SORCERY), Map.of(WISDOM, BASIC, SORCERY, BASIC)),
            hero("Gundula", BATTLE_MAGE, skill(OFFENSE), Map.of(WISDOM, BASIC, OFFENSE, BASIC)),
            hero("Oris", BATTLE_MAGE, skill(EAGLE_EYE), Map.of(WISDOM, BASIC, EAGLE_EYE, BASIC)),
            hero("Saurug", BATTLE_MAGE, other("+1 gem per day"), Map.of(WISDOM, BASIC, RESISTANCE, BASIC)),
            hero("Terek", BATTLE_MAGE, HASTE, Map.of(WISDOM, BASIC, TACTICS, BASIC)),
            hero("Vey", BATTLE_MAGE, creature(UnitCatalog.OGRE), Map.of(WISDOM, BASIC, LEADERSHIP, BASIC)),
            hero("Zubin", BATTLE_MAGE, unique(Spell.PRECISION), Map.of(WISDOM, BASIC, ARTILLERY, BASIC)),
            // Fortress — Beastmaster
            hero("Alkin", BEASTMASTER, creature(UnitCatalog.GORGON), Map.of(ARMORER, BASIC, OFFENSE, BASIC)),
            hero("Broghild", BEASTMASTER, creature(UnitCatalog.WYVERN),
                    Map.of(ARMORER, BASIC, SCOUTING, BASIC)),
            hero("Bron", BEASTMASTER, creature(UnitCatalog.BASILISK),
                    Map.of(ARMORER, BASIC, RESISTANCE, BASIC)),
            hero("Drakon", BEASTMASTER, creature(UnitCatalog.GNOLL),
                    Map.of(ARMORER, BASIC, LEADERSHIP, BASIC)),
            hero("Gerwulf", BEASTMASTER, BALLISTA, Map.of(ARMORER, BASIC, ARTILLERY, BASIC)),
            hero("Korbac", BEASTMASTER, creature(UnitCatalog.SERPENT_FLY),
                    Map.of(ARMORER, BASIC, PATHFINDING, BASIC)),
            TAZAR,
            hero("Wystan", BEASTMASTER, creature(UnitCatalog.LIZARDMAN),
                    Map.of(ARMORER, BASIC, ARCHERY, BASIC)),
            // Fortress — Witch
            hero("Andra", WITCH, skill(INTELLIGENCE), Map.of(WISDOM, BASIC, INTELLIGENCE, BASIC)),
            hero("Merist", WITCH, STONE_SKIN, Map.of(WISDOM, BASIC, LEARNING, BASIC)),
            hero("Mirlanda", WITCH, WEAKNESS, Map.of(WISDOM, ADVANCED)),
            hero("Rosic", WITCH, skill(MYSTICISM), Map.of(WISDOM, BASIC, MYSTICISM, BASIC)),
            hero("Styg", WITCH, skill(SORCERY), Map.of(WISDOM, BASIC, SORCERY, BASIC)),
            hero("Tiva", WITCH, skill(EAGLE_EYE), Map.of(WISDOM, BASIC, EAGLE_EYE, BASIC)),
            hero("Verdish", WITCH, skill(FIRST_AID), Map.of(WISDOM, BASIC, FIRST_AID, BASIC)),
            hero("Voy", WITCH, skill(NAVIGATION), Map.of(WISDOM, BASIC, NAVIGATION, BASIC)),
            // Conflux — Planeswalker
            hero("Erdamon", PLANESWALKER, fixed(UnitCatalog.EARTH_ELEMENTAL, 2, 1, 5),
                    Map.of(TACTICS, BASIC, ESTATES, BASIC)),
            FIUR,
            hero("Ignissa", PLANESWALKER, fixed(UnitCatalog.FIRE_ELEMENTAL, 1, 2, 2),
                    Map.of(OFFENSE, BASIC, ARTILLERY, BASIC)),
            hero("Kalt", PLANESWALKER, fixed(UnitCatalog.WATER_ELEMENTAL, 2, 0, 0),
                    Map.of(TACTICS, BASIC, LEARNING, BASIC)),
            hero("Lacus", PLANESWALKER, fixed(UnitCatalog.WATER_ELEMENTAL, 2, 0, 0),
                    Map.of(TACTICS, ADVANCED)),
            hero("Monere", PLANESWALKER, fixed(UnitCatalog.PSYCHIC_ELEMENTAL, 3, 3, 0),
                    Map.of(OFFENSE, BASIC, LOGISTICS, BASIC)),
            hero("Pasis", PLANESWALKER, fixed(UnitCatalog.PSYCHIC_ELEMENTAL, 3, 3, 0),
                    Map.of(OFFENSE, BASIC, ARTILLERY, BASIC)),
            hero("Thunar", PLANESWALKER, fixed(UnitCatalog.EARTH_ELEMENTAL, 2, 1, 5),
                    Map.of(TACTICS, BASIC, ESTATES, BASIC)),
            // Conflux — Elementalist
            hero("Aenain", ELEMENTALIST, unique(Spell.DISRUPTING_RAY),
                    Map.of(WISDOM, BASIC, AIR_MAGIC, BASIC)),
            hero("Brissa", ELEMENTALIST, HASTE, Map.of(WISDOM, BASIC, AIR_MAGIC, BASIC)),
            hero("Ciele", ELEMENTALIST, new HeroSpecialty.CommonSpell(Spell.MAGIC_ARROW, 50),
                    Map.of(WISDOM, BASIC, WATER_MAGIC, BASIC)),
            hero("Gelare", ELEMENTALIST, GOLD,
                    Map.of(WISDOM, BASIC, WATER_MAGIC, BASIC)),
            hero("Grindan", ELEMENTALIST, GOLD,
                    Map.of(WISDOM, BASIC, EARTH_MAGIC, BASIC)),
            hero("Inteus", ELEMENTALIST, unique(Spell.BLOODLUST), Map.of(WISDOM, BASIC, FIRE_MAGIC, BASIC)),
            hero("Labetha", ELEMENTALIST, unique(Spell.STONE_SKIN),
                    Map.of(WISDOM, BASIC, EARTH_MAGIC, BASIC)),
            hero("Luna", ELEMENTALIST, spell("Fire Wall x2"),
                    Map.of(WISDOM, BASIC, FIRE_MAGIC, BASIC)));

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

    private static Hero hero(String name, HeroClass heroClass, HeroSpecialty specialty,
                             Map<SecondarySkill, SkillLevel> skills) {
        Set<Spell> spells = EnumSet.noneOf(Spell.class);
        spells.addAll(specialty.startingSpells());
        if (heroClass.magic()) {
            spells.addAll(MageGuild.firstLevel(heroClass.faction()));
        }
        return new Hero(name, heroClass, heroClass.faction(), heroClass.attack(), heroClass.defense(),
                heroClass.power(), heroClass.knowledge(), skills, specialty, 1, spells);
    }

    private static HeroSpecialty creature(Unit unit) {
        return new HeroSpecialty.Creature(unit);
    }

    private static HeroSpecialty skill(SecondarySkill skill) {
        return new HeroSpecialty.Skill(skill);
    }

    private static HeroSpecialty fixed(Unit unit, int attack, int defense, int damage) {
        return new HeroSpecialty.FixedCreature(unit, attack, defense, damage);
    }

    private static HeroSpecialty other(String description) {
        return new HeroSpecialty.Other(description);
    }

    private static HeroSpecialty spell(String spell) {
        return other("Spell: " + spell);
    }

    private static HeroSpecialty commonSpell(Spell spell) {
        return new HeroSpecialty.CommonSpell(spell, 0);
    }

    private static HeroSpecialty unique(Spell spell) {
        return new HeroSpecialty.UniqueSpell(spell);
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
