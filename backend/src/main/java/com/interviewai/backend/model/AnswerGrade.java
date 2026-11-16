package com.interviewai.backend.model;

public record AnswerGrade(int score, String strengths, String improvements, String explanation) {

    public boolean passed() {
        return score >= 70;
    }
}
