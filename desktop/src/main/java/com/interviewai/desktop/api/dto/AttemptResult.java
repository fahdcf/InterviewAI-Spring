package com.interviewai.desktop.api.dto;

public record AttemptResult(long id, String status, Integer score, String strengths,
                            String improvements, String explanation, int xpAwarded, String mode, String answer) {
}
