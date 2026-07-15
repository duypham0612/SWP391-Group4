# KẾ HOẠCH FIX SAU AUDIT — 4 ROLE (com.cafe)

> Nguồn: báo cáo audit `DAC_TA_FUNCTION_AUDIT_4_ROLE.md` + quét code thực tế (2026-06-29).
> Mục tiêu: đưa **BARISTA đạt chuẩn (≥7 màn, ≥2 CRUD)**, bổ sung **Cashier Order Inbox**, sửa **bug thật**, không phá contract C1–C5.
> Quy tắc thực thi: **mỗi mục xong → `mvn test-compile` + chạy thử + commit** (đây là thứ đã thiếu khiến phải dò thủ công luồng pha chế).

---

## 0. Hiện trạng (từ audit)

| Role | Function đủ | Màn CRUD | Kết luận |
|---|---|---|---|
| ADMIN | 25/38 | 7 | ✅ ĐẠT |
| MANAGER | 30/34 | 4 | ✅ ĐẠT |
| CASHIER | 21/29 | 4 (8 màn) | ✅ ĐẠT (nợ C4 Inbox) |
| BARISTA | 12/22 | **0** (5 màn) | 🔴 **KHÔNG ĐẠT** |

Contract **C1–C5 đều ĐẠT**, LUỒNG PHA CHẾ (auto-deduct modifier-aware + chống double-count + ledger) chính xác, có unit test. Phần thiếu là **độ phủ màn/CRUD** và một số bug nhỏ.

### ⚠️ Bẫy C4 — chỗ dễ phá tồn kho nhất (soi kỹ khi review)
Mọi thao tác **cancel mẻ Prep / sửa-xoá Waste** PHẢI hoàn kho bằng **TXN BÙ qua `InventoryService`** (đảo `PREP_IN`/`PREP_OUT`, cộng bù `WASTE`). **Tuyệt đối không** hard-delete dòng tồn, **không** UPDATE thẳng `inventory.BranchInventory`. Giữ row gốc + đánh dấu trạng thái `CANCELLED/VOIDED`.

---

## 1. Thay đổi SCHEMA (nêu lý do trước — theo CLAUDE.md §0)

CLAUDE.md cấm tự ý đổi schema; 3 thay đổi dưới đây **bắt buộc** để hiện thực kế hoạch đã duyệt, đều **cộng thêm (additive)**, không phá dữ liệu cũ. Đóng gói trong `sql/migration_audit_fix.sql` (idempotent) **và** cập nhật `sql/database.sql` cho cài mới.

| # | Thay đổi | Lý do | Ảnh hưởng |
|---|---|---|---|
| S1 | `inventory.PrepBatch` + cột `Status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'` CHECK IN ('ACTIVE','CANCELLED'), `VoidedAt DATETIME2 NULL` | B4 cancelBatch cần đánh dấu mẻ đã huỷ (giữ row + ghi txn bù, không hard-delete) | Additive, mặc định ACTIVE |
| S2 | `inventory.WasteLog` + cột `Status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'` CHECK IN ('ACTIVE','VOIDED'), `VoidedAt DATETIME2 NULL` | B5 delete (void) cần đánh dấu đã huỷ + txn bù | Additive |
| S3 | bảng mới `hr.ShiftHandover` | B7 Shift Handover (ghi chú bàn giao ca) | Bảng mới |

```sql
-- hr.ShiftHandover (B7)
CREATE TABLE hr.ShiftHandover (
    ShiftHandoverId INT IDENTITY PRIMARY KEY,
    BranchId        INT NOT NULL,
    Note            NVARCHAR(1000) NOT NULL,
    CreatedBy       INT NOT NULL,
    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_SH_Branch FOREIGN KEY (BranchId)  REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_SH_User   FOREIGN KEY (CreatedBy) REFERENCES iam.[User](UserId)
);
```

---

## 2. ƯU TIÊN 1 — BARISTA đạt chuẩn 🔴 (BẮT BUỘC)

Mục tiêu: **5 màn/0 CRUD → 7 màn/2 CRUD**.

### 1.1 · B6 Recipe Lookup `/barista/recipe` — [S] → màn thứ 6
- `CatalogReadService` (+method): `getRecipeForProduct(productId)` (gồm ProductRecipe + PrepRecipe theo từng ingredient PREPPED) + `getModifierImpactsForProduct(productId)` (qua ProductModifierGroup → option → impact). Read-only.
- `ProductService.getProductList()` đã có → dùng để search/list.
- Mới: `RecipeLookupServlet @WebServlet("/barista/recipe")` (doGet: list/search + view by productId).
- View `barista/recipe.jsp`: ô tìm + bảng công thức (NL, định mức, loại RAW/PREPPED) + chi phí modifier (option → QtyDelta).
- Sidebar Barista: thêm link.

### 1.2 · B7 Shift Handover `/barista/handover` — [M] → màn thứ 7
- SQL S3 (bảng `hr.ShiftHandover`).
- Model `ShiftHandover` + `ShiftHandoverDao` (insert, findByBranch).
- KPI lead-time: `OrderItemDao.leadTimeStats(branchId)` = AVG DATEDIFF(second, StartedAt, DoneAt) + COUNT cho món SERVED/READY có đủ StartedAt&DoneAt hôm nay.
- `HandoverService` (list/create/getKpi).
- `ShiftHandoverServlet @WebServlet("/barista/handover")` (doGet list+KPI, doPost create).
- View `barista/handover.jsp` + sidebar link.

### 1.3 · B4 Prep +updateBatch +cancelBatch — [M, bẫy C4] = **CRUD đầy đủ #1**
- SQL S1.
- `InventoryService`:
  - `cancelPrepBatch(branchId, batchId, userId)`: trong 1 tx — đọc batch (chặn nếu đã CANCELLED), đảo: với mỗi PrepRecipe `applyTxn(+consumed, PREP_OUT?)`… **chính xác**: ghi txn BÙ = cộng lại RAW (`+consumed`, type `PREP_OUT` mang dấu dương để net=0) và trừ PREPPED (`-qtyProduced`, type `PREP_IN` mang dấu âm). Đánh dấu `Status='CANCELLED', VoidedAt=now`. *(RefTable='PrepBatch', RefId=batchId — truy vết).* ⚠️ Nếu tồn PREPPED đã bị tiêu thụ < qtyProduced thì vẫn ghi txn bù (tồn có thể âm tạm — chấp nhận, đúng nguyên tắc ledger; cảnh báo low-stock tự bắn).
  - `updatePrepBatch(branchId, batchId, newQtyProduced, userId)`: tính delta = new − old; áp txn cho phần delta (RAW: −delta×ratio PREP_OUT; PREPPED: +delta PREP_IN), cập nhật `QuantityProduced`. (Hoặc đơn giản: cancel cũ rồi tạo mới — nhưng giữ updateBatch theo delta cho gọn lịch sử.)
- `PrepBatchDao`: +`findById`, +`updateStatus`, +`updateQuantity`; `findByBranch` đọc thêm `Status`.
- `PrepService`: +`cancelBatch`, +`updateBatch`.
- `PrepServlet`: +action `cancelBatch`, `updateBatch`.
- `prep.jsp`: cột Trạng thái + nút Huỷ/Sửa (chỉ khi ACTIVE).

### 1.4 · B5 Waste +update +delete — [S-M, bẫy C4] = **CRUD đầy đủ #2**
- SQL S2.
- `InventoryService`:
  - `updateWaste(branchId, wasteLogId, newQty, wasteType, reason, userId)`: delta = newQty − oldQty; `applyTxn(−delta, WASTE)` (delta>0 trừ thêm, <0 hoàn lại); cập nhật WasteLog.
  - `voidWaste(branchId, wasteLogId, userId)`: `applyTxn(+oldQty, WASTE)` (hoàn kho) + `Status='VOIDED'`. **Không xoá cứng.**
- `WasteLogDao`: +`findById`, +`update`, +`updateStatus`; `findByBranch` đọc thêm `Status`.
- `WasteService`: +`updateWaste`, +`voidWaste`.
- `WasteServlet`: +action `update`, `void`.
- `waste.jsp`: cột Trạng thái + nút Sửa/Huỷ (chỉ khi ACTIVE).

**Mốc kiểm Ưu tiên 1:** Barista = 7 màn (KDS, Pickup, Prep, Waste, 86, Recipe, Handover), 2 CRUD đầy đủ (Prep, Waste). Chạy golden path Barista + audit lại.

---

## 3. ƯU TIÊN 2 — CASHIER Order Inbox 🟠

C4 `/cashier/inbox`. **Mô hình chốt:** giữ auto-to-KDS hiện tại — Inbox đóng vai **monitor + void đơn sai**, KHÔNG biến thành cổng chặn (không phá luồng đặt đơn).
- `OrderService`: +`getIncomingOrders(branchId)` (đơn ACTIVE, kèm itemCount/source/table/thời điểm), +`voidOrder(orderId, userId)` (Order.Status='CANCELLED' + OrderItem chưa SERVED → CANCELLED, publish `order.status_changed`; **không trừ kho** vì chưa deduct). `confirmOrder` = no-op/ack tuỳ chọn (đánh dấu đã xem) — tối thiểu bỏ qua, ghi chú trong UI.
- `OrderDao`: +`findActiveByBranch`, +query itemCount.
- `OrderInboxServlet @WebServlet("/cashier/inbox")` + view `cashier/inbox.jsp` + sidebar link.

---

## 4. ƯU TIÊN 3 — Bug thật 🟡

### 3.1 · "Toggle giả" Branch / Product / Voucher / Supplier — [S, gộp 1 lần]
`toggleActive` hiện luôn truyền `false` → không bật lại. Sửa: đọc trạng thái hiện tại rồi đảo (service `setXxxActive(id, !current)` hoặc thêm `toggleActive(id)` đọc-đảo trong 1 tx).
- File: `BranchServlet`+`BranchService`, `ProductServlet`+`ProductService`, `VoucherServlet`+`VoucherService`, `SupplierServlet`+`SupplierService` (+DAO `findById`/đọc cờ nếu thiếu).

### 3.2 · C5 Split bill no-drift — [M]
- VAT: tính **VAT tổng một lần** trên subtotal gốc rồi **phân bổ theo tỷ lệ** subtotal từng bill, dồn phần dư (remainder) vào bill cuối → tổng VAT các bill = VAT bill gốc (không lệch ±0.01).
- Voucher: khi tách, **chia discount theo tỷ lệ** subtotal (largest-remainder), không để bill tách mất discount.
- File: `BillingService.splitItems` + `BillCalculator` (thêm helper phân bổ). Giữ/ thêm test trong `BillCalculatorTest`.

---

## 5. ƯU TIÊN 4 — Nâng chất (nếu còn thời gian) 🟢
- KDS polling 3–5s (`kds.jsp` AJAX hoặc meta refresh) — fix cảm giác "luồng chết".
- M1 doanh thu hôm nay (dashboard manager); M4 export Excel/CSV payroll.
- A5 Recipe `updateLine`; Modifier delete-group / update-option.
- B1 `bump`; C6 history lọc-theo-ca + reprint + void-kèm-lý-do.
- A1 forgot/reset password (nếu cần).

---

## 6. KHÔNG làm (skip có chủ đích)
- **Quick-create customer (A1.F4)**: loyalty đã bỏ → gần như không dùng. Chỉ làm nếu POS thực sự cần gắn khách theo SĐT.
- **Refactor hard-code status → `OrderItemStatus.*`**: cosmetic, giá trị vẫn nhất quán → không phải bug.
- **Gold-plate Admin/Manager**: đã đạt.

---

## 7. Thứ tự thực thi & checklist commit

1. SQL migration (S1,S2,S3) → cập nhật `database.sql` + `migration_audit_fix.sql`. *(không commit riêng — đi cùng mục dùng nó)*
2. **Ưu tiên 1** (1.1 → 1.2 → 1.3 → 1.4) — commit **sau mỗi mục**.
3. `mvn test-compile` + chạy thử golden path Barista → **audit lại Barista**.
4. **Ưu tiên 2** (Inbox) — commit.
5. **Ưu tiên 3** (toggle gộp 1 commit; split bill 1 commit).
6. **Ưu tiên 4** (nếu còn thời gian) — commit từng mục.

Mỗi commit: `mvn -o test-compile` xanh + message mô tả mục + cập nhật `docs/PROGRESS.md`.

### Acceptance
- [ ] Barista ≥7 màn, ≥2 CRUD đầy đủ; golden path pha chế vẫn chạy; deduct/ledger không đổi hành vi.
- [ ] Cancel Prep / void Waste sinh **txn bù** ở `InventoryTransaction` (không hard-delete, không UPDATE thẳng tồn).
- [ ] Cashier Inbox liệt kê đơn QR+quầy, void được đơn sai, không chặn luồng đặt.
- [ ] Toggle Branch/Product/Voucher/Supplier bật-tắt được 2 chiều.
- [ ] Split bill: tổng VAT & discount các bill = bill gốc (test xanh).
- [ ] JSP không scriptlet; transaction ở Service; lọc branch_id giữ nguyên.
