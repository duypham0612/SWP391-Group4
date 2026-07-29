# Luồng code tổng quát role Cashier

> Tài liệu này được đối chiếu trực tiếp với code hiện tại trên nhánh
> `nguyeenquanganhh`. Nội dung mô tả implementation đang chạy, không phải kiến
> trúc mong muốn trong tương lai.

## 1. Mục đích và cách đọc

Tài liệu trả lời bốn câu hỏi cho từng màn Cashier:

1. Người dùng bấm ở file JSP/JavaScript nào?
2. Request đi vào servlet, service và DAO nào?
3. Câu SQL nào thực sự chạy, tham số `?` được bind bằng giá trị gì?
4. Dữ liệu được đọc/ghi vào bảng nào và trạng thái thay đổi ra sao?

Quy ước đường dẫn:

- `src/main/webapp/WEB-INF/views/cashier/...`: giao diện JSP phía server.
- `src/main/java/com/cafe/controller/cashier/...`: servlet/controller nhận HTTP.
- `src/main/java/com/cafe/service/...`: nghiệp vụ và transaction.
- `src/main/java/com/cafe/dao/...`: nơi sở hữu câu SQL.
- `sql/database.sql`: schema SQL Server chuẩn của project.

Không có JSP nào kết nối database trực tiếp. Luồng chuẩn là:

```text
Browser
  -> Filter chain
  -> Cashier Servlet
  -> Service
  -> DAO
  -> SQL Server
  -> request attribute/model
  -> JSP render HTML
```

Với thao tác ghi:

```text
JSP form hoặc JavaScript fetch
  -> POST + CSRF
  -> Filter kiểm tra đăng nhập/role/chi nhánh/ca trực
  -> Servlet parse input
  -> Service validate nghiệp vụ + mở transaction
  -> DAO chạy PreparedStatement
  -> COMMIT hoặc ROLLBACK
  -> redirect/render JSON
```

## 2. Bản đồ màn hình và source code

| Màn hình | HTTP route | View | Controller chính | Service chính |
|---|---|---|---|---|
| Bảng điều khiển | `GET /cashier/dashboard` | `cashier/dashboard.jsp` | `CashierDashboardServlet` | `CashierShiftService` |
| Ca thu ngân | `GET/POST /cashier/shift` | `cashier/shift.jsp` | `CashierShiftServlet` | `CashierDutyService`, `CashierShiftService`, `AttendanceService` |
| Sơ đồ bàn | `GET/POST /cashier/table` | `cashier/table-map.jsp` | `TableServlet` | `TableSessionService` |
| In QR bàn | `GET /cashier/table-qr` | `cashier/table-qr.jsp` | `TableQrServlet` | `TableSessionService` |
| POS/Đặt món | `GET/POST /cashier/pos` | `cashier/pos.jsp` | `PosServlet` | `CatalogReadService`, `OrderService` |
| Đơn đến & bàn giao | `GET/POST /cashier/inbox` | `cashier/inbox.jsp`, `cashier/handoff/cards.jsp` | `OrderInboxServlet` | `OrderService`, `PickupService` |
| Route bàn giao cũ | `GET/POST /cashier/handoff` | fragment `cashier/handoff/cards.jsp` | `PickupServlet` | `PickupService` |
| Thanh toán | `GET/POST /cashier/checkout` | `cashier/checkout.jsp` | `CheckoutServlet` | `BillingService` |
| Lịch sử hóa đơn | `GET/POST /cashier/history` | `cashier/bill-history.jsp`, `cashier/bill-view.jsp` | `BillHistoryServlet` | `BillingService`, `CashierShiftService` |

Route `/cashier/handoff` không còn là màn độc lập:

- `GET /cashier/handoff` redirect sang `/cashier/inbox#handoff`.
- Các form mới nằm ngay trong `inbox.jsp`/`handoff/cards.jsp`.
- `POST /cashier/handoff?ajax=1` vẫn được giữ để tương thích với fragment/AJAX cũ.

## 3. Filter chạy trước mọi servlet Cashier

Thứ tự được khai báo trong `src/main/webapp/WEB-INF/web.xml`:

```text
1. CharsetFilter
2. AuthFilter
3. RbacFilter
4. BranchScopeFilter
5. CashierDutyGuardFilter (chỉ /cashier/*)
6. Servlet
```

### 3.1 `CharsetFilter`

- Ép request/response dùng UTF-8.
- Chạy trước khi servlet đọc parameter hoặc ghi HTML/JSON.

### 3.2 `AuthFilter`

File: `src/main/java/com/cafe/filter/AuthFilter.java`.

- Nếu chưa có user trong HTTP session, redirect về `/auth/login`.
- `/cashier/*` không thuộc whitelist public nên luôn cần đăng nhập.

### 3.3 `RbacFilter`

File: `src/main/java/com/cafe/filter/RbacFilter.java`.

- Prefix `/cashier/` yêu cầu `roleCode = CASHIER`.
- `ADMIN` được phép đi qua mọi vùng.
- Sai role trả HTTP `403`.

### 3.4 `BranchScopeFilter`

File: `src/main/java/com/cafe/filter/BranchScopeFilter.java`.

- Lấy `branchId` từ user trong session.
- Kiểm tra chi nhánh còn hoạt động.
- Đặt branch vào request attribute `Constants.ATTR_BRANCH_ID`.
- Đảm bảo session có CSRF token.

Các servlet Cashier đọc lại branch bằng:

```java
int branchId = InventoryDashboardServlet.branchId(req);
```

Đây là nguồn branch phía server. Client không được tự gửi `branchId`.

### 3.5 `CashierDutyGuardFilter`

File: `src/main/java/com/cafe/filter/CashierDutyGuardFilter.java`.

Filter gọi:

```text
CashierDutyService.getDutyState(userId, branchId)
```

Bốn trạng thái:

| Trạng thái | Chấm công | Két thu ngân |
|---|---:|---:|
| `OFF_DUTY` | Chưa vào ca | Chưa mở |
| `CLOCKED_NO_TILL` | Đã vào ca | Chưa mở |
| `ON_DUTY` | Đã vào ca | Đang mở |
| `TILL_ONLY` | Chưa/không còn vào ca | Vẫn mở |

Quy tắc:

- Mọi request `GET` vẫn được xem.
- `/cashier/shift` và `/cashier/dashboard` luôn được phép để Cashier có thể bắt đầu ca.
- Các `POST` khác chỉ qua khi state là `ON_DUTY`.
- POST JSON bị chặn trả `403` JSON và hai header:
  - `X-Cashier-Duty-Denied: true`
  - `X-Cashier-Duty-Redirect: <context>/cashier/shift`
- POST form bị chặn đặt `flashError` rồi redirect về màn ca.

### 3.6 CSRF ở controller

Mỗi servlet ghi đều kiểm tra thêm:

```java
if (!CsrfUtil.isValid(req)) {
    resp.sendError(403, "CSRF");
    return;
}
```

JSP form gửi `_csrf`; POS gửi token trên query string:

```javascript
fetch(CTX + '/cashier/pos?_csrf=' + encodeURIComponent(CSRF), ...)
```

## 4. Sơ đồ dữ liệu Cashier

### 4.1 Quan hệ chính

```text
org.Branch
  ├─< iam.User
  ├─< sales.DiningTable
  │    └─< sales.TableSession
  │          └─< sales.Orders
  ├─< sales.Orders
  │    └─< sales.OrderItem
  │          ├─< sales.OrderItemModifier
  │          └─1 payment.BillItem >─1 payment.Bill
  ├─< payment.CashierShift
  │    └─< payment.Bill
  └─< ops.OutboxEvent

hr.ShiftTemplate
  └─< hr.ShiftAssignment
       └─1 hr.Attendance

payment.Voucher
  ├─< payment.Bill
  └─< payment.VoucherRedemption >─1 payment.Bill
```

### 4.2 Trạng thái đơn và món

Trạng thái `sales.Orders.Status`:

```text
ACTIVE -> COMPLETED
ACTIVE -> CANCELLED
COMPLETED -> ACTIVE  (chỉ khi hoàn tác giao)
```

Trạng thái chính của `sales.OrderItem.Status`:

```text
WAITING
  -> MAKING
  -> READY
  -> PICKED_UP
  -> SERVED
```

Nhánh lỗi/ngoại lệ:

```text
WAITING/MAKING -> BLOCKED -> WAITING
WAITING/MAKING/READY -> luồng REMAKE -> WAITING
WAITING/MAKING/BLOCKED -> CANCELLED
SERVED -> PICKED_UP                  (hoàn tác giao trong code service)
```

Một order chỉ thành `COMPLETED` khi không còn item nào ngoài
`SERVED` hoặc `CANCELLED`:

```sql
UPDATE sales.Orders
SET Status='COMPLETED'
WHERE OrderId=?
  AND Status='ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM sales.OrderItem oi
      WHERE oi.OrderId=?
        AND oi.Status NOT IN ('SERVED','CANCELLED')
  )
```

### 4.3 Trạng thái hóa đơn

```text
UNPAID -> PAID
UNPAID -> VOID
```

Schema vẫn cho phép `REFUND`, nhưng luồng Cashier hiện tại:

- Không có nút hoàn tiền.
- Không có service/DAO chuyển bill sang `REFUND`.
- JSP lịch sử vẫn biết cách hiển thị bản ghi `REFUND` cũ nếu database đã có.

### 4.4 Order khác Bill

Đây là điểm quan trọng:

- Bấm **Gửi đơn** tạo `sales.Orders` và `sales.OrderItem`.
- Không tạo `payment.Bill` ngay lúc đặt món.
- Bill chỉ được tạo/đồng bộ khi Cashier mở màn Checkout cho bàn hoặc đơn mang đi.
- `payment.BillItem` là cầu nối từ một `OrderItem` sang đúng một `Bill`.
- Unique constraint `UQ_BItem UNIQUE (OrderItemId)` ngăn một dòng món nằm trên hai bill.

## 5. Màn Bảng điều khiển

### 5.1 Front-end

File: `src/main/webapp/WEB-INF/views/cashier/dashboard.jsp`.

Các link:

```text
Sơ đồ bàn        -> /cashier/table
POS / Đặt món    -> /cashier/pos
Đơn đến (Inbox)  -> /cashier/inbox
Thanh toán       -> /cashier/checkout
Ca thu ngân      -> /cashier/shift
Lịch sử hóa đơn  -> /cashier/history
```

### 5.2 GET `/cashier/dashboard`

```text
CashierDashboardServlet.doGet
  -> branchId(req)
  -> CashierShiftService.getTodayRevenue(branchId)
     -> BillDao.sumPaidToday
  -> CashierShiftService.getTodayBillCount(branchId)
     -> BillDao.countPaidToday
  -> set request attributes
  -> forward dashboard.jsp
```

Query doanh thu:

```sql
SELECT ISNULL(SUM(TotalAmount),0) AS Rev
FROM payment.Bill
WHERE BranchId=?
  AND Status='PAID'
  AND CAST(PaidAt AS DATE)=CAST(SYSUTCDATETIME() AS DATE)
```

Bind:

```text
?1 = branchId của user đăng nhập
```

Query số bill:

```sql
SELECT COUNT(*)
FROM payment.Bill
WHERE BranchId=?
  AND Status='PAID'
  AND CAST(PaidAt AS DATE)=CAST(SYSUTCDATETIME() AS DATE)
```

Bảng dùng: `payment.Bill`.

Lưu ý: hai query đang cắt ngày theo ngày UTC của SQL Server, không dùng mốc ngày
kinh doanh/giờ Việt Nam như một số màn order.

## 6. Màn Ca thu ngân

### 6.1 Front-end

File: `src/main/webapp/WEB-INF/views/cashier/shift.jsp`.

Form bắt đầu ca:

```text
POST /cashier/shift
_csrf=<token>
action=startDuty
openingCash=<quỹ đầu ca>
```

Form kết ca:

```text
POST /cashier/shift
_csrf=<token>
action=closeDuty
shiftId=<CashierShiftId>
closingCash=<tổng tiền mặt thực đếm trong két>
```

Link xem báo cáo:

```text
GET /cashier/shift?action=report&shiftId=<id>
```

### 6.2 GET `/cashier/shift`

```text
CashierShiftServlet.doGet
  -> getShiftReport(shiftId)             nếu action=report
  -> getCurrentShift(cashierId)
  -> CashierDutyService.getDutyState
  -> getShiftList(branchId)
  -> getTodayRevenue(branchId)
  -> getTodayBillCount(branchId)
  -> AttendanceService.getMyShiftStatus
  -> forward shift.jsp
```

Query tìm két đang mở:

```sql
SELECT cs.CashierShiftId, cs.BranchId, cs.CashierId,
       cs.OpeningCash, cs.ClosingCash, cs.OpenedAt, cs.ClosedAt,
       u.FullName AS CashierName
FROM payment.CashierShift cs
JOIN iam.[User] u ON u.UserId=cs.CashierId
WHERE cs.CashierId=?
  AND cs.ClosedAt IS NULL
```

Query lịch sử két của chi nhánh:

```sql
SELECT cs.CashierShiftId, cs.BranchId, cs.CashierId,
       cs.OpeningCash, cs.ClosingCash, cs.OpenedAt, cs.ClosedAt,
       u.FullName AS CashierName
FROM payment.CashierShift cs
JOIN iam.[User] u ON u.UserId=cs.CashierId
WHERE cs.BranchId=?
ORDER BY cs.OpenedAt DESC
```

Query báo cáo một ca:

```sql
SELECT COUNT(*) AS Cnt,
       ISNULL(
           SUM(CASE WHEN PaymentMethod='CASH'
                    THEN TotalAmount ELSE 0 END),
           0
       ) AS CashTotal
FROM payment.Bill
WHERE CashierShiftId=?
  AND Status='PAID'
```

Báo cáo hiện chỉ lấy:

- Số bill `PAID`.
- Tổng bill `PAID` có `PaymentMethod='CASH'`.

### 6.3 Bấm **Bắt đầu ca**

Luồng:

```text
shift.jsp
  -> POST action=startDuty
  -> CashierShiftServlet.parseMoney(openingCash)
  -> CashierDutyService.startDuty(userId, branchId, openingCash)
  -> CashierCashReconciliation.requireValidMoney
  -> BEGIN TRANSACTION
     -> CashierShiftService.openShift(connection, ...)
        -> khóa dòng org.Branch theo branchId
        -> khóa và kiểm tra mọi CashierShift OPEN của branch
        -> nếu branch trống: insert CashierShift
     -> AttendanceService.clockIn(connection, userId, branchId)
  -> COMMIT
  -> redirect /cashier/shift
```

#### Chặn hai Cashier cùng mở két

Trước khi chấm công hoặc insert, DAO khóa dòng chi nhánh:

```sql
SELECT BranchId
FROM org.Branch WITH (UPDLOCK, HOLDLOCK)
WHERE BranchId=?
```

Update lock trên đúng dòng branch tồn tại tới lúc commit/rollback. Hai request
mở ca cùng chi nhánh không thể cùng đọc trạng thái “chưa có ca” rồi cùng
insert; request thứ hai phải chờ request thứ nhất hoàn tất. Hai chi nhánh khác
nhau vẫn mở ca độc lập.

Sau khi khóa branch, service đọc và khóa tất cả ca mở:

```sql
SELECT cs.CashierShiftId, cs.BranchId, cs.CashierId,
       cs.OpeningCash, cs.ClosingCash, cs.OpenedAt, cs.ClosedAt,
       u.FullName AS CashierName
FROM payment.CashierShift cs WITH (UPDLOCK, HOLDLOCK)
JOIN iam.[User] u ON u.UserId=cs.CashierId
WHERE cs.BranchId=?
  AND cs.ClosedAt IS NULL
ORDER BY cs.OpenedAt, cs.CashierShiftId
```

Quy tắc:

- Không có ca mở: được tạo ca mới.
- Chỉ có ca của chính Cashier: trả lại ca đó, giúp double-click/idempotent.
- Có ca của Cashier khác: rollback toàn bộ và báo rõ mã ca, tên người đang giữ
  két; Attendance của người mới cũng chưa bị tạo.
- Database cũ đang có nhiều ca trùng: mọi Cashier đều bị chặn mở tiếp cho tới
  khi các ca cũ được đối soát và kết.

Thông báo:

```text
Chi nhánh còn ca thu ngân #<id> của <tên> chưa kết.
Vui lòng kết ca cũ hoặc nhờ Quản lý xử lý trước khi bắt đầu ca mới.
```

#### Kiểm tra đã được xếp ca

`AttendanceService` đọc assignment của hôm trước và hôm nay:

```sql
SELECT sa.ShiftAssignmentId, sa.ShiftTemplateId, sa.UserId, sa.WorkDate,
       st.Name AS TemplateName, st.StartTime, st.EndTime,
       u.FullName AS UserName
FROM hr.ShiftAssignment sa
JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId
JOIN iam.[User] u ON u.UserId=sa.UserId
WHERE sa.UserId=?
  AND st.BranchId=?
  AND sa.WorkDate BETWEEN ? AND ?
ORDER BY sa.WorkDate, st.StartTime
```

Bind:

```text
?1 = cashier userId
?2 = branchId
?3 = ngày Việt Nam hiện tại - 1 ngày
?4 = ngày Việt Nam hiện tại
```

Java tiếp tục lọc bằng `ShiftWindow.isClockable(...)`. Nếu không có assignment
đang trong cửa sổ vào ca:

```text
IllegalStateException("Hôm nay bạn chưa được xếp ca.")
```

#### Khóa và tạo Attendance

Đọc attendance với khóa:

```sql
SELECT ...
FROM hr.Attendance a WITH (UPDLOCK, HOLDLOCK)
JOIN hr.ShiftAssignment sa ON ...
JOIN hr.ShiftTemplate st ON ...
...
WHERE a.ShiftAssignmentId=?
```

Nếu chưa có:

```sql
SELECT SYSUTCDATETIME()
```

```sql
INSERT INTO hr.Attendance
    (ShiftAssignmentId, CheckInAt, CheckOutAt, Status)
VALUES
    (?, ?, NULL, 'PENDING')
```

Nếu attendance tồn tại nhưng chưa có `CheckInAt`:

```sql
UPDATE hr.Attendance
SET CheckInAt=?, CheckOutAt=NULL
WHERE AttendanceId=?
```

Sau đó status duyệt được đưa về `PENDING`.

#### Mở két

Nếu chi nhánh chưa có két mở:

```sql
INSERT INTO payment.CashierShift
    (BranchId, CashierId, OpeningCash)
VALUES
    (?, ?, ?)
```

Bind:

```text
?1 = branchId
?2 = cashier userId
?3 = openingCash
```

Attendance và CashierShift nằm trong cùng transaction. Một bước lỗi thì cả hai
rollback, tránh tình trạng đã chấm công nhưng chưa mở két hoặc ngược lại.

Database có lớp bảo vệ thứ hai:

```sql
CREATE UNIQUE INDEX UX_CashierShift_OneOpenPerBranch
ON payment.CashierShift(BranchId)
WHERE ClosedAt IS NULL;
```

Khi chạy `database.sql` trên database đang có nhiều ca OPEN cùng branch, script
không tự đóng ca và tạm chưa tạo index. Sau khi đối soát và kết hết ca trùng,
chạy lại `database.sql` sẽ tạo index.

### 6.4 Bấm **Kết ca**

Luồng:

```text
shift.jsp
  -> POST action=closeDuty
  -> CashierShiftServlet.parseMoney(closingCash)
  -> CashierDutyService.closeDuty
  -> BEGIN TRANSACTION
     -> CashierShiftService.closeShift(connection, ...)
     -> AttendanceService.clockOut(connection, ...)
  -> COMMIT
  -> redirect report
```

#### Bước 1: khóa két của đúng Cashier

```sql
SELECT cs.CashierShiftId, cs.BranchId, cs.CashierId,
       cs.OpeningCash, cs.ClosingCash, cs.OpenedAt, cs.ClosedAt,
       u.FullName AS CashierName
FROM payment.CashierShift cs WITH (UPDLOCK, HOLDLOCK)
JOIN iam.[User] u ON u.UserId=cs.CashierId
WHERE cs.CashierId=?
  AND cs.ClosedAt IS NULL
```

Service kiểm tra đồng thời:

- Đúng `shiftId` từ form.
- Đúng `cashierId` đang đăng nhập.
- Đúng `branchId`.
- Két chưa đóng.

#### Bước 2: tính đúng tiền mặt phải có

```sql
SELECT ISNULL(SUM(TotalAmount),0) AS CashTotal
FROM payment.Bill WITH (UPDLOCK, HOLDLOCK)
WHERE CashierShiftId=?
  AND Status='PAID'
  AND PaymentMethod='CASH'
```

Công thức:

```text
expectedClosingCash = OpeningCash + CashTotal
```

`CashierCashReconciliation.requireMatchingClosingCash(...)` yêu cầu số nhập vào
phải bằng chính xác `expectedClosingCash`. Chuyển khoản và QR ngân hàng không
được cộng vào số tiền mặt phải đếm.

Nếu sai, service ném exception, transaction rollback và Cashier phải nhập lại.

#### Bước 3: đóng két

```sql
UPDATE payment.CashierShift
SET ClosingCash=?,
    ClosedAt=SYSUTCDATETIME()
WHERE CashierShiftId=?
  AND ClosedAt IS NULL
```

Phải update đúng một dòng. Nếu bằng `0`, ca vừa bị một request khác kết thúc.

#### Bước 4: tan ca chấm công

Attendance đang mở cũng được đọc bằng `UPDLOCK, HOLDLOCK`, sau đó:

```sql
SELECT SYSUTCDATETIME()
```

```sql
UPDATE hr.Attendance
SET CheckInAt=?,
    CheckOutAt=?
WHERE AttendanceId=?
```

Status duyệt tiếp tục là `PENDING` để Manager duyệt chấm công.

Bảng dùng:

- `hr.ShiftTemplate`
- `hr.ShiftAssignment`
- `hr.Attendance`
- `payment.CashierShift`
- `payment.Bill`
- `iam.User`

## 7. Màn Sơ đồ bàn

### 7.1 GET `/cashier/table`

```text
TableServlet.doGet
  -> TableSessionService.getFloorMap(branchId)
  -> getPendingOpenRequests(branchId)
  -> getPendingSignals(branchId)
  -> tạo menu URL từ QrLink
  -> forward table-map.jsp
```

Query sơ đồ:

```sql
SELECT dt.DiningTableId, dt.BranchId, dt.TableNumber, dt.QrCode, dt.Status,
       ts.TableSessionId AS ActiveSessionId,
       (
           SELECT COUNT(*)
           FROM sales.OrderItem oi
           JOIN sales.Orders o ON o.OrderId=oi.OrderId
           WHERE o.TableSessionId=ts.TableSessionId
             AND oi.Status<>'CANCELLED'
       ) AS ItemCount
FROM sales.DiningTable dt
LEFT JOIN sales.TableSession ts
       ON ts.DiningTableId=dt.DiningTableId
      AND ts.Status='OPEN'
WHERE dt.BranchId=?
ORDER BY dt.TableNumber
```

Query khách đang yêu cầu mở bàn qua QR:

```sql
SELECT AggregateId, MIN(CreatedAt) AS FirstAt
FROM ops.OutboxEvent
WHERE EventType=?
  AND BranchId=?
  AND ProcessedAt IS NULL
GROUP BY AggregateId
ORDER BY MIN(CreatedAt)
```

Bind `EventType = 'table.open_requested'`.

Query tín hiệu gọi nhân viên/xin thanh toán:

```sql
SELECT oe.AggregateId, oe.EventType, dt.DiningTableId
FROM ops.OutboxEvent oe
JOIN sales.TableSession ts
  ON ts.TableSessionId=TRY_CONVERT(INT, oe.AggregateId)
JOIN sales.DiningTable dt
  ON dt.DiningTableId=ts.DiningTableId
WHERE oe.EventType IN (?,?)
  AND oe.ProcessedAt IS NULL
  AND ts.Status='OPEN'
  AND ts.BranchId=?
  AND dt.BranchId=?
ORDER BY CASE WHEN oe.EventType=? THEN 0 ELSE 1 END,
         oe.CreatedAt
```

Các event là `service.call` và `bill.requested`; `bill.requested` được ưu tiên.

### 7.2 Bấm bàn đang mở

Link trong `table-map.jsp`:

```text
GET /cashier/pos?sessionId=<activeSessionId>
```

Không ghi database; chuyển sang POS của phiên đó.

### 7.3 Bấm **Mở tại quầy**

Form:

```text
POST /cashier/table
action=openTable
tableId=<DiningTableId>
mode=counter
```

Luồng transaction:

1. Tìm phiên OPEN hiện có:

```sql
SELECT ts.TableSessionId, ts.BranchId, ts.DiningTableId,
       ts.OpenedBy, ts.OpenedAt, ts.ClosedAt, ts.Status,
       dt.TableNumber
FROM sales.TableSession ts
JOIN sales.DiningTable dt ON dt.DiningTableId=ts.DiningTableId
WHERE ts.DiningTableId=?
  AND ts.Status='OPEN'
```

2. Nếu chưa có, tạo phiên:

```sql
INSERT INTO sales.TableSession
    (BranchId, DiningTableId, OpenedBy, Status)
VALUES
    (?, ?, ?, 'OPEN')
```

3. Đánh dấu bàn có khách:

```sql
UPDATE sales.DiningTable
SET Status='OCCUPIED'
WHERE DiningTableId=?
```

4. Hạ yêu cầu mở bàn còn treo:

```sql
UPDATE ops.OutboxEvent
SET ProcessedAt=SYSUTCDATETIME()
WHERE EventType=?
  AND AggregateId=?
  AND ProcessedAt IS NULL
```

5. Commit rồi redirect:

```text
/cashier/pos?sessionId=<new-or-existing-session-id>
```

### 7.4 Bấm **Mở bằng QR**

Form giống trên nhưng `mode=qr`. Database transaction hoàn toàn giống mở tại
quầy. Sau commit, controller redirect:

```text
/cashier/table?qr=<tableId>
```

JSP bật modal QR. QR chứa URL do `QrLink.menuUrl(baseUrl, QrCode)` tạo; việc tạo
ảnh QR không chạy query khác.

### 7.5 Bấm **Đã tiếp nhận**

Form:

```text
POST /cashier/table
action=ackSignal
sessionId=<TableSessionId>
```

Service đọc session để xác minh:

- Session tồn tại.
- `session.BranchId == branchId` hiện tại.
- Status là `OPEN`.

Sau đó:

```sql
UPDATE ops.OutboxEvent
SET ProcessedAt=SYSUTCDATETIME()
WHERE EventType IN (?,?)
  AND AggregateId=?
  AND ProcessedAt IS NULL
```

### 7.6 Bấm **Đóng bàn**

Form:

```text
POST /cashier/table
action=closeTable
sessionId=<TableSessionId>
```

Service:

1. Đọc session và kiểm tra branch/status.
2. Đọc mọi món của session.
3. Chỉ cho đóng nếu mọi món đều `CANCELLED`.
4. Ghi:

```sql
UPDATE sales.TableSession
SET Status='CLOSED',
    ClosedAt=SYSUTCDATETIME()
WHERE TableSessionId=?
```

```sql
UPDATE sales.DiningTable
SET Status='EMPTY'
WHERE DiningTableId=?
```

5. Hạ `service.call` và `bill.requested` trong `ops.OutboxEvent`.

Thanh toán bill cuối cùng cũng tự đóng session và trả bàn về `EMPTY`; vì vậy
nút đóng bàn thủ công chủ yếu dành cho phiên rỗng/đã hủy hết món.

### 7.7 Đổi trạng thái bàn

POST parameters:

```text
action=setStatus
tableId=<id>
status=EMPTY|OCCUPIED|CLEANING
```

Controller/service đọc bàn bằng:

```sql
SELECT DiningTableId, BranchId, TableNumber, QrCode, Status
FROM sales.DiningTable
WHERE DiningTableId=?
```

Sau khi xác minh branch:

```sql
UPDATE sales.DiningTable
SET Status=?
WHERE DiningTableId=?
```

### 7.8 Gộp phiên bàn

POST parameters:

```text
action=merge
srcSessionId=<phiên nguồn>
dstSessionId=<phiên đích>
```

Service xác minh hai phiên:

- Khác nhau.
- Cùng branch đang đăng nhập.
- Cả hai `OPEN`.

Transaction:

```sql
UPDATE sales.Orders
SET TableSessionId=?
WHERE TableSessionId=?
```

```text
?1 = dstSessionId
?2 = srcSessionId
```

Sau đó đóng session nguồn, trả bàn nguồn về `EMPTY`, hạ tín hiệu của session
nguồn và commit.

### 7.9 Màn In QR bàn

`GET /cashier/table-qr` gọi lại đúng query `findFloorMap`. View
`cashier/table-qr.jsp` render QR và nút `window.print()`. Màn này chỉ đọc dữ
liệu, không có POST.

Bảng dùng:

- `sales.DiningTable`
- `sales.TableSession`
- `sales.Orders`
- `sales.OrderItem`
- `ops.OutboxEvent`

## 8. Màn POS/Đặt món

### 8.1 GET `/cashier/pos`

```text
PosServlet.doGet
  -> CatalogReadService.getPosMenu(branchId)
  -> TableSessionService.getOpenSessions(branchId)
  -> nếu có sessionId:
       -> getSession(sessionId), kiểm tra branch + OPEN
       -> lấy draft từ HTTP session
       -> OrderService.getSessionItemStatuses(sessionId)
  -> forward pos.jsp
```

### 8.2 Query nạp menu

Menu chi nhánh:

```sql
SELECT p.ProductId, p.Name, p.BasePrice, p.ImageUrl,
       bm.IsAvailable, bm.LocalPrice, bm.Is86, bm.BackInEta,
       CASE WHEN bm.ProductId IS NULL THEN 0 ELSE 1 END AS Published
FROM catalog.Product p
LEFT JOIN catalog.BranchMenu bm
       ON bm.ProductId=p.ProductId
      AND bm.BranchId=?
WHERE p.IsActive=1
ORDER BY p.Name
```

Món cạn nguyên liệu:

```sql
SELECT DISTINCT pr.ProductId
FROM catalog.ProductRecipe pr
JOIN catalog.BranchMenu bm
  ON bm.ProductId=pr.ProductId
 AND bm.BranchId=?
JOIN inventory.BranchInventory bi
  ON bi.IngredientId=pr.IngredientId
 AND bi.BranchId=?
WHERE bi.QuantityOnHand<=0
```

Java chỉ đưa lên POS món thỏa tất cả:

```text
Published = true
IsAvailable = true
Is86 = false
ProductId không nằm trong tập depleted
```

Nhóm modifier của từng product:

```sql
SELECT pmg.ProductId, pmg.ModifierGroupId, g.Name AS GroupName
FROM catalog.ProductModifierGroup pmg
JOIN catalog.ModifierGroup g
  ON pmg.ModifierGroupId=g.ModifierGroupId
WHERE pmg.ProductId=?
ORDER BY ...
```

Chi tiết group:

```sql
SELECT ModifierGroupId, Name, IsRequired, MinSelect, MaxSelect
FROM catalog.ModifierGroup
WHERE ModifierGroupId=?
```

Option:

```sql
SELECT ModifierOptionId, ModifierGroupId, Name, PriceDelta, IsActive
FROM catalog.ModifierOption
WHERE ModifierGroupId=?
ORDER BY ModifierOptionId
```

View hiện chỉ nhận ba nhóm lựa chọn: `Size`, `Đường`, `Đá`.

### 8.3 Giỏ hàng phía browser

`pos.jsp` giữ:

```javascript
let cart = [...]
```

Mỗi line:

```json
{
  "productId": 10,
  "name": "Cà phê đen",
  "quantity": 2,
  "unit": 35000,
  "optionIds": [1, 5],
  "optNames": ["Size S", "Bình thường"]
}
```

`name`, `unit`, `optNames` chỉ dùng để hiển thị. Khi gửi server, JavaScript chỉ
gửi:

```json
{
  "sessionId": 123,
  "orderType": "DINE_IN",
  "items": [
    {
      "productId": 10,
      "quantity": 2,
      "optionIds": [1, 5]
    }
  ]
}
```

Server không tin giá từ client. `UnitPrice` được tính lại từ
`BranchMenu.LocalPrice/BasePrice + SUM(ModifierOption.PriceDelta)`.

### 8.4 Validate số lượng tối đa 20

Front-end:

- Input có `min=1`, `max=20`.
- `addToCart` cộng tất cả line cùng `productId`.
- Tổng cùng loại vượt 20 thì không thêm.

Back-end:

```text
PosServlet
  -> CashierOrderValidator.validate(lines)
```

Validator:

- Danh sách không rỗng.
- `productId > 0`.
- `quantity > 0`.
- Cộng tổng theo `productId` bằng `Math.addExact`.
- Tổng mỗi product không vượt `20`.

Do có validate cả client và server, sửa JSON bằng DevTools cũng không vượt được
giới hạn.

### 8.5 Bấm **Gửi đơn**

Front-end:

```javascript
fetch('/cashier/pos?_csrf=<token>', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  body: JSON.stringify(payload)
})
```

Controller:

```text
PosServlet.doPost
  -> CSRF
  -> đọc JSON bằng Jackson ObjectMapper
  -> lấy sessionId
  -> nếu có session: kiểm tra session thuộc branch và OPEN
  -> tự suy orderType:
       sessionId == null ? TAKEAWAY : DINE_IN
  -> CashierOrderValidator
  -> OrderService.placeOrder(
         branchId,
         sessionId,
         "COUNTER",
         orderType,
         userId,
         lines
     )
```

Giá trị `orderType` do client gửi không được sử dụng để quyết định loại đơn.

#### Transaction tạo order

`OrderService.placeOrder` mở một transaction cho toàn bộ order.

1. Đọc lại menu, cờ `IsAvailable`, `Is86`, giá và tồn kho.
2. Insert order:

```sql
INSERT INTO sales.Orders
    (BranchId, TableSessionId, CustomerId, Source,
     OrderType, Status, CreatedBy)
VALUES
    (?, ?, NULL, 'COUNTER', ?, 'ACTIVE', ?)
```

3. Sinh mã gọi món. Số thứ tự được đếm từ đầu ngày kinh doanh:

```sql
SELECT COUNT(*)
FROM sales.Orders
WHERE BranchId=?
  AND CreatedAt>=?
```

Prefix:

```text
T = TAKEAWAY
D = DINE_IN tại quầy
G = đơn QR của khách
```

Ghi mã:

```sql
UPDATE sales.Orders
SET PickupCode=?
WHERE OrderId=?
```

4. Với từng line, validate lại product còn bán/còn hàng/chưa 86.
5. Validate modifier thuộc đúng product và số lựa chọn đạt `MinSelect/MaxSelect`.
6. Tính:

```text
UnitPrice = localPrice nếu có, ngược lại BasePrice
          + tổng PriceDelta của option đã chọn
```

7. Insert món:

```sql
INSERT INTO sales.OrderItem
    (OrderId, ProductId, Quantity, UnitPrice, Note, Status)
VALUES
    (?, ?, ?, ?, ?, 'WAITING')
```

8. Insert modifier:

```sql
INSERT INTO sales.OrderItemModifier
    (OrderItemId, ModifierOptionId, PriceDelta)
VALUES
    (?, ?, ?)
```

9. Ghi event:

```sql
INSERT INTO ops.OutboxEvent
    (EventType, AggregateId, BranchId, Payload)
VALUES
    ('order.created', <OrderId>, <BranchId>, <JSON>)
```

10. Commit và trả JSON:

```json
{"orderId": 811}
```

Browser hiển thị “Đã gửi đơn #811 tới bếp”, xóa giỏ và vẫn ở màn POS. Đơn mang
đi không tự nhảy sang Checkout.

Nếu bất kỳ product/modifier nào không hợp lệ, transaction rollback cả order.

### 8.6 Từ POS sang Barista và bàn giao

Ngay khi commit:

- Order có `Status='ACTIVE'`.
- Item có `Status='WAITING'`.
- Query KDS của Barista đọc trực tiếp `sales.OrderItem` có `WAITING`.
- Barista claim: `WAITING -> MAKING`.
- Barista hoàn thành: `MAKING -> READY`.
- Khi hoàn thành, service Barista mới trừ kho và ghi audit/event.
- Cashier thấy `READY` trong khu bàn giao của Inbox.
- Cashier nhận: `READY -> PICKED_UP`.
- Cashier giao khách: `PICKED_UP -> SERVED`.
- Nếu mọi item cuối cùng là `SERVED/CANCELLED`, order thành `COMPLETED`.

POS không gọi Checkout và không tạo Bill.

### 8.7 **Tạm dừng**

Chỉ hiện khi POS đang gắn `sessionId`.

Form:

```text
POST /cashier/pos
action=saveDraft
sessionId=<id>
cartJson=<JSON>
```

`PosServlet`:

- Parse JSON.
- Yêu cầu JSON là array.
- Xác minh session thuộc branch và `OPEN`.
- Lưu vào map trong HTTP session:

```text
session[Constants.SESSION_DRAFT_CARTS][sessionId] = safeJson
```

Không có câu SQL ghi draft. Draft:

- Mất khi session đăng nhập hết hạn.
- Không chia sẻ giữa hai máy/trình duyệt.
- Chưa đi vào Barista/Inbox.

### 8.8 **Hủy đặt món** khi còn là draft

Form `action=discardDraft`.

Controller:

1. Xóa draft khỏi HTTP session.
2. Gọi `closeSessionIfNoActiveItems(sessionId)`.
3. Nếu phiên chưa có món thật, đóng session và trả bàn về `EMPTY`.
4. Nếu đã có món không `CANCELLED`, session không bị đóng.

Bảng dùng:

- `catalog.Product`
- `catalog.BranchMenu`
- `catalog.ProductRecipe`
- `catalog.ModifierGroup`
- `catalog.ModifierOption`
- `catalog.ProductModifierGroup`
- `inventory.BranchInventory`
- `sales.TableSession`
- `sales.Orders`
- `sales.OrderItem`
- `sales.OrderItemModifier`
- `ops.OutboxEvent`

## 9. Màn Đơn đến & Bàn giao

### 9.1 GET `/cashier/inbox`

```text
OrderInboxServlet.doGet
  -> OrderService.getIncomingOrders(branchId)
  -> đếm stale order
  -> PickupService.getReadyTickets(branchId)
  -> PickupService.getPickedUpItems(branchId)
  -> forward inbox.jsp
```

### 9.2 Query danh sách order đang xử lý

```sql
SELECT o.OrderId, o.BranchId, o.TableSessionId, o.CustomerId,
       o.Source, o.OrderType, o.Status, o.CreatedBy,
       o.CreatedAt, o.PickupCode, dt.TableNumber
FROM sales.Orders o
LEFT JOIN sales.TableSession ts
       ON ts.TableSessionId=o.TableSessionId
LEFT JOIN sales.DiningTable dt
       ON dt.DiningTableId=ts.DiningTableId
WHERE o.BranchId=?
  AND o.Status='ACTIVE'
ORDER BY
  CASE WHEN o.CreatedAt<? THEN 0 ELSE 1 END,
  o.CreatedAt DESC
```

Bind:

```text
?1 = branchId
?2 = mốc đầu ngày kinh doanh UTC của chi nhánh
```

Order cũ hơn mốc ngày kinh doanh được đưa lên đầu và gắn `stale=true`.

Mỗi order được nạp item:

```sql
SELECT oi..., p.Name, c.Name, o.BranchId, o.OrderType,
       o.CreatedAt, o.PickupCode, dt.TableNumber, ...
FROM sales.OrderItem oi
JOIN catalog.Product p ON p.ProductId=oi.ProductId
JOIN catalog.Category c ON c.CategoryId=p.CategoryId
JOIN sales.Orders o ON o.OrderId=oi.OrderId
LEFT JOIN sales.TableSession ts ON ...
LEFT JOIN sales.DiningTable dt ON ...
LEFT JOIN iam.[User] bu ON ...
LEFT JOIN iam.[User] cu ON ...
WHERE oi.OrderId=?
ORDER BY oi.OrderItemId
```

Modifier được nạp theo lô:

```sql
SELECT oim.OrderItemModifierId, oim.OrderItemId,
       oim.ModifierOptionId, oim.PriceDelta,
       mo.Name AS OptionName
FROM sales.OrderItemModifier oim
JOIN catalog.ModifierOption mo
  ON mo.ModifierOptionId=oim.ModifierOptionId
WHERE oim.OrderItemId IN (?, ?, ...)
ORDER BY oim.OrderItemId, oim.OrderItemModifierId
```

### 9.3 Vì sao order `PAID` có thể từng hiện trong Inbox?

Inbox lọc theo `sales.Orders.Status='ACTIVE'`, không lọc trực tiếp theo
`payment.Bill.Status`.

Nhãn thanh toán chỉ là dữ liệu suy ra:

Với order tại bàn:

```sql
SELECT Status
FROM payment.Bill
WHERE TableSessionId=?
```

Với takeaway:

```sql
SELECT DISTINCT b.Status
FROM payment.Bill b
JOIN payment.BillItem bi ON bi.BillId=b.BillId
JOIN sales.OrderItem oi ON oi.OrderItemId=bi.OrderItemId
WHERE oi.OrderId=?
```

Java suy:

```text
có PAID và không còn UNPAID -> PAID
có VOID/REFUND và chưa PAID -> ERROR
còn lại                    -> PAYING
```

Do đó một bill đã `PAID` vẫn có thể xuất hiện nếu order còn `ACTIVE`. Với luồng
hiện tại, trường hợp bình thường phải tránh được vì thanh toán chỉ cho phép khi
mọi BillItem là `SERVED`; item cuối cùng được giao sẽ chuyển order sang
`COMPLETED`, nên query Inbox không còn lấy order đó.

Nếu vẫn thấy order `PAID`, cần kiểm tra lệch dữ liệu:

```sql
SELECT o.OrderId, o.Status AS OrderStatus,
       oi.OrderItemId, oi.Status AS ItemStatus,
       b.BillId, b.Status AS BillStatus
FROM sales.Orders o
JOIN sales.OrderItem oi ON oi.OrderId=o.OrderId
LEFT JOIN payment.BillItem bi ON bi.OrderItemId=oi.OrderItemId
LEFT JOIN payment.Bill b ON b.BillId=bi.BillId
WHERE o.OrderId=?
```

Khả năng chính là dữ liệu cũ đã pay trước khi item chuyển `SERVED`, hoặc order
không được chạy `completeIfAllItemsFinal`.

### 9.4 Query khu **Món sẵn bàn giao**

Món `READY`:

```sql
SELECT ...
FROM sales.OrderItem oi
JOIN sales.Orders o ON o.OrderId=oi.OrderId
...
WHERE o.BranchId=?
  AND o.Status='ACTIVE'
  AND oi.Status='READY'
ORDER BY oi.DoneAt, oi.OrderItemId
```

Service gom theo `OrderId`, sau đó nạp toàn bộ item của các order trong một query
`WHERE oi.OrderId IN (...)` để Cashier đối chiếu đủ món.

Món đã nhận, đang mang giao:

```sql
SELECT ...
FROM sales.OrderItem oi
JOIN sales.Orders o ON o.OrderId=oi.OrderId
...
WHERE o.BranchId=?
  AND o.Status='ACTIVE'
  AND oi.Status='PICKED_UP'
ORDER BY oi.PickedUpAt, oi.OrderItemId
```

### 9.5 Bấm **Đã nhận N món**

Form trong `cashier/handoff/cards.jsp`:

```text
POST /cashier/inbox
action=pickUp
orderItemId=<id>
```

Luồng:

```text
OrderInboxServlet
  -> PickupService.pickUpItem
  -> OrderService.markItemPickedUp
  -> transaction
```

Update nguyên tử, có branch scope và status guard:

```sql
UPDATE oi
SET oi.Status='PICKED_UP',
    oi.PickedUpBy=?,
    oi.PickedUpAt=SYSUTCDATETIME()
FROM sales.OrderItem oi
JOIN sales.Orders o ON o.OrderId=oi.OrderId
WHERE oi.OrderItemId=?
  AND o.BranchId=?
  AND oi.Status='READY'
```

Chỉ request thắng race mới update được một dòng.

Audit:

```sql
INSERT INTO ops.OrderItemActionLog
    (OrderItemId, BranchId, ActionType, FromStatus,
     ToStatus, Reason, PerformedBy)
VALUES
    (?, ?, 'PICK_UP', 'READY', 'PICKED_UP', NULL, ?)
```

Event:

```sql
INSERT INTO ops.OutboxEvent(...)
VALUES ('item.picked_up', <OrderItemId>, <BranchId>, <JSON>)
```

### 9.6 Bấm **Đã nhận tất cả món sẵn sàng**

Form:

```text
action=pickUpAllReady
orderId=<id>
```

Service mở một transaction, đọc toàn bộ item của order, lặp các item đang
`READY`, chạy đúng update `READY -> PICKED_UP` và audit cho từng item.

### 9.7 Bấm **Đã giao khách**

Form:

```text
action=serve
orderItemId=<id>
```

Update:

```sql
UPDATE oi
SET oi.Status='SERVED',
    oi.ServedAt=SYSUTCDATETIME()
FROM sales.OrderItem oi
JOIN sales.Orders o ON o.OrderId=oi.OrderId
WHERE oi.OrderItemId=?
  AND o.BranchId=?
  AND oi.Status IN ('PICKED_UP')
```

Sau update:

1. Ghi `ops.OrderItemActionLog` với action `SERVE`.
2. Ghi event `order.status_changed` cho item.
3. Chạy query `completeIfAllItemsFinal` cho order.
4. Nếu item cuối cùng vừa hoàn tất, order chuyển `ACTIVE -> COMPLETED`.

Đây là cổng làm cho order takeaway đủ điều kiện mở Checkout.

### 9.8 Bấm **Hủy đơn**

Form:

```text
action=void
orderId=<id>
```

`OrderService.voidOrder`:

1. Đọc order và yêu cầu `Status='ACTIVE'`.
2. Đọc item.
3. Nếu có item `MAKING`, `READY`, `PICKED_UP` hoặc `SERVED`, trả false.
4. Item `WAITING` được đổi:

```sql
UPDATE sales.OrderItem
SET Status='CANCELLED'
WHERE OrderItemId=?
```

5. Order:

```sql
UPDATE sales.Orders
SET Status='CANCELLED'
WHERE OrderId=?
```

6. Ghi `order.status_changed` vào Outbox.
7. Commit.

Luồng này không trừ/hoàn kho cho order chưa pha. Nếu item remake đang giữ
reservation, service giải phóng reservation trước khi hủy.

### 9.9 Bấm **Hủy món** bị BLOCKED

Form:

```text
action=cancelItem
orderItemId=<id>
reason="Hủy món bị chặn từ Inbox"
```

Service chỉ chấp nhận item ở `WAITING`, `MAKING` hoặc `BLOCKED`.

Trước khi hủy, kiểm tra item đã lên bill chưa:

```sql
SELECT 1
FROM payment.BillItem
WHERE OrderItemId=?
```

Nếu chưa lên bill:

```sql
UPDATE oi
SET oi.Status='CANCELLED'
FROM sales.OrderItem oi
JOIN sales.Orders o ON o.OrderId=oi.OrderId
WHERE oi.OrderItemId=?
  AND o.BranchId=?
  AND oi.Status IN ('WAITING','MAKING','BLOCKED')
```

Sau đó ghi audit, outbox và thử complete order.

Bảng dùng:

- `sales.Orders`
- `sales.OrderItem`
- `sales.OrderItemModifier`
- `sales.TableSession`
- `sales.DiningTable`
- `catalog.Product`
- `catalog.Category`
- `catalog.ModifierOption`
- `payment.Bill`
- `payment.BillItem`
- `ops.OrderItemActionLog`
- `ops.OutboxEvent`

## 10. Màn Checkout/Thanh toán

### 10.1 GET Checkout chưa chọn đối tượng

Route:

```text
GET /cashier/checkout
```

Controller:

```text
CheckoutServlet.doGet
  -> lấy current CashierShift
  -> TableSessionService.getOpenSessions(branchId)
  -> BillingService.getTakeawayOrdersAwaitingPayment(branchId)
  -> forward checkout.jsp
```

Open session:

```sql
SELECT ts..., dt.TableNumber
FROM sales.TableSession ts
JOIN sales.DiningTable dt ON dt.DiningTableId=ts.DiningTableId
WHERE ts.BranchId=?
  AND ts.Status='OPEN'
ORDER BY dt.TableNumber
```

Takeaway chờ thanh toán:

```sql
SELECT o..., dt.TableNumber
FROM sales.Orders o
LEFT JOIN sales.TableSession ts ON ...
LEFT JOIN sales.DiningTable dt ON ...
WHERE o.BranchId=?
  AND o.OrderType='TAKEAWAY'
  AND o.Status<>'CANCELLED'
  AND (
      NOT EXISTS (
          SELECT 1
          FROM sales.OrderItem oi
          JOIN payment.BillItem bi
            ON bi.OrderItemId=oi.OrderItemId
          WHERE oi.OrderId=o.OrderId
      )
      OR EXISTS (
          SELECT 1
          FROM sales.OrderItem oi
          JOIN payment.BillItem bi
            ON bi.OrderItemId=oi.OrderItemId
          JOIN payment.Bill b
            ON b.BillId=bi.BillId
          WHERE oi.OrderId=o.OrderId
            AND b.Status='UNPAID'
      )
  )
ORDER BY o.CreatedAt DESC
```

Nhờ nhánh `OR EXISTS (... UNPAID)`, đơn takeaway đã mở Checkout rồi thoát ra vẫn
được liệt kê để thanh toán tiếp.

JSP:

- Order `COMPLETED`: hiện nút **Thanh toán**.
- Order chưa `COMPLETED`: hiện **Theo dõi đơn** về Inbox.

### 10.2 Mở Checkout cho bàn

Link:

```text
GET /cashier/checkout?sessionId=<id>
```

Controller:

1. Đọc session.
2. Yêu cầu session thuộc branch hiện tại và status `OPEN`.
3. Lấy `shiftId` đang mở nếu có.
4. Gọi `BillingService.buildSessionBill`.

#### Dựng bill cho session

Transaction:

1. Tìm bill chưa thu:

```sql
SELECT b..., dt.TableNumber, v.Code AS VoucherCode
FROM payment.Bill b
LEFT JOIN sales.TableSession ts ON ...
LEFT JOIN sales.DiningTable dt ON ...
LEFT JOIN payment.Voucher v ON ...
WHERE b.TableSessionId=?
  AND b.Status='UNPAID'
ORDER BY b.BillId
```

2. Đọc mọi item của session.
3. Bỏ item `CANCELLED`.
4. Với item chưa có trong `payment.BillItem`, tạo bill mặc định nếu cần:

```sql
INSERT INTO payment.Bill
    (BranchId, TableSessionId, CashierShiftId, Status)
VALUES
    (?, ?, ?, 'UNPAID')
```

5. Gắn item:

```sql
INSERT INTO payment.BillItem
    (BillId, OrderItemId, Amount)
VALUES
    (?, ?, ?)
```

`Amount = OrderItem.UnitPrice * OrderItem.Quantity`.

6. Recompute toàn bộ bill `UNPAID` của session.
7. Commit.
8. Đọc lại bill + item để render.

Việc dựng bill là idempotent nhờ query `existsForOrderItem` và unique
`payment.BillItem(OrderItemId)`.

### 10.3 Mở Checkout cho takeaway

Link:

```text
GET /cashier/checkout?orderId=<id>
```

`BillingService.buildTakeawayBill` chỉ chấp nhận:

```text
order tồn tại
order.BranchId == branchId
TableSessionId == null
OrderType == TAKEAWAY
Status == COMPLETED
```

Nếu order chưa được Cashier giao đủ món, service trả list rỗng và controller
redirect về Inbox với lỗi.

Sau khi hợp lệ:

- Tìm bill đã có của order.
- Tái sử dụng bill `UNPAID`.
- Gắn các item chưa có BillItem.
- Recompute.
- Commit.

Takeaway dùng `TableSessionId=NULL` trên `payment.Bill`.

### 10.4 Tính tiền bill

Code thuần nằm ở `com.cafe.common.BillCalculator`.

```text
subtotal = SUM(BillItem.Amount)
discount = voucher PERCENT hoặc FIXED, kẹp trong [0, subtotal]
net      = subtotal - discount
vat      = net * 0.08
total    = net + vat
```

Ghi:

```sql
UPDATE payment.Bill
SET Subtotal=?,
    DiscountAmount=?,
    VatAmount=?,
    TotalAmount=?,
    VoucherId=?
WHERE BillId=?
```

Với một session có nhiều bill:

- Discount và VAT được tính một lần trên toàn session.
- `BillCalculator.allocateByWeight` phân bổ theo tỷ trọng.
- Thuật toán largest remainder đảm bảo tổng các bill không lệch `0.01`.
- VoucherId chỉ gắn ở bill đầu để số lượt voucher chỉ tăng một lần.

### 10.5 Bấm **Áp dụng voucher**

Form:

```text
POST /cashier/checkout
action=applyVoucher
sessionId=<optional>
orderId=<optional>
billId=<id>
code=<voucher code>
```

Luồng:

```text
CheckoutServlet
  -> BillingService.applyVoucher
  -> getBill
  -> VoucherService.validateVoucher
  -> VoucherDao.findByCode
  -> recompute session hoặc bill takeaway
  -> update payment.Bill
```

Query:

```sql
SELECT v.VoucherId, v.Code, v.DiscountType, v.DiscountValue,
       v.MinOrderAmount, v.Scope, v.BranchId,
       v.StartDate, v.EndDate, v.UsageLimit, v.UsedCount,
       v.IsActive, b.Name AS BranchName
FROM payment.Voucher v
LEFT JOIN org.Branch b ON v.BranchId=b.BranchId
WHERE v.Code=?
```

Validate:

- Voucher tồn tại và active.
- Voucher branch áp dụng đúng branch.
- Đã tới `StartDate`, chưa quá `EndDate`.
- Chưa hết `UsageLimit`.
- Bill đạt `MinOrderAmount`.

### 10.6 Bấm **Bỏ voucher**

Form `action=removeVoucher`, `billId`.

Service recompute lại session/bill với `VoucherId=NULL`, sau đó chạy câu
`UPDATE payment.Bill SET ... VoucherId=?`.

### 10.7 Bấm **Tách món đã chọn**

Chỉ áp dụng cho dine-in, không hiện cho takeaway.

Form:

```text
action=splitBill
sessionId=<id>
billItemId=<id 1>
billItemId=<id 2>
...
```

Transaction:

1. Tạo bill `UNPAID` mới.
2. Với mỗi BillItem:

```sql
SELECT BillItemId, BillId, OrderItemId, Amount
FROM payment.BillItem
WHERE BillItemId=?
```

```sql
UPDATE payment.BillItem
SET BillId=?
WHERE BillItemId=?
```

3. Bill nào rỗng:

```sql
SELECT COUNT(*)
FROM payment.BillItem
WHERE BillId=?
```

```sql
UPDATE payment.Bill
SET Status='VOID'
WHERE BillId=?
  AND Status<>'PAID'
```

4. Recompute toàn session rồi commit.

### 10.8 Bấm **Gộp hóa đơn**

Form:

```text
action=mergeBill
sessionId=<id>
billId=<id 1>
billId=<id 2>
...
```

Bill đầu tiên trong danh sách là target. Các item bill sau được chuyển:

```sql
UPDATE payment.BillItem
SET BillId=<targetBillId>
WHERE BillItemId=?
```

Bill nguồn được `VOID`; target được recompute; toàn bộ trong một transaction.

### 10.9 Bấm **Thu tiền**: trace đầy đủ

Đây là luồng ví dụ “bấm nút thanh toán bill này đi từ đâu đến đâu”.

#### Bước A - điều kiện để form thanh toán xuất hiện

`cashier/checkout.jsp` kiểm tra:

```jsp
<c:when test="${b.readyForPayment}">
```

`Bill.isReadyForPayment()` trả true khi:

- Bill có ít nhất một item.
- Mọi `BillItem.status` đọc từ `sales.OrderItem.Status` đều là `SERVED`.

Nếu chưa đủ, JSP chỉ hiện:

```text
Chờ Barista pha xong và Cashier bàn giao đủ món trước khi thanh toán.
```

#### Bước B - payload từ form

```text
POST /cashier/checkout
_csrf=<token>
action=pay
sessionId=<id nếu dine-in>
orderId=<id nếu takeaway>
billId=<bill cần thu>
method=CASH|TRANSFER|QR_BANK
```

Browser hỏi confirm. Với QR, text nút đổi thành “Đã nhận tiền”; QR được tạo tại
browser từ payload server, không gọi database.

#### Bước C - filter

Request phải qua:

1. Đăng nhập.
2. Role Cashier.
3. Branch active.
4. Duty state `ON_DUTY`.
5. CSRF hợp lệ.

#### Bước D - controller validate lại bill

`CheckoutServlet.validatePayable(billId, branchId)` không tin trạng thái JSP.

Đọc bill:

```sql
SELECT b.BillId, b.BranchId, b.TableSessionId, b.CashierShiftId,
       b.Subtotal, b.VatAmount, b.DiscountAmount, b.TotalAmount,
       b.VoucherId, b.PaymentMethod, b.Status, b.PaidAt, b.CreatedAt,
       dt.TableNumber, v.Code AS VoucherCode
FROM payment.Bill b
LEFT JOIN sales.TableSession ts ON ts.TableSessionId=b.TableSessionId
LEFT JOIN sales.DiningTable dt ON dt.DiningTableId=ts.DiningTableId
LEFT JOIN payment.Voucher v ON v.VoucherId=b.VoucherId
WHERE b.BillId=?
```

Đọc item:

```sql
SELECT bi.BillItemId, bi.BillId, bi.OrderItemId, bi.Amount,
       p.Name AS ProductName, oi.Quantity, oi.Status
FROM payment.BillItem bi
JOIN sales.OrderItem oi ON oi.OrderItemId=bi.OrderItemId
JOIN catalog.Product p ON p.ProductId=oi.ProductId
WHERE bi.BillId=?
ORDER BY bi.BillItemId
```

Controller yêu cầu:

- Bill tồn tại.
- `bill.BranchId == branchId`.
- `Status='UNPAID'`.
- Có item.
- Mọi item `SERVED`.
- `TotalAmount > 0`.
- Voucher, nếu có, vẫn còn hợp lệ.

#### Bước E - tìm ca đang mở

```text
CashierShiftService.getCurrentShift(currentUserId)
```

Nếu không có `shiftId`, `BillingService.payBill` trả false.

#### Bước F - service mở transaction thanh toán

```text
BillingService.payBill(billId, method, shiftId)
  -> method phải thuộc CASH, TRANSFER, QR_BANK
  -> BEGIN TRANSACTION
```

Service đọc lại bill và voucher trong transaction.

Đếm item:

```sql
SELECT COUNT(*)
FROM payment.BillItem
WHERE BillId=?
```

Yêu cầu count > 0 và total > 0.

#### Bước G - câu query thanh toán bill

Đây là câu query quyết định bill đã thanh toán:

```sql
UPDATE b
SET Status='PAID',
    PaymentMethod=?,
    CashierShiftId=cs.CashierShiftId,
    PaidAt=SYSUTCDATETIME()
FROM payment.Bill b
JOIN payment.CashierShift cs WITH (UPDLOCK, HOLDLOCK)
  ON cs.CashierShiftId=?
 AND cs.BranchId=b.BranchId
 AND cs.ClosedAt IS NULL
WHERE b.BillId=?
  AND b.Status='UNPAID'
```

Bind:

```text
?1 = method, ví dụ CASH
?2 = shiftId của Cashier hiện tại
?3 = billId từ form
```

Ví dụ thu tiền mặt bill `#811` trong ca `#55`, PreparedStatement bind:

```text
ps.setString(1, "CASH");
ps.setInt(2, 55);
ps.setInt(3, 811);
```

SQL Server chỉ đổi bill `#811` sang `PAID` nếu ca `#55` còn mở, cùng branch với
bill và bill vẫn đang `UNPAID`.

Câu SQL đồng thời bảo vệ:

- Không double-pay: chỉ update bill còn `UNPAID`.
- Ca phải còn mở.
- Ca và bill phải cùng branch.
- Dòng CashierShift bị khóa tới cuối transaction.

Nếu `executeUpdate()` trả `0`, service rollback và trả thất bại.

#### Bước H - voucher nếu có

```sql
UPDATE payment.Voucher
SET UsedCount=UsedCount+1
WHERE VoucherId=?
```

```sql
INSERT INTO payment.VoucherRedemption
    (VoucherId, BillId, DiscountApplied)
VALUES
    (?, ?, ?)
```

#### Bước I - ghi event thanh toán

```sql
INSERT INTO ops.OutboxEvent
    (EventType, AggregateId, BranchId, Payload)
VALUES
    (
      'payment.completed',
      <BillId>,
      <BranchId>,
      '{"billId":...,"method":"...","total":...}'
    )
```

#### Bước J - nếu bill thuộc bàn

Service kiểm tra còn bill `UNPAID` của session không:

```sql
SELECT b...
FROM payment.Bill b
...
WHERE b.TableSessionId=?
  AND b.Status='UNPAID'
ORDER BY b.BillId
```

Nếu không còn:

```sql
UPDATE sales.TableSession
SET Status='CLOSED',
    ClosedAt=SYSUTCDATETIME()
WHERE TableSessionId=?
```

```sql
UPDATE sales.DiningTable
SET Status='EMPTY'
WHERE DiningTableId=?
```

```sql
UPDATE ops.OutboxEvent
SET ProcessedAt=SYSUTCDATETIME()
WHERE EventType='bill.requested'
  AND AggregateId=<sessionId>
  AND ProcessedAt IS NULL
```

#### Bước K - commit và redirect

Tất cả các bước G-J cùng transaction.

- Takeaway: đặt `flashOk`, redirect `/cashier/history`.
- Dine-in còn bill chưa thu: quay lại Checkout của session.
- Dine-in đã đóng session: redirect `/cashier/table`.

Sau commit, bill có:

```text
Status           = PAID
PaymentMethod    = phương thức đã chọn
CashierShiftId   = ca hiện tại
PaidAt           = giờ UTC từ SQL Server
```

Bảng dùng trong thanh toán:

- `payment.Bill`
- `payment.BillItem`
- `payment.CashierShift`
- `payment.Voucher`
- `payment.VoucherRedemption`
- `sales.OrderItem`
- `catalog.Product`
- `sales.TableSession`
- `sales.DiningTable`
- `ops.OutboxEvent`

### 10.10 Hủy bill chưa thu

Luồng hiện có thể gọi từ service Checkout hoặc màn lịch sử, nhưng UI chi tiết
chỉ hiện nút khi `Status='UNPAID'`.

Query:

```sql
UPDATE payment.Bill
SET Status='VOID'
WHERE BillId=?
  AND Status<>'PAID'
```

Sau đó ghi `bill.voided` kèm `reason` và `userId` vào `ops.OutboxEvent`.

Không có query hoàn tiền trong role Cashier.

## 11. Màn Lịch sử hóa đơn

### 11.1 GET danh sách

Route mặc định:

```text
GET /cashier/history
```

Nếu Cashier đang có ca mở và không truyền `scope=branch`:

```sql
SELECT TOP 200 b..., dt.TableNumber, v.Code AS VoucherCode
FROM payment.Bill b
LEFT JOIN sales.TableSession ts ON ...
LEFT JOIN sales.DiningTable dt ON ...
LEFT JOIN payment.Voucher v ON ...
WHERE b.CashierShiftId=?
ORDER BY b.CreatedAt DESC
```

Nếu không có ca mở hoặc chọn toàn chi nhánh:

```sql
SELECT TOP 100 b..., dt.TableNumber, v.Code AS VoucherCode
FROM payment.Bill b
LEFT JOIN ...
WHERE b.BranchId=?
ORDER BY b.CreatedAt DESC
```

Filter hình thức:

```text
?method=CASH
?method=TRANSFER
?method=QR_BANK
```

Được `BillHistoryServlet.filterByMethod` lọc trong Java sau khi query, không
thêm điều kiện SQL.

Thời gian hiển thị:

```text
PaidAt nếu có, ngược lại CreatedAt
-> BusinessDay.fmtFullDateTimeVn(...)
```

### 11.2 Bấm **Xem**

Link:

```text
GET /cashier/history?action=view&billId=<id>
```

Controller gọi:

```text
BillingService.getBill
  -> BillDao.findById
  -> BillItemDao.findByBill
  -> forward bill-view.jsp
```

Hai query là query bill và BillItem đã ghi tại mục 10.9.

### 11.3 In/Tái in

Nút gọi `window.print()` tại browser. Không có request mới, không ghi database.

### 11.4 Hủy hóa đơn UNPAID

Form tại `bill-view.jsp`:

```text
POST /cashier/history
action=void
billId=<id>
reason=<bắt buộc, maxlength=255 ở HTML>
```

Controller bắt buộc reason không rỗng, sau đó gọi
`BillingService.voidBill`; query và event như mục 10.10.

Bảng dùng:

- `payment.Bill`
- `payment.BillItem`
- `payment.Voucher`
- `sales.OrderItem`
- `catalog.Product`
- `sales.TableSession`
- `sales.DiningTable`
- `ops.OutboxEvent`

## 12. Luồng nghiệp vụ chính từ đặt món đến doanh thu

```text
1. Cashier mở/được xếp ca
   hr.ShiftAssignment -> hr.Attendance
   payment.CashierShift OPEN

2. Cashier mở bàn (dine-in) hoặc chọn takeaway
   sales.TableSession OPEN (dine-in)

3. Cashier gửi đơn
   sales.Orders ACTIVE
   sales.OrderItem WAITING
   ops.OutboxEvent order.created

4. Barista nhận pha
   OrderItem WAITING -> MAKING

5. Barista pha xong
   OrderItem MAKING -> READY
   trừ inventory

6. Cashier nhận khỏi quầy
   OrderItem READY -> PICKED_UP

7. Cashier giao khách
   OrderItem PICKED_UP -> SERVED
   order -> COMPLETED khi mọi item final

8. Cashier mở Checkout
   payment.Bill UNPAID
   payment.BillItem được dựng idempotent

9. Cashier thu tiền
   payment.Bill UNPAID -> PAID
   gắn PaymentMethod + CashierShiftId + PaidAt
   ops.OutboxEvent payment.completed

10. Dashboard/lịch sử/kết ca đọc payment.Bill PAID

11. Cashier kết ca
    expected cash = OpeningCash + SUM(PAID CASH bills)
    nhập đúng mới đóng payment.CashierShift
    hr.Attendance được clock-out
```

## 13. Bảng tra cứu: thao tác nào ghi bảng nào

| Thao tác | Bảng đọc chính | Bảng ghi chính |
|---|---|---|
| Xem dashboard | `payment.Bill` | Không |
| Bắt đầu ca | `hr.ShiftAssignment`, `hr.Attendance`, `payment.CashierShift` | `hr.Attendance`, `payment.CashierShift` |
| Kết ca | `payment.CashierShift`, `payment.Bill`, `hr.Attendance` | `payment.CashierShift`, `hr.Attendance` |
| Xem sơ đồ bàn | `sales.DiningTable`, `sales.TableSession`, `sales.OrderItem`, `ops.OutboxEvent` | Không |
| Mở bàn | `sales.TableSession` | `sales.TableSession`, `sales.DiningTable`, `ops.OutboxEvent` |
| Đóng bàn | `sales.TableSession`, `sales.OrderItem` | `sales.TableSession`, `sales.DiningTable`, `ops.OutboxEvent` |
| Gộp bàn | `sales.TableSession` | `sales.Orders`, `sales.TableSession`, `sales.DiningTable`, `ops.OutboxEvent` |
| Nạp POS | `catalog.*`, `inventory.BranchInventory`, `sales.TableSession` | Không |
| Gửi đơn | `catalog.*`, `inventory.BranchInventory` | `sales.Orders`, `sales.OrderItem`, `sales.OrderItemModifier`, `ops.OutboxEvent` |
| Lưu draft | HTTP session | HTTP session, không DB |
| Xem Inbox | `sales.*`, `payment.Bill`, `payment.BillItem` | Không |
| Nhận món | `sales.OrderItem` | `sales.OrderItem`, `ops.OrderItemActionLog`, `ops.OutboxEvent` |
| Giao món | `sales.OrderItem` | `sales.OrderItem`, `sales.Orders`, `ops.OrderItemActionLog`, `ops.OutboxEvent` |
| Mở Checkout | `sales.*`, `payment.*` | `payment.Bill`, `payment.BillItem` |
| Áp voucher | `payment.Bill`, `payment.Voucher` | `payment.Bill` |
| Tách/gộp bill | `payment.Bill`, `payment.BillItem` | `payment.Bill`, `payment.BillItem` |
| Thu tiền | `payment.*`, `sales.*` | `payment.Bill`, `payment.Voucher`, `payment.VoucherRedemption`, `sales.TableSession`, `sales.DiningTable`, `ops.OutboxEvent` |
| Xem lịch sử | `payment.Bill`, `payment.BillItem`, `sales.*`, `catalog.Product` | Không |
| Hủy bill UNPAID | `payment.Bill` | `payment.Bill`, `ops.OutboxEvent` |

## 14. Transaction, chống thao tác đồng thời và nguồn sự thật

### 14.1 Transaction quan trọng

Các luồng sau commit/rollback nguyên khối:

- Chấm công + mở két.
- Đóng két + tan ca.
- Mở/đóng/gộp phiên bàn.
- Tạo order + item + modifier + outbox.
- Nhận/giao/hủy món + audit + outbox.
- Dựng/tách/gộp/recompute bill.
- Thanh toán + voucher redemption + đóng bàn + outbox.

### 14.2 Chống double action

- Thanh toán: `WHERE b.Status='UNPAID'`.
- Kết ca: `UPDLOCK,HOLDLOCK` trên CashierShift và tập bill tiền mặt.
- Nhận món: `WHERE oi.Status='READY'`.
- Giao món: `WHERE oi.Status IN ('PICKED_UP')`.
- Complete order: `NOT EXISTS` item chưa final.
- Attendance: `UPDLOCK,HOLDLOCK` và unique theo ShiftAssignment.
- BillItem: unique `OrderItemId`.

### 14.3 Nguồn sự thật

| Dữ liệu | Nguồn sự thật |
|---|---|
| User/role/branch | HTTP session sau login + `iam.User` |
| Có được thao tác Cashier không | `hr.Attendance` + `payment.CashierShift` |
| Giá bán | `catalog.BranchMenu.LocalPrice`, fallback `catalog.Product.BasePrice` |
| Tình trạng món bán được | `BranchMenu.IsAvailable`, `Is86`, tồn `BranchInventory` |
| Tiến độ pha/giao | `sales.OrderItem.Status` |
| Đơn đã hoàn tất chưa | `sales.Orders.Status` |
| Một món thuộc bill nào | `payment.BillItem` |
| Đã thu tiền chưa | `payment.Bill.Status` |
| Doanh thu | `payment.Bill` có `Status='PAID'` |
| Tiền mặt kết ca | `OpeningCash + SUM(PAID CASH Bill.TotalAmount)` |
| Giỏ tạm chưa gửi | HTTP session, không phải database |

## 15. Các điểm cần lưu ý trong code hiện tại

Đây là các quan sát khi lần theo source; tài liệu không tự sửa các điểm này.

### 15.1 Một số action còn thiếu kiểm tra ownership ở tầng service

Các luồng đọc/ghi chính đã có branch scope tốt, nhưng một số method nhận ID trực
tiếp mà chưa xác minh đầy đủ đối tượng thuộc branch hiện tại:

- `OrderInboxServlet -> OrderService.voidOrder(orderId, userId)` không truyền
  `branchId`, trong khi `voidOrder` không so `Order.BranchId` với session branch.
- `BillHistoryServlet` xem detail theo `billId` nhưng không kiểm tra
  `bill.BranchId` trước khi render.
- `BillHistoryServlet -> BillingService.voidBill` không truyền/kiểm tra branch.
- `BillingService.removeVoucher(billId)` không nhận branch.
- `splitItems` đọc `billItemId` rồi reassign nhưng chưa xác minh BillItem thuộc
  đúng session truyền vào.
- `mergeBills` nhận danh sách bill ID nhưng service chưa xác minh tất cả cùng
  session/branch và đều `UNPAID`.

Vì ID là số tăng dần có thể đoán, đây là nhóm cần ưu tiên harden nếu tiếp tục
phát triển.

### 15.2 Nút thanh toán takeaway ở Inbox rộng hơn điều kiện service

`inbox.jsp` hiện link Checkout khi:

```text
OrderType == TAKEAWAY và paymentStatus != PAID
```

Nhưng `buildTakeawayBill` chỉ cho vào khi `Order.Status == COMPLETED`. Vì vậy
Cashier có thể nhìn thấy link sớm; bấm vào sẽ bị redirect về Inbox với thông báo
chưa hoàn tất bàn giao. Màn tổng Checkout hiển thị đúng hơn: chỉ order
`COMPLETED` mới có nút Thanh toán.

### 15.3 Dashboard dùng ngày UTC

`sumPaidToday` và `countPaidToday` so `CAST(PaidAt AS DATE)` với
`SYSUTCDATETIME()`. Trong khi order stale và ca làm việc dùng timezone Việt Nam
và giờ mở cửa chi nhánh. Gần nửa đêm có thể có chênh lệch khái niệm “hôm nay”.

### 15.4 Filter GET cho phép xem khi chưa vào ca

Đây là chủ ý hiện tại:

- Cashier chưa vào ca vẫn xem được các màn.
- Chỉ POST bị chặn.

Nếu dữ liệu màn hình cũng cần ẩn khi off-duty, phải thay đổi policy filter; đây
không phải bug của servlet riêng lẻ.

### 15.5 Draft không bền vững

Draft POS chỉ ở HTTP session. Nó không hỗ trợ:

- Khôi phục sau logout/session timeout.
- Mở tiếp trên thiết bị khác.
- Audit draft.

Đơn đã bấm **Gửi đơn** thì bền vững trong database; draft chưa gửi thì không.

### 15.6 `REFUND` còn trong schema và view, không còn thao tác

Role Cashier hiện không có thao tác hoàn tiền, nhưng:

- Constraint `payment.Bill.Status` vẫn chứa `REFUND`.
- JSP lịch sử vẫn render nhãn “Đã hoàn”.
- Logic suy trạng thái Inbox vẫn coi `REFUND` là lỗi nếu chưa có bill PAID.

Đây là tương thích dữ liệu cũ, không phải một chức năng hoàn tiền đang hoạt động.

## 16. Source index

### Controller

- `src/main/java/com/cafe/controller/cashier/CashierDashboardServlet.java`
- `src/main/java/com/cafe/controller/cashier/CashierShiftServlet.java`
- `src/main/java/com/cafe/controller/cashier/TableServlet.java`
- `src/main/java/com/cafe/controller/cashier/TableQrServlet.java`
- `src/main/java/com/cafe/controller/cashier/PosServlet.java`
- `src/main/java/com/cafe/controller/cashier/OrderInboxServlet.java`
- `src/main/java/com/cafe/controller/cashier/PickupServlet.java`
- `src/main/java/com/cafe/controller/cashier/CheckoutServlet.java`
- `src/main/java/com/cafe/controller/cashier/BillHistoryServlet.java`

### Service

- `src/main/java/com/cafe/service/cashier/CashierDutyService.java`
- `src/main/java/com/cafe/service/cashier/CashierShiftService.java`
- `src/main/java/com/cafe/service/cashier/CashierCashReconciliation.java`
- `src/main/java/com/cafe/service/cashier/CashierOrderValidator.java`
- `src/main/java/com/cafe/service/cashier/TableSessionService.java`
- `src/main/java/com/cafe/service/cashier/PickupService.java`
- `src/main/java/com/cafe/service/cashier/BillingService.java`
- `src/main/java/com/cafe/service/shared/OrderService.java`
- `src/main/java/com/cafe/service/shared/CatalogReadService.java`
- `src/main/java/com/cafe/service/shared/VoucherService.java`
- `src/main/java/com/cafe/service/manager/AttendanceService.java`

### DAO chứa SQL chính

- `src/main/java/com/cafe/dao/cashier/CashierShiftDao.java`
- `src/main/java/com/cafe/dao/cashier/DiningTableDao.java`
- `src/main/java/com/cafe/dao/cashier/TableSessionDao.java`
- `src/main/java/com/cafe/dao/cashier/BillDao.java`
- `src/main/java/com/cafe/dao/cashier/BillItemDao.java`
- `src/main/java/com/cafe/dao/cashier/VoucherRedemptionDao.java`
- `src/main/java/com/cafe/dao/shared/OrderDao.java`
- `src/main/java/com/cafe/dao/shared/OrderItemDao.java`
- `src/main/java/com/cafe/dao/shared/OrderItemModifierDao.java`
- `src/main/java/com/cafe/dao/shared/BranchMenuDao.java`
- `src/main/java/com/cafe/dao/shared/ProductRecipeDao.java`
- `src/main/java/com/cafe/dao/shared/ModifierGroupDao.java`
- `src/main/java/com/cafe/dao/shared/ModifierOptionDao.java`
- `src/main/java/com/cafe/dao/shared/ProductModifierGroupDao.java`
- `src/main/java/com/cafe/dao/shared/VoucherDao.java`
- `src/main/java/com/cafe/dao/shared/OutboxEventDao.java`
- `src/main/java/com/cafe/dao/shared/OrderItemActionDao.java`
- `src/main/java/com/cafe/dao/manager/AttendanceDao.java`

### View

- `src/main/webapp/WEB-INF/views/cashier/dashboard.jsp`
- `src/main/webapp/WEB-INF/views/cashier/shift.jsp`
- `src/main/webapp/WEB-INF/views/cashier/table-map.jsp`
- `src/main/webapp/WEB-INF/views/cashier/table-qr.jsp`
- `src/main/webapp/WEB-INF/views/cashier/pos.jsp`
- `src/main/webapp/WEB-INF/views/cashier/inbox.jsp`
- `src/main/webapp/WEB-INF/views/cashier/handoff/cards.jsp`
- `src/main/webapp/WEB-INF/views/cashier/checkout.jsp`
- `src/main/webapp/WEB-INF/views/cashier/bill-history.jsp`
- `src/main/webapp/WEB-INF/views/cashier/bill-view.jsp`

### Schema

- `sql/database.sql`
