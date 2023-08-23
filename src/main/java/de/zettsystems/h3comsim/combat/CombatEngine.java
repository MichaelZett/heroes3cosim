package de.zettsystems.h3comsim.combat;

import de.zettsystems.h3comsim.unit.common.Stack;
import de.zettsystems.h3comsim.unit.common.UnitSpeciality;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

public final class CombatEngine {

    private CombatEngine() {
        //not intended
    }

    public static void solveCombat(CombatSetup arena) {
        CombatLogger.logStartOfCombat(arena.getAttackerName(), arena.getAttackerCount(), arena.getDefenderName(), arena.getDefenderCount());

        while (arena.bothAlive()) {
            doTurn(arena);
        }
        CombatLogger.logMiddleDelimiter();

        if (arena.isAttackerAlive()) {
            CombatLogger.logDeath(arena.getDefenderName());
        } else {
            CombatLogger.logDeath(arena.getAttackerName());
        }
    }

    private static void doTurn(CombatSetup arena) {
        Queue<Stack> queue = determineMoveOrder(arena.getAttacker(), arena.getDefender());
        for (Stack activeStack : queue) {
            if (activeStack.isAbleToAct()) {
                Stack passiveStack = arena.getTarget(activeStack);
                attack(activeStack, passiveStack);
            }
            CombatLogger.logShortDelimiter();
        }
        queue.forEach(Stack::endTurn);
    }

    private static boolean retaliationPossible(Stack activeStack) {
        return !activeStack.hasSpeciality(UnitSpeciality.NO_RETALIATION);
    }

    private static void attack(Stack activeStack, Stack passiveStack) {
        dealDamage(activeStack, passiveStack);
        //check whether counterattack takes place
        if (!retaliationPossible(activeStack)) {
            CombatLogger.logImmuneToRetaliation(activeStack.getName());
        } else if (passiveStack.isAbleToAct()) {
            CombatLogger.logRetaliation(passiveStack.getName());
            dealDamage(passiveStack, activeStack);
        }
    }

    private static void dealDamage(Stack activeStack, Stack passiveStack) {
        int currentDamage = activeStack.calculateCurrentDamage();
        int boniMaliPercentage = activeStack.calculateAttackBoniMaliPercentage(passiveStack.getDefense());
        int realDamage = (currentDamage * (100 + boniMaliPercentage)) / 100;

        CombatLogger.logAttack(activeStack.getName(), passiveStack.getName());
        passiveStack.retrieveDamage(realDamage, activeStack.getAttackerSpecialities());

        if (passiveStack.isAlive()) {
            doDeathStare(activeStack, passiveStack);
            doThunderbolts(activeStack, passiveStack);
            doPetrifying(activeStack, passiveStack);
            doCursing(activeStack, passiveStack);
            if (passiveStack.isAlive()) {
                CombatLogger.logRemainingHealth(passiveStack.getName(), passiveStack.getCurrentHealth());
            } else {
                CombatLogger.logLastUnitDead(passiveStack.getName());
            }
        } else {
            CombatLogger.logLastUnitDead(passiveStack.getName());
        }
    }

    static void doDeathStare(Stack activeStack, Stack currentDefender) {
        if (activeStack.hasSpeciality(UnitSpeciality.DEATH_STARE)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 10) {
                currentDefender.retrieveDamageToDeath();
                CombatLogger.logDeathStare(activeStack.getName(), currentDefender.getName());
            }
        }
    }

    private static void doThunderbolts(Stack activeStack, Stack currentDefender) {
        if (activeStack.hasSpeciality(UnitSpeciality.THUNDERBOLTS)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 20) {
                currentDefender.retrieveDamage(10, activeStack.getAttackerSpecialities());
                CombatLogger.logThunderbolting(activeStack.getName(), currentDefender.getName(), currentDefender.getCurrentHealth());
            }
        }
    }

    private static void doPetrifying(Stack activeStack, Stack currentDefender) {
        if (activeStack.hasSpeciality(UnitSpeciality.PETRYFYING)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 20) {
                currentDefender.petrify();
                CombatLogger.logPetrifying(activeStack.getName(), currentDefender.getName());
            }
        }
    }

    private static void doCursing(Stack activeStack, Stack currentDefender) {
        if (activeStack.hasSpeciality(UnitSpeciality.CURSING)) {
            int value = ThreadLocalRandom.current().nextInt(1, 101);
            if (value <= 20) {
                currentDefender.curse();
                CombatLogger.logCurse(activeStack.getName(), currentDefender.getName());
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
