/* ===========================================================================
   fixture_demo_hoi_dong.sql — Dữ liệu trình diễn cho buổi bảo vệ đồ án.

   DỰNG LẠI TỪ ĐẦU TRÊN MÁY MỚI (4 bước — dữ liệu cũ sẽ mất, sao lưu trước nếu cần):

     1) Tạo bản database.sql có bật seed (KHÔNG sửa file gốc, deploy luôn cần @SeedDemo = 0):
          sed 's/DECLARE @SeedDemo BIT = 0;/DECLARE @SeedDemo BIT = 1;/' \
              sql/database.sql > /tmp/database-seed-demo.sql
     2) Dừng Tomcat để nhả kết nối, rồi xoá database cũ:
          sqlcmd -S <host> -U sa -P '<pass>' -N disable -Q \
            "ALTER DATABASE CafeChain SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE CafeChain;"
     3) Dựng schema + dữ liệu seed (script tự CREATE DATABASE và USE):
          sqlcmd -S <host> -U sa -P '<pass>' -N disable -i /tmp/database-seed-demo.sql
     4) Chạy file này để dọn main flow và bơm dữ liệu trình diễn:
          sqlcmd -S <host> -U sa -P '<pass>' -N disable -d CafeChain -i sql/fixture_demo_hoi_dong.sql

   Chạy SAU sql/database.sql (bản đã bật @SeedDemo = 1).
   IDEMPOTENT: chạy lại nhiều lần không nhân bản — mọi bản ghi do file này sinh ra
   đều mang tiền tố 'HĐ:' và được kiểm tra trước khi chèn.
   (Tránh ngoặc vuông trong nhãn — LIKE của T-SQL coi [..] là lớp ký tự.)
   KHÔNG đổi schema. KHÔNG xoá dữ liệu lịch sử (hoá đơn, chấm công, lương giữ nguyên).

   Ba việc:
     1) Dọn MAIN FLOW về trạng thái sạch — bàn trống, KDS rỗng — để demo trực tiếp
        từ đầu. GIỮ NGUYÊN ca thu ngân đang mở.
     2) Bơm dữ liệu vận hành cho các màn đang mỏng: nhà cung cấp, phiếu nhập kho,
        mẻ pha sẵn, hao hụt — phân bổ đều cho cả 3 chi nhánh.
     3) Chốt lại tồn kho = Σ sổ cái, vì InventoryTransaction là nguồn sự thật duy
        nhất còn BranchInventory chỉ là số dư cache.
   =========================================================================== */
USE CafeChain;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

/* ---------------------------------------------------------------------------
   1) DỌN MAIN FLOW
   Seed sinh sẵn đơn đang chờ + bàn đang phục vụ để test. Khi demo trực tiếp thì
   đó là nhiễu: đơn mới tạo trên sân khấu sẽ lẫn giữa hàng chục món cũ trong KDS.
   Huỷ chứ không xoá — sales.Orders/OrderItem có FK ON DELETE CASCADE và app không
   bao giờ xoá đơn; huỷ là đủ vì KDS lọc theo Status.
   --------------------------------------------------------------------------- */
PRINT N'[1] Dọn main flow...';

UPDATE oi SET oi.Status = 'CANCELLED'
FROM sales.OrderItem oi
JOIN sales.Orders o ON o.OrderId = oi.OrderId
WHERE o.Status = 'ACTIVE'
  AND oi.Status IN ('WAITING','MAKING','READY','BLOCKED','REMAKE');

UPDATE sales.Orders SET Status = 'CANCELLED' WHERE Status = 'ACTIVE';

UPDATE sales.TableSession
   SET Status = 'CLOSED', ClosedAt = SYSUTCDATETIME()
 WHERE Status = 'OPEN';

UPDATE sales.DiningTable SET Status = 'EMPTY' WHERE Status <> 'EMPTY';

-- Tín hiệu khách (gọi NV / xin thanh toán / xin mở bàn) còn treo từ lần test trước.
UPDATE ops.OutboxEvent SET ProcessedAt = SYSUTCDATETIME()
 WHERE ProcessedAt IS NULL
   AND EventType IN ('service.call','bill.requested','table.open_requested');
GO

/* ---------------------------------------------------------------------------
   2) NHÀ CUNG CẤP — thêm cho danh sách đủ dày để lọc/tìm kiếm có ý nghĩa.
   --------------------------------------------------------------------------- */
PRINT N'[2] Nhà cung cấp...';

MERGE inventory.Supplier AS t
USING (VALUES
    (N'Công ty Sữa Việt Nam — CN Miền Nam', '02838445678', N'10 Tân Trào, Quận 7, TP.HCM'),
    (N'HTX Chè Tân Cương Thái Nguyên',      '02083825417', N'Xóm Hồng Thái, Tân Cương, Thái Nguyên'),
    (N'Công ty CP Đường Biên Hoà',          '02513836199', N'KCN Biên Hoà 1, Đồng Nai'),
    (N'Nhà máy Nước đá Tinh khiết An Phú',  '02839114455', N'25 An Phú, TP. Thủ Đức, TP.HCM')
) AS s(Name, Phone, Address)
ON t.Name = s.Name
WHEN NOT MATCHED THEN
    INSERT (Name, Phone, Address, IsActive) VALUES (s.Name, s.Phone, s.Address, 1);
GO

/* ---------------------------------------------------------------------------
   3) PHIẾU NHẬP KHO ĐỊNH KỲ
   Mỗi chi nhánh: 4 phiếu CONFIRMED rải trong 3 tuần + 1 phiếu DRAFT đang chờ
   duyệt, để màn "Phiếu nhập" có đủ trạng thái chứ không phải một dòng trống trơn.
   Mỗi phiếu ghi kèm InventoryTransaction 'RECEIPT' — nếu chỉ chèn phiếu mà quên
   sổ cái thì tồn kho hiển thị sẽ lệch với chứng từ, và đó đúng là thứ hội đồng soi.
   --------------------------------------------------------------------------- */
PRINT N'[3] Phiếu nhập kho...';

IF NOT EXISTS (SELECT 1 FROM inventory.StockReceipt WHERE Note LIKE N'HĐ:%')
BEGIN
    DECLARE @now DATETIME2 = SYSUTCDATETIME();
    DECLARE @b INT, @mgr INT, @sup INT, @rid INT, @dayBack INT, @seq INT;

    DECLARE cB CURSOR LOCAL FAST_FORWARD FOR
        SELECT b.BranchId,
               (SELECT TOP (1) u.UserId FROM iam.[User] u
                  JOIN iam.Role r ON r.RoleId = u.RoleId
                 WHERE u.BranchId = b.BranchId AND r.Code = 'BRANCH_MANAGER'
                 ORDER BY u.UserId)
          FROM org.Branch b
         WHERE b.IsActive = 1;

    OPEN cB;
    FETCH NEXT FROM cB INTO @b, @mgr;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        IF @mgr IS NOT NULL
        BEGIN
            SET @seq = 0;
            -- 4 đợt nhập: cách nhau khoảng một tuần.
            DECLARE cD CURSOR LOCAL FAST_FORWARD FOR
                SELECT v FROM (VALUES (23),(16),(9),(3)) AS d(v);
            OPEN cD;
            FETCH NEXT FROM cD INTO @dayBack;
            WHILE @@FETCH_STATUS = 0
            BEGIN
                SET @seq += 1;
                -- Xoay vòng nhà cung cấp để chứng từ không bị một mối duy nhất.
                SET @sup = (SELECT TOP (1) SupplierId FROM inventory.Supplier
                             WHERE IsActive = 1
                             ORDER BY (SupplierId + @b + @seq) % 7, SupplierId);

                INSERT INTO inventory.StockReceipt(BranchId, SupplierId, ReceivedBy, Status, TotalCost, Note, ReceiptDate)
                VALUES (@b, @sup, @mgr, 'CONFIRMED', 0,
                        N'HĐ: Nhập hàng định kỳ đợt ' + CAST(@seq AS NVARCHAR(2)),
                        DATEADD(DAY, -@dayBack, @now));
                SET @rid = SCOPE_IDENTITY();

                -- Nhập nguyên liệu thô, lượng theo mức tiêu thụ thực tế của quán.
                INSERT INTO inventory.StockReceiptDetail(StockReceiptId, IngredientId, Quantity, UnitCost, Unit)
                SELECT @rid, i.IngredientId,
                       CASE i.Name WHEN N'Cà phê hạt'  THEN 5000
                                   WHEN N'Sữa tươi'    THEN 12000
                                   WHEN N'Sữa đặc'     THEN 4000
                                   WHEN N'Đường'       THEN 6000
                                   WHEN N'Đá'          THEN 25000
                                   WHEN N'Trà đen'     THEN 1500
                                   WHEN N'Trà sen'     THEN 1200
                                   WHEN N'Đào ngâm'    THEN 3000
                                   WHEN N'Vải ngâm'    THEN 2500
                                   ELSE 800 END,
                       CASE i.Name WHEN N'Cà phê hạt'  THEN 0.32
                                   WHEN N'Sữa tươi'    THEN 0.025
                                   WHEN N'Sữa đặc'     THEN 0.045
                                   WHEN N'Đường'       THEN 0.020
                                   WHEN N'Đá'          THEN 0.002
                                   ELSE 0.060 END,
                       i.Unit
                  FROM catalog.Ingredient i
                 WHERE i.IngredientType = 'RAW' AND i.IsActive = 1;

                INSERT INTO inventory.InventoryTransaction(BranchId, IngredientId, ChangeQty, TxnType, RefTable, RefId, CreatedBy, CreatedAt)
                SELECT @b, d.IngredientId, d.Quantity, 'RECEIPT', 'StockReceipt', @rid, @mgr,
                       DATEADD(DAY, -@dayBack, @now)
                  FROM inventory.StockReceiptDetail d
                 WHERE d.StockReceiptId = @rid;

                UPDATE inventory.StockReceipt
                   SET TotalCost = (SELECT SUM(d.Quantity * d.UnitCost)
                                      FROM inventory.StockReceiptDetail d
                                     WHERE d.StockReceiptId = @rid)
                 WHERE StockReceiptId = @rid;

                FETCH NEXT FROM cD INTO @dayBack;
            END
            CLOSE cD; DEALLOCATE cD;

            -- Phiếu DRAFT: hàng đã về, quản lý chưa xác nhận → CHƯA ghi sổ cái.
            INSERT INTO inventory.StockReceipt(BranchId, SupplierId, ReceivedBy, Status, TotalCost, Note, ReceiptDate)
            VALUES (@b, @sup, @mgr, 'DRAFT', 1600,
                    N'HĐ: Phiếu nháp — chờ đối chiếu hoá đơn nhà cung cấp',
                    DATEADD(DAY, -1, @now));
            SET @rid = SCOPE_IDENTITY();
            INSERT INTO inventory.StockReceiptDetail(StockReceiptId, IngredientId, Quantity, UnitCost, Unit)
            SELECT @rid, i.IngredientId, 5000, 0.32, i.Unit
              FROM catalog.Ingredient i WHERE i.Name = N'Cà phê hạt';
        END

        FETCH NEXT FROM cB INTO @b, @mgr;
    END
    CLOSE cB; DEALLOCATE cB;
END
GO

/* ---------------------------------------------------------------------------
   4) MẺ PHA SẴN GẦN ĐÂY
   Màn Prep của barista chỉ có ý nghĩa khi có mẻ còn hạn. Ghi PREP_OUT (trừ thô)
   + PREP_IN (cộng pha sẵn) để không bị trừ thô hai lần lúc bán.
   --------------------------------------------------------------------------- */
PRINT N'[4] Mẻ pha sẵn...';

IF NOT EXISTS (SELECT 1 FROM inventory.PrepBatch pb
                WHERE pb.MadeAt >= DATEADD(DAY, -3, SYSUTCDATETIME())
                  AND pb.QuantityProduced = 12000)
BEGIN
    DECLARE @now2 DATETIME2 = SYSUTCDATETIME();
    DECLARE @bp INT, @bar INT, @pb INT;
    DECLARE @iCafe  INT = (SELECT IngredientId FROM catalog.Ingredient WHERE Name = N'Cà phê hạt');
    DECLARE @iCold  INT = (SELECT IngredientId FROM catalog.Ingredient WHERE Name = N'Cold Brew');

    DECLARE cP CURSOR LOCAL FAST_FORWARD FOR
        SELECT b.BranchId,
               (SELECT TOP (1) u.UserId FROM iam.[User] u
                  JOIN iam.Role r ON r.RoleId = u.RoleId
                 WHERE u.BranchId = b.BranchId AND r.Code = 'BARISTA'
                 ORDER BY u.UserId)
          FROM org.Branch b WHERE b.IsActive = 1;

    OPEN cP;
    FETCH NEXT FROM cP INTO @bp, @bar;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        IF @bar IS NOT NULL AND @iCold IS NOT NULL AND @iCafe IS NOT NULL
        BEGIN
            INSERT INTO inventory.PrepBatch(BranchId, PreppedIngredientId, QuantityProduced, MadeBy, MadeAt, ExpiresAt, Status)
            VALUES (@bp, @iCold, 12000, @bar, DATEADD(HOUR, -20, @now2), DATEADD(DAY, 2, @now2), 'ACTIVE');
            SET @pb = SCOPE_IDENTITY();
            INSERT INTO inventory.InventoryTransaction(BranchId, IngredientId, ChangeQty, TxnType, RefTable, RefId, CreatedBy, CreatedAt)
            VALUES (@bp, @iCafe, -2400, 'PREP_OUT', 'PrepBatch', @pb, @bar, DATEADD(HOUR, -20, @now2)),
                   (@bp, @iCold, 12000, 'PREP_IN',  'PrepBatch', @pb, @bar, DATEADD(HOUR, -20, @now2));
        END
        FETCH NEXT FROM cP INTO @bp, @bar;
    END
    CLOSE cP; DEALLOCATE cP;
END
GO

/* ---------------------------------------------------------------------------
   5) HAO HỤT CHO CN2 / CN3
   Seed chỉ ghi hao hụt cho CN01, nên báo cáo hao hụt của hai chi nhánh kia trống.
   Lý do viết như nhật ký quán thật, không phải "test 1/test 2".
   --------------------------------------------------------------------------- */
PRINT N'[5] Hao hụt CN2/CN3...';

IF NOT EXISTS (SELECT 1 FROM inventory.WasteLog WHERE Reason LIKE N'HĐ:%')
BEGIN
    DECLARE @now3 DATETIME2 = SYSUTCDATETIME();
    INSERT INTO inventory.WasteLog(BranchId, IngredientId, Quantity, WasteType, Reason, LoggedBy, LoggedAt, Status)
    SELECT b.BranchId, i.IngredientId, w.Qty, w.WType,
           N'HĐ: ' + w.Reason,
           (SELECT TOP (1) u.UserId FROM iam.[User] u
              JOIN iam.Role r ON r.RoleId = u.RoleId
             WHERE u.BranchId = b.BranchId AND r.Code = 'BARISTA' ORDER BY u.UserId),
           DATEADD(DAY, -w.DayBack, @now3), 'ACTIVE'
      FROM org.Branch b
      CROSS JOIN (VALUES
            (N'Sữa tươi',   450, 'EXPIRED', N'Sữa mở hộp quá 24 giờ, huỷ theo quy định vệ sinh',  1),
            (N'Sữa tươi',   300, 'SPILL',   N'Đổ ca sữa khi chuyển bình',                          3),
            (N'Cà phê hạt', 120, 'REMAKE',  N'Pha lại 2 ly latte do khách báo nhạt',               2),
            (N'Đá',        2000, 'EXPIRED', N'Máy đá hỏng qua đêm, đá tan phải bỏ',                4),
            (N'Đào ngâm',   250, 'EXPIRED', N'Hộp đào mở quá 3 ngày',                              5),
            (N'Trà đen',     80, 'SPILL',   N'Rơi vãi khi cân định lượng',                         6),
            (N'Sữa đặc',    150, 'OTHER',   N'Lon móp khi nhập, không dùng được',                  7),
            (N'Vải ngâm',   200, 'EXPIRED', N'Quá hạn sử dụng sau khi mở nắp',                     8)
        ) AS w(IngName, Qty, WType, Reason, DayBack)
      JOIN catalog.Ingredient i ON i.Name = w.IngName
     WHERE b.IsActive = 1
       AND b.BranchId <> (SELECT MIN(BranchId) FROM org.Branch WHERE IsActive = 1);

    -- Sổ cái phải phản ánh hao hụt, nếu không tồn kho sẽ cao hơn thực tế.
    INSERT INTO inventory.InventoryTransaction(BranchId, IngredientId, ChangeQty, TxnType, RefTable, RefId, CreatedBy, CreatedAt)
    SELECT wl.BranchId, wl.IngredientId, -wl.Quantity, 'WASTE', 'WasteLog', wl.WasteLogId, wl.LoggedBy, wl.LoggedAt
      FROM inventory.WasteLog wl
     WHERE wl.Reason LIKE N'HĐ:%';
END
GO

/* ---------------------------------------------------------------------------
   6) CHỐT TỒN KHO = Σ SỔ CÁI
   Bắt buộc chạy cuối: mọi bước trên đều ghi InventoryTransaction, còn
   BranchInventory chỉ là số dư cache. Giữ nguyên quy ước ngưỡng của seed —
   đồ PHA SẴN cố ý để dưới ngưỡng nhằm demo được cảnh báo sắp hết.
   --------------------------------------------------------------------------- */
PRINT N'[6] Chốt tồn kho từ sổ cái...';

DELETE FROM inventory.BranchInventory;
INSERT INTO inventory.BranchInventory(BranchId, IngredientId, QuantityOnHand, MinThreshold, UpdatedAt)
SELECT BranchId, IngredientId, SUM(ChangeQty), 0, SYSUTCDATETIME()
  FROM inventory.InventoryTransaction
 GROUP BY BranchId, IngredientId;

UPDATE inventory.BranchInventory SET MinThreshold = 3000;

UPDATE bi SET MinThreshold = bi.QuantityOnHand + 3000
  FROM inventory.BranchInventory bi
  JOIN catalog.Ingredient i ON i.IngredientId = bi.IngredientId
 WHERE i.IngredientType = 'PREPPED';

UPDATE bi SET PrepTargetQty = bi.QuantityOnHand + 5000
  FROM inventory.BranchInventory bi
  JOIN catalog.Ingredient i ON i.IngredientId = bi.IngredientId
 WHERE i.IngredientType = 'PREPPED';
GO

/* ---------------------------------------------------------------------------
   7) ẢNH MÓN DÙNG FILE TRONG REPO, KHÔNG HOTLINK
   Seed để ImageUrl trỏ thẳng Unsplash. Lúc bảo vệ mà phòng máy mất mạng hoặc wifi
   chậm là 15 ảnh trắng cùng lúc. Ảnh đã tải sẵn vào assets/img/products/p<id>.jpg
   và đi kèm repo, nên trỏ về đường dẫn nội bộ để không phụ thuộc internet.
   JSP tự nối contextPath cho đường dẫn không bắt đầu bằng http.
   --------------------------------------------------------------------------- */
PRINT N'[7] Ảnh món → file nội bộ...';

UPDATE catalog.Product
   SET ImageUrl = '/assets/img/products/p' + CAST(ProductId AS VARCHAR(10)) + '.jpg'
 WHERE ImageUrl LIKE 'http%';
GO

PRINT N'=== XONG. Kiểm tra nhanh: ===';
SELECT N'Bàn chưa trống'     = (SELECT COUNT(*) FROM sales.DiningTable  WHERE Status <> 'EMPTY'),
       N'Phiên còn mở'       = (SELECT COUNT(*) FROM sales.TableSession WHERE Status = 'OPEN'),
       N'Món treo trong KDS' = (SELECT COUNT(*) FROM sales.OrderItem    WHERE Status IN ('WAITING','MAKING','READY')),
       N'Ca thu ngân đang mở'= (SELECT COUNT(*) FROM payment.CashierShift WHERE ClosedAt IS NULL),
       N'Nhà cung cấp'       = (SELECT COUNT(*) FROM inventory.Supplier),
       N'Phiếu nhập'         = (SELECT COUNT(*) FROM inventory.StockReceipt),
       N'Mẻ pha sẵn'         = (SELECT COUNT(*) FROM inventory.PrepBatch),
       N'Dòng hao hụt'       = (SELECT COUNT(*) FROM inventory.WasteLog),
       N'Tồn <= 0'           = (SELECT COUNT(*) FROM inventory.BranchInventory WHERE QuantityOnHand <= 0),
       N'Ảnh còn hotlink'    = (SELECT COUNT(*) FROM catalog.Product WHERE ImageUrl LIKE 'http%');
GO
