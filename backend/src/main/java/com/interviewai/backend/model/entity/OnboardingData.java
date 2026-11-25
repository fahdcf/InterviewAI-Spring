package com.interviewai.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "onboarding_data")
public class OnboardingData {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "interview_type", length = 50, nullable = false)
    private String interviewType;

    @Column(name = "language", length = 50, nullable = false)
    private String language;

    @Column(name = "timeline", length = 50, nullable = false)
    private String timeline;

    @Column(name = "context", columnDefinition = "text")
    private String context;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "cv_path", length = 500)
    private String cvPath;

    @Column(name = "created_at", columnDefinition = "timestamp", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "timestamp", insertable = false, updatable = false, nullable = false)
    private LocalDateTime updatedAt;

    protected OnboardingData() {
    }
}
