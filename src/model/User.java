package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Model người dùng cho hệ thống MFA-TOTP.
 * Theo chuẩn: userId (UUID), username, email, passwordHash, totpEnabled, totpSecretKey,
 * totpActivatedAt, backupCodes (10), usedBackupCodes, createdAt.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 2L;

    /** Định danh duy nhất, tự sinh (UUID). */
    private final String userId;
    /** Tên đăng nhập, 3-50 ký tự, UNIQUE. */
    private final String username;
    /** Địa chỉ email hợp lệ. */
    private final String email;
    /** SHA-256 hash của mật khẩu (format salt:hash do PasswordHasher). */
    private String passwordHash;
    /** Trạng thái TOTP, mặc định false. */
    private boolean totpEnabled;
    /** Secret key TOTP dạng Base32; null nếu chưa kích hoạt. */
    private String totpSecretKey;
    /** Thời điểm kích hoạt TOTP; null nếu chưa kích hoạt. */
    private LocalDateTime totpActivatedAt;
    /** 10 backup codes; null nếu chưa kích hoạt TOTP. */
    private List<String> backupCodes;
    /** Tập backup codes đã sử dụng (bắt buộc, mặc định rỗng). */
    private Set<String> usedBackupCodes;
    /** Thời điểm tạo tài khoản. */
    private final LocalDateTime createdAt;

    public User(String username, String email, String passwordHash) {
        this.userId = UUID.randomUUID().toString();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.totpEnabled = false;
        this.totpSecretKey = null;
        this.totpActivatedAt = null;
        this.backupCodes = null;
        this.usedBackupCodes = new HashSet<>();
        this.createdAt = LocalDateTime.now();
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isTotpEnabled() { return totpEnabled; }
    public String getTotpSecretKey() { return totpSecretKey; }
    public LocalDateTime getTotpActivatedAt() { return totpActivatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** Trả về danh sách backup codes (rỗng nếu chưa kích hoạt). */
    public List<String> getBackupCodes() {
        return backupCodes == null ? new ArrayList<>() : new ArrayList<>(backupCodes);
    }

    public Set<String> getUsedBackupCodes() {
        return usedBackupCodes == null ? new HashSet<>() : new HashSet<>(usedBackupCodes);
    }

    /** Bật TOTP: lưu secret Base32, 10 backup codes và thời điểm kích hoạt. */
    public void enableTOTP(String secret, List<String> codes) {
        this.totpSecretKey = secret;
        this.backupCodes = codes != null && codes.size() <= 10
                ? new ArrayList<>(codes)
                : (codes != null ? new ArrayList<>(codes.subList(0, Math.min(10, codes.size()))) : new ArrayList<>());
        this.usedBackupCodes = usedBackupCodes != null ? usedBackupCodes : new HashSet<>();
        this.usedBackupCodes.clear();
        this.totpEnabled = true;
        this.totpActivatedAt = LocalDateTime.now();
    }

    /** Tắt TOTP: xóa secret, backup codes và thời điểm kích hoạt. */
    public void disableTOTP() {
        this.totpSecretKey = null;
        this.totpActivatedAt = null;
        this.backupCodes = null;
        this.usedBackupCodes = usedBackupCodes != null ? usedBackupCodes : new HashSet<>();
        this.usedBackupCodes.clear();
        this.totpEnabled = false;
    }

    /**
     * Dùng một backup code. Mỗi code chỉ dùng một lần.
     * @return true nếu code hợp lệ và chưa dùng.
     */
    public boolean useBackupCode(String code) {
        if (code == null || backupCodes == null) return false;
        if (usedBackupCodes == null) usedBackupCodes = new HashSet<>();
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
