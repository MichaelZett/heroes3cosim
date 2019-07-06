package de.zettsystems.h3comsim.unit.concrete;

import de.zettsystems.h3comsim.unit.common.AbstractUnit;
import de.zettsystems.h3comsim.unit.common.AttackType;
import de.zettsystems.h3comsim.unit.common.Movement;
import de.zettsystems.h3comsim.unit.common.UnitSpeciality;

import java.util.Collections;

public class Thunderbird extends AbstractUnit {
    private final static String NAME = "Thunderbird";
    private final static int ATTACK = 13;
    private final static int DEFENSE = 11;
    private final static int HEALTH = 60;
    private final static int SPEED = 11;
    private final static int MIN_DAMAGE = 11;
    private final static int MAX_DAMAGE = 15;
    private final static Movement MOVEMENT = Movement.FLYING;
    private final static int SHOTS = 0;
    private final static int COST = 700;
    private final static AttackType ATTACK_TYPE = AttackType.HAND_TO_HAND;

    public Thunderbird() {
        super(HEALTH, Collections.singleton(UnitSpeciality.THUNDERBOLTS));
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
