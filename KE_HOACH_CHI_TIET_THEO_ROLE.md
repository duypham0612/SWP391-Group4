# KẾ HOẠCH CHI TIẾT THEO ROLE — Hệ thống Quản lý Chuỗi Cafe
### Bản đặc tả build cho Claude Code · JSP + Servlet + JSTL + SQL Server (MVC) · **Folder chia theo ROLE**

> File này **mở rộng & chi tiết hóa** `KE_HOACH_TRIEN_KHAI_CLAUDE_CODE.md`. Khác biệt chính: cấu trúc Java **chia theo role** (dễ nhận diện, mỗi dev 1 folder) và **liệt kê đúng tên class/method/route/màn** cho từng role. Khi xung đột, **file này thắng** về chi tiết đặt tên; các nguyên tắc bất biến vẫn theo file gốc.

---

## 0. Nguyên tắc bất biến (nhắc lại — KHÔNG phá vỡ)

1. **JSP chỉ JSTL/EL** — cấm scriptlet `<% %>`.
2. **Tồn kho chỉ đổi qua `InventoryService` + ghi `InventoryTransaction` (ledger)** — không UPDATE thẳng `BranchInventory`.
3. **Status món dùng enum chung `OrderItemStatus`** (`WAITING → MAKING → READY → SERVED → CANCELLED`, `READY`="Sẵn lấy").
4. **Cờ RAW/PREPPED**: trừ thô khi pha; đồ pha sẵn trừ từ tồn PREPPED (raw đã trừ lúc `PrepBatch`) → không trừ thô 2 lần.
5. **Cashier sở hữu mọi order entry** (quầy + QR), cùng `OrderService`.
6. **Auth = HttpSession + Filter** (không JWT); mọi query lọc `branch_id` từ `BranchContext`.
7. **Transaction ở Service**; Controller không SQL; DAO không nghiệp vụ.

---

## 1. Cấu trúc thư mục — **ROLE-BASED** (mỗi dev nhận 1 folder)

```
com.cafechain/
├─ common/                         # HẠ TẦNG DÙNG CHUNG (không thuộc role nào)
│   ├─ config/    DBConnection, AppConfig
│   ├─ db/        BaseDao, TxManager
│   ├─ security/  AuthService, PasswordHasher, BranchContext
│   ├─ event/     EventPublisher, EventType
│   ├─ enums/     OrderItemStatus, IngredientType, TxnType, BillStatus, ...
│   └─ util/      Validators, JsonUtil, ExcelExporter
│
├─ filter/                         # DÙNG CHUNG: AuthFilter, RbacFilter, BranchScopeFilter
│
├─ model/                          # DÙNG CHUNG: tất cả entity (User, Branch, Product, Order, OrderItem, Bill...)
│
├─ shared/                         # SERVICE/DAO SPAN NHIỀU ROLE (xem mục 3) — KHÔNG nhét vào 1 role
│   ├─ service/  OrderService, InventoryService, CatalogReadService, VoucherService
│   └─ dao/      OrderDao, OrderItemDao, OrderItemModifierDao, BranchInventoryDao,
│                InventoryTransactionDao, ProductRecipeDao, ModifierIngredientImpactDao, BranchMenuDao
│
├─ admin/        # Dev 1
│   ├─ controller/  service/  dao/
├─ manager/      # Dev 2
│   ├─ controller/  service/  dao/
├─ cashier/      # Dev 3  (gồm cả QR app khách)
│   ├─ controller/  service/  dao/
└─ barista/      # Dev 4
    ├─ controller/  service/  dao/
```
Views: `WEB-INF/views/{admin|manager|cashier|barista|customer}/...` + `layout/` dùng chung. `customer/` (QR) thuộc **Dev 3**.

**Ai sở hữu cái gì** (để chia việc & tránh đụng độ):
- `shared/` được dùng chung nhưng **mỗi service có 1 dev chủ trì**: `OrderService` & `VoucherService(validate)` → Dev 3; `InventoryService` → Dev 2 chủ trì, Dev 4 gọi; `CatalogReadService` → Dev 1.
- `common/`, `filter/`, `model/` → Dev 1 dựng ở Phase 0–1, cả nhóm dùng.

---

## 2. Quy ước đặt tên (BẮT BUỘC — Claude Code tuân thủ tuyệt đối)

**Class**
- Controller: `{Feature}Servlet`  (vd `ProductServlet`, `KdsServlet`)
- Service: `{Domain}Service`       (vd `ProductService`, `BillingService`)
- DAO: `{Entity}Dao`               (vd `ProductDao`, `OrderItemDao`)
- Model: danh từ số ít             (vd `Product`, `OrderItem`)

**Method — DAO (động từ persistence, nhận `Connection conn`):**
`findAll(conn)` · `findById(conn,id)` · `findByBranch(conn,branchId)` · `findBy{Field}(...)` · `insert(conn,e)` · `update(conn,e)` · `delete(conn,id)` · `update{Field}(conn,...)`

**Method — Service (động từ nghiệp vụ):**
- Đọc: `get{Noun}List(...)` · `get{Noun}(id)`
- Ghi: `create{Noun}(...)` · `update{Noun}(...)` · `delete{Noun}(id)` · `set{Noun}{Field}(...)`
- Nghiệp vụ: `confirm{X}` · `void{X}` · `apply{X}` · `mark{X}` · `split{X}` · `assign{X}` · `open{X}` · `close{X}` · `validate{X}`

**Action param trong Servlet:** `?action={camelCaseVerb}` khớp ý service (`list`,`create`,`update`,`delete`,`confirm`,`markReady`,...). Mặc định `action=list`.

**Route:** staff = `/{role}/{feature}` · khách = `/qr/{feature}` · auth = `/auth/{action}`

**JSP:** `views/{role}/{feature}-list.jsp` · `{feature}-form.jsp` · fragment dùng chung tiền tố `_` (`_statusBadge.jsp`).

**Event (đặt tên `{aggregate}.{pastVerb}`):** `order.created` · `order.status_changed` · `payment.completed` · `inventory.deducted` · `stock.low`

---

## 3. SHARED LAYER (backbone — đặt tên CHÍNH XÁC, mọi role gọi vào)

**`EventPublisher`** (common/event)
- `publish(EventType type, String aggregateId, Integer branchId, String payloadJson)` → INSERT `ops.OutboxEvent`.

**`AuthService`** (common/security) — Dev 1
- `authenticate(String username, String rawPwd)` → User | null
- `hashPassword(String raw)` · `verifyPassword(String raw, String hash)`  (BCrypt)

**`OrderService`** (shared/service) — Dev 3 chủ trì, Dev 4 gọi phần status
- `createOrder(int branchId, Integer sessionId, OrderSource source, Integer createdBy)` → orderId
- `addOrderItem(int orderId, int productId, int qty, List<Integer> optionIds, String note)`
- `removeOrderItem(int orderItemId)`
- `submitOrder(int orderId)` → publish `order.created`
- `confirmOrder(int orderId)` · `voidOrder(int orderId, String reason)`
- `getIncomingOrders(int branchId)` (Order Inbox) · `getSessionOrders(int sessionId)`
- `getKdsQueue(int branchId)` · `getReadyItems(int branchId)`
- `startItem(int orderItemId)` → WAITING→MAKING, set StartedAt, publish `order.status_changed`
- `markItemReady(int orderItemId)` → **gọi `InventoryService.deductForOrderItem`** rồi MAKING→READY, set DoneAt, publish `inventory.deducted` + `order.status_changed` (cùng 1 tx)
- `markItemServed(int orderItemId)` → READY→SERVED, publish `order.status_changed`
- `getSessionItemStatuses(int sessionId)` (cho QR tracking)

**`InventoryService`** (shared/service) — Dev 2 chủ trì, Dev 4 gọi · **CỬA DUY NHẤT đổi tồn**
- `applyTxn(int branchId, int ingredientId, BigDecimal delta, TxnType type, String refTable, long refId, Integer userId)` *(lõi: INSERT InventoryTransaction + UPDATE BranchInventory + publish `stock.low` nếu chạm ngưỡng — TẤT CẢ trong tx do caller mở)*
- `confirmReceiptStock(int receiptId)` (Manager) → mỗi dòng `applyTxn(+qty, RECEIPT)`
- `createPrepBatch(int branchId, int preppedIngredientId, BigDecimal qtyProduced, int userId)` (Barista) → trừ RAW (`PREP_OUT`) + cộng PREPPED (`PREP_IN`) — xem mục 6
- `logWaste(int branchId, int ingredientId, BigDecimal qty, WasteType type, String reason, int userId)` → `applyTxn(-qty, WASTE)`
- `createAdjustment(int branchId, int ingredientId, BigDecimal actualQty, String reason, int userId)` → `applyTxn(diff, ADJUST)`
- `deductForOrderItem(int orderItemId)` (Barista, gọi qua OrderService) → **modifier-aware**, xem mục 6
- `getBranchInventory(int branchId)` · `getLowStock(int branchId)` · `getIngredientLedger(int branchId, int ingredientId)`

**`CatalogReadService`** (shared/service) — Dev 1 chủ trì
- `getBranchMenuForCustomer(int branchId)` (lọc IsAvailable + Is86)
- `getProduct(int productId)` · `getRecipeForProduct(int productId)`
- `getModifierGroupsForProduct(int productId)` · `getModifierImpacts(int optionId)`

**`VoucherService`** (shared/service) — Dev 1 sở hữu, Dev 3 gọi
- `validateVoucher(String code, int branchId, BigDecimal orderAmount)` → VoucherResult (**1 nguồn duy nhất**)
- `getVoucherList()` · `createVoucher(...)` · `updateVoucher(...)` · `setVoucherActive(int id, boolean active)` · `incrementUsed(int voucherId)`

---

## 4. DEV 1 — ADMIN  (package `com.cafechain.admin`) · 6 màn

#### A0 · Auth Gateway → `/auth/login`, `/auth/logout`
- Controller `AuthServlet` — actions: `showLogin`, `login`, `logout`
- Service: `AuthService` (common); DAO: `UserDao.findByUsername`
- View: `views/auth/login.jsp` · Bảng: iam.User, iam.Role · Contract #1

#### A1 · IAM Dashboard → `/admin/user`
- Controller `UserServlet` — `list`,`create`,`update`,`toggleStatus`,`resetPassword`,`assignBranch`
- Service `UserService` — `getUserList(filter)`, `getUser(id)`, `createUser(dto)`, `updateUser(dto)`, `setUserStatus(id,status)`, `resetPassword(id,newPwd)`, `assignBranch(userId,branchId)`
- DAO `UserDao` — `findAll`,`findById`,`findByUsername`,`findByBranch`,`insert`,`update`,`updateStatus`,`updatePassword`
- View: `admin/user-list.jsp`, `user-form.jsp` · Bảng: iam.User, iam.Role, org.Branch

#### A2 · Branch Management → `/admin/branch`
- Controller `BranchServlet` — `list`,`create`,`update`,`toggleActive`,`assignManager`
- Service `BranchService` — `getBranchList()`,`getBranch(id)`,`createBranch(dto)`,`updateBranch(dto)`,`setBranchActive(id,active)`,`assignManager(branchId,userId)`
- DAO `BranchDao` — `findAll`,`findById`,`insert`,`update`,`updateManager`,`updateActive`
- View: `admin/branch-list.jsp`, `branch-form.jsp` · Bảng: org.Branch

#### A3 · Global Catalog (Category + Product) → `/admin/category`, `/admin/product`
- Controllers `CategoryServlet`, `ProductServlet` — `list`,`create`,`update`,`delete`(category) / `toggleActive`(product), `publishToBranch`
- Services:
  - `CategoryService` — `getCategoryList()`,`getCategory(id)`,`createCategory(dto)`,`updateCategory(dto)`,`deleteCategory(id)`
  - `ProductService` — `getProductList(filter)`,`getProduct(id)`,`createProduct(dto)`,`updateProduct(dto)`,`setProductActive(id,active)`,`publishToBranch(productId,branchId)`
- DAO `CategoryDao`, `ProductDao` — chuẩn `findAll/findById/findByCategory/insert/update/delete`
- View: `admin/category-list.jsp`,`category-form.jsp`,`product-list.jsp`,`product-form.jsp` · Bảng: catalog.Category, catalog.Product, catalog.BranchMenu

#### A4 · Recipe / BOM Builder → `/admin/recipe`, `/admin/ingredient`, `/admin/modifier`
- Controllers `RecipeServlet`, `IngredientServlet`, `ModifierServlet`
- Services:
  - `IngredientService` — `getIngredientList()`,`createIngredient(name,unit,IngredientType type)`,`updateIngredient(dto)`  ← **đặt cờ RAW/PREPPED ở đây**
  - `RecipeService` — `getProductRecipe(productId)`,`addRecipeLine(productId,ingredientId,qty)`,`removeRecipeLine(lineId)`,`getPrepRecipe(preppedIngredientId)`,`savePrepRecipe(preppedId,List<rawLine>,yieldQty)`
  - `ModifierService` — `getModifierGroups()`,`createModifierGroup(dto)`,`createModifierOption(groupId,dto)`,`assignGroupToProduct(productId,groupId)`,`saveModifierImpact(optionId,ingredientId,qtyDelta)`
- DAO `IngredientDao`,`ProductRecipeDao`,`PrepRecipeDao`,`ModifierGroupDao`,`ModifierOptionDao`,`ModifierIngredientImpactDao`,`ProductModifierGroupDao`
- View: `admin/recipe-builder.jsp`,`ingredient-list.jsp`,`modifier-list.jsp`
- Bảng: catalog.{Ingredient, ProductRecipe, PrepRecipe, ModifierGroup, ModifierOption, ModifierIngredientImpact, ProductModifierGroup} · **Contract #2**

#### A5 · Voucher & Promotion → `/admin/voucher`
- Controller `VoucherServlet` — `list`,`create`,`update`,`toggleActive`
- Service `VoucherService` (shared — xem mục 3) · DAO `VoucherDao` — `findAll`,`findById`,`findByCode`,`insert`,`update`,`incrementUsed`,`updateActive`
- View: `admin/voucher-list.jsp`,`voucher-form.jsp` · Bảng: payment.Voucher

---

## 5. DEV 2 — BRANCH MANAGER  (package `com.cafechain.manager`) · 8 màn

#### M1 · Branch Dashboard → `/manager/dashboard`
- Controller `ManagerDashboardServlet` — `show`
- Service `ManagerDashboardService` — `getTodaySummary(branchId)`,`getStaffOnShift(branchId)`,`getLowStockAlerts(branchId)`,`getPendingApprovals(branchId)`
- View: `manager/dashboard.jsp` · Bảng: payment.Bill, hr.*, inventory.vw_LowStock

#### M2 · Shift Scheduling → `/manager/shift`  ★ Standout: Shift Conflict Resolver
- Controller `ShiftServlet` — `week`,`createTemplate`,`assign`,`unassign`
- Service `ShiftService` — `getShiftTemplates(branchId)`,`createShiftTemplate(dto)`,`getWeekSchedule(branchId,weekStart)`,`assignShift(templateId,userId,date)`,`unassignShift(assignmentId)`,`detectConflict(userId,date,templateId)`
- DAO `ShiftTemplateDao`,`ShiftAssignmentDao` — `findByBranch`,`findByUserAndDate`(conflict),`insert`,`delete`
- View: `manager/shift-calendar.jsp` · Bảng: hr.ShiftTemplate, hr.ShiftAssignment

#### M3 · Attendance Approval → `/manager/attendance`
- Controller `AttendanceServlet` — `list`,`approve`,`reject`,`edit`
- Service `AttendanceService` — `getPendingAttendance(branchId)`,`approveAttendance(id,approverId)`,`rejectAttendance(id,approverId)`,`updateAttendance(dto)`,`computeWorkHours(assignmentId)`
- DAO `AttendanceDao` — `findByStatus`,`findById`,`updateStatus`,`update`
- View: `manager/attendance-list.jsp` · Bảng: hr.Attendance, hr.ShiftAssignment

#### M4 · Payroll Summary → `/manager/payroll`
- Controller `PayrollServlet` — `show`,`exportExcel`
- Service `PayrollService` — `getMonthlyPayroll(branchId,month)`,`exportPayrollExcel(branchId,month)`
- View: `manager/payroll.jsp` · Bảng: hr.Attendance, hr.ShiftAssignment, iam.User

#### M5 · Inventory Dashboard → `/manager/inventory`
- Controller `InventoryDashboardServlet` — `list`,`viewLedger`
- Service: `InventoryService` (shared) — `getBranchInventory`,`getLowStock`,`getIngredientLedger`
- View: `manager/inventory-list.jsp`,`inventory-ledger.jsp` · Bảng: inventory.BranchInventory, InventoryTransaction

#### M6 · Stock Receipt / PO → `/manager/receipt`, `/manager/supplier`
- Controllers `StockReceiptServlet`, `SupplierServlet` — `list`,`create`,`addLine`,`confirm`,`cancel` / `list`,`create`,`update`
- Service `StockReceiptService` — `getReceiptList(branchId)`,`getReceipt(id)`,`createDraftReceipt(dto)`,`addReceiptLine(receiptId,ingredientId,qty,cost)`,`confirmReceipt(receiptId)`(→ `InventoryService.confirmReceiptStock`),`cancelReceipt(id)` ; `SupplierService` chuẩn CRUD
- DAO `StockReceiptDao`,`StockReceiptDetailDao`,`SupplierDao`
- View: `manager/receipt-list.jsp`,`receipt-form.jsp`,`supplier-list.jsp` · Bảng: inventory.StockReceipt, StockReceiptDetail, Supplier + ledger · **Contract: ledger khi confirm**

#### M7 · Stock Reconciliation → `/manager/reconciliation`
- Controller `ReconciliationServlet` — `list`,`create`,`apply`
- Service `StockAdjustmentService` — `getAdjustmentList(branchId)`,`createAdjustment(...)`(→ `InventoryService.createAdjustment`)
- DAO `StockAdjustmentDao` · View: `manager/reconciliation-list.jsp`,`reconciliation-form.jsp` · Bảng: inventory.StockAdjustment + ledger

#### M8 · Branch Menu Config → `/manager/menu`
- Controller `BranchMenuServlet` — `list`,`toggleAvailable`,`setLocalPrice`
- Service `BranchMenuService` (shared với Barista 86) — `getBranchMenu(branchId)`,`setAvailability(branchId,productId,available)`,`setLocalPrice(branchId,productId,price)`
- DAO `BranchMenuDao` · View: `manager/branch-menu.jsp` · Bảng: catalog.BranchMenu

---

## 6. DEV 3 — CASHIER  (package `com.cafechain.cashier`, gồm QR app khách) · 8 màn

#### C1 · Open/Close Shift → `/cashier/shift`
- Controller `CashierShiftServlet` — `open`,`close`,`report`
- Service `CashierShiftService` — `openShift(branchId,cashierId,openingCash)`,`closeShift(shiftId,closingCash)`,`getCurrentShift(cashierId)`,`getShiftReport(shiftId)`
- DAO `CashierShiftDao` · View: `cashier/shift-open.jsp`,`shift-report.jsp` · Bảng: payment.CashierShift

#### C2 · POS Order (quầy) → `/cashier/pos`
- Controller `PosServlet` — `newOrder`,`addItem`,`removeItem`,`submit`
- Service: `OrderService` (shared) — `createOrder(source=COUNTER)`,`addOrderItem`,`removeOrderItem`,`submitOrder`
- View: `cashier/pos.jsp` · Bảng: sales.Orders, OrderItem, OrderItemModifier · **Contract #1 (order.created), #3**

#### C3 · Table Floor Map → `/cashier/table`
- Controller `TableServlet` — `map`,`openTable`,`closeTable`,`merge`,`move`,`setStatus`
- Service `TableSessionService` — `getFloorMap(branchId)`,`openSession(tableId,cashierId)`,`closeSession(sessionId)`,`mergeSessions(srcId,dstId)`,`moveSession(sessionId,newTableId)`,`setTableStatus(tableId,status)`
- DAO `DiningTableDao`,`TableSessionDao` · View: `cashier/table-map.jsp` · Bảng: sales.DiningTable, TableSession

#### C4 · Order Inbox → `/cashier/inbox`
- Controller `OrderInboxServlet` — `list`,`confirm`,`void`
- Service: `OrderService` — `getIncomingOrders`,`confirmOrder`,`voidOrder`
- View: `cashier/order-inbox.jsp` · Bảng: sales.Orders, OrderItem · *(gác cổng đơn QR)*

#### C5 · Checkout & Tách/Gộp Bill → `/cashier/checkout`  ★ Standout: Dynamic Bill Splitting
- Controller `CheckoutServlet` — `showBill`,`applyVoucher`,`splitBill`,`mergeBill`,`pay`
- Service `BillingService` — `buildSessionBill(sessionId)`,`applyVoucher(billId,code)`(gọi `VoucherService.validateVoucher`),`splitBill(sessionId,grouping)`,`mergeBills(billIds)`,`payBill(billId,method)`(→ publish `payment.completed`),`computeVat(amount)`
- DAO `BillDao`,`BillItemDao`,`VoucherRedemptionDao`
- View: `cashier/checkout.jsp` · Bảng: payment.Bill, BillItem, Voucher, VoucherRedemption · **Contract #3 (payment.completed) · voucher 1 nguồn**

#### C6 · Transaction History → `/cashier/history`
- Controller `BillHistoryServlet` — `list`,`view`,`reprint`,`void`
- Service `BillingService` — `getBillHistory(branchId,shiftId,filter)`,`getBill(billId)`,`voidBill(billId,reason)`
- View: `cashier/bill-history.jsp` · Bảng: payment.Bill, BillItem

#### C7 · QR Menu & Đặt món (khách) → `/qr/menu?t={qrCode}`  *(mobile-first)*
- Controller `QrMenuServlet` — `scan`,`menu`,`addToCart`,`place`
- Service `QrOrderService` — `identifyTable(qrCode)`(tạo/mở session ẩn danh),`getMenu(branchId)`(gọi `CatalogReadService.getBranchMenuForCustomer`),`placeCustomerOrder(sessionId,cartDto)`(gọi `OrderService.createOrder(source=QR)`+`submitOrder`)
- DAO: dùng `DiningTableDao`,`BranchMenuDao`,`OrderDao` (shared)
- View: `customer/menu.jsp`,`customer/cart.jsp` · Bảng: sales.DiningTable, TableSession, catalog.BranchMenu, sales.Orders · **Contract #1, #3**

#### C8 · QR Theo dõi đơn (khách) → `/qr/track?s={sessionId}`  *(AJAX polling)*
- Controller `QrTrackServlet` — `status`(AJAX),`callStaff`,`requestBill`
- Service `QrOrderService` — `getSessionStatuses(sessionId)`(gọi `OrderService.getSessionItemStatuses`),`callStaff(sessionId)`,`requestBill(sessionId)` *(2 cái sau publish event `service.call` / `bill.requested` — không cần bảng mới)*
- View: `customer/track.jsp` · Bảng: sales.OrderItem · *(đọc status enum chung)*

---

## 7. DEV 4 — BARISTA  (package `com.cafechain.barista`) · 7 màn

#### B1 · KDS Queue Board → `/barista/kds`  ★ Standout: Modifier-Aware Auto-Deduction
- Controller `KdsServlet` — `queue`(AJAX),`start`,`markReady`,`bump`
- Service `KdsService` — `getQueue(branchId)`(gọi `OrderService.getKdsQueue`),`startItem(orderItemId)`(→`OrderService.startItem`),`markReady(orderItemId)`(→`OrderService.markItemReady` ⇒ trong đó `InventoryService.deductForOrderItem`),`bumpPriority(orderItemId)`
- View: `barista/kds.jsp` + JS polling 3–5s
- Bảng: sales.OrderItem, OrderItemModifier; đọc catalog.ProductRecipe, ModifierIngredientImpact; ghi inventory.* · **Contract #1, #2**

#### B2 · Ready / Pickup Board → `/barista/pickup`
- Controller `PickupServlet` — `list`,`markServed`
- Service `KdsService` — `getReadyItems(branchId)`(gọi `OrderService.getReadyItems`),`markServed(orderItemId)`(→`OrderService.markItemServed`)
- View: `barista/pickup.jsp` · Bảng: sales.OrderItem

#### B3 · 86 / Out-of-Stock → `/barista/eightysix`
- Controller `EightySixServlet` — `list`,`toggle86`,`setEta`
- Service `BranchMenuService` (shared) — `set86(branchId,productId,is86,eta)`,`getMenuAvailability(branchId)` *(khóa món lên POS + QR menu)*
- View: `barista/eightysix.jsp` · Bảng: catalog.BranchMenu

#### B4 · Prep Checklist → `/barista/prep`  *(lõi 2 tầng)*
- Controller `PrepServlet` — `list`,`createBatch`
- Service: `InventoryService.createPrepBatch(...)` + `PrepService.getPrepChecklist(branchId)`,`getTodayBatches(branchId)`
- DAO `PrepBatchDao` (+ shared PrepRecipeDao, InventoryTransactionDao, BranchInventoryDao)
- View: `barista/prep.jsp` · Bảng: inventory.PrepBatch, catalog.PrepRecipe, inventory.* · **Contract #2 (nơi DUY NHẤT đổi RAW→PREPPED)**

#### B5 · Waste & Remake Log → `/barista/waste`
- Controller `WasteServlet` — `list`,`create`
- Service: `InventoryService.logWaste(...)` + `WasteService.getWasteList(branchId)`
- DAO `WasteLogDao` · View: `barista/waste.jsp` · Bảng: inventory.WasteLog + ledger

#### B6 · Recipe & Modifier Lookup → `/barista/recipe`  *(read-only)*
- Controller `RecipeLookupServlet` — `search`,`view`
- Service: `CatalogReadService.getRecipeForProduct`,`getModifierImpacts`
- View: `barista/recipe-lookup.jsp` · Bảng: catalog.ProductRecipe, ModifierIngredientImpact (read)

#### B7 · Shift Handover → `/barista/handover`  ⚠ cần thêm bảng (mục 9)
- Controller `HandoverServlet` — `list`,`create`
- Service `HandoverService` — `getHandoverNotes(branchId,date)`,`createHandover(branchId,userId,note)`,`getShiftKpi(branchId,userId,date)`(lead time = AVG(DoneAt-StartedAt) từ sales.OrderItem; số ly)
- DAO `ShiftHandoverDao` (+ đọc OrderItem cho KPI) · View: `barista/handover.jsp`
- Bảng: **hr.ShiftHandover (mới)** + sales.OrderItem (KPI)

---

## 8. THUẬT TOÁN LÕI (đặt trong `InventoryService` — viết unit test TRƯỚC)

**`deductForOrderItem(orderItemId)`** — gọi từ `OrderService.markItemReady`, cùng tx:
```
required = {}                                  # ingredientId -> qty
for line in ProductRecipeDao.findByProduct(productId):
    required[line.ingredientId] += line.quantity * item.quantity
for opt in OrderItemModifierDao.findByItem(orderItemId):
    for imp in ModifierIngredientImpactDao.findByOption(opt.optionId):
        required[imp.ingredientId] += imp.qtyDelta * item.quantity
for (ing, qty) in required:
    applyTxn(branchId, ing, -qty, DEDUCT, 'OrderItem', orderItemId, baristaUserId)
```
> **Không phân nhánh RAW/PREPPED ở đây.** Tồn PREPPED đã tồn tại nhờ `PrepBatch`; cứ trừ ingredient mà công thức tham chiếu → không trừ thô 2 lần.

**`createPrepBatch(branchId, preppedIng, qtyProduced, userId)`** — nơi DUY NHẤT đổi RAW→PREPPED:
```
for (rawIng, qtyPerYield, yieldQty) in PrepRecipeDao.findByPrepped(preppedIng):
    consumed = qtyProduced / yieldQty * qtyPerYield
    applyTxn(branchId, rawIng, -consumed, PREP_OUT, 'PrepBatch', batchId, userId)
applyTxn(branchId, preppedIng, +qtyProduced, PREP_IN, 'PrepBatch', batchId, userId)
```

---

## 9. ⚠ Bổ sung DB cần thiết (chưa có trong `cafe_chain_sqlserver.sql`)

Thêm 1 bảng cho màn B7 (giữ nguyên 37 bảng còn lại):
```sql
CREATE TABLE hr.ShiftHandover (
    ShiftHandoverId INT IDENTITY PRIMARY KEY,
    BranchId  INT NOT NULL,
    UserId    INT NOT NULL,
    ShiftDate DATE NOT NULL,
    Note      NVARCHAR(1000) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_SH_Branch FOREIGN KEY (BranchId) REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_SH_User   FOREIGN KEY (UserId)   REFERENCES iam.[User](UserId)
);
```
- QR `callStaff` / `requestBill` (C8): **không cần bảng** — chỉ publish `OutboxEvent` (`service.call`, `bill.requested`), Cashier đọc Order Inbox/notification. Nếu muốn lưu lịch sử, dùng luôn `OutboxEvent`.

---

## 10. Ánh xạ Phase ↔ Role (build tuần tự)

| Phase | Nội dung | Role/màn |
|---|---|---|
| 0 | Nền tảng + UI shell + login rỗng | common, filter, model, layout |
| 1 | Auth & RBAC | A0 + filters (Dev 1) |
| 2 ◀ *đang làm* | Admin Catalog & Config | A1–A5 (Dev 1) |
| 3 | Manager Inventory & HR | M1–M8 (Dev 2) |
| 4 | Sales + KDS (làm chung) | C1–C4 + B1–B6 (Dev 3+4) |
| 5 | Payment | C5–C6 (Dev 3) |
| 6 | QR app khách | C7–C8 (Dev 3) |
| 7 | Report + Handover + polish + E2E | M1 dashboard, B7, đồng nhất UI |

**Tiêu chí Done mỗi màn:** đúng tên class/method/route như trên · không scriptlet · dùng layout + component chung · tồn kho qua ledger · status enum chung · lọc `branch_id`.
