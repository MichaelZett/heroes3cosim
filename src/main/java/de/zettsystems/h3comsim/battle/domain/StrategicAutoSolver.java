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
    /** Spiegelt den Zwei-Kollateral-Deckel aus {@code Battle.applySplashShot}. */
    private static final int SPLASH_SHOT_COLLATERAL_CAP = 2;

    private final GreedyAutoSolver greedy;

    public StrategicAutoSolver() {
        this(true);
    }

    /** @see GreedyAutoSolver#GreedyAutoSolver(boolean) */
    public StrategicAutoSolver(boolean tacticalWait) {
        this.greedy = new GreedyAutoSolver(tacticalWait);
    }

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

        Stack chosen = pickBySpeciality(active, alive, battlefield);
        if (chosen == null) {
            // (C) Focus-Fire: Team-Plan vorgeben lassen, sonst Greedy.
            Stack focus = plan.focusOf(active.side());
            chosen = focus != null && focus.isAlive() && alive.contains(focus)
                    ? focus
                    : greedy.pickTarget(active, opponents, battlefield);
        }

        // (F) Friendly-Fire-Veto: gilt für JEDEN Pfad oben, nicht nur für die AoE-Heuristik.
        return avoidSelfSplash(active, chosen, alive);
    }

    /**
     * Letzte Instanz vor der Zielabgabe: verhindert, dass ein AoE-Schütze auf ein Ziel feuert,
     * dessen Splash-Radius eigene Stacks enthält, obwohl ein sauberes Ziel verfügbar wäre.
     *
     * <p>Nötig, weil {@link #pickByAoeHitCount} nur greift, solange irgendein Ziel einen
     * positiven Netto-Splash hat. Sonst übernimmt Focus-Fire bzw. Greedy — und beide kennen
     * Friendly Fire nicht. Genau dieser Pfad war der größere Anteil des gemessenen
     * Inferno-Eigenbeschusses.
     *
     * <p>Bewusst als Veto und nicht als Gewichtung formuliert: das Team-Ziel bleibt gesetzt,
     * solange es kein Eigentor ist. Nur wenn es eines wäre und eine treffergleiche saubere
     * Alternative existiert, wird umgeschwenkt.
     *
     * @return das ursprüngliche Ziel, oder das beste eigenbeschussfreie Alternativziel
     */
    private @Nullable Stack avoidSelfSplash(Stack active, @Nullable Stack chosen,
                                            List<Stack> alive) {
        if (chosen == null || !active.canShoot()) {
            return chosen;
        }
        boolean deathCloud = active.hasSpeciality(UnitSpeciality.DEATH_CLOUD);
        if (!deathCloud && !active.hasSpeciality(UnitSpeciality.SPLASH_SHOT)) {
            return chosen;
        }
        BattleSetup setup = currentSetup;
        List<Stack> aliveAll = setup == null ? alive : setup.aliveStacks();
        if (tallySplash(active, chosen, aliveAll, deathCloud).ownHits() == 0) {
            return chosen;
        }
        Stack cleanest = null;
        int bestEnemyHits = -1;
        int bestDanger = -1;
        for (Stack candidate : alive) {
            SplashTally tally = tallySplash(active, candidate, aliveAll, deathCloud);
            if (tally.ownHits() > 0) {
                continue;
            }
            int danger = dangerScore(candidate);
            if (tally.enemyHits() > bestEnemyHits
                    || (tally.enemyHits() == bestEnemyHits && danger > bestDanger)) {
                bestEnemyHits = tally.enemyHits();
                bestDanger = danger;
                cleanest = candidate;
            }
        }
        return cleanest != null ? cleanest : chosen;
    }

    /**
     * Speciality-getriebene Ziel-Wahl, die dem Focus-Fire vorgeht. Liefert {@code null},
     * wenn keine Speciality greift oder die jeweilige Heuristik kein Ziel findet — dann
     * entscheidet der Team-Plan.
     */
    private @Nullable Stack pickBySpeciality(Stack active, List<Stack> alive, Battlefield battlefield) {
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

        // (B) AoE-Schützen: Netto-Splash maximieren (Gegner-Treffer minus Eigenbeschuss).
        if (active.canShoot()
                && (active.hasSpeciality(UnitSpeciality.SPLASH_SHOT)
                || active.hasSpeciality(UnitSpeciality.DEATH_CLOUD))) {
            BattleSetup setup = currentSetup;
            return pickByAoeHitCount(active, alive,
                    setup == null ? alive : setup.aliveStacks());
        }

        // (B) Fire-Breath-Nahkämpfer: Inline-Paare bevorzugen.
        if (!active.canShoot() && active.hasSpeciality(UnitSpeciality.FIRE_BREATH)) {
            return pickInlineBreathTarget(active, alive);
        }
        return null;
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
        Map<Side, TeamStance> stance = stances(setup);
        return new RoundPlan(stance, focusTargets(setup), protectedShooters(setup, stance));
    }

    /**
     * Aggregierte Feuerkraft einer Seite, getrennt nach Fernkampf und Nahkampf.
     */
    private record SidePower(double ranged, double melee) {
    }

    private static SidePower powerOf(BattleSetup setup, Side side) {
        double ranged = 0;
        double melee = 0;
        for (Stack s : setup.stacksOf(side)) {
            if (!s.isAlive()) {
                continue;
            }
            double avgDmg = (s.unit().minDamage() + s.unit().maxDamage()) / 2.0;
            double basePower = avgDmg * s.getCount();
            if (s.canShoot()) {
                ranged += basePower * Math.min(s.shotsRemaining(), SHOT_HORIZON);
            } else {
                melee += basePower;
            }
        }
        return new SidePower(ranged, melee);
    }

    private static Map<Side, TeamStance> stances(BattleSetup setup) {
        // Side hat genau zwei Werte — direkt paaren statt über eine Zwischen-Map, deren
        // get() für NullAway nullable wäre.
        SidePower attacker = powerOf(setup, Side.ATTACKER);
        SidePower defender = powerOf(setup, Side.DEFENDER);
        Map<Side, TeamStance> stance = new EnumMap<>(Side.class);
        stance.put(Side.ATTACKER, stanceFor(attacker, defender));
        stance.put(Side.DEFENDER, stanceFor(defender, attacker));
        return stance;
    }

    private static TeamStance stanceFor(SidePower mine, SidePower theirs) {
        if (mine.ranged() > 0 && mine.ranged() > theirs.ranged() * DOMINANCE_THRESHOLD) {
            return TeamStance.RANGED_DOMINANT;
        }
        if (mine.melee() > theirs.melee() * DOMINANCE_THRESHOLD) {
            return TeamStance.MELEE_DOMINANT;
        }
        return TeamStance.BALANCED;
    }

    private static Map<Side, Stack> focusTargets(BattleSetup setup) {
        Map<Side, Stack> focus = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            // max() behält bei Gleichstand das erste Element → Slot-Reihenfolge entscheidet,
            // wie in der vorherigen "score > bestScore"-Schleife.
            setup.stacksOf(opposite(side)).stream()
                    .filter(Stack::isAlive)
                    .max(Comparator.comparingDouble(StrategicAutoSolver::focusScore))
                    .ifPresent(best -> focus.put(side, best));
        }
        return focus;
    }

    private static Set<Stack> protectedShooters(BattleSetup setup, Map<Side, TeamStance> stance) {
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
                if (s.isAlive() && s.canShoot() && needsTankCover(s, st, lastRow)) {
                    protect.add(s);
                }
            }
        }
        return protect;
    }

    /**
     * RANGED_DOMINANT: alle Schützen werden geschützt (klassisches Tank-Pattern). Sonst
     * (BALANCED): nur strukturell verwundbare Schützen am Rand — sie haben weniger
     * Adjazenz-Hexen, sodass 1–2 Tanks die Front komplett dichtmachen.
     */
    private static boolean needsTankCover(Stack shooter, @Nullable TeamStance stance, int lastRow) {
        if (stance == TeamStance.RANGED_DOMINANT) {
            return true;
        }
        int r = shooter.position().r();
        return r == 0 || r == lastRow;
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

    /**
     * Wählt für einen AoE-Schützen (SPLASH_SHOT / DEATH_CLOUD) das Ziel mit dem besten
     * Netto-Splash: getroffene Gegner minus getroffene <em>eigene</em> Stacks.
     *
     * <p>Friendly Fire ist engine-seitig aktiv — {@code Battle.findStackAt} iteriert beide
     * Seiten. Eine Heuristik, die nur Gegner-Cluster zählt, optimiert deshalb auf Eigentore.
     * Die Trefferzählung spiegelt die Engine-Geometrie exakt: SPLASH_SHOT nimmt die ersten
     * zwei Stacks in Nachbar-Reihenfolge (eigene konkurrieren um dieselben zwei Plätze),
     * DEATH_CLOUD trifft alle Nachbarn außer Untoten.
     *
     * @return bestes Ziel oder {@code null}, wenn kein Ziel einen positiven Netto-Splash hat
     */
    private static @Nullable Stack pickByAoeHitCount(Stack active, List<Stack> aliveEnemies,
                                                     List<Stack> aliveAll) {
        boolean deathCloud = active.hasSpeciality(UnitSpeciality.DEATH_CLOUD);
        Stack best = null;
        int bestScore = 0;
        int bestDanger = -1;
        for (Stack target : aliveEnemies) {
            SplashTally tally = tallySplash(active, target, aliveAll, deathCloud);
            int score = tally.enemyHits() - tally.ownHits();
            int danger = dangerScore(target);
            if (score > bestScore || (score == bestScore && danger > bestDanger)) {
                bestScore = score;
                bestDanger = danger;
                best = target;
            }
        }
        return bestScore > 0 ? best : null;
    }

    /**
     * Zählt die Splash-Kollateralen rund um {@code target}, getrennt nach Gegnern und eigenen
     * Stacks. Spiegelt {@code Battle.applySplashShot} / {@code Battle.applyDeathCloud}:
     * gleiche Nachbar-Reihenfolge, gleicher Zwei-Treffer-Deckel bei SPLASH_SHOT, gleiche
     * Undead-Ausnahme bei DEATH_CLOUD. Der Schütze selbst und das Hauptziel zählen nicht mit.
     */
    private static SplashTally tallySplash(Stack active, Stack target, List<Stack> aliveAll,
                                           boolean deathCloud) {
        int enemyHits = 0;
        int ownHits = 0;
        int found = 0;
        for (Hex neighbor : target.position().neighbors()) {
            if (!deathCloud && found >= SPLASH_SHOT_COLLATERAL_CAP) {
                break;
            }
            Stack collateral = splashVictimAt(neighbor, active, target, aliveAll, deathCloud);
            if (collateral != null) {
                found++;
                if (collateral.side() == active.side()) {
                    ownHits++;
                } else {
                    enemyHits++;
                }
            }
        }
        return new SplashTally(enemyHits, ownHits);
    }

    /**
     * Der Stack auf {@code hex}, den der Splash tatsächlich treffen würde — sonst {@code null}.
     * Der Schütze selbst und das Hauptziel zählen nicht als Kollateral, und Death Cloud
     * verschont Untote (auf beiden Seiten).
     */
    // s == target / s == active: Identitätsvergleich auf der mutable Stack-Entity (kein equals).
    @SuppressWarnings("ReferenceEquality")
    private static @Nullable Stack splashVictimAt(Hex hex, Stack active, Stack target,
                                                  List<Stack> aliveAll, boolean deathCloud) {
        for (Stack s : aliveAll) {
            if (s == active || s == target || !s.position().equals(hex)) {
                continue;
            }
            return deathCloud && s.unit().isUndead() ? null : s;
        }
        return null;
    }

    private record SplashTally(int enemyHits, int ownHits) {
    }

    /**
     * Wählt für einen FIRE_BREATH-Nahkämpfer ein Ziel, hinter dem ein zweiter gegnerischer
     * Stack steht. Nur Gegner sind Kandidaten, eigene Stacks im Breath-Hex können hier also
     * gar nicht gewählt werden.
     *
     * <p><strong>Grenze der Heuristik</strong>: Der Breath-Hex wird aus
     * {@code active.position()} <em>vor</em> der Bewegung bestimmt. {@code applyMeleeSplash}
     * wertet ihn aber aus der Position <em>nach</em> dem Anmarsch aus. Die Vorhersage stimmt
     * deshalb nur, wenn der Drache bereits in der Angriffslinie steht. Der gemessene
     * FIRE_BREATH-Eigenbeschuss entsteht folgerichtig nicht hier, sondern im Fallback-Pfad.
     * Sauber lösen lässt er sich nur über die Wahl des Lande-Hex, nicht über die Ziel-Wahl.
     *
     * @return bestes Inline-Ziel oder {@code null}, wenn keins existiert (dann entscheidet
     *         der Team-Plan bzw. Greedy)
     */
    private static @Nullable Stack pickInlineBreathTarget(Stack active, List<Stack> aliveEnemies) {
        Hex from = active.position();
        Stack best = null;
        int bestSecondaryDanger = -1;
        for (Stack target : aliveEnemies) {
            Stack secondary = stackAt(behindHex(from, target.position()), aliveEnemies, target);
            if (secondary == null) {
                continue;
            }
            int sec = dangerScore(secondary);
            if (sec > bestSecondaryDanger) {
                bestSecondaryDanger = sec;
                best = target;
            }
        }
        return best;
    }

    // s == excluded: Identitätsvergleich auf der mutable Stack-Entity (kein equals).
    @SuppressWarnings("ReferenceEquality")
    private static @Nullable Stack stackAt(Hex hex, List<Stack> candidates, Stack excluded) {
        for (Stack s : candidates) {
            if (s != excluded && s.position().equals(hex)) {
                return s;
            }
        }
        return null;
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
        // null, sobald eine der beiden Listen leer ist — dann gibt es nichts zu decken.
        ThreatenedShooter threatened = mostThreatened(myShooters, setup.opponentsOf(active));
        if (threatened == null) {
            return null;
        }
        return tankSpotFor(active, threatened, battlefield, setup);
    }

    /**
     * Der am dichtesten bedrängte eigene Schütze samt des Gegners, der ihn bedrängt.
     */
    private record ThreatenedShooter(Stack shooter, Stack threat) {
    }

    private static @Nullable ThreatenedShooter mostThreatened(List<Stack> shooters, List<Stack> enemies) {
        ThreatenedShooter best = null;
        int minDist = Integer.MAX_VALUE;
        for (Stack shooter : shooters) {
            for (Stack enemy : enemies) {
                int d = shooter.position().distanceTo(enemy.position());
                if (d < minDist) {
                    minDist = d;
                    best = new ThreatenedShooter(shooter, enemy);
                }
            }
        }
        return best;
    }

    /**
     * Bevorzugt der Hex zwischen Schütze und Bedrohung, sonst irgendein freier Adjacent.
     */
    private static @Nullable Hex tankSpotFor(Stack active, ThreatenedShooter threatened,
                                             Battlefield battlefield, BattleSetup setup) {
        Hex shooterPos = threatened.shooter().position();
        Hex preferred = adjacentTowards(shooterPos, threatened.threat().position());
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
            if (!candidate.canShoot()
                    || !hasReachableFreeAdjacent(candidate, from, speed, bf, active, setup)) {
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
            if (bf.isPassable(adj)
                    && from.distanceTo(adj) <= speed
                    && !Objects.equals(adj, mover.position())
                    && !isHexOccupiedByOther(adj, mover, setup)) {
                return true;
            }
        }
        return false;
    }

    // s != mover: Identitätsvergleich auf der mutable Stack-Entity (kein equals).
    @SuppressWarnings("ReferenceEquality")
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

    // other != active: Identitätsvergleich auf der mutable Stack-Entity (kein equals).
    @SuppressWarnings("ReferenceEquality")
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
