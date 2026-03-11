package test;

import totp.TOTPGenerator;

/**
 * Kiểm thử TOTP (RFC 6238): secret key, sinh mã, xác thực đúng/sai.
 * Dùng TOTPGenerator của dự án chính, không phụ thuộc code bên ngoài.
 */
public final class TOTPTest {

    public static void main(String[] args) {
        System.out.println("===== KIỂM THỬ TOTP (RFC 6238) =====\n");
        int pass = 0;
        int fail = 0;

        // 1. Tạo secret key
        String secret = TOTPGenerator.generateSecretKey();
        if (secret != null && !secret.isEmpty() && secret.matches("[A-Z2-7]+")) {
            System.out.println("  [PASS] generateSecretKey() — Secret Base32 hợp lệ");
            pass++;
        } else {
            System.out.println("  [FAIL] generateSecretKey() — Secret không đúng format Base32");
            fail++;
        }

        // 2. Sinh mã hiện tại
        String currentCode = TOTPGenerator.getCurrentCode(secret);
        if (currentCode != null && currentCode.length() == 6 && currentCode.matches("\\d{6}")) {
            System.out.println("  [PASS] getCurrentCode() — Mã 6 số: " + currentCode);
            pass++;
        } else {
            System.out.println("  [FAIL] getCurrentCode() — Mã không hợp lệ: " + currentCode);
            fail++;
        }

        // 3. verifyTOTP đúng mã
        if (currentCode != null && TOTPGenerator.verifyTOTP(secret, currentCode)) {
            System.out.println("  [PASS] verifyTOTP(secret, mã đúng) — Khớp");
            pass++;
        } else {
            System.out.println("  [FAIL] verifyTOTP(secret, mã đúng) — Không khớp");
            fail++;
        }

        // 4. verifyTOTP sai mã
        if (!TOTPGenerator.verifyTOTP(secret, "000000")) {
            System.out.println("  [PASS] verifyTOTP(secret, mã sai) — Từ chối đúng");
            pass++;
        } else {
            System.out.println("  [FAIL] verifyTOTP(secret, mã sai) — Không nên chấp nhận 000000");
            fail++;
        }

        // 5. Secret null/sai format
        if (!TOTPGenerator.verifyTOTP(null, "123456") && !TOTPGenerator.verifyTOTP("", "123456")) {
            System.out.println("  [PASS] verifyTOTP(secret null/rỗng) — Từ chối");
            pass++;
        } else {
            System.out.println("  [FAIL] verifyTOTP(secret null/rỗng)");
            fail++;
        }

        System.out.println("\n===== KẾT QUẢ: " + pass + " PASS, " + fail + " FAIL =====\n");
    }
}
