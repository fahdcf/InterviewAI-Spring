package com.interviewai.backend.dto.response;

import java.util.Map;

public record AdminOverview(int users, int activeUsers, int dailySignups, int weeklySignups, int monthlySignups,
                            int courses, int completedCourses, int completedChapters, int questions,
                            Map<String, Long> interviewTypes, Map<String, Long> languages,
                            Map<String, Long> signupTrend, Map<String, Long> usageTrend) {
}
