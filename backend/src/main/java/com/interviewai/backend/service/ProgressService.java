package com.interviewai.backend.service;

import com.interviewai.backend.dto.request.GoalRequest;
import com.interviewai.backend.dto.response.GoalResponse;
import com.interviewai.backend.dto.response.LeaderboardEntry;
import com.interviewai.backend.dto.response.ProgressSummary;
import com.interviewai.backend.dto.response.ProgressSummary.DailyXp;
import com.interviewai.backend.model.entity.UserGoal;
import com.interviewai.backend.repository.ChapterRepository;
import com.interviewai.backend.repository.CourseRepository;
import com.interviewai.backend.repository.GoalRepository;
import com.interviewai.backend.repository.QuestionRepository;
import com.interviewai.backend.repository.ProgressRepository;
import com.interviewai.backend.repository.ProgressRepository.XpEvent;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProgressService {

    private static final List<String> ANSWERED = List.of("COMPLETED", "INCORRECT");
    private final ProgressRepository progress;
    private final QuestionRepository questions;
    private final ChapterRepository chapters;
    private final CourseRepository courses;
    private final GoalRepository goals;
    private final AccountService accounts;
    private final Clock clock;

    public ProgressService(ProgressRepository progress, QuestionRepository questions, ChapterRepository chapters,
            CourseRepository courses, GoalRepository goals, AccountService accounts, Clock clock) {
        this.progress = progress;
        this.questions = questions;
        this.chapters = chapters;
        this.courses = courses;
        this.goals = goals;
        this.accounts = accounts;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProgressSummary summary(int userId) {
        LocalDate today = LocalDate.now(clock);
        int answered = questions.countByChapterCourseUserIdAndStatusIn(userId, ANSWERED);
        return new ProgressSummary(progress.findTotalXp(userId),
                DashboardService.calculateStreak(DashboardService.activityDates(userId, progress, clock), today),
                answered, questions.countByChapterCourseUserIdAndStatus(userId, "COMPLETED"), chapters.countByCourseUserIdAndStatus(userId, "COMPLETED"),
                courses.countByUserId(userId), estimateTime(userId, answered), recentXp(userId, today));
    }

    public List<LeaderboardEntry> leaderboard() {
        return progress.leaderboard(PageRequest.of(0, 20));
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> goals(int userId) {
        List<GoalResponse> result = new ArrayList<>();
        for (UserGoal goal : goals.findByUserIdAndActiveTrueOrderByCreatedAtAscIdAsc(userId)) {
            long current = metric(userId, goal.getType());
            if (!"STREAK".equals(goal.getType())) {
                current = Math.max(0, current - goal.getStartValue());
            }
            result.add(new GoalResponse(goal.getId(), goal.getName(), goal.getType(), goal.getTarget(),
                    goal.getStartValue(), current));
        }
        return result;
    }

    @Transactional
    public void addGoal(int userId, GoalRequest goal) {
        accounts.lockActiveAccount(userId);
        goals.save(new UserGoal(userId, goal.name().strip(), goal.type(), goal.target(), baseline(userId, goal.type())));
    }

    @Transactional
    public void updateGoal(int userId, int id, GoalRequest goal) {
        accounts.lockActiveAccount(userId);
        UserGoal existing = goals.findByUserIdAndIdAndActiveTrue(userId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal unavailable."));
        int startValue = existing.getType().equals(goal.type()) ? existing.getStartValue() : baseline(userId, goal.type());
        goals.updateGoal(userId, id, goal.name().strip(), goal.type(), goal.target(), startValue);
    }

    @Transactional
    public void removeGoal(int userId, int id) {
        accounts.lockActiveAccount(userId);
        if (goals.removeGoal(userId, id) != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal unavailable.");
        }
    }

    private int baseline(int userId, String type) {
        long current = "STREAK".equals(type) ? 0 : metric(userId, type);
        return (int) Math.min(Integer.MAX_VALUE, current);
    }

    private long metric(int userId, String type) {
        LocalDate today = LocalDate.now(clock);
        return switch (type) {
            case "QUESTIONS" -> questions.countByChapterCourseUserIdAndStatusIn(userId, ANSWERED);
            case "XP" -> progress.findTotalXp(userId);
            case "CHAPTERS" -> chapters.countByCourseUserIdAndStatusAndCompletedAtGreaterThanEqual(
                    userId, "COMPLETED", utcStart(today.withDayOfMonth(1)));
            case "TIME" -> estimateTime(userId, questions.countByChapterCourseUserIdAndStatusIn(userId, ANSWERED));
            case "COURSES" -> courses.countByUserId(userId);
            case "STREAK" -> DashboardService.calculateStreak(DashboardService.activityDates(userId, progress, clock), today);
            default -> 0;
        };
    }

    private long estimateTime(int userId, int answered) {
        return answered * 2L + progress.countByUserIdAndXpGreaterThan(userId, 0) * 15L;
    }

    private List<DailyXp> recentXp(int userId, LocalDate today) {
        LocalDate firstDay = today.minusDays(6);
        Map<LocalDate, Long> totals = new LinkedHashMap<>();
        for (int day = 0; day < 7; day++) {
            totals.put(firstDay.plusDays(day), 0L);
        }
        List<XpEvent> events = progress.findByUserIdAndLastUpdatedGreaterThanEqualOrderByLastUpdatedAscIdAsc(
                userId, utcStart(firstDay));
        for (XpEvent event : events) {
            LocalDate localDate = event.lastUpdated().atOffset(ZoneOffset.UTC).atZoneSameInstant(clock.getZone()).toLocalDate();
            totals.computeIfPresent(localDate, (date, xp) -> xp + event.points());
        }
        List<DailyXp> recent = new ArrayList<>();
        totals.forEach((date, xp) -> recent.add(new DailyXp(date.toString(), xp)));
        return recent;
    }

    private LocalDateTime utcStart(LocalDate day) {
        return LocalDateTime.ofInstant(day.atStartOfDay(clock.getZone()).toInstant(), ZoneOffset.UTC);
    }
}
