package com.eventalert.repository;

import com.eventalert.model.Channel;
import com.eventalert.model.ChannelType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
public class ChannelRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final RowMapper<Channel> rowMapper = this::mapRow;

    public ChannelRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public Channel save(Channel channel) {
        String sql = """
                INSERT INTO channels (id, user_id, type, config, verified, created_at)
                VALUES (:id, :userId, :type, :config, :verified, :createdAt)
                """;
        jdbc.update(sql, toParams(channel));
        return channel;
    }

    public Optional<Channel> findByIdAndUserId(UUID id, UUID userId) {
        String sql = "SELECT * FROM channels WHERE id = :id AND user_id = :userId";
        var params = new MapSqlParameterSource().addValue("id", id).addValue("userId", userId);
        return jdbc.query(sql, params, rowMapper).stream().findFirst();
    }

    public List<Channel> findAllByUserId(UUID userId) {
        String sql = "SELECT * FROM channels WHERE user_id = :userId ORDER BY created_at DESC";
        var params = new MapSqlParameterSource("userId", userId);
        return jdbc.query(sql, params, rowMapper);
    }

    public void deleteByIdAndUserId(UUID id, UUID userId) {
        String sql = "DELETE FROM channels WHERE id = :id AND user_id = :userId";
        var params = new MapSqlParameterSource().addValue("id", id).addValue("userId", userId);
        jdbc.update(sql, params);
    }

    private MapSqlParameterSource toParams(Channel channel) {
        return new MapSqlParameterSource()
                .addValue("id", channel.getId())
                .addValue("userId", channel.getUserId())
                .addValue("type", channel.getType().name())
                .addValue("config", toJsonb(channel.getConfig()), Types.OTHER)
                .addValue("verified", channel.isVerified())
                .addValue("createdAt", channel.getCreatedAt());
    }

    private Channel mapRow(ResultSet rs, int rowNum) throws SQLException {
        Channel channel = new Channel();
        channel.setId(UUID.fromString(rs.getString("id")));
        channel.setUserId(UUID.fromString(rs.getString("user_id")));
        channel.setType(ChannelType.valueOf(rs.getString("type")));
        channel.setConfig(fromJsonb(rs.getString("config")));
        channel.setVerified(rs.getBoolean("verified"));
        channel.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return channel;
    }

    private PGobject toJsonb(Map<String, Object> map) {
        try {
            PGobject pg = new PGobject();
            pg.setType("jsonb");
            pg.setValue(objectMapper.writeValueAsString(map == null ? Map.of() : map));
            return pg;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize channel config", e);
        }
    }

    private Map<String, Object> fromJsonb(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse channel config", e);
        }
    }
}
