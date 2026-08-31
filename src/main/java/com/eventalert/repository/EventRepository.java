package com.eventalert.repository;

import com.eventalert.model.Category;
import com.eventalert.model.Event;
import com.eventalert.model.RawEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC data access for the {@code events} table — no Spring Data JPA, hand-written SQL.
 */
@Repository
public class EventRepository {

    // Admin listing is unbounded by user, so cap it — see EventRepository#findAll.
    private static final int MAX_ADMIN_RESULTS = 200;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final RowMapper<Event> rowMapper = this::mapRow;

    public EventRepository(@NonNull NamedParameterJdbcTemplate jdbc, @NonNull ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    // The (source, external_id) unique constraint from V1 is what actually dedupes —
    // this just turns "insert, but it might already exist" into a single round trip
    // instead of a check-then-insert race.
    @NonNull
    public Optional<Event> insertIfNew(@NonNull RawEvent raw) {
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

    // Admin-only listing (M6) — optionally filtered by category and/or a minimum
    // occurred_at, capped at MAX_ADMIN_RESULTS most recent to keep it bounded.
    @NonNull
    public List<Event> findAll(@Nullable Category category, @Nullable OffsetDateTime since) {
        StringBuilder sql = new StringBuilder("SELECT * FROM events WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (category != null) {
            sql.append(" AND category = :category");
            params.addValue("category", category.name());
        }
        if (since != null) {
            sql.append(" AND occurred_at >= :since");
            params.addValue("since", since);
        }
        sql.append(" ORDER BY occurred_at DESC LIMIT ").append(MAX_ADMIN_RESULTS);

        return jdbc.query(sql.toString(), params, rowMapper);
    }

    @NonNull
    private PGobject toJsonb(@Nullable Map<String, Object> map) {
        try {
            PGobject pg = new PGobject();
            pg.setType("jsonb");
            pg.setValue(objectMapper.writeValueAsString(map == null ? Map.of() : map));
            return pg;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }

    @NonNull
    private Map<String, Object> fromJsonb(@NonNull String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse event payload", e);
        }
    }

    @NonNull
    private Event mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        Event event = new Event();
        event.setId(UUID.fromString(rs.getString("id")));
        event.setSource(rs.getString("source"));
        event.setExternalId(rs.getString("external_id"));
        event.setCategory(Category.valueOf(rs.getString("category")));
        event.setPayload(fromJsonb(rs.getString("payload")));
        event.setOccurredAt(rs.getObject("occurred_at", OffsetDateTime.class));
        event.setIngestedAt(rs.getObject("ingested_at", OffsetDateTime.class));
        return event;
    }
}
