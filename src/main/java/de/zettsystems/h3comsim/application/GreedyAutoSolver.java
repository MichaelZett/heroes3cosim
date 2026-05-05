package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.Battlefield;
import de.zettsystems.h3comsim.domain.Hex;
import de.zettsystems.h3comsim.domain.Stack;

/**
 * MVP-Heuristik:
 * <ul>
 *   <li>Bei Distanz 1 → Nahkampf-Angriff. Schützen können laut H3-Regel nicht schießen, wenn ein
 *       Gegner direkt benachbart ist; sie schlagen mit Nahkampf-Penalty zu.</li>
 *   <li>Sonst, wenn Schütze mit Schuss übrig → schießen, ohne sich zu bewegen.</li>
 *   <li>Sonst Bewegung in Richtung Gegner — wenn dabei Distanz 1 erreicht wird, kombiniert mit
 *       Nahkampf-Angriff im selben Zug.</li>
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
            return new Action.Shoot(opponent);
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
}
