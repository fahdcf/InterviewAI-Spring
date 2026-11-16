package com.interviewai.backend.repository;

import com.interviewai.backend.model.Account;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class AccountRepository {

    private static final String ACCOUNT_COLUMNS =
            "SELECT id, username, email, password_hash, is_active FROM users";

    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Account> findByLogin(String identifier) {
        Optional<Account> usernameMatch = jdbc.query(
                ACCOUNT_COLUMNS + " WHERE username = ?", this::mapAccount, identifier)
                .stream().findFirst();
        if (usernameMatch.isPresent()) {
            return usernameMatch;
        }
        return jdbc.query(ACCOUNT_COLUMNS + " WHERE email = ?", this::mapAccount, identifier)
                .stream().findFirst();
    }

    public Optional<Account> findById(int id) {
        return jdbc.query(ACCOUNT_COLUMNS + " WHERE id = ?", this::mapAccount, id)
                .stream().findFirst();
    }

    public boolean lockActiveAccount(int id) {
        List<Boolean> rows = jdbc.query("SELECT is_active FROM users WHERE id = ? FOR UPDATE",
                (row, index) -> row.getBoolean("is_active"), id);
        return !rows.isEmpty() && rows.getFirst();
    }

    public List<String> findRoles(int userId) {
        return jdbc.queryForList("""
                SELECT r.name
                FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = ?
                ORDER BY r.name
                """, String.class, userId);
    }

    public List<String> findPermissions(int userId) {
        return jdbc.queryForList("""
                SELECT DISTINCT p.name
                FROM user_roles ur
                JOIN role_permissions rp ON rp.role_id = ur.role_id
                JOIN permissions p ON p.id = rp.permission_id
                WHERE ur.user_id = ?
                ORDER BY p.name
                """, String.class, userId);
    }

    public int insertCandidate(String username, String email, String passwordHash) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)",
                    new String[]{"id"});
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            return statement;
        }, keyHolder);

        Number id = keyHolder.getKey();
        if (id == null) {
            throw new IllegalStateException("The database did not return the new user ID.");
        }
        int assigned = jdbc.update("""
                INSERT INTO user_roles (user_id, role_id)
                SELECT ?, id FROM roles WHERE name = 'CANDIDATE'
                """, id.intValue());
        if (assigned != 1) {
            throw new IllegalStateException("The candidate role is missing.");
        }
        return id.intValue();
    }

    public void upgradePasswordHash(int userId, String oldHash, String newHash) {
        jdbc.update("UPDATE users SET password_hash = ? WHERE id = ? AND password_hash = ?",
                newHash, userId, oldHash);
    }

    private Account mapAccount(ResultSet row, int rowNumber) throws SQLException {
        return new Account(row.getInt("id"), row.getString("username"), row.getString("email"),
                row.getString("password_hash"), row.getBoolean("is_active"));
    }
}
