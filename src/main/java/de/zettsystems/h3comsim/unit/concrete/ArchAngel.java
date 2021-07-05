package de.zettsystems.h3comsim.unit.concrete;

import de.zettsystems.h3comsim.unit.common.AbstractUnit;
import de.zettsystems.h3comsim.unit.common.AttackType;
import de.zettsystems.h3comsim.unit.common.Movement;

import java.util.Collections;

import static de.zettsystems.h3comsim.unit.common.UnitSpeciality.NO_HAND_TO_HAND_PENALTY;

public class ArchAngel extends AbstractUnit {

    private final static String NAME = "Arch Angel";
    private final static int ATTACK = 30;
    private final static int DEFENSE = 30;
    private final static int HEALTH = 250;
    private final static int SPEED = 18;
    private final static int MIN_DAMAGE = 50;
    private final static int MAX_DAMAGE = 50;
    private final static Movement MOVEMENT = Movement.FLYING;
    private final static int SHOTS = 0;
    private final static int COST = 5000;
    private final static AttackType ATTACK_TYPE = AttackType.HAND_TO_HAND;

    public ArchAngel() {
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
