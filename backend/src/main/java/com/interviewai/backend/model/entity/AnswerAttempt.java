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
@Table(name = "practice_attempts", uniqueConstraints = @UniqueConstraint(
        name = "uq_practice_request", columnNames = {"user_id", "request_id"}))
public class AnswerAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "question_id", nullable = false)
    private int questionId;

    @Column(name = "request_id", length = 36, nullable = false)
    private String requestId;

    @Column(name = "mode", length = 10, nullable = false)
    private String mode;

    @Column(name = "answer", columnDefinition = "text")
    private String answer;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "score")
    private Integer score;

    @Column(name = "strengths", columnDefinition = "text")
    private String strengths;

    @Column(name = "improvements", columnDefinition = "text")
    private String improvements;

    @Column(name = "explanation", columnDefinition = "text")
    private String explanation;

    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded;

    @Column(name = "created_at", columnDefinition = "timestamp", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    protected AnswerAttempt() {
    }

    public AnswerAttempt(int userId, int questionId, String requestId, String mode, String answer, String status) {
        this.userId = userId;
        this.questionId = questionId;
        this.requestId = requestId;
        this.mode = mode;
        this.answer = answer;
        this.status = status;
    }

    public Long getId() { return id; }
}
