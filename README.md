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
│   ├── PasswordHasher.java # Băm mật khẩu SHA-256 + Salt
│   └── QRCodeGenerator.java # Sinh file QR từ otpauth URI (cần ZXing)
├── totp/
│   └── TOTPGenerator.java # Sinh và xác thực mã TOTP (RFC 6238)
├── service/
│   └── AuthService.java   # Lớp dịch vụ xác thực (dùng cho CLI / API sau này)
└── test/
    └── TOTPTest.java      # Kiểm thử TOTP (menu 6)
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

- **QRCodeGenerator (`util/QRCodeGenerator.java`)**  
  Tạo file ảnh PNG chứa QR Code từ chuỗi `otpauth://totp/...` (quét bằng Google Authenticator). Cần thư viện **ZXing** (core + javase). Khi bật TOTP (menu 3), nếu classpath có ZXing thì tự tạo file `qrcode_<username>.png`.

- **TOTPGenerator (`totp/TOTPGenerator.java`)**  
  Theo RFC 6238: HMAC-SHA1, bước thời gian 30 giây, Dynamic Truncation để ra mã 6 số. Có `generateSecretKey()` (Base32), `generateTOTP(secret, counter)`, `getCurrentCode(secret)` (mã hiện tại để test), `verifyTOTP(secret, code)` với time window ±1.

- **AuthService (`service/AuthService.java`)**  
  Lớp dịch vụ tách logic nghiệp vụ khỏi giao diện. Cung cấp: `register()`, `loginStep1()` / `loginStep2()`, `enableTOTP()`, `disableTOTP()`, `getCurrentTOTPCode(username)` (mã TOTP hiện tại để test). Trả về các đối tượng kết quả (`RegisterResult`, `LoginStep1Result`, `TotpSetupResult`, …) để CLI hoặc front-end/API xử lý.

- **Main (`Main.java`)**  
  Menu CLI: Đăng ký, Đăng nhập, Bật TOTP, Tắt TOTP, Xem mã TOTP hiện tại, **Chạy test TOTP**, Thoát. Khi bật TOTP có thể tạo file QR (nếu có ZXing). Chỉ gọi `AuthService` và in kết quả ra console.

- **TOTPTest (`test/TOTPTest.java`)**  
  Bộ kiểm thử TOTP: `generateSecretKey()`, `getCurrentCode()`, `verifyTOTP()` đúng/sai, secret null. Chạy từ menu 6 hoặc `java -cp out test.TOTPTest`.

---

## Hướng dẫn chạy và test

### Yêu cầu

- JDK 8 trở lên (có `javac` và `java`).

### Biên dịch và chạy

**Cách 1 — Chỉ JDK (không QR Code):**

```bash
mkdir out
javac -encoding UTF-8 -d out src/util/Base32.java src/util/PasswordHasher.java src/model/User.java src/totp/TOTPGenerator.java src/database/UserDatabase.java src/service/AuthService.java src/test/TOTPTest.java src/Main.java
java -cp out Main
```

(Không biên dịch `util/QRCodeGenerator.java` vì cần ZXing; chương trình vẫn chạy, khi bật TOTP sẽ không tạo file QR.)

**Cách 2 — Maven (có QR Code):**

Nếu đã cài Maven và muốn tính năng tạo file QR khi bật TOTP:

```bash
mvn compile
mvn exec:java -Dexec.mainClass="Main"
```

Maven sẽ tải ZXing; biên dịch cả `QRCodeGenerator`; khi chọn menu 3 (Bật TOTP) sẽ xuất thêm file `qrcode_<username>.png`.

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

### Test TOTP tự động (menu 6)

Chọn **menu 6 – Chạy test TOTP** để chạy bộ kiểm thử: tạo secret, sinh mã, verify đúng/sai. Tất cả dùng `TOTPGenerator` của dự án (RFC 6238). Kết quả in ra số test PASS/FAIL.

### Tính năng tích hợp từ nhóm (QR Code, Test)

- **QR Code:** Từ ý tưởng nhóm (mfa-demo), đã tích hợp vào dự án chính: class `util.QRCodeGenerator` dùng ZXing, gọi khi bật TOTP (menu 3). Nếu chạy bằng Maven (có ZXing), sẽ tạo file PNG để quét bằng app Authenticator.
- **Test TOTP:** Thay vì test với generator giả (random), dự án có `test.TOTPTest` dùng đúng `TOTPGenerator` RFC 6238, chạy từ menu 6.

### Kết nối với front-end sau này

Toàn bộ logic nghiệp vụ nằm trong **`service.AuthService`**. CLI chỉ gọi các method của `AuthService` và in kết quả. Để kết nối với Web/React, Android hoặc REST API:

- Tạo `new AuthService(userDatabase)` (có thể thay `UserDatabase` bằng lớp lưu trữ khác).
- Gọi `register()`, `loginStep1()`, `loginStep2()`, `enableTOTP()`, `disableTOTP()`.
- Chuyển các đối tượng kết quả (`RegisterResult`, `LoginStep1Result`, `TotpSetupResult`, …) sang JSON hoặc HTTP status/body cho client.

---

## Đánh giá khả năng làm giao diện UI

### Hiện trạng

- **AuthService** đã tách sẵn: toàn bộ nghiệp vụ (đăng ký, đăng nhập 2 bước, bật/tắt TOTP) nằm trong `service.AuthService`, trả về object kết quả (success, message, data). Giao diện chỉ cần gọi service và hiển thị.
- **CLI hiện tại** là một cách “view”; thay bằng cửa sổ (Swing/JavaFX) hoặc Web không đòi hỏi sửa logic.

### Hướng làm UI desktop (Java thuần)

| Cách | Ưu | Nhược |
|------|----|--------|
| **Swing** (javax.swing) | Có sẵn trong JDK, không thêm dependency, dễ đóng gói. | Giao diện cũ, cần tự layout (GridBag, BorderLayout) hoặc form designer. |
| **JavaFX** (javax.*) | UI hiện đại, FXML + CSS, binding. | Từ JDK 11 trở đi JavaFX tách khỏi JDK, cần thêm dependency hoặc dùng JDK có bundled (e.g. Liberica Full). |

**Gợi ý triển khai nhanh:**

1. **Swing:** Một `JFrame` chính với card/panel: (1) Màn đăng nhập: username, password, nút “Đăng nhập” → gọi `authService.loginStep1()`; nếu `totpRequired` thì chuyển sang (2) Màn nhập mã TOTP/backup code → `authService.loginStep2()`; sau đó màn “Đăng nhập thành công” hoặc thông báo lỗi. Thêm menu hoặc nút: Đăng ký, Bật TOTP (nhập username → gọi `enableTOTP()` → hiển thị secret + đường dẫn file QR nếu có).
2. **JavaFX:** Tương tự với `Stage`/`Scene`, các `AnchorPane` hoặc FXML: màn login, màn OTP, màn kết quả; binding với property; gọi cùng `AuthService`.

### Hướng làm UI Web

- **Backend:** Tạo REST API (Servlet, Spring Boot, hoặc JAX-RS) gọi `AuthService`, trả JSON.
- **Front-end:** Trang HTML/JS (hoặc React/Vue) gọi API: form đăng nhập → nhập OTP → hiển thị kết quả. Có thể dùng thêm thư viện tạo QR trên web (e.g. `qrcode.js`) để hiển thị QR từ `otpauthUri` khi bật TOTP.

### Kết luận

- **Làm giao diện UI cho ứng dụng là khả thi.** Logic đã sẵn trong `AuthService`; chỉ cần lớp trình bày (Swing/JavaFX/Web) gọi đúng API và xử lý success/error. Nên bắt đầu bằng Swing nếu muốn không thêm dependency và đóng gói đơn giản; nếu ưu tiên giao diện đẹp và hiện đại thì dùng JavaFX hoặc Web.
