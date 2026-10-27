package de.zettsystems.h3comsim.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpaForwardingControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SpaForwardingController()).build();

    @ParameterizedTest
    @ValueSource(strings = {"/battle", "/matrix", "/matrix/result", "/factions", "/army", "/army/battle"})
    void every_client_route_is_served_by_the_app(String route) throws Exception {
        mockMvc.perform(get(route))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }
}
