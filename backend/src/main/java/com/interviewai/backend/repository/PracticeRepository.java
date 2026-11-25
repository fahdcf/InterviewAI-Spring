package com.interviewai.backend.repository;

import com.interviewai.backend.model.PracticeAttempt;
import com.interviewai.backend.model.entity.AnswerAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PracticeRepository extends JpaRepository<AnswerAttempt, Long> {

    Optional<PracticeAttempt> findByUserIdAndId(int userId, long id);
    Optional<PracticeAttempt> findByUserIdAndRequestId(int userId, String requestId);
    Optional<PracticeAttempt> findFirstByStatusOrderByIdAsc(String status);
    int countByUserIdAndStatusIn(int userId, List<String> statuses);

    String ATTEMPT = """
            select new com.interviewai.backend.model.PracticeAttempt(
                a.id, a.userId, a.questionId, a.mode, a.answer, a.status,
                a.score, a.strengths, a.improvements, a.explanation, a.xpAwarded)
            from AnswerAttempt a
            """;

    @Query(ATTEMPT + """
            where a.id in (
                select max(p.id) from AnswerAttempt p join Question q on q.id = p.questionId
                where p.userId = :userId and q.chapter.course.id = :courseId group by p.questionId
            )
            """)
    List<PracticeAttempt> latestAttempts(int userId, int courseId);

    @Transactional
    @Modifying
    @Query("update AnswerAttempt a set a.status = 'RUNNING' where a.id = :id and a.status = 'QUEUED'")
    int start(long id);

    @Transactional
    @Modifying
    @Query("""
            update AnswerAttempt a set a.status = 'FAILED', a.explanation = :message
            where a.id = :id and a.status = 'RUNNING'
            """)
    void fail(long id, String message);

    @Transactional
    @Modifying
    @Query("""
            update AnswerAttempt a set a.status = 'FAILED', a.explanation = 'Grading was interrupted. Try again.'
            where a.status = 'RUNNING'
            """)
    void recoverInterruptedAttempts();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AnswerAttempt a set a.status = 'GRADED', a.score = :score, a.strengths = :strengths,
                a.improvements = :improvements, a.explanation = :explanation, a.xpAwarded = :xp
            where a.id = :id and a.status = 'RUNNING'
            """)
    int complete(long id, int score, String strengths, String improvements, String explanation, int xp);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from AnswerAttempt a where a.userId = :userId")
    void deleteForUser(int userId);
}
