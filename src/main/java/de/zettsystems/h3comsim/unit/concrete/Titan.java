package de.zettsystems.h3comsim.unit.concrete;

import de.zettsystems.h3comsim.unit.common.AbstractUnit;
import de.zettsystems.h3comsim.unit.common.AttackType;
import de.zettsystems.h3comsim.unit.common.Movement;

import java.util.Collections;

import static de.zettsystems.h3comsim.unit.common.UnitSpeciality.NO_HAND_TO_HAND_PENALTY;

public class Titan extends AbstractUnit {

    private final static String NAME = "Titan";
    private final static int ATTACK = 24;
    private final static int DEFENSE = 24;
    private final static int HEALTH = 300;
    private final static int SPEED = 11;
    private final static int MIN_DAMAGE = 40;
    private final static int MAX_DAMAGE = 60;
    private final static Movement MOVEMENT = Movement.GROUND;
    private final static int SHOTS = 24;
    private final static int COST = 5000;
    private final static AttackType ATTACK_TYPE = AttackType.LONG_RANGE;

    public Titan() {
        super(HEALTH, Collections.singleton(NO_HAND_TO_HAND_PENALTY));
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
