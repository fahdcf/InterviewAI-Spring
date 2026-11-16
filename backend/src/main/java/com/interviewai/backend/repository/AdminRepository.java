package com.interviewai.backend.repository;

import com.interviewai.backend.dto.response.AdminCourse;
import com.interviewai.backend.dto.response.AdminOverview;
import com.interviewai.backend.dto.response.AdminPage;
import com.interviewai.backend.dto.response.AdminUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AdminRepository {

    public static final int PAGE_SIZE = 20;
    private static final String USER_COLUMNS = """
            SELECT u.id, u.username, u.email, u.is_active, u.created_at,
                (SELECT COALESCE(SUM(p.xp), 0) FROM progress p WHERE p.user_id = u.id) AS xp,
                (SELECT COUNT(*) FROM generated_courses g WHERE g.user_id = u.id) AS courses
            """;
    private static final String COURSE_COLUMNS = """
            SELECT g.id, g.user_id, u.username AS owner, g.course_title, g.status, g.created_at,
                (SELECT COUNT(*) FROM chapters c WHERE c.course_id = g.id) AS chapters,
                (SELECT COUNT(*) FROM chapters c WHERE c.course_id = g.id AND c.status = 'COMPLETED') AS completed,
                (SELECT COUNT(*) FROM questions q JOIN chapters c ON q.chapter_id = c.id
                    WHERE c.course_id = g.id) AS questions
            """;
    private final JdbcTemplate jdbc;
    private final AccountRepository accounts;

    public AdminRepository(JdbcTemplate jdbc, AccountRepository accounts) {
        this.jdbc = jdbc;
        this.accounts = accounts;
    }

    public AdminPage<AdminUser> users(String search, String status, String role, int page) {
        String filter = """
                FROM users u
                WHERE (LOWER(u.username) LIKE ? OR LOWER(COALESCE(u.email, '')) LIKE ?)
                """;
        List<Object> parameters = new ArrayList<>(List.of(search, search));
        if (!status.isBlank()) {
            filter += " AND u.is_active = ?";
            parameters.add("ACTIVE".equals(status));
        }
        if (!role.isBlank()) {
            filter += """
                     AND EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                         WHERE ur.user_id = u.id AND r.name = ?)
                    """;
            parameters.add(role);
        }
        int total = count("SELECT COUNT(*) " + filter, parameters.toArray());
        parameters.add(PAGE_SIZE);
        parameters.add(page * PAGE_SIZE);
        List<AdminUser> rows = jdbc.query(USER_COLUMNS + filter + " ORDER BY u.id DESC LIMIT ? OFFSET ?",
                this::mapUser, parameters.toArray());
        return new AdminPage<>(rows.stream().map(this::withRoles).toList(), page, total);
    }

    public Optional<AdminUser> user(int id) {
        return jdbc.query(USER_COLUMNS + " FROM users u WHERE u.id = ?", this::mapUser, id)
                .stream().findFirst().map(this::withRoles);
    }

    public boolean lockUser(int id) {
        return !jdbc.queryForList("SELECT id FROM users WHERE id = ? FOR UPDATE", Integer.class, id).isEmpty();
    }

    public void setActive(int id, boolean active) {
        jdbc.update("UPDATE users SET is_active = ?, session_version = session_version + 1 WHERE id = ?", active, id);
    }

    public void setEmail(int id, String email) {
        jdbc.update("UPDATE users SET email = ? WHERE id = ?", email, id);
    }

    public List<String> roles() {
        return jdbc.queryForList("SELECT name FROM roles ORDER BY name", String.class);
    }

    public void assignRole(int id, String role) {
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", id);
        int rows = jdbc.update("""
                INSERT INTO user_roles (user_id, role_id)
                SELECT ?, id FROM roles WHERE name = ?
                """, id, role);
        if (rows != 1) {
            throw new IllegalArgumentException("Unknown role.");
        }
        jdbc.update("UPDATE users SET role = ?, session_version = session_version + 1 WHERE id = ?", role, id);
    }

    public void resetXp(int id) {
        jdbc.update("UPDATE progress SET xp = 0 WHERE user_id = ?", id);
    }

    public void resetProgress(int id) {
        resetXp(id);
        jdbc.update("""
                UPDATE chapters SET status = 'NOT_STARTED', completed_at = NULL
                WHERE course_id IN (SELECT id FROM generated_courses WHERE user_id = ?)
                """, id);
        jdbc.update("""
                UPDATE questions SET status = 'NOT_STARTED', updated_at = CURRENT_TIMESTAMP
                WHERE chapter_id IN (SELECT c.id FROM chapters c JOIN generated_courses g ON c.course_id = g.id
                    WHERE g.user_id = ?)
                """, id);
        jdbc.update("DELETE FROM practice_attempts WHERE user_id = ?", id);
        jdbc.update("UPDATE user_goals SET start_value = 0, current_value = 0 WHERE user_id = ?", id);
    }

    public AdminPage<AdminCourse> courses(String search, String status, int page) {
        String filter = """
                FROM generated_courses g LEFT JOIN users u ON u.id = g.user_id
                WHERE (LOWER(COALESCE(g.course_title, '')) LIKE ? OR LOWER(COALESCE(u.username, '')) LIKE ?)
                """;
        List<Object> parameters = new ArrayList<>(List.of(search, search));
        if (!status.isBlank()) {
            filter += " AND UPPER(g.status) = ?";
            parameters.add(status);
        }
        int total = count("SELECT COUNT(*) " + filter, parameters.toArray());
        parameters.add(PAGE_SIZE);
        parameters.add(page * PAGE_SIZE);
        List<AdminCourse> rows = jdbc.query(COURSE_COLUMNS + filter + " ORDER BY g.id DESC LIMIT ? OFFSET ?",
                this::mapCourse, parameters.toArray());
        return new AdminPage<>(rows, page, total);
    }

    public Optional<AdminCourse> course(int id) {
        return jdbc.query(COURSE_COLUMNS + """
                 FROM generated_courses g LEFT JOIN users u ON u.id = g.user_id WHERE g.id = ?
                """, this::mapCourse, id).stream().findFirst();
    }

    public void setCourseStatus(int id, String status) {
        jdbc.update("UPDATE generated_courses SET status = ? WHERE id = ?", status, id);
    }

    public void deleteCourse(int id) {
        jdbc.update("DELETE FROM feedback WHERE course_id = ?", id);
        jdbc.update("DELETE FROM generated_courses WHERE id = ?", id);
    }

    public AdminOverview overview(LocalDateTime daily, LocalDateTime weekly, LocalDateTime monthly) {
        return new AdminOverview(count("SELECT COUNT(*) FROM users"),
                count("SELECT COUNT(*) FROM users WHERE is_active = TRUE"),
                count("SELECT COUNT(*) FROM users WHERE created_at >= ?", daily),
                count("SELECT COUNT(*) FROM users WHERE created_at >= ?", weekly),
                count("SELECT COUNT(*) FROM users WHERE created_at >= ?", monthly),
                count("SELECT COUNT(*) FROM generated_courses"),
                count("SELECT COUNT(*) FROM generated_courses WHERE status = 'COMPLETED'"),
                count("SELECT COUNT(*) FROM chapters WHERE status = 'COMPLETED'"),
                count("SELECT COUNT(*) FROM questions"), interviewTypes(), languages(), newAccounts(weekly), usage(weekly));
    }

    private int count(String sql, Object... values) {
        Integer result = jdbc.queryForObject(sql, Integer.class, values);
        return result == null ? 0 : result;
    }

    private Map<String, Long> interviewTypes() {
        return counts("SELECT interview_type, COUNT(*) FROM onboarding_data GROUP BY interview_type ORDER BY interview_type");
    }

    private Map<String, Long> languages() {
        return counts("SELECT language, COUNT(*) FROM onboarding_data GROUP BY language ORDER BY language");
    }

    private Map<String, Long> newAccounts(LocalDateTime start) {
        return counts("""
                SELECT CAST(created_at AS DATE), COUNT(*) FROM users WHERE created_at >= ?
                GROUP BY CAST(created_at AS DATE) ORDER BY 1
                """, start);
    }

    private Map<String, Long> usage(LocalDateTime start) {
        return counts("""
                SELECT CAST(last_updated AS DATE), COUNT(DISTINCT user_id) FROM progress WHERE last_updated >= ? AND xp > 0
                GROUP BY CAST(last_updated AS DATE) ORDER BY 1
                """, start);
    }

    private Map<String, Long> counts(String sql, Object... values) {
        return jdbc.query(sql, rows -> {
            Map<String, Long> result = new LinkedHashMap<>();
            while (rows.next()) {
                result.put(rows.getString(1), rows.getLong(2));
            }
            return result;
        }, values);
    }

    private AdminUser withRoles(AdminUser user) {
        return new AdminUser(user.id(), user.username(), user.email(), user.active(), user.xp(), user.courseCount(),
                user.joined(), accounts.findRoles(user.id()));
    }

    private AdminUser mapUser(ResultSet row, int index) throws SQLException {
        return new AdminUser(row.getInt("id"), row.getString("username"), row.getString("email"),
                row.getBoolean("is_active"), row.getLong("xp"), row.getInt("courses"),
                row.getString("created_at"), List.of());
    }

    private AdminCourse mapCourse(ResultSet row, int index) throws SQLException {
        return new AdminCourse(row.getInt("id"), row.getInt("user_id"), row.getString("owner"),
                row.getString("course_title"), row.getString("status"), row.getInt("chapters"),
                row.getInt("completed"), row.getInt("questions"), row.getString("created_at"));
    }
}
