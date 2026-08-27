package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.Hero;

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
                hero.skills());
    }
}
