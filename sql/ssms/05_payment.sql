/* Cụm ca két - hóa đơn - đối soát thanh toán. Chỉ đọc dữ liệu. */
SET NOCOUNT ON;

DECLARE @BranchId int = 1;
DECLARE @FromUtc datetime2 = DATEADD(DAY, -7, SYSUTCDATETIME());
DECLARE @TopRows int = 300;

-- 1. Ca thu ngân và tiền mặt dự kiến.
SELECT TOP (@TopRows)
       cs.CashierShiftId, cs.BranchId,
       u.Username AS CashierUsername, u.FullName AS CashierName,
       cs.OpeningCash,
       COALESCE(SUM(CASE WHEN b.Status = 'PAID' AND b.PaymentMethod = 'CASH'
                         THEN COALESCE(b.PaidAmount, b.TotalAmount) ELSE 0 END), 0) AS CashCollected,
       cs.OpeningCash + COALESCE(SUM(CASE WHEN b.Status = 'PAID' AND b.PaymentMethod = 'CASH'
                                          THEN COALESCE(b.PaidAmount, b.TotalAmount) ELSE 0 END), 0) AS ExpectedClosingCash,
       cs.ClosingCash,
       DATEADD(HOUR, 7, cs.OpenedAt) AS OpenedAtLocal,
       DATEADD(HOUR, 7, cs.ClosedAt) AS ClosedAtLocal,
       CASE WHEN cs.ClosedAt IS NULL THEN 'OPEN' ELSE 'CLOSED' END AS ShiftState
FROM payment.CashierShift cs
JOIN iam.UserAccount u ON u.UserId = cs.CashierId
LEFT JOIN payment.Bill b ON b.CashierShiftId = cs.CashierShiftId
WHERE (@BranchId IS NULL OR cs.BranchId = @BranchId)
  AND cs.OpenedAt >= @FromUtc
GROUP BY cs.CashierShiftId, cs.BranchId, u.Username, u.FullName,
         cs.OpeningCash, cs.ClosingCash, cs.OpenedAt, cs.ClosedAt
ORDER BY cs.OpenedAt DESC;

-- 2. Hóa đơn gần nhất.
SELECT TOP (@TopRows)
       b.BillId, b.BranchId, b.CashierShiftId, b.Status,
       b.Subtotal, b.VatAmount, b.DiscountAmount,
       b.RoundingAdjustment, b.TotalAmount,
       b.PaidAmount, b.PaymentMethod, b.CashTendered, b.CashChange,
       DATEADD(HOUR, 7, b.CreatedAt) AS CreatedAtLocal,
       DATEADD(HOUR, 7, b.PaidAt) AS PaidAtLocal,
       COUNT(oi.OrderItemId) AS ItemLines,
       COALESCE(SUM(oi.Quantity), 0) AS Cups
FROM payment.Bill b
LEFT JOIN sales.OrderItem oi ON oi.BillId = b.BillId
WHERE (@BranchId IS NULL OR b.BranchId = @BranchId)
  AND b.CreatedAt >= @FromUtc
GROUP BY b.BillId, b.BranchId, b.CashierShiftId, b.Status,
         b.Subtotal, b.VatAmount, b.DiscountAmount, b.RoundingAdjustment,
         b.TotalAmount, b.PaidAmount, b.PaymentMethod,
         b.CashTendered, b.CashChange, b.CreatedAt, b.PaidAt
ORDER BY b.CreatedAt DESC, b.BillId DESC;

-- 3. Tổng hợp doanh thu theo ngày kinh doanh và phương thức.
-- Gom về một dòng mỗi bill trước để TotalAmount không bị nhân theo số món.
;WITH BillByDay AS (
    SELECT b.BillId, b.PaymentMethod, b.TotalAmount,
           MIN(so.BusinessDate) AS BusinessDate,
           SUM(COALESCE(oi.Quantity, 0)) AS Cups
    FROM payment.Bill b
    JOIN sales.OrderItem oi ON oi.BillId = b.BillId
    JOIN sales.SalesOrder so ON so.OrderId = oi.OrderId
    WHERE (@BranchId IS NULL OR b.BranchId = @BranchId)
      AND b.Status = 'PAID'
      AND b.CreatedAt >= @FromUtc
    GROUP BY b.BillId, b.PaymentMethod, b.TotalAmount
)
SELECT BusinessDate, PaymentMethod,
       COUNT(*) AS PaidBills,
       SUM(TotalAmount) AS Revenue,
       SUM(Cups) AS Cups
FROM BillByDay
GROUP BY BusinessDate, PaymentMethod
ORDER BY BusinessDate DESC, PaymentMethod;

-- 4. Hóa đơn chưa thanh toán và món liên quan.
SELECT b.BillId, b.BranchId, b.TotalAmount,
       DATEADD(HOUR, 7, b.CreatedAt) AS CreatedAtLocal,
       so.OrderId, so.PickupCode, so.Status AS OrderStatus,
       COUNT(oi.OrderItemId) AS ItemLines
FROM payment.Bill b
LEFT JOIN sales.OrderItem oi ON oi.BillId = b.BillId
LEFT JOIN sales.SalesOrder so ON so.OrderId = oi.OrderId
WHERE (@BranchId IS NULL OR b.BranchId = @BranchId)
  AND b.Status = 'UNPAID'
GROUP BY b.BillId, b.BranchId, b.TotalAmount, b.CreatedAt,
         so.OrderId, so.PickupCode, so.Status
ORDER BY b.CreatedAt, b.BillId;
