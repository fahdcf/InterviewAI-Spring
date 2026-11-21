package com.interviewai.backend.dto.response;

import com.interviewai.backend.model.PracticeAttempt;

public record AttemptResponse(long id, String status, Integer score, String strengths, String improvements,
                              String explanation, int xpAwarded, String mode, String answer) {

    public static AttemptResponse from(PracticeAttempt attempt) {
        return new AttemptResponse(attempt.id(), attempt.status(), attempt.score(), attempt.strengths(),
                attempt.improvements(), attempt.explanation(), attempt.xpAwarded(), attempt.mode(), attempt.answer());
    }
}
