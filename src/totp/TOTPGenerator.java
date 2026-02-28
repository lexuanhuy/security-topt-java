package totp;

import util.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Sinh và xác thực mã TOTP theo RFC 6238.
 * - HMAC-SHA1 với counter (time step).
 * - Dynamic Truncation để lấy mã 6 số.
 * - Secret key dạng Base32 (tương thích Google Authenticator).
 */
public final class TOTPGenerator {

    /** Kích thước secret key (byte) — 160 bit phù hợp HMAC-SHA1. */
    private static final int SECRET_KEY_BYTES = 20;
    /** Bước thời gian (giây) theo RFC 6238. */
    private static final int TIME_STEP_SECONDS = 30;
    /** Số chữ số OTP. */
    private static final int DIGITS = 6;

    private TOTPGenerator() {}

    /**
     * Tạo secret key ngẫu nhiên, trả về chuỗi Base32 (để nhập vào Google Authenticator).
     */
    public static String generateSecretKey() {
        byte[] bytes = new byte[SECRET_KEY_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base32.encode(bytes);
    }

    /**
     * Sinh mã TOTP 6 số cho (secret, counter).
     * Counter = floor(unixTime / 30). Secret là chuỗi Base32.
     *
     * Thuật toán (RFC 6238 / RFC 4226):
     * 1. Decode Base32 secret -> key bytes.
     * 2. HMAC-SHA1(key, counter_8_bytes_big_endian) -> 20-byte hash.
     * 3. Dynamic Truncation: offset = hash[19] & 0x0F, lấy 4 byte từ hash[offset] tạo 31-bit int.
     * 4. OTP = int % 10^6, format 6 chữ số.
     */
    public static String generateTOTP(String secretBase32, long counter) {
        byte[] key = Base32.decode(secretBase32);
        if (key.length == 0) throw new IllegalArgumentException("Secret Base32 không hợp lệ");

        // Counter 8 byte big-endian (RFC 4226)
        byte[] counterBytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            counterBytes[i] = (byte) (counter & 0xff);
            counter >>= 8;
        }

        byte[] hash;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            hash = mac.doFinal(counterBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        // Dynamic Truncation (RFC 4226 §5.4)
        int offset = hash[hash.length - 1] & 0x0f;
        int binary =
                ((hash[offset] & 0x7f) << 24)
                        | ((hash[offset + 1] & 0xff) << 16)
                        | ((hash[offset + 2] & 0xff) << 8)
                        | (hash[offset + 3] & 0xff);

        int otp = binary % 1_000_000;
        return String.format("%06d", otp);
    }

    /**
     * Mã TOTP 6 số tại thời điểm hiện tại (để đối chiếu với app khi test).
     */
    public static String getCurrentCode(String secretBase32) {
        if (secretBase32 == null || secretBase32.isBlank()) return null;
        long currentStep = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        return generateTOTP(secretBase32.trim(), currentStep);
    }

    /**
     * Xác thực mã OTP người dùng nhập.
     * Time window ±1 (chấp nhận mã trước/sau 30s) để xử lý lệch đồng hồ (clock drift).
     */
    public static boolean verifyTOTP(String secretBase32, String userCode) {
        if (secretBase32 == null || userCode == null || userCode.length() != DIGITS) return false;
        long currentStep = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        for (int delta = -1; delta <= 1; delta++) {
            String expected = generateTOTP(secretBase32, currentStep + delta);
            if (expected.equals(userCode.trim())) return true;
        }
        return false;
    }
}
