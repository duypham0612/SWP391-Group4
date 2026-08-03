package com.cafe.service.shared;

import com.cafe.config.DBConnection;
import com.cafe.dao.org.BranchStatusDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Truy vấn trạng thái truy cập chi nhánh cho tầng web. */
public class BranchAccessService {
    private final BranchStatusDao branchStatusDao;

    public BranchAccessService() {
        this(new BranchStatusDao());
    }

    BranchAccessService(BranchStatusDao branchStatusDao) {
        this.branchStatusDao = Objects.requireNonNull(branchStatusDao, "branchStatusDao");
    }

    public Status getStatus(int branchId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            BranchStatusDao.AccessStatus status =
                    branchStatusDao.findAccessStatus(connection, branchId);
            return new Status(status.active(), status.managerAssigned());
        }
    }

    public record Status(boolean active, boolean managerAssigned) { }
}
