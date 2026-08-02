/* Kiểm tra nhanh schema và dữ liệu. Tất cả result set lý tưởng phải rỗng hoặc có IsHealthy = 1. */
SET NOCOUNT ON;

-- 1. Tổng quan sức khỏe constraint/trigger.
SELECT CAST(CASE WHEN NOT EXISTS (
           SELECT 1 FROM sys.foreign_keys WHERE is_disabled = 1 OR is_not_trusted = 1
       ) AND NOT EXISTS (
           SELECT 1 FROM sys.check_constraints WHERE is_disabled = 1 OR is_not_trusted = 1
       ) AND NOT EXISTS (
           SELECT 1 FROM sys.triggers WHERE is_ms_shipped = 0 AND is_disabled = 1
       ) THEN 1 ELSE 0 END AS bit) AS IsHealthy;

-- 2. Constraint hoặc trigger đang tắt/không trusted.
SELECT N'FOREIGN_KEY' AS ObjectType, name, is_disabled, is_not_trusted
FROM sys.foreign_keys
WHERE is_disabled = 1 OR is_not_trusted = 1
UNION ALL
SELECT N'CHECK', name, is_disabled, is_not_trusted
FROM sys.check_constraints
WHERE is_disabled = 1 OR is_not_trusted = 1
UNION ALL
SELECT N'TRIGGER', name, is_disabled, CAST(0 AS bit)
FROM sys.triggers
WHERE is_ms_shipped = 0 AND is_disabled = 1;

-- 3. User vận hành không thuộc chi nhánh hợp lệ.
SELECT u.UserId, u.Username, u.RoleCode, u.BranchId, u.Status
FROM iam.UserAccount u
LEFT JOIN org.Branch b ON b.BranchId = u.BranchId
WHERE (u.RoleCode IN ('BRANCH_MANAGER', 'CASHIER', 'BARISTA') AND b.BranchId IS NULL)
   OR (u.RoleCode = 'ADMIN' AND u.BranchId IS NOT NULL);

-- 4. Nhiều hơn một két đang mở trong cùng chi nhánh.
SELECT BranchId, COUNT(*) AS OpenCashierShifts
FROM payment.CashierShift
WHERE ClosedAt IS NULL
GROUP BY BranchId
HAVING COUNT(*) > 1;

-- 5. Dòng tồn khác tổng ledger. Chênh lệch có thể xuất hiện với seed ban đầu;
--    cần điều tra ReferenceType/ReferenceId trước khi sửa.
SELECT bi.BranchId, bi.IngredientId, i.Name AS IngredientName,
       bi.QuantityOnHand,
       COALESCE(SUM(it.ChangeQty), 0) AS LedgerTotal,
       bi.QuantityOnHand - COALESCE(SUM(it.ChangeQty), 0) AS Difference
FROM inventory.BranchInventory bi
JOIN catalog.Ingredient i ON i.IngredientId = bi.IngredientId
LEFT JOIN inventory.InventoryTransaction it
       ON it.BranchId = bi.BranchId AND it.IngredientId = bi.IngredientId
GROUP BY bi.BranchId, bi.IngredientId, i.Name, bi.QuantityOnHand
HAVING ABS(bi.QuantityOnHand - COALESCE(SUM(it.ChangeQty), 0)) > 0.001;

-- 6. Order item lệch BranchId với order/bill (FK ghép bình thường đã chặn trường hợp này).
SELECT oi.OrderItemId, oi.BranchId AS ItemBranchId,
       so.BranchId AS OrderBranchId, b.BranchId AS BillBranchId
FROM sales.OrderItem oi
JOIN sales.SalesOrder so ON so.OrderId = oi.OrderId
LEFT JOIN payment.Bill b ON b.BillId = oi.BillId
WHERE oi.BranchId <> so.BranchId
   OR (b.BillId IS NOT NULL AND oi.BranchId <> b.BranchId);

-- 7. Hóa đơn đã trả nhưng thiếu thông tin thanh toán.
SELECT BillId, BranchId, Status, PaymentMethod, PaidAmount, PaidAt
FROM payment.Bill
WHERE Status = 'PAID'
  AND (PaymentMethod IS NULL OR PaidAmount IS NULL OR PaidAt IS NULL);

-- 8. Ca chấm công sai vòng đời duyệt.
SELECT ShiftAssignmentId, UserId, WorkDate, CheckInAt, CheckOutAt,
       AttendanceStatus, ApprovedBy, ApprovedAt
FROM hr.ShiftAssignment
WHERE (AttendanceStatus IS NULL AND (ApprovedBy IS NOT NULL OR ApprovedAt IS NOT NULL))
   OR (AttendanceStatus = 'PENDING' AND (ApprovedBy IS NOT NULL OR ApprovedAt IS NOT NULL))
   OR (AttendanceStatus IN ('APPROVED', 'REJECTED') AND (ApprovedBy IS NULL OR ApprovedAt IS NULL));

