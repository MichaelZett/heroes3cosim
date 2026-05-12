package de.zettsystems.h3comsim.adapter.web;

import com.jayway.jsonpath.JsonPath;
import de.zettsystems.h3comsim.adapter.web.dto.MatrixRequestDto;
import de.zettsystems.h3comsim.application.experiment.StackSizingMode;
import de.zettsystems.h3comsim.domain.Faction;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ExperimentControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void matrix_post_returns_running_job_then_get_yields_completed_report() throws Exception {
        // Mini-Setup: nur Castle + Rampart zulassen, 1 Seed pro Matchup — schnell.
        MatrixRequestDto request = new MatrixRequestDto(
                20,
                Set.of(),
                Set.of(Faction.TOWER, Faction.INFERNO, Faction.NECROPOLIS, Faction.DUNGEON,
                        Faction.STRONGHOLD, Faction.FORTRESS, Faction.CONFLUX, Faction.NEUTRAL),
                Set.of(),
                StackSizingMode.EQUAL_COUNT,
                1);

        MvcResult startResult = mockMvc().perform(post("/api/experiments/matrix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").isString())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.status").isString())
                .andReturn();

        String jobId = JsonPath.read(startResult.getResponse().getContentAsString(), "$.jobId");
        int total = JsonPath.read(startResult.getResponse().getContentAsString(), "$.total");
        // 28 participants → 28*27/2 = 378 Match-ups × 1 Seed × 2 Rollen.
        assertThat(total).isEqualTo(378 * 2);

        Awaitility.await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> mockMvc().perform(get("/api/experiments/matrix/" + jobId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("COMPLETED")));

        mockMvc().perform(get("/api/experiments/matrix/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.stats").isArray())
                .andExpect(jsonPath("$.report.stats.length()").value(28))
                .andExpect(jsonPath("$.report.factionStats").isArray())
                .andExpect(jsonPath("$.report.factionStats.length()").value(2))
                .andExpect(jsonPath("$.report.anomalies").isArray())
                .andExpect(jsonPath("$.report.seedsPerMatchup").value(1))
                .andExpect(jsonPath("$.report.unitCount").value(20));
    }

    @Test
    void matrix_endpoint_rejects_zero_seeds() throws Exception {
        MatrixRequestDto request = new MatrixRequestDto(20, Set.of(), Set.of(), Set.of(), StackSizingMode.EQUAL_COUNT, 0);

        mockMvc().perform(post("/api/experiments/matrix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void matrix_endpoint_uses_defaults_when_fields_are_omitted() throws Exception {
        // Setze nur excludeFactions damit der Lauf schnell bleibt.
        String body = """
                {"excludeFactions":["TOWER","INFERNO","NECROPOLIS","DUNGEON","STRONGHOLD","FORTRESS","CONFLUX","NEUTRAL"],"seedsPerMatchup":1}
                """;

        MvcResult startResult = mockMvc().perform(post("/api/experiments/matrix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        String jobId = JsonPath.read(startResult.getResponse().getContentAsString(), "$.jobId");

        Awaitility.await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> mockMvc().perform(get("/api/experiments/matrix/" + jobId))
                        .andExpect(jsonPath("$.status").value("COMPLETED")));

        mockMvc().perform(get("/api/experiments/matrix/" + jobId))
                .andExpect(jsonPath("$.report.unitCount").value(20));
    }

    @Test
    void unknown_job_id_returns_404() throws Exception {
        mockMvc().perform(get("/api/experiments/matrix/does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
