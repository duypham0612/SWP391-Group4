/* Cụm tồn kho - nhập hàng - ledger - prep - kiểm kê - hao hụt. Chỉ đọc dữ liệu. */
SET NOCOUNT ON;

DECLARE @BranchId int = 1;
DECLARE @FromUtc datetime2 = DATEADD(DAY, -7, SYSUTCDATETIME());
DECLARE @TopRows int = 200;

-- 1. Tồn hiện tại và cảnh báo.
SELECT bi.BranchId, b.Code AS BranchCode,
       i.IngredientId, i.Name AS IngredientName, i.IngredientType, i.Unit,
       bi.QuantityOnHand, bi.MinThreshold, bi.PrepTargetQty,
       CASE
           WHEN bi.QuantityOnHand <= 0 THEN 'OUT'
           WHEN bi.QuantityOnHand <= bi.MinThreshold THEN 'LOW'
           ELSE 'OK'
       END AS StockState,
       DATEADD(HOUR, 7, bi.UpdatedAt) AS UpdatedAtLocal
FROM inventory.BranchInventory bi
JOIN org.Branch b ON b.BranchId = bi.BranchId
JOIN catalog.Ingredient i ON i.IngredientId = bi.IngredientId
WHERE @BranchId IS NULL OR bi.BranchId = @BranchId
ORDER BY StockState DESC, i.Name;

-- 2. Sổ cái biến động tồn gần nhất.
SELECT TOP (@TopRows)
       it.InventoryTransactionId, it.BranchId,
       i.Name AS IngredientName, it.ChangeQty, i.Unit,
       it.TxnType, it.ReferenceType, it.ReferenceId,
       u.Username AS CreatedByUsername,
       DATEADD(HOUR, 7, it.CreatedAt) AS CreatedAtLocal
FROM inventory.InventoryTransaction it
JOIN catalog.Ingredient i ON i.IngredientId = it.IngredientId
LEFT JOIN iam.UserAccount u ON u.UserId = it.CreatedBy
WHERE (@BranchId IS NULL OR it.BranchId = @BranchId)
  AND it.CreatedAt >= @FromUtc
ORDER BY it.CreatedAt DESC, it.InventoryTransactionId DESC;

-- 3. Phiếu nhập theo batch và chi tiết dòng.
SELECT TOP (@TopRows)
       sr.ReceiptBatchId, sr.StockReceiptLineId, sr.BranchId,
       sr.DocumentDate, sr.Status, s.Name AS SupplierName,
       i.Name AS IngredientName,
       sr.EnteredQuantity, sr.UnitNameAtEntry, sr.FactorToBaseAtEntry,
       sr.BaseQuantity, sr.UnitCost,
       u.Username AS ReceivedByUsername,
       DATEADD(HOUR, 7, sr.CreatedAt) AS CreatedAtLocal
FROM inventory.StockReceiptLine sr
JOIN catalog.Ingredient i ON i.IngredientId = sr.IngredientId
LEFT JOIN inventory.Supplier s ON s.SupplierId = sr.SupplierId
JOIN iam.UserAccount u ON u.UserId = sr.ReceivedBy
WHERE (@BranchId IS NULL OR sr.BranchId = @BranchId)
  AND sr.CreatedAt >= @FromUtc
ORDER BY sr.CreatedAt DESC, sr.ReceiptBatchId, sr.StockReceiptLineId;

-- 4. Batch sơ chế.
SELECT TOP (@TopRows)
       pb.PrepBatchId, pb.BranchId, i.Name AS PreppedIngredient,
       pb.QuantityProduced, i.Unit, pb.Status,
       maker.Username AS MadeByUsername,
       DATEADD(HOUR, 7, pb.MadeAt) AS MadeAtLocal,
       DATEADD(HOUR, 7, pb.ExpiresAt) AS ExpiresAtLocal,
       pb.RequiresApproval, reviewer.Username AS ReviewedByUsername,
       pb.WriteOffWasteEntryId
FROM inventory.PrepBatch pb
JOIN catalog.Ingredient i ON i.IngredientId = pb.PreppedIngredientId
JOIN iam.UserAccount maker ON maker.UserId = pb.MadeBy
LEFT JOIN iam.UserAccount reviewer ON reviewer.UserId = pb.ReviewedBy
WHERE (@BranchId IS NULL OR pb.BranchId = @BranchId)
  AND pb.MadeAt >= @FromUtc
ORDER BY pb.MadeAt DESC, pb.PrepBatchId DESC;

-- 5. Kiểm kê và chênh lệch.
SELECT TOP (@TopRows)
       sa.StockAdjustmentId, sa.CountBatchId, sa.BranchId,
       i.Name AS IngredientName,
       sa.SystemBaseQty, sa.ActualBaseQty, sa.DiffQty, i.Unit,
       sa.CountedQuantity, sa.UnitNameAtCount, sa.FactorToBaseAtCount,
       counter.Username AS CountedByUsername,
       adjuster.Username AS AdjustedByUsername,
       DATEADD(HOUR, 7, sa.AdjustedAt) AS AdjustedAtLocal,
       sa.Reason
FROM inventory.StockAdjustment sa
JOIN catalog.Ingredient i ON i.IngredientId = sa.IngredientId
LEFT JOIN iam.UserAccount counter ON counter.UserId = sa.CountedBy
JOIN iam.UserAccount adjuster ON adjuster.UserId = sa.AdjustedBy
WHERE (@BranchId IS NULL OR sa.BranchId = @BranchId)
  AND sa.AdjustedAt >= @FromUtc
ORDER BY sa.AdjustedAt DESC, sa.StockAdjustmentId DESC;

-- 6. Hao hụt và remake.
SELECT TOP (@TopRows)
       w.WasteEntryId, w.EventGroupId, w.EventKind, w.Source, w.BranchId,
       p.Name AS ProductName, i.Name AS IngredientName,
       w.CupQuantity, w.Quantity, i.Unit,
       w.WasteType, w.CauseCode, w.CauseDetail, w.Status,
       creator.Username AS CreatedByUsername,
       DATEADD(HOUR, 7, w.CreatedAt) AS CreatedAtLocal,
       w.ReviewType, w.ReviewStatus, resolver.Username AS ResolvedByUsername
FROM inventory.WasteEntry w
JOIN catalog.Ingredient i ON i.IngredientId = w.IngredientId
LEFT JOIN catalog.Product p ON p.ProductId = w.ProductId
JOIN iam.UserAccount creator ON creator.UserId = w.CreatedBy
LEFT JOIN iam.UserAccount resolver ON resolver.UserId = w.ResolvedBy
WHERE (@BranchId IS NULL OR w.BranchId = @BranchId)
  AND w.CreatedAt >= @FromUtc
ORDER BY w.CreatedAt DESC, w.WasteEntryId DESC;

