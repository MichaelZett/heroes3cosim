package de.zettsystems.h3comsim.setup.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CatalogControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void list_units_returns_full_catalog() throws Exception {
        mockMvc().perform(get("/api/units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Pikeman')]").exists())
                .andExpect(jsonPath("$[?(@.name=='Phoenix')]").exists())
                .andExpect(jsonPath("$[?(@.name=='Ghost Dragon')].specialities[?(@=='AGING')]").exists());
    }

    @Test
    void list_artifacts_returns_the_combat_artifacts_with_their_effects() throws Exception {
        mockMvc().perform(get("/api/artifacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(94))
                .andExpect(jsonPath("$[?(@.id=='SWORD_OF_HELLFIRE')].attack").value(6))
                .andExpect(jsonPath("$[?(@.id=='ELIXIR_OF_LIFE')].blocks[0]").value("RING"))
                .andExpect(jsonPath("$[?(@.id=='PENDANT_OF_COURAGE')].effects[0].type").value("LUCK"))
                .andExpect(jsonPath("$[?(@.id=='ARMOR_OF_THE_DAMNED')].effects[0].spells[0]").value("SLOW"));
    }

    @Test
    void list_heroes_returns_heroes_without_artifacts() throws Exception {
        mockMvc().perform(get("/api/heroes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Crag Hack')].artifacts[0]").doesNotExist());
    }

    @Test
    void list_creature_banks_returns_levels_and_guards() throws Exception {
        mockMvc().perform(get("/api/creature-banks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11))
                .andExpect(jsonPath("$[?(@.id=='CYCLOPS_STOCKPILE')].levels[3].chance").value(10))
                .andExpect(jsonPath("$[?(@.id=='CYCLOPS_STOCKPILE')].levels[0].guards[2].upgradeName")
                        .value("Cyclops King"))
                .andExpect(jsonPath("$[?(@.id=='CRYPT')].levels[0].guards[0].unitName").value("Skeleton"));
    }

    @Test
    void list_factions_returns_all_ten_factions() throws Exception {
        mockMvc().perform(get("/api/factions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10));
    }
}
