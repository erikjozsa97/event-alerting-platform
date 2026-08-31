package com.eventalert.service;

import com.eventalert.model.Category;
import com.eventalert.model.RawEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Event source implementation for fetching news headlines from the NewsAPI.org service.
 * <p>
 * Periodically retrieves recent top headlines and transforms article records into
 * standardized {@link RawEvent} instances for downstream keyword matching and filtering.
 */
// NewsAPI.org's /v2/top-headlines: broad, no search query needed — matching against
// user keyword criteria happens on our side (NewsCriteriaValidator / NewsEventMatcher),
// not by asking the provider to pre-filter.
@Component
public class NewsEventSource implements EventSource {

    private final RestClient restClient = RestClient.create();
    private final String apiKey;

    public NewsEventSource(@Value("${app.ingestion.newsapi.key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    @NonNull
    public Category getCategory() {
        return Category.NEWS;
    }

    @Override
    @NonNull
    public String getSourceName() {
        return "newsapi";
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    @SuppressWarnings("unchecked")
    @NonNull
    public List<RawEvent> fetchLatest() {
        if (apiKey == null || apiKey.isBlank()) {
            // No NEWSAPI_KEY configured — this source stays idle rather than failing
            // startup or every poll cycle. Set NEWSAPI_KEY to activate it.
            return List.of();
        }

        String url = "https://newsapi.org/v2/top-headlines?category=general&language=en&pageSize=20&apiKey=" + apiKey;

        Map<String, Object> response;
        try {
            response = restClient.get().uri(url).retrieve().body(Map.class);
        } catch (Exception e) {
            return List.of();
        }
        if (response == null || !(response.get("articles") instanceof List<?> articles)) {
            return List.of();
        }

        List<RawEvent> events = new ArrayList<>();
        for (Object articleObj : articles) {
            if (!(articleObj instanceof Map<?, ?> article)) {
                continue;
            }
            RawEvent event = toRawEvent((Map<String, Object>) article);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }


    @Nullable
    private RawEvent toRawEvent(@NonNull Map<String, Object> article) {
        Object articleUrl = article.get("url");
        if (!(articleUrl instanceof String url) || url.isBlank()) {
            return null; // url doubles as our external_id — skip anything without one
        }

        Object sourceInfo = article.get("source");
        Object sourceName = sourceInfo instanceof Map<?, ?> s ? s.get("name") : null;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", article.get("title"));
        payload.put("description", article.get("description"));
        payload.put("content", article.get("content"));
        payload.put("url", url);
        payload.put("sourceName", sourceName);

        return new RawEvent(getSourceName(), url, Category.NEWS, payload,
                parsePublishedAt((String) article.get("publishedAt")));
    }

    private OffsetDateTime parsePublishedAt(String publishedAt) {
        try {
            return OffsetDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return OffsetDateTime.now();
        }
    }
}
