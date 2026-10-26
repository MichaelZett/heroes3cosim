package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Das Glücks-Rating einer Armee (Manual S. 44): „Each army has a luck rating. Troops in armies
 * with good luck (a positive luck rating) have a chance to inflict double damage when they
 * strike." Chance 4.2 / 8.3 / 12.5 % bei Glück 1 / 2 / 3.
 *
 * <p>Zusammengesetzt aus der Luck-Fertigkeit des Helden (+1/2/3, Manual S. 37), dem gesammelten
 * Glück ({@link CollectedBonus}) und {@link UnitSpeciality#LOWERS_ENEMY_LUCK}: Devils und Arch
 * Devils senken das Glück der Gegenseite um eins (Manual S. 99), einmal pro Armee.
 *
 * <p>Zwei SoD-Regeln, die das Manual nicht ausspricht und die HotA später geändert hat
 * (heroes.thelazy.net): Glück wirkt nur, wenn ein Held die Armee führt, und negatives Glück gibt
 * es nicht — das Rating wird auf 0..3 gekappt. Eine führerlose Armee hat also immer Glück 0.
 */
public final class ArmyLuck {

    public static final int MIN = 0;
    public static final int MAX = 3;

    private ArmyLuck() {
    }

    /**
     * Glücks-Rating der Armee, bereits gekappt.
     *
     * @param hero        Anführer der Armee, {@code null} bei führerloser Armee
     * @param bonus       gesammelte Boni des Helden
     * @param enemyStacks alle Stacks der Gegenseite, auch bereits gefallene — das Rating wird zu
     *                    Kampfbeginn bestimmt
     */
    public static int ratingOf(@Nullable Hero hero, CollectedBonus bonus, List<Stack> enemyStacks) {
        if (hero == null) {
            return 0;
        }
        int rating = hero.luckBonus() + bonus.luck() + hero.artifacts().luck();
        boolean enemyLowersLuck = enemyStacks.stream()
                .anyMatch(stack -> stack.unit().hasSpeciality(UnitSpeciality.LOWERS_ENEMY_LUCK));
        if (enemyLowersLuck) {
            rating--;
        }
        return Math.clamp(rating, MIN, MAX);
    }
}
