package com.eventalert.controller;

import com.eventalert.model.AlertRuleRequest;
import com.eventalert.model.TestNotificationRequest;
import com.eventalert.service.AlertRuleService;
import com.eventalert.service.DeliveryService;
import com.eventalert.view.AlertRuleView;
import com.eventalert.view.DeliveryView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alert-rules")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;
    private final DeliveryService deliveryService;

    public AlertRuleController(AlertRuleService alertRuleService, DeliveryService deliveryService) {
        this.alertRuleService = alertRuleService;
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public List<AlertRuleView> list() {
        return alertRuleService.listForCurrentUser();
    }

    @PostMapping
    public ResponseEntity<AlertRuleView> create(@Valid @RequestBody AlertRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertRuleService.create(request));
    }

    @GetMapping("/{id}")
    public AlertRuleView get(@PathVariable UUID id) {
        return alertRuleService.getOwned(id);
    }

    @PutMapping("/{id}")
    public AlertRuleView update(@PathVariable UUID id, @Valid @RequestBody AlertRuleRequest request) {
        return alertRuleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        alertRuleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Manual trigger — sends through every channel linked to this rule. Useful for
    // testing a rule's channels independent of whether a real event has matched yet.
    @PostMapping("/{id}/test-notification")
    public List<DeliveryView> sendTestNotification(@PathVariable UUID id,
                                                     @Valid @RequestBody TestNotificationRequest request) {
        return deliveryService.sendTestNotification(id, request);
    }

    // Delivery log for this rule — includes both manual test sends and real
    // ingestion-triggered deliveries from the matching engine (M4).
    @GetMapping("/{id}/deliveries")
    public List<DeliveryView> listDeliveries(@PathVariable UUID id) {
        return deliveryService.listForRule(id);
    }
}
