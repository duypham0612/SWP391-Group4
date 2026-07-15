# BARISTA_COMMIT_PLAN.md — Lộ trình 10 ngày × 4 commit (role Barista, DEV4)

> Mục tiêu: mỗi ngày thêm **4 commit việc THẬT** cho role Barista, giữ nguyên toàn bộ commit cũ.
> Nhánh làm việc: `minhnhat`. **Không** reset/amend/force — chỉ **thêm** commit.

## ⚠ QUY TẮC TÀI KHOẢN GIT (BẮT BUỘC — mọi session)

- **CHỈ được thao tác Git/GitHub trên tài khoản `nhatlmhe160091`** (identity dự án: `Lê Minh Nhật <nhatlmhe160091@fpt.edu.vn>`).
- **TRƯỚC mọi lần push/PR/thao tác GitHub**, xác minh tài khoản active:
  ```
  gh auth status --active        # phải là nhatlmhe160091
  gh auth switch --user nhatlmhe160091   # nếu chưa đúng → chuyển về
  ```
- **KHÔNG** dùng tài khoản `17tuanphamanh` cho bất kỳ thao tác nào trên repo này (không có quyền ghi → 403; và sai identity dự án).
- Không đổi `git config user.name`/`user.email` khỏi `Lê Minh Nhật <nhatlmhe160091@fpt.edu.vn>`.
- Không `--force`, không `--no-verify`, không ghi nguồn AI trong commit/PR.

## Cách dùng (mỗi ngày)

1. Bảo agent: **"Làm ngày N trong `docs/BARISTA_COMMIT_PLAN.md`"**.
2. Agent thực hiện lần lượt 4 commit của ngày đó, mỗi commit:
   - viết/sửa code thật → `mvn test` (hoặc test riêng) PASS → `git add` đúng file → `git commit`.
3. Cuối ngày agent tick `[x]` các commit đã xong ở dưới + ghi 1 dòng vào `docs/PROGRESS.md`.

## Nguyên tắc bất biến (để bài đứng vững khi thầy soi history)

- **Mỗi commit = một thay đổi thật**, biên dịch được, test liên quan PASS. KHÔNG commit rỗng/giả.
- Tôn trọng kiến trúc dự án: `controller → service → dao`, JSP **chỉ JSTL/EL** (cấm scriptlet), tồn kho **chỉ đổi qua `InventoryService.applyTxn`/ledger**, `branchId` lấy từ session/`BranchContext`.
- Commit message: conventional tiếng Việt — `feat(barista): …`, `test(barista): …`, `fix(barista): …`, `docs(barista): …`. Không ghi nguồn AI.
- **Không đổi schema** trừ khi ngày đó ghi rõ + có file `sql/migration_*.sql` idempotent.
- Ưu tiên **additive**: thêm method/DAO/JSP mới, tránh sửa hành vi đang chạy của role khác.

## Ghi chú về test

- Test **logic thuần** (không DB) chạy bằng `mvn test -Dtest='<Class>'` — luôn làm được.
- Test **integration** (đánh dấu ⚙️DB) cần SQL Server `CafeChain` cho test. Nếu chưa dựng được DB test → commit phần code + test skeleton `@Disabled("cần DB test")` kèm lý do, KHÔNG bỏ hẳn.

---

## Ngày 1 — Widget "Hao hụt hôm nay" trên Dashboard Barista
Tái dùng `WasteService.WasteSummary` (đã có test) để hiện tổng quan hao hụt ngay trên `/barista/dashboard`.

- [ ] **C1** `feat(barista): WasteService.getTodayWasteSummary(branchId) gom hao hụt trong ngày` — thêm method wrap `getWasteLogs(branchId, WasteScope.today())` + `summarize(...)`.
- [ ] **C2** `feat(barista): BaristaDashboardServlet nạp wasteSummary vào request` — set attribute `wasteSummary`.
- [ ] **C3** `feat(barista): dashboard.jsp thẻ Hao hụt hôm nay (tổng chi phí, top nguyên liệu, số làm lại)` — thẻ JSTL, dùng `_statusBadge`/style chung.
- [ ] **C4** `test(barista): WasteService.getTodayWasteSummary — logs rỗng/nhiều loại, tổng khớp` — test logic (mock list qua summarize) hoặc mở rộng `WasteSummaryTest`.
- **Verify:** `mvn test -Dtest='WasteSummaryTest'` PASS; deploy xem thẻ hiện đúng số.

## Ngày 2 — Bảng "Ly đã pha trong ca" (brew history) ở màn Handover
Cho barista xem danh sách món đã hoàn tất (READY/SERVED) hôm nay khi bàn giao ca.

- [ ] **C1** `feat(barista): OrderItemDao.findBrewedToday(conn, branchId, fromUtc, toUtc)` — SQL đọc OrderItem có `DoneAt` trong khoảng, join tên món (PreparedStatement, nhận `Connection`).
- [ ] **C2** `feat(barista): HandoverService.getBrewHistory(branchId) trả danh sách đã pha` — mở connection đọc, đóng thủ công (pattern đọc độc lập).
- [ ] **C3** `feat(barista): handover.jsp thêm bảng Ly đã pha hôm nay (giờ xong, món, SL)` — bảng JSTL, empty-state khi chưa có.
- [ ] **C4** ⚙️DB `test(barista): HandoverService.getBrewHistory lọc đúng theo ngày/chi nhánh` — hoặc `@Disabled` nếu chưa có DB test.
- **Verify:** deploy, pha 1 món tới READY → thấy trong bảng; branch khác không lẫn.

## Ngày 3 — KPI cá nhân của barista trên Dashboard
"Hôm nay bạn đã pha X ly · lead time TB Y" — lấy theo `userId` từ session.

- [ ] **C1** `feat(barista): OrderItemDao.leadTimeStats overload lọc theo userId (người pha)` — thêm tham số `Integer userId`, WHERE `PreparedBy = ?` khi có.
- [ ] **C2** `feat(barista): HandoverService.getMyKpi(branchId, userId)` — tái dùng `HandoverKpi`.
- [ ] **C3** `feat(barista): dashboard.jsp thẻ KPI cá nhân (số ly + lead time của tôi)`.
- [ ] **C4** `test(barista): HandoverKpi.getAvgLeadDisplay — 0s, <60s, >60s, -1 (chưa có)` — test logic thuần.
- **Verify:** `mvn test`; deploy so KPI cá nhân ≤ KPI toàn ca.

## Ngày 4 — 86 board: lịch sử báo hết + tự mở lại khi quá ETA
Nâng màn `/barista/eightysix`: xem món nào đang 86, ETA còn bao lâu, tự gợi ý mở lại khi ETA đã qua.

- [ ] **C1** `feat(barista): BranchMenuService.getExpired86(branchId) — món 86 có ETA đã qua` — đọc `BranchMenu` where Is86=1 và BackInEta < now.
- [ ] **C2** `feat(barista): EightySixServlet nạp danh sách 86 quá ETA (gợi ý mở lại)`.
- [ ] **C3** `feat(barista): eightysix.jsp banner "Món tới giờ mở lại" + nút mở nhanh` — reuse action `toggle86`.
- [ ] **C4** `test(barista): so sánh ETA quá hạn (logic thuần trên mốc thời gian)`.
- **Verify:** set 86 với ETA 1 phút → sau đó vào lại thấy gợi ý mở.

## Ngày 5 — Pickup board: đồng hồ SLA + cảnh báo chờ giao quá lâu
Món READY chờ giao quá ngưỡng → đổi màu/nhấp nháy (giống tier của KDS).

- [ ] **C1** `feat(barista): PickupService trả thời gian chờ giao mỗi món (readyAt→now)` — thêm field waitSeconds.
- [ ] **C2** `feat(barista): Constants ngưỡng SLA pickup (WARN/CRIT) + tier ở servlet`.
- [ ] **C3** `feat(barista): pickup_cards.jsp tô màu theo SLA + badge "chờ giao N phút"`.
- [ ] **C4** `test(barista): tier SLA pickup theo ngưỡng (ok/warn/crit)` — test logic thuần.
- **Verify:** `mvn test`; deploy để món chờ lâu chuyển màu.

## Ngày 6 — Prep: gợi ý sản lượng theo tồn + cảnh báo prep sắp hết
Nâng `PrepChecklistRow`: gợi ý số lượng nên pha dựa trên ngưỡng tồn PREPPED.

- [ ] **C1** `feat(barista): PrepService tính suggestedQty (ngưỡng - tồn hiện tại, làm tròn mẻ)`.
- [ ] **C2** `feat(barista): prep.jsp hiện "nên pha ~X" cạnh mỗi đồ pha sẵn dưới ngưỡng`.
- [ ] **C3** `feat(barista): dashboard.jsp badge "N đồ pha sẵn cần pha thêm"` (đếm từ checklist).
- [ ] **C4** `test(barista): tính suggestedQty (trên/dưới/bằng ngưỡng, âm→0)` — test logic thuần.
- **Verify:** `mvn test`; deploy so gợi ý với tồn thật.

## Ngày 7 — Recipe lookup: lọc theo danh mục + đánh dấu món hay pha
Nâng `/barista/recipe` (read-only) dễ tra hơn khi đông khách.

- [ ] **C1** `feat(barista): RecipeLookupServlet lọc theo categoryId (query param)`.
- [ ] **C2** `feat(barista): recipe.jsp thêm dropdown danh mục + giữ từ khoá tìm`.
- [ ] **C3** `feat(barista): recipe.jsp đánh dấu món top hôm nay (join số lần pha)` — reuse brew count.
- [ ] **C4** ⚙️DB `test(barista): lọc recipe theo danh mục` — hoặc `@Disabled` nếu chưa có DB test.
- **Verify:** deploy, chọn danh mục → chỉ hiện món thuộc danh mục.

## Ngày 8 — KDS: cảnh báo món quá hạn (client) + guard "oldest first"
Món chờ pha quá `KDS_CRIT_SECONDS` → nhấp nháy + đảm bảo sort cũ-nhất-trước.

- [ ] **C1** `feat(barista): kds_cards.jsp hiệu ứng nhấp nháy thẻ tier=crit` (CSS thuần, no JS nặng).
- [ ] **C2** `feat(barista): kds.jsp badge tổng số món quá hạn trên đầu queue`.
- [ ] **C3** `refactor(barista): KdsService đảm bảo queue sort theo Priority DESC, CreatedAt ASC` (xác nhận/siết lại).
- [ ] **C4** `test(barista): KdsServlet.tier phân ngưỡng ok/warn/crit` — mở `tier` thành package-private + test thuần.
- **Verify:** `mvn test`; deploy để món chờ lâu nhấp nháy.

## Ngày 9 — Dashboard: mini throughput theo giờ (số ly/giờ)
Biểu đồ nhỏ số ly pha xong theo từng giờ trong ngày (thanh CSS, không thư viện ngoài).

- [ ] **C1** `feat(barista): OrderItemDao.hourlyThroughput(conn, branchId, fromUtc, toUtc)` — GROUP BY giờ.
- [ ] **C2** `feat(barista): BaristaDashboardServlet nạp mảng throughput theo giờ`.
- [ ] **C3** `feat(barista): dashboard.jsp thanh bar CSS số ly/giờ` (JSTL forEach, style chung).
- [ ] **C4** ⚙️DB `test(barista): hourlyThroughput gom đúng theo giờ` — hoặc `@Disabled`.
- **Verify:** deploy, pha vài ly ở các giờ khác nhau → bar khớp.

## Ngày 10 — Bộ integration test Barista + chốt tài liệu
Đóng lại lộ trình bằng test tích hợp cho logic rủi ro + cập nhật docs.

- [ ] **C1** ⚙️DB `test(barista): KdsService state machine — WAITING→MAKING→READY→SERVED, chặn nhảy bậc`.
- [ ] **C2** ⚙️DB `test(barista): markReady 2 lần chỉ trừ tồn 1 lần (WHERE-guard, affected≠1)`.
- [ ] **C3** ⚙️DB `test(barista): PrepService.createBatch Contract #2 — RAW→PREPPED, không trừ thô 2 lần trên DB thật`.
- [ ] **C4** `docs(barista): tổng kết lộ trình 10 ngày vào PROGRESS.md + đánh dấu hoàn tất plan`.
- **Verify:** `mvn test` toàn bộ PASS (hoặc các @Disabled ghi rõ lý do); build WAR OK.

---

## Bảng theo dõi nhanh

| Ngày | Chủ đề | Trạng thái |
|---|---|---|
| 1 | Widget hao hụt hôm nay (dashboard) | ⬜ |
| 2 | Brew history (handover) | ⬜ |
| 3 | KPI cá nhân barista | ⬜ |
| 4 | 86 board — lịch sử + tự mở lại | ⬜ |
| 5 | Pickup — SLA timer | ⬜ |
| 6 | Prep — gợi ý sản lượng | ⬜ |
| 7 | Recipe lookup — lọc danh mục | ⬜ |
| 8 | KDS — cảnh báo quá hạn | ⬜ |
| 9 | Dashboard — throughput theo giờ | ⬜ |
| 10 | Integration test + chốt docs | ⬜ |

> Cập nhật `⬜ → 🟢` sau khi hoàn tất đủ 4 commit của ngày đó.
