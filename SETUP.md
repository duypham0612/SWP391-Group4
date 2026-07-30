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

Mở **SSMS** → mở file `sql/database.sql` → **Execute** (F5). Đây là **file SQL duy nhất** của dự án.

Mặc định script chỉ tạo hoặc nâng cấp schema (an toàn cho DB hiện hữu), đồng thời áp dụng mọi migration còn thiếu.

Toàn bộ dữ liệu mẫu nằm trong chính file này, sau **4 cờ ở đầu file — mặc định đều là `0`**. Muốn bơm dữ liệu nào thì đổi cờ tương ứng thành `1` rồi Execute lại:

| Cờ | Phần | Nội dung | Lưu ý |
|---|---|---|---|
| `@SeedDemo` | PART 8 | 4 role, 3 chi nhánh, catalog 15 món, 31 ngày lịch sử bán, story hôm nay, demo hao hụt, KDS ZT1–ZT4 | Chỉ chạy khi DB **chưa có role nào** |
| `@FixtureBarista` | PART 9 | Dữ liệu đối chiếu role Barista ở CN01 | Ghi đè xếp ca hôm nay của barista1/2/4 |
| `@FixtureDemo` | PART 10 | Dữ liệu buổi bảo vệ, dọn sạch KDS/bàn | **Xoá và dựng lại `BranchInventory`** — chỉ dùng trên DB mới, chạy kèm `@SeedDemo = 1` |
| `@AdminDemoData` | PART 11 | CN04–CN08 + danh mục/sản phẩm/voucher demo màn Admin | **Phá bất biến "tồn = Σ sổ cái"** (ghi tồn không kèm `InventoryTransaction`) — chỉ dùng cho DB demo riêng |

Deploy bình thường: **giữ nguyên cả 4 số `0`**, cứ Execute là an toàn với dữ liệu đang có.

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

## 8. Kiểm thử integration Barista (Docker)

`mvn test` chỉ chạy unit test nhanh. Để chạy transaction thật với SQL Server disposable qua Testcontainers (không dùng `db.properties` local), cần Docker đang hoạt động rồi chạy:

```bash
mvn -Pintegration verify
```

Suite tạo database tạm từ `sql/database.sql`, kiểm KDS concurrent claim/complete/remake và tự xóa container sau khi chạy. GitHub Actions chạy cùng lệnh cho mọi pull request và push vào `main`.

---

## 9. Cấu trúc dự án (MVC, layer-based — package gốc `com.cafe`)

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
