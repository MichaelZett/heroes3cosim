package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Die Pfeiltürme der Stadt (heroes.thelazy.net, <em>Arrow tower</em>): der Hauptturm ab Citadel,
 * zwei Seitentürme mit Castle. Sie schießen zu Rundenbeginn vor dem Katapult, jeder einmal.
 * Schaden des Hauptturms (10–20) + (2–4) × N, der Seitentürme (6–12) + (2–4) × ⌊N/2⌋, mit N der
 * Zahl der „Gebäude" der Stadt — hier voll ausgebaut (Castle 17, Rampart 16, Inferno und Conflux
 * 17, sonst 18; ohne Castle eines weniger). Der Schaden ignoriert die Defense des Ziels, kennt
 * kein Glück und keine Fertigkeit.
 *
 * <p>Ziel: Das Spiel bewertet Angreifer in Klassen — wer sofort angreifen kann (Schützen), zählt
 * voll, wer erst später, die Hälfte oder ein Drittel. Hier: der Stack, bei dem der erwartete
 * Schaden den meisten Goldwert vernichtet, Schützen voll, alle anderen zur Hälfte.
 */
final class ArrowTowers {

    private static final List<WallPart> TOWERS = List.of(WallPart.KEEP, WallPart.UPPER_TOWER, WallPart.LOWER_TOWER);

    private ArrowTowers() {
    }

    /** Die stehenden Türme in Schussreihenfolge: Hauptturm, oberer, unterer. */
    static List<WallPart> standing(Siege siege) {
        return TOWERS.stream().filter(siege::stands).toList();
    }

    /** Gebäude der voll ausgebauten Stadt, die den Turmschaden heben. */
    static int structures(Siege siege) {
        int full = switch (siege.town()) {
            case RAMPART -> 16;
            case CASTLE, INFERNO, CONFLUX -> 17;
            default -> 18;
        };
        return siege.fortification() == Fortification.CASTLE ? full : full - 1;
    }

    static int damage(WallPart tower, Siege siege, RandomGenerator rng) {
        int n = structures(siege);
        int perStructure = rng.nextInt(2, 5);
        return tower == WallPart.KEEP
                ? rng.nextInt(10, 21) + perStructure * n
                : rng.nextInt(6, 13) + perStructure * (n / 2);
    }

    /** Mittlerer Schaden eines Turms, für die Zielwahl. */
    static double expectedDamage(WallPart tower, Siege siege) {
        int n = tower == WallPart.KEEP ? structures(siege) : structures(siege) / 2;
        return (tower == WallPart.KEEP ? 15 : 9) + 3.0 * n;
    }

    static @Nullable Stack target(WallPart tower, Siege siege, BattleSetup setup) {
        double damage = expectedDamage(tower, siege);
        Stack best = null;
        double bestValue = -1;
        for (Stack attacker : setup.stacksOf(Side.ATTACKER)) {
            if (!attacker.isAlive()) {
                continue;
            }
            int totalHealth = (attacker.getCount() - 1) * attacker.unit().health() + attacker.getCurrentHealth();
            double value = Math.min(damage, totalHealth) * attacker.unit().cost() / attacker.unit().health()
                    * (attacker.canShoot() ? 1 : 0.5);
            if (value > bestValue) {
                best = attacker;
                bestValue = value;
            }
        }
        return best;
    }
}
