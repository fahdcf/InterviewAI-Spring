package com.interviewai.backend.repository;

import com.interviewai.backend.dto.response.OnboardingResponse;
import com.interviewai.backend.model.entity.OnboardingData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OnboardingRepository extends JpaRepository<OnboardingData, Integer> {

    @Query("""
            select new com.interviewai.backend.dto.response.OnboardingResponse(
                o.interviewType, o.language, o.timeline, o.context, o.description,
                case when cv.userId is not null then true else false end)
            from OnboardingData o left join OnboardingCv cv on cv.userId = o.userId
            where o.userId = :userId
            """)
    Optional<OnboardingResponse> find(int userId);

    @Modifying
    @Query(value = """
            INSERT INTO onboarding_data (user_id, interview_type, language, timeline, context, description)
            VALUES (:userId, :type, :language, :timeline, :context, :description)
            ON DUPLICATE KEY UPDATE interview_type = :type, language = :language, timeline = :timeline,
                context = :context, description = :description
            """, nativeQuery = true)
    void savePreferences(int userId, String type, String language, String timeline, String context, String description);

    @Query("""
            select o.interviewType as label, count(o) as total from OnboardingData o
            group by o.interviewType order by o.interviewType
            """)
    List<CategoryCount> countInterviewTypes();

    @Query("""
            select o.language as label, count(o) as total from OnboardingData o
            group by o.language order by o.language
            """)
    List<CategoryCount> countLanguages();

    interface CategoryCount {
        String getLabel();
        long getTotal();
    }
}
