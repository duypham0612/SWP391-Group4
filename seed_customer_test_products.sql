USE MyCoffeeHouse;
GO

SET NOCOUNT ON;

IF NOT EXISTS (SELECT 1 FROM Categories WHERE CategoryName = N'Cafe')
    INSERT INTO Categories (CategoryName, Description)
    VALUES (N'Cafe', N'Cà phê, espresso, latte và cold brew');

IF NOT EXISTS (SELECT 1 FROM Categories WHERE CategoryName = N'Trà sữa')
    INSERT INTO Categories (CategoryName, Description)
    VALUES (N'Trà sữa', N'Trà sữa và đồ uống kem sữa');

IF NOT EXISTS (SELECT 1 FROM Categories WHERE CategoryName = N'Trà trái cây')
    INSERT INTO Categories (CategoryName, Description)
    VALUES (N'Trà trái cây', N'Trà giải khát kết hợp trái cây tươi');

IF NOT EXISTS (SELECT 1 FROM Categories WHERE CategoryName = N'Bánh ngọt')
    INSERT INTO Categories (CategoryName, Description)
    VALUES (N'Bánh ngọt', N'Bánh ăn kèm cà phê và trà');

IF NOT EXISTS (SELECT 1 FROM Categories WHERE CategoryName = N'Đồ ăn nhẹ')
    INSERT INTO Categories (CategoryName, Description)
    VALUES (N'Đồ ăn nhẹ', N'Món nhẹ dùng nhanh tại quán');
GO

DECLARE @Cafe int = (SELECT TOP 1 CategoryID FROM Categories WHERE CategoryName = N'Cafe');
DECLARE @MilkTea int = (SELECT TOP 1 CategoryID FROM Categories WHERE CategoryName = N'Trà sữa');
DECLARE @FruitTea int = (SELECT TOP 1 CategoryID FROM Categories WHERE CategoryName = N'Trà trái cây');
DECLARE @Cake int = (SELECT TOP 1 CategoryID FROM Categories WHERE CategoryName = N'Bánh ngọt');
DECLARE @Snack int = (SELECT TOP 1 CategoryID FROM Categories WHERE CategoryName = N'Đồ ăn nhẹ');

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Caramel Latte Muối')
    INSERT INTO Products (ProductName, CategoryID, BasePrice, ImageURL, Description, IsAvailable)
    VALUES (N'Caramel Latte Muối', @Cafe, 55000,
            N'https://images.unsplash.com/photo-1461023058943-07fcbe16d735?auto=format&fit=crop&w=900&q=85',
            N'Sự kết hợp hài hòa giữa vị đắng nhẹ của espresso, vị béo của sữa và chút mặn ngọt của caramel muối.',
            1);

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Trà Đào Cam Sả')
    INSERT INTO Products (ProductName, CategoryID, BasePrice, ImageURL, Description, IsAvailable)
    VALUES (N'Trà Đào Cam Sả', @FruitTea, 45000,
            N'https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&w=900&q=85',
            N'Trà thanh mát kết hợp đào miếng, cam tươi và hương sả nhẹ, phù hợp cho ngày nóng.',
            1);

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Bánh Croissant Socola')
    INSERT INTO Products (ProductName, CategoryID, BasePrice, ImageURL, Description, IsAvailable)
    VALUES (N'Bánh Croissant Socola', @Cake, 39000,
            N'https://images.unsplash.com/photo-1623334044303-241021148842?auto=format&fit=crop&w=900&q=85',
            N'Vỏ bánh ngàn lớp giòn rụm, nhân socola tan chảy, dùng ngon nhất khi ăn kèm latte nóng.',
            1);

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Matcha Latte Nhật Bản')
    INSERT INTO Products (ProductName, CategoryID, BasePrice, ImageURL, Description, IsAvailable)
    VALUES (N'Matcha Latte Nhật Bản', @MilkTea, 59000,
            N'https://images.unsplash.com/photo-1536256263959-770b48d82b0a?auto=format&fit=crop&w=900&q=85',
            N'Bột matcha nguyên chất hòa cùng sữa tươi, vị thơm thanh, béo nhẹ và hậu vị ngọt dịu.',
            1);

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Cold Brew Cam Vàng')
    INSERT INTO Products (ProductName, CategoryID, BasePrice, ImageURL, Description, IsAvailable)
    VALUES (N'Cold Brew Cam Vàng', @Cafe, 62000,
            N'https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?auto=format&fit=crop&w=900&q=85',
            N'Cold brew ủ lạnh vị êm, thêm lát cam vàng và syrup nhẹ để tạo hậu vị tươi mát.',
            1);

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Trà Vải Hoa Hồng')
    INSERT INTO Products (ProductName, CategoryID, BasePrice, ImageURL, Description, IsAvailable)
    VALUES (N'Trà Vải Hoa Hồng', @FruitTea, 52000,
            N'https://images.unsplash.com/photo-1544787219-7f47ccb76574?auto=format&fit=crop&w=900&q=85',
            N'Trà trái cây thơm hương hoa hồng, thêm vải mọng nước và lớp đá mát sảng khoái.',
            1);

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Trà Sữa Oolong Kem Cheese')
    INSERT INTO Products (ProductName, CategoryID, BasePrice, ImageURL, Description, IsAvailable)
    VALUES (N'Trà Sữa Oolong Kem Cheese', @MilkTea, 57000,
            N'https://images.unsplash.com/photo-1558857563-b371033873b8?auto=format&fit=crop&w=900&q=85',
            N'Trà oolong rang thơm, sữa béo vừa phải và lớp kem cheese mặn nhẹ.',
            1);

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Bạc Xỉu Kem Muối')
    INSERT INTO Products (ProductName, CategoryID, BasePrice, ImageURL, Description, IsAvailable)
    VALUES (N'Bạc Xỉu Kem Muối', @Cafe, 49000,
            N'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=900&q=85',
            N'Bạc xỉu đậm vị sữa, phủ kem muối mềm mịn giúp cân bằng độ ngọt.',
            1);

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Cheesecake Việt Quất')
    INSERT INTO Products (ProductName, CategoryID, BasePrice, ImageURL, Description, IsAvailable)
    VALUES (N'Cheesecake Việt Quất', @Cake, 46000,
            N'https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&w=900&q=85',
            N'Cheesecake mềm béo, sốt việt quất chua ngọt và đế bánh quy giòn nhẹ.',
            1);

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Sandwich Gà Phô Mai')
    INSERT INTO Products (ProductName, CategoryID, BasePrice, ImageURL, Description, IsAvailable)
    VALUES (N'Sandwich Gà Phô Mai', @Snack, 59000,
            N'https://images.unsplash.com/photo-1528735602780-2552fd46c7af?auto=format&fit=crop&w=900&q=85',
            N'Sandwich nóng với gà xé, phô mai kéo sợi và rau tươi, hợp cho bữa nhẹ.',
            1);

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Soda Dâu Chanh')
    INSERT INTO Products (ProductName, CategoryID, BasePrice, ImageURL, Description, IsAvailable)
    VALUES (N'Soda Dâu Chanh', @FruitTea, 42000,
            N'https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=900&q=85',
            N'Soda có gas nhẹ, dâu tươi và chanh vàng tạo vị chua ngọt tươi sáng.',
            1);

IF NOT EXISTS (SELECT 1 FROM Products WHERE ProductName = N'Cookie Hạnh Nhân')
    INSERT INTO Products (ProductName, CategoryID, BasePrice, ImageURL, Description, IsAvailable)
    VALUES (N'Cookie Hạnh Nhân', @Cake, 29000,
            N'https://images.unsplash.com/photo-1499636136210-6f4ee915583e?auto=format&fit=crop&w=900&q=85',
            N'Cookie bơ giòn, thơm hạnh nhân rang, tiện dùng kèm cà phê hoặc trà nóng.',
            1);
GO

SELECT ProductID, ProductName, CategoryID, BasePrice, IsAvailable
FROM Products
WHERE ProductName IN (
    N'Caramel Latte Muối',
    N'Trà Đào Cam Sả',
    N'Bánh Croissant Socola',
    N'Matcha Latte Nhật Bản',
    N'Cold Brew Cam Vàng',
    N'Trà Vải Hoa Hồng',
    N'Trà Sữa Oolong Kem Cheese',
    N'Bạc Xỉu Kem Muối',
    N'Cheesecake Việt Quất',
    N'Sandwich Gà Phô Mai',
    N'Soda Dâu Chanh',
    N'Cookie Hạnh Nhân'
)
ORDER BY ProductID;
GO
