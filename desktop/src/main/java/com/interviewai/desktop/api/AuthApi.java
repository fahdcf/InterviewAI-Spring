package com.interviewai.desktop.api;

import java.io.IOException;
import java.util.Map;

public class AuthApi {

    private final ApiClient apiClient;

    public AuthApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public AccountSummary login(String identifier, String password)
            throws IOException, InterruptedException {
        AccountSummary account = post("/api/v1/auth/login", Map.of("identifier", identifier, "password", password),
                AccountSummary.class);
        if (account == null || account.getId() < 1 || account.getUsername() == null) {
            throw new IOException("The backend did not return an account.");
        }
        return account;
    }

    public void register(String username, String email, String password)
            throws IOException, InterruptedException {
        post("/api/v1/auth/register",
                Map.of("username", username, "email", email, "password", password), AccountSummary.class);
    }

    public void logout() throws IOException, InterruptedException {
        post("/api/v1/auth/logout", Map.of(), Void.class);
    }

    private <T> T post(String path, Map<String, String> body, Class<T> responseType)
            throws IOException, InterruptedException {
        return apiClient.writeJson("POST", path, body, responseType);
    }
}
