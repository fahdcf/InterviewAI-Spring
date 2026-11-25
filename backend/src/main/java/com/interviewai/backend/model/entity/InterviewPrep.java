package com.interviewai.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_prep")
public class InterviewPrep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "technical_course_id")
    private Integer technicalCourseId;

    @Column(name = "softskills_course_id")
    private Integer softSkillsCourseId;

    @Column(name = "created_at", columnDefinition = "timestamp", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    protected InterviewPrep() {
    }

    public InterviewPrep(int userId, Integer technicalCourseId, Integer softSkillsCourseId) {
        this.userId = userId;
        this.technicalCourseId = technicalCourseId;
        this.softSkillsCourseId = softSkillsCourseId;
    }

    public Integer getId() { return id; }
    public Integer getTechnicalCourseId() { return technicalCourseId; }
    public Integer getSoftSkillsCourseId() { return softSkillsCourseId; }
}
