package de.zettsystems.h3comsim.singlebattle.ui;

import de.zettsystems.h3comsim.singlebattle.values.BattleConfigRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BattleControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void simulate_returns_result_and_events_with_discriminators() throws Exception {
        BattleConfigRequest request = new BattleConfigRequest("Arch Angel", 5, "Peasant", 1, 42L);

        MvcResult result = mockMvc().perform(post("/api/battles/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.winner").value("ATTACKER"))
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.events[0].type").value("BattleStart"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        JsonNode events = body.get("events");
        assertThat(events.size()).isPositive();
        JsonNode lastEvent = events.get(events.size() - 1);
        assertThat(lastEvent.get("type").asString()).isEqualTo("BattleEnd");
        assertThat(lastEvent.get("winner").asString()).isEqualTo("ATTACKER");
    }

    @Test
    void simulate_with_unknown_attacker_returns_bad_request() throws Exception {
        BattleConfigRequest request = new BattleConfigRequest("NotAUnit", 5, "Peasant", 1, 1L);

        mockMvc().perform(post("/api/battles/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simulate_with_zero_count_returns_bad_request() throws Exception {
        BattleConfigRequest request = new BattleConfigRequest("Pikeman", 0, "Peasant", 1, 1L);

        mockMvc().perform(post("/api/battles/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simulate_without_seed_still_succeeds() throws Exception {
        BattleConfigRequest request = new BattleConfigRequest("Arch Angel", 5, "Peasant", 1, null);

        mockMvc().perform(post("/api/battles/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.winner").value("ATTACKER"));
    }
}
