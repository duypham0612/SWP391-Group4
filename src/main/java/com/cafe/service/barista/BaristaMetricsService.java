package com.cafe.service.barista;

import com.cafe.config.DBConnection;
import com.cafe.dao.shared.OrderItemDao;
import com.cafe.dao.shared.PrepBatchDao;
import com.cafe.dao.shared.WasteEventDao;
import com.cafe.model.BaristaOpsSnapshot;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** Facade read-only cho KPI vận hành Barista. */
public class BaristaMetricsService {
    private final OrderItemDao orderItemDao = new OrderItemDao();
    private final WasteEventDao wasteEventDao = new WasteEventDao();
    private final PrepBatchDao prepBatchDao = new PrepBatchDao();

    public BaristaOpsSnapshot load(int branchId, int userId, LocalDateTime businessDayStartUtc) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            OrderItemDao.PreparationMetrics preparation =
                    orderItemDao.loadPreparationMetrics(conn, branchId, userId, businessDayStartUtc);
            WasteEventDao.EventMetrics waste =
                    wasteEventDao.loadMetrics(conn, branchId, userId, businessDayStartUtc);

            BaristaOpsSnapshot out = new BaristaOpsSnapshot();
            out.setMyMakingCups(preparation.myMakingCups());
            out.setMyCompletedCups(preparation.myCompletedCups());
            out.setMyAveragePreparationSeconds(preparation.myAveragePreparationSeconds());
            out.setBranchWaitingCups(preparation.branchWaitingCups());
            out.setBranchMakingCups(preparation.branchMakingCups());
            out.setBranchReadyCups(preparation.branchReadyCups());
            out.setBranchBlockedCups(preparation.branchBlockedCups());
            out.setMyRemakeCount(waste.myRemakes());
            out.setMyWasteCount(waste.myIngredientWastes());
            out.setBranchRemakeCount(waste.branchRemakes());
            out.setExpiredPrepBatchCount(prepBatchDao.countExpiredActive(conn, branchId));
            return out;
        }
    }
}
