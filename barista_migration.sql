/* =====================================================================
   MIGRATION cho module BARISTA (Pha chế)
   BẮT BUỘC chạy file này SAU khi import database.sql (idempotent — chạy
   lại nhiều lần an toàn nhờ COL_LENGTH/IF NOT EXISTS). database.sql CHƯA
   có các cột barista bên dưới (OrderDetails.StartedAt/CompletedAt,
   Orders.Priority), KHÔNG chạy file này thì các màn Barista sẽ lỗi
   "Invalid column name".
   ===================================================================== */
USE [MyCoffeeHouse];
GO

/* 1. Thời gian pha chế: mốc bắt đầu pha & hoàn thành cho từng món */
IF COL_LENGTH('OrderDetails', 'StartedAt') IS NULL
    ALTER TABLE [OrderDetails] ADD [StartedAt] datetime NULL;
GO
IF COL_LENGTH('OrderDetails', 'CompletedAt') IS NULL
    ALTER TABLE [OrderDetails] ADD [CompletedAt] datetime NULL;
GO

/* 2. Ưu tiên order: 0 = bình thường, số càng lớn càng ưu tiên (ghim lên đầu) */
IF COL_LENGTH('Orders', 'Priority') IS NULL
    ALTER TABLE [Orders] ADD [Priority] int NOT NULL DEFAULT ((0));
GO

/* Quy ước giá trị OrderDetails.ItemStatus:
   'Pending'    -> chờ pha
   'Preparing'  -> đang pha
   'Completed'  -> đã pha xong
   'OutOfStock' -> báo tạm hết, không pha được
*/
/* 3. Tài khoản Barista mẫu để đăng nhập test (RoleID = 4, IsActive = 1).
   Chỉ tạo nếu chưa tồn tại. Đăng nhập: barista / 123 */
IF NOT EXISTS (SELECT 1 FROM Users WHERE Username = 'barista')
    INSERT INTO Users (Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt)
    VALUES ('barista', '123', N'Nhân viên Pha chế', 'barista@mycoffee.com', '0900000004', 4, 1, GETDATE());
GO

PRINT 'Barista migration done.';
GO
