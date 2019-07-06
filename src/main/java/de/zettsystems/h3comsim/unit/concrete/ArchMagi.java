package de.zettsystems.h3comsim.unit.concrete;

import de.zettsystems.h3comsim.unit.common.AbstractUnit;
import de.zettsystems.h3comsim.unit.common.AttackType;
import de.zettsystems.h3comsim.unit.common.Movement;

import java.util.Collections;

import static de.zettsystems.h3comsim.unit.common.UnitSpeciality.NO_HAND_TO_HAND_PENALTY;

public class ArchMagi extends AbstractUnit {
    // TODO
//    Arch mage units suffer no  damage penalty for attacking adjacent enemies and reduce the casting cost of allied hero spells by two.
//    Arch mage attacks penetrate cover and deal full damage to enemies behind siege walls.

    private final static String NAME = "Arch Magi";
    private final static int ATTACK = 12;
    private final static int DEFENSE = 9;
    private final static int HEALTH = 30;
    private final static int SPEED = 7;
    private final static int MIN_DAMAGE = 7;
    private final static int MAX_DAMAGE = 9;
    private final static Movement MOVEMENT = Movement.GROUND;
    private final static int SHOTS = 24;
    private final static int COST = 350;
    private final static AttackType ATTACK_TYPE = AttackType.LONG_RANGE;

    public ArchMagi() {
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
