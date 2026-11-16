package com.interviewai.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class QuestRepository {

    private final JdbcTemplate jdbc;

    public QuestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int answeredSince(int userId, LocalDateTime start) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM questions q JOIN chapters c ON c.id = q.chapter_id
                JOIN generated_courses g ON g.id = c.course_id
                WHERE g.user_id = ? AND q.status IN ('COMPLETED', 'INCORRECT', 'IN_PROGRESS') AND q.updated_at > ?
                """, Integer.class, userId, start);
    }

    public LocalDateTime dailyReset(int userId) {
        return jdbc.queryForObject("SELECT daily_quest_last_reset FROM users WHERE id = ?",
                LocalDateTime.class, userId);
    }

    public LocalDate monthlyClaimDate(int userId) {
        return jdbc.queryForObject("SELECT last_monthly_quest_claim FROM users WHERE id = ?",
                LocalDate.class, userId);
    }

    public void recordDailyReset(int userId, LocalDate today, LocalDateTime now) {
        jdbc.update("""
                UPDATE users SET daily_quest_last_reset = ?, last_daily_quest_claim = ? WHERE id = ?
                """, now, today, userId);
    }

    public void recordMonthlyClaim(int userId, LocalDate today) {
        jdbc.update("UPDATE users SET last_monthly_quest_claim = ? WHERE id = ?", today, userId);
    }

    public Optional<QuestClaim> previousClaim(int userId, String requestId) {
        return jdbc.query("""
                SELECT quest_type, xp_awarded FROM quest_claims WHERE user_id = ? AND request_id = ?
                """, (row, index) -> new QuestClaim(row.getString("quest_type"), row.getInt("xp_awarded")),
                userId, requestId).stream().findFirst();
    }

    public void saveClaim(int userId, String requestId, String type, int xp) {
        jdbc.update("""
                INSERT INTO quest_claims (user_id, request_id, quest_type, xp_awarded) VALUES (?, ?, ?, ?)
                """, userId, requestId, type, xp);
    }

    public record QuestClaim(String type, int xpAwarded) {
    }
}
