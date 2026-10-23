package de.zettsystems.h3comsim.battle.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Das Schlachtfeld: ein Rechteck aus {@code height} Reihen mit versetzten Hexen, wie in H3.
 *
 * <p>{@link Hex} rechnet in axialen Koordinaten (Distanz und Nachbarn stimmen dort ohne
 * Sonderfälle). Ein Rechteck ist in axialen Koordinaten aber nicht {@code 0 ≤ q < width} — das
 * wäre ein Parallelogramm mit zwei spitzen und zwei stumpfen Ecken, und spiegelbildlich
 * aufgestellte Armeen stünden darin nicht gleichwertig. Stattdessen gehört eine Zelle dazu, wenn
 * ihre waagerechte Lage {@code q + r/2} innerhalb der Mittelreihe liegt:
 * {@code c ≤ 2q + r ≤ c + 2(width − 1)} mit {@code c = height / 2}.
 *
 * <p>Folgen: Die Mittelreihe {@code r = c} hat wie bisher {@code q = 0 … width − 1}. Reihen mit
 * derselben Parität haben {@code width} Zellen, die anderen eine weniger — nur so ist das Feld
 * exakt links-rechts-spiegelsymmetrisch ({@link #mirror(Hex)}). In den oberen Reihen beginnt q
 * später, in den unteren früher, auch negativ.
 */
public record Battlefield(int width, int height, Set<Hex> obstacles) {

    public Battlefield {
        Objects.requireNonNull(obstacles, "obstacles");
        obstacles = Set.copyOf(obstacles);
    }

    public static final Battlefield STANDARD = new Battlefield(15, 11, Set.of());

    public Battlefield withObstacles(Set<Hex> newObstacles) {
        return new Battlefield(width, height, newObstacles);
    }

    public boolean contains(Hex hex) {
        int lateral = 2 * hex.q() + hex.r();
        return hex.r() >= 0 && hex.r() < height
                && lateral >= centerRow() && lateral <= centerRow() + 2 * (width - 1);
    }

    /** Die Mittelreihe, deren Zellen wie früher {@code q = 0 … width − 1} tragen. */
    public int centerRow() {
        return height / 2;
    }

    /** Kleinstes q der Reihe {@code r} — die linke Randzelle, auf der der Angreifer aufstellt. */
    public int leftmostQ(int r) {
        return Math.ceilDiv(centerRow() - r, 2);
    }

    /** Größtes q der Reihe {@code r} — die rechte Randzelle, auf der der Verteidiger aufstellt. */
    public int rightmostQ(int r) {
        return Math.floorDiv(centerRow() + 2 * (width - 1) - r, 2);
    }

    /**
     * Spiegelbild an der senkrechten Mittelachse: dieselbe Reihe, gleicher Abstand zum
     * gegenüberliegenden Rand. Erhält Distanzen und bildet das Feld auf sich selbst ab.
     */
    public Hex mirror(Hex hex) {
        return new Hex(leftmostQ(hex.r()) + rightmostQ(hex.r()) - hex.q(), hex.r());
    }

    /** Alle Zellen des Felds, Reihe für Reihe von links nach rechts. */
    public List<Hex> cells() {
        List<Hex> cells = new ArrayList<>();
        for (int r = 0; r < height; r++) {
            for (int q = leftmostQ(r); q <= rightmostQ(r); q++) {
                cells.add(new Hex(q, r));
            }
        }
        return cells;
    }

    public boolean hasObstacle(Hex hex) {
        return obstacles.contains(hex);
    }

    public boolean isPassable(Hex hex) {
        return contains(hex) && !obstacles.contains(hex);
    }

    /**
     * Liefert eine Position auf einer Linie zwischen {@code from} und {@code target}, höchstens
     * {@code maxHexes} Schritte von {@code from} entfernt. Stoppt mindestens einen Hex vor dem
     * Ziel — Bewegung schließt nie auf das Zielfeld auf, weil dort der Gegner steht.
     *
     * <p>{@link Movement#FLYING} überspringt Hindernisse („surmount obstacles, including walls",
     * Manual) → gerade Linie. {@link Movement#GROUND} muss A* um Hindernisse herum.
     */
    public Hex moveToward(Hex from, Hex target, int maxHexes, Movement movement) {
        if (movement == Movement.FLYING || obstacles.isEmpty()) {
            return straightMoveToward(from, target, maxHexes);
        }
        return PathFinder.stepToward(this, from, target, maxHexes);
    }

    private Hex straightMoveToward(Hex from, Hex target, int maxHexes) {
        int distance = from.distanceTo(target);
        int hexesToMove = Math.clamp(distance - 1L, 0, maxHexes);
        if (hexesToMove == 0) {
            return from;
        }
        // Auf der Linie zwischen zwei Zellen bleibt man im Rechteck; nur das Runden kann am
        // Rand der kürzeren Reihen eine halbe Zelle hinausschieben. Dann einen Schritt weniger.
        for (int steps = hexesToMove; steps > 0; steps--) {
            double t = (double) steps / distance;
            int q = (int) Math.round(from.q() + t * (target.q() - from.q()));
            int r = (int) Math.round(from.r() + t * (target.r() - from.r()));
            Hex candidate = new Hex(q, r);
            if (contains(candidate)) {
                return candidate;
            }
        }
        return from;
    }

    /**
     * Liefert die Hex-für-Hex-Sequenz zwischen {@code from} (exklusive) und {@code destination}
     * (inklusive). {@link Movement#FLYING} fliegt direkte Cube-Linie (auch über Hindernisse),
     * {@link Movement#GROUND} läuft den kürzesten obstaclefreien A*-Pfad. Bei
     * {@code destination == from} oder fehlendem Pfad: leere Liste. Für die Replay-Animation,
     * damit Tokens nicht einen Sprung machen, sondern sichtbar Schritt für Schritt laufen.
     */
    public List<Hex> findPath(Hex from, Hex destination, Movement movement) {
        if (from.equals(destination)) {
            return List.of();
        }
        if (movement == Movement.FLYING || obstacles.isEmpty()) {
            return straightLine(from, destination);
        }
        return PathFinder.findPath(this, from, destination);
    }

    private static List<Hex> straightLine(Hex from, Hex destination) {
        int distance = from.distanceTo(destination);
        List<Hex> path = new ArrayList<>(distance);
        for (int i = 1; i <= distance; i++) {
            double t = (double) i / distance;
            int q = (int) Math.round(from.q() + t * (destination.q() - from.q()));
            int r = (int) Math.round(from.r() + t * (destination.r() - from.r()));
            path.add(new Hex(q, r));
        }
        return path;
    }
}
