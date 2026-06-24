USE [master]
GO

IF DB_ID(N'MyCoffeeHouse') IS NOT NULL
BEGIN
    ALTER DATABASE [MyCoffeeHouse] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [MyCoffeeHouse];
END
GO

CREATE DATABASE [MyCoffeeHouse];
GO

USE [MyCoffeeHouse]
GO
/****** Object:  Table [dbo].[Attendance]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Attendance](
	[AttendanceID] [int] IDENTITY(1,1) NOT NULL,
	[EmployeeID] [int] NOT NULL,
	[ShiftID] [int] NOT NULL,
	[Date] [date] NOT NULL,
	[CheckInTime] [datetime] NULL,
	[CheckOutTime] [datetime] NULL,
	[Status] [nvarchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[AttendanceID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Branches]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Branches](
	[BranchID] [int] IDENTITY(1,1) NOT NULL,
	[BranchName] [nvarchar](100) NOT NULL,
	[Address] [nvarchar](255) NOT NULL,
	[Phone] [varchar](15) NULL,
	[IsActive] [bit] NULL,
	[ManagerID] [int] NULL,
PRIMARY KEY CLUSTERED 
(
	[BranchID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Categories]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Categories](
	[CategoryID] [int] IDENTITY(1,1) NOT NULL,
	[CategoryName] [nvarchar](100) NOT NULL,
	[Description] [nvarchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[CategoryID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Customers]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Customers](
	[CustomerID] [int] NOT NULL,
	[MemberRank] [nvarchar](50) NULL,
	[CurrentPoints] [int] NULL,
PRIMARY KEY CLUSTERED 
(
	[CustomerID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Employees]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Employees](
	[EmployeeID] [int] NOT NULL,
	[BranchID] [int] NULL,
	[SalaryRate] [decimal](18, 2) NULL,
	[HireDate] [date] NULL,
PRIMARY KEY CLUSTERED 
(
	[EmployeeID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Feedbacks]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Feedbacks](
	[FeedbackID] [int] IDENTITY(1,1) NOT NULL,
	[OrderID] [int] NULL,
	[Rating] [int] NULL,
	[Comment] [nvarchar](max) NULL,
	[CreatedAt] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[FeedbackID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Ingredients]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Ingredients](
	[IngredientID] [int] IDENTITY(1,1) NOT NULL,
	[IngredientName] [nvarchar](100) NOT NULL,
	[Unit] [nvarchar](50) NOT NULL,
	[Description] [nvarchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[IngredientID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Inventory]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Inventory](
	[BranchID] [int] NOT NULL,
	[IngredientID] [int] NOT NULL,
	[Quantity] [decimal](10, 2) NOT NULL,
	[MinRequired] [decimal](10, 2) NOT NULL,
	[LastUpdated] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[BranchID] ASC,
	[IngredientID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[OrderDetails]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[OrderDetails](
	[OrderDetailID] [int] IDENTITY(1,1) NOT NULL,
	[OrderID] [int] NULL,
	[ProductID] [int] NULL,
	[Quantity] [int] NOT NULL,
	[UnitPrice] [decimal](18, 2) NOT NULL,
	[Note] [nvarchar](255) NULL,
	[ItemStatus] [nvarchar](50) NULL,
	[StartedAt] [datetime] NULL,
	[CompletedAt] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[OrderDetailID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Orders]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Orders](
	[OrderID] [int] IDENTITY(1,1) NOT NULL,
	[BranchID] [int] NOT NULL,
	[TableID] [int] NOT NULL,
	[CustomerID] [int] NULL,
	[CashierID] [int] NULL,
	[OrderType] [nvarchar](50) NOT NULL,
	[TotalAmount] [decimal](18, 2) NULL,
	[DiscountAmount] [decimal](18, 2) NULL,
	[FinalAmount] [decimal](18, 2) NULL,
	[OrderStatus] [nvarchar](50) NULL,
	[OrderDate] [datetime] NULL,
	[Priority] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[OrderID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Payments]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Payments](
	[PaymentID] [int] IDENTITY(1,1) NOT NULL,
	[OrderID] [int] NULL,
	[VoucherID] [int] NULL,
	[PaymentMethod] [nvarchar](50) NOT NULL,
	[PaymentTime] [datetime] NULL,
	[TransactionNo] [varchar](100) NULL,
PRIMARY KEY CLUSTERED 
(
	[PaymentID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ProductBranches]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ProductBranches](
	[ProductID] [int] NOT NULL,
	[BranchID] [int] NOT NULL,
	[CustomPrice] [decimal](18, 2) NULL,
	[IsAvailable] [bit] NULL,
PRIMARY KEY CLUSTERED 
(
	[ProductID] ASC,
	[BranchID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Products]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Products](
	[ProductID] [int] IDENTITY(1,1) NOT NULL,
	[ProductName] [nvarchar](100) NOT NULL,
	[CategoryID] [int] NULL,
	[BasePrice] [decimal](18, 2) NOT NULL,
	[ImageURL] [nvarchar](500) NULL,
	[Description] [nvarchar](max) NULL,
	[IsAvailable] [bit] NULL,
PRIMARY KEY CLUSTERED 
(
	[ProductID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Roles]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Roles](
	[RoleID] [int] IDENTITY(1,1) NOT NULL,
	[RoleName] [nvarchar](50) NOT NULL,
	[Description] [nvarchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[RoleID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Shifts]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Shifts](
	[ShiftID] [int] IDENTITY(1,1) NOT NULL,
	[ShiftName] [nvarchar](50) NOT NULL,
	[StartTime] [time](7) NOT NULL,
	[EndTime] [time](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[ShiftID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Tables]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Tables](
	[TableID] [int] IDENTITY(1,1) NOT NULL,
	[BranchID] [int] NULL,
	[TableName] [nvarchar](50) NOT NULL,
	[QRCodeURL] [nvarchar](500) NULL,
	[Status] [nvarchar](50) NULL,
	[Capacity] [int] NULL,
PRIMARY KEY CLUSTERED 
(
	[TableID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Users]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Users](
	[UserID] [int] IDENTITY(1,1) NOT NULL,
	[Username] [varchar](50) NOT NULL,
	[Password] [varchar](255) NOT NULL,
	[FullName] [nvarchar](100) NOT NULL,
	[Email] [varchar](100) NULL,
	[Phone] [varchar](15) NULL,
	[RoleID] [int] NULL,
	[IsActive] [bit] NULL,
	[CreatedAt] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[UserID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Vouchers]    Script Date: 6/24/2026 1:19:41 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Vouchers](
	[VoucherID] [int] IDENTITY(1,1) NOT NULL,
	[VoucherCode] [varchar](50) NOT NULL,
	[DiscountValue] [decimal](18, 2) NOT NULL,
	[IsPercentage] [bit] NULL,
	[MinOrderValue] [decimal](18, 2) NULL,
	[MaxDiscount] [decimal](10, 2) NULL,
	[UsageLimit] [int] NOT NULL,
	[UsedCount] [int] NOT NULL,
	[StartDate] [datetime] NULL,
	[EndDate] [datetime] NULL,
	[IsActive] [bit] NULL,
	[ProductID] [int] NULL,
PRIMARY KEY CLUSTERED 
(
	[VoucherID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
SET IDENTITY_INSERT [dbo].[Branches] ON 

INSERT [dbo].[Branches] ([BranchID], [BranchName], [Address], [Phone], [IsActive], [ManagerID]) VALUES (1, N'My Coffee House - Cầu Giấy', N'Số 1 Dịch Vọng Hậu, Cầu Giấy, Hà Nội', N'024777888', 1, 2)
INSERT [dbo].[Branches] ([BranchID], [BranchName], [Address], [Phone], [IsActive], [ManagerID]) VALUES (2, N'My Coffee House - Cầu Giấy', N'Số 1 Dịch Vọng Hậu, Cầu Giấy, Hà Nội', N'024777888', 1, 2)
SET IDENTITY_INSERT [dbo].[Branches] OFF
GO
SET IDENTITY_INSERT [dbo].[Categories] ON 

INSERT [dbo].[Categories] ([CategoryID], [CategoryName], [Description]) VALUES (1, N'Cà Phê Máy Và Espresso', N'Cà phê máy và ủ lạnh')
INSERT [dbo].[Categories] ([CategoryID], [CategoryName], [Description]) VALUES (2, N'Frappuccino Và Đá Xay', N'Thức uống đá xay và sinh tố')
INSERT [dbo].[Categories] ([CategoryID], [CategoryName], [Description]) VALUES (3, N'Trà Và Thức Uống Giải Khát', N'Trà, Trà xanh và Thức uống trái cây')
INSERT [dbo].[Categories] ([CategoryID], [CategoryName], [Description]) VALUES (4, N'Sô Cô La & Thức Uống Truyền Thống', N'Cacao và thức uống truyền thống')
SET IDENTITY_INSERT [dbo].[Categories] OFF
GO
INSERT [dbo].[Customers] ([CustomerID], [MemberRank], [CurrentPoints]) VALUES (4, N'Vàng', 150)
GO
INSERT [dbo].[Employees] ([EmployeeID], [BranchID], [SalaryRate], [HireDate]) VALUES (3, 1, CAST(25000.00 AS Decimal(18, 2)), CAST(N'2026-06-24' AS Date))
GO
SET IDENTITY_INSERT [dbo].[OrderDetails] ON 

INSERT [dbo].[OrderDetails] ([OrderDetailID], [OrderID], [ProductID], [Quantity], [UnitPrice], [Note], [ItemStatus], [StartedAt], [CompletedAt]) VALUES (1, 1, 1, 2, CAST(75000.00 AS Decimal(18, 2)), N'Ít đường', N'Pending', NULL, NULL)
INSERT [dbo].[OrderDetails] ([OrderDetailID], [OrderID], [ProductID], [Quantity], [UnitPrice], [Note], [ItemStatus], [StartedAt], [CompletedAt]) VALUES (2, 1, 2, 1, CAST(75000.00 AS Decimal(18, 2)), NULL, N'Pending', NULL, NULL)
INSERT [dbo].[OrderDetails] ([OrderDetailID], [OrderID], [ProductID], [Quantity], [UnitPrice], [Note], [ItemStatus], [StartedAt], [CompletedAt]) VALUES (3, 2, 3, 1, CAST(80000.00 AS Decimal(18, 2)), N'Không đá', N'Preparing', CAST(N'2026-06-24T11:42:26.843' AS DateTime), NULL)
INSERT [dbo].[OrderDetails] ([OrderDetailID], [OrderID], [ProductID], [Quantity], [UnitPrice], [Note], [ItemStatus], [StartedAt], [CompletedAt]) VALUES (4, 2, 4, 2, CAST(85000.00 AS Decimal(18, 2)), NULL, N'Pending', NULL, NULL)
INSERT [dbo].[OrderDetails] ([OrderDetailID], [OrderID], [ProductID], [Quantity], [UnitPrice], [Note], [ItemStatus], [StartedAt], [CompletedAt]) VALUES (5, 3, 5, 1, CAST(85000.00 AS Decimal(18, 2)), N'Nóng, mang gấp', N'Pending', NULL, NULL)
INSERT [dbo].[OrderDetails] ([OrderDetailID], [OrderID], [ProductID], [Quantity], [UnitPrice], [Note], [ItemStatus], [StartedAt], [CompletedAt]) VALUES (6, 3, 1, 1, CAST(75000.00 AS Decimal(18, 2)), NULL, N'Pending', NULL, NULL)
INSERT [dbo].[OrderDetails] ([OrderDetailID], [OrderID], [ProductID], [Quantity], [UnitPrice], [Note], [ItemStatus], [StartedAt], [CompletedAt]) VALUES (7, 4, 2, 1, CAST(75000.00 AS Decimal(18, 2)), NULL, N'Completed', CAST(N'2026-06-24T11:40:26.847' AS DateTime), CAST(N'2026-06-24T11:44:26.847' AS DateTime))
INSERT [dbo].[OrderDetails] ([OrderDetailID], [OrderID], [ProductID], [Quantity], [UnitPrice], [Note], [ItemStatus], [StartedAt], [CompletedAt]) VALUES (8, 4, 3, 2, CAST(80000.00 AS Decimal(18, 2)), NULL, N'Completed', CAST(N'2026-06-24T11:39:26.847' AS DateTime), CAST(N'2026-06-24T11:43:26.847' AS DateTime))
SET IDENTITY_INSERT [dbo].[OrderDetails] OFF
GO
SET IDENTITY_INSERT [dbo].[Orders] ON 

INSERT [dbo].[Orders] ([OrderID], [BranchID], [TableID], [CustomerID], [CashierID], [OrderType], [TotalAmount], [DiscountAmount], [FinalAmount], [OrderStatus], [OrderDate], [Priority]) VALUES (1, 1, 1, NULL, 1, N'Dine-in', CAST(225000.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(225000.00 AS Decimal(18, 2)), N'Pending', CAST(N'2026-06-24T11:37:26.843' AS DateTime), 0)
INSERT [dbo].[Orders] ([OrderID], [BranchID], [TableID], [CustomerID], [CashierID], [OrderType], [TotalAmount], [DiscountAmount], [FinalAmount], [OrderStatus], [OrderDate], [Priority]) VALUES (2, 1, 2, NULL, 1, N'Dine-in', CAST(250000.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(250000.00 AS Decimal(18, 2)), N'Pending', CAST(N'2026-06-24T11:40:26.843' AS DateTime), 0)
INSERT [dbo].[Orders] ([OrderID], [BranchID], [TableID], [CustomerID], [CashierID], [OrderType], [TotalAmount], [DiscountAmount], [FinalAmount], [OrderStatus], [OrderDate], [Priority]) VALUES (3, 1, 3, NULL, 1, N'Dine-in', CAST(160000.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(160000.00 AS Decimal(18, 2)), N'Pending', CAST(N'2026-06-24T11:43:26.847' AS DateTime), 1)
INSERT [dbo].[Orders] ([OrderID], [BranchID], [TableID], [CustomerID], [CashierID], [OrderType], [TotalAmount], [DiscountAmount], [FinalAmount], [OrderStatus], [OrderDate], [Priority]) VALUES (4, 1, 1, NULL, 1, N'Dine-in', CAST(235000.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(235000.00 AS Decimal(18, 2)), N'Pending', CAST(N'2026-06-24T11:38:26.847' AS DateTime), 0)
SET IDENTITY_INSERT [dbo].[Orders] OFF
GO
SET IDENTITY_INSERT [dbo].[Products] ON 

INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (1, N'Cà Phê Latte', 1, CAST(75000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (2, N'Cappuccino', 1, CAST(75000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (3, N'Flat White', 1, CAST(80000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (4, N'Mocha', 1, CAST(85000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (5, N'Caramel Macchiato', 1, CAST(85000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (6, N'Espresso Đá Đường Nâu Yến Mạch', 1, CAST(90000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (7, N'Americano', 1, CAST(60000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (8, N'Latte Dolce Á', 1, CAST(80000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (9, N'Espresso (Solo)', 1, CAST(40000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (10, N'Cold Brew', 1, CAST(65000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (11, N'Cold Brew Bưởi Hồng Mật Ong', 1, CAST(80000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (12, N'Frappuccino Cà Phê', 2, CAST(80000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (13, N'Frappuccino Caramel / Mocha', 2, CAST(90000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (14, N'Frappuccino Java Chip', 2, CAST(100000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (15, N'Frappuccino Kem Vanilla / Caramel', 2, CAST(90000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (16, N'Frappuccino Kem Trà Xanh', 2, CAST(100000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (17, N'Nước Ép Xoài Đam Mê', 2, CAST(80000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (18, N'Sô Cô La Nóng Signature', 4, CAST(75000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (19, N'Sữa Hấp / Sữa Đậu Nành', 4, CAST(40000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (20, N'Latte Matcha Nguyên Chất', 3, CAST(80000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (21, N'Trà Đen Bưởi Hồng Mật Ong', 3, CAST(75000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (22, N'Đá Trà Xanh Dâu Tây Lemonade', 3, CAST(65000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (23, N'Trà English Breakfast / Trà Chanh Bạc Hà', 3, CAST(55000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (24, N'Strawberry Açai Lemonade', 3, CAST(75000.00 AS Decimal(18, 2)), NULL, NULL, 1)
INSERT [dbo].[Products] ([ProductID], [ProductName], [CategoryID], [BasePrice], [ImageURL], [Description], [IsAvailable]) VALUES (25, N'Pink Drink Strawberry Açai', 3, CAST(80000.00 AS Decimal(18, 2)), NULL, NULL, 1)
SET IDENTITY_INSERT [dbo].[Products] OFF
GO
SET IDENTITY_INSERT [dbo].[Roles] ON 

INSERT [dbo].[Roles] ([RoleID], [RoleName], [Description]) VALUES (1, N'Admin', N'Quản trị viên hệ thống chuỗi')
INSERT [dbo].[Roles] ([RoleID], [RoleName], [Description]) VALUES (2, N'Branch Manager', N'Quản lý chi nhánh')
INSERT [dbo].[Roles] ([RoleID], [RoleName], [Description]) VALUES (3, N'Employee', N'Nhân viên cửa hàng')
INSERT [dbo].[Roles] ([RoleID], [RoleName], [Description]) VALUES (4, N'Customer', N'Khách hàng thành viên')
SET IDENTITY_INSERT [dbo].[Roles] OFF
GO
SET IDENTITY_INSERT [dbo].[Tables] ON 

INSERT [dbo].[Tables] ([TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity]) VALUES (1, 1, N'Bàn 01', N'qr_branch1_tb1.png', N'Empty', NULL)
INSERT [dbo].[Tables] ([TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity]) VALUES (2, 1, N'Bàn 02', N'qr_branch1_tb2.png', N'Occupied', NULL)
INSERT [dbo].[Tables] ([TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity]) VALUES (3, 1, N'Bàn 03', N'qr_branch1_tb3.png', N'Empty', NULL)
INSERT [dbo].[Tables] ([TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity]) VALUES (4, 1, N'Bàn 04', N'qr_branch1_tb4.png', N'Serving', NULL)
INSERT [dbo].[Tables] ([TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity]) VALUES (5, 1, N'Bàn 05 (VIP)', N'qr_branch1_tb5.png', N'Empty', NULL)
INSERT [dbo].[Tables] ([TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity]) VALUES (6, 1, N'Bàn 06 (VIP)', N'qr_branch1_tb6.png', N'Empty', NULL)
INSERT [dbo].[Tables] ([TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity]) VALUES (7, 1, N'Sân Thượng 1', N'qr_branch1_st1.png', N'Occupied', NULL)
INSERT [dbo].[Tables] ([TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity]) VALUES (8, 1, N'Sân Thượng 2', N'qr_branch1_st2.png', N'Empty', NULL)
INSERT [dbo].[Tables] ([TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity]) VALUES (9, 2, N'Tầng 1 - Bàn 1', N'qr_branch2_t1b1.png', N'Empty', NULL)
INSERT [dbo].[Tables] ([TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity]) VALUES (10, 2, N'Tầng 1 - Bàn 2', N'qr_branch2_t1b2.png', N'Empty', NULL)
INSERT [dbo].[Tables] ([TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity]) VALUES (11, 2, N'Tầng 1 - Bàn 3', N'qr_branch2_t1b3.png', N'Occupied', NULL)
INSERT [dbo].[Tables] ([TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity]) VALUES (12, 2, N'Tầng 2 - Bàn 1', N'qr_branch2_t2b1.png', N'Empty', NULL)
INSERT [dbo].[Tables] ([TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity]) VALUES (13, 2, N'Tầng 2 - Bàn 2', N'qr_branch2_t2b2.png', N'Occupied', NULL)
INSERT [dbo].[Tables] ([TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity]) VALUES (14, 2, N'Ban công 1', N'qr_branch2_bc1.png', N'Empty', NULL)
SET IDENTITY_INSERT [dbo].[Tables] OFF
GO
SET IDENTITY_INSERT [dbo].[Users] ON 

INSERT [dbo].[Users] ([UserID], [Username], [Password], [FullName], [Email], [Phone], [RoleID], [IsActive], [CreatedAt]) VALUES (1, N'admin1', N'hashed_pwd_123', N'Nguyễn Văn Admin', N'admin@cafe.com', N'0911111111', 1, 1, CAST(N'2026-06-24T11:41:58.930' AS DateTime))
INSERT [dbo].[Users] ([UserID], [Username], [Password], [FullName], [Email], [Phone], [RoleID], [IsActive], [CreatedAt]) VALUES (2, N'manager1', N'hashed_pwd_123', N'Trần Chi Nhánh', N'manager1@cafe.com', N'0922222222', 2, 1, CAST(N'2026-06-24T11:41:58.930' AS DateTime))
INSERT [dbo].[Users] ([UserID], [Username], [Password], [FullName], [Email], [Phone], [RoleID], [IsActive], [CreatedAt]) VALUES (3, N'staff1', N'hashed_pwd_123', N'Lê Pha Chế', N'staff1@cafe.com', N'0933333333', 3, 1, CAST(N'2026-06-24T11:41:58.930' AS DateTime))
INSERT [dbo].[Users] ([UserID], [Username], [Password], [FullName], [Email], [Phone], [RoleID], [IsActive], [CreatedAt]) VALUES (4, N'khachhang1', N'hashed_pwd_123', N'Phan Khách Quen', N'khach1@gmail.com', N'0944444444', 4, 1, CAST(N'2026-06-24T11:41:58.930' AS DateTime))
INSERT [dbo].[Users] ([UserID], [Username], [Password], [FullName], [Email], [Phone], [RoleID], [IsActive], [CreatedAt]) VALUES (6, N'barista', N'123', N'Nhân viên Pha chế', N'barista@mycoffee.com', N'0900000004', 4, 1, CAST(N'2026-06-24T11:45:11.037' AS DateTime))
SET IDENTITY_INSERT [dbo].[Users] OFF
GO
SET IDENTITY_INSERT [dbo].[Vouchers] ON 

INSERT [dbo].[Vouchers] ([VoucherID], [VoucherCode], [DiscountValue], [IsPercentage], [MinOrderValue], [MaxDiscount], [UsageLimit], [UsedCount], [StartDate], [EndDate], [IsActive], [ProductID]) VALUES (1, N'WELCOME30K', CAST(30000.00 AS Decimal(18, 2)), 0, CAST(100000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(10, 2)), 1000, 0, CAST(N'2026-01-01T00:00:00.000' AS DateTime), CAST(N'2026-12-31T23:59:59.000' AS DateTime), 1, NULL)
INSERT [dbo].[Vouchers] ([VoucherID], [VoucherCode], [DiscountValue], [IsPercentage], [MinOrderValue], [MaxDiscount], [UsageLimit], [UsedCount], [StartDate], [EndDate], [IsActive], [ProductID]) VALUES (2, N'SUMMER15', CAST(15.00 AS Decimal(18, 2)), 1, CAST(150000.00 AS Decimal(18, 2)), CAST(50000.00 AS Decimal(10, 2)), 500, 0, CAST(N'2026-06-01T00:00:00.000' AS DateTime), CAST(N'2026-06-15T23:59:59.000' AS DateTime), 1, NULL)
INSERT [dbo].[Vouchers] ([VoucherID], [VoucherCode], [DiscountValue], [IsPercentage], [MinOrderValue], [MaxDiscount], [UsageLimit], [UsedCount], [StartDate], [EndDate], [IsActive], [ProductID]) VALUES (3, N'WORLDCUP20', CAST(20.00 AS Decimal(18, 2)), 1, CAST(200000.00 AS Decimal(18, 2)), CAST(60000.00 AS Decimal(10, 2)), 300, 0, CAST(N'2026-06-16T00:00:00.000' AS DateTime), CAST(N'2026-06-30T23:59:59.000' AS DateTime), 1, NULL)
INSERT [dbo].[Vouchers] ([VoucherID], [VoucherCode], [DiscountValue], [IsPercentage], [MinOrderValue], [MaxDiscount], [UsageLimit], [UsedCount], [StartDate], [EndDate], [IsActive], [ProductID]) VALUES (4, N'TEA20_M1', CAST(20.00 AS Decimal(18, 2)), 1, CAST(50000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(10, 2)), 200, 0, CAST(N'2026-07-01T00:00:00.000' AS DateTime), CAST(N'2026-07-10T23:59:59.000' AS DateTime), 1, NULL)
INSERT [dbo].[Vouchers] ([VoucherID], [VoucherCode], [DiscountValue], [IsPercentage], [MinOrderValue], [MaxDiscount], [UsageLimit], [UsedCount], [StartDate], [EndDate], [IsActive], [ProductID]) VALUES (5, N'TEA20_M2', CAST(20.00 AS Decimal(18, 2)), 1, CAST(50000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(10, 2)), 200, 0, CAST(N'2026-07-01T00:00:00.000' AS DateTime), CAST(N'2026-07-10T23:59:59.000' AS DateTime), 1, NULL)
INSERT [dbo].[Vouchers] ([VoucherID], [VoucherCode], [DiscountValue], [IsPercentage], [MinOrderValue], [MaxDiscount], [UsageLimit], [UsedCount], [StartDate], [EndDate], [IsActive], [ProductID]) VALUES (6, N'STUDENT15K_M1', CAST(15000.00 AS Decimal(18, 2)), 0, CAST(0.00 AS Decimal(18, 2)), CAST(15000.00 AS Decimal(10, 2)), 300, 0, CAST(N'2026-07-15T00:00:00.000' AS DateTime), CAST(N'2026-07-20T23:59:59.000' AS DateTime), 1, NULL)
INSERT [dbo].[Vouchers] ([VoucherID], [VoucherCode], [DiscountValue], [IsPercentage], [MinOrderValue], [MaxDiscount], [UsageLimit], [UsedCount], [StartDate], [EndDate], [IsActive], [ProductID]) VALUES (7, N'STUDENT15K_M2', CAST(15000.00 AS Decimal(18, 2)), 0, CAST(0.00 AS Decimal(18, 2)), CAST(15000.00 AS Decimal(10, 2)), 300, 0, CAST(N'2026-07-15T00:00:00.000' AS DateTime), CAST(N'2026-07-20T23:59:59.000' AS DateTime), 1, NULL)
INSERT [dbo].[Vouchers] ([VoucherID], [VoucherCode], [DiscountValue], [IsPercentage], [MinOrderValue], [MaxDiscount], [UsageLimit], [UsedCount], [StartDate], [EndDate], [IsActive], [ProductID]) VALUES (8, N'LATTE15K', CAST(15000.00 AS Decimal(18, 2)), 0, CAST(0.00 AS Decimal(18, 2)), CAST(15000.00 AS Decimal(10, 2)), 100, 0, CAST(N'2026-08-01T00:00:00.000' AS DateTime), CAST(N'2026-08-03T23:59:59.000' AS DateTime), 1, NULL)
INSERT [dbo].[Vouchers] ([VoucherID], [VoucherCode], [DiscountValue], [IsPercentage], [MinOrderValue], [MaxDiscount], [UsageLimit], [UsedCount], [StartDate], [EndDate], [IsActive], [ProductID]) VALUES (9, N'CAPMOCHA20K_M1', CAST(20000.00 AS Decimal(18, 2)), 0, CAST(80000.00 AS Decimal(18, 2)), CAST(20000.00 AS Decimal(10, 2)), 150, 0, CAST(N'2026-08-10T00:00:00.000' AS DateTime), CAST(N'2026-08-15T23:59:59.000' AS DateTime), 1, NULL)
INSERT [dbo].[Vouchers] ([VoucherID], [VoucherCode], [DiscountValue], [IsPercentage], [MinOrderValue], [MaxDiscount], [UsageLimit], [UsedCount], [StartDate], [EndDate], [IsActive], [ProductID]) VALUES (10, N'CAPMOCHA20K_M2', CAST(20000.00 AS Decimal(18, 2)), 0, CAST(80000.00 AS Decimal(18, 2)), CAST(20000.00 AS Decimal(10, 2)), 150, 0, CAST(N'2026-08-10T00:00:00.000' AS DateTime), CAST(N'2026-08-15T23:59:59.000' AS DateTime), 1, NULL)
INSERT [dbo].[Vouchers] ([VoucherID], [VoucherCode], [DiscountValue], [IsPercentage], [MinOrderValue], [MaxDiscount], [UsageLimit], [UsedCount], [StartDate], [EndDate], [IsActive], [ProductID]) VALUES (11, N'VIP50K', CAST(50000.00 AS Decimal(18, 2)), 0, CAST(500000.00 AS Decimal(18, 2)), CAST(50000.00 AS Decimal(10, 2)), 200, 0, CAST(N'2026-01-01T00:00:00.000' AS DateTime), CAST(N'2026-12-31T23:59:59.000' AS DateTime), 1, NULL)
INSERT [dbo].[Vouchers] ([VoucherID], [VoucherCode], [DiscountValue], [IsPercentage], [MinOrderValue], [MaxDiscount], [UsageLimit], [UsedCount], [StartDate], [EndDate], [IsActive], [ProductID]) VALUES (12, N'EXPIRED50K', CAST(50000.00 AS Decimal(18, 2)), 0, CAST(100000.00 AS Decimal(18, 2)), CAST(50000.00 AS Decimal(10, 2)), 100, 0, CAST(N'2026-05-01T00:00:00.000' AS DateTime), CAST(N'2026-05-31T23:59:59.000' AS DateTime), 1, NULL)
SET IDENTITY_INSERT [dbo].[Vouchers] OFF
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ_Categories_CategoryName]    Script Date: 6/24/2026 1:19:41 PM ******/
ALTER TABLE [dbo].[Categories] ADD  CONSTRAINT [UQ_Categories_CategoryName] UNIQUE NONCLUSTERED 
(
	[CategoryName] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ_Roles_RoleName]    Script Date: 6/24/2026 1:19:41 PM ******/
ALTER TABLE [dbo].[Roles] ADD  CONSTRAINT [UQ_Roles_RoleName] UNIQUE NONCLUSTERED 
(
	[RoleName] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ_Users_Email]    Script Date: 6/24/2026 1:19:41 PM ******/
CREATE UNIQUE NONCLUSTERED INDEX [UQ_Users_Email] ON [dbo].[Users]
(
	[Email] ASC
)WHERE ([Email] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ_Users_Phone]    Script Date: 6/24/2026 1:19:41 PM ******/
CREATE UNIQUE NONCLUSTERED INDEX [UQ_Users_Phone] ON [dbo].[Users]
(
	[Phone] ASC
)WHERE ([Phone] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ_Users_Username]    Script Date: 6/24/2026 1:19:41 PM ******/
ALTER TABLE [dbo].[Users] ADD  CONSTRAINT [UQ_Users_Username] UNIQUE NONCLUSTERED 
(
	[Username] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ_Vouchers_VoucherCode]    Script Date: 6/24/2026 1:19:41 PM ******/
ALTER TABLE [dbo].[Vouchers] ADD  CONSTRAINT [UQ_Vouchers_VoucherCode] UNIQUE NONCLUSTERED 
(
	[VoucherCode] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Branches] ADD  DEFAULT ((1)) FOR [IsActive]
GO
ALTER TABLE [dbo].[Customers] ADD  DEFAULT ('Member') FOR [MemberRank]
GO
ALTER TABLE [dbo].[Customers] ADD  DEFAULT ((0)) FOR [CurrentPoints]
GO
ALTER TABLE [dbo].[Employees] ADD  DEFAULT ((0)) FOR [SalaryRate]
GO
ALTER TABLE [dbo].[Employees] ADD  DEFAULT (getdate()) FOR [HireDate]
GO
ALTER TABLE [dbo].[Feedbacks] ADD  DEFAULT (getdate()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Inventory] ADD  DEFAULT (getdate()) FOR [LastUpdated]
GO
ALTER TABLE [dbo].[OrderDetails] ADD  DEFAULT ('Pending') FOR [ItemStatus]
GO
ALTER TABLE [dbo].[Orders] ADD  DEFAULT ('Eat-in') FOR [OrderType]
GO
ALTER TABLE [dbo].[Orders] ADD  DEFAULT ((0)) FOR [TotalAmount]
GO
ALTER TABLE [dbo].[Orders] ADD  DEFAULT ((0)) FOR [DiscountAmount]
GO
ALTER TABLE [dbo].[Orders] ADD  DEFAULT ((0)) FOR [FinalAmount]
GO
ALTER TABLE [dbo].[Orders] ADD  DEFAULT ('Pending') FOR [OrderStatus]
GO
ALTER TABLE [dbo].[Orders] ADD  DEFAULT (getdate()) FOR [OrderDate]
GO
ALTER TABLE [dbo].[Orders] ADD  DEFAULT ((0)) FOR [Priority]
GO
ALTER TABLE [dbo].[Payments] ADD  DEFAULT (getdate()) FOR [PaymentTime]
GO
ALTER TABLE [dbo].[ProductBranches] ADD  DEFAULT ((1)) FOR [IsAvailable]
GO
ALTER TABLE [dbo].[Products] ADD  DEFAULT ((1)) FOR [IsAvailable]
GO
ALTER TABLE [dbo].[Tables] ADD  DEFAULT ('Empty') FOR [Status]
GO
ALTER TABLE [dbo].[Tables] ADD  DEFAULT ((4)) FOR [Capacity]
GO
ALTER TABLE [dbo].[Users] ADD  DEFAULT ((1)) FOR [IsActive]
GO
ALTER TABLE [dbo].[Users] ADD  DEFAULT (getdate()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Vouchers] ADD  DEFAULT ((0)) FOR [IsPercentage]
GO
ALTER TABLE [dbo].[Vouchers] ADD  DEFAULT ((0)) FOR [MinOrderValue]
GO
ALTER TABLE [dbo].[Vouchers] ADD  DEFAULT ((100)) FOR [UsageLimit]
GO
ALTER TABLE [dbo].[Vouchers] ADD  DEFAULT ((0)) FOR [UsedCount]
GO
ALTER TABLE [dbo].[Vouchers] ADD  DEFAULT ((1)) FOR [IsActive]
GO
ALTER TABLE [dbo].[Attendance]  WITH CHECK ADD  CONSTRAINT [FK_Attendance_Employees] FOREIGN KEY([EmployeeID])
REFERENCES [dbo].[Employees] ([EmployeeID])
GO
ALTER TABLE [dbo].[Attendance] CHECK CONSTRAINT [FK_Attendance_Employees]
GO
ALTER TABLE [dbo].[Attendance]  WITH CHECK ADD  CONSTRAINT [FK_Attendance_Shifts] FOREIGN KEY([ShiftID])
REFERENCES [dbo].[Shifts] ([ShiftID])
GO
ALTER TABLE [dbo].[Attendance] CHECK CONSTRAINT [FK_Attendance_Shifts]
GO
ALTER TABLE [dbo].[Branches]  WITH CHECK ADD  CONSTRAINT [FK_Branch_Manager] FOREIGN KEY([ManagerID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[Branches] CHECK CONSTRAINT [FK_Branch_Manager]
GO
ALTER TABLE [dbo].[Customers]  WITH CHECK ADD  CONSTRAINT [FK_Customers_Users] FOREIGN KEY([CustomerID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[Customers] CHECK CONSTRAINT [FK_Customers_Users]
GO
ALTER TABLE [dbo].[Employees]  WITH CHECK ADD  CONSTRAINT [FK_Employees_Branches] FOREIGN KEY([BranchID])
REFERENCES [dbo].[Branches] ([BranchID])
GO
ALTER TABLE [dbo].[Employees] CHECK CONSTRAINT [FK_Employees_Branches]
GO
ALTER TABLE [dbo].[Employees]  WITH CHECK ADD  CONSTRAINT [FK_Employees_Users] FOREIGN KEY([EmployeeID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[Employees] CHECK CONSTRAINT [FK_Employees_Users]
GO
ALTER TABLE [dbo].[Feedbacks]  WITH CHECK ADD  CONSTRAINT [FK_Feedbacks_Orders] FOREIGN KEY([OrderID])
REFERENCES [dbo].[Orders] ([OrderID])
GO
ALTER TABLE [dbo].[Feedbacks] CHECK CONSTRAINT [FK_Feedbacks_Orders]
GO
ALTER TABLE [dbo].[Inventory]  WITH CHECK ADD  CONSTRAINT [FK_Inventory_Branches] FOREIGN KEY([BranchID])
REFERENCES [dbo].[Branches] ([BranchID])
GO
ALTER TABLE [dbo].[Inventory] CHECK CONSTRAINT [FK_Inventory_Branches]
GO
ALTER TABLE [dbo].[Inventory]  WITH CHECK ADD  CONSTRAINT [FK_Inventory_Ingredients] FOREIGN KEY([IngredientID])
REFERENCES [dbo].[Ingredients] ([IngredientID])
GO
ALTER TABLE [dbo].[Inventory] CHECK CONSTRAINT [FK_Inventory_Ingredients]
GO
ALTER TABLE [dbo].[OrderDetails]  WITH CHECK ADD  CONSTRAINT [FK_OrderDetails_Orders] FOREIGN KEY([OrderID])
REFERENCES [dbo].[Orders] ([OrderID])
GO
ALTER TABLE [dbo].[OrderDetails] CHECK CONSTRAINT [FK_OrderDetails_Orders]
GO
ALTER TABLE [dbo].[OrderDetails]  WITH CHECK ADD  CONSTRAINT [FK_OrderDetails_Products] FOREIGN KEY([ProductID])
REFERENCES [dbo].[Products] ([ProductID])
GO
ALTER TABLE [dbo].[OrderDetails] CHECK CONSTRAINT [FK_OrderDetails_Products]
GO
ALTER TABLE [dbo].[Orders]  WITH CHECK ADD  CONSTRAINT [FK_Orders_Branches] FOREIGN KEY([BranchID])
REFERENCES [dbo].[Branches] ([BranchID])
GO
ALTER TABLE [dbo].[Orders] CHECK CONSTRAINT [FK_Orders_Branches]
GO
ALTER TABLE [dbo].[Orders]  WITH CHECK ADD  CONSTRAINT [FK_Orders_Cashier] FOREIGN KEY([CashierID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[Orders] CHECK CONSTRAINT [FK_Orders_Cashier]
GO
ALTER TABLE [dbo].[Orders]  WITH CHECK ADD  CONSTRAINT [FK_Orders_Customers] FOREIGN KEY([CustomerID])
REFERENCES [dbo].[Customers] ([CustomerID])
GO
ALTER TABLE [dbo].[Orders] CHECK CONSTRAINT [FK_Orders_Customers]
GO
ALTER TABLE [dbo].[Orders]  WITH CHECK ADD  CONSTRAINT [FK_Orders_Tables] FOREIGN KEY([TableID])
REFERENCES [dbo].[Tables] ([TableID])
GO
ALTER TABLE [dbo].[Orders] CHECK CONSTRAINT [FK_Orders_Tables]
GO
ALTER TABLE [dbo].[Payments]  WITH CHECK ADD  CONSTRAINT [FK_Payments_Orders] FOREIGN KEY([OrderID])
REFERENCES [dbo].[Orders] ([OrderID])
GO
ALTER TABLE [dbo].[Payments] CHECK CONSTRAINT [FK_Payments_Orders]
GO
ALTER TABLE [dbo].[Payments]  WITH CHECK ADD  CONSTRAINT [FK_Payments_Vouchers] FOREIGN KEY([VoucherID])
REFERENCES [dbo].[Vouchers] ([VoucherID])
GO
ALTER TABLE [dbo].[Payments] CHECK CONSTRAINT [FK_Payments_Vouchers]
GO
ALTER TABLE [dbo].[ProductBranches]  WITH CHECK ADD  CONSTRAINT [FK_ProductBranches_Branches] FOREIGN KEY([BranchID])
REFERENCES [dbo].[Branches] ([BranchID])
GO
ALTER TABLE [dbo].[ProductBranches] CHECK CONSTRAINT [FK_ProductBranches_Branches]
GO
ALTER TABLE [dbo].[ProductBranches]  WITH CHECK ADD  CONSTRAINT [FK_ProductBranches_Products] FOREIGN KEY([ProductID])
REFERENCES [dbo].[Products] ([ProductID])
GO
ALTER TABLE [dbo].[ProductBranches] CHECK CONSTRAINT [FK_ProductBranches_Products]
GO
ALTER TABLE [dbo].[Products]  WITH CHECK ADD  CONSTRAINT [FK_Products_Categories] FOREIGN KEY([CategoryID])
REFERENCES [dbo].[Categories] ([CategoryID])
GO
ALTER TABLE [dbo].[Products] CHECK CONSTRAINT [FK_Products_Categories]
GO
ALTER TABLE [dbo].[Tables]  WITH CHECK ADD  CONSTRAINT [FK_Tables_Branches] FOREIGN KEY([BranchID])
REFERENCES [dbo].[Branches] ([BranchID])
GO
ALTER TABLE [dbo].[Tables] CHECK CONSTRAINT [FK_Tables_Branches]
GO
ALTER TABLE [dbo].[Users]  WITH CHECK ADD  CONSTRAINT [FK_Users_Roles] FOREIGN KEY([RoleID])
REFERENCES [dbo].[Roles] ([RoleID])
GO
ALTER TABLE [dbo].[Users] CHECK CONSTRAINT [FK_Users_Roles]
GO
ALTER TABLE [dbo].[Vouchers]  WITH CHECK ADD  CONSTRAINT [FK_Vouchers_Products] FOREIGN KEY([ProductID])
REFERENCES [dbo].[Products] ([ProductID])
GO
ALTER TABLE [dbo].[Vouchers] CHECK CONSTRAINT [FK_Vouchers_Products]
GO
ALTER TABLE [dbo].[Feedbacks]  WITH CHECK ADD CHECK  (([Rating]>=(1) AND [Rating]<=(5)))
GO
USE [master]
GO
ALTER DATABASE [MyCoffeeHouse] SET  READ_WRITE 
GO
