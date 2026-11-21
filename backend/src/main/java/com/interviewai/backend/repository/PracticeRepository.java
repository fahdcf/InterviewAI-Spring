package com.interviewai.backend.repository;

import com.interviewai.backend.model.AnswerGrade;
import com.interviewai.backend.model.PracticeAttempt;
import com.interviewai.backend.model.PracticeQuestion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PracticeRepository {

    private static final String QUESTIONS = """
            SELECT q.id, q.chapter_id, g.id AS course_id, q.question, q.question_type, q.status,
                   q.correct_answer, q.explanation
            FROM questions q JOIN chapters c ON c.id = q.chapter_id
            JOIN generated_courses g ON g.id = c.course_id
            WHERE g.user_id = ? AND (g.status IS NULL OR UPPER(g.status) <> 'ARCHIVED')
            """;
    private static final String ATTEMPTS = """
            SELECT id, user_id, question_id, mode, answer, status, score, strengths, improvements, explanation, xp_awarded
            FROM practice_attempts
            """;
    private final JdbcTemplate jdbc;

    public PracticeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PracticeQuestion> listQuestions(int userId, int chapterId, boolean review) {
        String sql = QUESTIONS + " AND q.chapter_id = ?";
        if (review) {
            sql += " AND q.status = 'INCORRECT'";
        }
        return jdbc.query(sql + " ORDER BY q.id", this::mapQuestion, userId, chapterId);
    }

    public List<PracticeQuestion> listCourseReview(int userId, int courseId) {
        return jdbc.query(QUESTIONS + " AND g.id = ? AND q.status = 'INCORRECT' ORDER BY c.chapter_number, q.id",
                this::mapQuestion, userId, courseId);
    }

    public Optional<PracticeQuestion> findQuestion(int userId, int questionId) {
        return jdbc.query(QUESTIONS + " AND q.id = ?", this::mapQuestion, userId, questionId).stream().findFirst();
    }

    public boolean ownsChapter(int userId, int chapterId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM chapters c JOIN generated_courses g ON g.id = c.course_id
                WHERE g.user_id = ? AND c.id = ? AND (g.status IS NULL OR UPPER(g.status) <> 'ARCHIVED')
                """, Integer.class, userId, chapterId);
        return count != null && count > 0;
    }

    public Map<Integer, List<String>> findCourseChoices(int courseId) {
        return jdbc.query("""
                SELECT a.question_id, a.choice_text
                FROM choices a JOIN questions q ON q.id = a.question_id
                JOIN chapters c ON c.id = q.chapter_id
                WHERE c.course_id = ? ORDER BY a.question_id, a.id
                """, rows -> {
                    Map<Integer, List<String>> choices = new HashMap<>();
                    while (rows.next()) {
                        String text = rows.getString("choice_text");
                        if (text != null) {
                            choices.computeIfAbsent(rows.getInt("question_id"), id -> new ArrayList<>()).add(text);
                        }
                    }
                    return choices;
                }, courseId);
    }

    public List<String> findChoices(int questionId) {
        return jdbc.queryForList("""
                SELECT choice_text FROM choices WHERE question_id = ? AND choice_text IS NOT NULL ORDER BY id
                """, String.class, questionId);
    }

    public Optional<PracticeAttempt> findAttempt(int userId, long id) {
        return jdbc.query(ATTEMPTS + " WHERE user_id = ? AND id = ?", this::mapAttempt, userId, id)
                .stream().findFirst();
    }

    public Optional<PracticeAttempt> findByRequest(int userId, String requestId) {
        return jdbc.query(ATTEMPTS + " WHERE user_id = ? AND request_id = ?", this::mapAttempt, userId, requestId)
                .stream().findFirst();
    }

    public long addAttempt(int userId, int questionId, String requestId, String mode, String answer, String status) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO practice_attempts (user_id, question_id, request_id, mode, answer, status)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setInt(1, userId);
            statement.setInt(2, questionId);
            statement.setString(3, requestId);
            statement.setString(4, mode);
            statement.setString(5, answer);
            statement.setString(6, status);
            return statement;
        }, key);
        return java.util.Objects.requireNonNull(key.getKey(), "Missing attempt ID.").longValue();
    }

    public int pendingCount(int userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM practice_attempts WHERE user_id = ?"
                + " AND status IN ('QUEUED', 'RUNNING')", Integer.class, userId);
    }

    public Map<Integer, PracticeAttempt> latestAttempts(int userId, int courseId) {
        List<PracticeAttempt> attempts = jdbc.query(ATTEMPTS + """
                 WHERE id IN (
                    SELECT MAX(a.id) FROM practice_attempts a
                    JOIN questions q ON q.id = a.question_id
                    JOIN chapters c ON c.id = q.chapter_id
                    WHERE a.user_id = ? AND c.course_id = ? GROUP BY a.question_id
                 )
                """, this::mapAttempt, userId, courseId);
        Map<Integer, PracticeAttempt> latest = new HashMap<>();
        for (PracticeAttempt attempt : attempts) {
            latest.put(attempt.questionId(), attempt);
        }
        return latest;
    }

    public Optional<PracticeAttempt> nextQueued() {
        return jdbc.query(ATTEMPTS + " WHERE status = 'QUEUED' ORDER BY id LIMIT 1", this::mapAttempt)
                .stream().findFirst();
    }

    public boolean start(long id) {
        return jdbc.update("UPDATE practice_attempts SET status = 'RUNNING' WHERE id = ? AND status = 'QUEUED'",
                id) == 1;
    }

    public void fail(long id, String message) {
        jdbc.update("""
                UPDATE practice_attempts SET status = 'FAILED', explanation = ? WHERE id = ? AND status = 'RUNNING'
                """, message, id);
    }

    public void recoverInterruptedAttempts() {
        jdbc.update("""
                UPDATE practice_attempts SET status = 'FAILED', explanation = 'Grading was interrupted. Try again.'
                WHERE status = 'RUNNING'
                """);
    }

    public void complete(long id, AnswerGrade grade, int xp) {
        int updated = jdbc.update("""
                UPDATE practice_attempts
                SET status = 'GRADED', score = ?, strengths = ?, improvements = ?, explanation = ?, xp_awarded = ?
                WHERE id = ? AND status = 'RUNNING'
                """, grade.score(), grade.strengths(), grade.improvements(), grade.explanation(), xp, id);
        if (updated != 1) {
            throw new IllegalStateException("The answer attempt is no longer running.");
        }
    }

    public void updateQuestion(PracticeQuestion question, boolean passed) {
        jdbc.update("""
                UPDATE questions SET status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status <> 'COMPLETED'
                """, passed ? "COMPLETED" : "INCORRECT", question.id());
    }

    public void updateChapter(int chapterId) {
        Integer remaining = jdbc.queryForObject("""
                SELECT COUNT(*) FROM questions WHERE chapter_id = ? AND status NOT IN ('COMPLETED', 'INCORRECT')
                """, Integer.class, chapterId);
        if (remaining != null && remaining == 0) {
            jdbc.update("""
                    UPDATE chapters SET status = 'COMPLETED', completed_at = COALESCE(completed_at, CURRENT_TIMESTAMP)
                    WHERE id = ?
                    """, chapterId);
        } else {
            jdbc.update("UPDATE chapters SET status = 'IN_PROGRESS' WHERE id = ? AND status = 'NOT_STARTED'", chapterId);
        }
    }

    public void awardXp(int userId, int courseId, int xp) {
        jdbc.update("INSERT INTO progress (user_id, course_id, xp) VALUES (?, ?, ?)", userId, courseId, xp);
    }

    private PracticeQuestion mapQuestion(ResultSet row, int index) throws SQLException {
        return new PracticeQuestion(row.getInt("id"), row.getInt("chapter_id"), row.getInt("course_id"),
                row.getString("question"), row.getString("question_type"), row.getString("status"),
                row.getString("correct_answer"), row.getString("explanation"));
    }

    private PracticeAttempt mapAttempt(ResultSet row, int index) throws SQLException {
        return new PracticeAttempt(row.getLong("id"), row.getInt("user_id"), row.getInt("question_id"),
                row.getString("mode"), row.getString("answer"), row.getString("status"),
                row.getObject("score", Integer.class), row.getString("strengths"), row.getString("improvements"),
                row.getString("explanation"), row.getInt("xp_awarded"));
    }
}
