package com.interviewai.desktop.feature.practice;

import com.interviewai.desktop.api.AccountSummary;
import com.interviewai.desktop.navigation.ScreenController;
import com.interviewai.desktop.api.ApiException;
import com.interviewai.desktop.api.PracticeApi;
import com.interviewai.desktop.api.dto.AttemptResult;
import com.interviewai.desktop.api.dto.PracticeQuestion;
import com.interviewai.desktop.navigation.DesktopNavigator;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class PracticeController extends ScreenController {

    private final AccountSummary account;
    private final PracticeApi api;
    private final DesktopNavigator navigator;
    private final Executor executor;
    private final int courseId;
    private final Integer chapterId;
    private final boolean review;
    private final PauseTransition poll = new PauseTransition(Duration.seconds(1));
    private final ToggleGroup options = new ToggleGroup();

    @FXML private Label title;
    @FXML private Label questionLabel;
    @FXML private VBox choices;
    @FXML private TextArea answer;
    @FXML private Label status;
    @FXML private Label feedback;
    @FXML private Button submitButton;
    @FXML private Button nextButton;
    @FXML private Button skipButton;
    @FXML private Button explainButton;
    @FXML private Label additionalExplanation;

    private List<PracticeQuestion> questions = List.of();
    private int index;
    private boolean closed;
    private boolean complete;
    private UUID requestId;
    private String submittedAnswer;
    private boolean submittedSkip;
    private Long pendingAttempt;
    private Long gradedAttempt;

    public PracticeController(AccountSummary account, PracticeApi api, DesktopNavigator navigator,
            Executor executor, int courseId, Integer chapterId, boolean review) {
        this.account = account;
        this.api = api;
        this.navigator = navigator;
        this.executor = executor;
        this.courseId = courseId;
        this.chapterId = chapterId;
        this.review = review;
    }

    @FXML
    private void initialize() {
        title.setText(review ? "Review incorrect answers" : "Lesson practice");
        poll.setOnFinished(event -> checkAttempt());
        run("Loading questions…", () -> api.questions(courseId, chapterId, review), result -> {
            questions = result;
            while (!review && index < questions.size()
                    && List.of("COMPLETED", "INCORRECT").contains(questions.get(index).status())) {
                index++;
            }
            if (index == questions.size() && !questions.isEmpty()) {
                index = 0;
            }
            showQuestion();
        });
    }

    @FXML
    private void submit() {
        if (pendingAttempt != null) {
            checkAttempt();
            return;
        }
        PracticeQuestion question = questions.get(index);
        String input = answer.getText().strip();
        if (isMultipleChoice(question)) {
            input = options.getSelectedToggle() == null ? "" : (String) options.getSelectedToggle().getUserData();
        }
        if (input.isBlank()) {
            status.setText("Enter or select an answer.");
            return;
        }
        sendAnswer(question, input, false);
    }

    @FXML
    private void skip() {
        if (review) {
            next();
        } else {
            sendAnswer(questions.get(index), "", true);
        }
    }

    @FXML
    private void next() {
        index++;
        showQuestion();
    }

    @Override
    public void dispose() {
        super.dispose();
        closed = true;
        poll.stop();
    }

    @FXML
    private void back() {
        closed = true;
        poll.stop();
        navigator.showDashboard(account);
    }

    @FXML
    private void explain() {
        int questionId = questions.get(index).id();
        long attemptId = gradedAttempt;
        String score = status.getText();
        run("Loading explanation…", () -> api.explain(questionId, attemptId), explanation -> {
            additionalExplanation.setText(explanation);
            status.setText(score);
            updateButtons(false);
        });
    }

    private void sendAnswer(PracticeQuestion question, String input, boolean skip) {
        if (requestId == null || !input.equals(submittedAnswer) || skip != submittedSkip) {
            requestId = UUID.randomUUID();
            submittedAnswer = input;
            submittedSkip = skip;
        }
        UUID request = requestId;
        run("Submitting answer…", () -> api.submit(question.id(), review ? "REVIEW" : "LESSON", input, skip, request),
                this::showResult);
    }

    private void checkAttempt() {
        long id = pendingAttempt;
        run("Waiting for feedback…", () -> api.attempt(id), this::showResult);
    }

    private void showResult(AttemptResult result) {
        if ("QUEUED".equals(result.status()) || "RUNNING".equals(result.status())) {
            pendingAttempt = result.id();
            status.setText("Waiting for short-answer feedback…");
            poll.playFromStart();
            updateButtons(true);
            return;
        }
        pendingAttempt = null;
        if ("FAILED".equals(result.status())) {
            requestId = null;
            status.setText(result.explanation());
            updateButtons(false);
            return;
        }
        complete = true;
        gradedAttempt = submittedSkip ? null : result.id();
        status.setText(submittedSkip ? "Question skipped."
                : result.score() + "/100" + (result.xpAwarded() > 0 ? "   +" + result.xpAwarded() + " XP" : ""));
        StringBuilder details = new StringBuilder();
        String explanation = isMultipleChoice(questions.get(index)) || submittedSkip ? result.explanation() : null;
        for (String section : new String[]{result.strengths(), result.improvements(), explanation}) {
            if (section != null && !section.isBlank()) {
                if (!details.isEmpty()) {
                    details.append("\n\n");
                }
                details.append(section);
            }
        }
        feedback.setText(details.toString());
        updateButtons(false);
    }

    private void showQuestion() {
        complete = false;
        requestId = null;
        pendingAttempt = null;
        gradedAttempt = null;
        additionalExplanation.setText("");
        answer.clear();
        feedback.setText("");
        choices.getChildren().clear();
        options.getToggles().clear();
        status.setText("");
        if (index >= questions.size()) {
            questionLabel.setText(review ? "No more questions to review." : "No more questions in this lesson.");
            setAnswerVisible(false);
            updateButtons(false);
            return;
        }
        PracticeQuestion question = questions.get(index);
        questionLabel.setText((index + 1) + " / " + questions.size() + "   " + question.text());
        setAnswerVisible(!isMultipleChoice(question));
        for (String choice : question.choices()) {
            RadioButton option = new RadioButton(choice);
            option.getStyleClass().add("subtitle-dark");
            option.setWrapText(true);
            option.setUserData(choice);
            option.setToggleGroup(options);
            choices.getChildren().add(option);
        }
        updateButtons(false);
        AttemptResult saved = question.savedAttempt();
        String mode = review ? "REVIEW" : "LESSON";
        if (saved != null && mode.equals(saved.mode()) && !"GRADED".equals(saved.status())) {
            submittedAnswer = saved.answer();
            submittedSkip = false;
            answer.setText(saved.answer() == null ? "" : saved.answer());
            for (javafx.scene.control.Toggle option : options.getToggles()) {
                if (java.util.Objects.equals(option.getUserData(), saved.answer())) {
                    option.setSelected(true);
                }
            }
            showResult(saved);
        }
    }

    private void setAnswerVisible(boolean visible) {
        answer.setVisible(visible);
        answer.setManaged(visible);
    }

    private boolean isMultipleChoice(PracticeQuestion question) {
        return "MULTIPLE_CHOICE".equals(question.type());
    }

    private void updateButtons(boolean busy) {
        boolean hasQuestion = index < questions.size();
        boolean pending = pendingAttempt != null;
        submitButton.setDisable(busy || !hasQuestion || complete);
        submitButton.setText(pending ? "Check feedback" : "Submit answer");
        skipButton.setDisable(busy || pending || !hasQuestion || complete);
        nextButton.setDisable(busy || !complete || !hasQuestion);
        explainButton.setVisible(gradedAttempt != null && additionalExplanation.getText().isEmpty());
        explainButton.setManaged(explainButton.isVisible());
        explainButton.setDisable(busy);
        answer.setDisable(busy || pending || complete);
        choices.setDisable(busy || pending || complete);
    }

    private <T> void run(String message, Callable<T> action, Consumer<T> success) {
        status.setText(message);
        updateButtons(true);
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
        };
        task.setOnSucceeded(event -> {
            if (!closed) {
                success.accept(task.getValue());
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
            if (task.getException() instanceof ApiException error && error.getStatus() == 404) {
                navigator.showDashboard(account);
                return;
            }
            status.setText(task.getException() instanceof ApiException error
                    ? error.getMessage() : "Could not reach the backend. Try again.");
            updateButtons(false);
        });
        submit(task, executor);
    }
}
