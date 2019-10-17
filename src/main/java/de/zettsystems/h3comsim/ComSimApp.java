package de.zettsystems.h3comsim;

import de.zettsystems.h3comsim.arena.Arena;
import de.zettsystems.h3comsim.combat.CombatEngine;
import de.zettsystems.h3comsim.unit.concrete.DreadKnight;
import de.zettsystems.h3comsim.unit.concrete.NagaQueen;

public class ComSimApp {
    public static void main(String[] args) {
        Arena arena = new Arena(new DreadKnight(), new NagaQueen());

        CombatEngine.solveCombat(arena);
    }
}
