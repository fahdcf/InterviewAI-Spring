package com.interviewai.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OnboardingRequest {

    @NotBlank
    @Pattern(regexp = "JOB|VISA|INTERNSHIP|UNIVERSITY")
    private String interviewType;

    @NotBlank
    @Pattern(regexp = "ENGLISH|FRENCH|ARABIC|SPANISH")
    private String language;

    @NotBlank
    @Pattern(regexp = "TOMORROW|THIS_WEEK|LATER")
    private String timeline;

    @NotBlank(message = "Enter your interview context.")
    @Size(max = 2000)
    private String context;

    @Size(max = 4000)
    private String description;

    private boolean removeCv;

    public String getInterviewType() { return interviewType; }
    public void setInterviewType(String interviewType) { this.interviewType = interviewType; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getTimeline() { return timeline; }
    public void setTimeline(String timeline) { this.timeline = timeline; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isRemoveCv() { return removeCv; }
    public void setRemoveCv(boolean removeCv) { this.removeCv = removeCv; }
}
