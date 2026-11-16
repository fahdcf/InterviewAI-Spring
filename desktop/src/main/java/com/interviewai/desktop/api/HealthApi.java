package com.interviewai.desktop.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class HealthApi {

    private final ApiClient apiClient;

    public HealthApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public boolean isBackendUp() throws IOException, InterruptedException {
        JsonNode response = apiClient.getJson("/api/v1/health");
        return response != null && "UP".equals(response.path("status").asText());
    }
}
