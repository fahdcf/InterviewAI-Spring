package com.interviewai.desktop.feature.onboarding;

import com.interviewai.desktop.api.AccountSummary;
import com.interviewai.desktop.api.ApiException;
import com.interviewai.desktop.api.CourseJobApi;
import com.interviewai.desktop.api.dto.CourseJobStatus;
import com.interviewai.desktop.navigation.DesktopNavigator;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public class CourseWaitingController {

    private final AccountSummary account;
    private final CourseJobApi api;
    private final DesktopNavigator navigator;
    private final Executor executor;
    private final PauseTransition nextPoll = new PauseTransition(Duration.seconds(2));

    @FXML private Label statusLabel;
    @FXML private ProgressBar progress;
    @FXML private Button retryButton;

    private CourseJobStatus job;
    private UUID retryRequest;
    private boolean closed;

    public CourseWaitingController(AccountSummary account, CourseJobApi api, DesktopNavigator navigator,
            Executor executor, CourseJobStatus job) {
        this.account = account;
        this.api = api;
        this.navigator = navigator;
        this.executor = executor;
        this.job = job;
    }

    @FXML
    private void initialize() {
        nextPoll.setOnFinished(event -> refresh());
        if (job == null) {
            refresh();
        } else {
            display(job);
        }
    }

    @FXML
    private void retry() {
        if (job != null && isStopped(job)) {
            long id = job.getId();
            if (retryRequest == null) {
                retryRequest = UUID.randomUUID();
            }
            UUID request = retryRequest;
            load(() -> api.retry(id, request));
        } else {
            refresh();
        }
    }

    @FXML
    private void back() {
        closed = true;
        nextPoll.stop();
        navigator.showDashboard(account);
    }

    private void refresh() {
        if (job == null) {
            load(api::latest);
        } else {
            long id = job.getId();
            load(() -> api.get(id));
        }
    }

    private void load(Callable<CourseJobStatus> action) {
        retryButton.setDisable(true);
        Task<CourseJobStatus> task = new Task<>() {
            @Override
            protected CourseJobStatus call() throws Exception {
                return action.call();
            }
        };
        task.setOnSucceeded(event -> {
            if (!closed) {
                display(task.getValue());
            }
        });
        task.setOnFailed(event -> {
            if (closed) {
                return;
            }
            if (task.getException() instanceof ApiException error && error.getStatus() == 401) {
                closed = true;
                navigator.showLogin("Sign in again to continue.");
                return;
            }
            statusLabel.setText(task.getException() instanceof ApiException error
                    ? error.getMessage() : "Could not reach the backend. Check the connection and retry.");
            retryButton.setDisable(false);
        });
        executor.execute(task);
    }

    private void display(CourseJobStatus latest) {
        job = latest;
        if (job == null) {
            progress.setProgress(0);
            statusLabel.setText("No courses are being generated. Start from Interview preferences.");
            retryButton.setDisable(false);
            return;
        }
        retryRequest = null;
        statusLabel.setText(job.getMessage());
        progress.setProgress(job.getProgress() / 100.0);
        switch (job.getStatus()) {
            case "QUEUED", "RUNNING" -> {
                retryButton.setDisable(true);
                nextPoll.playFromStart();
            }
            case "SUCCEEDED" -> Platform.runLater(() -> {
                if (!closed) {
                    back();
                }
            });
            default -> retryButton.setDisable(false);
        }
    }

    private boolean isStopped(CourseJobStatus status) {
        return "FAILED".equals(status.getStatus()) || "INTERRUPTED".equals(status.getStatus());
    }
}
