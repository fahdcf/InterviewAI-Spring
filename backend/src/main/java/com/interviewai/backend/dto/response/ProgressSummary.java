package com.interviewai.backend.dto.response;

import java.util.List;

public record ProgressSummary(long totalXp, int streak, int answered, int correct, int completedChapters,
                              int totalCourses, long estimatedMinutes, List<DailyXp> recentXp) {

    public record DailyXp(String date, long xp) {
    }
}
