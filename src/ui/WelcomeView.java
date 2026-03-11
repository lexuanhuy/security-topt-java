package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class WelcomeView {

    public static VBox create() {
        Text title = new Text("Hệ thống MFA-TOTP");
        title.setFont(Font.font(22));

        Button btnLogin = new Button("Đăng nhập");
        btnLogin.setPrefWidth(200);
        btnLogin.setOnAction(e -> MfaApp.showLogin());

        Button btnRegister = new Button("Đăng ký");
        btnRegister.setPrefWidth(200);
        btnRegister.setOnAction(e -> MfaApp.showRegister());

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.getChildren().addAll(title, btnLogin, btnRegister);
        return root;
    }
}
