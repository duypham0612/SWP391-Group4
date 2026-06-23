USE MyCoffeeHouse;
GO

-- Xóa dữ liệu cũ của bảng Tables (nếu có) để tránh trùng lặp khi chạy lại
DELETE FROM [Tables];
DBCC CHECKIDENT ('[Tables]', RESEED, 0); -- Reset lại ID tự tăng về 0
GO

-- ============================================================
-- INSERT DỮ LIỆU BÀN CHO CHI NHÁNH 1 (VD: Cầu Giấy)
-- ============================================================
INSERT INTO [Tables] ([BranchID], [TableName], [QRCodeURL], [Status])
VALUES
(1, N'Bàn 01', 'qr_branch1_tb1.png', 'Empty'),
(1, N'Bàn 02', 'qr_branch1_tb2.png', 'Occupied'),
(1, N'Bàn 03', 'qr_branch1_tb3.png', 'Empty'),
(1, N'Bàn 04', 'qr_branch1_tb4.png', 'Empty'),
(1, N'Bàn 05 (VIP)', 'qr_branch1_tb5.png', 'Occupied'),
(1, N'Bàn 06 (VIP)', 'qr_branch1_tb6.png', 'Empty'),
(1, N'Sân Thượng 1', 'qr_branch1_st1.png', 'Occupied'),
(1, N'Sân Thượng 2', 'qr_branch1_st2.png', 'Empty');
GO

-- ============================================================
-- INSERT DỮ LIỆU BÀN CHO CHI NHÁNH 2 (VD: Quận 1)
-- ============================================================
INSERT INTO [Tables] ([BranchID], [TableName], [QRCodeURL], [Status])
VALUES
(2, N'Tầng 1 - Bàn 1', 'qr_branch2_t1b1.png', 'Empty'),
(2, N'Tầng 1 - Bàn 2', 'qr_branch2_t1b2.png', 'Empty'),
(2, N'Tầng 1 - Bàn 3', 'qr_branch2_t1b3.png', 'Occupied'),
(2, N'Tầng 2 - Bàn 1', 'qr_branch2_t2b1.png', 'Empty'),
(2, N'Tầng 2 - Bàn 2', 'qr_branch2_t2b2.png', 'Occupied'),
(2, N'Ban công 1', 'qr_branch2_bc1.png', 'Empty');
GO

DELETE FROM Products;
DELETE FROM Categories;

DBCC CHECKIDENT ('Products', RESEED, 0);
DBCC CHECKIDENT ('Categories', RESEED, 0);
GO

-- ==========================================================
-- 2. THÊM DANH MỤC (Bằng tiếng Việt)
-- ==========================================================
SET IDENTITY_INSERT Categories ON;
INSERT INTO Categories (CategoryID, CategoryName, Description) VALUES
                                                                   (1, N'Cà Phê Máy Và Espresso', N'Cà phê máy và ủ lạnh'),
                                                                   (2, N'Frappuccino Và Đá Xay', N'Thức uống đá xay và sinh tố'),
                                                                   (3, N'Trà Và Thức Uống Giải Khát', N'Trà, Trà xanh và Thức uống trái cây'),
                                                                   (4, N'Sô Cô La & Thức Uống Truyền Thống', N'Cacao và thức uống truyền thống');
SET IDENTITY_INSERT Categories OFF;
GO

-- ==========================================================
-- 3. THÊM SẢN PHẨM (Tên bằng tiếng Việt)
-- ==========================================================
SET IDENTITY_INSERT Products ON;
INSERT INTO Products (ProductID, ProductName, CategoryID, BasePrice, IsAvailable) VALUES
-- Nhóm 1: Cà Phê Máy Và Espresso
(1, N'Cà Phê Latte', 1, 75000, 1),
(2, N'Cappuccino', 1, 75000, 1),
(3, N'Flat White', 1, 80000, 1),
(4, N'Mocha', 1, 85000, 1),
(5, N'Caramel Macchiato', 1, 85000, 1),
(6, N'Espresso Đá Đường Nâu Yến Mạch', 1, 90000, 1),
(7, N'Americano', 1, 60000, 1),
(8, N'Latte Dolce Á', 1, 80000, 1),
(9, N'Espresso (Solo)', 1, 40000, 1),
(10, N'Cold Brew', 1, 65000, 1),
(11, N'Cold Brew Bưởi Hồng Mật Ong', 1, 80000, 1),

-- Nhóm 2: Frappuccino Và Đá Xay
(12, N'Frappuccino Cà Phê', 2, 80000, 1),
(13, N'Frappuccino Caramel / Mocha', 2, 90000, 1),
(14, N'Frappuccino Java Chip', 2, 100000, 1),
(15, N'Frappuccino Kem Vanilla / Caramel', 2, 90000, 1),
(16, N'Frappuccino Kem Trà Xanh', 2, 100000, 1),
(17, N'Nước Ép Xoài Đam Mê', 2, 80000, 1),

-- Nhóm 3: Sô Cô La & Thức Uống Truyền Thống
(18, N'Sô Cô La Nóng Signature', 4, 75000, 1),
(19, N'Sữa Hấp / Sữa Đậu Nành', 4, 40000, 1),

-- Nhóm 4: Trà Và Thức Uống Giải Khát
(20, N'Latte Matcha Nguyên Chất', 3, 80000, 1),
(21, N'Trà Đen Bưởi Hồng Mật Ong', 3, 75000, 1),
(22, N'Đá Trà Xanh Dâu Tây Lemonade', 3, 65000, 1),
(23, N'Trà English Breakfast / Trà Chanh Bạc Hà', 3, 55000, 1),
(24, N'Strawberry Açai Lemonade', 3, 75000, 1),
(25, N'Pink Drink Strawberry Açai', 3, 80000, 1);
SET IDENTITY_INSERT Products OFF;
GO

SELECT * FROM [dbo].[Products]

USE MyCoffeeHouse;
GO
ALTER TABLE [Tables] ADD Capacity int NULL DEFAULT 4; -- Mặc định mỗi bàn 4 ghế
GO