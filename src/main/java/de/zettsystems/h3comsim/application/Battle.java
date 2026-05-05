package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.AttackType;
import de.zettsystems.h3comsim.domain.Stack;
import de.zettsystems.h3comsim.domain.UnitSpeciality;

import java.util.LinkedList;
import java.util.Queue;
import java.util.random.RandomGenerator;

public final class Battle {

    private final RandomGenerator rng;

    public Battle(RandomGenerator rng) {
        this.rng = rng;
    }

    public BattleResult simulate(BattleSetup setup) {
        BattleLogger.logStartOfCombat(setup.getAttackerName(), setup.getAttackerCount(),
                setup.getDefenderName(), setup.getDefenderCount());

        int attackerStart = setup.getAttackerCount();
        int defenderStart = setup.getDefenderCount();
        int turn = 0;

        while (setup.bothAlive()) {
            doTurn(setup);
            turn++;
        }
        BattleLogger.logMiddleDelimiter();

        BattleResult.Side winner;
        if (setup.isAttackerAlive() && !setup.isDefenderAlive()) {
            BattleLogger.logDeath(setup.getDefenderName());
            winner = BattleResult.Side.ATTACKER;
        } else if (!setup.isAttackerAlive() && setup.isDefenderAlive()) {
            BattleLogger.logDeath(setup.getAttackerName());
            winner = BattleResult.Side.DEFENDER;
        } else {
            BattleLogger.logDeath(setup.getAttackerName());
            BattleLogger.logDeath(setup.getDefenderName());
            winner = BattleResult.Side.DRAW;
        }

        return new BattleResult(winner,
                attackerStart, setup.getAttackerCount(),
                defenderStart, setup.getDefenderCount(),
                turn);
    }

    private void doTurn(BattleSetup setup) {
        Queue<Stack> queue = determineMoveOrder(setup.getAttacker(), setup.getDefender());
        for (Stack activeStack : queue) {
            if (activeStack.isAbleToAct()) {
                Stack passiveStack = setup.getTarget(activeStack);
                attack(activeStack, passiveStack);
                if (activeStack.isAbleToAct()) {
                    if (activeStack.hasSpeciality(UnitSpeciality.TWO_BLOWS)) {
                        BattleLogger.logTwoBlows(activeStack.getName());
                        attack(activeStack, passiveStack);
                    } else if (activeStack.hasGoodMorale(rng)) {
                        BattleLogger.logGoodMorale(activeStack.getName());
                        attack(activeStack, passiveStack);
                    }
                }
            }
            BattleLogger.logShortDelimiter();
        }
        queue.forEach(Stack::endTurn);
    }

    private void attack(Stack activeStack, Stack passiveStack) {
        dealDamage(activeStack, passiveStack);
        if (activeStack.hasSpeciality(UnitSpeciality.NO_RETALIATION)) {
            BattleLogger.logImmuneToRetaliation(activeStack.getName());
        } else if (passiveStack.isAbleToAct()) {
            BattleLogger.logRetaliation(passiveStack.getName());
            dealDamage(passiveStack, activeStack);
        }
    }

    private void dealDamage(Stack activeStack, Stack passiveStack) {
        int currentDamage = activeStack.calculateCurrentDamage(AttackType.HAND_TO_HAND, rng);
        int boniMaliPercentage = activeStack.calculateAttackBoniMaliPercentage(passiveStack.getDefense());
        int realDamage = (currentDamage * (100 + boniMaliPercentage)) / 100;

        BattleLogger.logAttack(activeStack.getName(), passiveStack.getName());
        passiveStack.retrieveDamage(realDamage, activeStack.getAttackerSpecialities());

        if (passiveStack.isAlive()) {
            doDeathStare(activeStack, passiveStack);
            doThunderbolts(activeStack, passiveStack);
            doPetrifying(activeStack, passiveStack);
            doCursing(activeStack, passiveStack);
            if (passiveStack.isAlive()) {
                BattleLogger.logRemainingHealth(passiveStack.getName(), passiveStack.getCurrentHealth());
            } else {
                BattleLogger.logLastUnitDead(passiveStack.getName());
            }
        } else {
            BattleLogger.logLastUnitDead(passiveStack.getName());
        }
    }

    private void doDeathStare(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.DEATH_STARE) && rng.nextInt(100) < 10) {
            target.retrieveDamageToDeath();
            BattleLogger.logDeathStare(active.getName(), target.getName());
        }
    }

    private void doThunderbolts(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.THUNDERBOLTS) && rng.nextInt(100) < 20) {
            target.retrieveDamage(10, active.getAttackerSpecialities());
            BattleLogger.logThunderbolting(active.getName(), target.getName(), target.getCurrentHealth());
        }
    }

    private void doPetrifying(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.PETRYFYING) && rng.nextInt(100) < 20) {
            target.petrify();
            BattleLogger.logPetrifying(active.getName(), target.getName());
        }
    }

    private void doCursing(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.CURSING) && rng.nextInt(100) < 20) {
            target.curse();
            BattleLogger.logCurse(active.getName(), target.getName());
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
