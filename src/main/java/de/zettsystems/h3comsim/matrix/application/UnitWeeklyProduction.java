package de.zettsystems.h3comsim.matrix.application;

import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.Unit;

import java.util.Map;

/**
 * Pro Faktion und Tier die Grundproduktion pro Woche (Basis-Dwelling, ohne Sekundärbau-Boni wie
 * Griffin Tower, Goblin Barracks usw.). Quelle: {@code files/h3_manual.txt} (RoE-Manual, Section
 * <em>World Reference</em>); Conflux-Werte aus {@code files/ab_manual.txt} (AB-Manual). Skeleton
 * (Necropolis T1) ist im Manual nicht explizit mit „Creatures/Week" gelistet, der kanonische
 * SoD-Wert ist 12 (heroes.thelazy.net, ohne Necromancer-Boni).
 *
 * <p>Upgrade und Basis-Einheit teilen sich pro H3-Regel dasselbe Dwelling und damit dieselbe
 * Produktion — der Lookup ist deshalb nur {@code (Faction, tier)}.
 */
public final class UnitWeeklyProduction {

    private static final Map<Faction, int[]> BY_FACTION = Map.ofEntries(
            Map.entry(Faction.CASTLE, new int[]{14, 9, 7, 4, 3, 2, 1}),
            Map.entry(Faction.RAMPART, new int[]{14, 8, 7, 5, 3, 2, 1}),
            Map.entry(Faction.TOWER, new int[]{16, 9, 6, 4, 3, 2, 1}),
            Map.entry(Faction.INFERNO, new int[]{15, 8, 5, 4, 3, 2, 1}),
            Map.entry(Faction.NECROPOLIS, new int[]{12, 8, 7, 4, 3, 2, 1}),
            Map.entry(Faction.DUNGEON, new int[]{14, 8, 7, 4, 3, 2, 1}),
            Map.entry(Faction.STRONGHOLD, new int[]{15, 9, 7, 4, 3, 2, 1}),
            Map.entry(Faction.FORTRESS, new int[]{12, 8, 8, 4, 3, 2, 1}),
            Map.entry(Faction.CONFLUX, new int[]{20, 6, 6, 5, 4, 2, 2}),
            Map.entry(Faction.NEUTRAL, new int[]{1, 1, 1, 1, 1, 1, 1})
    );

    private UnitWeeklyProduction() {
    }

    public static int forUnit(Unit unit) {
        int[] perTier = BY_FACTION.get(unit.faction());
        if (perTier == null) {
            return 1;
        }
        return perTier[unit.tier() - 1];
    }
}
