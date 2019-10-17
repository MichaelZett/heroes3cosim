package de.zettsystems.h3comsim.arena;

import de.zettsystems.h3comsim.unit.common.Unit;

public class Arena {
    private Unit attacker;
    private Unit defender;

    public Arena(Unit attacker, Unit defender) {
        this.attacker = attacker;
        this.defender = defender;
    }

    public Unit getAttacker() {
        return attacker;
    }

    public Unit getDefender() {
        return defender;
    }
}
