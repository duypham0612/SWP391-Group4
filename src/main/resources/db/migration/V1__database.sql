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
CREATE TABLE [catalog].[PrepRecipe] (
    [PrepRecipeId]        INT             IDENTITY (1, 1) NOT NULL,
    [PreppedIngredientId] INT             NOT NULL,
    [YieldQty]            DECIMAL (12, 3) NOT NULL,
    [CreatedAt]           DATETIME2 (7)   NOT NULL,
    [UpdatedAt]           DATETIME2 (7)   NOT NULL,
    [PreppedTypeGuard]    AS              (CONVERT (VARCHAR (10), 'PREPPED')) PERSISTED,
    CONSTRAINT [PK_PrepRecipe] PRIMARY KEY CLUSTERED ([PrepRecipeId] ASC),
    CONSTRAINT [UQ_PrepRecipe_PreppedIngredient] UNIQUE NONCLUSTERED ([PreppedIngredientId] ASC)
);


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
    [BranchId]                 INT             NOT NULL,
    [ProductId]                INT             NOT NULL,
    [IsListed]                 BIT             NOT NULL,
    [LocalPrice]               DECIMAL (12, 2) NULL,
    [IsTemporarilyUnavailable] BIT             NOT NULL,
    [BackInEta]                DATETIME2 (7)   NULL,
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
CREATE TABLE [catalog].[HomeSetting] (
    [HomeSettingId] INT            NOT NULL,
    [HeroEyebrow]   NVARCHAR (150) NULL,
    [HeroTitle]     NVARCHAR (200) NULL,
    [HeroSubtitle]  NVARCHAR (500) NULL,
    [HeroImageUrl]  VARCHAR (500)  NULL,
    [UpdatedAt]     DATETIME2 (7)  NOT NULL,
    CONSTRAINT [PK_HomeSetting] PRIMARY KEY CLUSTERED ([HomeSettingId] ASC)
);


GO


GO
CREATE TABLE [catalog].[PrepRecipeIngredient] (
    [PrepRecipeIngredientId] INT             IDENTITY (1, 1) NOT NULL,
    [PrepRecipeId]           INT             NOT NULL,
    [RawIngredientId]        INT             NOT NULL,
    [Quantity]               DECIMAL (12, 3) NOT NULL,
    [RawTypeGuard]           AS              (CONVERT (VARCHAR (10), 'RAW')) PERSISTED,
    CONSTRAINT [PK_PrepRecipeIngredient] PRIMARY KEY CLUSTERED ([PrepRecipeIngredientId] ASC),
    CONSTRAINT [UQ_PrepRecipeIngredient_RecipeRawIngredient] UNIQUE NONCLUSTERED ([PrepRecipeId] ASC, [RawIngredientId] ASC)
);


GO


GO
CREATE TABLE [catalog].[ModifierIngredientImpact] (
    [ModifierIngredientImpactId] INT             IDENTITY (1, 1) NOT NULL,
    [ModifierOptionId]           INT             NOT NULL,
    [IngredientId]               INT             NOT NULL,
    [QtyDelta]                   DECIMAL (12, 3) NOT NULL,
    CONSTRAINT [PK_ModifierIngredientImpact] PRIMARY KEY CLUSTERED ([ModifierIngredientImpactId] ASC),
    CONSTRAINT [UQ_ModifierIngredientImpact_OptionIngredient] UNIQUE NONCLUSTERED ([ModifierOptionId] ASC, [IngredientId] ASC)
);


GO


GO
CREATE TABLE [catalog].[ModifierGroup] (
    [ModifierGroupId] INT           IDENTITY (1, 1) NOT NULL,
    [Name]            NVARCHAR (80) NOT NULL,
    [IsRequired]      BIT           NOT NULL,
    [MinSelect]       INT           NOT NULL,
    [MaxSelect]       INT           NOT NULL,
    [NameKey]         AS            ((upper(ltrim(rtrim([Name])))) COLLATE Latin1_General_100_CI_AI) PERSISTED,
    CONSTRAINT [PK_ModifierGroup] PRIMARY KEY CLUSTERED ([ModifierGroupId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_ModifierGroup_NameKey]
    ON [catalog].[ModifierGroup]([NameKey] ASC);


GO


GO
CREATE TABLE [catalog].[Ingredient] (
    [IngredientId]     INT            IDENTITY (1, 1) NOT NULL,
    [Name]             NVARCHAR (120) NOT NULL,
    [Unit]             NVARCHAR (20)  NOT NULL,
    [IngredientType]   VARCHAR (10)   NOT NULL,
    [ShelfLifeMinutes] INT            NULL,
    [IsActive]         BIT            NOT NULL,
    [NameKey]          AS             ((upper(ltrim(rtrim([Name])))) COLLATE Latin1_General_100_CI_AI) PERSISTED,
    [UnitKey]          AS             ((upper(ltrim(rtrim([Unit])))) COLLATE Latin1_General_100_CI_AI) PERSISTED,
    CONSTRAINT [PK_Ingredient] PRIMARY KEY CLUSTERED ([IngredientId] ASC),
    CONSTRAINT [UQ_Ingredient_IdType] UNIQUE NONCLUSTERED ([IngredientId] ASC, [IngredientType] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_Ingredient_NameUnitKey]
    ON [catalog].[Ingredient]([NameKey] ASC, [UnitKey] ASC);


GO


GO
CREATE TABLE [catalog].[ProductModifierGroup] (
    [ProductId]       INT NOT NULL,
    [ModifierGroupId] INT NOT NULL,
    CONSTRAINT [PK_ProductModifierGroup] PRIMARY KEY CLUSTERED ([ProductId] ASC, [ModifierGroupId] ASC)
);


GO


GO
CREATE TABLE [catalog].[ProductRecipe] (
    [ProductRecipeId] INT             IDENTITY (1, 1) NOT NULL,
    [ProductId]       INT             NOT NULL,
    [IngredientId]    INT             NOT NULL,
    [Quantity]        DECIMAL (12, 3) NOT NULL,
    CONSTRAINT [PK_ProductRecipe] PRIMARY KEY CLUSTERED ([ProductRecipeId] ASC),
    CONSTRAINT [UQ_ProductRecipe_ProductIngredient] UNIQUE NONCLUSTERED ([ProductId] ASC, [IngredientId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_ProductRecipe_Ingredient]
    ON [catalog].[ProductRecipe]([IngredientId] ASC)
    INCLUDE([ProductId], [Quantity]);


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
CREATE TABLE [catalog].[MenuBlockRequest] (
    [MenuBlockRequestId] INT            IDENTITY (1, 1) NOT NULL,
    [BranchId]           INT            NOT NULL,
    [ProductId]          INT            NOT NULL,
    [Reason]             VARCHAR (20)   NOT NULL,
    [Note]               NVARCHAR (255) NULL,
    [BackInEta]          DATETIME2 (7)  NULL,
    [RequestedBy]        INT            NOT NULL,
    [RequestedAt]        DATETIME2 (7)  NOT NULL,
    [ReopenRequestedAt]  DATETIME2 (7)  NULL,
    [Status]             VARCHAR (10)   NOT NULL,
    [ReviewedBy]         INT            NULL,
    [ReviewedAt]         DATETIME2 (7)  NULL,
    [ReviewNote]         NVARCHAR (255) NULL,
    [ClosedAt]           DATETIME2 (7)  NULL,
    CONSTRAINT [PK_MenuBlockRequest] PRIMARY KEY CLUSTERED ([MenuBlockRequestId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_MenuBlockRequest_Open]
    ON [catalog].[MenuBlockRequest]([BranchId] ASC, [ProductId] ASC) WHERE ([ClosedAt] IS NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_MenuBlockRequest_Queue]
    ON [catalog].[MenuBlockRequest]([BranchId] ASC, [ClosedAt] ASC, [BackInEta] ASC);


GO


GO
CREATE TABLE [catalog].[IngredientUnitConversion] (
    [IngredientUnitConversionId] INT             IDENTITY (1, 1) NOT NULL,
    [IngredientId]               INT             NOT NULL,
    [UnitName]                   NVARCHAR (20)   NOT NULL,
    [UnitNameKey]                AS              ((upper(ltrim(rtrim([UnitName])))) COLLATE Latin1_General_100_CI_AI) PERSISTED,
    [FactorToBase]               DECIMAL (18, 6) NOT NULL,
    [IsBaseUnit]                 BIT             NOT NULL,
    [IsActive]                   BIT             NOT NULL,
    [CreatedAt]                  DATETIME2 (7)   NOT NULL,
    [UpdatedAt]                  DATETIME2 (7)   NOT NULL,
    [UpdatedBy]                  INT             NULL,
    CONSTRAINT [PK_IngredientUnitConversion] PRIMARY KEY CLUSTERED ([IngredientUnitConversionId] ASC),
    CONSTRAINT [UQ_IngredientUnitConversion_IdIngredient] UNIQUE NONCLUSTERED ([IngredientUnitConversionId] ASC, [IngredientId] ASC),
    CONSTRAINT [UQ_IngredientUnitConversion_IngredientUnit] UNIQUE NONCLUSTERED ([IngredientId] ASC, [UnitNameKey] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_IngredientUnitConversion_OneBase]
    ON [catalog].[IngredientUnitConversion]([IngredientId] ASC) WHERE ([IsBaseUnit]=(1));


GO


GO
CREATE TABLE [hr].[ShiftAssignment] (
    [ShiftAssignmentId] INT  IDENTITY (1, 1) NOT NULL,
    [ShiftTemplateId]   INT  NOT NULL,
    [UserId]            INT  NOT NULL,
    [WorkDate]          DATE NOT NULL,
    [BranchId]          INT  NOT NULL,
    CONSTRAINT [PK_ShiftAssignment] PRIMARY KEY CLUSTERED ([ShiftAssignmentId] ASC),
    CONSTRAINT [UQ_ShiftAssignment_IdBranch] UNIQUE NONCLUSTERED ([ShiftAssignmentId] ASC, [BranchId] ASC),
    CONSTRAINT [UQ_ShiftAssignment_TemplateUserDate] UNIQUE NONCLUSTERED ([ShiftTemplateId] ASC, [UserId] ASC, [WorkDate] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_ShiftAssignment_BranchTemplate]
    ON [hr].[ShiftAssignment]([BranchId] ASC, [ShiftTemplateId] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_ShiftAssignment_UserDate]
    ON [hr].[ShiftAssignment]([UserId] ASC, [WorkDate] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_ShiftAssignment_TemplateBranch]
    ON [hr].[ShiftAssignment]([ShiftTemplateId] ASC, [BranchId] ASC);


GO


GO
CREATE TABLE [hr].[ShiftTemplate] (
    [ShiftTemplateId] INT           IDENTITY (1, 1) NOT NULL,
    [BranchId]        INT           NOT NULL,
    [Name]            NVARCHAR (60) NOT NULL,
    [StartTime]       TIME (7)      NOT NULL,
    [EndTime]         TIME (7)      NOT NULL,
    [NameKey]         AS            ((upper(ltrim(rtrim([Name])))) COLLATE Latin1_General_100_CI_AI) PERSISTED,
    CONSTRAINT [PK_ShiftTemplate] PRIMARY KEY CLUSTERED ([ShiftTemplateId] ASC),
    CONSTRAINT [UQ_ShiftTemplate_IdBranch] UNIQUE NONCLUSTERED ([ShiftTemplateId] ASC, [BranchId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_ShiftTemplate_Branch]
    ON [hr].[ShiftTemplate]([BranchId] ASC);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_ShiftTemplate_BranchNameKey]
    ON [hr].[ShiftTemplate]([BranchId] ASC, [NameKey] ASC);


GO


GO
CREATE TABLE [hr].[Attendance] (
    [AttendanceId]      INT           IDENTITY (1, 1) NOT NULL,
    [ShiftAssignmentId] INT           NOT NULL,
    [CheckInAt]         DATETIME2 (7) NULL,
    [CheckOutAt]        DATETIME2 (7) NULL,
    [Status]            VARCHAR (10)  NOT NULL,
    [ApprovedBy]        INT           NULL,
    [ApprovedAt]        DATETIME2 (7) NULL,
    CONSTRAINT [PK_Attendance] PRIMARY KEY CLUSTERED ([AttendanceId] ASC),
    CONSTRAINT [UQ_Attendance_ShiftAssignment] UNIQUE NONCLUSTERED ([ShiftAssignmentId] ASC)
);


GO


GO
CREATE TABLE [hr].[Payroll] (
    [PayrollId]    INT             IDENTITY (1, 1) NOT NULL,
    [BranchId]     INT             NOT NULL,
    [UserId]       INT             NOT NULL,
    [PayrollMonth] DATE            NOT NULL,
    [WorkedHours]  DECIMAL (10, 2) NOT NULL,
    [HourlyRate]   DECIMAL (12, 2) NOT NULL,
    [UpdatedBy]    INT             NULL,
    [UpdatedAt]    DATETIME2 (7)   NOT NULL,
    CONSTRAINT [PK_Payroll] PRIMARY KEY CLUSTERED ([PayrollId] ASC),
    CONSTRAINT [UQ_Payroll_BranchUserMonth] UNIQUE NONCLUSTERED ([BranchId] ASC, [UserId] ASC, [PayrollMonth] ASC)
);


GO


GO
CREATE TABLE [iam].[Role] (
    [RoleId] INT           IDENTITY (1, 1) NOT NULL,
    [Code]   VARCHAR (30)  NOT NULL,
    [Name]   NVARCHAR (80) NOT NULL,
    CONSTRAINT [PK_Role] PRIMARY KEY CLUSTERED ([RoleId] ASC),
    CONSTRAINT [UQ_Role_Code] UNIQUE NONCLUSTERED ([Code] ASC)
);


GO


GO
CREATE TABLE [iam].[UserAccount] (
    [UserId]       INT            IDENTITY (1, 1) NOT NULL,
    [Username]     VARCHAR (60)   NOT NULL,
    [PasswordHash] VARCHAR (255)  NOT NULL,
    [FullName]     NVARCHAR (120) NOT NULL,
    [Email]        VARCHAR (120)  NULL,
    [Phone]        VARCHAR (20)   NULL,
    [RoleId]       INT            NOT NULL,
    [BranchId]     INT            NULL,
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
CREATE TABLE [inventory].[StockReceipt] (
    [StockReceiptId] INT             IDENTITY (1, 1) NOT NULL,
    [BranchId]       INT             NOT NULL,
    [SupplierId]     INT             NULL,
    [ReceivedBy]     INT             NOT NULL,
    [DocumentDate]   DATE            NOT NULL,
    [CreatedAt]      DATETIME2 (7)   NOT NULL,
    [Status]         VARCHAR (12)    NOT NULL,
    [TotalCost]      DECIMAL (14, 2) NOT NULL,
    [Note]           NVARCHAR (255)  NULL,
    CONSTRAINT [PK_StockReceipt] PRIMARY KEY CLUSTERED ([StockReceiptId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_StockReceipt_BranchStatus]
    ON [inventory].[StockReceipt]([BranchId] ASC, [Status] ASC, [DocumentDate] DESC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_StockReceipt_Supplier]
    ON [inventory].[StockReceipt]([SupplierId] ASC) WHERE ([SupplierId] IS NOT NULL);


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
CREATE TABLE [inventory].[WasteEventAudit] (
    [WasteEventAuditId] BIGINT          IDENTITY (1, 1) NOT NULL,
    [WasteEventItemId]  INT             NULL,
    [WasteEventId]      BIGINT          NULL,
    [ActionType]        VARCHAR (20)    NOT NULL,
    [BeforeValue]       NVARCHAR (1000) NULL,
    [AfterValue]        NVARCHAR (1000) NULL,
    [Reason]            NVARCHAR (255)  NULL,
    [PerformedBy]       INT             NOT NULL,
    [PerformedAt]       DATETIME2 (7)   NOT NULL,
    CONSTRAINT [PK_WasteEventAudit] PRIMARY KEY CLUSTERED ([WasteEventAuditId] ASC)
);


GO


GO
CREATE TABLE [inventory].[WasteEventItem] (
    [WasteEventItemId] INT             IDENTITY (1, 1) NOT NULL,
    [BranchId]         INT             NOT NULL,
    [IngredientId]     INT             NOT NULL,
    [Quantity]         DECIMAL (12, 3) NOT NULL,
    [WasteType]        VARCHAR (12)    NOT NULL,
    [Reason]           NVARCHAR (255)  NULL,
    [LoggedBy]         INT             NOT NULL,
    [LoggedAt]         DATETIME2 (7)   NOT NULL,
    [Status]           VARCHAR (10)    NOT NULL,
    [VoidedAt]         DATETIME2 (7)   NULL,
    [WasteEventId]     BIGINT          NOT NULL,
    [UnitCostAtLog]    DECIMAL (12, 2) NULL,
    [CostBasis]        VARCHAR (20)    NULL,
    CONSTRAINT [PK_WasteEventItem] PRIMARY KEY CLUSTERED ([WasteEventItemId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEventItem_Event]
    ON [inventory].[WasteEventItem]([WasteEventId] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEventItem_Ingredient]
    ON [inventory].[WasteEventItem]([IngredientId] ASC, [LoggedAt] DESC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEventItem_WasteEventBranch]
    ON [inventory].[WasteEventItem]([WasteEventId] ASC, [BranchId] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEventItem_BranchLogged]
    ON [inventory].[WasteEventItem]([BranchId] ASC, [LoggedAt] DESC);


GO


GO
CREATE TABLE [inventory].[StockCount] (
    [StockCountId] INT            IDENTITY (1, 1) NOT NULL,
    [BranchId]     INT            NOT NULL,
    [CountedBy]    INT            NOT NULL,
    [CountedAt]    DATETIME2 (7)  NOT NULL,
    [Note]         NVARCHAR (255) NULL,
    CONSTRAINT [PK_StockCount] PRIMARY KEY CLUSTERED ([StockCountId] ASC),
    CONSTRAINT [UQ_StockCount_IdBranch] UNIQUE NONCLUSTERED ([StockCountId] ASC, [BranchId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_StockCount_BranchCounted]
    ON [inventory].[StockCount]([BranchId] ASC, [CountedAt] DESC)
    INCLUDE([CountedBy]);


GO


GO
CREATE TABLE [inventory].[StockReceiptDetail] (
    [StockReceiptDetailId]       INT             IDENTITY (1, 1) NOT NULL,
    [StockReceiptId]             INT             NOT NULL,
    [IngredientId]               INT             NOT NULL,
    [UnitCost]                   DECIMAL (12, 2) NOT NULL,
    [IngredientUnitConversionId] INT             NOT NULL,
    [EnteredQuantity]            DECIMAL (18, 6) NOT NULL,
    [UnitNameAtEntry]            NVARCHAR (20)   NOT NULL,
    [FactorToBaseAtEntry]        DECIMAL (18, 6) NOT NULL,
    [BaseQuantity]               AS              (CONVERT (DECIMAL (12, 3), [EnteredQuantity] * [FactorToBaseAtEntry])) PERSISTED,
    CONSTRAINT [PK_StockReceiptDetail] PRIMARY KEY CLUSTERED ([StockReceiptDetailId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_StockReceiptDetail_ReceiptIngredient]
    ON [inventory].[StockReceiptDetail]([StockReceiptId] ASC, [IngredientId] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_StockReceiptDetail_Ingredient]
    ON [inventory].[StockReceiptDetail]([IngredientId] ASC);


GO


GO
CREATE TABLE [inventory].[InventoryTransaction] (
    [InventoryTransactionId] BIGINT          IDENTITY (1, 1) NOT NULL,
    [BranchId]               INT             NOT NULL,
    [IngredientId]           INT             NOT NULL,
    [ChangeQty]              DECIMAL (12, 3) NOT NULL,
    [TxnType]                VARCHAR (12)    NOT NULL,
    [ReferenceType]          VARCHAR (40)    NULL,
    [ReferenceId]            BIGINT          NULL,
    [CreatedBy]              INT             NULL,
    [CreatedAt]              DATETIME2 (7)   NOT NULL,
    CONSTRAINT [PK_InventoryTransaction] PRIMARY KEY CLUSTERED ([InventoryTransactionId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_InventoryTransaction_ReceiptReference]
    ON [inventory].[InventoryTransaction]([BranchId] ASC, [IngredientId] ASC, [ReferenceId] ASC) WHERE ([TxnType]='RECEIPT' AND [ReferenceType]='STOCK_RECEIPT' AND [ReferenceId] IS NOT NULL);


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
    [WriteOffWasteEventItemId] INT             NULL,
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
    [StockCountId]               INT             NULL,
    [IngredientId]               INT             NOT NULL,
    [SystemBaseQty]              DECIMAL (12, 3) NOT NULL,
    [ActualBaseQty]              DECIMAL (12, 3) NOT NULL,
    [Reason]                     NVARCHAR (255)  NULL,
    [AdjustedBy]                 INT             NOT NULL,
    [AdjustedAt]                 DATETIME2 (7)   NOT NULL,
    [IngredientUnitConversionId] INT             NOT NULL,
    [CountedQuantity]            DECIMAL (18, 6) NOT NULL,
    [UnitNameAtCount]            NVARCHAR (20)   NOT NULL,
    [FactorToBaseAtCount]        DECIMAL (18, 6) NOT NULL,
    [DiffQty]                    AS              ([ActualBaseQty] - [SystemBaseQty]) PERSISTED,
    CONSTRAINT [PK_StockAdjustment] PRIMARY KEY CLUSTERED ([StockAdjustmentId] ASC)
);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_StockAdjustment_CountIngredient]
    ON [inventory].[StockAdjustment]([StockCountId] ASC, [IngredientId] ASC) WHERE ([StockCountId] IS NOT NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_StockAdjustment_BranchIngredient]
    ON [inventory].[StockAdjustment]([BranchId] ASC, [IngredientId] ASC, [AdjustedAt] DESC);


GO


GO
CREATE TABLE [inventory].[WasteEvent] (
    [WasteEventId]      BIGINT         IDENTITY (1, 1) NOT NULL,
    [BranchId]          INT            NOT NULL,
    [EventKind]         VARCHAR (20)   NOT NULL,
    [Source]            VARCHAR (12)   NOT NULL,
    [ProductId]         INT            NULL,
    [OrderItemId]       INT            NULL,
    [CupQuantity]       INT            NULL,
    [CauseCode]         VARCHAR (24)   NOT NULL,
    [CauseDetail]       NVARCHAR (255) NULL,
    [ShiftAssignmentId] INT            NULL,
    [CreatedBy]         INT            NOT NULL,
    [CreatedAt]         DATETIME2 (7)  NOT NULL,
    [ClientRequestId]   VARCHAR (64)   NULL,
    CONSTRAINT [PK_WasteEvent] PRIMARY KEY CLUSTERED ([WasteEventId] ASC),
    CONSTRAINT [UQ_WasteEvent_IdBranch] UNIQUE NONCLUSTERED ([WasteEventId] ASC, [BranchId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEvent_OrderItem]
    ON [inventory].[WasteEvent]([OrderItemId] ASC, [CreatedAt] DESC) WHERE ([OrderItemId] IS NOT NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEvent_OrderItemBranch]
    ON [inventory].[WasteEvent]([OrderItemId] ASC, [BranchId] ASC) WHERE ([OrderItemId] IS NOT NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEvent_ShiftAssignmentBranch]
    ON [inventory].[WasteEvent]([ShiftAssignmentId] ASC, [BranchId] ASC) WHERE ([ShiftAssignmentId] IS NOT NULL);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEvent_BranchActorTime]
    ON [inventory].[WasteEvent]([BranchId] ASC, [CreatedBy] ASC, [CreatedAt] ASC)
    INCLUDE([EventKind]);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_WasteEvent_ClientRequest]
    ON [inventory].[WasteEvent]([BranchId] ASC, [ClientRequestId] ASC) WHERE ([ClientRequestId] IS NOT NULL);


GO


GO
CREATE TABLE [inventory].[WasteEventReview] (
    [WasteEventReviewId] BIGINT          IDENTITY (1, 1) NOT NULL,
    [WasteEventId]       BIGINT          NOT NULL,
    [IngredientId]       INT             NOT NULL,
    [ReviewType]         VARCHAR (20)    NOT NULL,
    [QtyBefore]          DECIMAL (12, 3) NOT NULL,
    [QtyAfter]           DECIMAL (12, 3) NOT NULL,
    [Status]             VARCHAR (16)    NOT NULL,
    [Note]               NVARCHAR (255)  NULL,
    [CreatedAt]          DATETIME2 (7)   NOT NULL,
    [ResolvedBy]         INT             NULL,
    [ResolvedAt]         DATETIME2 (7)   NULL,
    [ResolutionNote]     NVARCHAR (255)  NULL,
    CONSTRAINT [PK_WasteEventReview] PRIMARY KEY CLUSTERED ([WasteEventReviewId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_WasteEventReview_Open]
    ON [inventory].[WasteEventReview]([Status] ASC, [CreatedAt] DESC)
    INCLUDE([WasteEventId], [IngredientId], [ReviewType]);


GO


GO
CREATE TABLE [ops].[LegacySchemaVersion] (
    [VersionCode] VARCHAR (64)   NOT NULL,
    [AppliedAt]   DATETIME2 (7)  NOT NULL,
    [Description] NVARCHAR (255) NOT NULL,
    CONSTRAINT [PK_LegacySchemaVersion] PRIMARY KEY CLUSTERED ([VersionCode] ASC)
);


GO


GO
CREATE TABLE [ops].[MigrationBackfillReport] (
    [MigrationBackfillReportId] BIGINT          IDENTITY (1, 1) NOT NULL,
    [VersionCode]               VARCHAR (64)    NOT NULL,
    [EntityName]                NVARCHAR (128)  NOT NULL,
    [EntityId]                  BIGINT          NOT NULL,
    [FieldName]                 NVARCHAR (128)  NOT NULL,
    [PreviousValue]             NVARCHAR (1000) NULL,
    [NewValue]                  NVARCHAR (1000) NULL,
    [Reason]                    NVARCHAR (500)  NOT NULL,
    [RecordedAt]                DATETIME2 (7)   NOT NULL,
    CONSTRAINT [PK_MigrationBackfillReport] PRIMARY KEY CLUSTERED ([MigrationBackfillReportId] ASC),
    CONSTRAINT [UQ_MigrationBackfillReport_VersionEntityField] UNIQUE NONCLUSTERED ([VersionCode] ASC, [EntityName] ASC, [EntityId] ASC, [FieldName] ASC)
);


GO


GO
CREATE TABLE [ops].[OrderItemActionLog] (
    [OrderItemActionLogId] BIGINT         IDENTITY (1, 1) NOT NULL,
    [OrderItemId]          INT            NOT NULL,
    [BranchId]             INT            NOT NULL,
    [ActionType]           VARCHAR (24)   NOT NULL,
    [FromStatus]           VARCHAR (16)   NULL,
    [ToStatus]             VARCHAR (16)   NULL,
    [Reason]               NVARCHAR (255) NULL,
    [PerformedBy]          INT            NULL,
    [CreatedAt]            DATETIME2 (7)  NOT NULL,
    CONSTRAINT [PK_OrderItemActionLog] PRIMARY KEY CLUSTERED ([OrderItemActionLogId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_OrderItemActionLog_OrderItemBranch]
    ON [ops].[OrderItemActionLog]([OrderItemId] ASC, [BranchId] ASC);


GO


GO
CREATE NONCLUSTERED INDEX [IX_OrderItemActionLog_Item]
    ON [ops].[OrderItemActionLog]([OrderItemId] ASC, [CreatedAt] DESC);


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
CREATE TABLE [ops].[MenuBlockTimestampArchive] (
    [MenuBlockRequestId] INT           NOT NULL,
    [RequestedAt]        DATETIME2 (7) NOT NULL,
    [ReopenRequestedAt]  DATETIME2 (7) NULL,
    [ReviewedAt]         DATETIME2 (7) NULL,
    [ClosedAt]           DATETIME2 (7) NULL,
    [ReviewStatus]       VARCHAR (12)  NOT NULL,
    [ArchivedAt]         DATETIME2 (7) NOT NULL,
    CONSTRAINT [PK_MenuBlockTimestampArchive] PRIMARY KEY CLUSTERED ([MenuBlockRequestId] ASC)
);


GO


GO
CREATE TABLE [ops].[AttendanceDuplicateArchive] (
    [AttendanceDuplicateArchiveId] INT            IDENTITY (1, 1) NOT NULL,
    [SourceAttendanceId]           INT            NOT NULL,
    [ShiftAssignmentId]            INT            NOT NULL,
    [CheckInAt]                    DATETIME2 (7)  NULL,
    [CheckOutAt]                   DATETIME2 (7)  NULL,
    [Status]                       VARCHAR (10)   NOT NULL,
    [ApprovedBy]                   INT            NULL,
    [ArchivedAt]                   DATETIME2 (7)  NOT NULL,
    [ArchiveReason]                NVARCHAR (300) NOT NULL,
    CONSTRAINT [PK_AttendanceDuplicateArchive] PRIMARY KEY CLUSTERED ([AttendanceDuplicateArchiveId] ASC)
);


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
CREATE TABLE [payment].[Voucher] (
    [VoucherId]      INT             IDENTITY (1, 1) NOT NULL,
    [Code]           VARCHAR (40)    NOT NULL,
    [DiscountType]   VARCHAR (8)     NOT NULL,
    [DiscountValue]  DECIMAL (12, 2) NOT NULL,
    [MinOrderAmount] DECIMAL (12, 2) NOT NULL,
    [Scope]          VARCHAR (8)     NOT NULL,
    [BranchId]       INT             NULL,
    [UsageLimit]     INT             NULL,
    [UsedCount]      INT             NOT NULL,
    [IsActive]       BIT             NOT NULL,
    [StartAtUtc]     DATETIME2 (7)   NULL,
    [EndAtUtc]       DATETIME2 (7)   NULL,
    CONSTRAINT [PK_Voucher] PRIMARY KEY CLUSTERED ([VoucherId] ASC),
    CONSTRAINT [UQ_Voucher_Code] UNIQUE NONCLUSTERED ([Code] ASC)
);


GO


GO
CREATE TABLE [payment].[BillItem] (
    [BillItemId]  INT             IDENTITY (1, 1) NOT NULL,
    [BillId]      INT             NOT NULL,
    [OrderItemId] INT             NOT NULL,
    [Amount]      DECIMAL (12, 2) NOT NULL,
    [BranchId]    INT             NOT NULL,
    CONSTRAINT [PK_BillItem] PRIMARY KEY CLUSTERED ([BillItemId] ASC),
    CONSTRAINT [UQ_BillItem_OrderItem] UNIQUE NONCLUSTERED ([OrderItemId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_BillItem_BillBranch]
    ON [payment].[BillItem]([BillId] ASC, [BranchId] ASC)
    INCLUDE([OrderItemId], [Amount]);


GO


GO
CREATE NONCLUSTERED INDEX [IX_BillItem_BranchBill]
    ON [payment].[BillItem]([BranchId] ASC, [BillId] ASC);


GO


GO
CREATE TABLE [payment].[Bill] (
    [BillId]             INT             IDENTITY (1, 1) NOT NULL,
    [BranchId]           INT             NOT NULL,
    [TableSessionId]     INT             NULL,
    [CashierShiftId]     INT             NULL,
    [Subtotal]           DECIMAL (14, 2) NOT NULL,
    [VatAmount]          DECIMAL (14, 2) NOT NULL,
    [DiscountAmount]     DECIMAL (14, 2) NOT NULL,
    [TotalAmount]        DECIMAL (14, 2) NOT NULL,
    [RoundingAdjustment] DECIMAL (14, 2) NOT NULL,
    [PaidAmount]         DECIMAL (14, 2) NULL,
    [CashTendered]       DECIMAL (14, 2) NULL,
    [CashChange]         DECIMAL (14, 2) NULL,
    [VoucherId]          INT             NULL,
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
CREATE NONCLUSTERED INDEX [IX_Bill_Session]
    ON [payment].[Bill]([TableSessionId] ASC);


GO


GO
CREATE TABLE [sales].[SalesOrder] (
    [OrderId]        INT           IDENTITY (1, 1) NOT NULL,
    [BranchId]       INT           NOT NULL,
    [TableSessionId] INT           NULL,
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
CREATE NONCLUSTERED INDEX [IX_SalesOrder_Session]
    ON [sales].[SalesOrder]([TableSessionId] ASC);


GO


GO
CREATE TABLE [sales].[PickupSequence] (
    [BranchId]     INT  NOT NULL,
    [BusinessDate] DATE NOT NULL,
    [NextValue]    INT  NOT NULL,
    CONSTRAINT [PK_PickupSequence] PRIMARY KEY CLUSTERED ([BranchId] ASC, [BusinessDate] ASC)
);


GO


GO
CREATE TABLE [sales].[TableSession] (
    [TableSessionId] INT           IDENTITY (1, 1) NOT NULL,
    [BranchId]       INT           NOT NULL,
    [DiningTableId]  INT           NOT NULL,
    [OpenedBy]       INT           NULL,
    [OpenedAt]       DATETIME2 (7) NOT NULL,
    [ClosedAt]       DATETIME2 (7) NULL,
    [Status]         VARCHAR (10)  NOT NULL,
    CONSTRAINT [PK_TableSession] PRIMARY KEY CLUSTERED ([TableSessionId] ASC),
    CONSTRAINT [UQ_TableSession_IdBranch] UNIQUE NONCLUSTERED ([TableSessionId] ASC, [BranchId] ASC)
);


GO


GO
CREATE NONCLUSTERED INDEX [IX_TableSession_BranchStatus]
    ON [sales].[TableSession]([BranchId] ASC, [Status] ASC)
    INCLUDE([DiningTableId], [OpenedAt]);


GO


GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_TableSession_OneOpenPerTable]
    ON [sales].[TableSession]([DiningTableId] ASC) WHERE ([Status]='OPEN');


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
ALTER TABLE [catalog].[PrepRecipe]
    ADD CONSTRAINT [DF_PrepRecipe_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


GO


GO
ALTER TABLE [catalog].[PrepRecipe]
    ADD CONSTRAINT [DF_PrepRecipe_UpdatedAt] DEFAULT (sysutcdatetime()) FOR [UpdatedAt];


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
ALTER TABLE [catalog].[HomeSetting]
    ADD CONSTRAINT [DF_HomeSetting_UpdatedAt] DEFAULT (sysutcdatetime()) FOR [UpdatedAt];


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
ALTER TABLE [catalog].[MenuBlockRequest]
    ADD CONSTRAINT [DF_MenuBlockRequest_Status] DEFAULT ('PENDING') FOR [Status];


GO


GO
ALTER TABLE [catalog].[MenuBlockRequest]
    ADD CONSTRAINT [DF_MenuBlockRequest_RequestedAt] DEFAULT (sysutcdatetime()) FOR [RequestedAt];


GO


GO
ALTER TABLE [catalog].[IngredientUnitConversion]
    ADD CONSTRAINT [DF_IngredientUnitConversion_IsActive] DEFAULT ((1)) FOR [IsActive];


GO


GO
ALTER TABLE [catalog].[IngredientUnitConversion]
    ADD CONSTRAINT [DF_IngredientUnitConversion_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


GO


GO
ALTER TABLE [catalog].[IngredientUnitConversion]
    ADD CONSTRAINT [DF_IngredientUnitConversion_IsBaseUnit] DEFAULT ((0)) FOR [IsBaseUnit];


GO


GO
ALTER TABLE [catalog].[IngredientUnitConversion]
    ADD CONSTRAINT [DF_IngredientUnitConversion_UpdatedAt] DEFAULT (sysutcdatetime()) FOR [UpdatedAt];


GO


GO
ALTER TABLE [hr].[Attendance]
    ADD CONSTRAINT [DF_Attendance_Status] DEFAULT ('PENDING') FOR [Status];


GO


GO
ALTER TABLE [hr].[Payroll]
    ADD CONSTRAINT [DF_Payroll_WorkedHours] DEFAULT ((0)) FOR [WorkedHours];


GO


GO
ALTER TABLE [hr].[Payroll]
    ADD CONSTRAINT [DF_Payroll_UpdatedAt] DEFAULT (sysutcdatetime()) FOR [UpdatedAt];


GO


GO
ALTER TABLE [hr].[Payroll]
    ADD CONSTRAINT [DF_Payroll_HourlyRate] DEFAULT ((0)) FOR [HourlyRate];


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
ALTER TABLE [inventory].[StockReceipt]
    ADD CONSTRAINT [DF_StockReceipt_DocumentDate] DEFAULT (CONVERT([date],sysutcdatetime())) FOR [DocumentDate];


GO


GO
ALTER TABLE [inventory].[StockReceipt]
    ADD CONSTRAINT [DF_StockReceipt_Status] DEFAULT ('DRAFT') FOR [Status];


GO


GO
ALTER TABLE [inventory].[StockReceipt]
    ADD CONSTRAINT [DF_StockReceipt_TotalCost] DEFAULT ((0)) FOR [TotalCost];


GO


GO
ALTER TABLE [inventory].[StockReceipt]
    ADD CONSTRAINT [DF_StockReceipt_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


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
ALTER TABLE [inventory].[WasteEventAudit]
    ADD CONSTRAINT [DF_WasteEventAudit_PerformedAt] DEFAULT (sysutcdatetime()) FOR [PerformedAt];


GO


GO
ALTER TABLE [inventory].[WasteEventItem]
    ADD CONSTRAINT [DF_WasteEventItem_LoggedAt] DEFAULT (sysutcdatetime()) FOR [LoggedAt];


GO


GO
ALTER TABLE [inventory].[WasteEventItem]
    ADD CONSTRAINT [DF_WasteEventItem_Status] DEFAULT ('ACTIVE') FOR [Status];


GO


GO
ALTER TABLE [inventory].[StockCount]
    ADD CONSTRAINT [DF_StockCount_CountedAt] DEFAULT (sysutcdatetime()) FOR [CountedAt];


GO


GO
ALTER TABLE [inventory].[StockReceiptDetail]
    ADD CONSTRAINT [DF_StockReceiptDetail_UnitCost] DEFAULT ((0)) FOR [UnitCost];


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
ALTER TABLE [inventory].[WasteEvent]
    ADD CONSTRAINT [DF_WasteEvent_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


GO


GO
ALTER TABLE [inventory].[WasteEventReview]
    ADD CONSTRAINT [DF_WasteEventReview_Status] DEFAULT ('OPEN') FOR [Status];


GO


GO
ALTER TABLE [inventory].[WasteEventReview]
    ADD CONSTRAINT [DF_WasteEventReview_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


GO


GO
ALTER TABLE [ops].[LegacySchemaVersion]
    ADD CONSTRAINT [DF_LegacySchemaVersion_AppliedAt] DEFAULT (sysutcdatetime()) FOR [AppliedAt];


GO


GO
ALTER TABLE [ops].[MigrationBackfillReport]
    ADD CONSTRAINT [DF_MigrationBackfillReport_RecordedAt] DEFAULT (sysutcdatetime()) FOR [RecordedAt];


GO


GO
ALTER TABLE [ops].[OrderItemActionLog]
    ADD CONSTRAINT [DF_OrderItemActionLog_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


GO


GO
ALTER TABLE [ops].[OutboxEvent]
    ADD CONSTRAINT [DF_OutboxEvent_CreatedAt] DEFAULT (sysutcdatetime()) FOR [CreatedAt];


GO


GO
ALTER TABLE [ops].[MenuBlockTimestampArchive]
    ADD CONSTRAINT [DF_MenuBlockTimestampArchive_ReviewStatus] DEFAULT ('PENDING') FOR [ReviewStatus];


GO


GO
ALTER TABLE [ops].[MenuBlockTimestampArchive]
    ADD CONSTRAINT [DF_MenuBlockTimestampArchive_ArchivedAt] DEFAULT (sysutcdatetime()) FOR [ArchivedAt];


GO


GO
ALTER TABLE [ops].[AttendanceDuplicateArchive]
    ADD CONSTRAINT [DF_AttendanceDuplicateArchive_ArchivedAt] DEFAULT (sysutcdatetime()) FOR [ArchivedAt];


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
ALTER TABLE [payment].[Voucher]
    ADD CONSTRAINT [DF_Voucher_IsActive] DEFAULT ((1)) FOR [IsActive];


GO


GO
ALTER TABLE [payment].[Voucher]
    ADD CONSTRAINT [DF_Voucher_MinOrderAmount] DEFAULT ((0)) FOR [MinOrderAmount];


GO


GO
ALTER TABLE [payment].[Voucher]
    ADD CONSTRAINT [DF_Voucher_Scope] DEFAULT ('CHAIN') FOR [Scope];


GO


GO
ALTER TABLE [payment].[Voucher]
    ADD CONSTRAINT [DF_Voucher_UsedCount] DEFAULT ((0)) FOR [UsedCount];


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
ALTER TABLE [sales].[TableSession]
    ADD CONSTRAINT [DF_TableSession_Status] DEFAULT ('OPEN') FOR [Status];


GO


GO
ALTER TABLE [sales].[TableSession]
    ADD CONSTRAINT [DF_TableSession_OpenedAt] DEFAULT (sysutcdatetime()) FOR [OpenedAt];


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

ALTER TABLE [catalog].[PrepRecipe] WITH NOCHECK
    ADD CONSTRAINT [FK_PrepRecipe_Ingredient_PreppedTyped] FOREIGN KEY ([PreppedIngredientId], [PreppedTypeGuard]) REFERENCES [catalog].[Ingredient] ([IngredientId], [IngredientType]);


GO


GO

ALTER TABLE [catalog].[PrepRecipe] WITH NOCHECK
    ADD CONSTRAINT [FK_PrepRecipe_Ingredient_PreppedIngredient] FOREIGN KEY ([PreppedIngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO

ALTER TABLE [catalog].[ModifierOption] WITH NOCHECK
    ADD CONSTRAINT [FK_ModifierOption_ModifierGroup] FOREIGN KEY ([ModifierGroupId]) REFERENCES [catalog].[ModifierGroup] ([ModifierGroupId]);


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

ALTER TABLE [catalog].[PrepRecipeIngredient] WITH NOCHECK
    ADD CONSTRAINT [FK_PrepRecipeIngredient_PrepRecipe] FOREIGN KEY ([PrepRecipeId]) REFERENCES [catalog].[PrepRecipe] ([PrepRecipeId]) ON DELETE CASCADE;


GO


GO

ALTER TABLE [catalog].[PrepRecipeIngredient] WITH NOCHECK
    ADD CONSTRAINT [FK_PrepRecipeIngredient_Ingredient_RawTyped] FOREIGN KEY ([RawIngredientId], [RawTypeGuard]) REFERENCES [catalog].[Ingredient] ([IngredientId], [IngredientType]);


GO


GO

ALTER TABLE [catalog].[PrepRecipeIngredient] WITH NOCHECK
    ADD CONSTRAINT [FK_PrepRecipeIngredient_Ingredient_RawIngredient] FOREIGN KEY ([RawIngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO

ALTER TABLE [catalog].[ModifierIngredientImpact] WITH NOCHECK
    ADD CONSTRAINT [FK_ModifierIngredientImpact_Ingredient] FOREIGN KEY ([IngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO

ALTER TABLE [catalog].[ModifierIngredientImpact] WITH NOCHECK
    ADD CONSTRAINT [FK_ModifierIngredientImpact_ModifierOption] FOREIGN KEY ([ModifierOptionId]) REFERENCES [catalog].[ModifierOption] ([ModifierOptionId]);


GO


GO

ALTER TABLE [catalog].[ProductModifierGroup] WITH NOCHECK
    ADD CONSTRAINT [FK_ProductModifierGroup_ModifierGroup] FOREIGN KEY ([ModifierGroupId]) REFERENCES [catalog].[ModifierGroup] ([ModifierGroupId]);


GO


GO

ALTER TABLE [catalog].[ProductModifierGroup] WITH NOCHECK
    ADD CONSTRAINT [FK_ProductModifierGroup_Product] FOREIGN KEY ([ProductId]) REFERENCES [catalog].[Product] ([ProductId]);


GO


GO

ALTER TABLE [catalog].[ProductRecipe] WITH NOCHECK
    ADD CONSTRAINT [FK_ProductRecipe_Ingredient] FOREIGN KEY ([IngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO

ALTER TABLE [catalog].[ProductRecipe] WITH NOCHECK
    ADD CONSTRAINT [FK_ProductRecipe_Product] FOREIGN KEY ([ProductId]) REFERENCES [catalog].[Product] ([ProductId]);


GO


GO

ALTER TABLE [catalog].[Product] WITH NOCHECK
    ADD CONSTRAINT [FK_Product_Category] FOREIGN KEY ([CategoryId]) REFERENCES [catalog].[Category] ([CategoryId]);


GO


GO

ALTER TABLE [catalog].[MenuBlockRequest] WITH NOCHECK
    ADD CONSTRAINT [FK_MenuBlockRequest_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [catalog].[MenuBlockRequest] WITH NOCHECK
    ADD CONSTRAINT [FK_MenuBlockRequest_UserAccount_ReviewedBy] FOREIGN KEY ([ReviewedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [catalog].[MenuBlockRequest] WITH NOCHECK
    ADD CONSTRAINT [FK_MenuBlockRequest_UserAccount_RequestedBy] FOREIGN KEY ([RequestedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [catalog].[MenuBlockRequest] WITH NOCHECK
    ADD CONSTRAINT [FK_MenuBlockRequest_Product] FOREIGN KEY ([ProductId]) REFERENCES [catalog].[Product] ([ProductId]);


GO


GO

ALTER TABLE [catalog].[IngredientUnitConversion] WITH NOCHECK
    ADD CONSTRAINT [FK_IngredientUnitConversion_UserAccount_UpdatedBy] FOREIGN KEY ([UpdatedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [catalog].[IngredientUnitConversion] WITH NOCHECK
    ADD CONSTRAINT [FK_IngredientUnitConversion_Ingredient] FOREIGN KEY ([IngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO

ALTER TABLE [hr].[ShiftAssignment] WITH NOCHECK
    ADD CONSTRAINT [FK_ShiftAssignment_UserAccount_User] FOREIGN KEY ([UserId]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [hr].[ShiftAssignment] WITH NOCHECK
    ADD CONSTRAINT [FK_ShiftAssignment_ShiftTemplate_Branch] FOREIGN KEY ([ShiftTemplateId], [BranchId]) REFERENCES [hr].[ShiftTemplate] ([ShiftTemplateId], [BranchId]);


GO


GO

ALTER TABLE [hr].[ShiftAssignment] WITH NOCHECK
    ADD CONSTRAINT [FK_ShiftAssignment_ShiftTemplate] FOREIGN KEY ([ShiftTemplateId]) REFERENCES [hr].[ShiftTemplate] ([ShiftTemplateId]);


GO


GO

ALTER TABLE [hr].[ShiftTemplate] WITH NOCHECK
    ADD CONSTRAINT [FK_ShiftTemplate_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [hr].[Attendance] WITH NOCHECK
    ADD CONSTRAINT [FK_Attendance_ShiftAssignment] FOREIGN KEY ([ShiftAssignmentId]) REFERENCES [hr].[ShiftAssignment] ([ShiftAssignmentId]);


GO


GO

ALTER TABLE [hr].[Attendance] WITH NOCHECK
    ADD CONSTRAINT [FK_Attendance_UserAccount_ApprovedBy] FOREIGN KEY ([ApprovedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [hr].[Payroll] WITH NOCHECK
    ADD CONSTRAINT [FK_Payroll_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [hr].[Payroll] WITH NOCHECK
    ADD CONSTRAINT [FK_Payroll_UserAccount_User] FOREIGN KEY ([UserId]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [hr].[Payroll] WITH NOCHECK
    ADD CONSTRAINT [FK_Payroll_UserAccount_UpdatedBy] FOREIGN KEY ([UpdatedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [iam].[UserAccount] WITH NOCHECK
    ADD CONSTRAINT [FK_UserAccount_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [iam].[UserAccount] WITH NOCHECK
    ADD CONSTRAINT [FK_UserAccount_Role] FOREIGN KEY ([RoleId]) REFERENCES [iam].[Role] ([RoleId]);


GO


GO

ALTER TABLE [inventory].[StockReceipt] WITH NOCHECK
    ADD CONSTRAINT [FK_StockReceipt_UserAccount_ReceivedBy] FOREIGN KEY ([ReceivedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [inventory].[StockReceipt] WITH NOCHECK
    ADD CONSTRAINT [FK_StockReceipt_Supplier] FOREIGN KEY ([SupplierId]) REFERENCES [inventory].[Supplier] ([SupplierId]);


GO


GO

ALTER TABLE [inventory].[StockReceipt] WITH NOCHECK
    ADD CONSTRAINT [FK_StockReceipt_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


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

ALTER TABLE [inventory].[WasteEventAudit] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEventAudit_UserAccount_PerformedBy] FOREIGN KEY ([PerformedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [inventory].[WasteEventAudit] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEventAudit_WasteEvent] FOREIGN KEY ([WasteEventId]) REFERENCES [inventory].[WasteEvent] ([WasteEventId]);


GO


GO

ALTER TABLE [inventory].[WasteEventAudit] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEventAudit_WasteEventItem_Item] FOREIGN KEY ([WasteEventItemId]) REFERENCES [inventory].[WasteEventItem] ([WasteEventItemId]);


GO


GO

ALTER TABLE [inventory].[WasteEventItem] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEventItem_Ingredient] FOREIGN KEY ([IngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO

ALTER TABLE [inventory].[WasteEventItem] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEventItem_UserAccount_LoggedBy] FOREIGN KEY ([LoggedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [inventory].[WasteEventItem] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEventItem_WasteEvent_Branch] FOREIGN KEY ([WasteEventId], [BranchId]) REFERENCES [inventory].[WasteEvent] ([WasteEventId], [BranchId]);


GO


GO

ALTER TABLE [inventory].[WasteEventItem] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEventItem_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [inventory].[StockCount] WITH NOCHECK
    ADD CONSTRAINT [FK_StockCount_UserAccount_CountedBy] FOREIGN KEY ([CountedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [inventory].[StockCount] WITH NOCHECK
    ADD CONSTRAINT [FK_StockCount_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [inventory].[StockReceiptDetail] WITH NOCHECK
    ADD CONSTRAINT [FK_StockReceiptDetail_Ingredient] FOREIGN KEY ([IngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO

ALTER TABLE [inventory].[StockReceiptDetail] WITH NOCHECK
    ADD CONSTRAINT [FK_StockReceiptDetail_StockReceipt] FOREIGN KEY ([StockReceiptId]) REFERENCES [inventory].[StockReceipt] ([StockReceiptId]) ON DELETE CASCADE;


GO


GO

ALTER TABLE [inventory].[StockReceiptDetail] WITH NOCHECK
    ADD CONSTRAINT [FK_StockReceiptDetail_UnitConversionIngredient] FOREIGN KEY ([IngredientUnitConversionId], [IngredientId]) REFERENCES [catalog].[IngredientUnitConversion] ([IngredientUnitConversionId], [IngredientId]);


GO


GO

ALTER TABLE [inventory].[StockReceiptDetail] WITH NOCHECK
    ADD CONSTRAINT [FK_StockReceiptDetail_IngredientUnitConversion] FOREIGN KEY ([IngredientUnitConversionId]) REFERENCES [catalog].[IngredientUnitConversion] ([IngredientUnitConversionId]);


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
    ADD CONSTRAINT [FK_PrepBatch_WasteEventItem_WriteOff] FOREIGN KEY ([WriteOffWasteEventItemId]) REFERENCES [inventory].[WasteEventItem] ([WasteEventItemId]);


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

ALTER TABLE [inventory].[StockAdjustment] WITH NOCHECK
    ADD CONSTRAINT [FK_StockAdjustment_UnitConversionIngredient] FOREIGN KEY ([IngredientUnitConversionId], [IngredientId]) REFERENCES [catalog].[IngredientUnitConversion] ([IngredientUnitConversionId], [IngredientId]);


GO


GO

ALTER TABLE [inventory].[StockAdjustment] WITH NOCHECK
    ADD CONSTRAINT [FK_StockAdjustment_Ingredient] FOREIGN KEY ([IngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO

ALTER TABLE [inventory].[StockAdjustment] WITH NOCHECK
    ADD CONSTRAINT [FK_StockAdjustment_IngredientUnitConversion] FOREIGN KEY ([IngredientUnitConversionId]) REFERENCES [catalog].[IngredientUnitConversion] ([IngredientUnitConversionId]);


GO


GO

ALTER TABLE [inventory].[StockAdjustment] WITH NOCHECK
    ADD CONSTRAINT [FK_StockAdjustment_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [inventory].[StockAdjustment] WITH NOCHECK
    ADD CONSTRAINT [FK_StockAdjustment_StockCount] FOREIGN KEY ([StockCountId], [BranchId]) REFERENCES [inventory].[StockCount] ([StockCountId], [BranchId]);


GO


GO

ALTER TABLE [inventory].[StockAdjustment] WITH NOCHECK
    ADD CONSTRAINT [FK_StockAdjustment_UserAccount_AdjustedBy] FOREIGN KEY ([AdjustedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [inventory].[WasteEvent] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEvent_OrderItem_ProductBranch] FOREIGN KEY ([OrderItemId], [ProductId], [BranchId]) REFERENCES [sales].[OrderItem] ([OrderItemId], [ProductId], [BranchId]);


GO


GO

ALTER TABLE [inventory].[WasteEvent] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEvent_UserAccount_CreatedBy] FOREIGN KEY ([CreatedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [inventory].[WasteEvent] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEvent_ShiftAssignment_Branch] FOREIGN KEY ([ShiftAssignmentId], [BranchId]) REFERENCES [hr].[ShiftAssignment] ([ShiftAssignmentId], [BranchId]);


GO


GO

ALTER TABLE [inventory].[WasteEvent] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEvent_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [inventory].[WasteEvent] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEvent_Product] FOREIGN KEY ([ProductId]) REFERENCES [catalog].[Product] ([ProductId]);


GO


GO

ALTER TABLE [inventory].[WasteEvent] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEvent_OrderItem_Branch] FOREIGN KEY ([OrderItemId], [BranchId]) REFERENCES [sales].[OrderItem] ([OrderItemId], [BranchId]);


GO


GO

ALTER TABLE [inventory].[WasteEvent] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEvent_ShiftAssignment] FOREIGN KEY ([ShiftAssignmentId]) REFERENCES [hr].[ShiftAssignment] ([ShiftAssignmentId]);


GO


GO

ALTER TABLE [inventory].[WasteEvent] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEvent_OrderItem] FOREIGN KEY ([OrderItemId]) REFERENCES [sales].[OrderItem] ([OrderItemId]);


GO


GO

ALTER TABLE [inventory].[WasteEventReview] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEventReview_WasteEvent] FOREIGN KEY ([WasteEventId]) REFERENCES [inventory].[WasteEvent] ([WasteEventId]);


GO


GO

ALTER TABLE [inventory].[WasteEventReview] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEventReview_UserAccount_ResolvedBy] FOREIGN KEY ([ResolvedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [inventory].[WasteEventReview] WITH NOCHECK
    ADD CONSTRAINT [FK_WasteEventReview_Ingredient] FOREIGN KEY ([IngredientId]) REFERENCES [catalog].[Ingredient] ([IngredientId]);


GO


GO

ALTER TABLE [ops].[OrderItemActionLog] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItemActionLog_OrderItem] FOREIGN KEY ([OrderItemId]) REFERENCES [sales].[OrderItem] ([OrderItemId]);


GO


GO

ALTER TABLE [ops].[OrderItemActionLog] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItemActionLog_OrderItem_Branch] FOREIGN KEY ([OrderItemId], [BranchId]) REFERENCES [sales].[OrderItem] ([OrderItemId], [BranchId]);


GO


GO

ALTER TABLE [ops].[OrderItemActionLog] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItemActionLog_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [ops].[OrderItemActionLog] WITH NOCHECK
    ADD CONSTRAINT [FK_OrderItemActionLog_UserAccount_PerformedBy] FOREIGN KEY ([PerformedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


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

ALTER TABLE [payment].[Voucher] WITH NOCHECK
    ADD CONSTRAINT [FK_Voucher_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [payment].[BillItem] WITH NOCHECK
    ADD CONSTRAINT [FK_BillItem_Bill] FOREIGN KEY ([BillId]) REFERENCES [payment].[Bill] ([BillId]) ON DELETE CASCADE;


GO


GO

ALTER TABLE [payment].[BillItem] WITH NOCHECK
    ADD CONSTRAINT [FK_BillItem_Bill_Branch] FOREIGN KEY ([BillId], [BranchId]) REFERENCES [payment].[Bill] ([BillId], [BranchId]);


GO


GO

ALTER TABLE [payment].[BillItem] WITH NOCHECK
    ADD CONSTRAINT [FK_BillItem_OrderItem] FOREIGN KEY ([OrderItemId]) REFERENCES [sales].[OrderItem] ([OrderItemId]);


GO


GO

ALTER TABLE [payment].[BillItem] WITH NOCHECK
    ADD CONSTRAINT [FK_BillItem_OrderItem_Branch] FOREIGN KEY ([OrderItemId], [BranchId]) REFERENCES [sales].[OrderItem] ([OrderItemId], [BranchId]);


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [FK_Bill_CashierShift] FOREIGN KEY ([CashierShiftId]) REFERENCES [payment].[CashierShift] ([CashierShiftId]);


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [FK_Bill_Voucher] FOREIGN KEY ([VoucherId]) REFERENCES [payment].[Voucher] ([VoucherId]);


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [FK_Bill_CashierShift_Branch] FOREIGN KEY ([CashierShiftId], [BranchId]) REFERENCES [payment].[CashierShift] ([CashierShiftId], [BranchId]);


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [FK_Bill_TableSession] FOREIGN KEY ([TableSessionId]) REFERENCES [sales].[TableSession] ([TableSessionId]);


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [FK_Bill_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [payment].[Bill] WITH NOCHECK
    ADD CONSTRAINT [FK_Bill_TableSession_Branch] FOREIGN KEY ([TableSessionId], [BranchId]) REFERENCES [sales].[TableSession] ([TableSessionId], [BranchId]);


GO


GO

ALTER TABLE [sales].[SalesOrder] WITH NOCHECK
    ADD CONSTRAINT [FK_SalesOrder_TableSession] FOREIGN KEY ([TableSessionId]) REFERENCES [sales].[TableSession] ([TableSessionId]);


GO


GO

ALTER TABLE [sales].[SalesOrder] WITH NOCHECK
    ADD CONSTRAINT [FK_SalesOrder_TableSession_Branch] FOREIGN KEY ([TableSessionId], [BranchId]) REFERENCES [sales].[TableSession] ([TableSessionId], [BranchId]);


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

ALTER TABLE [sales].[PickupSequence] WITH NOCHECK
    ADD CONSTRAINT [FK_PickupSequence_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


GO


GO

ALTER TABLE [sales].[TableSession] WITH NOCHECK
    ADD CONSTRAINT [FK_TableSession_UserAccount_OpenedBy] FOREIGN KEY ([OpenedBy]) REFERENCES [iam].[UserAccount] ([UserId]);


GO


GO

ALTER TABLE [sales].[TableSession] WITH NOCHECK
    ADD CONSTRAINT [FK_TableSession_DiningTable] FOREIGN KEY ([DiningTableId]) REFERENCES [sales].[DiningTable] ([DiningTableId]);


GO


GO

ALTER TABLE [sales].[TableSession] WITH NOCHECK
    ADD CONSTRAINT [FK_TableSession_DiningTable_Branch] FOREIGN KEY ([DiningTableId], [BranchId]) REFERENCES [sales].[DiningTable] ([DiningTableId], [BranchId]);


GO


GO

ALTER TABLE [sales].[TableSession] WITH NOCHECK
    ADD CONSTRAINT [FK_TableSession_Branch] FOREIGN KEY ([BranchId]) REFERENCES [org].[Branch] ([BranchId]);


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

ALTER TABLE [catalog].[PrepRecipe] WITH NOCHECK
    ADD CONSTRAINT [CK_PrepRecipe_YieldQty] CHECK ([YieldQty]>(0));


GO


GO

ALTER TABLE [catalog].[BranchMenu] WITH NOCHECK
    ADD CONSTRAINT [CK_BranchMenu_LocalPrice] CHECK ([LocalPrice] IS NULL OR [LocalPrice]>=(0));


GO


GO

ALTER TABLE [catalog].[HomeSetting] WITH NOCHECK
    ADD CONSTRAINT [CK_HomeSetting_Singleton] CHECK ([HomeSettingId]=(1));


GO


GO

ALTER TABLE [catalog].[PrepRecipeIngredient] WITH NOCHECK
    ADD CONSTRAINT [CK_PrepRecipeIngredient_Quantity] CHECK ([Quantity]>(0));


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

ALTER TABLE [catalog].[ProductRecipe] WITH NOCHECK
    ADD CONSTRAINT [CK_ProductRecipe_Quantity] CHECK ([Quantity]>(0));


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

ALTER TABLE [catalog].[MenuBlockRequest] WITH NOCHECK
    ADD CONSTRAINT [CK_MenuBlockRequest_Lifecycle] CHECK ([Status]='PENDING' AND [ReviewedBy] IS NULL AND [ReviewedAt] IS NULL AND [ClosedAt] IS NULL OR [Status]='APPROVED' AND [ReviewedBy] IS NOT NULL AND [ReviewedAt] IS NOT NULL AND [ClosedAt] IS NULL OR [Status]='REJECTED' AND [ReviewedBy] IS NOT NULL AND [ReviewedAt] IS NOT NULL AND [ClosedAt] IS NOT NULL OR [Status]='RESOLVED' AND [ReviewedBy] IS NOT NULL AND [ReviewedAt] IS NOT NULL AND [ClosedAt] IS NOT NULL);


GO


GO

ALTER TABLE [catalog].[MenuBlockRequest] WITH NOCHECK
    ADD CONSTRAINT [CK_MenuBlockRequest_Status] CHECK ([Status]='RESOLVED' OR [Status]='REJECTED' OR [Status]='APPROVED' OR [Status]='PENDING');


GO


GO

ALTER TABLE [catalog].[MenuBlockRequest] WITH NOCHECK
    ADD CONSTRAINT [CK_MenuBlockRequest_TimeOrder] CHECK (([BackInEta] IS NULL OR [BackInEta]>=[RequestedAt]) AND ([ReopenRequestedAt] IS NULL OR [ReopenRequestedAt]>=[RequestedAt]) AND ([ReviewedAt] IS NULL OR [ReviewedAt]>=[RequestedAt]) AND ([ClosedAt] IS NULL OR [ClosedAt]>=[RequestedAt]));


GO


GO

ALTER TABLE [catalog].[IngredientUnitConversion] WITH NOCHECK
    ADD CONSTRAINT [CK_IngredientUnitConversion_Factor] CHECK ([FactorToBase]>(0));


GO


GO

ALTER TABLE [catalog].[IngredientUnitConversion] WITH NOCHECK
    ADD CONSTRAINT [CK_IngredientUnitConversion_Base] CHECK ([IsBaseUnit]=(0) OR [FactorToBase]=CONVERT([decimal](18,6),(1)) AND [IsActive]=(1));


GO


GO

ALTER TABLE [hr].[ShiftTemplate] WITH NOCHECK
    ADD CONSTRAINT [CK_ShiftTemplate_NonZeroDuration] CHECK ([StartTime]<>[EndTime]);


GO


GO

ALTER TABLE [hr].[Attendance] WITH NOCHECK
    ADD CONSTRAINT [CK_Attendance_Status] CHECK ([Status]='REJECTED' OR [Status]='APPROVED' OR [Status]='PENDING');


GO


GO

ALTER TABLE [hr].[Attendance] WITH NOCHECK
    ADD CONSTRAINT [CK_Attendance_CheckOutAfterIn] CHECK ([CheckOutAt] IS NULL OR [CheckInAt] IS NULL OR [CheckOutAt]>=[CheckInAt]);


GO


GO

ALTER TABLE [hr].[Attendance] WITH NOCHECK
    ADD CONSTRAINT [CK_Attendance_ApprovalLifecycle] CHECK ([Status]='PENDING' AND [ApprovedBy] IS NULL AND [ApprovedAt] IS NULL OR ([Status]='REJECTED' OR [Status]='APPROVED') AND [ApprovedBy] IS NOT NULL AND [ApprovedAt] IS NOT NULL);


GO


GO

ALTER TABLE [hr].[Payroll] WITH NOCHECK
    ADD CONSTRAINT [CK_Payroll_NonNegative] CHECK ([WorkedHours]>=(0) AND [HourlyRate]>=(0));


GO


GO

ALTER TABLE [hr].[Payroll] WITH NOCHECK
    ADD CONSTRAINT [CK_Payroll_MonthStart] CHECK (datepart(day,[PayrollMonth])=(1));


GO


GO

ALTER TABLE [iam].[UserAccount] WITH NOCHECK
    ADD CONSTRAINT [CK_UserAccount_Status] CHECK ([Status]='LOCKED' OR [Status]='ACTIVE');


GO


GO

ALTER TABLE [inventory].[StockReceipt] WITH NOCHECK
    ADD CONSTRAINT [CK_StockReceipt_Status] CHECK ([Status]='CANCELLED' OR [Status]='CONFIRMED' OR [Status]='DRAFT');


GO


GO

ALTER TABLE [inventory].[StockReceipt] WITH NOCHECK
    ADD CONSTRAINT [CK_StockReceipt_TotalCost] CHECK ([TotalCost]>=(0));


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

ALTER TABLE [inventory].[WasteEventAudit] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEventAudit_Reference] CHECK ([WasteEventItemId] IS NOT NULL OR [WasteEventId] IS NOT NULL);


GO


GO

ALTER TABLE [inventory].[WasteEventAudit] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEventAudit_Action] CHECK ([ActionType]='REVIEW' OR [ActionType]='VOID' OR [ActionType]='UPDATE' OR [ActionType]='CREATE');


GO


GO

ALTER TABLE [inventory].[WasteEventItem] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEventItem_Quantity_Value] CHECK ([Quantity]>(0));


GO


GO

ALTER TABLE [inventory].[WasteEventItem] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEventItem_UnitCost] CHECK ([UnitCostAtLog] IS NULL OR [UnitCostAtLog]>=(0));


GO


GO

ALTER TABLE [inventory].[WasteEventItem] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEventItem_Status] CHECK ([Status]='VOIDED' OR [Status]='ACTIVE');


GO


GO

ALTER TABLE [inventory].[WasteEventItem] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEventItem_Type] CHECK ([WasteType]='OTHER' OR [WasteType]='REMAKE' OR [WasteType]='EXPIRED' OR [WasteType]='SPILL');


GO


GO

ALTER TABLE [inventory].[WasteEventItem] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEventItem_CostBasis] CHECK ([CostBasis] IS NULL OR ([CostBasis]='LEGACY_ESTIMATE' OR [CostBasis]='UNAVAILABLE' OR [CostBasis]='SNAPSHOT'));


GO


GO

ALTER TABLE [inventory].[StockReceiptDetail] WITH NOCHECK
    ADD CONSTRAINT [CK_StockReceiptDetail_EnteredQuantity] CHECK ([EnteredQuantity]>(0));


GO


GO

ALTER TABLE [inventory].[StockReceiptDetail] WITH NOCHECK
    ADD CONSTRAINT [CK_StockReceiptDetail_UnitCost] CHECK ([UnitCost]>=(0));


GO


GO

ALTER TABLE [inventory].[StockReceiptDetail] WITH NOCHECK
    ADD CONSTRAINT [CK_StockReceiptDetail_BasePrecision] CHECK ([EnteredQuantity]*[FactorToBaseAtEntry]=CONVERT([decimal](12,3),[EnteredQuantity]*[FactorToBaseAtEntry]));


GO


GO

ALTER TABLE [inventory].[StockReceiptDetail] WITH NOCHECK
    ADD CONSTRAINT [CK_StockReceiptDetail_FactorToBaseAtEntry] CHECK ([FactorToBaseAtEntry]>(0));


GO


GO

ALTER TABLE [inventory].[InventoryTransaction] WITH NOCHECK
    ADD CONSTRAINT [CK_InventoryTransaction_ChangeQty] CHECK ([ChangeQty]<>(0));


GO


GO

ALTER TABLE [inventory].[InventoryTransaction] WITH NOCHECK
    ADD CONSTRAINT [CK_InventoryTransaction_Reference] CHECK ([ReferenceType] IS NULL AND [ReferenceId] IS NULL OR (([ReferenceType]) collate Latin1_General_100_BIN2='STOCK_ADJUSTMENT' OR ([ReferenceType]) collate Latin1_General_100_BIN2='WASTE_EVENT_ITEM' OR ([ReferenceType]) collate Latin1_General_100_BIN2='PREP_BATCH' OR ([ReferenceType]) collate Latin1_General_100_BIN2='ORDER_ITEM' OR ([ReferenceType]) collate Latin1_General_100_BIN2='STOCK_RECEIPT') AND [ReferenceId] IS NOT NULL);


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
    ADD CONSTRAINT [CK_PrepBatch_Lifecycle] CHECK (([ReviewedBy] IS NULL AND [ReviewedAt] IS NULL OR [ReviewedBy] IS NOT NULL AND [ReviewedAt] IS NOT NULL) AND ([ExpiresAt] IS NULL OR [ExpiresAt]>=[MadeAt]) AND ([Status]<>'PENDING' OR [RequiresApproval]=(1) AND [ReviewedBy] IS NULL) AND ([Status]<>'REJECTED' OR [RequiresApproval]=(1) AND [ReviewedBy] IS NOT NULL) AND ([Status]<>'ACTIVE' OR [RequiresApproval]=(0) OR [ReviewedBy] IS NOT NULL) AND ([Status]<>'CANCELLED' OR [VoidedAt] IS NOT NULL) AND ([WrittenOffAt] IS NULL AND [WriteOffWasteEventItemId] IS NULL OR [WrittenOffAt] IS NOT NULL AND [WriteOffWasteEventItemId] IS NOT NULL));


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

ALTER TABLE [inventory].[WasteEvent] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEvent_Kind] CHECK ([EventKind]='REMAKE' OR [EventKind]='INGREDIENT_WASTE');


GO


GO

ALTER TABLE [inventory].[WasteEvent] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEvent_Remake] CHECK ([EventKind]='REMAKE' AND [ProductId] IS NOT NULL AND [CupQuantity] IS NOT NULL AND [CupQuantity]>(0) OR [EventKind]='INGREDIENT_WASTE' AND [ProductId] IS NULL AND [CupQuantity] IS NULL);


GO


GO

ALTER TABLE [inventory].[WasteEvent] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEvent_Source] CHECK ([Source]='KDS' OR [Source]='MANUAL');


GO


GO

ALTER TABLE [inventory].[WasteEventReview] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEventReview_Lifecycle] CHECK ([Status]='OPEN' AND [ResolvedBy] IS NULL AND [ResolvedAt] IS NULL OR [Status]='RESOLVED' AND [ResolvedBy] IS NOT NULL AND [ResolvedAt] IS NOT NULL);


GO


GO

ALTER TABLE [inventory].[WasteEventReview] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEventReview_Status] CHECK ([Status]='RESOLVED' OR [Status]='OPEN');


GO


GO

ALTER TABLE [inventory].[WasteEventReview] WITH NOCHECK
    ADD CONSTRAINT [CK_WasteEventReview_Type] CHECK ([ReviewType]='MANAGER_VOID' OR [ReviewType]='LATE_CORRECTION' OR [ReviewType]='HARD_NEGATIVE' OR [ReviewType]='SOFT_NEGATIVE');


GO


GO

ALTER TABLE [ops].[OrderItemActionLog] WITH NOCHECK
    ADD CONSTRAINT [CK_OrderItemActionLog_ActionType] CHECK ([ActionType]='UNDO_SERVE' OR [ActionType]='SERVE' OR [ActionType]='PICK_UP' OR [ActionType]='CANCEL' OR [ActionType]='REMAKE' OR [ActionType]='UNBLOCK' OR [ActionType]='BLOCK' OR [ActionType]='ISSUE' OR [ActionType]='RETURN_QUEUE' OR [ActionType]='COMPLETE' OR [ActionType]='CLAIM');


GO


GO

ALTER TABLE [ops].[OutboxEvent] WITH NOCHECK
    ADD CONSTRAINT [CK_OutboxEvent_PayloadJson] CHECK ([Payload] IS NULL OR isjson([Payload])=(1));


GO


GO

ALTER TABLE [ops].[MenuBlockTimestampArchive] WITH NOCHECK
    ADD CONSTRAINT [CK_MenuBlockTimestampArchive_ReviewStatus] CHECK ([ReviewStatus]='CORRECTED' OR [ReviewStatus]='CONFIRMED' OR [ReviewStatus]='PENDING');


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

ALTER TABLE [payment].[Voucher] WITH NOCHECK
    ADD CONSTRAINT [CK_Voucher_Usage] CHECK ([UsedCount]>=(0) AND ([UsageLimit] IS NULL OR [UsageLimit]>=(0) AND [UsedCount]<=[UsageLimit]));


GO


GO

ALTER TABLE [payment].[Voucher] WITH NOCHECK
    ADD CONSTRAINT [CK_Voucher_DiscountValue] CHECK ([DiscountValue]>=(0));


GO


GO

ALTER TABLE [payment].[Voucher] WITH NOCHECK
    ADD CONSTRAINT [CK_Voucher_Scope] CHECK ([Scope]='BRANCH' OR [Scope]='CHAIN');


GO


GO

ALTER TABLE [payment].[Voucher] WITH NOCHECK
    ADD CONSTRAINT [CK_Voucher_UtcRange] CHECK ([StartAtUtc] IS NULL OR [EndAtUtc] IS NULL OR [StartAtUtc]<[EndAtUtc]);


GO


GO

ALTER TABLE [payment].[Voucher] WITH NOCHECK
    ADD CONSTRAINT [CK_Voucher_Type] CHECK ([DiscountType]='FIXED' OR [DiscountType]='PERCENT');


GO


GO

ALTER TABLE [payment].[Voucher] WITH NOCHECK
    ADD CONSTRAINT [CK_Voucher_BranchScope] CHECK ([Scope]='CHAIN' AND [BranchId] IS NULL OR [Scope]='BRANCH' AND [BranchId] IS NOT NULL);


GO


GO

ALTER TABLE [payment].[Voucher] WITH NOCHECK
    ADD CONSTRAINT [CK_Voucher_Value] CHECK ([DiscountValue]>=(0) AND ([DiscountType]<>'PERCENT' OR [DiscountValue]<=(100)) AND [MinOrderAmount]>=(0));


GO


GO

ALTER TABLE [payment].[BillItem] WITH NOCHECK
    ADD CONSTRAINT [CK_BillItem_Amount] CHECK ([Amount]>=(0));


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
    ADD CONSTRAINT [CK_SalesOrder_TypeSession] CHECK ([OrderType]='DINE_IN' AND [TableSessionId] IS NOT NULL OR [OrderType]='TAKEAWAY' AND [TableSessionId] IS NULL);


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

ALTER TABLE [sales].[PickupSequence] WITH NOCHECK
    ADD CONSTRAINT [CK_PickupSequence_NextValue] CHECK ([NextValue]>(0));


GO


GO

ALTER TABLE [sales].[TableSession] WITH NOCHECK
    ADD CONSTRAINT [CK_TableSession_Lifecycle] CHECK ([Status]='OPEN' AND [ClosedAt] IS NULL OR [Status]='CLOSED' AND [ClosedAt] IS NOT NULL AND [ClosedAt]>=[OpenedAt]);


GO


GO

ALTER TABLE [sales].[TableSession] WITH NOCHECK
    ADD CONSTRAINT [CK_TableSession_Status] CHECK (([Status]) collate Latin1_General_100_BIN2='CLOSED' OR ([Status]) collate Latin1_General_100_BIN2='OPEN');


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
CREATE   TRIGGER catalog.TR_Ingredient_ProtectBaseUnit
ON catalog.Ingredient AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i JOIN deleted d ON d.IngredientId=i.IngredientId
              WHERE i.Unit<>d.Unit AND EXISTS(
                  SELECT 1 FROM inventory.InventoryTransaction t
                  WHERE t.IngredientId=i.IngredientId))
        THROW 51010,'Không được đổi đơn vị gốc của nguyên liệu đã có ledger.',1;

    UPDATE c SET UnitName=LTRIM(RTRIM(i.Unit)),UpdatedAt=SYSUTCDATETIME()
    FROM catalog.IngredientUnitConversion c
    JOIN inserted i ON i.IngredientId=c.IngredientId
    JOIN deleted d ON d.IngredientId=i.IngredientId
    WHERE c.IsBaseUnit=1 AND i.Unit<>d.Unit;
END;
GO


GO
CREATE   TRIGGER catalog.TR_Ingredient_CreateBaseConversion
ON catalog.Ingredient AFTER INSERT AS
BEGIN
    SET NOCOUNT ON;
    INSERT catalog.IngredientUnitConversion(IngredientId,UnitName,FactorToBase,IsBaseUnit,IsActive)
    SELECT i.IngredientId,LTRIM(RTRIM(i.Unit)),CONVERT(DECIMAL(18,6),1),1,1
    FROM inserted i
    WHERE NOT EXISTS(SELECT 1 FROM catalog.IngredientUnitConversion c
                     WHERE c.IngredientId=i.IngredientId AND c.IsBaseUnit=1);
END;
GO


GO
CREATE   TRIGGER catalog.TR_MenuBlockRequest_ActorBranch
ON catalog.MenuBlockRequest AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted i
        LEFT JOIN iam.UserAccount requester ON requester.UserId=i.RequestedBy
        LEFT JOIN iam.Role requesterRole ON requesterRole.RoleId=requester.RoleId
        LEFT JOIN iam.UserAccount reviewer ON reviewer.UserId=i.ReviewedBy
        LEFT JOIN iam.Role reviewerRole ON reviewerRole.RoleId=reviewer.RoleId
        WHERE requester.UserId IS NULL OR requester.BranchId<>i.BranchId
           OR requester.Status<>'ACTIVE' OR requesterRole.Code<>'BARISTA'
           OR (i.ReviewedBy IS NOT NULL AND (reviewer.UserId IS NULL OR reviewer.BranchId<>i.BranchId
               OR reviewer.Status<>'ACTIVE' OR reviewerRole.Code<>'BRANCH_MANAGER'))
    ) THROW 51145,N'Người báo phải là BARISTA và người duyệt phải là BRANCH_MANAGER active đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER catalog.TR_IngredientUnitConversion_NoDeleteBase
ON catalog.IngredientUnitConversion AFTER UPDATE,DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM deleted d
        WHERE d.IsBaseUnit=1 AND NOT EXISTS(
            SELECT 1 FROM inserted i
            WHERE i.IngredientUnitConversionId=d.IngredientUnitConversionId
              AND i.IngredientId=d.IngredientId AND i.IsBaseUnit=1))
        THROW 51011,'Không được xóa, chuyển nguyên liệu hoặc bỏ cờ đơn vị gốc.',1;

    IF EXISTS(
        SELECT 1 FROM inserted c
        JOIN catalog.Ingredient i ON i.IngredientId=c.IngredientId
        WHERE c.IsBaseUnit=1
          AND UPPER(LTRIM(RTRIM(c.UnitName))) COLLATE Latin1_General_100_CI_AI
              <>UPPER(LTRIM(RTRIM(i.Unit))) COLLATE Latin1_General_100_CI_AI)
        THROW 51012,'Tên đơn vị gốc phải khớp Ingredient.Unit.',1;
END;
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
CREATE   TRIGGER hr.TR_Attendance_ApproverBranch
ON hr.Attendance AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted i
        JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=i.ShiftAssignmentId
        LEFT JOIN iam.UserAccount u ON u.UserId=i.ApprovedBy
        LEFT JOIN iam.Role r ON r.RoleId=u.RoleId
        WHERE i.ApprovedBy IS NOT NULL AND (u.UserId IS NULL OR u.BranchId<>sa.BranchId
              OR u.Status<>'ACTIVE' OR r.Code<>'BRANCH_MANAGER')
    ) THROW 51139,N'Người duyệt chấm công phải là BRANCH_MANAGER active đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER hr.TR_Payroll_ImmutableIdentity ON hr.Payroll AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i JOIN deleted d ON d.PayrollId=i.PayrollId
              WHERE i.BranchId<>d.BranchId OR i.UserId<>d.UserId OR i.PayrollMonth<>d.PayrollMonth)
        THROW 51020,'Không được đổi branch/user/month của payroll lịch sử.',1;
END;
GO


GO
CREATE   TRIGGER hr.TR_Payroll_ActorBranch
ON hr.Payroll AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i LEFT JOIN iam.UserAccount u ON u.UserId=i.UpdatedBy
              LEFT JOIN iam.Role r ON r.RoleId=u.RoleId
              WHERE i.UpdatedBy IS NOT NULL AND (u.UserId IS NULL OR u.BranchId<>i.BranchId
                    OR u.Status<>'ACTIVE' OR r.Code<>'BRANCH_MANAGER'))
        THROW 51147,N'Người cập nhật bảng lương phải là BRANCH_MANAGER active đúng chi nhánh.',1;
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
        SELECT 1 FROM inserted i JOIN iam.Role r ON r.RoleId=i.RoleId
        WHERE (r.Code='ADMIN' AND i.BranchId IS NOT NULL)
           OR (r.Code<>'ADMIN' AND i.BranchId IS NULL)
    ) THROW 51131,N'Admin phải toàn hệ thống; nhân viên vận hành phải thuộc một chi nhánh.',1;

    DECLARE @todayVn DATE=CONVERT(date,DATEADD(hour,7,SYSUTCDATETIME()));
    IF EXISTS(
        SELECT 1 FROM inserted i JOIN deleted d ON d.UserId=i.UserId
        WHERE ISNULL(i.BranchId,-1)<>ISNULL(d.BranchId,-1)
          AND (
              EXISTS(SELECT 1 FROM org.Branch b WHERE b.ManagerUserId=i.UserId)
           OR EXISTS(SELECT 1 FROM payment.CashierShift cs WHERE cs.CashierId=i.UserId AND cs.ClosedAt IS NULL)
           OR EXISTS(SELECT 1 FROM hr.ShiftAssignment sa WHERE sa.UserId=i.UserId AND sa.WorkDate>=@todayVn)
           OR EXISTS(SELECT 1 FROM hr.Attendance a JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=a.ShiftAssignmentId
                     WHERE sa.UserId=i.UserId AND a.CheckInAt IS NOT NULL AND a.CheckOutAt IS NULL)
          )
    ) THROW 51132,N'Không thể chuyển chi nhánh khi nhân viên còn vai trò/ca/lịch đang hoạt động.',1;
END
GO


GO
CREATE   TRIGGER inventory.TR_StockReceipt_ImmutableIdentity ON inventory.StockReceipt AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i JOIN deleted d ON d.StockReceiptId=i.StockReceiptId
              WHERE i.BranchId<>d.BranchId OR i.ReceivedBy<>d.ReceivedBy)
        THROW 51021,'Không được đổi branch/receiver của phiếu nhập lịch sử.',1;
END;
GO


GO
/* ReceivedBy chỉ được xác minh thuộc branch tại thời điểm tạo. Sau đó người này có
   thể chuyển chi nhánh; immutable trigger ở trên vẫn bảo vệ identity lịch sử. */
CREATE   TRIGGER inventory.TR_StockReceipt_ActorBranch
ON inventory.StockReceipt AFTER INSERT AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i LEFT JOIN iam.UserAccount u ON u.UserId=i.ReceivedBy
              LEFT JOIN iam.Role r ON r.RoleId=u.RoleId
              WHERE u.UserId IS NULL OR u.BranchId<>i.BranchId OR u.Status<>'ACTIVE'
                 OR r.Code<>'BRANCH_MANAGER')
        THROW 51146,N'Người nhận phiếu nhập phải là BRANCH_MANAGER active đúng chi nhánh.',1;
END;
GO


GO
CREATE   TRIGGER inventory.TR_WasteEventAudit_ActorBranch
ON inventory.WasteEventAudit AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted i
        OUTER APPLY(SELECT COALESCE(i.WasteEventId,item.WasteEventId) WasteEventId
                    FROM inventory.WasteEventItem item WHERE item.WasteEventItemId=i.WasteEventItemId) ref
        LEFT JOIN inventory.WasteEvent e ON e.WasteEventId=COALESCE(i.WasteEventId,ref.WasteEventId)
        LEFT JOIN iam.UserAccount u ON u.UserId=i.PerformedBy
        LEFT JOIN iam.Role r ON r.RoleId=u.RoleId
        WHERE e.WasteEventId IS NULL OR u.UserId IS NULL OR u.BranchId<>e.BranchId
           OR u.Status<>'ACTIVE' OR r.Code NOT IN('BRANCH_MANAGER','BARISTA')
    ) THROW 51149,N'Người audit hao hụt phải là BRANCH_MANAGER/BARISTA active đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER inventory.TR_WasteEventAudit_ConsistentReference
ON inventory.WasteEventAudit AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted i
        JOIN inventory.WasteEventItem item ON item.WasteEventItemId=i.WasteEventItemId
        WHERE i.WasteEventId IS NOT NULL AND item.WasteEventId<>i.WasteEventId
    ) THROW 51130,N'WasteEventAudit tham chiếu item và event không cùng nguồn.',1;
END
GO


GO
CREATE   TRIGGER inventory.TR_WasteEventItem_ValidateKind
ON inventory.WasteEventItem AFTER INSERT,UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted wi JOIN inventory.WasteEvent e ON e.WasteEventId=wi.WasteEventId
              WHERE e.BranchId<>wi.BranchId
                 OR e.EventKind='REMAKE' AND wi.WasteType<>'REMAKE'
                 OR e.EventKind='INGREDIENT_WASTE' AND wi.WasteType NOT IN('SPILL','EXPIRED','OTHER'))
        THROW 51024,'Waste item không khớp event kind/branch.',1;
END;
GO


GO
CREATE   TRIGGER inventory.TR_WasteEventItem_ActorBranch
ON inventory.WasteEventItem AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i LEFT JOIN iam.UserAccount u ON u.UserId=i.LoggedBy
              LEFT JOIN iam.Role r ON r.RoleId=u.RoleId
              WHERE u.UserId IS NULL OR u.BranchId<>i.BranchId OR u.Status<>'ACTIVE' OR r.Code<>'BARISTA')
        THROW 51148,N'Người ghi dòng hao hụt phải là BARISTA active đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER inventory.TR_StockCount_ActorBranch
ON inventory.StockCount AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i LEFT JOIN iam.UserAccount u ON u.UserId=i.CountedBy
              LEFT JOIN iam.Role r ON r.RoleId=u.RoleId
              WHERE u.UserId IS NULL OR u.BranchId<>i.BranchId OR u.Status<>'ACTIVE' OR r.Code<>'BRANCH_MANAGER')
        THROW 51141,N'Người kiểm kê phải là BRANCH_MANAGER active đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER inventory.TR_StockReceiptDetail_DraftOnly
ON inventory.StockReceiptDetail
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1 FROM inserted i
        JOIN inventory.StockReceipt r ON r.StockReceiptId=i.StockReceiptId
        WHERE r.Status <> 'DRAFT'
    ) OR EXISTS (
        SELECT 1 FROM deleted d
        JOIN inventory.StockReceipt r ON r.StockReceiptId=d.StockReceiptId
        WHERE r.Status <> 'DRAFT'
    )
        THROW 51010, N'Chỉ được sửa chi tiết phiếu nhập khi phiếu còn DRAFT.', 1;
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
               i.ReferenceType='STOCK_RECEIPT' AND EXISTS(
                   SELECT 1 FROM inventory.StockReceipt r WHERE r.StockReceiptId=i.ReferenceId AND r.BranchId=i.BranchId)
            OR i.ReferenceType='ORDER_ITEM' AND EXISTS(
                   SELECT 1 FROM sales.OrderItem oi WHERE oi.OrderItemId=i.ReferenceId AND oi.BranchId=i.BranchId)
            OR i.ReferenceType='PREP_BATCH' AND EXISTS(
                   SELECT 1 FROM inventory.PrepBatch pb WHERE pb.PrepBatchId=i.ReferenceId AND pb.BranchId=i.BranchId)
            OR i.ReferenceType='WASTE_EVENT_ITEM' AND EXISTS(
                   SELECT 1 FROM inventory.WasteEventItem wi WHERE wi.WasteEventItemId=i.ReferenceId AND wi.BranchId=i.BranchId)
            OR i.ReferenceType='STOCK_ADJUSTMENT' AND EXISTS(
                   SELECT 1 FROM inventory.StockAdjustment sa WHERE sa.StockAdjustmentId=i.ReferenceId AND sa.BranchId=i.BranchId)))
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
              LEFT JOIN iam.Role r ON r.RoleId=u.RoleId
              WHERE i.CreatedBy IS NOT NULL AND (u.UserId IS NULL OR u.BranchId<>i.BranchId
                    OR u.Status<>'ACTIVE' OR r.Code NOT IN('BRANCH_MANAGER','BARISTA')))
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
        LEFT JOIN iam.Role makerRole ON makerRole.RoleId=maker.RoleId
        LEFT JOIN iam.Role reviewerRole ON reviewerRole.RoleId=reviewer.RoleId
        WHERE maker.UserId IS NULL OR maker.BranchId<>i.BranchId OR maker.Status<>'ACTIVE' OR makerRole.Code<>'BARISTA'
           OR (i.ReviewedBy IS NOT NULL AND (reviewer.UserId IS NULL OR reviewer.BranchId<>i.BranchId
               OR reviewer.Status<>'ACTIVE' OR reviewerRole.Code<>'BRANCH_MANAGER'))
    ) THROW 51140,N'Người pha phải là BARISTA và người duyệt phải là BRANCH_MANAGER active đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER inventory.TR_StockAdjustment_ActorBranch
ON inventory.StockAdjustment AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i LEFT JOIN iam.UserAccount u ON u.UserId=i.AdjustedBy
              LEFT JOIN iam.Role r ON r.RoleId=u.RoleId
              WHERE u.UserId IS NULL OR u.BranchId<>i.BranchId OR u.Status<>'ACTIVE'
                 OR r.Code NOT IN('BRANCH_MANAGER','BARISTA'))
        THROW 51142,N'Người điều chỉnh tồn phải là BRANCH_MANAGER/BARISTA active đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER inventory.TR_WasteEvent_ValidateOrder
ON inventory.WasteEvent AFTER INSERT,UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted e
              LEFT JOIN sales.OrderItem oi ON oi.OrderItemId=e.OrderItemId
              WHERE e.OrderItemId IS NOT NULL
                AND (oi.OrderItemId IS NULL OR oi.BranchId<>e.BranchId
                     OR e.ProductId IS NOT NULL AND oi.ProductId<>e.ProductId))
        THROW 51023,'WasteEvent không khớp order item/product/branch.',1;
    IF EXISTS(SELECT 1 FROM inserted e
              JOIN inventory.WasteEventItem wi ON wi.WasteEventId=e.WasteEventId
              WHERE e.EventKind='REMAKE' AND wi.WasteType<>'REMAKE'
                 OR e.EventKind='INGREDIENT_WASTE'
                    AND wi.WasteType NOT IN('SPILL','EXPIRED','OTHER'))
        THROW 51024,'Waste item không khớp event kind/branch.',1;
END;
GO


GO
CREATE   TRIGGER inventory.TR_WasteEvent_ActorBranch
ON inventory.WasteEvent AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i LEFT JOIN iam.UserAccount u ON u.UserId=i.CreatedBy
              LEFT JOIN iam.Role r ON r.RoleId=u.RoleId
              WHERE u.UserId IS NULL OR u.BranchId<>i.BranchId OR u.Status<>'ACTIVE' OR r.Code<>'BARISTA')
        THROW 51144,N'Người ghi hao hụt phải là BARISTA active đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER inventory.TR_WasteEventReview_ResolverBranch
ON inventory.WasteEventReview AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(
        SELECT 1 FROM inserted i JOIN inventory.WasteEvent e ON e.WasteEventId=i.WasteEventId
        LEFT JOIN iam.UserAccount u ON u.UserId=i.ResolvedBy
        LEFT JOIN iam.Role r ON r.RoleId=u.RoleId
        WHERE i.ResolvedBy IS NOT NULL AND (u.UserId IS NULL OR u.BranchId<>e.BranchId
              OR u.Status<>'ACTIVE' OR r.Code<>'BRANCH_MANAGER')
    ) THROW 51150,N'Người xử lý review hao hụt phải là BRANCH_MANAGER active đúng chi nhánh.',1;
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
        LEFT JOIN iam.Role r ON r.RoleId=u.RoleId
        WHERE b.ManagerUserId IS NOT NULL
          AND (u.UserId IS NULL OR u.BranchId<>b.BranchId OR u.Status<>'ACTIVE' OR r.Code<>'BRANCH_MANAGER')
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
              LEFT JOIN iam.Role r ON r.RoleId=u.RoleId
              WHERE u.UserId IS NULL OR u.BranchId<>i.BranchId OR u.Status<>'ACTIVE' OR r.Code<>'CASHIER')
        THROW 51135,N'CashierShift phải thuộc cashier active của đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER sales.TR_SalesOrder_CreatorBranch
ON sales.SalesOrder AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i LEFT JOIN iam.UserAccount u ON u.UserId=i.CreatedBy
              LEFT JOIN iam.Role r ON r.RoleId=u.RoleId
              WHERE i.CreatedBy IS NOT NULL AND (u.UserId IS NULL OR u.BranchId<>i.BranchId
                    OR u.Status<>'ACTIVE' OR r.Code<>'CASHIER'))
        THROW 51136,N'Người tạo đơn quầy phải là CASHIER active trong đúng chi nhánh.',1;
END
GO


GO
CREATE   TRIGGER sales.TR_TableSession_OpenedByBranch
ON sales.TableSession AFTER INSERT,UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS(SELECT 1 FROM inserted i LEFT JOIN iam.UserAccount u ON u.UserId=i.OpenedBy
              LEFT JOIN iam.Role r ON r.RoleId=u.RoleId
              WHERE i.OpenedBy IS NOT NULL AND (u.UserId IS NULL OR u.BranchId<>i.BranchId
                    OR u.Status<>'ACTIVE' OR r.Code<>'CASHIER'))
        THROW 51138,N'Người mở phiên bàn phải là CASHIER active trong đúng chi nhánh.',1;
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
        LEFT JOIN iam.Role br ON br.RoleId=b.RoleId
        LEFT JOIN iam.Role pr ON pr.RoleId=p.RoleId
        LEFT JOIN iam.Role irr ON irr.RoleId=ir.RoleId
        LEFT JOIN iam.Role pur ON pur.RoleId=pu.RoleId
        WHERE (i.BaristaId IS NOT NULL AND (b.UserId IS NULL OR b.BranchId<>i.BranchId OR b.Status<>'ACTIVE' OR br.Code<>'BARISTA'))
           OR (i.PreparedBy IS NOT NULL AND (p.UserId IS NULL OR p.BranchId<>i.BranchId OR p.Status<>'ACTIVE' OR pr.Code<>'BARISTA'))
           OR (i.IssueReportedBy IS NOT NULL AND (ir.UserId IS NULL OR ir.BranchId<>i.BranchId OR ir.Status<>'ACTIVE' OR irr.Code<>'BARISTA'))
           OR (i.PickedUpBy IS NOT NULL AND (pu.UserId IS NULL OR pu.BranchId<>i.BranchId OR pu.Status<>'ACTIVE' OR pur.Code<>'CASHIER'))
    ) THROW 51137,N'Actor của OrderItem sai role, inactive hoặc khác chi nhánh.',1;
END
GO


GO


GO
ALTER TABLE [catalog].[PrepRecipe] WITH CHECK CHECK CONSTRAINT [FK_PrepRecipe_Ingredient_PreppedTyped];

ALTER TABLE [catalog].[PrepRecipe] WITH CHECK CHECK CONSTRAINT [FK_PrepRecipe_Ingredient_PreppedIngredient];

ALTER TABLE [catalog].[ModifierOption] WITH CHECK CHECK CONSTRAINT [FK_ModifierOption_ModifierGroup];

ALTER TABLE [catalog].[BranchMenu] WITH CHECK CHECK CONSTRAINT [FK_BranchMenu_Product];

ALTER TABLE [catalog].[BranchMenu] WITH CHECK CHECK CONSTRAINT [FK_BranchMenu_Branch];

ALTER TABLE [catalog].[PrepRecipeIngredient] WITH CHECK CHECK CONSTRAINT [FK_PrepRecipeIngredient_PrepRecipe];

ALTER TABLE [catalog].[PrepRecipeIngredient] WITH CHECK CHECK CONSTRAINT [FK_PrepRecipeIngredient_Ingredient_RawTyped];

ALTER TABLE [catalog].[PrepRecipeIngredient] WITH CHECK CHECK CONSTRAINT [FK_PrepRecipeIngredient_Ingredient_RawIngredient];

ALTER TABLE [catalog].[ModifierIngredientImpact] WITH CHECK CHECK CONSTRAINT [FK_ModifierIngredientImpact_Ingredient];

ALTER TABLE [catalog].[ModifierIngredientImpact] WITH CHECK CHECK CONSTRAINT [FK_ModifierIngredientImpact_ModifierOption];

ALTER TABLE [catalog].[ProductModifierGroup] WITH CHECK CHECK CONSTRAINT [FK_ProductModifierGroup_ModifierGroup];

ALTER TABLE [catalog].[ProductModifierGroup] WITH CHECK CHECK CONSTRAINT [FK_ProductModifierGroup_Product];

ALTER TABLE [catalog].[ProductRecipe] WITH CHECK CHECK CONSTRAINT [FK_ProductRecipe_Ingredient];

ALTER TABLE [catalog].[ProductRecipe] WITH CHECK CHECK CONSTRAINT [FK_ProductRecipe_Product];

ALTER TABLE [catalog].[Product] WITH CHECK CHECK CONSTRAINT [FK_Product_Category];

ALTER TABLE [catalog].[MenuBlockRequest] WITH CHECK CHECK CONSTRAINT [FK_MenuBlockRequest_Branch];

ALTER TABLE [catalog].[MenuBlockRequest] WITH CHECK CHECK CONSTRAINT [FK_MenuBlockRequest_UserAccount_ReviewedBy];

ALTER TABLE [catalog].[MenuBlockRequest] WITH CHECK CHECK CONSTRAINT [FK_MenuBlockRequest_UserAccount_RequestedBy];

ALTER TABLE [catalog].[MenuBlockRequest] WITH CHECK CHECK CONSTRAINT [FK_MenuBlockRequest_Product];

ALTER TABLE [catalog].[IngredientUnitConversion] WITH CHECK CHECK CONSTRAINT [FK_IngredientUnitConversion_UserAccount_UpdatedBy];

ALTER TABLE [catalog].[IngredientUnitConversion] WITH CHECK CHECK CONSTRAINT [FK_IngredientUnitConversion_Ingredient];

ALTER TABLE [hr].[ShiftAssignment] WITH CHECK CHECK CONSTRAINT [FK_ShiftAssignment_UserAccount_User];

ALTER TABLE [hr].[ShiftAssignment] WITH CHECK CHECK CONSTRAINT [FK_ShiftAssignment_ShiftTemplate_Branch];

ALTER TABLE [hr].[ShiftAssignment] WITH CHECK CHECK CONSTRAINT [FK_ShiftAssignment_ShiftTemplate];

ALTER TABLE [hr].[ShiftTemplate] WITH CHECK CHECK CONSTRAINT [FK_ShiftTemplate_Branch];

ALTER TABLE [hr].[Attendance] WITH CHECK CHECK CONSTRAINT [FK_Attendance_ShiftAssignment];

ALTER TABLE [hr].[Attendance] WITH CHECK CHECK CONSTRAINT [FK_Attendance_UserAccount_ApprovedBy];

ALTER TABLE [hr].[Payroll] WITH CHECK CHECK CONSTRAINT [FK_Payroll_Branch];

ALTER TABLE [hr].[Payroll] WITH CHECK CHECK CONSTRAINT [FK_Payroll_UserAccount_User];

ALTER TABLE [hr].[Payroll] WITH CHECK CHECK CONSTRAINT [FK_Payroll_UserAccount_UpdatedBy];

ALTER TABLE [iam].[UserAccount] WITH CHECK CHECK CONSTRAINT [FK_UserAccount_Branch];

ALTER TABLE [iam].[UserAccount] WITH CHECK CHECK CONSTRAINT [FK_UserAccount_Role];

ALTER TABLE [inventory].[StockReceipt] WITH CHECK CHECK CONSTRAINT [FK_StockReceipt_UserAccount_ReceivedBy];

ALTER TABLE [inventory].[StockReceipt] WITH CHECK CHECK CONSTRAINT [FK_StockReceipt_Supplier];

ALTER TABLE [inventory].[StockReceipt] WITH CHECK CHECK CONSTRAINT [FK_StockReceipt_Branch];

ALTER TABLE [inventory].[BranchInventory] WITH CHECK CHECK CONSTRAINT [FK_BranchInventory_Ingredient];

ALTER TABLE [inventory].[BranchInventory] WITH CHECK CHECK CONSTRAINT [FK_BranchInventory_Ingredient_PrepTargetTyped];

ALTER TABLE [inventory].[BranchInventory] WITH CHECK CHECK CONSTRAINT [FK_BranchInventory_Branch];

ALTER TABLE [inventory].[WasteEventAudit] WITH CHECK CHECK CONSTRAINT [FK_WasteEventAudit_UserAccount_PerformedBy];

ALTER TABLE [inventory].[WasteEventAudit] WITH CHECK CHECK CONSTRAINT [FK_WasteEventAudit_WasteEvent];

ALTER TABLE [inventory].[WasteEventAudit] WITH CHECK CHECK CONSTRAINT [FK_WasteEventAudit_WasteEventItem_Item];

ALTER TABLE [inventory].[WasteEventItem] WITH CHECK CHECK CONSTRAINT [FK_WasteEventItem_Ingredient];

ALTER TABLE [inventory].[WasteEventItem] WITH CHECK CHECK CONSTRAINT [FK_WasteEventItem_UserAccount_LoggedBy];

ALTER TABLE [inventory].[WasteEventItem] WITH CHECK CHECK CONSTRAINT [FK_WasteEventItem_WasteEvent_Branch];

ALTER TABLE [inventory].[WasteEventItem] WITH CHECK CHECK CONSTRAINT [FK_WasteEventItem_Branch];

ALTER TABLE [inventory].[StockCount] WITH CHECK CHECK CONSTRAINT [FK_StockCount_UserAccount_CountedBy];

ALTER TABLE [inventory].[StockCount] WITH CHECK CHECK CONSTRAINT [FK_StockCount_Branch];

ALTER TABLE [inventory].[StockReceiptDetail] WITH CHECK CHECK CONSTRAINT [FK_StockReceiptDetail_Ingredient];

ALTER TABLE [inventory].[StockReceiptDetail] WITH CHECK CHECK CONSTRAINT [FK_StockReceiptDetail_StockReceipt];

ALTER TABLE [inventory].[StockReceiptDetail] WITH CHECK CHECK CONSTRAINT [FK_StockReceiptDetail_UnitConversionIngredient];

ALTER TABLE [inventory].[StockReceiptDetail] WITH CHECK CHECK CONSTRAINT [FK_StockReceiptDetail_IngredientUnitConversion];

ALTER TABLE [inventory].[InventoryTransaction] WITH CHECK CHECK CONSTRAINT [FK_InventoryTransaction_Ingredient];

ALTER TABLE [inventory].[InventoryTransaction] WITH CHECK CHECK CONSTRAINT [FK_InventoryTransaction_UserAccount_CreatedBy];

ALTER TABLE [inventory].[InventoryTransaction] WITH CHECK CHECK CONSTRAINT [FK_InventoryTransaction_Branch];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [FK_PrepBatch_UserAccount_ReviewedBy];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [FK_PrepBatch_WasteEventItem_WriteOff];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [FK_PrepBatch_Ingredient_PreppedTyped];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [FK_PrepBatch_Ingredient_PreppedIngredient];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [FK_PrepBatch_Branch];

ALTER TABLE [inventory].[PrepBatch] WITH CHECK CHECK CONSTRAINT [FK_PrepBatch_UserAccount_MadeBy];

ALTER TABLE [inventory].[StockAdjustment] WITH CHECK CHECK CONSTRAINT [FK_StockAdjustment_UnitConversionIngredient];

ALTER TABLE [inventory].[StockAdjustment] WITH CHECK CHECK CONSTRAINT [FK_StockAdjustment_Ingredient];

ALTER TABLE [inventory].[StockAdjustment] WITH CHECK CHECK CONSTRAINT [FK_StockAdjustment_IngredientUnitConversion];

ALTER TABLE [inventory].[StockAdjustment] WITH CHECK CHECK CONSTRAINT [FK_StockAdjustment_Branch];

ALTER TABLE [inventory].[StockAdjustment] WITH CHECK CHECK CONSTRAINT [FK_StockAdjustment_StockCount];

ALTER TABLE [inventory].[StockAdjustment] WITH CHECK CHECK CONSTRAINT [FK_StockAdjustment_UserAccount_AdjustedBy];

ALTER TABLE [inventory].[WasteEvent] WITH CHECK CHECK CONSTRAINT [FK_WasteEvent_OrderItem_ProductBranch];

ALTER TABLE [inventory].[WasteEvent] WITH CHECK CHECK CONSTRAINT [FK_WasteEvent_UserAccount_CreatedBy];

ALTER TABLE [inventory].[WasteEvent] WITH CHECK CHECK CONSTRAINT [FK_WasteEvent_ShiftAssignment_Branch];

ALTER TABLE [inventory].[WasteEvent] WITH CHECK CHECK CONSTRAINT [FK_WasteEvent_Branch];

ALTER TABLE [inventory].[WasteEvent] WITH CHECK CHECK CONSTRAINT [FK_WasteEvent_Product];

ALTER TABLE [inventory].[WasteEvent] WITH CHECK CHECK CONSTRAINT [FK_WasteEvent_OrderItem_Branch];

ALTER TABLE [inventory].[WasteEvent] WITH CHECK CHECK CONSTRAINT [FK_WasteEvent_ShiftAssignment];

ALTER TABLE [inventory].[WasteEvent] WITH CHECK CHECK CONSTRAINT [FK_WasteEvent_OrderItem];

ALTER TABLE [inventory].[WasteEventReview] WITH CHECK CHECK CONSTRAINT [FK_WasteEventReview_WasteEvent];

ALTER TABLE [inventory].[WasteEventReview] WITH CHECK CHECK CONSTRAINT [FK_WasteEventReview_UserAccount_ResolvedBy];

ALTER TABLE [inventory].[WasteEventReview] WITH CHECK CHECK CONSTRAINT [FK_WasteEventReview_Ingredient];

ALTER TABLE [ops].[OrderItemActionLog] WITH CHECK CHECK CONSTRAINT [FK_OrderItemActionLog_OrderItem];

ALTER TABLE [ops].[OrderItemActionLog] WITH CHECK CHECK CONSTRAINT [FK_OrderItemActionLog_OrderItem_Branch];

ALTER TABLE [ops].[OrderItemActionLog] WITH CHECK CHECK CONSTRAINT [FK_OrderItemActionLog_Branch];

ALTER TABLE [ops].[OrderItemActionLog] WITH CHECK CHECK CONSTRAINT [FK_OrderItemActionLog_UserAccount_PerformedBy];

ALTER TABLE [ops].[OutboxEvent] WITH CHECK CHECK CONSTRAINT [FK_OutboxEvent_Branch];

ALTER TABLE [org].[Branch] WITH CHECK CHECK CONSTRAINT [FK_Branch_UserAccount_Manager];

ALTER TABLE [payment].[CashierShift] WITH CHECK CHECK CONSTRAINT [FK_CashierShift_Branch];

ALTER TABLE [payment].[CashierShift] WITH CHECK CHECK CONSTRAINT [FK_CashierShift_UserAccount_Cashier];

ALTER TABLE [payment].[Voucher] WITH CHECK CHECK CONSTRAINT [FK_Voucher_Branch];

ALTER TABLE [payment].[BillItem] WITH CHECK CHECK CONSTRAINT [FK_BillItem_Bill];

ALTER TABLE [payment].[BillItem] WITH CHECK CHECK CONSTRAINT [FK_BillItem_Bill_Branch];

ALTER TABLE [payment].[BillItem] WITH CHECK CHECK CONSTRAINT [FK_BillItem_OrderItem];

ALTER TABLE [payment].[BillItem] WITH CHECK CHECK CONSTRAINT [FK_BillItem_OrderItem_Branch];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [FK_Bill_CashierShift];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [FK_Bill_Voucher];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [FK_Bill_CashierShift_Branch];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [FK_Bill_TableSession];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [FK_Bill_Branch];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [FK_Bill_TableSession_Branch];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [FK_SalesOrder_TableSession];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [FK_SalesOrder_TableSession_Branch];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [FK_SalesOrder_UserAccount_CreatedBy];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [FK_SalesOrder_Branch];

ALTER TABLE [sales].[PickupSequence] WITH CHECK CHECK CONSTRAINT [FK_PickupSequence_Branch];

ALTER TABLE [sales].[TableSession] WITH CHECK CHECK CONSTRAINT [FK_TableSession_UserAccount_OpenedBy];

ALTER TABLE [sales].[TableSession] WITH CHECK CHECK CONSTRAINT [FK_TableSession_DiningTable];

ALTER TABLE [sales].[TableSession] WITH CHECK CHECK CONSTRAINT [FK_TableSession_DiningTable_Branch];

ALTER TABLE [sales].[TableSession] WITH CHECK CHECK CONSTRAINT [FK_TableSession_Branch];

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

ALTER TABLE [catalog].[PrepRecipe] WITH CHECK CHECK CONSTRAINT [CK_PrepRecipe_YieldQty];

ALTER TABLE [catalog].[BranchMenu] WITH CHECK CHECK CONSTRAINT [CK_BranchMenu_LocalPrice];

ALTER TABLE [catalog].[HomeSetting] WITH CHECK CHECK CONSTRAINT [CK_HomeSetting_Singleton];

ALTER TABLE [catalog].[PrepRecipeIngredient] WITH CHECK CHECK CONSTRAINT [CK_PrepRecipeIngredient_Quantity];

ALTER TABLE [catalog].[ModifierGroup] WITH CHECK CHECK CONSTRAINT [CK_ModifierGroup_SelectionRange];

ALTER TABLE [catalog].[Ingredient] WITH CHECK CHECK CONSTRAINT [CK_Ingredient_Type];

ALTER TABLE [catalog].[Ingredient] WITH CHECK CHECK CONSTRAINT [CK_Ingredient_ShelfLife];

ALTER TABLE [catalog].[ProductRecipe] WITH CHECK CHECK CONSTRAINT [CK_ProductRecipe_Quantity];

ALTER TABLE [catalog].[Product] WITH CHECK CHECK CONSTRAINT [CK_Product_BasePrice_Value];

ALTER TABLE [catalog].[Product] WITH CHECK CHECK CONSTRAINT [CK_Product_PrepSeconds];

ALTER TABLE [catalog].[MenuBlockRequest] WITH CHECK CHECK CONSTRAINT [CK_MenuBlockRequest_Lifecycle];

ALTER TABLE [catalog].[MenuBlockRequest] WITH CHECK CHECK CONSTRAINT [CK_MenuBlockRequest_Status];

ALTER TABLE [catalog].[MenuBlockRequest] WITH CHECK CHECK CONSTRAINT [CK_MenuBlockRequest_TimeOrder];

ALTER TABLE [catalog].[IngredientUnitConversion] WITH CHECK CHECK CONSTRAINT [CK_IngredientUnitConversion_Factor];

ALTER TABLE [catalog].[IngredientUnitConversion] WITH CHECK CHECK CONSTRAINT [CK_IngredientUnitConversion_Base];

ALTER TABLE [hr].[ShiftTemplate] WITH CHECK CHECK CONSTRAINT [CK_ShiftTemplate_NonZeroDuration];

ALTER TABLE [hr].[Attendance] WITH CHECK CHECK CONSTRAINT [CK_Attendance_Status];

ALTER TABLE [hr].[Attendance] WITH CHECK CHECK CONSTRAINT [CK_Attendance_CheckOutAfterIn];

ALTER TABLE [hr].[Attendance] WITH CHECK CHECK CONSTRAINT [CK_Attendance_ApprovalLifecycle];

ALTER TABLE [hr].[Payroll] WITH CHECK CHECK CONSTRAINT [CK_Payroll_NonNegative];

ALTER TABLE [hr].[Payroll] WITH CHECK CHECK CONSTRAINT [CK_Payroll_MonthStart];

ALTER TABLE [iam].[UserAccount] WITH CHECK CHECK CONSTRAINT [CK_UserAccount_Status];

ALTER TABLE [inventory].[StockReceipt] WITH CHECK CHECK CONSTRAINT [CK_StockReceipt_Status];

ALTER TABLE [inventory].[StockReceipt] WITH CHECK CHECK CONSTRAINT [CK_StockReceipt_TotalCost];

ALTER TABLE [inventory].[BranchInventory] WITH CHECK CHECK CONSTRAINT [CK_BranchInventory_Threshold];

ALTER TABLE [inventory].[BranchInventory] WITH CHECK CHECK CONSTRAINT [CK_BranchInventory_PrepTarget];

ALTER TABLE [inventory].[WasteEventAudit] WITH CHECK CHECK CONSTRAINT [CK_WasteEventAudit_Reference];

ALTER TABLE [inventory].[WasteEventAudit] WITH CHECK CHECK CONSTRAINT [CK_WasteEventAudit_Action];

ALTER TABLE [inventory].[WasteEventItem] WITH CHECK CHECK CONSTRAINT [CK_WasteEventItem_Quantity_Value];

ALTER TABLE [inventory].[WasteEventItem] WITH CHECK CHECK CONSTRAINT [CK_WasteEventItem_UnitCost];

ALTER TABLE [inventory].[WasteEventItem] WITH CHECK CHECK CONSTRAINT [CK_WasteEventItem_Status];

ALTER TABLE [inventory].[WasteEventItem] WITH CHECK CHECK CONSTRAINT [CK_WasteEventItem_Type];

ALTER TABLE [inventory].[WasteEventItem] WITH CHECK CHECK CONSTRAINT [CK_WasteEventItem_CostBasis];

ALTER TABLE [inventory].[StockReceiptDetail] WITH CHECK CHECK CONSTRAINT [CK_StockReceiptDetail_EnteredQuantity];

ALTER TABLE [inventory].[StockReceiptDetail] WITH CHECK CHECK CONSTRAINT [CK_StockReceiptDetail_UnitCost];

ALTER TABLE [inventory].[StockReceiptDetail] WITH CHECK CHECK CONSTRAINT [CK_StockReceiptDetail_BasePrecision];

ALTER TABLE [inventory].[StockReceiptDetail] WITH CHECK CHECK CONSTRAINT [CK_StockReceiptDetail_FactorToBaseAtEntry];

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

ALTER TABLE [inventory].[WasteEvent] WITH CHECK CHECK CONSTRAINT [CK_WasteEvent_Kind];

ALTER TABLE [inventory].[WasteEvent] WITH CHECK CHECK CONSTRAINT [CK_WasteEvent_Remake];

ALTER TABLE [inventory].[WasteEvent] WITH CHECK CHECK CONSTRAINT [CK_WasteEvent_Source];

ALTER TABLE [inventory].[WasteEventReview] WITH CHECK CHECK CONSTRAINT [CK_WasteEventReview_Lifecycle];

ALTER TABLE [inventory].[WasteEventReview] WITH CHECK CHECK CONSTRAINT [CK_WasteEventReview_Status];

ALTER TABLE [inventory].[WasteEventReview] WITH CHECK CHECK CONSTRAINT [CK_WasteEventReview_Type];

ALTER TABLE [ops].[OrderItemActionLog] WITH CHECK CHECK CONSTRAINT [CK_OrderItemActionLog_ActionType];

ALTER TABLE [ops].[OutboxEvent] WITH CHECK CHECK CONSTRAINT [CK_OutboxEvent_PayloadJson];

ALTER TABLE [ops].[MenuBlockTimestampArchive] WITH CHECK CHECK CONSTRAINT [CK_MenuBlockTimestampArchive_ReviewStatus];

ALTER TABLE [org].[Branch] WITH CHECK CHECK CONSTRAINT [CK_Branch_PeakThreshold];

ALTER TABLE [payment].[CashierShift] WITH CHECK CHECK CONSTRAINT [CK_CashierShift_Money];

ALTER TABLE [payment].[CashierShift] WITH CHECK CHECK CONSTRAINT [CK_CashierShift_CloseState];

ALTER TABLE [payment].[Voucher] WITH CHECK CHECK CONSTRAINT [CK_Voucher_Usage];

ALTER TABLE [payment].[Voucher] WITH CHECK CHECK CONSTRAINT [CK_Voucher_DiscountValue];

ALTER TABLE [payment].[Voucher] WITH CHECK CHECK CONSTRAINT [CK_Voucher_Scope];

ALTER TABLE [payment].[Voucher] WITH CHECK CHECK CONSTRAINT [CK_Voucher_UtcRange];

ALTER TABLE [payment].[Voucher] WITH CHECK CHECK CONSTRAINT [CK_Voucher_Type];

ALTER TABLE [payment].[Voucher] WITH CHECK CHECK CONSTRAINT [CK_Voucher_BranchScope];

ALTER TABLE [payment].[Voucher] WITH CHECK CHECK CONSTRAINT [CK_Voucher_Value];

ALTER TABLE [payment].[BillItem] WITH CHECK CHECK CONSTRAINT [CK_BillItem_Amount];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [CK_Bill_CashSettlement];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [CK_Bill_PaymentMethod];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [CK_Bill_Amounts];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [CK_Bill_SettlementAmounts];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [CK_Bill_Status];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [CK_Bill_PaidHasAmount];

ALTER TABLE [payment].[Bill] WITH CHECK CHECK CONSTRAINT [CK_Bill_PaymentLifecycle];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [CK_SalesOrder_Source];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [CK_SalesOrder_Status];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [CK_SalesOrder_TypeSession];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [CK_SalesOrder_Type];

ALTER TABLE [sales].[SalesOrder] WITH CHECK CHECK CONSTRAINT [CK_SalesOrder_SourceCreator];

ALTER TABLE [sales].[PickupSequence] WITH CHECK CHECK CONSTRAINT [CK_PickupSequence_NextValue];

ALTER TABLE [sales].[TableSession] WITH CHECK CHECK CONSTRAINT [CK_TableSession_Lifecycle];

ALTER TABLE [sales].[TableSession] WITH CHECK CHECK CONSTRAINT [CK_TableSession_Status];

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
INSERT catalog.HomeSetting(HomeSettingId,HeroEyebrow,HeroTitle,HeroSubtitle,HeroImageUrl)
VALUES(1,N'Chuỗi cà phê thủ công',N'Thực đơn của Cà Phê Chain',
       N'Khám phá menu cà phê, trà và đá xay được pha chế tươi mỗi ngày.',
       '/assets/img/login-hero.svg');

INSERT ops.LegacySchemaVersion(VersionCode,Description) VALUES
('20260722_attendance_integrity',N'Bảo đảm một bản ghi chấm công cho mỗi ca.'),
('20260722_waste_event_controls',N'Bổ sung event, audit và đối soát hao hụt/remake.'),
('20260725_prep_batch_writeoff',N'Đánh dấu mẻ pha sẵn quá hạn đã ghi hao hụt.'),
('20260726_prep_worklist',N'Mức tồn mục tiêu, hạn dùng và idempotency cho mẻ pha sẵn.'),
('20260729_prep_approval',N'Duyệt mẻ pha bất thường trước khi cộng tồn PREPPED.'),
('20260731_critical_integrity',N'Bảo vệ transaction và quan hệ xuyên chi nhánh.'),
('20260731_integrity_v2',N'Branch integrity, lifecycle và actor validation.'),
('20260731_integrity_v3',N'Siết reviewer và chuỗi timestamp OrderItem.'),
('20260731_menublock_utc',N'MenuBlockRequest ghi timestamp UTC.'),
('20260731_menublock_utc_v2',N'Archive timestamp legacy mơ hồ.'),
('20260731_naming_v2',N'Chuẩn hóa tên bảng, cột và constraint.'),
('20260731_pickup_sequence_v1',N'Atomic pickup sequence theo ngày kinh doanh.'),
('20260731_prep_recipe_v2',N'Chuẩn hóa PrepRecipe header/detail.'),
('20260731_prep_type_guard',N'Bảo vệ kiểu RAW/PREPPED.'),
('20260731_stock_count_header',N'Nhóm các dòng kiểm kê trong StockCount.'),
('20260731_time_order_checks',N'Chặn giờ công và lead time âm.');

INSERT iam.Role(Code,Name) VALUES
('ADMIN',N'Quản trị hệ thống'),
('BRANCH_MANAGER',N'Quản lý chi nhánh'),
('CASHIER',N'Thu ngân'),
('BARISTA',N'Pha chế');

INSERT org.Branch(Code,Name,Address,Phone,OpenTime,CloseTime,PeakThresholdCups)
VALUES('CN01',N'Chi nhánh Demo',N'123 Lê Lợi, Quận 1, TP.HCM','0900000001','07:00','22:00',20);

DECLARE @DemoBranchId INT=(SELECT BranchId FROM org.Branch WHERE Code='CN01');
DECLARE @DemoHash VARCHAR(255)='$2a$10$BFdZOEu0.X9/U6Yme03Z.ec6H/lsprcbJavmdUw3B4O51T82onwGa';

INSERT iam.UserAccount(Username,PasswordHash,FullName,Email,Phone,RoleId,BranchId,Status) VALUES
('admin',@DemoHash,N'Quản trị viên','admin@cafe.local','0900000010',
 (SELECT RoleId FROM iam.Role WHERE Code='ADMIN'),NULL,'ACTIVE'),
('manager1',@DemoHash,N'Quản lý Demo','manager@cafe.local','0900000011',
 (SELECT RoleId FROM iam.Role WHERE Code='BRANCH_MANAGER'),@DemoBranchId,'ACTIVE'),
('cashier1',@DemoHash,N'Thu ngân Demo','cashier@cafe.local','0900000012',
 (SELECT RoleId FROM iam.Role WHERE Code='CASHIER'),@DemoBranchId,'ACTIVE'),
('barista1',@DemoHash,N'Pha chế Demo','barista@cafe.local','0900000013',
 (SELECT RoleId FROM iam.Role WHERE Code='BARISTA'),@DemoBranchId,'ACTIVE');

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

INSERT catalog.ProductRecipe(ProductId,IngredientId,Quantity)
SELECT p.ProductId,i.IngredientId,v.Quantity
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
