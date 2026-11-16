package com.interviewai.desktop.api;

import com.interviewai.desktop.api.dto.OnboardingPreferences;

import java.io.IOException;
import java.nio.file.Path;

public class OnboardingApi {

    private static final String PATH = "/api/v1/me/onboarding";
    private final ApiClient api;

    public OnboardingApi(ApiClient api) {
        this.api = api;
    }

    public OnboardingPreferences load() throws IOException, InterruptedException {
        return api.readJson(PATH, OnboardingPreferences.class);
    }

    public OnboardingPreferences save(OnboardingPreferences preferences, Path cv)
            throws IOException, InterruptedException {
        return api.uploadPreferences(PATH, preferences, cv, OnboardingPreferences.class);
    }
}
