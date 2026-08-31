package com.eventalert.service;

import com.eventalert.model.RawEvent;
import com.eventalert.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class IngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestionScheduler.class);

    private final List<EventSource> eventSources;
    private final EventRepository eventRepository;
    private final MatchingService matchingService;

    public IngestionScheduler(List<EventSource> eventSources,
                               EventRepository eventRepository,
                               MatchingService matchingService) {
        this.eventSources = eventSources;
        this.eventRepository = eventRepository;
        this.matchingService = matchingService;
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
            try {
                for (RawEvent raw : source.fetchLatest()) {
                    var inserted = eventRepository.insertIfNew(raw);
                    if (inserted.isPresent()) {
                        insertedCount++;
                        matchingService.processNewEvent(inserted.get());
                    }
                }
            } catch (Exception e) {
                log.warn("Ingestion failed for source {}: {}", source.getSourceName(), e.getMessage());
            }
            ingestedCountBySource.put(source.getSourceName(), insertedCount);
        }

        return ingestedCountBySource;
    }
}
