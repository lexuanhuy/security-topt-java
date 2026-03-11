package ui;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import util.QRCodeGenerator;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Dialog hiển thị Secret, QR Code (để quét bằng app Authenticator) và Backup Codes.
 */
public class EnableTOTPDialog {

    public static void show(String secret, String otpauthUri, List<String> backupCodes, String username) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(MfaApp.getPrimaryStage());
        dialog.setTitle("Bật TOTP - Quét QR");

        Label lbSecret = new Label("Secret Key (nhập thủ công nếu không quét được):");
        TextArea taSecret = new TextArea(secret);
        taSecret.setEditable(false);
        taSecret.setPrefRowCount(2);
        taSecret.setWrapText(true);

        ImageView qrView = new ImageView();
        qrView.setFitWidth(220);
        qrView.setPreserveRatio(true);
        BufferedImage buffered = QRCodeGenerator.generateToBufferedImage(otpauthUri);
        if (buffered != null) {
            Image img = SwingFXUtils.toFXImage(buffered, null);
            qrView.setImage(img);
        } else {
            qrView.setVisible(false);
        }

        Label lbQR = new Label("Quét mã QR bằng Google Authenticator (hoặc app tương tự):");
        Label lbBackup = new Label("Backup Codes (lưu lại, mỗi code dùng một lần):");
        StringBuilder sb = new StringBuilder();
        if (backupCodes != null) {
            for (String c : backupCodes) sb.append(c).append("\n");
        }
        TextArea taBackup = new TextArea(sb.toString().trim());
        taBackup.setEditable(false);
        taBackup.setPrefRowCount(6);

        Button btnClose = new Button("Đã lưu");
        btnClose.setOnAction(e -> dialog.close());

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(
                lbSecret, taSecret,
                lbQR, qrView,
                lbBackup, taBackup,
                btnClose
        );

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 380, 520);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
