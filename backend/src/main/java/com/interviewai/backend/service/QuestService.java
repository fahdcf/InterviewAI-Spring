package com.interviewai.backend.service;

import com.interviewai.backend.dto.response.QuestResponse;
import com.interviewai.backend.dto.response.CourseSummary;
import com.interviewai.backend.repository.CourseRepository;
import com.interviewai.backend.repository.PracticeRepository;
import com.interviewai.backend.repository.ProgressRepository;
import com.interviewai.backend.repository.QuestRepository;
import com.interviewai.backend.repository.QuestRepository.QuestClaim;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class QuestService {

    private final QuestRepository quests;
    private final ProgressRepository progress;
    private final CourseRepository courses;
    private final PracticeRepository xp;
    private final AccountService accounts;
    private final Clock clock;

    public QuestService(QuestRepository quests, ProgressRepository progress, CourseRepository courses,
            PracticeRepository xp, AccountService accounts, Clock clock) {
        this.quests = quests;
        this.progress = progress;
        this.courses = courses;
        this.xp = xp;
        this.accounts = accounts;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<QuestResponse> list(int userId) {
        LocalDate today = LocalDate.now(clock);
        return List.of(daily(userId, today), monthly(userId, today));
    }

    @Transactional
    public QuestClaim claim(int userId, String type, String requestId) {
        if (!"daily".equals(type) && !"monthly".equals(type)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quest unavailable.");
        }
        accounts.lockActiveAccount(userId);
        QuestClaim previous = quests.previousClaim(userId, requestId).orElse(null);
        if (previous != null) {
            if (!previous.type().equals(type)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This request ID belongs to another quest.");
            }
            return previous;
        }
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDate today = LocalDate.now(clock);
        QuestResponse quest = "daily".equals(type) ? daily(userId, today) : monthly(userId, today);
        if (!quest.available()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This reward is not ready to claim.");
        }
        List<CourseSummary> available = courses.findAvailableCourses(userId);
        if (available.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You need an active course to receive XP.");
        }
        if ("daily".equals(type)) {
            quests.recordDailyReset(userId, today, now);
        } else {
            quests.recordMonthlyClaim(userId, today);
        }
        xp.awardXp(userId, available.getFirst().getId(), quest.reward());
        quests.saveClaim(userId, requestId, type, quest.reward());
        return new QuestClaim(type, quest.reward());
    }

    private QuestResponse daily(int userId, LocalDate today) {
        LocalDateTime start = utcStart(today).minusSeconds(1);
        LocalDateTime lastReset = quests.dailyReset(userId);
        if (lastReset != null && lastReset.isAfter(start)) {
            start = lastReset;
        }
        int answered = quests.answeredSince(userId, start);
        return new QuestResponse("daily", answered, 20, 900, false, answered >= 20,
                today.plusDays(1).atStartOfDay(clock.getZone()).toOffsetDateTime().toString());
    }

    private QuestResponse monthly(int userId, LocalDate today) {
        YearMonth month = YearMonth.from(today);
        LocalDate claimedDate = quests.monthlyClaimDate(userId);
        boolean claimed = claimedDate != null && month.equals(YearMonth.from(claimedDate));
        int completed = progress.completedChapters(userId, utcStart(month.atDay(1)));
        return new QuestResponse("monthly", completed, 12, 30000, claimed, !claimed && completed >= 12,
                month.plusMonths(1).atDay(1).atStartOfDay(clock.getZone()).toOffsetDateTime().toString());
    }

    private LocalDateTime utcStart(LocalDate day) {
        return LocalDateTime.ofInstant(day.atStartOfDay(clock.getZone()).toInstant(), ZoneOffset.UTC);
    }
}
