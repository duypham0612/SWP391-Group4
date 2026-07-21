# PLAN — Quầy pha chế: gom nhóm bàn + bỏ phân tích thời gian

> Màn: `/barista/kds` · Mức thay đổi: **L3, nhiều file** · Không đổi schema DB.
> Trạng thái: CODE XONG — `mvn test` xanh; cần verify tay trên Tomcat với dữ liệu thật (mục 12).

## 0. Quyết định đã chốt

| # | Quyết định | Giá trị |
|---|---|---|
| 1 | Bỏ thời gian | **Bỏ hết** SLA label/màu, cao điểm, ước tính, "chờ lâu nhất", "trễ giờ" |
| 2 | Khóa gom — dine-in | **`TableSessionId`** (gộp mọi lượt gọi của cùng lượt khách ngồi bàn) |
| 2 | Khóa gom — mang đi/giao | **`OrderId`** (1 đơn = 1 lượt gọi của 1 người) |
| 3 | Bố cục | **1 danh sách khối nhóm** (bỏ 3 lane trạng thái), mỗi ly có nút riêng |
| 4 | Thứ tự nhóm | Theo `MIN(CreatedAt)` — bàn/đơn có lượt gọi **sớm nhất lên đầu**, KHÔNG ưu tiên chen ngang |

**Điểm còn để ngỏ (F.4) — bạn chỉnh khi làm:** món `BLOCKED` để trong **drawer "Cần xử lý"** (mặc định đề xuất) hay hiện mờ trong khối bàn?
- [x] Giữ ở drawer (đề xuất — ít thay đổi, nhất quán hành vi hiện tại)
- [ ] Đưa vào khối bàn (phải thêm nhánh render + vẫn để nút bỏ chặn)

---

## 1. DAO — `dao/shared/OrderItemDao.java`

### 1a. Thêm `TableSessionId` vào SELECT
`SELECT` (dòng ~16) hiện đã join `sales.TableSession ts`. Thêm cột:
```
... o.OrderType, o.CreatedAt AS OrderCreatedAt, o.PickupCode, o.TableSessionId, dt.TableNumber, ts.Status AS SessionStatus, ...
```

### 1b. Map cột mới trong `map()`
Tìm `map(ResultSet rs)` (cuối file), thêm:
```java
int tsId = rs.getInt("TableSessionId");
it.setTableSessionId(rs.wasNull() ? null : tsId);
```

### 1c. Bỏ ưu tiên chen ngang khỏi ORDER BY workbench (dòng ~101)
```java
// TỪ:
"ORDER BY CASE WHEN oi.RemakeCount>0 THEN 0 ELSE 1 END, oi.Priority DESC, o.CreatedAt, oi.OrderItemId";
// THÀNH (FIFO thuần theo lượt gọi):
"ORDER BY o.CreatedAt, oi.OrderItemId";
```
> Lưu ý: query trả list phẳng FIFO. Việc **gom nhóm + sắp nhóm theo MIN(createdAt)** làm ở tầng Java (mục 3),
> KHÔNG cần `GROUP BY` trong SQL. FIFO của item trong list giúp thứ tự trong-nhóm cũng ổn định.

- [x] 1a done  - [x] 1b done  - [x] 1c done

---

## 2. Model — `model/OrderItem.java`

### 2a. Thêm field (thêm mới, không phá gì)
```java
private Integer tableSessionId;   // sales.Orders.TableSessionId — khóa gom nhóm dine-in
public Integer getTableSessionId() { return tableSessionId; }
public void setTableSessionId(Integer v) { this.tableSessionId = v; }
```

### 2b. GIỮ NGUYÊN các method SLA/thời gian
`getSlaLabel/getSlaTier/getWaitProgressLabel/getServeTier/isStaleReady/getServeWaitDisplay/prepSeconds...`
**Không xóa** — màn **Pickup** (`pickup.jsp`, `PickupService`) và các test còn dùng. Ở KDS ta chỉ **ngừng render**,
không đụng model. (Grep `getSlaTier`/`getServeTier` trước khi định xóa bất cứ cái gì.)

### 2c. (Tùy chọn) helper khóa gom — cho gọn code servlet
```java
/** Khóa gom KDS: dine-in gom theo lượt bàn; còn lại gom theo đơn. */
public String getBrewGroupKey() {
    return "DINE_IN".equals(orderType) && tableSessionId != null
            ? "T" + tableSessionId : "O" + orderId;
}
```

- [x] 2a done  - [x] 2b xác nhận không xóa  - [x] 2c (tùy chọn)

---

## 3. Model gom nhóm mới — `model/BrewGroup.java`

> Theo đúng tiền lệ `StaleOrderGroup.java` / `KdsTicket.java` (gom ở tầng model, test được, không SQL).

```java
package com.cafe.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Nhóm pha theo lượt bàn (dine-in) hoặc theo đơn (mang đi/giao). Xếp theo lượt gọi sớm nhất. */
public class BrewGroup {
    private final String key;
    private final List<OrderItem> items = new ArrayList<>();
    private LocalDateTime earliestCreatedAt;   // mốc sắp nhóm (MIN CreatedAt)

    private BrewGroup(String key) { this.key = key; }

    public String getKey() { return key; }
    public List<OrderItem> getItems() { return items; }

    private void add(OrderItem it) {
        items.add(it);
        LocalDateTime c = it.getOrderCreatedAt();
        if (c != null && (earliestCreatedAt == null || c.isBefore(earliestCreatedAt))) earliestCreatedAt = c;
    }

    // ---- Hiển thị header ----
    public OrderItem head() { return items.get(0); }
    public boolean isDineIn() { return "DINE_IN".equals(head().getOrderType()); }
    public String getTableNumber() { return head().getTableNumber(); }   // đã gồm chữ "Bàn"
    public String getPickupCode()  { return head().getPickupCode(); }
    public String getOrderTypeLabel() { return head().getOrderTypeLabel(); }
    public int getOrderId() { return head().getOrderId(); }

    /** Tổng ly của nhóm (mỗi dòng có Quantity riêng). */
    public int getCups() {
        int t = 0; for (OrderItem i : items) t += i.getQuantity(); return t;
    }
    /** Breakdown "1 chờ · 1 pha · 1 xong" cho header. */
    public int getWaitingCups() { return cupsOf("WAITING"); }
    public int getMakingCups()  { return cupsOf("MAKING"); }
    public int getReadyCups()   { return cupsOf("READY"); }
    private int cupsOf(String st) {
        int t = 0; for (OrderItem i : items) if (st.equals(i.getStatus())) t += i.getQuantity(); return t;
    }

    /**
     * Gom danh sách item ĐÃ SẮP FIFO thành các nhóm, giữ thứ tự nhóm theo lượt gọi sớm nhất.
     * Vì input đã ORDER BY CreatedAt, LinkedHashMap giữ đúng thứ tự xuất hiện đầu tiên = FIFO nhóm.
     * Trong mỗi nhóm, item giữ nguyên thứ tự FIFO của input.
     */
    public static List<BrewGroup> from(List<OrderItem> fifoItems) {
        Map<String, BrewGroup> map = new LinkedHashMap<>();
        for (OrderItem it : fifoItems) {
            map.computeIfAbsent(it.getBrewGroupKey(), BrewGroup::new).add(it);
        }
        return new ArrayList<>(map.values());
    }
}
```

> **Sắp trong-nhóm theo trạng thái?** Mặc định giữ FIFO item (dễ đọc theo lúc gọi). Nếu muốn gom
> "chờ → đang pha → xong" trong khối thì sort `items` theo thứ tự status; cân nhắc sau khi xem thực tế.

- [x] Tạo `BrewGroup.java`  - [x] (tùy chọn) viết `BrewGroupTest` như `StaleOrderGroup` có test

---

## 4. Servlet — `controller/barista/KdsServlet.java` → `loadBoard()`

### 4a. Bỏ toàn bộ tính toán thời gian (dòng ~166–224)
Xóa/không set các attribute sau:
- `overdue`/`overdueCount`, vòng lặp tính `oldest`, `totalPrepSeconds`, `activeBaristas`
- `peakMode`, `peakQueueCups`, `peakEstLastMin`, `estimateLastWaitSeconds`, vòng set `seqNo`
- `oldestDisplay`, `oldestLocation`, `oldestProduct`, `oldestQty`, `oldestTier`

### 4b. Dựng danh sách nhóm thay cho 3 lane
```java
Map<String, List<OrderItem>> board = service.getWorkbenchBoard(branchId, dayStart);
List<OrderItem> waiting    = board.get("waiting");
List<OrderItem> inProgress = board.get("inProgress");
List<OrderItem> ready      = board.get("ready");
List<OrderItem> blocked    = board.get("blocked");

// Gộp 3 trạng thái đang-vận-hành thành 1 list, rồi gom nhóm.
// splitWorkbench đã tách theo status; ta cần lại list phẳng FIFO → dùng thẳng getBaristaWorkbench,
// hoặc merge 3 list theo CreatedAt. Đơn giản nhất: thêm 1 API trả list phẳng (mục 5b).
List<OrderItem> active = /* waiting+inProgress+ready, đã FIFO */;
List<com.cafe.model.BrewGroup> groups = com.cafe.model.BrewGroup.from(active);
req.setAttribute("brewGroups", groups);

// Đếm tổng (số việc, không phải thời gian) — cho dải tóm tắt gọn:
req.setAttribute("waitingCount", cups(waiting));
req.setAttribute("makingCount",  cups(inProgress));
req.setAttribute("readyCount",   cups(ready));
req.setAttribute("tableCount",   groups.size());   // số nhóm bàn/đơn đang chờ
```
> **Cách lấy list phẳng FIFO:** `splitWorkbench` phân theo status làm mất thứ tự trộn.
> Gợi ý: thêm `KdsService.getWorkbenchGroups(branchId, dayStart)` gọi thẳng
> `orderService.getBaristaWorkbench(...)` (đã FIFO, gồm WAITING/MAKING/READY/BLOCKED),
> **lọc bỏ BLOCKED** (để ra drawer) rồi `BrewGroup.from(...)`. Xem 5b.

### 4c. GIỮ NGUYÊN phần drawer + blocked + stale (dòng ~194–207)
`blockedItems`, `staleGroups`, `staleOrderCount`, `staleHasItems`, `staleHiddenOrders`, `staleCount`
→ không đụng. Đây là ngoại lệ theo NGÀY, không phải phân tích thời gian.

### 4d. GIỮ NGUYÊN doPost + các action
`start / markReady / returnQueue / reportIssue / unblock / remake` và WHERE-guard chống race —
**không đổi một dòng nào**. Ta chỉ đổi cách *đọc/hiển thị*, không đổi cách *ghi*.

- [x] 4a xóa tính thời gian  - [x] 4b dựng brewGroups  - [x] 4c giữ drawer  - [x] 4d xác nhận doPost nguyên vẹn

---

## 5. Service — `service/barista/KdsService.java`

### 5a. Bỏ code cao điểm (không còn dùng)
- Xóa `isPeak(...)` và `estimateLastWaitSeconds(...)` (nếu chắc không nơi nào khác gọi — grep trước).
- Nếu `KdsPeakTest` còn ref → xóa test đó (mục 7).

### 5b. Thêm API trả nhóm (khuyến nghị — giữ servlet mỏng)
```java
/** Nhóm pha của ngày kinh doanh hiện tại (WAITING/MAKING/READY), đã bỏ BLOCKED (ra drawer). */
public List<BrewGroup> getWorkbenchGroups(int branchId, LocalDateTime dayStartUtc) throws SQLException {
    List<OrderItem> flat = orderService.getBaristaWorkbench(branchId, dayStartUtc);
    List<OrderItem> active = new ArrayList<>();
    for (OrderItem it : flat) if (!"BLOCKED".equals(it.getStatus())) active.add(it);
    return BrewGroup.from(active);
}
/** Món BLOCKED riêng cho drawer "Cần xử lý". */
public List<OrderItem> getBlockedItems(int branchId, LocalDateTime dayStartUtc) throws SQLException {
    List<OrderItem> flat = orderService.getBaristaWorkbench(branchId, dayStartUtc);
    List<OrderItem> out = new ArrayList<>();
    for (OrderItem it : flat) if ("BLOCKED".equals(it.getStatus())) out.add(it);
    return out;
}
```
> Giữ `getWorkbenchBoard`/`splitWorkbench` cũ nếu dashboard/test còn dùng (grep `getWorkbenchBoard`).

- [x] 5a bỏ peak (sau khi grep)  - [x] 5b thêm API nhóm

> Đã dùng `WorkbenchSnapshot` để đọc list phẳng đúng một lần cho mỗi render, sau đó tách
> `brewGroups` và `blockedItems` từ cùng snapshot; tránh lệch trạng thái giữa hai query polling.

---

## 6. View — viết lại phần render

### 6a. `barista/kds.jsp`
- [x] Xóa chip lọc **"Trễ giờ"** (`#kdsUrgencyFilter`, dòng 17).
- [x] Giữ toolbar còn lại (owner filter, Quầy & Loại đơn, connection status).
- [x] Container `#kdsBoard` giữ nguyên id/endpoint (JS polling dựa vào đây).
- [x] Modal issue/remake/unblock giữ nguyên.

### 6b. `barista/kds_cards.jsp` (partial được polling render lại)
- [x] Xóa banner cao điểm (`c:if peakMode`, dòng 17–22).
- [x] Summary bar: **xóa ô "Trễ giờ" và "Chờ lâu nhất"**; giữ Chờ pha / Đang pha / Sẵn sàng; thêm ô **"Bàn đang chờ" = ${tableCount}**. Bỏ class động `kds-summary--${oldestTier}` (dùng class tĩnh).
- [x] **Xóa cụm lane tabs + 3 cột `kds-columns`** (dòng 81–120).
- [x] Thay bằng **1 danh sách khối**:
```jsp
<div class="kds-groups">
  <c:choose>
    <c:when test="${empty brewGroups}">
      <div class="kds-col__empty"><span>✓</span> Không còn nhóm nào chờ pha</div>
    </c:when>
    <c:otherwise>
      <c:forEach var="grp" items="${brewGroups}" varStatus="st">
        <section class="kds-group" data-group-key="${grp.key}">
          <header class="kds-group__head">
            <span class="kds-group__seq">${st.index + 1}</span>
            <strong class="kds-group__where">
              <c:choose>
                <c:when test="${grp.dineIn and not empty grp.tableNumber}"><c:out value="${grp.tableNumber}"/></c:when>
                <c:otherwise><c:if test="${not empty grp.pickupCode}"><span class="kds-code"><c:out value="${grp.pickupCode}"/></span> · </c:if>${grp.orderTypeLabel}</c:otherwise>
              </c:choose>
            </strong>
            <span class="kds-group__count">${grp.cups} ly
              <small>(${grp.waitingCups} chờ · ${grp.makingCups} pha · ${grp.readyCups} xong)</small>
            </span>
          </header>
          <div class="kds-group__body">
            <c:forEach var="item" items="${grp.items}">
              <c:set var="cardItem" value="${item}" scope="request" />
              <c:choose>
                <c:when test="${item.status eq 'WAITING'}"><jsp:include page="_kdsWaitingCard.jsp" /></c:when>
                <c:when test="${item.status eq 'MAKING'}"><jsp:include page="_kdsProgressCard.jsp" /></c:when>
                <c:otherwise><jsp:include page="_kdsReadyCard.jsp" /></c:otherwise>
              </c:choose>
            </c:forEach>
          </div>
        </section>
      </c:forEach>
    </c:otherwise>
  </c:choose>
  <div class="kds-filter-empty" hidden>Không có nhóm phù hợp bộ lọc.</div>
</div>
```
- [x] Giữ nguyên drawer "Cần xử lý" (blocked + stale) phía trên (dòng 37–79).

### 6c. `barista/_kdsCardHeader.jsp`
- [x] Xóa nhánh `slaLabel`/`seqNo`/`kds-sla` (dòng 8–13) — bỏ badge thời gian trên card.
- [x] Bỏ `${cardItem.waitProgressLabel}` ở meta-row (dòng 31); giữ nơi giao + `orderTypeLabel` + `#orderId`.
- [ ] (Tùy chọn) bỏ luôn `${cardItem.createdDisplay}` nếu muốn sạch hẳn thời gian — cân nhắc giữ giờ vào đơn vì hữu ích, không phải "phân tích".

### 6d. `_kdsWaitingCard.jsp` / `_kdsProgressCard.jsp`
- [x] Đổi class động `kds-${peakMode ? 'ok' : cardItem.slaTier}` → class tĩnh `kds-card` (bỏ màu SLA).
- [x] Bỏ attr `data-sla-tier` (JS không dùng nữa sau mục 7). Giữ `data-owner/data-station/data-order-type` cho bộ lọc.
- [x] Progress card: bỏ `· đang pha ${makingDisplay}` (thời gian); giữ "Pha bởi ... · nhận lúc ..." nếu muốn — "nhận lúc" là mốc sự kiện, không phải đếm ngược. Tùy bạn.
- [x] Giữ nguyên **các form/nút hành động** (Nhận pha / Đã pha xong / Trả lại / Báo sự cố).

### 6e. `_kdsReadyCard.jsp`
- [x] Xóa badge `Chờ nhận ${serveWaitDisplay}` + nhánh `staleReady` (dòng 8–13) — đây là SLA giao.
- [x] Class `is-stale` bỏ. Giữ facts "Pha bởi / Bắt đầu / Hoàn thành" nếu muốn (mốc sự kiện) hoặc rút gọn.
- [x] Giữ nút "Làm lại món".

- [x] 6a  - [x] 6b  - [x] 6c  - [x] 6d  - [x] 6e

---

## 7. JS — `assets/js/kds-board.js` (phần ẩn tốn công — đọc kỹ)

Cấu trúc hiện tại bám chặt **lane + tier**. Cần cắt:
- [x] **Bỏ lane tabs & lane counts**: `setActiveLane`, `updateLaneCounts`, mọi ref `data-lane`, `.kds-col__count`, `[data-lane-tab]`. Bố cục mới không có tab.
- [x] **Bỏ urgency filter**: xóa khỏi `DEFAULT_FILTERS`, `readFilters`, `applyFilters` (`urgencyMatches`), và nút `#kdsUrgencyFilter`.
- [x] **Bỏ tier-diff live notice**: `readTiers`, so `data-sla-tier` để đếm "urgent" (dòng ~71–74, 121–122). Không còn tier.
- [x] **GIỮ**: polling/fetch partial thay `#kdsBoard` innerHTML; owner/station/orderType filter chạy trên `[data-kds-item-id]`.
- [x] **Sửa filter theo khối**: khi ẩn item theo bộ lọc, nếu cả khối `.kds-group` không còn item hiện → ẩn luôn khối (tránh header bàn trống). Thêm vòng: sau `applyFilters`, mỗi `.kds-group` set `hidden = !groupHasVisibleCard`.
- [x] Kiểm mọi `getElementById` đã xóa (vd `kdsStationFilter` vẫn còn, `kdsUrgencyFilter` đã xóa) để không `null` → lỗi JS.

> Rủi ro cao nhất của cả task nằm ở file này. Làm sau khi JSP ổn, test kỹ polling + 3 bộ lọc còn lại.

- [ ] 7 done + test polling/filters

---

## 8. CSS — `assets/css/cafe-theme.css` (hoặc file KDS riêng)

- [x] Thêm style khối: `.kds-groups`, `.kds-group`, `.kds-group__head`, `.kds-group__seq` (badge số thứ tự tròn),
      `.kds-group__body` (chứa các card ly).
- [x] Không đụng biến `--st-*` dùng chung. Card ly bỏ nền màu SLA → nền `--cream` trung tính.
- [ ] Dọn class thừa nếu muốn: `.kds-sla*`, `.kds-peak`, `.kds-columns`, lane tab — có thể để lại vô hại, xóa sau.

---

## 9. Test — `src/test/java/com/cafe/...`

- [x] `KdsPeakTest` → **xóa** (peak đã bỏ) nếu 5a xóa `isPeak/estimateLastWaitSeconds`.
- [x] `BaristaWorkbenchItemTest` → rà: phần assert SLA tier/label không còn liên quan KDS; giữ nếu test model (model vẫn còn method), sửa nếu test hành vi KDS.
- [x] `KdsBoardTest` → nếu test `getQueueBoard`/`splitWorkbench` còn giữ thì OK; nếu test board 3-lane KDS thì viết lại theo `BrewGroup`.
- [x] **Thêm** `BrewGroupTest`: (a) dine-in 2 đơn cùng `tableSessionId` → 1 nhóm, cups cộng dồn; (b) 2 mang đi khác `orderId` → 2 nhóm; (c) thứ tự nhóm theo `MIN(createdAt)`; (d) item BLOCKED không lọt vào (được lọc ở service).
- [x] `mvn test` xanh trước khi coi là xong.

---

## 10. Edge case phải đúng (đối chiếu khi verify)

1. 1 lượt bàn có ly WAITING + MAKING + READY → 1 khối, mỗi ly nút riêng, header "(x chờ · y pha · z xong)".
2. Mang đi không có `tableSessionId` → gom theo `orderId`, header hiện `pickupCode` + "Mang đi".
3. Cùng 1 bàn gọi 2 lượt (2 Order, cùng TableSession) → **1 khối** (đúng ý "lượt bàn").
4. Bàn đã pha xong hết (mọi ly READY) → khối vẫn hiện tới khi thu ngân SERVED (rời khỏi query workbench). OK.
5. Race nhiều barista → WHERE-guard + `affected` ở Service giữ nguyên, không đổi.
6. F.4 blocked: theo lựa chọn ở mục 0.
7. Session hết hạn / ngoài ca → `BaristaShift.guardWrite` + Filter giữ nguyên.

---

## 11. Thứ tự thực hiện đề xuất

1. DAO (1) → Model (2) → `BrewGroup` (3) + `BrewGroupTest` → chạy test model.
2. Service (5) → Servlet (4). Deploy thử: board render nhóm được (kể cả khi JSP/JS chưa đẹp).
3. JSP (6) từng fragment → xem trực tiếp.
4. JS (7) → test polling + filter.
5. CSS (8).
6. Test (9) `mvn test` xanh.
7. Verify tay (mục 12).

Sau **mỗi** bước code: self-review nhẹ (correctness · jakarta/PreparedStatement · đóng JDBC · pattern DAO · không tin client) theo Agent Loop ⑤.

---

## 12. Verify tay (chưa có test servlet)

Deploy Tomcat → tạo dữ liệu:
- Bàn A: 1 đơn 2 ly (lúc T0).
- Bàn A: thêm 1 đơn 1 ly (lúc T2) — cùng TableSession.
- Mang đi: 1 đơn 1 ly (lúc T1).
- Bàn B: 1 đơn 1 ly (lúc T3).

Kỳ vọng:
- [ ] 3 khối: Bàn A (3 ly, gộp 2 đơn) → Mang đi → Bàn B, theo thứ tự T0 < T1 < T3 (MIN createdAt).
- [ ] Không còn chữ/màu "còn/trễ X phút", không "chờ lâu nhất", không "trễ giờ".
- [ ] Nút từng ly: Nhận pha → Đã pha xong (chọn nơi đặt) → chuyển trạng thái trong khối đúng.
- [ ] Bộ lọc Món của tôi / Chưa nhận / Quầy / Loại đơn còn chạy; khối trống bị ẩn khi lọc.
- [ ] Báo sự cố (hết NL/máy) → món sang drawer "Cần xử lý"; đơn treo ngày trước vẫn ở drawer.
- [ ] Polling ~ tự làm mới không mất bộ lọc/khối đang xem.
