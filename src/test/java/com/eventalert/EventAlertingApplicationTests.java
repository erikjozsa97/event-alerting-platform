package com.eventalert;

import org.junit.jupiter.api.Test;

// Now a Testcontainers-backed context-load smoke test — no manual
// `docker compose up -d postgres` needed before `mvn test`.
class EventAlertingApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
