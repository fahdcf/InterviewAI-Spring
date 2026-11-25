package com.interviewai.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "progress")
public class ProgressEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "course_id", nullable = false)
    private int courseId;

    @Column(name = "xp")
    private Integer xp;

    @Column(name = "last_updated", columnDefinition = "timestamp", insertable = false, updatable = false, nullable = false)
    private LocalDateTime lastUpdated;

    protected ProgressEvent() {
    }

    public ProgressEvent(int userId, int courseId, Integer xp) {
        this.userId = userId;
        this.courseId = courseId;
        this.xp = xp;
    }
}
