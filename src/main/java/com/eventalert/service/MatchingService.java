package com.eventalert.service;

import com.eventalert.model.AlertRule;
import com.eventalert.model.Event;
import com.eventalert.repository.AlertRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final AlertRuleRepository alertRuleRepository;
    private final EventMatcherDispatcher matcherDispatcher;
    private final DeliveryService deliveryService;

    public MatchingService(AlertRuleRepository alertRuleRepository,
                            EventMatcherDispatcher matcherDispatcher,
                            DeliveryService deliveryService) {
        this.alertRuleRepository = alertRuleRepository;
        this.matcherDispatcher = matcherDispatcher;
        this.deliveryService = deliveryService;
    }

    public void processNewEvent(Event event) {
        EventMatcher matcher = matcherDispatcher.get(event.getCategory());

        for (AlertRule rule : alertRuleRepository.findActiveByCategory(event.getCategory())) {
            try {
                if (matcher.matches(rule.getCriteria(), event.getPayload())) {
                    deliveryService.deliverForEvent(rule, event);
                }
            } catch (Exception e) {
                // One rule with unexpected/legacy criteria shouldn't stop matching
                // for the rest of this event, or for the rest of this poll cycle.
                log.warn("Skipping rule {} while matching event {}: {}", rule.getId(), event.getId(), e.getMessage());
            }
        }
    }
}
