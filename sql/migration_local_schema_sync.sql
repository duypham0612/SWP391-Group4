/*
   Additive schema sync for an existing CafeChain database created before the
   consolidated KDS/barista schema. This script is idempotent and preserves data.
*/
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF COL_LENGTH('org.Branch','PeakThresholdCups') IS NULL
        ALTER TABLE org.Branch ADD PeakThresholdCups INT NOT NULL CONSTRAINT DF_Branch_PeakThresholdCups DEFAULT (0) WITH VALUES;

    IF COL_LENGTH('catalog.Product','PrepSeconds') IS NULL
        ALTER TABLE catalog.Product ADD PrepSeconds INT NOT NULL CONSTRAINT DF_Product_PrepSeconds DEFAULT (720) WITH VALUES;
    IF COL_LENGTH('catalog.Product','SizeEnabled') IS NULL
        ALTER TABLE catalog.Product ADD SizeEnabled BIT NOT NULL CONSTRAINT DF_Product_SizeEnabled DEFAULT (1) WITH VALUES;
    IF COL_LENGTH('catalog.Product','SizeSDelta') IS NULL
        ALTER TABLE catalog.Product ADD SizeSDelta DECIMAL(12,2) NOT NULL CONSTRAINT DF_Product_SizeSDelta DEFAULT (0) WITH VALUES;
    IF COL_LENGTH('catalog.Product','SizeMDelta') IS NULL
        ALTER TABLE catalog.Product ADD SizeMDelta DECIMAL(12,2) NOT NULL CONSTRAINT DF_Product_SizeMDelta DEFAULT (0) WITH VALUES;
    IF COL_LENGTH('catalog.Product','SizeLDelta') IS NULL
        ALTER TABLE catalog.Product ADD SizeLDelta DECIMAL(12,2) NOT NULL CONSTRAINT DF_Product_SizeLDelta DEFAULT (5000) WITH VALUES;

    IF COL_LENGTH('sales.Orders','PickupCode') IS NULL
        ALTER TABLE sales.Orders ADD PickupCode VARCHAR(8) NULL;

    IF COL_LENGTH('sales.DiningTable','Capacity') IS NULL
        ALTER TABLE sales.DiningTable ADD Capacity INT NOT NULL CONSTRAINT DF_DiningTable_Capacity DEFAULT (4) WITH VALUES;
    IF COL_LENGTH('sales.DiningTable','IsVisible') IS NULL
        ALTER TABLE sales.DiningTable ADD IsVisible BIT NOT NULL CONSTRAINT DF_DiningTable_IsVisible DEFAULT (1) WITH VALUES;
    IF COL_LENGTH('sales.DiningTable','MergedIntoTableId') IS NULL
        ALTER TABLE sales.DiningTable ADD MergedIntoTableId INT NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID('sales.DiningTable') AND name='CK_DiningTable_Capacity')
        EXEC(N'ALTER TABLE sales.DiningTable ADD CONSTRAINT CK_DiningTable_Capacity CHECK (Capacity BETWEEN 1 AND 30)');
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID('sales.DiningTable') AND name='CK_DT_NotSelfMerged')
        EXEC(N'ALTER TABLE sales.DiningTable ADD CONSTRAINT CK_DT_NotSelfMerged CHECK (MergedIntoTableId IS NULL OR MergedIntoTableId <> DiningTableId)');
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID('sales.DiningTable') AND name='FK_DT_MergedInto')
        EXEC(N'ALTER TABLE sales.DiningTable ADD CONSTRAINT FK_DT_MergedInto FOREIGN KEY (MergedIntoTableId) REFERENCES sales.DiningTable(DiningTableId)');
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('sales.DiningTable') AND name='IX_DiningTable_Merged')
        EXEC(N'CREATE INDEX IX_DiningTable_Merged ON sales.DiningTable(MergedIntoTableId)');

    IF COL_LENGTH('sales.OrderItem','Size') IS NULL
        ALTER TABLE sales.OrderItem ADD Size VARCHAR(1) NOT NULL CONSTRAINT DF_OrderItem_Size DEFAULT ('M') WITH VALUES;
    IF COL_LENGTH('sales.OrderItem','IceLevel') IS NULL
        ALTER TABLE sales.OrderItem ADD IceLevel NVARCHAR(20) NOT NULL CONSTRAINT DF_OrderItem_IceLevel DEFAULT (N'Bình thường') WITH VALUES;
    IF COL_LENGTH('sales.OrderItem','SugarLevel') IS NULL
        ALTER TABLE sales.OrderItem ADD SugarLevel VARCHAR(5) NOT NULL CONSTRAINT DF_OrderItem_SugarLevel DEFAULT ('100%') WITH VALUES;
    IF COL_LENGTH('sales.OrderItem','BaristaId') IS NULL ALTER TABLE sales.OrderItem ADD BaristaId INT NULL;
    IF COL_LENGTH('sales.OrderItem','PreparedBy') IS NULL ALTER TABLE sales.OrderItem ADD PreparedBy INT NULL;
    IF COL_LENGTH('sales.OrderItem','HasIssue') IS NULL
        ALTER TABLE sales.OrderItem ADD HasIssue BIT NOT NULL CONSTRAINT DF_OrderItem_HasIssue DEFAULT (0) WITH VALUES;
    IF COL_LENGTH('sales.OrderItem','IssueReason') IS NULL ALTER TABLE sales.OrderItem ADD IssueReason NVARCHAR(255) NULL;
    IF COL_LENGTH('sales.OrderItem','IssueReportedBy') IS NULL ALTER TABLE sales.OrderItem ADD IssueReportedBy INT NULL;
    IF COL_LENGTH('sales.OrderItem','IssueReportedAt') IS NULL ALTER TABLE sales.OrderItem ADD IssueReportedAt DATETIME2 NULL;
    IF COL_LENGTH('sales.OrderItem','RemakeCount') IS NULL
        ALTER TABLE sales.OrderItem ADD RemakeCount INT NOT NULL CONSTRAINT DF_OrderItem_RemakeCount DEFAULT (0) WITH VALUES;
    IF COL_LENGTH('sales.OrderItem','RemakeInventoryReserved') IS NULL
        ALTER TABLE sales.OrderItem ADD RemakeInventoryReserved BIT NOT NULL CONSTRAINT DF_OrderItem_RemakeInventoryReserved DEFAULT (0) WITH VALUES;
    IF COL_LENGTH('sales.OrderItem','HandoverLocation') IS NULL ALTER TABLE sales.OrderItem ADD HandoverLocation NVARCHAR(80) NULL;
    IF COL_LENGTH('sales.OrderItem','PickedUpBy') IS NULL ALTER TABLE sales.OrderItem ADD PickedUpBy INT NULL;
    IF COL_LENGTH('sales.OrderItem','PickedUpAt') IS NULL ALTER TABLE sales.OrderItem ADD PickedUpAt DATETIME2 NULL;
    IF COL_LENGTH('sales.OrderItem','ServedAt') IS NULL ALTER TABLE sales.OrderItem ADD ServedAt DATETIME2 NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID('catalog.Product') AND name='CK_Product_SizeSDelta')
        EXEC(N'ALTER TABLE catalog.Product ADD CONSTRAINT CK_Product_SizeSDelta CHECK (SizeSDelta >= 0)');
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID('catalog.Product') AND name='CK_Product_SizeMDelta')
        EXEC(N'ALTER TABLE catalog.Product ADD CONSTRAINT CK_Product_SizeMDelta CHECK (SizeMDelta >= 0)');
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID('catalog.Product') AND name='CK_Product_SizeLDelta')
        EXEC(N'ALTER TABLE catalog.Product ADD CONSTRAINT CK_Product_SizeLDelta CHECK (SizeLDelta >= 0)');
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID('sales.OrderItem') AND name='CK_OrderItem_Size')
        EXEC(N'ALTER TABLE sales.OrderItem ADD CONSTRAINT CK_OrderItem_Size CHECK (Size IN (''S'',''M'',''L''))');

    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID('sales.OrderItem') AND name='CK_Item_Status')
        ALTER TABLE sales.OrderItem DROP CONSTRAINT CK_Item_Status;
    ALTER TABLE sales.OrderItem ADD CONSTRAINT CK_Item_Status
        CHECK (Status IN ('WAITING','MAKING','READY','PICKED_UP','SERVED','BLOCKED','CANCELLED','REMAKE'));

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID('sales.OrderItem') AND name='FK_OI_Barista')
        EXEC(N'ALTER TABLE sales.OrderItem ADD CONSTRAINT FK_OI_Barista FOREIGN KEY (BaristaId) REFERENCES iam.[User](UserId)');
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID('sales.OrderItem') AND name='FK_OI_PreparedBy')
        EXEC(N'ALTER TABLE sales.OrderItem ADD CONSTRAINT FK_OI_PreparedBy FOREIGN KEY (PreparedBy) REFERENCES iam.[User](UserId)');
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID('sales.OrderItem') AND name='FK_OI_IssueBy')
        EXEC(N'ALTER TABLE sales.OrderItem ADD CONSTRAINT FK_OI_IssueBy FOREIGN KEY (IssueReportedBy) REFERENCES iam.[User](UserId)');
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID('sales.OrderItem') AND name='FK_OI_PickedUpBy')
        EXEC(N'ALTER TABLE sales.OrderItem ADD CONSTRAINT FK_OI_PickedUpBy FOREIGN KEY (PickedUpBy) REFERENCES iam.[User](UserId)');

    IF OBJECT_ID('catalog.MenuBlockRequest','U') IS NULL
        EXEC(N'CREATE TABLE catalog.MenuBlockRequest (
            RequestId INT IDENTITY PRIMARY KEY, BranchId INT NOT NULL, ProductId INT NOT NULL,
            Reason VARCHAR(20) NOT NULL, Note NVARCHAR(255) NULL, BackInEta DATETIME2 NULL,
            RequestedBy INT NOT NULL, RequestedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
            ReopenRequestedAt DATETIME2 NULL, Status VARCHAR(10) NOT NULL DEFAULT ''PENDING'',
            ReviewedBy INT NULL, ReviewedAt DATETIME2 NULL, ReviewNote NVARCHAR(255) NULL, ClosedAt DATETIME2 NULL,
            CONSTRAINT FK_MBR_Branch FOREIGN KEY (BranchId) REFERENCES org.Branch(BranchId),
            CONSTRAINT FK_MBR_Product FOREIGN KEY (ProductId) REFERENCES catalog.Product(ProductId),
            CONSTRAINT FK_MBR_ReqBy FOREIGN KEY (RequestedBy) REFERENCES iam.[User](UserId),
            CONSTRAINT FK_MBR_RevBy FOREIGN KEY (ReviewedBy) REFERENCES iam.[User](UserId),
            CONSTRAINT CK_MBR_Status CHECK (Status IN (''PENDING'',''APPROVED'',''REJECTED'',''RESOLVED''))
        )');
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('catalog.MenuBlockRequest') AND name='UX_MenuBlockRequest_Open')
        EXEC(N'CREATE UNIQUE INDEX UX_MenuBlockRequest_Open ON catalog.MenuBlockRequest(BranchId,ProductId) WHERE ClosedAt IS NULL');
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('catalog.MenuBlockRequest') AND name='IX_MenuBlockRequest_Queue')
        EXEC(N'CREATE INDEX IX_MenuBlockRequest_Queue ON catalog.MenuBlockRequest(BranchId,ClosedAt,BackInEta)');

    IF OBJECT_ID('ops.OrderItemActionLog','U') IS NULL
        EXEC(N'CREATE TABLE ops.OrderItemActionLog (
            OrderItemActionLogId BIGINT IDENTITY PRIMARY KEY, OrderItemId INT NOT NULL, BranchId INT NOT NULL,
            ActionType VARCHAR(24) NOT NULL, FromStatus VARCHAR(16) NULL, ToStatus VARCHAR(16) NULL,
            Reason NVARCHAR(255) NULL, PerformedBy INT NULL, CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
            CONSTRAINT FK_OIAL_Item FOREIGN KEY (OrderItemId) REFERENCES sales.OrderItem(OrderItemId),
            CONSTRAINT FK_OIAL_Branch FOREIGN KEY (BranchId) REFERENCES org.Branch(BranchId),
            CONSTRAINT FK_OIAL_User FOREIGN KEY (PerformedBy) REFERENCES iam.[User](UserId)
        )');
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('ops.OrderItemActionLog') AND name='IX_OrderItemAction_Item')
        EXEC(N'CREATE INDEX IX_OrderItemAction_Item ON ops.OrderItemActionLog(OrderItemId,CreatedAt DESC)');

    IF OBJECT_ID('ops.BaristaActionLog','U') IS NULL
        EXEC(N'CREATE TABLE ops.BaristaActionLog (
            BaristaActionLogId BIGINT IDENTITY PRIMARY KEY, BranchId INT NOT NULL, ShiftId INT NULL,
            EntityType VARCHAR(32) NOT NULL, EntityId BIGINT NULL, ActionType VARCHAR(40) NOT NULL,
            BeforeJson NVARCHAR(MAX) NULL, AfterJson NVARCHAR(MAX) NULL, Reason NVARCHAR(255) NULL,
            PerformedBy INT NULL, CorrelationId VARCHAR(64) NULL, CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
            CONSTRAINT FK_BAL_Branch FOREIGN KEY (BranchId) REFERENCES org.Branch(BranchId),
            CONSTRAINT FK_BAL_Shift FOREIGN KEY (ShiftId) REFERENCES hr.Attendance(AttendanceId),
            CONSTRAINT FK_BAL_User FOREIGN KEY (PerformedBy) REFERENCES iam.[User](UserId),
            CONSTRAINT CK_BAL_Entity CHECK (EntityType IN (''PREP_BATCH'',''WASTE_LOG'',''MENU_86'',''MANUAL_REMAKE''))
        )');
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('ops.BaristaActionLog') AND name='IX_BAL_BranchCreated')
        EXEC(N'CREATE INDEX IX_BAL_BranchCreated ON ops.BaristaActionLog(BranchId,CreatedAt DESC,BaristaActionLogId DESC)');
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('ops.BaristaActionLog') AND name='IX_BAL_Entity')
        EXEC(N'CREATE INDEX IX_BAL_Entity ON ops.BaristaActionLog(EntityType,EntityId,CreatedAt DESC)');
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('ops.BaristaActionLog') AND name='IX_BAL_Correlation')
        EXEC(N'CREATE INDEX IX_BAL_Correlation ON ops.BaristaActionLog(CorrelationId) WHERE CorrelationId IS NOT NULL');
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('sales.OrderItem') AND name='IX_OrderItem_BaristaStatus')
        EXEC(N'CREATE INDEX IX_OrderItem_BaristaStatus ON sales.OrderItem(BaristaId,Status)');

    COMMIT TRANSACTION;
    SELECT 'SCHEMA_SYNC_OK' AS Result;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
