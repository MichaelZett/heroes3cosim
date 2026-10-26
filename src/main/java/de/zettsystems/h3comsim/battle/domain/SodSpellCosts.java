package de.zettsystems.h3comsim.battle.domain;

import java.util.Map;

/**
 * Zauberkosten nach Shadow of Death (heroes.thelazy.net, <em>List of spells</em>): Beherrscht der
 * Held die Schule eines Zaubers auf irgendeiner Stufe, kostet er weniger — etwa Haste 6/5,
 * Lightning Bolt 10/8. Die Stufe der Fertigkeit spielt dabei keine Rolle (<em>School of Air
 * Magic</em>: „The cost reduction is the same regardless of whether a hero holds the skill at
 * basic, advanced, or expert level"). Der erste Wert ist der des RoE-Manuals ({@link Spell#cost()}).
 * Teleport kostet in beiden Fassungen 15/12/6/3 ({@link Teleport#cost}).
 */
final class SodSpellCosts {

    private static final Map<Spell, Integer> REDUCED = Map.ofEntries(
            Map.entry(Spell.MAGIC_ARROW, 4),
            Map.entry(Spell.LIGHTNING_BOLT, 8),
            Map.entry(Spell.DESTROY_UNDEAD, 12),
            Map.entry(Spell.CHAIN_LIGHTNING, 20),
            Map.entry(Spell.DEATH_RIPPLE, 8),
            Map.entry(Spell.METEOR_SHOWER, 12),
            Map.entry(Spell.IMPLOSION, 25),
            Map.entry(Spell.FIRE_WALL, 6),
            Map.entry(Spell.FIREBALL, 12),
            Map.entry(Spell.LAND_MINE, 15),
            Map.entry(Spell.ARMAGEDDON, 20),
            Map.entry(Spell.ICE_BOLT, 6),
            Map.entry(Spell.FROST_RING, 9),
            Map.entry(Spell.HASTE, 5),
            Map.entry(Spell.DISRUPTING_RAY, 8),
            Map.entry(Spell.FORTUNE, 5),
            Map.entry(Spell.PRECISION, 6),
            Map.entry(Spell.PROTECTION_FROM_AIR, 5),
            Map.entry(Spell.AIR_SHIELD, 9),
            Map.entry(Spell.HYPNOTIZE, 15),
            Map.entry(Spell.COUNTERSTRIKE, 20),
            Map.entry(Spell.MAGIC_MIRROR, 20),
            Map.entry(Spell.SUMMON_AIR_ELEMENTAL, 20),
            Map.entry(Spell.SHIELD, 4),
            Map.entry(Spell.SLOW, 5),
            Map.entry(Spell.STONE_SKIN, 4),
            Map.entry(Spell.QUICKSAND, 6),
            Map.entry(Spell.ANIMATE_DEAD, 12),
            Map.entry(Spell.ANTI_MAGIC, 12),
            Map.entry(Spell.EARTHQUAKE, 17),
            Map.entry(Spell.FORCE_FIELD, 9),
            Map.entry(Spell.PROTECTION_FROM_EARTH, 9),
            Map.entry(Spell.RESURRECTION, 16),
            Map.entry(Spell.SORROW, 12),
            Map.entry(Spell.SUMMON_EARTH_ELEMENTAL, 20),
            Map.entry(Spell.BLOODLUST, 4),
            Map.entry(Spell.CURSE, 5),
            Map.entry(Spell.PROTECTION_FROM_FIRE, 4),
            Map.entry(Spell.BLIND, 8),
            Map.entry(Spell.MISFORTUNE, 9),
            Map.entry(Spell.BERSERK, 16),
            Map.entry(Spell.FIRE_SHIELD, 12),
            Map.entry(Spell.FRENZY, 12),
            Map.entry(Spell.SLAYER, 12),
            Map.entry(Spell.SACRIFICE, 20),
            Map.entry(Spell.SUMMON_FIRE_ELEMENTAL, 20),
            Map.entry(Spell.BLESS, 4),
            Map.entry(Spell.CURE, 5),
            Map.entry(Spell.DISPEL, 4),
            Map.entry(Spell.PROTECTION_FROM_WATER, 4),
            Map.entry(Spell.REMOVE_OBSTACLE, 5),
            Map.entry(Spell.WEAKNESS, 6),
            Map.entry(Spell.FORGETFULNESS, 9),
            Map.entry(Spell.MIRTH, 9),
            Map.entry(Spell.CLONE, 20),
            Map.entry(Spell.PRAYER, 12),
            Map.entry(Spell.SUMMON_WATER_ELEMENTAL, 20),
            Map.entry(Spell.VISIONS, 2),
            Map.entry(Spell.VIEW_AIR, 1),
            Map.entry(Spell.DISGUISE, 2),
            Map.entry(Spell.DIMENSION_DOOR, 20),
            Map.entry(Spell.FLY, 15),
            Map.entry(Spell.VIEW_EARTH, 1),
            Map.entry(Spell.TOWN_PORTAL, 12),
            Map.entry(Spell.SUMMON_BOAT, 7),
            Map.entry(Spell.SCUTTLE_BOAT, 6),
            Map.entry(Spell.WATER_WALK, 8),
            Map.entry(Spell.INFERNO, 12));

    private SodSpellCosts() {
    }

    /** Die Kosten mit beherrschter Schule; ohne Eintrag die RoE-Kosten. */
    static int reduced(Spell spell) {
        return REDUCED.getOrDefault(spell, spell.cost());
    }
}
