package database;

import model.User;
import util.PasswordHasher;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kho lưu trữ người dùng với persistence bằng Object Serialization.
 * Dữ liệu được ghi ra file khi thay đổi để không mất khi tắt chương trình.
 */
public class UserDatabase implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_STORE_PATH = "users.dat";

    private final Path storePath;
    /** Map username (lowercase) -> User. */
    private final Map<String, User> users;

    public UserDatabase() {
        this(DEFAULT_STORE_PATH);
    }

    public UserDatabase(String storePath) {
        this.storePath = Path.of(storePath);
        this.users = new ConcurrentHashMap<>();
        loadFromFile();
    }

    /** Độ dài username theo chuẩn: 3-50 ký tự. */
    private static final int USERNAME_MIN_LEN = 3;
    private static final int USERNAME_MAX_LEN = 50;

    /**
     * Đăng ký user mới: kiểm tra username 3-50 ký tự, UNIQUE, băm mật khẩu rồi lưu.
     * userId và createdAt do User tự sinh.
     * @return true nếu đăng ký thành công, false nếu username không hợp lệ hoặc đã tồn tại.
     */
    public boolean registerUser(String username, String email, String plainPassword) {
        String trimmed = username != null ? username.trim() : "";
        String key = trimmed.toLowerCase();
        if (key.length() < USERNAME_MIN_LEN || key.length() > USERNAME_MAX_LEN) return false;
        if (users.containsKey(key)) return false;
        String passwordHash = PasswordHasher.hash(plainPassword != null ? plainPassword : "");
        User user = new User(trimmed, email != null ? email.trim() : "", passwordHash);
        users.put(key, user);
        saveToFile();
        return true;
    }

    /**
     * Bước 1 đăng nhập: xác thực username + mật khẩu.
     * @return User nếu đúng, null nếu sai.
     */
    public User authenticateByPassword(String username, String plainPassword) {
        User u = users.get(username.trim().toLowerCase());
        if (u == null) return null;
        if (!PasswordHasher.verify(plainPassword, u.getPasswordHash())) return null;
        return u;
    }

    /**
     * Lấy user theo username (dùng cho bước 2 TOTP hoặc enable/disable TOTP).
     */
    public User getUser(String username) {
        return users.get(username.trim().toLowerCase());
    }

    /**
     * Cập nhật trạng thái TOTP của user (enable) và lưu DB.
     */
    public void enableTOTPForUser(String username, String secret, java.util.List<String> backupCodes) {
        User u = getUser(username);
        if (u == null) return;
        u.enableTOTP(secret, backupCodes);
        saveToFile();
    }

    /**
     * Tắt TOTP cho user và lưu DB.
     */
    public void disableTOTPForUser(String username) {
        User u = getUser(username);
        if (u == null) return;
        u.disableTOTP();
        saveToFile();
    }

    /** Ghi toàn bộ users ra file (Object Serialization). */
    public void saveToFile() {
        try {
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(storePath))) {
                oos.writeObject(new HashMap<>(users));
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể ghi database: " + storePath, e);
        }
    }

    /** Đọc users từ file (Object Serialization). */
    @SuppressWarnings("unchecked")
    private void loadFromFile() {
        if (!Files.isRegularFile(storePath)) return;
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(storePath))) {
            Object data = ois.readObject();
            if (data instanceof Map) {
                Map<String, User> loaded = (Map<String, User>) data;
                users.clear();
                users.putAll(loaded);
            }
        } catch (IOException | ClassNotFoundException e) {
            // File hỏng hoặc chưa có: bỏ qua, dùng DB rỗng
        }
    }
}
