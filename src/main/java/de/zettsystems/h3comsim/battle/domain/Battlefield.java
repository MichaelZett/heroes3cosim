package de.zettsystems.h3comsim.battle.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
        return hex.q() >= 0 && hex.q() < width && hex.r() >= 0 && hex.r() < height;
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
        double t = (double) hexesToMove / distance;
        int q = (int) Math.round(from.q() + t * (target.q() - from.q()));
        int r = (int) Math.round(from.r() + t * (target.r() - from.r()));
        return new Hex(q, r);
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
