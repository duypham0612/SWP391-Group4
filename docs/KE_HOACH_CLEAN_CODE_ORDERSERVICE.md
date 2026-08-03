# KẾ HOẠCH DỌN CODE — OrderService & tầng shared

> **Mục đích file này:** giữ agent/người làm bám đúng một phạm vi đã chốt, không lan sang việc khác.
> Mỗi phiên làm việc mới → **đọc §0 Nguyên tắc và §2 Ngoài phạm vi trước tiên.**
>
> Nối tiếp [`KE_HOACH_CLEAN_CODE_BARISTA.md`](KE_HOACH_CLEAN_CODE_BARISTA.md) — mục đầu tiên trong
> sổ ghi nhận §6 của file đó (`OrderService.java:50`).
> Trạng thái tổng: 🟢 **Đợt 1 + 2 + 2b xong** (2026-08-02) — tức toàn bộ phần khuyến nghị ở §8.
> ⚪ Đợt 3 (bỏ facade) chưa làm, **có điều kiện tiên quyết** — xem §5.

---

## ⚠️ ĐỌC TRƯỚC: plan này KHÁC plan Barista ở một điểm sống còn

Plan Barista có **lưới an toàn 345 unit test** — sai là `mvn test` đỏ ngay. Ở đây **không có**:

| Service đích | Unit test |
|---|---|
| `OrderPlacementService` · `OrderQueryService` · `KdsOrderWorkflowService` · `OrderIssueService` · `OrderHandoffService` · `OrderRepository` | **0** |

Các service này chỉ có **integration test** — chạy bằng **failsafe trong profile `integration`**,
cần **Testcontainers + Docker**. Đã kiểm: **Docker không chạy trên máy này**, nên `mvn verify`
cũng không cứu được *tại chỗ*.

> 📌 **Đính chính (2026-08-03).** Bản đầu của mục này viết "toàn bộ luồng đơn hàng không có test
> nào — rủi ro nền lớn nhất repo". **Nói quá.** Khảo sát lại: repo có **8 file IT / 1796 dòng**, và
> `.github/workflows/database-integration.yml` chạy `mvn verify -Pintegration` trên **mọi PR và push
> vào main**. `BaristaTransactionIT` phủ đúng ba đường nguy hiểm nhất (nhận trùng · **trừ kho hai
> lần** · đặt chỗ tồn khi làm lại). Kết luận "0 test" sai vì chỉ grep tên service trong `src/test`
> mà không xem IT phủ gì và CI có chạy không.
>
> Khoảng trống **thật** lúc đó: 3/14 method của hai service có test. Đã bổ sung
> `BaristaIssueWorkflowIT` (16 test) → **12/14** (`8fae5db`). Còn `cancelItem`, `voidOrder` —
> thao tác phía Thu ngân, nên nằm ở IT của cashier.

**Hệ quả bắt buộc cho mọi việc trong file này:**

> Chỉ làm những thay đổi mà **trình biên dịch chứng minh được là đúng**.
> Không làm thay đổi cần test mới biết đúng/sai.

Đó là lý do §5 (bỏ hẳn facade) bị xếp cuối và **có điều kiện tiên quyết**, chứ không phải vì nó khó.

---

## 0. Nguyên tắc bất di bất dịch

| # | Nguyên tắc |
|---|---|
| N1 | **KHÔNG đổi hành vi.** Refactor thuần. |
| N2 | **Chỉ thay đổi compiler kiểm được.** Xem cảnh báo phía trên. Đổi thứ tự tham số, gộp nhánh điều kiện, đổi kiểu trả về… đều KHÔNG thuộc nhóm này nếu compiler không bắt được. |
| N3 | **KHÔNG sửa bug gặp dọc đường** — ghi vào §6. |
| N4 | **KHÔNG động file ngoài §1.** |
| N5 | **Mỗi đợt = 1 commit.** Không trailer `Co-Authored-By`. |
| N6 | **`mvn test` + `mvn clean package` phải xanh sau mỗi đợt.** Biết rõ nó không phủ được OrderService, nhưng vẫn phải xanh để chắc không vỡ chỗ khác. |
| N7 | **Không thêm dependency**, không đổi `pom.xml`. |
| N8 | Comment tiếng Việt, theo quy ước repo. |

### Ràng buộc ArchUnit (`MvcArchitectureTest`)
- `service/` không được phụ thuộc `controller/`, `filter/`, `listener/`, `web/`.
- `service.*` **phải không có chu trình** (`service_packages_must_be_acyclic`) — quan trọng ở §5:
  nếu `service/barista` gọi thẳng `service/shared` thì vẫn một chiều, không tạo chu trình. An toàn.
- `controller/`, `web/` không được chạm `dao/`, `java.sql..`, `DBConnection`.

---

## 1. Phạm vi

| File | Dòng | Vai trò |
|---|---|---|
| `service/shared/OrderService.java` | **56** | Facade — trọng tâm |
| `model/WasteEvent.java` | 60 | POJO nhồi getter/setter |
| `model/WasteEventReview.java` | 55 | POJO nhồi getter/setter |

**Chỉ ở Đợt 3** (khi bỏ facade) mới được đụng 7 file gọi, liệt kê sẵn ở §5.

---

## 2. NGOÀI PHẠM VI

- ❌ 5 service đích (`OrderPlacementService`, `OrderQueryService`, `KdsOrderWorkflowService`,
  `OrderIssueService`, `OrderHandoffService`) và `OrderRepository` — **logic thật nằm ở đây**,
  đụng vào là rời khỏi vùng compiler bảo vệ.
- ❌ `InventoryService` và các service tồn kho — **cùng kiến trúc nhưng đã viết đúng chuẩn**,
  chính nó là khuôn mẫu (§3.1). Không sửa.
- ❌ `dao/**`, `sql/**`, `pom.xml`, JSP/CSS/JS.
- ❌ Viết test mới cho logic chưa có test (trừ khi làm §5 — xem điều kiện tiên quyết).
- ❌ Các file barista đã dọn xong ở plan trước.

---

## 3. Hiện trạng — số đo, không phải cảm tính

### 3.1 `OrderService` là facade uỷ thác thuần, kiến trúc ĐÃ ĐÚNG

56 dòng, không chứa logic nghiệp vụ nào. Nó chỉ chuyển tiếp sang 5 service chuyên trách. **Vấn đề
hoàn toàn là trình bày**, không phải thiết kế:

| Dòng | Độ dài | Nhồi bao nhiêu method |
|---|---|---|
| 50 | 760 ký tự | 7 |
| 51 | 875 ký tự | 7 |
| 52 | **1096 ký tự** | 8 |
| 53 | 239 ký tự | 2 |
| 54 | 680 ký tự | 6 |
| 55 | 374 ký tự | 4 |

**37 method public dồn vào 6 dòng.** Tham số bị rút còn một ký tự: `int i, Integer u, int b`.

**Đây là ca cá biệt, không phải bệnh chung của repo** — đã quét toàn bộ `src/main/java`:
chỉ `OrderService` (7 dòng nhồi) và 2 file model (13 dòng mỗi file) mắc lỗi này.

### 3.2 Đã có sẵn khuôn mẫu đúng trong repo

`InventoryService` là **y hệt kiến trúc** — facade 149 dòng uỷ thác sang 5 service tồn kho — nhưng
viết chuẩn:

```java
public PrepBatch createSuggestedPrepBatch(int branchId, int ingredientId, BigDecimal quantity,
                                          int userId, String requestId) throws SQLException {
    return prep.createSuggestedPrepBatch(branchId, ingredientId, quantity, userId, requestId);
}
```

→ Không phải nghĩ ra style mới. **Chép đúng style của `InventoryService`.**

### 3.3 Tên tham số đúng KHÔNG phải đoán

Chữ ký thật nằm sẵn ở service đích, chỉ việc chép sang:

| Facade viết | Service đích viết | Ghi chú |
|---|---|---|
| `int i` | `int orderItemId` | |
| `Integer u` | `Integer userId` / `Integer actorUserId` | `reclaimItem` dùng `actorUserId` |
| `int b` | `int sessionBranchId` **hoặc** `int branchId` | ⚠️ **KHÁC NHAU** — xem dưới |
| `int o` | `int orderId` | |
| `String r` | `String reason` | |
| `String n` | `String actorName` | |
| `Set<Integer> duty` | `Set<Integer> onDutyUserIds` | |
| `List<Integer> ids` | `List<Integer> ingredientIds` | |
| `LocalDateTime d` | `LocalDateTime businessDayStartUtc` | |

⚠️ **Chi tiết dễ mất:** `KdsOrderWorkflowService.startItem` nhận `sessionBranchId` (chi nhánh của
phiên đăng nhập, dùng để chặn thao tác chéo chi nhánh), còn `countMyMakingItems` nhận `branchId`
thường. Facade gọi cả hai là `b` nên xoá mất phân biệt này. **Chép đúng tên từ service đích, không
đặt lại theo ý mình.**

### 3.4 Có 5 method chết trong facade

Đã grep toàn `src/main` + `src/test` + `webapp`, 0 caller:

| Method | Ghi chú |
|---|---|
| `getKdsQueue` | `KdsService` cũng từng có bản chết, đã xoá ở plan Barista |
| `getOrder` | |
| `getStaleItems` | Khu "Đơn treo" đã gỡ khỏi UI |
| `serveAllReady` | ⚠️ Đừng nhầm với `serveAllPickedUp` và `pickUpAllReady` — hai cái này **đang dùng** |
| `getBaristaWorkbench(int)` | Chỉ overload **1 tham số** chết; bản 2 tham số đang dùng |

### 3.5 Hai kiểu kết quả bị nhân đôi

`OrderService.UnblockResult` và `OrderService.BulkReadyResult` là **bản sao** của
`OrderIssueService.UnblockResult` / `KdsOrderWorkflowService.BulkReadyResult`, kèm code chuyển đổi:

```java
OrderIssueService.UnblockResult x = issues.unblockItem(i, rs, u, b);
return new UnblockResult(x.isSuccess(), x.getRemainingBlockedWithRecountedIngredients());
```

Cùng 2 field, cùng tên getter. Thêm field mới phải sửa 2 nơi + hàm chuyển đổi.

### 3.6 Ai đang gọi facade

| File gọi | Số method | Thực chất cần service nào |
|---|---|---|
| `service/barista/KdsService.java` | 14 | kds(6), issues(5), query(3) |
| `controller/barista/KdsServlet.java` | 9 | issues(4), kds(3), query(2) |
| `service/cashier/PickupService.java` | 8 | handoff(5), query(3) |
| `controller/cashier/OrderInboxServlet.java` | 5 | issues(2), handoff(2), query(1) |
| `service/customer/QrOrderService.java` | 4 | query(2), placement(1), issues(1) |
| `controller/cashier/PosServlet.java` | 2 | placement(1), query(1) |
| `web/support/BaristaShiftSupport.java` | 1 | kds(1) |

Không file nào dùng cả 5 service → facade không phục vụ ai trọn vẹn.

---

## 4. Đợt 1 & 2 — an toàn, compiler bảo chứng

### Đợt 1 — Trải phẳng `OrderService` 🟢 xong `59792c9`

> **Rủi ro: rất thấp.** Chỉ xuống dòng và đổi tên tham số. Compiler bắt mọi sai sót.
> **Payoff cao nhất trên mỗi đơn vị rủi ro** — làm trước.

- [x] Xoá 5 method chết ở §3.4. **Xoá trước khi trải phẳng** để khỏi format thứ vứt đi.
- [x] Trải 6 dòng nhồi thành **30** method, theo đúng style `InventoryService`.
      *(Plan ước 32; đếm lại chính xác là 35 method uỷ thác − 5 chết = 30.)*
- [x] Khôi phục tên tham số **bằng cách chép từ service đích** (§3.3), giữ đúng
      `sessionBranchId` vs `branchId`.
      → Bắt thêm được một chỗ plan chưa ghi: `serveAllPickedUp` nhận **`tableNumber`** (số bàn
      hiển thị, kiểu chuỗi) chứ không phải `tableId`. Facade cũ gọi là `t`.
- [x] Nhóm method theo service đích, mỗi nhóm một comment một dòng.
- [x] Javadoc lớp ghi rõ: đây là facade tương thích, logic nằm ở 5 service, **thêm use case mới thì
      viết thẳng vào service chuyên trách chứ không nống facade to ra**.
- [x] Giữ nguyên `CartLine`, `UnblockResult`, `BulkReadyResult` (Đợt 2 mới đụng).

**Kết quả:** 56 → 240 dòng (dài hơn vì mỗi method 3–4 dòng + javadoc), 0 dòng nhồi,
**0 dòng >120 ký tự**, 352/352 test xanh, WAR build OK.

**Nghiệm thu:** không dòng nào >120 ký tự, không tham số 1 ký tự. `mvn test` xanh, WAR build được.

**Kiểm chứng bắt buộc (vì test không phủ tầng này) — ĐÃ CHẠY, kết quả ở dưới.**

⚠️ So bằng `grep ... 'public ... \w+\('` là **KHÔNG ĐỦ**: nó cắt ở dấu `(` nên một method bị đổi
kiểu tham số hay kiểu trả về vẫn lọt. Phải so **chữ ký đầy đủ theo KIỂU** (bỏ qua tên tham số, vì
đổi tên chính là mục tiêu của đợt này):

```python
# so chữ ký chuẩn hoá giữa HEAD và bản làm việc
import re, subprocess
def sigs(text):
    text = re.sub(r'\s+', ' ', text)
    out = set()
    for m in re.finditer(r'public (?:final )?(?:static )?([\w<>,.\[\] ]+?) (\w+)\(([^)]*)\)', text):
        ret, name, params = m.group(1).strip(), m.group(2), m.group(3)
        if name == 'OrderService': continue
        types = [' '.join(p.split()[:-1]) or p for p in
                 (x.strip() for x in params.split(',')) if p]
        out.add(f"{ret} {name}({', '.join(types)})")
    return out
```

**Kết quả thật:** 39 → 34 chữ ký · mất đúng 5 cái đã liệt kê §3.4 · **thêm mới RỖNG**
⇒ không method nào bị đổi kiểu trả về hay kiểu tham số. Đây là bằng chứng thay cho test.

📌 **Bẫy đo lường (áp dụng cho cả plan Barista):** `awk 'length>120'` đếm **BYTE**, không phải ký
tự. Tiếng Việt có dấu là 2–3 byte/ký tự, `─` là 3 byte — nên awk báo `OrderService` còn 11 dòng dài
trong khi thực tế là **0**. Đo độ dài dòng phải dùng `len()` của Python trên chuỗi đã decode UTF-8.

### Đợt 2 — Gỡ hai kiểu kết quả nhân đôi 🟢 xong `e95c72a`

> **Rủi ro: thấp–trung bình.** Đổi kiểu trả về công khai; compiler bắt hết điểm gọi.

- [x] Xoá `OrderService.UnblockResult`, trả thẳng `OrderIssueService.UnblockResult`.
- [x] Xoá `OrderService.BulkReadyResult`, trả thẳng `KdsOrderWorkflowService.BulkReadyResult`.
- [x] Sửa 4 điểm gọi — compiler chỉ ra **đúng 4 chỗ như plan dự đoán**.
      → Hiệu ứng phụ đáng ghi: `KdsServlet` **không còn tham chiếu `OrderService`** nữa,
      giờ chỉ biết `KdsService` + hai kiểu kết quả. Tầng phân chia rõ hơn.
- [x] Không đổi tên getter.
- [x] `CartLine` → **GIỮ NGUYÊN.** Đếm thật: **11 file** tham chiếu (`OrderCartForm`, `PosServlet`,
      `QrMenuServlet`, `CashierOrderValidator`, `OrderPlacementService`, `QrOrderService`,
      `OrderQuantityValidator` + 2 IT + 2 test). Vượt xa ngưỡng 3 file plan đặt để dừng → ghi §6.

⚠️ **Bẫy gặp phải:** `mvn -q compile` (biên dịch tăng dần) **báo thành công dù code đã hỏng** vì
dùng lại `.class` cũ. Phải `mvn -q clean compile` mới lộ 4 lỗi. Mọi lần kiểm chứng kiểu "để compiler
chỉ ra điểm gọi" trong file này **bắt buộc dùng `clean compile`**.

### Đợt 2b — Hai file model 🟢 xong `e95c72a`

> **Rủi ro: gần bằng 0.** POJO thuần, chỉ xuống dòng.

- [x] `model/WasteEvent.java` — tách 13 cặp getter/setter.
- [x] `model/WasteEventReview.java` — tách 13 cặp.
- [x] Đổi tham số setter `v` → tên field, thêm `this.`.
- [x] **Không đổi tên getter/setter.** Script tự `assert` danh sách tên method không đổi **trước
      khi ghi file** — mạnh hơn grep thủ công. Đối chiếu thêm: `productName` dùng ở 14 JSP,
      `status` ở 20 JSP, nên đổi tên là gãy diện rộng mà compiler không bắt.

**Kết quả chung:** toàn repo **hết sạch dòng nhồi nhiều method**. 352/352 test xanh.

---

## 5. Đợt 3 — Bỏ hẳn facade ⚪ · CÓ ĐIỀU KIỆN TIÊN QUYẾT

> **Không bắt tay khi chưa thoả điều kiện dưới đây.** Đây không phải "để sau cho đỡ ngại" —
> đây là việc sửa đúng 7 file thuộc 4 role mà **không có một unit test nào đỡ**.

**Điều kiện tiên quyết:**
1. ~~Có test cho `KdsOrderWorkflowService` + `OrderIssueService`~~ → 🟢 **đã xong** `8fae5db`
   (`BaristaIssueWorkflowIT`, 12/14 method). ⚠️ Nhưng vẫn cần **Docker cục bộ** thì lưới này mới
   dùng được lúc refactor — chạy trên CI nghĩa là biết mình sai *sau khi đẩy code*, không phải lúc gõ.
2. ~~`CartLine` phải chuyển ra khỏi facade TRƯỚC~~ → 🟢 **đã xong** — nay là `model/CartLine.java`,
   `OrderPlacementService` hết phụ thuộc ngược vào facade.
3. ⚪ Người dùng xác nhận muốn đổi kiến trúc, không chỉ muốn dễ đọc.

**Nội dung nếu làm:** mỗi caller tự giữ đúng service nó cần, bỏ tầng trung gian.

| File | Đổi thành |
|---|---|
| `service/barista/KdsService.java` | giữ `KdsOrderWorkflowService` + `OrderIssueService` + `OrderQueryService` |
| `controller/barista/KdsServlet.java` | qua `KdsService`, **không** giữ service shared trực tiếp |
| `service/cashier/PickupService.java` | `OrderHandoffService` + `OrderQueryService` |
| `controller/cashier/OrderInboxServlet.java` | qua service của cashier |
| `service/customer/QrOrderService.java` | `OrderQueryService` + `OrderPlacementService` + `OrderIssueService` |
| `controller/cashier/PosServlet.java` | `OrderPlacementService` + `OrderQueryService` |
| `web/support/BaristaShiftSupport.java` | chỉ `KdsOrderWorkflowService` (1 method) |

**Lợi ích:** đọc constructor là biết màn đó đụng vào phần nào của luồng đơn.
**Giá phải trả:** 7 file, 4 role, không lưới an toàn; constructor test phải sửa theo.

**Gợi ý thứ tự nếu làm:** `BaristaShiftSupport` (1 method) → `PosServlet` (2) → `QrOrderService` (4)
→ `OrderInboxServlet` (5) → `PickupService` (8) → `KdsServlet` (9) → `KdsService` (14).
Dễ nhất trước để lộ vấn đề sớm khi giá còn rẻ.

---

## 6. Sổ ghi nhận phát sinh

| Ngày | File | Vấn đề | Trạng thái |
|---|---|---|---|
| 2026-08-02 | 5 service đích + `OrderRepository` | ~~**0 unit test**, rủi ro nền của cả repo~~ → **ĐÁNH GIÁ SAI, đã đính chính**: repo có 8 file IT / 1796 dòng chạy trên CI mọi PR, `BaristaTransactionIT` phủ đúng 3 đường tranh chấp nguy hiểm nhất. Khoảng trống thật chỉ là 3/14 method. Nguyên nhân nhận định sai: chỉ grep tên service trong `src/test` mà không kiểm IT phủ gì và CI có chạy không. | 🟢 **Đã lấp hết** — `8fae5db` (`BaristaIssueWorkflowIT`) đưa 3/14 → 12/14, `CashierOrderCancellationIT` phủ nốt `cancelItem` + `voidOrder` → **14/14** |
| 2026-08-02 | `OrderService.CartLine` | **Đếm thật: 11 file tham chiếu** — vượt ngưỡng 3 file, Đợt 2 giữ nguyên theo đúng luật plan. Vấn đề sâu hơn: `OrderPlacementService.placeOrder` nhận `List<OrderService.CartLine>`, tức **service chuyên trách phụ thuộc ngược vào facade** cho DTO của chính nó. | 🟢 **Xong** — chuyển sang `model/CartLine.java`. Chọn `model/` vì cả ba tầng đều chạm: `web/form` dựng, `service/cashier`+`service/customer` kiểm tra, `OrderPlacementService` tiêu thụ. 11 file đổi import, compiler bắt hết |
| 2026-08-03 | `pom.xml` (profile `integration`) · `SqlServerIntegrationSupport` | **Lưới IT chưa từng chạy xanh.** Lần chạy CI đầu tiên (2026-08-03, workflow tạo 2026-08-01 nhưng tới nay mới có PR) đỏ 6/8 file. Nguyên nhân KHÔNG phải nội dung test: support dừng container ở `@AfterAll` rồi khởi động lại ở class sau → mỗi class một cổng khác, trong khi `DBConnection.DS` là pool `static final` giữ mãi cổng của class đầu tiên. Từ class thứ hai, mọi test đi qua service timeout 30s "Connection refused". | 🟢 **Đã sửa** — `reuseForks=false`, mỗi class một JVM. Không dùng chung container vì `DatabaseMigrationIT` khẳng định `catalog.Product` đúng 3 dòng |
| 2026-08-03 | `SqlServerIntegrationSupport.cafeJdbcUrl` | **Toàn bộ IT chạy nhầm trong `master`.** `MSSQLServerContainer.getJdbcUrl()` trả `jdbc:sqlserver://localhost:32769;encrypt=false` — không có `databaseName`, nên `replaceFirst` là phép thay thế không khớp gì; DB `CafeChain` tạo xong bị bỏ không. Lộ ra vì `DatabaseNormalizationIT` so DB hiện tại với master và thấy giống hệt. | 🟢 **Đã sửa** — helper `withDatabaseName` thay-hoặc-nối, dùng chung cho cả hai nhánh |
| 2026-08-03 | `OrderDao.reservePickupSequence` + `OrderPlacementService.placeOrder` | **Deadlock thật khi đặt đơn đồng thời.** `CriticalIntegrityIT.fifty_concurrent_orders_receive_unique_pickup_codes` đỏ với SQL error 1205. Cấp mã pickup là `SELECT MAX(...) WHERE BranchId/BusinessDate` rồi INSERT vào chính bảng đó → quét dải lấy khoá S rồi nâng cấp lên X, kinh điển sinh deadlock. Vòng thử lại ở `insertWithPickupCode` chỉ bắt trùng mã, KHÔNG bắt lỗi 1205, nên deadlock thoát nguyên vẹn ra ngoài. Hệ quả thật: nhiều khách quét QR đặt cùng lúc thì một số nhận lỗi. | ⚪ **Ghi nhận, KHÔNG sửa** (luật N2) — sửa là đổi hành vi giao dịch, nằm ngoài phạm vi cả hai plan. Cần task riêng: hoặc thử lại khi gặp 1205, hoặc thay bằng bảng cấp số có thứ tự khoá xác định |
| 2026-08-03 | `OrderIssueService.voidOrder` | Huỷ cả đơn **bỏ sót món BLOCKED**: guard R5 chỉ chặn MAKING/READY/PICKED_UP/SERVED nên đơn có món BLOCKED vẫn huỷ được, nhưng vòng lặp huỷ chỉ đụng món `WAITING` → đơn về CANCELLED mà món vẫn nằm BLOCKED. Món mồ côi trong đơn đã huỷ. | ⚪ **Ghi nhận, KHÔNG sửa** (luật N2). `CashierOrderCancellationIT` khoá đúng hành vi hiện tại — sửa hành vi thì test đỏ đúng chỗ, không âm thầm |
| 2026-08-02 | Quy trình | `mvn -q compile` tăng dần **báo xanh dù code đã hỏng** (dùng lại `.class` cũ). Đã suýt bỏ lọt 4 lỗi ở Đợt 2. | ✅ Đã ghi thành luật: kiểm chứng kiểu "để compiler chỉ ra" phải dùng `clean compile` |

---

## 7. Bảng theo dõi

| Đợt | Nội dung | Rủi ro | Trạng thái | Commit |
|---|---|---|---|---|
| 1 | Trải phẳng `OrderService` + xoá 5 method chết | Rất thấp | 🟢 | `59792c9` — 56→240 dòng, 30 method, 0 dòng >120 ký tự, 352/352 |
| 2 | Gỡ 2 kiểu kết quả nhân đôi | Thấp–TB | 🟢 | `e95c72a` — 4 điểm gọi, `KdsServlet` hết phụ thuộc `OrderService` |
| 2b | 2 file model | ~0 | 🟢 | `e95c72a` — 26 cặp getter/setter, tên method giữ nguyên |
| 3 | Bỏ facade | **Cao** | ⚪ | Chờ điều kiện tiên quyết §5 |

⚪ chưa làm · 🟡 đang làm · 🟢 xong · 🔴 bị chặn

---

## 8. Khuyến nghị

**Làm Đợt 1 + 2 + 2b.** Ba đợt này giải quyết trọn vẹn điều đã ghi trong sổ plan Barista
("7 method nhồi trên 1 dòng, tên tham số 1 ký tự"), đều nằm trong vùng compiler bảo chứng, và
không đụng file nào của Cashier/QR/Manager.

**Hoãn Đợt 3.** Nó đổi kiến trúc chứ không phải dọn dẹp, và giá trị của nó (đọc constructor biết
màn đụng gì) nhỏ hơn nhiều so với rủi ro sửa 7 file thuộc 4 role mà không có test.

Nếu muốn đầu tư tiếp vào vùng này, **việc đáng làm hơn Đợt 3 là bổ sung unit test cho
`KdsOrderWorkflowService` và `OrderIssueService`** — vừa tự nó có giá trị, vừa mở khoá Đợt 3.
