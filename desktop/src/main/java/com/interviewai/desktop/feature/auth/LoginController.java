package com.interviewai.desktop.feature.auth;

import com.interviewai.desktop.api.AccountSummary;
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

public class LoginController {

    private final AuthApi authApi;
    private final DesktopNavigator navigator;
    private final Executor executor;
    private final String initialMessage;

    @FXML private VBox form;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    public LoginController(AuthApi authApi, DesktopNavigator navigator, Executor executor, String initialMessage) {
        this.authApi = authApi;
        this.navigator = navigator;
        this.executor = executor;
        this.initialMessage = initialMessage;
    }

    @FXML
    private void initialize() {
        statusLabel.setText(initialMessage);
    }

    @FXML
    private void onLogin() {
        String identifier = usernameField.getText().strip();
        String password = passwordField.getText();
        if (identifier.isBlank() || password.isBlank()) {
            statusLabel.setText("Enter your username or email and password.");
            return;
        }

        form.setDisable(true);
        passwordField.clear();
        statusLabel.setText("Signing in…");
        Task<AccountSummary> task = new Task<>() {
            @Override
            protected AccountSummary call() throws Exception {
                return authApi.login(identifier, password);
            }
        };
        task.setOnSucceeded(event -> navigator.showAccount(task.getValue()));
        task.setOnFailed(event -> {
            form.setDisable(false);
            if (task.getException() instanceof ApiException error) {
                statusLabel.setText(error.getMessage());
            } else {
                statusLabel.setText("Could not sign in. Check the connection and try again.");
            }
        });
        executor.execute(task);
    }

    @FXML
    private void onOpenCreateAccount() {
        navigator.showRegistration();
    }

    @FXML
    private void onForgotPassword() {
        statusLabel.setText("Password reset is unavailable. Ask an administrator for help.");
    }

    @FXML
    private void onCheckConnection() {
        navigator.showConnection();
    }
}
