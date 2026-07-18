-- Add barista owner for personal KPI on dashboard (idempotent).
IF COL_LENGTH('sales.OrderItem', 'PreparedBy') IS NULL
BEGIN
    ALTER TABLE sales.OrderItem ADD PreparedBy INT NULL;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_OI_PreparedBy'
      AND parent_object_id = OBJECT_ID('sales.OrderItem')
)
BEGIN
    ALTER TABLE sales.OrderItem
        ADD CONSTRAINT FK_OI_PreparedBy FOREIGN KEY (PreparedBy) REFERENCES iam.[User](UserId);
END
GO
