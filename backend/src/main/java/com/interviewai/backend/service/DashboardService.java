package com.interviewai.backend.service;

import com.interviewai.backend.dto.request.NavigationRequest;
import com.interviewai.backend.dto.response.ChapterSummary;
import com.interviewai.backend.dto.response.CoursePair;
import com.interviewai.backend.dto.response.CourseSummary;
import com.interviewai.backend.dto.response.DashboardResponse;
import com.interviewai.backend.repository.CourseRepository;
import com.interviewai.backend.repository.DashboardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DashboardService {

    private final CourseRepository courses;
    private final DashboardRepository dashboard;
    private final Clock clock;

    public DashboardService(CourseRepository courses, DashboardRepository dashboard, Clock clock) {
        this.courses = courses;
        this.dashboard = dashboard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse load(int userId) {
        List<CourseSummary> available = courses.findAvailableCourses(userId);
        List<CoursePair> pairs = courses.findPairs(userId);
        NavigationRequest saved = dashboard.findLocation(userId);

        Integer courseId = findActiveCourse(available, pairs, saved.getCourseId());
        List<ChapterSummary> chapters = courseId == null ? List.of() : courses.findChapters(courseId);
        Integer chapterId = findActiveChapter(chapters,
                courseId != null && courseId.equals(saved.getCourseId()) ? saved.getChapterId() : null);
        return new DashboardResponse(available, pairs, courseId, chapterId, chapters,
                dashboard.findTotalXp(userId),
                calculateStreak(dashboard.findActivityDates(userId, clock.getZone()), LocalDate.now(clock)));
    }

    public List<CourseSummary> listCourses(int userId) {
        return courses.findAvailableCourses(userId);
    }

    public CourseSummary getCourse(int userId, int courseId) {
        return courses.findAvailableCourse(userId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course unavailable."));
    }

    @Transactional(readOnly = true)
    public List<ChapterSummary> listChapters(int userId, int courseId) {
        getCourse(userId, courseId);
        return courses.findChapters(courseId);
    }

    @Transactional
    public void saveLocation(int userId, NavigationRequest location) {
        int courseId = location.getCourseId();
        Integer chapterId = location.getChapterId();
        List<ChapterSummary> chapters = listChapters(userId, courseId);
        if (chapterId != null && chapters.stream().noneMatch(chapter -> chapter.getId() == chapterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter unavailable for this course.");
        }
        dashboard.saveLocation(userId, courseId, chapterId);
    }

    private Integer findActiveCourse(List<CourseSummary> available, List<CoursePair> pairs, Integer savedId) {
        Set<Integer> ids = new HashSet<>();
        for (CourseSummary course : available) {
            ids.add(course.getId());
        }
        if (ids.contains(savedId)) {
            return savedId;
        }
        for (CoursePair pair : pairs) {
            if (ids.contains(pair.getTechnicalCourseId())) {
                return pair.getTechnicalCourseId();
            }
            if (ids.contains(pair.getSoftSkillsCourseId())) {
                return pair.getSoftSkillsCourseId();
            }
        }
        return available.isEmpty() ? null : available.getFirst().getId();
    }

    private Integer findActiveChapter(List<ChapterSummary> chapters, Integer savedId) {
        for (ChapterSummary chapter : chapters) {
            if (savedId != null && chapter.getId() == savedId) {
                return savedId;
            }
        }
        for (ChapterSummary chapter : chapters) {
            if (!"COMPLETED".equals(chapter.getStatus())) {
                return chapter.getId();
            }
        }
        return chapters.isEmpty() ? null : chapters.getLast().getId();
    }

    static int calculateStreak(List<LocalDate> activityDates, LocalDate today) {
        if (activityDates.isEmpty()) {
            return 0;
        }
        LocalDate day = activityDates.getFirst();
        if (!day.equals(today) && !day.equals(today.minusDays(1))) {
            return 0;
        }
        int streak = 0;
        for (LocalDate date : activityDates) {
            if (!date.equals(day)) {
                break;
            }
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }
}
