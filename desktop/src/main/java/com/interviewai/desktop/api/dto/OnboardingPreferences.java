package com.interviewai.desktop.api.dto;

public class OnboardingPreferences {

    private String interviewType;
    private String language;
    private String timeline;
    private String context;
    private String description;
    private boolean hasCv;
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

    public boolean isHasCv() { return hasCv; }
    public void setHasCv(boolean hasCv) { this.hasCv = hasCv; }

    public boolean isRemoveCv() { return removeCv; }
    public void setRemoveCv(boolean removeCv) { this.removeCv = removeCv; }
}
