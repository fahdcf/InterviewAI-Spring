package com.interviewai.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String PASSWORD = "synthetic-test-password";

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
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (csrf != null) {
            builder.header(csrf.path("headerName").asText(), csrf.path("token").asText());
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + "/api/v1" + path);
    }
}
