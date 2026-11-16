package com.interviewai.desktop.feature.admin;

import com.interviewai.desktop.api.AccountSummary;
import com.interviewai.desktop.api.AdminApi;
import com.interviewai.desktop.api.AdminApi.Course;
import com.interviewai.desktop.api.AdminApi.User;
import com.interviewai.desktop.api.ApiException;
import com.interviewai.desktop.api.AuthApi;
import com.interviewai.desktop.navigation.DesktopNavigator;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class AdminController {

    private final AccountSummary account;
    private final AdminApi api;
    private final AuthApi auth;
    private final DesktopNavigator navigator;
    private final Executor executor;

    @FXML private BorderPane page;
    @FXML private Label welcome;
    @FXML private Label status;
    @FXML private TabPane tabs;
    @FXML private Tab overviewTab;
    @FXML private Tab userTab;
    @FXML private Tab courseTab;
    @FXML private Label overview;
    @FXML private LineChart<String, Number> trends;
    @FXML private PieChart interviewTypes;
    @FXML private PieChart languages;
    @FXML private TextField userSearch;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ListView<User> users;
    @FXML private Label userDetails;
    @FXML private TextField userEmail;
    @FXML private ComboBox<String> userRole;
    @FXML private Button userPrevious;
    @FXML private Button userNext;
    @FXML private TextField courseSearch;
    @FXML private ComboBox<String> courseFilter;
    @FXML private ListView<Course> courses;
    @FXML private Label courseDetails;
    @FXML private Button coursePrevious;
    @FXML private Button courseNext;

    private int userPage;
    private int coursePage;

    public AdminController(AccountSummary account, AdminApi api, AuthApi auth,
            DesktopNavigator navigator, Executor executor) {
        this.account = account;
        this.api = api;
        this.auth = auth;
        this.navigator = navigator;
        this.executor = executor;
    }

    @FXML
    private void initialize() {
        welcome.setText("Administration · " + account.getUsername());
        if (!account.canManageUsers()) {
            tabs.getTabs().remove(userTab);
        }
        if (!account.canManageCourses()) {
            tabs.getTabs().remove(courseTab);
        }
        if (!account.canViewAnalytics()) {
            tabs.getTabs().remove(overviewTab);
        }
        statusFilter.getItems().setAll("All", "ACTIVE", "SUSPENDED");
        statusFilter.getSelectionModel().selectFirst();
        roleFilter.getItems().setAll("All");
        roleFilter.getSelectionModel().selectFirst();
        courseFilter.getItems().setAll("All", "ACTIVE", "ARCHIVED", "COMPLETED");
        courseFilter.getSelectionModel().selectFirst();
        users.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.username() + (user.active() ? "" : " (suspended)"));
            }
        });
        courses.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);
                setText(empty || course == null ? null : course.title() + "  ·  " + course.status());
            }
        });
        users.getSelectionModel().selectedItemProperty().addListener((observable, old, user) -> showUser(user));
        courses.getSelectionModel().selectedItemProperty().addListener((observable, old, course) -> showCourse(course));
        tabs.getSelectionModel().selectedItemProperty().addListener((observable, old, tab) -> refresh());
        refresh();
    }

    @FXML
    private void refresh() {
        Tab tab = tabs.getSelectionModel().getSelectedItem();
        if (tab == overviewTab) {
            run("Loading overview…", api::overview, this::showOverview);
        } else if (tab == userTab) {
            loadUsers(userPage);
        } else if (tab == courseTab) {
            loadCourses(coursePage);
        }
    }

    @FXML
    private void searchUsers() { loadUsers(0); }
    @FXML
    private void previousUsers() { loadUsers(userPage - 1); }
    @FXML
    private void nextUsers() { loadUsers(userPage + 1); }

    private void loadUsers(int requestedPage) {
        String search = userSearch.getText();
        String filter = value(statusFilter);
        String role = value(roleFilter);
        run("Loading users…", () -> new UsersAndRoles(api.users(search, filter, role, requestedPage), api.roles()), result -> {
            userPage = result.users().page();
            users.getItems().setAll(result.users().items());
            roleFilter.getItems().setAll("All");
            roleFilter.getItems().addAll(result.roles());
            roleFilter.setValue(role.isEmpty() ? "All" : role);
            userRole.getItems().setAll(account.isSuperAdmin() ? result.roles() : List.of("CANDIDATE"));
            userPrevious.setDisable(userPage == 0);
            userNext.setDisable((userPage + 1) * 20 >= result.users().total());
            status.setText(result.users().total() + " users matching the search.");
        });
    }

    private void showUser(User user) {
        if (user == null) {
            userDetails.setText("Select a user.");
            userEmail.clear();
            userRole.setValue(null);
            return;
        }
        userDetails.setText(String.format("%s · %s%n%,d XP · %d courses%nJoined %s%nRoles: %s",
                user.username(), user.active() ? "Active" : "Suspended", user.xp(),
                user.courseCount(), user.joined(), String.join(", ", user.roles())));
        userEmail.setText(user.email() == null ? "" : user.email());
        userRole.setValue(user.roles().size() == 1 ? user.roles().getFirst() : null);
    }

    @FXML
    private void saveUser() {
        User user = selectedUser();
        if (user == null) {
            return;
        }
        Map<String, Object> change = new HashMap<>();
        change.put("email", userEmail.getText().strip());
        String role = userRole.getValue();
        if (role != null && !user.roles().equals(List.of(role))) {
            change.put("role", role);
        }
        run("Saving user…", () -> {
            api.updateUser(user.id(), change);
            return null;
        }, result -> refresh());
    }

    @FXML
    private void toggleUser() {
        User user = selectedUser();
        if (user == null || !confirm((user.active() ? "Suspend " : "Activate ") + user.username() + "?")) {
            return;
        }
        run("Updating user access…", () -> {
            api.updateUser(user.id(), Map.of("active", !user.active()));
            return null;
        }, result -> refresh());
    }

    @FXML
    private void resetXp() { reset(false); }
    @FXML
    private void resetProgress() { reset(true); }

    private void reset(boolean all) {
        User user = selectedUser();
        if (user == null || !confirm("Reset " + (all ? "lesson progress and XP" : "XP") + " for " + user.username() + "?")) {
            return;
        }
        run("Resetting progress…", () -> {
            api.reset(user.id(), all);
            return null;
        }, result -> refresh());
    }

    private User selectedUser() {
        User user = users.getSelectionModel().getSelectedItem();
        if (user == null) {
            status.setText("Select a user.");
        }
        return user;
    }

    @FXML
    private void searchCourses() { loadCourses(0); }
    @FXML
    private void previousCourses() { loadCourses(coursePage - 1); }
    @FXML
    private void nextCourses() { loadCourses(coursePage + 1); }

    private void loadCourses(int requestedPage) {
        String search = courseSearch.getText();
        String filter = value(courseFilter);
        run("Loading courses…", () -> api.courses(search, filter, requestedPage), result -> {
            coursePage = result.page();
            courses.getItems().setAll(result.items());
            coursePrevious.setDisable(coursePage == 0);
            courseNext.setDisable((coursePage + 1) * 20 >= result.total());
            status.setText(result.total() + " courses matching the search.");
        });
    }

    private void showCourse(Course course) {
        if (course == null) {
            courseDetails.setText("Select a course.");
            return;
        }
        courseDetails.setText(String.format("%s%nOwner: %s%nStatus: %s%n%d of %d chapters completed · %d questions%nCreated %s",
                course.title(), course.owner(), course.status(), course.completedChapters(), course.chapters(),
                course.questions(), course.created()));
    }

    @FXML
    private void archiveCourse() {
        Course course = selectedCourse();
        if (course == null) {
            return;
        }
        run("Updating course status…", () -> {
            api.archiveCourse(course.id(), !"ARCHIVED".equals(course.status()));
            return null;
        }, result -> refresh());
    }

    @FXML
    private void deleteCourse() {
        Course course = selectedCourse();
        if (course == null || !confirm("Permanently delete " + course.title() + "?")) {
            return;
        }
        run("Deleting course…", () -> {
            api.deleteCourse(course.id());
            return null;
        }, result -> refresh());
    }

    private Course selectedCourse() {
        Course course = courses.getSelectionModel().getSelectedItem();
        if (course == null) {
            status.setText("Select a course.");
        }
        return course;
    }

    private void showOverview(AdminApi.Overview summary) {
        overview.setText(String.format(
                "%d users (%d active)   ·   %d courses (%d completed)%n"
                        + "%d chapters completed   ·   %d questions%nSign-ups — today %d, last 7 days %d, this month %d",
                summary.users(), summary.activeUsers(), summary.courses(), summary.completedCourses(),
                summary.completedChapters(), summary.questions(), summary.dailySignups(),
                summary.weeklySignups(), summary.monthlySignups()));
        trends.getData().setAll(List.of(series("Sign-ups", summary.signupTrend()), series("Active learners", summary.usageTrend())));
        showPie(interviewTypes, summary.interviewTypes());
        showPie(languages, summary.languages());
        status.setText("");
    }

    private XYChart.Series<String, Number> series(String name, Map<String, Long> values) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);
        values.forEach((date, count) -> series.getData().add(new XYChart.Data<>(date, count)));
        return series;
    }

    private void showPie(PieChart chart, Map<String, Long> values) {
        chart.getData().clear();
        values.forEach((name, count) -> chart.getData().add(new PieChart.Data(name, count)));
    }

    @FXML
    private void profile() { navigator.showProfile(account); }

    @FXML
    private void logout() {
        run("Signing out…", () -> {
            auth.logout();
            return null;
        }, result -> navigator.showLogin("You signed out."));
    }

    private boolean confirm(String message) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.CANCEL, ButtonType.OK);
        return dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private String value(ComboBox<String> filter) {
        String selected = filter.getValue();
        return selected == null || "All".equals(selected) ? "" : selected;
    }

    private <T> void run(String message, Callable<T> work, Consumer<T> success) {
        page.setDisable(true);
        status.setText(message);
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(event -> {
            page.setDisable(false);
            success.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            page.setDisable(false);
            if (task.getException() instanceof ApiException error && error.getStatus() == 401) {
                navigator.showLogin("Sign in again to continue.");
                return;
            }
            status.setText(task.getException() instanceof ApiException error
                    ? error.getMessage() : "Could not load or save administrative data.");
        });
        executor.execute(task);
    }

    private record UsersAndRoles(AdminApi.UserPage users, List<String> roles) {
    }
}
