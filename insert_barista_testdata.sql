/* ============================================================
   DỮ LIỆU TEST cho module BARISTA (Pha chế)
   Tạo sẵn vài order + món với đủ trạng thái để demo các màn hình:
   Chờ pha / Đang pha / Hoàn thành + 1 order được ghim ưu tiên.

   YÊU CẦU chạy TRƯỚC (theo thứ tự):
     1) database.sql
     2) insertTable_Product.sql   (cần Products 1-5, Tables 1-3)
     3) barista_migration.sql     (cần cột StartedAt/CompletedAt/Priority)
   Cần sẵn: User #1 (admin) làm CashierID.

   Ghi chú:
   - Dùng GETDATE() tương đối -> dữ liệu luôn thuộc "hôm nay".
   - Dùng SCOPE_IDENTITY() để lấy OrderID tự tăng, không hard-code OrderID.
   - Có thể chạy lại nhiều lần (mỗi lần thêm 1 bộ order test mới).
   ============================================================ */
USE [MyCoffeeHouse];
GO

DECLARE @o1 INT, @o2 INT, @o3 INT, @o4 INT;

/* Order A: chờ pha (gọi ~8 phút trước) */
INSERT INTO Orders (BranchID, TableID, CashierID, OrderType, TotalAmount, DiscountAmount, FinalAmount, OrderStatus, OrderDate, Priority)
VALUES (1, 1, 1, 'Dine-in', 225000, 0, 225000, 'Pending', DATEADD(MINUTE, -8, GETDATE()), 0);
SET @o1 = SCOPE_IDENTITY();
INSERT INTO OrderDetails (OrderID, ProductID, Quantity, UnitPrice, Note, ItemStatus, StartedAt, CompletedAt) VALUES
    (@o1, 1, 2, 75000, N'Ít đường', 'Pending', NULL, NULL),
    (@o1, 2, 1, 75000, NULL,        'Pending', NULL, NULL);

/* Order B: 1 món đang pha + 1 chờ (gọi ~5 phút trước) */
INSERT INTO Orders (BranchID, TableID, CashierID, OrderType, TotalAmount, DiscountAmount, FinalAmount, OrderStatus, OrderDate, Priority)
VALUES (1, 2, 1, 'Dine-in', 250000, 0, 250000, 'Pending', DATEADD(MINUTE, -5, GETDATE()), 0);
SET @o2 = SCOPE_IDENTITY();
INSERT INTO OrderDetails (OrderID, ProductID, Quantity, UnitPrice, Note, ItemStatus, StartedAt, CompletedAt) VALUES
    (@o2, 3, 1, 80000, N'Không đá', 'Preparing', DATEADD(MINUTE, -3, GETDATE()), NULL),
    (@o2, 4, 2, 85000, NULL,        'Pending',   NULL, NULL);

/* Order C: được GHIM ưu tiên (Priority = 1), gọi ~2 phút trước */
INSERT INTO Orders (BranchID, TableID, CashierID, OrderType, TotalAmount, DiscountAmount, FinalAmount, OrderStatus, OrderDate, Priority)
VALUES (1, 3, 1, 'Dine-in', 160000, 0, 160000, 'Pending', DATEADD(MINUTE, -2, GETDATE()), 1);
SET @o3 = SCOPE_IDENTITY();
INSERT INTO OrderDetails (OrderID, ProductID, Quantity, UnitPrice, Note, ItemStatus, StartedAt, CompletedAt) VALUES
    (@o3, 5, 1, 85000, N'Nóng, mang gấp', 'Pending', NULL, NULL),
    (@o3, 1, 1, 75000, NULL,              'Pending', NULL, NULL);

/* Order D: 2 món đã hoàn thành (cho màn Hoàn thành / Lịch sử / Gọi món), gọi ~7 phút trước */
INSERT INTO Orders (BranchID, TableID, CashierID, OrderType, TotalAmount, DiscountAmount, FinalAmount, OrderStatus, OrderDate, Priority)
VALUES (1, 1, 1, 'Dine-in', 235000, 0, 235000, 'Pending', DATEADD(MINUTE, -7, GETDATE()), 0);
SET @o4 = SCOPE_IDENTITY();
INSERT INTO OrderDetails (OrderID, ProductID, Quantity, UnitPrice, Note, ItemStatus, StartedAt, CompletedAt) VALUES
    (@o4, 2, 1, 75000, NULL, 'Completed', DATEADD(MINUTE, -5, GETDATE()), DATEADD(MINUTE, -1, GETDATE())),
    (@o4, 3, 2, 80000, NULL, 'Completed', DATEADD(MINUTE, -6, GETDATE()), DATEADD(MINUTE, -2, GETDATE()));

PRINT N'Da them du lieu test Barista: 4 order (5 Pending, 1 Preparing, 2 Completed, 1 order uu tien).';
GO
