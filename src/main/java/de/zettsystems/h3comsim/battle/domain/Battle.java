package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.BattleEvent;
import de.zettsystems.h3comsim.battle.domain.events.EventCollector;
import de.zettsystems.h3comsim.battle.domain.events.HexCoord;
import de.zettsystems.h3comsim.battle.domain.events.NoopEventCollector;
import de.zettsystems.h3comsim.battle.domain.events.Side;
import de.zettsystems.h3comsim.battle.domain.events.StackSnapshot;
import de.zettsystems.h3comsim.battle.domain.events.Winner;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Comparator;
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
        StackSnapshot attackerSnap = snapshot(setup.getAttacker());
        StackSnapshot defenderSnap = snapshot(setup.getDefender());
        List<StackSnapshot> initialStacks = setup.aliveStacks().stream().map(Battle::snapshot).toList();
        events.emit(new BattleEvent.BattleStart(bf.width(), bf.height(), obstacleCoords,
                attackerSnap, defenderSnap, initialStacks));

        int attackerStart = setup.getAttackerCount();
        int defenderStart = setup.getDefenderCount();
        int turn = 0;

        int lastAttackerCount = attackerStart;
        int lastDefenderCount = defenderStart;
        int stalledRounds = 0;
        while (setup.bothAlive() && turn < TURN_CAP && stalledRounds < NO_PROGRESS_LIMIT) {
            doTurn(setup);
            turn++;
            int aCount = setup.getAttackerCount();
            int dCount = setup.getDefenderCount();
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

        List<StackSnapshot> finalStacks = new java.util.ArrayList<>();
        setup.attackerStacks().forEach(s -> finalStacks.add(snapshot(s)));
        setup.defenderStacks().forEach(s -> finalStacks.add(snapshot(s)));
        events.emit(new BattleEvent.BattleEnd(winner,
                setup.getAttackerCount(), setup.getDefenderCount(), turn, finalStacks));
        return new BattleResult(winner,
                attackerStart, setup.getAttackerCount(),
                defenderStart, setup.getDefenderCount(),
                turn);
    }

    private void doTurn(BattleSetup setup) {
        Battlefield battlefield = setup.battlefield();
        autoSolver.planRound(setup);
        Deque<Stack> queue = determineMoveOrder(setup);
        for (Stack activeStack : queue) {
            if (activeStack.isAbleToAct() && setup.bothAlive()) {
                Stack opponent = autoSolver.pickTarget(activeStack, setup.opponentsOf(activeStack), battlefield);
                if (opponent == null) {
                    BattleLogger.logShortDelimiter();
                    continue;
                }
                takeAction(activeStack, opponent, setup);
                if (activeStack.isAbleToAct() && opponent.isAlive() && activeStack.hasGoodMorale(rng)) {
                    BattleLogger.logGoodMorale(activeStack.getName());
                    events.emit(new BattleEvent.GoodMorale(activeStack.side(), activeStack.slot()));
                    Stack moralOpponent = opponent.isAlive() ? opponent
                            : autoSolver.pickTarget(activeStack, setup.opponentsOf(activeStack), battlefield);
                    if (moralOpponent != null) {
                        takeAction(activeStack, moralOpponent, setup);
                    }
                }
            }
            BattleLogger.logShortDelimiter();
        }
        queue.forEach(Stack::endTurn);
    }

    private void takeAction(Stack active, Stack opponent, BattleSetup setup) {
        Battlefield battlefield = setup.battlefield();
        Action action = autoSolver.decide(active, opponent, battlefield);
        switch (action) {
            case Action.Wait() -> {
                BattleLogger.logWait(active.getName());
                events.emit(new BattleEvent.Wait(active.side(), active.slot()));
            }
            case Action.Move(Hex destination) -> moveTo(active, destination, battlefield);
            case Action.MoveAndMelee(Hex destination, Stack target) -> {
                Hex startPos = active.position();
                int hexesMoved = startPos.distanceTo(destination);
                moveTo(active, destination, battlefield);
                meleeAttack(active, target, hexesMoved, setup);
                if (active.hasSpeciality(UnitSpeciality.MOVE_BACK) && active.isAlive()) {
                    BattleLogger.logMoveBack(active.getName(), startPos.q(), startPos.r());
                    Hex returnFrom = active.position();
                    List<HexCoord> backPath = battlefield.findPath(returnFrom, startPos,
                                    active.unit().movement()).stream()
                            .map(h -> new HexCoord(h.q(), h.r())).toList();
                    active.moveTo(startPos);
                    events.emit(new BattleEvent.MoveBack(active.side(), active.slot(),
                            startPos.q(), startPos.r(), backPath));
                }
            }
            case Action.Melee(Stack target) -> meleeAttack(active, target, 0, setup);
            case Action.Shoot(Stack target) -> rangedAttack(active, target, setup);
        }
    }

    private void moveTo(Stack active, Hex destination, Battlefield battlefield) {
        Hex from = active.position();
        List<HexCoord> path = battlefield.findPath(from, destination, active.unit().movement()).stream()
                .map(h -> new HexCoord(h.q(), h.r())).toList();
        BattleLogger.logMove(active.getName(), from.q(), from.r(), destination.q(), destination.r());
        active.moveTo(destination);
        events.emit(new BattleEvent.Move(active.side(), active.slot(),
                from.q(), from.r(), destination.q(), destination.r(), path));
    }

    private void meleeAttack(Stack active, Stack passive, int hexesMoved, BattleSetup setup) {
        int countBeforeFirst = passive.getCount();
        int dealt = dealDamage(active, passive, AttackType.HAND_TO_HAND, hexesMoved, 0, false);
        events.emit(new BattleEvent.Melee(active.side(), active.slot(),
                passive.side(), passive.slot(), hexesMoved,
                dealt, countBeforeFirst - passive.getCount(), snapshot(passive)));
        applyFireShield(active, passive, dealt);
        applyMeleeSplash(active, passive, setup);
        triggerRetaliation(active, passive);
        if (active.hasSpeciality(UnitSpeciality.TWO_BLOWS) && passive.isAlive() && active.isAlive()) {
            BattleLogger.logTwoBlows(active.getName());
            events.emit(new BattleEvent.TwoBlows(active.side(), active.slot()));
            int countBeforeSecond = passive.getCount();
            // Second blow does not gain Jousting-Bonus — kein erneutes Anfahren.
            int dealtSecond = dealDamage(active, passive, AttackType.HAND_TO_HAND, 0, 0, false);
            events.emit(new BattleEvent.Melee(active.side(), active.slot(),
                    passive.side(), passive.slot(), 0,
                    dealtSecond, countBeforeSecond - passive.getCount(), snapshot(passive)));
            applyFireShield(active, passive, dealtSecond);
            triggerRetaliation(active, passive);
        }
    }

    /**
     * Multi-Stack-Splash für Nahkampf: Three-Headed-Attack (Cerberus) trifft bis zu zwei
     * weitere adjazente Gegner-Stacks; Fire-Breath (Green/Gold/Red/Black-Dragon) trifft den
     * Stack auf dem inline-Hex direkt hinter dem Hauptziel. Splash-Hits triggern keine
     * Retaliation und keine After-Attack-Procs — sie sind reine Collateral-Hits.
     */
    private void applyMeleeSplash(Stack active, Stack primary, BattleSetup setup) {
        if (active.hasSpeciality(UnitSpeciality.THREE_HEADED_ATTACK)) {
            int splashes = 0;
            for (Hex neighbor : active.position().neighbors()) {
                if (splashes >= 2) break;
                Stack candidate = findOpponentAt(active, neighbor, setup);
                if (candidate != null && candidate != primary && candidate.isAlive()) {
                    applySplashHit(active, candidate, AttackType.HAND_TO_HAND);
                    splashes++;
                }
            }
        }
        if (active.hasSpeciality(UnitSpeciality.FIRE_BREATH)) {
            Hex behind = behindHex(active.position(), primary.position());
            Stack collateral = findOpponentAt(active, behind, setup);
            if (collateral != null && collateral != primary && collateral.isAlive()) {
                applySplashHit(active, collateral, AttackType.HAND_TO_HAND);
            }
        }
    }

    private static Hex behindHex(Hex from, Hex through) {
        return new Hex(through.q() + (through.q() - from.q()),
                through.r() + (through.r() - from.r()));
    }

    private @Nullable Stack findOpponentAt(Stack active, Hex hex, BattleSetup setup) {
        for (Stack candidate : setup.opponentsOf(active)) {
            if (candidate.position().equals(hex)) {
                return candidate;
            }
        }
        return null;
    }

    private void applySplashHit(Stack active, Stack secondary, AttackType attackType) {
        int currentDamage = active.calculateCurrentDamage(attackType, 0, rng);
        int effectiveDefense = secondary.effectiveDefenseAgainst(active.getAttackerSpecialities());
        int boniMaliPercentage = active.calculateAttackBoniMaliPercentage(effectiveDefense);
        int realDamage = (currentDamage * (100 + boniMaliPercentage)) / 100;
        int countBefore = secondary.getCount();
        BattleLogger.logAttack(active.getName(), secondary.getName());
        secondary.takeDamage(realDamage, active.getAttackerSpecialities());
        int killed = countBefore - secondary.getCount();
        if (attackType == AttackType.HAND_TO_HAND) {
            events.emit(new BattleEvent.Melee(active.side(), active.slot(),
                    secondary.side(), secondary.slot(), 0, realDamage, killed, snapshot(secondary)));
        } else {
            int distance = active.position().distanceTo(secondary.position());
            events.emit(new BattleEvent.Shoot(active.side(), active.slot(),
                    secondary.side(), secondary.slot(), distance, realDamage, killed, snapshot(secondary)));
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
        events.emit(new BattleEvent.FireShield(passive.side(), passive.slot(),
                active.side(), active.slot(), reverse, snapshot(active)));
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
        events.emit(new BattleEvent.Retaliation(passive.side(), passive.slot(),
                active.side(), active.slot(),
                dealt, countBefore - active.getCount(), snapshot(active)));
    }

    private void rangedAttack(Stack active, Stack passive, BattleSetup setup) {
        Battlefield battlefield = setup.battlefield();
        int distance = active.position().distanceTo(passive.position());
        boolean obstacleInLine = PathFinder.hasObstacleInLine(battlefield, active.position(), passive.position());
        BattleLogger.logShoot(active.getName(), passive.getName(), distance);
        int countBefore = passive.getCount();
        int dealt = dealDamage(active, passive, AttackType.LONG_RANGE, 0, distance, obstacleInLine);
        events.emit(new BattleEvent.Shoot(active.side(), active.slot(),
                passive.side(), passive.slot(), distance,
                dealt, countBefore - passive.getCount(), snapshot(passive)));
        applyRangedSplash(active, passive, setup);
        active.useShot();
        if (active.hasSpeciality(UnitSpeciality.TWO_SHOTS) && active.canShoot() && passive.isAlive()) {
            BattleLogger.logTwoShots(active.getName());
            events.emit(new BattleEvent.TwoShots(active.side(), active.slot()));
            int distance2 = active.position().distanceTo(passive.position());
            boolean obstacleInLine2 = PathFinder.hasObstacleInLine(battlefield, active.position(), passive.position());
            BattleLogger.logShoot(active.getName(), passive.getName(), distance2);
            int countBefore2 = passive.getCount();
            int dealt2 = dealDamage(active, passive, AttackType.LONG_RANGE, 0, distance2, obstacleInLine2);
            events.emit(new BattleEvent.Shoot(active.side(), active.slot(),
                    passive.side(), passive.slot(), distance2,
                    dealt2, countBefore2 - passive.getCount(), snapshot(passive)));
            applyRangedSplash(active, passive, setup);
            active.useShot();
        }
    }

    /**
     * Multi-Stack-Splash für Fernkampf: SPLASH_SHOT (Magog) trifft bis zu zwei zusätzliche
     * Gegner-Stacks adjazent zum Hauptziel; DEATH_CLOUD (Lich) trifft alle Gegner-Stacks im
     * 1-Hex-Radius rund ums Hauptziel. Splash-Hits triggern keine Procs.
     */
    private void applyRangedSplash(Stack active, Stack primary, BattleSetup setup) {
        if (active.hasSpeciality(UnitSpeciality.SPLASH_SHOT)) {
            int splashes = 0;
            for (Hex neighbor : primary.position().neighbors()) {
                if (splashes >= 2) break;
                Stack collateral = findOpponentAt(active, neighbor, setup);
                if (collateral != null && collateral != primary && collateral.isAlive()) {
                    applySplashHit(active, collateral, AttackType.LONG_RANGE);
                    splashes++;
                }
            }
        }
        if (active.hasSpeciality(UnitSpeciality.DEATH_CLOUD)) {
            for (Hex neighbor : primary.position().neighbors()) {
                Stack collateral = findOpponentAt(active, neighbor, setup);
                if (collateral != null && collateral != primary && collateral.isAlive()) {
                    applySplashHit(active, collateral, AttackType.LONG_RANGE);
                }
            }
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
            events.emit(new BattleEvent.Rebirth(passive.side(), passive.slot(),
                    passive.getCount(), snapshot(passive)));
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
            events.emit(new BattleEvent.DeathStare(active.side(), active.slot(),
                    target.side(), target.slot(), kills, snapshot(target)));
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
            events.emit(new BattleEvent.Thunderbolts(active.side(), active.slot(),
                    target.side(), target.slot(), damage, snapshot(target)));
        }
    }

    private void doPetrifying(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.PETRYFYING) && rng.nextInt(100) < 20) {
            target.petrify();
            BattleLogger.logPetrifying(active.getName(), target.getName());
            events.emit(new BattleEvent.Petrifying(active.side(), active.slot(),
                    target.side(), target.slot()));
        }
    }

    private void doCursing(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.CURSING) && rng.nextInt(100) < 20) {
            target.curse();
            BattleLogger.logCurse(active.getName(), target.getName());
            events.emit(new BattleEvent.Cursing(active.side(), active.slot(),
                    target.side(), target.slot()));
        }
    }

    private void doPoisoning(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.POISONOUS) && rng.nextInt(100) < 25) {
            target.poison();
            BattleLogger.logPoisoning(active.getName(), target.getName());
            events.emit(new BattleEvent.Poisoning(active.side(), active.slot(),
                    target.side(), target.slot()));
        }
    }

    private void doDiseasing(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.DISEASES) && rng.nextInt(100) < 20) {
            target.disease();
            BattleLogger.logDiseasing(active.getName(), target.getName());
            events.emit(new BattleEvent.Diseasing(active.side(), active.slot(),
                    target.side(), target.slot()));
        }
    }

    private void doAging(Stack active, Stack target) {
        if (active.hasSpeciality(UnitSpeciality.AGING) && rng.nextInt(100) < 20) {
            target.age();
            BattleLogger.logAging(active.getName(), target.getName());
            events.emit(new BattleEvent.Aging(active.side(), active.slot(),
                    target.side(), target.slot()));
        }
    }

    private static StackSnapshot snapshot(Stack stack) {
        return new StackSnapshot(stack.side(), stack.slot(), stack.getName(), stack.getCount(),
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
        return Math.max(0, stack.getCount() - 1) * max + stack.getCurrentHealth();
    }

    /**
     * Move-Order über alle lebenden Stacks beider Seiten. Sortier-Kette:
     * <ol>
     *   <li>Speed absteigend.</li>
     *   <li>Bei Speed-Gleichstand: Attacker vor Defender (H3-Tactical-Phase-Regel).</li>
     *   <li>Bei Speed + Seite gleich: niedrigerer Slot zuerst.</li>
     * </ol>
     * Tote Stacks tauchen nicht in der Queue auf; ein Stack, der während der Runde stirbt,
     * wird über {@link Stack#isAbleToAct()} im Loop übersprungen.
     */
    private static Deque<Stack> determineMoveOrder(BattleSetup setup) {
        Comparator<Stack> order = Comparator
                .comparingInt(Stack::getSpeed).reversed()
                .thenComparingInt((Stack s) -> s.side() == Side.ATTACKER ? 0 : 1)
                .thenComparingInt(Stack::slot);
        List<Stack> alive = setup.aliveStacks();
        alive.sort(order);
        return new ArrayDeque<>(alive);
    }

}
