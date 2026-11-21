package com.interviewai.desktop.feature.profile;

import com.interviewai.desktop.api.AccountSummary;
import com.interviewai.desktop.navigation.ScreenController;
import com.interviewai.desktop.api.ApiException;
import com.interviewai.desktop.api.ProfileApi;
import com.interviewai.desktop.navigation.DesktopNavigator;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class ProfileController extends ScreenController {

    private AccountSummary account;
    private final ProfileApi api;
    private final DesktopNavigator navigator;
    private final Executor executor;

    @FXML private VBox form;
    @FXML private TextField username;
    @FXML private PasswordField currentPassword;
    @FXML private PasswordField newPassword;
    @FXML private PasswordField confirmPassword;
    @FXML private Button deleteButton;
    @FXML private Label status;
    @FXML private Label statistics;

    public ProfileController(AccountSummary account, ProfileApi api, DesktopNavigator navigator, Executor executor) {
        this.account = account;
        this.api = api;
        this.navigator = navigator;
        this.executor = executor;
    }

    @FXML
    private void initialize() {
        username.setText(account.getUsername());
        deleteButton.setVisible(!account.isAdministrator());
        deleteButton.setManaged(!account.isAdministrator());
        run("Loading profile…", api::progress, progress -> {
            statistics.setText(String.format("%,d XP · %d day streak · %d courses · %d completed chapters",
                    progress.totalXp(), progress.streak(), progress.totalCourses(), progress.completedChapters()));
            status.setText("");
        });
    }

    @FXML
    private void rename() {
        String name = username.getText().strip();
        if (name.isBlank()) {
            status.setText("Enter a username.");
            return;
        }
        run("Saving username…", () -> api.rename(name), updated -> {
            account = updated;
            username.setText(account.getUsername());
            status.setText("Username saved.");
        });
    }

    @FXML
    private void changePassword() {
        String oldValue = currentPassword.getText();
        String replacement = newPassword.getText();
        if (oldValue.isBlank() || replacement.length() < 6 || !replacement.equals(confirmPassword.getText())) {
            status.setText("Enter your current password and matching new passwords of at least six characters.");
            return;
        }
        clearPasswords();
        run("Changing password…", () -> {
            api.changePassword(oldValue, replacement);
            return null;
        }, result -> navigator.showLogin("Password changed. Sign in again."));
    }

    @FXML
    private void delete() {
        String password = currentPassword.getText();
        if (password.isBlank()) {
            status.setText("Enter your current password to confirm deletion.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Permanently delete your account, courses, and progress?", ButtonType.CANCEL, ButtonType.OK);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        clearPasswords();
        run("Deleting account…", () -> {
            api.delete(password);
            return null;
        }, result -> navigator.showLogin("Your account was deleted."));
    }

    @FXML
    private void back() {
        navigator.showAccount(account);
    }

    private void clearPasswords() {
        currentPassword.clear();
        newPassword.clear();
        confirmPassword.clear();
    }

    private <T> void run(String message, Callable<T> action, Consumer<T> success) {
        form.setDisable(true);
        status.setText(message);
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception { return action.call(); }
        };
        task.setOnSucceeded(event -> {
            form.setDisable(false);
            success.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            form.setDisable(false);
            if (task.getException() instanceof ApiException error && error.getStatus() == 401) {
                navigator.showLogin("Sign in again to continue.");
                return;
            }
            status.setText(task.getException() instanceof ApiException error
                    ? error.getMessage() : "The backend could not save this change. Try again.");
        });
        submit(task, executor);
    }
}
