package de.zettsystems.h3comsim.battle.values;

import de.zettsystems.h3comsim.battle.domain.Faction;
import de.zettsystems.h3comsim.battle.domain.UnitCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnitMapperTest {

    @Test
    void maps_unit_fields_to_dto() {
        UnitDto dto = UnitMapper.toDto(UnitCatalog.PIKEMAN);

        assertThat(dto.name()).isEqualTo("Pikeman");
        assertThat(dto.faction()).isEqualTo(Faction.CASTLE);
        assertThat(dto.attack()).isEqualTo(4);
        assertThat(dto.defense()).isEqualTo(5);
        assertThat(dto.health()).isEqualTo(10);
        assertThat(dto.cost()).isEqualTo(60);
    }

    @Test
    void specialities_are_serialized_as_string_names() {
        UnitDto dto = UnitMapper.toDto(UnitCatalog.GHOST_DRAGON);

        assertThat(dto.specialities()).contains("AGING");
    }
}
