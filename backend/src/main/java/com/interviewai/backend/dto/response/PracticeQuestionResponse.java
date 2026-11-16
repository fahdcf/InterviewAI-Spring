package com.interviewai.backend.dto.response;

import java.util.List;

public record PracticeQuestionResponse(int id, int chapterId, String text, String type, String status,
                                       List<String> choices) {
}
