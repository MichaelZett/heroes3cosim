package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
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
    /** Noch nicht ausgelöste Landminen (nur Tower). */
    private final Set<Hex> mines = new HashSet<>();
    private Gate gate = Gate.CLOSED;
    private int version;

    public Siege(Fortification fortification, Faction town) {
        this.fortification = Objects.requireNonNull(fortification, "fortification");
        this.town = Objects.requireNonNull(town, "town");
        for (WallPart part : WallPart.values()) {
            health.put(part, initialHealth(part));
        }
        if (town == Faction.TOWER && fortification.hasKeepAndMoat()) {
            mines.addAll(SiegeLayout.MOAT);
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

    /** Zieht Treffer ab; gibt zurück, ob das Teil damit fällt. Ein gefallenes Tor öffnet den Weg. */
    boolean damage(WallPart part, int hits) {
        int before = health(part);
        if (before == 0 || hits <= 0) {
            return false;
        }
        health.put(part, Math.max(0, before - hits));
        version++;
        boolean falls = health(part) == 0;
        if (falls && part == WallPart.GATE) {
            gate = Gate.DESTROYED;
        }
        return falls;
    }

    /** Zählt jede Änderung; zum Verwerfen zwischengespeicherter Felder. */
    int version() {
        return version;
    }

    /** Ist ein Weg hinein offen: die Zugbrücke unten oder zerstört, ein Mauersegment gefallen? */
    boolean hasBreach() {
        return gate == Gate.OPEN || gate == Gate.DESTROYED
                || Arrays.stream(WallPart.values()).anyMatch(part -> part.isWall() && !stands(part));
    }

    /** Der Zustand der Zugbrücke. */
    public Gate gate() {
        return gate;
    }

    /**
     * Führt die Zugbrücke nach, nachdem sich etwas auf dem Feld bewegt hat oder gefallen ist
     * (VCMI, <em>updateGateState</em>). Ein Stack, lebend oder tot, auf einer der beiden Torhälften
     * hält sie offen — dorthin kommt bei geschlossener Brücke nur der Verteidiger, und erst dann
     * öffnet sie sich. Ein Stack auf dem Feld davor hält sie geschlossen und sperrt das Tor auch für
     * den Verteidiger. Beim breiten Graben von Fortress gehört das Feld davor zur Brücke: Dort hält
     * ein Stack die offene Brücke offen, und ein Verteidiger, der dort steht, hat sie geöffnet.
     *
     * @param occupied die Felder aller Stacks, auch der gefallenen
     * @param defenders die Felder der lebenden Stacks des Verteidigers
     * @return ob sich der Zustand geändert hat
     */
    boolean updateGate(Set<Hex> occupied, Set<Hex> defenders) {
        Gate before = gate;
        boolean onGate = occupied.contains(SiegeLayout.GATE_OUTER) || occupied.contains(SiegeLayout.GATE_INNER);
        boolean onBridge = occupied.contains(SiegeLayout.GATE_BRIDGE);
        if (!stands(WallPart.GATE)) {
            gate = Gate.DESTROYED;
        } else if (gate == Gate.OPEN) {
            gate = onGate || (onBridge && hasWideMoat()) ? Gate.OPEN : Gate.CLOSED;
        } else if (onGate || (hasWideMoat() && defenders.contains(SiegeLayout.GATE_BRIDGE))) {
            gate = Gate.OPEN;
        } else {
            gate = onBridge ? Gate.BLOCKED : Gate.CLOSED;
        }
        if (gate == before) {
            return false;
        }
        version++;
        return true;
    }

    private boolean hasWideMoat() {
        return town == Faction.FORTRESS && fortification.hasKeepAndMoat();
    }

    /**
     * Felder, die für diese Seite nicht begehbar sind: die Mauern; das Tor bei geschlossener Brücke
     * für den Angreifer, bei gesperrter für beide.
     */
    Set<Hex> blockedFor(Side side) {
        Set<Hex> blocked = shotBlockers();
        if (gate == Gate.BLOCKED || (gate == Gate.CLOSED && side == Side.ATTACKER)) {
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

    /**
     * Ein Grabenfeld, das Bodentruppen anhält und verletzt — bei Tower gibt es keins. Über das Feld
     * vor dem Tor im Graben von Fortress führt die offene oder zerstörte Brücke.
     */
    boolean isMoat(Hex hex) {
        if (hex.equals(SiegeLayout.GATE_BRIDGE) && (gate == Gate.OPEN || gate == Gate.DESTROYED)) {
            return false;
        }
        return town != Faction.TOWER && moat().contains(hex);
    }

    /**
     * Schaden des Grabens (SoD): 90 bei Dungeon, Inferno und Fortress, sonst 70
     * (heroes.thelazy.net, <em>Moat</em>).
     */
    int moatDamage() {
        return switch (town) {
            case DUNGEON, INFERNO, FORTRESS -> 90;
            default -> 70;
        };
    }

    /** Löst die Landmine auf diesem Feld aus, falls dort noch eine liegt. */
    boolean triggerMine(Hex hex) {
        return mines.remove(hex);
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

    /** Zustand der Zugbrücke (heroes.thelazy.net, <em>Wall</em>, Abschnitt Drawbridge). */
    @Schema(name = "Gate", description = "Zustand der Zugbrücke.", enumAsRef = true)
    public enum Gate {
        /** Hochgezogen: nur der Verteidiger kommt hindurch, und dabei öffnet sie sich. */
        CLOSED,
        /** Heruntergelassen: frei für beide Seiten, solange ein Stack auf dem Tor steht oder liegt. */
        OPEN,
        /** Ein Stack vor dem Tor hält sie oben; niemand kommt hindurch. */
        BLOCKED,
        /** Zerstört: frei für beide Seiten. */
        DESTROYED
    }
}
