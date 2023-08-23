package de.zettsystems.h3comsim.unit.common;

import static de.zettsystems.h3comsim.unit.common.UnitSpecialityType.AFTER_ATTACK;
import static de.zettsystems.h3comsim.unit.common.UnitSpecialityType.ATTACK;
import static de.zettsystems.h3comsim.unit.common.UnitSpecialityType.DEFENSE;
import static de.zettsystems.h3comsim.unit.common.UnitSpecialityType.SPECIAL;

//https://heroes.thelazy.net/index.php/Special_ability
public enum UnitSpeciality {
    NO_RETALIATION(DEFENSE), DEATH_STARE(AFTER_ATTACK), THUNDERBOLTS(AFTER_ATTACK), PETRYFYING(AFTER_ATTACK), CURSING(AFTER_ATTACK),
    DEATH_BLOW(AFTER_ATTACK), DEVIL_HATE(ATTACK), ANGEL_HATE((ATTACK)),
    // TODO:
    NO_HAND_TO_HAND_PENALTY(SPECIAL), RESURRECTION(SPECIAL), MORALE(SPECIAL),
    NO_OBSTACLE_PENALTY(SPECIAL), SPELL_COST_REDUCTION(SPECIAL);

    private UnitSpecialityType unitSpecialityType;

    private UnitSpeciality(UnitSpecialityType unitSpecialityType) {
        this.unitSpecialityType = unitSpecialityType;
    }

    public boolean isAttack() {
        return this.unitSpecialityType == UnitSpecialityType.ATTACK;
    }
}
