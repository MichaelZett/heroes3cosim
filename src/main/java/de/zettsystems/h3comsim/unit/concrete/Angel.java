package de.zettsystems.h3comsim.unit.concrete;

import de.zettsystems.h3comsim.unit.common.AbstractUnit;
import de.zettsystems.h3comsim.unit.common.AttackType;
import de.zettsystems.h3comsim.unit.common.Movement;

import java.util.Set;

import static de.zettsystems.h3comsim.unit.common.UnitSpeciality.DEVIL_HATE;
import static de.zettsystems.h3comsim.unit.common.UnitSpeciality.GOOD_ARMY_MORALE;
import static de.zettsystems.h3comsim.unit.common.UnitSpeciality.RESURRECTION;

public class Angel extends AbstractUnit {

    private final static String NAME = "Angel";
    private final static int ATTACK = 20;
    private final static int DEFENSE = 20;
    private final static int HEALTH = 200;
    private final static int SPEED = 12;
    private final static int MIN_DAMAGE = 50;
    private final static int MAX_DAMAGE = 50;
    private final static Movement MOVEMENT = Movement.FLYING;
    private final static int SHOTS = 0;
    private final static int COST = 3000;
    private final static AttackType ATTACK_TYPE = AttackType.HAND_TO_HAND;

    public Angel() {
        super(HEALTH, Set.of(RESURRECTION, GOOD_ARMY_MORALE, DEVIL_HATE));
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
