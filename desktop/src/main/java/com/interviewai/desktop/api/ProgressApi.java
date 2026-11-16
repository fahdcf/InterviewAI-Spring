package com.interviewai.desktop.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProgressApi {

    private final ApiClient api;

    public ProgressApi(ApiClient api) {
        this.api = api;
    }

    public Page load() throws IOException, InterruptedException {
        return new Page(
                api.readJson("/api/v1/progress", Summary.class),
                List.of(api.readJson("/api/v1/goals", Goal[].class)),
                List.of(api.readJson("/api/v1/quests", Quest[].class)),
                List.of(api.readJson("/api/v1/leaderboard", Ranking[].class)));
    }

    public void addGoal(String name, String type, int target) throws IOException, InterruptedException {
        api.writeJson("POST", "/api/v1/goals", Map.of("name", name, "type", type, "target", target), Void.class);
    }

    public void updateGoal(int id, String name, String type, int target) throws IOException, InterruptedException {
        api.writeJson("PATCH", "/api/v1/goals/" + id,
                Map.of("name", name, "type", type, "target", target), Void.class);
    }

    public void removeGoal(int id) throws IOException, InterruptedException {
        api.writeJson("DELETE", "/api/v1/goals/" + id, Map.of(), Void.class);
    }

    public void claim(String type, UUID requestId) throws IOException, InterruptedException {
        api.writeJson("POST", "/api/v1/quests/" + type + "/claims", Map.of("requestId", requestId), Void.class);
    }

    public record Page(Summary summary, List<Goal> goals, List<Quest> quests, List<Ranking> leaderboard) {
    }

    public record Summary(long totalXp, int streak, int answered, int correct, int completedChapters,
                          int totalCourses, long estimatedMinutes, List<DailyXp> recentXp) {
    }

    public record DailyXp(String date, long xp) {
    }

    public record Goal(int id, String name, String type, int target, int startValue, long currentValue) {
    }

    public record Quest(String type, int current, int target, int reward, boolean claimed,
                        boolean available, String resetsAt) {
    }

    public record Ranking(String username, long xp) {
    }
}
