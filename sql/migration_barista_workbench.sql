/* Nâng cấp Quầy pha chế. Chạy một lần trên DB đã tạo từ schema cũ. */
USE CafeChain;
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name='CK_Order_Type')
    ALTER TABLE sales.Orders DROP CONSTRAINT CK_Order_Type;
ALTER TABLE sales.Orders ADD CONSTRAINT CK_Order_Type CHECK (OrderType IN ('DINE_IN','TAKEAWAY','DELIVERY'));
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name='CK_Item_Status')
    ALTER TABLE sales.OrderItem DROP CONSTRAINT CK_Item_Status;
GO
ALTER TABLE sales.OrderItem ALTER COLUMN Status VARCHAR(16) NOT NULL;
-- Bản workbench trước đây từng đặt tên trạng thái đang pha là IN_PROGRESS; nay thống nhất
-- về MAKING theo Contract #1. Đổi dữ liệu cũ trước khi dựng lại CHECK, nếu không CHECK sẽ fail.
UPDATE sales.OrderItem SET Status='MAKING' WHERE Status='IN_PROGRESS';
ALTER TABLE sales.OrderItem ADD CONSTRAINT CK_Item_Status
    CHECK (Status IN ('WAITING','MAKING','READY','PICKED_UP','SERVED','BLOCKED','CANCELLED','REMAKE'));
GO

IF COL_LENGTH('sales.OrderItem','BaristaId') IS NULL ALTER TABLE sales.OrderItem ADD BaristaId INT NULL;
IF COL_LENGTH('sales.OrderItem','PreparedBy') IS NULL ALTER TABLE sales.OrderItem ADD PreparedBy INT NULL;
IF COL_LENGTH('sales.OrderItem','HasIssue') IS NULL ALTER TABLE sales.OrderItem ADD HasIssue BIT NOT NULL CONSTRAINT DF_OI_HasIssue DEFAULT 0;
IF COL_LENGTH('sales.OrderItem','IssueReason') IS NULL ALTER TABLE sales.OrderItem ADD IssueReason NVARCHAR(255) NULL;
IF COL_LENGTH('sales.OrderItem','IssueReportedBy') IS NULL ALTER TABLE sales.OrderItem ADD IssueReportedBy INT NULL;
IF COL_LENGTH('sales.OrderItem','IssueReportedAt') IS NULL ALTER TABLE sales.OrderItem ADD IssueReportedAt DATETIME2 NULL;
IF COL_LENGTH('sales.OrderItem','RemakeCount') IS NULL ALTER TABLE sales.OrderItem ADD RemakeCount INT NOT NULL CONSTRAINT DF_OI_RemakeCount DEFAULT 0;
IF COL_LENGTH('sales.OrderItem','RemakeInventoryReserved') IS NULL ALTER TABLE sales.OrderItem ADD RemakeInventoryReserved BIT NOT NULL CONSTRAINT DF_OI_RemakeReserved DEFAULT 0;
IF COL_LENGTH('sales.OrderItem','HandoverLocation') IS NULL ALTER TABLE sales.OrderItem ADD HandoverLocation NVARCHAR(80) NULL;
IF COL_LENGTH('sales.OrderItem','PickedUpBy') IS NULL ALTER TABLE sales.OrderItem ADD PickedUpBy INT NULL;
IF COL_LENGTH('sales.OrderItem','PickedUpAt') IS NULL ALTER TABLE sales.OrderItem ADD PickedUpAt DATETIME2 NULL;
IF COL_LENGTH('sales.OrderItem','ServedAt') IS NULL ALTER TABLE sales.OrderItem ADD ServedAt DATETIME2 NULL;
GO

-- Bản trước đây gọi cột này là CompletedBy; nay thống nhất một khái niệm một cột là PreparedBy.
-- Chuyển dữ liệu lịch sử sang để KPI cá nhân không mất số liệu cũ. Cột CompletedBy KHÔNG bị xoá
-- ở đây (thao tác huỷ dữ liệu) — xoá thủ công sau khi đã đối chiếu xong.
IF COL_LENGTH('sales.OrderItem','CompletedBy') IS NOT NULL
    EXEC sp_executesql N'UPDATE sales.OrderItem SET PreparedBy = CompletedBy
                         WHERE PreparedBy IS NULL AND CompletedBy IS NOT NULL';
GO

-- Chạy SAU khi đã có cột BaristaId: món đang pha mà không có người nhận (dữ liệu từ schema cũ)
-- thì trả về hàng chờ, tránh món treo vô chủ mà không ai bấm xong được.
UPDATE sales.OrderItem SET Status='WAITING', StartedAt=NULL WHERE Status='MAKING' AND BaristaId IS NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_OI_Barista') ALTER TABLE sales.OrderItem ADD CONSTRAINT FK_OI_Barista FOREIGN KEY (BaristaId) REFERENCES iam.[User](UserId);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_OI_PreparedBy') ALTER TABLE sales.OrderItem ADD CONSTRAINT FK_OI_PreparedBy FOREIGN KEY (PreparedBy) REFERENCES iam.[User](UserId);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_OI_IssueBy') ALTER TABLE sales.OrderItem ADD CONSTRAINT FK_OI_IssueBy FOREIGN KEY (IssueReportedBy) REFERENCES iam.[User](UserId);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_OI_PickedUpBy') ALTER TABLE sales.OrderItem ADD CONSTRAINT FK_OI_PickedUpBy FOREIGN KEY (PickedUpBy) REFERENCES iam.[User](UserId);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_OrderItem_BaristaStatus' AND object_id=OBJECT_ID('sales.OrderItem')) CREATE INDEX IX_OrderItem_BaristaStatus ON sales.OrderItem(BaristaId,Status);
GO

IF OBJECT_ID('ops.OrderItemActionLog','U') IS NULL
BEGIN
    CREATE TABLE ops.OrderItemActionLog (
        OrderItemActionLogId BIGINT IDENTITY PRIMARY KEY,
        OrderItemId INT NOT NULL,
        BranchId INT NOT NULL,
        ActionType VARCHAR(24) NOT NULL,
        FromStatus VARCHAR(16) NULL,
        ToStatus VARCHAR(16) NULL,
        Reason NVARCHAR(255) NULL,
        PerformedBy INT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_OIAL_Item FOREIGN KEY (OrderItemId) REFERENCES sales.OrderItem(OrderItemId),
        CONSTRAINT FK_OIAL_Branch FOREIGN KEY (BranchId) REFERENCES org.Branch(BranchId),
        CONSTRAINT FK_OIAL_User FOREIGN KEY (PerformedBy) REFERENCES iam.[User](UserId)
    );
    CREATE INDEX IX_OrderItemAction_Item ON ops.OrderItemActionLog(OrderItemId, CreatedAt DESC);
END;
GO
