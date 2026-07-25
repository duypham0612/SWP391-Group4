/* ===========================================================================
   DEMO — dữ liệu test hiển thị SIZE & MODIFIER trên màn Quầy pha chế (KDS).

   Vì sao cần: seed gốc trong database.sql tạo đơn cho KDS nhưng không gắn
   sales.OrderItemModifier nào (và trên DB dev dựng từ bản cũ, catalog modifier
   còn rỗng hoàn toàn), nên dải chip ở _kdsQueueRow.jsp không bao giờ hiện.
   Script này dựng đủ các nhánh hiển thị của dòng KDS:
     ZT1 · Tại bàn  — món nhiều chip (size + đường + topping) + ghi chú dài
                    — món KHÔNG chip nào (đối chứng: cả dải tags phải biến mất)
     ZT2 · Mang đi  — không có bàn → cột Bàn hiện nhãn loại đơn + mã gọi món
     ZT3 · QR       — món ĐANG PHA 6 chip (test xuống dòng) do barista nhận
                    — món ĐÃ PHA XONG có nơi đặt (làm mờ, không đánh số)
     ZT4 · Giao hàng— món BỊ CHẶN có chip sự cố đỏ cạnh chip size
                    — món LÀM LẠI (chip ⟳ cạnh tên, nhảy lên đầu danh sách)

   Chạy lại được nhiều lần: đơn demo nhận diện bằng PickupCode 'ZT%' (mã do app
   sinh luôn là D/T/G + số nên không đụng nhau); lần chạy sau HUỶ bộ demo cũ rồi
   dựng bộ mới — xem lý do không xoá ở mục 1.

   Chạy trong SSMS / DataGrip / IntelliJ Database — nguyên file là MỘT batch,
   không chèn GO vào giữa (mọi biến DECLARE dùng xuyên suốt).
   =========================================================================== */
SET NOCOUNT ON;
SET XACT_ABORT ON;

/* --- Chọn chi nhánh cần test: CN01 (barista1) · CN02 (barista3) · CN03 (barista6) --- */
DECLARE @BranchCode VARCHAR(20) = 'CN01';

DECLARE @b INT = (SELECT BranchId FROM org.Branch WHERE Code = @BranchCode);
IF @b IS NULL THROW 50001, N'Không tìm thấy chi nhánh theo @BranchCode.', 1;

DECLARE @bar INT = (SELECT TOP(1) u.UserId FROM iam.[User] u
                    JOIN iam.Role r ON r.RoleId = u.RoleId
                    WHERE u.BranchId = @b AND r.Code = 'BARISTA' ORDER BY u.UserId);
DECLARE @cas INT = (SELECT TOP(1) u.UserId FROM iam.[User] u
                    JOIN iam.Role r ON r.RoleId = u.RoleId
                    WHERE u.BranchId = @b AND r.Code = 'CASHIER' ORDER BY u.UserId);
IF @bar IS NULL THROW 50002, N'Chi nhánh chưa có tài khoản BARISTA.', 1;

-- DB lưu giờ UTC; các mốc dưới đây lùi vài phút để đơn luôn nằm trong ngày
-- kinh doanh hiện tại (BusinessDay.startUtc cắt theo giờ mở cửa chi nhánh).
DECLARE @now DATETIME2 = SYSUTCDATETIME();

/* ---------------------------------------------------------------------------
   1) DỌN DỮ LIỆU DEMO LẦN TRƯỚC — bằng cách HUỶ, không xoá.
   Không dùng DELETE trên sales.Orders/sales.OrderItem: các bảng này có FK
   ON DELETE CASCADE và SQL Server 2022 ở máy dev báo lỗi 8624 "could not
   produce a query plan" cho mọi DELETE trên chúng. App không bao giờ xoá đơn
   nên đây chỉ là giới hạn của script. Huỷ là đủ: truy vấn KDS lọc
   o.Status='ACTIVE' và oi.Status IN ('WAITING','MAKING','READY','BLOCKED'),
   nên đơn demo cũ biến mất khỏi màn ngay khi chuyển sang CANCELLED.
   --------------------------------------------------------------------------- */
DECLARE @oldItems TABLE (OrderItemId INT PRIMARY KEY);
DECLARE @oldSessions TABLE (TableSessionId INT PRIMARY KEY);

INSERT INTO @oldItems(OrderItemId)
SELECT oi.OrderItemId
FROM sales.OrderItem oi
JOIN sales.Orders o ON o.OrderId = oi.OrderId
WHERE o.BranchId = @b AND o.PickupCode LIKE 'ZT%';

INSERT INTO @oldSessions(TableSessionId)
SELECT DISTINCT o.TableSessionId
FROM sales.Orders o
WHERE o.BranchId = @b AND o.PickupCode LIKE 'ZT%' AND o.TableSessionId IS NOT NULL;

BEGIN TRAN;

-- Chip modifier là thứ duy nhất cần dọn sạch để lần chạy sau không cộng dồn.
DELETE FROM sales.OrderItemModifier WHERE OrderItemId IN (SELECT OrderItemId FROM @oldItems);

UPDATE sales.OrderItem
SET Status = 'CANCELLED', BaristaId = NULL, PreparedBy = NULL,
    HasIssue = 0, IssueReason = NULL, HandoverLocation = NULL
WHERE OrderItemId IN (SELECT OrderItemId FROM @oldItems);

UPDATE sales.Orders SET Status = 'CANCELLED'
WHERE BranchId = @b AND PickupCode LIKE 'ZT%';

-- Trả bàn của phiên demo cũ về trạng thái trống.
UPDATE sales.TableSession SET Status = 'CLOSED', ClosedAt = @now
WHERE TableSessionId IN (SELECT TableSessionId FROM @oldSessions) AND Status = 'OPEN';

UPDATE dt SET Status = 'EMPTY'
FROM sales.DiningTable dt
WHERE dt.DiningTableId IN (SELECT ts.DiningTableId FROM sales.TableSession ts
                           WHERE ts.TableSessionId IN (SELECT TableSessionId FROM @oldSessions))
  AND NOT EXISTS (SELECT 1 FROM sales.TableSession op
                  WHERE op.DiningTableId = dt.DiningTableId AND op.Status = 'OPEN');

/* ---------------------------------------------------------------------------
   2) CATALOG MODIFIER — tạo nếu DB chưa có
   DB dev có thể được dựng từ bản database.sql cũ hơn: bảng modifier tồn tại
   nhưng KHÔNG có dữ liệu (0 group / 0 option / 0 link). Khi đó POS lẫn QR đều
   không hiện ô chọn size, và KDS không có gì để in ra chip. Block này idempotent
   — chạy lại không nhân đôi dữ liệu.
   --------------------------------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM catalog.ModifierGroup WHERE Name = N'Đường')
    INSERT INTO catalog.ModifierGroup(Name, IsRequired, MinSelect, MaxSelect) VALUES (N'Đường', 1, 1, 1);
IF NOT EXISTS (SELECT 1 FROM catalog.ModifierGroup WHERE Name = N'Đá')
    INSERT INTO catalog.ModifierGroup(Name, IsRequired, MinSelect, MaxSelect) VALUES (N'Đá', 1, 1, 1);
IF NOT EXISTS (SELECT 1 FROM catalog.ModifierGroup WHERE Name = N'Topping')
    INSERT INTO catalog.ModifierGroup(Name, IsRequired, MinSelect, MaxSelect) VALUES (N'Topping', 0, 0, 3);

DECLARE @gSugar INT = (SELECT TOP(1) ModifierGroupId FROM catalog.ModifierGroup WHERE Name = N'Đường'   ORDER BY ModifierGroupId);
DECLARE @gIce   INT = (SELECT TOP(1) ModifierGroupId FROM catalog.ModifierGroup WHERE Name = N'Đá'      ORDER BY ModifierGroupId);
DECLARE @gTop   INT = (SELECT TOP(1) ModifierGroupId FROM catalog.ModifierGroup WHERE Name = N'Topping' ORDER BY ModifierGroupId);

DECLARE @opt TABLE (Gid INT, Name NVARCHAR(80), Delta DECIMAL(12,2));
INSERT INTO @opt(Gid, Name, Delta) VALUES
 (@gSugar, N'Không đường', 0), (@gSugar, N'Ít đường', 0), (@gSugar, N'Bình thường', 0), (@gSugar, N'Nhiều đường', 0),
 (@gIce,   N'Không đá',    0), (@gIce,   N'Ít đá',    0), (@gIce,   N'Bình thường', 0), (@gIce,   N'Nhiều đá',    0),
 (@gTop,   N'Thêm shot', 8000), (@gTop,  N'Trân châu', 7000), (@gTop, N'Kem cheese', 10000);

INSERT INTO catalog.ModifierOption(ModifierGroupId, Name, PriceDelta)
SELECT o.Gid, o.Name, o.Delta FROM @opt o
WHERE NOT EXISTS (SELECT 1 FROM catalog.ModifierOption mo
                  WHERE mo.ModifierGroupId = o.Gid AND mo.Name = o.Name);

-- Nhóm dùng chung áp cho mọi món đang bán.
INSERT INTO catalog.ProductModifierGroup(ProductId, ModifierGroupId)
SELECT p.ProductId, g.Gid
FROM catalog.Product p
CROSS JOIN (VALUES (@gSugar), (@gIce), (@gTop)) g(Gid)
WHERE p.IsActive = 1
  AND NOT EXISTS (SELECT 1 FROM catalog.ProductModifierGroup x
                  WHERE x.ProductId = p.ProductId AND x.ModifierGroupId = g.Gid);

-- Size: mỗi món một nhóm riêng (đúng cách database.sql làm) để impact nguyên
-- liệu của size không lẫn giữa các món.
DECLARE @pid INT, @gid INT, @mainIng INT, @mainQty DECIMAL(12,3);
DECLARE cP CURSOR LOCAL FAST_FORWARD FOR
    SELECT p.ProductId FROM catalog.Product p
    WHERE p.IsActive = 1
      AND NOT EXISTS (SELECT 1 FROM catalog.ProductModifierGroup pmg
                      JOIN catalog.ModifierGroup mg ON mg.ModifierGroupId = pmg.ModifierGroupId
                      WHERE pmg.ProductId = p.ProductId AND mg.Name = N'Size');
OPEN cP; FETCH NEXT FROM cP INTO @pid;
WHILE @@FETCH_STATUS = 0
BEGIN
    INSERT INTO catalog.ModifierGroup(Name, IsRequired, MinSelect, MaxSelect) VALUES (N'Size', 1, 1, 1);
    SET @gid = SCOPE_IDENTITY();
    INSERT INTO catalog.ModifierOption(ModifierGroupId, Name, PriceDelta) VALUES
        (@gid, N'Size S', 0), (@gid, N'Size M', 6000), (@gid, N'Size L', 10000);
    INSERT INTO catalog.ProductModifierGroup(ProductId, ModifierGroupId) VALUES (@pid, @gid);

    -- Size M/L tốn thêm nguyên liệu chính: +20% / +40% (auto-deduct lúc bấm Xong).
    SET @mainIng = NULL; SET @mainQty = NULL;
    SELECT TOP(1) @mainIng = pr.IngredientId, @mainQty = pr.Quantity
    FROM catalog.ProductRecipe pr WHERE pr.ProductId = @pid ORDER BY pr.Quantity DESC;
    IF @mainIng IS NOT NULL
        INSERT INTO catalog.ModifierIngredientImpact(ModifierOptionId, IngredientId, QtyDelta)
        SELECT mo.ModifierOptionId, @mainIng,
               CASE mo.Name WHEN N'Size M' THEN ROUND(@mainQty * 0.2, 1) ELSE ROUND(@mainQty * 0.4, 1) END
        FROM catalog.ModifierOption mo
        WHERE mo.ModifierGroupId = @gid AND mo.Name IN (N'Size M', N'Size L');

    FETCH NEXT FROM cP INTO @pid;
END
CLOSE cP; DEALLOCATE cP;

-- Impact của đường/đá/topping (bỏ qua nguyên liệu chưa khai trong Ingredient).
INSERT INTO catalog.ModifierIngredientImpact(ModifierOptionId, IngredientId, QtyDelta)
SELECT mo.ModifierOptionId, i.IngredientId, v.Qty
FROM (VALUES (N'Không đường', N'Đường',     CAST(-5   AS DECIMAL(12,3))),
             (N'Ít đường',    N'Đường',     CAST(-2.5 AS DECIMAL(12,3))),
             (N'Nhiều đường', N'Đường',     CAST( 5   AS DECIMAL(12,3))),
             (N'Không đá',    N'Đá',        CAST(-100 AS DECIMAL(12,3))),
             (N'Ít đá',       N'Đá',        CAST(-50  AS DECIMAL(12,3))),
             (N'Nhiều đá',    N'Đá',        CAST( 50  AS DECIMAL(12,3))),
             (N'Thêm shot',   N'Cà phê hạt',CAST( 18  AS DECIMAL(12,3))),
             (N'Trân châu',   N'Trân châu', CAST( 50  AS DECIMAL(12,3))),
             (N'Kem cheese',  N'Kem cheese',CAST( 30  AS DECIMAL(12,3)))) v(Opt, Ing, Qty)
JOIN catalog.ModifierOption mo ON mo.Name = v.Opt AND mo.ModifierGroupId IN (@gSugar, @gIce, @gTop)
JOIN catalog.Ingredient i      ON i.Name = v.Ing
WHERE NOT EXISTS (SELECT 1 FROM catalog.ModifierIngredientImpact x
                  WHERE x.ModifierOptionId = mo.ModifierOptionId AND x.IngredientId = i.IngredientId);

/* ---------------------------------------------------------------------------
   3) CHỌN MÓN & BÀN
   Lấy từ menu chi nhánh và chỉ lấy món CÓ nhóm Size, để chip size chắc chắn
   hiện. Ưu tiên 3 món của seed gốc, thiếu thì lấy món khác cùng chi nhánh.
   --------------------------------------------------------------------------- */
DECLARE @P TABLE (Seq INT IDENTITY(1,1), ProductId INT, Price DECIMAL(12,2));
INSERT INTO @P(ProductId, Price)
SELECT TOP(3) p.ProductId, COALESCE(bm.LocalPrice, p.BasePrice)
FROM catalog.BranchMenu bm
JOIN catalog.Product p ON p.ProductId = bm.ProductId AND p.IsActive = 1
WHERE bm.BranchId = @b AND bm.IsAvailable = 1 AND bm.Is86 = 0
  AND EXISTS (SELECT 1 FROM catalog.ProductModifierGroup pmg
              JOIN catalog.ModifierGroup mg ON mg.ModifierGroupId = pmg.ModifierGroupId
              WHERE pmg.ProductId = p.ProductId AND mg.Name = N'Size')
ORDER BY CASE p.Name WHEN N'Cà phê sữa' THEN 0 WHEN N'Cold Brew' THEN 1
                     WHEN N'Trà Đào'    THEN 2 ELSE 9 END, p.ProductId;

DECLARE @pA INT = (SELECT ProductId FROM @P WHERE Seq = 1);
DECLARE @pB INT = (SELECT ProductId FROM @P WHERE Seq = 2);
DECLARE @pC INT = (SELECT ProductId FROM @P WHERE Seq = 3);
DECLARE @prA DECIMAL(12,2) = (SELECT Price FROM @P WHERE Seq = 1);
DECLARE @prB DECIMAL(12,2) = (SELECT Price FROM @P WHERE Seq = 2);
DECLARE @prC DECIMAL(12,2) = (SELECT Price FROM @P WHERE Seq = 3);
IF @pA IS NULL THROW 50003, N'Menu chi nhánh chưa có món nào gắn nhóm Size.', 1;
SELECT @pB = ISNULL(@pB, @pA), @prB = ISNULL(@prB, @prA),
       @pC = ISNULL(@pC, @pA), @prC = ISNULL(@prC, @prA);

-- Ưu tiên bàn chưa có phiên OPEN để không đụng đơn đang chạy của seed gốc.
DECLARE @t1 INT = (SELECT TOP(1) dt.DiningTableId FROM sales.DiningTable dt
                   WHERE dt.BranchId = @b
                     AND NOT EXISTS (SELECT 1 FROM sales.TableSession ts
                                     WHERE ts.DiningTableId = dt.DiningTableId AND ts.Status = 'OPEN')
                   ORDER BY dt.DiningTableId);
DECLARE @t2 INT = (SELECT TOP(1) dt.DiningTableId FROM sales.DiningTable dt
                   WHERE dt.BranchId = @b AND dt.DiningTableId <> ISNULL(@t1, 0)
                     AND NOT EXISTS (SELECT 1 FROM sales.TableSession ts
                                     WHERE ts.DiningTableId = dt.DiningTableId AND ts.Status = 'OPEN')
                   ORDER BY dt.DiningTableId);
SELECT @t1 = ISNULL(@t1, (SELECT TOP(1) DiningTableId FROM sales.DiningTable WHERE BranchId = @b ORDER BY DiningTableId)),
       @t2 = ISNULL(@t2, (SELECT TOP(1) DiningTableId FROM sales.DiningTable WHERE BranchId = @b ORDER BY DiningTableId DESC));
IF @t1 IS NULL THROW 50004, N'Chi nhánh chưa có bàn nào.', 1;

DECLARE @s INT, @o INT, @i INT;

/* ---------------------------------------------------------------------------
   ZT1 · Đơn TẠI BÀN — nhiều chip vs không chip nào
   --------------------------------------------------------------------------- */
INSERT INTO sales.TableSession(BranchId, DiningTableId, OpenedBy, OpenedAt, Status)
VALUES (@b, @t1, @cas, DATEADD(MINUTE, -9, @now), 'OPEN');
SET @s = SCOPE_IDENTITY();

INSERT INTO sales.Orders(BranchId, TableSessionId, Source, OrderType, Status, CreatedBy, PickupCode, CreatedAt)
VALUES (@b, @s, 'COUNTER', 'DINE_IN', 'ACTIVE', @cas, 'ZT1', DATEADD(MINUTE, -9, @now));
SET @o = SCOPE_IDENTITY();

-- Món 1: size + đường + topping + ghi chú dài (test xuống dòng của dải chip).
INSERT INTO sales.OrderItem(OrderId, ProductId, Quantity, UnitPrice, Note, Status)
VALUES (@o, @pA, 2, @prA,
        N'Khách dị ứng sữa bò — pha loãng, ít bọt, mang ra cùng lúc với món còn lại của bàn.',
        'WAITING');
SET @i = SCOPE_IDENTITY();
INSERT INTO sales.OrderItemModifier(OrderItemId, ModifierOptionId, PriceDelta)
SELECT @i, mo.ModifierOptionId, mo.PriceDelta
FROM catalog.ProductModifierGroup pmg
JOIN catalog.ModifierGroup  mg ON mg.ModifierGroupId = pmg.ModifierGroupId
JOIN catalog.ModifierOption mo ON mo.ModifierGroupId = pmg.ModifierGroupId AND mo.IsActive = 1
WHERE pmg.ProductId = @pA AND mo.Name IN (N'Size L', N'Ít đường', N'Trân châu')
-- Thứ tự chip trên KDS = thứ tự dòng OrderItemModifier. POS/QR đẩy option theo
-- thứ tự nhóm của ProductModifierGroupDao (Size → Đường → Đá → Topping) nên seed
-- phải chèn cùng thứ tự đó, không thì demo xếp chip khác đơn thật.
ORDER BY CASE mg.Name WHEN N'Size' THEN 1 WHEN N'Đường' THEN 2 WHEN N'Đá' THEN 3
                      WHEN N'Topping' THEN 4 ELSE 5 END, mo.ModifierOptionId;

-- Món 2 (đối chứng): không modifier, không ghi chú → cả dải chip phải biến mất.
INSERT INTO sales.OrderItem(OrderId, ProductId, Quantity, UnitPrice, Status)
VALUES (@o, @pB, 1, @prB, 'WAITING');

/* ---------------------------------------------------------------------------
   ZT2 · Đơn MANG ĐI — không có bàn: cột Bàn hiện nhãn loại đơn
   --------------------------------------------------------------------------- */
INSERT INTO sales.Orders(BranchId, TableSessionId, Source, OrderType, Status, CreatedBy, PickupCode, CreatedAt)
VALUES (@b, NULL, 'COUNTER', 'TAKEAWAY', 'ACTIVE', @cas, 'ZT2', DATEADD(MINUTE, -7, @now));
SET @o = SCOPE_IDENTITY();

-- Size S có PriceDelta = 0 — kiểm tra chip vẫn hiện dù option không cộng tiền.
INSERT INTO sales.OrderItem(OrderId, ProductId, Quantity, UnitPrice, Status)
VALUES (@o, @pB, 1, @prB, 'WAITING');
SET @i = SCOPE_IDENTITY();
INSERT INTO sales.OrderItemModifier(OrderItemId, ModifierOptionId, PriceDelta)
SELECT @i, mo.ModifierOptionId, mo.PriceDelta
FROM catalog.ProductModifierGroup pmg
JOIN catalog.ModifierGroup  mg ON mg.ModifierGroupId = pmg.ModifierGroupId
JOIN catalog.ModifierOption mo ON mo.ModifierGroupId = pmg.ModifierGroupId AND mo.IsActive = 1
WHERE pmg.ProductId = @pB AND mo.Name IN (N'Size S', N'Ít đá')
ORDER BY CASE mg.Name WHEN N'Size' THEN 1 WHEN N'Đường' THEN 2 WHEN N'Đá' THEN 3
                      WHEN N'Topping' THEN 4 ELSE 5 END, mo.ModifierOptionId;

/* ---------------------------------------------------------------------------
   ZT3 · Đơn QR — ĐANG PHA (6 chip) + ĐÃ PHA XONG (có nơi đặt)
   --------------------------------------------------------------------------- */
INSERT INTO sales.TableSession(BranchId, DiningTableId, OpenedBy, OpenedAt, Status)
VALUES (@b, @t2, NULL, DATEADD(MINUTE, -6, @now), 'OPEN');
SET @s = SCOPE_IDENTITY();

INSERT INTO sales.Orders(BranchId, TableSessionId, Source, OrderType, Status, CreatedBy, PickupCode, CreatedAt)
VALUES (@b, @s, 'QR', 'DINE_IN', 'ACTIVE', NULL, 'ZT3', DATEADD(MINUTE, -6, @now));
SET @o = SCOPE_IDENTITY();

-- Đang pha bởi barista đầu tiên của chi nhánh → đăng nhập tài khoản đó sẽ thấy
-- nút "Xong"/menu ⋯; barista khác chỉ thấy dòng "Đang pha: <tên>".
INSERT INTO sales.OrderItem(OrderId, ProductId, Quantity, UnitPrice, Note, Status, StartedAt, BaristaId)
VALUES (@o, @pA, 1, @prA, N'Ly cho khách VIP, kiểm tra kỹ trước khi đưa ra.',
        'MAKING', DATEADD(MINUTE, -4, @now), @bar);
SET @i = SCOPE_IDENTITY();
INSERT INTO sales.OrderItemModifier(OrderItemId, ModifierOptionId, PriceDelta)
SELECT @i, mo.ModifierOptionId, mo.PriceDelta
FROM catalog.ProductModifierGroup pmg
JOIN catalog.ModifierGroup  mg ON mg.ModifierGroupId = pmg.ModifierGroupId
JOIN catalog.ModifierOption mo ON mo.ModifierGroupId = pmg.ModifierGroupId AND mo.IsActive = 1
WHERE pmg.ProductId = @pA
  AND mo.Name IN (N'Size M', N'Nhiều đường', N'Ít đá', N'Thêm shot', N'Kem cheese', N'Trân châu')
ORDER BY CASE mg.Name WHEN N'Size' THEN 1 WHEN N'Đường' THEN 2 WHEN N'Đá' THEN 3
                      WHEN N'Topping' THEN 4 ELSE 5 END, mo.ModifierOptionId;

-- Đã pha xong: dòng làm mờ, thay số thứ tự bằng ✓, hiện người pha + nơi đặt.
INSERT INTO sales.OrderItem(OrderId, ProductId, Quantity, UnitPrice, Note, Status,
                            StartedAt, DoneAt, BaristaId, PreparedBy, HandoverLocation)
VALUES (@o, @pC, 3, @prC, N'Giao kèm ống hút giấy.', 'READY',
        DATEADD(MINUTE, -5, @now), DATEADD(MINUTE, -2, @now), @bar, @bar, N'Bar trái');
SET @i = SCOPE_IDENTITY();
INSERT INTO sales.OrderItemModifier(OrderItemId, ModifierOptionId, PriceDelta)
SELECT @i, mo.ModifierOptionId, mo.PriceDelta
FROM catalog.ProductModifierGroup pmg
JOIN catalog.ModifierGroup  mg ON mg.ModifierGroupId = pmg.ModifierGroupId
JOIN catalog.ModifierOption mo ON mo.ModifierGroupId = pmg.ModifierGroupId AND mo.IsActive = 1
WHERE pmg.ProductId = @pC AND mo.Name IN (N'Size L', N'Nhiều đá')
ORDER BY CASE mg.Name WHEN N'Size' THEN 1 WHEN N'Đường' THEN 2 WHEN N'Đá' THEN 3
                      WHEN N'Topping' THEN 4 ELSE 5 END, mo.ModifierOptionId;

/* ---------------------------------------------------------------------------
   ZT4 · Đơn GIAO HÀNG — món BỊ CHẶN (chip sự cố) + món LÀM LẠI (lên đầu)
   --------------------------------------------------------------------------- */
INSERT INTO sales.Orders(BranchId, TableSessionId, Source, OrderType, Status, CreatedBy, PickupCode, CreatedAt)
VALUES (@b, NULL, 'COUNTER', 'DELIVERY', 'ACTIVE', @cas, 'ZT4', DATEADD(MINUTE, -12, @now));
SET @o = SCOPE_IDENTITY();

INSERT INTO sales.OrderItem(OrderId, ProductId, Quantity, UnitPrice, Status,
                            HasIssue, IssueReason, IssueReportedBy, IssueReportedAt)
VALUES (@o, @pC, 1, @prC, 'BLOCKED', 1, N'Hết nguyên liệu: syrup đào — chờ pha mẻ mới',
        @bar, DATEADD(MINUTE, -5, @now));
SET @i = SCOPE_IDENTITY();
INSERT INTO sales.OrderItemModifier(OrderItemId, ModifierOptionId, PriceDelta)
SELECT @i, mo.ModifierOptionId, mo.PriceDelta
FROM catalog.ProductModifierGroup pmg
JOIN catalog.ModifierGroup  mg ON mg.ModifierGroupId = pmg.ModifierGroupId
JOIN catalog.ModifierOption mo ON mo.ModifierGroupId = pmg.ModifierGroupId AND mo.IsActive = 1
WHERE pmg.ProductId = @pC AND mo.Name IN (N'Size L', N'Nhiều đá')
ORDER BY CASE mg.Name WHEN N'Size' THEN 1 WHEN N'Đường' THEN 2 WHEN N'Đá' THEN 3
                      WHEN N'Topping' THEN 4 ELSE 5 END, mo.ModifierOptionId;

-- RemakeCount > 0 → dòng này đứng ĐẦU danh sách và có chip "⟳ Làm lại · lần 1".
INSERT INTO sales.OrderItem(OrderId, ProductId, Quantity, UnitPrice, Note, Status, Priority, RemakeCount)
VALUES (@o, @pA, 1, @prA, N'Lần trước pha nhạt, khách trả lại.', 'WAITING', 10, 1);
SET @i = SCOPE_IDENTITY();
INSERT INTO sales.OrderItemModifier(OrderItemId, ModifierOptionId, PriceDelta)
SELECT @i, mo.ModifierOptionId, mo.PriceDelta
FROM catalog.ProductModifierGroup pmg
JOIN catalog.ModifierGroup  mg ON mg.ModifierGroupId = pmg.ModifierGroupId
JOIN catalog.ModifierOption mo ON mo.ModifierGroupId = pmg.ModifierGroupId AND mo.IsActive = 1
WHERE pmg.ProductId = @pA AND mo.Name IN (N'Size L', N'Thêm shot')
ORDER BY CASE mg.Name WHEN N'Size' THEN 1 WHEN N'Đường' THEN 2 WHEN N'Đá' THEN 3
                      WHEN N'Topping' THEN 4 ELSE 5 END, mo.ModifierOptionId;

/* ---------------------------------------------------------------------------
   4) CHỐT SỔ — giá dòng đơn cộng PriceDelta, bàn chuyển sang OCCUPIED
   (app tính UnitPrice = giá menu + tổng delta ngay lúc tạo đơn)
   --------------------------------------------------------------------------- */
UPDATE oi SET UnitPrice = oi.UnitPrice + ISNULL(m.Delta, 0)
FROM sales.OrderItem oi
JOIN sales.Orders o ON o.OrderId = oi.OrderId
OUTER APPLY (SELECT SUM(x.PriceDelta) AS Delta FROM sales.OrderItemModifier x
             WHERE x.OrderItemId = oi.OrderItemId) m
WHERE o.BranchId = @b AND o.PickupCode LIKE 'ZT%';

UPDATE sales.DiningTable SET Status = 'OCCUPIED' WHERE DiningTableId IN (@t1, @t2);

COMMIT;

/* ---------------------------------------------------------------------------
   5) KIỂM CHỨNG — đúng những gì màn KDS sẽ hiện (STRING_AGG cần SQL Server 2017+)
   --------------------------------------------------------------------------- */
SELECT o.PickupCode                          AS [Mã đơn],
       ISNULL(dt.TableNumber, o.OrderType)   AS [Bàn / Loại đơn],
       CAST(oi.Quantity AS VARCHAR(4)) + N'× ' + p.Name AS [Món],
       oi.Status                             AS [Trạng thái],
       STRING_AGG(mo.Name, N' · ') WITHIN GROUP (ORDER BY oim.OrderItemModifierId) AS [Chip modifier],
       oi.Note                               AS [Ghi chú]
FROM sales.Orders o
JOIN sales.OrderItem oi ON oi.OrderId = o.OrderId
JOIN catalog.Product p  ON p.ProductId = oi.ProductId
LEFT JOIN sales.TableSession ts ON ts.TableSessionId = o.TableSessionId
LEFT JOIN sales.DiningTable dt  ON dt.DiningTableId = ts.DiningTableId
LEFT JOIN sales.OrderItemModifier oim ON oim.OrderItemId = oi.OrderItemId
LEFT JOIN catalog.ModifierOption mo   ON mo.ModifierOptionId = oim.ModifierOptionId
WHERE o.BranchId = @b AND o.PickupCode LIKE 'ZT%' AND o.Status = 'ACTIVE'
GROUP BY o.PickupCode, dt.TableNumber, o.OrderType, oi.Quantity, p.Name, oi.Status, oi.Note, oi.OrderItemId
ORDER BY o.PickupCode, oi.OrderItemId;
