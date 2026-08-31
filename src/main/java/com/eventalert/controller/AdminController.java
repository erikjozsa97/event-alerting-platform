package com.eventalert.controller;

import com.eventalert.service.IngestionScheduler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final IngestionScheduler ingestionScheduler;

    public AdminController(IngestionScheduler ingestionScheduler) {
        this.ingestionScheduler = ingestionScheduler;
    }

    // Triggers an immediate poll of every EventSource instead of waiting for the
    // scheduled interval — mainly for testing/demoing ingestion + matching on demand.
    // Returns how many new (post-dedup) events each source produced this run.
    @PostMapping("/ingestion/poll-now")
    public Map<String, Integer> pollNow() {
        return ingestionScheduler.pollAllSourcesNow();
    }
}
