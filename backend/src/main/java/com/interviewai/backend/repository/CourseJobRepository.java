package com.interviewai.backend.repository;

import com.interviewai.backend.model.CourseInput;
import com.interviewai.backend.model.CourseJob;
import com.interviewai.backend.model.entity.GenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CourseJobRepository extends JpaRepository<GenerationJob, Long> {

    Optional<CourseJob> findByUserIdAndId(int userId, long id);
    Optional<CourseJob> findByUserIdAndRequestId(int userId, String requestId);
    Optional<CourseJob> findFirstByUserIdAndStatusInOrderByIdDesc(int userId, List<String> statuses);
    Optional<CourseJob> findFirstByUserIdOrderByIdDesc(int userId);
    Optional<CourseJob> findFirstByStatusOrderByIdAsc(String status);

    default Optional<CourseJob> findActive(int userId) {
        return findFirstByUserIdAndStatusInOrderByIdDesc(userId, List.of("QUEUED", "RUNNING"));
    }

    @Modifying
    @Query(value = """
            INSERT INTO course_generation_jobs
                (user_id, request_id, interview_type, language, timeline, context, description, cv_text)
            SELECT o.user_id, :requestId, o.interview_type, o.language, o.timeline, o.context,
                o.description, cv.extracted_text
            FROM onboarding_data o LEFT JOIN onboarding_cv cv ON cv.user_id = o.user_id
            WHERE o.user_id = :userId
            """, nativeQuery = true)
    int queue(int userId, String requestId);

    @Transactional
    @Modifying
    @Query("""
            update GenerationJob j set j.status = 'RUNNING', j.message = 'Generating outlines…'
            where j.id = :id and j.status = 'QUEUED'
            """)
    int start(long id);

    @Query("""
            select new com.interviewai.backend.model.CourseInput(
                j.interviewType, j.language, j.timeline, j.context, j.description, j.cvText)
            from GenerationJob j where j.id = :id
            """)
    CourseInput findInput(long id);

    @Transactional
    @Modifying
    @Query("""
            update GenerationJob j set j.progress = :progress, j.message = :message
            where j.id = :id and j.status = 'RUNNING'
            """)
    void saveProgress(long id, int progress, String message);

    @Modifying(flushAutomatically = true)
    @Query("""
            update GenerationJob j set j.status = 'SUCCEEDED', j.progress = 100, j.message = 'Courses ready.',
                j.technicalCourseId = :technical, j.softSkillsCourseId = :softSkills, j.cvText = null
            where j.id = :id and j.status = 'RUNNING'
            """)
    int succeed(long id, int technical, int softSkills);

    @Transactional
    @Modifying
    @Query("""
            update GenerationJob j set j.status = :status, j.message = :message, j.cvText = null
            where j.id = :id and j.status = 'RUNNING'
            """)
    void stop(long id, String status, String message);

    @Transactional
    @Modifying
    @Query("""
            update GenerationJob j set j.status = 'INTERRUPTED',
                j.message = 'The server restarted. You can retry.', j.cvText = null
            where j.status = 'RUNNING'
            """)
    void recoverInterruptedJobs();
}
