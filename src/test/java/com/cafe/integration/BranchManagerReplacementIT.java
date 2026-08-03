package com.cafe.integration;

import com.cafe.common.BusinessException;
import com.cafe.model.Branch;
import com.cafe.model.User;
import com.cafe.service.admin.BranchService;
import com.cafe.service.admin.UserService;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BranchManagerReplacementIT extends SqlServerIntegrationSupport {

    @Test
    void replace_manager_updates_all_three_states_in_one_transaction() throws Exception {
        int branchId = createBranch();
        UserService users = new UserService();
        int previousId = users.createUser(user("oldmanager", "BRANCH_MANAGER", branchId), "MvcTest!123");
        int replacementId = users.createUser(user("replacement", "BARISTA", branchId), "MvcTest!123");

        BranchService.ManagerReplacement result =
                new BranchService().replaceManager(branchId, replacementId);

        assertEquals(previousId, result.previousManagerId());
        assertEquals(replacementId, result.newManagerId());
        assertEquals(replacementId, scalarInt(
                "SELECT ManagerUserId FROM org.Branch WHERE BranchId=?", branchId));
        assertEquals("BRANCH_MANAGER", scalarString(
                "SELECT RoleCode FROM iam.UserAccount WHERE UserId=?", replacementId));
        assertEquals("LOCKED", scalarString(
                "SELECT Status FROM iam.UserAccount WHERE UserId=?", previousId));
        assertEquals(branchId, scalarInt(
                "SELECT BranchId FROM iam.UserAccount WHERE UserId=?", replacementId));
        assertEquals(branchId, scalarInt(
                "SELECT BranchId FROM iam.UserAccount WHERE UserId=?", previousId));
    }

    @Test
    void replace_manager_rolls_back_when_replacement_has_open_cashier_shift() throws Exception {
        int branchId = createBranch();
        UserService users = new UserService();
        int previousId = users.createUser(user("oldmanager", "BRANCH_MANAGER", branchId), "MvcTest!123");
        int replacementId = users.createUser(user("cashier", "CASHIER", branchId), "MvcTest!123");
        execute("INSERT payment.CashierShift(BranchId,CashierId,OpeningCash,OpenedAt) "
                + "VALUES (?,?,0,SYSUTCDATETIME())", branchId, replacementId);

        BusinessException error = assertThrows(BusinessException.class,
                () -> new BranchService().replaceManager(branchId, replacementId));

        assertEquals(true, error.getMessage().contains("ca thu ngân đang mở"));
        assertEquals(previousId, scalarInt(
                "SELECT ManagerUserId FROM org.Branch WHERE BranchId=?", branchId));
        assertEquals("CASHIER", scalarString(
                "SELECT RoleCode FROM iam.UserAccount WHERE UserId=?", replacementId));
        assertEquals("ACTIVE", scalarString(
                "SELECT Status FROM iam.UserAccount WHERE UserId=?", previousId));
    }

    private int createBranch() throws Exception {
        Branch branch = new Branch();
        branch.setName("Chi nhánh thay quản lý " + UUID.randomUUID().toString().substring(0, 8));
        branch.setAddress("Địa chỉ kiểm thử transaction");
        branch.setActive(true);
        return new BranchService().createBranch(branch);
    }

    private User user(String prefix, String roleCode, int branchId) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        User user = new User();
        user.setUsername(prefix + suffix);
        user.setFullName("Nhân sự thay quản lý");
        user.setEmail(prefix + suffix + "@example.test");
        user.setPhone("09" + String.format("%08d",
                Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 100_000_000L));
        user.setRoleCode(roleCode);
        user.setBranchId(branchId);
        user.setStatus("ACTIVE");
        return user;
    }

    private void execute(String sql, Object... args) throws Exception {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            statement.executeUpdate();
        }
    }

    private int scalarInt(String sql, Object... args) throws Exception {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    private String scalarString(String sql, Object... args) throws Exception {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }
}
