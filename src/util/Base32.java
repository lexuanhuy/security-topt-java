package util;

import java.util.Arrays;

/**
 * Mã hóa/giải mã Base32 theo RFC 4648.
 * Dùng cho secret key TOTP tương thích Google Authenticator (chỉ chấp nhận Base32).
 */
public final class Base32 {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Base32() {}

    /**
     * Mã hóa byte array thành chuỗi Base32 (không padding '=' để dễ copy vào app).
     */
    public static String encode(byte[] data) {
        if (data == null || data.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 31));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0)
            sb.append(ALPHABET.charAt((buffer << (5 - bitsLeft)) & 31));
        return sb.toString();
    }

    /**
     * Giải mã chuỗi Base32 thành byte array.
     * Chấp nhận chuỗi có hoặc không có padding '='.
     */
    public static byte[] decode(String encoded) {
        if (encoded == null) return new byte[0];
        encoded = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");
        if (encoded.isEmpty()) return new byte[0];
        int n = encoded.length();
        int outLen = (n * 5) / 8;
        byte[] result = new byte[outLen];
        int buffer = 0;
        int bitsLeft = 0;
        int idx = 0;
        for (char c : encoded.toCharArray()) {
            int v = ALPHABET.indexOf(c);
            if (v < 0) continue;
            buffer = (buffer << 5) | v;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[idx++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return idx < result.length ? Arrays.copyOf(result, idx) : result;
    }
}
