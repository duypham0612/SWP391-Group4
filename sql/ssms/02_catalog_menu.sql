/* Cụm danh mục - sản phẩm - công thức - menu chi nhánh. Chỉ đọc dữ liệu. */
SET NOCOUNT ON;

DECLARE @BranchId int = 1;
DECLARE @OnlyActive bit = 0;

-- 1. Danh mục và sản phẩm.
SELECT c.CategoryId, c.Name AS CategoryName, c.SortOrder AS CategorySort,
       p.ProductId, p.Name AS ProductName, p.BasePrice, p.PrepSeconds,
       p.ShowOnHome, p.HomeSortOrder, p.IsActive
FROM catalog.Category c
LEFT JOIN catalog.Product p ON p.CategoryId = c.CategoryId
WHERE @OnlyActive = 0 OR (c.IsActive = 1 AND p.IsActive = 1)
ORDER BY c.SortOrder, c.CategoryId, p.HomeSortOrder, p.ProductId;

-- 2. Menu thực tế của chi nhánh và giá đang áp dụng.
SELECT bm.BranchId, b.Code AS BranchCode, p.ProductId, p.Name AS ProductName,
       p.BasePrice, bm.LocalPrice,
       COALESCE(bm.LocalPrice, p.BasePrice) AS EffectivePrice,
       bm.IsListed, bm.IsTemporarilyUnavailable,
       bm.BlockReason, bm.BlockStatus, bm.BackInEta,
       requester.Username AS BlockRequestedBy,
       reviewer.Username AS BlockReviewedBy
FROM catalog.BranchMenu bm
JOIN org.Branch b ON b.BranchId = bm.BranchId
JOIN catalog.Product p ON p.ProductId = bm.ProductId
LEFT JOIN iam.UserAccount requester ON requester.UserId = bm.BlockRequestedBy
LEFT JOIN iam.UserAccount reviewer ON reviewer.UserId = bm.BlockReviewedBy
WHERE @BranchId IS NULL OR bm.BranchId = @BranchId
ORDER BY bm.BranchId, p.Name;

-- 3. Nhóm tùy chọn và từng lựa chọn của sản phẩm.
SELECT p.ProductId, p.Name AS ProductName,
       mg.ModifierGroupId, mg.Name AS ModifierGroupName,
       mg.IsRequired, mg.MinSelect, mg.MaxSelect, mg.SortOrder,
       mo.ModifierOptionId, mo.Name AS ModifierOptionName,
       mo.PriceDelta, mo.IsActive
FROM catalog.Product p
JOIN catalog.ModifierGroup mg ON mg.ProductId = p.ProductId
LEFT JOIN catalog.ModifierOption mo ON mo.ModifierGroupId = mg.ModifierGroupId
ORDER BY p.ProductId, mg.SortOrder, mg.ModifierGroupId, mo.ModifierOptionId;

-- 4. Công thức đa hình: PRODUCT, PREPPED hoặc MODIFIER.
SELECT r.RecipeId, r.OwnerType, r.OwnerId,
       CASE r.OwnerType
           WHEN 'PRODUCT' THEN p.Name
           WHEN 'PREPPED' THEN prepared.Name
           WHEN 'MODIFIER' THEN mo.Name
       END AS OwnerName,
       i.IngredientId, i.Name AS IngredientName, r.Quantity, i.Unit,
       i.IngredientType
FROM catalog.Recipe r
JOIN catalog.Ingredient i ON i.IngredientId = r.IngredientId
LEFT JOIN catalog.Product p ON r.OwnerType = 'PRODUCT' AND p.ProductId = r.OwnerId
LEFT JOIN catalog.Ingredient prepared ON r.OwnerType = 'PREPPED' AND prepared.IngredientId = r.OwnerId
LEFT JOIN catalog.ModifierOption mo ON r.OwnerType = 'MODIFIER' AND mo.ModifierOptionId = r.OwnerId
ORDER BY r.OwnerType, r.OwnerId, i.Name;

-- 5. Nguyên liệu và quy đổi đơn vị nhập hàng.
SELECT IngredientId, Name, IngredientType, Unit AS BaseUnit,
       PurchaseUnitName, PurchaseFactorToBase, ShelfLifeMinutes,
       PrepYieldQty, IsActive
FROM catalog.Ingredient
ORDER BY IngredientType, Name;

