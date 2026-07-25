package com.cafe.service.barista;

import com.cafe.config.DBConnection;
import com.cafe.dao.barista.BaristaMetricsDao;
import com.cafe.model.BaristaOpsSnapshot;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** Facade read-only cho KPI vận hành Barista. */
public class BaristaMetricsService {
    private final BaristaMetricsDao dao = new BaristaMetricsDao();

    public BaristaOpsSnapshot load(int branchId, int userId, LocalDateTime businessDayStartUtc) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return dao.load(conn, branchId, userId, businessDayStartUtc);
        }
    }
}
