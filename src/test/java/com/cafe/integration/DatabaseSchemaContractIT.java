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

/** Contract metadata, actor/branch, recipe và lifecycle của schema 25 bảng. */
public class DatabaseSchemaContractIT extends SqlServerIntegrationSupport {

    @Test
    void final_metadata_contains_only_25_business_tables_and_trusted_contracts() throws Exception {
        assertEquals(8, scalarInt("SELECT COUNT(*) FROM sys.schemas WHERE name IN "
                + "('iam','org','catalog','inventory','hr','sales','payment','ops')"));
        assertEquals(25, scalarInt("SELECT COUNT(*) FROM sys.tables t "
                + "JOIN sys.schemas s ON s.schema_id=t.schema_id WHERE s.name IN "
                + "('iam','org','catalog','inventory','hr','sales','payment','ops') "
                + "AND t.object_id<>OBJECT_ID('ops.flyway_schema_history')"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.tables WHERE object_id IN ("
                + "OBJECT_ID('iam.Role'),OBJECT_ID('hr.Attendance'),OBJECT_ID('hr.Payroll'),"
                + "OBJECT_ID('sales.TableSession'),OBJECT_ID('inventory.StockReceipt'),"
                + "OBJECT_ID('inventory.WasteEvent'),OBJECT_ID('inventory.WasteEventItem'),"
                + "OBJECT_ID('inventory.WasteEventAudit'),OBJECT_ID('ops.LegacySchemaVersion'))"));
        assertEquals(8, scalarInt("SELECT COUNT(*) FROM sys.triggers WHERE name IN ("
                + "'TR_Recipe_ValidateOwnerAndIngredient','TR_BranchMenu_BlockActorBranch',"
                + "'TR_ShiftAssignment_UserBranch','TR_ShiftAssignment_ApproverBranch',"
                + "'TR_StockReceiptLine_ActorBranch','TR_StockAdjustment_ActorBranch',"
                + "'TR_WasteEntry_ActorBranch','TR_SalesOrder_CreatorBranch') AND is_disabled=0"));
        assertEquals(5, scalarInt("SELECT COUNT(*) FROM sys.check_constraints WHERE name IN ("
                + "'CK_BranchMenu_BlockLifecycle','CK_BranchMenu_BlockStatus',"
                + "'CK_BranchMenu_BlockTimeOrder','CK_ShiftAssignment_AttendanceStatus',"
                + "'CK_ShiftAssignment_ApprovalLifecycle') AND is_disabled=0 AND is_not_trusted=0"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.foreign_keys "
                + "WHERE is_disabled=1 OR is_not_trusted=1"));
        assertEquals(0, scalarInt("SELECT COUNT(*) FROM sys.check_constraints "
                + "WHERE is_disabled=1 OR is_not_trusted=1"));
    }

    @Test
    void actor_triggers_reject_wrong_roles_and_cross_branch_users() throws Exception {
        Fixture f = fixture();

        // WasteEntry: creator/logger phải là BARISTA active đúng chi nhánh.
        rejects(wasteInsert(), f.branchA, f.cashierA, f.rawIngredientId, f.baristaA);
        rejects(wasteInsert(), f.branchA, f.baristaB, f.rawIngredientId, f.baristaB);

        // StockReceiptLine: người nhận phải là BRANCH_MANAGER active đúng chi nhánh.
        rejects(receiptInsert(), UUID.randomUUID().toString(), f.branchA, f.cashierA,
                f.rawIngredientId);
        rejects(receiptInsert(), UUID.randomUUID().toString(), f.branchA, f.managerB,
                f.rawIngredientId);

        // StockAdjustment: người điều chỉnh phải đúng role và branch.
        rejects(adjustmentInsert(), f.branchA, f.rawIngredientId, f.cashierA);
        rejects(adjustmentInsert(), f.branchA, f.rawIngredientId, f.baristaB);

        // SalesOrder COUNTER chỉ nhận CASHIER active đúng chi nhánh.
        rejects(orderInsert(), f.branchA, f.baristaA);
        rejects(orderInsert(), f.branchA, f.cashierB);

        // BranchMenu: requester phải là BARISTA của branch.
        rejects(blockPendingUpdate(), f.managerA, f.branchA, f.productId);
        rejects(blockPendingUpdate(), f.baristaB, f.branchA, f.productId);

        // ShiftAssignment user phải thuộc branch; approver phải là manager đúng branch.
        rejects("INSERT hr.ShiftAssignment(ShiftName,StartTime,EndTime,UserId,WorkDate,BranchId) "
                        + "VALUES (N'Cross branch','08:00','12:00',?,?,?)",
                f.baristaB, java.sql.Date.valueOf(LocalDate.now().plusDays(1)), f.branchA);
        int assignmentId = createPendingAssignment(f.branchA, f.baristaA);
        rejects(approveAssignmentUpdate(), f.cashierA, assignmentId);
        rejects(approveAssignmentUpdate(), f.managerB, assignmentId);
    }

    @Test
    void recipe_trigger_rejects_invalid_owner_ingredient_and_quantity() throws Exception {
        Fixture f = fixture();

        rejects("INSERT catalog.Recipe(OwnerType,OwnerId,IngredientId,Quantity) "
                        + "VALUES ('UNKNOWN',?,?,1)", f.productId, f.rawIngredientId);
        rejects("INSERT catalog.Recipe(OwnerType,OwnerId,IngredientId,Quantity) "
                        + "VALUES ('PREPPED',?,?,1)", f.preppedIngredientId, f.preppedIngredientId);
        rejects("INSERT catalog.Recipe(OwnerType,OwnerId,IngredientId,Quantity) "
                        + "VALUES ('PRODUCT',?,?,0)", f.productId, f.rawIngredientId);
        rejects("INSERT catalog.Recipe(OwnerType,OwnerId,IngredientId,Quantity) "
                        + "VALUES ('PREPPED',?,?,-1)", f.preppedIngredientId, f.rawIngredientId);

        // Modifier được phép âm để biểu diễn tác động bù trừ, nhưng không được bằng 0.
        execute("INSERT catalog.Recipe(OwnerType,OwnerId,IngredientId,Quantity) "
                + "VALUES ('MODIFIER',?,?,-1)", f.modifierOptionId, f.rawIngredientId);
        rejects("INSERT catalog.Recipe(OwnerType,OwnerId,IngredientId,Quantity) "
                        + "VALUES ('MODIFIER',?,?,0)", f.modifierOptionId, f.preppedIngredientId);
    }

    @Test
    void draft_receipt_cannot_create_inventory_ledger() throws Exception {
        Fixture f = fixture();
        String batchId = UUID.randomUUID().toString();
        execute(receiptInsert(), batchId, f.branchA, f.managerA, f.rawIngredientId);

        rejects("INSERT inventory.InventoryTransaction(BranchId,IngredientId,ChangeQty,TxnType,"
                        + "ReferenceType,ReferenceId,CreatedBy) "
                        + "VALUES (?,?,1,'RECEIPT','STOCK_RECEIPT_LINE',?,?)",
                f.branchA, f.rawIngredientId, batchId, f.managerA);
        assertEquals(0, scalarInt(
                "SELECT COUNT(*) FROM inventory.InventoryTransaction WHERE ReferenceType='STOCK_RECEIPT_LINE' "
                        + "AND ReferenceId=?", batchId));
        assertEquals("DRAFT", scalarString(
                "SELECT Status FROM inventory.StockReceiptLine WHERE ReceiptBatchId=?", batchId));
    }

    @Test
    void branch_menu_block_lifecycle_checks_reject_inconsistent_states() throws Exception {
        Fixture f = fixture();

        rejects("UPDATE catalog.BranchMenu SET BlockStatus='INVALID' WHERE BranchId=? AND ProductId=?",
                f.branchA, f.productId);
        rejects("UPDATE catalog.BranchMenu SET BlockStatus='PENDING',BlockReason='OUT_OF_STOCK',"
                        + "BlockRequestedBy=?,BlockRequestedAt=SYSUTCDATETIME(),"
                        + "BlockReviewedBy=?,BlockReviewedAt=SYSUTCDATETIME() "
                        + "WHERE BranchId=? AND ProductId=?",
                f.baristaA, f.managerA, f.branchA, f.productId);
        rejects("UPDATE catalog.BranchMenu SET BlockStatus='APPROVED',BlockReason='OUT_OF_STOCK',"
                        + "BlockRequestedBy=?,BlockRequestedAt=SYSUTCDATETIME() "
                        + "WHERE BranchId=? AND ProductId=?",
                f.baristaA, f.branchA, f.productId);
        rejects("UPDATE catalog.BranchMenu SET BlockStatus='PENDING',BlockReason='OUT_OF_STOCK',"
                        + "BlockRequestedBy=?,BlockRequestedAt=SYSUTCDATETIME(),"
                        + "BackInEta=DATEADD(minute,-1,SYSUTCDATETIME()) "
                        + "WHERE BranchId=? AND ProductId=?",
                f.baristaA, f.branchA, f.productId);
    }

    @Test
    void shift_attendance_lifecycle_checks_reject_inconsistent_states() throws Exception {
        Fixture f = fixture();
        int assignmentId = createPendingAssignment(f.branchA, f.baristaA);

        rejects("UPDATE hr.ShiftAssignment SET AttendanceStatus='INVALID' "
                + "WHERE ShiftAssignmentId=?", assignmentId);
        rejects("UPDATE hr.ShiftAssignment SET AttendanceStatus='APPROVED' "
                + "WHERE ShiftAssignmentId=?", assignmentId);
        rejects("UPDATE hr.ShiftAssignment SET AttendanceStatus='PENDING',ApprovedBy=?,"
                        + "ApprovedAt=SYSUTCDATETIME() WHERE ShiftAssignmentId=?",
                f.managerA, assignmentId);
        rejects("UPDATE hr.ShiftAssignment SET AttendanceStatus=NULL,ApprovedBy=?,ApprovedAt=NULL "
                + "WHERE ShiftAssignmentId=?", f.managerA, assignmentId);
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

        String categoryName = unique("schema-category");
        execute("INSERT catalog.Category(Name) VALUES (?)", categoryName);
        int categoryId = scalarInt("SELECT CategoryId FROM catalog.Category WHERE Name=?", categoryName);
        String productName = unique("schema-product");
        execute("INSERT catalog.Product(CategoryId,Name,BasePrice) VALUES (?,?,100)",
                categoryId, productName);
        int productId = scalarInt("SELECT ProductId FROM catalog.Product WHERE Name=?", productName);
        execute("INSERT catalog.BranchMenu(BranchId,ProductId,IsListed,IsTemporarilyUnavailable) "
                + "VALUES (?,?,1,0)", branchA, productId);

        String rawName = unique("raw");
        String preppedName = unique("prepped");
        execute("INSERT catalog.Ingredient(Name,Unit,IngredientType) VALUES (?,N'g','RAW')", rawName);
        execute("INSERT catalog.Ingredient(Name,Unit,IngredientType,ShelfLifeMinutes,PrepYieldQty) "
                + "VALUES (?,N'ml','PREPPED',60,100)", preppedName);
        int rawId = scalarInt("SELECT IngredientId FROM catalog.Ingredient WHERE Name=?", rawName);
        int preppedId = scalarInt("SELECT IngredientId FROM catalog.Ingredient WHERE Name=?", preppedName);

        String groupName = unique("modifier-group");
        execute("INSERT catalog.ModifierGroup(ProductId,Name,SortOrder) VALUES (?,?,1)",
                productId, groupName);
        int groupId = scalarInt("SELECT ModifierGroupId FROM catalog.ModifierGroup WHERE Name=?", groupName);
        String optionName = unique("modifier-option");
        execute("INSERT catalog.ModifierOption(ModifierGroupId,Name) VALUES (?,?)", groupId, optionName);
        int optionId = scalarInt("SELECT ModifierOptionId FROM catalog.ModifierOption "
                + "WHERE ModifierGroupId=? AND Name=?", groupId, optionName);

        return new Fixture(branchA, managerA, managerB, cashierA, cashierB, baristaA, baristaB,
                productId, optionId, rawId, preppedId);
    }

    private int createPendingAssignment(int branchId, int userId) throws SQLException {
        execute("INSERT hr.ShiftAssignment(ShiftName,StartTime,EndTime,UserId,WorkDate,BranchId,"
                        + "CheckInAt,AttendanceStatus) "
                        + "VALUES (N'Contract shift','08:00','12:00',?,?,?,SYSUTCDATETIME(),'PENDING')",
                userId, java.sql.Date.valueOf(LocalDate.now().plusDays(1)), branchId);
        return scalarInt("SELECT MAX(ShiftAssignmentId) FROM hr.ShiftAssignment "
                + "WHERE BranchId=? AND UserId=?", branchId, userId);
    }

    private String wasteInsert() {
        return "INSERT inventory.WasteEntry(BranchId,EventKind,Source,CauseCode,CreatedBy,"
                + "IngredientId,Quantity,WasteType,LoggedBy) "
                + "VALUES (?,'INGREDIENT_WASTE','MANUAL','OTHER',?, ?,1,'SPILL',?)";
    }

    private String receiptInsert() {
        return "INSERT inventory.StockReceiptLine(ReceiptBatchId,BranchId,ReceivedBy,DocumentDate,Status,"
                + "IngredientId,UnitCost,EnteredQuantity,UnitNameAtEntry,FactorToBaseAtEntry) "
                + "VALUES (?,?,?,CONVERT(date,SYSUTCDATETIME()),'DRAFT',?,1,1,N'g',1)";
    }

    private String adjustmentInsert() {
        return "INSERT inventory.StockAdjustment(BranchId,IngredientId,SystemBaseQty,ActualBaseQty,"
                + "AdjustedBy,CountedQuantity,UnitNameAtCount,FactorToBaseAtCount) "
                + "VALUES (?,?,0,1,?,1,N'g',1)";
    }

    private String orderInsert() {
        return "INSERT sales.SalesOrder(BranchId,Source,OrderType,Status,CreatedBy,BusinessDate) "
                + "VALUES (?,'COUNTER','TAKEAWAY','ACTIVE',?,"
                + "CONVERT(date,DATEADD(hour,7,SYSUTCDATETIME())))";
    }

    private String blockPendingUpdate() {
        return "UPDATE catalog.BranchMenu SET BlockStatus='PENDING',BlockReason='OUT_OF_STOCK',"
                + "BlockRequestedBy=?,BlockRequestedAt=SYSUTCDATETIME() "
                + "WHERE BranchId=? AND ProductId=?";
    }

    private String approveAssignmentUpdate() {
        return "UPDATE hr.ShiftAssignment SET AttendanceStatus='APPROVED',ApprovedBy=?,"
                + "ApprovedAt=SYSUTCDATETIME() WHERE ShiftAssignmentId=?";
    }

    private int createBranch() throws SQLException {
        String code = unique("B").substring(0, 12);
        execute("INSERT org.Branch(Code,Name,OpenTime,CloseTime) VALUES (?,N'Schema IT','00:00','23:59')",
                code);
        return scalarInt("SELECT BranchId FROM org.Branch WHERE Code=?", code);
    }

    private int createUser(int branchId, String roleCode) throws SQLException {
        String username = unique("schema-user");
        execute("INSERT iam.UserAccount(Username,PasswordHash,FullName,RoleCode,BranchId) "
                + "VALUES (?,'x',N'Schema IT',?,?)", username, roleCode, branchId);
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

    private String scalarString(String sql, Object... args) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    private void bind(PreparedStatement statement, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
    }

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record Fixture(int branchA, int managerA, int managerB, int cashierA, int cashierB,
                           int baristaA, int baristaB, int productId, int modifierOptionId,
                           int rawIngredientId, int preppedIngredientId) { }
}
