package com.eventalert.repository;

import com.eventalert.model.Role;
import com.eventalert.model.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    private static final RowMapper<User> USER_ROW_MAPPER = UserRepository::mapRow;

    private final NamedParameterJdbcTemplate jdbc;

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public User save(@NonNull User user) {
        String sql = """
                INSERT INTO users (id, email, password_hash, role, enabled, created_at)
                VALUES (:id, :email, :passwordHash, :role, :enabled, :createdAt)
                """;
        jdbc.update(sql, toParams(user));
        return user;
    }

    public Optional<User> findByEmail(@NonNull String email) {
        String sql = "SELECT * FROM users WHERE email = :email";
        var params = new MapSqlParameterSource("email", email);
        return jdbc.query(sql, params, USER_ROW_MAPPER).stream().findFirst();
    }

    public Optional<User> findById(@NonNull UUID id) {
        String sql = "SELECT * FROM users WHERE id = :id";
        var params = new MapSqlParameterSource("id", id);
        return jdbc.query(sql, params, USER_ROW_MAPPER).stream().findFirst();
    }

    public boolean existsByEmail(@Nullable String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = :email";
        var params = new MapSqlParameterSource("email", email);
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    @NonNull
    private static MapSqlParameterSource toParams(@NonNull User user) {
        return new MapSqlParameterSource()
                .addValue("id", user.getId())
                .addValue("email", user.getEmail())
                .addValue("passwordHash", user.getPasswordHash())
                .addValue("role", user.getRole().name())
                .addValue("enabled", user.isEnabled())
                .addValue("createdAt", user.getCreatedAt());
    }

    @NonNull
    private static User mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(UUID.fromString(rs.getString("id")));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(Role.valueOf(rs.getString("role")));
        user.setEnabled(rs.getBoolean("enabled"));
        user.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return user;
    }
}
