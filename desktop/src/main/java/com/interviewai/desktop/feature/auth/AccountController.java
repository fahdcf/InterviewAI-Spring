package com.interviewai.desktop.feature.auth;

import com.interviewai.desktop.api.AccountSummary;
import com.interviewai.desktop.api.AuthApi;
import com.interviewai.desktop.navigation.DesktopNavigator;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.concurrent.Executor;

public class AccountController {

    private final AuthApi authApi;
    private final DesktopNavigator navigator;
    private final Executor executor;
    private final AccountSummary account;

    @FXML private Label accountLabel;
    @FXML private Label statusLabel;
    @FXML private Button logoutButton;

    public AccountController(AuthApi authApi, DesktopNavigator navigator, Executor executor, AccountSummary account) {
        this.authApi = authApi;
        this.navigator = navigator;
        this.executor = executor;
        this.account = account;
    }

    @FXML
    private void initialize() {
        accountLabel.setText(account.getUsername());
        if (account.isAdministrator()) {
            statusLabel.setText("Signed in as an administrator.");
        } else {
            statusLabel.setText("Signed in as a candidate.");
        }
    }

    @FXML
    private void onLogout() {
        logoutButton.setDisable(true);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                authApi.logout();
                return null;
            }
        };
        task.setOnSucceeded(event -> navigator.showLogin("You signed out."));
        task.setOnFailed(event -> {
            logoutButton.setDisable(false);
            statusLabel.setText("Could not sign out. Check the connection and try again.");
        });
        executor.execute(task);
    }

    @FXML
    private void openProfile() {
        navigator.showProfile(account);
    }
}
