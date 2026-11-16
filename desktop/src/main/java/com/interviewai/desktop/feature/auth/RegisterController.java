package com.interviewai.desktop.feature.auth;

import com.interviewai.desktop.api.ApiException;
import com.interviewai.desktop.api.AuthApi;
import com.interviewai.desktop.navigation.DesktopNavigator;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.concurrent.Executor;

public class RegisterController {

    private final AuthApi authApi;
    private final DesktopNavigator navigator;
    private final Executor executor;

    @FXML private VBox form;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    public RegisterController(AuthApi authApi, DesktopNavigator navigator, Executor executor) {
        this.authApi = authApi;
        this.navigator = navigator;
        this.executor = executor;
    }

    @FXML
    private void onCreate() {
        String username = usernameField.getText().strip();
        String email = emailField.getText().strip();
        String password = passwordField.getText();
        if (username.isBlank() || password.isBlank()) {
            statusLabel.setText("Enter a username and password.");
            return;
        }

        form.setDisable(true);
        passwordField.clear();
        statusLabel.setText("Creating account…");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                authApi.register(username, email, password);
                return null;
            }
        };
        task.setOnSucceeded(event -> navigator.showLogin("Account created. Sign in to continue."));
        task.setOnFailed(event -> {
            form.setDisable(false);
            if (task.getException() instanceof ApiException error) {
                statusLabel.setText(error.getMessage());
            } else {
                statusLabel.setText("Could not create the account. Check the connection and try again.");
            }
        });
        executor.execute(task);
    }

    @FXML
    private void onBack() {
        navigator.showLogin("");
    }
}
