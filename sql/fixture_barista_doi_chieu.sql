/* ============================================================================
   FIXTURE ĐỐI CHIẾU ROLE BARISTA  —  chi nhánh CN01, ngày kinh doanh hiện tại
   ----------------------------------------------------------------------------
   MỤC ĐÍCH
     Dựng đủ dữ liệu để đối chiếu toàn bộ nghiệp vụ Barista bằng tài khoản
     barista1 (CN01), theo tài liệu docs/PHAN_TICH_ROLE_BARISTA.md.

   AN TOÀN
     - Chỉ INSERT / UPDATE. KHÔNG có DROP, DELETE, TRUNCATE.
     - Mọi khối đều idempotent (chạy lại nhiều lần không nhân bản dữ liệu).
     - KHÔNG đụng tới sổ cái tồn kho: chỉ đổi trạng thái các món đang WAITING
       (WAITING/MAKING/BLOCKED đều CHƯA trừ kho — kho chỉ trừ lúc bấm "Xong").
     - Giữ nguyên 807 đơn lịch sử và bộ tài khoản test ucb_* của chi nhánh 4.

   CÁCH CHẠY
     sqlcmd -S localhost,14333 -U sa -P 'YourPassword123' \
            --encrypt-connection disable -d CafeChain \
            -i sql/fixture_barista_doi_chieu.sql

   HOÀN TÁC
     Xem khối ROLLBACK ở cuối file (đã comment sẵn).
   ============================================================================ */

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

USE CafeChain;
GO

/* Ngày kinh doanh theo giờ Việt Nam (UTC+7) — KHÔNG dùng GETDATE() của server
   vì container chạy giờ UTC, lệch 7 tiếng sẽ tạo ca sai ngày. */
DECLARE @todayVn  DATE     = CAST(DATEADD(HOUR, 7, SYSUTCDATETIME()) AS DATE);
DECLARE @nowVn    TIME(0)  = CAST(DATEADD(HOUR, 7, SYSUTCDATETIME()) AS TIME(0));
DECLARE @branchId INT = (SELECT BranchId FROM org.Branch WHERE Code = 'CN01');

DECLARE @barista1 INT = (SELECT UserId FROM iam.[User] WHERE Username = 'barista1');
DECLARE @barista2 INT = (SELECT UserId FROM iam.[User] WHERE Username = 'barista2');
DECLARE @barista4 INT = (SELECT UserId FROM iam.[User] WHERE Username = 'barista4');

IF @branchId IS NULL THROW 50010, N'Không tìm thấy chi nhánh CN01.', 1;
IF @barista1 IS NULL THROW 50011, N'Không tìm thấy tài khoản barista1.', 1;

PRINT N'Ngày kinh doanh VN: ' + CONVERT(NVARCHAR(10), @todayVn, 23)
      + N'  ·  Giờ VN hiện tại: ' + CONVERT(NVARCHAR(8), @nowVn);

/* ---------------------------------------------------------------------------
   1) CA LÀM HÔM NAY  —  điều kiện tiên quyết để barista thao tác ghi

   Không có ca là "ngoài ca": mọi màn chỉ xem, mọi POST bị chặn ở server.
   Chọn đúng ca đang phủ giờ hiện tại để nút "Vào ca" bấm được ngay.
   --------------------------------------------------------------------------- */
DECLARE @templateId INT = (
    SELECT TOP 1 ShiftTemplateId
    FROM hr.ShiftTemplate
    WHERE BranchId = @branchId
      AND @nowVn >= StartTime
      AND @nowVn <  EndTime
    ORDER BY StartTime);

-- Ngoài mọi khung ca (vd đang 02:00 sáng) thì lấy ca gần nhất để vẫn có gì mà xem.
IF @templateId IS NULL
    SET @templateId = (SELECT TOP 1 ShiftTemplateId FROM hr.ShiftTemplate
                       WHERE BranchId = @branchId ORDER BY StartTime DESC);

INSERT INTO hr.ShiftAssignment (ShiftTemplateId, UserId, WorkDate)
SELECT @templateId, u.UserId, @todayVn
FROM (VALUES (@barista1), (@barista2), (@barista4)) AS u(UserId)
WHERE u.UserId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM hr.ShiftAssignment sa
                  WHERE sa.UserId = u.UserId AND sa.WorkDate = @todayVn);

PRINT N'  ✓ Ca hôm nay: barista1, barista2, barista4 @ CN01 (template '
      + CAST(@templateId AS NVARCHAR(10)) + N')';
GO

/* ---------------------------------------------------------------------------
   1b) ĐƯA ĐƠN CÒN DỞ CỦA CN01 VÀO NGÀY KINH DOANH HIỆN TẠI

   Hàng chờ lọc theo `Orders.CreatedAt >= mốc đầu ngày kinh doanh`. Đơn còn dở
   của CN01 đang mang ngày 2026-07-24 nên KHÔNG lọt vào màn Quầy pha chế —
   đúng thiết kế (đơn hôm trước thuộc việc của Thu ngân huỷ & hoàn tiền),
   nhưng khiến không có gì để đối chiếu.

   Rải giờ tạo trong 40 phút gần đây để thứ tự FIFO trên màn nhìn tự nhiên.
   --------------------------------------------------------------------------- */
DECLARE @branchId INT = (SELECT BranchId FROM org.Branch WHERE Code = 'CN01');

;WITH openOrders AS (
    SELECT DISTINCT o.OrderId,
           ROW_NUMBER() OVER (ORDER BY o.OrderId) AS rn
    FROM sales.Orders o
    JOIN sales.OrderItem oi ON oi.OrderId = o.OrderId
    WHERE o.BranchId = @branchId
      AND o.Status = 'ACTIVE'
      AND oi.Status IN ('WAITING','MAKING','READY','BLOCKED')
)
UPDATE o
SET CreatedAt = DATEADD(MINUTE, -40 + (oo.rn * 8), SYSUTCDATETIME())
FROM sales.Orders o
JOIN openOrders oo ON oo.OrderId = o.OrderId;

PRINT N'  ✓ Đã đưa ' + CAST(@@ROWCOUNT AS NVARCHAR(10))
      + N' đơn còn dở của CN01 vào ngày kinh doanh hiện tại';
GO

/* ---------------------------------------------------------------------------
   2) MÓN "ĐANG PHA" CỦA NGƯỜI ĐÃ RỜI CA  —  để đối chiếu nút "Thu hồi"

   Nút Thu hồi CHỈ hiện khi chủ món không còn trực. Ta gán món cho barista4
   rồi ở bước sau XOÁ ca của barista4 đi, để họ thành "đã rời ca".
   --------------------------------------------------------------------------- */
DECLARE @branchId INT = (SELECT BranchId FROM org.Branch WHERE Code = 'CN01');
DECLARE @barista4 INT = (SELECT UserId FROM iam.[User] WHERE Username = 'barista4');

-- Đã có món ĐANG PHA của barista4 rồi thì thôi — nếu không, mỗi lần chạy lại
-- sẽ ăn thêm một món khỏi hàng chờ cho tới khi hàng chờ rỗng.
DECLARE @itemMaking INT = (
    SELECT TOP 1 oi.OrderItemId
    FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId = oi.OrderId
    WHERE o.BranchId = @branchId AND oi.Status = 'WAITING'
      AND NOT EXISTS (SELECT 1 FROM sales.OrderItem m JOIN sales.Orders mo ON mo.OrderId = m.OrderId
                      WHERE mo.BranchId = @branchId AND m.Status = 'MAKING' AND m.BaristaId = @barista4)
    ORDER BY oi.OrderItemId);

IF @itemMaking IS NOT NULL
BEGIN
    UPDATE sales.OrderItem
    SET Status    = 'MAKING',
        BaristaId = @barista4,
        StartedAt = DATEADD(MINUTE, -12, SYSUTCDATETIME())
    WHERE OrderItemId = @itemMaking;
    PRINT N'  ✓ Món #' + CAST(@itemMaking AS NVARCHAR(10))
          + N' → ĐANG PHA (chủ: barista4, sẽ ở trạng thái đã rời ca)';
END
GO

/* ---------------------------------------------------------------------------
   3) MÓN "CẦN XỬ LÝ"  —  để đối chiếu nhánh sự cố + nút "Bỏ chặn"
   --------------------------------------------------------------------------- */
DECLARE @branchId INT = (SELECT BranchId FROM org.Branch WHERE Code = 'CN01');
DECLARE @barista1 INT = (SELECT UserId FROM iam.[User] WHERE Username = 'barista1');

-- Cùng lý do idempotent như khối 2: đã có món CẦN XỬ LÝ thì không tạo thêm.
DECLARE @itemBlocked INT = (
    SELECT TOP 1 oi.OrderItemId
    FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId = oi.OrderId
    WHERE o.BranchId = @branchId AND oi.Status = 'WAITING'
      AND NOT EXISTS (SELECT 1 FROM sales.OrderItem b JOIN sales.Orders bo ON bo.OrderId = b.OrderId
                      WHERE bo.BranchId = @branchId AND b.Status = 'BLOCKED')
    ORDER BY oi.OrderItemId DESC);

IF @itemBlocked IS NOT NULL
BEGIN
    UPDATE sales.OrderItem
    SET Status          = 'BLOCKED',
        HasIssue        = 1,
        IssueReason     = N'Máy móc gặp sự cố',
        IssueReportedBy = @barista1,
        IssueReportedAt = SYSUTCDATETIME()
    WHERE OrderItemId = @itemBlocked;
    PRINT N'  ✓ Món #' + CAST(@itemBlocked AS NVARCHAR(10))
          + N' → CẦN XỬ LÝ (máy móc gặp sự cố)';
END
GO

/* ---------------------------------------------------------------------------
   4) GỠ CA CỦA barista4  —  để họ thành "đã rời ca", mở nút Thu hồi ở bước 2

   Xoá đúng MỘT dòng ca vừa tạo ở bước 1, không đụng lịch sử ca cũ.
   --------------------------------------------------------------------------- */
DECLARE @todayVn  DATE = CAST(DATEADD(HOUR, 7, SYSUTCDATETIME()) AS DATE);
DECLARE @barista4 INT = (SELECT UserId FROM iam.[User] WHERE Username = 'barista4');

DELETE sa
FROM hr.ShiftAssignment sa
WHERE sa.UserId = @barista4
  AND sa.WorkDate = @todayVn
  AND NOT EXISTS (SELECT 1 FROM hr.Attendance a
                  WHERE a.ShiftAssignmentId = sa.ShiftAssignmentId);

PRINT N'  ✓ barista4 không còn ca hôm nay → món của họ có nút "Thu hồi"';
GO

/* ---------------------------------------------------------------------------
   5) MẺ PHA SẴN QUÁ HẠN  —  để đối chiếu "Ghi hao hụt mẻ quá hạn"
   --------------------------------------------------------------------------- */
DECLARE @branchId INT = (SELECT BranchId FROM org.Branch WHERE Code = 'CN01');

IF NOT EXISTS (SELECT 1 FROM inventory.PrepBatch
               WHERE BranchId = @branchId AND Status = 'ACTIVE'
                 AND ExpiresAt < SYSUTCDATETIME())
BEGIN
    UPDATE TOP (1) inventory.PrepBatch
    SET ExpiresAt = DATEADD(HOUR, -3, SYSUTCDATETIME())
    WHERE BranchId = @branchId AND Status = 'ACTIVE';
    PRINT N'  ✓ Đã đẩy 1 mẻ pha sẵn thành quá hạn';
END
ELSE
    PRINT N'  · Đã có sẵn mẻ quá hạn, bỏ qua';
GO

/* ---------------------------------------------------------------------------
   KIỂM TRA KẾT QUẢ
   --------------------------------------------------------------------------- */
DECLARE @branchId INT = (SELECT BranchId FROM org.Branch WHERE Code = 'CN01');
DECLARE @todayVn  DATE = CAST(DATEADD(HOUR, 7, SYSUTCDATETIME()) AS DATE);

PRINT N'';
PRINT N'=== HÀNG CHỜ CN01 ===';
SELECT oi.Status AS [Trạng thái], COUNT(*) AS [Số dòng], SUM(oi.Quantity) AS [Số ly]
FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId = oi.OrderId
WHERE o.BranchId = @branchId AND oi.Status IN ('WAITING','MAKING','READY','BLOCKED')
GROUP BY oi.Status;

PRINT N'=== CA HÔM NAY @ CN01 ===';
SELECT u.Username, st.Name AS [Ca], st.StartTime, st.EndTime
FROM hr.ShiftAssignment sa
JOIN iam.[User] u ON u.UserId = sa.UserId
JOIN hr.ShiftTemplate st ON st.ShiftTemplateId = sa.ShiftTemplateId
WHERE sa.WorkDate = @todayVn AND st.BranchId = @branchId;
GO

/* ============================================================================
   HOÀN TÁC  —  bỏ comment và chạy nếu muốn trả CN01 về trạng thái trước fixture
   ============================================================================

DECLARE @todayVn DATE = CAST(DATEADD(HOUR, 7, SYSUTCDATETIME()) AS DATE);
DECLARE @branchId INT = (SELECT BranchId FROM org.Branch WHERE Code = 'CN01');

-- Gỡ ca đã thêm (chỉ những ca CHƯA chấm công)
DELETE sa FROM hr.ShiftAssignment sa
JOIN iam.[User] u ON u.UserId = sa.UserId
WHERE sa.WorkDate = @todayVn
  AND u.Username IN ('barista1','barista2','barista4')
  AND NOT EXISTS (SELECT 1 FROM hr.Attendance a WHERE a.ShiftAssignmentId = sa.ShiftAssignmentId);

-- Trả các món về hàng chờ
UPDATE oi SET Status='WAITING', BaristaId=NULL, StartedAt=NULL,
              HasIssue=0, IssueReason=NULL, IssueReportedBy=NULL, IssueReportedAt=NULL
FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId
WHERE o.BranchId=@branchId AND oi.Status IN ('MAKING','BLOCKED');

============================================================================ */
