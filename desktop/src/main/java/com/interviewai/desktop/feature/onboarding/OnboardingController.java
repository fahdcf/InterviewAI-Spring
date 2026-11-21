package com.interviewai.desktop.feature.onboarding;

import com.interviewai.desktop.api.AccountSummary;
import com.interviewai.desktop.navigation.ScreenController;
import com.interviewai.desktop.api.ApiException;
import com.interviewai.desktop.api.CourseJobApi;
import com.interviewai.desktop.api.OnboardingApi;
import com.interviewai.desktop.api.dto.OnboardingPreferences;
import com.interviewai.desktop.navigation.DesktopNavigator;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class OnboardingController extends ScreenController {

    private final AccountSummary account;
    private final OnboardingApi api;
    private final CourseJobApi courseJobs;
    private final DesktopNavigator navigator;
    private final Executor executor;

    @FXML private VBox form;
    @FXML private ComboBox<String> interviewType;
    @FXML private ComboBox<String> language;
    @FXML private ComboBox<String> timeline;
    @FXML private TextField context;
    @FXML private TextArea description;
    @FXML private CheckBox removeCv;
    @FXML private Label cvStatus;
    @FXML private Label statusLabel;

    private Path cv;
    private UUID generationRequest;

    public OnboardingController(AccountSummary account, OnboardingApi api, CourseJobApi courseJobs,
            DesktopNavigator navigator,
            Executor executor) {
        this.account = account;
        this.api = api;
        this.courseJobs = courseJobs;
        this.navigator = navigator;
        this.executor = executor;
    }

    @FXML
    private void initialize() {
        configureOptions(interviewType, "JOB", "VISA", "INTERNSHIP", "UNIVERSITY");
        configureOptions(language, "ENGLISH", "FRENCH", "ARABIC", "SPANISH");
        configureOptions(timeline, "TOMORROW", "THIS_WEEK", "LATER");
        run("Loading preferences…", api::load, preferences -> {
            if (preferences != null) {
                interviewType.setValue(preferences.getInterviewType());
                language.setValue(preferences.getLanguage());
                timeline.setValue(preferences.getTimeline());
                context.setText(preferences.getContext());
                description.setText(preferences.getDescription());
            }
            showSavedCv(preferences != null && preferences.isHasCv());
            statusLabel.setText("");
        });
    }

    private void configureOptions(ComboBox<String> picker, String... options) {
        picker.getItems().setAll(options);
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                if (value == null) {
                    return "";
                }
                String lower = value.toLowerCase(Locale.ROOT).replace('_', ' ');
                return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
            }

            @Override
            public String fromString(String label) {
                throw new UnsupportedOperationException("Choose a listed option.");
            }
        });
        picker.getSelectionModel().selectFirst();
    }

    @FXML
    private void chooseCv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose your CV");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Documents (PDF, DOCX, DOC)", "*.pdf", "*.docx", "*.doc"));
        File chosen = chooser.showOpenDialog(form.getScene().getWindow());
        if (chosen != null) {
            cv = chosen.toPath();
            removeCv.setSelected(false);
            cvStatus.setText(chosen.getName() + " selected");
        }
    }

    @FXML
    private void save() {
        savePreferences(false);
    }

    @FXML
    private void generate() {
        savePreferences(true);
    }

    private void savePreferences(boolean generate) {
        if (context.getText().isBlank()) {
            statusLabel.setText("Enter the role, programme, or other context for the interview.");
            return;
        }
        OnboardingPreferences preferences = new OnboardingPreferences();
        preferences.setInterviewType(interviewType.getValue());
        preferences.setLanguage(language.getValue());
        preferences.setTimeline(timeline.getValue());
        preferences.setContext(context.getText().strip());
        preferences.setDescription(description.getText());
        preferences.setRemoveCv(removeCv.isSelected());
        Path file = preferences.isRemoveCv() ? null : cv;

        if (generate) {
            if (generationRequest == null) {
                generationRequest = UUID.randomUUID();
            }
            UUID request = generationRequest;
            run("Starting course generation…", () -> {
                api.save(preferences, file);
                return courseJobs.start(request);
            }, job -> navigator.showCourseJob(account, job));
            return;
        }
        run("Saving preferences…", () -> api.save(preferences, file), saved -> {
            cv = null;
            showSavedCv(saved.isHasCv());
            statusLabel.setText("Your preferences are saved.");
        });
    }

    @FXML
    private void back() {
        navigator.showDashboard(account);
    }

    private void showSavedCv(boolean hasCv) {
        removeCv.setSelected(false);
        removeCv.setVisible(hasCv);
        removeCv.setManaged(hasCv);
        cvStatus.setText(hasCv ? "CV text is saved." : "No CV selected.");
    }

    private <T> void run(String message, Callable<T> action, Consumer<T> success) {
        form.setDisable(true);
        statusLabel.setText(message);
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
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
            statusLabel.setText(task.getException() instanceof ApiException error
                    ? error.getMessage() : "Could not complete the request. Check your connection and file.");
        });
        submit(task, executor);
    }
}
