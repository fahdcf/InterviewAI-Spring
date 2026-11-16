package com.interviewai.backend.service;

import com.interviewai.backend.dto.request.GoalRequest;
import com.interviewai.backend.dto.response.GoalResponse;
import com.interviewai.backend.dto.response.LeaderboardEntry;
import com.interviewai.backend.dto.response.ProgressSummary;
import com.interviewai.backend.dto.response.ProgressSummary.DailyXp;
import com.interviewai.backend.repository.DashboardRepository;
import com.interviewai.backend.repository.ProgressRepository;
import com.interviewai.backend.repository.ProgressRepository.XpEvent;
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

    private final ProgressRepository progress;
    private final DashboardRepository dashboard;
    private final AccountService accounts;
    private final Clock clock;

    public ProgressService(ProgressRepository progress, DashboardRepository dashboard, AccountService accounts, Clock clock) {
        this.progress = progress;
        this.dashboard = dashboard;
        this.accounts = accounts;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProgressSummary summary(int userId) {
        LocalDate today = LocalDate.now(clock);
        int answered = progress.answered(userId);
        return new ProgressSummary(dashboard.findTotalXp(userId),
                DashboardService.calculateStreak(dashboard.findActivityDates(userId, clock.getZone()), today),
                answered, progress.correct(userId), progress.completedChapters(userId, null),
                progress.totalCourses(userId), estimateTime(userId, answered), recentXp(userId, today));
    }

    public List<LeaderboardEntry> leaderboard() {
        return progress.leaderboard();
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> goals(int userId) {
        List<GoalResponse> result = new ArrayList<>();
        for (GoalResponse goal : progress.goals(userId)) {
            long current = Math.max(0, metric(userId, goal.type()) - goal.startValue());
            result.add(new GoalResponse(goal.id(), goal.name(), goal.type(), goal.target(), goal.startValue(), current));
        }
        return result;
    }

    @Transactional
    public void addGoal(int userId, GoalRequest goal) {
        accounts.lockActiveAccount(userId);
        progress.addGoal(userId, goal.name().strip(), goal.type(), goal.target(), baseline(userId, goal.type()));
    }

    @Transactional
    public void updateGoal(int userId, int id, GoalRequest goal) {
        accounts.lockActiveAccount(userId);
        GoalResponse existing = progress.findGoal(userId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal unavailable."));
        int startValue = existing.type().equals(goal.type()) ? existing.startValue() : baseline(userId, goal.type());
        progress.updateGoal(userId, id, goal.name().strip(), goal.type(), goal.target(), startValue);
    }

    @Transactional
    public void removeGoal(int userId, int id) {
        accounts.lockActiveAccount(userId);
        if (!progress.removeGoal(userId, id)) {
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
            case "QUESTIONS" -> progress.answered(userId);
            case "XP" -> dashboard.findTotalXp(userId);
            case "CHAPTERS" -> progress.completedChapters(userId, utcStart(today.withDayOfMonth(1)));
            case "TIME" -> estimateTime(userId, progress.answered(userId));
            case "COURSES" -> progress.totalCourses(userId);
            case "STREAK" -> DashboardService.calculateStreak(dashboard.findActivityDates(userId, clock.getZone()), today);
            default -> 0;
        };
    }

    private long estimateTime(int userId, int answered) {
        return answered * 2L + progress.positiveXpEvents(userId) * 15L;
    }

    private List<DailyXp> recentXp(int userId, LocalDate today) {
        LocalDate firstDay = today.minusDays(6);
        Map<LocalDate, Long> totals = new LinkedHashMap<>();
        for (int day = 0; day < 7; day++) {
            totals.put(firstDay.plusDays(day), 0L);
        }
        for (XpEvent event : progress.xpSince(userId, utcStart(firstDay))) {
            LocalDate localDate = event.time().atOffset(ZoneOffset.UTC).atZoneSameInstant(clock.getZone()).toLocalDate();
            totals.computeIfPresent(localDate, (date, xp) -> xp + event.xp());
        }
        List<DailyXp> recent = new ArrayList<>();
        totals.forEach((date, xp) -> recent.add(new DailyXp(date.toString(), xp)));
        return recent;
    }

    private LocalDateTime utcStart(LocalDate day) {
        return LocalDateTime.ofInstant(day.atStartOfDay(clock.getZone()).toInstant(), ZoneOffset.UTC);
    }
}
