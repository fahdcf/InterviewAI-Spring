package com.interviewai.backend.model;

public class CourseJob {

    private final long id;
    private final int userId;
    private final String status;
    private final int progress;
    private final String message;
    private final Integer technicalCourseId;
    private final Integer softSkillsCourseId;

    public CourseJob(long id, int userId, String status, int progress, String message, Integer technicalCourseId, Integer softSkillsCourseId) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.progress = progress;
        this.message = message;
        this.technicalCourseId = technicalCourseId;
        this.softSkillsCourseId = softSkillsCourseId;
    }

    public long getId() { return id; }
    public int getUserId() { return userId; }
    public String getStatus() { return status; }
    public int getProgress() { return progress; }
    public String getMessage() { return message; }
    public Integer getTechnicalCourseId() { return technicalCourseId; }
    public Integer getSoftSkillsCourseId() { return softSkillsCourseId; }
}
