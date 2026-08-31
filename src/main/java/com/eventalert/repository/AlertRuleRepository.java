package com.eventalert.repository;

import com.eventalert.model.AlertRule;
import com.eventalert.model.Category;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AlertRuleRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final RowMapper<AlertRule> rowMapper = this::mapRow;

    public AlertRuleRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public AlertRule save(AlertRule rule) {
        String sql = """
                INSERT INTO alert_rules (id, user_id, category, name, criteria, active, created_at)
                VALUES (:id, :userId, :category, :name, :criteria, :active, :createdAt)
                """;
        jdbc.update(sql, toParams(rule));
        return rule;
    }

    public void update(AlertRule rule) {
        String sql = """
                UPDATE alert_rules
                SET category = :category, name = :name, criteria = :criteria, active = :active
                WHERE id = :id AND user_id = :userId
                """;
        jdbc.update(sql, toParams(rule));
    }

    public Optional<AlertRule> findByIdAndUserId(UUID id, UUID userId) {
        String sql = "SELECT * FROM alert_rules WHERE id = :id AND user_id = :userId";
        var params = new MapSqlParameterSource().addValue("id", id).addValue("userId", userId);
        return jdbc.query(sql, params, rowMapper).stream().findFirst();
    }

    public List<AlertRule> findAllByUserId(UUID userId) {
        String sql = "SELECT * FROM alert_rules WHERE user_id = :userId ORDER BY created_at DESC";
        var params = new MapSqlParameterSource("userId", userId);
        return jdbc.query(sql, params, rowMapper);
    }

    public void deleteByIdAndUserId(UUID id, UUID userId) {
        String sql = "DELETE FROM alert_rules WHERE id = :id AND user_id = :userId";
        var params = new MapSqlParameterSource().addValue("id", id).addValue("userId", userId);
        jdbc.update(sql, params);
    }

    public void replaceChannelLinks(UUID alertRuleId, List<UUID> channelIds) {
        jdbc.update("DELETE FROM alert_rule_channels WHERE alert_rule_id = :ruleId",
                new MapSqlParameterSource("ruleId", alertRuleId));

        if (channelIds == null || channelIds.isEmpty()) {
            return;
        }

        SqlParameterSource[] batchParams = channelIds.stream()
                .map(channelId -> (SqlParameterSource) new MapSqlParameterSource()
                        .addValue("ruleId", alertRuleId)
                        .addValue("channelId", channelId))
                .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate(
                "INSERT INTO alert_rule_channels (alert_rule_id, channel_id) VALUES (:ruleId, :channelId)",
                batchParams);
    }

    public List<UUID> findChannelIds(UUID alertRuleId) {
        String sql = "SELECT channel_id FROM alert_rule_channels WHERE alert_rule_id = :ruleId";
        var params = new MapSqlParameterSource("ruleId", alertRuleId);
        return jdbc.query(sql, params, (rs, rowNum) -> UUID.fromString(rs.getString("channel_id")));
    }

    private MapSqlParameterSource toParams(AlertRule rule) {
        return new MapSqlParameterSource()
                .addValue("id", rule.getId())
                .addValue("userId", rule.getUserId())
                .addValue("category", rule.getCategory().name())
                .addValue("name", rule.getName())
                .addValue("criteria", toJsonb(rule.getCriteria()), Types.OTHER)
                .addValue("active", rule.isActive())
                .addValue("createdAt", rule.getCreatedAt());
    }

    private AlertRule mapRow(ResultSet rs, int rowNum) throws SQLException {
        AlertRule rule = new AlertRule();
        rule.setId(UUID.fromString(rs.getString("id")));
        rule.setUserId(UUID.fromString(rs.getString("user_id")));
        rule.setCategory(Category.valueOf(rs.getString("category")));
        rule.setName(rs.getString("name"));
        rule.setCriteria(fromJsonb(rs.getString("criteria")));
        rule.setActive(rs.getBoolean("active"));
        rule.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return rule;
    }

    private PGobject toJsonb(Map<String, Object> map) {
        try {
            PGobject pg = new PGobject();
            pg.setType("jsonb");
            pg.setValue(objectMapper.writeValueAsString(map));
            return pg;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize criteria", e);
        }
    }

    private Map<String, Object> fromJsonb(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse criteria", e);
        }
    }
}
