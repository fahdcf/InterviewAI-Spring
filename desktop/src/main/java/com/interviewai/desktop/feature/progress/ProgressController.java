package com.interviewai.desktop.feature.progress;

import com.interviewai.desktop.api.AccountSummary;
import com.interviewai.desktop.api.ApiException;
import com.interviewai.desktop.api.ProgressApi;
import com.interviewai.desktop.api.ProgressApi.Goal;
import com.interviewai.desktop.api.ProgressApi.Page;
import com.interviewai.desktop.api.ProgressApi.Quest;
import com.interviewai.desktop.navigation.DesktopNavigator;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class ProgressController {

    private final AccountSummary account;
    private final ProgressApi api;
    private final DesktopNavigator navigator;
    private final Executor executor;
    private final Map<String, UUID> requestIds = new HashMap<>();

    @FXML private VBox content;
    @FXML private Label summaryLabel;
    @FXML private Label statusLabel;
    @FXML private BarChart<String, Number> xpChart;
    @FXML private ListView<Goal> goals;
    @FXML private ListView<String> rankings;
    @FXML private TextField goalName;
    @FXML private TextField goalTarget;
    @FXML private ComboBox<String> goalType;
    @FXML private Label dailyLabel;
    @FXML private Label monthlyLabel;
    @FXML private Button dailyClaim;
    @FXML private Button monthlyClaim;

    public ProgressController(AccountSummary account, ProgressApi api, DesktopNavigator navigator, Executor executor) {
        this.account = account;
        this.api = api;
        this.navigator = navigator;
        this.executor = executor;
    }

    @FXML
    private void initialize() {
        goalType.getItems().setAll("QUESTIONS", "XP", "CHAPTERS", "TIME", "COURSES", "STREAK");
        goalType.getSelectionModel().selectFirst();
        goals.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Goal goal, boolean empty) {
                super.updateItem(goal, empty);
                setText(empty || goal == null ? null : goal.name() + "  ·  " + goal.type()
                        + "  ·  " + goal.currentValue() + " / " + goal.target());
            }
        });
        refresh();
    }

    @FXML
    private void refresh() {
        run("Loading progress…", api::load, this::render);
    }

    @FXML
    private void addGoal() {
        String name = goalName.getText().strip();
        String type = goalType.getValue();
        int target;
        try {
            target = Integer.parseInt(goalTarget.getText().strip());
        } catch (NumberFormatException invalid) {
            statusLabel.setText("Enter a whole number as the goal target.");
            return;
        }
        if (name.isEmpty() || target < 1) {
            statusLabel.setText("Enter a goal name and a target greater than zero.");
            return;
        }
        run("Adding goal…", () -> {
            api.addGoal(name, type, target);
            return null;
        }, result -> {
            goalName.clear();
            goalTarget.clear();
            refresh();
        });
    }

    @FXML
    private void removeGoal() {
        Goal goal = goals.getSelectionModel().getSelectedItem();
        if (goal == null) {
            statusLabel.setText("Select a goal to remove.");
            return;
        }
        run("Removing goal…", () -> {
            api.removeGoal(goal.id());
            return null;
        }, result -> refresh());
    }

    @FXML
    private void claimDaily() {
        claim("daily");
    }

    @FXML
    private void claimMonthly() {
        claim("monthly");
    }

    private void claim(String type) {
        UUID id = requestIds.computeIfAbsent(type, key -> UUID.randomUUID());
        run("Claiming reward…", () -> {
            api.claim(type, id);
            return null;
        }, result -> {
            requestIds.remove(type);
            refresh();
        });
    }

    @FXML
    private void back() {
        navigator.showDashboard(account);
    }

    private void render(Page page) {
        ProgressApi.Summary summary = page.summary();
        String accuracy = summary.answered() == 0 ? "—"
                : Math.round(summary.correct() * 100.0 / summary.answered()) + "%";
        summaryLabel.setText(String.format("%,d XP  ·  %d day streak  ·  %s accuracy%n"
                        + "%d chapters completed  ·  %d courses  ·  %d estimated minutes",
                summary.totalXp(), summary.streak(), accuracy, summary.completedChapters(),
                summary.totalCourses(), summary.estimatedMinutes()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (ProgressApi.DailyXp day : summary.recentXp()) {
            series.getData().add(new XYChart.Data<>(day.date().substring(5), day.xp()));
        }
        xpChart.getData().clear();
        xpChart.getData().add(series);
        goals.getItems().setAll(page.goals());
        rankings.getItems().clear();
        for (ProgressApi.Ranking entry : page.leaderboard()) {
            rankings.getItems().add(String.format("%s  ·  %,d XP", entry.username(), entry.xp()));
        }
        for (Quest quest : page.quests()) {
            if ("daily".equals(quest.type())) {
                showQuest(quest, dailyLabel, dailyClaim);
            } else {
                showQuest(quest, monthlyLabel, monthlyClaim);
            }
        }
        statusLabel.setText("");
    }

    private void showQuest(Quest quest, Label label, Button button) {
        String reset = OffsetDateTime.parse(quest.resetsAt())
                .format(DateTimeFormatter.ofPattern("d MMM, HH:mm xxx"));
        label.setText(String.format("%d / %d  ·  %,d XP%s%nNext reset %s", quest.current(), quest.target(),
                quest.reward(), quest.claimed() ? "  ·  Claimed" : "", reset));
        button.setDisable(!quest.available());
    }

    private <T> void run(String message, Callable<T> work, Consumer<T> success) {
        content.setDisable(true);
        statusLabel.setText(message);
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(event -> {
            content.setDisable(false);
            success.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            content.setDisable(false);
            if (task.getException() instanceof ApiException error && error.getStatus() == 401) {
                navigator.showLogin("Sign in again to continue.");
                return;
            }
            statusLabel.setText(task.getException() instanceof ApiException error
                    ? error.getMessage() : "Could not load or save progress. Try again.");
        });
        executor.execute(task);
    }
}
