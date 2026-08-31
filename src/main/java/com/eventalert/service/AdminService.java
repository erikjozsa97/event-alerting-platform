package com.eventalert.service;

import com.eventalert.model.AlertRule;
import com.eventalert.model.Category;
import com.eventalert.model.DeliveryStatus;
import com.eventalert.model.User;
import com.eventalert.repository.AlertRuleRepository;
import com.eventalert.repository.DeliveryRepository;
import com.eventalert.repository.EventRepository;
import com.eventalert.repository.UserRepository;
import com.eventalert.view.AdminAlertRuleView;
import com.eventalert.view.AdminDeliveryView;
import com.eventalert.view.EventView;
import com.eventalert.view.UserView;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final EventRepository eventRepository;
    private final DeliveryRepository deliveryRepository;

    public AdminService(UserRepository userRepository,
                         AlertRuleRepository alertRuleRepository,
                         EventRepository eventRepository,
                         DeliveryRepository deliveryRepository) {
        this.userRepository = userRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.eventRepository = eventRepository;
        this.deliveryRepository = deliveryRepository;
    }

    public List<UserView> listUsers() {
        return userRepository.findAll().stream().map(UserView::from).toList();
    }

    public List<AdminAlertRuleView> listAlertRules() {
        Map<UUID, String> emailByUserId = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));

        return alertRuleRepository.findAll().stream()
                .map(rule -> AdminAlertRuleView.from(
                        rule,
                        emailByUserId.get(rule.getUserId()),
                        alertRuleRepository.findChannelIds(rule.getId())))
                .toList();
    }

    public List<EventView> listEvents(Category category, OffsetDateTime since) {
        return eventRepository.findAll(category, since).stream().map(EventView::from).toList();
    }

    public List<AdminDeliveryView> listDeliveries(DeliveryStatus status, OffsetDateTime since) {
        Map<UUID, UUID> userIdByRuleId = alertRuleRepository.findAll().stream()
                .collect(Collectors.toMap(AlertRule::getId, AlertRule::getUserId));

        return deliveryRepository.findAll(status, since).stream()
                .map(delivery -> AdminDeliveryView.from(delivery, userIdByRuleId.get(delivery.getAlertRuleId())))
                .toList();
    }
}
