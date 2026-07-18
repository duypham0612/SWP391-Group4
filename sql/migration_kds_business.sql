USE CafeChain;
GO

/* ===========================================================================
   Migration đợt 2 — nghiệp vụ KDS: thời gian pha chuẩn theo món, mã gọi món,
   ngưỡng cao điểm theo chi nhánh. Chạy tay, idempotent, an toàn chạy lại.
   HandoverLocation đã có sẵn (migration_barista_workbench.sql) — không thêm.
   =========================================================================== */

-- Thời gian pha chuẩn từng món (giây). DEFAULT 720 = 12 phút: giữ nguyên hành vi
-- SLA cũ cho mọi món hiện có cho tới khi Admin nhập số thật.
IF COL_LENGTH('catalog.Product','PrepSeconds') IS NULL
    ALTER TABLE catalog.Product ADD PrepSeconds INT NOT NULL
        CONSTRAINT DF_Product_PrepSeconds DEFAULT 720;
GO

-- Mã gọi món cấp cho mỗi đơn lúc tạo (vd D12/T07/G03) — dùng ở KDS, bàn giao,
-- màn khách QR để khớp ly với đơn. NULL cho đơn cũ (JSP kiểm not empty).
IF COL_LENGTH('sales.Orders','PickupCode') IS NULL
    ALTER TABLE sales.Orders ADD PickupCode VARCHAR(8) NULL;
GO

-- Ngưỡng cao điểm theo chi nhánh (số ly đang chờ+đang pha). 0 = dùng mặc định
-- toàn hệ (Constants.PEAK_THRESHOLD_CUPS). Manager chỉnh ở màn Cài đặt chi nhánh.
IF COL_LENGTH('org.Branch','PeakThresholdCups') IS NULL
    ALTER TABLE org.Branch ADD PeakThresholdCups INT NOT NULL
        CONSTRAINT DF_Branch_PeakCups DEFAULT 0;
GO
