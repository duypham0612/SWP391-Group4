/*
   Dữ liệu demo cho màn Barista > Hao hụt & Làm lại.
   - Thêm 28 dòng ở CN01 để kiểm tra tìm kiếm realtime, lọc và phân trang.
   - Có đủ SPILL / EXPIRED / REMAKE / OTHER và 3 dòng VOIDED.
   - Ghi InventoryTransaction cùng lúc để không lệch tồn kho.
   Script có thể chạy lại an toàn: mỗi ngày Việt Nam chỉ thêm một bộ demo.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

DECLARE @branchId INT = (SELECT BranchId FROM org.Branch WHERE Code = 'CN01');
DECLARE @baristaId INT = (SELECT UserId FROM iam.[User] WHERE Username = 'barista1');
DECLARE @now DATETIME2 = SYSUTCDATETIME();
DECLARE @vnToday DATE = CONVERT(DATE, DATEADD(HOUR, 7, @now));
DECLARE @vnTodayStartUtc DATETIME2 = DATEADD(HOUR, -7, CAST(@vnToday AS DATETIME2));

IF @branchId IS NULL OR @baristaId IS NULL
BEGIN
    ROLLBACK TRANSACTION;
    THROW 50001, N'Không tìm thấy CN01 hoặc tài khoản barista1 để tạo dữ liệu demo.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM inventory.WasteLog
    WHERE BranchId = @branchId
      AND Reason LIKE N'DEMO-WASTE-PAGER:%'
      AND LoggedAt >= @vnTodayStartUtc
)
BEGIN
    ROLLBACK TRANSACTION;
    PRINT N'Dữ liệu demo Hao hụt & Làm lại cho hôm nay đã tồn tại — không thêm trùng.';
    RETURN;
END;

DECLARE @demo TABLE (
    Seq INT PRIMARY KEY,
    IngredientId INT NOT NULL,
    Quantity DECIMAL(12,3) NOT NULL,
    WasteType VARCHAR(12) NOT NULL,
    Reason NVARCHAR(255) NOT NULL,
    MinutesAgo INT NOT NULL,
    Status VARCHAR(10) NOT NULL
);

INSERT INTO @demo(Seq, IngredientId, Quantity, WasteType, Reason, MinutesAgo, Status) VALUES
 ( 1,  4, 180.000, 'SPILL',   N'DEMO-WASTE-PAGER: Đổ đá khi sang khay',                    1, 'ACTIVE'),
 ( 2,  2,  35.000, 'REMAKE',  N'DEMO-WASTE-PAGER: Làm lại Cà phê sữa - sai định lượng',    2, 'ACTIVE'),
 ( 3,  1,  18.000, 'REMAKE',  N'DEMO-WASTE-PAGER: Làm lại Cà phê sữa - sai định lượng',    2, 'ACTIVE'),
 ( 4, 10, 120.000, 'EXPIRED', N'DEMO-WASTE-PAGER: Sữa tươi quá thời gian mở nắp',           3, 'ACTIVE'),
 ( 5,  8,  45.000, 'OTHER',   N'DEMO-WASTE-PAGER: Mẫu thử chất lượng topping',              4, 'ACTIVE'),
 ( 6, 11,  12.000, 'SPILL',   N'DEMO-WASTE-PAGER: Rơi trà sen khi cân nguyên liệu',         5, 'ACTIVE'),
 ( 7,  7,  30.000, 'REMAKE',  N'DEMO-WASTE-PAGER: Làm lại Trà Đào - khách yêu cầu',          6, 'ACTIVE'),
 ( 8,  5,  25.000, 'REMAKE',  N'DEMO-WASTE-PAGER: Làm lại Trà Đào - khách yêu cầu',          6, 'ACTIVE'),
 ( 9,  3,  20.000, 'OTHER',   N'DEMO-WASTE-PAGER: Kiểm kê lệch cuối ca',                     7, 'VOIDED'),
 (10,  6, 150.000, 'SPILL',   N'DEMO-WASTE-PAGER: Cold Brew bị đổ khi vệ sinh vòi',          8, 'ACTIVE'),
 (11, 18,   8.000, 'EXPIRED', N'DEMO-WASTE-PAGER: Bột Matcha hết hạn sử dụng',               9, 'ACTIVE'),
 (12, 19,  16.000, 'REMAKE',  N'DEMO-WASTE-PAGER: Làm lại Chocolate đá xay - lỗi chất lượng',10, 'ACTIVE'),
 (13, 17,  40.000, 'REMAKE',  N'DEMO-WASTE-PAGER: Làm lại Chocolate đá xay - lỗi chất lượng',10, 'ACTIVE'),
 (14, 13,  30.000, 'SPILL',   N'DEMO-WASTE-PAGER: Rơi vải ngâm khi thay hộp topping',       11, 'ACTIVE'),
 (15, 15,  25.000, 'OTHER',   N'DEMO-WASTE-PAGER: Mẫu thử công thức trà gừng',              12, 'ACTIVE'),
 (16, 12,  10.000, 'EXPIRED', N'DEMO-WASTE-PAGER: Trà đen bảo quản không đúng nhiệt độ',    13, 'VOIDED'),
 (17, 16,  18.000, 'SPILL',   N'DEMO-WASTE-PAGER: Vụn Cookie rơi khi thao tác',             14, 'ACTIVE'),
 (18,  9,  35.000, 'REMAKE',  N'DEMO-WASTE-PAGER: Làm lại Kem cheese - pha chưa đạt',       15, 'ACTIVE'),
 (19, 14,  15.000, 'OTHER',   N'DEMO-WASTE-PAGER: Điều chỉnh mẻ thử gừng mật ong',           16, 'ACTIVE'),
 (20,  2,  50.000, 'SPILL',   N'DEMO-WASTE-PAGER: Đổ sữa đặc khi thay chai',                17, 'ACTIVE'),
 (21,  1,  20.000, 'EXPIRED', N'DEMO-WASTE-PAGER: Cà phê hạt lưu kho quá hạn',              18, 'ACTIVE'),
 (22,  4, 220.000, 'REMAKE',  N'DEMO-WASTE-PAGER: Làm lại Matcha đá xay - khách yêu cầu',    19, 'ACTIVE'),
 (23, 18,   6.000, 'REMAKE',  N'DEMO-WASTE-PAGER: Làm lại Matcha đá xay - khách yêu cầu',    19, 'ACTIVE'),
 (24,  3,  30.000, 'OTHER',   N'DEMO-WASTE-PAGER: Kiểm kê lệch thùng đường',                20, 'ACTIVE'),
 (25, 10,  80.000, 'EXPIRED', N'DEMO-WASTE-PAGER: Sữa tươi hỏng do bảo quản lỗi',           21, 'VOIDED'),
 (26,  5,  40.000, 'SPILL',   N'DEMO-WASTE-PAGER: Đổ đào ngâm khi sang khay',               22, 'ACTIVE'),
 (27,  7,  45.000, 'REMAKE',  N'DEMO-WASTE-PAGER: Làm lại Trà Đào - sai công thức',         23, 'ACTIVE'),
 (28, 11,   9.000, 'OTHER',   N'DEMO-WASTE-PAGER: Mẫu thử định lượng trà sen',               24, 'ACTIVE');

DECLARE @inserted TABLE (
    WasteLogId INT NOT NULL,
    IngredientId INT NOT NULL,
    Quantity DECIMAL(12,3) NOT NULL,
    Status VARCHAR(10) NOT NULL,
    LoggedAt DATETIME2 NOT NULL
);

INSERT INTO inventory.WasteLog(BranchId, IngredientId, Quantity, WasteType, Reason, LoggedBy, LoggedAt, Status, VoidedAt)
OUTPUT inserted.WasteLogId, inserted.IngredientId, inserted.Quantity, inserted.Status, inserted.LoggedAt
    INTO @inserted(WasteLogId, IngredientId, Quantity, Status, LoggedAt)
SELECT @branchId, d.IngredientId, d.Quantity, d.WasteType, d.Reason, @baristaId,
       DATEADD(MINUTE, -d.MinutesAgo, @now), d.Status,
       CASE WHEN d.Status = 'VOIDED' THEN DATEADD(SECOND, 30, DATEADD(MINUTE, -d.MinutesAgo, @now)) END
FROM @demo d;

-- Ghi trừ tồn cho mọi dòng; dòng đã huỷ có thêm txn bù để tổng chênh lệch bằng 0.
INSERT INTO inventory.InventoryTransaction(BranchId, IngredientId, ChangeQty, TxnType, RefTable, RefId, CreatedBy, CreatedAt)
SELECT @branchId, i.IngredientId, -i.Quantity, 'WASTE', 'WasteLog', i.WasteLogId, @baristaId, i.LoggedAt
FROM @inserted i;

INSERT INTO inventory.InventoryTransaction(BranchId, IngredientId, ChangeQty, TxnType, RefTable, RefId, CreatedBy, CreatedAt)
SELECT @branchId, i.IngredientId, i.Quantity, 'WASTE', 'WasteLog', i.WasteLogId, @baristaId, DATEADD(SECOND, 30, i.LoggedAt)
FROM @inserted i
WHERE i.Status = 'VOIDED';

-- Đồng bộ cache tồn kho với sổ cái cho các dòng còn hiệu lực.
UPDATE bi
SET bi.QuantityOnHand = bi.QuantityOnHand - d.TotalQuantity,
    bi.UpdatedAt = @now
FROM inventory.BranchInventory bi
JOIN (
    SELECT IngredientId, SUM(Quantity) AS TotalQuantity
    FROM @inserted
    WHERE Status = 'ACTIVE'
    GROUP BY IngredientId
) d ON d.IngredientId = bi.IngredientId
WHERE bi.BranchId = @branchId;

COMMIT TRANSACTION;

PRINT N'Đã thêm 28 dữ liệu demo cho Hao hụt & Làm lại tại CN01.';
