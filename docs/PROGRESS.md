# PROGRESS.md — Nhật ký tiến trình dự án

> Cập nhật sau mỗi bước/Phase. Ngữ cảnh & quy ước cố định nằm ở `CLAUDE.md`.
>
> ⚠ **Nguồn mô tả CHỨC NĂNG chính xác = [`docs/FUNCTION_LIST.md`](FUNCTION_LIST.md)** (dựng trực tiếp từ source code, 2026-07-01). File PROGRESS này là nhật ký lịch sử — vài mô tả cũ đã lệch với code hiện tại (report gộp vào dashboard; `/dashboard` là router theo role; thêm `CashierDutyGuardFilter` + chấm giờ NV; 2 màn Home công khai/Home Editor; theme Espresso/Caramel chứ không phải Highlands đỏ). Khi cần biết chức năng thực tế → đọc FUNCTION_LIST.md.

## Trạng thái Phase

| Phase | Tên | Trạng thái |
|---|---|---|
| **0** | Nền tảng & UI shell | 🟢 Done (build WAR OK) |
| 1 | Auth & RBAC | 🟢 Done (build WAR OK) |
| 2 | Admin: Catalog & Config | 🟢 Done + đồng bộ đặc tả (chạy thật Tomcat OK) |
| 3 | Manager: Inventory & HR | 🟢 Done (M1–M8) · ledger + shift-conflict + attendance/payroll verify chạy thật |
| 4 | Sales + KDS | 🟢 Done · auto-deduct modifier-aware + Cold Brew + Contract #2 verify chạy thật |
| 5 | Payment | 🟢 Done · split/merge bill + voucher + payment.completed verify chạy thật |
| 6 | Customer QR app | 🟢 Done · scan→order(Source=QR) reuse OrderService→KDS chung→track verify thật |
| 7 | Report & Polish | 🟢 Done · dashboard doanh thu Admin + E2E 26 route 200 + invariant 0 mismatch |

Chú thích: ⚪ chưa làm · 🟡 đang làm/chờ duyệt · 🟢 Done.

---

## Gap analysis (2026-06-29)

**Kết quả khảo sát repo:**
- `sql/database.sql` = ĐÚNG schema chốt (database `CafeChain`, 8 schema, 37 bảng, có OutboxEvent / OrderItem.Status / InventoryTransaction / PrepRecipe / ModifierIngredientImpact + seed 4 role/4 user). Seed mật khẩu là `$2a$placeholder` → phải thay BCrypt thật.
- Stack đã đúng nhánh: Jakarta EE 5 + JSTL 3.0 (`jakarta.tags.core`) → **Tomcat 10+**; Maven war; mssql-jdbc + jbcrypt + jackson có sẵn. JSP hiện tại **0 scriptlet** (đạt quy ước).
- **Lệch lớn:** code Java/JSP hiện có là **app B2C webshop** (cart/checkout/register, model OrderDetail/CartItem, `db.properties` trỏ `CafeShopDB`) — **sai domain** so với plan dine-in. → bỏ, dựng lại.
- Thiếu: tầng `service/`, `common/` (OrderItemStatus enum, EventPublisher), `config/` HikariCP, `filter/` Auth/Rbac/BranchScope, `realtime/`, `cafe-theme.css` + design tokens, layout `sidebar/_statusBadge`.

---

## Quyết định đã chốt (2026-06-29)

1. **Code domain webshop cũ** → 🗑️ **Xóa & dựng lại sạch** (giữ `CharsetFilter` + pattern load properties).
2. **Connection pool** → ✅ **HikariCP** (`config/DBConnection.getDataSource()`).
3. **Base package** → giữ **`com.cafe`** (không đổi sang com.cafechain).
4. **File SQL** → giữ **`sql/database.sql`** (không đổi tên/vị trí; là schema chốt).

---

## Checklist Phase 0 (chờ duyệt)

**A. Dọn & chốt nền**
- [ ] Xóa code domain webshop (model/dao/controller/JSP cart-checkout-register-myorders); giữ CharsetFilter.
- [ ] `db.properties` → `databaseName=CafeChain` (+ config pool Hikari).
- [ ] Thêm HikariCP vào pom; viết `config/DBConnection.getDataSource()`.
- [ ] Thêm script thay `$2a$placeholder` bằng BCrypt thật (seed password bootstrap).

**B. Skeleton package** (rỗng + file mẫu)
- [ ] Tạo `config/ common/ model/ dao/ service/ controller/ filter/ realtime/`.
- [ ] `common/OrderItemStatus` enum; `common/EventPublisher` (stub ghi OutboxEvent); `common/Constants`.

**C. UI shell dùng chung**
- [ ] `assets/css/cafe-theme.css` (:root design tokens + font Be Vietnam Pro).
- [ ] Layout master: `layout/header.jsp`, `sidebar.jsp`, `footer.jsp`, `_statusBadge.jsp`.
- [ ] `auth/login.jsp` rỗng (form + CSRF; logic ở Phase 1).

**D. Smoke test**
- [ ] `DbHealthServlet` / trang `/health` chạy `SELECT 1` qua pool.
- [ ] `mvn clean package` → deploy Tomcat 10 → login page + health page xanh.

**Tiêu chí Done Phase 0:** skeleton chuẩn MVC chạy trên Tomcat, kết nối DB OK qua pool, master layout + cafe-theme.css + login page rỗng hiển thị, không scriptlet.

---

## Phase 0 — Done (2026-06-29)

- Xoá toàn bộ code webshop cũ; giữ `CharsetFilter` (đã bỏ @WebFilter, chuyển khai báo sang web.xml để kiểm soát thứ tự).
- `pom.xml`: thêm **HikariCP 5.1.0** + slf4j-simple. `db.properties` → `databaseName=CafeChain`.
- `config/DBConnection` (HikariCP pool, `getDataSource()`/`getConnection()`).
- `common/`: `OrderItemStatus` enum, `EventPublisher` (ghi ops.OutboxEvent), `Constants`, `PasswordUtil` (BCrypt), `CsrfUtil`, `SessionUtil`.
- **UI shell Highlands**: `assets/css/cafe-theme.css` (đỏ #A6182B / #7A0F1F + kem + nâu, badge 5 màu chuẩn), layout `header/sidebar/footer/_statusBadge`, `auth/login.jsp`, `index.jsp` router, error 403/404/500.
- `HealthServlet` `/health` (SELECT 1 qua pool) cho smoke test.
- **Build:** `mvn clean package` → `target/cafe-shop.war` OK.
- ⚠️ Chưa runtime-test trên Tomcat (môi trường này không có SQL Server) — cần bạn chạy thử: deploy WAR, mở `/health` (phải "OK"), `/login`.

## Phase 1 — Done (2026-06-29)

- Model `Role`, `Branch`, `User` (kèm roleCode/roleName/branchName join).
- `dao/UserDao` (findByUsername + helper seed hash). `service/AuthService` (BCrypt verify, chặn status≠ACTIVE).
- Controller `LoginServlet` (CSRF + chống session fixation), `LogoutServlet`, `DashboardServlet` (điều hướng theo role).
- Filter chuỗi (web.xml, đúng thứ tự): Charset → **AuthFilter** (whitelist public) → **RbacFilter** (prefix /admin /manager /cashier /barista; ADMIN xem tất cả) → **BranchScopeFilter** (đặt branchId vào request).
- `SeedPasswordListener`: khởi động app tự thay `$2a$placeholder` bằng BCrypt hash mật khẩu mặc định **`123456`** (idempotent).
- Landing 4 role: `admin/manager/cashier/barista/dashboard.jsp`.
- **Build:** WAR OK.

## Phase 2 — Đang làm (lát cắt MVC mẫu xong)

**Lát cắt CRUD mẫu (catalog.Category) — DONE, build OK.** Đây là khuôn để nhân bản cho mọi CRUD khác:
- `model/Category` → `dao/CategoryDao` (list/findById/insert/update/softDelete, nhận Connection) → `service/CategoryService` (mở conn + tx commit/rollback) → `controller/CategoryServlet` (`/admin/categories`, GET list/new/edit, POST save/delete, CSRF + validate server-side) → `views/admin/category-list.jsp` (data-table) + `category-form.jsp` (form-card). Đã gắn link vào sidebar admin.
- Pattern chốt cho replication: **soft-delete** (IsActive=0) để giữ FK; CSRF token mọi form ghi; validate trong controller; JSP chỉ JSTL/EL.

**Đã nhân bản xong (build OK):**
- [x] Category (`/admin/categories`) — lát cắt mẫu
- [x] Ingredient RAW/PREPPED (`/admin/ingredients`)
- [x] Product + dropdown Category + ảnh (`/admin/products`)
- [x] ProductRecipe / BOM (`/admin/recipes` — chọn product → thêm/xoá dòng nguyên liệu)
- [x] Modifier: Group → Option → **IngredientImpact** QtyDelta (`/admin/modifiers`)
- [x] Voucher PERCENT/FIXED + scope CHAIN/BRANCH + ngày + giới hạn (`/admin/vouchers`)
- [x] BranchMenu: publish/giá local/cờ 86 theo chi nhánh (`/admin/branch-menu`)
- [x] IAM: Staff CRUD + reset mật khẩu (`/admin/staff`) · Branch CRUD (`/admin/branches`)

**Carry-over (2 link-table chưa có UI — feed Phase 4, làm trước khi/khi vào Phase 4):**
- [ ] **ProductModifierGroup** — gán nhóm modifier nào áp dụng cho product nào.
- [ ] **PrepRecipe** — công thức pha sẵn (PREPPED từ N RAW + yield) cho PrepBatch.

⚠️ **Khuyến nghị mạnh:** đã có 13 servlet + 33 JSP nhưng **chưa chạy thật** (dev không có SQL Server). Nên smoke-test toàn bộ Admin trên Tomcat 10 + SQL Server trước khi sang Phase 3, để bắt lỗi SQL/JSP sớm.

## ✅ Runtime verification (2026-06-29) — chạy thật trên Tomcat 10 + SQL Edge

**Môi trường chạy thử (local):**
- DB: container Docker `cafechain-sql` (image `azure-sql-edge`, arm64) — cổng host **14333** (vì cổng 1433 đã bị `mycoffee-sql` của bạn chiếm). SA pwd `YourPassword123`. Đã nạp `sql/database.sql` (37 bảng, 4 user seed).
- App: Tomcat 10 (brew) chạy với **CATALINA_BASE riêng** ở scratchpad, cổng **8090**, context `/cafe-shop`.
- `db.properties` đang trỏ `localhost:14333` (đổi lại 1433 hoặc DB của bạn khi chạy môi trường thật).

**Kết quả test (đều PASS):**
- `/health` → `OK - DB pool connected`.
- Login `admin/123456` → 302 → dashboard đúng role. Barista login OK nhưng `/admin/*` → **403** (RBAC đúng).
- 11/11 trang Admin render 200 (kể cả form có dropdown join).
- CRUD ghi thật: tạo Category + Ingredient PREPPED → 302 + có dòng trong DB. CSRF chặn token cũ (403) đúng.

**2 BUG phát hiện & đã sửa nhờ chạy thật (compile không bắt được):**
1. `SeedPasswordListener`/`UserDao.findUsersWithoutRealHash`: lọc `NOT LIKE '$2%'` coi `$2a$placeholder` là hash hợp lệ → không seed mật khẩu → không login được. **Sửa:** lọc `LEN(PasswordHash) < 60` (BCrypt thật = 60 ký tự).
2. CSRF token không tồn tại trong session sau login (login xoay session chống fixation, chỉ `LoginServlet.doGet` tạo token) → mọi form ghi 403. **Sửa:** `BranchScopeFilter` gọi `CsrfUtil.getToken` cho mọi request đã đăng nhập.

**Lệnh chạy lại nhanh:**
```
# DB (1 lần): docker run -d --name cafechain-sql -e ACCEPT_EULA=1 -e MSSQL_SA_PASSWORD=YourPassword123 -p 14333:1433 mcr.microsoft.com/azure-sql-edge
# nạp schema: sqlcmd -S localhost,14333 -U sa -P YourPassword123 -N disable -i sql/database.sql
# build+deploy: mvn clean package -DskipTests; cp target/cafe-shop.war <CATALINA_BASE>/webapps/; catalina start
# URL: http://localhost:8090/cafe-shop/login  (admin|manager1|cashier1|barista1 / 123456)
```

## Phase 2 — Đồng bộ đặc tả `KE_HOACH_CHI_TIET_THEO_ROLE.md` (2026-06-29)

Quyết định user: **giữ `com.cafe` layer-based**, đổi tên/route đúng đặc tả + bù 2 mảng thiếu. Đã copy spec vào repo gốc.

**Đã đổi route (số nhiều → số ít) + class/method theo đặc tả:**
- Auth: gộp `Login/LogoutServlet` → **`AuthServlet`** @ `/auth/login`,`/auth/logout`. `PasswordUtil`→**`PasswordHasher`** (`hashPassword`/`verifyPassword`). `EventPublisher` dùng enum **`EventType`**.
- A1 `StaffServlet/Service`→**`UserServlet`/`UserService`** @ `/admin/user` (`getUserList`/`createUser`/`setUserStatus`/`resetPassword`/`assignBranch` + toggleStatus UI).
- A2 `/admin/branch` · `BranchService` (`getBranchList`/`setBranchActive`/`assignManager`); `BranchDao` (`findAll`/`updateActive`/`updateManager`).
- A3 `/admin/category`,`/admin/product` · method `getXxxList`…; `ProductService.setProductActive`/**`publishToBranch`** (+ UI Publish→chi nhánh); `ProductDao.findByCategory`/`updateActive`.
- A4 `/admin/recipe`,`/ingredient`,`/modifier` · `ProductRecipeService`→**`RecipeService`** (+ **PrepRecipe**: model/DAO/UI `prep-recipe.jsp`); `ModifierDao` tách → **`ModifierGroupDao`/`ModifierOptionDao`/`ModifierIngredientImpactDao`/`ProductModifierGroupDao`**; `ModifierService.assignGroupToProduct` (+ UI `modifier-assign.jsp`).
- A5 `/admin/voucher` · `VoucherService` (`getVoucherList`/`setVoucherActive`/`incrementUsed`/`validateVoucher`); `VoucherDao.findByCode`/`incrementUsed`/`updateActive`.
- DAO chuẩn hoá toàn bộ: `list`→`findAll`, `softDelete`→`delete`/`updateActive`. View `staff-*`→`user-*`, `recipe-form`→`recipe-builder`.

**Bù 2 carry-over A4 (đã xong + verify):** `PrepRecipe` (RAW→PREPPED) · `ProductModifierGroup` (gán nhóm cho product).

**✅ Verify chạy thật trên Tomcat 10 + SQL Edge:** build WAR OK · `/auth/login`+RBAC(403 barista) OK · 10/10 route Admin số ít = 200 · recipe-builder/prep/modifier-assign = 200 · ghi thật DB: addPrep (PrepRecipe), assignGroup (ProductModifierGroup), publishToBranch (BranchMenu) đều persist. Đã dọn row test.

## Phase 3 — Inventory half DONE (2026-06-29) · ledger verify

**Sổ cái tồn kho (contract lõi):** `common/TxnType` enum; model `BranchInventory`/`InventoryTransaction`/`Supplier`/`StockReceipt(+Detail)`/`StockAdjustment`; DAO tương ứng. **`InventoryService.applyTxn(conn, branch, ingredient, delta, TxnType, refTable, refId, userId)`** = INSERT InventoryTransaction + UPSERT BranchInventory + publish `stock.low`, tất cả trong tx của caller. **Không nơi nào UPDATE thẳng BranchInventory ngoài applyTxn.**
- **M6** Supplier (`/manager/supplier`) + Stock Receipt (`/manager/receipt`): tạo nháp → addLine → **confirm = cộng tồn qua ledger + chốt CONFIRMED, nguyên tử** (`StockReceiptService.confirmReceipt` gọi `InventoryService.confirmReceiptStock`).
- **M5** Inventory Dashboard (`/manager/inventory`): tồn theo chi nhánh + low-stock + đặt ngưỡng + **xem sổ cái** từng nguyên liệu.
- **M7** Reconciliation (`/manager/reconciliation`): nhập tồn thực tế → ghi `ADJUST` diff qua ledger.

**✅ Verify chạy thật (manager1, branch 1):** mọi trang manager 200 · receipt confirm → `QtyOnHand=1000 == SUM(ledger)=1000` (1 RECEIPT) · adjust 950 → `QtyOnHand=950 == SUM(ledger)=950` (+ADJUST). **Bất biến cache == tổng sổ cái GIỮ ĐÚNG.**

## Phase 3 — HR half DONE (2026-06-29) · shift-conflict + attendance/payroll verify

**M1 Dashboard** (`/manager/dashboard`, `ManagerDashboardServlet/Service`): thẻ tổng quan (tồn thấp / NV có ca hôm nay / chấm công chờ duyệt) + bảng ca hôm nay + cảnh báo tồn. `DashboardServlet` redirect role MANAGER → `/manager/dashboard`.
**M2 Ca làm** (`/manager/shift`, `ShiftServlet/Service`, `ShiftTemplateDao/ShiftAssignmentDao`): tạo mẫu ca, lịch tuần (template × 7 ngày), xếp/gỡ ca. **★ Shift Conflict Resolver** = `common/ShiftConflict.overlaps()` (logic thuần) + `ShiftService.detectConflict` (so giờ với các ca cùng NV cùng ngày). Chồng giờ → `ShiftConflictException` → flashError, KHÔNG ghi.
**M3 Chấm công** (`/manager/attendance`, `AttendanceServlet/Service/Dao`): tab PENDING/APPROVED/REJECTED, duyệt/từ chối (ghi ApprovedBy), sửa giờ check-in/out, `getWorkHours()` = (out−in).
**M4 Bảng lương** (`/manager/payroll`, `PayrollServlet/Service`): tổng hợp giờ từ chấm công APPROVED theo NV/tháng (`AttendanceDao.aggregateApprovedByMonth`, group SQL), điều hướng tháng.
**M8 Menu chi nhánh** (`/manager/menu`, `ManagerMenuServlet` dùng chung `BranchMenuService`): bật/tắt bán, giá địa phương, cờ 86 — khoá theo branch của manager.

**★ Test trước (logic rủi ro):** `src/test/java/.../ShiftConflictTest` — 5 case (chồng/chứa trọn/chạm biên/rời nhau/null) **PASS 5/5** (`mvn test`). Đã thêm JUnit 5 vào pom.

**✅ Verify chạy thật (Tomcat 8090 + SQL Edge):** 6/6 route manager = 200 · **conflict:** xếp Ca sáng 07–12 OK → xếp Ca 11–15 cùng NV/ngày bị chặn (flashError đúng), badge=1; xếp Ca chiều 13–17 (rời) OK, badge=2 · **attendance:** insert PENDING 07–12 → UI hiện 5.0 giờ → Duyệt → DB `Status=APPROVED, ApprovedBy=2` · **payroll** tháng 2026-06: NV 1 ca / 5.0 giờ · **M8:** toggle available product → `catalog.BranchMenu.IsAvailable` đổi & khôi phục. Sidebar manager đã nối Ca làm/Chấm công/Bảng lương/Menu (bỏ "soon").

**Dữ liệu demo để review còn lại trong DB:** 3 ShiftTemplate (Ca sáng/Ca 11-15/Ca chiều), 2 ShiftAssignment (NV manager1, 30/06), 1 Attendance APPROVED 5.0h.

**→ Phase 3 (M1–M8) HOÀN TẤT. Dừng chờ duyệt trước khi sang Phase 4 (Sales + KDS).**

## Phase 4 — Sales + KDS DONE (2026-06-29) · ★ Modifier-Aware Auto-Deduction verify

**★ Lõi rủi ro — test TRƯỚC:** `common/DeductionCalculator.computeRequired(recipe, impacts, qty)` (logic thuần) + `DeductionCalculatorTest` **4/4 PASS** (gồm test BẮT BUỘC Cold Brew PREPPED chỉ trừ Cold Brew, không trừ cà phê hạt lần 2). Tổng test toàn dự án **9/9**.

**Sales backbone (Cashier sở hữu order entry — Contract #1, #3):**
- model `DiningTable/TableSession/Order/OrderItem/OrderItemModifier/PrepBatch/WasteLog` · DAO tương ứng.
- **`OrderService`**: `placeOrder` (tạo Order+Item(WAITING)+Modifier, giá = giá menu chi nhánh + Σ priceDelta, publish `order.created`, 1 tx) · `startItem`(→MAKING) · **`markItemReady`** (★ auto-deduct + READY trong CÙNG 1 tx) · `markItemServed` · `getKdsQueue/getReadyItems/getSessionItemStatuses`.
- **`InventoryService.deductForOrderItem`** = đọc ProductRecipe + OrderItemModifier→ModifierIngredientImpact, tính qua DeductionCalculator, `applyTxn(-qty, DEDUCT)` từng ingredient, publish `inventory.deducted`. **PREPPED trừ tồn PREPPED, KHÔNG trừ RAW lần 2.**
- **`InventoryService.createPrepBatch`** (Contract #2 — nơi DUY NHẤT RAW→PREPPED): PREP_OUT raw theo PrepRecipe (consumed=qty/yield×qtyPer), PREP_IN prepped, 1 tx · **`logWaste`** (WASTE + ledger).
- `TableSessionService` (sơ đồ bàn, mở/đóng/gộp phiên) · `KdsService/PrepService/WasteService` (thin) · `CatalogReadService.getPosMenu` (món available chưa-86 + nhóm modifier) · `BranchMenuService.set86`.

**Màn (C2,C3 Cashier · B1–B5 Barista):** `/cashier/table` (sơ đồ bàn), `/cashier/pos` (POS giỏ JS→submit JSON), `/barista/kds` (hàng chờ, polling 5s, nút Xong=auto-deduct), `/barista/pickup` (READY→SERVED), `/barista/eightysix` (86 khoá POS+QR), `/barista/prep` (pha sẵn), `/barista/waste` (hao hụt). Sidebar cashier/barista đã nối (bỏ "soon").

**✅ Verify chạy thật (Tomcat 8090 + SQL Edge):**
- POS đặt **Cà phê sữa + "Thêm shot"** → đơn #1, UnitPrice 37000 (=29000+8000), `order.created` ✓.
- Barista start→**Xong**: ledger `DEDUCT` cà phê **-36g** (18 công thức + **18 modifier**) + sữa -30ml ✓ **modifier-aware**; item READY+DoneAt; events `order.status_changed`(MAKING)/`inventory.deducted`/`order.status_changed`(READY) ✓.
- **Test BẮT BUỘC Cold Brew (#2):** đặt Cold Brew → Xong → trừ Cold Brew(ing6) -180 + đá(ing4) -100, **KHÔNG trừ cà phê hạt(ing1)** ✓.
- **PrepBatch (Contract #2):** pha Syrup Đào 800 → PREP_IN ing7 +800, PREP_OUT đường -300 + đào -500 (1 tx) ✓.
- **Waste:** spill đá 50 → WASTE -50 ✓. **86:** 86 Trà Đào → biến mất khỏi POS menu (1,2,3→1,2), un-86 khôi phục ✓.
- **Bất biến cache==SUM(ledger): OK toàn bộ 7 ingredient.** (vài ingredient âm do chưa nhập tồn đầu — không có stock-guard ở phase này, sổ cái vẫn nhất quán.)

**Bug bắt nhờ chạy thật:** `kds.jsp` dùng `<c:forEach var="mod">` → `${mod.optionName}` lỗi parse vì **`mod` là toán tử EL (modulo)**. Sửa: đổi biến `mod`→`om`.

**Carry-over Phase 4 (đẩy sang/làm cùng phase sau):** C4 Order Inbox (gác đơn QR) → Phase 6; B6 Recipe lookup (read-only) + B7 Shift Handover (cần bảng `hr.ShiftHandover` mới) — chưa làm.

**Dữ liệu demo còn trong DB:** bàn 1 OPEN (phiên #1), đơn #1 (Cà phê sữa+shot, READY) & #2 (Cold Brew, READY), 2 PrepBatch, 1 WasteLog.

**→ Phase 4 HOÀN TẤT. Dừng chờ duyệt trước khi sang Phase 5 (Payment: Cashier shift, Checkout, Split/Merge bill, voucher, payment.completed).**

## Phase 5 — Payment DONE (2026-06-29) · ★ Dynamic Bill Splitting + payment.completed

**★ Lõi rủi ro — test TRƯỚC:** `common/BillCalculator` (logic thuần: discount PERCENT/FIXED kẹp [0,subtotal], VAT 8%, total=net+vat) + `BillCalculatorTest` **6/6 PASS**. Tổng test dự án **15/15**.

**C1 Ca thu ngân** (`/cashier/shift`, `CashierShiftServlet/Service/Dao`): mở/đóng ca (idempotent), báo cáo ca (số bill + tổng thu PAID).
**C5 Checkout + ★ Tách/Gộp bill** (`/cashier/checkout`, `CheckoutServlet`, **`BillingService`**, `BillDao/BillItemDao/VoucherRedemptionDao`):
- `buildSessionBill` — dồn mọi OrderItem chưa-bill của phiên vào 1 bill UNPAID, tính subtotal/VAT.
- `splitItems` — chuyển dòng đã chọn sang bill MỚI (giữ UNIQUE OrderItemId); `mergeBills` — dồn nhiều bill về 1, void bill rỗng.
- `applyVoucher` (gọi `VoucherService.validateVoucher` — voucher 1 nguồn `payment.Voucher`) → tính giảm qua BillCalculator.
- **`payBill`** (Contract #3): markPaid có guard `WHERE Status='UNPAID'` (chống double-pay) → tăng `Voucher.UsedCount` + ghi `VoucherRedemption` + publish **`payment.completed`** + nếu phiên hết bill UNPAID → đóng phiên + trả bàn EMPTY. Tất cả 1 tx.
**C6 Lịch sử HĐ** (`/cashier/history`, `BillHistoryServlet`): danh sách 100 bill gần nhất + xem chi tiết + void.

**✅ Verify chạy thật (cashier1, Tomcat 8090 + SQL Edge):**
- Mở ca quỹ 500k · build bill phiên 1 (5 món) → subtotal 195000, VAT 8% 15600, **total 210600** ✓.
- **★ Tách:** chọn 2 món → bill mới: bill#1[3 món]=111000, bill#2[2 món]=84000 ✓.
- **Voucher GRANDOPENING 20%** lên bill#1: discount 22200, VAT trên net 7104, **total 95904** ✓.
- **Thanh toán** bill#1 CASH + bill#2 TRANSFER → 2 `payment.completed`, `Voucher.UsedCount`=1, `VoucherRedemption`(22200) ✓; phiên 1 → CLOSED, bàn trả EMPTY ✓.
- **Chống double-pay:** thu lại bill#1 → không tạo event thứ 3 (guard `WHERE Status='UNPAID'`) ✓.
- **Báo cáo ca:** 2 bill / thu 186624 ✓ · 5/5 route payment = 200 · **bất biến tồn kho không đổi (payment không đụng inventory): 0 mismatch.**

**Carry-over:** thanh toán đơn TAKEAWAY (không phiên bàn) — checkout hiện theo phiên; bổ sung khi cần. `mergeBills` đã code (chung primitive `reassign` với split đã verify) nhưng chưa test live riêng.

**Dữ liệu demo trong DB:** ca #1 đang mở, bill #1 (PAID CASH, có voucher) + #2 (PAID TRANSFER), VoucherRedemption.

**→ Phase 5 HOÀN TẤT. Dừng chờ duyệt trước khi sang Phase 6 (Customer QR app: scan→menu→cart→đặt món reuse OrderService→tracking, mobile-first).**

## Phase 6 — Customer QR app DONE (2026-06-29) · ẩn danh, mobile-first, reuse OrderService

**Contract #1 (order.created) + #3 (đơn QR & quầy CÙNG bảng/service):** QR DÙNG LẠI `OrderService.placeOrder(source=QR)` — KHÔNG nhân bản logic đặt đơn.

**C7 QR Menu & Đặt món** (`/qr/menu?t={qrCode}`, `QrMenuServlet`, `QrOrderService`):
- `identifyTable(qrCode)` → `DiningTableDao.findByQrCode` + mở/lấy **phiên ẩn danh** (OpenedBy=NULL) qua `TableSessionService.openSession`.
- `getMenu` = `CatalogReadService.getPosMenu` (món available, chưa 86) · `placeCustomerOrder` → `OrderService.placeOrder(source=QR, createdBy=null)`.
- View `customer/menu.jsp` mobile-first (giỏ JS → submit JSON), `customer/invalid.jsp` (QR sai).
**C8 QR Tracking** (`/qr/track?s={sessionId}`, `QrTrackServlet`):
- `getSessionStatuses` → `OrderService.getSessionItemStatuses` · AJAX polling 5s (`?action=status` trả JSON).
- `callStaff`/`requestBill` → publish `service.call`/`bill.requested` (không bảng mới) · View `customer/track.jsp` (badge dùng chung `_statusBadge`).

**Hạ tầng:** `/qr/*` đã whitelist ở `AuthFilter` (không cần login) + không khớp RbacFilter (không role). CSRF: `QrMenuServlet`/`QrTrackServlet.doGet` gọi `CsrfUtil.getToken` seed token vào **session ẩn danh** → form/JSON POST gửi `_csrf`. EventType `SERVICE_CALL`/`BILL_REQUESTED` đã có sẵn.

**✅ Verify chạy thật (KHÁCH ẩn danh, không login):**
- Quét `QR-CN01-T01` → 200, mở phiên ẩn danh #3 (OpenedBy=NULL, bàn 1) ✓.
- Đặt Trà Đào ×2 → order #4 **Source=QR, CreatedBy=NULL**, `order.created` ✓.
- Track 200 · AJAX `[{name:"Trà Đào",qty:2,status:"WAITING"}]` ✓ · callStaff→`service.call`, requestBill→`bill.requested` ✓.
- QR sai (`BADCODE`) → 404 + trang invalid ✓.
- **★ Hợp nhất pipeline:** đơn QR #4 hiện trong **hàng chờ KDS của barista** (Trà Đào) — khách QR & quầy đi cùng một KDS/auto-deduct ✓.

**Dữ liệu demo:** phiên ẩn danh #3 (bàn 1, OPEN) + order QR #4 (Trà Đào ×2, WAITING). **Lưu ý:** seed có sẵn 4 QR (`QR-CN01-T01..T04`).

**→ Phase 6 HOÀN TẤT. Còn lại Phase 7 (Report & Polish: dashboard doanh thu, golden-path E2E, fix bug).**

## Phase 7 — Report & Polish DONE (2026-06-29) · 🎉 HOÀN TẤT DỰ ÁN (Phase 0–7)

**Dashboard doanh thu toàn chuỗi** (`/admin/report`, `ReportServlet/Service/Dao` — Admin xem chéo mọi chi nhánh, chỉ tính bill `PAID`):
- `chainSummary`: tổng/hôm nay (doanh thu + số HĐ), tổng giảm giá voucher, tổng VAT đã thu.
- `revenueByBranch` (GROUP BY chi nhánh) · `topProducts` (TOP 10 JOIN BillItem→OrderItem→Product) · `paymentBreakdown` (GROUP BY hình thức).
- Admin dashboard hiển thị số liệu thật + link báo cáo. Sidebar Admin nối "Doanh thu toàn chuỗi" (bỏ "soon").

**✅ Verify chạy thật (admin):** trang report 200, **khớp DB tuyệt đối**: doanh thu 186624 (2 bill) · CASH 95904 + TRANSFER 90720 · Top: Cold Brew 90000 / Cà phê sữa 66000 / Trà Đào 39000.

**✅ Golden-path E2E (toàn hệ thống):** **26/26 route 200** (Admin 6 · Manager 6 · Cashier 5 · Barista 5 · QR 2 ẩn danh) · **bất biến sổ cái tồn kho: 0 mismatch** · **15/15 unit test** (deduction modifier-aware, shift conflict, bill calculator).

**Tổng kết kiến trúc đã chạy thật:** JSP+Servlet+JSTL (0 scriptlet) · MVC controller→service→dao · HikariCP · 3 filter (Auth/Rbac/BranchScope) + CSRF · HttpSession auth · 4 role landing riêng · **3 contract giữ nguyên xuyên suốt**: (1) OutboxEvent + OrderItemStatus enum chung; (2) RAW/PREPPED không double-count (test bắt buộc Cold Brew pass); (3) Cashier/QR cùng OrderService/Orders. **Mọi đổi tồn qua `InventoryService.applyTxn` + sổ cái — invariant cache==Σ(ledger) luôn đúng.**

**Carry-over (ngoài phạm vi 7 phase lõi, làm khi cần):** B6 Recipe lookup · B7 Shift Handover (cần bảng `hr.ShiftHandover`) · C4 Order Inbox (gác đơn QR) · thanh toán TAKEAWAY · test `mergeBills` live riêng · seed BCrypt thật cho user (đang dùng listener seed "123456").

**🎉 DỰ ÁN HOÀN TẤT — Phase 0→7 đều chạy được + verify thật trên Tomcat 10 + SQL Edge.**

**Còn lại Phase 3 (HR + M8):** M1 Manager Dashboard, M2 Shift + conflict resolver, M3 Attendance, M4 Payroll, M8 Branch Menu config (`/manager/menu`).

- **2026-07-01** — **Gộp SQL về 1 file + làm giàu data demo (yêu cầu user, 0 đổi schema/code):**
  - Gộp 10 file SQL (`database.sql` + 6 `migration_*` + thư mục `migrations/` + 3 `seed_*`) → **1 file duy nhất `sql/database.sql`** (PART A schema · PART B catalog · PART C demo). Đã verify mọi migration nằm sẵn trong schema nên xoá an toàn.
  - **Viết lại PART B:** catalog **15 món**, mọi món đủ `ProductRecipe` + Size/Đường/Đá/Topping + ảnh Unsplash (verify 200).
  - **Viết lại PART C:** **3 chi nhánh, 16 user** (BCrypt `123456`), nhập kho lớn + pha sẵn, **generator 31 ngày** sinh **792 hoá đơn PAID** (trừ kho theo công thức) + story hôm nay đủ mọi role (KDS WAITING/MAKING/READY, ca mở, waste/adjust, 86, handover), voucher/payroll 3 tháng/outbox.
  - **Verify DB thật:** rebuild OK, **bất biến sổ cái = 0 mismatch**, 0 tồn âm, low-stock 6 dòng, KDS 12 item, doanh thu ~33M/CN × 3. Chưa runtime-test Tomcat đợt này.
- **2026-06-30** — **Seed demo 1 luồng hoàn chỉnh (`sql/seed_demo.sql`)** — yêu cầu user "thêm db toàn bộ để thành 1 luồng + demo". Không đổi code.
  - Dựng lại DB sạch: `DROP DATABASE CafeChain` → `database.sql` (schema + seed gốc) → `seed_demo.sql` (câu chuyện vận hành đầy đủ). Nạp vào container `cafechain-sql` (cổng 14333).
  - **Phủ kín mọi role:** 9 user (thêm cashier2/barista2 CN01; **CN02 Thủ Đức** + manager2/cashier3/barista3) — tất cả mật khẩu `123456` (hash BCrypt thật bake sẵn, không cần listener). 2 NCC, 4 phiếu nhập (3 CONFIRMED + 1 DRAFT), 4 PrepBatch, 1 Waste, 1 Adjust; ca làm + chấm công (2 APPROVED→bảng lương, 2 PENDING chờ duyệt) + Payroll tháng trước + ShiftHandover; phiên A(đóng, 2 bill PAID + voucher), B(mở: READY/MAKING/WAITING→KDS+pickup), C(QR ẩn danh→tracking), D(CN02 đóng, 1 bill PAID). Doanh thu chuỗi CN01 114.264 + CN02 79.920. 7 OutboxEvent.
  - **Bất biến `BranchInventory == Σ ledger` = 0 mismatch** (cuối script dựng lại BranchInventory từ tổng InventoryTransaction). Low-stock: Cold Brew CN01, Đường/Đào CN02.
  - Lưu ý môi trường: Azure SQL Edge **tắt CLR** → seed không dùng `FORMAT()` (thay bằng `CONVERT(...,23)`). App đang chạy ROOT http://localhost:8080 (pool đã nối).

## Điều chỉnh so với plan (đã thống nhất với user)

- **Giao diện theo phong cách Highlands Coffee** (đỏ trầm) thay bảng màu espresso/coffee gốc trong mục 4 của plan. Giữ nguyên *cấu trúc* design token + 5 màu status badge để contract trạng thái không đổi.

## Nhật ký

- **2026-06-29** — Gap analysis + chốt 4 quyết định; tạo `CLAUDE.md` + `docs/PROGRESS.md`.
- **2026-06-29** — Hoàn tất **Phase 0** (nền tảng + UI shell Highlands) và **Phase 1** (Auth & RBAC). Build WAR thành công. Bắt đầu **Phase 2**.
- **2026-06-29** — Hoàn tất **Phase 2** (Catalog & Config): 8 nhóm CRUD. Build WAR OK.
- **2026-06-29** — **Đồng bộ Phase 2 theo `KE_HOACH_CHI_TIET_THEO_ROLE.md`**: đổi route/class/method đúng đặc tả (giữ com.cafe), gộp AuthServlet, PasswordHasher, EventType, tách 4 Modifier DAO, bù PrepRecipe + ProductModifierGroup. Dựng SQL Edge + Tomcat 10 chạy thật, verify toàn bộ route mới + ghi DB OK. Sửa 2 bug runtime trước đó (seed password LEN<60, CSRF sau login).
- **2026-06-29** — **Làm lại UI (premium cafe, theo phản hồi user)**: viết lại `cafe-theme.css` (tông rượu vang + kem + vàng đồng, serif Playfair cho tiêu đề), **bỏ emoji** ở sidebar/dashboard, sidebar có nhãn nhóm + thanh nhấn vàng ở mục active, topbar có avatar chữ cái, login đổi sang **split brand-panel** có tagline. Build + redeploy + verify render OK.
- **2026-06-29** — **FIX SAU AUDIT** (kế hoạch: `docs/KE_HOACH_FIX_AUDIT.md`). Mỗi mục compile + commit riêng:
  - **Ưu tiên 1 — BARISTA đạt chuẩn (5 màn/0 CRUD → 7 màn/2 CRUD):**
    - B6 Recipe Lookup `/barista/recipe` (CatalogReadService.getRecipeForProduct/getPrepRecipe/getModifierImpactsForProduct).
    - B7 Shift Handover `/barista/handover` + bảng `hr.ShiftHandover` + KPI lead-time (OrderItemDao.leadTimeStatsToday).
    - B4 Prep **update/cancel** = CRUD #1; B5 Waste **update/void** = CRUD #2 — **hoàn kho bằng TXN BÙ** qua InventoryService (đảo PREP_IN/PREP_OUT, +qty WASTE), giữ row + Status CANCELLED/VOIDED, KHÔNG hard-delete, KHÔNG UPDATE thẳng tồn (contract C4 giữ vững).
  - **Ưu tiên 2 — Cashier Order Inbox** `/cashier/inbox`: OrderService.getIncomingOrders/voidOrder. Mô hình **giám sát + void đơn sai**, KHÔNG chặn luồng auto-to-KDS.
  - **Ưu tiên 3 — bug thật:** (a) sửa "toggle giả" Branch/Product/Voucher/Supplier (đọc trạng thái rồi đảo, JSP hiện nút 2 chiều); (b) **Split bill no-drift**: BillCalculator.allocateByWeight (largest-remainder) + recompute mức-phiên — VAT & discount tính 1 lần trên tổng tab rồi phân bổ theo tỷ lệ → tổng các bill tách == bản tính cả tab.
  - **Schema additive:** `inventory.PrepBatch`+Status/VoidedAt, `inventory.WasteLog`+Status/VoidedAt, bảng `hr.ShiftHandover` — cập nhật `sql/database.sql` + `sql/migration_audit_fix.sql` (idempotent).
  - **Test:** 20/20 pass (BillCalculator +6 test no-drift). Barista đủ 7 màn, 2 CRUD đầy đủ.
  - **Carry-over (Ưu tiên 4, làm khi còn thời gian):** KDS polling 3–5s · M1 doanh thu hôm nay · M4 export Excel · A5 Recipe updateLine · Modifier delete-group/update-option · B1 bump · C6 lọc-theo-ca/reprint/void-có-lý-do · A1 forgot password. **Skip có chủ đích:** quick-create customer (A1.F4), refactor status enum (cosmetic).
- **2026-06-29** — **Ưu tiên 4 (đã làm theo yêu cầu user):**
  - KDS/Pickup polling 5s mượt hơn (đếm ngược + pause khi tab nền) — *phát hiện đã có sẵn setTimeout-reload, audit grep `setInterval` nên báo nhầm thiếu*.
  - **Nhóm A — C6 Bill History:** void/refund **kèm lý do (bắt buộc) + ghi log** `ops.OutboxEvent` (bill.voided); lịch sử **lọc theo ca** hiện tại (toggle toàn chi nhánh); nút **In/Tái in** (window.print + @media print).
  - **Nhóm B — Manager:** M1 **doanh thu hôm nay** (card dashboard, BillDao.sumPaidToday); M4 **export CSV/Excel** bảng lương (BOM UTF-8).
  - **Nhóm C — CRUD còn thiếu:** A5 Recipe **updateLine** (sửa định mức); Modifier **delete-group** (dọn dependent 1 tx) + **update-option**; B1 **KDS bump** (cột `sales.OrderItem.Priority` additive, sort Priority DESC).
  - Schema bổ sung S4: `sales.OrderItem.Priority` (database.sql + migration). Test 20/20, WAR build OK.
  - **Còn lại chưa làm:** A1 forgot/reset password (chưa cần); refund bill đã PAID (chỉ void UNPAID kèm lý do). **Skip:** quick-create customer, refactor status enum.
- **2026-06-29** — **VERIFY CHẠY THẬT trên Tomcat 10 + SQL (cafechain-sql)** sau đợt fix:
  - Chạy `sql/migration_audit_fix.sql` (sửa lỗi CHECK cùng-batch → inline). Deploy WAR mới làm ROOT.
  - **Gỡ webapp `manager` mặc định của Tomcat** — nó chiếm context `/manager` che `/manager/*` của app (gây 404, không phải bug code).
  - E2E thật (login 4 role): **T1** Prep cancel txn-bù (tồn về đúng, Status=CANCELLED, không hard-delete); **T2** Waste void txn-bù; **T3** KDS markReady deduct (2 DEDUCT txns); **T4** Split bill no-drift + voucher 20% — tổng phiên TRƯỚC==SAU (122688), discount/VAT phân bổ đúng tỷ lệ, voucher gắn 1 bill (đếm lượt 1 lần); **T5** Inbox void order; **T6** C6 void bill kèm lý do → `ops.OutboxEvent(bill.voided)`.
  - **Invariant sổ cái `BranchInventory == Σ(InventoryTransaction)`: 0 mismatch** trước & sau toàn bộ. **0 bug.**
  - App chạy http://localhost:8080 (ROOT). Tài khoản seed: admin/manager1/cashier1/barista1 · mật khẩu `123456`.
- **2026-06-29** — **Hoàn thiện 🟡 các tính năng CHƯA LÀM còn lại (từ audit)** — không cần đổi schema (cột đã có sẵn) trừ refund:
  - **B3.F3 · 86 ETA:** Barista báo hết kèm "dự kiến có lại" (`catalog.BranchMenu.BackInEta`); mở bán lại tự xoá ETA. Model+DAO+service+JSP datetime-local.
  - **M3.F4 · Chấm công Trễ/Về sớm:** tính read-only (so `CheckInAt` vs `ShiftTemplate.StartTime`, `CheckOutAt` vs `EndTime`) → badge "Trễ X'" / "Sớm X'". Không đổi schema.
  - **A3.F2 + A2.F6 · Branch giờ mở-đóng + gán Manager:** `org.Branch.OpenTime/CloseTime/ManagerUserId` (đã có cột) nối vào model/DAO/form/list; dropdown Manager = user role `BRANCH_MANAGER`.
  - **A2 · Lọc nhân sự:** `/admin/user?roleId&branchId` (UserDao.findFiltered) + bộ lọc 2 dropdown trên list.
  - **A1 · Quên mật khẩu tự phục vụ:** `/auth/forgot` (whitelist AuthFilter) — xác minh username+email khớp tài khoản ACTIVE rồi đặt lại mật khẩu (BCrypt, 1 tx); link từ trang login + flash thành công.
  - **Refund hoá đơn ĐÃ PAID:** thêm `'REFUND'` vào `CK_Bill_Status` (migration S5, VARCHAR(8) đủ chứa); `BillingService.refundBill` PAID→REFUND kèm lý do bắt buộc + log `ops.OutboxEvent(bill.refunded)`, chống hoàn 2 lần bằng `WHERE Status='PAID'`.
  - **Verify chạy thật (Tomcat 10 + DB, login 4 role):** F1 ETA set→NULL khi mở lại ✅ · F2 trang 200 ✅ · F3 DB open=07:00/close=22:30/mgr=2 ✅ · F4 lọc BARISTA hiện barista1, ẩn cashier1 ✅ · F5 sai email bị từ chối, đúng→đặt lại→đăng nhập bằng pass mới OK ✅ · F6 bill PAID→REFUND + event bill.refunded, refund lần 2 không đổi (idempotent) ✅. Test 20/20, WAR build OK.
- **2026-06-29** — **Tinh chỉnh role Cashier (yêu cầu user, 5 mục — 0 đổi schema):**
  - **R1 · Ca thu ngân — doanh thu theo ngày:** `BillDao.countPaidToday`; `CashierShiftService.getTodayRevenue/getTodayBillCount`; `shift.jsp` thêm 2 thẻ "Doanh thu hôm nay" + "Số HĐ đã thu".
  - **R2 · Dashboard cashier có số liệu:** servlet mới `CashierDashboardServlet` @ `/cashier/dashboard`; `DashboardServlet` redirect role CASHIER sang đó (giống manager); `dashboard.jsp` thêm thẻ **Doanh thu hôm nay** + **Số đơn đã thực hiện** (= số bill PAID hôm nay) + lối tắt.
  - **R3 · Đơn Đến hiện trạng thái thanh toán tổng đơn:** `Order.paymentStatus` (transient); `BillDao.findStatusesBySession`; `OrderService.getIncomingOrders` suy trạng thái theo phiên — **Đã thanh toán** (PAID & hết UNPAID) · **Lỗi thanh toán** (VOID/REFUND, chưa thu được) · **Đang thanh toán** (còn lại). `inbox.jsp` render badge 3 màu. *(Không có cổng thanh toán → ánh xạ VOID/REFUND = "lỗi" là hợp lý nhất, không đổi schema.)*
  - **R4 · Lịch sử HĐ rõ CK & tiền mặt:** `bill-history.jsp` đổi nhãn `CASH→Tiền mặt`, `TRANSFER→Chuyển khoản`, `QR_BANK→QR ngân hàng` + bộ lọc theo hình thức (giữ phạm vi ca/chi nhánh); `BillHistoryServlet.filterByMethod`.
  - **R5 · Chặn huỷ đơn khi barista đã pha (Inbox + QR khách):** `OrderService.voidOrder` đổi trả `boolean` + **guard**: chỉ huỷ khi mọi món còn WAITING; có món MAKING/READY/SERVED → false (không đổi gì). `Order.isCancellable()`. Inbox: nút "Huỷ đơn" chỉ hiện khi `cancellable`, else "Đang/đã pha — không thể huỷ" + flashError. **Khách QR:** `QrOrderService.getCancellableOrders/cancelOrder`, `QrTrackServlet action=cancel`, `track.jsp` nút "Huỷ đơn" tự ẩn khi đã pha (server vẫn là nguồn chân lý).
  - **Build + Test:** `mvn clean package` WAR OK · **20/20 unit test PASS** (logic thuần không đụng). ⚠️ Chưa runtime-test trên Tomcat đợt này — cần deploy thử để verify badge/luồng huỷ.
- **2026-06-29** — **Manager · Màn Nhập kho — cải tiến nhập liệu (yêu cầu user, 5 mục):**
  - **Schema (additive):** `inventory.StockReceiptDetail.Unit NVARCHAR(20) NULL` — đơn vị nhập per-line (vd "Túi"); NULL = fallback đơn vị gốc nguyên liệu. **KHÔNG ảnh hưởng sổ cái** (tồn vẫn cộng theo Quantity). File: `database.sql` + `sql/migration_receipt_unit.sql` (idempotent `IF COL_LENGTH ... IS NULL ALTER ADD`).
  - **#1 Dropdown + đơn vị:** dropdown "Thêm dòng" bỏ hậu tố `(g/ml…)`, chỉ còn tên; thêm ô text **Đơn vị** tự điền theo nguyên liệu chọn (JS `data-unit`), sửa được. Lưu vào `StockReceiptDetail.Unit`, hiển thị qua `getDisplayUnit()`.
  - **#2/#3 Step:** ô Số lượng `step=0.001→5`; ô Đơn giá `step=100→5000` (mũi tên tăng/giảm theo 5 / 5000).
  - **#4 Nhập nhiều cùng lúc:** bảng tickbox liệt kê mọi nguyên liệu (checkbox + đơn vị + SL + đơn giá), nút "Thêm các mục đã chọn" → action `addLines` (bỏ qua dòng SL≤0), service `addReceiptLines` 1 transaction.
  - **#5 Huỷ nhiều phiếu:** list bọc form + cột checkbox **chỉ ở phiếu DRAFT** + "Huỷ phiếu đã chọn" → action `cancelMany`, service `cancelManyReceipts` (DAO vẫn guard `Status='DRAFT'` — phiếu CONFIRMED không bị huỷ).
  - **Build + Test:** WAR OK · 20/20 test PASS. ⚠️ **Phải chạy `sql/migration_receipt_unit.sql`** trên DB hiện có trước khi deploy (nếu không, query có cột `Unit` sẽ lỗi). Chưa runtime-test Tomcat đợt này.
- **2026-06-29** — **Manager · Nhà cung cấp + Đối soát tồn (yêu cầu user):**
  - **Nhà cung cấp — required SDT/Tên/Địa chỉ:** `supplier-form.jsp` thêm `required` cho phone & address (+ nhãn *); `SupplierServlet` validate server-side cả 3 trường (báo lỗi cụ thể, giữ dữ liệu đã nhập).
  - **Đối soát tồn — kiểm kê tickbox nhiều nguyên liệu + đơn vị tự nhập:**
    - **Schema (additive):** `inventory.StockAdjustment.Unit NVARCHAR(20) NULL` — đơn vị đếm per-line (vd "Túi" cho đá). NULL = fallback đơn vị gốc. **KHÔNG ảnh hưởng sổ cái** (chênh lệch vẫn = ActualQty−SystemQty). File: `database.sql` + `sql/migration_adjustment_unit.sql` (idempotent).
    - `reconciliation-form.jsp` đổi từ chọn-1-nguyên-liệu sang **bảng tickbox**: mỗi nguyên liệu 1 dòng (checkbox + đơn vị sửa được + tồn thực tế + lý do), bỏ hiển thị "(g)" gán sẵn; checkbox "chọn tất cả".
    - `ReconciliationServlet.doPost` parse nhiều dòng (`pick`/`actual_<id>`/`reason_<id>`/`unit_<id>`), bỏ qua dòng chưa nhập tồn; `StockAdjustmentService.createAdjustments` + `InventoryService.createAdjustments` ghi **tất cả trong 1 transaction** (tái dùng `applyAdjustmentLine` → vẫn qua `applyTxn`/sổ cái). `reconciliation-list.jsp` hiển thị `displayUnit`.
  - **Build + Test:** WAR OK · 20/20 test PASS. ⚠️ **Phải chạy `sql/migration_adjustment_unit.sql`** trước khi deploy. Chưa runtime-test Tomcat đợt này.
- **2026-06-30** — **Manager · Chấm công + Bảng lương + Menu chi nhánh (yêu cầu user):**
  - **Chấm công — gộp 1 màn + tickbox ✓ xanh:** bỏ 3 tab (Chờ/Đã duyệt/Từ chối) → 1 danh sách tất cả NV của cơ sở; mỗi dòng có **checkbox accent xanh** = duyệt. `AttendanceDao` thêm join Role/Branch + cột `Phone/RoleName/BranchName`, `findByBranch`, `updateApproval` (ApprovedBy nullable). `AttendanceService.setApprovalStates` (tick→APPROVED ghi người duyệt, bỏ tick→PENDING xoá người duyệt, 1 tx) + `reopenAttendance`. Servlet action `approveMany` (shown[]/approve[]) + giữ reject/edit/reopen. JSP hiện rõ **ngày-giờ ca, tên + role + SĐT, cơ sở**; checkbox dùng `form="bulkAtt"` để không lồng form với form sửa giờ/từ chối.
  - **Bảng lương — lương/giờ + sửa giờ & lương + thành tiền + Excel:** **bảng mới `hr.Payroll`** (userId, PayMonth, WorkedHours, HourlyRate, UNIQUE(user,month)) — `database.sql` + `sql/migration_payroll.sql`. `Payroll` model + `PayrollDao` (findByMonth→Map, upsert UPDATE-rồi-INSERT). `PayrollService.getMonthlyPayroll` lấy giờ từ chấm công APPROVED làm mặc định **overlay** giờ/lương đã chốt; `savePayroll` upsert từng NV 1 tx. `PayrollRow` thêm hourlyRate + `getSalary()` (giờ×lương). Servlet thêm `doPost` save + export CSV bổ sung cột Lương/giờ + Thành tiền. JSP: input sửa **giờ làm** + **lương/giờ** từng dòng, **thành tiền tính sống bằng JS**, tổng cộng, nút Lưu + Xuất Excel.
  - **Menu chi nhánh — ẩn nhiều món cùng lúc:** `BranchMenuService.hideMany` (set IsAvailable=0 cho các productId tick, giữ giá địa phương & cờ 86, 1 tx). `ManagerMenuServlet` action `hideMany` (pick[]). JSP thêm cột checkbox (chỉ ở món đang bán) + "chọn tất cả" + nút "Ẩn các món đã chọn" (dùng `form="bulkHide"`).
  - **Build + Test:** WAR OK · 20/20 test PASS. ⚠️ **Phải chạy `sql/migration_payroll.sql`** trước khi deploy. Chưa runtime-test Tomcat đợt này.
- **2026-07-15** — **Barista · Bổ sung unit test cho logic thuần (không đổi `src/main`, không đổi schema):**
  - **Bối cảnh:** module Barista (8 màn B1–B7 + dashboard) đã Done từ trước; đợt này chỉ **thêm test** để phủ logic rủi ro chưa có test, tăng độ tin cậy khi refactor. Toàn bộ file nằm trong `src/test/…` → không ảnh hưởng runtime/việc role khác.
  - **`WasteSummaryTest`** (`service/barista`, 5 test) — tổng hợp hao hụt/làm lại: bỏ dòng VOIDED, tách hao hụt nguyên liệu vs REMAKE (đếm + chi phí), dòng thiếu giá (`unitCost=null`) chỉ đếm không cộng tổng, **top nguyên liệu** = tốn nhất sau khi gộp.
  - **`WasteScopeTest`** (`service/barista`, 3 test) — cửa sổ lọc: `TODAY` đúng 24h với mốc đầu = 00:00 giờ VN quy về UTC; ca đang mở giữ `checkIn`/`to=null`; ca đã tan giữ cặp `checkIn`/`checkOut`.
  - **`DeductionCalculatorTest`** — mở rộng +4 ca (4→8 test): modifier giảm nhưng net vẫn dương, gộp 2 modifier cùng nguyên liệu, `recipe=null`/`impacts=null` không NPE.
  - **Kết quả:** 3 test class chạy độc lập PASS (WasteSummary 5, WasteScope 3, Deduction 8). 3 commit `test(barista): …` cộng thêm (giữ nguyên history cũ, không rewrite).
- **2026-07-16** — **Barista · Ngày 1 plan — Widget Hao hụt hôm nay trên dashboard:** thêm `WasteService.getTodayWasteSummary(branchId)`, dashboard servlet nạp `wasteSummary`, `dashboard.jsp` hiển thị thẻ tổng chi phí/top nguyên liệu/số làm lại và mở rộng `WasteSummaryTest` lên 6 test. Verify: `mvn test -Dtest='WasteSummaryTest'` PASS.
- **2026-07-18** — **Barista · Quầy pha chế: chặn món sự cố + dọn ngôn ngữ hiển thị:**
  - **Hợp nhất 2 luồng việc đã tách đôi:** nhánh remote dùng bộ trạng thái `WAITING/MAKING/READY/SERVED/CANCELLED`, nhánh workbench dùng `IN_PROGRESS` + `PICKED_UP/BLOCKED/REMAKE`. Chốt theo `MAKING` (khớp CLAUDE.md Contract #1), giữ các trạng thái mở rộng. Gộp `CompletedBy` → `PreparedBy` (một khái niệm một cột); `completeClaimed` vừa khoá theo `BaristaId` vừa ghi `PreparedBy` nên thẻ KPI cá nhân vẫn đủ dữ liệu.
  - **`BLOCKED` từ trạng thái chết thành luồng chạy được:** trước đây khai báo ở schema/enum/badge/track khách nhưng KHÔNG câu SQL nào ghi vào — "Báo sự cố" chỉ set `HasIssue` nên món vẫn nằm hàng chờ và barista khác vẫn bấm Nhận pha. Nay tách 6 lý do theo phạm vi ảnh hưởng: hết nguyên liệu (kiểm kê về 0 qua ledger + chặn món, cùng 1 tx), hỏng máy/ngừng bán (chặn món), còn lại chỉ gắn cờ cho Thu ngân.
  - **Không tự khoá menu khi hết nguyên liệu** — một nguyên liệu nằm trong nhiều món, khoá là quyết định doanh thu. Tồn về 0 → `findProductsWithDepletedIngredient` tự gợi ý ở màn Báo hết món để người dùng chủ động.
  - **Đóng vòng đời:** `BLOCKED` vào `findBaristaWorkbench` (thiếu thì món biến mất khỏi mọi màn), `cancelItem` nhận `BLOCKED`, thêm nút "Có lại rồi — trả về chờ pha". Khu "Cần xử lý" tách khỏi luồng pha, không tính vào số món trễ.
  - **Ngôn ngữ hiển thị:** bỏ `SLA` và `86` khỏi tầng UI (`Trễ giờ`, `Nên xong trong N phút`, `Báo tạm hết`); sửa lặp chữ `Bàn Bàn 02` (4 chỗ); enum thô `WAITING`/`READY` lọt ra dashboard; đơn vị `ly` → `món`; `1770 phút` → `29 tiếng 30 phút`. Quy tắc chốt: jargon sống ở DB/code, cấm ở tầng hiển thị.
  - **Verify:** `mvn clean test` 54/54 PASS. Deploy Tomcat 10.1 + đăng nhập `barista1` thật: chạy thật bắt được 2 lỗi mà test không thấy — `partial=recipe` thiếu `productId` trả **500** (đã sửa thành fragment rỗng) và `18.000 g` bị đọc nhầm thành 18 nghìn gam (đã thành `18 g`).
  - **Còn nợ:** mốc ngày kinh doanh theo `org.Branch.OpenTime` + màn cài đặt chi nhánh cho Manager (chưa làm); `cancelItem` vẫn chưa có màn Thu ngân nào gọi tới.
- **2026-07-18** — **Barista · Ngày 3 plan — KPI cá nhân trên dashboard:** thêm `sales.OrderItem.PreparedBy` + migration `sql/migration_orderitem_preparedby.sql`, ghi người pha khi món chuyển READY, `OrderItemDao.leadTimeStats(..., userId)` lọc KPI theo barista, dashboard nạp/hiển thị `myKpi`, thêm `HandoverKpiTest` cho hiển thị lead-time. Verify: `mvn test -Dtest='HandoverKpiTest'` PASS.
- **2026-07-18** — **Barista · Ngày 2 plan — Brew history tại bàn giao ca:** thêm `OrderItemDao.findBrewedToday(...)` lọc `READY/SERVED` theo chi nhánh và cửa sổ ngày Việt Nam, `HandoverService.getBrewHistory`, bảng “Ly đã pha hôm nay” với empty-state; thêm integration test có điều kiện `@Disabled` do cần SQL Server local. Verify: `mvn test` PASS (48 test, 1 skipped).
- **2026-07-18** — **Barista · Rà soát & chỉnh sửa 2 màn Hàng chờ pha (KDS) + Món chờ giao (Pickup) theo nghiệp vụ:**
  - **Bối cảnh:** rà soát vòng đời trạng thái món/đơn end-to-end phát hiện các lỗ hổng nghiệp vụ; lõi auto-deduct + claim nguyên tử giữ nguyên (đúng), chỉ vá phần vòng đời và trải nghiệm.
  - **Schema (additive):** `sales.OrderItem.ServedAt DATETIME2 NULL` — mốc giao khách (→SERVED), NULL khi hoàn tác. File: `database.sql` + `sql/migration_orderitem_servedat.sql` (idempotent). **Phải chạy migration trước khi deploy** (query có cột `ServedAt`).
  - **Fix#1 · Vòng đời đơn kết thúc:** `OrderDao.completeIfAllItemsFinal` (ACTIVE→COMPLETED nguyên tử, WHERE-guard `NOT EXISTS` món chưa SERVED/CANCELLED) + `reopenIfCompleted`. `OrderService` gọi trong cùng tx sau `markItemServed`/`serveAllReady`/`cancelItem` → đơn giao xong tự rời Order Inbox cashier (hết ticket "ma").
  - **Fix#2/#3 · Hết silent-failure:** `startItem`/`markItemReady`/`markItemServed` trả `boolean`; `cancelItem` trả mã `OK/NOT_FOUND/ALREADY_BILLED/CONFLICT`. Servlet map → flash (`flashOk`/`flashError`) render trong fragment `kds_cards.jsp`/`pickup_cards.jsp` (đọc + `c:remove` ngay). "Không pha được" khi món đã lên bill → báo rõ "nhờ thu ngân void/refund"; `cantMake`+`also86` báo 2 kết quả riêng.
  - **Fix#4/#5 · Pickup atomic + N+1:** `OrderService.serveAllReady` gộp 1 transaction (thay vòng lặp mở n connection); `getPickupTickets` gom món READY + toàn bộ món của đơn trong 1 connection (bỏ `getOrder` per-order + query modifier trùng). `findReady` thêm `o.Status='ACTIVE'` + tie-break `oi.OrderItemId`.
  - **Fix hoàn tác giao nhầm:** `unserveItem` SERVED→READY (xoá ServedAt, **không đụng ledger**, reopen đơn nếu đã COMPLETED); màn Pickup có khu "Vừa giao xong" (10 phút) với nút Hoàn tác.
  - **UI Pickup (Ngày 5 commit-plan):** SLA "Chờ giao Xm" theo `Constants.PICKUP_WARN_SECONDS`/`PICKUP_CRIT_SECONDS` (tô ok/warn/crit), badge "Khách đã thanh toán" (phiên CLOSED), submit AJAX + polling `suppressUntil` như KDS.
  - **KDS nhỏ:** nút "Xong cả đơn (n)" đếm đúng tổng cả đơn (`KdsTicket.orderPendingCount`, cảnh báo khi span 2 cột); badge "⚠ Chưa có công thức — sẽ không trừ kho" cho món thiếu recipe (`ProductRecipeDao.findProductIdsWithRecipe`).
  - **Đã cân nhắc & KHÔNG đổi:** fallback `InventoryDashboardServlet.branchId → 1` (đụng 22 file + phá preview ADMIN, để lại có chủ đích); guard "món đã lên bill" (giữ, chỉ thêm thông báo).
  - **Build + Test:** `mvn clean test` **47/47 PASS** (thêm `KdsBoardTest` 2, `PickupSlaTest` 2). ⚠️ Chưa runtime-test Tomcat đợt này — cần deploy + chạy migration để verify luồng COMPLETED/hoàn tác/flash.

---

- **2026-07-19** — **DB · Gộp về một file schema duy nhất:** `sql/database.sql` giờ là nguồn sự thật duy nhất. Đã fold 3 cột của đợt migration KDS vào định nghĩa bảng gốc (`org.Branch.PeakThresholdCups`, `catalog.Product.PrepSeconds`, `sales.Orders.PickupCode`) và đưa demo Hao hụt & Làm lại (28 dòng CN01) thành **PART D** ở cuối file. Xoá 5 file rời: `migration_barista_workbench.sql` (nội dung đã có sẵn trong schema), `migration_kds_business.sql`, `migration_orderitem_preparedby.sql`, `migration_orderitem_servedat.sql` (đều là tập con), `seed_waste_log_demo.sql`.
  - ⚠️ **DB đang chạy:** file gộp là script dựng mới (DROP/CREATE). Ai đã chạy các migration cũ thì DB hiện tại vẫn đúng, không cần làm gì. Ai chưa chạy `migration_kds_business.sql` thì phải dựng lại DB từ `database.sql` (hoặc tự `ALTER TABLE` thêm 3 cột trên) — nếu không, query KDS sẽ lỗi thiếu cột.
