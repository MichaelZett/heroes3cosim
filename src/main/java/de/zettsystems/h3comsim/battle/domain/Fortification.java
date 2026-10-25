package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ausbau der Stadtbefestigung (heroes.thelazy.net, <em>Fortifications</em>): Fort baut Mauern und
 * Tor, Citadel fügt den Hauptturm (Keep) und den Graben hinzu, Castle verstärkt die Mauern und
 * baut zwei weitere Türme. Mauersegmente halten 2 Treffer, mit Castle 3; das Tor immer 2.
 */
@Schema(description = "Befestigung der belagerten Stadt.", enumAsRef = true)
public enum Fortification {
    FORT(2, false, false),
    CITADEL(2, true, false),
    CASTLE(3, true, true);

    private final int wallHealth;
    private final boolean keepAndMoat;
    private final boolean sideTowers;

    Fortification(int wallHealth, boolean keepAndMoat, boolean sideTowers) {
        this.wallHealth = wallHealth;
        this.keepAndMoat = keepAndMoat;
        this.sideTowers = sideTowers;
    }

    /** Treffer, die ein Mauersegment aushält. */
    public int wallHealth() {
        return wallHealth;
    }

    /** Hauptturm und Graben ab Citadel. */
    public boolean hasKeepAndMoat() {
        return keepAndMoat;
    }

    /** Die beiden Seitentürme mit Castle. */
    public boolean hasSideTowers() {
        return sideTowers;
    }
}
