-- ============================================================
--  Script tạo Database cho dự án: My Coffee House (SWP391)
--  Hướng dẫn: Mở SQL Server Management Studio (SSMS),
--  kết nối vào server của bạn, chạy toàn bộ script này.
-- ============================================================

-- 1. Tạo Database
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'MyCoffeeHouse')
BEGIN
    CREATE DATABASE MyCoffeeHouse;
    PRINT 'Da tao database MyCoffeeHouse.';
END
GO

USE MyCoffeeHouse;
GO

-- ============================================================
-- 2. Tạo các bảng
-- ============================================================

-- Bảng Roles (Vai trò người dùng)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Roles')
BEGIN
    CREATE TABLE Roles (
        RoleID   INT IDENTITY(1,1) PRIMARY KEY,
        RoleName NVARCHAR(50) NOT NULL
    );
    PRINT 'Da tao bang Roles.';
END
GO

-- Bảng Users (Tài khoản người dùng)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Users')
BEGIN
    CREATE TABLE Users (
        UserID    INT IDENTITY(1,1) PRIMARY KEY,
        Username  NVARCHAR(100) NOT NULL UNIQUE,
        Password  NVARCHAR(255) NOT NULL,
        FullName  NVARCHAR(150) NOT NULL,
        Email     NVARCHAR(150) UNIQUE,
        Phone     NVARCHAR(20),
        RoleID    INT NOT NULL DEFAULT 2,
        IsActive  BIT NOT NULL DEFAULT 1,
        CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT FK_Users_Roles FOREIGN KEY (RoleID) REFERENCES Roles(RoleID)
    );
    PRINT 'Da tao bang Users.';
END
GO

-- Bảng Branches (Chi nhánh)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Branches')
BEGIN
    CREATE TABLE Branches (
        BranchID   INT IDENTITY(1,1) PRIMARY KEY,
        BranchName NVARCHAR(150) NOT NULL,
        Address    NVARCHAR(255)
    );
    PRINT 'Da tao bang Branches.';
END
GO

-- Bảng Employees (Nhân viên)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Employees')
BEGIN
    CREATE TABLE Employees (
        EmployeeID INT IDENTITY(1,1) PRIMARY KEY,
        UserID     INT NOT NULL,
        BranchID   INT NOT NULL,
        SalaryRate DECIMAL(18,2) DEFAULT 0,
        HireDate   DATE DEFAULT GETDATE(),
        CONSTRAINT FK_Employees_Users    FOREIGN KEY (UserID)   REFERENCES Users(UserID),
        CONSTRAINT FK_Employees_Branches FOREIGN KEY (BranchID) REFERENCES Branches(BranchID)
    );
    PRINT 'Da tao bang Employees.';
END
GO

-- Bảng Shifts (Ca làm việc)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Shifts')
BEGIN
    CREATE TABLE Shifts (
        ShiftID    INT IDENTITY(1,1) PRIMARY KEY,
        ShiftName  NVARCHAR(100) NOT NULL,
        StartTime  TIME,
        EndTime    TIME
    );
    PRINT 'Da tao bang Shifts.';
END
GO

-- Bảng Attendance (Chấm công)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Attendance')
BEGIN
    CREATE TABLE Attendance (
        AttendanceID INT IDENTITY(1,1) PRIMARY KEY,
        EmployeeID   INT NOT NULL,
        ShiftID      INT NOT NULL,
        Date         DATE NOT NULL DEFAULT GETDATE(),
        CheckInTime  TIME,
        CONSTRAINT FK_Attendance_Employees FOREIGN KEY (EmployeeID) REFERENCES Employees(EmployeeID),
        CONSTRAINT FK_Attendance_Shifts    FOREIGN KEY (ShiftID)    REFERENCES Shifts(ShiftID)
    );
    PRINT 'Da tao bang Attendance.';
END
GO

-- Bảng Ingredients (Nguyên liệu)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Ingredients')
BEGIN
    CREATE TABLE Ingredients (
        IngredientID   INT IDENTITY(1,1) PRIMARY KEY,
        IngredientName NVARCHAR(100) NOT NULL,
        Unit           NVARCHAR(50)  NOT NULL,
        Description    NVARCHAR(255)
    );
    PRINT 'Da tao bang Ingredients.';
END
GO

-- Bảng Inventory (Kho hàng)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Inventory')
BEGIN
    CREATE TABLE Inventory (
        InventoryID    INT IDENTITY(1,1) PRIMARY KEY,
        BranchID       INT NOT NULL,
        IngredientID   INT NOT NULL,
        Quantity       DECIMAL(18,2) NOT NULL DEFAULT 0,
        MinQuantity    DECIMAL(18,2) NOT NULL DEFAULT 10,
        LastUpdated    DATETIME DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT FK_Inventory_Branches    FOREIGN KEY (BranchID)     REFERENCES Branches(BranchID),
        CONSTRAINT FK_Inventory_Ingredients FOREIGN KEY (IngredientID) REFERENCES Ingredients(IngredientID)
    );
    PRINT 'Da tao bang Inventory.';
END
GO

-- Bảng MenuCategories (Danh mục thực đơn)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'MenuCategories')
BEGIN
    CREATE TABLE MenuCategories (
        CategoryID   INT IDENTITY(1,1) PRIMARY KEY,
        CategoryName NVARCHAR(100) NOT NULL
    );
    PRINT 'Da tao bang MenuCategories.';
END
GO

-- Bảng MenuItems (Thực đơn)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'MenuItems')
BEGIN
    CREATE TABLE MenuItems (
        ItemID      INT IDENTITY(1,1) PRIMARY KEY,
        CategoryID  INT NOT NULL,
        ItemName    NVARCHAR(150) NOT NULL,
        Description NVARCHAR(500),
        Price       DECIMAL(18,2) NOT NULL DEFAULT 0,
        ImageURL    NVARCHAR(500),
        IsAvailable BIT NOT NULL DEFAULT 1,
        CONSTRAINT FK_MenuItems_Categories FOREIGN KEY (CategoryID) REFERENCES MenuCategories(CategoryID)
    );
    PRINT 'Da tao bang MenuItems.';
END
GO

-- ============================================================
-- 3. Chèn dữ liệu mẫu (Sample Data)
-- ============================================================

-- Roles
IF NOT EXISTS (SELECT 1 FROM Roles)
BEGIN
    INSERT INTO Roles (RoleName) VALUES (N'Admin'), (N'Customer');
    PRINT 'Da them du lieu vao bang Roles.';
END
GO

-- Users - Tài khoản mặc định
-- Admin:    username=admin,    password=admin123
-- Customer: username=customer, password=customer123
IF NOT EXISTS (SELECT 1 FROM Users)
BEGIN
    INSERT INTO Users (Username, Password, FullName, Email, Phone, RoleID, IsActive) VALUES
    (N'admin',    N'admin123',    N'Administrator',    N'admin@mycoffee.com',    N'0900000001', 1, 1),
    (N'customer', N'customer123', N'Nguyen Van Khach',  N'customer@mycoffee.com', N'0900000002', 2, 1);
    PRINT 'Da them tai khoan mac dinh: admin/admin123 va customer/customer123';
END
GO

-- Branches
IF NOT EXISTS (SELECT 1 FROM Branches)
BEGIN
    INSERT INTO Branches (BranchName, Address) VALUES
    (N'Chi nhanh Quan 1', N'123 Nguyen Hue, Q.1, TP.HCM'),
    (N'Chi nhanh Quan 7', N'456 Nguyen Thi Thap, Q.7, TP.HCM');
    PRINT 'Da them du lieu vao bang Branches.';
END
GO

-- Shifts
IF NOT EXISTS (SELECT 1 FROM Shifts)
BEGIN
    INSERT INTO Shifts (ShiftName, StartTime, EndTime) VALUES
    (N'Ca Sang',  '06:00', '14:00'),
    (N'Ca Chieu', '14:00', '22:00');
    PRINT 'Da them du lieu vao bang Shifts.';
END
GO

-- Employees
IF NOT EXISTS (SELECT 1 FROM Employees)
BEGIN
    INSERT INTO Employees (UserID, BranchID, SalaryRate, HireDate) VALUES
    (2, 1, 5000000, '2026-01-01');
    PRINT 'Da them du lieu vao bang Employees.';
END
GO

-- Attendance (mẫu - hôm nay)
IF NOT EXISTS (SELECT 1 FROM Attendance)
BEGIN
    INSERT INTO Attendance (EmployeeID, ShiftID, Date, CheckInTime) VALUES
    (1, 1, CAST(GETDATE() AS DATE), '06:05');
    PRINT 'Da them du lieu mau vao bang Attendance.';
END
GO

-- Ingredients
IF NOT EXISTS (SELECT 1 FROM Ingredients)
BEGIN
    INSERT INTO Ingredients (IngredientName, Unit, Description) VALUES
    (N'Ca phe hat',    N'kg',  N'Ca phe Arabica nguyen hat'),
    (N'Sua tuoi',      N'lit', N'Sua tuoi nguyen chat'),
    (N'Duong',         N'kg',  N'Duong trang tinh luyen'),
    (N'Tra xanh bot',  N'kg',  N'Bot tra xanh Matcha Nhat'),
    (N'Kem tuoi',      N'lit', N'Whipping cream');
    PRINT 'Da them du lieu vao bang Ingredients.';
END
GO

-- Inventory
IF NOT EXISTS (SELECT 1 FROM Inventory)
BEGIN
    INSERT INTO Inventory (BranchID, IngredientID, Quantity, MinQuantity) VALUES
    (1, 1, 15.5, 10),
    (1, 2, 8.0,  10),
    (1, 3, 20.0, 5),
    (1, 4, 3.5,  5),
    (1, 5, 12.0, 5);
    PRINT 'Da them du lieu vao bang Inventory.';
END
GO

-- MenuCategories
IF NOT EXISTS (SELECT 1 FROM MenuCategories)
BEGIN
    INSERT INTO MenuCategories (CategoryName) VALUES
    (N'Ca phe'), (N'Tra sua'), (N'Matcha'), (N'Sinh to'), (N'Nuoc ep');
    PRINT 'Da them du lieu vao bang MenuCategories.';
END
GO

-- MenuItems
IF NOT EXISTS (SELECT 1 FROM MenuItems)
BEGIN
    INSERT INTO MenuItems (CategoryID, ItemName, Description, Price, IsAvailable) VALUES
    (1, N'Ca phe sua da',       N'Ca phe truyen thong voi sua dac va da bam',  35000, 1),
    (1, N'Bac xiu',             N'Ca phe sua nhieu sua it ca phe',              35000, 1),
    (1, N'Americano',           N'Ca phe den pha Espresso voi nuoc',            45000, 1),
    (2, N'Tra sua tran chau',   N'Tra sua voi tran chau den dai loan',          45000, 1),
    (2, N'Tra sua kem cheese',  N'Tra sua voi topping kem cheese beo ngay',     55000, 1),
    (3, N'Matcha latte',        N'Tra xanh hoa tan voi sua tuoi am',            55000, 1),
    (3, N'Matcha espresso',     N'Matcha ket hop voi shot espresso dam da',     60000, 1),
    (4, N'Sinh to bo',          N'Sinh to bo tuoi beo thom nguay',              55000, 1),
    (5, N'Nuoc cam ep',         N'Cam ep nguyen chat khong duong',              40000, 1);
    PRINT 'Da them du lieu vao bang MenuItems.';
END
GO

PRINT '';
PRINT '============================================================';
PRINT 'HOAN THANH! Database MyCoffeeHouse da san sang.';
PRINT 'Tai khoan mac dinh:';
PRINT '  Admin:    username=admin    | password=admin123';
PRINT '  Customer: username=customer | password=customer123';
PRINT '============================================================';
