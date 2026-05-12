package de.zettsystems.h3comsim.domain;

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
     * <p>Bei leerem Obstacle-Set wird die gerade Linie gewählt. Sonst übernimmt der A*-Pfadfinder:
     * Bewegung folgt dem kürzesten obstaclefreien Pfad, der mindestens einen Hex vor dem Ziel
     * endet. Findet er keinen Pfad, bleibt {@code from} unverändert (Wait-Fallback).
     */
    public Hex moveToward(Hex from, Hex target, int maxHexes) {
        if (obstacles.isEmpty()) {
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
}
