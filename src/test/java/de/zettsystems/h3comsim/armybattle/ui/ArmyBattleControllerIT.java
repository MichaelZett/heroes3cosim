package de.zettsystems.h3comsim.armybattle.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ArmyBattleControllerIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private static String request(String attackerExtra) {
        return "{\"attacker\": {\"stacks\": [{\"unitName\": \"Pikeman\", \"count\": 20}]"
                + attackerExtra
                + "}, \"defender\": {\"stacks\": [{\"unitName\": \"Imp\", \"count\": 20}]}, \"seed\": 7}";
    }

    @Test
    void every_army_gets_a_hero_and_the_collected_bonuses_reach_the_battle() throws Exception {
        mockMvc().perform(post("/api/army-battles/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(", \"luckBonus\": 2, \"moraleBonus\": -1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].type").value("BattleStart"))
                .andExpect(jsonPath("$.events[0].attackerHero").value("Sorsha"))
                .andExpect(jsonPath("$.events[0].defenderHero").value("Nymus"))
                .andExpect(jsonPath("$.events[0].attackerLuck").value(2))
                .andExpect(jsonPath("$.events[0].defenderLuck").value(0))
                // Ein-Stadt-Armee +1, Sorshas Basic Leadership +1, gesammelte Moral −1.
                .andExpect(jsonPath("$.events[0].attackerMorale").value(1));
    }

    @Test
    void a_luck_bonus_above_three_is_rejected() throws Exception {
        mockMvc().perform(post("/api/army-battles/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(", \"luckBonus\": 4")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void a_hero_level_outside_one_to_ninety_nine_is_rejected() throws Exception {
        for (String level : new String[]{"0", "100"}) {
            mockMvc().perform(post("/api/army-battles/simulate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request(", \"heroLevel\": " + level)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void a_morale_bonus_below_minus_three_is_rejected() throws Exception {
        mockMvc().perform(post("/api/army-battles/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(", \"moraleBonus\": -4")))
                .andExpect(status().isBadRequest());
    }
}
