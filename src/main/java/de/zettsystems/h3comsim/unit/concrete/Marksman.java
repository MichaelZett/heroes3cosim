package de.zettsystems.h3comsim.unit.concrete;

import de.zettsystems.h3comsim.unit.common.AbstractUnit;
import de.zettsystems.h3comsim.unit.common.AttackType;
import de.zettsystems.h3comsim.unit.common.Movement;

import java.util.Set;

import static de.zettsystems.h3comsim.unit.common.UnitSpeciality.TWO_SHOTS;

public class Marksman extends AbstractUnit {

    private final static String NAME = "Marksman";
    private final static int ATTACK = 6;
    private final static int DEFENSE = 3;
    private final static int HEALTH = 10;
    private final static int SPEED = 6;
    private final static int MIN_DAMAGE = 2;
    private final static int MAX_DAMAGE = 3;
    private final static Movement MOVEMENT = Movement.GROUND;
    private final static int SHOTS = 24;
    private final static int COST = 150;
    private final static AttackType ATTACK_TYPE = AttackType.LONG_RANGE;

    public Marksman() {
        super(HEALTH, Set.of(TWO_SHOTS));
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
