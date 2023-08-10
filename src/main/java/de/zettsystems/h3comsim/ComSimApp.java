package de.zettsystems.h3comsim;

import de.zettsystems.h3comsim.combat.CombatEngine;
import de.zettsystems.h3comsim.combat.CombatSetup;
import de.zettsystems.h3comsim.unit.common.Stack;
import de.zettsystems.h3comsim.unit.concrete.Champion;
import de.zettsystems.h3comsim.unit.concrete.DreadKnight;

public class ComSimApp {
    public static void main(String[] args) {
        Stack attacker = Stack.createStack(new Champion(), 1);
        Stack defender = Stack.createStack(new DreadKnight(), 1);
        CombatSetup combatSetup = new CombatSetup(attacker, defender);

        CombatEngine.solveCombat(combatSetup);
    }

}
