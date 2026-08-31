package com.eventalert.repository;

import com.eventalert.model.Delivery;
import com.eventalert.model.DeliveryStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class DeliveryRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<Delivery> rowMapper = this::mapRow;

    public DeliveryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Delivery save(Delivery delivery) {
        String sql = """
                INSERT INTO deliveries (id, alert_rule_id, event_id, channel_id, status, attempted_at, error_message)
                VALUES (:id, :alertRuleId, :eventId, :channelId, :status, :attemptedAt, :errorMessage)
                """;
        jdbc.update(sql, toParams(delivery));
        return delivery;
    }

    public List<Delivery> findByAlertRuleId(UUID alertRuleId) {
        String sql = "SELECT * FROM deliveries WHERE alert_rule_id = :alertRuleId ORDER BY attempted_at DESC";
        var params = new MapSqlParameterSource("alertRuleId", alertRuleId);
        return jdbc.query(sql, params, rowMapper);
    }

    // Admin-only listing (M6) — every delivery, across every user, optionally
    // filtered, capped at 200 most recent to keep it bounded.
    public List<Delivery> findAll(DeliveryStatus status, OffsetDateTime since) {
        StringBuilder sql = new StringBuilder("SELECT * FROM deliveries WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (status != null) {
            sql.append(" AND status = :status");
            params.addValue("status", status.name());
        }
        if (since != null) {
            sql.append(" AND attempted_at >= :since");
            params.addValue("since", since);
        }
        sql.append(" ORDER BY attempted_at DESC LIMIT 200");

        return jdbc.query(sql.toString(), params, rowMapper);
    }

    private MapSqlParameterSource toParams(Delivery delivery) {
        return new MapSqlParameterSource()
                .addValue("id", delivery.getId())
                .addValue("alertRuleId", delivery.getAlertRuleId())
                .addValue("eventId", delivery.getEventId(), Types.OTHER)
                .addValue("channelId", delivery.getChannelId())
                .addValue("status", delivery.getStatus().name())
                .addValue("attemptedAt", delivery.getAttemptedAt())
                .addValue("errorMessage", delivery.getErrorMessage());
    }

    private Delivery mapRow(ResultSet rs, int rowNum) throws SQLException {
        Delivery delivery = new Delivery();
        delivery.setId(UUID.fromString(rs.getString("id")));
        delivery.setAlertRuleId(UUID.fromString(rs.getString("alert_rule_id")));
        String eventId = rs.getString("event_id");
        delivery.setEventId(eventId == null ? null : UUID.fromString(eventId));
        delivery.setChannelId(UUID.fromString(rs.getString("channel_id")));
        delivery.setStatus(DeliveryStatus.valueOf(rs.getString("status")));
        delivery.setAttemptedAt(rs.getObject("attempted_at", OffsetDateTime.class));
        delivery.setErrorMessage(rs.getString("error_message"));
        return delivery;
    }
}
