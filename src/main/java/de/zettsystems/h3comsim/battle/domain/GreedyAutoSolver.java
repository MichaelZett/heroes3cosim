package de.zettsystems.h3comsim.battle.domain;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Heuristik mit Schützen-Intelligenz:
 * <ul>
 *   <li>Bei Distanz 1 → Nahkampf-Angriff.</li>
 *   <li>Schütze ohne Engagement-Drohung → schießen.</li>
 *   <li>Schütze gegen schnelleren Melee-Gegner, der dieses Mal nicht in Distanz 1 wäre → schießen.</li>
 *   <li>Schütze, der schneller als der heranrückende Melee-Gegner ist und nächste Runde im
 *       Nahkampf wäre → ausweichen in beliebiger Richtung, damit Distanz &gt; Gegner-Speed bleibt.
 *       Bei mehreren Kandidaten gewinnt der mit obstacle-freier Schusslinie (kein eigener
 *       Obstacle-Penalty).</li>
 *   <li>Schütze vs. Schütze: bleiben stehen und schießen, außer der Aktive hat
 *       {@code NO_OBSTACLE_PENALTY} und der Gegner nicht — dann wird, falls möglich, eine
 *       Position hinter Obstacle gesucht, damit der Gegner ½-Schaden eingehend bekommt während
 *       der Aktive ungerührt feuert. Setzt die Bewegung aber nur, wenn die Cover-Position
 *       erreichbar ist und die Distanz erhalten bleibt.</li>
 *   <li>Sonst → Bewegung Richtung Gegner, ggf. Move-and-Melee.</li>
 * </ul>
 *
 * <p><strong>Stack-Belegung</strong>: alle Kandidaten-Suchen (Charge, Kite, Cover, Flieger-
 * Landung) filtern Hexen heraus, die von einem anderen lebenden Stack besetzt sind. Dafür
 * speichert {@link #planRound(BattleSetup)} das aktuelle Setup. Ohne planRound-Call
 * (z.B. in einigen Unit-Tests) bleibt {@code currentSetup} null und der Filter ist inaktiv —
 * im Single-Battle-Pfad gibt's eh keine Stack-Konflikte.
 */
public final class GreedyAutoSolver implements AutoSolver {

    private @Nullable BattleSetup currentSetup;

    @Override
    public void planRound(BattleSetup setup) {
        this.currentSetup = setup;
    }

    /**
     * Multi-Target-Auswahl für Multi-Stack-Battles. Heuristik:
     * <ul>
     *   <li>Lebender Gegner auf Distanz 1 → diesen (Engagement zwingt zum Nahkampf).</li>
     *   <li>Schütze (kann noch schießen) → gefährlichster Gegner = {@code avgDmg × count};
     *       Tiebreak: kürzere Distanz, dann niedrigerer Slot.</li>
     *   <li>Sonst (Melee-Mover) → kürzeste Distanz, Tiebreak niedrigerer Slot.</li>
     * </ul>
     * Sichert dieselbe API wie die 1-vs-1-Variante: {@link #decide(Stack, Stack, Battlefield)}
     * läuft anschließend gegen das hier gewählte Single-Target weiter — bestehende
     * Kite/Cover/Charge-Heuristiken bleiben unverändert.
     */
    @Override
    public @Nullable Stack pickTarget(Stack active, List<Stack> opponents, Battlefield battlefield) {
        List<Stack> alive = opponents.stream().filter(Stack::isAlive).toList();
        if (alive.isEmpty()) {
            return null;
        }
        Hex from = active.position();
        Stack adjacent = alive.stream()
                .filter(o -> from.distanceTo(o.position()) == 1)
                .min(Comparator.comparingInt(Stack::slot))
                .orElse(null);
        if (adjacent != null) {
            return adjacent;
        }
        if (active.canShoot()) {
            return alive.stream()
                    .max(Comparator
                            .comparingInt((Stack o) -> dangerScore(o))
                            .thenComparingInt(o -> -from.distanceTo(o.position()))
                            .thenComparingInt(o -> -o.slot()))
                    .orElseThrow();
        }
        return alive.stream()
                .min(Comparator
                        .comparingInt((Stack o) -> from.distanceTo(o.position()))
                        .thenComparingInt(Stack::slot))
                .orElseThrow();
    }

    private static int dangerScore(Stack opponent) {
        int avgDmg = (opponent.unit().minDamage() + opponent.unit().maxDamage()) / 2;
        return avgDmg * opponent.getCount();
    }

    @Override
    public Action decide(Stack active, Stack opponent, Battlefield battlefield) {
        Hex from = active.position();
        Hex to = opponent.position();
        int distance = from.distanceTo(to);
        int speed = active.unit().speed();
        Movement movement = active.unit().movement();

        if (distance == 1) {
            return new Action.Melee(opponent);
        }
        if (active.canShoot()) {
            return decideShooter(active, opponent, battlefield, from, to, distance);
        }
        // Flieger umfliegen Tank-Walls: statt straight-line auf einen vermutlich belegten
        // Hex zu zielen, suche den nächsten freien Adjacent des Ziels. Findet sich keiner
        // (alle Adjacents belegt oder außer Reichweite), defendiere — semantisch korrekter
        // als ein blinder Move.
        if (movement == Movement.FLYING) {
            Hex landing = findFlyerLanding(active, opponent, battlefield);
            if (landing != null) {
                return new Action.MoveAndMelee(landing, opponent);
            }
            return new Action.Defend();
        }
        // IMPACT_DAMAGE-Units (Cavalier/Champion): maximalen Run-up wählen statt direkter Linie.
        // Engine zählt {@code hexesMoved = startPos.distanceTo(destination)} → straight-line.
        if (active.hasSpeciality(UnitSpeciality.IMPACT_DAMAGE)) {
            Hex charge = findMaxRunupCharge(active, to, battlefield, speed, movement);
            if (charge != null) {
                return new Action.MoveAndMelee(charge, opponent);
            }
        }
        Hex moveTarget = battlefield.moveToward(from, to, speed, movement);
        if (moveTarget.equals(from)) {
            return new Action.Defend();
        }
        if (isOccupiedByOther(moveTarget, active)) {
            // moveToward berücksichtigt keine Stacks → könnte auf den Tank vor dem Ziel zielen.
            // Statt blind hinzulaufen und vom Engine-Sicherheitsnetz zu Defend degradiert zu
            // werden, defendieren wir direkt.
            return new Action.Defend();
        }
        if (moveTarget.distanceTo(to) == 1) {
            return new Action.MoveAndMelee(moveTarget, opponent);
        }
        return new Action.Move(moveTarget);
    }

    /**
     * Findet für einen Flieger den am besten erreichbaren freien Adjacent-Hex des Ziels.
     * Cube-Distanz reicht (Flieger ignorieren Obstacles). Bei mehreren Kandidaten gewinnt
     * der mit der geringsten Distanz vom Startfeld. Liefert {@code null}, wenn alle
     * Adjacents blockiert oder außer Reichweite sind.
     */
    private @Nullable Hex findFlyerLanding(Stack active, Stack opponent, Battlefield bf) {
        Hex from = active.position();
        int speed = active.unit().speed();
        Hex best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Hex candidate : opponent.position().neighbors()) {
            if (candidate.equals(from)) {
                continue;
            }
            if (!bf.isPassable(candidate)) {
                continue;
            }
            int d = from.distanceTo(candidate);
            if (d > speed) {
                continue;
            }
            if (isOccupiedByOther(candidate, active)) {
                continue;
            }
            if (d < bestDist) {
                bestDist = d;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Sucht unter den sechs adjazenten Hexen des Gegners das mit der größten geraden Distanz vom
     * Startfeld — bei IMPACT_DAMAGE-Einheiten gibt jeder Hex Anlauf +5 % Schaden (gekappt bei +50 %
     * = 10 Hex). Kandidat muss passable, frei und in einer Runde erreichbar sein (sowohl
     * straight-line als auch A*-Pfad ≤ Speed). Liefert {@code null}, wenn kein adjazenter Hex
     * erreichbar ist — dann fällt der Solver auf den normalen Move-Pfad zurück.
     */
    private @Nullable Hex findMaxRunupCharge(Stack active, Hex to, Battlefield bf, int speed,
                                             Movement movement) {
        Hex from = active.position();
        return to.neighbors().stream()
                .filter(c -> isReachableInOneTurn(active, c, from, bf, speed, movement))
                .filter(c -> from.distanceTo(c) > 0)
                .max(Comparator.comparingInt(c -> Math.min(from.distanceTo(c), 10)))
                .orElse(null);
    }

    private boolean isReachableInOneTurn(Stack active, Hex candidate, Hex from, Battlefield bf,
                                         int speed, Movement movement) {
        if (!bf.isPassable(candidate)) {
            return false;
        }
        if (from.distanceTo(candidate) > speed) {
            return false;
        }
        if (isOccupiedByOther(candidate, active)) {
            return false;
        }
        List<Hex> path = bf.findPath(from, candidate, movement);
        return !path.isEmpty() && path.size() <= speed;
    }

    private boolean isOccupiedByOther(Hex hex, Stack mover) {
        BattleSetup setup = currentSetup;
        if (setup == null) {
            return false;
        }
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

    private Action decideShooter(Stack active, Stack opponent, Battlefield bf,
                                 Hex from, Hex to, int distance) {
        int opponentSpeed = opponent.getSpeed();
        int mySpeed = active.getSpeed();

        if (opponent.canShoot()) {
            return decideShooterVsShooter(active, opponent, bf, from, to, mySpeed);
        }

        // Keine unmittelbare Engagement-Drohung → schießen.
        if (distance - opponentSpeed > 1) {
            return new Action.Shoot(opponent);
        }
        // Wir sind nicht schneller — Kiten würde nichts bringen, lieber schießen.
        if (mySpeed <= opponentSpeed) {
            return new Action.Shoot(opponent);
        }
        // DPS-Race: Wenn wir den Gegner schätzungsweise erledigen, bevor er uns erreicht,
        // hat Kiten keinen Sinn — der verlorene Schuss wäre vermutlich der entscheidende.
        if (willOutpaceOpponent(active, opponent, distance)) {
            return new Action.Shoot(opponent);
        }
        // Kite nur bei wirklicher Lebensgefahr — wenn ein eingehender Treffer den Top-Creature
        // (oder mehr) one-shotten würde. Sonst Treffer kassieren und feuern; sonst landen Sims
        // in 50+ Rounds wegen Kite/Shoot-Wechsel.
        if (!threatIsDeadly(opponent, active)) {
            return new Action.Shoot(opponent);
        }
        Hex kite = findKitePosition(active, from, to, bf, mySpeed, opponentSpeed,
                active.unit().movement());
        if (kite == null) {
            return new Action.Shoot(opponent);
        }
        return new Action.Move(kite);
    }

    /**
     * Grobe Schätzung: würde ein durchschnittlicher Treffer des Gegners die Top-HP des Shooters
     * zerlegen? Att/Def-Boni werden ignoriert — die Heuristik ist bewusst pessimistisch zugunsten
     * von „lieber schießen", weil der gewählte Aktionsraum (Schuss verlieren) teuer ist.
     */
    private static boolean threatIsDeadly(Stack opponent, Stack active) {
        int avgBase = (opponent.unit().minDamage() + opponent.unit().maxDamage()) / 2;
        int avgIncoming = opponent.getCount() * avgBase;
        return avgIncoming >= active.getCurrentHealth();
    }

    /**
     * Erwartet der Schütze, den Gegner mit den verbleibenden Schüssen während dessen Anlauf zu
     * töten? Konservative Schätzung: avg-Schadens-Range × Stack-Count × Schüsse-pro-Runde ×
     * Runden-bis-Engagement gegen die Gesamt-HP des Gegners. Att/Def und Penalties bleiben außen
     * vor — die Heuristik soll Kite/Shoot-Loops abbrechen, sobald sich der Race klar abzeichnet.
     */
    private static boolean willOutpaceOpponent(Stack active, Stack opponent, int distance) {
        int opponentSpeed = Math.max(1, opponent.getSpeed());
        // Aufgerundete Runden bis Engagement; Engagement = Distanz 1.
        int turnsUntilMelee = Math.max(1, (distance - 1 + opponentSpeed - 1) / opponentSpeed);
        double avgPerShot = (active.unit().minDamage() + active.unit().maxDamage()) / 2.0;
        int shotsPerTurn = active.hasSpeciality(UnitSpeciality.TWO_SHOTS) ? 2 : 1;
        int shotsAvailable = Math.min(active.shotsRemaining(), turnsUntilMelee * shotsPerTurn);
        double projectedDamage = active.getCount() * avgPerShot * shotsAvailable;

        int opponentMaxHp = opponent.unit().health();
        int opponentTotalHp = Math.max(0, opponent.getCount() - 1) * opponentMaxHp
                + opponent.getCurrentHealth();
        return projectedDamage >= opponentTotalHp;
    }

    private Action decideShooterVsShooter(Stack active, Stack opponent, Battlefield bf,
                                          Hex from, Hex to, int mySpeed) {
        boolean iHaveCoverIgnore = active.unit().hasSpeciality(UnitSpeciality.NO_OBSTACLE_PENALTY);
        boolean opponentHasCoverIgnore = opponent.unit().hasSpeciality(UnitSpeciality.NO_OBSTACLE_PENALTY);
        // Cover-Repositionierung lohnt nur, wenn ich obstacle-immune bin (ich schieße voll durch)
        // und der Gegner nicht (er halbiert). Sonst ist der verlorene Schuss diese Runde teurer
        // als der Cover-Vorteil.
        if (!iHaveCoverIgnore || opponentHasCoverIgnore) {
            return new Action.Shoot(opponent);
        }
        if (bf.obstacles().isEmpty()) {
            return new Action.Shoot(opponent);
        }
        Hex cover = findCoverPosition(active, from, to, bf, mySpeed, active.unit().movement());
        if (cover == null) {
            return new Action.Shoot(opponent);
        }
        return new Action.Move(cover);
    }

    /**
     * Sucht ein Feld, das mindestens {@code opponentSpeed + 2} Hex vom Gegner entfernt ist
     * (damit der Gegner nächste Runde noch nicht adjacent ist) und vom Start in einem Zug
     * erreichbar ist. Bei mehreren Optionen gewinnt die größere Distanz; bei Gleichstand
     * die obstacle-freie eigene Schusslinie.
     */
    private @Nullable Hex findKitePosition(Stack active, Hex from, Hex to, Battlefield bf,
                                           int mySpeed, int opponentSpeed, Movement movement) {
        return reachableCandidates(active, from, to, bf, mySpeed, movement).stream()
                .filter(c -> c.distanceTo(to) - opponentSpeed > 1)
                .max(kiteScore(to, bf))
                .orElse(null);
    }

    private static Comparator<Hex> kiteScore(Hex to, Battlefield bf) {
        Comparator<Hex> byDistance = Comparator.comparingInt(c -> c.distanceTo(to));
        Comparator<Hex> byClearLine = Comparator.comparingInt(
                c -> PathFinder.hasObstacleInLine(bf, c, to) ? 0 : 1);
        return byDistance.thenComparing(byClearLine);
    }

    /**
     * Sucht ein Feld, von dem die Sichtlinie des Gegners auf mich durch ein Hindernis verläuft
     * (er bekommt Obstacle-Penalty), während meine Sichtlinie frei bleibt (oder ich ohnehin
     * obstacle-immun bin). Erreichbar in einem Zug, kein anderer Stack drauf.
     */
    private @Nullable Hex findCoverPosition(Stack active, Hex from, Hex to, Battlefield bf,
                                            int mySpeed, Movement movement) {
        return reachableCandidates(active, from, to, bf, mySpeed, movement).stream()
                .filter(c -> PathFinder.hasObstacleInLine(bf, to, c))
                .findFirst()
                .orElse(null);
    }

    private List<Hex> reachableCandidates(Stack active, Hex from, Hex to, Battlefield bf,
                                          int mySpeed, Movement movement) {
        List<Hex> candidates = new ArrayList<>();
        for (int q = 0; q < bf.width(); q++) {
            for (int r = 0; r < bf.height(); r++) {
                Hex c = new Hex(q, r);
                if (isKiteCandidate(active, c, from, to, bf, mySpeed, movement)) {
                    candidates.add(c);
                }
            }
        }
        return candidates;
    }

    private boolean isKiteCandidate(Stack active, Hex c, Hex from, Hex to, Battlefield bf,
                                    int mySpeed, Movement movement) {
        if (c.equals(from) || c.equals(to)) {
            return false;
        }
        return isReachableInOneTurn(active, c, from, bf, mySpeed, movement);
    }
}
