package com.interviewai.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "onboarding_cv")
public class OnboardingCv {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "extracted_text", columnDefinition = "text", nullable = false)
    private String extractedText;

    protected OnboardingCv() {
    }
}
