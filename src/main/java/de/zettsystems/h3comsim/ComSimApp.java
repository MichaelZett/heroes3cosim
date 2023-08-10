package de.zettsystems.h3comsim;

import de.zettsystems.h3comsim.arena.Arena;
import de.zettsystems.h3comsim.combat.CombatEngine;
import de.zettsystems.h3comsim.unit.common.Stack;
import de.zettsystems.h3comsim.unit.concrete.Champion;
import de.zettsystems.h3comsim.unit.concrete.Crusader;
import de.zettsystems.h3comsim.unit.concrete.DreadKnight;
import de.zettsystems.h3comsim.unit.concrete.Pegasus;
import de.zettsystems.h3comsim.unit.concrete.SilverPegasus;
import de.zettsystems.h3comsim.unit.concrete.Swordsman;
import de.zettsystems.h3comsim.unit.concrete.Zealot;

public class ComSimApp {
    public static void main(String[] args) {
        Stack attacker = Stack.createStack(new Champion(), 1);
        Stack defender = Stack.createStack(new DreadKnight(), 1);
        Arena arena = new Arena(attacker, defender);

        CombatEngine.solveCombat(arena);
    }

}
