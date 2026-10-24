package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.HeroCatalog;
import de.zettsystems.h3comsim.battle.domain.SecondarySkill;
import de.zettsystems.h3comsim.battle.domain.SkillLevel;
import de.zettsystems.h3comsim.battle.domain.Spell;
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

    @Test
    void maps_spell_specialties_and_starting_spells() {
        HeroDto ciele = HeroMapper.toDto(HeroCatalog.byName("Ciele").orElseThrow());
        assertThat(ciele.specialty()).isEqualTo(new HeroSpecialtyDto(HeroSpecialtyDto.Kind.SPELL,
                null, 0, null, 50, 0, 0, "MAGIC_ARROW", true));
        assertThat(ciele.spells()).contains(Spell.MAGIC_ARROW).hasSize(5);

        HeroDto adela = HeroMapper.toDto(HeroCatalog.byName("Adela").orElseThrow());
        assertThat(adela.specialty()).isEqualTo(new HeroSpecialtyDto(HeroSpecialtyDto.Kind.SPELL_UNIQUE,
                null, 0, null, 0, 0, 0, "BLESS", true));
        assertThat(adela.spells()).contains(Spell.BLESS);
        assertThat(HeroMapper.toDto(HeroCatalog.SORSHA).spells()).isEmpty();
    }
}
