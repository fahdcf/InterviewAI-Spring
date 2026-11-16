package com.interviewai.desktop.navigation;

import com.interviewai.desktop.api.AccountSummary;
import com.interviewai.desktop.api.AuthApi;
import com.interviewai.desktop.api.CourseJobApi;
import com.interviewai.desktop.api.DashboardApi;
import com.interviewai.desktop.api.HealthApi;
import com.interviewai.desktop.api.OnboardingApi;
import com.interviewai.desktop.api.PracticeApi;
import com.interviewai.desktop.api.ProgressApi;
import com.interviewai.desktop.api.ProfileApi;
import com.interviewai.desktop.api.dto.CourseJobStatus;
import com.interviewai.desktop.feature.auth.AccountController;
import com.interviewai.desktop.feature.auth.LoginController;
import com.interviewai.desktop.feature.auth.RegisterController;
import com.interviewai.desktop.feature.connection.ConnectionController;
import com.interviewai.desktop.feature.dashboard.DashboardController;
import com.interviewai.desktop.feature.onboarding.OnboardingController;
import com.interviewai.desktop.feature.onboarding.CourseWaitingController;
import com.interviewai.desktop.feature.practice.PracticeController;
import com.interviewai.desktop.feature.progress.ProgressController;
import com.interviewai.desktop.feature.profile.ProfileController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.Executor;

public class DesktopNavigator {

    private final Stage stage;
    private final AuthApi authApi;
    private final HealthApi healthApi;
    private final DashboardApi dashboardApi;
    private final OnboardingApi onboardingApi;
    private final CourseJobApi courseJobs;
    private final PracticeApi practice;
    private final ProgressApi progress;
    private final ProfileApi profile;
    private final Executor executor;
    private final String backendAddress;

    public DesktopNavigator(Stage stage, AuthApi authApi, HealthApi healthApi, DashboardApi dashboardApi, Executor executor,
            OnboardingApi onboardingApi, CourseJobApi courseJobs, PracticeApi practice, ProgressApi progress,
            ProfileApi profile, String backendAddress) {
        this.stage = stage;
        this.authApi = authApi;
        this.healthApi = healthApi;
        this.dashboardApi = dashboardApi;
        this.onboardingApi = onboardingApi;
        this.courseJobs = courseJobs;
        this.practice = practice;
        this.progress = progress;
        this.profile = profile;
        this.executor = executor;
        this.backendAddress = backendAddress;
    }

    public void showLogin(String message) {
        show("login-view.fxml", new LoginController(authApi, this, executor, message));
    }

    public void showRegistration() {
        show("register-view.fxml", new RegisterController(authApi, this, executor));
    }

    public void showAccount(AccountSummary account) {
        if (account.isAdministrator()) {
            show("account-view.fxml", new AccountController(authApi, this, executor, account));
        } else {
            showDashboard(account);
        }
    }

    public void showDashboard(AccountSummary account) {
        show("dashboard-view.fxml", new DashboardController(account, authApi, dashboardApi, this, executor));
    }

    public void showOnboarding(AccountSummary account) {
        show("onboarding-view.fxml", new OnboardingController(account, onboardingApi, courseJobs, this, executor));
    }

    public void showCourseJob(AccountSummary account, CourseJobStatus job) {
        show("course-waiting-view.fxml", new CourseWaitingController(account, courseJobs, this, executor, job));
    }

    public void showPractice(AccountSummary account, int courseId, Integer chapterId, boolean review) {
        show("practice-view.fxml",
                new PracticeController(account, practice, this, executor, courseId, chapterId, review));
    }

    public void showProgress(AccountSummary account) {
        show("progress-view.fxml", new ProgressController(account, progress, this, executor));
    }

    public void showProfile(AccountSummary account) {
        show("profile-view.fxml", new ProfileController(account, profile, this, executor));
    }

    public void showConnection() {
        show("connection-view.fxml",
                new ConnectionController(healthApi, executor, backendAddress, () -> showLogin("")));
    }

    private void show(String view, Object controller) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + view));
        loader.setControllerFactory(controllerType -> controller);
        try {
            Parent root = loader.load();
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                stage.getScene().setRoot(root);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("Could not open " + view, error);
        }
    }
}
