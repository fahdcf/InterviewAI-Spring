package com.interviewai.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.backend.fixture.ProviderFixtures;
import com.interviewai.backend.model.CourseJob;
import com.interviewai.backend.model.AnswerGrade;
import com.interviewai.backend.model.PracticeAttempt;
import com.interviewai.backend.repository.PracticeRepository;
import com.interviewai.backend.service.PracticeService;
import com.interviewai.backend.model.GeneratedChapter;
import com.interviewai.backend.model.GeneratedCourse;
import com.interviewai.backend.model.GeneratedQuestion;
import com.interviewai.backend.service.SaveGeneratedCourses;
import org.springframework.dao.DataIntegrityViolationException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext
class AuthControllerTest {

    private static final String PASSWORD = "synthetic-test-password";
    private static HttpServer provider;

    @DynamicPropertySource
    static void configureProvider(DynamicPropertyRegistry registry) throws Exception {
        provider = ProviderFixtures.start(0);
        registry.add("interviewai.gemini.key", () -> "test-key");
        registry.add("interviewai.gemini.url", () -> "http://127.0.0.1:" + provider.getAddress().getPort() + "/generate");
        registry.add("interviewai.openrouter.key", () -> "test-key");
        registry.add("interviewai.openrouter.url", () -> "http://127.0.0.1:" + provider.getAddress().getPort() + "/grade");
    }

    @AfterAll
    static void stopProvider() {
        if (provider != null) {
            provider.stop(0);
        }
    }

    @LocalServerPort private int port;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SaveGeneratedCourses courseSaver;
    @Autowired private PracticeService practiceService;
    @Autowired private PracticeRepository practiceRepository;

    private CookieManager cookies;
    private HttpClient client;
    private String username;

    @BeforeEach
    void createClient() {
        cookies = new CookieManager();
        client = HttpClient.newBuilder().cookieHandler(cookies).build();
        username = "candidate-" + UUID.randomUUID();
    }

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void candidateCanRegisterSignInByEmailAndSignOut() throws Exception {
        assertEquals(401, get("/me").statusCode());
        String email = username + "@example.com";
        JsonNode originalCsrf = csrf();
        String initialSession = sessionId();

        HttpResponse<String> registration = post("/auth/register", registration(email), originalCsrf);
        assertEquals(201, registration.statusCode(), registration.body());
        assertEquals("CANDIDATE", json.readTree(registration.body()).path("roles").get(0).asText());
        assertTrue(hashFor(username).startsWith("$2a$"));

        HttpResponse<String> login = login(email, PASSWORD);
        assertEquals(200, login.statusCode(), login.body());
        assertEquals(username, json.readTree(login.body()).path("username").asText());
        assertFalse(json.readTree(login.body()).path("administrator").asBoolean());
        assertFalse(json.readTree(login.body()).has("passwordHash"));
        assertNotEquals(initialSession, sessionId());
        assertEquals(403, post("/auth/logout", Map.of(), originalCsrf).statusCode());
        assertEquals(200, get("/me").statusCode());
        JsonNode emptyDashboard = json.readTree(get("/dashboard").body());
        assertTrue(emptyDashboard.path("courses").isEmpty());
        assertTrue(emptyDashboard.path("activeChapterId").isNull());
        assertEquals(403, get("/admin/users").statusCode());
        assertEquals(204, post("/auth/logout", Map.of(), csrf()).statusCode());
        assertEquals(401, get("/me").statusCode());

        assertEquals(200, login(username, PASSWORD).statusCode());
    }

    @Test
    void registrationRequiresCsrfAndRejectsDuplicates() throws Exception {
        String email = username + "@example.com";
        assertEquals(403, post("/auth/register", registration(email), null).statusCode());
        HttpResponse<String> created = post("/auth/register", registration(email), csrf());
        assertEquals(201, created.statusCode(), created.body());
        assertEquals(409, post("/auth/register", registration(""), csrf()).statusCode());
        Map<String, String> duplicateEmail = Map.of(
                "username", username + "-other", "email", email, "password", PASSWORD);
        assertEquals(409, post("/auth/register", duplicateEmail, csrf()).statusCode());
        assertEquals(400, post("/auth/register", Map.of("username", "", "password", PASSWORD), csrf()).statusCode());
    }

    @Test
    void badPasswordAndSuspendedAccountsAreRejected() throws Exception {
        HttpResponse<String> created = post("/auth/register", registration(""), csrf());
        assertEquals(201, created.statusCode(), created.body());
        assertEquals(401, login(username, "wrong password").statusCode());
        assertEquals(200, login(username, PASSWORD).statusCode());

        jdbc.update("UPDATE users SET is_active = FALSE WHERE username = ?", username);
        assertEquals(401, get("/me").statusCode());
        assertEquals(401, login(username, PASSWORD).statusCode());
    }

    @Test
    void legacyHashIsUpgradedOnlyAfterSuccessfulLogin() throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(PASSWORD.getBytes(StandardCharsets.UTF_8));
        String legacyHash = HexFormat.of().formatHex(digest);
        jdbc.update("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, legacyHash);

        assertEquals(401, login(username, "wrong password").statusCode());
        assertEquals(legacyHash, hashFor(username));
        assertEquals(200, login(username, PASSWORD).statusCode());
        assertTrue(new BCryptPasswordEncoder().matches(PASSWORD, hashFor(username)));
    }

    @Test
    void migratedAdminRoleIsReturnedAfterLogin() throws Exception {
        HttpResponse<String> created = post("/auth/register", registration(""), csrf());
        assertEquals(201, created.statusCode(), created.body());
        int userId = json.readTree(created.body()).path("id").asInt();

        jdbc.update("""
                INSERT INTO user_roles (user_id, role_id)
                SELECT ?, id FROM roles WHERE name = 'SUPER_ADMIN'
                """, userId);
        HttpResponse<String> login = login(username, PASSWORD);
        assertEquals(200, login.statusCode(), login.body());
        assertEquals("SUPER_ADMIN", json.readTree(login.body()).path("roles").get(1).asText());
        assertTrue(json.readTree(login.body()).path("administrator").asBoolean());
    }

    @Test
    void accountWithManageUsersPermissionGetsTheLegacyAdminRoutingFlag() throws Exception {
        HttpResponse<String> created = post("/auth/register", registration(""), csrf());
        assertEquals(201, created.statusCode(), created.body());
        int userId = json.readTree(created.body()).path("id").asInt();
        String role = "manager-" + UUID.randomUUID();
        jdbc.update("INSERT INTO roles (name) VALUES (?)", role);
        jdbc.update("""
                INSERT INTO role_permissions (role_id, permission_id)
                SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
                WHERE r.name = ? AND p.name = 'manage_users'
                """, role);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE name = ?",
                userId, role);

        HttpResponse<String> login = login(username, PASSWORD);
        assertEquals(200, login.statusCode(), login.body());
        assertTrue(json.readTree(login.body()).path("administrator").asBoolean());
        assertEquals(403, get("/admin/courses").statusCode());
    }

    private HttpResponse<String> login(String identifier, String password) throws Exception {
        return post("/auth/login", Map.of("identifier", identifier, "password", password), csrf());
    }

    @Test
    void onboardingSavesPreferencesAndOnlyKeepsTheSignedInUsersCvText() throws Exception {
        assertEquals(401, get("/me/onboarding").statusCode());
        assertEquals(201, post("/auth/register", registration(""), csrf()).statusCode());
        assertEquals(200, login(username, PASSWORD).statusCode());
        assertEquals(204, get("/me/onboarding").statusCode());

        String preferences = """
                {"interviewType":"JOB","language":"ENGLISH","timeline":"THIS_WEEK",
                 "context":"Software developer","description":"Backend interview"}
                """;
        HttpResponse<String> saved = savePreferences(preferences, null);
        assertEquals(200, saved.statusCode(), saved.body());
        assertFalse(json.readTree(saved.body()).path("hasCv").asBoolean());
        assertEquals("Backend interview", json.readTree(get("/me/onboarding").body()).path("description").asText());

        java.io.ByteArrayOutputStream cv = new java.io.ByteArrayOutputStream();
        try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            document.createParagraph().createRun().setText("Synthetic resume for integration testing.");
            document.write(cv);
        }
        HttpResponse<String> uploaded = savePreferences(preferences, cv.toByteArray());
        assertEquals(200, uploaded.statusCode(), uploaded.body());
        assertTrue(json.readTree(uploaded.body()).path("hasCv").asBoolean());
        assertFalse(uploaded.body().contains("Synthetic resume"));
        assertEquals(400, savePreferences(preferences, "invalid".getBytes(StandardCharsets.UTF_8)).statusCode());
        assertTrue(json.readTree(get("/me/onboarding").body()).path("hasCv").asBoolean());

        String remove = preferences.strip().replace("}", ",\"removeCv\":true}");
        assertEquals(200, savePreferences(remove, null).statusCode());
        assertFalse(json.readTree(get("/me/onboarding").body()).path("hasCv").asBoolean());
    }

    private HttpResponse<String> savePreferences(String preferences, byte[] cv) throws Exception {
        String boundary = "interviewai-test";
        java.util.List<byte[]> parts = new java.util.ArrayList<>();
        parts.add(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"preferences\"\r\n"
                + "Content-Type: application/json\r\n\r\n" + preferences).getBytes(StandardCharsets.UTF_8));
        if (cv != null) {
            parts.add(("\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"cv\"; filename=\"cv.docx\""
                    + "\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            parts.add(cv);
        }
        parts.add(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        JsonNode token = csrf();
        HttpRequest request = HttpRequest.newBuilder(uri("/me/onboarding"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header(token.path("headerName").asText(), token.path("token").asText())
                .POST(HttpRequest.BodyPublishers.ofByteArrays(parts)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void failedNestedCourseInsertRollsBackBothCoursesAndNavigation() throws Exception {
        int userId = registerAndSignIn();
        GeneratedQuestion validQuestion = new GeneratedQuestion("A question?", "MULTIPLE_CHOICE",
                "Expected", "Explanation", List.of("Expected", "Other"));
        GeneratedQuestion invalidQuestion = new GeneratedQuestion("Another question?", "MULTIPLE_CHOICE",
                "Expected", "Explanation", List.of("x".repeat(300)));
        GeneratedCourse technical = new GeneratedCourse("Technical",
                List.of(new GeneratedChapter(1, "Chapter", "Topics", List.of(validQuestion))));
        GeneratedCourse softSkills = new GeneratedCourse("Soft skills",
                List.of(new GeneratedChapter(1, "Chapter", "Topics", List.of(invalidQuestion))));
        CourseJob job = new CourseJob(-1, userId, "RUNNING", 95, "Saving", null, null);
        assertThrows(DataIntegrityViolationException.class, () -> courseSaver.save(job, technical, softSkills));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM generated_courses WHERE user_id = ?",
                Integer.class, userId));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM interview_prep WHERE user_id = ?",
                Integer.class, userId));
        assertTrue(json.readTree(get("/courses").body()).isEmpty());
    }

    @Test
    void generationSavesBothCoursesAndDeduplicatesRepeatedRequests() throws Exception {
        int userId = registerAndSignIn();
        saveGenerationInput("Software developer");
        Map<String, String> request = Map.of("requestId", UUID.randomUUID().toString());
        HttpResponse<String> accepted = post("/course-jobs", request, csrf());
        assertEquals(202, accepted.statusCode(), accepted.body());
        long jobId = json.readTree(accepted.body()).path("id").asLong();
        JsonNode duplicate = json.readTree(post("/course-jobs", request, csrf()).body());
        assertEquals(jobId, duplicate.path("id").asLong());

        JsonNode result = awaitCourseJob(jobId);
        assertEquals("SUCCEEDED", result.path("status").asText(), result.toString());
        assertEquals(100, result.path("progress").asInt());
        assertEquals(2, json.readTree(get("/courses").body()).size());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM interview_prep WHERE user_id = ?", Integer.class, userId));
        assertEquals(960, jdbc.queryForObject("""
                SELECT COUNT(*) FROM questions q JOIN chapters c ON c.id = q.chapter_id
                JOIN generated_courses g ON g.id = c.course_id WHERE g.user_id = ?
                """, Integer.class, userId));
        assertEquals(jobId, json.readTree(get("/course-jobs/latest").body()).path("id").asLong());

        int course = json.readTree(get("/courses").body()).get(0).path("id").asInt();
        int chapter = json.readTree(get("/courses/" + course + "/chapters").body()).get(0).path("id").asInt();
        assertEquals(204, saveLocation(course, chapter).statusCode());
        JsonNode questions = json.readTree(get("/chapters/" + chapter + "/questions").body());
        assertEquals(40, questions.size());
        int missedQuestion = questions.get(0).path("id").asInt();
        JsonNode incorrect = answer(missedQuestion, "LESSON", "Second", UUID.randomUUID().toString(), 200);
        assertEquals(0, incorrect.path("score").asInt());
        for (int index = 1; index < questions.size(); index++) {
            JsonNode question = questions.get(index);
            boolean multipleChoice = "MULTIPLE_CHOICE".equals(question.path("type").asText());
            int questionId = question.path("id").asInt();
            JsonNode answered = answer(questionId, "LESSON", multipleChoice ? "Example" : "mostly correct",
                    UUID.randomUUID().toString(), multipleChoice ? 200 : 202);
            if (!multipleChoice) {
                answered = awaitAttempt(answered);
            }
            assertEquals(10, answered.path("xpAwarded").asInt());
        }
        JsonNode review = json.readTree(get("/chapters/" + chapter + "/review").body());
        assertEquals(1, review.size());
        assertEquals(missedQuestion, review.get(0).path("id").asInt());
        assertEquals(0, answer(missedQuestion, "REVIEW", "Example", UUID.randomUUID().toString(), 200)
                .path("xpAwarded").asInt());
        JsonNode summary = json.readTree(get("/progress").body());
        assertEquals(390, summary.path("totalXp").asInt());
        assertEquals(1, summary.path("completedChapters").asInt());
        assertEquals(204, post("/auth/logout", Map.of(), csrf()).statusCode());
        assertEquals(401, get("/progress").statusCode());
    }

    @Test
    void anInvalidSecondCourseLeavesNoPartialCourseAndCanBeRetried() throws Exception {
        registerAndSignIn();
        saveGenerationInput("FAIL_SOFT_SKILLS");
        Map<String, String> request = Map.of("requestId", UUID.randomUUID().toString());
        HttpResponse<String> accepted = post("/course-jobs", request, csrf());
        assertEquals(202, accepted.statusCode(), accepted.body());
        long jobId = json.readTree(accepted.body()).path("id").asLong();
        JsonNode failed = awaitCourseJob(jobId);
        assertEquals("FAILED", failed.path("status").asText(), failed.toString());
        assertTrue(json.readTree(get("/courses").body()).isEmpty());

        saveGenerationInput("Software developer");
        HttpResponse<String> retry = post("/course-jobs/" + jobId + "/retry",
                Map.of("requestId", UUID.randomUUID().toString()), csrf());
        assertEquals(202, retry.statusCode(), retry.body());
        long retryId = json.readTree(retry.body()).path("id").asLong();
        assertEquals("SUCCEEDED", awaitCourseJob(retryId).path("status").asText());
    }

    private int registerAndSignIn() throws Exception {
        HttpResponse<String> registration = post("/auth/register", registration(""), csrf());
        assertEquals(201, registration.statusCode(), registration.body());
        assertEquals(200, login(username, PASSWORD).statusCode());
        return json.readTree(registration.body()).path("id").asInt();
    }

    private void saveGenerationInput(String context) throws Exception {
        String input = json.writeValueAsString(Map.of("interviewType", "JOB", "language", "ENGLISH",
                "timeline", "LATER", "context", context));
        HttpResponse<String> saved = savePreferences(input, null);
        assertEquals(200, saved.statusCode(), saved.body());
    }

    private JsonNode awaitCourseJob(long id) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            HttpResponse<String> response = get("/course-jobs/" + id);
            assertEquals(200, response.statusCode(), response.body());
            JsonNode job = json.readTree(response.body());
            String status = job.path("status").asText();
            if (!"QUEUED".equals(status) && !"RUNNING".equals(status)) {
                return job;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Course generation did not finish.");
    }

    @Test
    void lessonAnswersAndReviewUpdateProgressAndPreventDuplicateXp() throws Exception {
        int userId = registerAndSignIn();
        int course = insertCourse(userId, "Practice");
        int chapter = insertChapter(course, "Test questions");
        int first = addQuestion(chapter, "First?", "MULTIPLE_CHOICE");
        int second = addQuestion(chapter, "Second?", "MULTIPLE_CHOICE");
        JsonNode questions = json.readTree(get("/chapters/" + chapter + "/questions").body());
        assertFalse(questions.get(0).has("correctAnswer"));
        assertEquals(2, questions.size());

        String request = UUID.randomUUID().toString();
        JsonNode correct = answer(first, "LESSON", "Expected", request, 200);
        assertEquals(10, correct.path("xpAwarded").asInt());
        assertEquals(correct.path("id"), answer(first, "LESSON", "Expected", request, 200).path("id"));
        Map<String, String> explanationRequest = Map.of("attemptId", correct.path("id").asText());
        HttpResponse<String> explained = post("/questions/" + first + "/explanations", explanationRequest, csrf());
        assertEquals(200, explained.statusCode(), explained.body());
        assertEquals("Sample explanation.", json.readTree(explained.body()).path("explanation").asText());
        assertEquals(404, post("/questions/" + second + "/explanations", explanationRequest, csrf()).statusCode());
        assertEquals(0, answer(first, "LESSON", "Expected", UUID.randomUUID().toString(), 200)
                .path("xpAwarded").asInt());
        JsonNode incorrect = answer(second, "LESSON", "Other", UUID.randomUUID().toString(), 200);
        assertEquals(0, incorrect.path("score").asInt());
        assertEquals(503, post("/questions/" + second + "/explanations",
                Map.of("attemptId", incorrect.path("id").asText()), csrf()).statusCode());
        assertEquals(1, json.readTree(get("/courses/" + course + "/review").body()).size());
        assertEquals("COMPLETED", jdbc.queryForObject("SELECT status FROM chapters WHERE id = ?", String.class, chapter));
        assertEquals(0, answer(second, "REVIEW", "Expected", UUID.randomUUID().toString(), 200)
                .path("xpAwarded").asInt());
        assertTrue(json.readTree(get("/chapters/" + chapter + "/review").body()).isEmpty());
        assertEquals(10, json.readTree(get("/dashboard").body()).path("totalXp").asInt());
    }

    @Test
    void staleGradingResultRollsBackProgressWhenItsAttemptWasReset() throws Exception {
        int userId = registerAndSignIn();
        int course = insertCourse(userId, "Reset race");
        int chapter = insertChapter(course, "Reset chapter");
        int question = addQuestion(chapter, "Explain the concept.", "SHORT_ANSWER");
        long id = practiceRepository.addAttempt(userId, question, UUID.randomUUID().toString(),
                "LESSON", "Saved answer", "RUNNING");
        PracticeAttempt inFlight = practiceRepository.findAttempt(userId, id).orElseThrow();
        jdbc.update("DELETE FROM practice_attempts WHERE id = ?", id);
        assertThrows(IllegalStateException.class, () -> practiceService.completeGrading(inFlight,
                new AnswerGrade(100, "Clear.", "Add examples.", "Expected explanation.")));
        assertEquals("NOT_STARTED", jdbc.queryForObject("SELECT status FROM questions WHERE id = ?", String.class, question));
        assertEquals("NOT_STARTED", jdbc.queryForObject("SELECT status FROM chapters WHERE id = ?", String.class, chapter));
        assertEquals(0, json.readTree(get("/progress").body()).path("totalXp").asInt());
    }

    @Test
    void concurrentCorrectAnswersAwardXpOnceAndBoundPendingWork() throws Exception {
        int userId = registerAndSignIn();
        int course = insertCourse(userId, "Concurrent practice");
        int chapter = insertChapter(course, "Concurrent chapter");
        int question = addQuestion(chapter, "Choose an answer.", "MULTIPLE_CHOICE");
        String path = "/questions/" + question + "/attempts";
        JsonNode token = csrf();
        Map<String, String> first = Map.of("mode", "LESSON", "answer", "Expected", "requestId", UUID.randomUUID().toString());
        Map<String, String> second = Map.of("mode", "LESSON", "answer", "Expected", "requestId", UUID.randomUUID().toString());
        CompletableFuture<HttpResponse<String>> pending = client.sendAsync(
                writeRequest("POST", path, first, token), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> other = post(path, second, token);
        HttpResponse<String> completed = pending.get(5, TimeUnit.SECONDS);
        assertEquals(200, completed.statusCode(), completed.body());
        assertEquals(200, other.statusCode(), other.body());
        assertEquals(10, json.readTree(completed.body()).path("xpAwarded").asInt()
                + json.readTree(other.body()).path("xpAwarded").asInt());

        for (int count = 0; count < 10; count++) {
            jdbc.update("""
                    INSERT INTO practice_attempts (user_id, question_id, request_id, mode, answer, status)
                    VALUES (?, ?, ?, 'LESSON', 'Saved answer', 'RUNNING')
                    """, userId, question, UUID.randomUUID().toString());
        }
        assertEquals(409, post(path, Map.of("mode", "LESSON", "answer", "Other",
                "requestId", UUID.randomUUID().toString()), csrf()).statusCode());
        assertEquals(200, post(path, first, csrf()).statusCode());
        jdbc.update("UPDATE practice_attempts SET status = 'FAILED' WHERE user_id = ? AND status = 'RUNNING'", userId);
    }

    @Test
    void shortAnswersUseThePassingThresholdAndFailuresLeaveProgressAlone() throws Exception {
        int userId = registerAndSignIn();
        int course = insertCourse(userId, "Short answers");
        int chapter = insertChapter(course, "Test questions");
        int question = addQuestion(chapter, "Explain the concept.", "SHORT_ANSWER");
        JsonNode failure = awaitAttempt(answer(question, "LESSON", "unavailable", UUID.randomUUID().toString(), 202));
        assertEquals("FAILED", failure.path("status").asText());
        assertEquals("NOT_STARTED", jdbc.queryForObject("SELECT status FROM questions WHERE id = ?", String.class, question));
        JsonNode resumed = json.readTree(get("/chapters/" + chapter + "/questions").body()).get(0).path("savedAttempt");
        assertEquals("unavailable", resumed.path("answer").asText());
        assertEquals("FAILED", resumed.path("status").asText());
        assertEquals("LESSON", resumed.path("mode").asText());

        JsonNode below = awaitAttempt(answer(question, "LESSON", "partial", UUID.randomUUID().toString(), 202));
        assertEquals(69, below.path("score").asInt());
        assertEquals(0, below.path("xpAwarded").asInt());
        JsonNode passing = awaitAttempt(answer(question, "LESSON", "mostly correct", UUID.randomUUID().toString(), 202));
        assertEquals(70, passing.path("score").asInt());
        assertEquals(10, passing.path("xpAwarded").asInt());
        HttpResponse<String> explained = post("/questions/" + question + "/explanations",
                Map.of("attemptId", passing.path("id").asText()), csrf());
        assertEquals(200, explained.statusCode(), explained.body());
        assertEquals("Sample grade.", json.readTree(explained.body()).path("explanation").asText());
    }

    private int addQuestion(int chapter, String title, String type) {
        jdbc.update("""
                INSERT INTO questions (chapter_id, question, question_type, correct_answer, explanation)
                VALUES (?, ?, ?, 'Expected', 'Review the example.')
                """, chapter, title, type);
        int id = jdbc.queryForObject("SELECT id FROM questions WHERE chapter_id = ? AND question = ?",
                Integer.class, chapter, title);
        if ("MULTIPLE_CHOICE".equals(type)) {
            jdbc.update("INSERT INTO choices (question_id, choice_text) VALUES (?, 'Expected'), (?, 'Other')", id, id);
        }
        return id;
    }

    private JsonNode answer(int question, String mode, String text, String request, int expectedStatus) throws Exception {
        HttpResponse<String> response = post("/questions/" + question + "/attempts",
                Map.of("mode", mode, "answer", text, "requestId", request), csrf());
        assertEquals(expectedStatus, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    private JsonNode awaitAttempt(JsonNode accepted) throws Exception {
        long id = accepted.path("id").asLong();
        for (int attempt = 0; attempt < 50; attempt++) {
            HttpResponse<String> response = get("/attempts/" + id);
            assertEquals(200, response.statusCode(), response.body());
            JsonNode result = json.readTree(response.body());
            String status = result.path("status").asText();
            if ("GRADED".equals(status) || "FAILED".equals(status)) {
                return result;
            }
            Thread.sleep(150);
        }
        throw new AssertionError("Answer grading did not finish.");
    }

    @Test
    void goalsUseTheirStartingValueAndQuestClaimsAwardXpOnlyOnce() throws Exception {
        int userId = registerAndSignIn();
        int course = insertCourse(userId, "Quest course");
        int chapter = insertChapter(course, "Test questions");
        jdbc.update("INSERT INTO progress (user_id, course_id, xp) VALUES (?, ?, 40)", userId, course);
        HttpResponse<String> goal = post("/goals", Map.of("name", "Earn points", "type", "XP", "target", "100"), csrf());
        assertEquals(201, goal.statusCode(), goal.body());
        assertEquals(40, json.readTree(get("/goals").body()).get(0).path("startValue").asInt());
        for (int count = 1; count <= 20; count++) {
            jdbc.update("""
                    INSERT INTO questions (chapter_id, question, status, updated_at)
                    VALUES (?, ?, 'COMPLETED', CURRENT_TIMESTAMP)
                    """, chapter, "Question " + count);
        }
        jdbc.update("UPDATE users SET daily_quest_last_reset = NULL WHERE id = ?", userId);
        assertTrue(json.readTree(get("/quests").body()).get(0).path("available").asBoolean());
        String daily = "/quests/daily/claims";
        Map<String, String> claim = Map.of("requestId", UUID.randomUUID().toString());
        Map<String, String> otherClaim = Map.of("requestId", UUID.randomUUID().toString());
        JsonNode token = csrf();
        CompletableFuture<HttpResponse<String>> pendingClaim = client.sendAsync(
                writeRequest("POST", daily, claim, token), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> second = client.send(writeRequest("POST", daily, otherClaim, token),
                HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> first = pendingClaim.get(5, TimeUnit.SECONDS);
        HttpResponse<String> winner = first.statusCode() == 200 ? first : second;
        assertEquals(200, winner.statusCode(), winner.body());
        assertEquals(409, (first.statusCode() == 200 ? second : first).statusCode());
        assertEquals(900, json.readTree(winner.body()).path("xpAwarded").asInt());
        assertEquals(200, post(daily, first.statusCode() == 200 ? claim : otherClaim, csrf()).statusCode());
        assertEquals(409, post(daily, Map.of("requestId", UUID.randomUUID().toString()), csrf()).statusCode());
        assertEquals(900, json.readTree(get("/goals").body()).get(0).path("currentValue").asInt());
        int goalId = json.readTree(get("/goals").body()).get(0).path("id").asInt();
        String goalPath = "/goals/" + goalId;
        assertEquals(204, write("PATCH", goalPath, Map.of("name", "Earn more points", "type", "XP", "target", "1000"),
                csrf()).statusCode());
        JsonNode updatedGoal = json.readTree(get("/goals").body()).get(0);
        assertEquals("Earn more points", updatedGoal.path("name").asText());
        assertEquals(1000, updatedGoal.path("target").asInt());
        assertEquals(40, updatedGoal.path("startValue").asInt());
        assertEquals(900, updatedGoal.path("currentValue").asInt());
        assertEquals(404, write("PATCH", "/goals/-1", Map.of("name", "Missing", "type", "XP", "target", "1000"),
                csrf()).statusCode());
        assertEquals(204, write("PATCH", goalPath, Map.of("name", "Answer more questions", "type", "QUESTIONS",
                "target", "30"), csrf()).statusCode());
        JsonNode changedGoal = json.readTree(get("/goals").body()).get(0);
        assertEquals(20, changedGoal.path("startValue").asInt());
        assertEquals(0, changedGoal.path("currentValue").asInt());

        for (int count = 1; count <= 12; count++) {
            jdbc.update("""
                    INSERT INTO chapters (course_id, chapter_number, status, completed_at)
                    VALUES (?, ?, 'COMPLETED', CURRENT_TIMESTAMP)
                    """, course, count + 1);
        }
        String monthly = "/quests/monthly/claims";
        HttpResponse<String> monthlyResult = post(monthly, Map.of("requestId", UUID.randomUUID().toString()), csrf());
        assertEquals(200, monthlyResult.statusCode(), monthlyResult.body());
        assertEquals(409, post(monthly, Map.of("requestId", UUID.randomUUID().toString()), csrf()).statusCode());
        assertEquals(30940, json.readTree(get("/progress").body()).path("totalXp").asInt());
        assertEquals(7, json.readTree(get("/progress").body()).path("recentXp").size());
    }

    @Test
    void dashboardRestoresOnlyAnOwnedCourseAndChapter() throws Exception {
        HttpResponse<String> created = post("/auth/register", registration(""), csrf());
        assertEquals(201, created.statusCode(), created.body());
        int userId = json.readTree(created.body()).path("id").asInt();
        String otherUsername = username + "-other";
        jdbc.update("INSERT INTO users (username) VALUES (?)", otherUsername);
        int otherUser = jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Integer.class, otherUsername);
        int technical = insertCourse(userId, "Technical");
        int softSkills = insertCourse(userId, "Soft skills");
        int otherCourse = insertCourse(otherUser, "Private course");
        int chapter = insertChapter(technical, "First chapter");
        int softSkillsChapter = insertChapter(softSkills, "Second chapter");
        jdbc.update("""
                INSERT INTO interview_prep (user_id, technical_course_id, softskills_course_id)
                VALUES (?, ?, ?)
                """, userId, technical, softSkills);
        jdbc.update("INSERT INTO progress (user_id, course_id, xp) VALUES (?, ?, 42)", userId, technical);
        jdbc.update("INSERT INTO questions (chapter_id, question, status) VALUES (?, 'Example?', 'INCORRECT')", chapter);

        assertEquals(200, login(username, PASSWORD).statusCode());
        assertEquals(2, json.readTree(get("/courses").body()).size());
        JsonNode first = json.readTree(get("/dashboard").body());
        assertEquals(technical, first.path("activeCourseId").asInt());
        assertEquals(chapter, first.path("activeChapterId").asInt());
        assertEquals(1, first.path("chapters").get(0).path("answeredCount").asInt());
        assertEquals(42, first.path("totalXp").asInt());
        assertEquals(404, get("/courses/" + otherCourse + "/chapters").statusCode());
        assertEquals(404, saveLocation(otherCourse, null).statusCode());
        assertEquals(404, saveLocation(technical, softSkillsChapter).statusCode());
        assertEquals(204, saveLocation(softSkills, softSkillsChapter).statusCode());
        assertEquals(204, post("/auth/logout", Map.of(), csrf()).statusCode());
        assertEquals(200, login(username, PASSWORD).statusCode());

        JsonNode restored = json.readTree(get("/dashboard").body());
        assertEquals(softSkills, restored.path("activeCourseId").asInt());
        assertEquals(softSkillsChapter, restored.path("activeChapterId").asInt());
        jdbc.update("UPDATE generated_courses SET status = 'ARCHIVED' WHERE id = ?", softSkills);
        JsonNode fallback = json.readTree(get("/dashboard").body());
        assertEquals(technical, fallback.path("activeCourseId").asInt());
        assertEquals(chapter, fallback.path("activeChapterId").asInt());
    }

    private int insertCourse(int userId, String title) {
        jdbc.update("INSERT INTO generated_courses (user_id, course_title, status) VALUES (?, ?, 'ACTIVE')",
                userId, title);
        return jdbc.queryForObject("SELECT id FROM generated_courses WHERE user_id = ? AND course_title = ?",
                Integer.class, userId, title);
    }

    private int insertChapter(int courseId, String name) {
        jdbc.update("INSERT INTO chapters (course_id, chapter_number, name) VALUES (?, 1, ?)", courseId, name);
        return jdbc.queryForObject("SELECT id FROM chapters WHERE course_id = ?", Integer.class, courseId);
    }

    private HttpResponse<String> saveLocation(int courseId, Integer chapterId) throws Exception {
        Map<String, Integer> body = new java.util.HashMap<>();
        body.put("courseId", courseId);
        body.put("chapterId", chapterId);
        JsonNode csrf = csrf();
        HttpRequest request = HttpRequest.newBuilder(uri("/me/navigation"))
                .header("Content-Type", "application/json")
                .header(csrf.path("headerName").asText(), csrf.path("token").asText())
                .PUT(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Map<String, String> registration(String email) {
        return Map.of("username", username, "email", email, "password", PASSWORD);
    }

    private String hashFor(String name) {
        return jdbc.queryForObject("SELECT password_hash FROM users WHERE username = ?", String.class, name);
    }

    private String sessionId() {
        return cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("JSESSIONID"))
                .findFirst().map(HttpCookie::getValue).orElseThrow();
    }

    private JsonNode csrf() throws Exception {
        HttpResponse<String> response = get("/auth/csrf");
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, Map<String, String> body, JsonNode csrf) throws Exception {
        return write("POST", path, body, csrf);
    }

    private HttpResponse<String> write(String method, String path, Map<String, String> body, JsonNode csrf)
            throws Exception {
        return client.send(writeRequest(method, path, body, csrf), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest writeRequest(String method, String path, Map<String, String> body, JsonNode csrf)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (csrf != null) {
            builder.header(csrf.path("headerName").asText(), csrf.path("token").asText());
        }
        return builder.build();
    }

    @Test
    void profileChangesInvalidateOtherSessionsAndDeletionRemovesOwnedData() throws Exception {
        int userId = registerAndSignIn();
        int course = insertCourse(userId, "Owned course");
        insertChapter(course, "Owned chapter");
        jdbc.update("INSERT INTO progress (user_id, course_id, xp) VALUES (?, ?, 10)", userId, course);

        String name = username + "-renamed";
        HttpResponse<String> updated = write("PATCH", "/me", Map.of("username", name), csrf());
        assertEquals(200, updated.statusCode(), updated.body());
        assertEquals(name, json.readTree(get("/me").body()).path("username").asText());
        CookieManager secondSession = new CookieManager();
        try (HttpClient secondClient = HttpClient.newBuilder().cookieHandler(secondSession).build()) {
            HttpClient firstClient = client;
            client = secondClient;
            try {
                assertEquals(200, login(name, PASSWORD).statusCode());
            } finally {
                client = firstClient;
            }
            String replacement = "synthetic-updated-password";
            assertEquals(400, write("PUT", "/me/password",
                    Map.of("currentPassword", "wrong", "newPassword", replacement), csrf()).statusCode());
            assertEquals(204, write("PUT", "/me/password",
                    Map.of("currentPassword", PASSWORD, "newPassword", replacement), csrf()).statusCode());
            HttpRequest secondRequest = HttpRequest.newBuilder(uri("/me")).GET().build();
            assertEquals(401, secondClient.send(secondRequest, HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(401, login(name, PASSWORD).statusCode());
            assertEquals(200, login(name, replacement).statusCode());
            assertEquals(204, post("/me/account-deletion", Map.of("password", replacement), csrf()).statusCode());
        }
        assertEquals(401, get("/me").statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM generated_courses WHERE id = ?", Integer.class, course));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM progress WHERE user_id = ?", Integer.class, userId));
    }

    @Test
    void administratorCanFilterUsersResetProgressAndManageCourses() throws Exception {
        int adminId = registerAndSignIn();
        jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE name = 'SUPER_ADMIN'",
                adminId);
        assertEquals(200, login(username, PASSWORD).statusCode());
        String candidateName = username + "-managed";
        jdbc.update("INSERT INTO users (username) VALUES (?)", candidateName);
        int candidateId = jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Integer.class, candidateName);
        int courseId = insertCourse(candidateId, "Managed course");
        int chapterId = insertChapter(courseId, "Managed chapter");
        jdbc.update("UPDATE chapters SET status = 'COMPLETED' WHERE id = ?", chapterId);
        jdbc.update("INSERT INTO progress (user_id, course_id, xp) VALUES (?, ?, 50)", candidateId, courseId);

        HttpResponse<String> filtered = get("/admin/users?search=" + candidateName + "&status=ACTIVE");
        assertEquals(200, filtered.statusCode(), filtered.body());
        assertEquals(1, json.readTree(filtered.body()).path("total").asInt());
        String user = "/admin/users/" + candidateId;
        assertEquals(204, post(user + "/xp-reset", Map.of(), csrf()).statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT SUM(xp) FROM progress WHERE user_id = ?", Integer.class, candidateId));
        assertEquals("COMPLETED", jdbc.queryForObject("SELECT status FROM chapters WHERE id = ?", String.class, chapterId));
        assertEquals(204, post(user + "/progress-reset", Map.of(), csrf()).statusCode());
        assertEquals("NOT_STARTED", jdbc.queryForObject("SELECT status FROM chapters WHERE id = ?", String.class, chapterId));
        assertEquals(204, write("PATCH", user, Map.of("active", "false"), csrf()).statusCode());
        assertEquals(1, json.readTree(get("/admin/users?search=" + candidateName + "&status=SUSPENDED")
                .body()).path("total").asInt());
        assertEquals(204, write("PATCH", user, Map.of("active", "true"), csrf()).statusCode());
        HttpResponse<String> overviewResponse = get("/admin/overview");
        assertEquals(200, overviewResponse.statusCode());
        JsonNode overview = json.readTree(overviewResponse.body());
        assertEquals(jdbc.queryForObject("SELECT COUNT(*) FROM questions WHERE status IN (\'COMPLETED\', \'INCORRECT\')",
                Integer.class), overview.path("answeredQuestions").asInt());
        assertTrue(overview.path("averageXp").asDouble() >= 0);

        String course = "/admin/courses/" + courseId;
        assertEquals(204, write("PATCH", course, Map.of("status", "ARCHIVED"), csrf()).statusCode());
        assertEquals("ARCHIVED", json.readTree(get(course).body()).path("status").asText());
        assertEquals(204, write("PATCH", course, Map.of("status", "ACTIVE"), csrf()).statusCode());
        assertEquals(204, write("DELETE", course, Map.of(), csrf()).statusCode());
        assertEquals(404, get(course).statusCode());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + "/api/v1" + path);
    }
}
