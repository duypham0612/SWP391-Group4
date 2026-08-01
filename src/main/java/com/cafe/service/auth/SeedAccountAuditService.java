package com.cafe.service.auth;

import com.cafe.config.DBConnection;
import com.cafe.dao.admin.UserDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Lifecycle use case cho tài khoản seed và connection pool. */
public class SeedAccountAuditService {
    private final UserDao userDao;

    public SeedAccountAuditService() {
        this(new UserDao());
    }

    SeedAccountAuditService(UserDao userDao) {
        this.userDao = Objects.requireNonNull(userDao, "userDao");
    }

    public int countUsersWithoutRealHash() throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            return userDao.findUsersWithoutRealHash(connection).size();
        }
    }

    public void closeDatabasePool() {
        DBConnection.close();
    }
}
