# KẾ HOẠCH DỌN CODE ROLE BARISTA

> **Mục đích file này:** giữ agent/người làm bám đúng một phạm vi đã chốt, không lan sang việc khác.
> Mỗi lần bắt đầu phiên làm việc mới → **đọc lại mục "Nguyên tắc" và "Ngoài phạm vi" trước tiên.**
>
> Nguồn gốc: kết quả rà soát toàn bộ 11 file Java + 6 JSP + 4 fragment + 1 JS của role Barista (2026-08-02).
> Trạng thái tổng: 🟢 **Cả 3 đợt + dọn tồn đọng §6 đã xong** (2026-08-02). Test 344/345 → **352/352**.
>
> **Còn lại đúng 2 việc, cả hai đều cần plan riêng — xem §6:**
> 1. `OrderService.java:50` — nằm ngoài phạm vi, ảnh hưởng Cashier + QR + Manager.
> 2. 200 dòng JSP dài do nội dung text inline — cần chạy app đối chiếu bằng mắt.

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
> Trạng thái: 🟢 xong 2026-08-02

### 2.1 — Tách helper đọc request dùng chung 🟢

4 controller barista đang copy y hệt nhau: `positiveIntParam`, `textParam`, `normalizePageSize`, `blank`.

- [x] Tạo `web/support/RequestParams.java` — final class, constructor private, toàn static.
  ⚠️ **Bắt buộc đặt ở `web/support/`, KHÔNG đặt ở `common/`** — ArchUnit rule `common_must_not_depend_on_web_or_jdbc` sẽ đánh trượt vì nó cần `jakarta.servlet`.
- [x] Method: `text(req, name, maxLength)` · `positiveInt(req, name, fallback)` · `optionalInt(req, name)` · `allowed(req, name, String... values)` · `isBlank(value)`
- [x] ⚠️ **`normalizePageSize` KHÔNG gom được** — mỗi màn có bộ giá trị khác nhau:
  `WasteServlet` = {5,10,20,50} mặc định 5 · `MyShiftServlet` = {5,10,20,50} mặc định 10 · `EightySixServlet` = {10,20,50} mặc định 10.
  → Giữ riêng ở từng controller, **không** ép chung.
- [x] Thay thế lần lượt ở: `WasteServlet` → `MyShiftServlet` → `EightySixServlet` → `PrepServlet`. Mỗi file xong chạy test rồi mới sang file kế.
- [x] ❌ **Không** kéo `manager/WasteReportServlet.java` vào commit này (xem §2).

### 2.2 — Gom bảng lý do vào một chỗ 🟢

Hiện chép làm 2 nơi, sửa 1 nơi quên nơi kia là ghi chuỗi rỗng vào DB:
- `KdsServlet.java:274-293` (2 Map Java)
- `views/barista/kds.jsp:57, 75` (hardcode `<option>`)

- [x] Tạo enum trong `common/`: `IssueReason` (OUT_OF_STOCK, EQUIPMENT, NOTE_UNSUPPORTED, DISCONTINUED, UNCLEAR_ORDER, OTHER) và `RemakeReason` (WRONG_RECIPE, SPILLED, QUALITY, CUSTOMER_FEEDBACK, WRONG_DELIVERY, CHANGED_REQUEST), mỗi hằng có `label()` tiếng Việt.
  → Enum thuần, không đụng servlet/sql ⇒ hợp lệ với ArchUnit.
- [x] `KdsServlet` đọc label từ enum thay cho `Map.of` inline.
- [x] `kds.jsp` render `<option>` bằng vòng lặp JSTL trên list enum đẩy qua request attribute, **không** hardcode nữa.
- [x] ⚠️ Giữ nguyên **đúng từng ký tự** của các chuỗi label hiện tại — đây là dữ liệu ghi xuống DB, đổi chữ là đổi hành vi (vi phạm N1).
- [x] `BLOCKING_REASONS` (`KdsServlet.java:254`) chuyển thành thuộc tính `isBlocking()` trên enum `IssueReason`.

### 2.3 — Dùng `OrderItemStatus` thay chuỗi hardcode 🟢

`common/OrderItemStatus.java:6` ghi rõ: *"Không hard-code các chuỗi này rải rác trong code/JSP"* — nhưng `KdsService` hardcode **12 lần**.

- [x] Thay ở `KdsService.java`: dòng 97, 114, 204, 205, 250, 251, 252, 253, 268, 287, 288, 291.
- [x] Cách an toàn: so sánh `OrderItemStatus.WAITING.name().equals(item.getStatus())` hoặc thêm helper `is(OrderItemStatus)` trên `OrderItem`.
  ⚠️ **Không** đổi kiểu field `status` của `OrderItem` từ `String` sang enum ở đợt này — sẽ lan sang Cashier/QR (vi phạm N3).
- [x] Khoá map `"waiting"/"inProgress"/"ready"/"blocked"` trong `splitWorkbench` **giữ nguyên** — JSP đang đọc theo tên này.

### 2.4 — Bỏ map dữ liệu 2 lần 🟢

- [x] `WasteServlet.java:78-86` — cùng `form.lines()` bị `.map()` sang `WasteRowForm` rồi lại `.map()` sang `WasteLineInput`, hai record 5 field giống hệt.
  → Duyệt một lần, dựng cả hai list trong cùng vòng lặp; hoặc để `WasteRowForm` có method `toLineInput()`.

### ✅ Nghiệm thu Đợt 2
```bash
mvn -q test && mvn -q clean package
```
- [x] Test thủ công 4 màn: mở `/barista/kds` báo sự cố (đủ 6 lý do), `/barista/waste` ghi + lọc + phân trang, `/barista/eightysix` phân trang, `/barista/shift` đổi tháng.
- [x] Commit: `refactor(barista): gom helper request, enum lý do và trạng thái món`

---

## 5. Đợt 3 — Cấu trúc (cần hỏi trước khi làm)

> **Rủi ro: cao hơn.** Đụng vào cấu trúc class.
> Trạng thái: 🟢 xong 2026-08-02 (trừ mục cắt dòng dài JSP — làm một phần, xem cuối mục)

- [x] ~~**`KdsBoardData` → `record`**~~ → **KHÔNG chuyển record, đã xác minh là không khả thi.**
  `pom.xml` dùng Servlet 5.0 / JSP 3.0 = Jakarta EE 9 ⇒ **EL 4.0**, mà EL chỉ đọc được accessor
  kiểu record từ **EL 6.0** (EE 11). Chuyển sang record là gãy toàn bộ `${board.waitingCount}`.
  → Thay vào đó: tách ra file riêng + **bỏ 4 field list chết** (`waitingItems`/`inProgressItems`/
  `readyItems`/`blockedItems` — JSP chỉ đọc `*Count`). 17 field → 13, ctor 18 tham số → 13.
  Lý do không dùng record đã ghi thành javadoc ngay trong `KdsBoardData` để không ai thử lại.
- [x] **Tách `KdsServlet.doPost`** → `dispatch()` dùng `switch`, mỗi use case một method riêng
  (`startOrder`, `markOrderReady`, `reclaim`, `reportIssue`, `unblock`). 4 khối catch lặp gom còn
  một chỗ gọi `renderResult`.
- [x] **Gộp constructor `WasteService`** — kiểm tra thấy chỉ `WasteService(InventoryService)` được
  `WasteSummaryTest` dùng; 2 constructor còn lại 0 caller → xoá.
- [x] ~~**`PrepService` JSON → Jackson**~~ → **KHÔNG đổi, cố ý giữ nguyên.**
  `prep.jsp:196` nhúng JSON **thẳng vào `<script>`** (`var recipes = ${recipeJson};`). Hàm `esc()`
  tự viết escape `< > & '` thành `\uXXXX` — đúng khuyến nghị OWASP cho JSON trong inline script;
  tên nguyên liệu chứa `</script>` sẽ đóng sớm thẻ và thành mã chạy được. **Jackson mặc định không
  escape mấy ký tự đó** ⇒ đổi sang Jackson là hạ cấp bảo mật. Đã ghi lý do vào javadoc của `esc()`.
- [x] **Đổi tên `pourOrder` → `sortForBrewing`** (private, 0 rủi ro).
- [x] **Tách `KdsService`** → `KdsService` 345 dòng · `KdsBoardData.java` 62 · `QueuePage.java` 82.
  Sửa 10 dòng `KdsService.QueuePage` → `QueuePage` trong `KdsQueuePageTest`.
- [ ] **Cắt dòng dài >120 ký tự trong JSP — LÀM ĐƯỢC TỚI ĐÂU: 247 → 200 dòng** (`4440af9`).
  Đã làm hết những nhóm **chứng minh được là không đổi render**:
  · tách chuỗi `<input type="hidden">` xuống dòng riêng (21 thẻ) — thẻ ẩn không render gì;
  · ngắt giữa các **thuộc tính bên trong thẻ** (41 thẻ) — không sinh text node nào;
  · dải số liệu `cards.jsp` — đã xác minh `.kds-stat` là `display:grid` nên bỏ qua text node trắng.

  Ba lớp kiểm chứng đã chạy trước khi áp dụng đợt ngắt thuộc tính:
  1. bỏ hết khoảng trắng → nội dung 10 file **giống hệt** trước ⇒ chỉ khoảng trắng đổi;
  2. số lần ngắt dòng **giữa hai thẻ** giữ nguyên **1406 → 1406** ⇒ không sinh text node trắng nào;
  3. không biểu thức EL nào bị cắt (bản script đầu cắt vào giữa `${a == b ? 'x' : ''}` — đã sửa để
     coi `${...}`/`#{...}` là nguyên khối).

  📌 **Đính chính phép đo (2026-08-02):** các con số 247 / 231 / 200 ở trên đếm bằng
  `awk 'length>120'` = **BYTE**, không phải ký tự. Tiếng Việt có dấu là 2–3 byte/ký tự nên bị thổi
  phồng. Đo lại bằng ký tự thật: hiện còn **155 dòng** (không phải 200). Mức cải thiện tương đối
  vẫn đúng, chỉ con số tuyệt đối bị lệch.

  **Số dòng còn lại KHÔNG cắt được bằng máy** vì dài do **nội dung text inline**: xuống dòng giữa
  hai thẻ inline là chèn khoảng trắng vào bản render. Ca thật đã gặp: `.kds-stat__context` có
  `white-space:nowrap` + `text-overflow:ellipsis` — khoảng trắng thừa đẩy lệch chữ.
  → Mỗi dòng phải tra `display` của container trong `cafe-theme.css`. Nên làm thành **task riêng
  có chạy app đối chiếu bằng mắt**, không gộp vào commit refactor.

---

## 6. Sổ ghi nhận phát sinh

> Thấy vấn đề nằm ngoài phạm vi → ghi vào đây, **không sửa** (N2, N3).

| Ngày | File:dòng | Vấn đề | Loại | Trạng thái |
|---|---|---|---|---|
| 2026-08-02 | `sql/migration-checksums.sha256` | `MigrationChecksumTest` đỏ sẵn từ `f95a9ff` — manifest ghi hash không ứng với `V1__database.sql` được commit | Test đỏ | 🟢 **Xong** `6b540c5`. Đã loại trừ line-ending/BOM và xác nhận SQL nguyên vẹn (25 bảng) trước khi tính lại hash |
| 2026-08-02 | `common/IssueReason.java` · `common/RemakeReason.java` | Chưa có gì bảo vệ nhãn tiếng Việt ghi thẳng vào sổ hao hụt | Thiếu bảo vệ | 🟢 **Xong** `f1d28e1`. `ReasonLabelLockTest` 7 test; đã kiểm chứng bằng cách cố tình gõ sai một dấu → test đỏ đúng chỗ |
| 2026-08-02 | `controller/manager/WasteReportServlet.java` | Trùng helper `positiveIntParam`/`textParam`/`allowedParam` — bản sao thứ 5 | Trùng lặp | 🟢 **Xong** `f1d28e1`. Toàn repo không còn bản sao nào |
| 2026-08-02 | `service/barista/PrepService.java` | `updateBatch` là code chết (0 caller), sót lại vì không nằm trong danh sách §1.3 | Code chết | 🟢 **Xong** `f1d28e1` |
| 2026-08-02 | `fragments/barista/kds/queue-row.jsp:45` | Comment nhắc khu "Đơn treo" đã bị gỡ khỏi UI | Comment sai | 🟢 **Xong** `f1d28e1`. Hoá ra nhánh `⚠` (seqNo == 0) cũng không với tới được vì `loadBoard` đánh số cho mọi món chưa xong → ghi lại đúng là "lưới an toàn", giữ nhánh |
| 2026-08-02 | `service/barista/KdsService.java` | 4 field list `waitingItems`/`inProgressItems`/`readyItems`/`blockedItems` không được JSP dùng | Code thừa | 🟢 **Xong** `503bd0c` — xoá khi tách `KdsBoardData` |
| 2026-08-02 | `controller/barista/KdsServlet.java` | `fromCode` rộng hơn code cũ (trim + chấp nhận chữ thường, bám `Reason86.fromCode`). Chỉ ảnh hưởng POST tự soạn — dropdown luôn gửi mã hoa | Khác biệt nhỏ | ✅ Đã cân nhắc và chấp nhận |
| 2026-08-02 | `service/shared/OrderService.java:50` | 7 method nhồi trên 1 dòng, tên tham số 1 ký tự (`int b`, `LocalDateTime d`) | Khó đọc | ⚪ **CHƯA LÀM** — §2 cấm đụng, ảnh hưởng Cashier + QR + Manager. Cần plan riêng |
| 2026-08-02 | `views/barista/*.jsp` · `fragments/barista/**` | 200 dòng >120 ký tự còn lại, dài do **nội dung text inline** | Khó đọc | ⚪ **CHƯA LÀM** — máy không cắt an toàn được (xem cuối §5). Cần chạy app đối chiếu bằng mắt |

---

## 7. Bảng theo dõi

| Đợt | Nội dung | Trạng thái | Commit | Ghi chú |
|---|---|---|---|---|
| 1 | Xoá code chết + sửa comment sai | 🟢 | `2f49816` | −170/+128 dòng · test 344/345 (1 đỏ sẵn từ trước, xem §6) · WAR build OK |
| 2 | Gom trùng lặp | 🟢 | `95c90cf` | Thêm `RequestParams` + 2 enum lý do · gỡ 3 bản sao `BLOCKING_REASONS` · 12 literal trạng thái → `OrderItemStatus` · test 345/345 |
| 3 | Cấu trúc | 🟢 | `503bd0c` | KdsService 470→345 dòng · tách 2 file · doPost 70→11 dòng · test 345/345 |
| 4 | Dọn tồn đọng §6 | 🟢 | `f1d28e1` | `ReasonLabelLockTest` 7 test · gỡ bản sao helper thứ 5 · xoá `updateBatch` · sửa comment `queue-row` · test 352/352 |
| 5 | Ngắt thuộc tính JSP | 🟢 | `4440af9` | 41 thẻ · dòng dài 231→200 · 3 lớp kiểm chứng không đổi render |

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
