package com.interviewai.desktop.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseJobStatus {

    private long id;
    private String status;
    private int progress;
    private String message;
    private Integer technicalCourseId;
    private Integer softSkillsCourseId;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Integer getTechnicalCourseId() { return technicalCourseId; }
    public void setTechnicalCourseId(Integer technicalCourseId) { this.technicalCourseId = technicalCourseId; }

    public Integer getSoftSkillsCourseId() { return softSkillsCourseId; }
    public void setSoftSkillsCourseId(Integer softSkillsCourseId) { this.softSkillsCourseId = softSkillsCourseId; }
}
