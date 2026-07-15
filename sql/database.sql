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
   ----------------------------------------------------------------------------
   FILE SQL DUY NHẤT (đã gộp mọi migration + seed) — chạy 1 phát là xong:
     PART A (dưới đây) : schema 8 domain / 37 bảng + seed gốc.
     PART B            : catalog 15 món (đủ công thức + modifier) + ảnh Unsplash.
     PART C            : demo lớn — 3 chi nhánh, 16 user (BCrypt, mật khẩu 123456),
                         ~31 ngày lịch sử bán (≈800 hoá đơn PAID) + story hôm nay,
                         đủ inventory/HR/sales/payment cho MỌI role.
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
DROP TABLE IF EXISTS hr.Payroll;
DROP TABLE IF EXISTS hr.Attendance;
DROP TABLE IF EXISTS hr.ShiftAssignment;
DROP TABLE IF EXISTS hr.ShiftTemplate;
DROP TABLE IF EXISTS catalog.HomeSetting;
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
    ProductId     INT IDENTITY PRIMARY KEY,
    CategoryId    INT           NOT NULL,
    Name          NVARCHAR(150) NOT NULL,
    BasePrice     DECIMAL(12,2) NOT NULL CHECK (BasePrice >= 0),
    ImageUrl      VARCHAR(255)  NULL,
    IsActive      BIT NOT NULL DEFAULT 1,
    ShowOnHome    BIT NOT NULL DEFAULT 1,   -- hiển thị trên trang Home công khai (Admin chỉnh)
    HomeSortOrder INT NOT NULL DEFAULT 0,   -- thứ tự ưu tiên trong danh mục trên Home (nhỏ = trước)
    CONSTRAINT FK_Product_Category FOREIGN KEY (CategoryId) REFERENCES catalog.Category(CategoryId)
);
GO

-- Nội dung hero trang Home công khai (singleton: Id luôn = 1) — Admin chỉnh ở /admin/home
CREATE TABLE catalog.HomeSetting (
    Id           INT NOT NULL PRIMARY KEY CONSTRAINT CK_HomeSetting_Singleton CHECK (Id = 1),
    HeroEyebrow  NVARCHAR(150)  NULL,
    HeroTitle    NVARCHAR(200)  NULL,
    HeroSubtitle NVARCHAR(500)  NULL,
    HeroImageUrl VARCHAR(500)   NULL,
    UpdatedAt    DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);
GO

INSERT INTO catalog.HomeSetting (Id, HeroEyebrow, HeroTitle, HeroSubtitle, HeroImageUrl)
VALUES (1,
    N'Chuỗi cà phê thủ công',
    N'Thực đơn của Cà Phê Chain',
    N'Khám phá menu cà phê, trà và đá xay được pha chế tươi mỗi ngày. Đến quán, quét QR tại bàn để đặt món ngay.',
    '/assets/img/login-hero.svg');
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
    Unit                 NVARCHAR(20) NULL,             -- đơn vị nhập per-line (vd "Túi"); NULL = dùng đơn vị gốc nguyên liệu
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
    Unit              NVARCHAR(20) NULL,             -- đơn vị đếm per-line (vd "Túi"); NULL = dùng đơn vị gốc nguyên liệu
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

-- Bảng lương theo tháng (Manager) — chốt giờ làm + lương/giờ từng NV, upsert (S8)
CREATE TABLE hr.Payroll (
    PayrollId   INT IDENTITY PRIMARY KEY,
    BranchId    INT NOT NULL,
    UserId      INT NOT NULL,
    PayMonth    CHAR(7) NOT NULL,                 -- 'yyyy-MM'
    WorkedHours DECIMAL(10,2) NOT NULL DEFAULT 0,
    HourlyRate  DECIMAL(12,2) NOT NULL DEFAULT 0,
    UpdatedBy   INT NULL,
    UpdatedAt   DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Pay_Branch  FOREIGN KEY (BranchId)  REFERENCES org.Branch(BranchId),
    CONSTRAINT FK_Pay_User    FOREIGN KEY (UserId)    REFERENCES iam.[User](UserId),
    CONSTRAINT FK_Pay_Updater FOREIGN KEY (UpdatedBy) REFERENCES iam.[User](UserId),
    CONSTRAINT UQ_Pay UNIQUE (UserId, PayMonth)
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
    Priority    INT NOT NULL DEFAULT 0,          -- KDS bump: đẩy đơn quá giờ lên đầu (cao = ưu tiên)
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
                   CONSTRAINT CK_Bill_Status CHECK (Status IN ('UNPAID','PAID','VOID','REFUND')),
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
    (N'Syrup Đào',  N'ml', 'PREPPED'),   -- 7 (pha sẵn từ đào + đường)
    (N'Trân châu',  N'g',  'RAW'),       -- 8
    (N'Kem cheese', N'g',  'RAW');       -- 9
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
    (N'Size', 1, 1, 1),     -- 1: Size riêng Cà phê sữa
    (N'Size', 1, 1, 1),     -- 2: Size riêng Cold Brew
    (N'Size', 1, 1, 1),     -- 3: Size riêng Trà Đào
    (N'Đường', 1, 1, 1),    -- 4
    (N'Đá', 1, 1, 1),       -- 5
    (N'Topping', 0, 0, 3);  -- 6
GO
INSERT INTO catalog.ModifierOption(ModifierGroupId, Name, PriceDelta) VALUES
    (1, N'Size S', 0), (1, N'Size M', 6000), (1, N'Size L', 10000),
    (2, N'Size S', 0), (2, N'Size M', 6000), (2, N'Size L', 10000),
    (3, N'Size S', 0), (3, N'Size M', 6000), (3, N'Size L', 10000),
    (4, N'Không đường', 0), (4, N'Ít đường', 0), (4, N'Bình thường', 0), (4, N'Nhiều đường', 0),
    (5, N'Không đá', 0), (5, N'Ít đá', 0), (5, N'Bình thường', 0), (5, N'Nhiều đá', 0),
    (6, N'Thêm shot', 8000), (6, N'Trân châu', 7000), (6, N'Kem cheese', 10000);
GO
INSERT INTO catalog.ProductModifierGroup(ProductId, ModifierGroupId) VALUES
    (1,1), (1,4), (1,5), (1,6),
    (2,2),       (2,5), (2,6),
    (3,3), (3,4), (3,5), (3,6);
GO
-- Ví dụ ảnh hưởng định mức: "Thêm shot" = +18g cà phê hạt
INSERT INTO catalog.ModifierIngredientImpact(ModifierOptionId, IngredientId, QtyDelta) VALUES
    -- Size Cà phê sữa
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=1 AND mo.Name=N'Size M'), 1, 4),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=1 AND mo.Name=N'Size M'), 2, 10),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=1 AND mo.Name=N'Size M'), 4, 50),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=1 AND mo.Name=N'Size L'), 1, 8),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=1 AND mo.Name=N'Size L'), 2, 20),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=1 AND mo.Name=N'Size L'), 4, 100),
    -- Size Cold Brew
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=2 AND mo.Name=N'Size M'), 6, 30),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=2 AND mo.Name=N'Size M'), 4, 50),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=2 AND mo.Name=N'Size L'), 6, 60),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=2 AND mo.Name=N'Size L'), 4, 100),
    -- Size Trà Đào
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=3 AND mo.Name=N'Size M'), 7, 10),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=3 AND mo.Name=N'Size M'), 5, 20),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=3 AND mo.Name=N'Size M'), 4, 50),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=3 AND mo.Name=N'Size L'), 7, 20),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=3 AND mo.Name=N'Size L'), 5, 40),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=3 AND mo.Name=N'Size L'), 4, 100),
    -- Đường / đá dùng chung
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=4 AND mo.Name=N'Không đường'), 3, -5),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=4 AND mo.Name=N'Ít đường'), 3, -2.5),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=4 AND mo.Name=N'Nhiều đường'), 3, 5),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=5 AND mo.Name=N'Không đá'), 4, -100),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=5 AND mo.Name=N'Ít đá'), 4, -50),
    ((SELECT mo.ModifierOptionId FROM catalog.ModifierOption mo WHERE mo.ModifierGroupId=5 AND mo.Name=N'Nhiều đá'), 4, 50),
    -- Topping
    ((SELECT ModifierOptionId FROM catalog.ModifierOption WHERE ModifierGroupId=6 AND Name=N'Thêm shot'), 1, 18),
    ((SELECT ModifierOptionId FROM catalog.ModifierOption WHERE ModifierGroupId=6 AND Name=N'Trân châu'), 8, 50),
    ((SELECT ModifierOptionId FROM catalog.ModifierOption WHERE ModifierGroupId=6 AND Name=N'Kem cheese'), 9, 30);
GO

-- Menu chi nhánh
INSERT INTO catalog.BranchMenu(BranchId, ProductId, IsAvailable) VALUES (1,1,1),(1,2,1),(1,3,1);
GO

-- Tồn demo cho topping: ghi sổ cái trước, dựng cache BranchInventory từ ledger.
INSERT INTO inventory.InventoryTransaction(BranchId,IngredientId,ChangeQty,TxnType,RefTable,RefId,CreatedBy) VALUES
    (1,8,2000,'RECEIPT','Seed',NULL,(SELECT UserId FROM iam.[User] WHERE Username='manager1')),
    (1,9,1500,'RECEIPT','Seed',NULL,(SELECT UserId FROM iam.[User] WHERE Username='manager1'));
GO
INSERT INTO inventory.BranchInventory(BranchId,IngredientId,QuantityOnHand,MinThreshold,UpdatedAt)
SELECT BranchId, IngredientId, SUM(ChangeQty), 0, SYSUTCDATETIME()
FROM inventory.InventoryTransaction
GROUP BY BranchId, IngredientId;
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


/* ############################################################################
   PART B — CATALOG ĐẦY ĐỦ (menu 15 món, mọi món đủ công thức + modifier + ảnh)
   Idempotent (khoá theo Name). Layer lên PART A. Ảnh = Unsplash (đã verify 200).
   ############################################################################ */
USE CafeChain;
GO
SET XACT_ABORT ON; SET NOCOUNT ON;
BEGIN TRAN;

/* --- B1. Nguyên liệu bổ sung (RAW/PREPPED) -------------------------------- */
MERGE catalog.Ingredient AS t
USING (VALUES
    (N'Sữa tươi',      N'ml','RAW'),
    (N'Trà sen',       N'g', 'RAW'),
    (N'Trà đen',       N'g', 'RAW'),
    (N'Vải ngâm',      N'g', 'RAW'),
    (N'Gừng',          N'g', 'RAW'),
    (N'Mật ong',       N'ml','RAW'),
    (N'Vụn Cookie',    N'g', 'RAW'),
    (N'Kem whipping',  N'ml','RAW'),
    (N'Bột Matcha',    N'g', 'RAW'),
    (N'Bột Chocolate', N'g', 'RAW')
) AS s(Name,Unit,Type)
ON t.Name = s.Name
WHEN NOT MATCHED THEN INSERT(Name,Unit,IngredientType) VALUES(s.Name,s.Unit,s.Type);

/* --- B2. Sản phẩm (15 món) + ảnh Unsplash (MERGE theo Name) ---------------- */
DECLARE @U VARCHAR(60) = 'https://images.unsplash.com/photo-';
DECLARE @Q VARCHAR(40) = '?w=800&q=80&auto=format&fit=crop';
MERGE catalog.Product AS t
USING (VALUES
    (1,N'Cà phê sữa',       29000,'1517701550927-30cf4ba1dba5'),
    (1,N'Cà phê đen',       25000,'1510591509098-f4fdc6d0ff04'),
    (1,N'Bạc xỉu',          32000,'1509042239860-f550ce710b93'),
    (1,N'Cold Brew',        45000,'1461023058943-07fcbe16d735'),
    (1,N'Cappuccino',       45000,'1572442388796-11668a67e53d'),
    (1,N'Latte',            49000,'1541167760496-1628856ab772'),
    (1,N'Espresso',         25000,'1510707577719-ae7c14805e3a'),
    (1,N'Americano',        30000,'1551030173-122aabc4489c'),
    (2,N'Trà Đào',          39000,'1556679343-c7306c1976bc'),
    (2,N'Trà sen vàng',     42000,'1597318181409-cf64d0b5d8a2'),
    (2,N'Trà vải',          42000,'1499638673689-79a0b5115d87'),
    (2,N'Trà gừng mật ong', 38000,'1576092768241-dec231879fc3'),
    (3,N'Đá xay Cookie',    55000,'1572490122747-3968b75cc699'),
    (3,N'Matcha đá xay',    55000,'1536256263959-770b48d82b0a'),
    (3,N'Chocolate đá xay', 52000,'1619158401201-8fa932695178')
) AS s(Cat,Name,Price,Img)
ON t.Name = s.Name
WHEN MATCHED THEN UPDATE SET CategoryId=s.Cat, BasePrice=s.Price,
     ImageUrl=@U+s.Img+@Q, IsActive=1
WHEN NOT MATCHED THEN INSERT(CategoryId,Name,BasePrice,ImageUrl,IsActive)
     VALUES(s.Cat,s.Name,s.Price,@U+s.Img+@Q,1);

/* --- B3. Công thức món (ProductRecipe) — mọi món orderable + auto-deduct --- */
DECLARE @Recipe TABLE(P NVARCHAR(150), I NVARCHAR(120), Q DECIMAL(12,3));
INSERT INTO @Recipe(P,I,Q) VALUES
    (N'Cà phê sữa',N'Cà phê hạt',18),(N'Cà phê sữa',N'Sữa đặc',30),
    (N'Cà phê đen',N'Cà phê hạt',18),(N'Cà phê đen',N'Đường',8),
    (N'Bạc xỉu',N'Cà phê hạt',12),(N'Bạc xỉu',N'Sữa đặc',25),(N'Bạc xỉu',N'Sữa tươi',60),
    (N'Cold Brew',N'Cold Brew',180),(N'Cold Brew',N'Đá',100),
    (N'Cappuccino',N'Cà phê hạt',18),(N'Cappuccino',N'Sữa tươi',120),
    (N'Latte',N'Cà phê hạt',18),(N'Latte',N'Sữa tươi',150),
    (N'Espresso',N'Cà phê hạt',18),
    (N'Americano',N'Cà phê hạt',18),(N'Americano',N'Đá',50),
    (N'Trà Đào',N'Syrup Đào',40),(N'Trà Đào',N'Đào ngâm',50),(N'Trà Đào',N'Đá',100),
    (N'Trà sen vàng',N'Trà sen',8),(N'Trà sen vàng',N'Đường',10),(N'Trà sen vàng',N'Đá',100),
    (N'Trà vải',N'Trà đen',6),(N'Trà vải',N'Vải ngâm',60),(N'Trà vải',N'Đường',10),(N'Trà vải',N'Đá',100),
    (N'Trà gừng mật ong',N'Gừng',10),(N'Trà gừng mật ong',N'Mật ong',20),(N'Trà gừng mật ong',N'Trà đen',5),
    (N'Đá xay Cookie',N'Sữa tươi',120),(N'Đá xay Cookie',N'Vụn Cookie',30),(N'Đá xay Cookie',N'Đá',150),(N'Đá xay Cookie',N'Kem whipping',30),
    (N'Matcha đá xay',N'Bột Matcha',12),(N'Matcha đá xay',N'Sữa tươi',120),(N'Matcha đá xay',N'Đá',150),(N'Matcha đá xay',N'Kem whipping',30),
    (N'Chocolate đá xay',N'Bột Chocolate',25),(N'Chocolate đá xay',N'Sữa tươi',120),(N'Chocolate đá xay',N'Đá',150),(N'Chocolate đá xay',N'Kem whipping',30);

MERGE catalog.ProductRecipe AS t
USING (SELECT p.ProductId, i.IngredientId, r.Q
       FROM @Recipe r
       JOIN catalog.Product p    ON p.Name=r.P
       JOIN catalog.Ingredient i ON i.Name=r.I) AS s
ON t.ProductId=s.ProductId AND t.IngredientId=s.IngredientId
WHEN MATCHED THEN UPDATE SET Quantity=s.Q
WHEN NOT MATCHED THEN INSERT(ProductId,IngredientId,Quantity) VALUES(s.ProductId,s.IngredientId,s.Q);

/* --- B4. Modifier: Size riêng từng món (tránh lẫn impact) ------------------ */
-- Tạo 1 nhóm Size cho mỗi món chưa có, kèm option S/M/L + impact lên nguyên liệu chính.
WHILE EXISTS (
    SELECT 1 FROM catalog.Product p
    WHERE p.IsActive=1 AND NOT EXISTS (
        SELECT 1 FROM catalog.ProductModifierGroup pmg
        JOIN catalog.ModifierGroup mg ON mg.ModifierGroupId=pmg.ModifierGroupId
        WHERE pmg.ProductId=p.ProductId AND mg.Name=N'Size'))
BEGIN
    DECLARE @pid INT = (
        SELECT TOP (1) p.ProductId FROM catalog.Product p
        WHERE p.IsActive=1 AND NOT EXISTS (
            SELECT 1 FROM catalog.ProductModifierGroup pmg
            JOIN catalog.ModifierGroup mg ON mg.ModifierGroupId=pmg.ModifierGroupId
            WHERE pmg.ProductId=p.ProductId AND mg.Name=N'Size')
        ORDER BY p.ProductId);
    INSERT INTO catalog.ModifierGroup(Name,IsRequired,MinSelect,MaxSelect) VALUES(N'Size',1,1,1);
    DECLARE @gid INT = SCOPE_IDENTITY();
    INSERT INTO catalog.ProductModifierGroup(ProductId,ModifierGroupId) VALUES(@pid,@gid);
    INSERT INTO catalog.ModifierOption(ModifierGroupId,Name,PriceDelta) VALUES
        (@gid,N'Size S',0),(@gid,N'Size M',6000),(@gid,N'Size L',10000);
    -- nguyên liệu chính = dòng công thức có Quantity lớn nhất của món
    DECLARE @mainIng INT = (SELECT TOP (1) IngredientId FROM catalog.ProductRecipe
                            WHERE ProductId=@pid ORDER BY Quantity DESC, IngredientId);
    DECLARE @mainQty DECIMAL(12,3) = (SELECT TOP (1) Quantity FROM catalog.ProductRecipe
                            WHERE ProductId=@pid ORDER BY Quantity DESC, IngredientId);
    IF @mainIng IS NOT NULL
        INSERT INTO catalog.ModifierIngredientImpact(ModifierOptionId,IngredientId,QtyDelta)
        SELECT mo.ModifierOptionId, @mainIng,
               CASE mo.Name WHEN N'Size M' THEN ROUND(@mainQty*0.2,1)
                            WHEN N'Size L' THEN ROUND(@mainQty*0.4,1) END
        FROM catalog.ModifierOption mo
        WHERE mo.ModifierGroupId=@gid AND mo.Name IN (N'Size M',N'Size L');
END

/* --- B5. Gắn nhóm dùng chung Đá / Đường / Topping ------------------------- */
DECLARE @gDuong  INT = (SELECT TOP(1) ModifierGroupId FROM catalog.ModifierGroup WHERE Name=N'Đường'  ORDER BY ModifierGroupId);
DECLARE @gDa     INT = (SELECT TOP(1) ModifierGroupId FROM catalog.ModifierGroup WHERE Name=N'Đá'     ORDER BY ModifierGroupId);
DECLARE @gTopping INT = (SELECT TOP(1) ModifierGroupId FROM catalog.ModifierGroup WHERE Name=N'Topping' ORDER BY ModifierGroupId);

-- Đá + Topping cho mọi món
INSERT INTO catalog.ProductModifierGroup(ProductId,ModifierGroupId)
SELECT p.ProductId, g.g FROM catalog.Product p
CROSS JOIN (VALUES(@gDa),(@gTopping)) g(g)
WHERE p.IsActive=1 AND g.g IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM catalog.ProductModifierGroup x WHERE x.ProductId=p.ProductId AND x.ModifierGroupId=g.g);

-- Đường cho các món có vị ngọt điều chỉnh
INSERT INTO catalog.ProductModifierGroup(ProductId,ModifierGroupId)
SELECT p.ProductId, @gDuong FROM catalog.Product p
WHERE @gDuong IS NOT NULL AND p.IsActive=1
  AND p.Name IN (N'Cà phê sữa',N'Cà phê đen',N'Bạc xỉu',N'Trà Đào',N'Trà sen vàng',N'Trà vải',N'Trà gừng mật ong')
  AND NOT EXISTS (SELECT 1 FROM catalog.ProductModifierGroup x WHERE x.ProductId=p.ProductId AND x.ModifierGroupId=@gDuong);

/* --- B6. Publish menu CN01 cho toàn bộ món (idempotent) ------------------- */
INSERT INTO catalog.BranchMenu(BranchId,ProductId,IsAvailable)
SELECT 1, p.ProductId, 1 FROM catalog.Product p
WHERE p.IsActive=1 AND NOT EXISTS (SELECT 1 FROM catalog.BranchMenu bm WHERE bm.BranchId=1 AND bm.ProductId=p.ProductId);

COMMIT;
GO
PRINT N'== PART B (catalog 15 món) hoàn tất ==';
GO


/* ############################################################################
   PART C — VẬN HÀNH DEMO (thay toàn bộ story cũ): 3 chi nhánh, ~16 user,
   nhập kho lớn, pha sẵn, 31 ngày lịch sử bán (PAID + trừ kho), story hôm nay
   đủ mọi role, HR/voucher/outbox; chốt lại tồn = Σ sổ cái.
   ############################################################################ */
USE CafeChain;
GO
SET XACT_ABORT ON; SET NOCOUNT ON;
BEGIN TRAN;

/* ---- Hash BCrypt thật cho "123456" ---------------------------------------- */
DECLARE @PW VARCHAR(255) = '$2a$10$BFdZOEu0.X9/U6Yme03Z.ec6H/lsprcbJavmdUw3B4O51T82onwGa';

DECLARE @rManager INT = (SELECT RoleId FROM iam.Role WHERE Code='BRANCH_MANAGER');
DECLARE @rCashier INT = (SELECT RoleId FROM iam.Role WHERE Code='CASHIER');
DECLARE @rBarista INT = (SELECT RoleId FROM iam.Role WHERE Code='BARISTA');

DECLARE @now DATETIME2 = SYSUTCDATETIME();
DECLARE @today DATE = CAST(@now AS DATE);

/* ===========================================================================
   C0) USER — mật khẩu thật cho seed gốc + thêm nhân sự CN01
   =========================================================================== */
UPDATE iam.[User] SET PasswordHash=@PW WHERE Username IN ('admin','manager1','cashier1','barista1');
UPDATE iam.[User] SET Email='admin@cafechain.vn',    Phone='0901000000' WHERE Username='admin';
UPDATE iam.[User] SET Email='manager1@cafechain.vn', Phone='0901000001' WHERE Username='manager1';
UPDATE iam.[User] SET Email='cashier1@cafechain.vn', Phone='0901000002' WHERE Username='cashier1';
UPDATE iam.[User] SET Email='barista1@cafechain.vn', Phone='0901000003' WHERE Username='barista1';

DECLARE @b1 INT = (SELECT BranchId FROM org.Branch WHERE Code='CN01');
INSERT INTO iam.[User](Username,PasswordHash,FullName,Email,Phone,RoleId,BranchId) VALUES
 ('cashier2',@PW,N'Phạm Thu Hai',  'cashier2@cafechain.vn','0901000004',@rCashier,@b1),
 ('barista2',@PW,N'Võ Pha Chế Hai','barista2@cafechain.vn','0901000005',@rBarista,@b1),
 ('barista4',@PW,N'Đặng Pha Chế',  'barista4@cafechain.vn','0901000006',@rBarista,@b1);

/* ===========================================================================
   C1) CHI NHÁNH 2 & 3 + nhân sự + menu + bàn
   =========================================================================== */
INSERT INTO org.Branch(Code,Name,Address,Phone,OpenTime,CloseTime) VALUES
 ('CN02',N'Chi nhánh Thủ Đức',   N'45 Võ Văn Ngân, Thủ Đức, TP.HCM','0900000002','07:00','22:30'),
 ('CN03',N'Chi nhánh Hải Châu',  N'12 Bạch Đằng, Hải Châu, Đà Nẵng','0900000003','06:30','22:00');
DECLARE @b2 INT = (SELECT BranchId FROM org.Branch WHERE Code='CN02');
DECLARE @b3 INT = (SELECT BranchId FROM org.Branch WHERE Code='CN03');

INSERT INTO iam.[User](Username,PasswordHash,FullName,Email,Phone,RoleId,BranchId) VALUES
 ('manager2',@PW,N'Đỗ Quản Lý',   'manager2@cafechain.vn','0902000001',@rManager,@b2),
 ('cashier3',@PW,N'Bùi Thu Ngân', 'cashier3@cafechain.vn','0902000002',@rCashier,@b2),
 ('cashier4',@PW,N'Lý Thu Ngân',  'cashier4@cafechain.vn','0902000003',@rCashier,@b2),
 ('barista3',@PW,N'Hồ Pha Chế',   'barista3@cafechain.vn','0902000004',@rBarista,@b2),
 ('barista5',@PW,N'Trịnh Pha Chế','barista5@cafechain.vn','0902000005',@rBarista,@b2),
 ('manager3',@PW,N'Ngô Quản Lý',  'manager3@cafechain.vn','0903000001',@rManager,@b3),
 ('cashier5',@PW,N'Cao Thu Ngân', 'cashier5@cafechain.vn','0903000002',@rCashier,@b3),
 ('barista6',@PW,N'Dương Pha Chế','barista6@cafechain.vn','0903000003',@rBarista,@b3),
 ('barista7',@PW,N'Tô Pha Chế',   'barista7@cafechain.vn','0903000004',@rBarista,@b3);

UPDATE org.Branch SET ManagerUserId=(SELECT UserId FROM iam.[User] WHERE Username='manager2') WHERE BranchId=@b2;
UPDATE org.Branch SET ManagerUserId=(SELECT UserId FROM iam.[User] WHERE Username='manager3') WHERE BranchId=@b3;

-- Menu CN02 & CN03: toàn bộ món active
INSERT INTO catalog.BranchMenu(BranchId,ProductId,IsAvailable)
SELECT br.BranchId, p.ProductId, 1
FROM org.Branch br JOIN catalog.Product p ON p.IsActive=1
WHERE br.BranchId IN (@b2,@b3)
  AND NOT EXISTS (SELECT 1 FROM catalog.BranchMenu bm WHERE bm.BranchId=br.BranchId AND bm.ProductId=p.ProductId);

-- Bàn cho CN02, CN03
INSERT INTO sales.DiningTable(BranchId,TableNumber,QrCode) VALUES
 (@b2,N'Bàn 01','QR-CN02-T01'),(@b2,N'Bàn 02','QR-CN02-T02'),(@b2,N'Bàn 03','QR-CN02-T03'),(@b2,N'Bàn 04','QR-CN02-T04'),
 (@b3,N'Bàn 01','QR-CN03-T01'),(@b3,N'Bàn 02','QR-CN03-T02'),(@b3,N'Bàn 03','QR-CN03-T03'),(@b3,N'Bàn 04','QR-CN03-T04');

/* ===========================================================================
   C2) NHÀ CUNG CẤP + NHẬP KHO ĐẦU KỲ (lớn, đủ phủ 31 ngày) + PHA SẴN
       Ghi sổ cái RECEIPT/PREP; cuối script dựng lại BranchInventory.
   =========================================================================== */
INSERT INTO inventory.Supplier(Name,Phone,Address) VALUES
 (N'Công ty Cà Phê Tây Nguyên',       '0911111111',N'KCN Trảng Bom, Đồng Nai'),
 (N'NPP Nguyên Liệu Pha Chế Sài Gòn', '0922222222',N'Q.Tân Bình, TP.HCM'),
 (N'Trà & Trái Cây Bảo An',           '0933333333',N'Q.Gò Vấp, TP.HCM');
DECLARE @sup1 INT = (SELECT SupplierId FROM inventory.Supplier WHERE Phone='0911111111');
DECLARE @sup2 INT = (SELECT SupplierId FROM inventory.Supplier WHERE Phone='0922222222');
DECLARE @sup3 INT = (SELECT SupplierId FROM inventory.Supplier WHERE Phone='0933333333');

-- Nhập kho đầu kỳ: mỗi (chi nhánh × nguyên liệu RAW) 1 dòng số lượng lớn.
-- 1 phiếu CONFIRMED / chi nhánh, chi tiết = mọi nguyên liệu RAW, ghi ledger RECEIPT.
DECLARE @openQty DECIMAL(12,3) = 500000;   -- đủ lớn để 31 ngày bán vẫn dương (đá dùng nhiều)
DECLARE @bx TABLE(BranchId INT, Mgr INT, Bar INT, Cas INT, Sup INT);
INSERT INTO @bx VALUES
 (@b1,(SELECT UserId FROM iam.[User] WHERE Username='manager1'),(SELECT UserId FROM iam.[User] WHERE Username='barista1'),(SELECT UserId FROM iam.[User] WHERE Username='cashier1'),@sup1),
 (@b2,(SELECT UserId FROM iam.[User] WHERE Username='manager2'),(SELECT UserId FROM iam.[User] WHERE Username='barista3'),(SELECT UserId FROM iam.[User] WHERE Username='cashier3'),@sup2),
 (@b3,(SELECT UserId FROM iam.[User] WHERE Username='manager3'),(SELECT UserId FROM iam.[User] WHERE Username='barista6'),(SELECT UserId FROM iam.[User] WHERE Username='cashier5'),@sup3);

DECLARE @curB INT, @curMgr INT, @curBar INT, @curSup INT, @rid INT;
DECLARE cB CURSOR LOCAL FAST_FORWARD FOR SELECT BranchId,Mgr,Bar,Sup FROM @bx;
OPEN cB; FETCH NEXT FROM cB INTO @curB,@curMgr,@curBar,@curSup;
WHILE @@FETCH_STATUS=0
BEGIN
    INSERT INTO inventory.StockReceipt(BranchId,SupplierId,ReceivedBy,Status,TotalCost,Note,ReceiptDate)
    VALUES(@curB,@curSup,@curMgr,'CONFIRMED',@openQty*0.05,N'Nhập đầu kỳ (demo)',DATEADD(DAY,-31,@now));
    SET @rid = SCOPE_IDENTITY();
    INSERT INTO inventory.StockReceiptDetail(StockReceiptId,IngredientId,Quantity,UnitCost,Unit)
    SELECT @rid, i.IngredientId, @openQty, 0.05, NULL
    FROM catalog.Ingredient i WHERE i.IngredientType='RAW' AND i.IsActive=1;
    INSERT INTO inventory.InventoryTransaction(BranchId,IngredientId,ChangeQty,TxnType,RefTable,RefId,CreatedBy,CreatedAt)
    SELECT @curB, i.IngredientId, @openQty, 'RECEIPT','StockReceipt',@rid,@curMgr,DATEADD(DAY,-31,@now)
    FROM catalog.Ingredient i WHERE i.IngredientType='RAW' AND i.IsActive=1;

    -- Pha sẵn (đủ phủ nhu cầu 31 ngày): Cold Brew 80000ml (tiêu 16000g cà phê)
    -- + Syrup Đào 30000ml (tiêu 18750g đào + 11250g đường)
    DECLARE @iCafe INT=(SELECT IngredientId FROM catalog.Ingredient WHERE Name=N'Cà phê hạt');
    DECLARE @iCold INT=(SELECT IngredientId FROM catalog.Ingredient WHERE Name=N'Cold Brew');
    DECLARE @iDao  INT=(SELECT IngredientId FROM catalog.Ingredient WHERE Name=N'Đào ngâm');
    DECLARE @iDuong INT=(SELECT IngredientId FROM catalog.Ingredient WHERE Name=N'Đường');
    DECLARE @iSyrup INT=(SELECT IngredientId FROM catalog.Ingredient WHERE Name=N'Syrup Đào');
    DECLARE @pbc INT, @pbs INT;
    INSERT INTO inventory.PrepBatch(BranchId,PreppedIngredientId,QuantityProduced,MadeBy,MadeAt,ExpiresAt,Status)
    VALUES(@curB,@iCold,80000,@curBar,DATEADD(DAY,-30,@now),DATEADD(DAY,3,@now),'ACTIVE');
    SET @pbc = SCOPE_IDENTITY();
    INSERT INTO inventory.InventoryTransaction(BranchId,IngredientId,ChangeQty,TxnType,RefTable,RefId,CreatedBy,CreatedAt) VALUES
     (@curB,@iCafe,-16000,'PREP_OUT','PrepBatch',@pbc,@curBar,DATEADD(DAY,-30,@now)),
     (@curB,@iCold,80000,'PREP_IN','PrepBatch',@pbc,@curBar,DATEADD(DAY,-30,@now));
    INSERT INTO inventory.PrepBatch(BranchId,PreppedIngredientId,QuantityProduced,MadeBy,MadeAt,ExpiresAt,Status)
    VALUES(@curB,@iSyrup,30000,@curBar,DATEADD(DAY,-30,@now),DATEADD(DAY,5,@now),'ACTIVE');
    SET @pbs = SCOPE_IDENTITY();
    INSERT INTO inventory.InventoryTransaction(BranchId,IngredientId,ChangeQty,TxnType,RefTable,RefId,CreatedBy,CreatedAt) VALUES
     (@curB,@iDao,-18750,'PREP_OUT','PrepBatch',@pbs,@curBar,DATEADD(DAY,-30,@now)),
     (@curB,@iDuong,-11250,'PREP_OUT','PrepBatch',@pbs,@curBar,DATEADD(DAY,-30,@now)),
     (@curB,@iSyrup,30000,'PREP_IN','PrepBatch',@pbs,@curBar,DATEADD(DAY,-30,@now));

    FETCH NEXT FROM cB INTO @curB,@curMgr,@curBar,@curSup;
END
CLOSE cB; DEALLOCATE cB;

-- 1 phiếu nhập DRAFT (chờ xác nhận) mỗi chi nhánh — chưa ghi sổ cái
INSERT INTO inventory.StockReceipt(BranchId,SupplierId,ReceivedBy,Status,TotalCost,Note,ReceiptDate)
SELECT x.BranchId,x.Sup,x.Mgr,'DRAFT',0,N'Phiếu nháp chờ xác nhận',@now FROM @bx x;
INSERT INTO inventory.StockReceiptDetail(StockReceiptId,IngredientId,Quantity,UnitCost,Unit)
SELECT sr.StockReceiptId,(SELECT IngredientId FROM catalog.Ingredient WHERE Name=N'Cà phê hạt'),1000,0.30,N'Túi'
FROM inventory.StockReceipt sr WHERE sr.Status='DRAFT';

/* ===========================================================================
   C3) VOUCHER (nhiều loại)
   =========================================================================== */
IF NOT EXISTS(SELECT 1 FROM payment.Voucher WHERE Code='WELCOME10')
INSERT INTO payment.Voucher(Code,DiscountType,DiscountValue,MinOrderAmount,Scope,StartDate,EndDate,UsageLimit) VALUES
 ('WELCOME10','PERCENT',10,0,'CHAIN',DATEADD(DAY,-40,@now),DATEADD(MONTH,2,@now),5000);
IF NOT EXISTS(SELECT 1 FROM payment.Voucher WHERE Code='GIAM15K')
INSERT INTO payment.Voucher(Code,DiscountType,DiscountValue,MinOrderAmount,Scope,StartDate,EndDate,UsageLimit) VALUES
 ('GIAM15K','FIXED',15000,80000,'CHAIN',DATEADD(DAY,-40,@now),DATEADD(MONTH,1,@now),3000);
IF NOT EXISTS(SELECT 1 FROM payment.Voucher WHERE Code='THUDUC20')
INSERT INTO payment.Voucher(Code,DiscountType,DiscountValue,MinOrderAmount,Scope,BranchId,StartDate,EndDate,UsageLimit) VALUES
 ('THUDUC20','PERCENT',20,0,'BRANCH',@b2,DATEADD(DAY,-20,@now),DATEADD(MONTH,1,@now),500);
IF NOT EXISTS(SELECT 1 FROM payment.Voucher WHERE Code='EXPIRED5')
INSERT INTO payment.Voucher(Code,DiscountType,DiscountValue,MinOrderAmount,Scope,StartDate,EndDate,UsageLimit,IsActive) VALUES
 ('EXPIRED5','PERCENT',5,0,'CHAIN',DATEADD(DAY,-90,@now),DATEADD(DAY,-30,@now),1000,0);
DECLARE @vGrand INT = (SELECT VoucherId FROM payment.Voucher WHERE Code='GRANDOPENING');

/* ===========================================================================
   C4) HR — mẫu ca, xếp ca (tuần này + hôm nay), chấm công, lương 3 tháng, handover
   =========================================================================== */
INSERT INTO hr.ShiftTemplate(BranchId,Name,StartTime,EndTime)
SELECT br.BranchId, v.Name, v.S, v.E
FROM org.Branch br
CROSS JOIN (VALUES (N'Ca sáng',CAST('07:00' AS TIME),CAST('12:00' AS TIME)),
                   (N'Ca chiều',CAST('12:00' AS TIME),CAST('17:00' AS TIME)),
                   (N'Ca tối',CAST('17:00' AS TIME),CAST('22:00' AS TIME))) v(Name,S,E)
WHERE NOT EXISTS (SELECT 1 FROM hr.ShiftTemplate st WHERE st.BranchId=br.BranchId AND st.Name=v.Name);

-- Xếp ca hôm qua + hôm nay cho mỗi nhân viên non-admin (ca sáng) → manager dashboard "nhân sự trong ca"
DECLARE @yest DATE = DATEADD(DAY,-1,@today);
INSERT INTO hr.ShiftAssignment(ShiftTemplateId,UserId,WorkDate)
SELECT st.ShiftTemplateId, u.UserId, d.WorkDate
FROM iam.[User] u
JOIN iam.Role r ON r.RoleId=u.RoleId AND r.Code<>'ADMIN'
JOIN hr.ShiftTemplate st ON st.BranchId=u.BranchId AND st.Name=N'Ca sáng'
CROSS JOIN (VALUES(@yest),(@today)) d(WorkDate)
WHERE u.BranchId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM hr.ShiftAssignment sa WHERE sa.ShiftTemplateId=st.ShiftTemplateId AND sa.UserId=u.UserId AND sa.WorkDate=d.WorkDate);

-- Chấm công: hôm qua APPROVED (đủ giờ), hôm nay PENDING (đang trong ca)
DECLARE @inY DATETIME2 = DATEADD(HOUR,7,CAST(@yest AS DATETIME2));
DECLARE @outY DATETIME2 = DATEADD(HOUR,12,CAST(@yest AS DATETIME2));
DECLARE @inT DATETIME2 = DATEADD(HOUR,7,CAST(@today AS DATETIME2));
INSERT INTO hr.Attendance(ShiftAssignmentId,CheckInAt,CheckOutAt,Status,ApprovedBy)
SELECT sa.ShiftAssignmentId,@inY,@outY,'APPROVED',br.ManagerUserId
FROM hr.ShiftAssignment sa
JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId
JOIN org.Branch br ON br.BranchId=st.BranchId
WHERE sa.WorkDate=@yest;
INSERT INTO hr.Attendance(ShiftAssignmentId,CheckInAt,CheckOutAt,Status,ApprovedBy)
SELECT sa.ShiftAssignmentId,@inT,NULL,'PENDING',NULL
FROM hr.ShiftAssignment sa WHERE sa.WorkDate=@today;
-- 1 bản ghi REJECTED cho phong phú
UPDATE TOP (1) hr.Attendance SET Status='REJECTED'
WHERE Status='PENDING';

-- Bảng lương 3 tháng gần nhất cho mọi nhân sự non-admin
DECLARE @m1 CHAR(7)=LEFT(CONVERT(varchar(10),DATEADD(MONTH,-1,@now),23),7);
DECLARE @m2 CHAR(7)=LEFT(CONVERT(varchar(10),DATEADD(MONTH,-2,@now),23),7);
DECLARE @m3 CHAR(7)=LEFT(CONVERT(varchar(10),DATEADD(MONTH,-3,@now),23),7);
INSERT INTO hr.Payroll(BranchId,UserId,PayMonth,WorkedHours,HourlyRate,UpdatedBy,UpdatedAt)
SELECT u.BranchId,u.UserId,m.PayMonth,
       160 + (u.UserId%3)*8,
       CASE r.Code WHEN 'BRANCH_MANAGER' THEN 45000 WHEN 'CASHIER' THEN 32000 ELSE 30000 END,
       br.ManagerUserId,@now
FROM iam.[User] u
JOIN iam.Role r ON r.RoleId=u.RoleId AND r.Code<>'ADMIN'
JOIN org.Branch br ON br.BranchId=u.BranchId
CROSS JOIN (VALUES(@m1),(@m2),(@m3)) m(PayMonth)
WHERE u.BranchId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM hr.Payroll p WHERE p.UserId=u.UserId AND p.PayMonth=m.PayMonth);

-- Bàn giao ca (mỗi chi nhánh)
INSERT INTO hr.ShiftHandover(BranchId,Note,CreatedBy,CreatedAt)
SELECT x.BranchId,N'Cuối ca: Cold Brew còn đủ cho sáng mai, vệ sinh máy xay số 2.',x.Bar,DATEADD(DAY,-1,@now) FROM @bx x;

/* ===========================================================================
   C5) GENERATOR 31 NGÀY LỊCH SỬ BÁN (PAID + trừ kho). Tất định (modulo).
   =========================================================================== */
-- Danh sách sản phẩm đánh số để pick tất định
DECLARE @PZ TABLE(Seq INT IDENTITY(1,1), ProductId INT, Price DECIMAL(12,2));
INSERT INTO @PZ(ProductId,Price) SELECT ProductId,BasePrice FROM catalog.Product WHERE IsActive=1 ORDER BY ProductId;
DECLARE @pcount INT = (SELECT COUNT(*) FROM @PZ);

DECLARE @k BIGINT = 0;               -- bộ đếm toàn cục (tất định)
DECLARE @off INT;                    -- 30..0 ngày trước (0 = hôm nay)
DECLARE @rowB INT;

DECLARE cGen CURSOR LOCAL FAST_FORWARD FOR SELECT BranchId,Mgr,Bar,Cas FROM @bx;
DECLARE @gB INT,@gMgr INT,@gBar INT,@gCas INT;
OPEN cGen; FETCH NEXT FROM cGen INTO @gB,@gMgr,@gBar,@gCas;
WHILE @@FETCH_STATUS=0
BEGIN
    DECLARE @gTable INT = (SELECT TOP(1) DiningTableId FROM sales.DiningTable WHERE BranchId=@gB ORDER BY DiningTableId);
    SET @off = 30;
    WHILE @off >= 0
    BEGIN
        DECLARE @day DATE = DATEADD(DAY,-@off,@today);
        -- Ca thu ngân đóng cho ngày này (mở 7h, đóng 22h)
        DECLARE @csDay INT;
        INSERT INTO payment.CashierShift(BranchId,CashierId,OpeningCash,ClosingCash,OpenedAt,ClosedAt)
        VALUES(@gB,@gCas,500000,500000,DATEADD(HOUR,7,CAST(@day AS DATETIME2)),DATEADD(HOUR,22,CAST(@day AS DATETIME2)));
        SET @csDay = SCOPE_IDENTITY();

        -- Số hoá đơn/ngày: 6..12 (cuối tuần cao hơn), tất định theo ngày+chi nhánh
        DECLARE @nb INT = 6 + ((@off + @gB) % 5) + (CASE WHEN DATEPART(WEEKDAY,@day) IN (1,7) THEN 2 ELSE 0 END);
        DECLARE @bi INT = 0;
        WHILE @bi < @nb
        BEGIN
            SET @k += 1;
            DECLARE @when DATETIME2 = DATEADD(MINUTE, 60*(8 + (@bi % 13)) + (@k%59), CAST(@day AS DATETIME2));
            DECLARE @src VARCHAR(8) = CASE WHEN (@k%4)=0 THEN 'QR' ELSE 'COUNTER' END;

            DECLARE @ses INT;
            INSERT INTO sales.TableSession(BranchId,DiningTableId,OpenedBy,OpenedAt,ClosedAt,Status)
            VALUES(@gB,@gTable,CASE WHEN @src='QR' THEN NULL ELSE @gCas END,@when,DATEADD(MINUTE,50,@when),'CLOSED');
            SET @ses = SCOPE_IDENTITY();
            DECLARE @ord INT;
            INSERT INTO sales.Orders(BranchId,TableSessionId,Source,OrderType,Status,CreatedBy,CreatedAt)
            VALUES(@gB,@ses,@src,'DINE_IN','COMPLETED',CASE WHEN @src='QR' THEN NULL ELSE @gCas END,@when);
            SET @ord = SCOPE_IDENTITY();

            -- 1..3 món/đơn, pick tất định
            DECLARE @ni INT = 1 + (@k%3);
            DECLARE @j INT = 0;
            WHILE @j < @ni
            BEGIN
                DECLARE @seq INT = ((@k*7 + @j*5) % @pcount) + 1;
                DECLARE @pid2 INT=(SELECT ProductId FROM @PZ WHERE Seq=@seq);
                DECLARE @price2 DECIMAL(12,2)=(SELECT Price FROM @PZ WHERE Seq=@seq);
                DECLARE @qty INT = 1 + ((@k+@j)%2);
                INSERT INTO sales.OrderItem(OrderId,ProductId,Quantity,UnitPrice,Status,StartedAt,DoneAt)
                VALUES(@ord,@pid2,@qty,@price2,'SERVED',@when,DATEADD(MINUTE,7,@when));
                SET @j += 1;
            END

            -- Trừ kho theo công thức cho mọi item của đơn
            INSERT INTO inventory.InventoryTransaction(BranchId,IngredientId,ChangeQty,TxnType,RefTable,RefId,CreatedBy,CreatedAt)
            SELECT @gB, pr.IngredientId, -(pr.Quantity*oi.Quantity),'DEDUCT','OrderItem',oi.OrderItemId,@gBar,DATEADD(MINUTE,7,@when)
            FROM sales.OrderItem oi JOIN catalog.ProductRecipe pr ON pr.ProductId=oi.ProductId
            WHERE oi.OrderId=@ord;

            -- Hoá đơn PAID
            DECLARE @sub DECIMAL(14,2)=(SELECT SUM(UnitPrice*Quantity) FROM sales.OrderItem WHERE OrderId=@ord);
            DECLARE @useV BIT = CASE WHEN (@k%5)=0 THEN 1 ELSE 0 END;
            DECLARE @disc DECIMAL(14,2)= CASE WHEN @useV=1 THEN ROUND(@sub*0.2,0) ELSE 0 END;
            DECLARE @net DECIMAL(14,2)=@sub-@disc;
            DECLARE @vat DECIMAL(14,2)=ROUND(@net*0.08,0);
            DECLARE @tot DECIMAL(14,2)=@net+@vat;
            DECLARE @pm VARCHAR(10)=CASE (@k%3) WHEN 0 THEN 'CASH' WHEN 1 THEN 'TRANSFER' ELSE 'QR_BANK' END;
            DECLARE @paidAt DATETIME2=DATEADD(MINUTE,50,@when);
            DECLARE @bill INT;
            INSERT INTO payment.Bill(BranchId,TableSessionId,CashierShiftId,Subtotal,VatAmount,DiscountAmount,TotalAmount,VoucherId,PaymentMethod,Status,PaidAt,CreatedAt)
            VALUES(@gB,@ses,@csDay,@sub,@vat,@disc,@tot,CASE WHEN @useV=1 THEN @vGrand ELSE NULL END,@pm,'PAID',@paidAt,@when);
            SET @bill = SCOPE_IDENTITY();
            INSERT INTO payment.BillItem(BillId,OrderItemId,Amount)
            SELECT @bill,OrderItemId,UnitPrice*Quantity FROM sales.OrderItem WHERE OrderId=@ord;
            IF @useV=1
            BEGIN
                INSERT INTO payment.VoucherRedemption(VoucherId,BillId,DiscountApplied,RedeemedAt) VALUES(@vGrand,@bill,@disc,@paidAt);
                UPDATE payment.Voucher SET UsedCount=UsedCount+1 WHERE VoucherId=@vGrand;
            END
            SET @bi += 1;
        END
        SET @off -= 1;
    END
    FETCH NEXT FROM cGen INTO @gB,@gMgr,@gBar,@gCas;
END
CLOSE cGen; DEALLOCATE cGen;

/* ===========================================================================
   C6) STORY HÔM NAY — realtime cho mọi chi nhánh (KDS/Cashier sống)
   =========================================================================== */
DECLARE @pSua INT=(SELECT ProductId FROM catalog.Product WHERE Name=N'Cà phê sữa');
DECLARE @pCold INT=(SELECT ProductId FROM catalog.Product WHERE Name=N'Cold Brew');
DECLARE @pTra INT=(SELECT ProductId FROM catalog.Product WHERE Name=N'Trà Đào');
DECLARE @pLatte INT=(SELECT ProductId FROM catalog.Product WHERE Name=N'Latte');
DECLARE @iCafe2 INT=(SELECT IngredientId FROM catalog.Ingredient WHERE Name=N'Cà phê hạt');
DECLARE @iSua2 INT=(SELECT IngredientId FROM catalog.Ingredient WHERE Name=N'Sữa đặc');
DECLARE @iDa2 INT=(SELECT IngredientId FROM catalog.Ingredient WHERE Name=N'Đá');

DECLARE cLive CURSOR LOCAL FAST_FORWARD FOR SELECT BranchId,Bar,Cas FROM @bx;
DECLARE @lB INT,@lBar INT,@lCas INT;
OPEN cLive; FETCH NEXT FROM cLive INTO @lB,@lBar,@lCas;
WHILE @@FETCH_STATUS=0
BEGIN
    -- Ca thu ngân ĐANG MỞ hôm nay
    INSERT INTO payment.CashierShift(BranchId,CashierId,OpeningCash,OpenedAt)
    VALUES(@lB,@lCas,500000,@inT);

    DECLARE @tA INT=(SELECT TOP(1) DiningTableId FROM sales.DiningTable WHERE BranchId=@lB ORDER BY DiningTableId);
    DECLARE @tB INT=(SELECT DiningTableId FROM sales.DiningTable WHERE BranchId=@lB AND TableNumber=N'Bàn 02');
    DECLARE @tC INT=(SELECT DiningTableId FROM sales.DiningTable WHERE BranchId=@lB AND TableNumber=N'Bàn 03');

    -- Phiên OPEN có KDS đủ trạng thái (quầy)
    DECLARE @sO INT;
    INSERT INTO sales.TableSession(BranchId,DiningTableId,OpenedBy,OpenedAt,Status)
    VALUES(@lB,@tB,@lCas,DATEADD(MINUTE,-25,@now),'OPEN');
    SET @sO = SCOPE_IDENTITY();
    DECLARE @oO INT;
    INSERT INTO sales.Orders(BranchId,TableSessionId,Source,OrderType,Status,CreatedBy,CreatedAt)
    VALUES(@lB,@sO,'COUNTER','DINE_IN','ACTIVE',@lCas,DATEADD(MINUTE,-25,@now));
    SET @oO = SCOPE_IDENTITY();
    -- READY (đã trừ kho)
    DECLARE @oiR INT;
    INSERT INTO sales.OrderItem(OrderId,ProductId,Quantity,UnitPrice,Status,StartedAt,DoneAt)
    VALUES(@oO,@pSua,1,29000,'READY',DATEADD(MINUTE,-20,@now),DATEADD(MINUTE,-12,@now));
    SET @oiR = SCOPE_IDENTITY();
    INSERT INTO inventory.InventoryTransaction(BranchId,IngredientId,ChangeQty,TxnType,RefTable,RefId,CreatedBy,CreatedAt) VALUES
     (@lB,@iCafe2,-18,'DEDUCT','OrderItem',@oiR,@lBar,DATEADD(MINUTE,-12,@now)),
     (@lB,@iSua2,-30,'DEDUCT','OrderItem',@oiR,@lBar,DATEADD(MINUTE,-12,@now));
    -- MAKING (chưa trừ)
    INSERT INTO sales.OrderItem(OrderId,ProductId,Quantity,UnitPrice,Status,StartedAt)
    VALUES(@oO,@pLatte,1,49000,'MAKING',DATEADD(MINUTE,-6,@now));
    -- WAITING
    INSERT INTO sales.OrderItem(OrderId,ProductId,Quantity,UnitPrice,Status,Note)
    VALUES(@oO,@pTra,2,39000,'WAITING',N'Ít đá');

    -- Phiên QR ẩn danh (tracking)
    DECLARE @sQ INT;
    INSERT INTO sales.TableSession(BranchId,DiningTableId,OpenedBy,OpenedAt,Status)
    VALUES(@lB,@tC,NULL,DATEADD(MINUTE,-8,@now),'OPEN');
    SET @sQ = SCOPE_IDENTITY();
    DECLARE @oQ INT;
    INSERT INTO sales.Orders(BranchId,TableSessionId,Source,OrderType,Status,CreatedBy,CreatedAt)
    VALUES(@lB,@sQ,'QR','DINE_IN','ACTIVE',NULL,DATEADD(MINUTE,-8,@now));
    SET @oQ = SCOPE_IDENTITY();
    INSERT INTO sales.OrderItem(OrderId,ProductId,Quantity,UnitPrice,Status) VALUES
     (@oQ,@pCold,1,45000,'WAITING'),(@oQ,@pTra,1,39000,'WAITING');

    UPDATE sales.DiningTable SET Status='OCCUPIED' WHERE DiningTableId IN (@tB,@tC);

    -- Waste hôm nay
    DECLARE @wl INT;
    INSERT INTO inventory.WasteLog(BranchId,IngredientId,Quantity,WasteType,Reason,LoggedBy,LoggedAt,Status)
    VALUES(@lB,@iDa2,200,'SPILL',N'Đổ đá khi sang khay',@lBar,DATEADD(MINUTE,-40,@now),'ACTIVE');
    SET @wl = SCOPE_IDENTITY();
    INSERT INTO inventory.InventoryTransaction(BranchId,IngredientId,ChangeQty,TxnType,RefTable,RefId,CreatedBy,CreatedAt)
    VALUES(@lB,@iDa2,-200,'WASTE','WasteLog',@wl,@lBar,DATEADD(MINUTE,-40,@now));

    -- Điều chỉnh tồn hôm nay (Manager)
    DECLARE @mgrL INT=(SELECT ManagerUserId FROM org.Branch WHERE BranchId=@lB);
    DECLARE @adj INT;
    INSERT INTO inventory.StockAdjustment(BranchId,IngredientId,SystemQty,ActualQty,Reason,Unit,AdjustedBy,AdjustedAt)
    VALUES(@lB,@iSua2,3000,2950,N'Kiểm kê cuối ngày thiếu 50ml',NULL,@mgrL,DATEADD(MINUTE,-30,@now));
    SET @adj = SCOPE_IDENTITY();
    INSERT INTO inventory.InventoryTransaction(BranchId,IngredientId,ChangeQty,TxnType,RefTable,RefId,CreatedBy,CreatedAt)
    VALUES(@lB,@iSua2,-50,'ADJUST','StockAdjustment',@adj,@mgrL,DATEADD(MINUTE,-30,@now));

    FETCH NEXT FROM cLive INTO @lB,@lBar,@lCas;
END
CLOSE cLive; DEALLOCATE cLive;

-- Bảng 86 (hết tạm thời): tắt Matcha đá xay ở CN01
UPDATE catalog.BranchMenu SET Is86=1, BackInEta=DATEADD(HOUR,2,@now)
WHERE BranchId=@b1 AND ProductId=(SELECT ProductId FROM catalog.Product WHERE Name=N'Matcha đá xay');

/* ===========================================================================
   C7) OUTBOX EVENT — dấu vết sự kiện
   =========================================================================== */
INSERT INTO ops.OutboxEvent(EventType,AggregateId,BranchId,Payload,CreatedAt,ProcessedAt)
SELECT TOP (200) 'payment.completed',CAST(b.BillId AS VARCHAR),b.BranchId,
       N'{"method":"'+ISNULL(b.PaymentMethod,'?')+N'","total":'+CAST(CAST(b.TotalAmount AS INT) AS NVARCHAR(20))+N'}',
       b.PaidAt,b.PaidAt
FROM payment.Bill b WHERE b.Status='PAID' ORDER BY b.BillId DESC;

/* ===========================================================================
   C8) CHỐT TỒN KHO = Σ SỔ CÁI + ngưỡng low-stock + stock.low
   =========================================================================== */
DELETE FROM inventory.BranchInventory;
INSERT INTO inventory.BranchInventory(BranchId,IngredientId,QuantityOnHand,MinThreshold,UpdatedAt)
SELECT BranchId,IngredientId,SUM(ChangeQty),0,@now
FROM inventory.InventoryTransaction GROUP BY BranchId,IngredientId;

-- Ngưỡng mặc định + ép low-stock cho đồ PHA SẴN (Cold Brew/Syrup) ở mọi chi nhánh
UPDATE inventory.BranchInventory SET MinThreshold = 3000;
UPDATE bi SET MinThreshold = bi.QuantityOnHand + 3000
FROM inventory.BranchInventory bi
JOIN catalog.Ingredient i ON i.IngredientId=bi.IngredientId
WHERE i.IngredientType='PREPPED';

-- Sự kiện tồn thấp
INSERT INTO ops.OutboxEvent(EventType,AggregateId,BranchId,Payload,CreatedAt)
SELECT 'stock.low',CAST(bi.IngredientId AS VARCHAR),bi.BranchId,
       N'{"onHand":'+CAST(CAST(bi.QuantityOnHand AS INT) AS NVARCHAR(20))+N',"min":'+CAST(CAST(bi.MinThreshold AS INT) AS NVARCHAR(20))+N'}',@now
FROM inventory.BranchInventory bi WHERE bi.QuantityOnHand <= bi.MinThreshold;

COMMIT;
GO

/* ===========================================================================
   C9) KIỂM TRA BẤT BIẾN — phải in "0"
   =========================================================================== */
PRINT N'== PART C: kiểm tra bất biến sổ cái ==';
SELECT COUNT(*) AS LedgerMismatchCount
FROM (
    SELECT bi.BranchId,bi.IngredientId,bi.QuantityOnHand,ISNULL(SUM(it.ChangeQty),0) AS LedgerSum
    FROM inventory.BranchInventory bi
    LEFT JOIN inventory.InventoryTransaction it ON it.BranchId=bi.BranchId AND it.IngredientId=bi.IngredientId
    GROUP BY bi.BranchId,bi.IngredientId,bi.QuantityOnHand
    HAVING bi.QuantityOnHand <> ISNULL(SUM(it.ChangeQty),0)
) d;
GO
PRINT N'== DEMO HOÀN TẤT — 3 chi nhánh, 15 món, ~31 ngày doanh thu · mật khẩu 123456 ==';
GO
