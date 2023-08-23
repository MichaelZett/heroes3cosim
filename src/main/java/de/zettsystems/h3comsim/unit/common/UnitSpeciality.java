package de.zettsystems.h3comsim.unit.common;

import static de.zettsystems.h3comsim.unit.common.UnitSpecialityType.AFTER_ATTACK;
import static de.zettsystems.h3comsim.unit.common.UnitSpecialityType.ATTACK;
import static de.zettsystems.h3comsim.unit.common.UnitSpecialityType.DEFENSE;
import static de.zettsystems.h3comsim.unit.common.UnitSpecialityType.SPECIAL;

//https://heroes.thelazy.net/index.php/Special_ability
public enum UnitSpeciality {
    NO_RETALIATION(DEFENSE), DEATH_STARE(AFTER_ATTACK), THUNDERBOLTS(AFTER_ATTACK), PETRYFYING(AFTER_ATTACK), CURSING(AFTER_ATTACK),
    DEATH_BLOW(AFTER_ATTACK), DEVIL_HATE(ATTACK), ANGEL_HATE((ATTACK)), NO_HAND_TO_HAND_PENALTY(ATTACK), TWO_BLOWS(ATTACK),
    // TODO
    // programming
    IMMUNE_TO_BLIND(DEFENSE),


    // BattleField fehlt
    IMPACT_DAMAGE(ATTACK), NO_OBSTACLE_PENALTY(SPECIAL), TWO_SHOTS(ATTACK), MOVE_BACK(SPECIAL),
    // Mehrere Stacks fehlen
    RESURRECTION(SPECIAL), COUNTERSTRIKE_TWICE(DEFENSE), COUNERSTRIKE_UNLIMITED(DEFENSE),
    // Hero fehlt
    MORALE(SPECIAL), SPELL_COST_REDUCTION(SPECIAL),


    ;

    private UnitSpecialityType unitSpecialityType;

    private UnitSpeciality(UnitSpecialityType unitSpecialityType) {
        this.unitSpecialityType = unitSpecialityType;
    }

    public boolean isAttack() {
        return this.unitSpecialityType == UnitSpecialityType.ATTACK;
    }
}
