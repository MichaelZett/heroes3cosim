package de.zettsystems.h3comsim.battle.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Die zerstörbaren Teile der Befestigung (heroes.thelazy.net, <em>Fortifications</em>): vier
 * Mauersegmente, das Tor, der Hauptturm und die zwei Seitentürme. Der Hauptturm steht außerhalb
 * des Felds.
 */
@Schema(description = "Zerstörbarer Teil der Stadtbefestigung.", enumAsRef = true)
public enum WallPart {
    UPPER_WALL(29),
    MID_UPPER_WALL(78),
    MID_LOWER_WALL(130),
    LOWER_WALL(182),
    GATE(96),
    KEEP(-1),
    UPPER_TOWER(12),
    LOWER_TOWER(183);

    private final int h3Index;

    WallPart(int h3Index) {
        this.h3Index = h3Index;
    }

    /** Ein Mauersegment, das einen Weg öffnet, wenn es fällt. */
    public boolean isWall() {
        return this == UPPER_WALL || this == MID_UPPER_WALL || this == MID_LOWER_WALL || this == LOWER_WALL;
    }

    public boolean isTower() {
        return this == KEEP || this == UPPER_TOWER || this == LOWER_TOWER;
    }

    /** Das Feld des Teils, {@code null} für den Hauptturm außerhalb des Felds. */
    @Nullable Hex hex() {
        return h3Index < 0 ? null : SiegeLayout.h3(h3Index);
    }
}
