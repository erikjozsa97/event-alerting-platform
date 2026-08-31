package com.eventalert.service;

import com.eventalert.exception.ChannelNotFoundException;
import com.eventalert.model.Channel;
import com.eventalert.model.ChannelRequest;
import com.eventalert.model.ChannelType;
import com.eventalert.model.NotificationMessage;
import com.eventalert.model.User;
import com.eventalert.repository.ChannelRepository;
import com.eventalert.security.CurrentUserService;
import com.eventalert.view.ChannelView;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelConfigValidatorDispatcher configValidatorDispatcher;
    private final CurrentUserService currentUserService;
    private final NotificationChannelDispatcher notificationChannelDispatcher;

    public ChannelService(ChannelRepository channelRepository,
                           ChannelConfigValidatorDispatcher configValidatorDispatcher,
                           CurrentUserService currentUserService,
                           NotificationChannelDispatcher notificationChannelDispatcher) {
        this.channelRepository = channelRepository;
        this.configValidatorDispatcher = configValidatorDispatcher;
        this.currentUserService = currentUserService;
        this.notificationChannelDispatcher = notificationChannelDispatcher;
    }

    public ChannelView create(ChannelRequest request) {
        Map<String, Object> config = request.config() == null ? Map.of() : request.config();
        configValidatorDispatcher.validate(request.type(), config);

        User owner = currentUserService.getCurrentUser();

        Channel channel = new Channel();
        channel.setId(UUID.randomUUID());
        channel.setUserId(owner.getId());
        channel.setType(request.type());
        channel.setConfig(config);
        channel.setVerified(verify(request.type(), config, owner));
        channel.setCreatedAt(OffsetDateTime.now());

        channelRepository.save(channel);
        return ChannelView.from(channel);
    }

    public List<ChannelView> listForCurrentUser() {
        UUID userId = currentUserService.getCurrentUserId();
        return channelRepository.findAllByUserId(userId).stream()
                .map(ChannelView::from)
                .toList();
    }

    public void delete(UUID id) {
        UUID userId = currentUserService.getCurrentUserId();
        channelRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ChannelNotFoundException(id));
        channelRepository.deleteByIdAndUserId(id, userId);
    }

    // Reuses the same NotificationChannel that real deliveries go through (M3),
    // instead of a separate one-off HTTP call, so "verified" actually means
    // "the exact send path this channel will be used for works."
    private boolean verify(ChannelType type, Map<String, Object> config, User owner) {
        if (type == ChannelType.EMAIL) {
            // Uses the account's own, already-registered address — nothing to verify yet.
            return true;
        }

        Channel probe = new Channel();
        probe.setType(type);
        probe.setConfig(config);

        try {
            notificationChannelDispatcher.get(type).send(probe, owner,
                    new NotificationMessage("Channel connected",
                            "This channel is now connected to Event Alerting Platform."));
            return true;
        } catch (Exception e) {
            // The channel is still created — verified=false lets the user see it needs
            // attention (bad URL, revoked webhook, etc.) rather than failing the request.
            return false;
        }
    }
}
