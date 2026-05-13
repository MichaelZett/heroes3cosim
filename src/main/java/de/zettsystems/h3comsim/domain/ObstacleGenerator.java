package de.zettsystems.h3comsim.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Erzeugt deterministisch eine Obstacle-Verteilung auf einem leeren Battlefield. Die zwei
 * äußersten Spalten beider Seiten bleiben frei — dort spawnen die Stacks und sollen Bewegungs-
 * raum haben. Anzahl steuert die "Dichte" des Feldes; H3 erzeugt selbst meist 5–15 Hindernisse.
 */
public final class ObstacleGenerator {

    public static final int DEFAULT_OBSTACLE_COUNT = 10;
    private static final int SPAWN_COLUMN_MARGIN = 2;

    private ObstacleGenerator() {
    }

    public static Set<Hex> generate(Battlefield base, RandomGenerator rng) {
        return generate(base, rng, DEFAULT_OBSTACLE_COUNT);
    }

    public static Set<Hex> generate(Battlefield base, RandomGenerator rng, int count) {
        List<Hex> candidates = new ArrayList<>();
        for (int q = SPAWN_COLUMN_MARGIN; q < base.width() - SPAWN_COLUMN_MARGIN; q++) {
            for (int r = 0; r < base.height(); r++) {
                candidates.add(new Hex(q, r));
            }
        }
        // Fisher-Yates über RandomGenerator — bleibt determinitsisch bei festem Seed.
        for (int i = candidates.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Hex tmp = candidates.get(i);
            candidates.set(i, candidates.get(j));
            candidates.set(j, tmp);
        }
        int n = Math.clamp(count, 0, candidates.size());
        return Set.copyOf(candidates.subList(0, n));
    }
}
