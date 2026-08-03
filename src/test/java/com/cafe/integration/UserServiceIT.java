package com.cafe.integration;

import com.cafe.common.BusinessException;
import com.cafe.model.User;
import com.cafe.service.admin.UserService;
import com.cafe.service.auth.AuthService;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceIT extends SqlServerIntegrationSupport {

    @Test
    void create_user_through_service_can_authenticate() throws Exception {
        int branchId = scalarInt("SELECT TOP (1) BranchId FROM org.Branch WHERE IsActive=1 ORDER BY BranchId");
        String roleCode = "BARISTA";
        String username = unique("staff");
        String password = "MvcTest!123";

        int userId = new UserService().createUser(user(username, roleCode, branchId), password);

        assertTrue(userId > 0);
        User authenticated = new AuthService().authenticate(username, password);
        assertNotNull(authenticated);
        assertEquals(userId, authenticated.getUserId());
        assertNull(authenticated.getPasswordHash());
    }

    @Test
    void create_user_translates_duplicate_username_email_and_phone() throws Exception {
        int branchId = scalarInt("SELECT TOP (1) BranchId FROM org.Branch WHERE IsActive=1 ORDER BY BranchId");
        String roleCode = "BARISTA";
        UserService service = new UserService();
        User first = user(unique("duplicate"), roleCode, branchId);
        service.createUser(first, "MvcTest!123");

        User sameUsername = user(first.getUsername(), roleCode, branchId);
        BusinessException usernameError = assertThrows(BusinessException.class,
                () -> service.createUser(sameUsername, "MvcTest!123"));
        assertTrue(usernameError.getMessage().contains("Tên đăng nhập"));

        User sameEmail = user(unique("email"), roleCode, branchId);
        sameEmail.setEmail(first.getEmail());
        BusinessException emailError = assertThrows(BusinessException.class,
                () -> service.createUser(sameEmail, "MvcTest!123"));
        assertTrue(emailError.getMessage().contains("Email"));

        User samePhone = user(unique("phone"), roleCode, branchId);
        samePhone.setPhone(first.getPhone());
        BusinessException phoneError = assertThrows(BusinessException.class,
                () -> service.createUser(samePhone, "MvcTest!123"));
        assertTrue(phoneError.getMessage().contains("Số điện thoại"));
    }

    @Test
    void concurrent_manager_creation_allows_exactly_one_manager_per_branch() throws Exception {
        int branchId = createBranch();
        String managerRoleCode = "BRANCH_MANAGER";
        User first = user(unique("manager"), managerRoleCode, branchId);
        User second = user(unique("manager"), managerRoleCode, branchId);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> a = executor.submit(() -> createManager(first, ready, start));
            Future<Boolean> b = executor.submit(() -> createManager(second, ready, start));
            ready.await();
            start.countDown();
            int successes = (a.get() ? 1 : 0) + (b.get() ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(1, scalarInt(
                    "SELECT COUNT(*) FROM iam.UserAccount WHERE BranchId=? AND RoleCode=?",
                    branchId, managerRoleCode));
            assertTrue(scalarInt(
                    "SELECT COUNT(*) FROM org.Branch WHERE BranchId=? AND ManagerUserId IS NOT NULL",
                    branchId) == 1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void update_user_rechecks_admin_invariant_in_service() throws Exception {
        int branchId = scalarInt("SELECT TOP (1) BranchId FROM org.Branch WHERE IsActive=1 ORDER BY BranchId");
        UserService service = new UserService();
        int userId = service.createUser(user(unique("update"), "BARISTA", branchId), "MvcTest!123");
        User update = service.getUser(userId);
        update.setRoleCode("ADMIN");

        assertThrows(BusinessException.class, () -> service.updateUser(update));
        assertEquals("BARISTA", service.getUser(userId).getRoleCode());
    }

    private boolean createManager(User user, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            new UserService().createUser(user, "MvcTest!123");
            return true;
        } catch (BusinessException expected) {
            return false;
        }
    }

    private User user(String username, String roleCode, int branchId) {
        User user = new User();
        user.setUsername(username);
        user.setFullName("Nhân viên MVC");
        user.setEmail(username + "@example.test");
        user.setPhone(uniquePhone());
        user.setRoleCode(roleCode);
        user.setBranchId(branchId);
        user.setStatus("ACTIVE");
        return user;
    }

    private int createBranch() throws Exception {
        String code = unique("M").substring(0, 12).toUpperCase();
        execute("INSERT org.Branch(Code,Name,OpenTime,CloseTime) VALUES (?,N'MVC IT','07:00','22:00')", code);
        return scalarInt("SELECT BranchId FROM org.Branch WHERE Code=?", code);
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

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String uniquePhone() {
        long value = Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 100_000_000L;
        return String.format("09%08d", value);
    }
}
