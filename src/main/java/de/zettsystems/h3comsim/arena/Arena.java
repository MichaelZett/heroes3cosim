package de.zettsystems.h3comsim.arena;

import de.zettsystems.h3comsim.unit.common.Stack;

public class Arena {
    private Stack attacker;
    private Stack defender;

    public Arena(Stack attacker, Stack defender) {
        this.attacker = attacker;
        this.defender = defender;
    }

    public Stack getAttacker() {
        return attacker;
    }

    public Stack getDefender() {
        return defender;
    }

    public String getAttackerName() {
        return this.attacker.getName();
    }

    public String getDefenderName() {
        return this.defender.getName();
    }

    public boolean isAttackerAlive() {
        return this.attacker.isAlive();
    }

    public boolean isDefenderAlive() {
        return this.defender.isAlive();
    }

    public boolean bothAlive() {
        return isAttackerAlive() && isDefenderAlive();
    }

    public int getAttackerCount() {
        return this.attacker.getCount();
    }

    public int getDefenderCount() {
        return this.defender.getCount();
    }
}
