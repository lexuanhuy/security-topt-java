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
import service.AuthService;

public class RegisterView {

    public static VBox create() {
        Label lbUser = new Label("Username (3-50 ký tự):");
        TextField tfUser = new TextField();
        tfUser.setPromptText("username");
        tfUser.setPrefWidth(220);

        Label lbEmail = new Label("Email:");
        TextField tfEmail = new TextField();
        tfEmail.setPromptText("email@example.com");
        tfEmail.setPrefWidth(220);

        Label lbPass = new Label("Mật khẩu:");
        PasswordField pfPass = new PasswordField();
        pfPass.setPromptText("mật khẩu");
        pfPass.setPrefWidth(220);

        Button btnRegister = new Button("Đăng ký");
        Button btnBack = new Button("Quay lại");
        btnBack.setOnAction(e -> MfaApp.showWelcome());

        btnRegister.setOnAction(e -> {
            String username = tfUser.getText().trim();
            String email = tfEmail.getText().trim();
            String password = pfPass.getText();
            AuthService.RegisterResult r = MfaApp.getAuthService().register(username, email, password);
            Alert a = new Alert(r.success ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
            a.setContentText(r.message);
            a.showAndWait();
            if (r.success) MfaApp.showWelcome();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);
        form.addRow(0, lbUser, tfUser);
        form.addRow(1, lbEmail, tfEmail);
        form.addRow(2, lbPass, pfPass);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.getChildren().addAll(form, btnRegister, btnBack);
        return root;
    }
}
