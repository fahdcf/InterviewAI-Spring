package com.interviewai.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.net.InetSocketAddress;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        provider.createContext("/generate", exchange -> {
            try (exchange) {
                ObjectMapper json = new ObjectMapper();
                String prompt = json.readTree(exchange.getRequestBody())
                        .path("contents").path(0).path("parts").path(0).path("text").asText();
                ObjectNode content = json.createObjectNode();
                ArrayNode chapters = content.putArray("chapters");
                if (prompt.contains("Generate a comprehensive interview preparation course outline")) {
                    boolean failSoftSkills = prompt.contains("FAIL_SOFT_SKILLS") && prompt.contains("Behavioural");
                    content.put("course_title", "Generated preparation");
                    for (int number = 1; number <= (failSoftSkills ? 2 : 12); number++) {
                        chapters.addObject().put("chapter_number", number)
                                .put("name", "Chapter " + number).put("description", "Practice topics.");
                    }
                } else {
                    Matcher matcher = Pattern.compile("(?m)^Chapter (\\d+):").matcher(prompt);
                    while (matcher.find()) {
                        int number = Integer.parseInt(matcher.group(1));
                        ArrayNode questions = chapters.addObject().put("chapter_number", number).putArray("questions");
                        for (int count = 1; count <= 40; count++) {
                            boolean multipleChoice = count <= 20;
                            ObjectNode question = questions.addObject().put("question", "Question " + count + "?")
                                    .put("question_type", multipleChoice ? "MULTIPLE_CHOICE" : "SHORT_ANSWER")
                                    .put("correct_answer", "Example").put("explanation", "Sample explanation.");
                            if (multipleChoice) {
                                ArrayNode choices = question.putArray("choices");
                                for (String choice : List.of("Example", "Second", "Third", "Fourth")) {
                                    choices.add(choice);
                                }
                            }
                        }
                    }
                }
                byte[] response = json.writeValueAsBytes(Map.of("candidates", List.of(Map.of(
                        "finishReason", "STOP", "content", Map.of("parts", List.of(
                                Map.of("text", json.writeValueAsString(content))))))));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });
        provider.createContext("/grade", exchange -> {
            try (exchange) {
                ObjectMapper json = new ObjectMapper();
                String userMessage = json.readTree(exchange.getRequestBody())
                        .path("messages").path(1).path("content").asText();
                String answer = json.readTree(userMessage).path("candidateAnswer").asText();
                if ("unavailable".equals(answer)) {
                    exchange.sendResponseHeaders(503, -1);
                    return;
                }
                String grade = json.writeValueAsString(Map.of(
                        "score", "mostly correct".equals(answer) ? 70 : 69,
                        "strengths", "You covered the basic idea.",
                        "improvements", "You can add an example.",
                        "explanation", "Sample grade."));
                byte[] response = json.writeValueAsBytes(Map.of("choices", List.of(
                        Map.of("message", Map.of("content", grade)))));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });
        provider.start();
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
        assertEquals(0, answer(first, "LESSON", "Expected", UUID.randomUUID().toString(), 200)
                .path("xpAwarded").asInt());
        assertEquals(0, answer(second, "LESSON", "Other", UUID.randomUUID().toString(), 200).path("score").asInt());
        assertEquals(1, json.readTree(get("/courses/" + course + "/review").body()).size());
        assertEquals("COMPLETED", jdbc.queryForObject("SELECT status FROM chapters WHERE id = ?", String.class, chapter));
        assertEquals(0, answer(second, "REVIEW", "Expected", UUID.randomUUID().toString(), 200)
                .path("xpAwarded").asInt());
        assertTrue(json.readTree(get("/chapters/" + chapter + "/review").body()).isEmpty());
        assertEquals(10, json.readTree(get("/dashboard").body()).path("totalXp").asInt());
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

        JsonNode below = awaitAttempt(answer(question, "LESSON", "partial", UUID.randomUUID().toString(), 202));
        assertEquals(69, below.path("score").asInt());
        assertEquals(0, below.path("xpAwarded").asInt());
        JsonNode passing = awaitAttempt(answer(question, "LESSON", "mostly correct", UUID.randomUUID().toString(), 202));
        assertEquals(70, passing.path("score").asInt());
        assertEquals(10, passing.path("xpAwarded").asInt());
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
        HttpResponse<String> first = post(daily, claim, csrf());
        assertEquals(200, first.statusCode(), first.body());
        assertEquals(900, json.readTree(first.body()).path("xpAwarded").asInt());
        assertEquals(200, post(daily, claim, csrf()).statusCode());
        assertEquals(409, post(daily, Map.of("requestId", UUID.randomUUID().toString()), csrf()).statusCode());
        assertEquals(900, json.readTree(get("/goals").body()).get(0).path("currentValue").asInt());

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
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (csrf != null) {
            builder.header(csrf.path("headerName").asText(), csrf.path("token").asText());
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
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
        assertEquals(200, get("/admin/overview").statusCode());

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
