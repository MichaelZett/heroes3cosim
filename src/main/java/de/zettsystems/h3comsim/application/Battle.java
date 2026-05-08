package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.AttackType;
import de.zettsystems.h3comsim.domain.Battlefield;
import de.zettsystems.h3comsim.domain.Hex;
import de.zettsystems.h3comsim.domain.Stack;
import de.zettsystems.h3comsim.domain.UnitSpeciality;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
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
                if (activeStack.isAbleToAct() && opponent.isAlive() && activeStack.hasGoodMorale(rng)) {
                    BattleLogger.logGoodMorale(activeStack.getName());
                    takeAction(activeStack, opponent, battlefield);
                }
            }
            BattleLogger.logShortDelimiter();
        }
        queue.forEach(Stack::endTurn);
    }

    private void takeAction(Stack active, Stack opponent, Battlefield battlefield) {
        Action action = autoSolver.decide(active, opponent, battlefield);
        switch (action) {
            case Action.Wait() -> BattleLogger.logWait(active.getName());
            case Action.Move(Hex destination) -> moveTo(active, destination);
            case Action.MoveAndMelee(Hex destination, Stack target) -> {
                Hex startPos = active.position();
                int hexesMoved = startPos.distanceTo(destination);
                moveTo(active, destination);
                meleeAttack(active, target, hexesMoved);
                if (active.hasSpeciality(UnitSpeciality.MOVE_BACK) && active.isAlive()) {
                    BattleLogger.logMoveBack(active.getName(), startPos.q(), startPos.r());
                    active.moveTo(startPos);
                }
            }
            case Action.Melee(Stack target) -> meleeAttack(active, target, 0);
            case Action.Shoot(Stack target) -> rangedAttack(active, target);
        }
    }

    private void moveTo(Stack active, Hex destination) {
        Hex from = active.position();
        BattleLogger.logMove(active.getName(), from.q(), from.r(), destination.q(), destination.r());
        active.moveTo(destination);
    }

    private void meleeAttack(Stack active, Stack passive, int hexesMoved) {
        int dealt = dealDamage(active, passive, AttackType.HAND_TO_HAND, hexesMoved);
        applyFireShield(active, passive, dealt);
        triggerRetaliation(active, passive);
        if (active.hasSpeciality(UnitSpeciality.TWO_BLOWS) && passive.isAlive() && active.isAlive()) {
            BattleLogger.logTwoBlows(active.getName());
            // Second blow does not gain Jousting-Bonus — kein erneutes Anfahren.
            int dealtSecond = dealDamage(active, passive, AttackType.HAND_TO_HAND, 0);
            applyFireShield(active, passive, dealtSecond);
            triggerRetaliation(active, passive);
        }
    }

    private void applyFireShield(Stack active, Stack passive, int damageDealt) {
        if (damageDealt <= 0 || !active.isAlive()) {
            return;
        }
        int reverse = passive.fireShieldDamageFor(damageDealt);
        if (reverse <= 0) {
            return;
        }
        BattleLogger.logFireShield(passive.getName(), active.getName(), reverse);
        active.takeDamage(reverse, Set.of());
    }

    private void triggerRetaliation(Stack active, Stack passive) {
        if (active.hasSpeciality(UnitSpeciality.NO_RETALIATION)) {
            BattleLogger.logImmuneToRetaliation(active.getName());
            return;
        }
        if (!passive.isAbleToAct() || !passive.position().isAdjacent(active.position())) {
            return;
        }
        if (!passive.canRetaliate()) {
            return;
        }
        BattleLogger.logRetaliation(passive.getName());
        passive.recordRetaliation();
        // Retaliation kehrt aktiv/passiv bewusst um — passive schlaegt zurueck.
        dealDamage(/* active= */ passive, /* passive= */ active, AttackType.HAND_TO_HAND, 0);
    }

    private void rangedAttack(Stack active, Stack passive) {
        BattleLogger.logShoot(active.getName(), passive.getName(),
                active.position().distanceTo(passive.position()));
        dealDamage(active, passive, AttackType.LONG_RANGE, 0);
        active.useShot();
        if (active.hasSpeciality(UnitSpeciality.TWO_SHOTS) && active.canShoot() && passive.isAlive()) {
            BattleLogger.logTwoShots(active.getName());
            BattleLogger.logShoot(active.getName(), passive.getName(),
                    active.position().distanceTo(passive.position()));
            dealDamage(active, passive, AttackType.LONG_RANGE, 0);
            active.useShot();
        }
    }

    private int dealDamage(Stack active, Stack passive, AttackType attackType, int hexesMoved) {
        int currentDamage = active.calculateCurrentDamage(attackType, hexesMoved, rng);
        int effectiveDefense = passive.effectiveDefenseAgainst(active.getAttackerSpecialities());
        int boniMaliPercentage = active.calculateAttackBoniMaliPercentage(effectiveDefense);
        int realDamage = (currentDamage * (100 + boniMaliPercentage)) / 100;

        BattleLogger.logAttack(active.getName(), passive.getName());
        passive.takeDamage(realDamage, active.getAttackerSpecialities());

        if (passive.isAlive()) {
            doDeathStare(active, passive, attackType);
            doThunderbolts(active, passive, attackType);
            doPetrifying(active, passive);
            doCursing(active, passive);
            doPoisoning(active, passive);
            doDiseasing(active, passive);
            doAging(active, passive);
            if (passive.isAlive()) {
                BattleLogger.logRemainingHealth(passive.getName(), passive.getCurrentHealth());
            } else {
                BattleLogger.logLastUnitDead(passive.getName());
            }
        } else {
            BattleLogger.logLastUnitDead(passive.getName());
        }
        return realDamage;
    }

    private void doDeathStare(Stack active, Stack target, AttackType attackType) {
        // H3: Death Stare triggert nur bei Nahkampf.
        if (attackType != AttackType.HAND_TO_HAND) {
            return;
        }
        if (active.hasSpeciality(UnitSpeciality.DEATH_STARE) && rng.nextInt(100) < 10) {
            int kills = Math.max(1, active.getCount() / 10);
            target.loseTopCreatures(kills);
            BattleLogger.logDeathStare(active.getName(), target.getName(), kills);
        }
    }

    private void doThunderbolts(Stack active, Stack target, AttackType attackType) {
        // H3: Thunderbolts triggern nur bei Nahkampf.
        if (attackType != AttackType.HAND_TO_HAND) {
            return;
        }
        if (!active.hasSpeciality(UnitSpeciality.THUNDERBOLTS)) {
            return;
        }
        int hits = 0;
        int count = active.getCount();
        for (int i = 0; i < count; i++) {
            if (rng.nextInt(100) < 20) {
                hits++;
            }
        }
        if (hits > 0) {
            int damage = hits * 10;
            target.takeDamage(damage, active.getAttackerSpecialities());
            BattleLogger.logThunderbolting(active.getName(), target.getName(), damage, target.getCurrentHealth());
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

    private void doPoisoning(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.POISONOUS) && rng.nextInt(100) < 25) {
            target.poison();
            BattleLogger.logPoisoning(active.getName(), target.getName());
        }
    }

    private void doDiseasing(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.DISEASES) && rng.nextInt(100) < 20) {
            target.disease();
            BattleLogger.logDiseasing(active.getName(), target.getName());
        }
    }

    private void doAging(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.AGING) && rng.nextInt(100) < 20) {
            target.age();
            BattleLogger.logAging(active.getName(), target.getName());
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
