/*
 * File database SQL duy nhất của dự án CafeChain.
 * Dành cho demo localhost: tạo database CafeChain rỗng, sau đó chạy Flyway migrate.
 * Schema bên dưới là DDL cuối trực tiếp; không hỗ trợ nâng cấp database legacy.
 * Tài khoản demo mặc định: admin, manager1, cashier1, barista1 / mật khẩu 123456.
 */
SET ANSI_NULLS, ANSI_PADDING, ANSI_WARNINGS, ARITHABORT,
    CONCAT_NULL_YIELDS_NULL, QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
GO


GO
CREATE SCHEMA [catalog]
    AUTHORIZATION [dbo];


GO


GO
CREATE SCHEMA [hr]
    AUTHORIZATION [dbo];


GO


GO
CREATE SCHEMA [iam]
    AUTHORIZATION [dbo];


GO


GO
CREATE SCHEMA [inventory]
    AUTHORIZATION [dbo];


GO


GO
IF SCHEMA_ID(N'ops') IS NULL
    EXEC(N'CREATE SCHEMA [ops] AUTHORIZATION [dbo]');


GO


GO
CREATE SCHEMA [org]
    AUTHORIZATION [dbo];


GO


GO
CREATE SCHEMA [payment]
    AUTHORIZATION [dbo];


GO


GO
CREATE SCHEMA [sales]
    AUTHORIZATION [dbo];


GO


GO
CREATE TABLE [catalog].[Recipe] (
    [RecipeId]     INT             IDENTITY (1, 1) NOT NULL,
    [OwnerType]    VARCHAR (8)     NOT NULL,
    [OwnerId]      INT             NOT NULL,
    [IngredientId] INT             NOT NULL,
    [Quantity]     DECIMAL (12, 3) NOT NULL,
    CONSTRAINT [PK_Recipe] PRIMARY KEY CLUSTERED ([RecipeId] ASC),
    CONSTRAINT [UQ_Recipe_OwnerIngredient] UNIQUE NONCLUSTERED ([OwnerType] ASC, [OwnerId] ASC, [IngredientId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_Recipe_Ingredient]
    ON [catalog].[Recipe]([IngredientId] ASC)
    INCLUDE([OwnerType], [OwnerId], [Quantity]);


GO


GO
CREATE TABLE [catalog].[ModifierOption] (
    [ModifierOptionId] INT             IDENTITY (1, 1) NOT NULL,
    [ModifierGroupId]  INT             NOT NULL,
    [Name]             NVARCHAR (80)   NOT NULL,
    [PriceDelta]       DECIMAL (12, 2) NOT NULL,
    [IsActive]         BIT             NOT NULL,
    CONSTRAINT [PK_ModifierOption] PRIMARY KEY CLUSTERED ([ModifierOptionId] ASC),
    CONSTRAINT [UQ_ModifierOption_GroupName] UNIQUE NONCLUSTERED ([ModifierGroupId] ASC, [Name] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_ModifierOption_Group]
    ON [catalog].[ModifierOption]([ModifierGroupId] ASC);


GO


GO
CREATE TABLE [catalog].[Category] (
    [CategoryId] INT            IDENTITY (1, 1) NOT NULL,
    [Name]       NVARCHAR (100) NOT NULL,
    [SortOrder]  INT            NOT NULL,
    [IsActive]   BIT            NOT NULL,
    [NameKey]    AS             ((upper(ltrim(rtrim([Name])))) COLLATE Latin1_General_100_CI_AI) PERSISTED,
    CONSTRAINT [PK_Category] PRIMARY KEY CLUSTERED ([CategoryId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_Category_NameKey]
    ON [catalog].[Category]([NameKey] ASC);


GO


GO
CREATE TABLE [catalog].[BranchMenu] (
    [BranchId]                  INT             NOT NULL,
    [ProductId]                 INT             NOT NULL,
    [IsListed]                  BIT             NOT NULL,
    [LocalPrice]                DECIMAL (12, 2) NULL,
    [IsTemporarilyUnavailable]  BIT             NOT NULL,
    [BackInEta]                 DATETIME2 (7)   NULL,
    [BlockReason]               VARCHAR (20)    NULL,
    [BlockNote]                 NVARCHAR (255)  NULL,
    [BlockRequestedBy]          INT             NULL,
    [BlockRequestedAt]          DATETIME2 (7)   NULL,
    [BlockReopenRequestedAt]    DATETIME2 (7)   NULL,
    [BlockStatus]               VARCHAR (10)    NULL,
    [BlockReviewedBy]           INT             NULL,
    [BlockReviewedAt]           DATETIME2 (7)   NULL,
    [BlockReviewNote]           NVARCHAR (255)  NULL,
    CONSTRAINT [PK_BranchMenu] PRIMARY KEY CLUSTERED ([BranchId] ASC, [ProductId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_BranchMenu_TemporarilyUnavailable]
    ON [catalog].[BranchMenu]([BranchId] ASC, [IsTemporarilyUnavailable] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_BranchMenu_Product]
    ON [catalog].[BranchMenu]([ProductId] ASC);


GO


GO
CREATE TABLE [catalog].[ModifierGroup] (
    [ModifierGroupId] INT           IDENTITY (1, 1) NOT NULL,
    [ProductId]       INT           NOT NULL,
    [Name]            NVARCHAR (80) NOT NULL,
    [IsRequired]      BIT           NOT NULL,
    [MinSelect]       INT           NOT NULL,
    [MaxSelect]       INT           NOT NULL,
    [SortOrder]       INT           NOT NULL,
    [NameKey]         AS            ((upper(ltrim(rtrim([Name])))) COLLATE Latin1_General_100_CI_AI) PERSISTED,
    CONSTRAINT [PK_ModifierGroup] PRIMARY KEY CLUSTERED ([ModifierGroupId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_ModifierGroup_NameKey]
    ON [catalog].[ModifierGroup]([ProductId] ASC, [NameKey] ASC);


GO


GO
CREATE TABLE [catalog].[Ingredient] (
    [IngredientId]         INT             IDENTITY (1, 1) NOT NULL,
    [Name]                 NVARCHAR (120)  NOT NULL,
    [Unit]                 NVARCHAR (20)   NOT NULL,
    [IngredientType]       VARCHAR (10)    NOT NULL,
    [ShelfLifeMinutes]     INT             NULL,
    [PrepYieldQty]         DECIMAL (12, 3) NULL,
    [PurchaseUnitName]     NVARCHAR (20)   NULL,
    [PurchaseFactorToBase] DECIMAL (18, 6) NULL,
    [IsActive]             BIT             NOT NULL,
    [NameKey]              AS              ((upper(ltrim(rtrim([Name])))) COLLATE Latin1_General_100_CI_AI) PERSISTED,
    [UnitKey]              AS              ((upper(ltrim(rtrim([Unit])))) COLLATE Latin1_General_100_CI_AI) PERSISTED,
    CONSTRAINT [PK_Ingredient] PRIMARY KEY CLUSTERED ([IngredientId] ASC),
    CONSTRAINT [UQ_Ingredient_IdType] UNIQUE NONCLUSTERED ([IngredientId] ASC, [IngredientType] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_Ingredient_NameUnitKey]
    ON [catalog].[Ingredient]([NameKey] ASC, [UnitKey] ASC);


GO


GO
CREATE TABLE [catalog].[Product] (
    [ProductId]     INT             IDENTITY (1, 1) NOT NULL,
    [CategoryId]    INT             NOT NULL,
    [Name]          NVARCHAR (150)  NOT NULL,
    [BasePrice]     DECIMAL (12, 2) NOT NULL,
    [ImageUrl]      VARCHAR (255)   NULL,
    [IsActive]      BIT             NOT NULL,
    [ShowOnHome]    BIT             NOT NULL,
    [HomeSortOrder] INT             NOT NULL,
    [PrepSeconds]   INT             NOT NULL,
    [NameKey]       AS              ((upper(ltrim(rtrim([Name])))) COLLATE Latin1_General_100_CI_AI) PERSISTED,
    CONSTRAINT [PK_Product] PRIMARY KEY CLUSTERED ([ProductId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_Product_CategoryNameKey]
    ON [catalog].[Product]([CategoryId] ASC, [NameKey] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_Product_Category]
    ON [catalog].[Product]([CategoryId] ASC);


GO


GO
CREATE TABLE [hr].[ShiftAssignment] (
    [ShiftAssignmentId] INT             IDENTITY (1, 1) NOT NULL,
    [ShiftName]         NVARCHAR (60)   NOT NULL,
    [StartTime]         TIME (7)        NOT NULL,
    [EndTime]           TIME (7)        NOT NULL,
    [UserId]            INT             NOT NULL,
    [WorkDate]          DATE            NOT NULL,
    [BranchId]          INT             NOT NULL,
    [HourlyRateSnapshot] DECIMAL (12, 2) NULL,
    [CheckInAt]         DATETIME2 (7)   NULL,
    [CheckOutAt]        DATETIME2 (7)   NULL,
    [AttendanceStatus]  VARCHAR (10)    NULL,
    [ApprovedBy]        INT             NULL,
    [ApprovedAt]        DATETIME2 (7)   NULL,
    CONSTRAINT [PK_ShiftAssignment] PRIMARY KEY CLUSTERED ([ShiftAssignmentId] ASC),
    CONSTRAINT [UQ_ShiftAssignment_IdBranch] UNIQUE NONCLUSTERED ([ShiftAssignmentId] ASC, [BranchId] ASC),
    CONSTRAINT [UQ_ShiftAssignment_UserDateStart] UNIQUE NONCLUSTERED ([UserId] ASC, [WorkDate] ASC, [StartTime] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_ShiftAssignment_BranchDate]
    ON [hr].[ShiftAssignment]([BranchId] ASC, [WorkDate] ASC, [StartTime] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_ShiftAssignment_UserDate]
    ON [hr].[ShiftAssignment]([UserId] ASC, [WorkDate] ASC);


GO


GO
CREATE TABLE [iam].[UserAccount] (
    [UserId]       INT            IDENTITY (1, 1) NOT NULL,
    [Username]     VARCHAR (60)   NOT NULL,
    [PasswordHash] VARCHAR (255)  NOT NULL,
    [FullName]     NVARCHAR (120) NOT NULL,
    [Email]        VARCHAR (120)  NULL,
    [Phone]        VARCHAR (20)   NULL,
    [RoleCode]     VARCHAR (30)   NOT NULL,
    [BranchId]     INT            NULL,
    [HourlyRate]   DECIMAL (12, 2) NULL,
    [Status]       VARCHAR (10)   NOT NULL,
    [CreatedAt]    DATETIME2 (7)  NOT NULL,
    CONSTRAINT [PK_UserAccount] PRIMARY KEY CLUSTERED ([UserId] ASC),
    CONSTRAINT [UQ_UserAccount_IdBranch] UNIQUE NONCLUSTERED ([UserId] ASC, [BranchId] ASC),
    CONSTRAINT [UQ_UserAccount_Username] UNIQUE NONCLUSTERED ([Username] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_UserAccount_Branch]
    ON [iam].[UserAccount]([BranchId] ASC);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_UserAccount_Email]
    ON [iam].[UserAccount]([Email] ASC) WHERE ([Email] IS NOT NULL);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_UserAccount_Phone]
    ON [iam].[UserAccount]([Phone] ASC) WHERE ([Phone] IS NOT NULL);


GO


GO
CREATE TABLE [inventory].[Supplier] (
    [SupplierId] INT            IDENTITY (1, 1) NOT NULL,
    [Name]       NVARCHAR (150) NOT NULL,
    [Phone]      VARCHAR (20)   NULL,
    [Address]    NVARCHAR (255) NULL,
    [IsActive]   BIT            NOT NULL,
    CONSTRAINT [PK_Supplier] PRIMARY KEY CLUSTERED ([SupplierId] ASC)
);


GO


GO
CREATE TABLE [inventory].[StockReceiptLine] (
    [StockReceiptLineId]      INT             IDENTITY (1, 1) NOT NULL,
    [ReceiptBatchId]          VARCHAR (36)    NOT NULL,
    [BranchId]                INT             NOT NULL,
    [SupplierId]              INT             NULL,
    [ReceivedBy]              INT             NOT NULL,
    [DocumentDate]            DATE            NOT NULL,
    [Status]                  VARCHAR (12)    NOT NULL,
    [Note]                    NVARCHAR (255)  NULL,
    [CreatedAt]               DATETIME2 (7)   NOT NULL,
    [IngredientId]            INT             NOT NULL,
    [UnitCost]                DECIMAL (12, 2) NOT NULL,
    [EnteredQuantity]         DECIMAL (18, 6) NOT NULL,
    [UnitNameAtEntry]         NVARCHAR (20)   NOT NULL,
    [FactorToBaseAtEntry]     DECIMAL (18, 6) NOT NULL,
    [BaseQuantity]            AS              (CONVERT (DECIMAL (12, 3), [EnteredQuantity] * [FactorToBaseAtEntry])) PERSISTED,
    CONSTRAINT [PK_StockReceiptLine] PRIMARY KEY CLUSTERED ([StockReceiptLineId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_StockReceiptLine_BatchIngredient]
    ON [inventory].[StockReceiptLine]([ReceiptBatchId] ASC, [IngredientId] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_StockReceiptLine_BranchStatus]
    ON [inventory].[StockReceiptLine]([BranchId] ASC, [Status] ASC, [DocumentDate] DESC)
    INCLUDE([ReceiptBatchId], [IngredientId]);


GO


GO
CREATE NONCLUSTERED INDEX [IX_StockReceiptLine_Supplier]
    ON [inventory].[StockReceiptLine]([SupplierId] ASC) WHERE ([SupplierId] IS NOT NULL);


GO


GO
CREATE TABLE [inventory].[BranchInventory] (
    [BranchId]            INT             NOT NULL,
    [IngredientId]        INT             NOT NULL,
    [QuantityOnHand]      DECIMAL (12, 3) NOT NULL,
    [MinThreshold]        DECIMAL (12, 3) NOT NULL,
    [PrepTargetQty]       DECIMAL (12, 3) NULL,
    [UpdatedAt]           DATETIME2 (7)   NOT NULL,
    [PrepTargetTypeGuard] AS              (CASE WHEN [PrepTargetQty] IS NULL THEN CONVERT (VARCHAR (10), NULL) ELSE CONVERT (VARCHAR (10), 'PREPPED') END) PERSISTED,
    CONSTRAINT [PK_BranchInventory] PRIMARY KEY CLUSTERED ([BranchId] ASC, [IngredientId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_BranchInventory_Ingredient]
    ON [inventory].[BranchInventory]([IngredientId] ASC);


GO


GO
CREATE TABLE [inventory].[WasteEntry] (
    [WasteEntryId]       BIGINT          IDENTITY (1, 1) NOT NULL,
    [BranchId]           INT             NOT NULL,
    [EventGroupId]       VARCHAR (64)    NULL,
    [EventKind]          VARCHAR (20)    NOT NULL,
    [Source]             VARCHAR (12)    NOT NULL,
    [ProductId]          INT             NULL,
    [OrderItemId]        INT             NULL,
    [CupQuantity]        INT             NULL,
    [CauseCode]          VARCHAR (24)    NOT NULL,
    [CauseDetail]        NVARCHAR (255)  NULL,
    [ShiftAssignmentId]  INT             NULL,
    [CreatedBy]          INT             NOT NULL,
    [CreatedAt]          DATETIME2 (7)   NOT NULL,
    [IngredientId]       INT             NOT NULL,
    [Quantity]           DECIMAL (12, 3) NOT NULL,
    [WasteType]          VARCHAR (12)    NOT NULL,
    [Reason]             NVARCHAR (255)  NULL,
    [UnitCostAtLog]      DECIMAL (12, 2) NULL,
    [CostBasis]          VARCHAR (20)    NULL,
    [Status]             VARCHAR (10)    NOT NULL,
    [VoidedAt]           DATETIME2 (7)   NULL,
    [LoggedBy]           INT             NOT NULL,
    [LoggedAt]           DATETIME2 (7)   NOT NULL,
    [ReviewType]         VARCHAR (20)    NULL,
    [ReviewStatus]       VARCHAR (16)    NULL,
    [QtyBefore]          DECIMAL (12, 3) NULL,
    [QtyAfter]           DECIMAL (12, 3) NULL,
    [ReviewNote]         NVARCHAR (255)  NULL,
    [ResolvedBy]         INT             NULL,
    [ResolvedAt]         DATETIME2 (7)   NULL,
    [ResolutionNote]     NVARCHAR (255)  NULL,
    CONSTRAINT [PK_WasteEntry] PRIMARY KEY CLUSTERED ([WasteEntryId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_WasteEntry_EventGroupIngredient]
    ON [inventory].[WasteEntry]([BranchId] ASC, [EventGroupId] ASC, [IngredientId] ASC)
    WHERE ([EventGroupId] IS NOT NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEntry_Ingredient]
    ON [inventory].[WasteEntry]([IngredientId] ASC, [LoggedAt] DESC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEntry_OrderItemBranch]
    ON [inventory].[WasteEntry]([OrderItemId] ASC, [BranchId] ASC) WHERE ([OrderItemId] IS NOT NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEntry_BranchLogged]
    ON [inventory].[WasteEntry]([BranchId] ASC, [LoggedAt] DESC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEntry_ShiftAssignmentBranch]
    ON [inventory].[WasteEntry]([ShiftAssignmentId] ASC, [BranchId] ASC) WHERE ([ShiftAssignmentId] IS NOT NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEntry_BranchActorTime]
    ON [inventory].[WasteEntry]([BranchId] ASC, [CreatedBy] ASC, [CreatedAt] ASC)
    INCLUDE([EventKind]);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEntry_OpenReview]
    ON [inventory].[WasteEntry]([ReviewStatus] ASC, [CreatedAt] DESC)
    INCLUDE([EventGroupId], [IngredientId], [ReviewType]) WHERE ([ReviewStatus]='OPEN');


GO


GO
GO


GO
CREATE TABLE [inventory].[InventoryTransaction] (
    [InventoryTransactionId] BIGINT          IDENTITY (1, 1) NOT NULL,
    [BranchId]               INT             NOT NULL,
    [IngredientId]           INT             NOT NULL,
    [ChangeQty]              DECIMAL (12, 3) NOT NULL,
    [TxnType]                VARCHAR (12)    NOT NULL,
    [ReferenceType]          VARCHAR (40)    NULL,
    [ReferenceId]            VARCHAR (64)    NULL,
    [CreatedBy]              INT             NULL,
    [CreatedAt]              DATETIME2 (7)   NOT NULL,
    CONSTRAINT [PK_InventoryTransaction] PRIMARY KEY CLUSTERED ([InventoryTransactionId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_InventoryTransaction_ReceiptReference]
    ON [inventory].[InventoryTransaction]([BranchId] ASC, [IngredientId] ASC, [ReferenceId] ASC) WHERE ([TxnType]='RECEIPT' AND [ReferenceType]='STOCK_RECEIPT_LINE' AND [ReferenceId] IS NOT NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_InventoryTransaction_BranchIngredient]
    ON [inventory].[InventoryTransaction]([BranchId] ASC, [IngredientId] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_InventoryTransaction_Reference]
    ON [inventory].[InventoryTransaction]([BranchId] ASC, [ReferenceType] ASC, [ReferenceId] ASC, [TxnType] ASC)
    INCLUDE([IngredientId], [ChangeQty]);


GO


GO
CREATE NONCLUSTERED INDEX [IX_InventoryTransaction_IngredientCreatedAt]
    ON [inventory].[InventoryTransaction]([IngredientId] ASC, [CreatedAt] DESC);


GO


GO
CREATE TABLE [inventory].[PrepBatch] (
    [PrepBatchId]              INT             IDENTITY (1, 1) NOT NULL,
    [BranchId]                 INT             NOT NULL,
    [PreppedIngredientId]      INT             NOT NULL,
    [QuantityProduced]         DECIMAL (12, 3) NOT NULL,
    [MadeBy]                   INT             NOT NULL,
    [MadeAt]                   DATETIME2 (7)   NOT NULL,
    [ExpiresAt]                DATETIME2 (7)   NULL,
    [Status]                   VARCHAR (10)    NOT NULL,
    [VoidedAt]                 DATETIME2 (7)   NULL,
    [WrittenOffAt]             DATETIME2 (7)   NULL,
    [WriteOffWasteEntryId]     BIGINT          NULL,
    [ClientRequestId]          VARCHAR (36)    NULL,
    [RequiresApproval]         BIT             NOT NULL,
    [ReviewedAt]               DATETIME2 (7)   NULL,
    [ReviewedBy]               INT             NULL,
    [PreppedTypeGuard]         AS              (CONVERT (VARCHAR (10), 'PREPPED')) PERSISTED,
    CONSTRAINT [PK_PrepBatch] PRIMARY KEY CLUSTERED ([PrepBatchId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_PrepBatch_ClientRequest]
    ON [inventory].[PrepBatch]([BranchId] ASC, [ClientRequestId] ASC) WHERE ([ClientRequestId] IS NOT NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_PrepBatch_Unreviewed]
    ON [inventory].[PrepBatch]([BranchId] ASC, [Status] ASC, [RequiresApproval] ASC, [ReviewedAt] ASC) WHERE ([Status]='ACTIVE' AND [RequiresApproval]=(0) AND [ReviewedAt] IS NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_PrepBatch_Pending]
    ON [inventory].[PrepBatch]([BranchId] ASC, [Status] ASC, [MadeAt] ASC) WHERE ([Status]='PENDING');


GO


GO
CREATE TABLE [inventory].[StockAdjustment] (
    [StockAdjustmentId]          INT             IDENTITY (1, 1) NOT NULL,
    [BranchId]                   INT             NOT NULL,
    [CountBatchId]               VARCHAR (36)    NULL,
    [CountedAt]                  DATETIME2 (7)   NULL,
    [CountedBy]                  INT             NULL,
    [CountNote]                  NVARCHAR (255)  NULL,
    [IngredientId]               INT             NOT NULL,
    [SystemBaseQty]              DECIMAL (12, 3) NOT NULL,
    [ActualBaseQty]              DECIMAL (12, 3) NOT NULL,
    [Reason]                     NVARCHAR (255)  NULL,
    [AdjustedBy]                 INT             NOT NULL,
    [AdjustedAt]                 DATETIME2 (7)   NOT NULL,
    [CountedQuantity]            DECIMAL (18, 6) NOT NULL,
    [UnitNameAtCount]            NVARCHAR (20)   NOT NULL,
    [FactorToBaseAtCount]        DECIMAL (18, 6) NOT NULL,
    [DiffQty]                    AS              ([ActualBaseQty] - [SystemBaseQty]) PERSISTED,
    CONSTRAINT [PK_StockAdjustment] PRIMARY KEY CLUSTERED ([StockAdjustmentId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_StockAdjustment_CountIngredient]
    ON [inventory].[StockAdjustment]([CountBatchId] ASC, [IngredientId] ASC) WHERE ([CountBatchId] IS NOT NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_StockAdjustment_BranchIngredient]
    ON [inventory].[StockAdjustment]([BranchId] ASC, [IngredientId] ASC, [AdjustedAt] DESC);


GO


GO
CREATE TABLE [ops].[ActivityLog] (
    [ActivityLogId] BIGINT          IDENTITY (1, 1) NOT NULL,
    [EntityType]    VARCHAR (24)    NOT NULL,
    [EntityId]      BIGINT          NOT NULL,
    [BranchId]      INT             NULL,
    [ActionType]    VARCHAR (24)    NOT NULL,
    [FromValue]     NVARCHAR (1000) NULL,
    [ToValue]       NVARCHAR (1000) NULL,
    [Reason]        NVARCHAR (255)  NULL,
    [PerformedBy]   INT             NULL,
    [PerformedAt]   DATETIME2 (7)   NOT NULL,
    CONSTRAINT [PK_ActivityLog] PRIMARY KEY CLUSTERED ([ActivityLogId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_ActivityLog_Entity]
    ON [ops].[ActivityLog]([EntityType] ASC, [EntityId] ASC, [PerformedAt] DESC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_ActivityLog_Branch]
    ON [ops].[ActivityLog]([BranchId] ASC, [PerformedAt] DESC);


GO


GO
CREATE TABLE [ops].[OutboxEvent] (
    [OutboxEventId] BIGINT         IDENTITY (1, 1) NOT NULL,
    [EventType]     VARCHAR (50)   NOT NULL,
    [AggregateId]   VARCHAR (50)   NULL,
    [BranchId]      INT            NULL,
    [Payload]       NVARCHAR (MAX) NULL,
    [CreatedAt]     DATETIME2 (7)  NOT NULL,
    [ProcessedAt]   DATETIME2 (7)  NULL,
    CONSTRAINT [PK_OutboxEvent] PRIMARY KEY CLUSTERED ([OutboxEventId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_OutboxEvent_PendingQueue]
    ON [ops].[OutboxEvent]([BranchId] ASC, [EventType] ASC, [CreatedAt] ASC)
    INCLUDE([AggregateId]) WHERE ([ProcessedAt] IS NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_OutboxEvent_PendingAggregate]
    ON [ops].[OutboxEvent]([EventType] ASC, [AggregateId] ASC)
    INCLUDE([BranchId], [CreatedAt]) WHERE ([ProcessedAt] IS NULL);


GO


GO
CREATE TABLE [org].[Branch] (
    [BranchId]          INT            IDENTITY (1, 1) NOT NULL,
    [Code]              VARCHAR (20)   NOT NULL,
    [Name]              NVARCHAR (150) NOT NULL,
    [Address]           NVARCHAR (255) NULL,
    [Phone]             VARCHAR (20)   NULL,
    [OpenTime]          TIME (7)       NULL,
    [CloseTime]         TIME (7)       NULL,
    [ManagerUserId]     INT            NULL,
    [IsActive]          BIT            NOT NULL,
    [PeakThresholdCups] INT            NOT NULL,
    [CreatedAt]         DATETIME2 (7)  NOT NULL,
    [HeroEyebrow]       NVARCHAR (150) NULL,
    [HeroTitle]         NVARCHAR (200) NULL,
    [HeroSubtitle]      NVARCHAR (500) NULL,
    [HeroImageUrl]      VARCHAR (500)  NULL,
    CONSTRAINT [PK_Branch] PRIMARY KEY CLUSTERED ([BranchId] ASC),
    CONSTRAINT [UQ_Branch_Code] UNIQUE NONCLUSTERED ([Code] ASC)
);


GO


GO
CREATE TABLE [payment].[CashierShift] (
    [CashierShiftId] INT             IDENTITY (1, 1) NOT NULL,
    [BranchId]       INT             NOT NULL,
    [CashierId]      INT             NOT NULL,
    [OpeningCash]    DECIMAL (14, 2) NOT NULL,
    [ClosingCash]    DECIMAL (14, 2) NULL,
    [OpenedAt]       DATETIME2 (7)   NOT NULL,
    [ClosedAt]       DATETIME2 (7)   NULL,
    CONSTRAINT [PK_CashierShift] PRIMARY KEY CLUSTERED ([CashierShiftId] ASC),
    CONSTRAINT [UQ_CashierShift_IdBranch] UNIQUE NONCLUSTERED ([CashierShiftId] ASC, [BranchId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_CashierShift_OneOpenPerBranch]
    ON [payment].[CashierShift]([BranchId] ASC) WHERE ([ClosedAt] IS NULL);


GO


GO
CREATE TABLE [payment].[Bill] (
    [BillId]             INT             IDENTITY (1, 1) NOT NULL,
    [BranchId]           INT             NOT NULL,
    [CashierShiftId]     INT             NULL,
    [Subtotal]           DECIMAL (14, 2) NOT NULL,
    [VatAmount]          DECIMAL (14, 2) NOT NULL,
    [DiscountAmount]     DECIMAL (14, 2) NOT NULL,
    [TotalAmount]        DECIMAL (14, 2) NOT NULL,
    [RoundingAdjustment] DECIMAL (14, 2) NOT NULL,
    [PaidAmount]         DECIMAL (14, 2) NULL,
    [CashTendered]       DECIMAL (14, 2) NULL,
    [CashChange]         DECIMAL (14, 2) NULL,
    [PaymentMethod]      VARCHAR (10)    NULL,
    [Status]             VARCHAR (8)     NOT NULL,
    [PaidAt]             DATETIME2 (7)   NULL,
    [CreatedAt]          DATETIME2 (7)   NOT NULL,
    CONSTRAINT [PK_Bill] PRIMARY KEY CLUSTERED ([BillId] ASC),
    CONSTRAINT [UQ_Bill_IdBranch] UNIQUE NONCLUSTERED ([BillId] ASC, [BranchId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_Bill_BranchStatus]
    ON [payment].[Bill]([BranchId] ASC, [Status] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_Bill_BranchPaid]
    ON [payment].[Bill]([BranchId] ASC, [PaidAt] ASC)
    INCLUDE([TotalAmount], [VatAmount], [DiscountAmount], [PaymentMethod], [Status]);


GO


GO
CREATE NONCLUSTERED INDEX [IX_Bill_ShiftSettlement]
    ON [payment].[Bill]([CashierShiftId] ASC, [Status] ASC, [PaymentMethod] ASC)
    INCLUDE([PaidAmount], [TotalAmount]);


GO


GO
CREATE TABLE [sales].[SalesOrder] (
    [OrderId]        INT           IDENTITY (1, 1) NOT NULL,
    [BranchId]       INT           NOT NULL,
    [DiningTableId]  INT           NULL,
    [Source]         VARCHAR (8)   NOT NULL,
    [OrderType]      VARCHAR (16)  NOT NULL,
    [Status]         VARCHAR (12)  NOT NULL,
    [CreatedBy]      INT           NULL,
    [PickupCode]     VARCHAR (8)   NULL,
    [CreatedAt]      DATETIME2 (7) NOT NULL,
    [BusinessDate]   DATE          NOT NULL,
    CONSTRAINT [PK_SalesOrder] PRIMARY KEY CLUSTERED ([OrderId] ASC),
    CONSTRAINT [UQ_SalesOrder_IdBranch] UNIQUE NONCLUSTERED ([OrderId] ASC, [BranchId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_SalesOrder_BranchBusinessDatePickupCode]
    ON [sales].[SalesOrder]([BranchId] ASC, [BusinessDate] ASC, [PickupCode] ASC) WHERE ([PickupCode] IS NOT NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_SalesOrder_BranchStatus]
    ON [sales].[SalesOrder]([BranchId] ASC, [Status] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_SalesOrder_BranchCreated]
    ON [sales].[SalesOrder]([BranchId] ASC, [CreatedAt] ASC)
    INCLUDE([Status], [OrderType]);


GO


GO
CREATE NONCLUSTERED INDEX [IX_SalesOrder_DiningTable]
    ON [sales].[SalesOrder]([DiningTableId] ASC, [BranchId] ASC) WHERE ([DiningTableId] IS NOT NULL);


GO


GO
CREATE TABLE [sales].[DiningTable] (
    [DiningTableId] INT           IDENTITY (1, 1) NOT NULL,
    [BranchId]      INT           NOT NULL,
    [TableNumber]   NVARCHAR (20) NOT NULL,
    [QrCode]        VARCHAR (80)  NULL,
    [Status]        VARCHAR (10)  NOT NULL,
    CONSTRAINT [PK_DiningTable] PRIMARY KEY CLUSTERED ([DiningTableId] ASC),
    CONSTRAINT [UQ_DiningTable_BranchTableNumber] UNIQUE NONCLUSTERED ([BranchId] ASC, [TableNumber] ASC),
    CONSTRAINT [UQ_DiningTable_IdBranch] UNIQUE NONCLUSTERED ([DiningTableId] ASC, [BranchId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_DiningTable_QrCode]
    ON [sales].[DiningTable]([QrCode] ASC) WHERE ([QrCode] IS NOT NULL);


GO


GO
CREATE TABLE [sales].[OrderItemModifier] (
    [OrderItemModifierId]       INT             IDENTITY (1, 1) NOT NULL,
    [OrderItemId]               INT             NOT NULL,
    [ModifierOptionId]          INT             NOT NULL,
    [PriceDelta]                DECIMAL (12, 2) NOT NULL,
    [ModifierOptionNameAtOrder] NVARCHAR (80)   NOT NULL,
    CONSTRAINT [PK_OrderItemModifier] PRIMARY KEY CLUSTERED ([OrderItemModifierId] ASC),
    CONSTRAINT [UQ_OrderItemModifier_ItemOption] UNIQUE NONCLUSTERED ([OrderItemId] ASC, [ModifierOptionId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_OrderItemModifier_Item]
    ON [sales].[OrderItemModifier]([OrderItemId] ASC)
    INCLUDE([ModifierOptionId], [PriceDelta]);


GO


GO
CREATE TABLE [sales].[OrderItem] (
    [OrderItemId]             INT             IDENTITY (1, 1) NOT NULL,
    [OrderId]                 INT             NOT NULL,
    [ProductId]               INT             NOT NULL,
    [Quantity]                INT             NOT NULL,
    [UnitPrice]               DECIMAL (12, 2) NOT NULL,
    [Note]                    NVARCHAR (255)  NULL,
    [Status]                  VARCHAR (16)    NOT NULL,
    [Priority]                INT             NOT NULL,
    [StartedAt]               DATETIME2 (7)   NULL,
    [DoneAt]                  DATETIME2 (7)   NULL,
    [BaristaId]               INT             NULL,
    [PreparedBy]              INT             NULL,
    [HasIssue]                BIT             NOT NULL,
    [IssueReason]             NVARCHAR (255)  NULL,
    [IssueReportedBy]         INT             NULL,
    [IssueReportedAt]         DATETIME2 (7)   NULL,
    [RemakeCount]             INT             NOT NULL,
    [RemakeInventoryReserved] BIT             NOT NULL,
    [PickedUpBy]              INT             NULL,
    [PickedUpAt]              DATETIME2 (7)   NULL,
    [ServedAt]                DATETIME2 (7)   NULL,
    [BranchId]                INT             NOT NULL,
    [ProductNameAtOrder]      NVARCHAR (150)  NOT NULL,
    [BillId]                  INT             NULL,
    [BilledAmount]            DECIMAL (12, 2) NULL,
    CONSTRAINT [PK_OrderItem] PRIMARY KEY CLUSTERED ([OrderItemId] ASC),
    CONSTRAINT [UQ_OrderItem_IdBranch] UNIQUE NONCLUSTERED ([OrderItemId] ASC, [BranchId] ASC),
    CONSTRAINT [UQ_OrderItem_IdProductBranch] UNIQUE NONCLUSTERED ([OrderItemId] ASC, [ProductId] ASC, [BranchId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_OrderItem_Product]
    ON [sales].[OrderItem]([ProductId] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_OrderItem_BillBranch]
    ON [sales].[OrderItem]([BillId] ASC, [BranchId] ASC)
    INCLUDE([BilledAmount]) WHERE ([BillId] IS NOT NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_OrderItem_PreparedDone]
    ON [sales].[OrderItem]([PreparedBy] ASC, [DoneAt] ASC)
    INCLUDE([OrderId], [Quantity], [StartedAt], [Status]);


GO


GO
CREATE NONCLUSTERED INDEX [IX_OrderItem_BranchOrder]
    ON [sales].[OrderItem]([BranchId] ASC, [OrderId] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_OrderItem_OrderBranch]
    ON [sales].[OrderItem]([OrderId] ASC, [BranchId] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_OrderItem_BaristaStatus]
    ON [sales].[OrderItem]([BaristaId] ASC, [Status] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_OrderItem_Status]
    ON [sales].[OrderItem]([Status] ASC);


GO


GO
ALTER TABLE [catalog].[ModifierOption]
    ADD CONSTRAINT [DF_ModifierOption_IsActive] DEFAULT ((1)) FOR [IsActive];


GO


GO
ALTER TABLE [catalog].[ModifierOption]
    ADD CONSTRAINT [DF_ModifierOption_PriceDelta] DEFAULT ((0)) FOR [PriceDelta];


GO


GO
ALTER TABLE [catalog].[Category]
    ADD CONSTRAINT [DF_Category_IsActive] DEFAULT ((1)) FOR [IsActive];


GO


GO
ALTER TABLE [catalog].[Category]
    ADD CONSTRAINT [DF_Category_SortOrder] DEFAULT ((0)) FOR [SortOrder];


GO


GO
ALTER TABLE [catalog].[BranchMenu]
    ADD CONSTRAINT [DF_BranchMenu_IsListed] DEFAULT ((1)) FOR [IsListed];


GO


GO
ALTER TABLE [catalog].[BranchMenu]
    ADD CONSTRAINT [DF_BranchMenu_IsTemporarilyUnavailable] DEFAULT ((0)) FOR [IsTemporarilyUnavailable];


GO


GO
ALTER TABLE [catalog].[ModifierGroup]
    ADD CONSTRAINT [DF_ModifierGroup_IsRequired] DEFAULT ((0)) FOR [IsRequired];


GO


GO
ALTER TABLE [catalog].[ModifierGroup]
    ADD CONSTRAINT [DF_ModifierGroup_MaxSelect] DEFAULT ((1)) FOR [MaxSelect];


GO


GO
ALTER TABLE [catalog].[ModifierGroup]
    ADD CONSTRAINT [DF_ModifierGroup_MinSelect] DEFAULT ((0)) FOR [MinSelect];


GO


GO
ALTER TABLE [catalog].[ModifierGroup]
    ADD CONSTRAINT [DF_ModifierGroup_SortOrder] DEFAULT ((0)) FOR [SortOrder];


GO


GO
ALTER TABLE [catalog].[Ingredient]
    ADD CONSTRAINT [DF_Ingredient_IsActive] DEFAULT ((1)) FOR [IsActive];


GO


GO
ALTER TABLE [catalog].[Product]
    ADD CONSTRAINT [DF_Product_HomeSortOrder] DEFAULT ((0)) FOR [HomeSortOrder];


GO


GO
ALTER TABLE [catalog].[Product]
    ADD CONSTRAINT [DF_Product_IsActive] DEFAULT ((1)) FOR [IsActive];


GO


GO
ALTER TABLE [catalog].[Product]
    ADD CONSTRAINT [DF_Product_PrepSeconds] DEFAULT ((720)) FOR [PrepSeconds];


GO


GO
ALTER TABLE [catalog].[Product]
    ADD CONSTRAINT [DF_Product_ShowOnHome] DEFAULT ((1)) FOR [ShowOnHome];


GO


GO
ALTER TABLE [iam].[UserAccount]
    ADD CONSTRAINT [DF_UserAccount_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


GO


GO
ALTER TABLE [iam].[UserAccount]
    ADD CONSTRAINT [DF_UserAccount_Status] DEFAULT ('ACTIVE') FOR [Status];


GO


GO
ALTER TABLE [inventory].[Supplier]
    ADD CONSTRAINT [DF_Supplier_IsActive] DEFAULT ((1)) FOR [IsActive];


GO


GO
ALTER TABLE [inventory].[StockReceiptLine]
    ADD CONSTRAINT [DF_StockReceiptLine_DocumentDate] DEFAULT (CONVERT([date],sysutcdatetime())) FOR [DocumentDate];


GO


GO
ALTER TABLE [inventory].[StockReceiptLine]
    ADD CONSTRAINT [DF_StockReceiptLine_Status] DEFAULT ('DRAFT') FOR [Status];


GO


GO
ALTER TABLE [inventory].[StockReceiptLine]
    ADD CONSTRAINT [DF_StockReceiptLine_UnitCost] DEFAULT ((0)) FOR [UnitCost];


GO


GO
ALTER TABLE [inventory].[StockReceiptLine]
    ADD CONSTRAINT [DF_StockReceiptLine_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


GO


GO
ALTER TABLE [inventory].[BranchInventory]
    ADD CONSTRAINT [DF_BranchInventory_MinThreshold] DEFAULT ((0)) FOR [MinThreshold];


GO


GO
ALTER TABLE [inventory].[BranchInventory]
    ADD CONSTRAINT [DF_BranchInventory_QuantityOnHand] DEFAULT ((0)) FOR [QuantityOnHand];


GO


GO
ALTER TABLE [inventory].[BranchInventory]
    ADD CONSTRAINT [DF_BranchInventory_UpdatedAt] DEFAULT (sysutcdatetime()) FOR [UpdatedAt];


GO


GO
ALTER TABLE [inventory].[WasteEntry]
    ADD CONSTRAINT [DF_WasteEntry_LoggedAt] DEFAULT (sysutcdatetime()) FOR [LoggedAt];


GO


GO
ALTER TABLE [inventory].[WasteEntry]
    ADD CONSTRAINT [DF_WasteEntry_Status] DEFAULT ('ACTIVE') FOR [Status];


GO


GO
ALTER TABLE [inventory].[InventoryTransaction]
    ADD CONSTRAINT [DF_InventoryTransaction_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


GO


GO
ALTER TABLE [inventory].[PrepBatch]
    ADD CONSTRAINT [DF_PrepBatch_Status] DEFAULT ('ACTIVE') FOR [Status];


GO


GO
ALTER TABLE [inventory].[PrepBatch]
    ADD CONSTRAINT [DF_PrepBatch_MadeAt] DEFAULT (sysutcdatetime()) FOR [MadeAt];


GO


GO
ALTER TABLE [inventory].[PrepBatch]
    ADD CONSTRAINT [DF_PrepBatch_RequiresApproval] DEFAULT ((0)) FOR [RequiresApproval];


GO


GO
ALTER TABLE [inventory].[StockAdjustment]
    ADD CONSTRAINT [DF_StockAdjustment_AdjustedAt] DEFAULT (sysutcdatetime()) FOR [AdjustedAt];


GO


GO
ALTER TABLE [inventory].[WasteEntry]
    ADD CONSTRAINT [DF_WasteEntry_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


GO


GO
ALTER TABLE [ops].[ActivityLog]
    ADD CONSTRAINT [DF_ActivityLog_PerformedAt] DEFAULT (sysutcdatetime()) FOR [PerformedAt];


GO


GO
ALTER TABLE [ops].[OutboxEvent]
    ADD CONSTRAINT [DF_OutboxEvent_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


GO


GO
ALTER TABLE [org].[Branch]
    ADD CONSTRAINT [DF_Branch_PeakThresholdCups] DEFAULT ((0)) FOR [PeakThresholdCups];


GO


GO
ALTER TABLE [org].[Branch]
    ADD CONSTRAINT [DF_Branch_IsActive] DEFAULT ((1)) FOR [IsActive];


GO


GO
ALTER TABLE [org].[Branch]
    ADD CONSTRAINT [DF_Branch_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


GO


GO
ALTER TABLE [payment].[CashierShift]
    ADD CONSTRAINT [DF_CashierShift_OpenedAt] DEFAULT (sysutcdatetime()) FOR [OpenedAt];


GO


GO
ALTER TABLE [payment].[CashierShift]
    ADD CONSTRAINT [DF_CashierShift_OpeningCash] DEFAULT ((0)) FOR [OpeningCash];


GO


GO
ALTER TABLE [payment].[Bill]
    ADD CONSTRAINT [DF_Bill_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


GO


GO
ALTER TABLE [payment].[Bill]
    ADD CONSTRAINT [DF_Bill_TotalAmount] DEFAULT ((0)) FOR [TotalAmount];


GO


GO
ALTER TABLE [payment].[Bill]
    ADD CONSTRAINT [DF_Bill_DiscountAmount] DEFAULT ((0)) FOR [DiscountAmount];


GO


GO
ALTER TABLE [payment].[Bill]
    ADD CONSTRAINT [DF_Bill_VatAmount] DEFAULT ((0)) FOR [VatAmount];


GO


GO
ALTER TABLE [payment].[Bill]
    ADD CONSTRAINT [DF_Bill_RoundingAdjustment] DEFAULT ((0)) FOR [RoundingAdjustment];


GO


GO
ALTER TABLE [payment].[Bill]
    ADD CONSTRAINT [DF_Bill_Status] DEFAULT ('UNPAID') FOR [Status];


GO


GO
ALTER TABLE [payment].[Bill]
    ADD CONSTRAINT [DF_Bill_Subtotal] DEFAULT ((0)) FOR [Subtotal];


GO


GO
ALTER TABLE [sales].[SalesOrder]
    ADD CONSTRAINT [DF_SalesOrder_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


GO


GO
ALTER TABLE [sales].[SalesOrder]
    ADD CONSTRAINT [DF_SalesOrder_Status] DEFAULT ('ACTIVE') FOR [Status];


GO


GO
ALTER TABLE [sales].[SalesOrder]
    ADD CONSTRAINT [DF_SalesOrder_OrderType] DEFAULT ('DINE_IN') FOR [OrderType];


GO


GO
ALTER TABLE [sales].[DiningTable]
    ADD CONSTRAINT [DF_DiningTable_Status] DEFAULT ('EMPTY') FOR [Status];


GO


GO
ALTER TABLE [sales].[OrderItemModifier]
    ADD CONSTRAINT [DF_OrderItemModifier_PriceDelta] DEFAULT ((0)) FOR [PriceDelta];


GO


GO
ALTER TABLE [sales].[OrderItem]
    ADD CONSTRAINT [DF_OrderItem_Quantity] DEFAULT ((1)) FOR [Quantity];


GO


GO
ALTER TABLE [sales].[OrderItem]
    ADD CONSTRAINT [DF_OrderItem_HasIssue] DEFAULT ((0)) FOR [HasIssue];


GO


GO
ALTER TABLE [sales].[OrderItem]
    ADD CONSTRAINT [DF_OrderItem_RemakeInventoryReserved] DEFAULT ((0)) FOR [RemakeInventoryReserved];


GO


GO
ALTER TABLE [sales].[OrderItem]
    ADD CONSTRAINT [DF_OrderItem_RemakeCount] DEFAULT ((0)) FOR [RemakeCount];


GO


GO
ALTER TABLE [sales].[OrderItem]
    ADD CONSTRAINT [DF_OrderItem_Priority] DEFAULT ((0)) FOR [Priority];


GO


GO
ALTER TABLE [sales].[OrderItem]
    ADD CONSTRAINT [DF_OrderItem_Status] DEFAULT ('WAITING') FOR [Status];


GO


GO



GO


GO



GO


GO

ALTER TABLE [catalog].[ModifierOption] WITH NOCHECK
    ADD CONSTRAINT [FK_ModifierOption_ModifierGroup] FOREIGN KEY ([ModifierGroupId]) REFERENCES [catalog].[ModifierGroup] ([ModifierGroupId]);


GO


GO

ALTER TABLE [catalog].[ModifierGroup] WITH NOCHECK
    ADD CONSTRAINT [FK_ModifierGroup_Product] FOREIGN KEY ([ProductId]) REFERENCES [catalog].[Product] ([ProductId]);


GO


GO

ALTER TABLE [catalog].[BranchMenu] WITH NOCHECK
    ADD CONSTRAINT [FK_BranchMenu_Product] FOREIGN KEY ([ProductId]) REFERENCES [catalog].[Product] ([ProductId]);


GO


GO

ALTER TABLE [catalog].[BranchMenu] WITH NOCHECK
    ADD CONSTRAINT [FK_BranchMenu_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO



GO


GO



GO


GO



GO


GO



GO


GO



GO


GO



GO


GO



GO


GO



GO


GO



GO


GO

ALTER TABLE [catalog].[Product] WITH NOCHECK
    ADD CONSTRAINT [FK_Product_Category] FOREIGN KEY ([CategoryId]) REFERENCES [catalog].[Category] ([CategoryId]);


GO


GO

ALTER TABLE [catalog].[BranchMenu] WITH NOCHECK
    ADD CONSTRAINT [FK_BranchMenu_UserAccount_BlockReviewedBy] FOREIGN KEY ([BlockReviewedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [catalog].[BranchMenu] WITH NOCHECK
    ADD CONSTRAINT [FK_BranchMenu_UserAccount_BlockRequestedBy] FOREIGN KEY ([BlockRequestedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO



GO


GO



GO


GO



GO


GO



GO


GO

ALTER TABLE [hr].[ShiftAssignment] WITH NOCHECK
    ADD CONSTRAINT [FK_ShiftAssignment_UserAccount_User] FOREIGN KEY ([UserId]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [hr].[ShiftAssignment] WITH NOCHECK
    ADD CONSTRAINT [FK_ShiftAssignment_UserAccount_ApprovedBy] FOREIGN KEY ([ApprovedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [iam].[UserAccount] WITH NOCHECK
    ADD CONSTRAINT [FK_UserAccount_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [inventory].[StockReceiptLine] WITH NOCHECK
    ADD CONSTRAINT [FK_StockReceiptLine_UserAccount_ReceivedBy] FOREIGN KEY ([ReceivedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [inventory].[StockReceiptLine] WITH NOCHECK
    ADD CONSTRAINT [FK_StockReceiptLine_Supplier] FOREIGN KEY ([SupplierId]) REFERENCES [inventory].[Supplier] ([SupplierId]);


GO


GO

ALTER TABLE [inventory].[StockReceiptLine] WITH NOCHECK
    ADD CONSTRAINT [FK_StockReceiptLine_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [inventory].[StockReceiptLine] WITH NOCHECK
    ADD CONSTRAINT [FK_StockReceiptLine_Ingredient] FOREIGN KEY ([IngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO

ALTER TABLE [inventory].[BranchInventory] WITH NOCHECK
    ADD CONSTRAINT [FK_BranchInventory_Ingredient] FOREIGN KEY ([IngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO

ALTER TABLE [inventory].[BranchInventory] WITH NOCHECK
    ADD CONSTRAINT [FK_BranchInventory_Ingredient_PrepTargetTyped] FOREIGN KEY ([IngredientId], [PrepTargetTypeGuard]) REFERENCES [catalog].[Ingredient] ([IngredientId], [IngredientType]);


GO


GO

ALTER TABLE [inventory].[BranchInventory] WITH NOCHECK
    ADD CONSTRAINT [FK_BranchInventory_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEntry_Ingredient] FOREIGN KEY ([IngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEntry_UserAccount_LoggedBy] FOREIGN KEY ([LoggedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEntry_UserAccount_CreatedBy] FOREIGN KEY ([CreatedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEntry_UserAccount_ResolvedBy] FOREIGN KEY ([ResolvedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEntry_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEntry_Product] FOREIGN KEY ([ProductId]) REFERENCES [catalog].[Product] ([ProductId]);


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEntry_OrderItem] FOREIGN KEY ([OrderItemId]) REFERENCES [sales].[OrderItem] ([OrderItemId]);


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEntry_OrderItem_Branch] FOREIGN KEY ([OrderItemId], [BranchId]) REFERENCES [sales].[OrderItem] ([OrderItemId], [BranchId]);


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEntry_OrderItem_ProductBranch] FOREIGN KEY ([OrderItemId], [ProductId], [BranchId]) REFERENCES [sales].[OrderItem] ([OrderItemId], [ProductId], [BranchId]);


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEntry_ShiftAssignment] FOREIGN KEY ([ShiftAssignmentId]) REFERENCES [hr].[ShiftAssignment] ([ShiftAssignmentId]);


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEntry_ShiftAssignment_Branch] FOREIGN KEY ([ShiftAssignmentId], [BranchId]) REFERENCES [hr].[ShiftAssignment] ([ShiftAssignmentId], [BranchId]);


GO


GO



GO


GO



GO


GO

ALTER TABLE [inventory].[InventoryTransaction] WITH NOCHECK
    ADD CONSTRAINT [FK_InventoryTransaction_Ingredient] FOREIGN KEY ([IngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO

ALTER TABLE [inventory].[InventoryTransaction] WITH NOCHECK
    ADD CONSTRAINT [FK_InventoryTransaction_UserAccount_CreatedBy] FOREIGN KEY ([CreatedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [inventory].[InventoryTransaction] WITH NOCHECK
    ADD CONSTRAINT [FK_InventoryTransaction_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [inventory].[PrepBatch] WITH NOCHECK
    ADD CONSTRAINT [FK_PrepBatch_UserAccount_ReviewedBy] FOREIGN KEY ([ReviewedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [inventory].[PrepBatch] WITH NOCHECK
    ADD CONSTRAINT [FK_PrepBatch_WasteEntry_WriteOff] FOREIGN KEY ([WriteOffWasteEntryId]) REFERENCES [inventory].[WasteEntry] ([WasteEntryId]);


GO


GO

ALTER TABLE [inventory].[PrepBatch] WITH NOCHECK
    ADD CONSTRAINT [FK_PrepBatch_Ingredient_PreppedTyped] FOREIGN KEY ([PreppedIngredientId], [PreppedTypeGuard]) REFERENCES [catalog].[Ingredient] ([IngredientId], [IngredientType]);


GO


GO

ALTER TABLE [inventory].[PrepBatch] WITH NOCHECK
    ADD CONSTRAINT [FK_PrepBatch_Ingredient_PreppedIngredient] FOREIGN KEY ([PreppedIngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO

ALTER TABLE [inventory].[PrepBatch] WITH NOCHECK
    ADD CONSTRAINT [FK_PrepBatch_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [inventory].[PrepBatch] WITH NOCHECK
    ADD CONSTRAINT [FK_PrepBatch_UserAccount_MadeBy] FOREIGN KEY ([MadeBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO



GO


GO

ALTER TABLE [inventory].[StockAdjustment] WITH NOCHECK
    ADD CONSTRAINT [FK_StockAdjustment_Ingredient] FOREIGN KEY ([IngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO



GO


GO

ALTER TABLE [inventory].[StockAdjustment] WITH NOCHECK
    ADD CONSTRAINT [FK_StockAdjustment_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [inventory].[StockAdjustment] WITH NOCHECK
    ADD CONSTRAINT [FK_StockAdjustment_UserAccount_CountedBy] FOREIGN KEY ([CountedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [inventory].[StockAdjustment] WITH NOCHECK
    ADD CONSTRAINT [FK_StockAdjustment_UserAccount_AdjustedBy] FOREIGN KEY ([AdjustedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [ops].[ActivityLog] WITH NOCHECK
    ADD CONSTRAINT [FK_ActivityLog_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [ops].[ActivityLog] WITH NOCHECK
    ADD CONSTRAINT [FK_ActivityLog_UserAccount_PerformedBy] FOREIGN KEY ([PerformedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [ops].[OutboxEvent] WITH NOCHECK
    ADD CONSTRAINT [FK_OutboxEvent_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [org].[Branch] WITH NOCHECK
    ADD CONSTRAINT [FK_Branch_UserAccount_Manager] FOREIGN KEY ([ManagerUserId]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [payment].[CashierShift] WITH NOCHECK
    ADD CONSTRAINT [FK_CashierShift_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [payment].[CashierShift] WITH NOCHECK
    ADD CONSTRAINT [FK_CashierShift_UserAccount_Cashier] FOREIGN KEY ([CashierId]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [FK_Bill_CashierShift] FOREIGN KEY ([CashierShiftId]) REFERENCES [payment].[CashierShift] ([CashierShiftId]);


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [FK_Bill_CashierShift_Branch] FOREIGN KEY ([CashierShiftId], [BranchId]) REFERENCES [payment].[CashierShift] ([CashierShiftId], [BranchId]);


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [FK_Bill_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [sales].[SalesOrder] WITH NOCHECK
    ADD CONSTRAINT [FK_SalesOrder_DiningTable] FOREIGN KEY ([DiningTableId]) REFERENCES [sales].[DiningTable] ([DiningTableId]);


GO


GO

ALTER TABLE [sales].[SalesOrder] WITH NOCHECK
    ADD CONSTRAINT [FK_SalesOrder_DiningTable_Branch] FOREIGN KEY ([DiningTableId], [BranchId]) REFERENCES [sales].[DiningTable] ([DiningTableId], [BranchId]);


GO


GO

ALTER TABLE [sales].[SalesOrder] WITH NOCHECK
    ADD CONSTRAINT [FK_SalesOrder_UserAccount_CreatedBy] FOREIGN KEY ([CreatedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [sales].[SalesOrder] WITH NOCHECK
    ADD CONSTRAINT [FK_SalesOrder_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [sales].[DiningTable] WITH NOCHECK
    ADD CONSTRAINT [FK_DiningTable_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [sales].[OrderItemModifier] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItemModifier_OrderItem] FOREIGN KEY ([OrderItemId]) REFERENCES [sales].[OrderItem] ([OrderItemId]) ON DELETE CASCADE;


GO


GO

ALTER TABLE [sales].[OrderItemModifier] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItemModifier_ModifierOption] FOREIGN KEY ([ModifierOptionId]) REFERENCES [catalog].[ModifierOption] ([ModifierOptionId]);


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItem_UserAccount_Barista] FOREIGN KEY ([BaristaId]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItem_SalesOrder_Branch] FOREIGN KEY ([OrderId], [BranchId]) REFERENCES [sales].[SalesOrder] ([OrderId], [BranchId]);


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItem_UserAccount_PreparedBy] FOREIGN KEY ([PreparedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItem_Product] FOREIGN KEY ([ProductId]) REFERENCES [catalog].[Product] ([ProductId]);


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItem_UserAccount_IssueReportedBy] FOREIGN KEY ([IssueReportedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItem_SalesOrder] FOREIGN KEY ([OrderId]) REFERENCES [sales].[SalesOrder] ([OrderId]) ON DELETE CASCADE;


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItem_UserAccount_PickedUpBy] FOREIGN KEY ([PickedUpBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItem_Bill] FOREIGN KEY ([BillId]) REFERENCES [payment].[Bill] ([BillId]);


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItem_Bill_Branch] FOREIGN KEY ([BillId], [BranchId]) REFERENCES [payment].[Bill] ([BillId], [BranchId]);


GO


GO



GO


GO

ALTER TABLE [catalog].[BranchMenu] WITH NOCHECK
    ADD CONSTRAINT [CK_BranchMenu_LocalPrice] CHECK ([LocalPrice] IS NULL OR [LocalPrice]>=(0));


GO


GO

ALTER TABLE [catalog].[BranchMenu] WITH NOCHECK
    ADD CONSTRAINT [CK_BranchMenu_BlockLifecycle] CHECK ([BlockReason] IS NULL AND [BlockNote] IS NULL AND [BlockRequestedBy] IS NULL AND [BlockRequestedAt] IS NULL AND [BlockReopenRequestedAt] IS NULL AND [BlockStatus] IS NULL AND [BlockReviewedBy] IS NULL AND [BlockReviewedAt] IS NULL AND [BlockReviewNote] IS NULL OR [BlockStatus]='PENDING' AND [BlockReason] IS NOT NULL AND [BlockRequestedBy] IS NOT NULL AND [BlockRequestedAt] IS NOT NULL AND [BlockReviewedBy] IS NULL AND [BlockReviewedAt] IS NULL OR [BlockStatus]='APPROVED' AND [BlockReason] IS NOT NULL AND [BlockRequestedBy] IS NOT NULL AND [BlockRequestedAt] IS NOT NULL AND [BlockReviewedBy] IS NOT NULL AND [BlockReviewedAt] IS NOT NULL);


GO


GO

ALTER TABLE [catalog].[BranchMenu] WITH NOCHECK
    ADD CONSTRAINT [CK_BranchMenu_BlockStatus] CHECK ([BlockStatus] IS NULL OR [BlockStatus]='APPROVED' OR [BlockStatus]='PENDING');


GO


GO

ALTER TABLE [catalog].[BranchMenu] WITH NOCHECK
    ADD CONSTRAINT [CK_BranchMenu_BlockTimeOrder] CHECK (([BackInEta] IS NULL OR [BlockRequestedAt] IS NULL OR [BackInEta]>=[BlockRequestedAt]) AND ([BlockReopenRequestedAt] IS NULL OR [BlockReopenRequestedAt]>=[BlockRequestedAt]) AND ([BlockReviewedAt] IS NULL OR [BlockReviewedAt]>=[BlockRequestedAt]));


GO


GO

ALTER TABLE [catalog].[ModifierGroup] WITH NOCHECK
    ADD CONSTRAINT [CK_ModifierGroup_SelectionRange] CHECK ([MinSelect]>=(0) AND [MaxSelect]>=[MinSelect] AND ([IsRequired]=(0) OR [MinSelect]>=(1)));


GO


GO

ALTER TABLE [catalog].[Ingredient] WITH NOCHECK
    ADD CONSTRAINT [CK_Ingredient_Type] CHECK ([IngredientType]='PREPPED' OR [IngredientType]='RAW');


GO


GO

ALTER TABLE [catalog].[Ingredient] WITH NOCHECK
    ADD CONSTRAINT [CK_Ingredient_ShelfLife] CHECK ([IngredientType]='RAW' AND [ShelfLifeMinutes] IS NULL OR [IngredientType]='PREPPED' AND ([ShelfLifeMinutes] IS NULL OR [ShelfLifeMinutes]>=(60) AND [ShelfLifeMinutes]<=(43200)));


GO


GO

ALTER TABLE [catalog].[Ingredient] WITH NOCHECK
    ADD CONSTRAINT [CK_Ingredient_PrepYieldQty] CHECK ([PrepYieldQty] IS NULL OR [IngredientType]='PREPPED' AND [PrepYieldQty]>(0));


GO


GO

ALTER TABLE [catalog].[Ingredient] WITH NOCHECK
    ADD CONSTRAINT [CK_Ingredient_PurchaseUnit] CHECK ([PurchaseUnitName] IS NULL AND [PurchaseFactorToBase] IS NULL OR [PurchaseUnitName] IS NOT NULL AND [PurchaseFactorToBase] IS NOT NULL AND [PurchaseFactorToBase]>(0));


GO


GO

ALTER TABLE [catalog].[Product] WITH NOCHECK
    ADD CONSTRAINT [CK_Product_BasePrice_Value] CHECK ([BasePrice]>=(0));


GO


GO

ALTER TABLE [catalog].[Product] WITH NOCHECK
    ADD CONSTRAINT [CK_Product_PrepSeconds] CHECK ([PrepSeconds]>(0));


GO


GO



GO


GO



GO


GO



GO


GO



GO


GO



GO


GO

ALTER TABLE [hr].[ShiftAssignment] WITH NOCHECK
    ADD CONSTRAINT [CK_ShiftAssignment_NonZeroDuration] CHECK ([StartTime]<>[EndTime]);


GO


GO
ALTER TABLE [hr].[ShiftAssignment] WITH NOCHECK
    ADD CONSTRAINT [CK_ShiftAssignment_HourlyRateSnapshot] CHECK ([HourlyRateSnapshot] IS NULL OR [HourlyRateSnapshot]>=(0));


GO


GO

ALTER TABLE [hr].[ShiftAssignment] WITH NOCHECK
    ADD CONSTRAINT [CK_ShiftAssignment_AttendanceStatus] CHECK ([AttendanceStatus] IS NULL OR [AttendanceStatus] IN ('PENDING','APPROVED','REJECTED'));


GO


GO

ALTER TABLE [hr].[ShiftAssignment] WITH NOCHECK
    ADD CONSTRAINT [CK_ShiftAssignment_CheckOutAfterIn] CHECK ([CheckOutAt] IS NULL OR [CheckInAt] IS NULL OR [CheckOutAt]>=[CheckInAt]);


GO


GO

ALTER TABLE [hr].[ShiftAssignment] WITH NOCHECK
    ADD CONSTRAINT [CK_ShiftAssignment_ApprovalLifecycle] CHECK (([AttendanceStatus] IS NULL AND [ApprovedBy] IS NULL AND [ApprovedAt] IS NULL) OR ([AttendanceStatus] IS NOT NULL AND (([AttendanceStatus]='PENDING' AND [ApprovedBy] IS NULL AND [ApprovedAt] IS NULL) OR ([AttendanceStatus] IN ('APPROVED','REJECTED') AND [ApprovedBy] IS NOT NULL AND [ApprovedAt] IS NOT NULL))));


GO


GO

ALTER TABLE [iam].[UserAccount] WITH NOCHECK
    ADD CONSTRAINT [CK_UserAccount_Status] CHECK ([Status]='LOCKED' OR [Status]='ACTIVE');


GO


GO

ALTER TABLE [iam].[UserAccount] WITH NOCHECK
    ADD CONSTRAINT [CK_UserAccount_RoleCode] CHECK ([RoleCode] IN ('ADMIN','BRANCH_MANAGER','CASHIER','BARISTA'));


GO


GO

ALTER TABLE [iam].[UserAccount] WITH NOCHECK
    ADD CONSTRAINT [CK_UserAccount_HourlyRate] CHECK ([HourlyRate] IS NULL OR [HourlyRate]>=(0));


GO


GO

ALTER TABLE [inventory].[StockReceiptLine] WITH NOCHECK
    ADD CONSTRAINT [CK_StockReceiptLine_Status] CHECK ([Status]='CANCELLED' OR [Status]='CONFIRMED' OR [Status]='DRAFT');


GO


GO

ALTER TABLE [inventory].[StockReceiptLine] WITH NOCHECK
    ADD CONSTRAINT [CK_StockReceiptLine_EnteredQuantity] CHECK ([EnteredQuantity]>(0));


GO


GO

ALTER TABLE [inventory].[BranchInventory] WITH NOCHECK
    ADD CONSTRAINT [CK_BranchInventory_Threshold] CHECK ([MinThreshold]>=(0));


GO


GO

ALTER TABLE [inventory].[BranchInventory] WITH NOCHECK
    ADD CONSTRAINT [CK_BranchInventory_PrepTarget] CHECK ([PrepTargetQty] IS NULL OR [PrepTargetQty]>[MinThreshold]);


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEntry_Quantity_Value] CHECK ([Quantity]>(0));


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEntry_UnitCost] CHECK ([UnitCostAtLog] IS NULL OR [UnitCostAtLog]>=(0));


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEntry_Status] CHECK ([Status]='VOIDED' OR [Status]='ACTIVE');


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEntry_Type] CHECK ([WasteType]='OTHER' OR [WasteType]='REMAKE' OR [WasteType]='EXPIRED' OR [WasteType]='SPILL');


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEntry_CostBasis] CHECK ([CostBasis] IS NULL OR ([CostBasis]='LEGACY_ESTIMATE' OR [CostBasis]='UNAVAILABLE' OR [CostBasis]='SNAPSHOT'));


GO


GO

ALTER TABLE [inventory].[StockReceiptLine] WITH NOCHECK
    ADD CONSTRAINT [CK_StockReceiptLine_UnitCost] CHECK ([UnitCost]>=(0));


GO


GO

ALTER TABLE [inventory].[StockReceiptLine] WITH NOCHECK
    ADD CONSTRAINT [CK_StockReceiptLine_BasePrecision] CHECK ([EnteredQuantity]*[FactorToBaseAtEntry]=CONVERT([decimal](12,3),[EnteredQuantity]*[FactorToBaseAtEntry]));


GO


GO

ALTER TABLE [inventory].[StockReceiptLine] WITH NOCHECK
    ADD CONSTRAINT [CK_StockReceiptLine_FactorToBaseAtEntry] CHECK ([FactorToBaseAtEntry]>(0));


GO


GO

ALTER TABLE [inventory].[InventoryTransaction] WITH NOCHECK
    ADD CONSTRAINT [CK_InventoryTransaction_ChangeQty] CHECK ([ChangeQty]<>(0));


GO


GO

ALTER TABLE [inventory].[InventoryTransaction] WITH NOCHECK
    ADD CONSTRAINT [CK_InventoryTransaction_Reference] CHECK ([ReferenceType] IS NULL AND [ReferenceId] IS NULL OR (([ReferenceType]) collate Latin1_General_100_BIN2='STOCK_ADJUSTMENT' OR ([ReferenceType]) collate Latin1_General_100_BIN2='WASTE_ENTRY' OR ([ReferenceType]) collate Latin1_General_100_BIN2='PREP_BATCH' OR ([ReferenceType]) collate Latin1_General_100_BIN2='ORDER_ITEM' OR ([ReferenceType]) collate Latin1_General_100_BIN2='STOCK_RECEIPT_LINE') AND [ReferenceId] IS NOT NULL AND (([ReferenceType]) collate Latin1_General_100_BIN2='STOCK_RECEIPT_LINE' OR TRY_CONVERT(BIGINT,[ReferenceId]) IS NOT NULL));


GO


GO

ALTER TABLE [inventory].[InventoryTransaction] WITH NOCHECK
    ADD CONSTRAINT [CK_InventoryTransaction_Type] CHECK ([TxnType]='ADJUST' OR [TxnType]='PREP_OUT' OR [TxnType]='PREP_IN' OR [TxnType]='WASTE' OR [TxnType]='DEDUCT' OR [TxnType]='RECEIPT');


GO


GO

ALTER TABLE [inventory].[PrepBatch] WITH NOCHECK
    ADD CONSTRAINT [CK_PrepBatch_QuantityProduced_Value] CHECK ([QuantityProduced]>(0));


GO


GO

ALTER TABLE [inventory].[PrepBatch] WITH NOCHECK
    ADD CONSTRAINT [CK_PrepBatch_Lifecycle] CHECK (([ReviewedBy] IS NULL AND [ReviewedAt] IS NULL OR [ReviewedBy] IS NOT NULL AND [ReviewedAt] IS NOT NULL) AND ([ExpiresAt] IS NULL OR [ExpiresAt]>=[MadeAt]) AND ([Status]<>'PENDING' OR [RequiresApproval]=(1) AND [ReviewedBy] IS NULL) AND ([Status]<>'REJECTED' OR [RequiresApproval]=(1) AND [ReviewedBy] IS NOT NULL) AND ([Status]<>'ACTIVE' OR [RequiresApproval]=(0) OR [ReviewedBy] IS NOT NULL) AND ([Status]<>'CANCELLED' OR [VoidedAt] IS NOT NULL) AND ([WrittenOffAt] IS NULL AND [WriteOffWasteEntryId] IS NULL OR [WrittenOffAt] IS NOT NULL AND [WriteOffWasteEntryId] IS NOT NULL));


GO


GO

ALTER TABLE [inventory].[PrepBatch] WITH NOCHECK
    ADD CONSTRAINT [CK_PrepBatch_Status] CHECK ([Status]='REJECTED' OR [Status]='PENDING' OR [Status]='CANCELLED' OR [Status]='ACTIVE');


GO


GO

ALTER TABLE [inventory].[StockAdjustment] WITH NOCHECK
    ADD CONSTRAINT [CK_StockAdjustment_ConvertedActual] CHECK ([ActualBaseQty]=CONVERT([decimal](12,3),[CountedQuantity]*[FactorToBaseAtCount]));


GO


GO

ALTER TABLE [inventory].[StockAdjustment] WITH NOCHECK
    ADD CONSTRAINT [CK_StockAdjustment_FactorToBaseAtCount] CHECK ([FactorToBaseAtCount]>(0));


GO


GO

ALTER TABLE [inventory].[StockAdjustment] WITH NOCHECK
    ADD CONSTRAINT [CK_StockAdjustment_ActualBaseQty] CHECK ([ActualBaseQty]>=(0));


GO


GO

ALTER TABLE [inventory].[StockAdjustment] WITH NOCHECK
    ADD CONSTRAINT [CK_StockAdjustment_CountedQuantity] CHECK ([CountedQuantity]>=(0));


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEntry_Kind] CHECK ([EventKind]='REMAKE' OR [EventKind]='INGREDIENT_WASTE');


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEntry_EventShape] CHECK ([EventKind]='REMAKE' AND [ProductId] IS NOT NULL AND [CupQuantity] IS NOT NULL AND [CupQuantity]>(0) AND [WasteType]='REMAKE' OR [EventKind]='INGREDIENT_WASTE' AND [ProductId] IS NULL AND [CupQuantity] IS NULL AND [WasteType] IN ('SPILL','EXPIRED','OTHER'));


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEntry_Source] CHECK ([Source]='KDS' OR [Source]='MANUAL');


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEntry_ReviewLifecycle] CHECK (
        [ReviewStatus] IS NULL AND [ReviewType] IS NULL AND [QtyBefore] IS NULL AND [QtyAfter] IS NULL
            AND [ReviewNote] IS NULL AND [ResolvedBy] IS NULL AND [ResolvedAt] IS NULL AND [ResolutionNote] IS NULL
        OR [ReviewStatus]='OPEN' AND [ReviewType] IS NOT NULL AND [QtyBefore] IS NOT NULL AND [QtyAfter] IS NOT NULL
            AND [ResolvedBy] IS NULL AND [ResolvedAt] IS NULL
        OR [ReviewStatus]='RESOLVED' AND [ReviewType] IS NOT NULL AND [QtyBefore] IS NOT NULL AND [QtyAfter] IS NOT NULL
            AND [ResolvedBy] IS NOT NULL AND [ResolvedAt] IS NOT NULL);


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEntry_ReviewStatus] CHECK ([ReviewStatus] IS NULL OR [ReviewStatus]='RESOLVED' OR [ReviewStatus]='OPEN');


GO


GO

ALTER TABLE [inventory].[WasteEntry] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEntry_ReviewType] CHECK ([ReviewType] IS NULL OR [ReviewType]='MANAGER_VOID' OR [ReviewType]='LATE_CORRECTION' OR [ReviewType]='HARD_NEGATIVE' OR [ReviewType]='SOFT_NEGATIVE');


GO


GO

ALTER TABLE [ops].[ActivityLog] WITH NOCHECK
    ADD CONSTRAINT [CK_ActivityLog_EntityType] CHECK ([EntityType] IN ('ORDER_ITEM','WASTE_ENTRY','MENU_BLOCK'));


GO


GO

ALTER TABLE [ops].[OutboxEvent] WITH NOCHECK
    ADD CONSTRAINT [CK_OutboxEvent_PayloadJson] CHECK ([Payload] IS NULL OR isjson([Payload])=(1));


GO


GO

ALTER TABLE [org].[Branch] WITH NOCHECK
    ADD CONSTRAINT [CK_Branch_PeakThreshold] CHECK ([PeakThresholdCups]>=(0));


GO


GO

ALTER TABLE [payment].[CashierShift] WITH NOCHECK
    ADD CONSTRAINT [CK_CashierShift_Money] CHECK ([OpeningCash]>=(0) AND ([ClosingCash] IS NULL OR [ClosingCash]>=(0)));


GO


GO

ALTER TABLE [payment].[CashierShift] WITH NOCHECK
    ADD CONSTRAINT [CK_CashierShift_CloseState] CHECK ([ClosedAt] IS NULL AND [ClosingCash] IS NULL OR [ClosedAt] IS NOT NULL AND [ClosingCash] IS NOT NULL AND [ClosedAt]>=[OpenedAt]);


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [CK_OrderItem_BilledAmount] CHECK ([BilledAmount] IS NULL OR [BilledAmount]>=(0));


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [CK_Bill_CashSettlement] CHECK ([PaymentMethod]='CASH' AND [CashTendered] IS NOT NULL AND [CashChange] IS NOT NULL OR ([PaymentMethod]='QR_BANK' OR [PaymentMethod]='TRANSFER') AND [CashTendered] IS NULL AND [CashChange] IS NULL OR [PaymentMethod] IS NULL);


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [CK_Bill_PaymentMethod] CHECK ([PaymentMethod]='QR_BANK' OR [PaymentMethod]='TRANSFER' OR [PaymentMethod]='CASH');


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [CK_Bill_Amounts] CHECK ([Subtotal]>=(0) AND [VatAmount]>=(0) AND [DiscountAmount]>=(0) AND [TotalAmount]>=(0) AND [DiscountAmount]<=[Subtotal] AND [TotalAmount]=(([Subtotal]-[DiscountAmount])+[VatAmount]));


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [CK_Bill_SettlementAmounts] CHECK ([RoundingAdjustment]>=(-500) AND [RoundingAdjustment]<=(500) AND ([PaidAmount] IS NULL OR [PaidAmount]=([TotalAmount]+[RoundingAdjustment])) AND ([CashTendered] IS NULL OR [CashTendered]>=(0)) AND ([CashChange] IS NULL OR [CashChange]>=(0)) AND ([CashTendered] IS NULL OR [CashChange] IS NULL OR [PaidAmount]=([CashTendered]-[CashChange])));


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [CK_Bill_Status] CHECK (([Status]) collate Latin1_General_100_BIN2='VOID' OR ([Status]) collate Latin1_General_100_BIN2='PAID' OR ([Status]) collate Latin1_General_100_BIN2='UNPAID');


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [CK_Bill_PaidHasAmount] CHECK ([Status]<>'PAID' OR [PaidAmount] IS NOT NULL);


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [CK_Bill_PaymentLifecycle] CHECK ([Status]<>'PAID' OR [PaymentMethod] IS NOT NULL AND [CashierShiftId] IS NOT NULL AND [PaidAmount] IS NOT NULL AND [PaidAt] IS NOT NULL);


GO


GO

ALTER TABLE [sales].[SalesOrder] WITH NOCHECK
    ADD CONSTRAINT [CK_SalesOrder_Source] CHECK ([Source]='QR' OR [Source]='COUNTER');


GO


GO

ALTER TABLE [sales].[SalesOrder] WITH NOCHECK
    ADD CONSTRAINT [CK_SalesOrder_Status] CHECK ([Status]='CANCELLED' OR [Status]='COMPLETED' OR [Status]='ACTIVE');


GO


GO

ALTER TABLE [sales].[SalesOrder] WITH NOCHECK
    ADD CONSTRAINT [CK_SalesOrder_TypeDiningTable] CHECK ([OrderType]='DINE_IN' AND [DiningTableId] IS NOT NULL OR [OrderType]='TAKEAWAY' AND [DiningTableId] IS NULL);


GO


GO

ALTER TABLE [sales].[SalesOrder] WITH NOCHECK
    ADD CONSTRAINT [CK_SalesOrder_Type] CHECK (([OrderType]) collate Latin1_General_100_BIN2='TAKEAWAY' OR ([OrderType]) collate Latin1_General_100_BIN2='DINE_IN');


GO


GO

ALTER TABLE [sales].[SalesOrder] WITH NOCHECK
    ADD CONSTRAINT [CK_SalesOrder_SourceCreator] CHECK ([Source]='COUNTER' AND [CreatedBy] IS NOT NULL OR [Source]='QR' AND [CreatedBy] IS NULL);


GO


GO

ALTER TABLE [sales].[DiningTable] WITH NOCHECK
    ADD CONSTRAINT [CK_DiningTable_Status] CHECK (([Status]) collate Latin1_General_100_BIN2='OCCUPIED' OR ([Status]) collate Latin1_General_100_BIN2='EMPTY');


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [CK_OrderItem_Status] CHECK ([Status]='REMAKE' OR [Status]='CANCELLED' OR [Status]='BLOCKED' OR [Status]='SERVED' OR [Status]='PICKED_UP' OR [Status]='READY' OR [Status]='MAKING' OR [Status]='WAITING');


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [CK_OrderItem_ServedAfterDone] CHECK ([ServedAt] IS NULL OR [DoneAt] IS NOT NULL AND [ServedAt]>=[DoneAt]);


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [CK_OrderItem_PickedUpAfterDone] CHECK ([PickedUpAt] IS NULL OR [DoneAt] IS NOT NULL AND [PickedUpAt]>=[DoneAt]);


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [CK_OrderItem_DoneAfterStarted] CHECK ([DoneAt] IS NULL OR [StartedAt] IS NOT NULL AND [DoneAt]>=[StartedAt]);


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [CK_OrderItem_StatusTimestamps] CHECK (([Status]<>'MAKING' OR [StartedAt] IS NOT NULL) AND ([Status]<>'READY' OR [StartedAt] IS NOT NULL AND [DoneAt] IS NOT NULL) AND ([Status]<>'PICKED_UP' OR [DoneAt] IS NOT NULL AND [PickedUpAt] IS NOT NULL) AND ([Status]<>'SERVED' OR [DoneAt] IS NOT NULL AND [PickedUpAt] IS NOT NULL AND [ServedAt] IS NOT NULL));


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [CK_OrderItem_Quantity_Value] CHECK ([Quantity]>(0));


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [CK_OrderItem_ServedAfterPickedUp] CHECK ([ServedAt] IS NULL OR [PickedUpAt] IS NOT NULL AND [ServedAt]>=[PickedUpAt]);


GO


GO

ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [CK_OrderItem_NonNegative] CHECK ([UnitPrice]>=(0) AND [RemakeCount]>=(0));


GO


GO
ALTER TABLE [sales].[OrderItem] WITH NOCHECK
    ADD CONSTRAINT [CK_OrderItem_BillingLifecycle] CHECK ([BillId] IS NULL AND [BilledAmount] IS NULL OR [BillId] IS NOT NULL AND [BilledAmount] IS NOT NULL);


GO


GO
CREATE   TRIGGER catalog.TR_Ingredient_ProtectBaseUnit
ON catalog.Ingredient AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i JOIN deleted d ON d.IngredientId=i.IngredientId
              WHERE i.Unit<>d.Unit AND EXISTS(
                  SELECT 1 FROM inventory.InventoryTransaction t
                  WHERE t.IngredientId=i.IngredientId))
        THROW 51010,'Không được đổi đơn vị gốc của nguyên liệu đã có ledger.',1;
END;
GO


GO
CREATE   TRIGGER catalog.TR_Recipe_ValidateOwnerAndIngredient
ON catalog.Recipe AFTER INSERT,UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1
        FROM inserted i
        WHERE i.OwnerType NOT IN ('PRODUCT','PREPPED','MODIFIER')
           OR (i.OwnerType='PRODUCT' AND NOT EXISTS(
               SELECT 1 FROM catalog.Product p WHERE p.ProductId=i.OwnerId))
           OR (i.OwnerType='PREPPED' AND NOT EXISTS(
               SELECT 1 FROM catalog.Ingredient ownerIngredient
               WHERE ownerIngredient.IngredientId=i.OwnerId
                 AND ownerIngredient.IngredientType='PREPPED'))
           OR (i.OwnerType='PREPPED' AND NOT EXISTS(
               SELECT 1 FROM catalog.Ingredient rawIngredient
               WHERE rawIngredient.IngredientId=i.IngredientId
                 AND rawIngredient.IngredientType='RAW'))
           OR (i.OwnerType='MODIFIER' AND NOT EXISTS(
               SELECT 1 FROM catalog.ModifierOption mo WHERE mo.ModifierOptionId=i.OwnerId))
           OR NOT EXISTS(
               SELECT 1 FROM catalog.Ingredient ingredient
               WHERE ingredient.IngredientId=i.IngredientId)
           OR (i.OwnerType='MODIFIER' AND i.Quantity=(0))
           OR (i.OwnerType<>'MODIFIER' AND i.Quantity<=(0))
    ) THROW 51013,N'Công thức không hợp lệ: chủ sở hữu, loại nguyên liệu hoặc số lượng không đúng.',1;
END;
GO


GO
CREATE   TRIGGER catalog.TR_BranchMenu_BlockActorBranch
ON catalog.BranchMenu AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted i
        LEFT JOIN iam.UserAccount requester ON requester.UserId=i.BlockRequestedBy
        LEFT JOIN iam.UserAccount reviewer ON reviewer.UserId=i.BlockReviewedBy
        WHERE (i.BlockRequestedBy IS NOT NULL AND (requester.UserId IS NULL OR requester.BranchId<>i.BranchId
               OR requester.Status<>'ACTIVE' OR requester.RoleCode<>'BARISTA'))
           OR (i.BlockReviewedBy IS NOT NULL AND (reviewer.UserId IS NULL OR reviewer.BranchId<>i.BranchId
               OR reviewer.Status<>'ACTIVE' OR reviewer.RoleCode<>'BRANCH_MANAGER'))
    ) THROW 51145,N'Người báo phải là BARISTA và người duyệt phải là BRANCH_MANAGER active đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER hr.TR_ShiftAssignment_UserBranch
ON hr.ShiftAssignment AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i LEFT JOIN iam.UserAccount u ON u.UserId=i.UserId
              WHERE u.UserId IS NULL OR u.BranchId<>i.BranchId OR u.Status<>'ACTIVE')
        THROW 51134,N'Nhân viên được xếp ca phải active trong đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER hr.TR_ShiftAssignment_ApproverBranch
ON hr.ShiftAssignment AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted i
        LEFT JOIN iam.UserAccount u ON u.UserId=i.ApprovedBy
        WHERE i.ApprovedBy IS NOT NULL AND (u.UserId IS NULL OR u.BranchId<>i.BranchId
              OR u.Status<>'ACTIVE' OR u.RoleCode<>'BRANCH_MANAGER')
    ) THROW 51139,N'Người duyệt chấm công phải là BRANCH_MANAGER active đúng chi nhánh.',1;
END
GO


GO
/* Actor/role validation: kiểm tra tại thời điểm ghi, không khóa lịch sử khi nhân
   viên được chuyển chi nhánh về sau. */
CREATE   TRIGGER iam.TR_UserAccount_RoleBranch
ON iam.UserAccount AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted i
        WHERE (i.RoleCode='ADMIN' AND i.BranchId IS NOT NULL)
           OR (i.RoleCode<>'ADMIN' AND i.BranchId IS NULL)
    ) THROW 51131,N'Admin phải toàn hệ thống; nhân viên vận hành phải thuộc một chi nhánh.',1;

    DECLARE @todayVn DATE=CONVERT(date,DATEADD(hour,7,SYSUTCDATETIME()));
    IF EXISTS(
        SELECT 1 FROM inserted i JOIN deleted d ON d.UserId=i.UserId
        WHERE ISNULL(i.BranchId,-1)<>ISNULL(d.BranchId,-1)
          AND (
              EXISTS(SELECT 1 FROM org.Branch b WHERE b.ManagerUserId=i.UserId)
           OR EXISTS(SELECT 1 FROM payment.CashierShift cs WHERE cs.CashierId=i.UserId AND cs.ClosedAt IS NULL)
           OR EXISTS(SELECT 1 FROM hr.ShiftAssignment sa WHERE sa.UserId=i.UserId AND sa.WorkDate>=@todayVn)
           OR EXISTS(SELECT 1 FROM hr.ShiftAssignment sa
                     WHERE sa.UserId=i.UserId AND sa.CheckInAt IS NOT NULL AND sa.CheckOutAt IS NULL)
          )
    ) THROW 51132,N'Không thể chuyển chi nhánh khi nhân viên còn vai trò/ca/lịch đang hoạt động.',1;
END
GO


GO
CREATE   TRIGGER inventory.TR_StockReceiptLine_ImmutableIdentity ON inventory.StockReceiptLine AFTER INSERT,UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i JOIN deleted d ON d.StockReceiptLineId=i.StockReceiptLineId
              WHERE i.ReceiptBatchId<>d.ReceiptBatchId OR i.BranchId<>d.BranchId
                 OR i.IngredientId<>d.IngredientId OR i.ReceivedBy<>d.ReceivedBy)
        THROW 51021,'Không được đổi batch/branch/receiver của phiếu nhập lịch sử.',1;

    /* Các cột header lặp phải đồng nhất trong toàn bộ batch. */
    IF EXISTS(
        SELECT 1 FROM inserted i
        JOIN inventory.StockReceiptLine r ON r.ReceiptBatchId=i.ReceiptBatchId
        WHERE r.StockReceiptLineId<>i.StockReceiptLineId
          AND (r.BranchId<>i.BranchId OR r.ReceivedBy<>i.ReceivedBy OR r.DocumentDate<>i.DocumentDate
               OR r.Status<>i.Status OR ISNULL(r.SupplierId,-1)<>ISNULL(i.SupplierId,-1)
               OR ISNULL(r.Note,N'')<>ISNULL(i.Note,N'') OR r.CreatedAt<>i.CreatedAt))
        THROW 51025,N'Các dòng cùng ReceiptBatchId phải có thông tin phiếu đồng nhất.',1;
END;
GO


GO
/* ReceivedBy chỉ được xác minh thuộc branch tại thời điểm tạo. Sau đó người này có
   thể chuyển chi nhánh; immutable trigger ở trên vẫn bảo vệ identity lịch sử. */
CREATE   TRIGGER inventory.TR_StockReceiptLine_ActorBranch
ON inventory.StockReceiptLine AFTER INSERT AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i LEFT JOIN iam.UserAccount u ON u.UserId=i.ReceivedBy
               WHERE u.UserId IS NULL OR u.BranchId<>i.BranchId OR u.Status<>'ACTIVE'
                  OR u.RoleCode<>'BRANCH_MANAGER')
        THROW 51146,N'Người nhận phiếu nhập phải là BRANCH_MANAGER active đúng chi nhánh.',1;
END;
GO


GO
CREATE   TRIGGER inventory.TR_StockReceiptLine_DraftOnly
ON inventory.StockReceiptLine
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1 FROM inserted i
        LEFT JOIN deleted d ON d.StockReceiptLineId=i.StockReceiptLineId
        WHERE d.StockReceiptLineId IS NULL AND i.Status <> 'DRAFT'
    ) OR EXISTS (
        SELECT 1 FROM deleted d
        WHERE d.Status <> 'DRAFT'
    )
        THROW 51010, N'Chỉ được thêm, sửa hoặc xóa dòng phiếu nhập khi batch còn DRAFT.', 1;

    IF EXISTS(
        SELECT 1 FROM inserted i
        JOIN inventory.InventoryTransaction t
          ON t.ReferenceType='STOCK_RECEIPT_LINE' AND t.ReferenceId=i.ReceiptBatchId
         AND t.BranchId=i.BranchId AND t.IngredientId=i.IngredientId
        WHERE i.Status<>'CONFIRMED')
        THROW 51026,N'Phiếu nhập DRAFT không được sinh giao dịch kho.',1;
END
GO


GO
CREATE   TRIGGER inventory.TR_InventoryTransaction_ValidateReference
ON inventory.InventoryTransaction AFTER INSERT,UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted i
        WHERE i.ReferenceType IS NOT NULL AND NOT EXISTS(
            SELECT 1 WHERE
               i.ReferenceType='STOCK_RECEIPT_LINE' AND EXISTS(
                   SELECT 1 FROM inventory.StockReceiptLine r
                   WHERE r.ReceiptBatchId=i.ReferenceId AND r.BranchId=i.BranchId
                     AND r.IngredientId=i.IngredientId AND r.Status='CONFIRMED')
             OR i.ReferenceType='ORDER_ITEM' AND EXISTS(
                   SELECT 1 FROM sales.OrderItem oi
                   WHERE oi.OrderItemId=TRY_CONVERT(BIGINT,i.ReferenceId) AND oi.BranchId=i.BranchId)
             OR i.ReferenceType='PREP_BATCH' AND EXISTS(
                   SELECT 1 FROM inventory.PrepBatch pb
                   WHERE pb.PrepBatchId=TRY_CONVERT(BIGINT,i.ReferenceId) AND pb.BranchId=i.BranchId)
             OR i.ReferenceType='WASTE_ENTRY' AND EXISTS(
                   SELECT 1 FROM inventory.WasteEntry we
                   WHERE we.WasteEntryId=TRY_CONVERT(BIGINT,i.ReferenceId) AND we.BranchId=i.BranchId)
             OR i.ReferenceType='STOCK_ADJUSTMENT' AND EXISTS(
                   SELECT 1 FROM inventory.StockAdjustment sa
                   WHERE sa.StockAdjustmentId=TRY_CONVERT(BIGINT,i.ReferenceId) AND sa.BranchId=i.BranchId)))
        THROW 51022,'Inventory transaction reference không tồn tại hoặc sai chi nhánh.',1;
END;
GO


GO
CREATE   TRIGGER inventory.TR_InventoryTransaction_ActorBranch
ON inventory.InventoryTransaction AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i LEFT JOIN iam.UserAccount u ON u.UserId=i.CreatedBy
               WHERE i.CreatedBy IS NOT NULL AND (u.UserId IS NULL OR u.BranchId<>i.BranchId
                     OR u.Status<>'ACTIVE' OR u.RoleCode NOT IN('BRANCH_MANAGER','BARISTA')))
        THROW 51143,N'Người ghi sổ kho phải là BRANCH_MANAGER/BARISTA active đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER inventory.TR_PrepBatch_ActorBranch
ON inventory.PrepBatch AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted i
        LEFT JOIN iam.UserAccount maker ON maker.UserId=i.MadeBy
        LEFT JOIN iam.UserAccount reviewer ON reviewer.UserId=i.ReviewedBy
        WHERE maker.UserId IS NULL OR maker.BranchId<>i.BranchId OR maker.Status<>'ACTIVE' OR maker.RoleCode<>'BARISTA'
           OR (i.ReviewedBy IS NOT NULL AND (reviewer.UserId IS NULL OR reviewer.BranchId<>i.BranchId
               OR reviewer.Status<>'ACTIVE' OR reviewer.RoleCode<>'BRANCH_MANAGER'))
    ) THROW 51140,N'Người pha phải là BARISTA và người duyệt phải là BRANCH_MANAGER active đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER inventory.TR_StockAdjustment_ActorBranch
ON inventory.StockAdjustment AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted i
        LEFT JOIN iam.UserAccount adjuster ON adjuster.UserId=i.AdjustedBy
        LEFT JOIN iam.UserAccount counter ON counter.UserId=i.CountedBy
        WHERE adjuster.UserId IS NULL OR adjuster.BranchId<>i.BranchId OR adjuster.Status<>'ACTIVE'
           OR adjuster.RoleCode NOT IN('BRANCH_MANAGER','BARISTA')
           OR (i.CountedBy IS NOT NULL AND (counter.UserId IS NULL OR counter.BranchId<>i.BranchId
               OR counter.Status<>'ACTIVE' OR counter.RoleCode<>'BRANCH_MANAGER')))
        THROW 51142,N'Người điều chỉnh/kiểm kê phải active, đúng role và đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER inventory.TR_WasteEntry_ValidateRules
ON inventory.WasteEntry AFTER INSERT,UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted e
               LEFT JOIN sales.OrderItem oi ON oi.OrderItemId=e.OrderItemId
               WHERE e.EventKind='REMAKE' AND (e.ProductId IS NULL OR e.CupQuantity IS NULL OR e.CupQuantity<=(0))
                  OR e.EventKind='INGREDIENT_WASTE' AND (e.ProductId IS NOT NULL OR e.CupQuantity IS NOT NULL)
                  OR e.OrderItemId IS NOT NULL
                     AND (oi.OrderItemId IS NULL OR oi.BranchId<>e.BranchId
                          OR e.ProductId IS NOT NULL AND oi.ProductId<>e.ProductId))
        THROW 51023,'WasteEntry không khớp order item/product/branch.',1;

    IF EXISTS(SELECT 1 FROM inserted e
              WHERE e.EventKind='REMAKE' AND e.WasteType<>'REMAKE'
                 OR e.EventKind='INGREDIENT_WASTE' AND e.WasteType NOT IN('SPILL','EXPIRED','OTHER'))
        THROW 51024,'WasteEntry không khớp event kind.',1;

    IF EXISTS(SELECT 1 FROM inserted e
              WHERE e.EventKind='REMAKE' AND (e.ProductId IS NULL OR e.CupQuantity IS NULL OR e.CupQuantity<=(0))
                 OR e.EventKind='INGREDIENT_WASTE' AND (e.ProductId IS NOT NULL OR e.CupQuantity IS NOT NULL))
        THROW 51028,'EventKind không khớp ProductId/CupQuantity.',1;

    /* Các cột cấp sự kiện lặp phải đồng nhất trong cùng một nhóm. */
    IF EXISTS(
        SELECT 1 FROM inserted i
        JOIN inventory.WasteEntry e
          ON e.BranchId=i.BranchId AND e.EventGroupId=i.EventGroupId AND e.WasteEntryId<>i.WasteEntryId
        WHERE i.EventGroupId IS NOT NULL
          AND (e.EventKind<>i.EventKind OR e.Source<>i.Source
               OR ISNULL(e.ProductId,-1)<>ISNULL(i.ProductId,-1)
               OR ISNULL(e.OrderItemId,-1)<>ISNULL(i.OrderItemId,-1)
               OR ISNULL(e.CupQuantity,-1)<>ISNULL(i.CupQuantity,-1)
               OR e.CauseCode<>i.CauseCode OR ISNULL(e.CauseDetail,N'')<>ISNULL(i.CauseDetail,N'')
               OR ISNULL(e.ShiftAssignmentId,-1)<>ISNULL(i.ShiftAssignmentId,-1)
               OR e.CreatedBy<>i.CreatedBy OR e.CreatedAt<>i.CreatedAt))
        THROW 51027,N'Các dòng cùng EventGroupId phải có thông tin sự kiện đồng nhất.',1;
END;
GO


GO
CREATE   TRIGGER inventory.TR_WasteEntry_ActorBranch
ON inventory.WasteEntry AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted i
        LEFT JOIN iam.UserAccount creator ON creator.UserId=i.CreatedBy
        LEFT JOIN iam.UserAccount logger ON logger.UserId=i.LoggedBy
        LEFT JOIN iam.UserAccount resolver ON resolver.UserId=i.ResolvedBy
        WHERE creator.UserId IS NULL OR creator.BranchId<>i.BranchId OR creator.Status<>'ACTIVE' OR creator.RoleCode<>'BARISTA'
           OR logger.UserId IS NULL OR logger.BranchId<>i.BranchId OR logger.Status<>'ACTIVE' OR logger.RoleCode<>'BARISTA'
           OR (i.ResolvedBy IS NOT NULL AND (resolver.UserId IS NULL OR resolver.BranchId<>i.BranchId
               OR resolver.Status<>'ACTIVE' OR resolver.RoleCode<>'BRANCH_MANAGER'))
    ) THROW 51144,N'Actor của WasteEntry phải active, đúng role và đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER org.TR_Branch_Manager
ON org.Branch AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted b
        LEFT JOIN iam.UserAccount u ON u.UserId=b.ManagerUserId
        WHERE b.ManagerUserId IS NOT NULL
          AND (u.UserId IS NULL OR u.BranchId<>b.BranchId OR u.Status<>'ACTIVE' OR u.RoleCode<>'BRANCH_MANAGER')
    ) THROW 51133,N'Quản lý chi nhánh phải là BRANCH_MANAGER active của chính chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER payment.TR_CashierShift_UserBranch
ON payment.CashierShift AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i LEFT JOIN iam.UserAccount u ON u.UserId=i.CashierId
               WHERE u.UserId IS NULL OR u.BranchId<>i.BranchId OR u.Status<>'ACTIVE' OR u.RoleCode<>'CASHIER')
        THROW 51135,N'CashierShift phải thuộc cashier active của đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER sales.TR_SalesOrder_CreatorBranch
ON sales.SalesOrder AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted i
        LEFT JOIN iam.UserAccount u ON u.UserId=i.CreatedBy
        LEFT JOIN sales.DiningTable dt ON dt.DiningTableId=i.DiningTableId
        WHERE i.CreatedBy IS NOT NULL AND (u.UserId IS NULL OR u.BranchId<>i.BranchId
              OR u.Status<>'ACTIVE' OR u.RoleCode<>'CASHIER')
           OR i.DiningTableId IS NOT NULL AND (dt.DiningTableId IS NULL OR dt.BranchId<>i.BranchId))
        THROW 51136,N'Người tạo đơn và bàn phục vụ phải thuộc đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER sales.TR_OrderItem_ActorBranch
ON sales.OrderItem AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted i
        LEFT JOIN iam.UserAccount b ON b.UserId=i.BaristaId
        LEFT JOIN iam.UserAccount p ON p.UserId=i.PreparedBy
        LEFT JOIN iam.UserAccount ir ON ir.UserId=i.IssueReportedBy
        LEFT JOIN iam.UserAccount pu ON pu.UserId=i.PickedUpBy
        WHERE (i.BaristaId IS NOT NULL AND (b.UserId IS NULL OR b.BranchId<>i.BranchId OR b.Status<>'ACTIVE' OR b.RoleCode<>'BARISTA'))
           OR (i.PreparedBy IS NOT NULL AND (p.UserId IS NULL OR p.BranchId<>i.BranchId OR p.Status<>'ACTIVE' OR p.RoleCode<>'BARISTA'))
           OR (i.IssueReportedBy IS NOT NULL AND (ir.UserId IS NULL OR ir.BranchId<>i.BranchId OR ir.Status<>'ACTIVE' OR ir.RoleCode<>'BARISTA'))
           OR (i.PickedUpBy IS NOT NULL AND (pu.UserId IS NULL OR pu.BranchId<>i.BranchId OR pu.Status<>'ACTIVE' OR pu.RoleCode<>'CASHIER'))
    ) THROW 51137,N'Actor của OrderItem sai role, inactive hoặc khác chi nhánh.',1;
END
GO


GO


GO
ALTER TABLE [catalog].[ModifierOption] WITH CHECK CHECK CONSTRAINT [FK_ModifierOption_ModifierGroup];

ALTER TABLE [catalog].[ModifierGroup] WITH CHECK CHECK CONSTRAINT [FK_ModifierGroup_Product];

ALTER TABLE [catalog].[BranchMenu] WITH CHECK CHECK CONSTRAINT [FK_BranchMenu_Product];

ALTER TABLE [catalog].[BranchMenu] WITH CHECK CHECK CONSTRAINT [FK_BranchMenu_Branch];

ALTER TABLE [catalog].[Product] WITH CHECK CHECK CONSTRAINT [FK_Product_Category];

ALTER TABLE [catalog].[BranchMenu] WITH CHECK CHECK CONSTRAINT [FK_BranchMenu_UserAccount_BlockReviewedBy];

ALTER TABLE [catalog].[BranchMenu] WITH CHECK CHECK CONSTRAINT [FK_BranchMenu_UserAccount_BlockRequestedBy];

ALTER TABLE [hr].[ShiftAssignment] WITH CHECK CHECK CONSTRAINT [FK_ShiftAssignment_UserAccount_User];

ALTER TABLE [hr].[ShiftAssignment] WITH CHECK CHECK CONSTRAINT [FK_ShiftAssignment_UserAccount_ApprovedBy];

ALTER TABLE [iam].[UserAccount] WITH CHECK CHECK CONSTRAINT [FK_UserAccount_Branch];

ALTER TABLE [inventory].[StockReceiptLine] WITH CHECK CHECK CONSTRAINT [FK_StockReceiptLine_UserAccount_ReceivedBy];

ALTER TABLE [inventory].[StockReceiptLine] WITH CHECK CHECK CONSTRAINT [FK_StockReceiptLine_Supplier];

ALTER TABLE [inventory].[StockReceiptLine] WITH CHECK CHECK CONSTRAINT [FK_StockReceiptLine_Branch];

ALTER TABLE [inventory].[StockReceiptLine] WITH CHECK CHECK CONSTRAINT [FK_StockReceiptLine_Ingredient];

ALTER TABLE [inventory].[BranchInventory] WITH CHECK CHECK CONSTRAINT [FK_BranchInventory_Ingredient];

ALTER TABLE [inventory].[BranchInventory] WITH CHECK CHECK CONSTRAINT [FK_BranchInventory_Ingredient_PrepTargetTyped];

ALTER TABLE [inventory].[BranchInventory] WITH CHECK CHECK CONSTRAINT [FK_BranchInventory_Branch];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [FK_WasteEntry_Ingredient];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [FK_WasteEntry_UserAccount_LoggedBy];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [FK_WasteEntry_UserAccount_CreatedBy];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [FK_WasteEntry_UserAccount_ResolvedBy];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [FK_WasteEntry_Branch];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [FK_WasteEntry_Product];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [FK_WasteEntry_OrderItem];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [FK_WasteEntry_OrderItem_Branch];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [FK_WasteEntry_OrderItem_ProductBranch];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [FK_WasteEntry_ShiftAssignment];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [FK_WasteEntry_ShiftAssignment_Branch];

ALTER TABLE [inventory].[InventoryTransaction] WITH CHECK CHECK CONSTRAINT [FK_InventoryTransaction_Ingredient];

ALTER TABLE [inventory].[InventoryTransaction] WITH CHECK CHECK CONSTRAINT [FK_InventoryTransaction_UserAccount_CreatedBy];

ALTER TABLE [inventory].[InventoryTransaction] WITH CHECK CHECK CONSTRAINT [FK_InventoryTransaction_Branch];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [FK_PrepBatch_UserAccount_ReviewedBy];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [FK_PrepBatch_WasteEntry_WriteOff];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [FK_PrepBatch_Ingredient_PreppedTyped];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [FK_PrepBatch_Ingredient_PreppedIngredient];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [FK_PrepBatch_Branch];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [FK_PrepBatch_UserAccount_MadeBy];

ALTER TABLE [inventory].[StockAdjustment] WITH CHECK CHECK CONSTRAINT [FK_StockAdjustment_Ingredient];

ALTER TABLE [inventory].[StockAdjustment] WITH CHECK CHECK CONSTRAINT [FK_StockAdjustment_Branch];

ALTER TABLE [inventory].[StockAdjustment] WITH CHECK CHECK CONSTRAINT [FK_StockAdjustment_UserAccount_CountedBy];

ALTER TABLE [inventory].[StockAdjustment] WITH CHECK CHECK CONSTRAINT [FK_StockAdjustment_UserAccount_AdjustedBy];

ALTER TABLE [ops].[ActivityLog] WITH CHECK CHECK CONSTRAINT [FK_ActivityLog_Branch];

ALTER TABLE [ops].[ActivityLog] WITH CHECK CHECK CONSTRAINT [FK_ActivityLog_UserAccount_PerformedBy];

ALTER TABLE [ops].[OutboxEvent] WITH CHECK CHECK CONSTRAINT [FK_OutboxEvent_Branch];

ALTER TABLE [org].[Branch] WITH CHECK CHECK CONSTRAINT [FK_Branch_UserAccount_Manager];

ALTER TABLE [payment].[CashierShift] WITH CHECK CHECK CONSTRAINT [FK_CashierShift_Branch];

ALTER TABLE [payment].[CashierShift] WITH CHECK CHECK CONSTRAINT [FK_CashierShift_UserAccount_Cashier];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [FK_Bill_CashierShift];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [FK_Bill_CashierShift_Branch];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [FK_Bill_Branch];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [FK_SalesOrder_DiningTable];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [FK_SalesOrder_DiningTable_Branch];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [FK_SalesOrder_UserAccount_CreatedBy];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [FK_SalesOrder_Branch];

ALTER TABLE [sales].[DiningTable] WITH CHECK CHECK CONSTRAINT [FK_DiningTable_Branch];

ALTER TABLE [sales].[OrderItemModifier] WITH CHECK CHECK CONSTRAINT [FK_OrderItemModifier_OrderItem];

ALTER TABLE [sales].[OrderItemModifier] WITH CHECK CHECK CONSTRAINT [FK_OrderItemModifier_ModifierOption];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [FK_OrderItem_UserAccount_Barista];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [FK_OrderItem_SalesOrder_Branch];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [FK_OrderItem_UserAccount_PreparedBy];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [FK_OrderItem_Product];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [FK_OrderItem_UserAccount_IssueReportedBy];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [FK_OrderItem_SalesOrder];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [FK_OrderItem_UserAccount_PickedUpBy];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [FK_OrderItem_Bill];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [FK_OrderItem_Bill_Branch];

ALTER TABLE [catalog].[BranchMenu] WITH CHECK CHECK CONSTRAINT [CK_BranchMenu_LocalPrice];

ALTER TABLE [catalog].[BranchMenu] WITH CHECK CHECK CONSTRAINT [CK_BranchMenu_BlockLifecycle];

ALTER TABLE [catalog].[BranchMenu] WITH CHECK CHECK CONSTRAINT [CK_BranchMenu_BlockStatus];

ALTER TABLE [catalog].[BranchMenu] WITH CHECK CHECK CONSTRAINT [CK_BranchMenu_BlockTimeOrder];

ALTER TABLE [catalog].[ModifierGroup] WITH CHECK CHECK CONSTRAINT [CK_ModifierGroup_SelectionRange];

ALTER TABLE [catalog].[Ingredient] WITH CHECK CHECK CONSTRAINT [CK_Ingredient_Type];

ALTER TABLE [catalog].[Ingredient] WITH CHECK CHECK CONSTRAINT [CK_Ingredient_ShelfLife];

ALTER TABLE [catalog].[Ingredient] WITH CHECK CHECK CONSTRAINT [CK_Ingredient_PrepYieldQty];

ALTER TABLE [catalog].[Ingredient] WITH CHECK CHECK CONSTRAINT [CK_Ingredient_PurchaseUnit];

ALTER TABLE [catalog].[Product] WITH CHECK CHECK CONSTRAINT [CK_Product_BasePrice_Value];

ALTER TABLE [catalog].[Product] WITH CHECK CHECK CONSTRAINT [CK_Product_PrepSeconds];

ALTER TABLE [hr].[ShiftAssignment] WITH CHECK CHECK CONSTRAINT [CK_ShiftAssignment_NonZeroDuration];

ALTER TABLE [hr].[ShiftAssignment] WITH CHECK CHECK CONSTRAINT [CK_ShiftAssignment_HourlyRateSnapshot];

ALTER TABLE [hr].[ShiftAssignment] WITH CHECK CHECK CONSTRAINT [CK_ShiftAssignment_AttendanceStatus];

ALTER TABLE [hr].[ShiftAssignment] WITH CHECK CHECK CONSTRAINT [CK_ShiftAssignment_CheckOutAfterIn];

ALTER TABLE [hr].[ShiftAssignment] WITH CHECK CHECK CONSTRAINT [CK_ShiftAssignment_ApprovalLifecycle];

ALTER TABLE [iam].[UserAccount] WITH CHECK CHECK CONSTRAINT [CK_UserAccount_Status];

ALTER TABLE [iam].[UserAccount] WITH CHECK CHECK CONSTRAINT [CK_UserAccount_RoleCode];

ALTER TABLE [iam].[UserAccount] WITH CHECK CHECK CONSTRAINT [CK_UserAccount_HourlyRate];

ALTER TABLE [inventory].[StockReceiptLine] WITH CHECK CHECK CONSTRAINT [CK_StockReceiptLine_Status];

ALTER TABLE [inventory].[StockReceiptLine] WITH CHECK CHECK CONSTRAINT [CK_StockReceiptLine_EnteredQuantity];

ALTER TABLE [inventory].[BranchInventory] WITH CHECK CHECK CONSTRAINT [CK_BranchInventory_Threshold];

ALTER TABLE [inventory].[BranchInventory] WITH CHECK CHECK CONSTRAINT [CK_BranchInventory_PrepTarget];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [CK_WasteEntry_Quantity_Value];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [CK_WasteEntry_UnitCost];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [CK_WasteEntry_Status];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [CK_WasteEntry_Type];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [CK_WasteEntry_CostBasis];

ALTER TABLE [inventory].[StockReceiptLine] WITH CHECK CHECK CONSTRAINT [CK_StockReceiptLine_UnitCost];

ALTER TABLE [inventory].[StockReceiptLine] WITH CHECK CHECK CONSTRAINT [CK_StockReceiptLine_BasePrecision];

ALTER TABLE [inventory].[StockReceiptLine] WITH CHECK CHECK CONSTRAINT [CK_StockReceiptLine_FactorToBaseAtEntry];

ALTER TABLE [inventory].[InventoryTransaction] WITH CHECK CHECK CONSTRAINT [CK_InventoryTransaction_ChangeQty];

ALTER TABLE [inventory].[InventoryTransaction] WITH CHECK CHECK CONSTRAINT [CK_InventoryTransaction_Reference];

ALTER TABLE [inventory].[InventoryTransaction] WITH CHECK CHECK CONSTRAINT [CK_InventoryTransaction_Type];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [CK_PrepBatch_QuantityProduced_Value];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [CK_PrepBatch_Lifecycle];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [CK_PrepBatch_Status];

ALTER TABLE [inventory].[StockAdjustment] WITH CHECK CHECK CONSTRAINT [CK_StockAdjustment_ConvertedActual];

ALTER TABLE [inventory].[StockAdjustment] WITH CHECK CHECK CONSTRAINT [CK_StockAdjustment_FactorToBaseAtCount];

ALTER TABLE [inventory].[StockAdjustment] WITH CHECK CHECK CONSTRAINT [CK_StockAdjustment_ActualBaseQty];

ALTER TABLE [inventory].[StockAdjustment] WITH CHECK CHECK CONSTRAINT [CK_StockAdjustment_CountedQuantity];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [CK_WasteEntry_Kind];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [CK_WasteEntry_EventShape];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [CK_WasteEntry_Source];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [CK_WasteEntry_ReviewLifecycle];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [CK_WasteEntry_ReviewStatus];

ALTER TABLE [inventory].[WasteEntry] WITH CHECK CHECK CONSTRAINT [CK_WasteEntry_ReviewType];

ALTER TABLE [ops].[ActivityLog] WITH CHECK CHECK CONSTRAINT [CK_ActivityLog_EntityType];

ALTER TABLE [ops].[OutboxEvent] WITH CHECK CHECK CONSTRAINT [CK_OutboxEvent_PayloadJson];

ALTER TABLE [org].[Branch] WITH CHECK CHECK CONSTRAINT [CK_Branch_PeakThreshold];

ALTER TABLE [payment].[CashierShift] WITH CHECK CHECK CONSTRAINT [CK_CashierShift_Money];

ALTER TABLE [payment].[CashierShift] WITH CHECK CHECK CONSTRAINT [CK_CashierShift_CloseState];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [CK_OrderItem_BilledAmount];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [CK_OrderItem_BillingLifecycle];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [CK_Bill_CashSettlement];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [CK_Bill_PaymentMethod];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [CK_Bill_Amounts];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [CK_Bill_SettlementAmounts];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [CK_Bill_Status];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [CK_Bill_PaidHasAmount];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [CK_Bill_PaymentLifecycle];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [CK_SalesOrder_Source];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [CK_SalesOrder_Status];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [CK_SalesOrder_TypeDiningTable];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [CK_SalesOrder_Type];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [CK_SalesOrder_SourceCreator];

ALTER TABLE [sales].[DiningTable] WITH CHECK CHECK CONSTRAINT [CK_DiningTable_Status];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [CK_OrderItem_Status];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [CK_OrderItem_ServedAfterDone];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [CK_OrderItem_PickedUpAfterDone];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [CK_OrderItem_DoneAfterStarted];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [CK_OrderItem_StatusTimestamps];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [CK_OrderItem_Quantity_Value];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [CK_OrderItem_ServedAfterPickedUp];

ALTER TABLE [sales].[OrderItem] WITH CHECK CHECK CONSTRAINT [CK_OrderItem_NonNegative];


GO


GO

/* Dữ liệu tối thiểu để dự án sinh viên có thể demo ngay trên localhost. */
INSERT org.Branch(Code,Name,Address,Phone,OpenTime,CloseTime,PeakThresholdCups,
                  HeroEyebrow,HeroTitle,HeroSubtitle,HeroImageUrl)
VALUES('CN01',N'Chi nhánh Demo',N'123 Lê Lợi, Quận 1, TP.HCM','0900000001','07:00','22:00',20,
       N'Chuỗi cà phê thủ công',N'Thực đơn của Cà Phê Chain',
       N'Khám phá menu cà phê, trà và đá xay được pha chế tươi mỗi ngày.',
       '/assets/img/login-hero.svg');

DECLARE @DemoBranchId INT=(SELECT BranchId FROM org.Branch WHERE Code='CN01');
DECLARE @DemoHash VARCHAR(255)='$2a$10$BFdZOEu0.X9/U6Yme03Z.ec6H/lsprcbJavmdUw3B4O51T82onwGa';

INSERT iam.UserAccount(Username,PasswordHash,FullName,Email,Phone,RoleCode,BranchId,Status) VALUES
('admin',@DemoHash,N'Quản trị viên','admin@cafe.local','0900000010','ADMIN',NULL,'ACTIVE'),
('manager1',@DemoHash,N'Quản lý Demo','manager@cafe.local','0900000011','BRANCH_MANAGER',@DemoBranchId,'ACTIVE'),
('cashier1',@DemoHash,N'Thu ngân Demo','cashier@cafe.local','0900000012','CASHIER',@DemoBranchId,'ACTIVE'),
('barista1',@DemoHash,N'Pha chế Demo','barista@cafe.local','0900000013','BARISTA',@DemoBranchId,'ACTIVE');

DECLARE @DemoManagerId INT=(SELECT UserId FROM iam.UserAccount WHERE Username='manager1');
UPDATE org.Branch SET ManagerUserId=@DemoManagerId WHERE BranchId=@DemoBranchId;

INSERT catalog.Category(Name,SortOrder) VALUES
(N'Cà phê',1),(N'Trà',2),(N'Đá xay',3);

INSERT catalog.Product(CategoryId,Name,BasePrice,ImageUrl,PrepSeconds) VALUES
((SELECT CategoryId FROM catalog.Category WHERE Name=N'Cà phê'),N'Cà phê sữa',29000,
 '/assets/img/login-hero.svg',420),
((SELECT CategoryId FROM catalog.Category WHERE Name=N'Cà phê'),N'Cold Brew',45000,
 '/assets/img/login-hero.svg',300),
((SELECT CategoryId FROM catalog.Category WHERE Name=N'Trà'),N'Trà đào',39000,
 '/assets/img/login-hero.svg',360);

INSERT catalog.Ingredient(Name,Unit,IngredientType,ShelfLifeMinutes) VALUES
(N'Cà phê hạt',N'g','RAW',NULL),
(N'Sữa đặc',N'ml','RAW',NULL),
(N'Đường',N'g','RAW',NULL),
(N'Đá',N'g','RAW',NULL),
(N'Trà đen',N'g','RAW',NULL),
(N'Đào ngâm',N'g','RAW',NULL);

INSERT catalog.Recipe(OwnerType,OwnerId,IngredientId,Quantity)
SELECT 'PRODUCT',p.ProductId,i.IngredientId,v.Quantity
FROM (VALUES
 (N'Cà phê sữa',N'Cà phê hạt',CONVERT(DECIMAL(12,3),18)),
 (N'Cà phê sữa',N'Sữa đặc',CONVERT(DECIMAL(12,3),30)),
 (N'Cà phê sữa',N'Đá',CONVERT(DECIMAL(12,3),150)),
 (N'Cold Brew',N'Cà phê hạt',CONVERT(DECIMAL(12,3),20)),
 (N'Cold Brew',N'Đá',CONVERT(DECIMAL(12,3),150)),
 (N'Trà đào',N'Trà đen',CONVERT(DECIMAL(12,3),5)),
 (N'Trà đào',N'Đào ngâm',CONVERT(DECIMAL(12,3),50)),
 (N'Trà đào',N'Đường',CONVERT(DECIMAL(12,3),15)),
 (N'Trà đào',N'Đá',CONVERT(DECIMAL(12,3),150))
) v(ProductName,IngredientName,Quantity)
JOIN catalog.Product p ON p.Name=v.ProductName
JOIN catalog.Ingredient i ON i.Name=v.IngredientName;

INSERT catalog.BranchMenu(BranchId,ProductId,IsListed,IsTemporarilyUnavailable)
SELECT @DemoBranchId,ProductId,1,0 FROM catalog.Product;

INSERT inventory.BranchInventory(BranchId,IngredientId,QuantityOnHand,MinThreshold)
SELECT @DemoBranchId,IngredientId,
       CASE WHEN Unit=N'ml' THEN CONVERT(DECIMAL(12,3),10000)
            ELSE CONVERT(DECIMAL(12,3),15000) END,
       CONVERT(DECIMAL(12,3),1000)
FROM catalog.Ingredient;

INSERT inventory.InventoryTransaction(BranchId,IngredientId,ChangeQty,TxnType,CreatedBy)
SELECT BranchId,IngredientId,QuantityOnHand,'ADJUST',@DemoManagerId
FROM inventory.BranchInventory WHERE BranchId=@DemoBranchId;

INSERT sales.DiningTable(BranchId,TableNumber,QrCode,Status) VALUES
(@DemoBranchId,N'Bàn 01','DEMO-CN01-T01','EMPTY'),
(@DemoBranchId,N'Bàn 02','DEMO-CN01-T02','EMPTY'),
(@DemoBranchId,N'Bàn 03','DEMO-CN01-T03','EMPTY'),
(@DemoBranchId,N'Bàn 04','DEMO-CN01-T04','EMPTY'),
(@DemoBranchId,N'Bàn 05','DEMO-CN01-T05','EMPTY');
GO
