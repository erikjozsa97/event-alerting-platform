package com.eventalert.controller;

import com.eventalert.model.Category;
import com.eventalert.model.DeliveryStatus;
import com.eventalert.service.AdminService;
import com.eventalert.service.IngestionScheduler;
import com.eventalert.view.AdminAlertRuleView;
import com.eventalert.view.AdminDeliveryView;
import com.eventalert.view.EventView;
import com.eventalert.view.SourceStatusView;
import com.eventalert.view.UserView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final IngestionScheduler ingestionScheduler;
    private final AdminService adminService;

    public AdminController(IngestionScheduler ingestionScheduler, AdminService adminService) {
        this.ingestionScheduler = ingestionScheduler;
        this.adminService = adminService;
    }

    // Triggers an immediate poll of every EventSource instead of waiting for the
    // scheduled interval — mainly for testing/demoing ingestion + matching on demand.
    // Returns how many new (post-dedup) events each source produced this run.
    @PostMapping("/ingestion/poll-now")
    public Map<String, Integer> pollNow() {
        return ingestionScheduler.pollAllSourcesNow();
    }

    @GetMapping("/users")
    public List<UserView> listUsers() {
        return adminService.listUsers();
    }

    @GetMapping("/alert-rules")
    public List<AdminAlertRuleView> listAlertRules() {
        return adminService.listAlertRules();
    }

    @GetMapping("/events")
    public List<EventView> listEvents(@RequestParam(required = false) Category category,
                                       @RequestParam(required = false) OffsetDateTime since) {
        return adminService.listEvents(category, since);
    }

    @GetMapping("/deliveries")
    public List<AdminDeliveryView> listDeliveries(@RequestParam(required = false) DeliveryStatus status,
                                                    @RequestParam(required = false) OffsetDateTime since) {
        return adminService.listDeliveries(status, since);
    }

    @GetMapping("/sources/status")
    public List<SourceStatusView> sourceStatus() {
        return ingestionScheduler.getSourceStatuses();
    }
}
