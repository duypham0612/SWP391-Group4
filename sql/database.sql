/* ============================================================================
   HỆ THỐNG QUẢN LÝ CHUỖI CAFE  —  DATABASE (SQL Server)
   SWP391 · 4 Role (Admin / Branch Manager / Cashier / Barista) · Dine-in
   ----------------------------------------------------------------------------
   NGUYÊN TẮC THIẾT KẾ (bám 3 contract đã chốt):
   1) EVENT BUS + STATUS ENUM CHUNG:
        - ops.OutboxEvent ghi mọi domain event: order.created,
          order.status_changed, payment.completed, inventory.deducted, stock.low.
        - Trạng thái item (KDS/khách) dùng CHUNG ở sales.OrderItem.Status
          (WAITING -> MAKING -> READY('Sẵn lấy') -> SERVED).
   2) Cờ RAW / PREPPED:
        - catalog.Ingredient.IngredientType = 'RAW' | 'PREPPED'.
        - PREPPED (cold brew, syrup...) được tạo từ RAW qua inventory.PrepBatch.
        - Khi pha xong: món dùng RAW -> trừ thẳng tồn; món dùng PREPPED -> trừ
          tồn PREPPED (RAW đã bị trừ lúc làm batch) => KHÔNG trừ thô 2 lần.
   3) CASHIER SỞ HỮU MỌI ORDER ENTRY:
        - sales.Orders.Source = 'COUNTER' | 'QR' nhưng cùng 1 bảng/backend.

   XƯƠNG SỐNG DINE-IN: sales.TableSession (tab theo bàn). Không có giao hàng.
   SỔ CÁI TỒN KHO: inventory.InventoryTransaction là nguồn sự thật duy nhất cho
   mọi thay đổi tồn (nhập / trừ khi pha / hao hụt / prep / điều chỉnh);
   inventory.BranchInventory.QuantityOnHand chỉ là số dư cache.
   ============================================================================ */

IF DB_ID('CafeChain') IS NULL CREATE DATABASE CafeChain;
GO
USE CafeChain;
GO

/* ---------------------------------------------------------------------------
   TEARDOWN (để chạy lại script trên DB đang phát triển) — thứ tự con -> cha
   --------------------------------------------------------------------------- */
IF OBJECT_ID('org.FK_Branch_Manager','F') IS NOT NULL
    ALTER TABLE org.Branch DROP CONSTRAINT FK_Branch_Manager;
GO
DROP VIEW IF EXISTS sales.vw_KdsQueue;
DROP VIEW IF EXISTS inventory.vw_LowStock;
GO
DROP TABLE IF EXISTS payment.VoucherRedemption;
DROP TABLE IF EXISTS payment.BillItem;
DROP TABLE IF EXISTS payment.Bill;
DROP TABLE IF EXISTS payment.CashierShift;
DROP TABLE IF EXISTS payment.Voucher;
DROP TABLE IF EXISTS sales.OrderItemModifier;
DROP TABLE IF EXISTS sales.OrderItem;
DROP TABLE IF EXISTS sales.Orders;
DROP TABLE IF EXISTS sales.TableSession;
DROP TABLE IF EXISTS sales.DiningTable;
DROP TABLE IF EXISTS inventory.InventoryTransaction;
DROP TABLE IF EXISTS inventory.StockAdjustment;
DROP TABLE IF EXISTS inventory.WasteLog;
DROP TABLE IF EXISTS inventory.PrepBatch;
DROP TABLE IF EXISTS inventory.StockReceiptDetail;
DROP TABLE IF EXISTS inventory.StockReceipt;
DROP TABLE IF EXISTS inventory.BranchInventory;
DROP TABLE IF EXISTS inventory.Supplier;
DROP TABLE IF EXISTS hr.Attendance;
DROP TABLE IF EXISTS hr.ShiftAssignment;
DROP TABLE IF EXISTS hr.ShiftTemplate;
DROP TABLE IF EXISTS catalog.BranchMenu;
DROP TABLE IF EXISTS catalog.ModifierIngredientImpact;
DROP TABLE IF EXISTS catalog.ProductModifierGroup;
DROP TABLE IF EXISTS catalog.ModifierOption;
DROP TABLE IF EXISTS catalog.ModifierGroup;
DROP TABLE IF EXISTS catalog.PrepRecipe;
DROP TABLE IF EXISTS catalog.ProductRecipe;
DROP TABLE IF EXISTS catalog.Product;
DROP TABLE IF EXISTS catalog.Category;
DROP TABLE IF EXISTS catalog.Ingredient;
DROP TABLE IF EXISTS ops.OutboxEvent;
DROP TABLE IF EXISTS iam.RefreshToken;
DROP TABLE IF EXISTS iam.Customer;
DROP TABLE IF EXISTS iam.[User];
DROP TABLE IF EXISTS org.Branch;
DROP TABLE IF EXISTS iam.Role;
GO

/* ---------------------------------------------------------------------------
   SCHEMAS (nhóm theo domain, ánh xạ các module)
   --------------------------------------------------------------------------- */
IF SCHEMA_ID('iam')       IS NULL EXEC('CREATE SCHEMA iam');        -- xác thực & phân quyền (Admin)
GO
IF SCHEMA_ID('org')       IS NULL EXEC('CREATE SCHEMA org');        -- chi nhánh (Admin)
GO
IF SCHEMA_ID('catalog')   IS NULL EXEC('CREATE SCHEMA catalog');    -- menu, công thức, modifier (Admin)
GO
IF SCHEMA_ID('inventory') IS NULL EXEC('CREATE SCHEMA inventory');  -- kho (Manager + Barista)
GO
IF SCHEMA_ID('hr')        IS NULL EXEC('CREATE SCHEMA hr');         -- nhân sự (Manager)
GO
IF SCHEMA_ID('sales')     IS NULL EXEC('CREATE SCHEMA sales');      -- bàn, đơn (Cashier + Barista)
GO
IF SCHEMA_ID('payment')   IS NULL EXEC('CREATE SCHEMA payment');    -- thanh toán, voucher (Cashier + Admin)
GO
IF SCHEMA_ID('ops')       IS NULL EXEC('CREATE SCHEMA ops');        -- event bus / outbox
GO

/* ===========================================================================
   1. IAM + ORG  (Admin)
   =========================================================================== */
CREATE TABLE iam.Role (
    RoleId      INT IDENTITY PRIMARY KEY,
    Code        VARCHAR(30)  NOT NULL UNIQUE,   -- ADMIN / BRANCH_MANAGER / CASHIER / BARISTA
    Name        NVARCHAR(80) NOT NULL
);
GO

-- Tạo Branch trước (chưa gắn FK Manager để tránh vòng phụ thuộc với iam.User)
CREATE TABLE org.Branch (
    BranchId        INT IDENTITY PRIMARY KEY,
    Code            VARCHAR(20)   NOT NULL UNIQUE,
    Name            NVARCHAR(150) NOT NULL,
    Address         NVARCHAR(255) NULL,
    Phone           VARCHAR(20)   NULL,
    OpenTime        TIME          NULL,
    CloseTime       TIME          NULL,
    ManagerUserId   INT           NULL,          -- FK thêm sau (ALTER)
    IsActive        BIT           NOT NULL DEFAULT 1,
    CreatedAt       DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()
);
GO

CREATE TABLE iam.[User] (
    UserId        INT IDENTITY PRIMARY KEY,
    Username      VARCHAR(60)   NOT NULL UNIQUE,
    PasswordHash  VARCHAR(255)  NOT NULL,        -- Bcrypt/Argon2
    FullName      NVARCHAR(120) NOT NULL,
    Email         VARCHAR(120)  NULL,
    Phone         VARCHAR(20)   NULL,
    RoleId        INT           NOT NULL,
    BranchId      INT           NULL,            -- NULL với Admin (toàn chuỗi)
    Status        VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE'
                  CONSTRAINT CK_User_Status CHECK (Status IN ('ACTIVE','LOCKED')),
    CreatedAt     DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_User_Role   FOREIGN KEY (RoleId)   REFERENCES iam.Role(RoleId),
    CONSTRAINT FK_User_Branch FOREIGN KEY (BranchId) REFERENCES org.Branch(BranchId)
);
GO

-- Giờ mới gắn FK Branch.Manager -> User
ALTER TABLE org.Branch
    ADD CONSTRAINT FK_Branch_Manager FOREIGN KEY (ManagerUserId) REFERENCES iam.[User](UserId);
GO

CREATE TABLE iam.RefreshToken (
    TokenId     BIGINT IDENTITY PRIMARY KEY,
    UserId      INT          NOT NULL,
    TokenHash   VARCHAR(255) NOT NULL,
    ExpiresAt   DATETIME2    NOT NULL,
    RevokedAt   DATETIME2    NULL,
    CreatedAt   DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_RefreshToken_User FOREIGN KEY (UserId) REFERENCES iam.[User](UserId)
);
GO

-- Khách hàng: tạo nhanh tại quầy (SĐT) hoặc định danh phiên QR. Dine-in nên có thể ẩn danh.
CREATE TABLE iam.Customer (
    CustomerId  INT IDENTITY PRIMARY KEY,
    Phone       VARCHAR(20)   NULL UNIQUE,
    FullName    NVARCHAR(120) NULL,
    CreatedAt   DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()
);
GO

/* ===========================================================================
   2. CATALOG: danh mục, sản phẩm, nguyên liệu, công thức, modifier  (Admin)
   =========================================================================== */
CREATE TABLE catalog.Category (
    CategoryId  INT IDENTITY PRIMARY KEY,
    Name        NVARCHAR(100) NOT NULL,
    SortOrder   INT NOT NULL DEFAULT 0,
    IsActive    BIT NOT NULL DEFAULT 1
);
GO

CREATE TABLE catalog.Product (
    ProductId   INT IDENTITY PRIMARY KEY,
    CategoryId  INT           NOT NULL,
    Name        NVARCHAR(150) NOT NULL,
    BasePrice   DECIMAL(12,2) NOT NULL CHECK (BasePrice >= 0),
    ImageUrl    VARCHAR(255)  NULL,
    IsActive    BIT NOT NULL DEFAULT 1,
    CONSTRAINT FK_Product_Category FOREIGN KEY (CategoryId) REFERENCES catalog.Category(CategoryId)
);
GO

-- Nguyên liệu / vật tư kho. RAW = mua về; PREPPED = pha sẵn tại quán từ RAW.
CREATE TABLE catalog.Ingredient (
    IngredientId    INT IDENTITY PRIMARY KEY,
    Name            NVARCHAR(120) NOT NULL,
    Unit            NVARCHAR(20)  NOT NULL,      -- g, ml, cái, kg, L...
    IngredientType  VARCHAR(10)   NOT NULL
                    CONSTRAINT CK_Ingredient_Type CHECK (IngredientType IN ('RAW','PREPPED')),
    IsActive        BIT NOT NULL DEFAULT 1
);
GO

-- Công thức món: 1 Product cần N Ingredient (RAW hoặc PREPPED)
CREATE TABLE catalog.ProductRecipe (
    ProductRecipeId INT IDENTITY PRIMARY KEY,
    ProductId       INT NOT NULL,
    IngredientId    INT NOT NULL,
    Quantity        DECIMAL(12,3) NOT NULL CHECK (Quantity > 0),
    CONSTRAINT FK_PR_Product    FOREIGN KEY (ProductId)    REFERENCES catalog.Product(ProductId),
    CONSTRAINT FK_PR_Ingredient FOREIGN KEY (IngredientId) REFERENCES catalog.Ingredient(IngredientId),
    CONSTRAINT UQ_PR UNIQUE (ProductId, IngredientId)
);
GO

-- Công thức pha sẵn: 1 PREPPED được tạo từ N RAW, kèm sản lượng (yield)
CREATE TABLE catalog.PrepRecipe (
    PrepRecipeId        INT IDENTITY PRIMARY KEY,
    PreppedIngredientId INT NOT NULL,            -- phải là Ingredient type PREPPED
    RawIngredientId     INT NOT NULL,            -- phải là Ingredient type RAW
    Quantity            DECIMAL(12,3) NOT NULL CHECK (Quantity > 0),
    YieldQty            DECIMAL(12,3) NOT NULL CHECK (YieldQty > 0),  -- 1 mẻ ra bao nhiêu PREPPED
    CONSTRAINT FK_PrepR_Prepped FOREIGN KEY (PreppedIngredientId) REFERENCES catalog.Ingredient(IngredientId),
    CONSTRAINT FK_PrepR_Raw     FOREIGN KEY (RawIngredientId)     REFERENCES catalog.Ingredient(IngredientId),
    CONSTRAINT UQ_PrepR UNIQUE (PreppedIngredientId, RawIngredientId)
);
GO

-- Nhóm modifier (Size, Đường, Sữa, Topping...)
CREATE TABLE catalog.ModifierGroup (
    ModifierGroupId INT IDENTITY PRIMARY KEY,
    Name            NVARCHAR(80) NOT NULL,
    IsRequired      BIT NOT NULL DEFAULT 0,
    MinSelect       INT NOT NULL DEFAULT 0,
    MaxSelect       INT NOT NULL DEFAULT 1
);
GO

CREATE TABLE catalog.ModifierOption (
    ModifierOptionId INT IDENTITY PRIMARY KEY,
    ModifierGroupId  INT NOT NULL,
    Name             NVARCHAR(80)  NOT NULL,     -- Size L, Oat milk, Extra shot...
    PriceDelta       DECIMAL(12,2) NOT NULL DEFAULT 0,
    IsActive         BIT NOT NULL DEFAULT 1,
    CONSTRAINT FK_MO_Group FOREIGN KEY (ModifierGroupId) REFERENCES catalog.ModifierGroup(ModifierGroupId)
);
GO

-- Gắn nhóm modifier nào áp dụng cho product nào
CREATE TABLE catalog.ProductModifierGroup (
    ProductId       INT NOT NULL,
    ModifierGroupId INT NOT NULL,
    CONSTRAINT PK_PMG PRIMARY KEY (ProductId, ModifierGroupId),
    CONSTRAINT FK_PMG_Product FOREIGN KEY (ProductId)       REFERENCES catalog.Product(ProductId),
    CONSTRAINT FK_PMG_Group   FOREIGN KEY (ModifierGroupId) REFERENCES catalog.ModifierGroup(ModifierGroupId)
);
GO

-- Ảnh hưởng của 1 option lên định mức nguyên liệu (QtyDelta dương: thêm; âm: bớt/đổi)
CREATE TABLE catalog.ModifierIngredientImpact (
    ImpactId         INT IDENTITY PRIMARY KEY,
    ModifierOptionId INT NOT NULL,
    IngredientId     INT NOT NULL,
    QtyDelta         DECIMAL(12,3) NOT NULL,     -- +18g cà phê (extra shot), -200ml sữa bò + 200ml oat...
    CONSTRAINT FK_MII_Option     FOREIGN KEY (ModifierOptionId) REFERENCES catalog.ModifierOption(ModifierOptionId),
    CONSTRAINT FK_MII_Ingredient FOREIGN KEY (IngredientId)     REFERENCES catalog.Ingredient(IngredientId),
    CONSTRAINT UQ_MII UNIQUE (ModifierOptionId, IngredientId)
);
GO

-- Menu theo chi nhánh: bật/tắt, giá local, cờ 86 (hết món)
CREATE TABLE catalog.BranchMenu (
    BranchId    INT NOT NULL,
    ProductId   INT NOT NULL,
    IsAvailable BIT NOT NULL DEFAULT 1,
    LocalPrice  DECIMAL(12,2) NULL,              -- NULL = dùng BasePrice
    Is86        BIT NOT NULL DEFAULT 0,          -- hết tạm thời (Barista toggle)
    BackInEta   DATETIME2 NULL,
    CONSTRAINT PK_BranchMenu PRIMARY KEY (BranchId, ProductId),
    CONSTRAINT FK_BM_Branch  FOREIGN KEY (BranchId)  REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_BM_Product FOREIGN KEY (ProductId) REFERENCES catalog.Product(ProductId)
);
GO

/* ===========================================================================
   3. INVENTORY: kho theo chi nhánh  (Manager nhập/đối soát; Barista trừ/prep/waste)
   =========================================================================== */
CREATE TABLE inventory.Supplier (
    SupplierId  INT IDENTITY PRIMARY KEY,
    Name        NVARCHAR(150) NOT NULL,
    Phone       VARCHAR(20)   NULL,
    Address     NVARCHAR(255) NULL,
    IsActive    BIT NOT NULL DEFAULT 1
);
GO

-- Tồn hiện tại (số dư cache). Nguồn sự thật là InventoryTransaction.
CREATE TABLE inventory.BranchInventory (
    BranchId       INT NOT NULL,
    IngredientId   INT NOT NULL,
    QuantityOnHand DECIMAL(12,3) NOT NULL DEFAULT 0,
    MinThreshold   DECIMAL(12,3) NOT NULL DEFAULT 0,   -- ngưỡng cảnh báo min-stock
    UpdatedAt      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_BranchInventory PRIMARY KEY (BranchId, IngredientId),
    CONSTRAINT FK_BI_Branch     FOREIGN KEY (BranchId)     REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_BI_Ingredient FOREIGN KEY (IngredientId) REFERENCES catalog.Ingredient(IngredientId)
);
GO

-- Phiếu nhập kho (cộng tồn) — Manager
CREATE TABLE inventory.StockReceipt (
    StockReceiptId INT IDENTITY PRIMARY KEY,
    BranchId       INT NOT NULL,
    SupplierId     INT NULL,
    ReceivedBy     INT NOT NULL,                 -- User (Manager)
    ReceiptDate    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    Status         VARCHAR(12) NOT NULL DEFAULT 'DRAFT'
                   CONSTRAINT CK_Receipt_Status CHECK (Status IN ('DRAFT','CONFIRMED','CANCELLED')),
    TotalCost      DECIMAL(14,2) NOT NULL DEFAULT 0,
    Note           NVARCHAR(255) NULL,
    CONSTRAINT FK_SR_Branch   FOREIGN KEY (BranchId)   REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_SR_Supplier FOREIGN KEY (SupplierId) REFERENCES inventory.Supplier(SupplierId),
    CONSTRAINT FK_SR_User     FOREIGN KEY (ReceivedBy) REFERENCES iam.[User](UserId)
);
GO

CREATE TABLE inventory.StockReceiptDetail (
    StockReceiptDetailId INT IDENTITY PRIMARY KEY,
    StockReceiptId       INT NOT NULL,
    IngredientId         INT NOT NULL,
    Quantity             DECIMAL(12,3) NOT NULL CHECK (Quantity > 0),
    UnitCost             DECIMAL(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT FK_SRD_Receipt    FOREIGN KEY (StockReceiptId) REFERENCES inventory.StockReceipt(StockReceiptId) ON DELETE CASCADE,
    CONSTRAINT FK_SRD_Ingredient FOREIGN KEY (IngredientId)   REFERENCES catalog.Ingredient(IngredientId)
);
GO

-- Mẻ pha sẵn (Barista) — trừ RAW, cộng PREPPED
CREATE TABLE inventory.PrepBatch (
    PrepBatchId         INT IDENTITY PRIMARY KEY,
    BranchId            INT NOT NULL,
    PreppedIngredientId INT NOT NULL,            -- Ingredient type PREPPED
    QuantityProduced    DECIMAL(12,3) NOT NULL CHECK (QuantityProduced > 0),
    MadeBy              INT NOT NULL,            -- User (Barista)
    MadeAt              DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ExpiresAt           DATETIME2 NULL,
    Status              VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'   -- huỷ mẻ = ghi txn bù + đánh dấu CANCELLED (không hard-delete)
                        CONSTRAINT CK_PrepBatch_Status CHECK (Status IN ('ACTIVE','CANCELLED')),
    VoidedAt            DATETIME2 NULL,
    CONSTRAINT FK_PB_Branch  FOREIGN KEY (BranchId)            REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_PB_Prepped FOREIGN KEY (PreppedIngredientId) REFERENCES catalog.Ingredient(IngredientId),
    CONSTRAINT FK_PB_User    FOREIGN KEY (MadeBy)              REFERENCES iam.[User](UserId)
);
GO

-- Hao hụt / làm lại (Barista) — trừ tồn, báo Manager
CREATE TABLE inventory.WasteLog (
    WasteLogId   INT IDENTITY PRIMARY KEY,
    BranchId     INT NOT NULL,
    IngredientId INT NOT NULL,
    Quantity     DECIMAL(12,3) NOT NULL CHECK (Quantity > 0),
    WasteType    VARCHAR(12) NOT NULL
                 CONSTRAINT CK_Waste_Type CHECK (WasteType IN ('SPILL','EXPIRED','REMAKE','OTHER')),
    Reason       NVARCHAR(255) NULL,
    LoggedBy     INT NOT NULL,
    LoggedAt     DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    Status       VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'   -- huỷ = ghi txn bù (hoàn kho) + đánh dấu VOIDED (không hard-delete)
                 CONSTRAINT CK_WasteLog_Status CHECK (Status IN ('ACTIVE','VOIDED')),
    VoidedAt     DATETIME2 NULL,
    CONSTRAINT FK_WL_Branch     FOREIGN KEY (BranchId)     REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_WL_Ingredient FOREIGN KEY (IngredientId) REFERENCES catalog.Ingredient(IngredientId),
    CONSTRAINT FK_WL_User       FOREIGN KEY (LoggedBy)     REFERENCES iam.[User](UserId)
);
GO

-- Điều chỉnh tồn sau kiểm kê / đối soát (Manager)
CREATE TABLE inventory.StockAdjustment (
    StockAdjustmentId INT IDENTITY PRIMARY KEY,
    BranchId          INT NOT NULL,
    IngredientId      INT NOT NULL,
    SystemQty         DECIMAL(12,3) NOT NULL,
    ActualQty         DECIMAL(12,3) NOT NULL,
    DiffQty           AS (ActualQty - SystemQty) PERSISTED,
    Reason            NVARCHAR(255) NULL,
    AdjustedBy        INT NOT NULL,
    AdjustedAt        DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_SA_Branch     FOREIGN KEY (BranchId)     REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_SA_Ingredient FOREIGN KEY (IngredientId) REFERENCES catalog.Ingredient(IngredientId),
    CONSTRAINT FK_SA_User       FOREIGN KEY (AdjustedBy)   REFERENCES iam.[User](UserId)
);
GO

-- SỔ CÁI TỒN KHO: mọi thay đổi tồn đi qua đây (nguồn sự thật duy nhất)
CREATE TABLE inventory.InventoryTransaction (
    InventoryTxnId BIGINT IDENTITY PRIMARY KEY,
    BranchId       INT NOT NULL,
    IngredientId   INT NOT NULL,
    ChangeQty      DECIMAL(12,3) NOT NULL,       -- dương: cộng; âm: trừ
    TxnType        VARCHAR(12) NOT NULL
                   CONSTRAINT CK_Txn_Type CHECK (TxnType IN
                   ('RECEIPT','DEDUCT','WASTE','PREP_IN','PREP_OUT','ADJUST')),
    RefTable       VARCHAR(40) NULL,             -- bảng nguồn (StockReceipt, OrderItem, PrepBatch...)
    RefId          BIGINT      NULL,             -- id bản ghi nguồn
    CreatedBy      INT         NULL,
    CreatedAt      DATETIME2   NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_IT_Branch     FOREIGN KEY (BranchId)     REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_IT_Ingredient FOREIGN KEY (IngredientId) REFERENCES catalog.Ingredient(IngredientId),
    CONSTRAINT FK_IT_User       FOREIGN KEY (CreatedBy)    REFERENCES iam.[User](UserId)
);
GO

/* ===========================================================================
   4. HR: ca làm & chấm công  (Manager)
   =========================================================================== */
CREATE TABLE hr.ShiftTemplate (
    ShiftTemplateId INT IDENTITY PRIMARY KEY,
    BranchId        INT NOT NULL,
    Name            NVARCHAR(60) NOT NULL,       -- Ca sáng / chiều / tối
    StartTime       TIME NOT NULL,
    EndTime         TIME NOT NULL,
    CONSTRAINT FK_ST_Branch FOREIGN KEY (BranchId) REFERENCES org.Branch(BranchId)
);
GO

CREATE TABLE hr.ShiftAssignment (
    ShiftAssignmentId INT IDENTITY PRIMARY KEY,
    ShiftTemplateId   INT  NOT NULL,
    UserId            INT  NOT NULL,
    WorkDate          DATE NOT NULL,
    CONSTRAINT FK_SAsg_Shift FOREIGN KEY (ShiftTemplateId) REFERENCES hr.ShiftTemplate(ShiftTemplateId),
    CONSTRAINT FK_SAsg_User  FOREIGN KEY (UserId)          REFERENCES iam.[User](UserId),
    CONSTRAINT UQ_SAsg UNIQUE (ShiftTemplateId, UserId, WorkDate)   -- chặn trùng ca
);
GO

CREATE TABLE hr.Attendance (
    AttendanceId      INT IDENTITY PRIMARY KEY,
    ShiftAssignmentId INT NOT NULL,
    CheckInAt         DATETIME2 NULL,
    CheckOutAt        DATETIME2 NULL,
    Status            VARCHAR(10) NOT NULL DEFAULT 'PENDING'
                      CONSTRAINT CK_Att_Status CHECK (Status IN ('PENDING','APPROVED','REJECTED')),
    ApprovedBy        INT NULL,
    CONSTRAINT FK_Att_Assignment FOREIGN KEY (ShiftAssignmentId) REFERENCES hr.ShiftAssignment(ShiftAssignmentId),
    CONSTRAINT FK_Att_Approver   FOREIGN KEY (ApprovedBy)        REFERENCES iam.[User](UserId)
);
GO

-- Bàn giao ca (Barista) — ghi chú đầu/cuối ca (B7)
CREATE TABLE hr.ShiftHandover (
    ShiftHandoverId INT IDENTITY PRIMARY KEY,
    BranchId        INT NOT NULL,
    Note            NVARCHAR(1000) NOT NULL,
    CreatedBy       INT NOT NULL,
    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_SH_Branch FOREIGN KEY (BranchId)  REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_SH_User   FOREIGN KEY (CreatedBy) REFERENCES iam.[User](UserId)
);
GO

/* ===========================================================================
   5. SALES: bàn, phiên bàn, đơn  (Cashier sở hữu order entry; Barista cập nhật KDS)
   =========================================================================== */
CREATE TABLE sales.DiningTable (
    DiningTableId INT IDENTITY PRIMARY KEY,
    BranchId      INT NOT NULL,
    TableNumber   NVARCHAR(20) NOT NULL,
    QrCode        VARCHAR(80)  NULL UNIQUE,      -- mã QR dán tại bàn
    Status        VARCHAR(10)  NOT NULL DEFAULT 'EMPTY'
                  CONSTRAINT CK_Table_Status CHECK (Status IN ('EMPTY','OCCUPIED','CLEANING')),
    CONSTRAINT FK_DT_Branch FOREIGN KEY (BranchId) REFERENCES org.Branch(BranchId),
    CONSTRAINT UQ_DT UNIQUE (BranchId, TableNumber)
);
GO

-- Phiên bàn (tab) — XƯƠNG SỐNG. Đơn gắn vào phiên này.
CREATE TABLE sales.TableSession (
    TableSessionId INT IDENTITY PRIMARY KEY,
    BranchId       INT NOT NULL,
    DiningTableId  INT NOT NULL,
    OpenedBy       INT NULL,                     -- Cashier mở; NULL nếu khách tự mở qua QR
    OpenedAt       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ClosedAt       DATETIME2 NULL,
    Status         VARCHAR(10) NOT NULL DEFAULT 'OPEN'
                   CONSTRAINT CK_Session_Status CHECK (Status IN ('OPEN','BILLED','CLOSED')),
    CONSTRAINT FK_TS_Branch FOREIGN KEY (BranchId)      REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_TS_Table  FOREIGN KEY (DiningTableId) REFERENCES sales.DiningTable(DiningTableId),
    CONSTRAINT FK_TS_User   FOREIGN KEY (OpenedBy)      REFERENCES iam.[User](UserId)
);
GO

-- Đơn hàng (cả COUNTER lẫn QR — cùng 1 bảng, Cashier sở hữu)
CREATE TABLE sales.Orders (
    OrderId        INT IDENTITY PRIMARY KEY,
    BranchId       INT NOT NULL,
    TableSessionId INT NULL,                     -- NULL nếu takeaway tại quầy
    CustomerId     INT NULL,
    Source         VARCHAR(8)  NOT NULL
                   CONSTRAINT CK_Order_Source CHECK (Source IN ('COUNTER','QR')),
    OrderType      VARCHAR(16) NOT NULL DEFAULT 'DINE_IN'
                   CONSTRAINT CK_Order_Type CHECK (OrderType IN ('DINE_IN','TAKEAWAY')),
    Status         VARCHAR(12) NOT NULL DEFAULT 'ACTIVE'
                   CONSTRAINT CK_Order_Status CHECK (Status IN ('ACTIVE','COMPLETED','CANCELLED')),
    CreatedBy      INT NULL,                     -- User (Cashier) nếu đơn quầy
    CreatedAt      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Ord_Branch   FOREIGN KEY (BranchId)       REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_Ord_Session  FOREIGN KEY (TableSessionId) REFERENCES sales.TableSession(TableSessionId),
    CONSTRAINT FK_Ord_Customer FOREIGN KEY (CustomerId)     REFERENCES iam.Customer(CustomerId),
    CONSTRAINT FK_Ord_User     FOREIGN KEY (CreatedBy)      REFERENCES iam.[User](UserId)
);
GO

-- Dòng đơn — STATUS DÙNG CHUNG cho KDS (Barista) và tracking (khách)
CREATE TABLE sales.OrderItem (
    OrderItemId INT IDENTITY PRIMARY KEY,
    OrderId     INT NOT NULL,
    ProductId   INT NOT NULL,
    Quantity    INT NOT NULL DEFAULT 1 CHECK (Quantity > 0),
    UnitPrice   DECIMAL(12,2) NOT NULL,          -- giá tại thời điểm đặt (đã gồm modifier)
    Note        NVARCHAR(255) NULL,
    Status      VARCHAR(10) NOT NULL DEFAULT 'WAITING'
                CONSTRAINT CK_Item_Status CHECK (Status IN ('WAITING','MAKING','READY','SERVED','CANCELLED')),
    StartedAt   DATETIME2 NULL,                  -- để tính lead time
    DoneAt      DATETIME2 NULL,
    CONSTRAINT FK_OI_Order   FOREIGN KEY (OrderId)   REFERENCES sales.Orders(OrderId) ON DELETE CASCADE,
    CONSTRAINT FK_OI_Product FOREIGN KEY (ProductId) REFERENCES catalog.Product(ProductId)
);
GO

-- Modifier khách chọn cho từng dòng đơn (đầu vào cho auto-deduct theo modifier)
CREATE TABLE sales.OrderItemModifier (
    OrderItemModifierId INT IDENTITY PRIMARY KEY,
    OrderItemId         INT NOT NULL,
    ModifierOptionId    INT NOT NULL,
    PriceDelta          DECIMAL(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT FK_OIM_Item   FOREIGN KEY (OrderItemId)      REFERENCES sales.OrderItem(OrderItemId) ON DELETE CASCADE,
    CONSTRAINT FK_OIM_Option FOREIGN KEY (ModifierOptionId) REFERENCES catalog.ModifierOption(ModifierOptionId)
);
GO

/* ===========================================================================
   6. PAYMENT: ca thu ngân, hóa đơn (tách bill), voucher  (Cashier + Admin)
   =========================================================================== */
CREATE TABLE payment.CashierShift (
    CashierShiftId INT IDENTITY PRIMARY KEY,
    BranchId       INT NOT NULL,
    CashierId      INT NOT NULL,
    OpeningCash    DECIMAL(14,2) NOT NULL DEFAULT 0,
    ClosingCash    DECIMAL(14,2) NULL,
    OpenedAt       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ClosedAt       DATETIME2 NULL,
    CONSTRAINT FK_CS_Branch  FOREIGN KEY (BranchId)  REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_CS_Cashier FOREIGN KEY (CashierId) REFERENCES iam.[User](UserId)
);
GO

-- Voucher toàn hệ thống (chỉ Admin sở hữu — một nguồn duy nhất)
CREATE TABLE payment.Voucher (
    VoucherId    INT IDENTITY PRIMARY KEY,
    Code         VARCHAR(40) NOT NULL UNIQUE,
    DiscountType VARCHAR(8)  NOT NULL
                 CONSTRAINT CK_Voucher_Type CHECK (DiscountType IN ('PERCENT','FIXED')),
    DiscountValue DECIMAL(12,2) NOT NULL CHECK (DiscountValue >= 0),
    MinOrderAmount DECIMAL(12,2) NOT NULL DEFAULT 0,
    Scope        VARCHAR(8) NOT NULL DEFAULT 'CHAIN'
                 CONSTRAINT CK_Voucher_Scope CHECK (Scope IN ('CHAIN','BRANCH')),
    BranchId     INT NULL,                       -- chỉ dùng khi Scope = BRANCH
    StartDate    DATETIME2 NULL,
    EndDate      DATETIME2 NULL,
    UsageLimit   INT NULL,                        -- NULL = không giới hạn
    UsedCount    INT NOT NULL DEFAULT 0,
    IsActive     BIT NOT NULL DEFAULT 1,
    CONSTRAINT FK_Voucher_Branch FOREIGN KEY (BranchId) REFERENCES org.Branch(BranchId)
);
GO

-- Hóa đơn: 1 phiên bàn có thể tách thành nhiều Bill (Dynamic Bill Splitting)
CREATE TABLE payment.Bill (
    BillId         INT IDENTITY PRIMARY KEY,
    BranchId       INT NOT NULL,
    TableSessionId INT NULL,                     -- NULL nếu takeaway
    CashierShiftId INT NULL,
    Subtotal       DECIMAL(14,2) NOT NULL DEFAULT 0,
    VatAmount      DECIMAL(14,2) NOT NULL DEFAULT 0,
    DiscountAmount DECIMAL(14,2) NOT NULL DEFAULT 0,
    TotalAmount    DECIMAL(14,2) NOT NULL DEFAULT 0,
    VoucherId      INT NULL,
    PaymentMethod  VARCHAR(10) NULL
                   CONSTRAINT CK_Bill_Method CHECK (PaymentMethod IN ('CASH','TRANSFER','QR_BANK')),
    Status         VARCHAR(8) NOT NULL DEFAULT 'UNPAID'
                   CONSTRAINT CK_Bill_Status CHECK (Status IN ('UNPAID','PAID','VOID')),
    PaidAt         DATETIME2 NULL,
    CreatedAt      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Bill_Branch  FOREIGN KEY (BranchId)       REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_Bill_Session FOREIGN KEY (TableSessionId) REFERENCES sales.TableSession(TableSessionId),
    CONSTRAINT FK_Bill_Shift   FOREIGN KEY (CashierShiftId) REFERENCES payment.CashierShift(CashierShiftId),
    CONSTRAINT FK_Bill_Voucher FOREIGN KEY (VoucherId)      REFERENCES payment.Voucher(VoucherId)
);
GO

-- Dòng đơn nào nằm trên Bill nào (cho phép tách bill)
CREATE TABLE payment.BillItem (
    BillItemId  INT IDENTITY PRIMARY KEY,
    BillId      INT NOT NULL,
    OrderItemId INT NOT NULL,
    Amount      DECIMAL(12,2) NOT NULL,
    CONSTRAINT FK_BItem_Bill  FOREIGN KEY (BillId)      REFERENCES payment.Bill(BillId) ON DELETE CASCADE,
    CONSTRAINT FK_BItem_OItem FOREIGN KEY (OrderItemId) REFERENCES sales.OrderItem(OrderItemId),
    CONSTRAINT UQ_BItem UNIQUE (OrderItemId)     -- 1 dòng đơn chỉ thuộc 1 bill
);
GO

CREATE TABLE payment.VoucherRedemption (
    RedemptionId    INT IDENTITY PRIMARY KEY,
    VoucherId       INT NOT NULL,
    BillId          INT NOT NULL,
    DiscountApplied DECIMAL(12,2) NOT NULL,
    RedeemedAt      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_VR_Voucher FOREIGN KEY (VoucherId) REFERENCES payment.Voucher(VoucherId),
    CONSTRAINT FK_VR_Bill    FOREIGN KEY (BillId)    REFERENCES payment.Bill(BillId)
);
GO

/* ===========================================================================
   7. OPS: Event Bus / Outbox  (contract #1)
   =========================================================================== */
CREATE TABLE ops.OutboxEvent (
    EventId     BIGINT IDENTITY PRIMARY KEY,
    EventType   VARCHAR(50) NOT NULL,            -- order.created, order.status_changed,
                                                 -- payment.completed, inventory.deducted, stock.low
    AggregateId VARCHAR(50) NULL,                -- id thực thể liên quan (OrderId, BillId...)
    BranchId    INT NULL,
    Payload     NVARCHAR(MAX) NULL,              -- JSON
    CreatedAt   DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ProcessedAt DATETIME2 NULL,
    CONSTRAINT FK_Event_Branch FOREIGN KEY (BranchId) REFERENCES org.Branch(BranchId)
);
GO

/* ---------------------------------------------------------------------------
   INDEXES (FK & cột truy vấn nóng)
   --------------------------------------------------------------------------- */
CREATE INDEX IX_User_Branch        ON iam.[User](BranchId);
CREATE INDEX IX_Product_Category   ON catalog.Product(CategoryId);
CREATE INDEX IX_BranchMenu_86      ON catalog.BranchMenu(BranchId, Is86);
CREATE INDEX IX_Inv_Txn_BranchIng  ON inventory.InventoryTransaction(BranchId, IngredientId);
CREATE INDEX IX_SRD_Receipt        ON inventory.StockReceiptDetail(StockReceiptId);
CREATE INDEX IX_Order_Session      ON sales.Orders(TableSessionId);
CREATE INDEX IX_Order_BranchStatus ON sales.Orders(BranchId, Status);
CREATE INDEX IX_OrderItem_Status   ON sales.OrderItem(Status);          -- KDS queue
CREATE INDEX IX_OrderItem_Order    ON sales.OrderItem(OrderId);
CREATE INDEX IX_Bill_Session       ON payment.Bill(TableSessionId);
CREATE INDEX IX_Bill_BranchStatus  ON payment.Bill(BranchId, Status);
CREATE INDEX IX_Outbox_Unprocessed ON ops.OutboxEvent(ProcessedAt) WHERE ProcessedAt IS NULL;
GO

/* ---------------------------------------------------------------------------
   VIEWS tiện ích
   --------------------------------------------------------------------------- */
GO
CREATE VIEW inventory.vw_LowStock AS
    SELECT bi.BranchId, b.Name AS BranchName, bi.IngredientId, i.Name AS IngredientName,
           bi.QuantityOnHand, bi.MinThreshold
    FROM inventory.BranchInventory bi
    JOIN org.Branch b ON b.BranchId = bi.BranchId
    JOIN catalog.Ingredient i ON i.IngredientId = bi.IngredientId
    WHERE bi.QuantityOnHand <= bi.MinThreshold;
GO
CREATE VIEW sales.vw_KdsQueue AS
    SELECT oi.OrderItemId, o.BranchId, o.OrderId, ts.DiningTableId,
           p.Name AS ProductName, oi.Quantity, oi.Status, oi.Note, o.CreatedAt
    FROM sales.OrderItem oi
    JOIN sales.Orders o   ON o.OrderId = oi.OrderId
    LEFT JOIN sales.TableSession ts ON ts.TableSessionId = o.TableSessionId
    JOIN catalog.Product p ON p.ProductId = oi.ProductId
    WHERE oi.Status IN ('WAITING','MAKING');
GO

/* ===========================================================================
   8. SEED DATA (dữ liệu mẫu để chạy thử)
   =========================================================================== */
INSERT INTO iam.Role(Code, Name) VALUES
    ('ADMIN', N'Quản trị hệ thống'),
    ('BRANCH_MANAGER', N'Quản lý chi nhánh'),
    ('CASHIER', N'Thu ngân'),
    ('BARISTA', N'Pha chế');
GO

INSERT INTO org.Branch(Code, Name, Address, Phone, OpenTime, CloseTime) VALUES
    ('CN01', N'Chi nhánh Quận 1', N'123 Lê Lợi, Q1, TP.HCM', '0900000001', '07:00', '22:00');
GO

-- Lưu ý: PasswordHash dưới đây chỉ là placeholder, thay bằng hash thật khi seed app
INSERT INTO iam.[User](Username, PasswordHash, FullName, RoleId, BranchId) VALUES
    ('admin',   '$2a$placeholder', N'System Admin', (SELECT RoleId FROM iam.Role WHERE Code='ADMIN'), NULL),
    ('manager1','$2a$placeholder', N'Trần Quản Lý',  (SELECT RoleId FROM iam.Role WHERE Code='BRANCH_MANAGER'), 1),
    ('cashier1','$2a$placeholder', N'Lê Thu Ngân',   (SELECT RoleId FROM iam.Role WHERE Code='CASHIER'), 1),
    ('barista1','$2a$placeholder', N'Nguyễn Pha Chế', (SELECT RoleId FROM iam.Role WHERE Code='BARISTA'), 1);
GO
UPDATE org.Branch SET ManagerUserId = (SELECT UserId FROM iam.[User] WHERE Username='manager1') WHERE Code='CN01';
GO

INSERT INTO catalog.Category(Name, SortOrder) VALUES
    (N'Cà phê', 1), (N'Trà', 2), (N'Đá xay', 3);
GO

INSERT INTO catalog.Product(CategoryId, Name, BasePrice) VALUES
    (1, N'Cà phê sữa', 29000),
    (1, N'Cold Brew',  45000),
    (2, N'Trà Đào',    39000);
GO

-- RAW = mua về; PREPPED = pha sẵn tại quán
INSERT INTO catalog.Ingredient(Name, Unit, IngredientType) VALUES
    (N'Cà phê hạt', N'g',  'RAW'),       -- 1
    (N'Sữa đặc',    N'ml', 'RAW'),       -- 2
    (N'Đường',      N'g',  'RAW'),       -- 3
    (N'Đá',         N'g',  'RAW'),       -- 4
    (N'Đào ngâm',   N'g',  'RAW'),       -- 5
    (N'Cold Brew',  N'ml', 'PREPPED'),   -- 6 (pha sẵn từ cà phê hạt)
    (N'Syrup Đào',  N'ml', 'PREPPED');   -- 7 (pha sẵn từ đào + đường)
GO

-- Công thức pha sẵn: Cold Brew từ cà phê hạt; Syrup đào từ đào + đường
INSERT INTO catalog.PrepRecipe(PreppedIngredientId, RawIngredientId, Quantity, YieldQty) VALUES
    (6, 1, 200, 1000),   -- 200g cà phê hạt -> 1000ml Cold Brew
    (7, 5, 500,  800),   -- 500g đào -> 800ml syrup
    (7, 3, 300,  800);   -- + 300g đường
GO

-- Công thức món
INSERT INTO catalog.ProductRecipe(ProductId, IngredientId, Quantity) VALUES
    (1, 1, 18), (1, 2, 30),          -- Cà phê sữa: 18g cà phê (RAW) + 30ml sữa
    (2, 6, 180), (2, 4, 100),        -- Cold Brew: 180ml Cold Brew (PREPPED) + 100g đá
    (3, 7, 40),  (3, 5, 50), (3, 4, 100); -- Trà đào: syrup (PREPPED) + đào + đá
GO

-- Modifier mẫu
INSERT INTO catalog.ModifierGroup(Name, IsRequired, MinSelect, MaxSelect) VALUES
    (N'Size', 1, 1, 1), (N'Đường', 0, 0, 1), (N'Topping', 0, 0, 3);
GO
INSERT INTO catalog.ModifierOption(ModifierGroupId, Name, PriceDelta) VALUES
    (1, N'Size S', 0), (1, N'Size M', 6000), (1, N'Size L', 10000),
    (2, N'Ít đường', 0), (2, N'100% đường', 0),
    (3, N'Thêm shot', 8000), (3, N'Trân châu', 7000);
GO
-- Ví dụ ảnh hưởng định mức: "Thêm shot" = +18g cà phê hạt
INSERT INTO catalog.ModifierIngredientImpact(ModifierOptionId, IngredientId, QtyDelta) VALUES
    ((SELECT ModifierOptionId FROM catalog.ModifierOption WHERE Name=N'Thêm shot'), 1, 18);
GO

-- Menu chi nhánh
INSERT INTO catalog.BranchMenu(BranchId, ProductId, IsAvailable) VALUES (1,1,1),(1,2,1),(1,3,1);
GO

-- Bàn
INSERT INTO sales.DiningTable(BranchId, TableNumber, QrCode) VALUES
    (1, N'Bàn 01', 'QR-CN01-T01'), (1, N'Bàn 02', 'QR-CN01-T02'),
    (1, N'Bàn 03', 'QR-CN01-T03'), (1, N'Bàn 04', 'QR-CN01-T04');
GO

-- Voucher toàn hệ thống
INSERT INTO payment.Voucher(Code, DiscountType, DiscountValue, Scope, StartDate, EndDate, UsageLimit) VALUES
    ('GRANDOPENING', 'PERCENT', 20, 'CHAIN', SYSUTCDATETIME(), DATEADD(MONTH,1,SYSUTCDATETIME()), 1000);
GO

PRINT N'== Tạo CSDL CafeChain hoàn tất ==';
GO
