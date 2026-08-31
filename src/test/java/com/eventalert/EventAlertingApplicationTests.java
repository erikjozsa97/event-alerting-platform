package com.eventalert;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// NOTE: this loads the full context, which requires a reachable Postgres
// (matching application.yml). Run `docker compose up -d postgres` first.
// Testcontainers-based tests that don't need an external DB arrive in M7.
@SpringBootTest
class EventAlertingApplicationTests {

    @Test
    void contextLoads() {
    }
}
