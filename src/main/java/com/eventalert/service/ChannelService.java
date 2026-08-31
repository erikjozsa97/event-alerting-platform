package com.eventalert.service;

import com.eventalert.exception.ChannelNotFoundException;
import com.eventalert.model.Channel;
import com.eventalert.model.ChannelRequest;
import com.eventalert.model.ChannelType;
import com.eventalert.repository.ChannelRepository;
import com.eventalert.security.CurrentUserService;
import com.eventalert.view.ChannelView;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelConfigValidatorDispatcher configValidatorDispatcher;
    private final CurrentUserService currentUserService;
    private final RestClient restClient;

    public ChannelService(ChannelRepository channelRepository,
                           ChannelConfigValidatorDispatcher configValidatorDispatcher,
                           CurrentUserService currentUserService) {
        this.channelRepository = channelRepository;
        this.configValidatorDispatcher = configValidatorDispatcher;
        this.currentUserService = currentUserService;
        this.restClient = RestClient.create();
    }

    public ChannelView create(ChannelRequest request) {
        Map<String, Object> config = request.config() == null ? Map.of() : request.config();
        configValidatorDispatcher.validate(request.type(), config);

        Channel channel = new Channel();
        channel.setId(UUID.randomUUID());
        channel.setUserId(currentUserService.getCurrentUserId());
        channel.setType(request.type());
        channel.setConfig(config);
        channel.setVerified(verify(request.type(), config));
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

    private boolean verify(ChannelType type, Map<String, Object> config) {
        if (type == ChannelType.EMAIL) {
            // Uses the account's own, already-registered address — nothing to verify yet.
            return true;
        }

        String webhookUrl = (String) config.get("webhookUrl");
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", "This Slack channel is now connected to Event Alerting Platform."))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            // The channel is still created — verified=false lets the user see it needs
            // attention (bad URL, revoked webhook, etc.) rather than failing the request.
            return false;
        }
    }
}
