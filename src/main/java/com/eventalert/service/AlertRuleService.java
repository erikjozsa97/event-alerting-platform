package com.eventalert.service;

import com.eventalert.exception.AlertRuleNotFoundException;
import com.eventalert.exception.ChannelNotFoundException;
import com.eventalert.model.AlertRule;
import com.eventalert.model.AlertRuleRequest;
import com.eventalert.model.Channel;
import com.eventalert.repository.AlertRuleRepository;
import com.eventalert.repository.ChannelRepository;
import com.eventalert.security.CurrentUserService;
import com.eventalert.view.AlertRuleView;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final ChannelRepository channelRepository;
    private final CriteriaValidatorDispatcher criteriaValidatorDispatcher;
    private final CurrentUserService currentUserService;

    public AlertRuleService(AlertRuleRepository alertRuleRepository,
                             ChannelRepository channelRepository,
                             CriteriaValidatorDispatcher criteriaValidatorDispatcher,
                             CurrentUserService currentUserService) {
        this.alertRuleRepository = alertRuleRepository;
        this.channelRepository = channelRepository;
        this.criteriaValidatorDispatcher = criteriaValidatorDispatcher;
        this.currentUserService = currentUserService;
    }

    public AlertRuleView create(AlertRuleRequest request) {
        criteriaValidatorDispatcher.validate(request.category(), request.criteria());
        UUID userId = currentUserService.getCurrentUserId();
        List<UUID> channelIds = resolveOwnedChannelIds(userId, request.channelIds());

        AlertRule rule = new AlertRule();
        rule.setId(UUID.randomUUID());
        rule.setUserId(userId);
        rule.setCategory(request.category());
        rule.setName(request.name());
        rule.setCriteria(request.criteria());
        rule.setActive(request.active() == null || request.active());
        rule.setCreatedAt(OffsetDateTime.now());

        alertRuleRepository.save(rule);
        alertRuleRepository.replaceChannelLinks(rule.getId(), channelIds);

        return AlertRuleView.from(rule, channelIds);
    }

    public List<AlertRuleView> listForCurrentUser() {
        UUID userId = currentUserService.getCurrentUserId();
        return alertRuleRepository.findAllByUserId(userId).stream()
                .map(rule -> AlertRuleView.from(rule, alertRuleRepository.findChannelIds(rule.getId())))
                .toList();
    }

    public AlertRuleView getOwned(UUID id) {
        UUID userId = currentUserService.getCurrentUserId();
        AlertRule rule = alertRuleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AlertRuleNotFoundException(id));
        return AlertRuleView.from(rule, alertRuleRepository.findChannelIds(id));
    }

    public AlertRuleView update(UUID id, AlertRuleRequest request) {
        criteriaValidatorDispatcher.validate(request.category(), request.criteria());
        UUID userId = currentUserService.getCurrentUserId();
        AlertRule existing = alertRuleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AlertRuleNotFoundException(id));

        List<UUID> channelIds = resolveOwnedChannelIds(userId, request.channelIds());

        existing.setCategory(request.category());
        existing.setName(request.name());
        existing.setCriteria(request.criteria());
        existing.setActive(request.active() == null || request.active());

        alertRuleRepository.update(existing);
        alertRuleRepository.replaceChannelLinks(id, channelIds);

        return AlertRuleView.from(existing, channelIds);
    }

    public void delete(UUID id) {
        UUID userId = currentUserService.getCurrentUserId();
        alertRuleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AlertRuleNotFoundException(id));
        alertRuleRepository.deleteByIdAndUserId(id, userId);
    }

    private List<UUID> resolveOwnedChannelIds(UUID userId, List<UUID> requestedChannelIds) {
        if (requestedChannelIds == null || requestedChannelIds.isEmpty()) {
            return List.of();
        }
        List<UUID> owned = channelRepository.findAllByUserId(userId).stream()
                .map(Channel::getId)
                .toList();
        for (UUID channelId : requestedChannelIds) {
            if (!owned.contains(channelId)) {
                throw new ChannelNotFoundException(channelId);
            }
        }
        return requestedChannelIds;
    }
}
