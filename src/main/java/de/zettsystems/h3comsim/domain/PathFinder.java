package de.zettsystems.h3comsim.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Hex-A* für Bewegung um Obstacles. Findet den kürzesten obstaclefreien Pfad von {@code from} zu
 * einem Nachbarn von {@code target} (das Zielfeld selbst darf nicht betreten werden, dort steht
 * der Gegner). Ohne Obstacles tut sich Battlefield mit der geraden Linie leichter — daher wird
 * dieser Klasse nur dann gerufen, wenn {@code battlefield.obstacles()} nicht leer ist.
 */
public final class PathFinder {

    private static final int ITERATION_CAP = 200;

    private PathFinder() {
    }

    /**
     * Bewegt sich um bis zu {@code maxHexes} Schritte Richtung Ziel, ausweichend um Obstacles.
     * Liefert {@code from} zurück, wenn der Stack schon adjacent ist oder kein Pfad existiert.
     */
    public static Hex stepToward(Battlefield bf, Hex from, Hex target, int maxHexes) {
        if (maxHexes <= 0 || from.distanceTo(target) <= 1) {
            return from;
        }
        List<Hex> path = shortestPath(bf, from, target);
        if (path.isEmpty()) {
            return from;
        }
        int stepIndex = Math.min(maxHexes, path.size()) - 1;
        return path.get(stepIndex);
    }

    /**
     * Liefert den vollständigen A*-Pfad von {@code from} (exklusive) bis {@code destination}
     * (inklusive). Leer, wenn der Endpunkt nicht passable ist oder kein Pfad gefunden wird.
     */
    public static List<Hex> findPath(Battlefield bf, Hex from, Hex destination) {
        if (from.equals(destination) || !bf.isPassable(destination)) {
            return List.of();
        }
        Map<Hex, Hex> cameFrom = new HashMap<>();
        Map<Hex, Integer> gScore = new HashMap<>();
        gScore.put(from, 0);
        PriorityQueue<Hex> open = new PriorityQueue<>(
                Comparator.comparingInt(h -> gScore.getOrDefault(h, Integer.MAX_VALUE) + h.distanceTo(destination)));
        open.add(from);
        Set<Hex> closed = new HashSet<>();

        int iterations = 0;
        while (!open.isEmpty() && iterations++ < ITERATION_CAP) {
            Hex current = open.poll();
            if (current.equals(destination)) {
                return reconstruct(cameFrom, current);
            }
            if (!closed.add(current)) {
                continue;
            }
            int currentG = gScore.getOrDefault(current, Integer.MAX_VALUE);
            for (Hex neighbor : current.neighbors()) {
                if (!bf.contains(neighbor) || bf.hasObstacle(neighbor)) {
                    continue;
                }
                int tentativeG = currentG + 1;
                if (tentativeG < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    open.add(neighbor);
                }
            }
        }
        return List.of();
    }

    /**
     * Schießt eine gerade Hex-Linie von {@code from} nach {@code to} und prüft, ob ein
     * Obstacle dazwischen liegt. Endpunkte werden nicht geprüft.
     */
    public static boolean hasObstacleInLine(Battlefield bf, Hex from, Hex to) {
        int distance = from.distanceTo(to);
        if (distance <= 1 || bf.obstacles().isEmpty()) {
            return false;
        }
        for (int i = 1; i < distance; i++) {
            double t = (double) i / distance;
            Hex sample = cubeRound(
                    from.q() + (to.q() - from.q()) * t,
                    from.r() + (to.r() - from.r()) * t);
            if (bf.hasObstacle(sample)) {
                return true;
            }
        }
        return false;
    }

    private static List<Hex> shortestPath(Battlefield bf, Hex from, Hex target) {
        Set<Hex> goals = neighborGoals(bf, target);
        if (goals.isEmpty()) {
            return List.of();
        }
        Map<Hex, Hex> cameFrom = new HashMap<>();
        Map<Hex, Integer> gScore = new HashMap<>();
        gScore.put(from, 0);
        PriorityQueue<Hex> open = new PriorityQueue<>(
                Comparator.comparingInt(h -> gScore.getOrDefault(h, Integer.MAX_VALUE) + minDistance(h, goals)));
        open.add(from);
        Set<Hex> closed = new HashSet<>();

        int iterations = 0;
        while (!open.isEmpty() && iterations++ < ITERATION_CAP) {
            Hex current = open.poll();
            if (goals.contains(current)) {
                return reconstruct(cameFrom, current);
            }
            if (!closed.add(current)) {
                continue;
            }
            int currentG = gScore.getOrDefault(current, Integer.MAX_VALUE);
            for (Hex neighbor : current.neighbors()) {
                if (!bf.contains(neighbor) || bf.hasObstacle(neighbor)) {
                    continue;
                }
                int tentativeG = currentG + 1;
                if (tentativeG < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    open.add(neighbor);
                }
            }
        }
        return List.of();
    }

    private static Set<Hex> neighborGoals(Battlefield bf, Hex target) {
        Set<Hex> goals = new HashSet<>();
        for (Hex n : target.neighbors()) {
            if (bf.isPassable(n)) {
                goals.add(n);
            }
        }
        return goals;
    }

    private static int minDistance(Hex from, Set<Hex> goals) {
        int best = Integer.MAX_VALUE;
        for (Hex g : goals) {
            int d = from.distanceTo(g);
            if (d < best) {
                best = d;
            }
        }
        return best;
    }

    private static List<Hex> reconstruct(Map<Hex, Hex> cameFrom, Hex end) {
        List<Hex> reverse = new ArrayList<>();
        Hex current = end;
        while (cameFrom.containsKey(current)) {
            reverse.add(current);
            current = cameFrom.get(current);
        }
        List<Hex> path = new ArrayList<>(reverse.size());
        for (int i = reverse.size() - 1; i >= 0; i--) {
            path.add(reverse.get(i));
        }
        return path;
    }

    private static Hex cubeRound(double qf, double rf) {
        double sf = -qf - rf;
        int q = (int) Math.round(qf);
        int r = (int) Math.round(rf);
        int s = (int) Math.round(sf);
        double dq = Math.abs(q - qf);
        double dr = Math.abs(r - rf);
        double ds = Math.abs(s - sf);
        if (dq > dr && dq > ds) {
            q = -r - s;
        } else if (dr > ds) {
            r = -q - s;
        }
        return new Hex(q, r);
    }
}
