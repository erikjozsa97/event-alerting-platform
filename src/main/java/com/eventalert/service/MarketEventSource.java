package com.eventalert.service;

import com.eventalert.model.Category;
import com.eventalert.model.RawEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Event source implementation for polling financial stock market quote updates via the Finnhub API.
 * <p>
 * Iterates over a configurable symbol watchlist, fetching quote data for each ticker and mapping
 * price movements and percentage changes into standardized {@link RawEvent} instances.
 */
// Finnhub's /quote is per-symbol (no "everything that moved" endpoint on the free
// tier), so this polls a configurable watchlist rather than a single feed URL.
@Component
public class MarketEventSource implements EventSource {

    private final RestClient restClient = RestClient.create();
    private final String apiKey;
    private final List<String> symbols;

    public MarketEventSource(@Value("${app.ingestion.finnhub.key:}") @Nullable String apiKey,
                              @Value("${app.ingestion.finnhub.symbols:AAPL,MSFT,GOOGL,AMZN,TSLA}") @NonNull String symbolsCsv) {
        this.apiKey = apiKey;
        this.symbols = Arrays.stream(symbolsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Override
    @NonNull
    public Category getCategory() {
        return Category.MARKET;
    }

    @Override
    @NonNull
    public String getSourceName() {
        return "finnhub";
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    @NonNull
    public List<RawEvent> fetchLatest() {
        if (apiKey == null || apiKey.isBlank()) {
            // No FINNHUB_KEY configured — this source stays idle rather than failing
            // startup or every poll cycle. Set FINNHUB_KEY to activate it.
            return List.of();
        }

        List<RawEvent> events = new ArrayList<>();
        for (String symbol : symbols) {
            fetchQuote(symbol).ifPresent(events::add);
        }
        return events;
    }

    private Optional<RawEvent> fetchQuote(String symbol) {
        String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol + "&token=" + apiKey;

        Map<?, ?> response;
        try {
            response = restClient.get().uri(url).retrieve().body(Map.class);
        } catch (Exception e) {
            return Optional.empty();
        }
        if (response == null) {
            return Optional.empty();
        }

        Object current = response.get("c");
        Object previousClose = response.get("pc");
        Object changePercent = response.get("dp");
        Object change = response.get("d");
        Object timestampRaw = response.get("t");

        if (!(current instanceof Number) || !(previousClose instanceof Number pcNum) || pcNum.doubleValue() == 0) {
            return Optional.empty(); // symbol not found / market not open yet / no data
        }

        long timestamp = timestampRaw instanceof Number n ? n.longValue() : 0L;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("symbol", symbol);
        payload.put("current", current);
        payload.put("previousClose", previousClose);
        payload.put("changePercent", changePercent);
        payload.put("change", change);

        // Finnhub's own last-trade timestamp is the dedup key — if it hasn't moved
        // since the last poll, this collides with the existing row and is skipped.
        String externalId = symbol + ":" + timestamp;

        return Optional.of(new RawEvent(getSourceName(), externalId, Category.MARKET, payload,
                Instant.ofEpochSecond(timestamp).atOffset(ZoneOffset.UTC)));
    }
}
