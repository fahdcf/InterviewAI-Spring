package com.interviewai.backend.repository;

import com.interviewai.backend.model.CourseInput;
import com.interviewai.backend.model.CourseJob;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class CourseJobRepository {

    private static final String SUMMARY = """
            SELECT id, user_id, status, progress, message, technical_course_id, softskills_course_id
            FROM course_generation_jobs
            """;
    private final JdbcTemplate jdbc;

    public CourseJobRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void lockUser(int userId) {
        jdbc.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", Integer.class, userId);
    }

    public Optional<CourseJob> find(int userId, long id) {
        return jdbc.query(SUMMARY + " WHERE user_id = ? AND id = ?", this::map, userId, id).stream().findFirst();
    }

    public Optional<CourseJob> findByRequest(int userId, String requestId) {
        return jdbc.query(SUMMARY + " WHERE user_id = ? AND request_id = ?", this::map, userId, requestId)
                .stream().findFirst();
    }

    public Optional<CourseJob> findActive(int userId) {
        return jdbc.query(SUMMARY + " WHERE user_id = ? AND status IN ('QUEUED', 'RUNNING') ORDER BY id DESC LIMIT 1",
                this::map, userId).stream().findFirst();
    }

    public Optional<CourseJob> findLatest(int userId) {
        return jdbc.query(SUMMARY + " WHERE user_id = ? ORDER BY id DESC LIMIT 1",
                this::map, userId).stream().findFirst();
    }

    public boolean queue(int userId, String requestId) {
        return jdbc.update("""
                INSERT INTO course_generation_jobs
                    (user_id, request_id, interview_type, language, timeline, context, description, cv_text)
                SELECT o.user_id, ?, o.interview_type, o.language, o.timeline, o.context, o.description, cv.extracted_text
                FROM onboarding_data o LEFT JOIN onboarding_cv cv ON cv.user_id = o.user_id
                WHERE o.user_id = ?
                """, requestId, userId) == 1;
    }

    public Optional<CourseJob> nextQueued() {
        return jdbc.query(SUMMARY + " WHERE status = 'QUEUED' ORDER BY id LIMIT 1", this::map).stream().findFirst();
    }

    public boolean start(long id) {
        return jdbc.update("""
                UPDATE course_generation_jobs SET status = 'RUNNING', message = 'Generating outlines…'
                WHERE id = ? AND status = 'QUEUED'
                """, id) == 1;
    }

    public CourseInput findInput(long id) {
        return jdbc.queryForObject("""
                SELECT interview_type, language, timeline, context, description, cv_text
                FROM course_generation_jobs WHERE id = ?
                """, (row, index) -> new CourseInput(row.getString("interview_type"), row.getString("language"),
                row.getString("timeline"), row.getString("context"), row.getString("description"),
                row.getString("cv_text")), id);
    }

    public void saveProgress(long id, int progress, String message) {
        jdbc.update("UPDATE course_generation_jobs SET progress = ?, message = ? WHERE id = ? AND status = 'RUNNING'",
                progress, message, id);
    }

    public void succeed(long id, int technical, int softSkills) {
        int updated = jdbc.update("""
                UPDATE course_generation_jobs
                SET status = 'SUCCEEDED', progress = 100, message = 'Courses ready.',
                    technical_course_id = ?, softskills_course_id = ?, cv_text = NULL
                WHERE id = ? AND status = 'RUNNING'
                """, technical, softSkills, id);
        if (updated != 1) {
            throw new IllegalStateException("The generation job is no longer running.");
        }
    }

    public void stop(long id, String status, String message) {
        jdbc.update("""
                UPDATE course_generation_jobs SET status = ?, message = ?, cv_text = NULL
                WHERE id = ? AND status = 'RUNNING'
                """, status, message, id);
    }

    public void recoverInterruptedJobs() {
        jdbc.update("""
                UPDATE course_generation_jobs
                SET status = 'INTERRUPTED', message = 'The server restarted. You can retry.', cv_text = NULL
                WHERE status = 'RUNNING'
                """);
    }

    private CourseJob map(ResultSet row, int index) throws SQLException {
        return new CourseJob(row.getLong("id"), row.getInt("user_id"), row.getString("status"),
                row.getInt("progress"), row.getString("message"), row.getObject("technical_course_id", Integer.class),
                row.getObject("softskills_course_id", Integer.class));
    }
}
