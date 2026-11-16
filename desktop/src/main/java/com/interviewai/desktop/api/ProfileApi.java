package com.interviewai.desktop.api;

import java.io.IOException;
import java.util.Map;

public class ProfileApi {

    private final ApiClient api;

    public ProfileApi(ApiClient api) {
        this.api = api;
    }

    public AccountSummary rename(String username) throws IOException, InterruptedException {
        return api.writeJson("PATCH", "/api/v1/me", Map.of("username", username), AccountSummary.class);
    }

    public void changePassword(String current, String replacement) throws IOException, InterruptedException {
        api.writeJson("PUT", "/api/v1/me/password",
                Map.of("currentPassword", current, "newPassword", replacement), Void.class);
    }

    public void delete(String password) throws IOException, InterruptedException {
        api.writeJson("POST", "/api/v1/me/account-deletion", Map.of("password", password), Void.class);
    }
}
