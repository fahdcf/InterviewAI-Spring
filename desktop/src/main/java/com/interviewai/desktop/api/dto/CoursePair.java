package com.interviewai.desktop.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CoursePair {

    private int id;
    private Integer technicalCourseId;
    private Integer softSkillsCourseId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getTechnicalCourseId() { return technicalCourseId; }
    public void setTechnicalCourseId(Integer technicalCourseId) { this.technicalCourseId = technicalCourseId; }

    public Integer getSoftSkillsCourseId() { return softSkillsCourseId; }
    public void setSoftSkillsCourseId(Integer softSkillsCourseId) { this.softSkillsCourseId = softSkillsCourseId; }
}
