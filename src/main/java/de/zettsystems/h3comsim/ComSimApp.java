package de.zettsystems.h3comsim;

import de.zettsystems.h3comsim.arena.Arena;
import de.zettsystems.h3comsim.combat.CombatEngine;
import de.zettsystems.h3comsim.unit.common.Stack;
import de.zettsystems.h3comsim.unit.concrete.ArchAngel;
import de.zettsystems.h3comsim.unit.concrete.CentaurCaptain;
import de.zettsystems.h3comsim.unit.concrete.Peasant;

public class ComSimApp {
    public static void main(String[] args) {
        Stack attacker = Stack.createStack(new Peasant(), 100);
        Stack defender = Stack.createStack(new CentaurCaptain(), 16);
        Arena arena = new Arena(attacker, defender);

        CombatEngine.solveCombat(arena);
    }

}
