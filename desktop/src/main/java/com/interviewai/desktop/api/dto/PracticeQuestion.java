package com.interviewai.desktop.api.dto;

import java.util.List;

public record PracticeQuestion(int id, int chapterId, String text, String type, String status, List<String> choices, AttemptResult savedAttempt) {
}
