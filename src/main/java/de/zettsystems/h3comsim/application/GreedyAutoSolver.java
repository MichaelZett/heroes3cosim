package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.Battlefield;
import de.zettsystems.h3comsim.domain.Hex;
import de.zettsystems.h3comsim.domain.PathFinder;
import de.zettsystems.h3comsim.domain.Stack;
import de.zettsystems.h3comsim.domain.UnitSpeciality;
import org.jspecify.annotations.Nullable;

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

        if (distance == 1) {
            return new Action.Melee(opponent);
        }
        if (active.canShoot()) {
            return decideShooter(active, opponent, battlefield, from, to, distance);
        }
        Hex moveTarget = battlefield.moveToward(from, to, active.unit().speed());
        if (moveTarget.equals(from)) {
            return new Action.Wait();
        }
        if (moveTarget.distanceTo(to) == 1) {
            return new Action.MoveAndMelee(moveTarget, opponent);
        }
        return new Action.Move(moveTarget);
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
        // Kite nur bei wirklicher Lebensgefahr — wenn ein eingehender Treffer den Top-Creature
        // (oder mehr) one-shotten würde. Sonst Treffer kassieren und feuern; sonst landen Sims
        // in 50+ Rounds wegen Kite/Shoot-Wechsel.
        if (!threatIsDeadly(opponent, active)) {
            return new Action.Shoot(opponent);
        }
        Hex kite = findKitePosition(from, to, bf, mySpeed, opponentSpeed);
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
        Hex cover = findCoverPosition(from, to, bf, mySpeed);
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
                                                  int mySpeed, int opponentSpeed) {
        Hex best = null;
        int bestDist = -1;
        int bestPenaltyScore = -1;
        for (int q = 0; q < bf.width(); q++) {
            for (int r = 0; r < bf.height(); r++) {
                Hex c = new Hex(q, r);
                if (c.equals(from) || c.equals(to)) {
                    continue;
                }
                if (!bf.isPassable(c)) {
                    continue;
                }
                if (from.distanceTo(c) > mySpeed) {
                    continue;
                }
                if (bf.findPath(from, c).isEmpty()) {
                    continue;
                }
                int d = c.distanceTo(to);
                if (d - opponentSpeed <= 1) {
                    continue;
                }
                int penaltyScore = PathFinder.hasObstacleInLine(bf, c, to) ? 0 : 1;
                if (d > bestDist || (d == bestDist && penaltyScore > bestPenaltyScore)) {
                    bestDist = d;
                    bestPenaltyScore = penaltyScore;
                    best = c;
                }
            }
        }
        return best;
    }

    /**
     * Sucht ein Feld, von dem die Sichtlinie des Gegners auf mich durch ein Hindernis verläuft
     * (er bekommt Obstacle-Penalty), während meine Sichtlinie frei bleibt (oder ich ohnehin
     * obstacle-immun bin). Erreichbar in einem Zug.
     */
    private static @Nullable Hex findCoverPosition(Hex from, Hex to, Battlefield bf, int mySpeed) {
        for (int q = 0; q < bf.width(); q++) {
            for (int r = 0; r < bf.height(); r++) {
                Hex c = new Hex(q, r);
                if (c.equals(from) || c.equals(to)) {
                    continue;
                }
                if (!bf.isPassable(c)) {
                    continue;
                }
                if (from.distanceTo(c) > mySpeed) {
                    continue;
                }
                if (bf.findPath(from, c).isEmpty()) {
                    continue;
                }
                // Cover ist nur dann nützlich, wenn der Gegner durch ein Obstacle schießen müsste.
                if (!PathFinder.hasObstacleInLine(bf, to, c)) {
                    continue;
                }
                return c;
            }
        }
        return null;
    }
}
