/*
  Bản đồ tổng thể database. Chỉ đọc dữ liệu.
  Chạy file này đầu tiên để xác nhận đang đứng đúng database và hiểu các quan hệ.
*/
SET NOCOUNT ON;

IF DB_NAME() IN (N'master', N'model', N'msdb', N'tempdb')
    THROW 51000, N'Hãy chọn database CafeChain trước khi chạy.', 1;

SELECT DB_NAME() AS DatabaseName,
       @@SERVERNAME AS ServerName,
       SYSUTCDATETIME() AS CurrentUtc,
       DATEADD(HOUR, 7, SYSUTCDATETIME()) AS CurrentVietnamTime;

-- 1. Danh sách bảng theo schema nghiệp vụ.
SELECT s.name AS SchemaName,
       t.name AS TableName,
       SUM(CASE WHEN p.index_id IN (0, 1) THEN p.rows ELSE 0 END) AS ApproxRowCount
FROM sys.tables t
JOIN sys.schemas s ON s.schema_id = t.schema_id
JOIN sys.partitions p ON p.object_id = t.object_id
WHERE t.is_ms_shipped = 0
  AND NOT (s.name = N'dbo' AND t.name = N'sysdiagrams')
  AND t.name <> N'flyway_schema_history'
GROUP BY s.name, t.name
ORDER BY s.name, t.name;

-- 2. Foreign key, gồm cả khóa ghép.
SELECT fk.name AS ForeignKeyName,
       QUOTENAME(ps.name) + N'.' + QUOTENAME(pt.name) AS FromTable,
       STRING_AGG(QUOTENAME(pc.name), N', ') WITHIN GROUP (ORDER BY fkc.constraint_column_id) AS FromColumns,
       QUOTENAME(rs.name) + N'.' + QUOTENAME(rt.name) AS ToTable,
       STRING_AGG(QUOTENAME(rc.name), N', ') WITHIN GROUP (ORDER BY fkc.constraint_column_id) AS ToColumns,
       fk.delete_referential_action_desc AS OnDelete,
       fk.is_disabled AS IsDisabled,
       fk.is_not_trusted AS IsNotTrusted
FROM sys.foreign_keys fk
JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
JOIN sys.tables pt ON pt.object_id = fk.parent_object_id
JOIN sys.schemas ps ON ps.schema_id = pt.schema_id
JOIN sys.columns pc ON pc.object_id = pt.object_id AND pc.column_id = fkc.parent_column_id
JOIN sys.tables rt ON rt.object_id = fk.referenced_object_id
JOIN sys.schemas rs ON rs.schema_id = rt.schema_id
JOIN sys.columns rc ON rc.object_id = rt.object_id AND rc.column_id = fkc.referenced_column_id
GROUP BY fk.name, ps.name, pt.name, rs.name, rt.name,
         fk.delete_referential_action_desc, fk.is_disabled, fk.is_not_trusted
ORDER BY FromTable, fk.name;

-- 3. Index nghiệp vụ.
SELECT QUOTENAME(s.name) + N'.' + QUOTENAME(t.name) AS TableName,
       i.name AS IndexName,
       i.type_desc AS IndexType,
       i.is_unique AS IsUnique,
       i.has_filter AS HasFilter,
       i.filter_definition AS FilterDefinition
FROM sys.indexes i
JOIN sys.tables t ON t.object_id = i.object_id
JOIN sys.schemas s ON s.schema_id = t.schema_id
WHERE i.index_id > 0 AND i.is_hypothetical = 0
ORDER BY s.name, t.name, i.is_primary_key DESC, i.name;

-- 4. Trigger bảo vệ quy tắc nghiệp vụ.
SELECT QUOTENAME(s.name) + N'.' + QUOTENAME(t.name) AS TableName,
       tr.name AS TriggerName,
       tr.is_disabled AS IsDisabled
FROM sys.triggers tr
JOIN sys.tables t ON t.object_id = tr.parent_id
JOIN sys.schemas s ON s.schema_id = t.schema_id
WHERE tr.is_ms_shipped = 0
ORDER BY s.name, t.name, tr.name;

