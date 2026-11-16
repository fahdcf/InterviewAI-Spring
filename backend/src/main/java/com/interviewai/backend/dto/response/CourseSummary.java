package com.interviewai.backend.dto.response;

public class CourseSummary {

    private final int id;
    private final String title;
    private final String status;

    public CourseSummary(int id, String title, String status) {
        this.id = id;
        this.title = title;
        this.status = status;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
}
