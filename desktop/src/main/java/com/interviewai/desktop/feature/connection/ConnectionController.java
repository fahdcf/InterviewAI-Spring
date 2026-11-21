package com.interviewai.desktop.feature.connection;

import com.interviewai.desktop.api.HealthApi;
import com.interviewai.desktop.navigation.ScreenController;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.concurrent.Executor;

public class ConnectionController extends ScreenController {

    private final HealthApi healthApi;
    private final Executor executor;
    private final String backendAddress;
    private final Runnable openLogin;

    @FXML
    private Label addressLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button retryButton;

    public ConnectionController(HealthApi healthApi, Executor executor, String backendAddress, Runnable openLogin) {
        this.healthApi = healthApi;
        this.executor = executor;
        this.backendAddress = backendAddress;
        this.openLogin = openLogin;
    }

    @FXML
    private void onBack() {
        openLogin.run();
    }

    @FXML
    private void initialize() {
        addressLabel.setText(backendAddress);
        checkConnection();
    }

    @FXML
    private void checkConnection() {
        retryButton.setDisable(true);
        statusLabel.setText("Checking the backend…");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return healthApi.isBackendUp();
            }
        };

        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                statusLabel.setText("Connected to the backend.");
            } else {
                statusLabel.setText("The backend did not report that it is ready.");
            }
            retryButton.setDisable(false);
        });
        task.setOnFailed(event -> {
            statusLabel.setText("Could not connect. Check that the backend is running and retry.");
            retryButton.setDisable(false);
        });
        submit(task, executor);
    }
}
