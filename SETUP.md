# Cà Phê Chain (CafeChain) — Hướng dẫn cài đặt và chạy

Hệ thống quản lý chuỗi cafe dine-in với bốn vai trò: Admin, Branch Manager,
Cashier và Barista. Ứng dụng dùng mô hình MVC với JSP, Servlet và SQL Server.

## Công nghệ

| Hạng mục | Phiên bản/cấu hình |
|---|---|
| JDK | 17 |
| Servlet/JSP | Jakarta Servlet 5.0, JSP 3.0, JSTL 3.0 |
| Server | Apache Tomcat 10.1.x; không dùng Tomcat 9 |
| Build | Maven, đóng gói WAR `cafe-shop.war` |
| Database | Microsoft SQL Server 2017+, mặc định `CafeChain_v2` |
| Migration | Flyway 13.1.0, schema history `ops.flyway_schema_history` |
| Connection pool | HikariCP |

## 1. Chuẩn bị

Cài JDK 17, Maven 3.8+, SQL Server và Tomcat 10.1.x. SQL Server phải bật
TCP/IP; với cấu hình local thường dùng cổng `1433`.

Kiểm tra nhanh:

```powershell
java -version
mvn -version
Test-NetConnection localhost -Port 1433
```

## 2. Dựng database

Nguồn schema duy nhất của dự án là:

```text
src/main/resources/db/migration/V1__database.sql
```

Không dùng đường dẫn cũ `sql/database.sql`; file đó đã bị xóa. WAR cũng không
tự chạy migration khi khởi động.

Cách nhanh trên Windows:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\tools\setup-database.ps1 -ServerInstance localhost
```

Script sẽ:

1. Tạo database mới `CafeChain_v2`.
2. Tạo login `cafechain_app` và user trong database.
3. Chạy `V1__database.sql` qua Flyway.
4. Kiểm tra 25 bảng nghiệp vụ, version schema và dữ liệu demo.

Schema 25 bảng không nâng cấp tại chỗ từ schema legacy 49 bảng. Nếu
`CafeChain_v2` đã tồn tại, script sẽ dừng an toàn. Chỉ dùng `-Force` khi chấp
nhận xóa toàn bộ dữ liệu trong chính database đó:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\tools\setup-database.ps1 -ServerInstance localhost -Force
```

Xem quy trình thủ công và cách dùng database riêng tại
[`docs/DATABASE-SETUP.md`](docs/DATABASE-SETUP.md).

## 3. Cấu hình kết nối

Không ghi URL, username hoặc password vào `db.properties`. File này chỉ chứa driver
và tham số HikariCP. Cấp credential cho tiến trình Tomcat bằng biến môi trường:

```powershell
$env:DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=CafeChain_v2;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME = 'cafechain_app'
$env:DB_PASSWORD = 'CafeChain@2026Dev'
```

Có thể dùng Java system properties `db.url`, `db.username`, `db.password` thay cho biến
môi trường. Với IDE, khai báo ba biến trên trong cấu hình chạy Tomcat.

Ứng dụng sẽ fail-fast nếu không kết nối được DB hoặc Flyway version không
khớp `src/main/resources/db/expected-schema.properties`.

## 4. Build và chạy

Build WAR:

```powershell
mvn clean package -DskipTests
```

File sinh ra tại `target/cafe-shop.war`. Copy file này vào `<TOMCAT>/webapps/`,
sau đó khởi động Tomcat từ cùng terminal đã set biến DB:

```powershell
Copy-Item .\target\cafe-shop.war '<TOMCAT>\webapps\cafe-shop.war' -Force
& '<TOMCAT>\bin\startup.bat'
```

Với NetBeans hoặc IntelliJ Ultimate, deploy artifact `cafe-shop:war exploded` và đặt
application context là `/cafe-shop`.

Kiểm tra sau khi server chạy:

- Health: <http://localhost:8080/cafe-shop/health>
- Đăng nhập: <http://localhost:8080/cafe-shop/auth/login>

Health phải trả `HTTP 200` và hiển thị schema version `1`.

## 5. Tài khoản demo

| Vai trò | Username | Mật khẩu |
|---|---|---|
| Admin | `admin` | `123456` |
| Branch Manager | `manager1` | `123456` |
| Cashier | `cashier1` | `123456` |
| Barista | `barista1` | `123456` |

Hash demo được seed trực tiếp bởi migration. `SeedPasswordListener` chỉ cảnh báo
tài khoản không có BCrypt hash hợp lệ; listener không tự gán mật khẩu.

## 6. Kiểm thử

Chạy unit test:

```powershell
mvn test
```

Chạy integration test bằng SQL Server 2022 trong Testcontainers; Docker phải hoạt động:

```powershell
mvn clean verify -Pintegration
```

Integration suite tạo database disposable, chạy Flyway từ
`classpath:db/migration`, thực thi test và xóa container. CI dùng cùng lệnh trên
Ubuntu x86_64.

Muốn dùng SQL Server có sẵn, phải tạo database test riêng và truyền
`it.db.url`, `it.db.username`, `it.db.password`. Không trỏ integration test vào
`CafeChain_v2` đang dùng demo.

## 7. Lỗi thường gặp

| Triệu chứng | Cách xử lý |
|---|---|
| `javax.servlet` not found hoặc JSP lỗi | Dùng Tomcat 10.1.x thay vì Tomcat 9. |
| Health trả `503` | Kiểm tra ba biến DB, TCP/IP, cổng SQL Server và Flyway version. |
| `DBConnection init failed` | Credential chưa được cấp cho đúng tiến trình Tomcat. |
| Root `localhost:8080` trả 404 | Truy cập context `/cafe-shop`. |
| Migration validate fail | Không sửa database đã migrate; dựng database local mới. |

## 8. Cấu trúc dự án

```text
src/main/java/com/cafe/
├── common/       logic và enum dùng chung
├── config/       HikariCP và schema version guard
├── controller/   Servlet theo vai trò
├── dao/          truy cập SQL/JDBC
├── filter/       charset, auth, RBAC, branch scope, cashier duty
├── listener/     lifecycle database/schema/assets
├── model/        model và DTO
├── service/      nghiệp vụ và transaction
└── web/          form binding, renderer, support, view model

src/main/resources/
├── db.properties
└── db/
    ├── expected-schema.properties
    └── migration/V1__database.sql

src/main/webapp/
├── WEB-INF/views/
├── WEB-INF/fragments/
└── assets/
```

Chi tiết schema và quy trình migration nằm trong
[`docs/DATABASE-SETUP.md`](docs/DATABASE-SETUP.md) và
[`docs/RA_SOAT_DATABASE.md`](docs/RA_SOAT_DATABASE.md).
