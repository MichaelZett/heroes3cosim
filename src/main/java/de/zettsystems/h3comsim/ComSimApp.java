package de.zettsystems.h3comsim;

import de.zettsystems.h3comsim.arena.Arena;
import de.zettsystems.h3comsim.unit.concrete.DreadKnight;
import de.zettsystems.h3comsim.unit.concrete.NagaQueen;

public class ComSimApp {
    public static void main(String[] args) {
        new Arena(new DreadKnight(), new NagaQueen()).startCombat();
    }
}
