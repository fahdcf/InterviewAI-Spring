package com.interviewai.backend.model;

public class CourseInput {

    private final String interviewType;
    private final String language;
    private final String timeline;
    private final String context;
    private final String description;
    private final String cvText;

    public CourseInput(String interviewType, String language, String timeline, String context, String description, String cvText) {
        this.interviewType = interviewType;
        this.language = language;
        this.timeline = timeline;
        this.context = context;
        this.description = description;
        this.cvText = cvText;
    }

    public String getInterviewType() { return interviewType; }
    public String getLanguage() { return language; }
    public String getTimeline() { return timeline; }
    public String getContext() { return context; }
    public String getDescription() { return description; }
    public String getCvText() { return cvText; }
}
