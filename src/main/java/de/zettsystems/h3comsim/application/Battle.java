package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.AttackType;
import de.zettsystems.h3comsim.domain.Battlefield;
import de.zettsystems.h3comsim.domain.Hex;
import de.zettsystems.h3comsim.domain.PathFinder;
import de.zettsystems.h3comsim.domain.Stack;
import de.zettsystems.h3comsim.domain.UnitSpeciality;
import de.zettsystems.h3comsim.domain.events.BattleEvent;
import de.zettsystems.h3comsim.domain.events.EventCollector;
import de.zettsystems.h3comsim.domain.events.HexCoord;
import de.zettsystems.h3comsim.domain.events.NoopEventCollector;
import de.zettsystems.h3comsim.domain.events.StackSnapshot;
import de.zettsystems.h3comsim.domain.events.Winner;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

public final class Battle {

    /**
     * Harte Obergrenze gegen pathologische Endlos-Schleifen.
     */
    private static final int TURN_CAP = 200;
    /**
     * Sicherheitsabbruch, wenn so viele aufeinanderfolgende Runden ohne Verluste auf beiden Seiten
     * vergehen. Trifft typische Kite-Patterns, in denen ein Schütze ohne Schüsse aus dem Stand
     * läuft, ohne dass jemand stirbt.
     */
    private static final int NO_PROGRESS_LIMIT = 20;

    private final RandomGenerator rng;
    private final AutoSolver autoSolver;
    private final EventCollector events;

    public Battle(RandomGenerator rng) {
        this(rng, new GreedyAutoSolver(), NoopEventCollector.INSTANCE);
    }

    public Battle(RandomGenerator rng, AutoSolver autoSolver) {
        this(rng, autoSolver, NoopEventCollector.INSTANCE);
    }

    public Battle(RandomGenerator rng, AutoSolver autoSolver, EventCollector events) {
        this.rng = rng;
        this.autoSolver = autoSolver;
        this.events = events;
    }

    public BattleResult simulate(BattleSetup setup) {
        BattleLogger.logStartOfCombat(setup.getAttackerName(), setup.getAttackerCount(),
                setup.getDefenderName(), setup.getDefenderCount());
        Battlefield bf = setup.battlefield();
        List<HexCoord> obstacleCoords = bf.obstacles().stream()
                .map(h -> new HexCoord(h.q(), h.r()))
                .toList();
        events.emit(new BattleEvent.BattleStart(bf.width(), bf.height(), obstacleCoords,
                snapshot(setup.getAttacker()), snapshot(setup.getDefender())));

        int attackerStart = setup.getAttackerCount();
        int defenderStart = setup.getDefenderCount();
        int turn = 0;

        int lastAttackerCount = setup.getAttacker().getCount();
        int lastDefenderCount = setup.getDefender().getCount();
        int stalledRounds = 0;
        while (setup.bothAlive() && turn < TURN_CAP && stalledRounds < NO_PROGRESS_LIMIT) {
            doTurn(setup);
            turn++;
            int aCount = setup.getAttacker().getCount();
            int dCount = setup.getDefender().getCount();
            if (aCount == lastAttackerCount && dCount == lastDefenderCount) {
                stalledRounds++;
            } else {
                stalledRounds = 0;
                lastAttackerCount = aCount;
                lastDefenderCount = dCount;
            }
        }
        BattleLogger.logMiddleDelimiter();

        Winner winner = determineWinner(setup);

        events.emit(new BattleEvent.BattleEnd(winner,
                setup.getAttackerCount(), setup.getDefenderCount(), turn));
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
                    events.emit(new BattleEvent.GoodMorale(activeStack.side()));
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
            case Action.Wait() -> {
                BattleLogger.logWait(active.getName());
                events.emit(new BattleEvent.Wait(active.side()));
            }
            case Action.Move(Hex destination) -> moveTo(active, destination, battlefield);
            case Action.MoveAndMelee(Hex destination, Stack target) -> {
                Hex startPos = active.position();
                int hexesMoved = startPos.distanceTo(destination);
                moveTo(active, destination, battlefield);
                meleeAttack(active, target, hexesMoved);
                if (active.hasSpeciality(UnitSpeciality.MOVE_BACK) && active.isAlive()) {
                    BattleLogger.logMoveBack(active.getName(), startPos.q(), startPos.r());
                    Hex returnFrom = active.position();
                    List<HexCoord> backPath = battlefield.findPath(returnFrom, startPos,
                                    active.unit().movement()).stream()
                            .map(h -> new HexCoord(h.q(), h.r())).toList();
                    active.moveTo(startPos);
                    events.emit(new BattleEvent.MoveBack(active.side(),
                            startPos.q(), startPos.r(), backPath));
                }
            }
            case Action.Melee(Stack target) -> meleeAttack(active, target, 0);
            case Action.Shoot(Stack target) -> rangedAttack(active, target, battlefield);
        }
    }

    private void moveTo(Stack active, Hex destination, Battlefield battlefield) {
        Hex from = active.position();
        List<HexCoord> path = battlefield.findPath(from, destination, active.unit().movement()).stream()
                .map(h -> new HexCoord(h.q(), h.r())).toList();
        BattleLogger.logMove(active.getName(), from.q(), from.r(), destination.q(), destination.r());
        active.moveTo(destination);
        events.emit(new BattleEvent.Move(active.side(),
                from.q(), from.r(), destination.q(), destination.r(), path));
    }

    private void meleeAttack(Stack active, Stack passive, int hexesMoved) {
        int countBeforeFirst = passive.getCount();
        int dealt = dealDamage(active, passive, AttackType.HAND_TO_HAND, hexesMoved, 0, false);
        events.emit(new BattleEvent.Melee(active.side(), passive.side(), hexesMoved,
                dealt, countBeforeFirst - passive.getCount(), snapshot(passive)));
        applyFireShield(active, passive, dealt);
        triggerRetaliation(active, passive);
        if (active.hasSpeciality(UnitSpeciality.TWO_BLOWS) && passive.isAlive() && active.isAlive()) {
            BattleLogger.logTwoBlows(active.getName());
            events.emit(new BattleEvent.TwoBlows(active.side()));
            int countBeforeSecond = passive.getCount();
            // Second blow does not gain Jousting-Bonus — kein erneutes Anfahren.
            int dealtSecond = dealDamage(active, passive, AttackType.HAND_TO_HAND, 0, 0, false);
            events.emit(new BattleEvent.Melee(active.side(), passive.side(), 0,
                    dealtSecond, countBeforeSecond - passive.getCount(), snapshot(passive)));
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
        events.emit(new BattleEvent.FireShield(passive.side(), active.side(), reverse, snapshot(active)));
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
        int countBefore = active.getCount();
        // Retaliation kehrt aktiv/passiv bewusst um — passive schlaegt zurueck.
        int dealt = dealDamage(/* active= */ passive, /* passive= */ active, AttackType.HAND_TO_HAND, 0, 0, false);
        events.emit(new BattleEvent.Retaliation(passive.side(), active.side(),
                dealt, countBefore - active.getCount(), snapshot(active)));
    }

    private void rangedAttack(Stack active, Stack passive, Battlefield battlefield) {
        int distance = active.position().distanceTo(passive.position());
        boolean obstacleInLine = PathFinder.hasObstacleInLine(battlefield, active.position(), passive.position());
        BattleLogger.logShoot(active.getName(), passive.getName(), distance);
        int countBefore = passive.getCount();
        int dealt = dealDamage(active, passive, AttackType.LONG_RANGE, 0, distance, obstacleInLine);
        events.emit(new BattleEvent.Shoot(active.side(), passive.side(), distance,
                dealt, countBefore - passive.getCount(), snapshot(passive)));
        active.useShot();
        if (active.hasSpeciality(UnitSpeciality.TWO_SHOTS) && active.canShoot() && passive.isAlive()) {
            BattleLogger.logTwoShots(active.getName());
            events.emit(new BattleEvent.TwoShots(active.side()));
            int distance2 = active.position().distanceTo(passive.position());
            boolean obstacleInLine2 = PathFinder.hasObstacleInLine(battlefield, active.position(), passive.position());
            BattleLogger.logShoot(active.getName(), passive.getName(), distance2);
            int countBefore2 = passive.getCount();
            int dealt2 = dealDamage(active, passive, AttackType.LONG_RANGE, 0, distance2, obstacleInLine2);
            events.emit(new BattleEvent.Shoot(active.side(), passive.side(), distance2,
                    dealt2, countBefore2 - passive.getCount(), snapshot(passive)));
            active.useShot();
        }
    }

    private int dealDamage(Stack active, Stack passive, AttackType attackType,
                           int hexesMoved, int distance, boolean obstacleInLine) {
        int currentDamage = active.calculateCurrentDamage(attackType, hexesMoved, rng);
        if (attackType == AttackType.LONG_RANGE) {
            Set<UnitSpeciality> attackerSpecs = active.getAttackerSpecialities();
            if (distance > 10 && !attackerSpecs.contains(UnitSpeciality.NO_DISTANCE_PENALTY)) {
                currentDamage = currentDamage / 2;
                BattleLogger.logDistancePenalty(active.getName(), distance);
            }
            if (obstacleInLine && !attackerSpecs.contains(UnitSpeciality.NO_OBSTACLE_PENALTY)) {
                currentDamage = currentDamage / 2;
                BattleLogger.logObstaclePenalty(active.getName());
            }
        }
        int effectiveDefense = passive.effectiveDefenseAgainst(active.getAttackerSpecialities());
        int boniMaliPercentage = active.calculateAttackBoniMaliPercentage(effectiveDefense);
        int realDamage = (currentDamage * (100 + boniMaliPercentage)) / 100;

        BattleLogger.logAttack(active.getName(), passive.getName());
        boolean rebirthUsedBefore = passive.isRebirthUsed();
        passive.takeDamage(realDamage, active.getAttackerSpecialities());
        emitRebirthIfFired(passive, rebirthUsedBefore);

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

    private void emitRebirthIfFired(Stack passive, boolean rebirthUsedBefore) {
        if (!rebirthUsedBefore && passive.isRebirthUsed()) {
            BattleLogger.logRebirth(passive.getName(), passive.getCount());
            events.emit(new BattleEvent.Rebirth(passive.side(), passive.getCount(), snapshot(passive)));
        }
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
            events.emit(new BattleEvent.DeathStare(active.side(), target.side(), kills, snapshot(target)));
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
            events.emit(new BattleEvent.Thunderbolts(active.side(), target.side(), damage, snapshot(target)));
        }
    }

    private void doPetrifying(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.PETRYFYING) && rng.nextInt(100) < 20) {
            target.petrify();
            BattleLogger.logPetrifying(active.getName(), target.getName());
            events.emit(new BattleEvent.Petrifying(active.side(), target.side()));
        }
    }

    private void doCursing(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.CURSING) && rng.nextInt(100) < 20) {
            target.curse();
            BattleLogger.logCurse(active.getName(), target.getName());
            events.emit(new BattleEvent.Cursing(active.side(), target.side()));
        }
    }

    private void doPoisoning(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.POISONOUS) && rng.nextInt(100) < 25) {
            target.poison();
            BattleLogger.logPoisoning(active.getName(), target.getName());
            events.emit(new BattleEvent.Poisoning(active.side(), target.side()));
        }
    }

    private void doDiseasing(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.DISEASES) && rng.nextInt(100) < 20) {
            target.disease();
            BattleLogger.logDiseasing(active.getName(), target.getName());
            events.emit(new BattleEvent.Diseasing(active.side(), target.side()));
        }
    }

    private void doAging(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.AGING) && rng.nextInt(100) < 20) {
            target.age();
            BattleLogger.logAging(active.getName(), target.getName());
            events.emit(new BattleEvent.Aging(active.side(), target.side()));
        }
    }

    private static StackSnapshot snapshot(Stack stack) {
        return new StackSnapshot(stack.side(), stack.getName(), stack.getCount(),
                stack.getCurrentHealth(), stack.position().q(), stack.position().r());
    }

    private static Winner determineWinner(BattleSetup setup) {
        boolean attackerAlive = setup.isAttackerAlive();
        boolean defenderAlive = setup.isDefenderAlive();
        if (attackerAlive && !defenderAlive) {
            BattleLogger.logDeath(setup.getDefenderName());
            return Winner.ATTACKER;
        }
        if (!attackerAlive && defenderAlive) {
            BattleLogger.logDeath(setup.getAttackerName());
            return Winner.DEFENDER;
        }
        if (!attackerAlive) {
            BattleLogger.logDeath(setup.getAttackerName());
            BattleLogger.logDeath(setup.getDefenderName());
            return Winner.DRAW;
        }
        // Beide noch lebendig — Turn-Cap oder Stall. Sieger über Gesamt-HP.
        int aHp = totalHp(setup.getAttacker());
        int dHp = totalHp(setup.getDefender());
        if (aHp > dHp) {
            return Winner.ATTACKER;
        }
        if (dHp > aHp) {
            return Winner.DEFENDER;
        }
        return Winner.DRAW;
    }

    private static int totalHp(Stack stack) {
        int max = stack.unit().health();
        return Math.max(0, (stack.getCount() - 1)) * max + stack.getCurrentHealth();
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
