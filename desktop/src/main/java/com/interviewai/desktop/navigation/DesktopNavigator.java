package com.interviewai.desktop.navigation;

import com.interviewai.desktop.api.AccountSummary;
import com.interviewai.desktop.api.AuthApi;
import com.interviewai.desktop.api.HealthApi;
import com.interviewai.desktop.feature.auth.AccountController;
import com.interviewai.desktop.feature.auth.LoginController;
import com.interviewai.desktop.feature.auth.RegisterController;
import com.interviewai.desktop.feature.connection.ConnectionController;
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
    private final Executor executor;
    private final String backendAddress;

    public DesktopNavigator(Stage stage, AuthApi authApi, HealthApi healthApi, Executor executor,
            String backendAddress) {
        this.stage = stage;
        this.authApi = authApi;
        this.healthApi = healthApi;
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
        show("account-view.fxml", new AccountController(authApi, this, executor, account));
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
