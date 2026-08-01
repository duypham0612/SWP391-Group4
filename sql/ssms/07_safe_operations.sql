/*
  Mẫu thao tác thủ công có bảo vệ.

  - Mặc định @ApplyChanges = 0 nên mọi thay đổi sẽ ROLLBACK.
  - Điền khóa chính/username rõ ràng, chạy riêng block cần dùng và kiểm tra result set.
  - Chỉ đổi @ApplyChanges thành 1 sau khi preview đúng.
  - Không dùng file này để sửa tồn kho, đơn hàng đã tạo hay hóa đơn đã thanh toán.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

/* A. Đổi giá/ẩn hiện một sản phẩm trên menu chi nhánh. */
DECLARE @ApplyChanges bit = 0;
DECLARE @MenuBranchId int = 1;
DECLARE @ProductId int = NULL;          -- Bắt buộc điền
DECLARE @LocalPrice decimal(12,2) = NULL; -- NULL = dùng BasePrice
DECLARE @IsListed bit = 1;

IF @ProductId IS NOT NULL
BEGIN
    BEGIN TRANSACTION;

    UPDATE catalog.BranchMenu
       SET LocalPrice = @LocalPrice,
           IsListed = @IsListed
     WHERE BranchId = @MenuBranchId
       AND ProductId = @ProductId;

    IF @@ROWCOUNT <> 1
        THROW 51001, N'Không tìm thấy đúng một dòng menu cần sửa.', 1;

    SELECT bm.BranchId, bm.ProductId, p.Name,
           p.BasePrice, bm.LocalPrice,
           COALESCE(bm.LocalPrice, p.BasePrice) AS EffectivePrice,
           bm.IsListed
    FROM catalog.BranchMenu bm
    JOIN catalog.Product p ON p.ProductId = bm.ProductId
    WHERE bm.BranchId = @MenuBranchId AND bm.ProductId = @ProductId;

    IF @ApplyChanges = 1 COMMIT TRANSACTION;
    ELSE BEGIN ROLLBACK TRANSACTION; PRINT N'PREVIEW: thay đổi menu đã rollback.'; END
END
ELSE PRINT N'Bỏ qua block A: hãy điền @ProductId.';
GO

/* B. Cập nhật trạng thái hoặc lương giờ của một tài khoản; không đụng PasswordHash. */
DECLARE @ApplyChanges bit = 0;
DECLARE @Username varchar(60) = NULL;   -- Bắt buộc điền
DECLARE @NewStatus varchar(10) = 'ACTIVE';
DECLARE @NewHourlyRate decimal(12,2) = NULL;

IF @Username IS NOT NULL
BEGIN
    BEGIN TRANSACTION;

    UPDATE iam.UserAccount
       SET Status = @NewStatus,
           HourlyRate = @NewHourlyRate
     WHERE Username = @Username;

    IF @@ROWCOUNT <> 1
        THROW 51002, N'Không tìm thấy đúng một tài khoản cần sửa.', 1;

    SELECT UserId, Username, FullName, RoleCode, BranchId, HourlyRate, Status
    FROM iam.UserAccount
    WHERE Username = @Username;

    IF @ApplyChanges = 1 COMMIT TRANSACTION;
    ELSE BEGIN ROLLBACK TRANSACTION; PRINT N'PREVIEW: thay đổi tài khoản đã rollback.'; END
END
ELSE PRINT N'Bỏ qua block B: hãy điền @Username.';
GO

/* C. Xếp một ca mới. Chấm công vẫn để trống để nhân viên tự vào ca trên ứng dụng. */
DECLARE @ApplyChanges bit = 0;
DECLARE @ShiftUsername varchar(60) = NULL; -- Bắt buộc điền
DECLARE @ShiftBranchId int = 1;
DECLARE @WorkDate date = CONVERT(date, DATEADD(HOUR, 7, SYSUTCDATETIME()));
DECLARE @ShiftName nvarchar(60) = N'Ca sáng';
DECLARE @StartTime time = '07:00';
DECLARE @EndTime time = '12:00';

IF @ShiftUsername IS NOT NULL
BEGIN
    BEGIN TRANSACTION;

    DECLARE @UserId int;
    DECLARE @HourlyRate decimal(12,2);

    SELECT @UserId = UserId, @HourlyRate = HourlyRate
    FROM iam.UserAccount
    WHERE Username = @ShiftUsername
      AND BranchId = @ShiftBranchId
      AND Status = 'ACTIVE';

    IF @UserId IS NULL
        THROW 51003, N'Tài khoản không active hoặc không thuộc chi nhánh đã chọn.', 1;

    IF EXISTS (
        SELECT 1
        FROM hr.ShiftAssignment
        WHERE UserId = @UserId AND WorkDate = @WorkDate
          AND StartTime < @EndTime AND EndTime > @StartTime
    )
        THROW 51004, N'Ca mới bị chồng thời gian với ca đã có trong ngày.', 1;

    INSERT hr.ShiftAssignment
        (ShiftName, StartTime, EndTime, UserId, WorkDate, BranchId, HourlyRateSnapshot)
    VALUES
        (@ShiftName, @StartTime, @EndTime, @UserId, @WorkDate, @ShiftBranchId, @HourlyRate);

    SELECT sa.ShiftAssignmentId, u.Username, sa.WorkDate,
           sa.ShiftName, sa.StartTime, sa.EndTime, sa.HourlyRateSnapshot
    FROM hr.ShiftAssignment sa
    JOIN iam.UserAccount u ON u.UserId = sa.UserId
    WHERE sa.ShiftAssignmentId = SCOPE_IDENTITY();

    IF @ApplyChanges = 1 COMMIT TRANSACTION;
    ELSE BEGIN ROLLBACK TRANSACTION; PRINT N'PREVIEW: ca mới đã rollback.'; END
END
ELSE PRINT N'Bỏ qua block C: hãy điền @ShiftUsername.';
GO

