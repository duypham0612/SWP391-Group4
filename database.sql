-- ============================================================
--  Script tao Database cho du an: My Coffee House (SWP391)
--  Phù hợp chuỗi chi nhánh, bán tại quầy/tại bàn qua QR Code
-- ============================================================

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'MyCoffeeHouse')
BEGIN
    CREATE DATABASE MyCoffeeHouse;
END
GO

USE MyCoffeeHouse;
GO

-- 1. Table: Roles
IF OBJECT_ID('Roles', 'U') IS NOT NULL DROP TABLE [Roles];
GO
CREATE TABLE [Roles] (
    [RoleID] int IDENTITY(1,1) NOT NULL,
    [RoleName] nvarchar(50) NOT NULL, -- Admin, Branch Manager, Employee, Customer
    [Description] nvarchar(255) NULL,
    PRIMARY KEY ([RoleID]),
    CONSTRAINT [UQ_Roles_RoleName] UNIQUE ([RoleName])
);
GO

-- 2. Table: Users (Dành cho Admin, Manager, Nhân viên và Khách có tài khoản)
IF OBJECT_ID('Users', 'U') IS NOT NULL DROP TABLE [Users];
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
    CONSTRAINT [UQ_Users_Username] UNIQUE ([Username]),
    CONSTRAINT [UQ_Users_Email] UNIQUE ([Email]),
    CONSTRAINT [UQ_Users_Phone] UNIQUE ([Phone]),
    CONSTRAINT [FK_Users_Roles] FOREIGN KEY ([RoleID]) REFERENCES [Roles] ([RoleID])
);
GO

-- 3. Table: Branches (Quản lý nhiều chi nhánh)
IF OBJECT_ID('Branches', 'U') IS NOT NULL DROP TABLE [Branches];
GO
CREATE TABLE [Branches] (
    [BranchID] int IDENTITY(1,1) NOT NULL,
    [BranchName] nvarchar(100) NOT NULL,
    [Address] nvarchar(255) NOT NULL,
    [Phone] varchar(15) NULL,
    [IsActive] bit NULL DEFAULT ((1)),
    [ManagerID] int NULL, -- Liên kết tới Users có Role là Branch Manager
    PRIMARY KEY ([BranchID]),
    CONSTRAINT [FK_Branch_Manager] FOREIGN KEY ([ManagerID]) REFERENCES [Users] ([UserID])
);
GO

-- 4. Table: Customers (Chỉ lưu thông tin thành viên đăng ký tích điểm)
IF OBJECT_ID('Customers', 'U') IS NOT NULL DROP TABLE [Customers];
GO
CREATE TABLE [Customers] (
    [CustomerID] int NOT NULL, -- Kế thừa thừa UserID từ bảng Users
    [MemberRank] nvarchar(50) NULL DEFAULT ('Member'),
    [CurrentPoints] int NULL DEFAULT ((0)),
    PRIMARY KEY ([CustomerID]),
    CONSTRAINT [FK_Customers_Users] FOREIGN KEY ([CustomerID]) REFERENCES [Users] ([UserID])
);
GO

-- 5. Table: Employees (Nhân viên làm việc theo chi nhánh)
IF OBJECT_ID('Employees', 'U') IS NOT NULL DROP TABLE [Employees];
GO
CREATE TABLE [Employees] (
    [EmployeeID] int NOT NULL, -- Kế thừa UserID từ bảng Users
    [BranchID] int NULL,
    [SalaryRate] decimal(18,2) NULL DEFAULT ((0)),
    [HireDate] date NULL DEFAULT (getdate()),
    PRIMARY KEY ([EmployeeID]),
    CONSTRAINT [FK_Employees_Users] FOREIGN KEY ([EmployeeID]) REFERENCES [Users] ([UserID]),
    CONSTRAINT [FK_Employees_Branches] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID])
);
GO

-- 6. Table: Shifts (Ca làm việc của nhân viên)
IF OBJECT_ID('Shifts', 'U') IS NOT NULL DROP TABLE [Shifts];
GO
CREATE TABLE [Shifts] (
    [ShiftID] int IDENTITY(1,1) NOT NULL,
    [ShiftName] nvarchar(50) NOT NULL,
    [StartTime] time NOT NULL,
    [EndTime] time NOT NULL,
    PRIMARY KEY ([ShiftID])
);
GO

-- 7. Table: Attendance (Điểm danh nhân viên)
IF OBJECT_ID('Attendance', 'U') IS NOT NULL DROP TABLE [Attendance];
GO
CREATE TABLE [Attendance] (
    [AttendanceID] int IDENTITY(1,1) NOT NULL,
    [EmployeeID] int NOT NULL,
    [ShiftID] int NOT NULL,
    [Date] date NOT NULL,
    [CheckInTime] datetime NULL,
    [CheckOutTime] datetime NULL,
    [Status] nvarchar(50) NULL, -- Đúng giờ, Đi muộn, Vắng mặt...
    PRIMARY KEY ([AttendanceID]),
    CONSTRAINT [FK_Attendance_Employees] FOREIGN KEY ([EmployeeID]) REFERENCES [Employees] ([EmployeeID]),
    CONSTRAINT [FK_Attendance_Shifts] FOREIGN KEY ([ShiftID]) REFERENCES [Shifts] ([ShiftID])
);
GO

-- 8. Table: Categories (Danh mục đồ uống/bánh)
IF OBJECT_ID('Categories', 'U') IS NOT NULL DROP TABLE [Categories];
GO
CREATE TABLE [Categories] (
    [CategoryID] int IDENTITY(1,1) NOT NULL,
    [CategoryName] nvarchar(100) NOT NULL,
    [Description] nvarchar(255) NULL,
    PRIMARY KEY ([CategoryID]),
    CONSTRAINT [UQ_Categories_CategoryName] UNIQUE ([CategoryName])
);
GO

-- 9. Table: Products (Danh sách món ăn/nước uống tổng)
IF OBJECT_ID('Products', 'U') IS NOT NULL DROP TABLE [Products];
GO
CREATE TABLE [Products] (
    [ProductID] int IDENTITY(1,1) NOT NULL,
    [ProductName] nvarchar(100) NOT NULL,
    [CategoryID] int NULL,
    [BasePrice] decimal(18,2) NOT NULL,
    [ImageURL] nvarchar(500) NULL,
    [Description] nvarchar(max) NULL,
    [IsAvailable] bit NULL DEFAULT ((1)),
    PRIMARY KEY ([ProductID]),
    CONSTRAINT [FK_Products_Categories] FOREIGN KEY ([CategoryID]) REFERENCES [Categories] ([CategoryID])
);
GO

-- 10. Table: ProductBranches (Giá và trạng thái món ăn theo từng chi nhánh)
IF OBJECT_ID('ProductBranches', 'U') IS NOT NULL DROP TABLE [ProductBranches];
GO
CREATE TABLE [ProductBranches] (
    [ProductID] int NOT NULL,
    [BranchID] int NOT NULL,
    [CustomPrice] decimal(18,2) NULL, -- Giá có thể khác nhau giữa các chi nhánh (Q1 đắt hơn Cầu Giấy chẳng hạn)
    [IsAvailable] bit NULL DEFAULT ((1)),
    PRIMARY KEY ([ProductID], [BranchID]),
    CONSTRAINT [FK_ProductBranches_Products] FOREIGN KEY ([ProductID]) REFERENCES [Products] ([ProductID]),
    CONSTRAINT [FK_ProductBranches_Branches] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID])
);
GO

-- 11. Table: Tables (Quản lý bàn và mã QR theo từng chi nhánh)
IF OBJECT_ID('Tables', 'U') IS NOT NULL DROP TABLE [Tables];
GO
CREATE TABLE [Tables] (
    [TableID] int IDENTITY(1,1) NOT NULL,
    [BranchID] int NULL,
    [TableName] nvarchar(50) NOT NULL,
    [QRCodeURL] nvarchar(500) NULL, -- URL chứa thông tin bàn để app/web nhận diện khi quét QR
    [Status] nvarchar(50) NULL DEFAULT ('Empty'), -- Trống, Đang ngồi, Chờ dọn bàn
    PRIMARY KEY ([TableID]),
    CONSTRAINT [FK_Tables_Branches] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID])
);
GO

-- 12. Table: Orders (Hóa đơn đặt đồ)
IF OBJECT_ID('Orders', 'U') IS NOT NULL DROP TABLE [Orders];
GO
CREATE TABLE [Orders] (
    [OrderID] int IDENTITY(1,1) NOT NULL,
    [BranchID] int NOT NULL,
    [TableID] int NOT NULL,            -- Bắt buộc có vì khách ngồi tại bàn quét QR code
    [CustomerID] int NULL,            -- NULL: Khách vãng lai quét QR | NOT NULL: Khách đã đăng ký thành viên
    [CashierID] int NULL,             -- Nhân viên xác nhận/thu ngân (nếu tự phục vụ hoàn toàn có thể NULL ban đầu)
    [OrderType] nvarchar(50) NOT NULL DEFAULT ('Eat-in'), -- Eat-in (Tại bàn), Take-away (Mang về)
    [TotalAmount] decimal(18,2) NULL DEFAULT ((0)),
    [DiscountAmount] decimal(18,2) NULL DEFAULT ((0)),
    [FinalAmount] decimal(18,2) NULL DEFAULT ((0)),
    [OrderStatus] nvarchar(50) NULL DEFAULT ('Pending'), -- Pending, Preparing, Completed, Cancelled
    [OrderDate] datetime NULL DEFAULT (getdate()),
    PRIMARY KEY ([OrderID]),
    CONSTRAINT [FK_Orders_Branches] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]),
    CONSTRAINT [FK_Orders_Tables] FOREIGN KEY ([TableID]) REFERENCES [Tables] ([TableID]),
    CONSTRAINT [FK_Orders_Customers] FOREIGN KEY ([CustomerID]) REFERENCES [Customers] ([CustomerID]),
    CONSTRAINT [FK_Orders_Cashier] FOREIGN KEY ([CashierID]) REFERENCES [Users] ([UserID])
);
GO

-- 13. Table: OrderDetails (Chi tiết các món trong Order)
IF OBJECT_ID('OrderDetails', 'U') IS NOT NULL DROP TABLE [OrderDetails];
GO
CREATE TABLE [OrderDetails] (
    [OrderDetailID] int IDENTITY(1,1) NOT NULL,
    [OrderID] int NULL,
    [ProductID] int NULL,
    [Quantity] int NOT NULL,
    [UnitPrice] decimal(18,2) NOT NULL,
    [Note] nvarchar(255) NULL, -- Ít đá, nhiều đường, bỏ hành...
    [ItemStatus] nvarchar(50) NULL DEFAULT ('Pending'), -- Chờ làm, Đang làm, Đã xong
    PRIMARY KEY ([OrderDetailID]),
    CONSTRAINT [FK_OrderDetails_Orders] FOREIGN KEY ([OrderID]) REFERENCES [Orders] ([OrderID]),
    CONSTRAINT [FK_OrderDetails_Products] FOREIGN KEY ([ProductID]) REFERENCES [Products] ([ProductID])
);
GO

-- 14. Table: Vouchers
IF OBJECT_ID('Vouchers', 'U') IS NOT NULL DROP TABLE [Vouchers];
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
    CONSTRAINT [UQ_Vouchers_VoucherCode] UNIQUE ([VoucherCode])
);
GO

-- 15. Table: Payments (Thông tin thanh toán hóa đơn)
IF OBJECT_ID('Payments', 'U') IS NOT NULL DROP TABLE [Payments];
GO
CREATE TABLE [Payments] (
    [PaymentID] int IDENTITY(1,1) NOT NULL,
    [OrderID] int NULL,
    [VoucherID] int NULL,
    [PaymentMethod] nvarchar(50) NOT NULL, -- Tiền mặt, Cổng VNPay, Momo, Chuyển khoản ngân hàng
    [PaymentTime] datetime NULL DEFAULT (getdate()),
    [TransactionNo] varchar(100) NULL, -- Mã giao dịch từ ví điện tử/ngân hàng nếu có
    PRIMARY KEY ([PaymentID]),
    CONSTRAINT [FK_Payments_Orders] FOREIGN KEY ([OrderID]) REFERENCES [Orders] ([OrderID]),
    CONSTRAINT [FK_Payments_Vouchers] FOREIGN KEY ([VoucherID]) REFERENCES [Vouchers] ([VoucherID])
);
GO

-- 16. Table: Ingredients (Nguyên liệu trong kho)
IF OBJECT_ID('Ingredients', 'U') IS NOT NULL DROP TABLE [Ingredients];
GO
CREATE TABLE [Ingredients] (
    [IngredientID] int IDENTITY(1,1) NOT NULL,
    [IngredientName] nvarchar(100) NOT NULL,
    [Unit] nvarchar(50) NOT NULL, -- kg, lít, lon, thùng...
    [Description] nvarchar(255) NULL,
    PRIMARY KEY ([IngredientID])
);
GO

-- 17. Table: Inventory (Quản lý tồn kho nguyên liệu riêng biệt từng chi nhánh)
IF OBJECT_ID('Inventory', 'U') IS NOT NULL DROP TABLE [Inventory];
GO
CREATE TABLE [Inventory] (
    [BranchID] int NOT NULL,
    [IngredientID] int NOT NULL,
    [Quantity] decimal(10,2) NOT NULL,
    [MinRequired] decimal(10,2) NOT NULL, -- Mức tối thiểu để cảnh báo nhập thêm kho
    [LastUpdated] datetime NULL DEFAULT (getdate()),
    PRIMARY KEY ([BranchID], [IngredientID]),
    CONSTRAINT [FK_Inventory_Branches] FOREIGN KEY ([BranchID]) REFERENCES [Branches] ([BranchID]),
    CONSTRAINT [FK_Inventory_Ingredients] FOREIGN KEY ([IngredientID]) REFERENCES [Ingredients] ([IngredientID])
);
GO

-- 18. Table: Feedbacks (Đánh giá của khách hàng sau khi ăn uống)
IF OBJECT_ID('Feedbacks', 'U') IS NOT NULL DROP TABLE [Feedbacks];
GO
CREATE TABLE [Feedbacks] (
    [FeedbackID] int IDENTITY(1,1) NOT NULL,
    [OrderID] int NULL,               -- Đánh giá dựa trên chính hóa đơn đã ăn
    [Rating] int NULL CHECK (Rating BETWEEN 1 AND 5),
    [Comment] nvarchar(max) NULL,
    [CreatedAt] datetime NULL DEFAULT (getdate()),
    PRIMARY KEY ([FeedbackID]),
    CONSTRAINT [FK_Feedbacks_Orders] FOREIGN KEY ([OrderID]) REFERENCES [Orders] ([OrderID])
);
GO


-- ============================================================
--  Sample Data cơ bản để test cấu trúc hình học
-- ============================================================

INSERT INTO [Roles] ([RoleName], [Description]) VALUES 
(N'Admin', N'Quản trị viên hệ thống chuỗi'),
(N'Branch Manager', N'Quản lý chi nhánh'),
(N'Employee', N'Nhân viên cửa hàng'),
(N'Customer', N'Khách hàng thành viên');

INSERT INTO [Users] ([Username], [Password], [FullName], [Email], [Phone], [RoleID], [IsActive]) VALUES 
('admin1', 'hashed_pwd_123', N'Nguyễn Văn Admin', 'admin@cafe.com', '0911111111', 1, 1),
('manager1', 'hashed_pwd_123', N'Trần Chi Nhánh', 'manager1@cafe.com', '0922222222', 2, 1),
('staff1', 'hashed_pwd_123', N'Lê Pha Chế', 'staff1@cafe.com', '0933333333', 3, 1),
('khachhang1', 'hashed_pwd_123', N'Phan Khách Quen', 'khach1@gmail.com', '0944444444', 4, 1);

INSERT INTO [Branches] ([BranchName], [Address], [Phone], [ManagerID]) VALUES 
(N'My Coffee House - Cầu Giấy', N'Số 1 Dịch Vọng Hậu, Cầu Giấy, Hà Nội', '024777888', 2);

-- Đăng ký khách hàng thành viên dựa trên UserID = 4
INSERT INTO [Customers] ([CustomerID], [MemberRank], [CurrentPoints]) VALUES (4, N'Vàng', 150);

-- Đăng ký nhân viên dựa trên UserID = 3 làm việc tại BranchID = 1
INSERT INTO [Employees] ([EmployeeID], [BranchID], [SalaryRate]) VALUES (3, 1, 25000.00);

-- Tạo bàn test cho chi nhánh Cầu Giấy
INSERT INTO [Tables] ([BranchID], [TableName], [QRCodeURL], [Status]) VALUES 
(1, N'Bàn 01', 'https://mycoffeehouse.com/qr?branch=1&table=1', 'Empty'),
(1, N'Bàn 02', 'https://mycoffeehouse.com/qr?branch=1&table=2', 'Empty');
GO