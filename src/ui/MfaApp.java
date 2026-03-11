package ui;

import database.UserDatabase;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import model.User;
import service.AuthService;

/**
 * Ứng dụng JavaFX: Đăng ký, đăng nhập (TOTP/QR), màn hình chính, đăng xuất.
 */
public class MfaApp extends Application {

    private static Stage primaryStage;
    private static AuthService authService;
    private static User currentUser;

    public static Stage getPrimaryStage() { return primaryStage; }
    public static AuthService getAuthService() { return authService; }
    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        UserDatabase db = new UserDatabase();
        authService = new AuthService(db);

        stage.setTitle("Hệ thống MFA-TOTP");
        stage.setMinWidth(420);
        stage.setMinHeight(380);
        stage.setScene(new Scene(new Pane(), 420, 380));
        showWelcome();
        stage.show();
    }

    public static void showWelcome() {
        primaryStage.getScene().setRoot(WelcomeView.create());
    }

    public static void showRegister() {
        primaryStage.getScene().setRoot(RegisterView.create());
    }

    public static void showLogin() {
        primaryStage.getScene().setRoot(LoginView.create());
    }

    public static void showLoginTOTP(User user) {
        primaryStage.getScene().setRoot(LoginTOTPView.create(user));
    }

    public static void showMain() {
        primaryStage.getScene().setRoot(MainView.create());
    }

    public static void logout() {
        currentUser = null;
        showWelcome();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
