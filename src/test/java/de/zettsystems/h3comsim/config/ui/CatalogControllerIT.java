package de.zettsystems.h3comsim.config.ui;

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
    void list_factions_returns_all_ten_factions() throws Exception {
        mockMvc().perform(get("/api/factions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10));
    }
}
