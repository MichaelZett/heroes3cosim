package de.zettsystems.h3comsim.battle.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Stufe 1 der Magiergilde je Fraktion: die fünf Kampfzauber der Stufe 1, die ein Magier dort
 * lernt. Im Spiel zieht die Gilde ihre fünf Zauber zufällig nach den Wahrscheinlichkeiten der
 * Stadt (heroes.thelazy.net, <em>Mage Guild</em> und die Seiten der einzelnen Zauber, die auch die
 * Städte nennen, in denen ein Zauber nie erscheint). Hier stehen fest die fünf wahrscheinlichsten
 * Kampfzauber, bei gleicher Wahrscheinlichkeit alphabetisch; Abenteuerzauber (Summon Boat, View
 * Air, View Earth) bleiben außen vor.
 *
 * <p>Gewichte (Castle … Conflux, Tower ohne Library): Magic Arrow 88–94 überall; Dispel 52–63
 * überall; Bless 54 Castle, 0 Inferno und Necropolis; Bloodlust 0 Castle, Rampart, Tower;
 * Curse 54 Necropolis, 0 Castle, Rampart, Fortress, Conflux; Cure 54 Castle und Rampart, 0
 * Necropolis; Slow und Stone Skin 63 Tower.
 */
public final class MageGuild {

    private static final Map<Faction, Set<Spell>> FIRST_LEVEL = new EnumMap<>(Faction.class);

    static {
        put(Faction.CASTLE, Spell.MAGIC_ARROW, Spell.BLESS, Spell.CURE, Spell.DISPEL, Spell.SHIELD);
        put(Faction.RAMPART, Spell.MAGIC_ARROW, Spell.CURE, Spell.DISPEL, Spell.HASTE, Spell.SLOW);
        put(Faction.TOWER, Spell.MAGIC_ARROW, Spell.DISPEL, Spell.SLOW, Spell.STONE_SKIN, Spell.BLESS);
        put(Faction.INFERNO, Spell.MAGIC_ARROW, Spell.BLOODLUST, Spell.DISPEL, Spell.SLOW, Spell.CURE);
        put(Faction.NECROPOLIS, Spell.MAGIC_ARROW, Spell.CURSE, Spell.DISPEL, Spell.SHIELD, Spell.STONE_SKIN);
        put(Faction.DUNGEON, Spell.MAGIC_ARROW, Spell.DISPEL, Spell.HASTE, Spell.BLOODLUST, Spell.CURE);
        put(Faction.STRONGHOLD, Spell.MAGIC_ARROW, Spell.BLOODLUST, Spell.DISPEL, Spell.STONE_SKIN, Spell.CURE);
        put(Faction.FORTRESS, Spell.MAGIC_ARROW, Spell.CURE, Spell.DISPEL, Spell.HASTE, Spell.BLESS);
        put(Faction.CONFLUX, Spell.MAGIC_ARROW, Spell.DISPEL, Spell.HASTE, Spell.SLOW, Spell.BLESS);
    }

    private MageGuild() {
    }

    private static void put(Faction faction, Spell... spells) {
        FIRST_LEVEL.put(faction, Set.of(spells));
    }

    /** Die fünf Stufe-1-Kampfzauber der Gilde dieser Fraktion; leer für neutrale. */
    public static Set<Spell> firstLevel(Faction faction) {
        return FIRST_LEVEL.getOrDefault(faction, Set.of());
    }
}
