# KE_HOACH_QR_KHACH.md — Hoàn thiện luồng QR đặt món tại bàn (role Cashier + màn khách)

> Trạng thái nền: luồng QR đã chạy được end-to-end sau commit "thu ngân mở bàn mới cho đặt món".
> Tài liệu này liệt kê **5 việc còn thiếu**, xếp theo mức chặn nghiệp vụ. Làm từ T1 xuống.
> Nhánh làm việc: `minhnhat`. Chỉ **thêm** commit, không reset/amend/force.

## Nguyên tắc bất biến (theo chuẩn repo)

- Kiến trúc `controller → service → dao`. JSP **chỉ JSTL/EL**, cấm scriptlet.
- `branchId` lấy từ session (`InventoryDashboardServlet.branchId(req)`), không nhận từ client.
- Tồn kho chỉ đổi qua `InventoryService.applyTxn`/ledger.
- Ghi event chỉ qua `EventPublisher.publish(...)` trong **cùng transaction** nghiệp vụ.
- **Không đổi schema**. Cả 5 việc dưới đây đều không cần migration.
- Ưu tiên additive: thêm servlet/JSP/method mới, hạn chế sửa hành vi role khác.
- Commit message conventional tiếng Việt: `feat(cashier): …`, `fix(cashier): …`, `test(customer): …`. Không ghi nguồn AI.
- Trước mỗi commit: `mvn -o test` PASS.

## Bối cảnh kỹ thuật đã xác minh (đọc trước khi code)

| Điều | Giá trị | Nguồn |
|---|---|---|
| Duty guard cho qua mọi GET | trang chỉ-GET dưới `/cashier/*` xem được **kể cả chưa mở ca** | `CashierDutyGuardFilter.isAllowed()` dòng 68 |
| RBAC theo prefix | `/cashier/*` = CASHIER + ADMIN. **Manager không vào được** | `RbacFilter.requiredRole()` dòng 41-44 |
| `/qr/*` là public | không cần đăng nhập | `AuthFilter.isPublic()` dòng 41 |
| Thư viện vẽ QR | `assets/js/qrcode.min.js` đã có sẵn, đã chạy thật | `cashier/checkout.jsp:181-182` (VietQR thanh toán) |
| Dữ liệu QR bàn | `findFloorMap` **đã** SELECT + `setQrCode` | `DiningTableDao:17,34` |
| Tạo bàn | **không có UI**, bàn chỉ sinh từ SQL seed. 12/12 bàn đã có `QrCode` | grep + DB |
| `DiningTable.QrCode` | `VARCHAR(80) NULL UNIQUE` → JSP vẫn phải xử lý case NULL | `sql/database.sql:673` |
| CSRF | form POST phải có `_csrf`; token ở `sessionScope.csrfToken`, **không** xoay vòng | `CsrfUtil` |

---

## T1 — Trang in mã QR bàn ⭐ CHẶN TOÀN BỘ LUỒNG

**Vì sao ưu tiên số 1:** cột `DiningTable.QrCode` hiện **không được JSP nào hiển thị**. Nhân viên
không có cách nào lấy mã để in dán lên bàn → khách ngồi xuống không có gì để quét → cả luồng QR
không dùng được ngoài đời thật. Node "Scan Table QR" trong sơ đồ đang thiếu điểm khởi đầu.

### File đụng vào

| Loại | Đường dẫn |
|---|---|
| MỚI | `src/main/java/com/cafe/common/QrLink.java` |
| MỚI | `src/main/java/com/cafe/controller/cashier/TableQrServlet.java` |
| MỚI | `src/main/webapp/WEB-INF/views/cashier/table-qr.jsp` |
| MỚI | `src/test/java/com/cafe/common/QrLinkTest.java` |
| SỬA | `src/main/webapp/WEB-INF/views/cashier/table-map.jsp` (thêm nút dẫn sang) |

### Bước 1 — `QrLink` (logic thuần, để test được không cần servlet)

```java
package com.cafe.common;

/** Dựng URL tuyệt đối nhúng vào mã QR dán tại bàn. Tách khỏi servlet để test thuần. */
public final class QrLink {
    private QrLink() { }

    /** scheme://host[:port]+contextPath — bỏ port khi là cổng chuẩn của scheme. */
    public static String absoluteBase(String scheme, String serverName, int port, String contextPath) {
        boolean standard = ("http".equals(scheme) && port == 80)
                        || ("https".equals(scheme) && port == 443);
        return scheme + "://" + serverName + (standard ? "" : ":" + port)
             + (contextPath == null ? "" : contextPath);
    }

    /** URL khách quét ra: <base>/qr/menu?t=<qrCode>. */
    public static String menuUrl(String base, String qrCode) { ... }
}
```

`menuUrl` nhớ URL-encode `qrCode` (`URLEncoder.encode(qrCode, StandardCharsets.UTF_8)`).

### Bước 2 — `TableQrServlet` (chỉ `doGet`)

- `@WebServlet("/cashier/table-qr")`
- Lấy `branchId` từ `InventoryDashboardServlet.branchId(req)`.
- Dùng lại `TableSessionService.getFloorMap(branchId)` — **không viết DAO mới**, dữ liệu `qrCode` đã có sẵn.
- Set attribute: `tables`, `baseUrl` (từ `QrLink.absoluteBase(req.getScheme(), req.getServerName(), req.getServerPort(), req.getContextPath())`).
- Forward `/WEB-INF/views/cashier/table-qr.jsp`.
- **Không** viết `doPost` — giữ chỉ-GET để lọt qua duty guard.

### Bước 3 — `table-qr.jsp`

JSP **đứng riêng** (tự khai `<html>`, không include `header.jsp`) để trang in sạch, giống các
màn `customer/*.jsp`. Có link `← Về sơ đồ bàn` về `${ctx}/cashier/table`.

- Nhúng `<script src="${ctx}/assets/js/qrcode.min.js"></script>`.
- Ô input cho phép **sửa base URL** (xem cảnh báo bên dưới), mặc định `${baseUrl}`, `oninput` vẽ lại.
- Lưới thẻ, mỗi bàn: số bàn + khung QR + chuỗi `qrCode` in nhỏ bên dưới (để đối chiếu khi dán nhầm).
- `<c:if test="${empty t.qrCode}">` → hiện "Bàn chưa có mã QR" thay vì vẽ QR rỗng.
- JS vẽ: duyệt `[data-qr-code]`, dựng URL từ ô input, rồi
  `new QRCode(box, {text:url, width:180, height:180, correctLevel: QRCode.CorrectLevel.M})`
  (nhớ `box.innerHTML=''` trước khi vẽ lại).
- Nút `In` → `window.print()`, class `no-print`.
- CSS in:
  ```css
  @media print { .no-print{display:none!important} .qr-grid{grid-template-columns:repeat(2,1fr)} }
  ```

### ⚠ Cảnh báo quan trọng — base URL

`req.getServerName()` trên máy dev trả về **`localhost`**. In QR chứa `localhost` thì điện thoại
khách quét sẽ **không vào được** (localhost của điện thoại ≠ máy chủ). Vì vậy trang **bắt buộc**
có ô sửa base URL để nhập IP LAN (vd `http://192.168.1.12:8080/cafe-shop`) hoặc domain thật.
Đây là lý do ô input ở Bước 3 không phải trang trí.

### Tiêu chí nghiệm thu

1. `curl -b cash.txt -o q.html 'http://localhost:8080/cafe-shop/cashier/table-qr'` → **HTTP 200**.
2. `grep -c 'data-qr-code' q.html` → bằng số bàn của chi nhánh (CN01 = 4).
3. **Chưa mở ca** vẫn xem được trang (đăng nhập cashier mới, không `startDuty`) → 200.
4. Đăng nhập bằng `manager1` vào `/cashier/table-qr` → **403** (đúng theo RBAC).
5. Mở bằng trình duyệt: sửa base URL sang IP LAN → QR vẽ lại → **quét bằng điện thoại thật**
   ra đúng màn "Bàn chưa được mở" (nếu bàn chưa mở) hoặc menu.
6. `Ctrl+P` → bản in chỉ còn lưới QR, không có nút/link.

### Test

`QrLinkTest` (thuần, không DB):
- `absoluteBase("http","shop.vn",80,"/cafe-shop")` → `http://shop.vn/cafe-shop`
- `absoluteBase("https","shop.vn",443,"")` → `https://shop.vn`
- `absoluteBase("http","localhost",8080,"/cafe-shop")` → `http://localhost:8080/cafe-shop`
- `menuUrl(base,"QR-CN01-T01")` → `.../qr/menu?t=QR-CN01-T01`
- `menuUrl` với mã có ký tự cần encode → phải được encode

**Commit:** `feat(cashier): trang in mã QR bàn cho khách quét`

---

## T2 — Cho khách gọi thêm món

**Vấn đề:** `grep -c 'qr/menu' track.jsp` = **0**. Khách đặt xong đợt 1 bị kẹt ở màn theo dõi,
muốn gọi thêm phải ra quét lại sticker trên bàn. Dine-in gọi nhiều đợt là chuyện thường.

### File đụng vào
- SỬA `controller/customer/QrMenuServlet.java` (doGet)
- SỬA `views/customer/track.jsp`
- SỬA `views/customer/menu.jsp`

### Bước
1. `QrMenuServlet.doGet`: khi tham số `t` rỗng → **fallback** sang `qrSessionId` trong HTTP session.
   Nếu phiên đó còn `OPEN` → render menu như thường; ngược lại → `invalid.jsp`.
   *(Đồng thời sửa luôn một lỗi nhỏ đang có: nhánh `sendRedirect(ctx + "/qr/menu")` ở
   `doPost` khi thiếu `qrPendingTableId` hiện đang rơi vào 404 vì không có `t`.)*
2. `track.jsp`: thêm nút **"Gọi thêm món"** → `${ctx}/qr/menu` (không cần truyền `qrCode`).
3. `menu.jsp`: thêm link **"Xem đơn đã gọi"** → `${ctx}/qr/track?s=${sessionId}` để đi lại hai chiều.

### Tiêu chí nghiệm thu
- Đặt đơn 1 → track → "Gọi thêm món" → menu → đặt đơn 2.
- DB: `SELECT OrderId, TableSessionId FROM sales.Orders WHERE TableSessionId=<sid>` → **2 đơn cùng một phiên**.
- Bàn đã bị thu ngân đóng → bấm "Gọi thêm món" → ra `invalid.jsp`, **không** tạo phiên mới.

**Commit:** `feat(customer): cho khách gọi thêm món từ màn theo dõi`

---

## T3 — Chặn đóng bàn khi còn món chưa xử lý (lỗi có sẵn)

**Vấn đề đã kiểm chứng bằng dữ liệu thật:**
```
đơn 867: status=ACTIVE | phiên 799 = CLOSED
món trong đơn: WAITING x2
```
Nút "Đóng bàn" gọi thẳng `TableSessionService.closeSession()` — đóng phiên bất kể còn món.
Phiên `CLOSED`, bàn về `EMPTY`, nhưng **barista vẫn thấy và vẫn pha 2 ly đó**.
Repo **đã có sẵn** `closeSessionIfNoActiveItems()` làm đúng việc này nhưng không ai gọi.

### File đụng vào
- SỬA `controller/cashier/TableServlet.java` nhánh `"closeTable"`

### Bước
1. Đổi sang gọi `closeSessionIfNoActiveItems(sessionId)`.
2. Trả `false` → `flashError`: *"Bàn còn món chưa phục vụ — huỷ món ở Đơn đến hoặc thu tiền trước khi đóng bàn."* kèm link `/cashier/inbox`.
3. **Không** tự động huỷ món ngầm — sẽ mất dấu vết ai huỷ, vi phạm nguyên tắc audit của repo.

> Lưu ý nghiệp vụ: phiên mà mọi món đã `SERVED` nhưng chưa thu tiền cũng sẽ **không** đóng được
> bằng nút này (đúng — phải đi qua Thanh toán). Đường đóng bàn hợp lệ sau thanh toán đã có sẵn ở
> `BillingService.payBill()` dòng 179-187.

### Tiêu chí nghiệm thu
- Mở bàn → đặt món → bấm "Đóng bàn" → hiện cảnh báo; DB: phiên vẫn `OPEN`, bàn vẫn `OCCUPIED`.
- Huỷ hết món ở `/cashier/inbox` → bấm lại "Đóng bàn" → đóng được, bàn về `EMPTY`.

**Commit:** `fix(cashier): chặn đóng bàn khi còn món chưa xử lý`

---

## T4 — Quầy nhìn thấy "Gọi nhân viên" / "Xin thanh toán"

**Vấn đề:** hai nút này của khách chỉ ghi `service.call` / `bill.requested` vào `ops.OutboxEvent`
rồi thôi — **không màn hình nào của thu ngân đọc**. Mũi tên `Request Payment → Prepare Bill`
trong sơ đồ hiện chưa nối thật.

### File đụng vào
- SỬA `dao/shared/OutboxEventDao.java` (thêm 2 method)
- SỬA `service/cashier/TableSessionService.java`
- SỬA `controller/cashier/TableServlet.java`
- SỬA `views/cashier/table-map.jsp`

### Bước
1. `OutboxEventDao.findPendingSignals(conn, branchId)`:
   `AggregateId` của 2 event này là **sessionId**, nên JOIN
   `ops.OutboxEvent → sales.TableSession → sales.DiningTable` để ra `DiningTableId`.
   Trả `Map<Integer /*tableId*/, String /*loại tín hiệu*/>` (hoặc model nhỏ nếu cần cả thời điểm).
   Lọc `EventType IN ('service.call','bill.requested') AND ProcessedAt IS NULL`.
   Dùng lại `parseTableId` có sẵn — nhớ đây là **sessionId**, đừng nhầm với `table.open_requested`
   (loại đó `AggregateId` là **tableId**).
2. `markSignalsProcessed(conn, sessionId)`.
3. Sơ đồ bàn: ô bàn đang có tín hiệu → badge **"Gọi NV"** / **"Xin thanh toán"** + nút
   **"Đã tiếp nhận"** (POST `action=ackSignal`, có `_csrf`).
4. `bill.requested` nên tự `Processed` khi phiên đó thanh toán xong trong `payBill` (cùng tx).

### Tiêu chí nghiệm thu
- Khách bấm "Xin thanh toán" → `/cashier/table` hiện badge ở đúng bàn.
- Bấm "Đã tiếp nhận" → badge biến mất; DB `ProcessedAt` khác NULL.
- Thanh toán xong → tín hiệu `bill.requested` của phiên đó tự tắt.

**Commit:** `feat(cashier): hiện yêu cầu gọi nhân viên / xin thanh toán từ khách`

---

## T5 — Tự cập nhật trạng thái + màn cảm ơn sau thanh toán

**Vấn đề A:** `QrTrackServlet` dòng 29-44 **đã có sẵn** endpoint trả JSON, comment ghi rõ
*"AJAX polling"*, nhưng `track.jsp` có **0 thẻ `<script>`** → không ai gọi. Khách phải bấm
"Làm mới" thủ công.

**Vấn đề B:** sau khi thu ngân thu tiền xong, khách vẫn thấy "Đơn của bạn", badge "Chờ pha",
và vẫn còn nút "Xin thanh toán" — không có gì báo đã xong. Node "Payment Completed" trong sơ đồ
chưa có màn tương ứng. *(Đã kiểm chứng trên phiên 799 sau khi đóng bàn.)*

### File đụng vào
- SỬA `views/customer/track.jsp`
- SỬA `controller/customer/QrTrackServlet.java` (doGet)

### Bước
1. `track.jsp`: thêm `<script>` gọi `/qr/track?action=status&s=${sessionId}` mỗi **10s**, render lại
   danh sách món. Endpoint có sẵn, **không phải sửa** phần JSON.
   Dùng đúng bộ nhãn tiếng Việt đang có (Chờ pha / Đang pha / Đã pha xong / …), đừng in mã enum thô.
2. `QrTrackServlet.doGet`: nếu `session.getStatus()` khác `OPEN` → set attribute `sessionClosed=true`.
3. `track.jsp` khi `sessionClosed`: hiện **"Cảm ơn quý khách — đã thanh toán xong"**, ẩn 3 nút
   (Huỷ đơn / Gọi NV / Xin thanh toán) và **dừng polling**.

### Tiêu chí nghiệm thu
- Barista chuyển món sang READY → màn khách tự đổi badge trong ≤10s, **không reload tay**.
- Thu ngân thu tiền xong → màn khách hiện lời cảm ơn, 3 nút biến mất, network ngừng gọi polling.

**Commit:** `feat(customer): tự cập nhật trạng thái món và màn cảm ơn sau thanh toán`

---

## Phụ lục A — Quy trình build & deploy (máy dev này)

```bash
mvn -o -q package -DskipTests
T=/opt/homebrew/Cellar/tomcat@10/10.1.54/libexec
mv "$T/webapps/cafe-shop.war" "$T/webapps/cafe-shop.war.bak-$(date +%Y%m%d-%H%M%S)"
rm -rf "$T/webapps/cafe-shop" "$T/work/Catalina/localhost/cafe-shop"
cp target/cafe-shop.war "$T/webapps/cafe-shop.war"
brew services restart tomcat@10
curl -s http://localhost:8080/cafe-shop/health     # phải ra "OK - DB pool connected"
```

`db.properties` trong git trỏ `localhost:1433` nhưng máy này chạy cổng **14333** — override đã đặt
sẵn trong `setenv.sh` của Tomcat. **Đừng sửa `db.properties` để chữa cháy.**

## Phụ lục B — Truy vấn DB

```bash
sqlcmd -S tcp:localhost,14333 -U sa -P 'YourPassword123' -N disable -d CafeChain -h-1 -W -Q "..."
```
Bắt buộc `-N disable`: bản `sqlcmd` mới bắt tay TLS lỗi với cert của azure-sql-edge
(`x509: negative serial number`).

## Phụ lục C — Test bằng curl (khách QR không cần đăng nhập)

```bash
# khách quét QR
curl -s -c c1.txt 'http://localhost:8080/cafe-shop/qr/menu?t=QR-CN01-T01' -o p.html
CSRF=$(grep -oE "CSRF='[^']*'" p.html | sed "s/CSRF='//;s/'//")   # menu.jsp
CSRF=$(grep -oE 'name="_csrf" value="[^"]*"' p.html | head -1 | sed 's/.*value="//;s/"//')  # form JSP

# thu ngân
curl -s -c cash.txt .../auth/login -o login.html          # lấy _csrf rồi POST cashier1/123456
```

**Bẫy đã gặp:** đừng dùng chung `-L` với `-c` cùng lúc khi POST — cookie jar bị ghi đè giữa chừng
làm phiên lệch, request trả 403 gây hiểu nhầm là lỗi CSRF.

Tài khoản seed: `cashier1` / `123456` · mã QR bàn CN01: `QR-CN01-T01..T04`.

## Phụ lục D — Trạng thái test còn sót

Đơn `#867` (2 món `WAITING`) đang treo ở phiên `#799` đã đóng — sinh ra lúc kiểm chứng T3.
Sau khi làm xong T3 nhớ dọn:
```sql
UPDATE sales.OrderItem SET Status='CANCELLED' WHERE OrderId=867;
UPDATE sales.Orders    SET Status='VOID'      WHERE OrderId=867;
```

---

## Checklist bàn giao

- [ ] T1 — Trang in mã QR bàn
- [ ] T2 — Gọi thêm món
- [ ] T3 — Chặn đóng bàn còn món
- [ ] T4 — Quầy đọc tín hiệu khách
- [ ] T5 — Auto refresh + màn cảm ơn

Mỗi mục xong: `mvn -o test` PASS → commit → ghi 1 dòng vào `docs/PROGRESS.md`.
Khi xong (hoặc xong từng mục), nhắn để review: sẽ soi **đúng nghiệp vụ** (phân quyền, transaction,
guard trạng thái), **bảo mật** (CSRF, IDOR, quyền sở hữu phiên), và **rò rỉ luồng** (ngõ cụt UX).
