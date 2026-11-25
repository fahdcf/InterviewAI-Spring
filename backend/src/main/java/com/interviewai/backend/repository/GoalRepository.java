package com.interviewai.backend.repository;

import com.interviewai.backend.model.entity.UserGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<UserGoal, Integer> {

    List<UserGoal> findByUserIdAndActiveTrueOrderByCreatedAtAscIdAsc(int userId);
    Optional<UserGoal> findByUserIdAndIdAndActiveTrue(int userId, int id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update UserGoal g set g.name = :name, g.type = :type, g.target = :target, g.startValue = :baseline
            where g.userId = :userId and g.id = :id and g.active = true
            """)
    void updateGoal(int userId, int id, String name, String type, int target, int baseline);

    @Modifying
    @Query("update UserGoal g set g.active = false where g.userId = :userId and g.id = :id and g.active = true")
    int removeGoal(int userId, int id);

    @Modifying
    @Query("update UserGoal g set g.startValue = 0, g.currentValue = 0 where g.userId = :userId")
    void resetProgress(int userId);
}
