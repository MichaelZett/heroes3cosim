package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.HeroCatalog;
import de.zettsystems.h3comsim.battle.domain.SecondarySkill;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeroMapperTest {

    @Test
    void maps_primary_values_skills_and_a_creature_specialty() {
        HeroDto dto = HeroMapper.toDto(HeroCatalog.SORSHA);

        assertThat(dto.name()).isEqualTo("Sorsha");
        assertThat(dto.attack()).isEqualTo(2);
        assertThat(dto.defense()).isEqualTo(2);
        assertThat(dto.skills()).containsEntry(SecondarySkill.LEADERSHIP, SkillLevel.BASIC);
        assertThat(dto.specialty()).isEqualTo(new HeroSpecialtyDto(HeroSpecialtyDto.Kind.CREATURE,
                "Swordsman", 4, null, 0, 0, 0, null, true));
    }

    @Test
    void maps_skill_fixed_ballista_and_other_specialties() {
        assertThat(HeroMapper.toDto(HeroCatalog.TAZAR).specialty())
                .isEqualTo(new HeroSpecialtyDto(HeroSpecialtyDto.Kind.SKILL,
                        null, 0, SecondarySkill.ARMORER, 0, 0, 0, null, true));
        assertThat(HeroMapper.toDto(HeroCatalog.FIUR).specialty())
                .isEqualTo(new HeroSpecialtyDto(HeroSpecialtyDto.Kind.FIXED_CREATURE,
                        "Fire Elemental", 4, null, 1, 2, 2, null, true));
        assertThat(HeroMapper.toDto(HeroCatalog.byName("Torosar").orElseThrow()).specialty())
                .isEqualTo(new HeroSpecialtyDto(HeroSpecialtyDto.Kind.BALLISTA,
                        null, 0, null, 0, 0, 0, null, true));
        assertThat(HeroMapper.toDto(HeroCatalog.JENOVA).specialty())
                .isEqualTo(new HeroSpecialtyDto(HeroSpecialtyDto.Kind.OTHER,
                        null, 0, null, 0, 0, 0, "+350 gold per day", false));
    }
}
