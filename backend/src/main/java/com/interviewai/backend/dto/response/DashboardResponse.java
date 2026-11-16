package com.interviewai.backend.dto.response;

import java.util.List;

public class DashboardResponse {

    private final List<CourseSummary> courses;
    private final List<CoursePair> pairs;
    private final Integer activeCourseId;
    private final Integer activeChapterId;
    private final List<ChapterSummary> chapters;
    private final long totalXp;
    private final int streak;

    public DashboardResponse(List<CourseSummary> courses, List<CoursePair> pairs, Integer activeCourseId, Integer activeChapterId, List<ChapterSummary> chapters, long totalXp, int streak) {
        this.courses = courses;
        this.pairs = pairs;
        this.activeCourseId = activeCourseId;
        this.activeChapterId = activeChapterId;
        this.chapters = chapters;
        this.totalXp = totalXp;
        this.streak = streak;
    }

    public List<CourseSummary> getCourses() { return courses; }
    public List<CoursePair> getPairs() { return pairs; }
    public Integer getActiveCourseId() { return activeCourseId; }
    public Integer getActiveChapterId() { return activeChapterId; }
    public List<ChapterSummary> getChapters() { return chapters; }
    public long getTotalXp() { return totalXp; }
    public int getStreak() { return streak; }
}
