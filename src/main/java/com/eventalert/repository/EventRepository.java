package com.eventalert.repository;

import com.eventalert.model.Event;
import com.eventalert.model.RawEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EventRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public EventRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    // The (source, external_id) unique constraint from V1 is what actually dedupes —
    // this just turns "insert, but it might already exist" into a single round trip
    // instead of a check-then-insert race.
    public Optional<Event> insertIfNew(RawEvent raw) {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setSource(raw.source());
        event.setExternalId(raw.externalId());
        event.setCategory(raw.category());
        event.setPayload(raw.payload());
        event.setOccurredAt(raw.occurredAt());
        event.setIngestedAt(OffsetDateTime.now());

        String sql = """
                INSERT INTO events (id, source, external_id, category, payload, occurred_at, ingested_at)
                VALUES (:id, :source, :externalId, :category, :payload, :occurredAt, :ingestedAt)
                ON CONFLICT (source, external_id) DO NOTHING
                RETURNING id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", event.getId())
                .addValue("source", event.getSource())
                .addValue("externalId", event.getExternalId())
                .addValue("category", event.getCategory().name())
                .addValue("payload", toJsonb(event.getPayload()), Types.OTHER)
                .addValue("occurredAt", event.getOccurredAt())
                .addValue("ingestedAt", event.getIngestedAt());

        List<UUID> insertedIds = jdbc.query(sql, params, (rs, rowNum) -> UUID.fromString(rs.getString("id")));
        return insertedIds.isEmpty() ? Optional.empty() : Optional.of(event);
    }

    private PGobject toJsonb(Map<String, Object> map) {
        try {
            PGobject pg = new PGobject();
            pg.setType("jsonb");
            pg.setValue(objectMapper.writeValueAsString(map == null ? Map.of() : map));
            return pg;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }
}
