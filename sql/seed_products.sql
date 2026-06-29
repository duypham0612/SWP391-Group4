/* ============================================================================
   SEED chất lượng — Menu + ảnh sản phẩm (catalog.Product.ImageUrl).
   Idempotent: chạy lại nhiều lần không tạo trùng (khoá theo Name).
   Ảnh = SVG cục bộ /assets/img/products/*.svg (offline-safe, theo theme café).
   Sau khi sửa ảnh thật, dùng form Admin > Sản phẩm để đổi ImageUrl.
   ============================================================================ */
USE CafeChain;
GO

-- Helper upsert: thêm product nếu chưa có (theo Name), rồi set giá + ảnh + category.
-- (SQL Server: dùng MERGE đơn giản theo Name.)
MERGE catalog.Product AS tgt
USING (VALUES
    -- CategoryId, Name, BasePrice, ImageSlug
    (1, N'Cà phê sữa',        29000, 'ca-phe-sua'),
    (1, N'Cold Brew',         45000, 'cold-brew'),
    (1, N'Cà phê đen',        25000, 'ca-phe-den'),
    (1, N'Bạc xỉu',           32000, 'bac-xiu'),
    (1, N'Cappuccino',        45000, 'cappuccino'),
    (1, N'Latte',             49000, 'latte'),
    (2, N'Trà Đào',           39000, 'tra-dao'),
    (2, N'Trà sen vàng',      42000, 'tra-sen-vang'),
    (2, N'Trà vải',           42000, 'tra-vai'),
    (2, N'Trà gừng mật ong',  38000, 'tra-gung'),
    (3, N'Đá xay Cookie',     55000, 'da-xay-cookie'),
    (3, N'Matcha đá xay',     55000, 'matcha-da-xay'),
    (3, N'Chocolate đá xay',  52000, 'choco-da-xay')
) AS src (CategoryId, Name, BasePrice, ImageSlug)
ON tgt.Name = src.Name
WHEN MATCHED THEN
    UPDATE SET tgt.ImageUrl = '/assets/img/products/' + src.ImageSlug + '.svg',
               tgt.CategoryId = src.CategoryId,
               tgt.IsActive = 1
WHEN NOT MATCHED THEN
    INSERT (CategoryId, Name, BasePrice, ImageUrl, IsActive)
    VALUES (src.CategoryId, src.Name, src.BasePrice,
            '/assets/img/products/' + src.ImageSlug + '.svg', 1);
GO

-- Publish toàn bộ product đang active vào menu chi nhánh 1 (nếu chưa có dòng) — IsAvailable=1.
INSERT INTO catalog.BranchMenu (BranchId, ProductId, IsAvailable)
SELECT 1, p.ProductId, 1
FROM catalog.Product p
WHERE p.IsActive = 1
  AND NOT EXISTS (SELECT 1 FROM catalog.BranchMenu bm WHERE bm.BranchId = 1 AND bm.ProductId = p.ProductId);
GO

PRINT 'Seed products done. Tổng product/menu:';
SELECT CONCAT('products=', (SELECT COUNT(*) FROM catalog.Product),
              ' co_anh=',  (SELECT COUNT(*) FROM catalog.Product WHERE ImageUrl IS NOT NULL),
              ' branchmenu_CN1=', (SELECT COUNT(*) FROM catalog.BranchMenu WHERE BranchId=1));
GO
