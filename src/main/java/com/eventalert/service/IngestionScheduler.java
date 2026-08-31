package com.eventalert.service;

import com.eventalert.model.RawEvent;
import com.eventalert.repository.EventRepository;
import com.eventalert.view.SourceStatusView;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestionScheduler.class);

    private final List<EventSource> eventSources;
    private final EventRepository eventRepository;
    private final MatchingService matchingService;
    private final MeterRegistry meterRegistry;

    // In-memory only — a restart resets it, which is fine for "is this source
    // healthy right now" reporting. Read by the admin source-status endpoint
    // (a web thread) while written by the scheduler thread, hence ConcurrentHashMap.
    private final Map<String, SourceStatusView> statusBySource = new ConcurrentHashMap<>();

    public IngestionScheduler(List<EventSource> eventSources,
                               EventRepository eventRepository,
                               MatchingService matchingService,
                               MeterRegistry meterRegistry) {
        this.eventSources = eventSources;
        this.eventRepository = eventRepository;
        this.matchingService = matchingService;
        this.meterRegistry = meterRegistry;

        for (EventSource source : eventSources) {
            statusBySource.put(source.getSourceName(),
                    new SourceStatusView(source.getSourceName(), source.getCategory(),
                            source.isConfigured(), null, 0, null));
        }
    }

    @Scheduled(fixedDelayString = "${app.ingestion.poll-interval-ms:120000}")
    public void pollOnSchedule() {
        pollAllSourcesNow();
    }

    // Shared by the scheduled trigger above and the manual /api/admin/ingestion/poll-now
    // endpoint — returns how many genuinely new (post-dedup) events each source produced.
    public Map<String, Integer> pollAllSourcesNow() {
        Map<String, Integer> ingestedCountBySource = new LinkedHashMap<>();

        for (EventSource source : eventSources) {
            int insertedCount = 0;
            String error = null;
            try {
                for (RawEvent raw : source.fetchLatest()) {
                    var inserted = eventRepository.insertIfNew(raw);
                    if (inserted.isPresent()) {
                        insertedCount++;
                        matchingService.processNewEvent(inserted.get());
                    }
                }
            } catch (Exception e) {
                error = e.getMessage();
                log.warn("Ingestion failed for source {}: {}", source.getSourceName(), error);
            }

            ingestedCountBySource.put(source.getSourceName(), insertedCount);
            statusBySource.put(source.getSourceName(),
                    new SourceStatusView(source.getSourceName(), source.getCategory(),
                            source.isConfigured(), OffsetDateTime.now(), insertedCount, error));

            meterRegistry.counter("ingestion.events.ingested", "source", source.getSourceName())
                    .increment(insertedCount);
        }

        return ingestedCountBySource;
    }

    public List<SourceStatusView> getSourceStatuses() {
        return List.copyOf(statusBySource.values());
    }
}
