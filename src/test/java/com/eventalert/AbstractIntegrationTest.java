package com.eventalert;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// @ServiceConnection auto-wires spring.datasource.* to point at this container — no
// manual property overrides needed, and Flyway runs both migrations against it on
// context startup exactly like it would against the real docker-compose Postgres.
// Each subclass gets its own container (not a shared singleton) — simpler, at the
// cost of a slightly slower test suite; worth revisiting if this suite grows a lot.
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
}
