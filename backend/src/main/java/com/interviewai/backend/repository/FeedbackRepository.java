package com.interviewai.backend.repository;

import com.interviewai.backend.model.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {

    @Modifying
    @Query("""
            delete from Feedback f where f.userId = :userId
                or f.courseId in (select c.id from Course c where c.userId = :userId)
            """)
    void deleteByUserOrOwnedCourse(int userId);

    @Modifying
    @Query("delete from Feedback f where f.courseId = :courseId")
    void deleteForCourse(int courseId);
}
