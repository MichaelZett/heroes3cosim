package de.zettsystems.h3comsim;

import de.zettsystems.h3comsim.arena.Arena;
import de.zettsystems.h3comsim.unit.ArchMagi;
import de.zettsystems.h3comsim.unit.MedusaQueen;

public class ComSimApp {
    public static void main(String[] args) {
        new Arena(new MedusaQueen(), new ArchMagi()).startCombat();
    }
}