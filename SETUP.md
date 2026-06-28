# Café Aroma - Hướng Dẫn Cài Đặt & Chạy

## Tech Stack
- **Backend**: Java Servlet (Jakarta EE 5)
- **View**: JSP + JSTL 2.0
- **Database**: Microsoft SQL Server
- **Build**: Maven 3.x
- **Server**: Apache Tomcat 10.x

---

## 1. Chuẩn Bị Môi Trường

| Tool | Version |
|------|---------|
| JDK | 11+ |
| Maven | 3.8+ |
| SQL Server | 2017+ |
| Tomcat | 10.1.x |

---

## 2. Tạo Database

Mở SQL Server Management Studio (SSMS), chạy file:
```
sql/database.sql
```

Script sẽ:
- Tạo database `CafeShopDB`
- Tạo đầy đủ các bảng
- Import dữ liệu mẫu (5 danh mục, 18 sản phẩm)
- Tạo tài khoản admin

---

## 3. Cấu Hình Kết Nối DB

Mở file `src/main/resources/db.properties`:

```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=CafeShopDB;encrypt=false;trustServerCertificate=true
db.username=sa
db.password=YourPassword123   ← Đổi thành mật khẩu SQL Server của bạn
```

---

## 4. Build Project

```bash
mvn clean package -DskipTests
```

File WAR sẽ xuất hiện tại: `target/cafe-shop.war`

---

## 5. Deploy lên Tomcat

**Cách 1 - Copy WAR:**
```
Copy target/cafe-shop.war → TOMCAT_HOME/webapps/
```

**Cách 2 - IntelliJ IDEA:**
1. Run → Edit Configurations → Tomcat Server → Local
2. Deployment tab → + → Artifact → cafe-shop:war
3. Application context: `/`
4. Click Run ▶

---

## 6. Truy Cập

| Trang | URL |
|-------|-----|
| Trang chủ | http://localhost:8080/ |
| Menu | http://localhost:8080/menu |
| Giỏ hàng | http://localhost:8080/cart |
| Đăng nhập | http://localhost:8080/login |
| Admin | http://localhost:8080/admin/dashboard |

---

## 7. Tài Khoản Mặc Định

| Vai trò | Username | Password |
|---------|----------|----------|
| Admin | `admin` | `admin123` |
| Staff | `staff1` | `admin123` |

---

## Cấu Trúc Project (MVC)

```
src/main/java/com/cafe/
├── controller/          ← C: Servlet (xử lý request)
│   ├── HomeServlet
│   ├── MenuServlet
│   ├── CartServlet
│   ├── CheckoutServlet
│   ├── OrderServlet
│   ├── AuthServlet
│   └── AdminServlet
├── model/               ← M: POJO (dữ liệu)
│   ├── Product, Category
│   ├── Order, OrderDetail
│   ├── User, CartItem
├── dao/                 ← M: Data Access (truy vấn DB)
│   ├── ProductDAO, CategoryDAO
│   ├── OrderDAO, UserDAO
├── util/                ← Tiện ích
│   ├── DBConnection
│   ├── SessionUtil
│   └── CartUtil
└── filter/
    └── CharsetFilter

src/main/webapp/         ← V: View (giao diện)
├── WEB-INF/views/
│   ├── home.jsp         ← Trang chủ
│   ├── menu.jsp         ← Menu
│   ├── cart.jsp         ← Giỏ hàng
│   ├── checkout.jsp     ← Thanh toán
│   ├── login.jsp        ← Đăng nhập
│   ├── register.jsp     ← Đăng ký
│   ├── order-detail.jsp ← Chi tiết đơn
│   ├── my-orders.jsp    ← Lịch sử đơn
│   ├── common/
│   │   ├── header.jsp
│   │   └── footer.jsp
│   └── admin/
│       ├── dashboard.jsp
│       ├── products.jsp
│       ├── orders.jsp
│       └── users.jsp
├── css/
│   ├── style.css        ← Giao diện chính (cafe theme)
│   └── admin.css        ← Giao diện admin
└── js/main.js

sql/database.sql         ← Schema + seed data
```
