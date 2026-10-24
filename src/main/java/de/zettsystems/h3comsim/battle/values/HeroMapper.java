package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.Hero;
import de.zettsystems.h3comsim.battle.domain.HeroSpecialty;

public final class HeroMapper {

    private HeroMapper() {
    }

    public static HeroDto toDto(Hero hero) {
        return new HeroDto(
                hero.name(),
                hero.heroClass(),
                hero.faction(),
                hero.attack(),
                hero.defense(),
                hero.power(),
                hero.knowledge(),
                hero.skills(),
                toDto(hero.specialty()));
    }

    static HeroSpecialtyDto toDto(HeroSpecialty specialty) {
        boolean effective = specialty.effectiveInCombat();
        return switch (specialty) {
            case HeroSpecialty.Creature c -> new HeroSpecialtyDto(HeroSpecialtyDto.Kind.CREATURE,
                    c.creature().name(), c.creature().tier(), null, 0, 0, 0, null, effective);
            case HeroSpecialty.Skill s -> new HeroSpecialtyDto(HeroSpecialtyDto.Kind.SKILL,
                    null, 0, s.skill(), 0, 0, 0, null, effective);
            case HeroSpecialty.FixedCreature f -> new HeroSpecialtyDto(HeroSpecialtyDto.Kind.FIXED_CREATURE,
                    f.creature().name(), f.creature().tier(), null, f.attack(), f.defense(), f.damage(), null, effective);
            case HeroSpecialty.Ballista b -> new HeroSpecialtyDto(HeroSpecialtyDto.Kind.BALLISTA,
                    null, 0, null, 0, 0, 0, null, effective);
            case HeroSpecialty.SpellDamage d -> new HeroSpecialtyDto(HeroSpecialtyDto.Kind.SPELL,
                    null, 0, null, d.fixedPercent(), 0, 0, d.spell().name(), effective);
            case HeroSpecialty.Other o -> new HeroSpecialtyDto(HeroSpecialtyDto.Kind.OTHER,
                    null, 0, null, 0, 0, 0, o.description(), effective);
        };
    }
}
