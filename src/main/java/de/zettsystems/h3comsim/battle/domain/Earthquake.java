package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Earthquake (heroes.thelazy.net, <em>Earthquake</em>): je ein Treffer auf zwei, drei oder vier
 * zufällige Mauern oder Türme, nur in einer Belagerung. Welche Teile infrage kommen, lässt der
 * Wortlaut offen; hier wie in VCMI jedes stehende Teil der Befestigung samt Tor, und jeder Treffer
 * wählt neu unter den noch stehenden.
 */
final class Earthquake {

    private Earthquake() {
    }

    static int hits(SkillLevel level) {
        return SpellDamage.byLevel(level, 2, 3, 4);
    }

    /** Ein Treffer auf ein zufälliges stehendes Teil, oder {@code null}, wenn nichts mehr steht. */
    static @Nullable WallPart strike(Siege siege, RandomGenerator rng) {
        List<WallPart> standing = Arrays.stream(WallPart.values()).filter(siege::stands).toList();
        if (standing.isEmpty()) {
            return null;
        }
        WallPart hit = standing.get(rng.nextInt(standing.size()));
        siege.damage(hit, 1);
        return hit;
    }

    /** Wahrscheinlichkeit, dass so viele Treffer ein Mauersegment oder das Tor zum Einsturz bringen. */
    static double breachChance(Siege siege, int hits) {
        Map<WallPart, Integer> health = new EnumMap<>(WallPart.class);
        for (WallPart part : WallPart.values()) {
            if (siege.stands(part)) {
                health.put(part, siege.health(part));
            }
        }
        return breachChance(health, hits);
    }

    private static double breachChance(Map<WallPart, Integer> health, int hits) {
        boolean open = health.entrySet().stream()
                .anyMatch(e -> e.getValue() == 0 && (e.getKey().isWall() || e.getKey() == WallPart.GATE));
        if (open) {
            return 1;
        }
        List<WallPart> standing = health.entrySet().stream().filter(e -> e.getValue() > 0).map(Map.Entry::getKey)
                .toList();
        if (hits == 0 || standing.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (WallPart part : standing) {
            Map<WallPart, Integer> after = new EnumMap<>(health);
            after.merge(part, -1, Integer::sum);
            sum += breachChance(after, hits - 1);
        }
        return sum / standing.size();
    }
}
