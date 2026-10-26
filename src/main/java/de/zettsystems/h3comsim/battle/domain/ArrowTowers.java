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
 * <p>Ziel: Ohne Artillery steuert sie die Spiel-KI. Sie teilt die Angreifer in Klassen — wer
 * sofort angreifen kann (Schützen), zählt voll, wer im nächsten Zug, die Hälfte, wer erst später,
 * ein Drittel — und rechnet mit einem Turmschaden von 1 (ein Fehler des Spiels): Sie schießt
 * damit auf den höchsten Wert je Lebenspunkt, egal wie viel der Schuss wirklich vernichtet, bei
 * Gleichstand auf den Stack ganz rechts in der Armee. Ein Held mit Artillery steuert die Türme
 * selbst (heroes.thelazy.net, <em>Arrow tower</em>, <em>Artillery</em>); hier wählt er den Stack,
 * bei dem der erwartete Schaden den meisten Goldwert vernichtet, Schützen voll, alle anderen zur
 * Hälfte.
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
        Hero defender = setup.heroOf(Side.DEFENDER);
        boolean controlled = defender != null && defender.levelOf(SecondarySkill.ARTILLERY) != SkillLevel.NONE;
        double damage = expectedDamage(tower, siege);
        Stack best = null;
        double bestValue = -1;
        for (Stack attacker : setup.stacksOf(Side.ATTACKER)) {
            if (!attacker.isAlive()) {
                continue;
            }
            double value = controlled ? controlledValue(attacker, damage) : gameAiValue(attacker, setup);
            if (controlled ? value > bestValue : value >= bestValue) {
                best = attacker;
                bestValue = value;
            }
        }
        return best;
    }

    private static double controlledValue(Stack attacker, double damage) {
        int totalHealth = (attacker.getCount() - 1) * attacker.maxHealth() + attacker.getCurrentHealth();
        return Math.min(damage, totalHealth) * attacker.unit().cost() / attacker.maxHealth()
                * (attacker.canShoot() ? 1 : 0.5);
    }

    /**
     * Wert je Lebenspunkt nach Klasse: sofort angreifen kann, wer schießt oder in Reichweite steht,
     * im nächsten Zug, wer höchstens doppelt so weit steht — die Mauern bleiben dabei außer Acht.
     * Die Stacks kommen in Slot-Reihenfolge, der letzte gewinnt Gleichstände.
     */
    private static double gameAiValue(Stack attacker, BattleSetup setup) {
        double perHealth = (double) attacker.unit().cost() / attacker.maxHealth();
        if (attacker.canShoot()) {
            return perHealth;
        }
        int distance = setup.stacksOf(Side.DEFENDER).stream().filter(Stack::isAlive)
                .mapToInt(attacker::distanceTo).min().orElse(Integer.MAX_VALUE);
        int speed = attacker.getSpeed();
        if (distance <= speed + 1) {
            return perHealth;
        }
        return perHealth * (distance <= 2 * speed + 1 ? 0.5 : 1.0 / 3);
    }
}
