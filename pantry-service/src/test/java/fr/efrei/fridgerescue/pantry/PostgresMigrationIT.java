package fr.efrei.fridgerescue.pantry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** Vérifie les migrations Flyway et le mapping JPA sur un vrai PostgreSQL (même version qu'en cluster). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.12-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    @Test
    void appliesMigrationsAndServesDemoData() throws Exception {
        assertThat(jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from demo_seed", Integer.class)).isEqualTo(1);
        mvc.perform(get("/api/items/stats")).andExpect(jsonPath("$.total").value(9));
    }
}
