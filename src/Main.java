import database.UserDatabase;
import service.AuthService;

import java.util.Scanner;

/**
 * Giao diện dòng lệnh (CLI) cho hệ thống MFA-TOTP.
 * Dùng AuthService để xử lý logic — sau này front-end / REST API có thể gọi cùng AuthService.
 */
public class Main {

    static final Scanner sc = new Scanner(System.in);
    static final UserDatabase db = new UserDatabase();
    static final AuthService authService = new AuthService(db);

    public static void main(String[] args) {
        printBanner();
        while (true) {
            printMenu();
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            int choice;
            try {
                choice = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Chọn không hợp lệ. Nhập số.");
                continue;
            }
            switch (choice) {
                case 1 -> register();
                case 2 -> login();
                case 3 -> enableTOTP();
                case 4 -> disableTOTP();
                case 5 -> showCurrentTOTP();
                case 0 -> {
                    System.out.println("Thoát.");
                    return;
                }
                default -> System.out.println("Chọn không hợp lệ.");
            }
        }
    }

    static void printBanner() {
        System.out.println("\n========================================");
        System.out.println("   HỆ THỐNG MFA-TOTP (CLI)");
        System.out.println("========================================\n");
    }

    static void printMenu() {
        System.out.println("----- MENU -----");
        System.out.println("1. Đăng ký");
        System.out.println("2. Đăng nhập");
        System.out.println("3. Bật TOTP");
        System.out.println("4. Tắt TOTP");
        System.out.println("5. Xem mã TOTP hiện tại (test với app Authenticator)");
        System.out.println("0. Thoát");
        System.out.print("Chọn: ");
    }

    static void register() {
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Email: ");
        String email = sc.nextLine().trim();
        System.out.print("Password: ");
        String pwd = sc.nextLine();

        var r = authService.register(username, email, pwd);
        System.out.println(r.message);
    }

    static void login() {
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Password: ");
        String pwd = sc.nextLine();

        var r1 = authService.loginStep1(username, pwd);
        System.out.println(r1.message);
        if (!r1.success) return;
        if (!r1.totpRequired) return;

        System.out.print("Nhập mã TOTP (6 số) hoặc Backup Code: ");
        String code = sc.nextLine();
        var r2 = authService.loginStep2(r1.user, code);
        System.out.println(r2.message);
    }

    static void enableTOTP() {
        System.out.print("Username: ");
        String username = sc.nextLine().trim();

        var r = authService.enableTOTP(username);
        System.out.println(r.message);
        if (!r.success) return;

        System.out.println("\n========== MÃ TOTP (dùng cho app Authenticator) ==========");
        System.out.println("  Đây là thông tin để bạn LẤY MÃ 6 SỐ trong app (Google Authenticator, v.v.):");
        System.out.println();
        System.out.println("  Secret Key (Base32) — nhập vào app khi thêm tài khoản:");
        System.out.println("  >>> " + r.secret + " <<<");
        System.out.println();
        System.out.println("  Hoặc dùng URI (một số app cho phép quét QR từ URI):");
        System.out.println("  " + r.otpauthUri);
        System.out.println();
        System.out.println("  Hướng dẫn: Mở app > Thêm tài khoản > Nhập key thủ công > Dán Secret Key trên.");
        System.out.println("  Sau khi thêm, app sẽ hiển thị mã 6 số thay đổi mỗi ~30 giây.");
        System.out.println("==============================================================");

        System.out.println("\n--- Mã TOTP hiện tại (đối chiếu với app ngay) ---");
        String current = authService.getCurrentTOTPCode(username);
        if (current != null) {
            System.out.println("  Mã máy chủ: " + current + "  (phải trùng với mã trong app trong vòng ~30s)");
        }
        System.out.println("\n--- Backup Codes (dùng khi mất app, mỗi code một lần) ---");
        r.backupCodes.forEach(c -> System.out.println("  " + c));
    }

    static void disableTOTP() {
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        var r = authService.disableTOTP(username);
        System.out.println(r.message);
    }

    /** Xem mã TOTP 6 số hiện tại để đối chiếu với app (test). */
    static void showCurrentTOTP() {
        System.out.print("Username (đã bật TOTP): ");
        String username = sc.nextLine().trim();
        String code = authService.getCurrentTOTPCode(username);
        if (code == null) {
            System.out.println("Không tìm thấy user hoặc user chưa bật TOTP.");
            return;
        }
        System.out.println("Mã TOTP hiện tại: " + code);
        System.out.println("(So sánh với mã 6 số trong app Authenticator — phải trùng trong cùng cửa sổ ~30 giây.)");
    }
}
