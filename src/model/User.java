package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Model người dùng cho hệ thống MFA-TOTP.
 * Lưu thông tin đăng nhập và trạng thái xác thực hai yếu tố.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String email;
    /** Băm mật khẩu (format do PasswordHasher quy định, không lưu plaintext). */
    private String passwordHash;
    /** Secret key TOTP dạng Base32 (tương thích Google Authenticator). */
    private String totpSecret;
    private boolean isTotpEnabled;
    /** Danh sách mã dự phòng dùng một lần khi mất thiết bị TOTP. */
    private List<String> backupCodes;
    /** Các backup code đã sử dụng để tránh dùng lại. */
    private Set<String> usedBackupCodes;

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.totpSecret = null;
        this.isTotpEnabled = false;
        this.backupCodes = new ArrayList<>();
        this.usedBackupCodes = new HashSet<>();
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getTotpSecret() { return totpSecret; }
    public boolean isTotpEnabled() { return isTotpEnabled; }
    public List<String> getBackupCodes() { return backupCodes == null ? new ArrayList<>() : new ArrayList<>(backupCodes); }

    /** Bật TOTP: lưu secret Base32 và danh sách backup codes. */
    public void enableTOTP(String secret, List<String> codes) {
        this.totpSecret = secret;
        this.backupCodes = codes != null ? new ArrayList<>(codes) : new ArrayList<>();
        this.usedBackupCodes = new HashSet<>();
        this.isTotpEnabled = true;
    }

    /** Tắt TOTP: xóa secret và backup codes. */
    public void disableTOTP() {
        this.totpSecret = null;
        this.backupCodes = new ArrayList<>();
        this.usedBackupCodes = new HashSet<>();
        this.isTotpEnabled = false;
    }

    /**
     * Dùng một backup code. Mỗi code chỉ dùng một lần.
     * @return true nếu code hợp lệ và chưa dùng.
     */
    public boolean useBackupCode(String code) {
        if (code == null || backupCodes == null) return false;
        if (usedBackupCodes.contains(code)) return false;
        for (String c : backupCodes) {
            if (c.equals(code)) {
                usedBackupCodes.add(code);
                return true;
            }
        }
        return false;
    }
}
