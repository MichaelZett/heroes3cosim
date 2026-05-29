package de.zettsystems.h3comsim.battle.domain;

import de.zettsystems.h3comsim.battle.domain.events.Side;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Team-koordinierter Multi-Stack-Solver. Pro Runde wird ein {@link RoundPlan} berechnet, der
 * Schützen-Schutz, Focus-Fire und AoE-Targeting in die Stack-Entscheidungen einbringt.
 *
 * <p>Drei Optimierungen gegenüber {@link GreedyAutoSolver}:
 * <ol>
 *   <li><strong>Tank-Pattern (A)</strong>: bei {@link TeamStance#RANGED_DOMINANT} positioniert
 *       sich jeder eigene Nahkämpfer zwischen dem nächsten Threat und dem bedrohtesten eigenen
 *       Schützen, statt vorzustürmen.</li>
 *   <li><strong>AoE-aware Target-Pick (B)</strong>: Schützen mit
 *       {@link UnitSpeciality#SPLASH_SHOT} / {@link UnitSpeciality#DEATH_CLOUD} bevorzugen
 *       Gegner mit den meisten adjazenten Verbündeten; Nahkämpfer mit
 *       {@link UnitSpeciality#FIRE_BREATH} bevorzugen Inline-Paare.</li>
 *   <li><strong>Focus-Fire (C)</strong>: ohne lokales Engagement zielen alle eigenen Stacks
 *       auf denselben vom Plan gewählten {@code focusTarget} — gefährlichster Gegner gewichtet
 *       nach Specials.</li>
 * </ol>
 *
 * <p>Stateful: speichert den Plan + Setup-Referenz zwischen {@link #planRound(BattleSetup)} und
 * den nachfolgenden {@link #pickTarget}/{@link #decide}-Aufrufen. Pro {@link Battle} eine
 * eigene Instanz verwenden.
 */
public final class StrategicAutoSolver implements AutoSolver {

    private static final double DOMINANCE_THRESHOLD = 1.2;
    private static final int SHOT_HORIZON = 4;

    private final GreedyAutoSolver greedy = new GreedyAutoSolver();

    private RoundPlan plan = RoundPlan.EMPTY;
    private @Nullable BattleSetup currentSetup;

    @Override
    public void planRound(BattleSetup setup) {
        this.currentSetup = setup;
        this.plan = buildPlan(setup);
        // Greedy braucht das Setup für seine Stack-Belegung-Filter (Flieger-Landung,
        // Charge-Adjacent, Kite/Cover), weil Strategic an Greedy delegiert.
        this.greedy.planRound(setup);
    }

    public RoundPlan currentPlan() {
        return plan;
    }

    @Override
    public @Nullable Stack pickTarget(Stack active, List<Stack> opponents, Battlefield battlefield) {
        List<Stack> alive = opponents.stream().filter(Stack::isAlive).toList();
        if (alive.isEmpty()) {
            return null;
        }
        Hex from = active.position();

        // Engagement-Override: ein adjazenter Gegner zwingt zum Nahkampf.
        Stack adjacent = alive.stream()
                .filter(o -> from.distanceTo(o.position()) == 1)
                .min(Comparator.comparingInt(Stack::slot))
                .orElse(null);
        if (adjacent != null) {
            return adjacent;
        }

        // (E) Flieger priorisieren einen ungeschützten gegnerischen Schützen, sofern dieser
        // in Speed-Reichweite ist. Ein Schütze gilt als "ungeschützt", wenn mindestens ein
        // freier passierbarer Adjacent-Hex existiert, der vom aktiven Stack erreicht werden
        // kann. Greift VOR Fire-Breath-/Focus-Fire-Heuristiken, weil das Ausschalten der
        // gegnerischen Distanzwaffe meist mehr Schadens-Output kostet als ein Inline-Hit.
        if (active.unit().movement() == Movement.FLYING) {
            Stack unguarded = pickUnguardedEnemyShooter(active, alive, battlefield);
            if (unguarded != null) {
                return unguarded;
            }
        }

        // (B) AoE-Schützen: Splash/Death-Cloud-Hits maximieren.
        if (active.canShoot()
                && (active.hasSpeciality(UnitSpeciality.SPLASH_SHOT)
                || active.hasSpeciality(UnitSpeciality.DEATH_CLOUD))) {
            Stack aoeBest = pickByAoeHitCount(alive);
            if (aoeBest != null) {
                return aoeBest;
            }
        }

        // (B) Fire-Breath-Nahkämpfer: Inline-Paare bevorzugen.
        if (!active.canShoot() && active.hasSpeciality(UnitSpeciality.FIRE_BREATH)) {
            Stack inlineBest = pickInlineBreathTarget(active, alive);
            if (inlineBest != null) {
                return inlineBest;
            }
        }

        // (C) Focus-Fire: Team-Plan vorgeben lassen.
        Stack focus = plan.focusOf(active.side());
        if (focus != null && focus.isAlive() && alive.contains(focus)) {
            return focus;
        }

        // Fallback: Greedy.
        return greedy.pickTarget(active, opponents, battlefield);
    }

    @Override
    public Action decide(Stack active, Stack opponent, Battlefield battlefield) {
        // (A) Tank-Pattern: positionieren statt chargen, wenn eigene Schützen Schutz brauchen.
        // Aktiviert sich bei RANGED_DOMINANT (alle Schützen schutzwürdig) und bei BALANCED
        // mit Rand-Schützen — siehe RoundPlan.hasTankDuty / StrategicAutoSolver#buildPlan.
        if (plan.hasTankDuty(active.side())
                && isTankCandidate(active)
                && !active.canShoot()
                && active.position().distanceTo(opponent.position()) > 1) {
            // Tank steht schon adjacent zu einem eigenen Schützen → Position halten und
            // Defend (+20 % Defense) statt überflüssig auf einen anderen Adjacent zu wandern.
            // Ein zweiter Tank, der noch im Anflug ist, findet via findTankPosition trotzdem
            // den verbleibenden freien Adjacent.
            if (isAdjacentToOwnProtectedShooter(active)) {
                return new Action.Defend();
            }
            Hex tankSpot = findTankPosition(active, battlefield);
            if (tankSpot != null && !tankSpot.equals(active.position())) {
                return new Action.Move(tankSpot);
            }
        }
        // Flieger umfliegen Tank-Walls: Greedy.decide hat dafür eine eigene findFlyerLanding-
        // Heuristik (greedy.currentSetup wird via planRound-Delegate gesetzt). Strategic
        // delegiert direkt — kein doppelter Flieger-Block hier.
        return greedy.decide(active, opponent, battlefield);
    }

    // ------------------------------------------------------------------ //
    // Plan-Bau
    // ------------------------------------------------------------------ //

    private static RoundPlan buildPlan(BattleSetup setup) {
        Map<Side, Double> rangedPower = new EnumMap<>(Side.class);
        Map<Side, Double> meleePower = new EnumMap<>(Side.class);

        for (Side side : Side.values()) {
            double rp = 0;
            double mp = 0;
            for (Stack s : setup.stacksOf(side)) {
                if (!s.isAlive()) {
                    continue;
                }
                double avgDmg = (s.unit().minDamage() + s.unit().maxDamage()) / 2.0;
                double basePower = avgDmg * s.getCount();
                if (s.canShoot()) {
                    int shots = Math.min(s.shotsRemaining(), SHOT_HORIZON);
                    rp += basePower * shots;
                } else {
                    mp += basePower;
                }
            }
            rangedPower.put(side, rp);
            meleePower.put(side, mp);
        }

        Map<Side, TeamStance> stance = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            Side opp = opposite(side);
            double myR = rangedPower.getOrDefault(side, 0.0);
            double oppR = rangedPower.getOrDefault(opp, 0.0);
            double myM = meleePower.getOrDefault(side, 0.0);
            double oppM = meleePower.getOrDefault(opp, 0.0);
            if (myR > 0 && myR > oppR * DOMINANCE_THRESHOLD) {
                stance.put(side, TeamStance.RANGED_DOMINANT);
            } else if (myM > oppM * DOMINANCE_THRESHOLD) {
                stance.put(side, TeamStance.MELEE_DOMINANT);
            } else {
                stance.put(side, TeamStance.BALANCED);
            }
        }

        Map<Side, Stack> focus = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            Stack best = null;
            double bestScore = -1;
            for (Stack enemy : setup.stacksOf(opposite(side))) {
                if (!enemy.isAlive()) {
                    continue;
                }
                double score = focusScore(enemy);
                if (score > bestScore) {
                    bestScore = score;
                    best = enemy;
                }
            }
            if (best != null) {
                focus.put(side, best);
            }
        }

        // LinkedHashSet → deterministische Iteration im Tank-Pattern: Setup-Reihenfolge =
        // Slot-Reihenfolge ist die einzige Quelle stabiler Reihenfolge (Stack.hashCode() ist
        // Identity-basiert und damit run-spezifisch).
        Set<Stack> protect = new LinkedHashSet<>();
        int lastRow = setup.battlefield().height() - 1;
        for (Side side : Side.values()) {
            TeamStance st = stance.get(side);
            if (st == TeamStance.MELEE_DOMINANT) {
                // Charge-Modus überwiegt — keine Tank-Aufträge, sonst zieht ein Melee Truppen
                // ab, die im Sturm gebraucht werden.
                continue;
            }
            for (Stack s : setup.stacksOf(side)) {
                if (!s.isAlive() || !s.canShoot()) {
                    continue;
                }
                boolean isRangedDominant = st == TeamStance.RANGED_DOMINANT;
                int r = s.position().r();
                boolean atEdge = r == 0 || r == lastRow;
                // RANGED_DOMINANT: alle Schützen werden geschützt (klassisches Tank-Pattern).
                // Sonst (BALANCED): nur strukturell verwundbare Schützen am Rand — sie haben
                // weniger Adjazenz-Hexen, sodass 1–2 Tanks die Front komplett dichtmachen.
                if (isRangedDominant || atEdge) {
                    protect.add(s);
                }
            }
        }

        return new RoundPlan(stance, focus, protect);
    }

    private static double focusScore(Stack enemy) {
        double avgDmg = (enemy.unit().minDamage() + enemy.unit().maxDamage()) / 2.0;
        double threat = avgDmg * enemy.getCount();
        double abilityWeight = 0;
        if (enemy.hasSpeciality(UnitSpeciality.NO_RETALIATION)) {
            abilityWeight += 30;
        }
        if (enemy.hasSpeciality(UnitSpeciality.DEATH_STARE)
                || enemy.hasSpeciality(UnitSpeciality.THUNDERBOLTS)
                || enemy.hasSpeciality(UnitSpeciality.PETRYFYING)) {
            abilityWeight += 25;
        }
        if (enemy.hasSpeciality(UnitSpeciality.DEATH_CLOUD)
                || enemy.hasSpeciality(UnitSpeciality.SPLASH_SHOT)
                || enemy.hasSpeciality(UnitSpeciality.FIRE_BREATH)
                || enemy.hasSpeciality(UnitSpeciality.THREE_HEADED_ATTACK)) {
            abilityWeight += 20;
        }
        if (enemy.hasSpeciality(UnitSpeciality.MOVE_BACK)
                || enemy.hasSpeciality(UnitSpeciality.TWO_SHOTS)) {
            abilityWeight += 15;
        }
        return threat + abilityWeight;
    }

    // ------------------------------------------------------------------ //
    // Target-Pick-Helpers
    // ------------------------------------------------------------------ //

    private static @Nullable Stack pickByAoeHitCount(List<Stack> aliveEnemies) {
        Stack best = null;
        int bestScore = 0;
        int bestDanger = -1;
        for (Stack target : aliveEnemies) {
            int score = 0;
            for (Stack other : aliveEnemies) {
                if (other == target) {
                    continue;
                }
                if (target.position().distanceTo(other.position()) == 1) {
                    score++;
                }
            }
            int danger = dangerScore(target);
            if (score > bestScore || (score == bestScore && danger > bestDanger)) {
                bestScore = score;
                bestDanger = danger;
                best = target;
            }
        }
        return bestScore > 0 ? best : null;
    }

    private static @Nullable Stack pickInlineBreathTarget(Stack active, List<Stack> aliveEnemies) {
        Hex from = active.position();
        Stack best = null;
        int bestSecondaryDanger = -1;
        for (Stack target : aliveEnemies) {
            Hex behind = behindHex(from, target.position());
            for (Stack other : aliveEnemies) {
                if (other == target) {
                    continue;
                }
                if (other.position().equals(behind)) {
                    int sec = dangerScore(other);
                    if (sec > bestSecondaryDanger) {
                        bestSecondaryDanger = sec;
                        best = target;
                    }
                    break;
                }
            }
        }
        return best;
    }

    private static int dangerScore(Stack s) {
        int avgDmg = (s.unit().minDamage() + s.unit().maxDamage()) / 2;
        return avgDmg * s.getCount();
    }

    private static Hex behindHex(Hex from, Hex through) {
        return new Hex(through.q() + (through.q() - from.q()),
                through.r() + (through.r() - from.r()));
    }

    // ------------------------------------------------------------------ //
    // Tank-Pattern
    // ------------------------------------------------------------------ //

    /**
     * Nur „langsame, schwache" Stacks dürfen Tank-Babysitter sein — High-Damage- oder
     * High-Mobility-Attacker (Black Dragon, Champion, Vampire Lord, Cerberus etc.)
     * sollen chargen, nicht vor Schützen kleben. Heuristik: Tier ≤ 5 UND keine
     * aggressiven Specials. Schützen werden ohnehin nicht durch diese Methode geprüft —
     * sie bekommen via {@link #decide} keinen Tank-Pattern-Pfad.
     */
    private static boolean isTankCandidate(Stack stack) {
        Unit u = stack.unit();
        if (u.hasSpeciality(UnitSpeciality.MOVE_BACK)) return false;
        if (u.hasSpeciality(UnitSpeciality.IMPACT_DAMAGE)) return false;
        if (u.hasSpeciality(UnitSpeciality.FIRE_BREATH)) return false;
        if (u.hasSpeciality(UnitSpeciality.THREE_HEADED_ATTACK)) return false;
        if (u.hasSpeciality(UnitSpeciality.TELEPORT_NO_COST)) return false;
        if (u.hasSpeciality(UnitSpeciality.LIFE_DRAIN)) return false;
        if (u.hasSpeciality(UnitSpeciality.NO_RETALIATION)) return false;
        return u.tier() <= 5;
    }

    private boolean isAdjacentToOwnProtectedShooter(Stack active) {
        Side mySide = active.side();
        Hex from = active.position();
        for (Stack shooter : plan.protectedShooters()) {
            if (shooter.side() != mySide || !shooter.isAlive()) {
                continue;
            }
            if (from.distanceTo(shooter.position()) == 1) {
                return true;
            }
        }
        return false;
    }

    private @Nullable Hex findTankPosition(Stack active, Battlefield battlefield) {
        BattleSetup setup = currentSetup;
        if (setup == null) {
            return null;
        }
        Side mySide = active.side();
        // Slot-sortiert iterieren — RoundPlan.protectedShooters ist nach Set.copyOf nicht mehr
        // stabil, wir brauchen aber deterministische Wahl bei mehreren gleich-bedrohten Schützen.
        List<Stack> myShooters = plan.protectedShooters().stream()
                .filter(s -> s.side() == mySide && s.isAlive())
                .sorted(Comparator.comparingInt(Stack::slot))
                .toList();
        if (myShooters.isEmpty()) {
            return null;
        }
        List<Stack> enemies = setup.opponentsOf(active);
        if (enemies.isEmpty()) {
            return null;
        }
        Stack mostThreatened = null;
        Stack closestThreat = null;
        int minDist = Integer.MAX_VALUE;
        for (Stack shooter : myShooters) {
            for (Stack enemy : enemies) {
                int d = shooter.position().distanceTo(enemy.position());
                if (d < minDist) {
                    minDist = d;
                    mostThreatened = shooter;
                    closestThreat = enemy;
                }
            }
        }
        if (mostThreatened == null || closestThreat == null) {
            return null;
        }

        Hex shooterPos = mostThreatened.position();
        Hex threatPos = closestThreat.position();
        Hex preferred = adjacentTowards(shooterPos, threatPos);
        if (isReachableLandingSpot(active, preferred, battlefield, setup)) {
            return preferred;
        }
        for (Hex neighbor : shooterPos.neighbors()) {
            if (isReachableLandingSpot(active, neighbor, battlefield, setup)) {
                return neighbor;
            }
        }
        return null;
    }

    /**
     * Findet unter den lebenden Gegnern den gefährlichsten Schützen, der noch mindestens
     * einen freien und vom Aktiven erreichbaren Adjacent-Hex hat. Liefert {@code null},
     * wenn alle gegnerischen Schützen entweder ohne Adjacent-Reichweite oder durch eine
     * vollständige Tank-Wall abgedeckt sind — dann fällt {@link #pickTarget} zurück auf
     * die normale Focus-/Greedy-Auswahl.
     */
    private @Nullable Stack pickUnguardedEnemyShooter(Stack active, List<Stack> aliveEnemies,
                                                       Battlefield bf) {
        BattleSetup setup = currentSetup;
        if (setup == null) {
            return null;
        }
        Hex from = active.position();
        int speed = active.unit().speed();
        Stack best = null;
        int bestThreat = -1;
        for (Stack candidate : aliveEnemies) {
            if (!candidate.canShoot()) {
                continue;
            }
            if (!hasReachableFreeAdjacent(candidate, from, speed, bf, active, setup)) {
                continue;
            }
            int threat = (candidate.unit().minDamage() + candidate.unit().maxDamage())
                    / 2 * candidate.getCount();
            if (threat > bestThreat) {
                bestThreat = threat;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean hasReachableFreeAdjacent(Stack target, Hex from, int speed,
                                                    Battlefield bf, Stack mover, BattleSetup setup) {
        for (Hex adj : target.position().neighbors()) {
            if (!bf.isPassable(adj)) {
                continue;
            }
            if (from.distanceTo(adj) > speed) {
                continue;
            }
            if (Objects.equals(adj, mover.position())) {
                continue;
            }
            if (isHexOccupiedByOther(adj, mover, setup)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean isHexOccupiedByOther(Hex hex, Stack mover, BattleSetup setup) {
        for (Stack s : setup.attackerStacks()) {
            if (s != mover && s.isAlive() && s.position().equals(hex)) {
                return true;
            }
        }
        for (Stack s : setup.defenderStacks()) {
            if (s != mover && s.isAlive() && s.position().equals(hex)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isReachableLandingSpot(Stack active, Hex hex, Battlefield bf,
                                                  BattleSetup setup) {
        if (hex.equals(active.position())) {
            return false;
        }
        if (!bf.isPassable(hex)) {
            return false;
        }
        // TELEPORT_NO_COST (Devil/Arch Devil): Speed-Schranke entfällt.
        if (!active.hasSpeciality(UnitSpeciality.TELEPORT_NO_COST)
                && active.position().distanceTo(hex) > active.unit().speed()) {
            return false;
        }
        for (Stack other : setup.attackerStacks()) {
            if (other != active && other.isAlive() && other.position().equals(hex)) {
                return false;
            }
        }
        for (Stack other : setup.defenderStacks()) {
            if (other != active && other.isAlive() && other.position().equals(hex)) {
                return false;
            }
        }
        List<Hex> path = bf.findPath(active.position(), hex, active.unit().movement());
        return !path.isEmpty() && path.size() <= active.unit().speed();
    }

    private static Hex adjacentTowards(Hex from, Hex to) {
        Hex best = from;
        int bestDist = Integer.MAX_VALUE;
        for (Hex n : from.neighbors()) {
            int d = n.distanceTo(to);
            if (d < bestDist) {
                bestDist = d;
                best = n;
            }
        }
        return best;
    }

    private static Side opposite(Side side) {
        return side == Side.ATTACKER ? Side.DEFENDER : Side.ATTACKER;
    }
}
