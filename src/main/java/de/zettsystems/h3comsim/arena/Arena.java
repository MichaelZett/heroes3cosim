package de.zettsystems.h3comsim.arena;

import de.zettsystems.h3comsim.unit.common.Stack;
import de.zettsystems.h3comsim.unit.common.Unit;

public class Arena {
    private Stack<Unit> attacker;
    private Stack<Unit> defender;

    public Arena(Stack<Unit> attacker, Stack<Unit> defender) {
        this.attacker = attacker;
        this.defender = defender;
    }

    public Stack<Unit> getAttacker() {
        return attacker;
    }

    public Stack<Unit> getDefender() {
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
}
