package com.interviewai.backend.model;

import java.util.List;

public class GeneratedChapter {

    private final int number;
    private final String name;
    private final String description;
    private final List<GeneratedQuestion> questions;

    public GeneratedChapter(int number, String name, String description, List<GeneratedQuestion> questions) {
        this.number = number;
        this.name = name;
        this.description = description;
        this.questions = questions;
    }

    public int getNumber() { return number; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<GeneratedQuestion> getQuestions() { return questions; }
}
