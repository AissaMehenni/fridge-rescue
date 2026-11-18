package fr.efrei.fridgerescue.pantry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "fridge.demo-data=false")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FoodItemApiTest {

    /** Date figée : 2026-01-10. */
    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-01-10T08:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    MockMvc mvc;

    private String create(String name, String expiresOn) throws Exception {
        String body = """
                {"name":"%s","quantity":2,"unit":"pcs","category":"VEGETABLE","expiresOn":"%s"}
                """.formatted(name, expiresOn);
        return mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/items/")))
                .andReturn().getResponse().getHeader("Location");
    }

    @Test
    void createsAndListsItemsSortedByExpiry() throws Exception {
        create("Carotte", "2026-01-20");
        create("Tomate", "2026-01-11");

        mvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Tomate"))
                .andExpect(jsonPath("$[0].daysLeft").value(1))
                .andExpect(jsonPath("$[0].freshness").value("URGENT"))
                .andExpect(jsonPath("$[1].freshness").value("FRESH"));
    }

    @Test
    void filtersExpiringItemsAndComputesStats() throws Exception {
        create("Lait", "2026-01-09");
        create("Courgette", "2026-01-12");
        create("Riz", "2026-06-01");

        mvc.perform(get("/api/items/expiring").param("days", "3"))
                .andExpect(jsonPath("$[*].name", contains("Lait", "Courgette")));

        mvc.perform(get("/api/items/stats"))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.expired").value(1))
                .andExpect(jsonPath("$.soon").value(1))
                .andExpect(jsonPath("$.fresh").value(1));
    }

    @Test
    void updatesAndDeletesAnItem() throws Exception {
        String location = create("Poireau", "2026-01-15");
        mvc.perform(put(location).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Poireau bio","quantity":3,"unit":"pcs","category":"VEGETABLE","expiresOn":"2026-01-16"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Poireau bio"));

        mvc.perform(delete(location)).andExpect(status().isNoContent());
        mvc.perform(get(location))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Aliment introuvable"));
    }

    @Test
    void rejectsInvalidPayload() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"quantity\":-1,\"unit\":\"pcs\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesKubernetesProbes() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/readiness")).andExpect(jsonPath("$.status").value("UP"));
    }
}
