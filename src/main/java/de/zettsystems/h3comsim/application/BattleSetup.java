package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.Stack;

public class BattleSetup {

    private final Stack attacker;
    private final Stack defender;

    public BattleSetup(Stack attacker, Stack defender) {
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
        return attacker.getName();
    }

    public String getDefenderName() {
        return defender.getName();
    }

    public boolean isAttackerAlive() {
        return attacker.isAlive();
    }

    public boolean isDefenderAlive() {
        return defender.isAlive();
    }

    public boolean bothAlive() {
        return isAttackerAlive() && isDefenderAlive();
    }

    public int getAttackerCount() {
        return attacker.getCount();
    }

    public int getDefenderCount() {
        return defender.getCount();
    }

    public Stack getTarget(Stack activeStack) {
        return attacker.equals(activeStack) ? defender : attacker;
    }
}
