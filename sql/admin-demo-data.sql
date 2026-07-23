USE CafeChain;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRAN;

DECLARE @now DATETIME2 = SYSUTCDATETIME();

MERGE org.Branch AS t
USING (VALUES
    ('CN04', N'Chi nhánh Phú Nhuận', N'128 Phan Xích Long, Phường 7, Quận Phú Nhuận', '02839990004', '07:00', '22:00'),
    ('CN05', N'Chi nhánh Tân Bình', N'62 Trường Sơn, Phường 2, Quận Tân Bình', '02839990005', '06:30', '22:30'),
    ('CN06', N'Chi nhánh Bình Thạnh', N'210 Nguyễn Gia Trí, Phường 25, Quận Bình Thạnh', '02839990006', '07:00', '23:00'),
    ('CN07', N'Chi nhánh Quận 7', N'45 Nguyễn Thị Thập, Phường Tân Phú, Quận 7', '02839990007', '07:00', '22:00'),
    ('CN08', N'Chi nhánh Gò Vấp', N'19 Quang Trung, Phường 10, Quận Gò Vấp', '02839990008', '06:30', '22:00')
) AS s(Code, Name, Address, Phone, OpenTime, CloseTime)
ON t.Code = s.Code
WHEN MATCHED THEN UPDATE SET
    Name = s.Name,
    Address = s.Address,
    Phone = s.Phone,
    OpenTime = CONVERT(time, s.OpenTime),
    CloseTime = CONVERT(time, s.CloseTime),
    IsActive = 1
WHEN NOT MATCHED THEN
    INSERT (Code, Name, Address, Phone, OpenTime, CloseTime, IsActive)
    VALUES (s.Code, s.Name, s.Address, s.Phone, CONVERT(time, s.OpenTime), CONVERT(time, s.CloseTime), 1);

MERGE catalog.Category AS t
USING (VALUES
    (N'Nước ép', 5),
    (N'Sinh tố', 6),
    (N'Trà sữa', 7),
    (N'Đồ ăn nhẹ', 8),
    (N'Cà phê đặc sản', 9)
) AS s(Name, SortOrder)
ON t.Name = s.Name
WHEN MATCHED THEN UPDATE SET SortOrder = s.SortOrder, IsActive = 1
WHEN NOT MATCHED THEN
    INSERT (Name, SortOrder, IsActive) VALUES (s.Name, s.SortOrder, 1);

MERGE catalog.Ingredient AS t
USING (VALUES
    (N'Hạt arabica rang mộc', N'g', 'RAW'),
    (N'Hạt robusta rang đậm', N'g', 'RAW'),
    (N'Sữa yến mạch', N'ml', 'RAW'),
    (N'Nước cam vắt', N'ml', 'RAW'),
    (N'Xoài chín', N'g', 'RAW'),
    (N'Dâu tây', N'g', 'RAW'),
    (N'Trà ô long', N'g', 'RAW'),
    (N'Trân châu khô', N'g', 'RAW'),
    (N'Phô mai kem', N'g', 'RAW'),
    (N'Bột cacao', N'g', 'RAW'),
    (N'Nền cold brew', N'ml', 'PREPPED'),
    (N'Siro đào nhà làm', N'ml', 'PREPPED'),
    (N'Sốt caramel', N'ml', 'PREPPED'),
    (N'Kem cheese', N'g', 'PREPPED'),
    (N'Trân châu đen', N'g', 'PREPPED'),
    (N'Thạch cà phê', N'g', 'PREPPED')
) AS s(Name, Unit, IngredientType)
ON t.Name = s.Name
WHEN MATCHED THEN UPDATE SET Unit = s.Unit, IngredientType = s.IngredientType, IsActive = 1
WHEN NOT MATCHED THEN
    INSERT (Name, Unit, IngredientType, IsActive) VALUES (s.Name, s.Unit, s.IngredientType, 1);

DECLARE @products TABLE (
    CategoryName NVARCHAR(100),
    ProductName NVARCHAR(150),
    BasePrice DECIMAL(12,2),
    PrepSeconds INT
);

INSERT INTO @products(CategoryName, ProductName, BasePrice, PrepSeconds) VALUES
    (N'Cà phê đặc sản', N'Espresso nóng', 30000, 240),
    (N'Cà phê đặc sản', N'Cappuccino nóng', 46000, 480),
    (N'Cà phê đặc sản', N'Cold Brew cam vàng', 52000, 360),
    (N'Nước ép', N'Nước ép cam tươi', 45000, 300),
    (N'Nước ép', N'Nước ép dâu xoài', 49000, 360),
    (N'Sinh tố', N'Sinh tố xoài sữa', 52000, 420),
    (N'Trà sữa', N'Trà sữa ô long kem cheese', 56000, 420),
    (N'Trà sữa', N'Trà sữa trân châu đen', 50000, 360),
    (N'Đồ ăn nhẹ', N'Bánh mì bơ tỏi', 32000, 240),
    (N'Đồ ăn nhẹ', N'Bánh phô mai nướng', 39000, 300);

MERGE catalog.Product AS t
USING (
    SELECT c.CategoryId, p.ProductName, p.BasePrice, p.PrepSeconds
    FROM @products p
    JOIN catalog.Category c ON c.Name = p.CategoryName
) AS s
ON t.Name = s.ProductName
WHEN MATCHED THEN UPDATE SET
    CategoryId = s.CategoryId,
    BasePrice = s.BasePrice,
    PrepSeconds = s.PrepSeconds,
    ImageUrl = '/assets/img/login-hero.svg',
    IsActive = 1,
    ShowOnHome = 1
WHEN NOT MATCHED THEN
    INSERT (CategoryId, Name, BasePrice, ImageUrl, IsActive, ShowOnHome, PrepSeconds)
    VALUES (s.CategoryId, s.ProductName, s.BasePrice, '/assets/img/login-hero.svg', 1, 1, s.PrepSeconds);

DECLARE @prep TABLE (Prepped NVARCHAR(120), RawName NVARCHAR(120), Quantity DECIMAL(12,3), YieldQty DECIMAL(12,3));
INSERT INTO @prep(Prepped, RawName, Quantity, YieldQty) VALUES
    (N'Nền cold brew', N'Hạt arabica rang mộc', 500, 2500),
    (N'Siro đào nhà làm', N'Đường', 600, 1800),
    (N'Sốt caramel', N'Đường', 700, 1600),
    (N'Kem cheese', N'Phô mai kem', 800, 1400),
    (N'Trân châu đen', N'Trân châu khô', 1000, 1800),
    (N'Thạch cà phê', N'Bột cacao', 400, 1600);

MERGE catalog.PrepRecipe AS t
USING (
    SELECT pre.IngredientId AS PreppedIngredientId,
           raw.IngredientId AS RawIngredientId,
           p.Quantity,
           p.YieldQty
    FROM @prep p
    JOIN catalog.Ingredient pre ON pre.Name = p.Prepped
    JOIN catalog.Ingredient raw ON raw.Name = p.RawName
) AS s
ON t.PreppedIngredientId = s.PreppedIngredientId AND t.RawIngredientId = s.RawIngredientId
WHEN MATCHED THEN UPDATE SET Quantity = s.Quantity, YieldQty = s.YieldQty
WHEN NOT MATCHED THEN
    INSERT (PreppedIngredientId, RawIngredientId, Quantity, YieldQty)
    VALUES (s.PreppedIngredientId, s.RawIngredientId, s.Quantity, s.YieldQty);

DECLARE @recipes TABLE (ProductName NVARCHAR(150), IngredientName NVARCHAR(120), Quantity DECIMAL(12,3));
INSERT INTO @recipes(ProductName, IngredientName, Quantity) VALUES
    (N'Espresso nóng', N'Hạt arabica rang mộc', 18),
    (N'Cappuccino nóng', N'Hạt arabica rang mộc', 18),
    (N'Cappuccino nóng', N'Sữa tươi', 140),
    (N'Cold Brew cam vàng', N'Nền cold brew', 180),
    (N'Cold Brew cam vàng', N'Nước cam vắt', 60),
    (N'Nước ép cam tươi', N'Nước cam vắt', 220),
    (N'Nước ép dâu xoài', N'Dâu tây', 80),
    (N'Nước ép dâu xoài', N'Xoài chín', 120),
    (N'Sinh tố xoài sữa', N'Xoài chín', 160),
    (N'Sinh tố xoài sữa', N'Sữa tươi', 120),
    (N'Trà sữa ô long kem cheese', N'Trà ô long', 8),
    (N'Trà sữa ô long kem cheese', N'Kem cheese', 45),
    (N'Trà sữa trân châu đen', N'Trà ô long', 8),
    (N'Trà sữa trân châu đen', N'Trân châu đen', 60),
    (N'Bánh mì bơ tỏi', N'Bánh croissant', 1),
    (N'Bánh phô mai nướng', N'Phô mai kem', 35);

MERGE catalog.ProductRecipe AS t
USING (
    SELECT p.ProductId, i.IngredientId, r.Quantity
    FROM @recipes r
    JOIN catalog.Product p ON p.Name = r.ProductName
    JOIN catalog.Ingredient i ON i.Name = r.IngredientName
) AS s
ON t.ProductId = s.ProductId AND t.IngredientId = s.IngredientId
WHEN MATCHED THEN UPDATE SET Quantity = s.Quantity
WHEN NOT MATCHED THEN
    INSERT (ProductId, IngredientId, Quantity) VALUES (s.ProductId, s.IngredientId, s.Quantity);

MERGE catalog.BranchMenu AS t
USING (
    SELECT b.BranchId, p.ProductId
    FROM org.Branch b
    CROSS JOIN catalog.Product p
    WHERE b.IsActive = 1 AND p.IsActive = 1
) AS s
ON t.BranchId = s.BranchId AND t.ProductId = s.ProductId
WHEN MATCHED THEN UPDATE SET IsAvailable = 1, Is86 = 0
WHEN NOT MATCHED THEN
    INSERT (BranchId, ProductId, IsAvailable, Is86) VALUES (s.BranchId, s.ProductId, 1, 0);

MERGE payment.Voucher AS t
USING (VALUES
    ('MORNING15', 'PERCENT', 15, 45000, 'CHAIN', NULL, DATEADD(DAY, -10, @now), DATEADD(DAY, 20, @now), 300, 1),
    ('FAMILY30K', 'FIXED', 30000, 150000, 'CHAIN', NULL, DATEADD(DAY, -5, @now), DATEADD(DAY, 45, @now), 120, 1),
    ('WEEKEND12', 'PERCENT', 12, 60000, 'CHAIN', NULL, DATEADD(DAY, 2, @now), DATEADD(DAY, 30, @now), 200, 1),
    ('SAIGON25', 'PERCENT', 25, 100000, 'CHAIN', NULL, DATEADD(DAY, -20, @now), DATEADD(DAY, -1, @now), 50, 1),
    ('DANANG20K', 'FIXED', 20000, 90000, 'CHAIN', NULL, DATEADD(DAY, 7, @now), DATEADD(DAY, 60, @now), 90, 1),
    ('BRANCHQ7', 'PERCENT', 18, 70000, 'BRANCH', (SELECT BranchId FROM org.Branch WHERE Code = 'CN07'), DATEADD(DAY, -1, @now), DATEADD(DAY, 25, @now), 80, 1),
    ('LATTE5K', 'FIXED', 5000, 35000, 'CHAIN', NULL, DATEADD(DAY, -30, @now), DATEADD(DAY, 10, @now), 500, 1),
    ('SUNSET10', 'PERCENT', 10, 50000, 'CHAIN', NULL, DATEADD(DAY, -60, @now), DATEADD(DAY, -5, @now), 100, 1),
    ('COLDBREW12', 'FIXED', 12000, 65000, 'CHAIN', NULL, DATEADD(DAY, -2, @now), DATEADD(DAY, 28, @now), 160, 1)
) AS s(Code, DiscountType, DiscountValue, MinOrderAmount, Scope, BranchId, StartDate, EndDate, UsageLimit, IsActive)
ON t.Code = s.Code
WHEN MATCHED THEN UPDATE SET
    DiscountType = s.DiscountType,
    DiscountValue = s.DiscountValue,
    MinOrderAmount = s.MinOrderAmount,
    Scope = s.Scope,
    BranchId = s.BranchId,
    StartDate = s.StartDate,
    EndDate = s.EndDate,
    UsageLimit = s.UsageLimit,
    IsActive = s.IsActive
WHEN NOT MATCHED THEN
    INSERT (Code, DiscountType, DiscountValue, MinOrderAmount, Scope, BranchId, StartDate, EndDate, UsageLimit, IsActive)
    VALUES (s.Code, s.DiscountType, s.DiscountValue, s.MinOrderAmount, s.Scope, s.BranchId, s.StartDate, s.EndDate, s.UsageLimit, s.IsActive);

MERGE inventory.BranchInventory AS t
USING (
    SELECT b.BranchId, i.IngredientId,
           CASE i.IngredientType WHEN 'PREPPED' THEN 1200 ELSE 5000 END AS QuantityOnHand,
           CASE i.IngredientType WHEN 'PREPPED' THEN 200 ELSE 500 END AS MinThreshold
    FROM org.Branch b
    CROSS JOIN catalog.Ingredient i
    WHERE b.IsActive = 1 AND i.IsActive = 1
) AS s
ON t.BranchId = s.BranchId AND t.IngredientId = s.IngredientId
WHEN MATCHED THEN UPDATE SET QuantityOnHand = s.QuantityOnHand, MinThreshold = s.MinThreshold, UpdatedAt = @now
WHEN NOT MATCHED THEN
    INSERT (BranchId, IngredientId, QuantityOnHand, MinThreshold, UpdatedAt)
    VALUES (s.BranchId, s.IngredientId, s.QuantityOnHand, s.MinThreshold, @now);

COMMIT TRAN;

SELECT N'Nhân sự' AS ScreenName, COUNT(*) AS Total FROM iam.[User]
UNION ALL SELECT N'Chi nhánh', COUNT(*) FROM org.Branch
UNION ALL SELECT N'Danh mục', COUNT(*) FROM catalog.Category
UNION ALL SELECT N'Sản phẩm', COUNT(*) FROM catalog.Product
UNION ALL SELECT N'Nguyên liệu', COUNT(*) FROM catalog.Ingredient
UNION ALL SELECT N'Công thức món', COUNT(DISTINCT ProductId) FROM catalog.ProductRecipe
UNION ALL SELECT N'Công thức pha sẵn', COUNT(DISTINCT PreppedIngredientId) FROM catalog.PrepRecipe
UNION ALL SELECT N'Voucher', COUNT(*) FROM payment.Voucher
UNION ALL SELECT N'Menu chi nhánh', COUNT(*) FROM catalog.BranchMenu;
GO
