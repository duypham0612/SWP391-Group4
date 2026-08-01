package com.cafe.integration;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Contract metadata, cross-branch, lifecycle và value của schema cuối. */
public class DatabaseSchemaContractIT extends SqlServerIntegrationSupport {

    @Test
    void final_metadata_uses_only_canonical_names_and_trusted_constraints() throws Exception {
        assertEquals(8, scalarInt("SELECT COUNT(*) FROM sys.schemas WHERE name IN " +
                "('iam','org','catalog','inventory','hr','sales','payment','ops')"));
        assertEquals(49, scalarInt("SELECT COUNT(*) FROM sys.tables t " +
                "JOIN sys.schemas s ON s.schema_id=t.schema_id WHERE s.name IN " +
                "('iam','org','catalog','inventory','hr','sales','payment','ops') " +
                "AND t.object_id<>OBJECT_ID('ops.flyway_schema_history')"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.tables t JOIN sys.schemas s ON s.schema_id=t.schema_id " +
                "WHERE (s.name='iam' AND t.name='User') OR (s.name='sales' AND t.name='Orders') " +
                "OR (s.name='inventory' AND t.name IN('WasteLog','WasteAuditLog','WasteReview'))"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.columns WHERE " +
                "(object_id=OBJECT_ID('catalog.HomeSetting') AND name='Id') " +
                "OR (object_id=OBJECT_ID('catalog.MenuBlockRequest') AND name='RequestId') " +
                "OR (object_id=OBJECT_ID('catalog.ModifierIngredientImpact') AND name='ImpactId') " +
                "OR (object_id=OBJECT_ID('inventory.InventoryTransaction') AND name='InventoryTxnId') " +
                "OR (object_id=OBJECT_ID('catalog.BranchMenu') AND name IN('IsAvailable','Is86')) " +
                "OR (object_id=OBJECT_ID('hr.Payroll') AND name='PayMonth') " +
                "OR (object_id=OBJECT_ID('inventory.StockReceipt') AND name='ReceiptDate')"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.objects WHERE parent_object_id<>OBJECT_ID('ops.flyway_schema_history') AND (" +
                "name LIKE 'PK[_][_]%' OR name LIKE 'FK[_][_]%' OR name LIKE 'CK[_][_]%' OR name LIKE 'DF[_][_]%'" +
                ")"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.foreign_keys WHERE name IN(" +
                "'FK_ShiftAssignment_User','FK_Payroll_User','FK_WasteEventAudit_WasteLog')"));
        assertEquals(3, scalarInt("SELECT COUNT(*) FROM sys.foreign_keys WHERE name IN(" +
                "'FK_ShiftAssignment_UserAccount_User','FK_Payroll_UserAccount_User'," +
                "'FK_WasteEventAudit_WasteEventItem_Item')"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.key_constraints k JOIN sys.tables t " +
                "ON t.object_id=k.parent_object_id WHERE k.type='PK' AND k.name<>'PK_'+t.name " +
                "AND t.object_id<>OBJECT_ID('ops.flyway_schema_history')"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.default_constraints d JOIN sys.tables t " +
                "ON t.object_id=d.parent_object_id JOIN sys.columns c ON c.object_id=d.parent_object_id " +
                "AND c.column_id=d.parent_column_id WHERE d.name<>'DF_'+t.name+'_'+c.name " +
                "AND t.object_id<>OBJECT_ID('ops.flyway_schema_history')"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.check_constraints c JOIN sys.tables t " +
                "ON t.object_id=c.parent_object_id WHERE c.name NOT LIKE 'CK[_]'+t.name+'[_]%'"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.foreign_keys f JOIN sys.tables t " +
                "ON t.object_id=f.parent_object_id WHERE f.name NOT LIKE 'FK[_]'+t.name+'[_]%'"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.indexes i JOIN sys.tables t ON t.object_id=i.object_id " +
                "WHERE i.index_id>0 AND i.is_primary_key=0 AND i.is_unique_constraint=0 " +
                "AND i.name NOT LIKE 'IX[_]'+t.name+'[_]%' AND i.name NOT LIKE 'UX[_]'+t.name+'[_]%' " +
                "AND t.object_id<>OBJECT_ID('ops.flyway_schema_history')"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.foreign_keys WHERE is_disabled=1 OR is_not_trusted=1"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.check_constraints WHERE is_disabled=1 OR is_not_trusted=1"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.indexes WHERE " +
                "(object_id=OBJECT_ID('inventory.StockReceiptDetail') AND name='IX_SRD_Receipt') " +
                "OR (object_id=OBJECT_ID('inventory.StockAdjustment') AND name='IX_StockAdjustment_Count')"));
        assertEquals(2, scalarInt("SELECT COUNT(*) FROM sys.indexes WHERE object_id=OBJECT_ID('ops.OutboxEvent') " +
                "AND name IN('IX_OutboxEvent_PendingQueue','IX_OutboxEvent_PendingAggregate') AND has_filter=1"));
        assertEquals(7, scalarInt("SELECT COUNT(*) FROM sys.indexes WHERE name IN(" +
                "'IX_ShiftAssignment_TemplateBranch','IX_OrderItem_OrderBranch','IX_BillItem_BillBranch'," +
                "'IX_WasteEvent_OrderItemBranch','IX_WasteEvent_ShiftAssignmentBranch'," +
                "'IX_OrderItemActionLog_OrderItemBranch','IX_WasteEventItem_WasteEventBranch')"));
        assertEquals(5, scalarInt("SELECT COUNT(*) FROM ops.LegacySchemaVersion WHERE VersionCode IN(" +
                "'20260731_naming_v2','20260731_prep_recipe_v2','20260731_integrity_v2'," +
                "'20260731_pickup_sequence_v1','20260731_menublock_utc_v2')"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM inventory.InventoryTransaction WHERE ReferenceType='WasteLog'"));
    }

    @Test
    void every_structural_cross_branch_relationship_is_rejected() throws Exception {
        Fixture f = fixture();

        rejects("INSERT hr.ShiftAssignment(ShiftTemplateId,BranchId,UserId,WorkDate) VALUES (?,?,?,?)",
                f.shiftTemplateA, f.branchB, f.baristaB, java.sql.Date.valueOf(LocalDate.now().plusDays(2)));
        rejects("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status,ProductNameAtOrder) " +
                        "VALUES (?,?,?,1,100,'WAITING',N'Snapshot')",
                f.orderA, f.branchB, f.productId);
        rejects("INSERT payment.BillItem(BillId,BranchId,OrderItemId,Amount) VALUES (?,?,?,100)",
                f.billA, f.branchB, f.orderItemB);
        rejects("INSERT payment.BillItem(BillId,BranchId,OrderItemId,Amount) VALUES (?,?,?,100)",
                f.billA, f.branchA, f.orderItemB);
        rejects("INSERT inventory.WasteEvent(BranchId,EventKind,Source,OrderItemId,CauseCode,CreatedBy) " +
                        "VALUES (?,'INGREDIENT_WASTE','MANUAL',?,'CROSS_BRANCH',?)",
                f.branchB, f.orderItemA, f.baristaB);
        String otherProductName = unique("other-product");
        execute("INSERT catalog.Product(CategoryId,Name,BasePrice) VALUES (?,?,100)",
                f.categoryId, otherProductName);
        int otherProductId = scalarInt("SELECT ProductId FROM catalog.Product WHERE Name=?",
                otherProductName);
        rejects("INSERT inventory.WasteEvent(BranchId,EventKind,Source,OrderItemId,ProductId,CauseCode,CreatedBy) " +
                        "VALUES (?,'REMAKE','ORDER',?,?,'WRONG_PRODUCT',?)",
                f.branchA, f.orderItemA, otherProductId, f.baristaA);
        rejects("INSERT inventory.WasteEvent(BranchId,EventKind,Source,ShiftAssignmentId,CauseCode,CreatedBy) " +
                        "VALUES (?,'INGREDIENT_WASTE','MANUAL',?,'CROSS_SHIFT',?)",
                f.branchB, f.shiftAssignmentA, f.baristaB);
        rejects("INSERT ops.OrderItemActionLog(OrderItemId,BranchId,ActionType) VALUES (?,?,'CLAIM')",
                f.orderItemA, f.branchB);
    }

    @Test
    void invalid_lifecycle_states_are_rejected() throws Exception {
        Fixture f = fixture();

        rejects("INSERT sales.TableSession(BranchId,DiningTableId,OpenedBy,Status) " +
                        "VALUES (?,?,?,'BILLED')", f.branchA, f.tableA, f.cashierA);
        rejects("UPDATE sales.DiningTable SET Status='CLEANING' WHERE DiningTableId=?", f.tableA);
        rejects("INSERT payment.Bill(BranchId,Status) VALUES (?,'REFUND')", f.branchA);
        rejects("INSERT sales.SalesOrder(BranchId,Source,OrderType,Status,CreatedBy,BusinessDate) " +
                        "VALUES (?,'COUNTER','DELIVERY','ACTIVE',?,CONVERT(date,DATEADD(hour,7,SYSUTCDATETIME())))",
                f.branchA, f.cashierA);

        rejects("INSERT catalog.MenuBlockRequest(BranchId,ProductId,Reason,RequestedBy,Status,ReviewedBy,ReviewedAt) " +
                        "VALUES (?,?, 'OUT_OF_STOCK',?,'PENDING',?,SYSUTCDATETIME())",
                f.branchA, f.productId, f.baristaA, f.managerA);
        rejects("INSERT catalog.MenuBlockRequest(BranchId,ProductId,Reason,RequestedBy,Status,ReviewedAt,ClosedAt) " +
                        "VALUES (?,?,'EQUIPMENT',?,'RESOLVED',DATEADD(minute,1,SYSUTCDATETIME())," +
                        "DATEADD(minute,1,SYSUTCDATETIME()))",
                f.branchA, f.productId, f.baristaA);
        rejects("INSERT hr.Attendance(ShiftAssignmentId,Status) VALUES (?,'APPROVED')", f.shiftAssignmentA);
        rejects("INSERT inventory.PrepBatch(BranchId,PreppedIngredientId,QuantityProduced,MadeBy,Status,RequiresApproval) " +
                        "VALUES (?,?,10,?,'PENDING',0)",
                f.branchA, f.preppedIngredientId, f.baristaA);
        rejects("INSERT inventory.PrepBatch(BranchId,PreppedIngredientId,QuantityProduced,MadeBy," +
                        "MadeAt,ExpiresAt,Status,RequiresApproval) VALUES (?,?,10,?," +
                        "SYSUTCDATETIME(),DATEADD(minute,-1,SYSUTCDATETIME()),'ACTIVE',0)",
                f.branchA, f.preppedIngredientId, f.baristaA);
        rejects("INSERT sales.TableSession(BranchId,DiningTableId,OpenedBy,Status) VALUES (?,?,?,'CLOSED')",
                f.branchA, f.tableA, f.cashierA);
        rejects("INSERT sales.TableSession(BranchId,DiningTableId,OpenedBy,OpenedAt,ClosedAt,Status) " +
                        "VALUES (?,?,?,SYSUTCDATETIME(),DATEADD(minute,-1,SYSUTCDATETIME()),'CLOSED')",
                f.branchA, f.tableA, f.cashierA);
        rejects("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status,ProductNameAtOrder) " +
                        "VALUES (?,?,?,1,100,'READY',N'Snapshot')",
                f.orderA, f.branchA, f.productId);
        rejects("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status,ProductNameAtOrder," +
                        "StartedAt,DoneAt,PickedUpAt,ServedAt) VALUES (?,?,?,1,100,'SERVED',N'Snapshot'," +
                        "DATEADD(minute,-1,SYSUTCDATETIME()),SYSUTCDATETIME()," +
                        "DATEADD(minute,-2,SYSUTCDATETIME()),SYSUTCDATETIME())",
                f.orderA, f.branchA, f.productId);
        rejects("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status,ProductNameAtOrder,DoneAt) " +
                        "VALUES (?,?,?,1,100,'WAITING',N'Snapshot',SYSUTCDATETIME())",
                f.orderA, f.branchA, f.productId);
        rejects("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status,ProductNameAtOrder,PickedUpAt) " +
                        "VALUES (?,?,?,1,100,'WAITING',N'Snapshot',SYSUTCDATETIME())",
                f.orderA, f.branchA, f.productId);
        rejects("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status,ProductNameAtOrder,ServedAt) " +
                        "VALUES (?,?,?,1,100,'WAITING',N'Snapshot',SYSUTCDATETIME())",
                f.orderA, f.branchA, f.productId);
        rejects("INSERT payment.Bill(BranchId,Status) VALUES (?,'PAID')", f.branchA);
        rejects("INSERT payment.Bill(BranchId,Status,PaymentMethod,CashTendered,CashChange) " +
                "VALUES (?,'UNPAID','TRANSFER',100,0)", f.branchA);
        execute("INSERT payment.CashierShift(BranchId,CashierId,OpeningCash) VALUES (?,?,0)",
                f.branchA, f.cashierA);
        int shiftId = scalarInt("SELECT MAX(CashierShiftId) FROM payment.CashierShift WHERE BranchId=?",
                f.branchA);
        rejects("INSERT payment.Bill(BranchId,CashierShiftId,Subtotal,VatAmount,TotalAmount," +
                        "PaidAmount,PaymentMethod,Status,PaidAt) " +
                        "VALUES (?,?,100,0,100,100,'CASH','PAID',SYSUTCDATETIME())",
                f.branchA, shiftId);
        rejects("INSERT inventory.WasteEventAudit(ActionType,PerformedBy) VALUES ('CREATE',?)", f.baristaA);
        execute("INSERT inventory.WasteEventItem(WasteEventId,BranchId,IngredientId,Quantity,WasteType,LoggedBy) " +
                        "VALUES (?,?,?,1,'SPILL',?)",
                f.wasteEventA, f.branchA, f.rawIngredientId, f.baristaA);
        int wasteItemId = scalarInt("SELECT MAX(WasteEventItemId) FROM inventory.WasteEventItem WHERE WasteEventId=?",
                f.wasteEventA);
        String secondCause = unique("second-waste");
        execute("INSERT inventory.WasteEvent(BranchId,EventKind,Source,CauseCode,CreatedBy) " +
                        "VALUES (?,'INGREDIENT_WASTE','MANUAL',?,?)",
                f.branchA, secondCause, f.baristaA);
        long secondWasteEventId = scalarLong(
                "SELECT WasteEventId FROM inventory.WasteEvent WHERE BranchId=? AND CauseCode=?",
                f.branchA, secondCause);
        rejects("INSERT inventory.WasteEventAudit(WasteEventItemId,WasteEventId,ActionType,PerformedBy) " +
                        "VALUES (?,?,'CREATE',?)",
                wasteItemId, secondWasteEventId, f.baristaA);
        rejects("INSERT inventory.WasteEventReview(WasteEventId,IngredientId,ReviewType,QtyBefore,QtyAfter,Status) " +
                        "VALUES (?,?,'SOFT_NEGATIVE',1,-1,'RESOLVED')",
                f.wasteEventA, f.rawIngredientId);
        rejects("INSERT inventory.WasteEventReview(WasteEventId,IngredientId,ReviewType,QtyBefore,QtyAfter," +
                        "Status,ResolvedBy,ResolvedAt) VALUES (?,?,'SOFT_NEGATIVE',1,-1,'OPEN',?,SYSUTCDATETIME())",
                f.wasteEventA, f.rawIngredientId, f.managerA);
        rejects("INSERT ops.OutboxEvent(EventType,Payload) VALUES ('invalid.json',N'{broken')");
        rejects("INSERT ops.OrderItemActionLog(OrderItemId,BranchId,ActionType) VALUES (?,?,'UNKNOWN')",
                f.orderItemA, f.branchA);
    }

    @Test
    void invalid_values_and_duplicate_modifiers_are_rejected() throws Exception {
        Fixture f = fixture();

        rejects("INSERT catalog.Product(CategoryId,Name,BasePrice) VALUES (?,?, -1)",
                f.categoryId, unique("negative-product"));
        rejects("INSERT catalog.BranchMenu(BranchId,ProductId,LocalPrice) VALUES (?,?, -1)",
                f.branchA, f.productId);
        rejects("INSERT inventory.StockReceiptDetail(StockReceiptId,IngredientId,EnteredQuantity,UnitCost," +
                        "IngredientUnitConversionId,UnitNameAtEntry,FactorToBaseAtEntry) " +
                        "SELECT ?,?,1,-1,c.IngredientUnitConversionId,c.UnitName,c.FactorToBase " +
                        "FROM catalog.IngredientUnitConversion c WHERE c.IngredientId=? AND c.IsBaseUnit=1",
                f.stockReceiptA, f.rawIngredientId, f.rawIngredientId);
        rejects("INSERT hr.Payroll(BranchId,UserId,PayrollMonth,WorkedHours,HourlyRate,UpdatedBy) " +
                        "VALUES (?,?,?, -1,1,?)",
                f.branchA, f.baristaA, java.sql.Date.valueOf(LocalDate.now().withDayOfMonth(1)), f.managerA);
        rejects("INSERT hr.Payroll(BranchId,UserId,PayrollMonth,WorkedHours,HourlyRate,UpdatedBy) " +
                        "VALUES (?,?,?,1,1,?)",
                f.branchA, f.baristaA, java.sql.Date.valueOf(LocalDate.now().withDayOfMonth(2)), f.managerA);
        rejects("INSERT inventory.BranchInventory(BranchId,IngredientId,QuantityOnHand,MinThreshold,PrepTargetQty) " +
                "VALUES (?,?,0,0,10)", f.branchA, f.rawIngredientId);
        rejects("INSERT inventory.BranchInventory(BranchId,IngredientId,QuantityOnHand,MinThreshold) " +
                "VALUES (?,?,0,-1)", f.branchA, f.rawIngredientId);
        rejects("INSERT inventory.StockAdjustment(BranchId,IngredientId,SystemBaseQty,ActualBaseQty," +
                        "IngredientUnitConversionId,CountedQuantity,UnitNameAtCount,FactorToBaseAtCount,AdjustedBy) " +
                        "SELECT ?,?,0,-1,c.IngredientUnitConversionId,-1,c.UnitName,c.FactorToBase,? " +
                        "FROM catalog.IngredientUnitConversion c WHERE c.IngredientId=? AND c.IsBaseUnit=1",
                f.branchA, f.rawIngredientId, f.baristaA, f.rawIngredientId);
        rejects("INSERT inventory.InventoryTransaction(BranchId,IngredientId,ChangeQty,TxnType,CreatedBy) " +
                "VALUES (?,?,0,'ADJUST',?)", f.branchA, f.rawIngredientId, f.managerA);
        rejects("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status,ProductNameAtOrder) " +
                "VALUES (?,?,?,1,-1,'WAITING',N'Snapshot')", f.orderA, f.branchA, f.productId);
        rejects("INSERT payment.BillItem(BillId,BranchId,OrderItemId,Amount) VALUES (?,?,?,-1)",
                f.billA, f.branchA, f.orderItemA);
        rejects("INSERT catalog.ModifierGroup(Name,IsRequired,MinSelect,MaxSelect) VALUES (?,1,0,1)",
                unique("bad-group"));

        String groupName = unique("modifier-group");
        execute("INSERT catalog.ModifierGroup(Name) VALUES (?)", groupName);
        int groupId = scalarInt("SELECT ModifierGroupId FROM catalog.ModifierGroup WHERE Name=?", groupName);
        String optionName = unique("option");
        execute("INSERT catalog.ModifierOption(ModifierGroupId,Name) VALUES (?,?)", groupId, optionName);
        int optionId = scalarInt("SELECT ModifierOptionId FROM catalog.ModifierOption WHERE ModifierGroupId=? AND Name=?",
                groupId, optionName);
        rejects("INSERT catalog.ModifierOption(ModifierGroupId,Name) VALUES (?,?)", groupId, optionName);
        execute("INSERT sales.OrderItemModifier(OrderItemId,ModifierOptionId,PriceDelta,ModifierOptionNameAtOrder) " +
                "VALUES (?,?,0,?)", f.orderItemA, optionId, optionName);
        rejects("INSERT sales.OrderItemModifier(OrderItemId,ModifierOptionId,PriceDelta,ModifierOptionNameAtOrder) " +
                "VALUES (?,?,0,?)", f.orderItemA, optionId, optionName);

        // Non-negative means zero is valid, not merely positive.
        execute("INSERT inventory.StockReceiptDetail(StockReceiptId,IngredientId,EnteredQuantity,UnitCost," +
                        "IngredientUnitConversionId,UnitNameAtEntry,FactorToBaseAtEntry) " +
                        "SELECT ?,?,1,0,c.IngredientUnitConversionId,c.UnitName,c.FactorToBase " +
                        "FROM catalog.IngredientUnitConversion c WHERE c.IngredientId=? AND c.IsBaseUnit=1",
                f.stockReceiptA, f.rawIngredientId, f.rawIngredientId);
    }

    private Fixture fixture() throws Exception {
        int branchA = createBranch();
        int branchB = createBranch();
        int managerA = createUser(branchA, "BRANCH_MANAGER");
        int managerB = createUser(branchB, "BRANCH_MANAGER");
        int cashierA = createUser(branchA, "CASHIER");
        int cashierB = createUser(branchB, "CASHIER");
        int baristaA = createUser(branchA, "BARISTA");
        int baristaB = createUser(branchB, "BARISTA");
        execute("UPDATE org.Branch SET ManagerUserId=? WHERE BranchId=?", managerA, branchA);
        execute("UPDATE org.Branch SET ManagerUserId=? WHERE BranchId=?", managerB, branchB);

        String categoryName = unique("schema-category");
        execute("INSERT catalog.Category(Name) VALUES (?)", categoryName);
        int categoryId = scalarInt("SELECT CategoryId FROM catalog.Category WHERE Name=?", categoryName);
        String productName = unique("schema-product");
        execute("INSERT catalog.Product(CategoryId,Name,BasePrice) VALUES (?,?,100)", categoryId, productName);
        int productId = scalarInt("SELECT ProductId FROM catalog.Product WHERE Name=?", productName);
        String rawName = unique("raw");
        String preppedName = unique("prepped");
        execute("INSERT catalog.Ingredient(Name,Unit,IngredientType) VALUES (?,N'g','RAW')", rawName);
        execute("INSERT catalog.Ingredient(Name,Unit,IngredientType,ShelfLifeMinutes) VALUES (?,N'ml','PREPPED',60)",
                preppedName);
        int rawId = scalarInt("SELECT IngredientId FROM catalog.Ingredient WHERE Name=?", rawName);
        int preppedId = scalarInt("SELECT IngredientId FROM catalog.Ingredient WHERE Name=?", preppedName);

        int orderA = createOrder(branchA, cashierA);
        int orderB = createOrder(branchB, cashierB);
        execute("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status,ProductNameAtOrder) " +
                "VALUES (?,?,?,1,100,'WAITING',?)", orderA, branchA, productId, productName);
        int itemA = scalarInt("SELECT MAX(OrderItemId) FROM sales.OrderItem WHERE OrderId=?", orderA);
        execute("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status,ProductNameAtOrder) " +
                "VALUES (?,?,?,1,100,'WAITING',?)", orderB, branchB, productId, productName);
        int itemB = scalarInt("SELECT MAX(OrderItemId) FROM sales.OrderItem WHERE OrderId=?", orderB);

        String shiftName = unique("schema-shift");
        execute("INSERT hr.ShiftTemplate(BranchId,Name,StartTime,EndTime) VALUES (?,?,'08:00','12:00')",
                branchA, shiftName);
        int shiftTemplateA = scalarInt("SELECT ShiftTemplateId FROM hr.ShiftTemplate WHERE BranchId=? AND Name=?",
                branchA, shiftName);
        execute("INSERT hr.ShiftAssignment(ShiftTemplateId,BranchId,UserId,WorkDate) VALUES (?,?,?,?)",
                shiftTemplateA, branchA, baristaA, java.sql.Date.valueOf(LocalDate.now().plusDays(1)));
        int shiftAssignmentA = scalarInt("SELECT MAX(ShiftAssignmentId) FROM hr.ShiftAssignment WHERE ShiftTemplateId=?",
                shiftTemplateA);

        String tableNumber = unique("table");
        execute("INSERT sales.DiningTable(BranchId,TableNumber) VALUES (?,?)", branchA, tableNumber);
        int tableA = scalarInt("SELECT DiningTableId FROM sales.DiningTable WHERE BranchId=? AND TableNumber=?",
                branchA, tableNumber);
        execute("INSERT payment.Bill(BranchId,Status) VALUES (?,'UNPAID')", branchA);
        int billA = scalarInt("SELECT MAX(BillId) FROM payment.Bill WHERE BranchId=?", branchA);
        execute("INSERT inventory.StockReceipt(BranchId,ReceivedBy,Status) VALUES (?,?,'DRAFT')", branchA, managerA);
        int receiptA = scalarInt("SELECT MAX(StockReceiptId) FROM inventory.StockReceipt WHERE BranchId=?", branchA);
        String cause = unique("schema-waste");
        execute("INSERT inventory.WasteEvent(BranchId,EventKind,Source,CauseCode,CreatedBy) " +
                "VALUES (?,'INGREDIENT_WASTE','MANUAL',?,?)", branchA, cause, baristaA);
        long wasteEventA = scalarLong("SELECT WasteEventId FROM inventory.WasteEvent WHERE BranchId=? AND CauseCode=?",
                branchA, cause);

        return new Fixture(branchA, branchB, managerA, cashierA, baristaA, baristaB, categoryId,
                productId, rawId, preppedId, orderA, orderB, itemA, itemB, shiftTemplateA,
                shiftAssignmentA, tableA, billA, receiptA, wasteEventA);
    }

    private int createOrder(int branchId, int cashierId) throws SQLException {
        execute("INSERT sales.SalesOrder(BranchId,Source,OrderType,Status,CreatedBy,BusinessDate) " +
                        "VALUES (?,'COUNTER','TAKEAWAY','ACTIVE',?,CONVERT(date,DATEADD(hour,7,SYSUTCDATETIME())))",
                branchId, cashierId);
        return scalarInt("SELECT MAX(OrderId) FROM sales.SalesOrder WHERE BranchId=?", branchId);
    }

    private int createBranch() throws SQLException {
        String code = unique("B").substring(0, 13);
        execute("INSERT org.Branch(Code,Name,OpenTime,CloseTime) VALUES (?,N'Schema IT','00:00','23:59')", code);
        return scalarInt("SELECT BranchId FROM org.Branch WHERE Code=?", code);
    }

    private int createUser(int branchId, String roleCode) throws SQLException {
        execute("IF NOT EXISTS(SELECT 1 FROM iam.Role WHERE Code=?) INSERT iam.Role(Code,Name) VALUES (?,?)",
                roleCode, roleCode, roleCode);
        int roleId = scalarInt("SELECT RoleId FROM iam.Role WHERE Code=?", roleCode);
        String username = unique("schema-user");
        execute("INSERT iam.UserAccount(Username,PasswordHash,FullName,RoleId,BranchId) " +
                "VALUES (?,'x',N'Schema IT',?,?)", username, roleId, branchId);
        return scalarInt("SELECT UserId FROM iam.UserAccount WHERE Username=?", username);
    }

    private void rejects(String sql, Object... args) {
        assertThrows(SQLException.class, () -> execute(sql, args));
    }

    private void execute(String sql, Object... args) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            statement.executeUpdate();
        }
    }

    private int scalarInt(String sql, Object... args) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private long scalarLong(String sql, Object... args) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? rs.getLong(1) : 0; }
        }
    }

    private void bind(PreparedStatement statement, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
    }

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record Fixture(int branchA, int branchB, int managerA, int cashierA,
                           int baristaA, int baristaB, int categoryId, int productId,
                           int rawIngredientId, int preppedIngredientId, int orderA, int orderB,
                           int orderItemA, int orderItemB, int shiftTemplateA,
                           int shiftAssignmentA, int tableA, int billA, int stockReceiptA,
                           long wasteEventA) { }
}
