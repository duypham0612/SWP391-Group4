# Kế hoạch: đưa "Yêu cầu tạm hết" và "Hao hụt" lên tầng Branch Manager

> Trạng thái: chưa thực hiện · Phạm vi: ~9 file sửa, 7 file tạo (3 là test) · Không đổi schema · Không đụng dữ liệu seed/config

---

## 1. Bối cảnh

Hai luồng việc của Pha chế hiện **không có tín hiệu nào** ở tầng Quản lý chi nhánh.

**Yêu cầu tạm hết.** Khi pha chế bấm "Báo tạm hết", `BranchMenuService.request86()` gọi `dao.updateIs86(..., true, ...)` — món bị **chặn bán ngay lập tức**; manager duyệt chỉ là hậu kiểm. Nhưng `manager/dashboard.jsp` không nhắc gì, sidebar không có số đếm, `menu-block.jsp` không tự làm mới. Manager phải tự nhớ mở `/manager/menu-block`. Món có thể nằm ngoài menu nhiều giờ mà không ai biết.

**Hao hụt & làm lại.** `logWaste` ghi `WasteLog` + `applyTxn(..., TxnType.WASTE, "WasteLog", ...)` vào sổ cái. Không controller manager nào đọc `WasteLog` — grep `views/manager/` cho "waste|hao hụt" ra **0 kết quả**. Manager chỉ thấy gián tiếp khi mở sổ cái **từng nguyên liệu một** ở `/manager/inventory?ingredientId=X`; không tổng hợp được theo ngày, không biết ai ghi gì.

Đối chiếu: màn thu ngân `/cashier/handoff` tự làm mới 5 giây/lần. Trải nghiệm hai bên lệch hẳn.

**Kết quả mong muốn:** manager mở dashboard là thấy ngay hai việc cần xử lý, và có màn riêng soi hao hụt theo khoảng ngày.

---

## 2. Ràng buộc

- **Không đổi schema.** Không đụng luồng ghi (`request86`, `logWaste`, `applyTxn`). Nếu thấy mình đang sửa mấy hàm đó là đã đi lạc.
- **Chỉ báo trên dashboard** — không badge sidebar, không polling. Tránh thêm truy vấn vào mọi request.
- **Quy tắc chữ:** jargon `86` chỉ sống trong định danh code (`request86`, `Reason86`, `is86`). Chữ hiển thị dùng **"Yêu cầu tạm hết"** / **"Món tạm hết"**. Codebase hiện đã tuân thủ sạch — đừng phá.
- JSP cấm scriptlet, chỉ JSTL + EL. Comment tiếng Việt.
- Không sửa `sql/database.sql`, `src/main/resources/db.properties`, user seed hoặc account demo trong phạm vi này. Nếu cần dữ liệu test thủ công thì tạo qua giao diện đang có.
- Không đổi quyền trong `RbacFilter`; route mới `/manager/waste` tự nằm dưới prefix `/manager/`.

---

## 3. Năm rủi ro — triệt tiêu bằng cấu trúc, không bằng "nhớ cẩn thận"

| | Rủi ro | Thiết kế loại bỏ |
|---|---|---|
| **R1** | Service manager gọi service barista để dùng `summarize` → lệch tầng | **Xoá** nested class `WasteSummary` khỏi package barista, đưa lên `service/shared/`. Sau đó *không tồn tại* đường nào để lệch tầng |
| **R2** | Dashboard gọi `getOpenRequests` 2 lần (đếm + liệt kê) | Số đếm và danh sách **là cùng một object**: `Summary.openMenuBlocks` giữ `List`, `getOpenMenuBlockCount()` chỉ là `list.size()` |
| **R3** | Tổng tiền tính theo trang thay vì cả khoảng → **sai âm thầm** | `summarize()` không nhận `page/pageSize`; `WasteSummary` không có factory nhận `WasteLogPage`. Muốn sai phải viết một dòng lạ mắt |
| **R4** | `WasteLog.LoggedAt` lưu UTC, manager nhập ngày giờ VN → lệch 7 tiếng | Quy đổi **đặt tên + gom một chỗ + có test**. `Range` giữ cả `fromDate` (VN) lẫn `fromUtc` — kiểu dữ liệu tự nói mốc nào thuộc múi giờ nào |
| **R5** | `.stat .label`/`.value` là `<span>` không `display:block` → nhãn dài dính vào số | Sửa tận gốc rule dùng chung |

**Vì sao R5 an toàn:** đã khảo sát 38 thẻ `.stat` trên 9 file JSP — markup đồng nhất **100%** (`<span class="label">` rồi `<span class="value">`), `.label`/`.value` không dùng ở đâu ngoài `.stat`, không màn nào phụ thuộc việc chúng nằm cùng dòng. Thêm bằng chứng: `margin-top:6px` trên `.value` **đang bị bỏ qua** vì phần tử inline, và tác giả đã tự viết `display:block` cho `<small>` ở dòng 1118 — cùng lỗi, đã vá cục bộ một lần.

---

## 4. Tài sản tái dùng (đã xác minh tồn tại)

| Thứ cần | Có sẵn ở |
|---|---|
| Danh sách yêu cầu tạm hết đang mở | `BranchMenuService.getOpenRequests(branchId)` — `service/shared/BranchMenuService.java:41`. DAO đã `ORDER BY` quá hạn lên đầu |
| Getter hiển thị của yêu cầu | `MenuBlockRequest`: `getProductName()`, `getReasonLabel()`, `getRequestedAtText()`, `getBackInEtaText()`, `getRequesterName()`, `isOverdue()` |
| Bảng hao hụt + lọc + phân trang | `InventoryService.getWasteLogPage(...)` — `service/shared/InventoryService.java:618` |
| Danh sách hao hụt theo khoảng | `InventoryService.getWasteLogs(branchId, fromUtc, toUtc)` — dòng 608 |
| Getter hiển thị của dòng hao hụt | `WasteLog`: `getLoggedAtDisplay()`, `getCostDisplay()`, `getWasteTypeLabel()` (dòng 101), `getLoggedByName()`, `isRemake()`, `isActive()` |
| Múi giờ hệ thống | `BusinessDay.VN_ZONE` — `common/BusinessDay.java:26` |
| Lấy branchId trong servlet manager | `InventoryDashboardServlet.branchId(req)` |
| Phân quyền | `RbacFilter` lọc **theo prefix** `/manager/` → route mới tự được bảo vệ |

**`WasteLogDao.findByBranchBetween` chịu được `from`/`to` null** và dùng `>=?` / `<?` → khớp đúng khoảng nửa mở `[from, to)`.

**Tiền lệ cho R3:** `controller/barista/WasteServlet.java:115-116` đã làm đúng khuôn này, kèm comment *"Tổng quan giữ nguyên toàn bộ phạm vi; bảng nhật ký thì chỉ lấy đúng trang từ DB"*. Màn manager chỉ nhân bản, không phát minh.

---

## 4.1. Checklist trước khi code

Làm 5 phút này trước khi sửa để tránh trộn nhầm với thay đổi đang có sẵn trong worktree:

```bash
git status --short
mvn -q test
rg -n "WasteService\\.WasteSummary|service\\.barista|request86|logWaste|applyTxn" src/main/java src/test/java
rg -n "class=\\\"card stat\\\"|class=\\\"label\\\"|class=\\\"value\\\"" src/main/webapp/WEB-INF/views
```

Ghi lại baseline:

- `mvn -q test` có đang xanh không. Nếu đỏ từ trước, chụp tên test lỗi rồi vẫn chạy lại sau từng bước để đảm bảo không thêm lỗi mới.
- Danh sách file đang bẩn. Chỉ stage/commit những file thuộc kế hoạch này; những thay đổi ở KDS, auth, customer, seed... để nguyên.
- Nếu `rg "WasteService\\.WasteSummary"` ra nhiều hơn `BaristaDashboardServlet` + `WasteSummaryTest`, cập nhật Bước 3 cho đủ call site trước khi tách class.
- Nếu `views/manager/menu-block.jsp` hoặc `MenuBlockServlet` chưa có trong nhánh hiện tại, dừng tính năng dashboard tạm hết ở mức thẻ link/bảng và merge sau khi luồng xử lý manager đã vào nhánh.

---

## 5. Các bước

Sau **mỗi** bước chạy `mvn -q test` phải xanh.

### Bước 1 · CSS (độc lập hoàn toàn)

`src/main/webapp/assets/css/cafe-theme.css`, dòng 209-210. **Chỉ thêm `display:block`, không xoá thuộc tính nào:**

```css
.stat .label{display:block; color:var(--muted); font-size:12px; letter-spacing:.04em; text-transform:uppercase; font-weight:700}
.stat .value{display:block; font-family:'Playfair Display',serif; font-size:var(--fs-3xl); font-weight:700; color:var(--brand-700); margin-top:6px; line-height:1}
.stat .muted{display:block; margin-top:6px; font-size:var(--fs-sm)}
```

Rule thứ ba xử lý dòng phụ của `barista/dashboard.jsp` (dùng `<span class="muted">`). Nó được scope trong `.stat` nên **không chạm** `<span class="muted">` ở `admin/report.jsp` / `admin/dashboard.jsp` (nằm trong `.card` không có `.stat`).

Làm riêng một bước để nếu vỡ thì `git diff` chỉ 3 dòng, revert tức thì. Kiểm mắt 9 màn: `admin/dashboard`, `admin/report`, `manager/dashboard`, `cashier/dashboard`, `cashier/shift`, `barista/dashboard`, `barista/shift`, `barista/waste`, `barista/handover`.

---

### Bước 2 · `BusinessDay` + test múi giờ

`src/main/java/com/cafe/common/BusinessDay.java` — thêm 2 hàm. **Không cần import mới** (file đã có `LocalDate`, `LocalDateTime`, `ZoneOffset`).

```java
    /** Đầu ngày VN (00:00) quy về UTC, để so với cột DATETIME2 lưu UTC. */
    public static LocalDateTime vnDayStartUtc(LocalDate vnDate) {
        if (vnDate == null) return null;
        return vnDate.atStartOfDay(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** Đầu ngày VN kế tiếp quy về UTC — mốc KẾT THÚC theo kiểu nửa mở [from, to). */
    public static LocalDateTime vnDayEndExclusiveUtc(LocalDate vnDate) {
        if (vnDate == null) return null;
        return vnDate.plusDays(1).atStartOfDay(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
```

Null-safe để khoảng mở một đầu vẫn chạy (DAO đã hỗ trợ null).

**Test mới** `src/test/java/com/cafe/common/BusinessDayVnRangeTest.java` — JUnit 5, thuần, không DB:

1. `vnDayStartUtc(2026-07-19)` == `2026-07-18T17:00` — chênh đúng 7h, **đúng chiều**
2. `vnDayEndExclusiveUtc(2026-07-19)` == `2026-07-19T17:00`
3. Một ngày dài đúng 24h: `Duration.between(start, end).toHours() == 24`
4. **`vnDayEndExclusiveUtc(d).equals(vnDayStartUtc(d.plusDays(1)))`** — test giết bug mất dòng lúc nửa đêm
5. `null` in → `null` out

---

### Bước 3 · Tách `WasteSummary` (R1) — một commit nguyên tử

**3a. Tạo** `src/main/java/com/cafe/service/shared/WasteSummary.java`. Copy **nguyên xi** thân hàm từ `WasteService` dòng 183-234, chỉ đổi: class thành top-level, `from` nâng lên `public`.

```java
package com.cafe.service.shared;

import com.cafe.model.WasteLog;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tổng hợp hao hụt/làm lại từ một danh sách WasteLog — hàm thuần, không đụng DB.
 * Đặt ở service.shared vì cả Pha chế lẫn Quản lý chi nhánh đều dùng.
 */
public final class WasteSummary {

    private int activeCount;
    private int ingredientWasteCount;
    private int remakeCount;
    private int missingCostCount;
    private BigDecimal totalCost = BigDecimal.ZERO;
    private BigDecimal ingredientWasteCost = BigDecimal.ZERO;
    private BigDecimal remakeCost = BigDecimal.ZERO;
    private String topIngredientName;
    private BigDecimal topIngredientCost = BigDecimal.ZERO;

    private WasteSummary() { }

    public static WasteSummary from(List<WasteLog> logs) {
        WasteSummary s = new WasteSummary();
        Map<String, BigDecimal> byIngredient = new LinkedHashMap<>();
        if (logs == null) return s;
        for (WasteLog log : logs) {
            if (log == null || !log.isActive()) continue;
            s.activeCount++;
            if (log.isRemake()) s.remakeCount++; else s.ingredientWasteCount++;

            BigDecimal cost = log.getLineCost();
            if (cost == null) {
                s.missingCostCount++;
                continue;
            }
            s.totalCost = s.totalCost.add(cost);
            if (log.isRemake()) s.remakeCost = s.remakeCost.add(cost);
            else s.ingredientWasteCost = s.ingredientWasteCost.add(cost);

            String name = log.getIngredientName() == null
                    ? "Nguyên liệu #" + log.getIngredientId() : log.getIngredientName();
            byIngredient.merge(name, cost, BigDecimal::add);
        }
        for (Map.Entry<String, BigDecimal> e : byIngredient.entrySet()) {
            if (s.topIngredientName == null || e.getValue().compareTo(s.topIngredientCost) > 0) {
                s.topIngredientName = e.getKey();
                s.topIngredientCost = e.getValue();
            }
        }
        return s;
    }

    public int getActiveCount() { return activeCount; }
    public int getIngredientWasteCount() { return ingredientWasteCount; }
    public int getRemakeCount() { return remakeCount; }
    public int getMissingCostCount() { return missingCostCount; }
    public BigDecimal getTotalCost() { return totalCost; }
    public BigDecimal getIngredientWasteCost() { return ingredientWasteCost; }
    public BigDecimal getRemakeCost() { return remakeCost; }
    public String getTopIngredientName() { return topIngredientName; }
    public BigDecimal getTopIngredientCost() { return topIngredientCost; }
    public boolean isHasTopIngredient() { return topIngredientName != null; }
}
```

**3b. `service/barista/WasteService.java`:** xoá toàn bộ nested class (dòng 183-234), thêm `import com.cafe.service.shared.WasteSummary;`. Hai method giữ nguyên thân, chỉ đổi kiểu tham chiếu:

```java
    public WasteSummary getTodayWasteSummary(int branchId) throws SQLException {
        return summarize(getWasteLogs(branchId, WasteScope.today()));
    }

    public WasteSummary summarize(List<WasteLog> logs) {
        return WasteSummary.from(logs);
    }
```

Sau khi xoá, kiểm import `LinkedHashMap`/`Map` còn ai dùng không; nếu không thì bỏ.

**3c. `controller/barista/BaristaDashboardServlet.java:40`:** `WasteService.WasteSummary` → `WasteSummary`, thêm import.

**3d. `test/service/barista/WasteSummaryTest.java`:** bỏ prefix `WasteService.` ở 6 chỗ gọi, thêm `import com.cafe.service.shared.WasteSummary;`.

> ⚠️ **KHÔNG tách `WasteScope`.** Nó phụ thuộc 3 constant `VN_ZONE`/`DATE_FMT`/`DATE_TIME_FMT` của outer class, và `WasteServlet.java` đang có thay đổi chưa commit → dễ xung đột.

> ✅ **Bằng chứng không đổi hành vi:** 6 test của `WasteSummaryTest` xanh mà **không assertion nào bị sửa**. Nếu phải sửa assertion nghĩa là đã đổi logic — dừng lại, xem lại bước 3a.

---

### Bước 4 · `ManagerDashboardService` + servlet + test

**4a.** `src/main/java/com/cafe/service/manager/ManagerDashboardService.java` — thêm import `BranchMenuService`, `InventoryService`, `WasteSummary`, `MenuBlockRequest`, `BusinessDay`, `LocalDateTime`, `java.util.List`; thêm 2 field:

```java
    private final BranchMenuService branchMenuService = new BranchMenuService();
    private final InventoryService inventoryService = new InventoryService();
```

Thêm 2 method:

```java
    /** Yêu cầu tạm hết đang chờ manager xử lý. DAO đã xếp món quá hạn lên đầu. */
    public List<MenuBlockRequest> getOpenMenuBlockRequests(int branchId) throws SQLException {
        return branchMenuService.getOpenRequests(branchId);
    }

    /** Hao hụt + làm lại của NGÀY VN hôm nay (WasteLog.LoggedAt lưu UTC nên phải quy đổi). */
    public WasteSummary getTodayWasteSummary(int branchId, LocalDate todayVn) throws SQLException {
        LocalDateTime fromUtc = BusinessDay.vnDayStartUtc(todayVn);
        LocalDateTime toUtc = BusinessDay.vnDayEndExclusiveUtc(todayVn);
        return WasteSummary.from(inventoryService.getWasteLogs(branchId, fromUtc, toUtc));
    }
```

Trong `getTodaySummary`, thêm trước `return s;` — **gọi đúng một lần** (R2):

```java
        s.openMenuBlocks = getOpenMenuBlockRequests(branchId);
        s.todayWaste = getTodayWasteSummary(branchId, today);
```

Mở rộng class `Summary` (giữ style field public + getter):

```java
        public List<MenuBlockRequest> openMenuBlocks = List.of();
        public WasteSummary todayWaste = WasteSummary.from(List.of());

        public List<MenuBlockRequest> getOpenMenuBlocks() { return openMenuBlocks; }
        public int getOpenMenuBlockCount() { return openMenuBlocks == null ? 0 : openMenuBlocks.size(); }
        public boolean isHasOpenMenuBlocks() { return getOpenMenuBlockCount() > 0; }
        public WasteSummary getTodayWaste() { return todayWaste; }
        public int getOverdueMenuBlockCount() {
            int n = 0;
            if (openMenuBlocks == null) return 0;
            for (MenuBlockRequest r : openMenuBlocks) if (r != null && r.isOverdue()) n++;
            return n;
        }
```

> ⚠️ `todayWaste` **phải** khởi tạo bằng `WasteSummary.from(List.of())`, không để `null`. EL trên null cho ra chuỗi rỗng → thẻ hiện `" ₫"` thay vì `"0 ₫"`, trang vẫn 200. Xem §7(d).
> ⚠️ Getter list null-safe để test `new Summary()` phản ánh đúng hành vi JSP: render được cả khi service phụ lỗi hoặc test dựng object tối giản.

**4b.** `controller/manager/ManagerDashboardServlet.java:23` — sửa lỗi múi giờ đang tồn tại sẵn:

```java
        LocalDate today = LocalDate.now(BusinessDay.VN_ZONE);
```

thêm `import com.cafe.common.BusinessDay;`. Không set attribute mới nào — mọi thứ đã nằm trong `summary`.

**4c. Test mới** `src/test/java/com/cafe/service/manager/ManagerDashboardSignalsTest.java` — dựng `MenuBlockRequest` bằng setter, không DB:

1. List rỗng → `openMenuBlockCount == 0`, `overdueMenuBlockCount == 0`, `hasOpenMenuBlocks == false`
2. 3 yêu cầu, 1 có `backInEta` quá khứ + `closedAt == null` → `count == 3`, `overdue == 1`
3. `backInEta == null` → tính vào `count`, **không** vào `overdue`
4. `new Summary()` chưa gán gì → `todayWaste != null`, `getTotalCost()` bằng 0, `openMenuBlockCount == 0`

---

### Bước 5 · `manager/dashboard.jsp`

**Thêm 2 thẻ** vào `<div class="card-grid">`, sau thẻ "Chấm công chờ duyệt":

```jsp
    <a class="card stat" href="${ctx}/manager/menu-block"
       style="${summary.overdueMenuBlockCount gt 0 ? 'border-color:var(--st-cancelled)' : ''}">
        <span class="label">Yêu cầu tạm hết chờ xử lý</span>
        <span class="value">${summary.openMenuBlockCount}</span>
        <span class="muted">
            <c:choose>
                <c:when test="${summary.overdueMenuBlockCount > 0}">${summary.overdueMenuBlockCount} món đã quá hạn dự kiến có lại</c:when>
                <c:when test="${summary.openMenuBlockCount > 0}">món đang bị chặn bán</c:when>
                <c:otherwise>Không có món nào bị chặn bán</c:otherwise>
            </c:choose>
        </span>
    </a>
    <a class="card stat" href="${ctx}/manager/waste">
        <span class="label">Hao hụt hôm nay</span>
        <span class="value"><fmt:formatNumber value="${summary.todayWaste.totalCost}" maxFractionDigits="0"/> ₫</span>
        <span class="muted">${summary.todayWaste.activeCount} dòng · ${summary.todayWaste.remakeCount} làm lại</span>
    </a>
```

**Thêm bảng** ngay trước `<div class="grid-2">`, bọc trong `c:if` để dashboard không phình lúc bình thường:

```jsp
<c:if test="${summary.hasOpenMenuBlocks}">
<div class="card" style="margin-bottom:var(--s5)">
    <h3 style="margin-top:0">Yêu cầu tạm hết đang chờ</h3>
    <p class="muted">Món đã bị chặn bán ngay khi pha chế báo. Duyệt hoặc mở bán lại để món quay về menu.</p>
    <table class="table">
        <thead><tr><th>Món</th><th>Lý do</th><th>Người báo</th><th>Báo lúc</th><th>Dự kiến có lại</th><th></th></tr></thead>
        <tbody>
            <c:forEach var="r" items="${summary.openMenuBlocks}">
                <tr>
                    <td><strong>${r.productName}</strong>
                        <c:if test="${r.overdue}"><span class="badge badge-cancelled" style="margin-left:6px">Quá hạn</span></c:if>
                    </td>
                    <td>${r.reasonLabel}</td>
                    <td>${r.requesterName}</td>
                    <td>${r.requestedAtText}</td>
                    <td><c:choose>
                        <c:when test="${empty r.backInEtaText}">Chưa rõ</c:when>
                        <c:otherwise>${r.backInEtaText}</c:otherwise>
                    </c:choose></td>
                    <td><a class="btn btn-ghost btn-sm" href="${ctx}/manager/menu-block">Xử lý</a></td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</div>
</c:if>
```

Kiểm file đã khai `<%@ taglib prefix="fmt" ... %>` chưa (đang dùng `fmt:formatNumber` cho doanh thu nên chắc có). Thẻ hao hụt tạm 404 cho tới bước 7.

---

### Bước 6 · `WasteReportService` + test

`src/main/java/com/cafe/service/manager/WasteReportService.java`:

```java
package com.cafe.service.manager;

import com.cafe.common.BusinessDay;
import com.cafe.service.shared.InventoryService;
import com.cafe.service.shared.WasteSummary;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * M · WasteReportService — nhật ký hao hụt TOÀN chi nhánh cho Quản lý.
 * Khác WasteService của Pha chế: không giới hạn theo ca, xem được mọi người ghi.
 */
public class WasteReportService {

    /** Nạp cả khoảng vào RAM để tổng hợp nên phải chặn khoảng vô hạn. */
    private static final int MAX_DAYS = 92;
    private static final int DEFAULT_DAYS = 7;

    private final InventoryService inventoryService = new InventoryService();

    /** Chuẩn hoá khoảng ngày từ tham số URL. Hàm thuần — test được, không đụng DB. */
    public static Range resolveRange(String fromParam, String toParam, LocalDate todayVn) {
        LocalDate from = parseOrNull(fromParam);
        LocalDate to = parseOrNull(toParam);
        if (from == null && to == null) {
            to = todayVn;
            from = todayVn.minusDays(DEFAULT_DAYS - 1);
        } else if (from == null) {
            from = to.minusDays(DEFAULT_DAYS - 1);
        } else if (to == null) {
            to = from.plusDays(DEFAULT_DAYS - 1);
        }
        if (to.isBefore(from)) { LocalDate tmp = from; from = to; to = tmp; }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) from = to.minusDays(MAX_DAYS - 1);
        return new Range(from, to);
    }

    /** Tổng hợp TOÀN khoảng — không phân trang, không áp bộ lọc bảng. */
    public WasteSummary summarize(int branchId, Range range) throws SQLException {
        return WasteSummary.from(
                inventoryService.getWasteLogs(branchId, range.getFromUtc(), range.getToUtc()));
    }

    /** Đúng một trang cho bảng; lọc và phân trang đều làm ở DB. */
    public InventoryService.WasteLogPage page(int branchId, Range range, String query,
                                              String wasteType, String status,
                                              int page, int pageSize) throws SQLException {
        return inventoryService.getWasteLogPage(branchId, range.getFromUtc(), range.getToUtc(),
                query, wasteType, status, page, pageSize);
    }

    /** Ngày rác trên URL không được làm 500. */
    private static LocalDate parseOrNull(String s) {
        try { return s == null || s.isBlank() ? null : LocalDate.parse(s.trim()); }
        catch (DateTimeParseException e) { return null; }
    }

    /** Giữ CẢ mốc VN (render vào input) lẫn mốc UTC (query) để không lẫn múi giờ. */
    public static final class Range {
        private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private final LocalDate fromDate;
        private final LocalDate toDate;

        private Range(LocalDate fromDate, LocalDate toDate) {
            this.fromDate = fromDate;
            this.toDate = toDate;
        }

        public LocalDate getFromDate() { return fromDate; }
        public LocalDate getToDate() { return toDate; }
        public LocalDateTime getFromUtc() { return BusinessDay.vnDayStartUtc(fromDate); }
        public LocalDateTime getToUtc() { return BusinessDay.vnDayEndExclusiveUtc(toDate); }
        public long getDayCount() { return ChronoUnit.DAYS.between(fromDate, toDate) + 1; }
        public String getLabel() { return fromDate.format(LABEL_FMT) + " – " + toDate.format(LABEL_FMT); }
    }
}
```

`getFromDate()` trả `LocalDate`, EL render ra `yyyy-MM-dd` — đúng định dạng `<input type="date">` cần.

**Test mới** `src/test/java/com/cafe/service/manager/WasteRangeTest.java`:

1. Param null/rỗng → 7 ngày gần nhất, `toDate == todayVn`
2. Param rác (`"hom-nay"`) → về mặc định, **không ném exception**
3. `from > to` → hoán đổi, `dayCount > 0`
4. `from == to` → `dayCount == 1`, `toUtc - fromUtc == 24h`
5. Khoảng 400 ngày → kẹp còn `dayCount == 92`

---

### Bước 7 · Servlet + JSP + sidebar

**7a.** `src/main/java/com/cafe/controller/manager/WasteReportServlet.java`, `@WebServlet("/manager/waste")`, **chỉ `doGet`** — manager chỉ xem; sửa/huỷ hao hụt vẫn thuộc `/barista/waste`.

Luồng:
1. `int branchId = InventoryDashboardServlet.branchId(req);`
2. `LocalDate todayVn = LocalDate.now(BusinessDay.VN_ZONE);`
3. `Range range = WasteReportService.resolveRange(req.getParameter("from"), req.getParameter("to"), todayVn);`
4. Đọc bộ lọc bảng bằng **whitelist** — chép 4 helper private từ `WasteServlet` (`textParam` cắt 100 ký tự, `allowedParam`, `positiveIntParam`, `pageSize`): `wasteType ∈ {SPILL, EXPIRED, REMAKE, OTHER}`, `status ∈ {ACTIVE, VOIDED}`. Có thể giữ `pageSize() = 5` để cùng nhịp với màn pha chế, hoặc đổi thành `10` nếu muốn màn manager ít bấm trang hơn; chọn một số cố định, không lấy từ URL.
5. `req.setAttribute("summary", service.summarize(branchId, range));` ← **toàn khoảng**
6. `req.setAttribute("wasteLogPage", p);` và `req.setAttribute("logs", p.getLogs());` ← **một trang**
7. `req.setAttribute("range", range)`, các attribute bộ lọc, `pageTitle = "Hao hụt & làm lại"`
8. Forward `/WEB-INF/views/manager/waste.jsp`

**7b.** `src/main/webapp/WEB-INF/views/manager/waste.jsp`. Bám khuôn `barista/waste.jsp` (453 dòng) để tái dùng CSS sẵn có. Bố cục:

1. **`.page-header`** — eyebrow "Quản lý chi nhánh", `<h1>Hao hụt & làm lại</h1>`. Bên phải `<div class="waste-scope">${range.label}</div>` (class có sẵn, CSS dòng 1110)
2. **Form khoảng ngày** `method="get"` — 2 `<input type="date" name="from"/"to" value="${range.fromDate}"/"${range.toDate}">`, hidden `page=1`, và 3 nút preset (Hôm nay / 7 ngày / 30 ngày) dựng bằng `<c:url>`
3. **`<section class="waste-summary">`** — 4 thẻ `.stat`, EL **chỉ đọc `${summary.*}`**, tuyệt đối không đụng `${wasteLogPage.logs}`:
   - Tổng chi phí `${summary.totalCost}` + `<small>${summary.activeCount} dòng hiệu lực</small>`
   - Hao hụt nguyên liệu `${summary.ingredientWasteCount}` / `${summary.ingredientWasteCost}`
   - Làm lại món `${summary.remakeCount}` / `${summary.remakeCost}`
   - Hao nhiều nhất `${summary.topIngredientName}` (dùng `class="value waste-top-name"` + `isHasTopIngredient`)
4. **Dòng chú thích bắt buộc** ngay dưới:
   > `Bốn số trên tính cho toàn bộ khoảng ngày ${range.label}, không chịu ảnh hưởng của bộ lọc và phân trang bên dưới.`
5. **Cảnh báo thiếu giá** — chép `barista/waste.jsp:67-69` (`${summary.missingCostCount > 0}`)
6. **Bộ lọc bảng** — chép khuôn `barista/waste.jsp:250-277`, đổi `action` thành `${ctx}/manager/waste`, **thêm 2 hidden `from` và `to`**
7. **Bảng** — chép `barista/waste.jsp:278-336`, **bỏ cột "Thao tác"**. 8 cột: Thời gian `${w.loggedAtDisplay}` · Nguyên liệu `${w.ingredientName}` · Số lượng `${w.quantity} ${w.ingredientUnit}` · Loại `${w.wasteTypeLabel}` · Lý do `${w.reason}` · Thành tiền `${w.costDisplay}` · Người ghi `${w.loggedByName}` · Trạng thái
8. **Phân trang** — chép `barista/waste.jsp:337-370`, đổi `value="/manager/waste"`, và **mọi `<c:url>` phải thêm `<c:param name="from">` + `<c:param name="to">` + filter hiện tại (`q`, `wasteType`, `status`)**

> ⚠️ **Chỗ dễ quên nhất:** bước 6 và 8 — nếu thiếu `from`/`to`, bấm sang trang 2 hoặc lọc sẽ làm khoảng ngày âm thầm về mặc định 7 ngày. Trang vẫn 200. Xem §7(b).

**7c.** `views/layout/sidebar.jsp` — thêm vào nhóm manager, sau `/manager/reconciliation`:

```jsp
<li><a class="${curPath == ctx.concat('/manager/waste') ? 'active' : ''}" href="${ctx}/manager/waste" title="Hao hụt &amp; làm lại"><svg class="ic"><use href="#ic-scale"/></svg>Hao hụt &amp; làm lại</a></li>
```

Không badge, không đếm.

---

## 6. Kiểm chứng

### Tự động

`mvn -q test` xanh. Đặc biệt `WasteSummaryTest` 6/6 và **không assertion nào bị sửa** — đó là bằng chứng bước 3 không đổi hành vi.

Sau khi code xong, chạy thêm:

```bash
mvn -q -DskipTests package
rg -n "WasteService\\.WasteSummary|service\\.barista" src/main/java/com/cafe/service/manager src/main/java/com/cafe/controller/manager src/test/java/com/cafe/service/manager
rg -n ">[^<]*86[^<]*<" src/main/webapp/WEB-INF/views/manager src/main/webapp/WEB-INF/views/layout/sidebar.jsp
```

Kỳ vọng:

- `package` build được WAR, bắt lỗi compile servlet/JSP taglib sớm hơn chỉ chạy unit test.
- Hai lệnh `rg` cuối rỗng. `86` trong tên class/method Java vẫn được, nhưng không được lọt ra text giao diện manager.

### Thủ công (deploy Tomcat, `manager1` / `123456`)

| # | Thao tác | Kỳ vọng |
|---|---|---|
| 1 | `grep -rn "service.barista" src/main/java/com/cafe/service/manager src/main/java/com/cafe/controller/manager` | **Rỗng** (R1) |
| 2 | Mở 9 màn có `.stat`, nhất là `barista/dashboard` và `barista/waste` (tên nguyên liệu dài) | Nhãn / số / dòng phụ nằm **3 dòng riêng** (R5) |
| 3 | Pha chế báo tạm hết 1 món → mở dashboard manager cùng chi nhánh | Thẻ hiện `1`, bảng đúng tên món / người báo / giờ báo |
| 4 | Bấm "Xử lý" → duyệt ở `/manager/menu-block` → quay lại dashboard | Thẻ về `0`, bảng biến mất |
| 5 | `grep -rn "86" views/manager/dashboard.jsp views/manager/waste.jsp views/layout/sidebar.jsp` | Không có "86" nào lọt ra giao diện |
| 6 | `barista1` gõ `/manager/waste` | 403 |
| 7 | Đăng nhập manager chi nhánh 2 | Không thấy dữ liệu chi nhánh 1 |
| 8 | `?from=rác`, `?page=-5`, `from` sau `to` | Không 500 |
| 9 | Chọn khoảng 30 ngày, lọc `REMAKE`, sang trang 2 rồi quay lại `ACTIVE` | `from`/`to` vẫn giữ nguyên, tổng phía trên không đổi theo filter |
| 10 | Mở `/manager/waste?from=2026-07-19&to=2026-07-19`, ghi hao hụt sáng cùng ngày VN | Dòng có mặt trong bảng, tổng dashboard hôm nay khớp màn report cùng ngày |

---

## 7. Năm chỗ có thể SAI ÂM THẦM

Trang vẫn 200, số vẫn hiện, nhưng sai. Xếp theo mức khó phát hiện:

**(a) Tổng tiền chỉ của trang đang xem.** Xảy ra nếu ai đó "tối ưu" bằng cách bỏ query `summarize` và dùng `${wasteLogPage.logs}`.
→ **Kiểm:** chọn khoảng ngày có >20 dòng, ghi tổng ở trang 1, bấm trang 2 và trang cuối. Tổng phải **y hệt**.

**(b) Phân trang / bộ lọc làm rơi khoảng ngày.** Thiếu `<c:param name="from">`/`"to"`.
→ **Kiểm:** chọn khoảng khác mặc định (tháng trước), bấm sang trang 2. Hai ô `<input type="date">` và `${range.label}` phải **giữ nguyên**.

**(c) Lệch 7 tiếng ở biên ngày.** Cực khó thấy vì chỉ sai ở hai đầu khoảng.
→ **Kiểm:** ghi một dòng hao hụt lúc **07:00–09:00 sáng giờ VN** (vùng UTC còn nằm ở ngày hôm trước), vào `/manager/waste` chọn `from = to = hôm nay`. Dòng đó **phải** xuất hiện. Đối chiếu chéo: tổng ở `/manager/waste` = thẻ "Hao hụt hôm nay" trên dashboard = `/barista/waste` scope TODAY.

**(d) `summary.todayWaste` null → EL ra rỗng.**
→ **Kiểm:** đăng nhập manager của chi nhánh chưa có hao hụt nào. Thẻ phải hiện `0 ₫`, không được trống.

**(e) Đếm không khớp màn xử lý.** Thẻ dashboard, bảng dashboard và bảng `/manager/menu-block` cùng gọi `getOpenRequests` nên phải luôn khớp. Ba con số, một truy vấn — lệch nghĩa là có ai đó thêm điều kiện lọc ở một phía.

---

## 7.1. Contract dữ liệu & hiệu năng

- Dashboard thêm 2 query theo request manager: `getOpenRequests(branchId)` và `getWasteLogs(branchId, todayFromUtc, todayToUtc)`. Không thêm query vào sidebar hoặc filter chung.
- `/manager/waste` cố ý chạy 2 query hao hụt: một query toàn khoảng để tổng hợp, một query phân trang cho bảng. Đây là trade-off đúng để tránh R3.
- `MAX_DAYS = 92` là chốt hiệu năng vì `summarize()` nạp toàn khoảng vào RAM. Nếu sau này cần xem 1 năm, phải thêm DAO aggregate (`SUM`, `COUNT`, `GROUP BY`) thay vì tăng số ngày.
- `WasteSummary` là hàm thuần và bỏ qua log `VOIDED` qua `!log.isActive()`. Bảng vẫn có thể xem `VOIDED` khi filter status, nhưng tổng chi phí không tính các dòng đã huỷ.
- `Range` dùng ngày VN cho input/label, UTC cho query. Không render `fromUtc`/`toUtc` ra giao diện.

---

## 7.2. Rollback nhanh

Nếu cần rút tính năng khi gần demo:

1. Revert riêng Bước 7 trước: xoá sidebar link, `WasteReportServlet`, `manager/waste.jsp`. Dashboard vẫn build nếu chưa bấm thẻ `/manager/waste`; nếu muốn sạch UX thì bỏ thẻ "Hao hụt hôm nay".
2. Nếu dashboard bị chậm hoặc lỗi dữ liệu, revert Bước 4-5. Bước 1-3 có thể giữ lại vì là refactor/bugfix độc lập và đã có test.
3. Không rollback bằng `git reset --hard` khi worktree đang có thay đổi người khác. Dùng `git diff -- <file>` rồi revert từng file thuộc kế hoạch.

---

## 8. Ngoài phạm vi — ghi lại để không quên

**1 · Cảnh báo "quên bấm Tan ca" bắn nhầm vào ca đang làm dở.**
`MonthlyAttendanceRow.isOpen()` = `checkIn != null && checkOut == null`, **không xét ngày** → ca hôm nay đang làm bị đếm là quên tan ca. Màn `/barista/shift` hiện đồng thời "Đang trong ca từ 14:00" và "Có 1 ca bạn quên bấm Tan ca". Bảng chi tiết cũng gắn nhãn "Chưa tan ca" cho ca hiện tại.

Bề mặt nhỏ, đúng 4 chỗ: `MonthlyWorkSummary`, `AttendanceService:136`, `shift.jsp:70,72`, 1 test.
Hướng sửa: overload `summarize(rows, LocalDate today)` thuần, so `workDate` với `LocalDate.now(BusinessDay.VN_ZONE)`.
⚠️ Helper `open()` trong `AttendanceMonthlySummaryTest` **không set `workDate`** (null) → mọi so sánh phải null-safe. Ca đêm vắt qua nửa đêm là edge case riêng.

**2 · `MenuBlockRequest.isOverdue()` dùng giờ JVM** (dòng 89-91: `LocalDateTime.now()`) so với `backInEta` cũng do `request86` ghi bằng giờ JVM → hai bên **nhất quán**, sửa một phía sẽ tạo lệch. Không sửa. Nhưng khi kiểm badge "Quá hạn", phải xác nhận timezone JVM chạy Tomcat là `Asia/Ho_Chi_Minh`, nếu không badge lệch và dễ bị quy oan cho code mới.

**3 · Dữ liệu seed lệch múi giờ.** `sql/database.sql` seed `CheckInAt=07:00` theo giờ VN trong khi app ghi UTC (`SYSUTCDATETIME()`). Ca seeded tan ca qua app ra **0 giờ công**. Không phải bug `clockIn`/`clockOut`.

---

## 9. Thứ tự merge

Ba bước đầu (CSS · `BusinessDay` · tách `WasteSummary`) **độc lập với tính năng mới** — có thể merge sớm, không chờ phần còn lại.

Đề xuất chia PR/commit:

1. `ui: ổn định stat card layout` — chỉ CSS + kiểm mắt.
2. `common: thêm range ngày VN sang UTC` — `BusinessDay` + test.
3. `shared: tách WasteSummary khỏi barista service` — class shared + cập nhật call site + test cũ xanh.
4. `manager: thêm tín hiệu tạm hết và hao hụt trên dashboard` — service/servlet/JSP dashboard + test signals.
5. `manager: thêm report hao hụt theo khoảng ngày` — service report, servlet, JSP, sidebar + test range.

Điểm dừng demo an toàn: hết PR/commit 4 thì manager đã thấy cảnh báo trên dashboard; report chi tiết `/manager/waste` có thể vào sau mà không phá luồng xử lý tạm hết.
