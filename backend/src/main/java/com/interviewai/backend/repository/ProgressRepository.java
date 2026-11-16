package com.interviewai.backend.repository;

import com.interviewai.backend.dto.response.GoalResponse;
import com.interviewai.backend.dto.response.LeaderboardEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ProgressRepository {

    private final JdbcTemplate jdbc;

    public ProgressRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<XpEvent> xpSince(int userId, LocalDateTime start) {
        return jdbc.query("""
                SELECT xp, last_updated FROM progress WHERE user_id = ? AND last_updated >= ?
                ORDER BY last_updated, id
                """, (row, index) -> new XpEvent(row.getObject("last_updated", LocalDateTime.class), row.getInt("xp")),
                userId, start);
    }

    public int answered(int userId) {
        return count("""
                SELECT COUNT(*) FROM questions q JOIN chapters c ON c.id = q.chapter_id
                JOIN generated_courses g ON g.id = c.course_id
                WHERE g.user_id = ? AND q.status IN ('COMPLETED', 'INCORRECT')
                """, userId);
    }

    public int correct(int userId) {
        return count("""
                SELECT COUNT(*) FROM questions q JOIN chapters c ON c.id = q.chapter_id
                JOIN generated_courses g ON g.id = c.course_id WHERE g.user_id = ? AND q.status = 'COMPLETED'
                """, userId);
    }

    public int completedChapters(int userId, LocalDateTime start) {
        String sql = """
                SELECT COUNT(*) FROM chapters c JOIN generated_courses g ON g.id = c.course_id
                WHERE g.user_id = ? AND c.status = 'COMPLETED'
                """;
        if (start == null) {
            return count(sql, userId);
        }
        return count(sql + " AND c.completed_at >= ?", userId, start);
    }

    public int totalCourses(int userId) {
        return count("SELECT COUNT(*) FROM generated_courses WHERE user_id = ?", userId);
    }

    public int positiveXpEvents(int userId) {
        return count("SELECT COUNT(*) FROM progress WHERE user_id = ? AND xp > 0", userId);
    }

    public List<LeaderboardEntry> leaderboard() {
        return jdbc.query("""
                SELECT u.username, COALESCE(SUM(p.xp), 0) AS total_xp
                FROM users u LEFT JOIN progress p ON p.user_id = u.id
                WHERE u.is_active = TRUE
                GROUP BY u.id, u.username ORDER BY total_xp DESC, u.username ASC LIMIT 20
                """, (row, index) -> new LeaderboardEntry(row.getString("username"), row.getLong("total_xp")));
    }

    public List<GoalResponse> goals(int userId) {
        return readGoals("""
                SELECT id, goal_name, goal_type, target_value, start_value FROM user_goals
                WHERE user_id = ? AND is_active = TRUE ORDER BY created_at, id
                """, userId);
    }

    public Optional<GoalResponse> findGoal(int userId, int id) {
        return readGoals("""
                SELECT id, goal_name, goal_type, target_value, start_value FROM user_goals
                WHERE user_id = ? AND id = ? AND is_active = TRUE
                """, userId, id).stream().findFirst();
    }

    private List<GoalResponse> readGoals(String sql, Object... parameters) {
        return jdbc.query(sql, (row, index) -> new GoalResponse(row.getInt("id"), row.getString("goal_name"),
                row.getString("goal_type"), row.getInt("target_value"), row.getInt("start_value"), 0), parameters);
    }

    public void updateGoal(int userId, int id, String name, String type, int target, int baseline) {
        jdbc.update("""
                UPDATE user_goals SET goal_name = ?, goal_type = ?, target_value = ?, start_value = ?
                WHERE user_id = ? AND id = ? AND is_active = TRUE
                """, name, type, target, baseline, userId, id);
    }

    public void addGoal(int userId, String name, String type, int target, int baseline) {
        jdbc.update("""
                INSERT INTO user_goals (user_id, goal_name, goal_type, target_value, start_value)
                VALUES (?, ?, ?, ?, ?)
                """, userId, name, type, target, baseline);
    }

    public boolean removeGoal(int userId, int id) {
        return jdbc.update("""
                UPDATE user_goals SET is_active = FALSE WHERE user_id = ? AND id = ? AND is_active = TRUE
                """, userId, id) == 1;
    }

    private int count(String sql, Object... args) {
        Integer result = jdbc.queryForObject(sql, Integer.class, args);
        return result == null ? 0 : result;
    }

    public record XpEvent(LocalDateTime time, int xp) {
    }
}
