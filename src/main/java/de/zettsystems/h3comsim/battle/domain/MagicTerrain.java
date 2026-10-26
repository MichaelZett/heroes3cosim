package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Magische Gelände über dem Grundgelände (heroes.thelazy.net, <em>Terrain</em>, Abschnitt
 * <em>Magical Terrains</em>; Boni wie VCMI {@code config/battlefields.json}).
 *
 * <ul>
 *   <li>Magic Plains: alle Zauber auf Expert, auch die der Kreaturen.</li>
 *   <li>Rocklands, Fiery Fields, Lucid Pools, Magic Clouds: Zauber der Schule Earth, Fire, Water
 *       bzw. Air auf Expert, nicht die der Kreaturen (SoD).</li>
 *   <li>Cursed Ground: Helden nur Zauber der Stufe 1, Kreaturen gar keine; keine Heimatboni, keine
 *       positive Moral und kein positives Glück.</li>
 *   <li>Holy Ground: gute Kreaturen +1 Moral, böse −1; Evil Fog umgekehrt.</li>
 *   <li>Clover Field: Kreaturen der neutralen Fraktionen (Stronghold, Fortress, Conflux) +2 Glück,
 *       neutrale Kreaturen nicht.</li>
 * </ul>
 */
@Schema(description = "Magisches Gelände des Kampfs, sonst null.", enumAsRef = true)
public enum MagicTerrain {
    MAGIC_PLAINS,
    CURSED_GROUND,
    ROCKLANDS,
    FIERY_FIELDS,
    LUCID_POOLS,
    MAGIC_CLOUDS,
    HOLY_GROUND,
    EVIL_FOG,
    CLOVER_FIELD;

    /** Zaubert ein Held hier einen Zauber dieser Schule auf Expert? */
    boolean expertSchool(MagicSchool school) {
        return switch (this) {
            case MAGIC_PLAINS -> true;
            case ROCKLANDS -> school == MagicSchool.EARTH;
            case FIERY_FIELDS -> school == MagicSchool.FIRE;
            case LUCID_POOLS -> school == MagicSchool.WATER;
            case MAGIC_CLOUDS -> school == MagicSchool.AIR;
            default -> false;
        };
    }

    /** Moral, die das Gelände einer Kreatur dieser Fraktion gibt oder nimmt. */
    int moraleFor(Faction faction) {
        int alignment = alignment(faction);
        return switch (this) {
            case HOLY_GROUND -> alignment;
            case EVIL_FOG -> -alignment;
            default -> 0;
        };
    }

    /** Glück, das das Gelände einer Kreatur dieser Fraktion gibt. */
    int luckFor(Faction faction) {
        return this == CLOVER_FIELD && faction != Faction.NEUTRAL && alignment(faction) == 0 ? 2 : 0;
    }

    /** Gut +1, böse −1, neutral 0 (heroes.thelazy.net, <em>Alignment</em>). */
    private static int alignment(Faction faction) {
        return switch (faction) {
            case CASTLE, RAMPART, TOWER -> 1;
            case INFERNO, NECROPOLIS, DUNGEON -> -1;
            case STRONGHOLD, FORTRESS, CONFLUX, NEUTRAL -> 0;
        };
    }

    /** Stufe, mit der ein Held hier zaubert, ausgehend von seiner eigenen. */
    static SkillLevel heroLevel(@Nullable MagicTerrain terrain, Spell spell, SkillLevel own) {
        if (terrain != null && spell.schools().stream().anyMatch(terrain::expertSchool)) {
            return SkillLevel.EXPERT;
        }
        return own;
    }
}
