-- Migration: thêm cột ServedAt vào sales.OrderItem cho DB đã tồn tại.
-- Mốc giao khách (PICKED_UP → SERVED); NULL khi nhân viên hoàn tác giao nhầm.
-- An toàn với dữ liệu cũ: cột NULL, không đặt DEFAULT, không đụng ràng buộc CHECK sẵn có.
-- Chạy MỘT LẦN trên DB CafeChain đang chạy (idempotent: bọc kiểm tra tồn tại cột).

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('sales.OrderItem') AND name = 'ServedAt'
)
BEGIN
    ALTER TABLE sales.OrderItem ADD ServedAt DATETIME2 NULL;
END
GO
