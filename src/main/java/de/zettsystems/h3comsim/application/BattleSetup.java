package de.zettsystems.h3comsim.application;

import de.zettsystems.h3comsim.domain.Battlefield;
import de.zettsystems.h3comsim.domain.Hex;
import de.zettsystems.h3comsim.domain.Stack;
import de.zettsystems.h3comsim.domain.Unit;

public class BattleSetup {

    private static final Hex DEFAULT_ATTACKER_POSITION = new Hex(0, 5);
    private static final Hex DEFAULT_DEFENDER_POSITION = new Hex(14, 5);

    private final Stack attacker;
    private final Stack defender;
    private final Battlefield battlefield;

    public BattleSetup(Unit attackerUnit, int attackerCount,
                       Unit defenderUnit, int defenderCount) {
        this(attackerUnit, attackerCount, defenderUnit, defenderCount,
                Battlefield.STANDARD, DEFAULT_ATTACKER_POSITION, DEFAULT_DEFENDER_POSITION);
    }

    public BattleSetup(Unit attackerUnit, int attackerCount,
                       Unit defenderUnit, int defenderCount,
                       Battlefield battlefield, Hex attackerPosition, Hex defenderPosition) {
        this.battlefield = battlefield;
        this.attacker = new Stack(attackerUnit, attackerCount, attackerPosition);
        this.defender = new Stack(defenderUnit, defenderCount, defenderPosition);
    }

    public Stack getAttacker() {
        return attacker;
    }

    public Stack getDefender() {
        return defender;
    }

    public Battlefield battlefield() {
        return battlefield;
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
