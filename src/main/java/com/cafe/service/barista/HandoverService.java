package com.cafe.service.barista;

import com.cafe.config.DBConnection;
import com.cafe.dao.manager.ShiftHandoverDao;
import com.cafe.dao.shared.OrderItemDao;
import com.cafe.model.ShiftHandover;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** B7 · HandoverService — bàn giao ca: ghi chú + KPI lead-time (read OrderItem StartedAt/DoneAt). */
public class HandoverService {

    private final ShiftHandoverDao handoverDao = new ShiftHandoverDao();
    private final OrderItemDao orderItemDao = new OrderItemDao();

    public List<ShiftHandover> getHandovers(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return handoverDao.findByBranch(conn, branchId);
        }
    }

    public int createHandover(int branchId, String note, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int id = handoverDao.insert(conn, branchId, note, userId);
                conn.commit();
                return id;
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public HandoverKpi getKpi(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            long[] s = orderItemDao.leadTimeStatsToday(conn, branchId);
            return new HandoverKpi(s[0], s[1]);
        }
    }

    /** KPI hôm nay: lead time TB (giây, -1 = chưa có) + số ly đã pha xong. */
    public static class HandoverKpi {
        private final long avgLeadSeconds;
        private final long cupCount;
        public HandoverKpi(long avgLeadSeconds, long cupCount) {
            this.avgLeadSeconds = avgLeadSeconds; this.cupCount = cupCount;
        }
        public long getAvgLeadSeconds() { return avgLeadSeconds; }
        public long getCupCount() { return cupCount; }
        public boolean isHasLead() { return avgLeadSeconds >= 0; }
        /** Hiển thị "m phút s giây" hoặc "—". */
        public String getAvgLeadDisplay() {
            if (avgLeadSeconds < 0) return "—";
            long m = avgLeadSeconds / 60, s = avgLeadSeconds % 60;
            return (m > 0 ? m + " phút " : "") + s + " giây";
        }
    }
}
