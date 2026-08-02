# KẾ HOẠCH DỌN CODE ROLE BARISTA

> **Mục đích file này:** giữ agent/người làm bám đúng một phạm vi đã chốt, không lan sang việc khác.
> Mỗi lần bắt đầu phiên làm việc mới → **đọc lại mục "Nguyên tắc" và "Ngoài phạm vi" trước tiên.**
>
> Nguồn gốc: kết quả rà soát toàn bộ 11 file Java + 6 JSP + 4 fragment + 1 JS của role Barista (2026-08-02).
> Trạng thái tổng: 🟢 Đợt 1 xong (2026-08-02) · ⚪ Đợt 2, 3 chưa bắt đầu

---

## 0. Nguyên tắc bất di bất dịch

Áp dụng cho **mọi** task trong file này. Vi phạm một điều = dừng lại, hỏi lại người dùng.

| # | Nguyên tắc |
|---|---|
| N1 | **KHÔNG đổi hành vi.** Đây là refactor thuần. Sau mỗi đợt, app phải chạy y hệt trước đó — cùng URL, cùng thông báo, cùng dữ liệu ghi vào DB. |
| N2 | **KHÔNG sửa bug phát hiện được dọc đường.** Ghi vào mục [§6 Sổ ghi nhận](#6-sổ-ghi-nhận-phát-sinh) rồi đi tiếp. Sửa bug là task riêng, commit riêng. |
| N3 | **KHÔNG động vào file ngoài danh sách §1.** Kể cả khi thấy file khác cũng bẩn tương tự. |
| N4 | **Mỗi đợt = 1 commit.** Không gộp đợt. Commit message chỉ ghi thao tác, **không** thêm trailer `Co-Authored-By`. |
| N5 | **Chạy `mvn -q test` sau mỗi đợt.** Đỏ thì sửa hoặc revert, không commit đè lên test đỏ. |
| N6 | **Không thêm dependency mới** vào `pom.xml`. |
| N7 | **Không đổi tên file/class công khai** ở Đợt 1–2. Việc đổi tên nằm ở Đợt 3 và phải hỏi trước. |
| N8 | Comment và tên biến viết **tiếng Việt** cho phần diễn giải nghiệp vụ, giữ nguyên quy ước hiện có của repo. |

### Ràng buộc kiến trúc (ArchUnit sẽ đánh trượt nếu phạm)

Đã kiểm tra `src/test/java/com/cafe/architecture/MvcArchitectureTest.java`:

- `common/` **không được** phụ thuộc `jakarta.servlet..` → helper đọc request **phải** đặt ở `web/support/`, **không** đặt ở `common/`.
- `controller/`, `web/` **không được** chạm `dao/`, `java.sql..`, `DBConnection`.
- `service/` **không được** phụ thuộc `controller/`, `web/`.
- `model/` phải độc lập, không dính servlet/sql.
- Các package con của `service/` phải không có chu trình.

---

## 1. Phạm vi — danh sách file được phép sửa

Chỉ 12 file Java + 6 JSP + 4 fragment + 1 JS dưới đây. Ngoài danh sách này là **ngoài phạm vi**.

### Java (main)
| File | Dòng | Vai trò |
|---|---|---|
| `controller/barista/KdsServlet.java` | 307 | Màn quầy pha chế |
| `controller/barista/WasteServlet.java` | 311 | Hao hụt nguyên liệu |
| `controller/barista/EightySixServlet.java` | 153 | Báo hết món (86) |
| `controller/barista/PrepServlet.java` | 131 | Pha sẵn |
| `controller/barista/MyShiftServlet.java` | 126 | Ca làm của tôi |
| `controller/barista/RecipeLookupServlet.java` | 157 | Tra cứu công thức |
| `service/barista/KdsService.java` | 483 | Logic quầy pha |
| `service/barista/WasteService.java` | 270 | Logic hao hụt |
| `service/barista/PrepService.java` | 156 | Logic pha sẵn |
| `web/support/BaristaShiftSupport.java` | 99 | Guard trực ca |
| `web/support/BaristaWritePolicy.java` | 48 | Allowlist action |
| `web/support/RequestParams.java` | *(mới)* | Helper dùng chung — tạo ở Đợt 2 |

### View / asset
`views/barista/{kds,waste,eightysix,prep,shift,recipe}.jsp` ·
`fragments/barista/kds/{cards,queue-row,ingredient-picker,recount-picker}.jsp` ·
`assets/js/barista/kds-board.js`

### Test (chỉ được sửa khi task nói rõ)
`test/.../service/barista/*` · `test/.../controller/barista/*`

---

## 2. NGOÀI PHẠM VI — tuyệt đối không đụng

Đây là mục quan trọng nhất để chống lệch task. Nếu thấy "chỗ này cũng nên sửa" mà nó nằm dưới đây → **ghi vào §6, không sửa.**

- ❌ `service/shared/OrderService.java` — dù dòng 50 nhồi 7 method trên một dòng, rất bẩn. **Ảnh hưởng Cashier + QR + Manager**, không phải việc của đợt này.
- ❌ `service/shared/InventoryService.java`, `OrderQueryService.java`, `CatalogReadService.java`, `BranchMenuService.java`
- ❌ Bất kỳ file nào trong `controller/manager/`, `controller/cashier/`, `controller/admin/`, `controller/customer/`
- ❌ `controller/manager/WasteReportServlet.java` — có helper trùng với barista, nhưng **để Đợt 2 xử lý riêng sau**, không kéo vào cùng commit.
- ❌ `dao/**` — không có việc gì ở tầng DAO trong kế hoạch này.
- ❌ `sql/**`, `src/main/resources/db/migration/**` — **không đổi schema.**
- ❌ `pom.xml`, `.github/workflows/**`
- ❌ CSS/theme, layout chung (`views/layout/**`) — trừ `_baristaShiftBanner.jsp` nếu Đợt 3 cần, và phải hỏi trước.
- ❌ Viết thêm test mới cho logic chưa có test. (Việc tốt, nhưng là task khác.)

---

## 3. Đợt 1 — Xoá code chết & sửa comment sai

> **Rủi ro: rất thấp.** Chỉ xoá và sửa chữ, không viết logic mới.
> **Mục tiêu:** giảm ~90 dòng chết + hết comment nói sai.
> Trạng thái: 🟢

### 1.1 — Sửa 3 javadoc gắn nhầm hàm 🟢

Đây là thứ khiến đọc code hiểu sai nhất. Comment còn sót từ lần refactor trước, mô tả code **đã bị chuyển sang file khác**.

- [x] `KdsServlet.java:234-242` — có **hai javadoc chồng lên một hàm** `pageParam`. Cái đầu ("Số dòng mỗi trang. Chọn 12...") mô tả hằng số `QUEUE_PAGE_SIZE` hiện nằm ở `KdsService.java:22`.
  → **Chuyển** nội dung đó sang đặt trên `KdsService.QUEUE_PAGE_SIZE`; giữ lại **một** javadoc đúng cho `pageParam`.
- [x] `KdsServlet.java:244-251` — javadoc mô tả *"Thứ tự danh sách một cột: việc còn phải làm... món ĐÃ pha xong dồn xuống cuối"* nhưng đang gắn trên `flashConflict()` — hàm chỉ set một câu thông báo.
  → **Chuyển** sang `KdsService.pourOrder()` (`KdsService.java:202`); viết javadoc mới đúng 1 dòng cho `flashConflict`.
- [x] `KdsServlet.java:25` và `KdsServlet.java:220-224` — ghi *"ba cột"* nhưng UI thực tế là **một cột** (xác nhận ở `KdsService.java:60-63` và `fragments/barista/kds/cards.jsp`).
  → Sửa lại thành mô tả đúng: một danh sách hàng chờ + dải số liệu trạng thái.

**Nghiệm thu:** không còn javadoc nào trong `controller/barista/` mô tả hàm/hằng số nằm ở file khác.

### 1.2 — Xoá method chết trong `KdsService.java` 🟢

Đã verify bằng grep toàn repo (`src/main` + `src/test` + `webapp`), 0 caller:

- [x] `getQueue` (dòng 41)
- [x] `getWorkbenchBoard` — cả **2 overload** (dòng 48, 53)
- [x] `getStaleItems` (dòng 70)
- [x] `getBranchOpenTime` (dòng 76)
- [x] `getBranch` (dòng 82) — `loadBoard` đã gọi thẳng `branchService.getBranch`
- [x] `estimateLastWaitSeconds` (dòng 238) — ⚠️ **chỉ có test gọi**, production không dùng.
  → Xoá kèm 4 assert ở `KdsPeakTest.java:41-44`.
  → **CHECKPOINT: hỏi người dùng trước khi xoá cái này** — nếu họ định dùng cho tính năng "ước tính thời gian chờ" sau này thì giữ lại.
- [x] Sau khi xoá: dọn import thừa trong `KdsService.java` (`OrderGroupInfo` và các import khác nếu không còn dùng).

### 1.3 — Xoá method chết trong `PrepService.java` 🟢

6/15 method là code chết:

- [x] `getTodayBatches` (36) · `getTodayBatchPage` (40) · `createBatch` (54) · `createBatches` (66) · `cancelBatch` (71) · `writeOffExpiredBatch` (76)
- [x] ⚠️ Lưu ý: `writeOffExpiredBatchSuggested` (80) **có gọi** `writeOffExpiredBatch` bên trong → khi xoá phải inline lời gọi `inventoryService.writeOffExpiredPrepBatch(...)` vào thẳng, **không xoá cụt**.
- [x] ⚠️ `"cancelBatch"` là action **của Manager** (`ManagerPrepServlet.java:50` + `views/manager/prep-list.jsp:121`) — xoá method ở `PrepService` **không** ảnh hưởng, nhưng **không được** đụng tới file Manager.

### 1.4 — Xoá method chết trong `WasteService.java` 🟢

- [x] `logIngredientWasteLines` (dòng 129) — 0 caller (`logIngredientWasteBatch` mới là đường đang dùng).

### 1.5 — Dọn thụt lề & vị trí khai báo 🟢

- [x] Thụt lề thừa 4 space: `KdsServlet.java:57, 64` · `WasteServlet.java:166, 175` · `PrepServlet.java:117`
- [x] `KdsServlet.java:254` — hằng số `BLOCKING_REASONS` khai báo ở giữa file, sau cả loạt method, trong khi dùng từ dòng 147 → **đưa lên đầu class** cùng các field khác.
- [x] `WasteServlet.java:195` — comment tiếng Anh duy nhất giữa toàn bộ comment tiếng Việt → dịch sang tiếng Việt.

### 1.6 — Dọn import 🟢

- [x] `KdsService.java` — import `com.cafe.model.Branch` ở dòng 4 nhưng dòng 77, 82 lại viết full-qualified `com.cafe.model.Branch`. Thống nhất dùng tên ngắn.
- [x] `KdsService.java:53, 64, 438, 452, 464, 470` — thay `java.time.LocalDateTime`, `java.util.List`, `java.util.Set`, `com.cafe.model.StockAdjustment`, `com.cafe.model.Recipe` viết đầy đủ bằng import + tên ngắn. Chữ ký hàm sẽ ngắn đi một nửa.
- [x] 5 controller barista: `com.cafe.web.support.BranchContext.requireBranchId(req)` → import `BranchContext`.

### ✅ Nghiệm thu Đợt 1

```bash
mvn -q test                    # phải xanh
mvn -q clean package           # WAR build được
```
- [x] `git diff --stat` cho thấy **chỉ có dòng bị xoá và dòng comment/import bị sửa**, không có logic mới.
- [x] Commit: `refactor(barista): xoá code chết và sửa comment sai lệch`

---

## 4. Đợt 2 — Gom trùng lặp

> **Rủi ro: trung bình.** Có tạo file mới và đổi chỗ code, nhưng vẫn không đổi hành vi.
> Trạng thái: ⚪ · **Chỉ bắt đầu sau khi Đợt 1 đã commit.**

### 2.1 — Tách helper đọc request dùng chung ⚪

4 controller barista đang copy y hệt nhau: `positiveIntParam`, `textParam`, `normalizePageSize`, `blank`.

- [ ] Tạo `web/support/RequestParams.java` — final class, constructor private, toàn static.
  ⚠️ **Bắt buộc đặt ở `web/support/`, KHÔNG đặt ở `common/`** — ArchUnit rule `common_must_not_depend_on_web_or_jdbc` sẽ đánh trượt vì nó cần `jakarta.servlet`.
- [ ] Method: `text(req, name, maxLength)` · `positiveInt(req, name, fallback)` · `optionalInt(req, name)` · `allowed(req, name, String... values)` · `isBlank(value)`
- [ ] ⚠️ **`normalizePageSize` KHÔNG gom được** — mỗi màn có bộ giá trị khác nhau:
  `WasteServlet` = {5,10,20,50} mặc định 5 · `MyShiftServlet` = {5,10,20,50} mặc định 10 · `EightySixServlet` = {10,20,50} mặc định 10.
  → Giữ riêng ở từng controller, **không** ép chung.
- [ ] Thay thế lần lượt ở: `WasteServlet` → `MyShiftServlet` → `EightySixServlet` → `PrepServlet`. Mỗi file xong chạy test rồi mới sang file kế.
- [ ] ❌ **Không** kéo `manager/WasteReportServlet.java` vào commit này (xem §2).

### 2.2 — Gom bảng lý do vào một chỗ ⚪

Hiện chép làm 2 nơi, sửa 1 nơi quên nơi kia là ghi chuỗi rỗng vào DB:
- `KdsServlet.java:274-293` (2 Map Java)
- `views/barista/kds.jsp:57, 75` (hardcode `<option>`)

- [ ] Tạo enum trong `common/`: `IssueReason` (OUT_OF_STOCK, EQUIPMENT, NOTE_UNSUPPORTED, DISCONTINUED, UNCLEAR_ORDER, OTHER) và `RemakeReason` (WRONG_RECIPE, SPILLED, QUALITY, CUSTOMER_FEEDBACK, WRONG_DELIVERY, CHANGED_REQUEST), mỗi hằng có `label()` tiếng Việt.
  → Enum thuần, không đụng servlet/sql ⇒ hợp lệ với ArchUnit.
- [ ] `KdsServlet` đọc label từ enum thay cho `Map.of` inline.
- [ ] `kds.jsp` render `<option>` bằng vòng lặp JSTL trên list enum đẩy qua request attribute, **không** hardcode nữa.
- [ ] ⚠️ Giữ nguyên **đúng từng ký tự** của các chuỗi label hiện tại — đây là dữ liệu ghi xuống DB, đổi chữ là đổi hành vi (vi phạm N1).
- [ ] `BLOCKING_REASONS` (`KdsServlet.java:254`) chuyển thành thuộc tính `isBlocking()` trên enum `IssueReason`.

### 2.3 — Dùng `OrderItemStatus` thay chuỗi hardcode ⚪

`common/OrderItemStatus.java:6` ghi rõ: *"Không hard-code các chuỗi này rải rác trong code/JSP"* — nhưng `KdsService` hardcode **12 lần**.

- [ ] Thay ở `KdsService.java`: dòng 97, 114, 204, 205, 250, 251, 252, 253, 268, 287, 288, 291.
- [ ] Cách an toàn: so sánh `OrderItemStatus.WAITING.name().equals(item.getStatus())` hoặc thêm helper `is(OrderItemStatus)` trên `OrderItem`.
  ⚠️ **Không** đổi kiểu field `status` của `OrderItem` từ `String` sang enum ở đợt này — sẽ lan sang Cashier/QR (vi phạm N3).
- [ ] Khoá map `"waiting"/"inProgress"/"ready"/"blocked"` trong `splitWorkbench` **giữ nguyên** — JSP đang đọc theo tên này.

### 2.4 — Bỏ map dữ liệu 2 lần ⚪

- [ ] `WasteServlet.java:78-86` — cùng `form.lines()` bị `.map()` sang `WasteRowForm` rồi lại `.map()` sang `WasteLineInput`, hai record 5 field giống hệt.
  → Duyệt một lần, dựng cả hai list trong cùng vòng lặp; hoặc để `WasteRowForm` có method `toLineInput()`.

### ✅ Nghiệm thu Đợt 2
```bash
mvn -q test && mvn -q clean package
```
- [ ] Test thủ công 4 màn: mở `/barista/kds` báo sự cố (đủ 6 lý do), `/barista/waste` ghi + lọc + phân trang, `/barista/eightysix` phân trang, `/barista/shift` đổi tháng.
- [ ] Commit: `refactor(barista): gom helper request, enum lý do và trạng thái món`

---

## 5. Đợt 3 — Cấu trúc (cần hỏi trước khi làm)

> **Rủi ro: cao hơn.** Đụng vào cấu trúc class.
> Trạng thái: ⚪ · **Phải hỏi người dùng xác nhận từng mục trước khi bắt tay.**

- [ ] **`KdsBoardData` → `record`** (`KdsService.java:139-200`). 62 dòng boilerplate cho 16 field, constructor 16 tham số không tên gọi rất dễ truyền nhầm thứ tự.
  ⚠️ JSP gọi `board.getWaitingCount()`... — record sinh accessor `waitingCount()` **không có tiền tố `get`**, JSTL EL sẽ **gãy**. Phải hoặc giữ getter thủ công, hoặc sửa đồng loạt mọi `${board.xxx}` trong `cards.jsp` + `kds.jsp`. **Kiểm tra kỹ trước khi chọn.**
- [ ] **Tách `KdsServlet.doPost`** (`:104-175`): if-else 70 dòng, 9 nhánh action, và **4 khối catch lặp gần hệt nhau** (`:177-191`) → mỗi nhánh một private method + gom xử lý lỗi vào một chỗ.
- [ ] **Gộp 4 constructor telescoping** của `WasteService` (`:46-65`) — 3 cái package-private chỉ để phục vụ test.
- [ ] **`PrepService.getRecipeJson` / `getRawOnHandJson`** (`:103-155`): build JSON bằng `StringBuilder` thủ công + hàm `esc()` tự viết escape 7 ký tự.
  → Repo đã có `jackson-databind` trong `pom.xml`. Nhưng đây là đổi cách sinh chuỗi ⇒ **rủi ro đổi hành vi**, phải so sánh output byte-by-byte trước/sau.
- [ ] **Đổi tên `pourOrder` → `sortForBrewing`** (`KdsService.java:202`). Trong file đầy `orderId`/`OrderItem`, đọc `pourOrder` rất dễ tưởng là "đơn hàng đang rót", trong khi nghĩa thật là "thứ tự pha".
- [ ] **Tách `KdsService` (483 dòng)** đang ôm 4 vai trò: delegate mỏng sang `OrderService` (17 method 1 dòng), hàm thuần static, DTO `KdsBoardData`, và `QueuePage`.
  → Đề xuất: tách `KdsBoardData` + `QueuePage` ra file riêng cùng package.
- [ ] **Cắt dòng dài >120 ký tự** trong JSP: `waste.jsp` 58 dòng · `recipe.jsp` 43 · `queue-row.jsp` 39 · `eightysix.jsp` 29 · `shift.jsp` 24 · `kds.jsp` 19 · `prep.jsp` 17 · `kds-board.js` 16.

---

## 6. Sổ ghi nhận phát sinh

> Thấy vấn đề nằm ngoài phạm vi → ghi vào đây, **không sửa** (N2, N3).

| Ngày | File:dòng | Vấn đề | Loại | Xử lý sau |
|---|---|---|---|---|
| 2026-08-02 | `service/shared/OrderService.java:50` | 7 method nhồi trên 1 dòng, tên tham số 1 ký tự (`int b`, `LocalDateTime d`) | Khó đọc | Task riêng — ảnh hưởng Cashier + QR + Manager |
| 2026-08-02 | `controller/manager/WasteReportServlet.java` | Trùng helper `positiveIntParam`/`textParam` với barista | Trùng lặp | Sau Đợt 2, dùng lại `RequestParams` |
| 2026-08-02 | `sql/migration-checksums.sha256` | **Test `MigrationChecksumTest` đang ĐỎ SẴN** trên nhánh `minhnhat`. Commit `f95a9ff` ("tối giản schema từ 49 xuống 25 bảng") đổi `V1__database.sql` nhưng không cập nhật manifest checksum. Đã verify bằng `git stash` — không liên quan Đợt 1. | Test đỏ | ❗Cần xử lý riêng, thuộc phần DB (§2 cấm đụng) |
| 2026-08-02 | `service/barista/PrepService.java` | `updateBatch` cũng là code chết (0 caller), nhưng **không nằm trong danh sách §1.3** nên Đợt 1 giữ nguyên | Code chết | Gộp vào Đợt 2 |
| 2026-08-02 | `service/barista/KdsService.java` | 4 field list `waitingItems`/`inProgressItems`/`readyItems`/`blockedItems` của `KdsBoardData` **không được JSP dùng** — JSP chỉ đọc `*Count`. Nạp list rồi vứt đi. | Code thừa | Đợt 3, khi làm `KdsBoardData` → record |
| 2026-08-02 | `fragments/barista/kds/queue-row.jsp:45` | Comment nhắc khu "Đơn treo" — khu này **không còn trong UI** (đã xác nhận `getStaleItems` là code chết) | Comment sai | Đợt 2 hoặc 3, khi đụng JSP |

---

## 7. Bảng theo dõi

| Đợt | Nội dung | Trạng thái | Commit | Ghi chú |
|---|---|---|---|---|
| 1 | Xoá code chết + sửa comment sai | 🟢 | `2f49816` | −170/+128 dòng · test 344/345 (1 đỏ sẵn từ trước, xem §6) · WAR build OK |
| 2 | Gom trùng lặp | ⚪ | | Sẵn sàng bắt đầu |
| 3 | Cấu trúc | ⚪ | | Cần hỏi trước |

Chú thích: ⚪ chưa làm · 🟡 đang làm · 🟢 xong · 🔴 bị chặn

---

## 8. Từ điển thuật ngữ

Tên tiếng Anh trong code là **jargon ngành F&B**, không phải đặt bừa — nhưng không có chỗ nào giải thích, nên ghi lại đây:

| Tên | Nghĩa | Xuất hiện ở |
|---|---|---|
| **KDS** | Kitchen Display System — màn hình bếp | `KdsServlet`, `KdsService`, `kds-board.js` |
| **86 / EightySix** | Tiếng lóng nhà hàng: "hết món, ngưng bán" | `EightySixServlet`, `Reason86`, `Menu86Validator` |
| **86 (soft)** | 86 tự động sinh ra do hết nguyên liệu | `EightySixServlet:53` |
| **Workbench** | Bàn thao tác của barista = hàng chờ pha | `getBaristaWorkbench`, `splitWorkbench` |
| **pourOrder** | "Thứ tự rót" = thứ tự pha. **KHÔNG** phải đơn hàng (`Order`) | `KdsService:202` |
| **Prep / PREPPED** | Nguyên liệu pha sẵn (đối lập `RAW`) | `PrepService`, `PrepBatch` |
| **Ledger / txn bù** | Sổ cái kho — sửa/huỷ ghi giao dịch đối ứng, không xoá cứng | `InventoryService`, `WasteService` |
| **Business day** | Ngày kinh doanh, cắt theo giờ mở cửa chi nhánh chứ không phải nửa đêm | `BusinessDay`, `WasteScope` |
| **Stale item** | Món dang dở còn sót từ ngày kinh doanh trước | `getStaleItems` |
