package com.interviewai.backend.dto.response;

public class OnboardingResponse {

    private final String interviewType;
    private final String language;
    private final String timeline;
    private final String context;
    private final String description;
    private final boolean hasCv;

    public OnboardingResponse(String interviewType, String language, String timeline,
            String context, String description, boolean hasCv) {
        this.interviewType = interviewType;
        this.language = language;
        this.timeline = timeline;
        this.context = context;
        this.description = description;
        this.hasCv = hasCv;
    }

    public String getInterviewType() { return interviewType; }
    public String getLanguage() { return language; }
    public String getTimeline() { return timeline; }
    public String getContext() { return context; }
    public String getDescription() { return description; }
    public boolean isHasCv() { return hasCv; }
}
