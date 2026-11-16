package com.interviewai.backend.repository;

import com.interviewai.backend.dto.request.OnboardingRequest;
import com.interviewai.backend.dto.response.OnboardingResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OnboardingRepository {

    private final JdbcTemplate jdbc;

    public OnboardingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<OnboardingResponse> find(int userId) {
        return jdbc.query("""
                SELECT o.interview_type, o.language, o.timeline, o.context, o.description, cv.user_id AS cv_owner
                FROM onboarding_data o LEFT JOIN onboarding_cv cv ON cv.user_id = o.user_id
                WHERE o.user_id = ?
                """, (row, index) -> new OnboardingResponse(row.getString("interview_type"),
                row.getString("language"), row.getString("timeline"), row.getString("context"),
                row.getString("description"), row.getObject("cv_owner") != null), userId).stream().findFirst();
    }

    public void save(int userId, OnboardingRequest request) {
        String type = request.getInterviewType();
        String language = request.getLanguage();
        String timeline = request.getTimeline();
        String context = request.getContext().strip();
        String description = request.getDescription();
        jdbc.update("""
                INSERT INTO onboarding_data (user_id, interview_type, language, timeline, context, description)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE interview_type = ?, language = ?, timeline = ?, context = ?, description = ?
                """, userId, type, language, timeline, context, description,
                type, language, timeline, context, description);
    }

    public void saveCv(int userId, String text) {
        jdbc.update("""
                INSERT INTO onboarding_cv (user_id, extracted_text) VALUES (?, ?)
                ON DUPLICATE KEY UPDATE extracted_text = ?
                """, userId, text, text);
    }

    public void removeCv(int userId) {
        jdbc.update("DELETE FROM onboarding_cv WHERE user_id = ?", userId);
    }
}
