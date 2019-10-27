package de.zettsystems.h3comsim;

import de.zettsystems.h3comsim.arena.Arena;
import de.zettsystems.h3comsim.combat.CombatEngine;
import de.zettsystems.h3comsim.unit.common.Stack;
import de.zettsystems.h3comsim.unit.concrete.DreadKnight;
import de.zettsystems.h3comsim.unit.concrete.NagaQueen;

public class ComSimApp {
    public static void main(String[] args) {
        Stack attacker = Stack.createStack(new DreadKnight(), 1);
        Stack defender = Stack.createStack(new NagaQueen(), 1);
        Arena arena = new Arena(attacker, defender);

        CombatEngine.solveCombat(arena);
    }

}
