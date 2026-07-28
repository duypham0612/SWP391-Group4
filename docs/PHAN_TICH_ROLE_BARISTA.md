# Phân tích role BARISTA

> Tài liệu mô tả toàn bộ nghiệp vụ của role Barista trong hệ thống CafeChain:
> làm được gì, không được làm gì, và **vì sao** lại thiết kế như vậy.

---

## Mục lục

- [1. Đọc nhanh trong 2 phút](#1-đọc-nhanh-trong-2-phút)
- [2. Bản đồ màn hình](#2-bản-đồ-màn-hình)
- [3. Ba cổng bảo vệ mọi thao tác ghi](#3-ba-cổng-bảo-vệ-mọi-thao-tác-ghi)
- [4. Miền A — Vòng đời ca làm việc](#4-miền-a--vòng-đời-ca-làm-việc)
- [5. Miền B — Sản xuất đơn hàng tại quầy](#5-miền-b--sản-xuất-đơn-hàng-tại-quầy)
- [6. Miền C — Tồn kho vận hành tại quầy](#6-miền-c--tồn-kho-vận-hành-tại-quầy)
- [7. Miền D — Khả dụng menu & công thức](#7-miền-d--khả-dụng-menu--công-thức)
- [8. Dashboard — lớp đọc xuyên miền](#8-dashboard--lớp-đọc-xuyên-miền)
- [9. Quy ước kỹ thuật dùng chung](#9-quy-ước-kỹ-thuật-dùng-chung)
- [10. Điểm cần cải thiện](#10-điểm-cần-cải-thiện)

---

## 1. Đọc nhanh trong 2 phút

### Barista làm gì?

Một câu: **nhận đơn từ hàng chờ, pha, báo xong** — cộng với các việc phụ trợ để quầy chạy được (pha sẵn nguyên liệu, ghi hao hụt, báo sự cố, bàn giao ca).

### Ranh giới quyền — nguyên tắc vàng

| Barista **ĐƯỢC** | Barista **KHÔNG ĐƯỢC** |
|---|---|
| Sửa **dữ liệu thực tế**: tồn kho, hao hụt, trạng thái món | Ra **quyết định thương mại**: khóa món khỏi menu, hủy đơn, hoàn tiền |
| Kiểm kê nguyên liệu tại quầy | Duyệt công, sửa giờ công, xếp lịch ca |
| Chặn / bỏ chặn món ở hàng chờ | Giao món cho khách (READY → SERVED) |
| **Đề nghị** báo hết món | **Tự** báo hết món |

> **Vì sao?** Một nguyên liệu nằm trong nhiều món. Khóa menu là quyết định ảnh hưởng doanh thu → phải có Quản lý duyệt.
> Nhưng nếu barista sửa **sổ kho** cho đúng thực tế (tồn = 0), thì món tự ẩn khỏi POS/QR như **hệ quả**, và tự hiện lại khi có tồn.
> Hai cơ chế này cố ý tách nhau: **sửa dữ liệu thật thì tự động, quyết định thương mại thì phải có người duyệt.**

### 4 miền nghiệp vụ

```
┌─ A. VÒNG ĐỜI CA ────────────────────────────────────────────┐
│  Vào ca → Trực ca → Lập bàn giao → Tan ca                    │
│  (gate cho tất cả các miền còn lại: ngoài ca = chỉ xem)      │
└──────────────────────────────────────────────────────────────┘
          │
          ▼
┌─ B. SẢN XUẤT ĐƠN ────┐   ┌─ C. TỒN KHO ──────┐   ┌─ D. MENU ─────┐
│  Hàng chờ, pha món    │──▶│  Pha sẵn          │──▶│  Báo hết món  │
│  Xử lý sự cố món      │   │  Hao hụt          │   │  Tra công thức│
└───────────────────────┘   │  Kiểm kê          │   └───────────────┘
          │                 └───────────────────┘           │
          └──────────────────────┬────────────────────────────┘
                                 ▼
                    ┌─ DASHBOARD (chỉ đọc) ─┐
                    └────────────────────────┘
```

### Ba quy tắc bất biến

1. **Tồn kho chỉ đổi qua sổ cái** (`InventoryService.applyTxn`) — không bao giờ xóa cứng; sửa/hủy đều bằng giao dịch bù.
2. **Mọi thao tác ghi qua 3 cổng**: đúng role → đúng action trong whitelist → đang trong ca.
3. **Không tin dữ liệu từ client** — mọi tham số lọc, phân trang, lý do, vị trí đặt món đều phải nằm trong danh sách cho phép; giá trị lạ rơi về mặc định thay vì gây lỗi 500.

---

## 2. Bản đồ màn hình

Barista có **8 màn**, tất cả dưới đường dẫn `/barista/`:

| # | Màn hình | Đường dẫn | Miền | Ghi |
|---|---|---|---|---|
| 1 | Bảng điều khiển ca | `/barista/dashboard` | — | ❌ chỉ đọc |
| 2 | **Quầy pha chế** | `/barista/kds` | B | ✅ |
| 3 | Pha sẵn nguyên liệu | `/barista/prep` | C | ✅ |
| 4 | Hao hụt & Làm lại | `/barista/waste` | C | ✅ |
| 5 | Báo hết món | `/barista/eightysix` | D | ✅ (đề nghị) |
| 6 | Tra cứu công thức | `/barista/recipe` | D | ❌ chỉ đọc |
| 7 | Ca làm của tôi | `/barista/shift` | A | ✅ (chấm công) |
| 8 | Bàn giao ca | `/barista/handover` | A | ✅ |

**Lưu ý về phạm vi:** kế hoạch ban đầu có màn "Ready/Pickup Board" cho barista, nhưng cuối cùng **không** làm bên barista — việc giao món cho khách nằm ở Thu ngân. Barista dừng lại ở trạng thái `READY`.

---

## 3. Ba cổng bảo vệ mọi thao tác ghi

Mỗi lần barista bấm một nút "ghi", request phải đi qua **3 cổng độc lập**:

```
POST /barista/xxx
   │
   ├─▶ CỔNG 1: RbacFilter          ── sai role ────▶ 403
   │   Đường dẫn /barista/* chỉ cho BARISTA (+ ADMIN đi xuyên).
   │   MANAGER cũng KHÔNG vào được — họ có màn riêng.
   │
   ├─▶ CỔNG 2: CsrfUtil            ── thiếu token ─▶ 403 CSRF
   │
   ├─▶ CỔNG 3a: BaristaWritePolicy ── action lạ ───▶ "Thao tác không hợp lệ"
   │   Whitelist action riêng cho từng màn.
   │
   ├─▶ CỔNG 3b: BaristaShift       ── ngoài ca ────▶ "Cần vào ca trước khi thao tác"
   │   Nguồn sự thật = chấm công thật (ShiftAssignment + Attendance),
   │   KHÔNG phải cờ trong session.
   │
   └─▶ Service (nghiệp vụ + transaction)
```

### Whitelist action theo màn

| Màn | Action được phép |
|---|---|
| Quầy pha chế | `start`, `startOrder`, `markReady`, `markOrderReady`, `reclaim`, `returnQueue`, `reportIssue`, `unblock`, `remake` |
| Pha sẵn | `createBatch`, `updateBatch`, `cancelBatch`, `writeOffExpired` |
| Hao hụt | `createIngredientWaste`, `update`, `void` |
| Báo hết món | `report86`, `askReopen` |
| Bàn giao | `create`, `createAndClockOut`, `acknowledge`, `claim`, `updateTask` |
| *(mọi màn)* | `clockIn`, `clockOut` |

> **Vì sao cần whitelist?** Không có nó, một POST tự soạn hoặc gõ sai tên action sẽ trở thành "request im lặng không làm gì" — người dùng tưởng đã lưu mà thực ra không có gì xảy ra.

### Hai kiểu phản hồi khi bị chặn

| Loại màn | Cách phản hồi |
|---|---|
| Trang thường | flash message + redirect (mẫu PRG) |
| Fragment AJAX (Quầy pha chế) | HTTP 403 + JSON + header `X-Barista-Write-Denied` |

> **Vì sao Quầy pha chế phải khác?** Nếu redirect, lệnh `fetch` sẽ đi theo 302, nhận về **nguyên cả trang** rồi nhồi luôn header/sidebar vào trong khung bảng — giao diện vỡ đúng lúc barista đang cần thao tác nhất.

---

## 4. Miền A — Vòng đời ca làm việc

**Gồm:** chấm công (`/barista/shift`) + bàn giao ca (`/barista/handover`)

**Vì sao gộp:** hai việc này **không tách rời được** — tan ca bị chặn bởi trạng thái bàn giao, và bàn giao chỉ lập được khi còn ca đang mở.

```
                    ┌─────────────────────────────────┐
                    │                                 │
   [NGOÀI CA] ──vào ca──▶ [TRONG CA] ──lập bàn giao──▶ [ĐÃ BÀN GIAO]
        ▲                     │                            │
        │                     │ ✗ còn ly đang pha          │
        └────────tan ca───────┴────────────────────────────┘
                     (phải qua CẢ HAI cổng)
```

### A1 · Vào ca

- Bấm nút "Vào ca" ở banner (hiện trên **mọi** màn barista).
- Thành công → mọi nút ghi ở 5 màn được mở khóa.
- Thất bại (ngoài cửa sổ chấm công, chưa xếp ca) → thông báo lỗi, không đổi gì.

### A2 · Tan ca — **hai cổng chặn**

```
Bấm "Tan ca"
   │
   ├─ Cổng 1: còn ly nào đang pha dưới tên mình không?
   │     CÓ → "Bạn còn N ly đang pha — bấm Xong hoặc Trả lại chờ cho từng ly"
   │           → đưa về /barista/kds?owner=mine (đã lọc sẵn món của mình)
   │
   ├─ Cổng 2: ca này đã lập bàn giao chưa?
   │     CHƯA → "Bạn cần bàn giao ca trước khi tan ca"
   │             → đưa sang /barista/handover
   │
   └─ Qua cả hai → chấm công tan ca ✓
```

> **Vì sao có Cổng 1?** Món đang pha mang tên người nhận (`BaristaId`). Cả nút "Xong" lẫn "Trả lại chờ" đều chỉ cho phép **chủ món** bấm.
> Nếu barista về mà chưa gỡ, ly đó **khóa cứng**: ca sau nhìn thấy nhưng không đụng được, khách ngồi đợi mãi.
> Chặn ở đây là chính đáng vì lối thoát nằm trọn trong tay barista (bấm Xong, hoặc Trả lại chờ — không đụng kho, chạy được cả với món chưa có công thức).

> **Vì sao có Cổng 2?** Không có nó, việc tồn của ca (còn mấy ly chưa pha, máy nào đang hỏng) biến mất khỏi hệ thống khi người ta về.

### A3 · Xem bảng công tháng của tôi

Chỉ đọc. Xem tổng hợp tháng + lịch sử từng ngày, lọc theo trạng thái (`Đã duyệt` / `Chờ duyệt` / `Từ chối` / `Đang mở` / `Vắng`), điều hướng tháng trước/sau.

> Barista chỉ **xem**. Duyệt công và sửa giờ thuộc Quản lý.

### A4 · Lập bàn giao ca

Khi mở màn bàn giao, hệ thống **điền sẵn giúp barista**:

| Nguồn gợi ý | Nội dung |
|---|---|
| Từ hàng chờ | "Hàng chờ còn 12 ly chưa pha", "5 ly đã pha xong đang chờ nhân viên nhận", "Món tạm dừng cần xử lý: 2 × Cold Brew (máy xay lỗi)" |
| Từ ca trước | Việc tồn chưa hoàn tất được chuyển tiếp |

Barista chỉ cần tick chọn + gõ thêm nếu muốn.

**Giới hạn:** tối đa 10 việc, mỗi việc ≤ 500 ký tự, ghi chú chung ≤ 1000 ký tự. Việc trùng nhau bị **khử tự động**.

> **Vì sao gợi ý sẵn?** Trước đây barista phải tự gõ lại "còn mấy ly chưa pha" — mà đúng lúc tan ca thì đó là thứ dễ quên nhất, và quên thì ca sau nhận một quầy không biết đang nợ gì.

**Hai quyền tách nhau — chi tiết quan trọng:**

| Quyền | Điều kiện | Ý nghĩa |
|---|---|---|
| Lập bàn giao | Còn **ca đang mở** | Ca đã quá hạn bấm tan ca **vẫn phải giao được việc** cho ca sau |
| Lưu bàn giao & Tan ca | Còn trong **cửa sổ chấm công** | Nếu không, nút này thành đường vòng chấm công trễ vô hạn |

Quá hạn chấm công → *"Bấm Lưu bàn giao để giao việc cho ca sau, rồi nhờ Quản lý chốt giờ tan ca giúp bạn."*

### A5 · Hệ thống tự dò người nhận — 3 tầng

```
① Ca barista bắt đầu SỚM NHẤT kể từ lúc ca mình kết thúc
   (nhiều barista cùng ca → TẤT CẢ cùng nhận)
        │ không có
        ▼
② Quản lý chi nhánh
        │ chi nhánh chưa gán quản lý
        ▼
③ ĐỂ TRỐNG người nhận — bàn giao vẫn lưu,
   barista vào ca sau tự bấm "Tiếp nhận"
```

> **Chi tiết dễ sai ở tầng ①:** so sánh là `>=` chứ không phải `>`. Lịch quán xếp nối đuôi — ca 12:00–17:00 bắt đầu **đúng lúc** ca 07:00–12:00 kết thúc. Dùng `>` sẽ bỏ qua đúng ca tiếp quản quầy.

> **Vì sao cần tầng ③?** Phiên bản trước bỏ chặn tan ca khi không dò được người nhận. Hôm nào lịch chưa xếp ca sau **và** chi nhánh cũng chưa gán quản lý thì ca đó tan sạch, việc tồn biến mất. Còn nếu chặn tan ca thì barista bị kẹt vì một lỗi xếp lịch họ không sửa được. Tầng ③ giải quyết cả hai.

### A6 · Nhận & theo dõi bàn giao

| Thao tác | Ai | Điều kiện |
|---|---|---|
| **Xác nhận đã nhận** | Người được chỉ định | Đang trong ca |
| **Tiếp nhận** (bàn giao mồ côi/quá hạn) | Bất kỳ barista đang trực | Đang trong ca; bàn giao chưa ai nhận và đã quá hạn |
| **Cập nhật việc** (Mới → Đang xử lý → Xong) | Người đã xác nhận | Phải xác nhận **trước** |

> **Nguyên tắc "Tiếp nhận":** nhận là xác nhận luôn — người bấm chính là người sẽ gánh việc, không có bước duyệt ở giữa.

**Nhắc nhở tự động:** mọi trang barista hiện banner *"N bàn giao ca đang chờ bạn xác nhận"* khi có việc chưa nhận.

### A7 · Tra cứu bàn giao

Lọc theo: gửi cho tôi / tôi gửi · trạng thái (chờ nhận / đang xử lý / hoàn tất) · từ khóa.

Sau mỗi lần cập nhật, hệ thống đưa về **đúng bộ lọc, đúng trang, và neo tới đúng thẻ vừa thao tác** — danh sách dài mà nhảy về đầu trang thì phải cuộn tìm lại chỗ cũ sau mỗi lần bấm.

---

## 5. Miền B — Sản xuất đơn hàng tại quầy

**Màn:** `/barista/kds` (Quầy pha chế) — trục xương sống của cả role

**Gồm:** pha món + xử lý sự cố món.
**Vì sao gộp:** đây là **hai nhánh của cùng một máy trạng thái** trên món; nhánh sự cố chính là lối thoát bắt buộc của nhánh chính.

### Máy trạng thái

```
        ┌──────── Trả lại chờ / Thu hồi ────────┐
        ▼                                       │
    ĐANG CHỜ ──Nhận pha──▶ ĐANG PHA ──Xong──▶ ĐÃ XONG ──▶ (Thu ngân giao khách)
        │                      │                    ▲
        │                      │                    │ ★ TRỪ KHO tại đây
        └── Báo sự cố A/B ─────┴──▶ CẦN XỬ LÝ       │
                                       │            │
                                       └─Bỏ chặn────┘ (về ĐANG CHỜ)

    Làm lại ──▶ về ĐANG CHỜ với ưu tiên lên đầu
```

### B1 · Cách hàng chờ được sắp xếp và hiển thị

Đây không chỉ là chuyện giao diện — nó là nghiệp vụ.

| Quy tắc | Nội dung | Vì sao |
|---|---|---|
| **Thứ tự pha** | Món làm lại lên đầu → còn lại theo giờ đặt (FIFO) | Món làm lại là khách đang đợi lần hai |
| **Món đã xong dồn cuối** | Không đánh số thứ tự | Với barista chúng không còn là việc; xen giữa thì việc thật bị đẩy khuất xuống dưới |
| **Không hiện số liệu thời gian** | Không có đồng hồ đếm, không tô đỏ theo phút | Ở cao điểm **mọi** món đều "trễ" nếu tính theo đồng hồ chờ song song — số ly đỏ chỉ đo lượng khách, không đo năng lực |
| **Chế độ cao điểm** | Đo bằng **số ly** đang chờ + đang pha so với ngưỡng chi nhánh | Thuần khối lượng việc, không dính đồng hồ |
| **Đếm theo SỐ LY** | Số dòng món và số đơn chỉ là thông tin phụ | Số ly mới là khối lượng việc pha thật |
| **Đơn của hôm trước không vào hàng chờ** | Nằm riêng | Mốc cắt ngày sau giờ đóng cửa nhiều tiếng → khách của những ly đó đã về. "Pha nốt" sẽ trừ kho thật cho ly không ai uống |

### B2 · Lọc và phân trang

**Bộ lọc:** người phụ trách (tất cả / của tôi / chưa ai nhận) · quầy (cà phê / trà / máy xay) · loại đơn (tại chỗ / mang đi / giao hàng).

**Bốn ràng buộc, mỗi cái sửa một lỗi thật:**

| Ràng buộc | Vì sao |
|---|---|
| Món **Cần xử lý** luôn hiện, không bộ lọc nào giấu được | Đó là cảnh báo an toàn |
| **Lọc trước, cắt trang sau** | Nếu ẩn bằng JS thì bấm "Món của tôi" ở trang 1 sẽ trống trong khi món nằm ở trang 3 |
| **Không tách đơn qua 2 trang** | Pha hết trang 1 mà đơn còn hai ly ở trang 2 là cách chắc chắn nhất để giao thiếu |
| **Số thứ tự pha là vị trí thật trong cả hàng chờ** | Không đánh lại theo từng trang |

**Nhãn "món 2/3"** đếm trên hàng chờ đầy đủ, **trước khi lọc** — nếu đếm sau lọc thì nhãn đổi nghĩa mỗi lần bấm chip, trong khi cái barista cần biết là đơn thật sự có mấy ly.

### B3 · Làm mới bảng

**Không có tự động làm mới định kỳ.** Bảng làm mới khi: bấm nút "Làm mới" · sau mỗi thao tác · đổi bộ lọc hoặc trang.

Có **4 chốt chặn làm mới ngầm** (để không giật mất thao tác đang dở): tab đang ẩn · vừa submit xong (1.8 giây) · đang mở hộp thoại · con trỏ đang trong ô nhập.
Bấm tay thì bỏ qua hết các chốt này — nếu không, nút sẽ im lặng không làm gì và trông như hỏng.

### B4 · Nhận pha và hoàn thành

| Thao tác | Chuyển | Ai được làm |
|---|---|---|
| **Nhận pha** (1 món) | Chờ → Đang pha | Bất kỳ ai đang trong ca |
| **Nhận pha cả đơn** | nhiều món cùng lúc | Bất kỳ ai — đơn nhiều ly thường do một người pha trọn |
| **Xong** (1 món) | Đang pha → Đã xong | **Chỉ chủ món** |
| **Xong cả đơn** | nhiều món cùng lúc | Chỉ món của **chính mình** |
| **Trả lại chờ** | Đang pha → Chờ | **Chỉ chủ món** |

**★ "Xong" là điểm trừ kho duy nhất theo đơn.**

```
Bấm "Xong"
   │
   ├─ ① Chốt trạng thái Đang pha → Đã xong (nguyên tử)
   │      thất bại (người khác vừa xử lý) → DỪNG, KHÔNG đụng kho
   │
   ├─ ② Trừ kho theo công thức + modifier
   │      (bỏ qua nếu là món làm lại đã giữ kho từ trước)
   │
   └─ ③ Ghi nhật ký + phát sự kiện cho màn khách
```

> **Vì sao chốt trạng thái trước rồi mới trừ kho?** Hai barista bấm "Xong" cùng lúc trên một món: chỉ request **thắng cuộc** mới đi tiếp bước trừ kho. Nếu trừ trước rồi mới chốt, kho bị trừ hai lần cho một ly.

**"Xong cả đơn" — xử lý món chưa có công thức:**
Món chưa khai công thức bị loại ra **trước** vòng lặp, không để lỗi ném ra giữa chừng — vì ném giữa chừng thì cả đơn bị hủy bỏ chỉ vì một dòng, các ly đã pha xong thật sẽ quay ngược về "đang pha".

Kết quả: *"Đã hoàn thành 3 món. Còn 1 món chưa có công thức — hãy bấm Báo sự cố cho món đó."*
→ Nói rõ **phần chưa xong** và **lối thoát**, vì món thiếu công thức sẽ không bao giờ tự xong được.

### B5 · Thu hồi món của người đã rời ca

**Tình huống:** barista A về mà quên gỡ một ly đang pha. Ly đó khóa dưới tên A, không ai đụng được.

```
Barista B bấm "Thu hồi"
   │
   ├─ Chủ món là chính mình?     → dùng "Trả lại chờ" thay thế
   ├─ Chủ món VẪN đang trực?     → "Người này vẫn đang trong ca —
   │                                nhờ họ bấm Trả lại chờ cho món này"
   └─ Chủ món đã rời ca ✓        → món về hàng chờ, ai cũng nhận pha tiếp được
                                    (ghi nhật ký: "Thu hồi từ A bởi B")
```

> **Điều kiện "đã rời ca" được kiểm lại ở server**, không tin nút hiện trên màn — bảng có thể đã dựng vài phút trước và chủ món vừa quay lại quầy.

Đây là **lối gỡ duy nhất tại quầy**; không có nó thì phải nhờ Thu ngân hủy món.

### B6 · Báo sự cố — 3 nhóm, 3 hành vi khác nhau

> Trước đây cả ba nhóm cùng ghi một cái cờ, nghĩa là "báo sự cố" **không đổi hành vi hệ thống** — báo xong món vẫn nằm nguyên đó.

| Nhóm | Lý do | Hành vi | Vì sao khác nhau |
|---|---|---|---|
| **A** | Hết nguyên liệu | Sửa **sổ kho** (tồn = 0) **+** chặn món | Nguyên nhân gốc là sổ kho đang lạc quan hơn thực tế |
| **B** | Máy móc hỏng, món ngừng bán | Chặn món (**rời** hàng chờ) | Để barista khác không "Nhận pha" rồi vấp lại đúng vấn đề đó |
| **C** | Không đáp ứng được ghi chú, thông tin đơn không rõ, khác | **Chỉ gắn cờ** cho Thu ngân — món không đổi trạng thái, không bị hủy | Những lý do này cần **người khác** xử lý (hỏi lại khách, sửa đơn) |

#### Nhóm A chi tiết — nghiệp vụ phức tạp nhất của miền B

```
① Bấm "Báo sự cố" → chọn "Hết nguyên liệu"
② Hộp thoại nạp danh sách nguyên liệu CỦA MÓN ĐÓ (nạp theo yêu cầu,
   không nhúng sẵn vào 60 thẻ × N nguyên liệu)
③ Tick nguyên liệu đã hết → Gửi
④ Kiểm: mọi nguyên liệu tick PHẢI thuộc công thức của món này
⑤ Chặn món TRƯỚC (thua tranh chấp → không đụng sổ kho)
⑥ Ghi tồn = 0 qua sổ cái, lý do "Barista báo hết tại quầy pha chế"
   ─── tất cả trong MỘT giao dịch ───
```

**Chốt an toàn ở bước ④:** thiếu nó, một request tự soạn ép được tồn của **nguyên liệu bất kỳ** ở chi nhánh về 0, kéo theo mọi món dùng nguyên liệu đó biến mất khỏi POS và QR.

**Hiệu ứng dây chuyền:**
```
Tồn = 0 ──▶ Món dùng nguyên liệu đó tự ẩn khỏi POS/QR
        └──▶ Hiện trong "Gợi ý báo hết món" (miền D)
             ⚠ Khóa menu vẫn là thao tác CÓ Ý THỨC của người dùng, KHÔNG tự động
```

Thông báo: *"Đã ghi hết nguyên liệu vào sổ kho — các món dùng nguyên liệu này tự ẩn khỏi POS/QR, tự hiện lại khi có tồn."*

### B7 · Bỏ chặn món — đường thoát bắt buộc

Không có nó thì món kẹt vĩnh viễn.

| Cách | Khi nào |
|---|---|
| **Bỏ chặn thường** | Máy đã sửa xong |
| **Bỏ chặn kèm kiểm kê** | Nguyên liệu đã có lại → nhập tồn thật cho từng loại |

Phản hồi có giá trị nhất: *"Đã trả món về hàng chờ. Còn 3 món đang cần xử lý dùng nguyên liệu vừa kiểm lại."*
→ Cho biết thao tác này vừa gỡ được bao nhiêu món khác.

### B8 · Làm lại món

Lý do: pha sai công thức · làm đổ/hư · chất lượng không đạt · khách phản hồi · giao nhầm · khách đổi yêu cầu.

**Hành vi:** món về hàng chờ với **ưu tiên lên đầu**, ghi một dòng vào nhật ký hao hụt, và **đánh dấu đã giữ kho** → lần bấm "Xong" sau **không trừ kho lần hai**.

---

## 6. Miền C — Tồn kho vận hành tại quầy

**Màn:** `/barista/prep` (Pha sẵn) + `/barista/waste` (Hao hụt)

**Vì sao gộp:** cả hai đều ghi vào **cùng một sổ cái**, cùng nguyên tắc "không xóa cứng — sửa/hủy bằng giao dịch bù", và liên thông với nhau (mẻ quá hạn → ghi hao hụt).

### Bốn cửa ghi tồn của barista

| Cửa | Ở đâu | Loại giao dịch |
|---|---|---|
| Hoàn thành món | Quầy pha chế (B4) | Trừ theo công thức |
| Tạo/sửa/hủy mẻ pha sẵn | Pha sẵn (C1) | Trừ nguyên liệu thô + cộng nguyên liệu pha sẵn |
| Ghi hao hụt | Hao hụt (C2) | Trừ hao hụt |
| Kiểm kê tại quầy | Quầy pha chế (B6-A, B7) | Điều chỉnh |

> **Không có cửa thứ năm.** Mọi thay đổi tồn kho đều đi qua sổ cái.

### C1 · Pha sẵn nguyên liệu (thô → pha sẵn)

> Đây là **nơi duy nhất trong toàn hệ thống** chuyển nguyên liệu thô thành nguyên liệu pha sẵn.

**Checklist "cần pha hôm nay":** nguyên liệu pha sẵn có tồn ≤ ngưỡng → đầu vào quyết định cho việc tạo mẻ.

**Tạo mẻ:**

| Hạng mục | Nội dung |
|---|---|
| Đầu vào | Nhiều dòng: nguyên liệu · sản lượng · hạn dùng |
| Kiểm tra | Sản lượng > 0 · hạn dùng phải ở **tương lai** · nguyên liệu phải có trong danh mục pha sẵn |
| Xử lý | **Tất-cả-hoặc-không** trong một giao dịch: trừ nguyên liệu thô + cộng nguyên liệu pha sẵn |
| Khi lỗi | **Giữ nguyên toàn bộ dữ liệu đã nhập** trên form, không phải gõ lại |
| Thông báo lỗi | Gọi **tên nguyên liệu** ("Si-rô đường: Sản lượng phải > 0") thay vì "Dòng 3" khi đã xác định được |

**Xem trước không cần tải lại trang:** nhập sản lượng là thấy ngay "sẽ trừ bao nhiêu nguyên liệu thô" và cảnh báo nếu không đủ tồn. Có nút làm mới tồn riêng (gọi ngầm) giữ nguyên các dòng đang gõ dở.

**Sửa / hủy mẻ:**
- Sửa sản lượng → chỉ ghi **phần chênh lệch** vào sổ cái.
- Hủy mẻ → hoàn kho bằng **giao dịch bù**.
- Hủy mẻ đã hủy rồi → *"Mẻ này đã được huỷ trước đó — tồn kho giữ nguyên"* (không báo "đã hoàn kho" cho một thao tác rỗng).

**Ghi hao hụt mẻ quá hạn:** hệ thống liệt kê mẻ quá hạn còn hiệu lực, **gợi ý sẵn lượng hao hụt**, barista xác nhận → ghi hao hụt + đóng mẻ trong một giao dịch.

> **Chi tiết quan trọng của thuật toán gợi ý:** phân bổ theo thứ tự hết hạn sớm nhất trước, **trừ dần vào tồn còn lại** của cùng nguyên liệu.
> Vì tồn kho ghi nhận theo *nguyên liệu*, không theo *từng mẻ*. Nếu mỗi mẻ đều lấy `min(sản lượng, tồn)` thì tổng gợi ý của nhiều mẻ cùng nguyên liệu sẽ **vượt tồn thực và làm âm kho** khi barista làm theo.

### C2 · Ghi hao hụt nguyên liệu

**Phân loại 2 tầng:** Loại (Đổ vỡ / Hết hạn / Khác) → Lý do cụ thể → Mã nguyên nhân lưu vào DB.

| Loại | Lý do chọn được |
|---|---|
| Đổ vỡ | Đổ khi pha · Rơi khi thao tác · Sai định lượng |
| Hết hạn | Hết hạn · Nguyên liệu hỏng · Bảo quản lỗi · Quá thời gian mở nắp |
| Khác | Mẫu thử/QC · Khác |

**Kiểm tra:** lý do phải **khớp với loại** đã chọn · chọn "Khác" thì **bắt buộc** nhập diễn giải · số lượng > 0 · tối đa 20 dòng mỗi lần.

**Gộp dòng trùng:** cùng (nguyên liệu, loại, nguyên nhân, lý do) → **cộng dồn số lượng**, không tạo hai dòng.

**Chống gửi trùng:** mỗi lần mở form sinh một mã ngẫu nhiên; bấm gửi hai lần chỉ ghi một lần → *"Yêu cầu này đã được ghi trước đó."*

**Quyền sửa — chặt nhất trong toàn role, ba điều kiện đồng thời:**

```
① KHÔNG phải dòng "làm lại món"
   → "Dòng làm lại món không sửa lẻ; hãy huỷ rồi ghi lại nếu cần"
② Bản ghi do CHÍNH MÌNH tạo
   → "Bạn chỉ được sửa bản ghi do chính mình tạo"
③ Trong vòng 15 PHÚT
   → "Bản ghi đã quá 15 phút, hãy gửi Quản lý đối soát"
```

Sửa → ghi **chênh lệch**. Hủy → **giao dịch bù**, không xóa cứng.

### C3 · Phạm vi xem nhật ký hao hụt — tự suy theo người xem

| Tình huống | Phạm vi hiển thị |
|---|---|
| Đang trong ca | Từ giờ mình vào ca → nay ("Ca đang mở") |
| Vừa tan ca | Toàn bộ ca vừa rồi ("Ca vừa tan") |
| Không có ca nào | **Ngày kinh doanh** của chi nhánh |

> **Vì sao "ngày kinh doanh" chứ không phải "hôm nay"?** Ngày kinh doanh tính từ **giờ mở cửa chi nhánh**, không phải nửa đêm.
> Cắt theo nửa đêm thì **ca đêm đang chạy bị đứt đôi lúc 00:00** và nửa đầu ca biến mất khỏi bảng.
> Mốc này dùng chung với Quầy pha chế và Dashboard để ba màn không nói ba chuyện khác nhau về "hôm nay".

---

## 7. Miền D — Khả dụng menu & công thức

**Màn:** `/barista/eightysix` (Báo hết món) + `/barista/recipe` (Tra cứu công thức)

**Vì sao gộp:** cùng đọc dữ liệu danh mục — tra cứu là **đọc**, báo hết món là **đề nghị thay đổi** trên chính tập dữ liệu đó.

### D1 · Báo hết món — barista chỉ ĐỀ NGHỊ

```
Barista bấm "Báo tạm hết"  ──▶  Ghi yêu cầu  ──▶  Quản lý duyệt  ──▶  Món bị khóa
                                "chờ quản lý xử lý"
```

**Lý do chọn được — chỉ nhóm "sự cố":**

| Lý do | Ghi chú bấm nhanh |
|---|---|
| Máy móc hỏng | Máy pha lỗi · Máy xay lỗi · Máy đá lỗi · Tủ mát lỗi · Mất điện |
| Lỗi chất lượng | Vị không đạt · Pha bị lỗi mẻ · Sai công thức |
| Khác | *(không có chip — buộc ghi rõ bằng tay)* |

**Lý do KHÔNG còn chọn được nữa:**

| Lý do cũ | Nay đi đường nào |
|---|---|
| "Hết nguyên liệu" | Báo hết ở Quầy pha chế → tồn = 0 → món **tự ẩn** |
| "Hỏng / quá hạn" | Ghi Hao hụt → tồn tụt → món **tự ẩn** |

> **Nguyên tắc phân định:**
> - Con số kho **phản ánh được** vấn đề → để cơ chế tự động lo.
> - Con số kho **không phản ánh được** (máy hỏng, lỗi chất lượng) → barista khóa tay, quản lý gác mở lại.

**Chip ghi chú nhanh:** barista đứng máy giữa ca không gõ tay được; chip chỉ là lối tắt điền sẵn, ô ghi chú vẫn sửa tay được.

**Gợi ý báo hết món:** danh sách món có nguyên liệu đã cạn — suy tự động từ tồn kho, là đầu ra trực tiếp của miền B và C. Chỉ là gợi ý mềm, **không tự khóa**.

**Đề nghị mở bán lại:** khi máy đã sửa → *"Đã gửi yêu cầu, chờ quản lý duyệt."*

### D2 · Tra cứu công thức — chỉ đọc tuyệt đối

Màn này **không có thao tác ghi nào** (servlet không có `doPost`).

Hiển thị:
1. **Công thức gốc** của món.
2. **Tác động của modifier** — đánh dấu rõ nguyên liệu nào phát sinh **ngoài** công thức gốc. Đây là thứ giải thích vì sao cùng một món lại trừ kho khác nhau.
3. **Định mức pha sẵn** của từng nguyên liệu pha sẵn trong công thức → nối sang miền C.

**Lọc:** từ khóa · nhóm món · có/chưa có công thức · chỉ món của chi nhánh mình (mặc định bật).

Trang **50 dòng** (không phải 10) — màn này cần quét nhanh giữa lúc pha, menu một chi nhánh thường vài chục món.

**Chốt bảo mật:** chọn món ngoài phạm vi chi nhánh/bộ lọc bằng cách đoán mã món → trả thông báo trung tính *"Món được chọn không còn thuộc phạm vi tra cứu hiện tại"*, **không trả dữ liệu**.

---

## 8. Dashboard — lớp đọc xuyên miền

`/barista/dashboard` — chỉ đọc, tổng hợp từ cả 4 miền.

### KPI tách bạch hai lớp

| Của tôi | Của quầy |
|---|---|
| Ly đang pha · ly đã xong | Ly đang chờ / đang pha / đã xong / bị chặn |
| Thời gian pha trung bình | Số lần làm lại toàn chi nhánh |
| Số lần làm lại · số lần ghi hao hụt | Số mẻ pha quá hạn |

### Cảnh báo

- Tồn thấp
- **Âm kho** — đếm **riêng** vì khẩn hơn (bán/dùng quá tồn lý thuyết, cần đối soát), và **cố ý không cộng vào tổng cảnh báo** để tránh đếm đôi
- Số món đang tạm hết · số gợi ý báo hết
- Mẻ pha quá hạn

### Top 5 món chờ lâu nhất

Giữ nguyên thứ tự pha (làm lại trước, rồi FIFO) để đúng là **5 dòng đầu barista sẽ pha**, khớp với màn Quầy pha chế.

> **Nhất quán số liệu:** mọi con số cắt theo **ngày kinh doanh**. Trước đây màn này dùng hàng chờ không cắt ngày nên hai màn cho hai con số "đang chờ" khác nhau ở cùng một thời điểm — không giải thích được cho người dùng.

---

## 9. Quy ước kỹ thuật dùng chung

### Xử lý lỗi phân tầng

| Loại lỗi | Cách xử lý |
|---|---|
| Vi phạm nghiệp vụ | Thông báo thân thiện + giữ lại dữ liệu đã nhập |
| Dữ liệu số không hợp lệ | *"Dữ liệu món không hợp lệ. Vui lòng tải lại và thử lại."* |
| Lỗi hạ tầng (SQL) | Thông báo trung tính, **không lộ chi tiết nội bộ** |

> Thứ tự bắt lỗi có chủ đích: lỗi định dạng số bắt **trước** lỗi tham số chung — nếu không, thông báo máy móc kiểu *"For input string: abc"* sẽ hiện thẳng lên banner của barista.

### Không tin dữ liệu từ client

Mọi tham số đều qua danh sách cho phép: bộ lọc · số dòng mỗi trang · vị trí đặt món · trạng thái việc · lý do sự cố. Giá trị lạ → rơi về mặc định, **không** gây lỗi 500.

### Giữ ngữ cảnh sau mỗi thao tác (mẫu PRG)

Sau khi ghi, hệ thống đưa về **đúng bộ lọc + đúng trang** đang xem. Ngoại lệ có chủ đích: ghi hao hụt mới → về trang 1 (dòng mới nhất nằm trên cùng); sửa/hủy → giữ nguyên trang.

### Phạm vi chi nhánh

`branchId` **luôn** lấy từ phiên đăng nhập, không bao giờ từ tham số. Mọi câu lệnh cập nhật đều có điều kiện chi nhánh; màn bàn giao có thêm lớp kiểm riêng chống giả mạo mã.

### Kiểm thử

92 test đơn vị ở `controller/barista` + `service/barista`, cộng một bộ test tích hợp chạy SQL Server thật qua Testcontainers.

> Logic thuần được **tách khỏi DB có chủ đích** (phân giỏ trạng thái, lọc, đánh dấu khối đơn, dò ca nhận bàn giao, dựng URL, chuẩn hóa số dòng) — đó là lý do phủ được nhiều đến vậy mà không cần dựng cơ sở dữ liệu.

---

## 10. Điểm cần cải thiện

| # | Vấn đề | Chi tiết | Mức |
|---|---|---|---|
| 1 | **Rò rỉ công thức** | Endpoint nạp nguyên liệu của hộp thoại "Hết nguyên liệu" trả công thức cho **bất kỳ** mã món, không giới hạn chi nhánh, không kiểm món có trong hàng chờ. Trong khi màn Tra cứu công thức lại cố ý chống chính điều này → hai màn cùng role đang áp hai chuẩn khác nhau | Trung bình |
| 2 | **Nhánh ghi hao hụt cũ bỏ qua kiểm tra** | Action `create` (form một dòng cũ) không bắt buộc chọn lý do preset → request thủ công né được toàn bộ ràng buộc lý do ↔ loại. Form thật đã dùng action mới; nên gỡ nhánh cũ khỏi whitelist | Trung bình |
| 3 | **Truy vấn trạng thái ca lặp lại** | Một POST ở Quầy pha chế truy vấn trạng thái chấm công 2–3 lần, mỗi lần mở kết nối riêng. Dashboard nặng hơn: ~7 lời gọi service, mỗi cái tự mở kết nối | Hiệu năng |
| 4 | **Mất bộ lọc khi bị chặn ngoài ca** | Màn Báo hết món dựng sẵn URL giữ bộ lọc, nhưng cổng chặn ngoài ca lại quay về URL trần → bấm nhầm lúc ngoài ca là văng về trang 1 không lọc. Các màn khác đã xử lý đúng | Nhỏ |
| 5 | **Lỗi chấm công bị nuốt** | Khi truy vấn chấm công lỗi, hệ thống hiện thông báo rồi vẫn trả về màn cũ → dễ tưởng đã vào ca | Nhỏ |
| 6 | **Servlet Quầy pha chế quá lớn** | 410 dòng, gánh cả phân tích tham số, ánh xạ lý do, dựng bảng và điều phối phản hồi. Phần ánh xạ lý do hợp lý hơn nếu nằm cạnh enum lý do trong `common` | Bảo trì |

---

## Phụ lục — Bản đồ phụ thuộc giữa các miền

```
[A] TRỰC CA ──gate──▶ [B] [C] [D]        (ngoài ca = chỉ xem)
                └────▶ Tra cứu công thức  (vốn đã chỉ đọc)

[B] Hoàn thành món ──trừ kho────┐
[B] Báo hết nguyên liệu ──kiểm kê──┤
[C] Tạo/sửa/hủy mẻ pha ────────┤──▶ SỔ CÁI TỒN KHO
[C] Ghi/sửa/hủy hao hụt ───────┘    (nơi DUY NHẤT tồn thay đổi)

[B] Tồn = 0 ──gợi ý──▶ [D] Báo hết món ──đề nghị──▶ Quản lý duyệt
[B] Hàng chờ ──gợi ý việc──▶ [A] Bàn giao ca
[A] Tan ca ──chặn bởi──▶ [B] còn ly đang pha  ∧  [A] chưa bàn giao
[B][C][D] ──tổng hợp──▶ Dashboard
```
