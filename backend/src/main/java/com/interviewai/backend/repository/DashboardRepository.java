package com.interviewai.backend.repository;

import com.interviewai.backend.dto.request.NavigationRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbc;

    public DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public NavigationRequest findLocation(int userId) {
        return jdbc.queryForObject("""
                SELECT last_course_id, last_chapter_id FROM users WHERE id = ?
                """, (row, index) -> {
                    NavigationRequest location = new NavigationRequest();
                    location.setCourseId(row.getObject("last_course_id", Integer.class));
                    location.setChapterId(row.getObject("last_chapter_id", Integer.class));
                    return location;
                }, userId);
    }

    public void saveLocation(int userId, int courseId, Integer chapterId) {
        jdbc.update("UPDATE users SET last_course_id = ?, last_chapter_id = ? WHERE id = ?",
                courseId, chapterId, userId);
    }

    public long findTotalXp(int userId) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(xp), 0) FROM progress WHERE user_id = ?",
                Long.class, userId);
    }

    public List<LocalDate> findActivityDates(int userId, ZoneId zone) {
        return jdbc.query("""
                SELECT last_updated
                FROM progress WHERE user_id = ? AND xp > 0
                ORDER BY last_updated DESC
                """, (row, index) -> row.getObject("last_updated", LocalDateTime.class)
                .atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalDate(), userId).stream().distinct().toList();
    }
}
