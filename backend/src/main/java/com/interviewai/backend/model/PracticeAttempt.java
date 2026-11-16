package com.interviewai.backend.model;

public record PracticeAttempt(long id, int userId, int questionId, String mode, String answer, String status,
                              Integer score, String strengths, String improvements, String explanation,
                              int xpAwarded) {
}
