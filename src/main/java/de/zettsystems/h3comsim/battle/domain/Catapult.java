package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Das Katapult des Angreifers (heroes.thelazy.net, <em>Fortifications</em> und Trefferliste der
 * Ballistics-Seite, Werte für SoD): ein Schuss je Runde, zwei ab Advanced Ballistics.
 *
 * <p>Ziel: Ohne Ballistics das Mauersegment mit den wenigsten Treffern (bei Gleichstand zufällig),
 * erst danach Tor, Hauptturm, Seitentürme. Mit Ballistics zuerst das Tor, dann die Mauern, solange
 * alle vier stehen; ist eine gefallen, der Hauptturm und die Seitentürme, zuletzt wieder Mauern.
 *
 * <p>Treffer: Mauer 50/60/60/75 %, Tor 25/30/30/40 %, Hauptturm 5/7/7/10 %, Seitenturm
 * 10/15/15/20 % (ohne, Basic, Advanced, Expert); steht keine Mauer mehr, trifft jeder Schuss. Ein
 * Fehlschuss geht an das nächste stehende Mauersegment entlang der Mauer. Schaden: ohne
 * Ballistics 10 % keiner, 60 % einer, 30 % zwei Treffer; Basic und Advanced je zur Hälfte einer
 * oder zwei; Expert immer zwei.
 */
final class Catapult {

    /** Die Mauersegmente in ihrer Reihenfolge entlang der Mauer, von oben nach unten. */
    private static final List<WallPart> ALONG_THE_WALL = List.of(
            WallPart.UPPER_WALL, WallPart.MID_UPPER_WALL, WallPart.MID_LOWER_WALL, WallPart.LOWER_WALL);

    /** Ein Schuss: worauf gezielt, was getroffen, wie viele Treffer. */
    record Shot(WallPart aimed, WallPart hit, int damage) {
    }

    private Catapult() {
    }

    static int shots(SkillLevel ballistics) {
        return ballistics == SkillLevel.ADVANCED || ballistics == SkillLevel.EXPERT ? 2 : 1;
    }

    /** Ein Schuss auf die Befestigung, oder {@code null}, wenn nichts mehr steht. */
    static @Nullable Shot fire(Siege siege, SkillLevel ballistics, RandomGenerator rng) {
        WallPart aimed = target(siege, ballistics, rng);
        if (aimed == null) {
            return null;
        }
        WallPart hit = aimed;
        boolean wallsLeft = ALONG_THE_WALL.stream().anyMatch(siege::stands);
        if (wallsLeft && rng.nextInt(100) >= hitPercent(aimed, ballistics)) {
            hit = redirect(siege, aimed, rng);
        }
        return new Shot(aimed, hit, damage(ballistics, rng));
    }

    static @Nullable WallPart target(Siege siege, SkillLevel ballistics, RandomGenerator rng) {
        List<WallPart> walls = ALONG_THE_WALL.stream().filter(siege::stands).toList();
        if (ballistics == SkillLevel.NONE) {
            if (!walls.isEmpty()) {
                return weakest(siege, walls, rng);
            }
            return firstStanding(siege, WallPart.GATE, WallPart.KEEP, WallPart.UPPER_TOWER, WallPart.LOWER_TOWER);
        }
        if (siege.stands(WallPart.GATE)) {
            return WallPart.GATE;
        }
        if (walls.size() == ALONG_THE_WALL.size()) {
            return weakest(siege, walls, rng);
        }
        WallPart tower = firstStanding(siege, WallPart.KEEP, WallPart.UPPER_TOWER, WallPart.LOWER_TOWER);
        if (tower != null) {
            return tower;
        }
        return walls.isEmpty() ? null : weakest(siege, walls, rng);
    }

    private static WallPart weakest(Siege siege, List<WallPart> walls, RandomGenerator rng) {
        int least = walls.stream().mapToInt(siege::health).min().orElseThrow();
        List<WallPart> candidates = walls.stream().filter(w -> siege.health(w) == least).toList();
        return candidates.size() == 1 ? candidates.getFirst() : candidates.get(rng.nextInt(candidates.size()));
    }

    private static @Nullable WallPart firstStanding(Siege siege, WallPart... parts) {
        for (WallPart part : parts) {
            if (siege.stands(part)) {
                return part;
            }
        }
        return null;
    }

    static int hitPercent(WallPart aimed, SkillLevel ballistics) {
        int level = switch (ballistics) {
            case NONE -> 0;
            case BASIC -> 1;
            case ADVANCED -> 2;
            case EXPERT -> 3;
        };
        if (aimed.isWall()) {
            return new int[] {50, 60, 60, 75}[level];
        }
        return switch (aimed) {
            case GATE -> new int[] {25, 30, 30, 40}[level];
            case KEEP -> new int[] {5, 7, 7, 10}[level];
            default -> new int[] {10, 15, 15, 20}[level];
        };
    }

    /** Wohin ein Fehlschuss geht: das nächste stehende Segment entlang der Mauer. */
    private static WallPart redirect(Siege siege, WallPart aimed, RandomGenerator rng) {
        WallPart first = switch (aimed) {
            case UPPER_WALL, UPPER_TOWER -> aimed == WallPart.UPPER_WALL ? WallPart.MID_UPPER_WALL : WallPart.UPPER_WALL;
            case MID_UPPER_WALL -> WallPart.UPPER_WALL;
            case MID_LOWER_WALL -> WallPart.LOWER_WALL;
            case LOWER_WALL -> WallPart.MID_LOWER_WALL;
            case GATE -> rng.nextBoolean() ? WallPart.MID_UPPER_WALL : WallPart.MID_LOWER_WALL;
            case KEEP, LOWER_TOWER -> WallPart.LOWER_WALL;
        };
        return nearestStanding(siege, first);
    }

    private static WallPart nearestStanding(Siege siege, WallPart start) {
        int index = ALONG_THE_WALL.indexOf(start);
        List<WallPart> byDistance = new ArrayList<>(ALONG_THE_WALL);
        byDistance.sort((a, b) -> Integer.compare(Math.abs(ALONG_THE_WALL.indexOf(a) - index),
                Math.abs(ALONG_THE_WALL.indexOf(b) - index)));
        return byDistance.stream().filter(siege::stands).findFirst().orElse(start);
    }

    static int damage(SkillLevel ballistics, RandomGenerator rng) {
        int roll = rng.nextInt(100);
        return switch (ballistics) {
            case NONE -> roll < 10 ? 0 : roll < 70 ? 1 : 2;
            case BASIC, ADVANCED -> roll < 50 ? 1 : 2;
            case EXPERT -> 2;
        };
    }
}
