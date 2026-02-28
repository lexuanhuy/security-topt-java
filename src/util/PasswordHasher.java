package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Utility băm mật khẩu bằng SHA-256 kết hợp Salt.
 * Không lưu plaintext: mỗi lần hash dùng salt ngẫu nhiên 16 byte,
 * lưu dạng "hex(salt):hex(SHA-256(salt||password))" để verify sau.
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH_BYTES = 16;
    private static final String SEP = ":";

    private PasswordHasher() {}

    /**
     * Băm mật khẩu: tạo salt ngẫu nhiên, SHA-256(salt || password), trả về "saltHex:hashHex".
     */
    public static String hash(String password) {
        if (password == null) password = "";
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = sha256(concat(salt, password.getBytes(StandardCharsets.UTF_8)));
        return HexFormat.of().formatHex(salt) + SEP + HexFormat.of().formatHex(hash);
    }

    /**
     * Kiểm tra mật khẩu với giá trị đã lưu (format saltHex:hashHex).
     */
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null || !stored.contains(SEP)) return false;
        int i = stored.indexOf(SEP);
        String saltHex = stored.substring(0, i);
        String hashHex = stored.substring(i + 1);
        byte[] salt = HexFormat.of().parseHex(saltHex);
        byte[] computed = sha256(concat(salt, password.getBytes(StandardCharsets.UTF_8)));
        return HexFormat.of().formatHex(computed).equals(hashHex);
    }

    private static byte[] sha256(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            return md.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(ALGORITHM + " không khả dụng", e);
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
