# Cà Phê Chain (CafeChain) — Hướng Dẫn Cài Đặt & Chạy

> Hệ thống Quản lý Chuỗi Cafe (SWP391) · **Dine-in**, 4 role: Admin · Branch Manager · Cashier · Barista.
> Kiến trúc **MVC**: JSP + Servlet + JSTL + SQL Server.

---

## Tech Stack (theo code hiện tại)

| Hạng mục | Giá trị |
|---|---|
| JDK | **17** |
| Servlet/JSP | **Jakarta EE** (`jakarta.servlet 5.0`) + **JSTL 3.0** (`uri="jakarta.tags.core"`) |
| Server | **Apache Tomcat 10.1.x** (Jakarta — **KHÔNG dùng Tomcat 9**) |
| Build | Maven (`war`, finalName **`cafe-shop`** → context **`/cafe-shop`**) |
| Database | Microsoft SQL Server 2017+, database **`CafeChain`** |
| Connection pool | HikariCP (đọc `src/main/resources/db.properties`) |
| Auth | HttpSession + Servlet Filter, mật khẩu BCrypt |

---

## 1. Chuẩn bị môi trường (mỗi máy cài sẵn)

| Tool | Version | Ghi chú |
|---|---|---|
| JDK | **17** | Temurin/Oracle. Đặt làm Project SDK trong IDE. |
| SQL Server | 2017+ | Kèm **SSMS** để chạy script. Bật **TCP/IP** + SQL Auth (user `sa`). |
| Tomcat | **10.1.x** | Tải bản zip, giải nén ra một thư mục. |
| Maven | 3.8+ | (Đã tích hợp sẵn trong NetBeans/IntelliJ.) |
| IDE | NetBeans **hoặc** IntelliJ | Xem mục 4. |

---

## 2. Tạo Database

Mở **SSMS** → mở file `sql/database.sql` → **Execute** (F5).

Script `database.sql` giờ là **file DUY NHẤT** — chạy 1 phát ra DB demo đầy đủ (schema + toàn bộ seed, đã gộp mọi migration). Gồm 3 phần:
- **PART A** — Database **`CafeChain`** (8 schema, 37 bảng) + seed gốc.
- **PART B** — Catalog **15 món** (mọi món đủ công thức + modifier) + ảnh thật (Unsplash).
- **PART C** — Demo lớn: **3 chi nhánh**, **16 tài khoản** (BCrypt thật, mật khẩu `123456`), **~31 ngày lịch sử bán (≈800 hoá đơn)** + story hôm nay đủ mọi role → **đăng nhập được ngay, dashboard/biểu đồ đầy dữ liệu**.

---

## 3. Cấu hình kết nối DB (mỗi người sửa theo máy mình)

Mở `src/main/resources/db.properties`:

```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=CafeChain;encrypt=false;trustServerCertificate=true
db.username=sa
db.password=YourPassword123      ← đổi thành mật khẩu sa của máy bạn
db.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
```

> ⚠️ **Cổng (port):** đa số máy SQL Server chạy ở **1433**. Nếu file đang để cổng khác (vd `14333`), đổi lại cho khớp máy bạn. Mỗi thành viên chỉnh `url` (port) + `password` theo máy mình.

---

## 4. Chạy dự án

### 4A. NetBeans (khuyên dùng — miễn phí, tích hợp sẵn Tomcat)

1. **Thêm Tomcat:** `Tools → Servers → Add Server… → Apache Tomcat or TomEE` → trỏ tới thư mục Tomcat 10 đã tải → Finish.
2. **Mở dự án:** `File → Open Project…` → chọn thư mục dự án (NetBeans tự nhận Maven qua `pom.xml`).
3. **Gán server:** chuột phải project → `Properties → Run → Server = Apache Tomcat 10` → OK.
4. **Chạy:** chuột phải project → **Run** (F6). NetBeans tự build war + deploy + mở trình duyệt.
5. Vào: **http://localhost:8080/cafe-shop/auth/login**

### 4B. IntelliJ IDEA **Ultimate** (có tích hợp Tomcat)

1. `Open` → chọn thư mục dự án (tự import Maven).
2. `Run → Edit Configurations → + → Tomcat Server → Local` → trỏ tới Tomcat 10 home.
3. Tab **Deployment → + → Artifact → `cafe-shop:war exploded`**; **Application context = `/cafe-shop`**.
4. Bấm **Run ▶** → vào **http://localhost:8080/cafe-shop/auth/login**

### 4C. IntelliJ IDEA **Community** (KHÔNG có server — chạy thủ công)

> IntelliJ Community không deploy được lên Tomcat. Dùng cách thủ công hoặc cài plugin **Smart Tomcat**.

```bash
mvn clean package -DskipTests          # tạo ra target/cafe-shop.war
# Copy target/cafe-shop.war  →  <TOMCAT>/webapps/
# Khởi động Tomcat:
#   Windows:   <TOMCAT>\bin\startup.bat
#   mac/linux: <TOMCAT>/bin/startup.sh
```
→ vào **http://localhost:8080/cafe-shop/auth/login**

---

## 5. Đăng nhập

| Vai trò | Username | Mật khẩu |
|---|---|---|
| Admin | `admin` | `123456` |
| Branch Manager | `manager1` | `123456` |
| Cashier | `cashier1` | `123456` |
| Barista | `barista1` | `123456` |

> Mật khẩu seed được app **tự gán khi khởi động lần đầu** (qua `SeedPasswordListener`, chỉ chạy khi đã kết nối được DB). Đổi mật khẩu mặc định khi lên production.

---

## 6. Lỗi thường gặp

| Triệu chứng | Nguyên nhân & cách sửa |
|---|---|
| JSP lỗi / `javax.servlet` not found / 404 toàn trang | Đang dùng **Tomcat 9**. Phải **Tomcat 10.1+** (Jakarta). |
| App chạy nhưng **login sai mật khẩu** | DB chưa kết nối (sai port/password) → `SeedPasswordListener` chưa set được mật khẩu. Sửa `db.properties`, restart app. |
| **Connect DB fail** | Bật **TCP/IP** trong SQL Server Configuration Manager, mở port, dùng SQL Auth (user `sa`), kiểm tra firewall. |
| Build/IDE báo sai phiên bản Java | Project SDK phải là **JDK 17**. |
| Vào `http://localhost:8080/` ra 404 | Context là **`/cafe-shop`** → phải vào `http://localhost:8080/cafe-shop/auth/login`. |

---

## 7. Build ra file WAR (để nộp / deploy)

```bash
mvn clean package -DskipTests
```
File WAR: `target/cafe-shop.war` → copy vào `<TOMCAT>/webapps/`.

---

## 8. Cấu trúc dự án (MVC, layer-based — package gốc `com.cafe`)

```
src/main/java/com/cafe/
├── config/        ← DBConnection (HikariCP đọc db.properties)
├── common/        ← Constants, EventType/EventPublisher, BCrypt, CSRF, BusinessException, DeductionCalculator…
├── model/         ← POJO (Order, OrderItem, Product, PrepBatch, BranchInventory…)
├── dao/           ← JDBC theo entity (nhận Connection tham số; không tự mở tx)
│   ├── admin/  cashier/  manager/  shared/
├── service/       ← Nghiệp vụ + transaction (mở/commit/rollback Connection)
│   ├── admin/  cashier/  manager/  barista/  customer/  shared/
├── controller/    ← Servlet (xử lý request) theo role
│   ├── admin/  cashier/  manager/  barista/  customer/  auth/
├── filter/        ← AuthFilter, RbacFilter, BranchScopeFilter
├── listener/      ← SeedPasswordListener (set mật khẩu seed lúc khởi động)
└── realtime/      ← polling/sự kiện KDS-QR

src/main/webapp/
├── WEB-INF/web.xml
├── WEB-INF/views/
│   ├── layout/    ← header / sidebar / footer / _statusBadge.jsp (DÙNG CHUNG)
│   ├── auth/  admin/  manager/  cashier/  barista/  customer/
├── assets/css/cafe-theme.css   ← design system DUY NHẤT
└── assets/js/  assets/img/

src/main/resources/db.properties   ← cấu hình kết nối DB
sql/database.sql                   ← schema + toàn bộ seed demo (file SQL DUY NHẤT)
```

> Tài liệu chi tiết: `CLAUDE.md`, `KE_HOACH_CHI_TIET_THEO_ROLE.md`, `docs/PROGRESS.md`.
