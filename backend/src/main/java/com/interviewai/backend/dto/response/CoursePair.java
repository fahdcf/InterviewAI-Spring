package com.interviewai.backend.dto.response;

public class CoursePair {

    private final int id;
    private final Integer technicalCourseId;
    private final Integer softSkillsCourseId;

    public CoursePair(int id, Integer technicalCourseId, Integer softSkillsCourseId) {
        this.id = id;
        this.technicalCourseId = technicalCourseId;
        this.softSkillsCourseId = softSkillsCourseId;
    }

    public int getId() { return id; }
    public Integer getTechnicalCourseId() { return technicalCourseId; }
    public Integer getSoftSkillsCourseId() { return softSkillsCourseId; }
}
