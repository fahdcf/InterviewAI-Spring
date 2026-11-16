package com.interviewai.desktop;

import com.interviewai.desktop.api.ApiClient;
import com.interviewai.desktop.api.AuthApi;
import com.interviewai.desktop.api.DashboardApi;
import com.interviewai.desktop.api.HealthApi;
import com.interviewai.desktop.api.OnboardingApi;
import com.interviewai.desktop.navigation.DesktopNavigator;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DesktopApplication extends Application {

    private static final String DEFAULT_BACKEND_URL = "http://127.0.0.1:8080";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER))
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    public void start(Stage stage) {
        String backendAddress = System.getenv("INTERVIEWAI_API_URL");
        if (backendAddress == null || backendAddress.isBlank()) {
            backendAddress = DEFAULT_BACKEND_URL;
        }

        try {
            stage.setTitle("InterviewAI");
            ApiClient apiClient = new ApiClient(URI.create(backendAddress), httpClient);
            DesktopNavigator navigator = new DesktopNavigator(stage, new AuthApi(apiClient),
                    new HealthApi(apiClient), new DashboardApi(apiClient), executor, new OnboardingApi(apiClient),
                    backendAddress);
            navigator.showLogin("");
            stage.setMinWidth(420);
            stage.setMinHeight(440);
            stage.show();
        } catch (IllegalArgumentException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Set INTERVIEWAI_API_URL to a backend address such as " + DEFAULT_BACKEND_URL + ".");
            alert.setHeaderText("Invalid backend address");
            alert.showAndWait();
        }
    }

    @Override
    public void stop() {
        executor.shutdownNow();
        httpClient.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
