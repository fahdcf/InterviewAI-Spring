package com.interviewai.desktop.api;

import com.interviewai.desktop.api.dto.DashboardResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DashboardApi {

    private final ApiClient api;

    public DashboardApi(ApiClient api) {
        this.api = api;
    }

    public DashboardResponse load() throws IOException, InterruptedException {
        return api.readJson("/api/v1/dashboard", DashboardResponse.class);
    }

    public void saveLocation(int courseId, Integer chapterId) throws IOException, InterruptedException {
        Map<String, Object> location = new HashMap<>();
        location.put("courseId", courseId);
        location.put("chapterId", chapterId);
        api.writeJson("PUT", "/api/v1/me/navigation", location, Void.class);
    }
}
