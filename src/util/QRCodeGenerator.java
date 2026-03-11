package util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Sinh file ảnh QR Code từ otpauth URI (để quét bằng Google Authenticator, v.v.).
 * Phụ thuộc: ZXing (com.google.zxing:core, com.google.zxing:javase).
 * Nếu không có ZXing trên classpath, chương trình vẫn chạy nhưng không tạo được file QR.
 */
public final class QRCodeGenerator {

    private static final int SIZE = 300;

    private QRCodeGenerator() {}

    /**
     * Tạo file PNG chứa QR Code từ otpauth URI.
     *
     * @param otpauthUri chuỗi otpauth://totp/...
     * @param outputPath đường dẫn file xuất (ví dụ "qrcode_admin.png")
     * @return đường dẫn tuyệt đối file đã ghi, hoặc null nếu lỗi
     */
    public static String generateToFile(String otpauthUri, String outputPath) {
        if (otpauthUri == null || outputPath == null || otpauthUri.isBlank()) return null;
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                    otpauthUri.trim(),
                    BarcodeFormat.QR_CODE,
                    SIZE,
                    SIZE
            );
            Path path = Paths.get(outputPath);
            MatrixToImageWriter.writeToPath(matrix, "PNG", path);
            return path.toAbsolutePath().toString();
        } catch (Exception e) {
            return null;
        }
    }
}
