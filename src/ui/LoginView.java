package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import model.User;
import service.AuthService;

public class LoginView {

    public static VBox create() {
        Label lbUser = new Label("Username:");
        TextField tfUser = new TextField();
        tfUser.setPromptText("username");
        tfUser.setPrefWidth(220);

        Label lbPass = new Label("Mật khẩu:");
        PasswordField pfPass = new PasswordField();
        pfPass.setPromptText("mật khẩu");
        pfPass.setPrefWidth(220);

        Button btnLogin = new Button("Đăng nhập");
        Button btnBack = new Button("Quay lại");
        btnBack.setOnAction(e -> MfaApp.showWelcome());

        btnLogin.setOnAction(e -> {
            String username = tfUser.getText().trim();
            String password = pfPass.getText();
            AuthService.LoginStep1Result r = MfaApp.getAuthService().loginStep1(username, password);
            if (!r.success) {
                new Alert(Alert.AlertType.WARNING, r.message).showAndWait();
                return;
            }
            MfaApp.setCurrentUser(r.user);
            if (r.totpRequired) {
                MfaApp.showLoginTOTP(r.user);
            } else {
                MfaApp.showMain();
            }
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);
        form.addRow(0, lbUser, tfUser);
        form.addRow(1, lbPass, pfPass);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.getChildren().addAll(form, btnLogin, btnBack);
        return root;
    }
}
