package com.interviewai.backend.service;

import com.interviewai.backend.dto.response.QuestResponse;
import com.interviewai.backend.dto.response.CourseSummary;
import com.interviewai.backend.repository.CourseRepository;
import com.interviewai.backend.model.entity.ProgressEvent;
import com.interviewai.backend.model.entity.QuestClaimRecord;
import com.interviewai.backend.repository.AccountRepository;
import com.interviewai.backend.repository.ChapterRepository;
import com.interviewai.backend.repository.ProgressRepository;
import com.interviewai.backend.repository.QuestionRepository;
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
    private final AccountRepository users;
    private final QuestionRepository questions;
    private final ChapterRepository chapters;
    private final CourseRepository courses;
    private final ProgressRepository xp;
    private final AccountService accounts;
    private final Clock clock;

    public QuestService(QuestRepository quests, AccountRepository users, QuestionRepository questions, ChapterRepository chapters,
            CourseRepository courses, ProgressRepository xp, AccountService accounts, Clock clock) {
        this.quests = quests;
        this.users = users;
        this.questions = questions;
        this.chapters = chapters;
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
        QuestClaim previous = quests.findByUserIdAndRequestId(userId, requestId).orElse(null);
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
            users.recordDailyReset(userId, today, now);
        } else {
            users.recordMonthlyClaim(userId, today);
        }
        xp.save(new ProgressEvent(userId, available.getFirst().getId(), quest.reward()));
        quests.save(new QuestClaimRecord(userId, requestId, type, quest.reward()));
        return new QuestClaim(type, quest.reward());
    }

    private QuestResponse daily(int userId, LocalDate today) {
        LocalDateTime start = utcStart(today).minusSeconds(1);
        LocalDateTime lastReset = users.dailyReset(userId);
        if (lastReset != null && lastReset.isAfter(start)) {
            start = lastReset;
        }
        int answered = questions.countByChapterCourseUserIdAndStatusInAndUpdatedAtAfter(
                userId, List.of("COMPLETED", "INCORRECT", "IN_PROGRESS"), start);
        return new QuestResponse("daily", answered, 20, 900, false, answered >= 20,
                today.plusDays(1).atStartOfDay(clock.getZone()).toOffsetDateTime().toString());
    }

    private QuestResponse monthly(int userId, LocalDate today) {
        YearMonth month = YearMonth.from(today);
        LocalDate claimedDate = users.monthlyClaimDate(userId);
        boolean claimed = claimedDate != null && month.equals(YearMonth.from(claimedDate));
        int completed = chapters.countByCourseUserIdAndStatusAndCompletedAtGreaterThanEqual(
                userId, "COMPLETED", utcStart(month.atDay(1)));
        return new QuestResponse("monthly", completed, 12, 30000, claimed, !claimed && completed >= 12,
                month.plusMonths(1).atDay(1).atStartOfDay(clock.getZone()).toOffsetDateTime().toString());
    }

    private LocalDateTime utcStart(LocalDate day) {
        return LocalDateTime.ofInstant(day.atStartOfDay(clock.getZone()).toInstant(), ZoneOffset.UTC);
    }
}
