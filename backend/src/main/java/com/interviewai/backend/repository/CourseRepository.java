package com.interviewai.backend.repository;

import com.interviewai.backend.dto.response.CourseSummary;
import com.interviewai.backend.model.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Integer> {

    String AVAILABLE = " where c.userId = :userId and (c.status is null or upper(c.status) <> 'ARCHIVED')";
    String SUMMARY = "select new com.interviewai.backend.dto.response.CourseSummary(c.id, c.title, c.status) from Course c";

    @Query(SUMMARY + AVAILABLE + " order by c.createdAt desc, c.id desc")
    List<CourseSummary> findAvailableCourses(int userId);

    @Query(SUMMARY + AVAILABLE + " and c.id = :courseId")
    Optional<CourseSummary> findAvailableCourse(int userId, int courseId);

    int countByUserId(int userId);
    int countByStatus(String status);

    @Modifying
    @Query("""
            update Course c set c.status = 'COMPLETED'
            where c.id = :courseId and (c.status is null or upper(c.status) <> 'ARCHIVED')
                and exists (select 1 from Chapter ch where ch.course = c)
                and not exists (select 1 from Chapter ch where ch.course = c
                    and (ch.status is null or ch.status <> 'COMPLETED'))
            """)
    void completeIfReady(int courseId);

    @Modifying
    @Query("update Course c set c.status = 'ACTIVE' where c.userId = :userId and upper(c.status) = 'COMPLETED'")
    void reopenCompleted(int userId);

    @Modifying
    @Query("update Course c set c.status = :status where c.id = :id")
    void setCourseStatus(int id, String status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Course c where c.userId = :userId")
    void deleteByOwner(int userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Course c where c.id = :id")
    void deleteCourse(int id);

    String ADMIN_COURSE = """
            select c.id as id, c.userId as userId, u.username as owner, c.title as title,
                   c.status as status, c.createdAt as createdAt,
                   (select count(ch) from Chapter ch where ch.course = c) as chapterCount,
                   (select count(ch) from Chapter ch where ch.course = c and ch.status = 'COMPLETED') as completed,
                   (select count(q) from Question q where q.chapter.course = c) as questionCount
            from Course c left join UserAccount u on u.id = c.userId
            """;

    String ADMIN_FILTER = """
            where (lower(coalesce(c.title, '')) like :search escape '\\'
                or lower(coalesce(u.username, '')) like :search escape '\\')
                and (:status = '' or upper(c.status) = :status)
            """;

    @Query(value = ADMIN_COURSE + ADMIN_FILTER + " order by c.id desc",
            countQuery = "select count(c) from Course c left join UserAccount u on u.id = c.userId " + ADMIN_FILTER)
    Page<CourseDetails> findAdminCourses(String search, String status, Pageable page);

    @Query(ADMIN_COURSE + " where c.id = :id")
    Optional<CourseDetails> findAdminCourse(int id);

    interface CourseDetails {
        int getId();
        int getUserId();
        String getOwner();
        String getTitle();
        String getStatus();
        LocalDateTime getCreatedAt();
        int getChapterCount();
        int getCompleted();
        int getQuestionCount();
    }
}
