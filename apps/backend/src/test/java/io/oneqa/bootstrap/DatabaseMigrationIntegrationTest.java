package io.oneqa.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = OneQaApplication.class)
class DatabaseMigrationIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesInitialMigration() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = true",
            Integer.class
        );
        assertEquals(1, count);
        Integer tableCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'schema_version'",
            Integer.class
        );
        assertEquals(1, tableCount);
    }
}
