package com.interviewai.backend.dto.response;

public record AdminCourse(int id, int userId, String owner, String title, String status, int chapters,
                          int completedChapters, int questions, String created) {
}
