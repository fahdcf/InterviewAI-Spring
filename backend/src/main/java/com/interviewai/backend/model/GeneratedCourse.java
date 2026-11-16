package com.interviewai.backend.model;

import java.util.List;

public class GeneratedCourse {

    private final String title;
    private final List<GeneratedChapter> chapters;

    public GeneratedCourse(String title, List<GeneratedChapter> chapters) {
        this.title = title;
        this.chapters = chapters;
    }

    public String getTitle() { return title; }
    public List<GeneratedChapter> getChapters() { return chapters; }
}
