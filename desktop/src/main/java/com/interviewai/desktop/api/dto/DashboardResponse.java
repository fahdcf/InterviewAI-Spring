package com.interviewai.desktop.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardResponse {

    private List<CourseSummary> courses;
    private List<CoursePair> pairs;
    private Integer activeCourseId;
    private Integer activeChapterId;
    private List<ChapterSummary> chapters;
    private long totalXp;
    private int streak;

    public List<CourseSummary> getCourses() { return courses; }
    public void setCourses(List<CourseSummary> courses) { this.courses = courses; }

    public List<CoursePair> getPairs() { return pairs; }
    public void setPairs(List<CoursePair> pairs) { this.pairs = pairs; }

    public Integer getActiveCourseId() { return activeCourseId; }
    public void setActiveCourseId(Integer activeCourseId) { this.activeCourseId = activeCourseId; }

    public Integer getActiveChapterId() { return activeChapterId; }
    public void setActiveChapterId(Integer activeChapterId) { this.activeChapterId = activeChapterId; }

    public List<ChapterSummary> getChapters() { return chapters; }
    public void setChapters(List<ChapterSummary> chapters) { this.chapters = chapters; }

    public long getTotalXp() { return totalXp; }
    public void setTotalXp(long totalXp) { this.totalXp = totalXp; }

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }
}
