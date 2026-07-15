# FUNCTION_LIST.md — Danh mục chức năng (dựng từ SOURCE CODE)

> Bảng này được rút trực tiếp từ mã nguồn (`@WebServlet` routes + action dispatch trong controller),
> **không** dựa vào changelog. Là nguồn mô tả chức năng chính xác để đối chiếu với SRS/SDS.
> Cập nhật: 2026-07-01 · phạm vi: 41 servlet, 34 service, 39 DAO, 47 model.
>
> **Quy ước cột:** `In Charge` = phân công theo folder role (DEV1 Admin · DEV2 Manager · DEV3 Cashier+QR · DEV4 Barista) — thay bằng tên thành viên thật khi nộp. `SRS` = mã use-case theo `KE_HOACH_CHI_TIET_THEO_ROLE.md`. `SDS` = route + servlet (design artifact). `Status` = có mã nguồn hoàn chỉnh (chưa tự chạy lại để re-verify runtime trong lần rà này).

---

## ⚠ Khác biệt thực tế so với đặc tả/changelog (đã kiểm chứng trong code)

- **Báo cáo KHÔNG còn là màn riêng.** `ReportServlet` (`/admin/report`) chỉ **xuất CSV**; xem thường thì **redirect về `/dashboard`**. Doanh thu đã **gộp vào Admin Dashboard**.
- **`/dashboard` là bộ định tuyến theo role**, không phải một trang: redirect Manager/Cashier/Barista sang dashboard riêng; chỉ Admin render tại chỗ.
- **Thêm filter thứ 5:** `CashierDutyGuardFilter` chặn thao tác ghi của thu ngân tới khi "bắt đầu ca".
- **Chấm giờ nhân viên (clock-in/out)** ở cả Cashier shift và Barista handover (`ShiftClockStatus`, `_shiftClockCard.jsp`) — ngoài đặc tả ca gốc.
- **2 màn ngoài đặc tả 4-role:** trang **Home công khai** (`/home`) và **Home Editor** (`/admin/home`).
- **POS** gửi đơn bằng **JSON fetch** kèm `orderType` (COUNTER), không phải form thường.
- **Waste** giàu hơn đặc tả: `createIngredientWaste`, **`remakeProduct`** (làm lại), `update`, `void`.
- **Theme** hiện là Espresso/Caramel (nâu), không phải "Highlands đỏ" như changelog cũ mô tả.

Chú thích ⚠ trong cột Notes = điểm lệch so với đặc tả gốc.

---

## ADMIN — `com.cafe.controller.admin` (In Charge: DEV1)

| Screen/Function | Description (action thực tế) | In Charge | SRS | SDS | Status | Notes |
|---|---|---|---|---|---|---|
| Login / Logout / Forgot | showLogin/login/logout/forgot — BCrypt, CSRF, reset qua username+email | DEV1 | A0 | `/auth/login,/logout,/forgot` · `AuthServlet` | ✅ Done | 3 route trong 1 servlet |
| User Management | list, new/edit, toggleStatus, resetPassword, assignBranch, lọc role/branch | DEV1 | A1 | `/admin/user` · `UserServlet` | ✅ Done | |
| Branch Management | list, new/edit, toggleActive (+ manager, giờ mở/đóng trong form) | DEV1 | A2 | `/admin/branch` · `BranchServlet` | ✅ Done | |
| Category | list, new/edit, delete (soft) | DEV1 | A3 | `/admin/category` · `CategoryServlet` | ✅ Done | |
| Product | list, new/edit, toggleActive, publishToBranch, **publishManyToBranch** | DEV1 | A3 | `/admin/product` · `ProductServlet` | ✅ Done | bulk-publish ngoài đặc tả |
| Ingredient (RAW/PREPPED) | list, new/edit, delete | DEV1 | A4 | `/admin/ingredient` · `IngredientServlet` | ✅ Done | |
| Recipe / BOM + PrepRecipe | addLine/updateLine/deleteLine + addPrep/deletePrep | DEV1 | A4 | `/admin/recipe` · `RecipeServlet` | ✅ Done | **Contract #2** |
| Modifier (group/option/impact/assign) | saveGroup, deleteGroup, add/update/deleteOption, add/deleteImpact, assign/unassignGroup | DEV1 | A4 | `/admin/modifier` · `ModifierServlet` | ✅ Done | 4 sub-view |
| Voucher | list, new/edit, toggleActive | DEV1 | A5 | `/admin/voucher` · `VoucherServlet` | ✅ Done | |
| Branch Menu (admin) | chọn chi nhánh → publish / giá địa phương / 86 (2 bước) | DEV1 | A3/M8 | `/admin/branch-menu` · `BranchMenuServlet` | ✅ Done | có branch-picker |
| Admin Dashboard + Doanh thu | doanh thu chuỗi tại chỗ: summary, by-branch, top product, payment breakdown, daily | DEV1 | A6 | `/dashboard`, `/admin/report?action=export` · `DashboardServlet`/`ReportServlet` | ✅ Done | ⚠ **report gộp vào dashboard**; route report = xuất CSV |
| Home Editor (trang công khai) | toggleHome, saveProduct, saveContent | DEV1 | — | `/admin/home` · `HomeAdminServlet` | ✅ Done | ⚠ **ngoài đặc tả** |

## BRANCH MANAGER — `com.cafe.controller.manager` (In Charge: DEV2)

| Screen/Function | Description | In Charge | SRS | SDS | Status | Notes |
|---|---|---|---|---|---|---|
| Manager Dashboard | tổng quan hôm nay, NV có ca, tồn thấp, chờ duyệt, doanh thu hôm nay | DEV2 | M1 | `/manager/dashboard` · `ManagerDashboardServlet` | ✅ Done | |
| Shift Scheduling | createTemplate, deleteTemplate, assign, unassign · **★ conflict resolver** | DEV2 | M2 | `/manager/shift` · `ShiftServlet` | ✅ Done | |
| Attendance Approval | approveMany (tickbox), reject, reopen, edit giờ | DEV2 | M3 | `/manager/attendance` · `AttendanceServlet` | ✅ Done | |
| Payroll | giờ theo tháng + lương/giờ + thành tiền, save, xuất CSV | DEV2 | M4 | `/manager/payroll` · `PayrollServlet` | ✅ Done | bảng `hr.Payroll` |
| Inventory Dashboard | tồn + low-stock + **xem sổ cái** từng nguyên liệu | DEV2 | M5 | `/manager/inventory` · `InventoryDashboardServlet` | ✅ Done | |
| Stock Receipt / PO | create, addLine, **addLines** (nhiều), removeLine, confirm→ledger, cancel, cancelMany | DEV2 | M6 | `/manager/receipt` · `StockReceiptServlet` | ✅ Done | |
| Supplier | list, new/edit, toggleActive | DEV2 | M6 | `/manager/supplier` · `SupplierServlet` | ✅ Done | |
| Reconciliation | kiểm kê tickbox nhiều nguyên liệu → ADJUST qua ledger | DEV2 | M7 | `/manager/reconciliation` · `ReconciliationServlet` | ✅ Done | |
| Branch Menu Config | toggleAvailable, setLocalPrice, **hideMany** | DEV2 | M8 | `/manager/menu` · `ManagerMenuServlet` | ✅ Done | dùng chung `BranchMenuService` |

## CASHIER + QR — `com.cafe.controller.cashier` / `customer` (In Charge: DEV3)

| Screen/Function | Description | In Charge | SRS | SDS | Status | Notes |
|---|---|---|---|---|---|---|
| Cashier Dashboard | doanh thu hôm nay + số đơn + lối tắt | DEV3 | R2 | `/cashier/dashboard` · `CashierDashboardServlet` | ✅ Done | ⚠ thêm ngoài đặc tả |
| Open/Close Shift + Duty | startDuty/closeDuty, clockIn/clockOut, open/close, report | DEV3 | C1 | `/cashier/shift` · `CashierShiftServlet` | ✅ Done | ⚠ **duty/chấm giờ** + `CashierDutyGuardFilter` |
| POS Order | JSON cart → `placeOrder(orderType=COUNTER)`, `order.created` | DEV3 | C2 | `/cashier/pos` · `PosServlet` | ✅ Done | ⚠ JSON POST |
| Table Floor Map | openTable, closeTable, setStatus, merge | DEV3 | C3 | `/cashier/table` · `TableServlet` | ✅ Done | không có action "move" |
| Order Inbox | list, confirm, void (guard: chỉ khi chưa pha) | DEV3 | C4 | `/cashier/inbox` · `OrderInboxServlet` | ✅ Done | |
| Checkout + Tách/Gộp | applyVoucher, removeVoucher, splitBill, mergeBill, pay, void · **★ bill splitting** | DEV3 | C5 | `/cashier/checkout` · `CheckoutServlet` | ✅ Done | `payment.completed` |
| Transaction History | list, view, void (lý do), **refund** (lý do) | DEV3 | C6 | `/cashier/history` · `BillHistoryServlet` | ✅ Done | lọc theo ca/hình thức |
| QR Menu & Đặt món (khách) | scan→menu→JSON cart→`placeOrder(QR)` / trang QR sai | DEV3 | C7 | `/qr/menu` · `QrMenuServlet` | ✅ Done | phiên ẩn danh |
| QR Track (khách) | status(AJAX), callStaff, requestBill, **cancel** | DEV3 | C8 | `/qr/track` · `QrTrackServlet` | ✅ Done | |

## BARISTA — `com.cafe.controller.barista` (In Charge: DEV4)

| Screen/Function | Description | In Charge | SRS | SDS | Status | Notes |
|---|---|---|---|---|---|---|
| Barista Dashboard | KDS KPI + tồn thấp + món 86 | DEV4 | — | `/barista/dashboard` · `BaristaDashboardServlet` | ✅ Done | ⚠ thêm ngoài đặc tả |
| KDS Queue | queue(AJAX cards), start, markReady, bump · **★ auto-deduct** | DEV4 | B1 | `/barista/kds` · `KdsServlet` | ✅ Done | **Contract #1,#2** |
| Pickup Board | list, markServed, **serveAllReady** | DEV4 | B2 | `/barista/pickup` · `PickupServlet` | ✅ Done | |
| 86 / Out-of-Stock | toggle86 + ETA | DEV4 | B3 | `/barista/eightysix` · `EightySixServlet` | ✅ Done | |
| Prep Checklist | createBatch, cancelBatch, updateBatch · RAW→PREPPED | DEV4 | B4 | `/barista/prep` · `PrepServlet` | ✅ Done | **Contract #2 nơi DUY NHẤT** |
| Waste & Remake | createIngredientWaste, create, **remakeProduct**, update, void | DEV4 | B5 | `/barista/waste` · `WasteServlet` | ✅ Done | ⚠ remake ngoài đặc tả |
| Recipe Lookup | search/view (read-only) | DEV4 | B6 | `/barista/recipe` · `RecipeLookupServlet` | ✅ Done | |
| Shift Handover + Clock | clockIn, clockOut, create note + KPI | DEV4 | B7 | `/barista/handover` · `ShiftHandoverServlet` | ✅ Done | ⚠ **chấm giờ** thêm; `hr.ShiftHandover` |

## Shared / Infra

| Screen/Function | Description | In Charge | SRS | SDS | Status | Notes |
|---|---|---|---|---|---|---|
| Public Home | trang khách xem thực đơn theo danh mục | DEV3 | — | `/home` · `HomeServlet` | ✅ Done | ⚠ **ngoài đặc tả** |
| Health check | `SELECT 1` qua HikariCP | Shared | — | `/health` · `HealthServlet` | ✅ Done | |
| Filter chain | Charset→Auth→Rbac→BranchScope→**CashierDutyGuard** | Shared | — | `web.xml` | ✅ Done | ⚠ 5 filter (thêm DutyGuard) |
