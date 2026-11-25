package com.interviewai.backend.service;

import com.interviewai.backend.dto.request.NavigationRequest;
import com.interviewai.backend.dto.response.ChapterSummary;
import com.interviewai.backend.dto.response.CoursePair;
import com.interviewai.backend.dto.response.CourseSummary;
import com.interviewai.backend.dto.response.DashboardResponse;
import com.interviewai.backend.model.entity.UserAccount;
import com.interviewai.backend.repository.AccountRepository;
import com.interviewai.backend.repository.ChapterRepository;
import com.interviewai.backend.repository.CourseRepository;
import com.interviewai.backend.repository.InterviewPrepRepository;
import com.interviewai.backend.repository.ProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DashboardService {

    private final CourseRepository courses;
    private final AccountRepository accounts;
    private final ChapterRepository chapters;
    private final InterviewPrepRepository pairs;
    private final ProgressRepository progress;
    private final Clock clock;

    public DashboardService(CourseRepository courses, AccountRepository accounts, ChapterRepository chapters,
            InterviewPrepRepository pairs, ProgressRepository progress, Clock clock) {
        this.courses = courses;
        this.accounts = accounts;
        this.chapters = chapters;
        this.pairs = pairs;
        this.progress = progress;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse load(int userId) {
        List<CourseSummary> available = courses.findAvailableCourses(userId);
        List<CoursePair> availablePairs = pairs.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(pair -> new CoursePair(pair.getId(), pair.getTechnicalCourseId(), pair.getSoftSkillsCourseId()))
                .toList();
        UserAccount saved = accounts.findById(userId).orElseThrow();

        Integer courseId = findActiveCourse(available, availablePairs, saved.getLastCourseId());
        List<ChapterSummary> chapters = courseId == null ? List.of() : readChapters(courseId);
        Integer chapterId = findActiveChapter(chapters,
                courseId != null && courseId.equals(saved.getLastCourseId()) ? saved.getLastChapterId() : null);
        return new DashboardResponse(available, availablePairs, courseId, chapterId, chapters,
                progress.findTotalXp(userId),
                calculateStreak(activityDates(userId, progress, clock), LocalDate.now(clock)));
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
        return readChapters(courseId);
    }

    @Transactional
    public void saveLocation(int userId, NavigationRequest location) {
        int courseId = location.getCourseId();
        Integer chapterId = location.getChapterId();
        List<ChapterSummary> chapters = listChapters(userId, courseId);
        if (chapterId != null && chapters.stream().noneMatch(chapter -> chapter.getId() == chapterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter unavailable for this course.");
        }
        accounts.saveLocation(userId, courseId, chapterId);
    }

    private List<ChapterSummary> readChapters(int courseId) {
        return chapters.findChapters(courseId).stream()
                .map(chapter -> new ChapterSummary(chapter.getId(), chapter.getNumber(), chapter.getName(),
                        chapter.getDescription(), chapter.getStatus(), chapter.getQuestionCount(), chapter.getAnsweredCount()))
                .toList();
    }

    static List<LocalDate> activityDates(int userId, ProgressRepository progress, Clock clock) {
        return progress.findActivityTimes(userId).stream()
                .map(time -> time.atOffset(ZoneOffset.UTC).atZoneSameInstant(clock.getZone()).toLocalDate())
                .distinct().toList();
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
