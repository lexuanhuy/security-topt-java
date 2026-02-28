# Hệ thống xác thực đa yếu tố (MFA) – TOTP

Dự án Java offline mô phỏng hệ thống xác thực hai yếu tố (2FA) sử dụng TOTP theo chuẩn RFC 6238. Ứng dụng chạy trên giao diện dòng lệnh (CLI), có thể mở rộng kết nối với front-end hoặc REST API sau này.

---

## Các thành phần đã làm

### Cấu trúc package

```
src/
├── Main.java              # Giao diện dòng lệnh (CLI)
├── model/
│   └── User.java          # Model người dùng
├── database/
│   └── UserDatabase.java  # Lưu trữ user và persistence
├── util/
│   ├── Base32.java        # Mã hóa/giải mã Base32
│   └── PasswordHasher.java # Băm mật khẩu SHA-256 + Salt
├── totp/
│   └── TOTPGenerator.java # Sinh và xác thực mã TOTP (RFC 6238)
└── service/
    └── AuthService.java   # Lớp dịch vụ xác thực (dùng cho CLI / API sau này)
```

### Mô tả từng thành phần

- **User (`model/User.java`)**  
  Lưu: `userId` (UUID), `username`, `email`, `passwordHash`, `totpSecretKey` (Base32), `totpEnabled`, `totpActivatedAt`, `backupCodes`, `usedBackupCodes`, `createdAt`. Có phương thức `enableTOTP()`, `disableTOTP()`, `useBackupCode()`.

- **UserDatabase (`database/UserDatabase.java`)**  
  Quản lý user trong bộ nhớ (map theo username), lưu/đọc ra file `users.dat` bằng Object Serialization để dữ liệu không mất khi tắt chương trình. Cung cấp: đăng ký (kiểm tra trùng, hash mật khẩu), xác thực mật khẩu, lấy user, bật/tắt TOTP cho user.

- **PasswordHasher (`util/PasswordHasher.java`)**  
  Utility băm mật khẩu bằng SHA-256 kết hợp salt ngẫu nhiên (16 byte). Lưu dạng `hex(salt):hex(hash)`; không lưu mật khẩu plaintext. Có `hash()` và `verify()`.

- **Base32 (`util/Base32.java`)**  
  Mã hóa/giải mã Base32 theo RFC 4648, dùng cho secret key TOTP tương thích ứng dụng như Google Authenticator.

- **TOTPGenerator (`totp/TOTPGenerator.java`)**  
  Theo RFC 6238: HMAC-SHA1, bước thời gian 30 giây, Dynamic Truncation để ra mã 6 số. Có `generateSecretKey()` (Base32), `generateTOTP(secret, counter)`, `getCurrentCode(secret)` (mã hiện tại để test), `verifyTOTP(secret, code)` với time window ±1.

- **AuthService (`service/AuthService.java`)**  
  Lớp dịch vụ tách logic nghiệp vụ khỏi giao diện. Cung cấp: `register()`, `loginStep1()` / `loginStep2()`, `enableTOTP()`, `disableTOTP()`, `getCurrentTOTPCode(username)` (mã TOTP hiện tại để test). Trả về các đối tượng kết quả (`RegisterResult`, `LoginStep1Result`, `TotpSetupResult`, …) để CLI hoặc front-end/API xử lý.

- **Main (`Main.java`)**  
  Menu CLI: Đăng ký, Đăng nhập, Bật TOTP, Tắt TOTP, Xem mã TOTP hiện tại (test), Thoát. Chỉ gọi `AuthService` và in kết quả ra console.

---

## Hướng dẫn chạy và test

### Yêu cầu

- JDK 8 trở lên (có `javac` và `java`).

### Biên dịch và chạy

Mở terminal tại thư mục gốc dự án (ví dụ `D:\huylx\projects\security-topt-java`) và chạy:

```bash
# Tạo thư mục output (nếu chưa có)
mkdir out

# Biên dịch (theo thứ tự phụ thuộc)
javac -encoding UTF-8 -d out src/util/Base32.java src/util/PasswordHasher.java src/model/User.java src/totp/TOTPGenerator.java src/database/UserDatabase.java src/service/AuthService.java src/Main.java

# Chạy chương trình
java -cp out Main
```

### Luồng test cơ bản

1. **Đăng ký (menu 1)**  
   Nhập username (3–50 ký tự), email, mật khẩu. Kiểm tra: đăng ký lại cùng username sẽ báo “Username đã tồn tại”.

2. **Bật TOTP (menu 3)**  
   Nhập username. Hệ thống in ra **Secret Key (Base32)** và **otpauth URI** dùng cho app Authenticator, kèm **mã TOTP hiện tại** để đối chiếu ngay, và 10 **backup codes** (dùng khi mất app).

3. **Đăng nhập (menu 2)**  
   - Bước 1: username + mật khẩu.  
   - Nếu user đã bật TOTP: nhập tiếp mã TOTP 6 số (từ app) hoặc một backup code.  
   Kiểm tra: đăng nhập đúng mật khẩu, sau đó nhập đúng mã từ app TOTP hoặc backup code.

4. **Tắt TOTP (menu 4)**  
   Nhập username để tắt TOTP cho user đó. Sau đó đăng nhập chỉ cần username + mật khẩu (không hỏi mã TOTP).

Dữ liệu user được lưu trong file `users.dat` (Object Serialization) tại thư mục chạy chương trình; lần chạy sau dữ liệu vẫn được giữ.

### Cách test TOTP

Mã TOTP là mã **6 số** thay đổi mỗi ~30 giây, do **app** (Google Authenticator, Microsoft Authenticator, v.v.) tạo ra khi bạn thêm **Secret Key** vào app. Backup codes chỉ dùng dự phòng khi mất app.

**Các bước test TOTP:**

1. **Đăng ký** (menu 1) rồi **Bật TOTP** (menu 3), nhập username.
2. Trong phần **“MÃ TOTP (dùng cho app Authenticator)”**, copy **Secret Key** (dòng in giữa `>>> ... <<<`).
3. Mở app Authenticator (ví dụ Google Authenticator) → **Thêm tài khoản** → **Nhập key thủ công** → dán Secret Key vừa copy.
4. **Đối chiếu mã ngay:**
   - Ngay sau khi bật TOTP: xem dòng **“Mã máy chủ: XXXXXX”** in trên CLI — phải trùng với mã 6 số trong app (trong vòng ~30 giây).
   - Bất kỳ lúc nào: chọn **menu 5 – Xem mã TOTP hiện tại**, nhập username → so sánh mã in ra với mã trong app (cùng thời điểm).
5. **Đăng nhập** (menu 2): bước 1 nhập user + mật khẩu, bước 2 nhập **mã 6 số từ app** (không phải backup code) → nếu đúng sẽ báo “Xác thực 2FA thành công”.

Nếu mã máy chủ và mã trong app trùng nhau trong cùng cửa sổ thời gian thì TOTP đã cấu hình đúng.

### Kết nối với front-end sau này

Toàn bộ logic nghiệp vụ nằm trong **`service.AuthService`**. CLI chỉ gọi các method của `AuthService` và in kết quả. Để kết nối với Web/React, Android hoặc REST API:

- Tạo `new AuthService(userDatabase)` (có thể thay `UserDatabase` bằng lớp lưu trữ khác).
- Gọi `register()`, `loginStep1()`, `loginStep2()`, `enableTOTP()`, `disableTOTP()`.
- Chuyển các đối tượng kết quả (`RegisterResult`, `LoginStep1Result`, `TotpSetupResult`, …) sang JSON hoặc HTTP status/body cho client.
