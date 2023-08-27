package de.zettsystems.h3comsim;

import de.zettsystems.h3comsim.combat.CombatEngine;
import de.zettsystems.h3comsim.combat.CombatSetup;
import de.zettsystems.h3comsim.unit.common.Stack;
import de.zettsystems.h3comsim.unit.concrete.ArchAngel;
import de.zettsystems.h3comsim.unit.concrete.GrandElf;

public class ComSimApp {

    public static void main(String[] args) {
        Stack attacker = Stack.createStack(new GrandElf(), 10);
        Stack defender = Stack.createStack(new ArchAngel(), 1);
        CombatSetup combatSetup = new CombatSetup(attacker, defender);

        CombatEngine.solveCombat(combatSetup);
        // Done: Castle, Dungeon
    }

}
