package com.interviewai.desktop;

import com.interviewai.desktop.api.AdminApi;
import com.interviewai.desktop.api.ApiClient;
import com.interviewai.desktop.api.AuthApi;
import com.interviewai.desktop.api.CourseJobApi;
import com.interviewai.desktop.api.DashboardApi;
import com.interviewai.desktop.api.HealthApi;
import com.interviewai.desktop.api.OnboardingApi;
import com.interviewai.desktop.api.PracticeApi;
import com.interviewai.desktop.api.ProfileApi;
import com.interviewai.desktop.api.ProgressApi;
import com.interviewai.desktop.navigation.DesktopNavigator;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.net.CookieManager;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "interviewai.ui.tests", matches = "true")
class DesktopViewsTest {

    private Stage stage;

    @BeforeAll
    static void startJavaFx() {
        Platform.startup(() -> Platform.setImplicitExit(false));
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void candidateAndAdministratorCanOpenTheirScreens() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1", exchange -> {
            try (exchange) {
                String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String response = responseBody(exchange.getRequestURI().getPath(), request);
                if (response == null) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }
                byte[] body = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
        });
        server.start();

        URI baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        try (HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
                ExecutorService executor = Executors.newSingleThreadExecutor()) {
            try {
                onFx(() -> {
                    ApiClient api = new ApiClient(baseUrl, http);
                    stage = new Stage();
                    stage.setOpacity(0);
                    DesktopNavigator navigator = new DesktopNavigator(stage, new AuthApi(api),
                            new HealthApi(api), new DashboardApi(api), executor, new OnboardingApi(api),
                            new CourseJobApi(api), new PracticeApi(api), new ProgressApi(api), new ProfileApi(api),
                            new AdminApi(api), baseUrl.toString());
                    navigator.showLogin("");
                    stage.show();
                    return null;
                });

                press("Create account");
                assertNotNull(onFx(() -> node("#emailField")));
                press("Back to sign-in");
                signIn("candidate");
                waitForText("#xpLabel", "25 XP");

                press("Open lesson");
                waitForText("#questionLabel", "Synthetic question");
                press("Skip");
                waitForText("#status", "Question skipped.");
                press("Back to dashboard");
                waitForText("#xpLabel", "25 XP");

                press("Open lesson");
                waitForText("#questionLabel", "Synthetic question");
                onFx(() -> {
                    ((RadioButton) node("#choices").lookup(".radio-button")).fire();
                    return null;
                });
                press("Submit answer");
                waitForText("#status", "0/100");
                press("Explain answer");
                waitForText("#additionalExplanation", "A sample explanation.");
                press("Back to dashboard");
                waitForText("#xpLabel", "25 XP");

                press("Review this course");
                waitForText("#questionLabel", "No more questions to review.");
                press("Back to dashboard");
                waitForText("#xpLabel", "25 XP");
                press("Interview preferences");
                assertNotNull(onFx(() -> node("#interviewType")));
                waitForButton("Back to dashboard");
                press("Back to dashboard");
                waitForText("#xpLabel", "25 XP");

                press("Generation status");
                waitForText("#statusLabel", "No courses are being generated.");
                press("Back to dashboard");
                waitForText("#xpLabel", "25 XP");
                press("Progress and quests");
                waitForText("#summaryLabel", "25 XP");
                assertNotNull(onFx(() -> node("#xpChart")));
                press("Back to dashboard");
                waitForText("#xpLabel", "25 XP");
                press("Profile");
                assertNotNull(onFx(() -> node("#username")));
                press("Back");
                waitForText("#xpLabel", "25 XP");
                press("Sign out");
                waitForText("#statusLabel", "You signed out.");

                signIn("administrator");
                waitForText("#welcome", "Administration");
                waitForText("#overview", "3 users");
                assertNotNull(onFx(() -> node("#tabs")));
            } finally {
                onFx(() -> {
                    if (stage != null) {
                        stage.close();
                    }
                    return null;
                });
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "interviewai.journey", matches = "true")
    void candidateCompletesGeneratedChapterAgainstPackagedBackend() throws Exception {
        assertNotNull(System.getenv("INTERVIEWAI_API_URL"), "Set the disposable backend's origin.");
        DesktopApplication app = onFx(() -> {
            DesktopApplication application = new DesktopApplication();
            stage = new Stage();
            stage.setOpacity(0);
            application.start(stage);
            return application;
        });
        try {
            String username = "desktop-" + UUID.randomUUID().toString().substring(0, 12);
            press("Create account");
            fill("#usernameField", username);
            fill("#passwordField", "synthetic-password");
            press("Create account");
            waitForText("#statusLabel", "Account created");
            signIn(username);
            waitForText("#xpLabel", "0 XP");

            press("Interview preferences");
            waitForButton("Generate courses");
            fill("#context", "Software developer");
            press("Generate courses");
            waitFor(() -> node("#coursePicker") instanceof ComboBox<?> picker && picker.getItems().size() == 2,
                    "both generated courses");

            press("Open lesson");
            for (int number = 1; number <= 40; number++) {
                waitForText("#questionLabel", number + " / 40   Question " + number + "?");
                if (number <= 20) {
                    chooseAnswer(number == 1 ? "Second" : "Example");
                } else {
                    fill("#answer", "mostly correct");
                }
                press("Submit answer");
                waitForText("#status", number == 1 ? "0/100" : "+10 XP");
                press("Next");
            }
            waitForText("#questionLabel", "No more questions in this lesson.");
            press("Back to dashboard");
            waitForText("#xpLabel", "390 XP");
            press("Review this chapter");
            waitForText("#questionLabel", "1 / 1   Question 1?");
            chooseAnswer("Example");
            press("Submit answer");
            waitForText("#status", "100/100");
            press("Explain answer");
            waitForText("#additionalExplanation", "Sample explanation.");
            press("Back to dashboard");
            waitForText("#xpLabel", "390 XP");

            press("Progress and quests");
            waitForText("#summaryLabel", "1 chapters completed");
            waitForButton("Claim daily reward");
            press("Claim daily reward");
            String rewardedXp = String.format("%,d XP", 1290);
            waitForText("#summaryLabel", rewardedXp);
            assertTrue(onFx(() -> button("Claim daily reward").isDisabled()));

            fill("#goalName", "Practice daily");
            fill("#goalTarget", "10");
            press("Add goal");
            waitFor(() -> ((ListView<?>) node("#goals")).getItems().size() == 1, "saved goal");
            onFx(() -> {
                ((ListView<?>) node("#goals")).getSelectionModel().selectFirst();
                return null;
            });
            fill("#goalName", "Practice weekly");
            press("Update selected");
            waitFor(() -> ((ListView<?>) node("#goals")).getItems().getFirst().toString().contains("Practice weekly"),
                    "updated goal");

            press("Back to dashboard");
            waitForText("#xpLabel", rewardedXp);
            press("Sign out");
            waitForText("#statusLabel", "You signed out.");
            signIn(username);
            waitForText("#xpLabel", rewardedXp);
            waitForText("#chapterName", "Chapter 1");
            press("Review this chapter");
            waitForText("#questionLabel", "No more questions to review.");
        } finally {
            onFx(() -> {
                stage.close();
                app.stop();
                return null;
            });
        }
    }

    private void chooseAnswer(String answer) throws Exception {
        onFx(() -> {
            RadioButton option = node("#choices").lookupAll(".radio-button").stream()
                    .map(RadioButton.class::cast)
                    .filter(button -> answer.equals(button.getText()))
                    .findFirst().orElseThrow(() -> new AssertionError("Choice not found: " + answer));
            option.fire();
            return null;
        });
    }

    private void fill(String selector, String value) throws Exception {
        onFx(() -> {
            ((TextInputControl) node(selector)).setText(value);
            return null;
        });
    }

    private static String responseBody(String path, String request) throws IOException {
        return switch (path) {
            case "/api/v1/auth/csrf" -> "{\"headerName\":\"X-CSRF-TOKEN\",\"token\":\"test\"}";
            case "/api/v1/auth/login" -> request.contains("administrator")
                    ? """
                      {"id":2,"username":"Administrator","administrator":true,"roles":["SUPER_ADMIN"],"permissions":[]}
                      """
                    : """
                      {"id":1,"username":"Candidate","administrator":false,"roles":["CANDIDATE"],"permissions":[]}
                      """;
            case "/api/v1/auth/logout", "/api/v1/me/onboarding", "/api/v1/course-jobs/latest",
                    "/api/v1/me/navigation" -> null;
            case "/api/v1/dashboard" -> """
                    {"courses":[{"id":1,"title":"Synthetic course","status":"ACTIVE"}],"pairs":[],
                     "activeCourseId":1,"activeChapterId":2,"chapters":[{"id":2,"number":1,"name":"First chapter",
                     "description":"Chapter description","status":"NOT_STARTED","questionCount":1,"answeredCount":0}],
                     "totalXp":25,"streak":1}
                    """;
            case "/api/v1/chapters/2/questions" -> """
                    [{"id":1,"chapterId":2,"text":"Synthetic question","type":"MULTIPLE_CHOICE",
                      "status":"NOT_STARTED","choices":["One","Two"]}]
                    """;
            case "/api/v1/questions/1/attempts" -> """
                    {"id":1,"status":"GRADED","score":0,"explanation":"Expected answer: One","xpAwarded":0}
                    """;
            case "/api/v1/questions/1/explanations" -> """
                    {"explanation":"A sample explanation."}
                    """;
            case "/api/v1/courses/1/review", "/api/v1/goals", "/api/v1/quests", "/api/v1/leaderboard" -> "[]";
            case "/api/v1/progress" -> """
                    {"totalXp":25,"streak":1,"answered":1,"correct":1,"completedChapters":0,"totalCourses":1,
                     "estimatedMinutes":17,"recentXp":[{"date":"2026-11-16","xp":25}]}
                    """;
            case "/api/v1/admin/overview" -> """
                    {"users":3,"activeUsers":2,"dailySignups":1,"weeklySignups":2,"monthlySignups":3,
                     "courses":1,"completedCourses":0,"completedChapters":0,"questions":1,
                     "interviewTypes":{"JOB":1},"languages":{"ENGLISH":1},
                     "signupTrend":{"2026-11-16":1},"usageTrend":{"2026-11-16":1}}
                    """;
            default -> throw new IOException("Unexpected smoke fixture path: " + path);
        };
    }

    private void signIn(String username) throws Exception {
        fill("#usernameField", username);
        fill("#passwordField", "synthetic-password");
        press("Sign in");
    }

    private void press(String label) throws Exception {
        onFx(() -> {
            Button button = button(label);
            assertTrue(!button.isDisabled(), "Button disabled: " + label);
            button.fire();
            return null;
        });
    }

    private void waitForButton(String label) throws Exception {
        waitFor(() -> !button(label).isDisabled(), label);
    }

    private void waitForText(String selector, String text) throws Exception {
        waitFor(() -> {
            Node match = node(selector);
            return match instanceof Label label && label.getText().contains(text);
        }, text);
    }

    private void waitFor(Callable<Boolean> condition, String expected) throws Exception {
        for (int attempt = 0; attempt < 150; attempt++) {
            if (onFx(condition)) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("The desktop did not display: " + expected);
    }

    private Button button(String text) {
        stage.getScene().getRoot().applyCss();
        return stage.getScene().getRoot().lookupAll(".button").stream()
                .filter(node -> node instanceof Button button && text.equals(button.getText()))
                .map(Button.class::cast)
                .findFirst().orElseThrow(() -> new AssertionError("Button not found: " + text));
    }

    private Node node(String selector) {
        stage.getScene().getRoot().applyCss();
        return stage.getScene().lookup(selector);
    }

    private <T> T onFx(Callable<T> operation) throws Exception {
        FutureTask<T> task = new FutureTask<>(operation);
        Platform.runLater(task);
        return task.get(6, TimeUnit.SECONDS);
    }
}
