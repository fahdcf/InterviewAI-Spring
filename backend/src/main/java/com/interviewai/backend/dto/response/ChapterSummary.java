package com.interviewai.backend.dto.response;

public class ChapterSummary {

    private final int id;
    private final int number;
    private final String name;
    private final String description;
    private final String status;
    private final int questionCount;
    private final int answeredCount;

    public ChapterSummary(int id, int number, String name, String description, String status, int questionCount, int answeredCount) {
        this.id = id;
        this.number = number;
        this.name = name;
        this.description = description;
        this.status = status;
        this.questionCount = questionCount;
        this.answeredCount = answeredCount;
    }

    public int getId() { return id; }
    public int getNumber() { return number; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public int getQuestionCount() { return questionCount; }
    public int getAnsweredCount() { return answeredCount; }
}
