/* Cụm bàn - đơn hàng - món - modifier - trạng thái KDS. Chỉ đọc dữ liệu. */
SET NOCOUNT ON;

DECLARE @BranchId int = 1;
DECLARE @BusinessDate date = CONVERT(date, DATEADD(HOUR, 7, SYSUTCDATETIME()));
DECLARE @TopRows int = 300;

-- 1. Bàn tại chi nhánh.
SELECT dt.DiningTableId, dt.BranchId, dt.TableNumber, dt.QrCode, dt.Status
FROM sales.DiningTable dt
WHERE @BranchId IS NULL OR dt.BranchId = @BranchId
ORDER BY dt.BranchId, dt.TableNumber;

-- 2. Đơn và tổng quan tiến độ món.
SELECT TOP (@TopRows)
       so.OrderId, so.BranchId, so.BusinessDate, so.PickupCode,
       so.Source, so.OrderType, so.Status AS OrderStatus,
       dt.TableNumber, creator.Username AS CreatedByUsername,
       DATEADD(HOUR, 7, so.CreatedAt) AS CreatedAtLocal,
       COUNT(oi.OrderItemId) AS ItemLines,
       COALESCE(SUM(oi.Quantity), 0) AS Cups,
       SUM(CASE WHEN oi.Status IN ('WAITING', 'MAKING', 'BLOCKED', 'REMAKE') THEN 1 ELSE 0 END) AS OpenItemLines,
       SUM(CASE WHEN oi.Status IN ('READY', 'PICKED_UP', 'SERVED') THEN 1 ELSE 0 END) AS FinishedItemLines
FROM sales.SalesOrder so
LEFT JOIN sales.DiningTable dt ON dt.DiningTableId = so.DiningTableId
LEFT JOIN iam.UserAccount creator ON creator.UserId = so.CreatedBy
LEFT JOIN sales.OrderItem oi ON oi.OrderId = so.OrderId
WHERE (@BranchId IS NULL OR so.BranchId = @BranchId)
  AND so.BusinessDate = @BusinessDate
GROUP BY so.OrderId, so.BranchId, so.BusinessDate, so.PickupCode,
         so.Source, so.OrderType, so.Status, dt.TableNumber,
         creator.Username, so.CreatedAt
ORDER BY so.CreatedAt DESC, so.OrderId DESC;

-- 3. Chi tiết món và người pha/phục vụ.
SELECT TOP (@TopRows)
       so.OrderId, so.PickupCode, so.OrderType, dt.TableNumber,
       oi.OrderItemId, oi.ProductId, oi.ProductNameAtOrder,
       oi.Quantity, oi.UnitPrice, oi.BilledAmount,
       oi.Status AS ItemStatus, oi.Priority, oi.Note,
       barista.Username AS BaristaUsername,
       preparer.Username AS PreparedByUsername,
       picker.Username AS PickedUpByUsername,
       DATEADD(HOUR, 7, oi.StartedAt) AS StartedAtLocal,
       DATEADD(HOUR, 7, oi.DoneAt) AS DoneAtLocal,
       DATEADD(HOUR, 7, oi.PickedUpAt) AS PickedUpAtLocal,
       DATEADD(HOUR, 7, oi.ServedAt) AS ServedAtLocal,
       oi.HasIssue, oi.IssueReason, oi.RemakeCount, oi.BillId
FROM sales.OrderItem oi
JOIN sales.SalesOrder so ON so.OrderId = oi.OrderId
LEFT JOIN sales.DiningTable dt ON dt.DiningTableId = so.DiningTableId
LEFT JOIN iam.UserAccount barista ON barista.UserId = oi.BaristaId
LEFT JOIN iam.UserAccount preparer ON preparer.UserId = oi.PreparedBy
LEFT JOIN iam.UserAccount picker ON picker.UserId = oi.PickedUpBy
WHERE (@BranchId IS NULL OR oi.BranchId = @BranchId)
  AND so.BusinessDate = @BusinessDate
ORDER BY oi.Priority DESC, so.CreatedAt, oi.OrderItemId;

-- 4. Modifier đã snapshot trên từng món.
SELECT so.OrderId, oi.OrderItemId, oi.ProductNameAtOrder,
       oim.OrderItemModifierId, oim.ModifierOptionId,
       oim.ModifierOptionNameAtOrder, oim.PriceDelta
FROM sales.OrderItemModifier oim
JOIN sales.OrderItem oi ON oi.OrderItemId = oim.OrderItemId
JOIN sales.SalesOrder so ON so.OrderId = oi.OrderId
WHERE (@BranchId IS NULL OR oi.BranchId = @BranchId)
  AND so.BusinessDate = @BusinessDate
ORDER BY so.OrderId, oi.OrderItemId, oim.OrderItemModifierId;

-- 5. Hàng đợi KDS đang mở.
SELECT oi.Priority, so.OrderId, so.PickupCode, dt.TableNumber,
       oi.OrderItemId, oi.ProductNameAtOrder, oi.Quantity,
       oi.Status, barista.Username AS CurrentBarista,
       DATEDIFF(MINUTE, so.CreatedAt, SYSUTCDATETIME()) AS WaitingMinutes
FROM sales.OrderItem oi
JOIN sales.SalesOrder so ON so.OrderId = oi.OrderId
LEFT JOIN sales.DiningTable dt ON dt.DiningTableId = so.DiningTableId
LEFT JOIN iam.UserAccount barista ON barista.UserId = oi.BaristaId
WHERE (@BranchId IS NULL OR oi.BranchId = @BranchId)
  AND so.BusinessDate = @BusinessDate
  AND oi.Status IN ('WAITING', 'MAKING', 'BLOCKED', 'REMAKE')
ORDER BY oi.Priority DESC, so.CreatedAt, oi.OrderItemId;

