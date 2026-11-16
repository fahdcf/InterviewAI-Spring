package com.interviewai.backend.model;

public record PracticeQuestion(int id, int chapterId, int courseId, String text, String type, String status,
                               String correctAnswer, String explanation) {
}
