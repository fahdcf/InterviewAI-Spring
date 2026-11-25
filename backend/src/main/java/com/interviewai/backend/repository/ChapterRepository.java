package com.interviewai.backend.repository;

import com.interviewai.backend.model.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Integer> {

    @Query("""
            select c.id as id, coalesce(c.number, 0) as number, c.name as name,
                   c.description as description, c.status as status, count(q) as questionCount,
                   sum(case when q.status in ('COMPLETED', 'INCORRECT') then 1 else 0 end) as answeredCount
            from Chapter c left join Question q on q.chapter = c
            where c.course.id = :courseId
            group by c.id, c.number, c.name, c.description, c.status order by c.number, c.id
            """)
    List<ChapterDetails> findChapters(int courseId);

    @Query("""
            select count(c) > 0 from Chapter c where c.id = :chapterId and c.course.userId = :userId
                and (c.course.status is null or upper(c.course.status) <> 'ARCHIVED')
            """)
    boolean ownsChapter(int userId, int chapterId);

    int countByStatus(String status);
    int countByCourseUserIdAndStatus(int userId, String status);
    int countByCourseUserIdAndStatusAndCompletedAtGreaterThanEqual(int userId, String status, LocalDateTime start);

    @Modifying
    @Query("""
            update Chapter c set c.status = 'COMPLETED', c.completedAt = coalesce(c.completedAt, local datetime)
            where c.id = :chapterId
            """)
    void completeChapter(int chapterId);

    @Modifying
    @Query("update Chapter c set c.status = 'IN_PROGRESS' where c.id = :chapterId and c.status = 'NOT_STARTED'")
    void startChapter(int chapterId);

    @Modifying
    @Query("""
            update Chapter c set c.status = 'NOT_STARTED', c.completedAt = null
            where c.course.id in (select g.id from Course g where g.userId = :userId)
            """)
    void resetProgress(int userId);

    interface ChapterDetails {
        int getId();
        int getNumber();
        String getName();
        String getDescription();
        String getStatus();
        int getQuestionCount();
        int getAnsweredCount();
    }
}
