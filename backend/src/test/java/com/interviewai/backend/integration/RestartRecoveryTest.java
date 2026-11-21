package com.interviewai.backend.integration;

import com.interviewai.backend.InterviewAiApplication;
import com.interviewai.backend.dto.request.RegisterRequest;
import com.interviewai.backend.fixture.ProviderFixtures;
import com.interviewai.backend.repository.CourseJobRepository;
import com.interviewai.backend.repository.PracticeRepository;
import com.interviewai.backend.service.AccountService;
import com.interviewai.backend.service.ProfileService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestartRecoveryTest {

    @Test
    @Timeout(90)
    void restartedBackendRecoversQueuedAndRunningWork() throws Exception {
        HttpServer fixture = ProviderFixtures.start(0);
        String origin = "http://127.0.0.1:" + fixture.getAddress().getPort();
        String username = "restart-" + UUID.randomUUID();
        JdbcTemplate jdbc;
        int userId;
        try {
            try (ConfigurableApplicationContext first = start(origin)) {
                RegisterRequest registration = new RegisterRequest();
                registration.setUsername(username);
                registration.setPassword("synthetic-restart-password");
                userId = first.getBean(AccountService.class).register(registration).getId();
                Environment configuration = first.getEnvironment();
                jdbc = new JdbcTemplate(new DriverManagerDataSource(
                        configuration.getProperty("spring.datasource.url"),
                        configuration.getProperty("spring.datasource.username"),
                        configuration.getProperty("spring.datasource.password")));
            }
            // These rows represent work saved immediately before the previous process stopped.
            long runningJob = addJob(jdbc, userId, "RUNNING");
            long queuedJob = addJob(jdbc, userId, "QUEUED");
            jdbc.update("INSERT INTO generated_courses (user_id, course_title, status) VALUES (?, 'Recovery', 'ACTIVE')", userId);
            int course = jdbc.queryForObject("SELECT id FROM generated_courses WHERE user_id = ?", Integer.class, userId);
            jdbc.update("INSERT INTO chapters (course_id, chapter_number, name) VALUES (?, 1, 'Recovery')", course);
            int chapter = jdbc.queryForObject("SELECT id FROM chapters WHERE course_id = ?", Integer.class, course);
            jdbc.update("""
                    INSERT INTO questions (chapter_id, question, question_type, correct_answer)
                    VALUES (?, 'A synthetic question?', 'SHORT_ANSWER', 'mostly correct')
                    """, chapter);
            int question = jdbc.queryForObject("SELECT id FROM questions WHERE chapter_id = ?", Integer.class, chapter);
            long runningAttempt = addAttempt(jdbc, userId, question, "RUNNING");
            long queuedAttempt = addAttempt(jdbc, userId, question, "QUEUED");
            try (ConfigurableApplicationContext restarted = start(origin)) {
                CourseJobRepository jobs = restarted.getBean(CourseJobRepository.class);
                PracticeRepository attempts = restarted.getBean(PracticeRepository.class);
                assertEquals("INTERRUPTED", jobs.find(userId, runningJob).orElseThrow().getStatus());
                assertEquals("FAILED", attempts.findAttempt(userId, runningAttempt).orElseThrow().status());
                boolean finished = false;
                for (int check = 0; check < 100; check++) {
                    finished = jobs.find(userId, queuedJob).orElseThrow().getStatus().equals("SUCCEEDED")
                            && attempts.findAttempt(userId, queuedAttempt).orElseThrow().status().equals("GRADED");
                    if (finished) {
                        break;
                    }
                    Thread.sleep(100);
                }
                assertTrue(finished, "Saved queued work did not finish after restart.");
                assertEquals(10, attempts.findAttempt(userId, queuedAttempt).orElseThrow().xpAwarded());
                assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM generated_courses WHERE user_id = ?",
                        Integer.class, userId));
                assertEquals("mostly correct", attempts.findAttempt(userId, runningAttempt).orElseThrow().answer());
                restarted.getBean(ProfileService.class).deleteAccount(userId, "synthetic-restart-password");
            }
        } finally {
            fixture.stop(0);
        }
    }

    private ConfigurableApplicationContext start(String providerOrigin) {
        return new SpringApplicationBuilder(InterviewAiApplication.class).run(
                "--spring.profiles.active=test", "--server.port=0",
                "--interviewai.gemini.key=synthetic-fixture-key", "--interviewai.gemini.url=" + providerOrigin + "/generate",
                "--interviewai.openrouter.key=synthetic-fixture-key", "--interviewai.openrouter.url=" + providerOrigin + "/grade");
    }

    private long addJob(JdbcTemplate jdbc, int userId, String status) {
        String request = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO course_generation_jobs (user_id, request_id, status, interview_type, language, timeline, context)
                VALUES (?, ?, ?, 'JOB', 'ENGLISH', 'LATER', 'Synthetic interview')
                """, userId, request, status);
        return jdbc.queryForObject("SELECT id FROM course_generation_jobs WHERE user_id = ? AND request_id = ?",
                Long.class, userId, request);
    }

    private long addAttempt(JdbcTemplate jdbc, int userId, int questionId, String status) {
        String request = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO practice_attempts (user_id, question_id, request_id, mode, answer, status)
                VALUES (?, ?, ?, 'LESSON', 'mostly correct', ?)
                """, userId, questionId, request, status);
        return jdbc.queryForObject("SELECT id FROM practice_attempts WHERE user_id = ? AND request_id = ?",
                Long.class, userId, request);
    }
}
