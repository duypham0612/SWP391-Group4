# Dựng database CafeChain

Schema hiện tại có **25 bảng**. Bản trước có 49 bảng và quan hệ khác hẳn, nên
**không nâng cấp tại chỗ được** — phải tạo database mới rồi migrate.

`src/main/resources/db/migration/V1__database.sql` là **file SQL duy nhất** của dự án
(có test `MigrationChecksumTest` khoá điều này). Không thêm file `.sql` nào khác vào
`src/` hoặc `sql/`.

---

## Cách nhanh — chạy script

```powershell
.\tools\setup-database.ps1 -ServerInstance 'localhost\HOANGANH'
```

Script sẽ: tạo database `CafeChain_v2` → tạo SQL login `cafechain_app` → chạy Flyway
migrate → kiểm tra lại kết quả → in ra biến môi trường cần set.

Nếu database đã tồn tại và muốn làm lại từ đầu (**mất toàn bộ dữ liệu trong đó**):

```powershell
.\tools\setup-database.ps1 -ServerInstance 'localhost\HOANGANH' -Force
```

Đổi tên database hoặc thông tin đăng nhập:

```powershell
.\tools\setup-database.ps1 -ServerInstance 'localhost\HOANGANH' `
    -DatabaseName CafeChain_demo -AppLogin cafe_user -AppPassword 'MatKhau@123'
```

---

## Cách thủ công

**1. Tạo database rỗng và login**

```sql
CREATE DATABASE CafeChain_v2;
GO
CREATE LOGIN cafechain_app WITH PASSWORD = 'CafeChain@2026Dev', CHECK_POLICY = OFF;
GO
USE CafeChain_v2;
CREATE USER cafechain_app FOR LOGIN cafechain_app;
ALTER ROLE db_owner ADD MEMBER cafechain_app;
GO
```

**2. Chạy Flyway**

```bash
mvn -Pdb-migrate flyway:migrate \
  "-Dflyway.url=jdbc:sqlserver://localhost:1433;databaseName=CafeChain_v2;encrypt=true;trustServerCertificate=true" \
  -Dflyway.user=cafechain_app \
  -Dflyway.password='CafeChain@2026Dev'
```

**3. Kiểm tra**

```sql
SELECT COUNT(*) FROM sys.tables WHERE name <> 'flyway_schema_history';  -- 25
SELECT version, success FROM ops.flyway_schema_history;                 -- 1, true
SELECT COUNT(*) FROM iam.UserAccount;                                   -- 4
```

---

## Cấu hình ứng dụng

Credential **không** nằm trong WAR — cấp qua biến môi trường:

```
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=CafeChain_v2;encrypt=true;trustServerCertificate=true
DB_USERNAME=cafechain_app
DB_PASSWORD=CafeChain@2026Dev
```

> **Dùng port, đừng dùng tên instance.** JDBC driver trong WAR không resolve được
> `localhost\HOANGANH` kể cả khi SQL Browser đang chạy — app sẽ fail lúc khởi động với
> lỗi `TCP/IP connection ... port 1433 has failed`. Lấy port thật bằng:
>
> ```sql
> SELECT DISTINCT port FROM sys.dm_tcp_listener_states WHERE type = 0 AND state = 0;
> ```

Tài khoản demo (mật khẩu `123456`): `admin`, `manager1`, `cashier1`, `barista1`.

---

## Chạy integration test

Test mặc định dùng Testcontainers (cần Docker). Muốn chạy trên SQL Server có sẵn:

```bash
mvn -Pintegration verify \
  "-Dit.db.url=jdbc:sqlserver://localhost:1433;databaseName=CafeChain_it;encrypt=true;trustServerCertificate=true" \
  -Dit.db.username=cafechain_app \
  -Dit.db.password='CafeChain@2026Dev'
```

> Dùng database **riêng** cho test. Các `*IT` chèn dữ liệu vào database đang chạy;
> nếu chạy chung với database demo thì `DatabaseMigrationIT` sẽ fail vì nó assert
> `catalog.Product` chỉ có 3 dòng seed.

---

## Đổi file SQL thì phải làm gì

`MigrationChecksumTest` khoá checksum của file migration. Sau khi sửa
`V1__database.sql`, cập nhật lại manifest:

```bash
sha256sum src/main/resources/db/migration/V1__database.sql
# chép hash vào sql/migration-checksums.sha256, giữ nguyên định dạng "<hash>  <đường dẫn>"
```

Vì checksum đổi, Flyway sẽ báo lỗi validate trên database đã migrate — phải tạo lại
database (chạy script với `-Force`).
