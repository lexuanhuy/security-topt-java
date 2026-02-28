package service;

import database.UserDatabase;
import model.User;
import totp.TOTPGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lớp dịch vụ xác thực MFA-TOTP.
 * Tách biệt logic nghiệp vụ khỏi giao diện (CLI / Web API sau này).
 * Front-end hoặc REST API chỉ cần gọi các method này và xử lý kết quả.
 */
public class AuthService {

    private final UserDatabase db;

    public AuthService(UserDatabase db) {
        this.db = db;
    }

    /** Kết quả đăng ký. */
    public static class RegisterResult {
        public final boolean success;
        public final String message;
        public RegisterResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    /** Kết quả bước 1 đăng nhập (mật khẩu). */
    public static class LoginStep1Result {
        public final boolean success;
        public final String message;
        public final User user; // null nếu thất bại
        public final boolean totpRequired; // true nếu đã bật TOTP, cần bước 2
        public LoginStep1Result(boolean success, String message, User user, boolean totpRequired) {
            this.success = success;
            this.message = message;
            this.user = user;
            this.totpRequired = totpRequired;
        }
    }

    /** Kết quả bước 2 đăng nhập (TOTP hoặc backup code). */
    public static class LoginStep2Result {
        public final boolean success;
        public final boolean usedBackupCode;
        public final String message;
        public LoginStep2Result(boolean success, boolean usedBackupCode, String message) {
            this.success = success;
            this.usedBackupCode = usedBackupCode;
            this.message = message;
        }
    }

    /** Kết quả bật TOTP (trả secret + URI + backup codes cho client hiển thị / lưu). */
    public static class TotpSetupResult {
        public final boolean success;
        public final String message;
        public final String secret;
        public final String otpauthUri;
        public final List<String> backupCodes;
        public TotpSetupResult(boolean success, String message, String secret, String otpauthUri, List<String> backupCodes) {
            this.success = success;
            this.message = message;
            this.secret = secret;
            this.otpauthUri = otpauthUri;
            this.backupCodes = backupCodes != null ? new ArrayList<>(backupCodes) : List.of();
        }
    }

    /** Kết quả thao tác đơn giản (tắt TOTP, v.v.). */
    public static class ActionResult {
        public final boolean success;
        public final String message;
        public ActionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    // --- Đăng ký ---
    public RegisterResult register(String username, String email, String password) {
        if (username == null || username.isBlank())
            return new RegisterResult(false, "Username không được để trống.");
        if (db.registerUser(username, email, password != null ? password : ""))
            return new RegisterResult(true, "Đăng ký thành công.");
        return new RegisterResult(false, "Username đã tồn tại.");
    }

    // --- Đăng nhập bước 1 ---
    public LoginStep1Result loginStep1(String username, String password) {
        User user = db.authenticateByPassword(username, password);
        if (user == null)
            return new LoginStep1Result(false, "Sai tài khoản hoặc mật khẩu.", null, false);
        return new LoginStep1Result(
                true,
                user.isTotpEnabled() ? "Nhập mã TOTP hoặc backup code." : "Đăng nhập thành công.",
                user,
                user.isTotpEnabled()
        );
    }

    // --- Đăng nhập bước 2 (TOTP hoặc backup code) ---
    public LoginStep2Result loginStep2(User user, String code) {
        if (user == null)
            return new LoginStep2Result(false, false, "Phiên không hợp lệ.");
        if (code == null) code = "";
        code = code.trim();
        if (TOTPGenerator.verifyTOTP(user.getTotpSecret(), code))
            return new LoginStep2Result(true, false, "Xác thực 2FA thành công.");
        if (user.useBackupCode(code)) {
            db.saveToFile();
            return new LoginStep2Result(true, true, "Dùng backup code thành công.");
        }
        return new LoginStep2Result(false, false, "Sai mã OTP hoặc backup code.");
    }

    // --- Bật TOTP ---
    public TotpSetupResult enableTOTP(String username) {
        User user = db.getUser(username);
        if (user == null)
            return new TotpSetupResult(false, "Không tìm thấy user.", null, null, null);
        String secret = TOTPGenerator.generateSecretKey();
        List<String> backupCodes = generateBackupCodes();
        String issuer = "MFA";
        String otpauthUri = "otpauth://totp/" + issuer + ":" + username + "?secret=" + secret + "&issuer=" + issuer + "&period=30";
        db.enableTOTPForUser(username, secret, backupCodes);
        return new TotpSetupResult(true, "Đã bật TOTP. Lưu secret và backup codes.", secret, otpauthUri, backupCodes);
    }

    // --- Tắt TOTP ---
    public ActionResult disableTOTP(String username) {
        User user = db.getUser(username);
        if (user == null)
            return new ActionResult(false, "Không tìm thấy user.");
        db.disableTOTPForUser(username);
        return new ActionResult(true, "Đã tắt TOTP cho user: " + username);
    }

    public UserDatabase getDatabase() {
        return db;
    }

    private static List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>(10);
        Random r = new Random();
        for (int i = 0; i < 10; i++)
            codes.add(String.format("%08d", r.nextInt(100_000_000)));
        return codes;
    }
}
