package com.interviewai.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class NavigationRequest {

    @NotNull(message = "Select a course.")
    @Positive(message = "Select a valid course.")
    private Integer courseId;

    @Positive(message = "Select a valid chapter.")
    private Integer chapterId;

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }

    public Integer getChapterId() { return chapterId; }
    public void setChapterId(Integer chapterId) { this.chapterId = chapterId; }
}
