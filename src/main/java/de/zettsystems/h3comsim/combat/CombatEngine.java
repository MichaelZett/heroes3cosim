package de.zettsystems.h3comsim.combat;

import de.zettsystems.h3comsim.arena.Arena;
import de.zettsystems.h3comsim.unit.common.Stack;
import de.zettsystems.h3comsim.unit.common.UnitSpeciality;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

public final class CombatEngine {

    private CombatEngine() {
        //not intended
    }
    public static void solveCombat(Arena arena) {
        CombatLogger.logStartOfCombat(arena.getAttackerName(), arena.getAttackerCount(), arena.getDefenderName(), arena.getDefenderCount());

        Queue<Stack> queue = determineMoveOrder(arena.getAttacker(), arena.getDefender());

        while (arena.isAttackerAlive() && arena.isDefenderAlive()) {
            Stack currentAttacker = queue.poll();
            assert currentAttacker != null;
            Stack currentDefender = queue.poll();
            assert currentDefender != null;
            queue.offer(currentDefender);
            queue.offer(currentAttacker);
            attack(currentAttacker, currentDefender, false);
            //check whether counterattack takes place
            if (retaliationPossible(currentAttacker) && arena.bothAlive() && !currentAttacker.isPetrified()) {
                attack(currentDefender, currentAttacker, true);
            } else if (!retaliationPossible(currentAttacker)) {
                CombatLogger.logImmuneToRetaliation(currentAttacker.getName());
            } else if (currentAttacker.isPetrified()) {
                CombatLogger.logDoNotRetaliateAgainstPetrified(currentDefender);
            }
            CombatLogger.logShortDelimiter();
            queue.forEach(Stack::endTurn);
        }
        CombatLogger.logMiddleDelimiter();

        queue.stream().filter(u -> !u.isAlive()).forEach(u -> CombatLogger.logDeath(u.getName()));
    }

    private static boolean retaliationPossible(Stack currentAttacker) {
        return !currentAttacker.hasSpeciality(UnitSpeciality.NO_RETALIATION);
    }

    private static void attack(Stack currentAttacker, Stack currentDefender, boolean counterattack) {
        if (currentAttacker.isPetrified()) {
            CombatLogger.logPetrified(currentAttacker.getName());
        } else {
            int currentDamage = currentAttacker.calculateCurrentDamage();
            int boniMaliPercentage = currentAttacker.calculateAttackBoniMaliPercentage(currentDefender.getDefense());
            int realDamage = (currentDamage * (100 + boniMaliPercentage)) / 100;
            if (counterattack) {
                CombatLogger.logCounterAttack(currentAttacker.getName());
            } else {
                CombatLogger.logAttack(currentAttacker.getName(), currentDefender.getName());
            }
            currentDefender.retrieveDamage(realDamage);
            if (currentDefender.isAlive()) {
                CombatLogger.logRemainingHealth(currentDefender.getName(), currentDefender.getCurrentHealth());
            } else {
                CombatLogger.logLastUnitDead(currentDefender.getName());
            }

            if (!counterattack) {
                doDeathStare(currentAttacker, currentDefender);
                doThunderbolts(currentAttacker, currentDefender);
                doPetryfing(currentAttacker, currentDefender);
            }
            doCursing(currentAttacker, currentDefender);
        }
    }

    static void doDeathStare(Stack currentAttacker, Stack currentDefender) {
        if (currentAttacker.hasSpeciality(UnitSpeciality.DEATH_STARE)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 10) {
                currentDefender.retrieveDamageToDeath();
                CombatLogger.logDeathStare(currentAttacker.getName(), currentDefender.getName());
            }
        }
    }

    private static void doThunderbolts(Stack currentAttacker, Stack currentDefender) {
        if (currentAttacker.hasSpeciality(UnitSpeciality.THUNDERBOLTS)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 20) {
                currentDefender.retrieveDamage(10);
                CombatLogger.logThunderbolting(currentAttacker.getName(), currentDefender.getName(), currentDefender.getCurrentHealth());
            }
        }
    }


    private static void doPetryfing(Stack currentAttacker, Stack currentDefender) {
        if (currentAttacker.hasSpeciality(UnitSpeciality.PETRYFYING)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 20) {
                currentDefender.petrify();
                CombatLogger.logPetrifying(currentAttacker.getName(), currentDefender.getName());
            }
        }
    }

    private static void doCursing(Stack currentAttacker, Stack currentDefender) {
        if (currentAttacker.hasSpeciality(UnitSpeciality.CURSING)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 20) {
                currentDefender.curse();
                CombatLogger.logCurse(currentAttacker.getName(), currentDefender.getName());
            }
        }
    }

    private static Queue<Stack> determineMoveOrder(Stack attacker, Stack defender) {
        Queue<Stack> units = new LinkedList<>();
        if (attacker.getSpeed() >= defender.getSpeed()) {
            units.offer(attacker);
            units.offer(defender);
        } else {
            units.offer(defender);
            units.offer(attacker);
        }
        return units;
    }
}
