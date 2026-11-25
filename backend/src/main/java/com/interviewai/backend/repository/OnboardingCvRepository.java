package com.interviewai.backend.repository;

import com.interviewai.backend.model.entity.OnboardingCv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OnboardingCvRepository extends JpaRepository<OnboardingCv, Integer> {

    @Modifying
    @Query(value = """
            INSERT INTO onboarding_cv (user_id, extracted_text) VALUES (:userId, :text)
            ON DUPLICATE KEY UPDATE extracted_text = :text
            """, nativeQuery = true)
    void saveCv(int userId, String text);

    @Modifying
    @Query("delete from OnboardingCv cv where cv.userId = :userId")
    void removeCv(int userId);
}
