package de.zettsystems.h3comsim.unit.concrete;

import de.zettsystems.h3comsim.unit.common.AbstractUnit;
import de.zettsystems.h3comsim.unit.common.AttackType;
import de.zettsystems.h3comsim.unit.common.Movement;
import de.zettsystems.h3comsim.unit.common.UnitSpeciality;

import java.util.Set;

import static de.zettsystems.h3comsim.unit.common.UnitSpeciality.MOVE_BACK;
import static de.zettsystems.h3comsim.unit.common.UnitSpeciality.NO_RETALIATION;

public class Harpy extends AbstractUnit {

    private final static String NAME = "Harpies";
    private final static int ATTACK = 6;
    private final static int DEFENSE = 5;
    private final static int HEALTH = 14;
    private final static int SPEED = 6;
    private final static int MIN_DAMAGE = 1;
    private final static int MAX_DAMAGE = 4;
    private final static Movement MOVEMENT = Movement.FLYING;
    private final static int SHOTS = 0;
    private final static int COST = 130;
    private final static AttackType ATTACK_TYPE = AttackType.HAND_TO_HAND;

    public Harpy() {
        super(HEALTH, Set.of(MOVE_BACK, NO_RETALIATION));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getAttack() {
        return ATTACK;
    }

    @Override
    public int getDefense() {
        return DEFENSE;
    }

    @Override
    public int getHealth() {
        return HEALTH;
    }

    @Override
    public int getSpeed() {
        return SPEED;
    }

    @Override
    public int getMinDamage() {
        return MIN_DAMAGE;
    }

    @Override
    public int getMaxDamage() {
        return MAX_DAMAGE;
    }

    @Override
    public Movement getMovement() {
        return MOVEMENT;
    }

    @Override
    public int getShots() {
        return SHOTS;
    }

    @Override
    public int getCost() {
        return COST;
    }

    @Override
    public AttackType getAttackType() {
        return ATTACK_TYPE;
    }
}
