/* Cụm nhật ký nghiệp vụ - outbox tích hợp. Chỉ đọc dữ liệu. */
SET NOCOUNT ON;

DECLARE @BranchId int = 1;
DECLARE @FromUtc datetime2 = DATEADD(DAY, -7, SYSUTCDATETIME());
DECLARE @TopRows int = 300;

-- 1. Nhật ký thao tác.
SELECT TOP (@TopRows)
       a.ActivityLogId, a.EntityType, a.EntityId,
       a.BranchId, a.ActionType,
       actor.Username AS PerformedByUsername,
       a.FromValue, a.ToValue, a.Reason,
       DATEADD(HOUR, 7, a.PerformedAt) AS PerformedAtLocal
FROM ops.ActivityLog a
LEFT JOIN iam.UserAccount actor ON actor.UserId = a.PerformedBy
WHERE (@BranchId IS NULL OR a.BranchId = @BranchId)
  AND a.PerformedAt >= @FromUtc
ORDER BY a.PerformedAt DESC, a.ActivityLogId DESC;

-- 2. Event chưa được consumer xử lý.
SELECT TOP (@TopRows)
       e.OutboxEventId, e.EventType, e.AggregateId, e.BranchId,
       JSON_QUERY(e.Payload) AS Payload,
       DATEADD(HOUR, 7, e.CreatedAt) AS CreatedAtLocal,
       DATEDIFF(MINUTE, e.CreatedAt, SYSUTCDATETIME()) AS PendingMinutes
FROM ops.OutboxEvent e
WHERE (@BranchId IS NULL OR e.BranchId = @BranchId)
  AND e.ProcessedAt IS NULL
ORDER BY e.CreatedAt, e.OutboxEventId;

-- 3. Thống kê outbox theo loại và trạng thái.
SELECT e.EventType,
       CASE WHEN e.ProcessedAt IS NULL THEN 'PENDING' ELSE 'PROCESSED' END AS ProcessingState,
       COUNT(*) AS EventCount,
       MIN(DATEADD(HOUR, 7, e.CreatedAt)) AS OldestCreatedAtLocal,
       MAX(DATEADD(HOUR, 7, e.CreatedAt)) AS LatestCreatedAtLocal
FROM ops.OutboxEvent e
WHERE (@BranchId IS NULL OR e.BranchId = @BranchId)
  AND e.CreatedAt >= @FromUtc
GROUP BY e.EventType,
         CASE WHEN e.ProcessedAt IS NULL THEN 'PENDING' ELSE 'PROCESSED' END
ORDER BY e.EventType, ProcessingState;

