package com.interviewai.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "generated_courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "course_title", length = 255)
    private String title;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "created_at", columnDefinition = "datetime", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Course() {
    }

    public Course(int userId, String title, String status) {
        this.userId = userId;
        this.title = title;
        this.status = status;
    }

    public Integer getId() { return id; }
}
