package com.interviewai.backend.service;

import com.interviewai.backend.dto.request.GoalRequest;
import com.interviewai.backend.dto.request.RegisterRequest;
import com.interviewai.backend.dto.response.GoalResponse;
import com.interviewai.backend.dto.response.QuestResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(CalendarProgressTest.TimeConfiguration.class)
@DirtiesContext
class CalendarProgressTest {

    @Autowired private AccountService accounts;
    @Autowired private ProfileService profiles;
    @Autowired private ProgressService progress;
    @Autowired private QuestService quests;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AdjustableClock clock;

    @Test
    void allGoalTypesKeepTheirBaselineAndProgressUsesLocalDates() {
        clock.set("2026-11-21T12:00:00Z");
        int userId = register();
        try {
            int course = course(userId, "First");
            jdbc.update("""
                    INSERT INTO progress (user_id, course_id, xp, last_updated)
                    VALUES (?, ?, 40, '2026-11-20 12:00:00')
                    """, userId, course);
            int chapter = chapter(course, 1, "2026-11-20 12:00:00");
            questions(chapter, 1, "2026-11-20 12:00:00");
            for (String type : List.of("QUESTIONS", "XP", "CHAPTERS", "TIME", "COURSES", "STREAK")) {
                progress.addGoal(userId, new GoalRequest(type, type, 100));
            }
            jdbc.update("UPDATE user_goals SET start_value = 99 WHERE user_id = ? AND goal_type = \'STREAK\'", userId);
            Map<String, GoalResponse> before = goals(userId);
            assertEquals(40, before.get("XP").startValue());
            assertEquals(17, before.get("TIME").startValue());
            assertEquals(99, before.get("STREAK").startValue());
            assertEquals(1, before.get("STREAK").currentValue());

            jdbc.update("""
                    INSERT INTO progress (user_id, course_id, xp, last_updated)
                    VALUES (?, ?, 100, ?)
                    """, userId, course, java.time.LocalDateTime.ofInstant(
                    LocalDate.of(2026, 11, 21).atStartOfDay(clock.getZone()).plusMinutes(30).toInstant(),
                    java.time.ZoneOffset.UTC));
            questions(chapter, 1, "2026-11-21 12:00:00");
            chapter(course, 2, "2026-11-21 12:00:00");
            course(userId, "Second");
            Map<String, GoalResponse> after = goals(userId);
            assertEquals(1, after.get("QUESTIONS").currentValue());
            assertEquals(100, after.get("XP").currentValue());
            assertEquals(1, after.get("CHAPTERS").currentValue());
            assertEquals(17, after.get("TIME").currentValue());
            assertEquals(1, after.get("COURSES").currentValue());
            assertEquals(2, after.get("STREAK").currentValue());
            assertEquals(100, progress.summary(userId).recentXp().getLast().xp());
            assertEquals(2, progress.summary(userId).streak());

            clock.set("2026-12-01T12:00:00Z");
            assertEquals(0, progress.summary(userId).streak());
            assertEquals(0, goals(userId).get("CHAPTERS").currentValue());
        } finally {
            profiles.deleteAccount(userId, "synthetic-calendar-password");
        }
    }

    @Test
    void dailyCyclesWithinOneSecondAndMonthlyRolloverAwardOnceAndRollbackOnFailure() {
        clock.set("2026-11-21T12:00:00.500Z");
        int userId = register();
        try {
            int course = course(userId, "Quest course");
            int firstChapter = chapter(course, 1, "2026-11-21 12:00:00");
            jdbc.update("UPDATE users SET daily_quest_last_reset = '2026-11-21 12:00:00.100000' WHERE id = ?", userId);
            questions(firstChapter, 20, "2026-11-21 12:00:00.200000");
            assertTrue(daily(userId).available());
            assertThrows(DataIntegrityViolationException.class,
                    () -> quests.claim(userId, "daily", "x".repeat(50)));
            assertTrue(daily(userId).available());
            assertEquals(0, progress.summary(userId).totalXp());
            assertEquals(900, quests.claim(userId, "daily", UUID.randomUUID().toString()).xpAwarded());
            assertFalse(daily(userId).available());

            questions(firstChapter, 20, "2026-11-21 12:00:00.600000");
            clock.set("2026-11-21T12:00:00.900Z");
            assertTrue(daily(userId).available());
            assertEquals(900, quests.claim(userId, "daily", UUID.randomUUID().toString()).xpAwarded());
            assertFalse(daily(userId).available());

            for (int number = 2; number <= 12; number++) {
                chapter(course, number, "2026-11-21 12:00:00");
            }
            assertTrue(monthly(userId).available());
            quests.claim(userId, "monthly", UUID.randomUUID().toString());
            assertFalse(monthly(userId).available());
            clock.set("2026-12-01T12:00:00Z");
            assertFalse(monthly(userId).claimed());
            assertEquals(0, monthly(userId).current());
            for (int number = 13; number <= 24; number++) {
                chapter(course, number, "2026-12-01 12:00:00");
            }
            assertTrue(monthly(userId).available());
            quests.claim(userId, "monthly", UUID.randomUUID().toString());
            assertEquals(61800, progress.summary(userId).totalXp());
        } finally {
            profiles.deleteAccount(userId, "synthetic-calendar-password");
        }
    }

    @Test
    void dailyResetFollowsCasablancaOffsetChanges() {
        ZoneId zone = clock.getZone();
        var transition = zone.getRules().nextTransition(Instant.parse("2026-01-01T00:00:00Z"));
        LocalDate transitionDay = transition.getDateTimeBefore().toLocalDate();
        Instant start = transitionDay.atStartOfDay(zone).toInstant();
        Instant nextDay = transitionDay.plusDays(1).atStartOfDay(zone).toInstant();
        clock.set(start.plusSeconds(1800).toString());
        int userId = register();
        try {
            Instant reset = OffsetDateTime.parse(daily(userId).resetsAt()).toInstant();
            assertEquals(nextDay, reset);
            assertTrue(Duration.between(start, reset).toHours() != 24);
        } finally {
            profiles.deleteAccount(userId, "synthetic-calendar-password");
        }
    }

    @Test
    void streaksHandleTodayYesterdayAndGaps() {
        LocalDate today = LocalDate.of(2026, 11, 21);
        assertEquals(0, DashboardService.calculateStreak(List.of(), today));
        assertEquals(1, DashboardService.calculateStreak(List.of(today), today));
        assertEquals(1, DashboardService.calculateStreak(List.of(today.minusDays(1)), today));
        assertEquals(0, DashboardService.calculateStreak(List.of(today.minusDays(2)), today));
        assertEquals(2, DashboardService.calculateStreak(List.of(today, today.minusDays(1), today.minusDays(3)), today));
    }

    private Map<String, GoalResponse> goals(int userId) {
        return progress.goals(userId).stream().collect(Collectors.toMap(GoalResponse::type, goal -> goal));
    }

    private QuestResponse daily(int userId) { return quests.list(userId).getFirst(); }
    private QuestResponse monthly(int userId) { return quests.list(userId).getLast(); }

    private int register() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("calendar-" + UUID.randomUUID());
        request.setPassword("synthetic-calendar-password");
        return accounts.register(request).getId();
    }

    private int course(int userId, String name) {
        jdbc.update("INSERT INTO generated_courses (user_id, course_title, status) VALUES (?, ?, 'ACTIVE')", userId, name);
        return jdbc.queryForObject("SELECT id FROM generated_courses WHERE user_id = ? AND course_title = ?",
                Integer.class, userId, name);
    }

    private int chapter(int courseId, int number, String completedAt) {
        jdbc.update("""
                INSERT INTO chapters (course_id, chapter_number, name, status, completed_at)
                VALUES (?, ?, 'Synthetic chapter', 'COMPLETED', ?)
                """, courseId, number, completedAt);
        return jdbc.queryForObject("SELECT id FROM chapters WHERE course_id = ? AND chapter_number = ?",
                Integer.class, courseId, number);
    }

    private void questions(int chapterId, int count, String updatedAt) {
        for (int number = 0; number < count; number++) {
            jdbc.update("INSERT INTO questions (chapter_id, question, status, updated_at)"
                    + " VALUES (?, 'Synthetic question?', 'COMPLETED', ?)", chapterId, updatedAt);
        }
    }

    @TestConfiguration
    static class TimeConfiguration {
        @Bean
        @Primary
        AdjustableClock adjustableClock() { return new AdjustableClock(); }
    }

    static class AdjustableClock extends Clock {
        private Instant now = Instant.parse("2026-11-21T12:00:00Z");
        void set(String instant) { now = Instant.parse(instant); }
        @Override public ZoneId getZone() { return ZoneId.of("Africa/Casablanca"); }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        @Override public Instant instant() { return now; }
    }
}
