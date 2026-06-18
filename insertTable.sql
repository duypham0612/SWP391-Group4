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


