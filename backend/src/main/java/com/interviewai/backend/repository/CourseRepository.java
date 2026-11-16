package com.interviewai.backend.repository;

import com.interviewai.backend.dto.response.ChapterSummary;
import com.interviewai.backend.dto.response.CourseSummary;
import com.interviewai.backend.dto.response.CoursePair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CourseRepository {

    private final JdbcTemplate jdbc;

    public CourseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CourseSummary> findAvailableCourses(int userId) {
        return jdbc.query("""
                SELECT id, course_title, status
                FROM generated_courses
                WHERE user_id = ? AND (status IS NULL OR UPPER(status) <> 'ARCHIVED')
                ORDER BY created_at DESC, id DESC
                """, (row, index) -> new CourseSummary(row.getInt("id"), row.getString("course_title"),
                row.getString("status")), userId);
    }

    public List<CoursePair> findPairs(int userId) {
        return jdbc.query("""
                SELECT id, technical_course_id, softskills_course_id
                FROM interview_prep
                WHERE user_id = ?
                ORDER BY created_at DESC, id DESC
                """, (row, index) -> new CoursePair(row.getInt("id"),
                row.getObject("technical_course_id", Integer.class),
                row.getObject("softskills_course_id", Integer.class)), userId);
    }

    public List<ChapterSummary> findChapters(int courseId) {
        return jdbc.query("""
                SELECT c.id, c.chapter_number, c.name, c.description, c.status,
                       COUNT(q.id) AS question_count,
                       SUM(CASE WHEN q.status IN ('COMPLETED', 'INCORRECT') THEN 1 ELSE 0 END) AS answered_count
                FROM chapters c LEFT JOIN questions q ON q.chapter_id = c.id
                WHERE c.course_id = ?
                GROUP BY c.id, c.chapter_number, c.name, c.description, c.status
                ORDER BY c.chapter_number, c.id
                """, (row, index) -> new ChapterSummary(row.getInt("id"),
                row.getInt("chapter_number"), row.getString("name"), row.getString("description"),
                row.getString("status"), row.getInt("question_count"), row.getInt("answered_count")),
                courseId);
    }

    public Optional<CourseSummary> findAvailableCourse(int userId, int courseId) {
        return findAvailableCourses(userId).stream().filter(course -> course.getId() == courseId).findFirst();
    }
}
