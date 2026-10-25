package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Eine Belagerung: Befestigung und Stadt des Verteidigers, dazu der Zustand von Mauern, Tor und
 * Türmen. Mauern blockieren jede Bewegung am Boden, das geschlossene Tor nur die Angreifer
 * (heroes.thelazy.net, <em>Wall</em>); Flieger fliegen darüber. Ein Schuss von draußen über eine
 * stehende Mauer verliert die Hälfte (<em>Obstacle penalty</em>), die Torhälften halbieren nicht.
 * Ein gefallenes Segment wird begehbar.
 */
public final class Siege {

    private final Fortification fortification;
    private final Faction town;
    private final Map<WallPart, Integer> health = new EnumMap<>(WallPart.class);
    private int version;

    public Siege(Fortification fortification, Faction town) {
        this.fortification = Objects.requireNonNull(fortification, "fortification");
        this.town = Objects.requireNonNull(town, "town");
        for (WallPart part : WallPart.values()) {
            health.put(part, initialHealth(part));
        }
    }

    private int initialHealth(WallPart part) {
        if (part.isWall()) {
            return fortification.wallHealth();
        }
        return switch (part) {
            case GATE -> 2;
            case KEEP -> fortification.hasKeepAndMoat() ? 2 : 0;
            case UPPER_TOWER, LOWER_TOWER -> fortification.hasSideTowers() ? 2 : 0;
            default -> 0;
        };
    }

    public Fortification fortification() {
        return fortification;
    }

    /** Die Stadt des Verteidigers; bestimmt Graben und Turmschaden. */
    public Faction town() {
        return town;
    }

    /** Verbleibende Treffer des Teils; 0, wenn es fehlt oder zerstört ist. */
    public int health(WallPart part) {
        return health.getOrDefault(part, 0);
    }

    public boolean stands(WallPart part) {
        return health(part) > 0;
    }

    /** Zieht Treffer ab; gibt zurück, ob das Teil damit fällt. */
    boolean damage(WallPart part, int hits) {
        int before = health(part);
        if (before == 0 || hits <= 0) {
            return false;
        }
        health.put(part, Math.max(0, before - hits));
        version++;
        return health(part) == 0;
    }

    /** Zählt jede Änderung; zum Verwerfen zwischengespeicherter Felder. */
    int version() {
        return version;
    }

    /** Felder, die für diese Seite nicht begehbar sind: Mauern, für Angreifer auch das stehende Tor. */
    Set<Hex> blockedFor(Side side) {
        Set<Hex> blocked = shotBlockers();
        if (side == Side.ATTACKER && stands(WallPart.GATE)) {
            blocked.add(SiegeLayout.GATE_OUTER);
            blocked.add(SiegeLayout.GATE_INNER);
        }
        return blocked;
    }

    /** Felder, die Schüsse halbieren: die unzerstörbaren Stücke und die stehenden Segmente. */
    private Set<Hex> shotBlockers() {
        Set<Hex> blockers = new HashSet<>(SiegeLayout.PERMANENT_WALL);
        for (WallPart part : WallPart.values()) {
            Hex hex = part.hex();
            if (part.isWall() && hex != null && stands(part)) {
                blockers.add(hex);
            }
        }
        return blockers;
    }

    /**
     * Halbiert die Mauer einen Schuss von {@code from} auf {@code to}? Nur, wenn der Schütze
     * draußen steht, das Ziel drinnen und die Linie über eine stehende Mauer führt.
     */
    boolean crossesWall(Hex from, Hex to, Battlefield field) {
        if (!SiegeLayout.isOutside(from) || SiegeLayout.isOutside(to)) {
            return false;
        }
        return PathFinder.hasObstacleInLine(field.withObstacles(shotBlockers()), from, to);
    }

    /**
     * Die Grabenfelder: ab Citadel, bei Fortress zweireihig. Bei Tower liegen dort Landminen statt
     * eines Grabens.
     */
    Set<Hex> moat() {
        if (!fortification.hasKeepAndMoat()) {
            return Set.of();
        }
        return town == Faction.FORTRESS ? SiegeLayout.FORTRESS_MOAT : SiegeLayout.MOAT;
    }

    /** Das Teil auf diesem Feld, oder {@code null}. */
    static @Nullable WallPart partAt(Hex hex) {
        for (WallPart part : WallPart.values()) {
            if (hex.equals(part.hex())) {
                return part;
            }
        }
        return null;
    }
}
