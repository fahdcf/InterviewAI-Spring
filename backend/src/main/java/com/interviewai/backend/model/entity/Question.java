package com.interviewai.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(name = "question", columnDefinition = "text")
    private String text;

    @Column(name = "question_type", length = 50)
    private String type;

    @Column(name = "correct_answer", length = 255)
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "text")
    private String explanation;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "updated_at", columnDefinition = "datetime(6)", insertable = false, updatable = false, nullable = false)
    private LocalDateTime updatedAt;

    protected Question() {
    }

    public Question(Chapter chapter, String text, String type, String correctAnswer, String explanation) {
        this.chapter = chapter;
        this.text = text;
        this.type = type;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.status = "NOT_STARTED";
    }
}
