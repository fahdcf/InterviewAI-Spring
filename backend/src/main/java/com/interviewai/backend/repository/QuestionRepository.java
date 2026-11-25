package com.interviewai.backend.repository;

import com.interviewai.backend.model.PracticeQuestion;
import com.interviewai.backend.model.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Integer> {

    String AVAILABLE = """
            select new com.interviewai.backend.model.PracticeQuestion(
                q.id, c.id, g.id, q.text, q.type, q.status, q.correctAnswer, q.explanation)
            from Question q join q.chapter c join c.course g
            where g.userId = :userId and (g.status is null or upper(g.status) <> 'ARCHIVED')
            """;

    @Query(AVAILABLE + " and c.id = :chapterId and (:review = false or q.status = 'INCORRECT') order by q.id")
    List<PracticeQuestion> listQuestions(int userId, int chapterId, boolean review);

    @Query(AVAILABLE + " and g.id = :courseId and q.status = 'INCORRECT' order by c.number, q.id")
    List<PracticeQuestion> listCourseReview(int userId, int courseId);

    @Query(AVAILABLE + " and q.id = :questionId")
    Optional<PracticeQuestion> findQuestion(int userId, int questionId);

    int countByStatusIn(Collection<String> statuses);
    int countByChapterCourseUserIdAndStatusIn(int userId, Collection<String> statuses);
    int countByChapterCourseUserIdAndStatus(int userId, String status);
    int countByChapterIdAndStatusNotIn(int chapterId, Collection<String> statuses);
    int countByChapterCourseUserIdAndStatusInAndUpdatedAtAfter(int userId, Collection<String> statuses, LocalDateTime start);

    @Modifying
    @Query(value = """
            UPDATE questions SET status = :status, updated_at = CURRENT_TIMESTAMP(6)
            WHERE id = :id AND status <> 'COMPLETED'
            """, nativeQuery = true)
    void updateStatus(int id, String status);

    @Modifying
    @Query(value = """
            UPDATE questions SET status = 'NOT_STARTED', updated_at = CURRENT_TIMESTAMP
            WHERE chapter_id IN (SELECT c.id FROM chapters c JOIN generated_courses g ON c.course_id = g.id
                WHERE g.user_id = :userId)
            """, nativeQuery = true)
    void resetProgress(int userId);
}
