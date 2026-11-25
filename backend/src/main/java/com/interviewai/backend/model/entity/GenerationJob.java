package com.interviewai.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_generation_jobs", uniqueConstraints = @UniqueConstraint(
        name = "uq_course_job_request", columnNames = {"user_id", "request_id"}))
public class GenerationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "request_id", length = 36, nullable = false)
    private String requestId;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "progress", nullable = false)
    private int progress;

    @Column(name = "message", length = 255, nullable = false)
    private String message;

    @Column(name = "interview_type", length = 50, nullable = false)
    private String interviewType;

    @Column(name = "language", length = 50, nullable = false)
    private String language;

    @Column(name = "timeline", length = 50, nullable = false)
    private String timeline;

    @Column(name = "context", columnDefinition = "text", nullable = false)
    private String context;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "cv_text", columnDefinition = "text")
    private String cvText;

    @Column(name = "technical_course_id")
    private Integer technicalCourseId;

    @Column(name = "softskills_course_id")
    private Integer softSkillsCourseId;

    @Column(name = "created_at", columnDefinition = "timestamp", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "timestamp", insertable = false, updatable = false, nullable = false)
    private LocalDateTime updatedAt;

    protected GenerationJob() {
    }
}
