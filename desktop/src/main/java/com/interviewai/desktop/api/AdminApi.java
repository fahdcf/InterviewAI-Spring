package com.interviewai.desktop.api;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class AdminApi {

    private static final String BASE = "/api/v1/admin";
    private final ApiClient api;

    public AdminApi(ApiClient api) {
        this.api = api;
    }

    public Overview overview() throws IOException, InterruptedException {
        return api.readJson(BASE + "/overview", Overview.class);
    }

    public UserPage users(String search, String status, String role, int page)
            throws IOException, InterruptedException {
        return api.readJson(BASE + "/users?search=" + encode(search) + "&status=" + encode(status)
                + "&role=" + encode(role) + "&page=" + page, UserPage.class);
    }

    public List<String> roles() throws IOException, InterruptedException {
        return List.of(api.readJson(BASE + "/users/roles", String[].class));
    }

    public CoursePage courses(String search, String status, int page) throws IOException, InterruptedException {
        return api.readJson(BASE + "/courses?search=" + encode(search) + "&status=" + encode(status)
                + "&page=" + page, CoursePage.class);
    }

    public void updateUser(int id, Map<String, Object> change) throws IOException, InterruptedException {
        api.writeJson("PATCH", BASE + "/users/" + id, change, Void.class);
    }

    public void reset(int id, boolean progress) throws IOException, InterruptedException {
        api.writeJson("POST", BASE + "/users/" + id + (progress ? "/progress-reset" : "/xp-reset"), Map.of(), Void.class);
    }

    public void archiveCourse(int id, boolean archive) throws IOException, InterruptedException {
        api.writeJson("PATCH", BASE + "/courses/" + id, Map.of("status", archive ? "ARCHIVED" : "ACTIVE"), Void.class);
    }

    public void deleteCourse(int id) throws IOException, InterruptedException {
        api.writeJson("DELETE", BASE + "/courses/" + id, Map.of(), Void.class);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record User(int id, String username, String email, boolean active, long xp,
                       int courseCount, String joined, List<String> roles) {
    }

    public record Course(int id, int userId, String owner, String title, String status, int chapters,
                         int completedChapters, int questions, String created) {
    }

    public record UserPage(List<User> items, int page, int total) {
    }

    public record CoursePage(List<Course> items, int page, int total) {
    }

    public record Overview(int users, int activeUsers, int dailySignups, int weeklySignups, int monthlySignups,
                           int courses, int completedCourses, int completedChapters, int questions,
                           int answeredQuestions, double averageXp,
                           Map<String, Long> interviewTypes, Map<String, Long> languages,
                           Map<String, Long> signupTrend, Map<String, Long> usageTrend) {
    }
}
