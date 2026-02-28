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

        System.out.println("\n--- Lưu Secret Key (Base32) ---");
        System.out.println(r.secret);
        System.out.println("\n--- Thêm vào Google Authenticator ---");
        System.out.println("  Cách 1: Mở app GA > Thêm tài khoản > Nhập key thủ công > Dán key trên.");
        System.out.println("  Cách 2: Dùng URI sau (một số app hỗ trợ quét QR từ URI):");
        System.out.println(r.otpauthUri);
        System.out.println("\n--- Backup Codes (mỗi code dùng một lần) ---");
        r.backupCodes.forEach(c -> System.out.println("  " + c));
    }

    static void disableTOTP() {
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        var r = authService.disableTOTP(username);
        System.out.println(r.message);
    }
}
