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
@Table(name = "chapters")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "chapter_number")
    private Integer number;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "completed_at", columnDefinition = "datetime")
    private LocalDateTime completedAt;

    protected Chapter() {
    }

    public Chapter(Course course, Integer number, String name, String description) {
        this.course = course;
        this.number = number;
        this.name = name;
        this.description = description;
        this.status = "NOT_STARTED";
    }
}
