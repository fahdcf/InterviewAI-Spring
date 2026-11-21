package com.interviewai.desktop.feature.dashboard;

import com.interviewai.desktop.api.AccountSummary;
import com.interviewai.desktop.navigation.ScreenController;
import com.interviewai.desktop.api.ApiException;
import com.interviewai.desktop.api.AuthApi;
import com.interviewai.desktop.api.DashboardApi;
import com.interviewai.desktop.api.dto.ChapterSummary;
import com.interviewai.desktop.api.dto.CoursePair;
import com.interviewai.desktop.api.dto.CourseSummary;
import com.interviewai.desktop.api.dto.DashboardResponse;
import com.interviewai.desktop.navigation.DesktopNavigator;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class DashboardController extends ScreenController {

    private final AccountSummary account;
    private final AuthApi auth;
    private final DashboardApi dashboard;
    private final DesktopNavigator navigator;
    private final Executor executor;

    @FXML private BorderPane dashboardPane;
    @FXML private Label welcomeLabel;
    @FXML private Label xpLabel;
    @FXML private Label streakLabel;
    @FXML private Label courseProgress;
    @FXML private Label chapterName;
    @FXML private Label chapterDescription;
    @FXML private Label statusLabel;
    @FXML private ComboBox<CourseSummary> coursePicker;
    @FXML private ListView<ChapterSummary> chapterList;
    @FXML private Button saveButton;
    @FXML private Button reviewChapterButton;
    @FXML private Button reviewCourseButton;

    private DashboardResponse current;
    private boolean rendering;

    public DashboardController(AccountSummary account, AuthApi auth, DashboardApi dashboard,
            DesktopNavigator navigator, Executor executor) {
        this.account = account;
        this.auth = auth;
        this.dashboard = dashboard;
        this.navigator = navigator;
        this.executor = executor;
    }

    @FXML
    private void initialize() {
        welcomeLabel.setText("Welcome, " + account.getUsername());
        coursePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(CourseSummary course) {
                return course == null ? "" : courseLabel(course);
            }

            @Override
            public CourseSummary fromString(String label) {
                throw new UnsupportedOperationException("Choose an existing course.");
            }
        });
        chapterList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ChapterSummary chapter, boolean empty) {
                super.updateItem(chapter, empty);
                setText(empty || chapter == null ? null : chapter.getNumber() + ". " + chapter.getName());
            }
        });
        chapterList.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, chapter) -> showChapter(chapter));
        refresh();
    }

    @FXML
    private void refresh() {
        run("Loading courses…", dashboard::load, this::render);
    }

    @FXML
    private void openOnboarding() {
        navigator.showOnboarding(account);
    }

    @FXML
    private void openCourseJob() {
        navigator.showCourseJob(account, null);
    }

    @FXML
    private void openProgress() {
        navigator.showProgress(account);
    }

    @FXML
    private void openProfile() {
        navigator.showProfile(account);
    }

    @FXML
    private void selectCourse() {
        CourseSummary course = coursePicker.getValue();
        if (rendering || current == null || course == null
                || Objects.equals(course.getId(), current.getActiveCourseId())) {
            return;
        }
        run("Opening course…", () -> {
            dashboard.saveLocation(course.getId(), null);
            return dashboard.load();
        }, this::render);
    }

    @FXML
    private void openLesson() {
        ChapterSummary chapter = chapterList.getSelectionModel().getSelectedItem();
        if (current == null || current.getActiveCourseId() == null || chapter == null) {
            return;
        }
        int courseId = current.getActiveCourseId();
        run("Opening lesson…", () -> {
            dashboard.saveLocation(courseId, chapter.getId());
            return null;
        }, result -> navigator.showPractice(account, courseId, chapter.getId(), false));
    }

    @FXML
    private void reviewChapter() {
        ChapterSummary chapter = chapterList.getSelectionModel().getSelectedItem();
        if (current != null && current.getActiveCourseId() != null && chapter != null) {
            navigator.showPractice(account, current.getActiveCourseId(), chapter.getId(), true);
        }
    }

    @FXML
    private void reviewCourse() {
        if (current != null && current.getActiveCourseId() != null) {
            navigator.showPractice(account, current.getActiveCourseId(), null, true);
        }
    }

    @FXML
    private void logout() {
        run("Signing out…", () -> {
            auth.logout();
            return null;
        }, result -> navigator.showLogin("You signed out."));
    }

    private void render(DashboardResponse result) {
        current = result;
        rendering = true;
        coursePicker.getItems().setAll(result.getCourses());
        for (CourseSummary course : result.getCourses()) {
            if (Objects.equals(course.getId(), result.getActiveCourseId())) {
                coursePicker.getSelectionModel().select(course);
                break;
            }
        }
        chapterList.getItems().setAll(result.getChapters());
        for (ChapterSummary chapter : result.getChapters()) {
            if (Objects.equals(chapter.getId(), result.getActiveChapterId())) {
                chapterList.getSelectionModel().select(chapter);
                break;
            }
        }
        xpLabel.setText(String.format("%,d XP", result.getTotalXp()));
        streakLabel.setText(result.getStreak() + " day streak");
        long completed = result.getChapters().stream()
                .filter(chapter -> "COMPLETED".equals(chapter.getStatus())).count();
        courseProgress.setText(completed + " of " + result.getChapters().size() + " chapters completed");
        statusLabel.setText(result.getCourses().isEmpty() ? "Your courses will appear here after onboarding." : "");
        showChapter(chapterList.getSelectionModel().getSelectedItem());
        rendering = false;
    }

    private String courseLabel(CourseSummary course) {
        if (current != null) {
            for (CoursePair pair : current.getPairs()) {
                if (Objects.equals(course.getId(), pair.getTechnicalCourseId())) {
                    return "Technical · " + course.getTitle();
                }
                if (Objects.equals(course.getId(), pair.getSoftSkillsCourseId())) {
                    return "Soft skills · " + course.getTitle();
                }
            }
        }
        return course.getTitle() == null ? "Course " + course.getId() : course.getTitle();
    }

    private void showChapter(ChapterSummary chapter) {
        saveButton.setDisable(chapter == null);
        reviewChapterButton.setDisable(chapter == null);
        reviewCourseButton.setDisable(current == null || current.getActiveCourseId() == null);
        if (chapter == null) {
            chapterName.setText("Select a chapter");
            chapterDescription.setText("");
            return;
        }
        chapterName.setText(chapter.getName());
        String description = chapter.getDescription() == null ? "" : chapter.getDescription() + "\n\n";
        chapterDescription.setText(description + chapter.getAnsweredCount() + " of "
                + chapter.getQuestionCount() + " questions answered");
    }

    private <T> void run(String message, Callable<T> action, Consumer<T> onSuccess) {
        dashboardPane.setDisable(true);
        statusLabel.setText(message);
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
        };
        task.setOnSucceeded(event -> {
            dashboardPane.setDisable(false);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            dashboardPane.setDisable(false);
            if (task.getException() instanceof ApiException error && error.getStatus() == 401) {
                navigator.showLogin("Sign in again to continue.");
                return;
            }
            if (current != null) {
                render(current);
            }
            statusLabel.setText(task.getException() instanceof ApiException error
                    ? error.getMessage() : "Could not reach the backend. Try again.");
        });
        submit(task, executor);
    }
}
