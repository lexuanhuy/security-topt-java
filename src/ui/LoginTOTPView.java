package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import model.User;
import service.AuthService;

public class LoginTOTPView {

    public static VBox create(User user) {
        Label lbHint = new Label("Nhập mã TOTP (6 số) hoặc Backup Code:");
        TextField tfCode = new TextField();
        tfCode.setPromptText("000000 hoặc backup code");
        tfCode.setPrefWidth(220);
        tfCode.setMaxWidth(220);

        Button btnConfirm = new Button("Xác nhận");
        Button btnBack = new Button("Quay lại");
        btnBack.setOnAction(e -> MfaApp.showLogin());

        btnConfirm.setOnAction(e -> {
            String code = tfCode.getText().trim();
            AuthService.LoginStep2Result r = MfaApp.getAuthService().loginStep2(user, code);
            Alert a = new Alert(r.success ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
            a.setContentText(r.message);
            a.showAndWait();
            if (r.success) {
                MfaApp.setCurrentUser(user);
                MfaApp.showMain();
            }
        });

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.getChildren().addAll(lbHint, tfCode, btnConfirm, btnBack);
        return root;
    }
}
