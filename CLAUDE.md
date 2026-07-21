# CLAUDE.md — Ngữ cảnh dự án (đọc mỗi session)

> Hệ thống Quản lý Chuỗi Cafe (SWP391) · Stack: **JSP + Servlet + JSTL + SQL Server**, kiến trúc **MVC**.
> Nguồn sự thật: `KE_HOACH_TRIEN_KHAI_CLAUDE_CODE.md` (nguyên tắc) + **`KE_HOACH_CHI_TIET_THEO_ROLE.md` (đặc tả tên class/method/route — THẮNG khi mâu thuẫn về đặt tên)** + `sql/database.sql`.
> Nhật ký tiến trình & quyết định: `docs/PROGRESS.md` (đọc để biết đang ở Phase nào).
>
> **Quy ước tên (đang áp dụng):** Controller `{Feature}Servlet`, Service `{Domain}Service`, DAO `{Entity}Dao`. DAO: `findAll/findById/findBy{X}/insert/update/delete/update{Field}`. Service: `get{Noun}List/get{Noun}/create/update/delete/set{Noun}{Field}` + động từ nghiệp vụ. Route số ít `/{role}/{feature}` (vd `/admin/product`), auth `/auth/{action}`. **Giữ base package `com.cafe`, layer-based** (không role-based) — quyết định đã chốt.

---

## 0. Nguyên tắc BẤT BIẾN (không được phá vỡ)

- **4 role** full-stack: ADMIN · BRANCH MANAGER · CASHIER · BARISTA.
- **Dine-in only** — không giao hàng, không giỏ hàng B2C. Xương sống là **table session** (`sales.TableSession`).
- **Database đã chốt**: SQL Server, database `CafeChain`, **37 bảng / 8 schema** (`iam, org, catalog, inventory, hr, sales, payment, ops`). File: `sql/database.sql`. **Không tự ý đổi schema** — cần đổi thì nêu lý do & hỏi trước.
- **3 contract phải tôn trọng trong code**:
  1. **Event bus + status enum chung**: mọi domain event ghi vào `ops.OutboxEvent`; trạng thái món dùng chung ở `sales.OrderItem.Status` (`WAITING → MAKING → READY → SERVED`, `READY` = "Sẵn lấy").
  2. **Cờ RAW/PREPPED**: nguyên liệu thô trừ trực tiếp; đồ pha sẵn (PREPPED) trừ từ tồn PREPPED (raw đã bị trừ lúc `PrepBatch`) — **không trừ thô 2 lần**.
  3. **Cashier sở hữu mọi order entry**: đơn quầy & đơn QR đi cùng một bảng `sales.Orders` / cùng một service.
- **Sổ cái tồn kho**: mọi thay đổi tồn đi qua `inventory.InventoryTransaction`; **KHÔNG BAO GIỜ** UPDATE thẳng `inventory.BranchInventory.QuantityOnHand` mà không ghi ledger.

---

## 1. Stack & quyết định kiến trúc (đã chốt ở gap analysis)

| Hạng mục | Lựa chọn |
|---|---|
| Java | 17 |
| Servlet/JSP | **Jakarta EE** (`jakarta.servlet 5.0`) + **JSTL 3.0** (`uri="jakarta.tags.core"`) → **Tomcat 10+** |
| Build | Maven (`war`, finalName `cafe-shop`) |
| DB driver | `mssql-jdbc` |
| Connection pool | **HikariCP** (`config/DBConnection.getDataSource()`) — *quyết định Phase 0* |
| Auth | **HttpSession + Servlet Filter** (KHÔNG JWT) |
| RBAC | `AuthFilter` + `RbacFilter` + `BranchScopeFilter` (lọc theo role + branch_id) |
| Mật khẩu | BCrypt (`jBCrypt`) — seed đang `$2a$placeholder`, phải thay hash thật |
| Realtime (KDS/QR) | **AJAX polling 3–5s** trước, WebSocket sau; `OutboxEvent` vẫn ghi event |
| Base package | **`com.cafe`** (giữ nguyên, không đổi sang com.cafechain) |
| QR app khách | JSP server-rendered, mobile-first, session ẩn danh gắn table token (không login) |

---

## 2. Layering & quy ước code (tuân thủ xuyên suốt)

```
controller/ (Servlet) → service/ (nghiệp vụ + transaction) → dao/ (JDBC)
```
- **JSP CẤM scriptlet `<% %>` / `<%= %>`** — chỉ JSTL (`c:forEach/c:if/c:choose`) + EL `${}` + `fmt`.
- **Transaction sống ở Service**: Service mở/commit/rollback Connection; **DAO nhận `Connection` tham số**, không tự mở. Controller **không** chứa SQL; DAO **không** chứa nghiệp vụ. Một nghiệp vụ = một transaction.
- **Naming**: `XxxServlet`, `XxxService`, `XxxDao`; model = danh từ số ít (`Order`, `OrderItem`).
- **Sổ cái tồn kho**: chỉ đổi tồn qua `InventoryService.applyTxn(branch, ingredient, deltaQty, type, refTable, refId)` — hàm này vừa INSERT `InventoryTransaction` vừa UPDATE `BranchInventory` trong cùng tx, publish `stock.low` nếu chạm ngưỡng.
- **Status enum chung**: `enum OrderItemStatus { WAITING, MAKING, READY, SERVED, CANCELLED }` — dùng ở KDS, Cashier, QR tracking; không hard-code string.
- **Event**: một điểm duy nhất `EventPublisher.publish(type, aggregateId, branchId, payloadJson)` ghi vào `ops.OutboxEvent`. Loại: `order.created`, `order.status_changed`, `payment.completed`, `inventory.deducted`, `stock.low`.
- **Branch scoping**: mọi query có chi nhánh **phải** filter `branch_id` lấy từ `BranchContext` trong session. Chỉ ADMIN xem chéo chi nhánh.
- **Validation + CSRF**: validate server-side mọi form POST; token CSRF cho form ghi.
- **Không hard-code connection**: lấy từ `DBConnection.getDataSource()`.

---

## 3. Cấu trúc thư mục đích (MVC)

```
src/main/java/com/cafe/
  config/   common/   model/   dao/   service/   controller/   filter/   realtime/
src/main/webapp/
  WEB-INF/web.xml
  WEB-INF/views/layout/  (header.jsp, sidebar.jsp, footer.jsp, _statusBadge.jsp — DÙNG CHUNG)
  WEB-INF/views/{auth,admin,manager,cashier,barista,customer}/
  assets/css/cafe-theme.css   (design system DUY NHẤT)
  assets/js/  assets/img/
```

---

## 4. Design system (CHỐT — dùng chung toàn bộ)

Một `cafe-theme.css` + một master layout (include header/sidebar/footer). Không màn nào tự chế style.

```css
:root{
  --espresso:#3B2417; --coffee:#6F4E37; --caramel:#B07D4E;
  --latte:#E8D9C5; --foam:#FBF7F0; --cream:#FFFDF9; --sage:#5B7B5A;
  --text:#2E2218; --muted:#8A7A6A; --line:#E5DACB;
  --st-waiting:#E0A100; --st-making:#2F6FB0; --st-ready:#3E8E5A;
  --st-served:#9A8C7C; --st-cancelled:#C0392B;
  --radius:14px; --shadow:0 2px 10px rgba(59,36,23,.08);
}
```
- Font body **Be Vietnam Pro**; heading/logo display ấm (Playfair Display / Pacifico cho logo).
- Nền `--foam`, card `--cream` bo `--radius` + `--shadow`, sidebar nền `--espresso` chữ kem, nút chính `--coffee` hover `--caramel`.
- **Status badge** dùng đúng 5 màu trên — Barista/Cashier/QR khách render **giống hệt** qua fragment `_statusBadge.jsp`.
- QR khách = mobile-first (1 cột, nút lớn); portal nhân viên = desktop-first (sidebar trái + content phải).
- Component dùng lại: page-header, data-table, form-card, status-badge, alert/toast, empty-state, pagination.

---

## 5. Thuật toán lõi — Modifier-Aware Auto-Deduction (rủi ro cao → viết unit test TRƯỚC)

**Khi Barista bấm "Xong" (READY) cho một OrderItem:**
```
required = {}
for line in ProductRecipe(productId):           required[line.ing] += line.qty * orderItem.qty
for opt in OrderItemModifier(orderItemId):
    for imp in ModifierIngredientImpact(opt):    required[imp.ing] += imp.qtyDelta * orderItem.qty
begin tx
  for (ing, qty) in required:
      InventoryService.applyTxn(branch, ing, -qty, 'DEDUCT', 'OrderItem', orderItemId)
  OrderItem.Status = READY ; OrderItem.DoneAt = now
  EventPublisher.publish('inventory.deducted', orderId, branch, ...)
  EventPublisher.publish('order.status_changed', orderId, branch, {status:'READY'})
commit
```
**Chống double-count:** mỗi ingredient (RAW *hoặc* PREPPED) có dòng tồn riêng ở `BranchInventory`. Ở bước pha món, cứ trừ đúng ingredient mà công thức tham chiếu — **không** phân nhánh RAW/PREPPED tại đây. Phân nhánh chỉ xảy ra một lần, tại `PrepBatch`:
```
createPrepBatch(preppedIng, qtyProduced):
  begin tx
  for (rawIng, qtyPerYield, yieldQty) in PrepRecipe(preppedIng):
      consumed = qtyProduced / yieldQty * qtyPerYield
      InventoryService.applyTxn(branch, rawIng, -consumed, 'PREP_OUT', 'PrepBatch', batchId)
  InventoryService.applyTxn(branch, preppedIng, +qtyProduced, 'PREP_IN', 'PrepBatch', batchId)
  commit
```
**Unit test bắt buộc:** pha 1 ly Cold Brew (PREPPED) → chỉ trừ tồn Cold Brew, **không** trừ cà phê hạt lần 2.

---

## 6. Lộ trình Phase (tuần tự, mỗi Phase chạy được + test được rồi mới sang Phase sau)

`Auth → (Catalog, Recipe, Voucher, Branch) → (Inventory, HR) → (Order, KDS) → Payment → QR app → Report`

| Phase | Nội dung |
|---|---|
| 0 | Nền tảng & UI shell (skeleton, pom/web.xml, HikariCP, chạy DB script, master layout + cafe-theme.css + login rỗng, smoke test) |
| 1 | Auth & RBAC (login session, BCrypt, 3 filter, landing theo role) |
| 2 | Admin: Catalog & Config (IAM, Branch, Category, Product, Recipe/BOM RAW/PREPPED + modifier impact, Voucher, BranchMenu) — làm **lát cắt CRUD mẫu** trước |
| 3 | Manager: Inventory & HR (Supplier, Stock Receipt→ledger, low-stock, Reconciliation, Shift, Attendance, Payroll) |
| 4 | Sales + KDS (Table session, POS order + `order.created`, KDS queue + auto-deduct modifier-aware, Prep, Waste, 86 board) — polling |
| 5 | Payment (Cashier shift, Checkout, Split/Merge Bill, voucher, `payment.completed`) |
| 6 | Customer QR app (scan→menu→cart→place order reuse order service→tracking) mobile-first |
| 7 | Report & Polish (dashboard doanh thu, golden-path E2E, fix bug) |

**Quy tắc làm việc:** Phase 0–2 làm **lát cắt MVC mẫu** (gợi ý `catalog.Category`) trước rồi nhân bản. Logic rủi ro (deduction, prep, voucher, shift conflict): **viết test trước**. Dừng xin xác nhận giữa các Phase. Khi định viết scriptlet trong JSP hoặc UPDATE thẳng tồn kho không qua ledger → **dừng & sửa**: đó là vi phạm quy ước.
