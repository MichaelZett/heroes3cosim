package de.zettsystems.h3comsim.battle.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.ARCHERY;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.ARMORER;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.LEADERSHIP;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.NECROMANCY;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.OFFENSE;
import static de.zettsystems.h3comsim.battle.domain.SecondarySkill.SCHOLAR;
import static de.zettsystems.h3comsim.battle.domain.SkillLevel.ADVANCED;
import static de.zettsystems.h3comsim.battle.domain.SkillLevel.BASIC;

/**
 * Ein Held je Fraktion, Werte aus dem RoE-Manual (Abschnitt <em>Individual Heroes</em>);
 * Fiur steht im Armageddon's-Blade-Manual, weil Conflux erst dort dazukam.
 *
 * <p>Ausgewählt ist jeweils die <strong>martialische</strong> Klasse der Fraktion — Power und
 * Knowledge wirken ohne Zaubersystem nicht, ein Magier wäre also ein Held ohne Wirkung.
 * Innerhalb der Klasse entschied die Startfertigkeit: bevorzugt eine kampfrelevante auf
 * {@code ADVANCED}, weil sie mit der nächsten Ausbaustufe unmittelbar greift. Bei Tower und
 * Necropolis geht das nicht auf — dort bringt kein Held der martialischen Klasse ausschließlich
 * kampfrelevante Fertigkeiten mit, gewählt ist der mit der besten davon.
 *
 * <p>Warum die Primärwerte innerhalb einer Klasse gleich aussehen: sie hängen an der Klasse,
 * nicht am Helden (siehe {@link HeroClass}).
 */
public final class HeroCatalog {

    /** Castle — Knight, 2/2/1/1. */
    public static final Hero SORSHA = new Hero("Sorsha", HeroClass.KNIGHT, Faction.CASTLE,
            2, 2, 1, 1, Map.of(LEADERSHIP, BASIC, OFFENSE, BASIC));
    /** Rampart — Ranger, 1/3/1/1. Advanced Archery passt zu den Grand Elves des Presets. */
    public static final Hero JENOVA = new Hero("Jenova", HeroClass.RANGER, Faction.RAMPART,
            1, 3, 1, 1, Map.of(ARCHERY, ADVANCED));
    /** Tower — Alchemist, 1/1/2/2. Kein Alchemist hat rein kampfrelevante Startfertigkeiten. */
    public static final Hero NEELA = new Hero("Neela", HeroClass.ALCHEMIST, Faction.TOWER,
            1, 1, 2, 2, Map.of(SCHOLAR, BASIC, ARMORER, BASIC));
    /** Inferno — Demoniac, 2/2/1/1. */
    public static final Hero NYMUS = new Hero("Nymus", HeroClass.DEMONIAC, Faction.INFERNO,
            2, 2, 1, 1, Map.of(OFFENSE, ADVANCED));
    /** Necropolis — Death Knight, 1/2/2/1. Necromancy wirkt nach der Schlacht, nicht in ihr. */
    public static final Hero TAMIKA = new Hero("Tamika", HeroClass.DEATH_KNIGHT, Faction.NECROPOLIS,
            1, 2, 2, 1, Map.of(NECROMANCY, BASIC, OFFENSE, BASIC));
    /** Dungeon — Overlord, 2/2/1/1. */
    public static final Hero DAMACON = new Hero("Damacon", HeroClass.OVERLORD, Faction.DUNGEON,
            2, 2, 1, 1, Map.of(OFFENSE, ADVANCED));
    /** Stronghold — Barbarian, 4/0/1/1. Höchster Attack-Wert im Katalog. */
    public static final Hero CRAG_HACK = new Hero("Crag Hack", HeroClass.BARBARIAN, Faction.STRONGHOLD,
            4, 0, 1, 1, Map.of(OFFENSE, ADVANCED));
    /** Fortress — Beastmaster, 0/4/1/1. Höchster Defense-Wert im Katalog. */
    public static final Hero TAZAR = new Hero("Tazar", HeroClass.BEASTMASTER, Faction.FORTRESS,
            0, 4, 1, 1, Map.of(ARMORER, ADVANCED));
    /** Conflux — Planeswalker, 3/1/1/1. Werte aus dem AB-Manual. */
    public static final Hero FIUR = new Hero("Fiur", HeroClass.PLANESWALKER, Faction.CONFLUX,
            3, 1, 1, 1, Map.of(OFFENSE, ADVANCED));

    private static final List<Hero> ALL = List.of(
            SORSHA, JENOVA, NEELA, NYMUS, TAMIKA, DAMACON, CRAG_HACK, TAZAR, FIUR);

    private static final Map<String, Hero> BY_NAME = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(Hero::name, h -> h));

    private static final Map<Faction, Hero> BY_FACTION = ALL.stream()
            .collect(Collectors.toMap(Hero::faction, h -> h, (a, b) -> a,
                    () -> new EnumMap<>(Faction.class)));

    private HeroCatalog() {
    }

    public static List<Hero> all() {
        return ALL;
    }

    public static Optional<Hero> byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }

    /** Der Held dieser Fraktion; leer für {@link Faction#NEUTRAL}. */
    public static Optional<Hero> byFaction(Faction faction) {
        return Optional.ofNullable(BY_FACTION.get(faction));
    }
}
