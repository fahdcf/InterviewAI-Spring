package com.interviewai.desktop;

import com.interviewai.desktop.api.ApiClient;
import com.interviewai.desktop.api.HealthApi;
import com.interviewai.desktop.feature.connection.ConnectionController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DesktopApplication extends Application {

    private static final String DEFAULT_BACKEND_URL = "http://127.0.0.1:8080";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    public void start(Stage stage) throws Exception {
        String backendAddress = System.getenv("INTERVIEWAI_API_URL");
        if (backendAddress == null || backendAddress.isBlank()) {
            backendAddress = DEFAULT_BACKEND_URL;
        }

        try {
            stage.setTitle("InterviewAI");
            stage.setScene(createScene(backendAddress));
            stage.setMinWidth(420);
            stage.setMinHeight(260);
            stage.show();
        } catch (IllegalArgumentException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Set INTERVIEWAI_API_URL to a backend address such as " + DEFAULT_BACKEND_URL + ".");
            alert.setHeaderText("Invalid backend address");
            alert.showAndWait();
        }
    }

    private Scene createScene(String backendAddress) throws Exception {
        ApiClient apiClient = new ApiClient(URI.create(backendAddress), httpClient);
        HealthApi healthApi = new HealthApi(apiClient);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/connection-view.fxml"));
        loader.setControllerFactory(controllerType -> {
            if (controllerType == ConnectionController.class) {
                return new ConnectionController(healthApi, executor, backendAddress);
            }
            throw new IllegalArgumentException("Unknown controller: " + controllerType.getName());
        });

        return new Scene(loader.load());
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
