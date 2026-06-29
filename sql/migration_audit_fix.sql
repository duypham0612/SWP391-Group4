/* ============================================================================
   MIGRATION — fix sau audit (additive, idempotent). Chạy trên DB CafeChain hiện có.
   S1: inventory.PrepBatch + Status/VoidedAt   (B4 cancelBatch)
   S2: inventory.WasteLog  + Status/VoidedAt   (B5 void)
   S3: hr.ShiftHandover (bảng mới)             (B7 handover)
   Không phá dữ liệu cũ; mọi dòng cũ mặc định Status='ACTIVE'.
   ============================================================================ */
USE CafeChain;
GO

-- S1 · PrepBatch.Status + VoidedAt  (CHECK inline trong cùng câu ADD — tránh "Invalid column name" do compile cả batch)
IF COL_LENGTH('inventory.PrepBatch','Status') IS NULL
    ALTER TABLE inventory.PrepBatch ADD
        Status   VARCHAR(10) NOT NULL CONSTRAINT DF_PrepBatch_Status DEFAULT 'ACTIVE'
                 CONSTRAINT CK_PrepBatch_Status CHECK (Status IN ('ACTIVE','CANCELLED')),
        VoidedAt DATETIME2 NULL;
GO

-- S2 · WasteLog.Status + VoidedAt
IF COL_LENGTH('inventory.WasteLog','Status') IS NULL
    ALTER TABLE inventory.WasteLog ADD
        Status   VARCHAR(10) NOT NULL CONSTRAINT DF_WasteLog_Status DEFAULT 'ACTIVE'
                 CONSTRAINT CK_WasteLog_Status CHECK (Status IN ('ACTIVE','VOIDED')),
        VoidedAt DATETIME2 NULL;
GO

-- S3 · hr.ShiftHandover
IF OBJECT_ID('hr.ShiftHandover','U') IS NULL
BEGIN
    CREATE TABLE hr.ShiftHandover (
        ShiftHandoverId INT IDENTITY PRIMARY KEY,
        BranchId        INT NOT NULL,
        Note            NVARCHAR(1000) NOT NULL,
        CreatedBy       INT NOT NULL,
        CreatedAt       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_SH_Branch FOREIGN KEY (BranchId)  REFERENCES org.Branch(BranchId),
        CONSTRAINT FK_SH_User   FOREIGN KEY (CreatedBy) REFERENCES iam.[User](UserId)
    );
END
GO

-- S4 · sales.OrderItem.Priority (KDS bump)
IF COL_LENGTH('sales.OrderItem','Priority') IS NULL
    ALTER TABLE sales.OrderItem ADD Priority INT NOT NULL CONSTRAINT DF_OrderItem_Priority DEFAULT 0;
GO
