# Kế hoạch: Báo tạm hết món có kiểm soát (Barista → Manager duyệt)

> Trạng thái: **chưa làm** (trừ mục 0 đã xong).
> Phạm vi: 7 file, 2 module (barista + manager). Mức thay đổi: **L2** (thêm bảng DB, thêm route).

---

## Vấn đề đang có

Màn `/barista/eightysix` hiện tại cho barista thao tác tuỳ ý:

| Lỗ hổng | Chỗ cụ thể |
|---|---|
| ETA để trống vẫn báo hết được | `EightySixServlet.doPost` — `etaStr` null thì bỏ qua |
| ETA sai định dạng bị nuốt thành `null` | `EightySixServlet.java:51-52` — `catch (DateTimeParseException ignore)` |
| Không có lý do | `catalog.BranchMenu` chỉ có `Is86` + `BackInEta` |
| Không biết ai báo, lúc nào | Chỉ có `ops.OutboxEvent` (log, không ai đọc) |
| Barista tự bấm "Mở bán lại", không ai duyệt | `eightysix.jsp:72-78` |
| Nút gợi ý báo hết 1 click, không ETA không lý do | `eightysix.jsp:17-24` |

Món bị 86 là bị khoá khỏi **POS + menu QR** → ảnh hưởng trực tiếp doanh thu. Cần siết.

---

## Quyết định thiết kế (đã chốt)

1. **Báo tạm hết có hiệu lực NGAY**, không chờ duyệt. Nguyên liệu hết là sự thật vật lý — chờ manager 10 phút thì khách vẫn đặt được món không pha nổi.
2. **Chiều mở bán lại mới là chiều bị siết.** Barista không có nút "Mở bán lại"; chỉ có "Xin mở bán lại" → manager quyết.
3. **Không có auto-reopen** khi tồn kho về dương. Hai cửa mở bán song song sẽ đá nhau, quyền kiểm soát vừa dựng lên đã thủng. Một cửa duy nhất: manager.
4. **Quá ETA không tự mở bán** — chỉ đẩy lên đầu danh sách manager kèm nhãn "quá hạn". Tự mở một món không pha được thì nguy hiểm hơn là để khoá.
5. **Chip ghi chú không lưu vào DB.** Chip là lối tắt điền form; thống kê group theo `Reason` là đủ.
6. **`Is86` ≠ `IsAvailable`.** Tạm hết (barista, ngắn hạn) khác ngừng bán (manager, dài hạn). Không gộp hai cờ.

### Máy trạng thái

```
Barista "Báo tạm hết"
   → BranchMenu.Is86 = 1, BackInEta = <eta>     (khoá POS + QR NGAY)
   → MenuBlockRequest mới: PENDING, ClosedAt = NULL

Barista "Xin mở bán lại"
   → MenuBlockRequest.ReopenRequestedAt = now   (KHÔNG đổi Is86)

Manager duyệt      PENDING → APPROVED    Is86 giữ 1
Manager từ chối    PENDING → REJECTED    Is86 = 0, BackInEta = NULL, ClosedAt = now
Manager mở bán lại PENDING|APPROVED → RESOLVED   Is86 = 0, BackInEta = NULL, ClosedAt = now
```

`REJECTED` = "barista báo sai, mở lại ngay". `RESOLVED` = "đã hết thật, giờ có lại rồi". Hai cái khác nhau về ý nghĩa thống kê nên tách.

---

## 0. ĐÃ XONG — tầng validate

Không phải làm lại, chỉ việc gọi:

- `common/Reason86.java` — enum 5 lý do + nhãn tiếng Việt + chip bấm nhanh. `fromCode()` chấp nhận thừa khoảng trắng / khác hoa-thường, mã lạ trả `null`.
- `common/Menu86Validator.java` — `validate(reasonCode, note, backInEta, now)` → `Validated{reason, note, backInEta}`, ném `BusinessException` với message hiển thị được.
- `common/Constants.java:46-50` — 4 ngưỡng: 255 ký tự / 10 ký tự / 15 phút / 7 ngày.
- `test/common/Menu86ValidatorTest.java` — 37 test, đã pass.

Luật đang áp: lý do bắt buộc thuộc enum · lý do "Khác" thì ghi chú ≥ 10 ký tự · ghi chú ≤ 255 · ETA bắt buộc, phải tương lai, cách hiện tại ≥ 15 phút, ≤ 7 ngày · ghi chú chuẩn hoá NFC + trim.

---

## 1. Schema — `sql/database.sql`

Thêm sau khối `catalog.BranchMenu` (dòng ~318). **Không sửa `BranchMenu`** — giữ `Is86`/`BackInEta` làm trạng thái hiện hành để POS/QR đọc nhanh, bảng mới là lịch sử.

```sql
CREATE TABLE catalog.MenuBlockRequest (
    RequestId    INT IDENTITY PRIMARY KEY,
    BranchId     INT NOT NULL,
    ProductId    INT NOT NULL,
    Reason       VARCHAR(20)   NOT NULL,   -- INGREDIENT_OUT|SPOILED|EQUIPMENT|QUALITY|OTHER
    Note         NVARCHAR(255) NULL,
    BackInEta    DATETIME2     NOT NULL,
    RequestedBy  INT NOT NULL,
    RequestedAt  DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    ReopenRequestedAt DATETIME2 NULL,      -- barista xin mở bán lại
    Status       VARCHAR(10) NOT NULL DEFAULT 'PENDING', -- PENDING|APPROVED|REJECTED|RESOLVED
    ReviewedBy   INT NULL,
    ReviewedAt   DATETIME2 NULL,
    ReviewNote   NVARCHAR(255) NULL,
    ClosedAt     DATETIME2 NULL,           -- NULL = yêu cầu còn mở (xem index dưới)
    CONSTRAINT FK_MBR_Branch  FOREIGN KEY (BranchId)    REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_MBR_Product FOREIGN KEY (ProductId)   REFERENCES catalog.Product(ProductId),
    CONSTRAINT FK_MBR_ReqBy   FOREIGN KEY (RequestedBy) REFERENCES iam.[User](UserId),
    CONSTRAINT FK_MBR_RevBy   FOREIGN KEY (ReviewedBy)  REFERENCES iam.[User](UserId),
    CONSTRAINT CK_MBR_Status  CHECK (Status IN ('PENDING','APPROVED','REJECTED','RESOLVED'))
);
GO

-- Mỗi món chỉ được có 1 yêu cầu đang mở tại một thời điểm.
-- Đây là chốt chặn race: 2 barista bấm cùng lúc thì đứa thứ 2 văng lỗi trùng khoá.
CREATE UNIQUE INDEX UX_MenuBlockRequest_Open
    ON catalog.MenuBlockRequest(BranchId, ProductId)
    WHERE ClosedAt IS NULL;
GO

-- Hàng chờ duyệt của manager: lọc theo chi nhánh + còn mở, xếp theo ETA.
CREATE INDEX IX_MenuBlockRequest_Queue
    ON catalog.MenuBlockRequest(BranchId, ClosedAt, BackInEta);
GO
```

**Lưu ý:** dùng `ClosedAt IS NULL` làm điều kiện filtered index chứ không dùng `Status IN (...)` — `IS NULL` chắc chắn hợp lệ với filtered index của SQL Server, còn predicate `IN` tuỳ phiên bản có thể bị từ chối. `ClosedAt` hơi thừa so với `Status` nhưng đổi lại được ràng buộc ở tầng DB, đáng.

**Quy tắc bất biến:** `ClosedAt IS NULL` ⟺ `Status IN ('PENDING','APPROVED')`. Mọi câu UPDATE phải set hai cột cùng lúc, đừng để lệch.

---

## 2. Model — `model/MenuBlockRequest.java`

Class thường (dự án không dùng record), getter/setter đủ các cột trên, cộng field đọc kèm để JSP khỏi query thêm:

```java
private String productName;      // JOIN catalog.Product
private String requesterName;    // JOIN iam.[User]
private String reviewerName;     // JOIN iam.[User]
private Reason86 reasonEnum;     // đổi từ cột Reason, JSP hiện reasonEnum.label()
```

Thêm 1 method tiện cho JSP (đừng tính trong JSP):

```java
/** Quá hạn dự kiến có lại mà chưa mở bán → manager cần xử lý gấp. */
public boolean isOverdue() {
    return closedAt == null && backInEta != null && backInEta.isBefore(LocalDateTime.now());
}
```

---

## 3. DAO — `dao/shared/MenuBlockRequestDao.java`

Theo **pattern ghi** của dự án: nhận `Connection`, try-with-resources, ném `SQLException` cho Service rollback.

```java
int insert(Connection conn, MenuBlockRequest r) throws SQLException;
// INSERT ... ; trả RequestId (Statement.RETURN_GENERATED_KEYS)

MenuBlockRequest findOpen(Connection conn, int branchId, int productId) throws SQLException;
// WHERE BranchId=? AND ProductId=? AND ClosedAt IS NULL — trả null nếu không có

List<MenuBlockRequest> findOpenByBranch(Connection conn, int branchId) throws SQLException;
// JOIN Product + User; WHERE ClosedAt IS NULL; ORDER BY BackInEta

List<MenuBlockRequest> findHistoryByBranch(Connection conn, int branchId, int limit) throws SQLException;
// ClosedAt IS NOT NULL, ORDER BY ClosedAt DESC — cho tab lịch sử

int markReopenRequested(Connection conn, int requestId, int branchId) throws SQLException;
// UPDATE ... SET ReopenRequestedAt = SYSDATETIME()
// WHERE RequestId=? AND BranchId=? AND ClosedAt IS NULL AND ReopenRequestedAt IS NULL

int review(Connection conn, int requestId, int branchId, String newStatus,
           int reviewerId, String reviewNote, boolean close) throws SQLException;
// UPDATE ... SET Status=?, ReviewedBy=?, ReviewedAt=SYSDATETIME(), ReviewNote=?,
//                ClosedAt = CASE WHEN ? = 1 THEN SYSDATETIME() ELSE NULL END
// WHERE RequestId=? AND BranchId=? AND ClosedAt IS NULL      ← WHERE-guard
```

**Bắt buộc:** mọi câu UPDATE đều có `AND BranchId = ?` (cách ly chi nhánh) **và** `AND ClosedAt IS NULL` (WHERE-guard chống race). Trả `int affected` để Service kiểm — `affected != 1` nghĩa là ai đó vừa xử lý xong trước, phải báo lỗi chứ không im lặng.

---

## 4. Service — `service/shared/BranchMenuService.java`

Thêm 4 method. Mỗi cái **một transaction**, đúng mẫu `set86()` đang có (`setAutoCommit(false)` → thao tác → `commit()`, catch `rollback()`, finally `setAutoCommit(true)`).

```java
void request86(int branchId, int productId, String reasonCode, String note,
               LocalDateTime backInEta, int userId)
```
1. `Menu86Validator.validate(...)` — **ngoài transaction**, sai thì ném `BusinessException` luôn, khỏi mở connection.
2. Mở tx: `dao.findOpen(...)` != null → ném `BusinessException("Món này đang có yêu cầu chờ xử lý.")`.
3. `menuBlockDao.insert(...)` (PENDING, ClosedAt NULL).
4. `branchMenuDao.updateIs86(conn, branchId, productId, true, Timestamp.valueOf(eta))`.
5. `EventPublisher.publish(conn, MENU_86_CHANGED, productId, branchId, payload)` — payload thêm `reason`, `by`, `requestId`.
6. Commit.

> Bắt riêng `SQLIntegrityConstraintViolationException` / lỗi trùng khoá từ `UX_MenuBlockRequest_Open` → đổi thành `BusinessException` chữ tiếng Việt, đừng để văng 500 khi 2 barista bấm cùng lúc.

```java
void requestReopen(int branchId, int productId, int userId)
```
`markReopenRequested`, `affected != 1` → `BusinessException("Món này không còn chờ xử lý.")`. **Không đụng `Is86`.**

```java
void approve86(int branchId, int requestId, int reviewerId, String reviewNote)
```
`review(..., "APPROVED", close=false)`. Không đụng `Is86` (đang khoá, giữ khoá). `affected != 1` → `BusinessException`.

```java
void reopen86(int branchId, int requestId, int reviewerId, String reviewNote, boolean rejected)
```
1. `review(..., rejected ? "REJECTED" : "RESOLVED", close=true)`, kiểm `affected == 1`.
2. Cần `productId` để mở khoá — lấy bằng `findOpen` **trước** khi review, hoặc cho `review()` trả về productId.
3. `branchMenuDao.updateIs86(conn, branchId, productId, false, null)` — hàm này đã tự xoá ETA khi `is86=false`.
4. `EventPublisher.publish(...)` payload `{is86:false, requestId, by}`.
5. Commit.

**Giữ nguyên `set86()` cũ** — `ManagerMenuServlet` và test đang dùng. Đừng xoá, đừng đổi chữ ký (breaking change).

---

## 5. Barista — `controller/barista/EightySixServlet.java`

`doGet` thêm:
```java
req.setAttribute("openRequests", service.getOpenRequestsMap(branchId)); // Map<productId, MenuBlockRequest>
req.setAttribute("reasons", Reason86.values());
```

`doPost` — thay khối `toggle86` bằng:

| action | Xử lý |
|---|---|
| `report86` | đọc `reasonCode`, `note`, `backInEta` → `service.request86(...)` → flashOk |
| `askReopen` | `service.requestReopen(branchId, productId, userId)` → flashOk "Đã gửi yêu cầu, chờ quản lý duyệt." |

- **Bỏ hẳn** nhánh cho barista set `is86=false`.
- **Bỏ `catch (DateTimeParseException ignore)`** — parse lỗi phải báo lỗi. Bắt như `AttendanceServlet.java:67-69`: `flashError("Định dạng thời gian không hợp lệ.")`.
- Thêm `catch (BusinessException e)` → `flashError(e.getMessage())` + redirect (đúng mẫu `AttendanceServlet.java:64-66`).
- Giữ nguyên `CsrfUtil.isValid` và `BaristaShift.guardWrite` ở đầu `doPost`.
- `userId` lấy từ `SessionUtil.currentUser(req)` — **không** nhận từ form.

---

## 6. Barista — `views/barista/eightysix.jsp`

**Cột thao tác, 3 trạng thái:**

| Trạng thái | Hiện gì |
|---|---|
| Còn bán | Nút "Báo tạm hết" → mở form (`<details>` hoặc modal) |
| Tạm hết, chưa xin mở | Badge "Tạm hết" + ETA + trạng thái duyệt · nút "Xin mở bán lại" |
| Tạm hết, đã xin mở | Badge + dòng mờ "Đã gửi yêu cầu mở bán — chờ quản lý", nút bị disable |

**Form báo tạm hết** (mỗi dòng một form, `method="post"`):
```
_csrf · action=report86 · productId
<select name="reasonCode" required>  ← option value=${r} text=${r.label}
<div class="chips">                  ← render chip từ ${r.quickNotes}, lọc theo lý do đang chọn
<input name="note" maxlength="255">
<input type="datetime-local" name="backInEta" required min="<now+15p>" max="<now+7d>">
<button>Báo tạm hết</button>
```

**JS cho chip** (viết inline như JS đang có ở cuối file này):
- Đổ toàn bộ chip vào `data-quick-notes` trên từng `<option>` (JSON escape), đổi `<select>` thì render lại chip.
- Bấm chip → toggle, nối các chip đang chọn bằng `" · "` vào ô ghi chú.
- Người dùng gõ tay vào ô ghi chú → bỏ trạng thái chọn của chip (tránh chip ghi đè chữ họ vừa viết).
- Chọn "Khác" → ẩn chip, focus ô ghi chú, `minlength="10"`.
- `min`/`max` của input thời gian tính ở **Servlet** rồi set attribute, đừng tính trong JSP.

**Khối gợi ý (dòng 12-29):** đổi từ form submit thẳng → nút mở đúng form của dòng tương ứng, điền sẵn lý do `INGREDIENT_OUT` + chip tên nguyên liệu đang cạn. Bỏ luôn nút submit 1 click.

**Chữ trên màn hình:** không để `86`, `PENDING`, `INGREDIENT_OUT` lọt ra. Dùng "tạm hết", "chờ quản lý duyệt", `${r.label}`.

Giữ nguyên phần tìm kiếm / lọc / phân trang ở cuối file — không đụng.

---

## 7. Manager — servlet + JSP mới

**`controller/manager/MenuBlockServlet.java`** → `@WebServlet("/manager/menu-block")`

`doGet`: `openRequests` (đang mở, `isOverdue()` xếp lên đầu) + `history` (20 dòng gần nhất) → `/WEB-INF/views/manager/menu-block.jsp`.

`doPost` — bám sát `AttendanceServlet`:

| action | Gọi |
|---|---|
| `approve` | `service.approve86(branchId, requestId, reviewerId, reviewNote)` |
| `reject` | `service.reopen86(..., rejected=true)` |
| `reopen` | `service.reopen86(..., rejected=false)` |

Cấu trúc `doPost` copy khung của `AttendanceServlet.java:39-74`: CSRF → lấy `reviewerId` từ session → switch action → redirect; catch `BusinessException` / `NumberFormatException` → flashError + redirect.

**`views/manager/menu-block.jsp`**: bảng — Món · Lý do (`reasonEnum.label()`) · Ghi chú · Dự kiến có lại (đỏ nếu `overdue`) · Người báo · Lúc · Trạng thái · Thao tác. Dòng có `reopenRequestedAt` gắn nhãn "Barista xin mở bán".

**`views/layout/sidebar.jsp`**: thêm link sau dòng 90 (`/manager/menu`):
```jsp
<li><a class="${curPath == ctx.concat('/manager/menu-block') ? 'active' : ''}"
       href="${ctx}/manager/menu-block">…Món tạm hết</a></li>
```
Kèm badge đếm số yêu cầu đang chờ nếu làm được (đọc từ attribute chung).

**RBAC:** không phải sửa `RbacFilter` — `/manager/*` đã map sẵn `ROLE_MANAGER` (`RbacFilter.java:42`).

---

## Edge case phải xử lý

| Tình huống | Cách xử lý |
|---|---|
| 2 barista báo cùng 1 món cùng lúc | `UX_MenuBlockRequest_Open` chặn; bắt lỗi trùng khoá → `BusinessException` tiếng Việt |
| Manager duyệt trong khi barista vừa xin mở | WHERE-guard `ClosedAt IS NULL`, `affected != 1` → báo "Yêu cầu đã được xử lý" |
| Món đang có ly dở trong KDS lúc bị 86 | **Không tự huỷ** — luồng thu ngân huỷ món bị chặn đã có sẵn lo |
| Món chưa có dòng trong `BranchMenu` (`published=false`) | Không cho báo hết — JSP đã lọc `c:if ${m.published}`, chặn thêm ở Service |
| Barista tan ca giữa chừng | `BaristaShift.guardWrite` đã chặn mọi POST ngoài ca |
| Quá ETA chưa ai mở | Không tự mở; `isOverdue()` đẩy lên đầu danh sách manager |
| Manager từ chối rồi barista báo lại ngay | Cho phép — yêu cầu cũ đã `ClosedAt`, tạo yêu cầu mới bình thường |
| Đổi chi nhánh giữa chừng | Mọi query + update đều có `BranchId` từ session, không từ form |

---

## Thứ tự làm (mỗi bước build được)

1. **Schema** — chạy DDL, `SELECT` thử, kiểm unique index có chặn thật không (insert 2 dòng cùng món).
2. **Model + DAO** — chưa ai gọi, vẫn compile được.
3. **Service** — 4 method, gọi vào DAO. Chạy `mvn test` xem có gãy test cũ không (`set86()` phải còn nguyên).
4. **Servlet barista + JSP barista** — tới đây báo tạm hết đã chạy end-to-end, chỉ chưa mở lại được.
5. **Servlet manager + JSP manager + sidebar** — khép vòng.
6. Chạy full `mvn test` + deploy Tomcat kiểm tay.

Bước 4 là mốc đáng commit riêng ("siết form báo tạm hết"), bước 5 là commit thứ hai ("manager duyệt mở bán lại").

---

## Cách verify (chưa có test DB, phải kiểm tay)

**Test tự động viết được ngay** (logic thuần, không DB) — khuyến nghị làm trước bước 3:
- Tách hàm `Menu86Transition.canReview(status, action)` rồi test: `PENDING`→approve OK, `RESOLVED`→approve phải chặn, `REJECTED`→reopen phải chặn. Cùng kiểu với `Menu86ValidatorTest` đã có.

**Kiểm tay sau khi deploy:**

| # | Thao tác | Kỳ vọng |
|---|---|---|
| 1 | Báo tạm hết, để trống ETA | Chặn, hiện "Vui lòng chọn thời gian dự kiến có lại." |
| 2 | Chọn ETA cách 5 phút | Chặn, "ít nhất 15 phút" |
| 3 | Chọn lý do "Khác", ghi chú 5 ký tự | Chặn, "phải ghi rõ, tối thiểu 10 ký tự" |
| 4 | Bấm 2 chip | Ô ghi chú thành "Hết sữa tươi · Hết đá" |
| 5 | Báo tạm hết hợp lệ | Món biến mất khỏi POS **và** menu QR ngay |
| 6 | Kiểm DB | 1 dòng `MenuBlockRequest` PENDING + 1 dòng `ops.OutboxEvent` |
| 7 | Tìm nút "Mở bán lại" ở màn barista | **Không tồn tại** |
| 8 | Barista bấm "Xin mở bán lại" | `ReopenRequestedAt` có giá trị, `Is86` **vẫn = 1** |
| 9 | Manager duyệt | Status APPROVED, món **vẫn khoá** |
| 10 | Manager mở bán lại | Status RESOLVED, `ClosedAt` có, `Is86=0`, `BackInEta=NULL`, món về POS + QR |
| 11 | Manager từ chối một yêu cầu PENDING | Status REJECTED, món mở bán lại ngay |
| 12 | Mở 2 tab, cùng bấm duyệt 1 yêu cầu | Tab sau báo lỗi tiếng Việt, không 500 |
| 13 | Đặt ETA quá khứ bằng cách sửa HTML | Server vẫn chặn (không tin client) |
| 14 | Barista ngoài ca thao tác | Bị chặn, "cần vào ca trước khi thao tác" |

---

## Rủi ro

- **Filtered index**: nếu SQL Server từ chối predicate, kiểm lại là đang dùng `WHERE ClosedAt IS NULL` chứ không phải `Status IN (...)`.
- **Lệch `Status` vs `ClosedAt`**: hai cột phải luôn nhất quán. Nếu thấy phiền, cân nhắc bỏ `ClosedAt` và đổi unique index sang cột computed persisted — nhưng chỉ làm nếu thật sự vướng.
- **`set86()` cũ**: `ManagerMenuServlet` đang gọi. Đừng xoá khi refactor, sẽ gãy màn Menu chi nhánh.
- **JS chip**: viết vanilla inline như JS sẵn có trong `eightysix.jsp`, đừng kéo thư viện mới.
