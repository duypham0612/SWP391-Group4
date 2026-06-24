-- ============================================================
--  Script tao Database cho du an: My Coffee House (SWP391)
--  Tu dong export boi Antigravity
-- ============================================================

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'MyCoffeeHouse')
BEGIN
    CREATE DATABASE MyCoffeeHouse;
END
GO

USE MyCoffeeHouse;
GO

-- Table: Attendance
IF OBJECT_ID('Attendance', 'U') IS NOT NULL
    DROP TABLE [Attendance];
GO
CREATE TABLE [Attendance] (
    [AttendanceID] int IDENTITY(1,1) NOT NULL,
    [EmployeeID] int NOT NULL,
    [ShiftID] int NOT NULL,
    [Date] date NOT NULL,
    [CheckInTime] datetime NULL,
    [CheckOutTime] datetime NULL,
    [Status] nvarchar(50) NULL,
    PRIMARY KEY ([AttendanceID])
);
GO

-- Table: Attendances
IF OBJECT_ID('Attendances', 'U') IS NOT NULL
    DROP TABLE [Attendances];
GO
CREATE TABLE [Attendances] (
    [AttendanceID] int IDENTITY(1,1) NOT NULL,
    [EmployeeID] int NULL,
    [ShiftID] int NULL,
    [Date] date NULL DEFAULT (getdate()),
    [CheckInTime] datetime NULL,
    [CheckOutTime] datetime NULL,
    [Status] nvarchar(50) NULL,
    PRIMARY KEY ([AttendanceID])
);
GO

-- Table: Branches
IF OBJECT_ID('Branches', 'U') IS NOT NULL
    DROP TABLE [Branches];
GO
CREATE TABLE [Branches] (
    [BranchID] int IDENTITY(1,1) NOT NULL,
    [BranchName] nvarchar(100) NOT NULL,
    [Address] nvarchar(255) NOT NULL,
    [Phone] varchar(15) NULL,
    [IsActive] bit NULL DEFAULT ((1)),
    [ManagerID] int NULL,
    PRIMARY KEY ([BranchID])
);
GO

-- Table: Categories
IF OBJECT_ID('Categories', 'U') IS NOT NULL
    DROP TABLE [Categories];
GO
CREATE TABLE [Categories] (
    [CategoryID] int IDENTITY(1,1) NOT NULL,
    [CategoryName] nvarchar(100) NOT NULL,
    [Description] nvarchar(255) NULL,
    PRIMARY KEY ([CategoryID]),
    CONSTRAINT [UQ__Categori__8517B2E019858C29] UNIQUE ([CategoryName]),
    CONSTRAINT [UQ__Categori__8517B2E00A539514] UNIQUE ([CategoryName]),
    CONSTRAINT [UQ__Categori__8517B2E03DC2134C] UNIQUE ([CategoryName]),
    CONSTRAINT [UQ__Categori__8517B2E0F1E76669] UNIQUE ([CategoryName]),
    CONSTRAINT [UQ__Categori__8517B2E018D3CD13] UNIQUE ([CategoryName])
);
GO

-- Table: Customers
IF OBJECT_ID('Customers', 'U') IS NOT NULL
    DROP TABLE [Customers];
GO
CREATE TABLE [Customers] (
    [CustomerID] int NOT NULL,
    [MemberRank] nvarchar(50) NULL DEFAULT ('Member'),
    [CurrentPoints] int NULL DEFAULT ((0)),
    PRIMARY KEY ([CustomerID])
);
GO

-- Table: Employees
IF OBJECT_ID('Employees', 'U') IS NOT NULL
    DROP TABLE [Employees];
GO
CREATE TABLE [Employees] (
    [EmployeeID] int NOT NULL,
    [BranchID] int NULL,
    [SalaryRate] decimal(18,2) NULL DEFAULT ((0)),
    [HireDate] date NULL DEFAULT (getdate()),
    PRIMARY KEY ([EmployeeID])
);
GO

-- Table: Feedbacks
IF OBJECT_ID('Feedbacks', 'U') IS NOT NULL
    DROP TABLE [Feedbacks];
GO
CREATE TABLE [Feedbacks] (
    [FeedbackID] int IDENTITY(1,1) NOT NULL,
    [CustomerID] int NULL,
    [BranchID] int NULL,
    [Rating] int NULL,
    [Comment] nvarchar(max) NULL,
    [CreatedAt] datetime NULL DEFAULT (getdate()),
    PRIMARY KEY ([FeedbackID])
);
GO

-- Table: Ingredients
IF OBJECT_ID('Ingredients', 'U') IS NOT NULL
    DROP TABLE [Ingredients];
GO
CREATE TABLE [Ingredients] (
    [IngredientID] int IDENTITY(1,1) NOT NULL,
    [IngredientName] nvarchar(100) NOT NULL,
    [Unit] nvarchar(50) NOT NULL,
    [Description] nvarchar(255) NULL,
    PRIMARY KEY ([IngredientID])
);
GO

-- Table: Inventory
IF OBJECT_ID('Inventory', 'U') IS NOT NULL
    DROP TABLE [Inventory];
GO
CREATE TABLE [Inventory] (
    [BranchID] int NOT NULL,
    [IngredientID] int NOT NULL,
    [Quantity] decimal(10,2) NOT NULL,
    [MinRequired] decimal(10,2) NOT NULL,
    [LastUpdated] datetime NULL DEFAULT (getdate()),
    PRIMARY KEY ([BranchID], [IngredientID])
);
GO

-- Table: Notifications
IF OBJECT_ID('Notifications', 'U') IS NOT NULL
    DROP TABLE [Notifications];
GO
CREATE TABLE [Notifications] (
    [NotificationID] int IDENTITY(1,1) NOT NULL,
    [UserID] int NULL,
    [Title] nvarchar(100) NOT NULL,
    [Message] nvarchar(max) NOT NULL,
    [IsRead] bit NULL DEFAULT ((0)),
    [CreatedAt] datetime NULL DEFAULT (getdate()),
    PRIMARY KEY ([NotificationID])
);
GO

-- Table: OrderDetails
IF OBJECT_ID('OrderDetails', 'U') IS NOT NULL
    DROP TABLE [OrderDetails];
GO
CREATE TABLE [OrderDetails] (
    [OrderDetailID] int IDENTITY(1,1) NOT NULL,
    [OrderID] int NULL,
    [ProductID] int NULL,
    [Quantity] int NOT NULL,
    [UnitPrice] decimal(18,2) NOT NULL,
    [Note] nvarchar(255) NULL,
    [ItemStatus] nvarchar(50) NULL DEFAULT ('Pending'),
    [StartedAt] datetime NULL,
    [CompletedAt] datetime NULL,
    PRIMARY KEY ([OrderDetailID])
);
GO

-- Table: Orders
IF OBJECT_ID('Orders', 'U') IS NOT NULL
    DROP TABLE [Orders];
GO
CREATE TABLE [Orders] (
    [OrderID] int IDENTITY(1,1) NOT NULL,
    [BranchID] int NULL,
    [TableID] int NULL,
    [CashierID] int NULL,
    [OrderType] nvarchar(50) NOT NULL,
    [TotalAmount] decimal(18,2) NULL DEFAULT ((0)),
    [DiscountAmount] decimal(18,2) NULL DEFAULT ((0)),
    [FinalAmount] decimal(18,2) NULL DEFAULT ((0)),
    [OrderStatus] nvarchar(50) NULL DEFAULT ('Pending'),
    [OrderDate] datetime NULL DEFAULT (getdate()),
    [Priority] int NOT NULL DEFAULT ((0)),
    PRIMARY KEY ([OrderID])
);
GO

-- Table: Payments
IF OBJECT_ID('Payments', 'U') IS NOT NULL
    DROP TABLE [Payments];
GO
CREATE TABLE [Payments] (
    [PaymentID] int IDENTITY(1,1) NOT NULL,
    [OrderID] int NULL,
    [CustomerID] int NULL,
    [VoucherID] int NULL,
    [PaymentMethod] nvarchar(50) NOT NULL,
    [PaymentTime] datetime NULL DEFAULT (getdate()),
    [TransactionNo] varchar(100) NULL,
    PRIMARY KEY ([PaymentID])
);
GO

-- Table: ProductBranches
IF OBJECT_ID('ProductBranches', 'U') IS NOT NULL
    DROP TABLE [ProductBranches];
GO
CREATE TABLE [ProductBranches] (
    [ProductID] int NOT NULL,
    [BranchID] int NOT NULL,
    [CustomPrice] decimal(18,2) NULL,
    [IsAvailable] bit NULL DEFAULT ((1)),
    PRIMARY KEY ([ProductID], [BranchID])
);
GO

-- Table: Products
IF OBJECT_ID('Products', 'U') IS NOT NULL
    DROP TABLE [Products];
GO
CREATE TABLE [Products] (
    [ProductID] int IDENTITY(1,1) NOT NULL,
    [ProductName] nvarchar(100) NOT NULL,
    [CategoryID] int NULL,
    [BasePrice] decimal(18,2) NOT NULL,
    [ImageURL] nvarchar(500) NULL,
    [Description] nvarchar(max) NULL,
    [IsAvailable] bit NULL DEFAULT ((1)),
    PRIMARY KEY ([ProductID])
);
GO

-- Table: Roles
IF OBJECT_ID('Roles', 'U') IS NOT NULL
    DROP TABLE [Roles];
GO
CREATE TABLE [Roles] (
    [RoleID] int IDENTITY(1,1) NOT NULL,
    [RoleName] nvarchar(50) NOT NULL,
    [Description] nvarchar(255) NULL,
    PRIMARY KEY ([RoleID]),
    CONSTRAINT [UQ__Roles__8A2B6160DCEC36C4] UNIQUE ([RoleName]),
    CONSTRAINT [UQ__Roles__8A2B6160B3C81F43] UNIQUE ([RoleName]),
    CONSTRAINT [UQ__Roles__8A2B61600F5DF67B] UNIQUE ([RoleName]),
    CONSTRAINT [UQ__Roles__8A2B6160EA52F5D0] UNIQUE ([RoleName]),
    CONSTRAINT [UQ__Roles__8A2B6160BC9A3FD8] UNIQUE ([RoleName])
);
GO

-- Table: Shifts
IF OBJECT_ID('Shifts', 'U') IS NOT NULL
    DROP TABLE [Shifts];
GO
CREATE TABLE [Shifts] (
    [ShiftID] int IDENTITY(1,1) NOT NULL,
    [ShiftName] nvarchar(50) NOT NULL,
    [StartTime] time NOT NULL,
    [EndTime] time NOT NULL,
    PRIMARY KEY ([ShiftID])
);
GO

-- Table: Tables
IF OBJECT_ID('Tables', 'U') IS NOT NULL
    DROP TABLE [Tables];
GO
CREATE TABLE [Tables] (
    [TableID] int IDENTITY(1,1) NOT NULL,
    [BranchID] int NULL,
    [TableName] nvarchar(50) NOT NULL,
    [QRCodeURL] nvarchar(500) NULL,
    [Status] nvarchar(50) NULL DEFAULT ('Empty'),
    PRIMARY KEY ([TableID])
);
GO

-- Table: Users
IF OBJECT_ID('Users', 'U') IS NOT NULL
    DROP TABLE [Users];
GO
CREATE TABLE [Users] (
    [UserID] int IDENTITY(1,1) NOT NULL,
    [Username] varchar(50) NOT NULL,
    [Password] varchar(255) NOT NULL,
    [FullName] nvarchar(100) NOT NULL,
    [Email] varchar(100) NULL,
    [Phone] varchar(15) NULL,
    [RoleID] int NULL,
    [IsActive] bit NULL DEFAULT ((1)),
    [CreatedAt] datetime NULL DEFAULT (getdate()),
    PRIMARY KEY ([UserID]),
    CONSTRAINT [UQ__Users__5C7E359E260B02CA] UNIQUE ([Phone]),
    CONSTRAINT [UQ__Users__536C85E44D534923] UNIQUE ([Username]),
    CONSTRAINT [UQ__Users__536C85E4549304CC] UNIQUE ([Username]),
    CONSTRAINT [UQ__Users__A9D10534FD79A8A8] UNIQUE ([Email]),
    CONSTRAINT [UQ__Users__A9D105342853D7AA] UNIQUE ([Email]),
    CONSTRAINT [UQ__Users__A9D105349F3F313D] UNIQUE ([Email]),
    CONSTRAINT [UQ__Users__536C85E482E2171F] UNIQUE ([Username]),
    CONSTRAINT [UQ__Users__5C7E359E61334DEC] UNIQUE ([Phone]),
    CONSTRAINT [UQ__Users__5C7E359E59243501] UNIQUE ([Phone]),
    CONSTRAINT [UQ__Users__536C85E491C1BCC8] UNIQUE ([Username]),
    CONSTRAINT [UQ__Users__5C7E359E25A99C71] UNIQUE ([Phone]),
    CONSTRAINT [UQ__Users__5C7E359E5242981C] UNIQUE ([Phone]),
    CONSTRAINT [UQ__Users__A9D10534BB166829] UNIQUE ([Email]),
    CONSTRAINT [UQ__Users__A9D10534DA914232] UNIQUE ([Email]),
    CONSTRAINT [UQ__Users__536C85E4C63B408F] UNIQUE ([Username])
);
GO

-- Table: Vouchers
IF OBJECT_ID('Vouchers', 'U') IS NOT NULL
    DROP TABLE [Vouchers];
GO
CREATE TABLE [Vouchers] (
    [VoucherID] int IDENTITY(1,1) NOT NULL,
    [VoucherCode] varchar(50) NOT NULL,
    [DiscountValue] decimal(18,2) NOT NULL,
    [IsPercentage] bit NULL DEFAULT ((0)),
    [MinOrderValue] decimal(18,2) NULL DEFAULT ((0)),
    [StartDate] date NULL,
    [EndDate] date NULL,
    [IsActive] bit NULL DEFAULT ((1)),
    PRIMARY KEY ([VoucherID]),
    CONSTRAINT [UQ__Vouchers__7F0ABCA9908828BE] UNIQUE ([VoucherCode]),
    CONSTRAINT [UQ__Vouchers__7F0ABCA954A6C9F7] UNIQUE ([VoucherCode]),
    CONSTRAINT [UQ__Vouchers__7F0ABCA9BE7CD998] UNIQUE ([VoucherCode]),
    CONSTRAINT [UQ__Vouchers__7F0ABCA950AFC538] UNIQUE ([VoucherCode]),
    CONSTRAINT [UQ__Vouchers__7F0ABCA9D5F2A559] UNIQUE ([VoucherCode])
);
GO

-- Foreign Key Constraints
ALTER TABLE [Attendance] ADD CONSTRAINT [FK__Attendanc__Emplo__7908F585] FOREIGN KEY ([EmployeeID]) REFERENCES [Employees] ([EmployeeID]);
ALTER TABLE [Attendance] ADD CONSTRAINT [FK__Attendanc__Shift__79FD19BE] FOREIGN KEY ([ShiftID]) REFERENCES [Shifts] ([ShiftID]);
ALTER TABLE [Attendances] ADD CONSTRAINT [FK__Attendanc__Emplo__05D8E0BE] FOREIGN KEY ([EmployeeID]) REFERENCES [Employees] ([EmployeeID]);
ALTER TABLE [Attendances] ADD CONSTRAINT [FK__Attendanc__Emplo__2F9A1060] FOREIGN KEY ([EmployeeID]) REFERENCES [Employees] ([EmployeeID]);
ALTER TABLE [Attendances] ADD CONSTRAINT [FK__Attendanc__Emplo__3D2915A8] FOREIGN KEY ([EmployeeID]) REFERENCES [Employees] ([EmployeeID]);
ALTER TABLE [Attendances] ADD CONSTRAINT [FK__Attendanc__Emplo__5B78929E] FOREIGN KEY ([EmployeeID]) REFERENCES [Employees] ([EmployeeID]);
ALTER TABLE [Attendances] ADD CONSTRAINT [FK__Attendanc__Emplo__76619304] FOREIGN KEY ([EmployeeID]) REFERENCES [Employees] ([EmployeeID]);
ALTER TABLE [Attendances] ADD CONSTRAINT [FK__Attendanc__Shift__06CD04F7] FOREIGN KEY ([ShiftID]) REFERENCES [Shifts] ([ShiftID]);
ALTER TABLE [Attendances] ADD CONSTRAINT [FK__Attendanc__Shift__308E3499] FOREIGN KEY ([ShiftID]) REFERENCES [Shifts] ([ShiftID]);
ALTER TABLE [Attendances] ADD CONSTRAINT [FK__Attendanc__Shift__3E1D39E1] FOREIGN KEY ([ShiftID]) REFERENCES [Shifts] ([ShiftID]);
ALTER TABLE [Attendances] ADD CONSTRAINT [FK__Attendanc__Shift__5C6CB6D7] FOREIGN KEY ([ShiftID]) REFERENCES [Shifts] ([ShiftID]);
ALTER TABLE [Attendances] ADD CONSTRAINT [FK__Attendanc__Shift__7755B73D] FOREIGN KEY ([ShiftID]) REFERENCES [Shifts] ([ShiftID]);
ALTER TABLE [Branches] ADD CONSTRAINT [FK_Branch_Manager] FOREIGN KEY ([ManagerID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Customers] ADD CONSTRAINT [FK__Customers__Custo__08B54D69] FOREIGN KEY ([CustomerID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Customers] ADD CONSTRAINT [FK__Customers__Custo__318258D2] FOREIGN KEY ([CustomerID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Customers] ADD CONSTRAINT [FK__Customers__Custo__3F115E1A] FOREIGN KEY ([CustomerID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Customers] ADD CONSTRAINT [FK__Customers__Custo__5D60DB10] FOREIGN KEY ([CustomerID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Customers] ADD CONSTRAINT [FK__Customers__Custo__7849DB76] FOREIGN KEY ([CustomerID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Employees] ADD CONSTRAINT [FK__Employees__Emplo__0A9D95DB] FOREIGN KEY ([EmployeeID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Employees] ADD CONSTRAINT [FK__Employees__Emplo__336AA144] FOREIGN KEY ([EmployeeID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Employees] ADD CONSTRAINT [FK__Employees__Emplo__40F9A68C] FOREIGN KEY ([EmployeeID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Employees] ADD CONSTRAINT [FK__Employees__Emplo__5F492382] FOREIGN KEY ([EmployeeID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Employees] ADD CONSTRAINT [FK__Employees__Emplo__7A3223E8] FOREIGN KEY ([EmployeeID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Employees] ADD CONSTRAINT [FK__Employees__Branc__09A971A2] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Employees] ADD CONSTRAINT [FK__Employees__Branc__32767D0B] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Employees] ADD CONSTRAINT [FK__Employees__Branc__40058253] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Employees] ADD CONSTRAINT [FK__Employees__Branc__5E54FF49] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Employees] ADD CONSTRAINT [FK__Employees__Branc__793DFFAF] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Feedbacks] ADD CONSTRAINT [FK__Feedbacks__Custo__0C85DE4D] FOREIGN KEY ([CustomerID]) REFERENCES [Customers] ([CustomerID]);
ALTER TABLE [Feedbacks] ADD CONSTRAINT [FK__Feedbacks__Custo__3552E9B6] FOREIGN KEY ([CustomerID]) REFERENCES [Customers] ([CustomerID]);
ALTER TABLE [Feedbacks] ADD CONSTRAINT [FK__Feedbacks__Custo__42E1EEFE] FOREIGN KEY ([CustomerID]) REFERENCES [Customers] ([CustomerID]);
ALTER TABLE [Feedbacks] ADD CONSTRAINT [FK__Feedbacks__Custo__61316BF4] FOREIGN KEY ([CustomerID]) REFERENCES [Customers] ([CustomerID]);
ALTER TABLE [Feedbacks] ADD CONSTRAINT [FK__Feedbacks__Custo__7C1A6C5A] FOREIGN KEY ([CustomerID]) REFERENCES [Customers] ([CustomerID]);
ALTER TABLE [Feedbacks] ADD CONSTRAINT [FK__Feedbacks__Branc__0B91BA14] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Feedbacks] ADD CONSTRAINT [FK__Feedbacks__Branc__345EC57D] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Feedbacks] ADD CONSTRAINT [FK__Feedbacks__Branc__41EDCAC5] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Feedbacks] ADD CONSTRAINT [FK__Feedbacks__Branc__603D47BB] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Feedbacks] ADD CONSTRAINT [FK__Feedbacks__Branc__7B264821] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Inventory] ADD CONSTRAINT [FK__Inventory__Ingre__762C88DA] FOREIGN KEY ([IngredientID]) REFERENCES [Ingredients] ([IngredientID]);
ALTER TABLE [Notifications] ADD CONSTRAINT [FK__Notificat__UserI__0D7A0286] FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Notifications] ADD CONSTRAINT [FK__Notificat__UserI__36470DEF] FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Notifications] ADD CONSTRAINT [FK__Notificat__UserI__43D61337] FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Notifications] ADD CONSTRAINT [FK__Notificat__UserI__6225902D] FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Notifications] ADD CONSTRAINT [FK__Notificat__UserI__7D0E9093] FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [OrderDetails] ADD CONSTRAINT [FK__OrderDeta__Order__0E6E26BF] FOREIGN KEY ([OrderID]) REFERENCES [Orders] ([OrderID]);
ALTER TABLE [OrderDetails] ADD CONSTRAINT [FK__OrderDeta__Order__373B3228] FOREIGN KEY ([OrderID]) REFERENCES [Orders] ([OrderID]);
ALTER TABLE [OrderDetails] ADD CONSTRAINT [FK__OrderDeta__Order__44CA3770] FOREIGN KEY ([OrderID]) REFERENCES [Orders] ([OrderID]);
ALTER TABLE [OrderDetails] ADD CONSTRAINT [FK__OrderDeta__Order__6319B466] FOREIGN KEY ([OrderID]) REFERENCES [Orders] ([OrderID]);
ALTER TABLE [OrderDetails] ADD CONSTRAINT [FK__OrderDeta__Order__7E02B4CC] FOREIGN KEY ([OrderID]) REFERENCES [Orders] ([OrderID]);
ALTER TABLE [OrderDetails] ADD CONSTRAINT [FK__OrderDeta__Produ__0F624AF8] FOREIGN KEY ([ProductID]) REFERENCES [Products] ([ProductID]);
ALTER TABLE [OrderDetails] ADD CONSTRAINT [FK__OrderDeta__Produ__382F5661] FOREIGN KEY ([ProductID]) REFERENCES [Products] ([ProductID]);
ALTER TABLE [OrderDetails] ADD CONSTRAINT [FK__OrderDeta__Produ__45BE5BA9] FOREIGN KEY ([ProductID]) REFERENCES [Products] ([ProductID]);
ALTER TABLE [OrderDetails] ADD CONSTRAINT [FK__OrderDeta__Produ__640DD89F] FOREIGN KEY ([ProductID]) REFERENCES [Products] ([ProductID]);
ALTER TABLE [OrderDetails] ADD CONSTRAINT [FK__OrderDeta__Produ__7EF6D905] FOREIGN KEY ([ProductID]) REFERENCES [Products] ([ProductID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__BranchID__10566F31] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__BranchID__39237A9A] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__BranchID__46B27FE2] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__BranchID__6501FCD8] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__BranchID__7FEAFD3E] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__TableID__01D345B0] FOREIGN KEY ([TableID]) REFERENCES [Tables] ([TableID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__TableID__123EB7A3] FOREIGN KEY ([TableID]) REFERENCES [Tables] ([TableID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__TableID__3B0BC30C] FOREIGN KEY ([TableID]) REFERENCES [Tables] ([TableID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__TableID__489AC854] FOREIGN KEY ([TableID]) REFERENCES [Tables] ([TableID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__TableID__66EA454A] FOREIGN KEY ([TableID]) REFERENCES [Tables] ([TableID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__CashierI__00DF2177] FOREIGN KEY ([CashierID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__CashierI__114A936A] FOREIGN KEY ([CashierID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__CashierI__3A179ED3] FOREIGN KEY ([CashierID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__CashierI__47A6A41B] FOREIGN KEY ([CashierID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Orders] ADD CONSTRAINT [FK__Orders__CashierI__65F62111] FOREIGN KEY ([CashierID]) REFERENCES [Users] ([UserID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__OrderI__03BB8E22] FOREIGN KEY ([OrderID]) REFERENCES [Orders] ([OrderID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__OrderI__14270015] FOREIGN KEY ([OrderID]) REFERENCES [Orders] ([OrderID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__OrderI__3CF40B7E] FOREIGN KEY ([OrderID]) REFERENCES [Orders] ([OrderID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__OrderI__4A8310C6] FOREIGN KEY ([OrderID]) REFERENCES [Orders] ([OrderID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__OrderI__68D28DBC] FOREIGN KEY ([OrderID]) REFERENCES [Orders] ([OrderID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__Custom__02C769E9] FOREIGN KEY ([CustomerID]) REFERENCES [Customers] ([CustomerID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__Custom__1332DBDC] FOREIGN KEY ([CustomerID]) REFERENCES [Customers] ([CustomerID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__Custom__3BFFE745] FOREIGN KEY ([CustomerID]) REFERENCES [Customers] ([CustomerID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__Custom__498EEC8D] FOREIGN KEY ([CustomerID]) REFERENCES [Customers] ([CustomerID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__Custom__67DE6983] FOREIGN KEY ([CustomerID]) REFERENCES [Customers] ([CustomerID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__Vouche__04AFB25B] FOREIGN KEY ([VoucherID]) REFERENCES [Vouchers] ([VoucherID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__Vouche__151B244E] FOREIGN KEY ([VoucherID]) REFERENCES [Vouchers] ([VoucherID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__Vouche__3DE82FB7] FOREIGN KEY ([VoucherID]) REFERENCES [Vouchers] ([VoucherID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__Vouche__4B7734FF] FOREIGN KEY ([VoucherID]) REFERENCES [Vouchers] ([VoucherID]);
ALTER TABLE [Payments] ADD CONSTRAINT [FK__Payments__Vouche__69C6B1F5] FOREIGN KEY ([VoucherID]) REFERENCES [Vouchers] ([VoucherID]);
ALTER TABLE [ProductBranches] ADD CONSTRAINT [FK__ProductBr__Produ__0697FACD] FOREIGN KEY ([ProductID]) REFERENCES [Products] ([ProductID]);
ALTER TABLE [ProductBranches] ADD CONSTRAINT [FK__ProductBr__Produ__17036CC0] FOREIGN KEY ([ProductID]) REFERENCES [Products] ([ProductID]);
ALTER TABLE [ProductBranches] ADD CONSTRAINT [FK__ProductBr__Produ__3FD07829] FOREIGN KEY ([ProductID]) REFERENCES [Products] ([ProductID]);
ALTER TABLE [ProductBranches] ADD CONSTRAINT [FK__ProductBr__Produ__4D5F7D71] FOREIGN KEY ([ProductID]) REFERENCES [Products] ([ProductID]);
ALTER TABLE [ProductBranches] ADD CONSTRAINT [FK__ProductBr__Produ__6BAEFA67] FOREIGN KEY ([ProductID]) REFERENCES [Products] ([ProductID]);
ALTER TABLE [ProductBranches] ADD CONSTRAINT [FK__ProductBr__Branc__05A3D694] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [ProductBranches] ADD CONSTRAINT [FK__ProductBr__Branc__160F4887] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [ProductBranches] ADD CONSTRAINT [FK__ProductBr__Branc__3EDC53F0] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [ProductBranches] ADD CONSTRAINT [FK__ProductBr__Branc__4C6B5938] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [ProductBranches] ADD CONSTRAINT [FK__ProductBr__Branc__6ABAD62E] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Products] ADD CONSTRAINT [FK__Products__Catego__078C1F06] FOREIGN KEY ([CategoryID]) REFERENCES [Categories] ([CategoryID]);
ALTER TABLE [Products] ADD CONSTRAINT [FK__Products__Catego__17F790F9] FOREIGN KEY ([CategoryID]) REFERENCES [Categories] ([CategoryID]);
ALTER TABLE [Products] ADD CONSTRAINT [FK__Products__Catego__40C49C62] FOREIGN KEY ([CategoryID]) REFERENCES [Categories] ([CategoryID]);
ALTER TABLE [Products] ADD CONSTRAINT [FK__Products__Catego__4E53A1AA] FOREIGN KEY ([CategoryID]) REFERENCES [Categories] ([CategoryID]);
ALTER TABLE [Products] ADD CONSTRAINT [FK__Products__Catego__6CA31EA0] FOREIGN KEY ([CategoryID]) REFERENCES [Categories] ([CategoryID]);
ALTER TABLE [Tables] ADD CONSTRAINT [FK__Tables__BranchID__0880433F] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Tables] ADD CONSTRAINT [FK__Tables__BranchID__18EBB532] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Tables] ADD CONSTRAINT [FK__Tables__BranchID__41B8C09B] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Tables] ADD CONSTRAINT [FK__Tables__BranchID__4F47C5E3] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Tables] ADD CONSTRAINT [FK__Tables__BranchID__6D9742D9] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]);
ALTER TABLE [Users] ADD CONSTRAINT [FK__Users__RoleID__09746778] FOREIGN KEY ([RoleID]) REFERENCES [Roles] ([RoleID]);
ALTER TABLE [Users] ADD CONSTRAINT [FK__Users__RoleID__19DFD96B] FOREIGN KEY ([RoleID]) REFERENCES [Roles] ([RoleID]);
ALTER TABLE [Users] ADD CONSTRAINT [FK__Users__RoleID__42ACE4D4] FOREIGN KEY ([RoleID]) REFERENCES [Roles] ([RoleID]);
ALTER TABLE [Users] ADD CONSTRAINT [FK__Users__RoleID__503BEA1C] FOREIGN KEY ([RoleID]) REFERENCES [Roles] ([RoleID]);
ALTER TABLE [Users] ADD CONSTRAINT [FK__Users__RoleID__6E8B6712] FOREIGN KEY ([RoleID]) REFERENCES [Roles] ([RoleID]);
GO

-- ============================================================
--  Sample Data
-- ============================================================

-- Data for Table: Attendance
SET IDENTITY_INSERT [Attendance] ON;
INSERT INTO [Attendance] ([AttendanceID], [EmployeeID], [ShiftID], [Date], [CheckInTime], [CheckOutTime], [Status]) VALUES (1, 1, 1, '6/11/2026 12:00:00 AM', '6/11/2026 8:41:48 AM', NULL, N'Äang lÃ m viá»‡c');
SET IDENTITY_INSERT [Attendance] OFF;
GO

-- Data for Table: Branches
SET IDENTITY_INSERT [Branches] ON;
INSERT INTO [Branches] ([BranchID], [BranchName], [Address], [Phone], [IsActive], [ManagerID]) VALUES (1, N'Cáº§u Giáº¥y', N'HÃ  Ná»™i', '024111222', NULL, NULL);
INSERT INTO [Branches] ([BranchID], [BranchName], [Address], [Phone], [IsActive], [ManagerID]) VALUES (2, N'My Coffee House - Quận 1', N'100 Lê Lợi, Quận 1, TP.HCM', '028333444', NULL, NULL);
SET IDENTITY_INSERT [Branches] OFF;
GO

-- Data for Table: Employees
INSERT INTO [Employees] ([EmployeeID], [BranchID], [SalaryRate], [HireDate]) VALUES (1, 1, 30000.00, '6/11/2026 12:00:00 AM');
GO

-- Data for Table: Ingredients
SET IDENTITY_INSERT [Ingredients] ON;
INSERT INTO [Ingredients] ([IngredientID], [IngredientName], [Unit], [Description]) VALUES (1, N'Hat Ca Phe Robusta', N'kg', N'Ca phe nguyen chat');
INSERT INTO [Ingredients] ([IngredientID], [IngredientName], [Unit], [Description]) VALUES (2, N'Sua Dac Ngoi Sao Phuong Nam', N'lon', N'Sua dac pha che');
INSERT INTO [Ingredients] ([IngredientID], [IngredientName], [Unit], [Description]) VALUES (3, N'Duong Cat Trang', N'kg', N'Duong tinh luyen');
INSERT INTO [Ingredients] ([IngredientID], [IngredientName], [Unit], [Description]) VALUES (4, N'Bot Matcha Uji', N'kg', N'Bot tra xanh nhap khau');
SET IDENTITY_INSERT [Ingredients] OFF;
GO

-- Data for Table: Inventory
INSERT INTO [Inventory] ([BranchID], [IngredientID], [Quantity], [MinRequired], [LastUpdated]) VALUES (1, 1, 15.50, 5.00, '6/11/2026 8:41:48 AM');
INSERT INTO [Inventory] ([BranchID], [IngredientID], [Quantity], [MinRequired], [LastUpdated]) VALUES (1, 2, 2.00, 10.00, '6/11/2026 8:41:48 AM');
INSERT INTO [Inventory] ([BranchID], [IngredientID], [Quantity], [MinRequired], [LastUpdated]) VALUES (1, 3, 12.00, 2.00, '6/11/2026 8:41:48 AM');
INSERT INTO [Inventory] ([BranchID], [IngredientID], [Quantity], [MinRequired], [LastUpdated]) VALUES (1, 4, 0.50, 1.00, '6/11/2026 8:41:48 AM');
GO

-- Data for Table: Roles
SET IDENTITY_INSERT [Roles] ON;
INSERT INTO [Roles] ([RoleID], [RoleName], [Description]) VALUES (1, N'System Admin', N'Quan tri vien toan chuoi');
INSERT INTO [Roles] ([RoleID], [RoleName], [Description]) VALUES (2, N'Branch Manager', N'Quan ly mot chi nhanh');
INSERT INTO [Roles] ([RoleID], [RoleName], [Description]) VALUES (3, N'Cashier', N'Thu ngan va ban hang');
INSERT INTO [Roles] ([RoleID], [RoleName], [Description]) VALUES (4, N'Barista', N'Pha che do uong');
INSERT INTO [Roles] ([RoleID], [RoleName], [Description]) VALUES (5, N'Customer', N'Khach hang hoi vien');
SET IDENTITY_INSERT [Roles] OFF;
GO

-- Data for Table: Shifts
SET IDENTITY_INSERT [Shifts] ON;
INSERT INTO [Shifts] ([ShiftID], [ShiftName], [StartTime], [EndTime]) VALUES (1, N'Ca Sang', '07:00:00', '12:00:00');
INSERT INTO [Shifts] ([ShiftID], [ShiftName], [StartTime], [EndTime]) VALUES (2, N'Ca Chieu', '12:00:00', '17:00:00');
INSERT INTO [Shifts] ([ShiftID], [ShiftName], [StartTime], [EndTime]) VALUES (3, N'Ca Toi', '17:00:00', '22:00:00');
SET IDENTITY_INSERT [Shifts] OFF;
GO

-- Data for Table: Users
SET IDENTITY_INSERT [Users] ON;
INSERT INTO [Users] ([UserID], [Username], [Password], [FullName], [Email], [Phone], [RoleID], [IsActive], [CreatedAt]) VALUES (1, 'admin', 'admin123', N'Admin', 'admin@mycoffee.com', '0987654321', 1, NULL, '6/8/2026 2:47:51 PM');
INSERT INTO [Users] ([UserID], [Username], [Password], [FullName], [Email], [Phone], [RoleID], [IsActive], [CreatedAt]) VALUES (3, 'custest99', '123', N'Customer Test', 'custest99@gmail.com', '0987654399', 2, NULL, '6/11/2026 11:55:05 AM');
INSERT INTO [Users] ([UserID], [Username], [Password], [FullName], [Email], [Phone], [RoleID], [IsActive], [CreatedAt]) VALUES (4, 'custest', '123', N'Customer Test', 'custest_new@gmail.com', '0987654999', 2, NULL, '6/11/2026 11:57:14 AM');
INSERT INTO [Users] ([UserID], [Username], [Password], [FullName], [Email], [Phone], [RoleID], [IsActive], [CreatedAt]) VALUES (5, 'demo@test.com', '123456', N'Hoàng Linh', 'demo@test.com', '0389005769', 2, NULL, '6/11/2026 5:06:43 PM');
INSERT INTO [Users] ([UserID], [Username], [Password], [FullName], [Email], [Phone], [RoleID], [IsActive], [CreatedAt]) VALUES (6, 'barista', '123', N'Nhân viên Pha chế', 'barista@mycoffee.com', '0900000004', 4, 1, '6/22/2026 12:00:00 AM');
SET IDENTITY_INSERT [Users] OFF;
GO

