package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Wann ein Stack eine gegnerische Kriegsmaschine angreift: nur, wenn er in seinem Zug sonst keine
 * Kreatur angreift (der Solver will ziehen oder verteidigen) und eine Maschine erreicht. Die
 * teuerste erreichbare zuerst (Ballista vor Munitionswagen vor Zelt), bei Gleichstand die nähere.
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
        List<Stack> machines = setup.enemyMachinesOf(active);
        Action best = null;
        int bestCost = -1;
        int bestSteps = Integer.MAX_VALUE;
        for (Stack machine : machines) {
            int cost = machine.unit().cost();
            if (active.position().distanceTo(machine.position()) == 1) {
                if (cost > bestCost || (cost == bestCost && bestSteps > 0)) {
                    best = new Action.Melee(machine);
                    bestCost = cost;
                    bestSteps = 0;
                }
                continue;
            }
            for (Hex landing : machine.position().neighbors()) {
                int steps = stepsTo(active, landing, setup);
                if (steps > 0 && (cost > bestCost || (cost == bestCost && steps < bestSteps))) {
                    best = new Action.MoveAndMelee(landing, machine);
                    bestCost = cost;
                    bestSteps = steps;
                }
            }
        }
        return best;
    }

    /** Schritte bis zu einem freien Feld, oder 0, wenn es in diesem Zug nicht erreichbar ist. */
    private static int stepsTo(Stack active, Hex landing, BattleSetup setup) {
        Battlefield field = setup.battlefield();
        if (!field.contains(landing) || !setup.isFree(landing)) {
            return 0;
        }
        Hex from = active.position();
        int steps = active.unit().movement() == Movement.FLYING
                ? from.distanceTo(landing)
                : field.findPath(from, landing, Movement.GROUND).size();
        return steps <= active.getSpeed() ? steps : 0;
    }
}
