package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.AttackType;
import de.zettsystems.h3comsim.domain.Battlefield;
import de.zettsystems.h3comsim.domain.Hex;
import de.zettsystems.h3comsim.domain.Stack;
import de.zettsystems.h3comsim.domain.UnitSpeciality;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.random.RandomGenerator;

public final class Battle {

    private final RandomGenerator rng;
    private final AutoSolver autoSolver;

    public Battle(RandomGenerator rng) {
        this(rng, new GreedyAutoSolver());
    }

    public Battle(RandomGenerator rng, AutoSolver autoSolver) {
        this.rng = rng;
        this.autoSolver = autoSolver;
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
        Battlefield battlefield = setup.battlefield();
        Deque<Stack> queue = determineMoveOrder(setup.getAttacker(), setup.getDefender());
        for (Stack activeStack : queue) {
            if (activeStack.isAbleToAct() && setup.bothAlive()) {
                Stack opponent = setup.getTarget(activeStack);
                takeAction(activeStack, opponent, battlefield);
                if (activeStack.isAbleToAct() && opponent.isAlive()) {
                    if (activeStack.hasSpeciality(UnitSpeciality.TWO_BLOWS)) {
                        BattleLogger.logTwoBlows(activeStack.getName());
                        takeAction(activeStack, opponent, battlefield);
                    } else if (activeStack.hasGoodMorale(rng)) {
                        BattleLogger.logGoodMorale(activeStack.getName());
                        takeAction(activeStack, opponent, battlefield);
                    }
                }
            }
            BattleLogger.logShortDelimiter();
        }
        queue.forEach(Stack::endTurn);
    }

    private void takeAction(Stack active, Stack opponent, Battlefield battlefield) {
        Action action = autoSolver.decide(active, opponent, battlefield);
        switch (action) {
            case Action.Wait ignored -> BattleLogger.logWait(active.getName());
            case Action.Move move -> moveTo(active, move.destination());
            case Action.MoveAndMelee moveAndMelee -> {
                moveTo(active, moveAndMelee.destination());
                meleeAttack(active, moveAndMelee.target());
            }
            case Action.Melee melee -> meleeAttack(active, melee.target());
            case Action.Shoot shoot -> rangedAttack(active, shoot.target());
        }
    }

    private void moveTo(Stack active, Hex destination) {
        Hex from = active.position();
        BattleLogger.logMove(active.getName(), from.q(), from.r(), destination.q(), destination.r());
        active.setPosition(destination);
    }

    private void meleeAttack(Stack active, Stack passive) {
        dealDamage(active, passive, AttackType.HAND_TO_HAND);
        if (active.hasSpeciality(UnitSpeciality.NO_RETALIATION)) {
            BattleLogger.logImmuneToRetaliation(active.getName());
        } else if (passive.isAbleToAct() && passive.position().isAdjacent(active.position())) {
            BattleLogger.logRetaliation(passive.getName());
            dealDamage(passive, active, AttackType.HAND_TO_HAND);
        }
    }

    private void rangedAttack(Stack active, Stack passive) {
        BattleLogger.logShoot(active.getName(), passive.getName(),
                active.position().distanceTo(passive.position()));
        dealDamage(active, passive, AttackType.LONG_RANGE);
        active.useShot();
    }

    private void dealDamage(Stack active, Stack passive, AttackType attackType) {
        int currentDamage = active.calculateCurrentDamage(attackType, rng);
        int boniMaliPercentage = active.calculateAttackBoniMaliPercentage(passive.getDefense());
        int realDamage = (currentDamage * (100 + boniMaliPercentage)) / 100;

        BattleLogger.logAttack(active.getName(), passive.getName());
        passive.retrieveDamage(realDamage, active.getAttackerSpecialities());

        if (passive.isAlive()) {
            doDeathStare(active, passive);
            doThunderbolts(active, passive);
            doPetrifying(active, passive);
            doCursing(active, passive);
            if (passive.isAlive()) {
                BattleLogger.logRemainingHealth(passive.getName(), passive.getCurrentHealth());
            } else {
                BattleLogger.logLastUnitDead(passive.getName());
            }
        } else {
            BattleLogger.logLastUnitDead(passive.getName());
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

    private static Deque<Stack> determineMoveOrder(Stack attacker, Stack defender) {
        Deque<Stack> units = new ArrayDeque<>(2);
        if (attacker.getSpeed() >= defender.getSpeed()) {
            units.addLast(attacker);
            units.addLast(defender);
        } else {
            units.addLast(defender);
            units.addLast(attacker);
        }
        return units;
    }
}
