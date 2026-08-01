/* Cụm chi nhánh - tài khoản - ca làm. Chỉ đọc dữ liệu. */
SET NOCOUNT ON;

DECLARE @BranchId int = 1;
DECLARE @FromDate date = DATEADD(DAY, -7, CONVERT(date, DATEADD(HOUR, 7, SYSUTCDATETIME())));
DECLARE @ToDate date = DATEADD(DAY, 7, CONVERT(date, DATEADD(HOUR, 7, SYSUTCDATETIME())));

-- 1. Chi nhánh và quản lý phụ trách.
SELECT b.BranchId, b.Code, b.Name, b.Address, b.Phone,
       b.OpenTime, b.CloseTime, b.IsActive, b.PeakThresholdCups,
       m.UserId AS ManagerUserId, m.Username AS ManagerUsername, m.FullName AS ManagerName
FROM org.Branch b
LEFT JOIN iam.UserAccount m ON m.UserId = b.ManagerUserId
WHERE @BranchId IS NULL OR b.BranchId = @BranchId
ORDER BY b.BranchId;

-- 2. Nhân sự; chủ ý không đọc PasswordHash.
SELECT u.UserId, u.Username, u.FullName, u.Email, u.Phone,
       u.RoleCode, u.BranchId, b.Code AS BranchCode,
       u.HourlyRate, u.Status, DATEADD(HOUR, 7, u.CreatedAt) AS CreatedAtLocal
FROM iam.UserAccount u
LEFT JOIN org.Branch b ON b.BranchId = u.BranchId
WHERE @BranchId IS NULL OR u.BranchId = @BranchId
ORDER BY u.BranchId, u.RoleCode, u.Username;

-- 3. Lịch làm và chấm công trong khoảng ngày.
SELECT sa.ShiftAssignmentId, sa.BranchId, b.Code AS BranchCode,
       sa.UserId, u.Username, u.FullName, u.RoleCode,
       sa.WorkDate, sa.ShiftName, sa.StartTime, sa.EndTime,
       DATEADD(HOUR, 7, sa.CheckInAt) AS CheckInAtLocal,
       DATEADD(HOUR, 7, sa.CheckOutAt) AS CheckOutAtLocal,
       sa.AttendanceStatus, sa.HourlyRateSnapshot,
       approver.Username AS ApprovedByUsername,
       DATEADD(HOUR, 7, sa.ApprovedAt) AS ApprovedAtLocal,
       CASE
           WHEN sa.CheckInAt IS NULL THEN N'Chưa vào ca'
           WHEN sa.CheckOutAt IS NULL THEN N'Đang trong ca'
           WHEN sa.AttendanceStatus = 'APPROVED' THEN N'Đã duyệt'
           WHEN sa.AttendanceStatus = 'REJECTED' THEN N'Từ chối'
           ELSE N'Chờ duyệt'
       END AS ShiftState
FROM hr.ShiftAssignment sa
JOIN iam.UserAccount u ON u.UserId = sa.UserId
JOIN org.Branch b ON b.BranchId = sa.BranchId
LEFT JOIN iam.UserAccount approver ON approver.UserId = sa.ApprovedBy
WHERE (@BranchId IS NULL OR sa.BranchId = @BranchId)
  AND sa.WorkDate BETWEEN @FromDate AND @ToDate
ORDER BY sa.WorkDate DESC, sa.StartTime, u.Username;

-- 4. Người đang có bản ghi vào ca nhưng chưa ra ca.
SELECT sa.BranchId, u.Username, u.FullName, u.RoleCode,
       sa.ShiftAssignmentId, sa.ShiftName, sa.WorkDate, sa.StartTime, sa.EndTime,
       DATEADD(HOUR, 7, sa.CheckInAt) AS CheckInAtLocal
FROM hr.ShiftAssignment sa
JOIN iam.UserAccount u ON u.UserId = sa.UserId
WHERE (@BranchId IS NULL OR sa.BranchId = @BranchId)
  AND sa.CheckInAt IS NOT NULL
  AND sa.CheckOutAt IS NULL
ORDER BY sa.BranchId, sa.CheckInAt;

