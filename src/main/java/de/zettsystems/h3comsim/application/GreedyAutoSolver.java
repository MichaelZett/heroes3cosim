package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.Battlefield;
import de.zettsystems.h3comsim.domain.Hex;
import de.zettsystems.h3comsim.domain.Movement;
import de.zettsystems.h3comsim.domain.PathFinder;
import de.zettsystems.h3comsim.domain.Stack;
import de.zettsystems.h3comsim.domain.UnitSpeciality;
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
 */
public final class GreedyAutoSolver implements AutoSolver {

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
        // IMPACT_DAMAGE-Units (Cavalier/Champion): maximalen Run-up wählen statt direkter Linie.
        // Engine zählt {@code hexesMoved = startPos.distanceTo(destination)} → straight-line.
        if (active.hasSpeciality(UnitSpeciality.IMPACT_DAMAGE)) {
            Hex charge = findMaxRunupCharge(from, to, battlefield, speed, movement);
            if (charge != null) {
                return new Action.MoveAndMelee(charge, opponent);
            }
        }
        Hex moveTarget = battlefield.moveToward(from, to, speed, movement);
        if (moveTarget.equals(from)) {
            return new Action.Wait();
        }
        if (moveTarget.distanceTo(to) == 1) {
            return new Action.MoveAndMelee(moveTarget, opponent);
        }
        return new Action.Move(moveTarget);
    }

    /**
     * Sucht unter den sechs adjazenten Hexen des Gegners das mit der größten geraden Distanz vom
     * Startfeld — bei IMPACT_DAMAGE-Einheiten gibt jeder Hex Anlauf +5 % Schaden (gekappt bei +50 %
     * = 10 Hex). Kandidat muss passable und in einer Runde erreichbar sein (sowohl straight-line
     * als auch A*-Pfad ≤ Speed). Liefert {@code null}, wenn kein adjazenter Hex erreichbar ist —
     * dann fällt der Solver auf den normalen Move-Pfad zurück.
     */
    private static @Nullable Hex findMaxRunupCharge(Hex from, Hex to, Battlefield bf, int speed,
                                                    Movement movement) {
        return to.neighbors().stream()
                .filter(c -> isReachableInOneTurn(c, from, bf, speed, movement))
                .filter(c -> from.distanceTo(c) > 0)
                .max(Comparator.comparingInt(c -> Math.min(from.distanceTo(c), 10)))
                .orElse(null);
    }

    private static boolean isReachableInOneTurn(Hex candidate, Hex from, Battlefield bf,
                                                int speed, Movement movement) {
        if (!bf.isPassable(candidate)) return false;
        if (from.distanceTo(candidate) > speed) return false;
        List<Hex> path = bf.findPath(from, candidate, movement);
        return !path.isEmpty() && path.size() <= speed;
    }

    private static Action decideShooter(Stack active, Stack opponent, Battlefield bf,
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
        Hex kite = findKitePosition(from, to, bf, mySpeed, opponentSpeed, active.unit().movement());
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

    private static Action decideShooterVsShooter(Stack active, Stack opponent, Battlefield bf,
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
        Hex cover = findCoverPosition(from, to, bf, mySpeed, active.unit().movement());
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
    private static @Nullable Hex findKitePosition(Hex from, Hex to, Battlefield bf,
                                                  int mySpeed, int opponentSpeed, Movement movement) {
        return reachableCandidates(from, to, bf, mySpeed, movement).stream()
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
     * obstacle-immun bin). Erreichbar in einem Zug.
     */
    private static @Nullable Hex findCoverPosition(Hex from, Hex to, Battlefield bf, int mySpeed,
                                                   Movement movement) {
        return reachableCandidates(from, to, bf, mySpeed, movement).stream()
                .filter(c -> PathFinder.hasObstacleInLine(bf, to, c))
                .findFirst()
                .orElse(null);
    }

    private static List<Hex> reachableCandidates(Hex from, Hex to, Battlefield bf, int mySpeed,
                                                 Movement movement) {
        List<Hex> candidates = new ArrayList<>();
        for (int q = 0; q < bf.width(); q++) {
            for (int r = 0; r < bf.height(); r++) {
                Hex c = new Hex(q, r);
                if (isKiteCandidate(c, from, to, bf, mySpeed, movement)) {
                    candidates.add(c);
                }
            }
        }
        return candidates;
    }

    private static boolean isKiteCandidate(Hex c, Hex from, Hex to, Battlefield bf,
                                           int mySpeed, Movement movement) {
        if (c.equals(from) || c.equals(to)) return false;
        return isReachableInOneTurn(c, from, bf, mySpeed, movement);
    }
}
