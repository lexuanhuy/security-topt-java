package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import model.User;
import service.AuthService;

public class MainView {

    public static VBox create() {
        User user = MfaApp.getCurrentUser();
        if (user == null) {
            MfaApp.showWelcome();
            return new VBox();
        }

        Label lbWelcome = new Label("Chào, " + user.getUsername() + "!");
        lbWelcome.setFont(Font.font(18));

        Button btnEnableTOTP = new Button("Bật TOTP (quét QR)");
        Button btnDisableTOTP = new Button("Tắt TOTP");
        Button btnLogout = new Button("Đăng xuất");

        btnEnableTOTP.setVisible(!user.isTotpEnabled());
        btnDisableTOTP.setVisible(user.isTotpEnabled());

        btnEnableTOTP.setOnAction(e -> {
            AuthService.TotpSetupResult r = MfaApp.getAuthService().enableTOTP(user.getUsername());
            if (!r.success) {
                new Alert(Alert.AlertType.WARNING, r.message).showAndWait();
                return;
            }
            EnableTOTPDialog.show(r.secret, r.otpauthUri, r.backupCodes, user.getUsername());
            MfaApp.setCurrentUser(MfaApp.getAuthService().getDatabase().getUser(user.getUsername()));
            btnEnableTOTP.setVisible(false);
            btnDisableTOTP.setVisible(true);
        });

        btnDisableTOTP.setOnAction(e -> {
            AuthService.ActionResult r = MfaApp.getAuthService().disableTOTP(user.getUsername());
            new Alert(r.success ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING, r.message).showAndWait();
            if (r.success) {
                MfaApp.setCurrentUser(MfaApp.getAuthService().getDatabase().getUser(user.getUsername()));
                btnEnableTOTP.setVisible(true);
                btnDisableTOTP.setVisible(false);
            }
        });

        btnLogout.setOnAction(e -> MfaApp.logout());

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.getChildren().addAll(lbWelcome, btnEnableTOTP, btnDisableTOTP, btnLogout);
        return root;
    }
}
