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
 * Team-koordinierter Multi-Stack-Solver. Pro Runde wird ein {@link RoundPlan} mit einem
 * Focus-Fire-Ziel je Seite berechnet; den Rest der Entscheidung trifft {@link GreedyAutoSolver}.
 *
 * <p>Vorrang in der Ziel-Wahl:
 * <ol>
 *   <li><strong>Engagement</strong>: Ein Schütze mit Gegner daneben kann nicht schießen und
 *       schlägt den Nachbarn. Ein Nahkämpfer bleibt nicht kleben, wenn er ein lohnenderes Ziel
 *       diese Runde erreicht.</li>
 *   <li><strong>Flieger auf Schützen (E)</strong>: ein ungedeckter gegnerischer Schütze in
 *       Reichweite.</li>
 *   <li><strong>AoE-aware Target-Pick (B)</strong>: Schützen mit
 *       {@link UnitSpeciality#SPLASH_SHOT} / {@link UnitSpeciality#DEATH_CLOUD} maximieren den
 *       Netto-Splash; Nahkämpfer mit {@link UnitSpeciality#FIRE_BREATH} bevorzugen Inline-Paare.</li>
 *   <li><strong>Focus-Fire (C)</strong>: das vom Plan gewählte Ziel, für Schützen immer, für
 *       Nahkämpfer nur, wenn sie es diese Runde angreifen können.</li>
 *   <li>sonst Greedy.</li>
 * </ol>
 *
 * <p>Früher gab es zusätzlich ein Tank-Pattern (Nahkämpfer decken die eigenen Schützen), den
 * Fokus auch auf unerreichbare Ziele und für jeden den Zwang, den Nachbarn zu schlagen. Mit
 * zweifeldrigen Kreaturen verlor jedes davon im direkten Duell gegen dieselbe Fassung ohne
 * (0.469, 0.458, 0.480; je 6480 Kämpfe). Ohne alle drei und mit Greedys Warten vor ungedeckten
 * Anläufen gewinnt der Solver 0.619 gegen die alte Fassung und 0.580 gegen Greedy; die alte
 * verlor gegen Greedy mit 0.397. Seit Greedy in der Wait-Phase nur noch bis außer Reichweite der
 * Gegner vorrückt, die noch ziehen ({@code GreedyAutoSolver#safeStep}), sind es 0.519 gegen die
 * Fassung ohne und 0.564 gegen Greedy mit derselben Regel.
 *
 * <p>Stateful: speichert den Plan + Setup-Referenz zwischen {@link #planRound(BattleSetup)} und
 * den nachfolgenden {@link #pickTarget}/{@link #decide}-Aufrufen. Pro {@link Battle} eine
 * eigene Instanz verwenden.
 */
public final class StrategicAutoSolver implements AutoSolver {

    private final GreedyAutoSolver greedy;

    public StrategicAutoSolver() {
        this(true);
    }

    /** @see GreedyAutoSolver#GreedyAutoSolver(boolean) */
    public StrategicAutoSolver(boolean tacticalWait) {
        this.greedy = new GreedyAutoSolver(tacticalWait);
    }

    /** @see GreedyAutoSolver#GreedyAutoSolver(boolean, boolean) */
    public StrategicAutoSolver(boolean tacticalWait, boolean safeApproach) {
        this.greedy = new GreedyAutoSolver(tacticalWait, safeApproach);
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
        // Engagement: Ein Schütze mit Gegner daneben kann nicht schießen. Ein Nahkämpfer prüft
        // erst die lohnenderen Ziele; Greedy fällt am Ende ohnehin auf den Nachbarn zurück.
        if (active.canShoot() && !active.shootsWhenAdjacent()) {
            Stack adjacent = alive.stream()
                    .filter(o -> active.distanceTo(o) == 1)
                    .min(Comparator.comparingInt(Stack::slot))
                    .orElse(null);
            if (adjacent != null) {
                return adjacent;
            }
        }

        Stack chosen = pickBySpeciality(active, alive, battlefield);
        if (chosen == null) {
            // (C) Focus-Fire: Team-Plan vorgeben lassen, sofern das Ziel diese Runde angreifbar
            // ist, sonst Greedy. Ein Nahkämpfer, der dem Fokus hinterherläuft, fehlt vorne.
            Stack focus = plan.focusOf(active.side());
            chosen = focus != null && focus.isAlive() && alive.contains(focus)
                    && (active.canShoot() || canAttackThisTurn(active, focus, battlefield))
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
        // Sonderaktionen vor allem anderen: ein Arch Angel, der lieber angreift, verschenkt
        // seinen einzigen Wurf.
        Action special = GreedyAutoSolver.specialAction(active, opponent, currentSetup);
        if (special != null) {
            return special;
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
        return new RoundPlan(focusTargets(setup));
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
     * Die Trefferzählung spiegelt die Engine-Geometrie exakt: SPLASH_SHOT trifft alle Nachbarn,
     * DEATH_CLOUD alle Nachbarn außer Untoten.
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
     * alle Nachbarn des Ziels, Undead-Ausnahme bei DEATH_CLOUD. Der Schütze selbst und das Hauptziel zählen nicht mit.
     */
    private static SplashTally tallySplash(Stack active, Stack target, List<Stack> aliveAll,
                                           boolean deathCloud) {
        // Jeder Kollateral einmal, auch wenn er zweifeldrig an zwei Felder des Ziels grenzt.
        Set<Stack> victims = new LinkedHashSet<>();
        for (Hex own : target.hexes()) {
            for (Hex neighbor : own.neighbors()) {
                Stack collateral = splashVictimAt(neighbor, active, target, aliveAll, deathCloud);
                if (collateral != null) {
                    victims.add(collateral);
                }
            }
        }
        int enemyHits = 0;
        int ownHits = 0;
        for (Stack collateral : victims) {
            if (collateral.side() == active.side()) {
                ownHits++;
            } else {
                enemyHits++;
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
            if (s == active || s == target || !s.occupies(hex)) {
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
            Hex hit = target.nearestHexTo(from);
            Hex behind = behindHex(active.nearestHexTo(hit), hit);
            if (target.occupies(behind)) {
                behind = behindHex(hit, behind);
            }
            Stack secondary = stackAt(behind, aliveEnemies, target);
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
            if (s != excluded && s.occupies(hex)) {
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
    // Reichweite
    // ------------------------------------------------------------------ //

    /** Ob {@code active} {@code target} diese Runde im Nahkampf erreicht (oder schon daneben steht). */
    private boolean canAttackThisTurn(Stack active, Stack target, Battlefield bf) {
        BattleSetup setup = currentSetup;
        if (setup == null || active.distanceTo(target) == 1) {
            return true;
        }
        for (Hex head : setup.attackHeads(active, target)) {
            if (isReachableLandingSpot(active, head, bf, setup)) {
                return true;
            }
        }
        return false;
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
        int speed = active.getSpeed();
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
        for (Hex adj : setup.attackHeads(mover, target)) {
            if (bf.isPassable(adj)
                    && from.distanceTo(adj) <= speed
                    && !Objects.equals(adj, mover.position())) {
                return true;
            }
        }
        return false;
    }

    static boolean isReachableLandingSpot(Stack active, Hex hex, Battlefield bf,
                                          BattleSetup setup) {
        if (hex.equals(active.position())) {
            return false;
        }
        if (!bf.isPassable(hex)) {
            return false;
        }
        // TELEPORT_NO_COST (Devil/Arch Devil): Speed-Schranke entfällt.
        if (!active.hasSpeciality(UnitSpeciality.TELEPORT_NO_COST)
                && active.position().distanceTo(hex) > active.getSpeed()) {
            return false;
        }
        // Platz für alle Felder des Stacks, frei von anderen Stacks und Maschinen.
        if (!active.hexesAt(hex).stream().allMatch(bf::isPassable) || !setup.fits(active, hex)) {
            return false;
        }
        List<Hex> path = bf.pathFor(active, hex);
        return !path.isEmpty() && path.size() <= active.getSpeed();
    }

    private static Side opposite(Side side) {
        return side == Side.ATTACKER ? Side.DEFENDER : Side.ATTACKER;
    }
}
