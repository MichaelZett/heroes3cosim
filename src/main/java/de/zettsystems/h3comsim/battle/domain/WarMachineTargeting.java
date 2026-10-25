package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Wann ein Stack eine gegnerische Kriegsmaschine angreift: nur, wenn er in seinem Zug sonst keine
 * Kreatur angreift (der Solver will ziehen oder verteidigen) und eine Maschine erreicht. Die
 * teuerste erreichbare zuerst (Ballista vor Munitionswagen vor Zelt), über das nächste freie Feld daneben.
 * Schützen, die schießen, bleiben bei ihrem Ziel — eine Kreatur zu treffen, ist fast immer mehr
 * wert als 250 Lebenspunkte Holz.
 */
final class WarMachineTargeting {

    private WarMachineTargeting() {
    }

    /** Der Angriff auf eine Maschine, oder {@code null}, wenn keine erreichbar ist. */
    static @Nullable Action attack(Stack active, BattleSetup setup) {
        if (active.isWarMachine()) {
            return null;
        }
        List<Stack> byCost = setup.enemyMachinesOf(active).stream()
                .sorted(Comparator.comparingInt((Stack m) -> m.unit().cost()).reversed())
                .toList();
        for (Stack machine : byCost) {
            Action action = approach(active, machine, setup);
            if (action != null) {
                return action;
            }
        }
        return null;
    }

    /** Der kürzeste Angriff auf diese Maschine in diesem Zug, oder {@code null}. */
    private static @Nullable Action approach(Stack active, Stack machine, BattleSetup setup) {
        if (active.distanceTo(machine) == 1) {
            return new Action.Melee(machine);
        }
        Hex best = null;
        int bestSteps = Integer.MAX_VALUE;
        for (Hex landing : setup.attackHeads(active, machine)) {
            int steps = stepsTo(active, landing, setup);
            if (steps > 0 && steps < bestSteps) {
                best = landing;
                bestSteps = steps;
            }
        }
        return best == null ? null : new Action.MoveAndMelee(best, machine);
    }

    /** Schritte bis zu einem freien Feld, oder 0, wenn es in diesem Zug nicht erreichbar ist. */
    private static int stepsTo(Stack active, Hex landing, BattleSetup setup) {
        Battlefield field = setup.battlefield();
        if (!field.contains(landing) || !setup.fits(active, landing)) {
            return 0;
        }
        Hex from = active.position();
        int steps = active.unit().movement() == Movement.FLYING
                ? from.distanceTo(landing)
                : field.findPath(from, landing, Movement.GROUND).size();
        return steps <= active.getSpeed() ? steps : 0;
    }
}
