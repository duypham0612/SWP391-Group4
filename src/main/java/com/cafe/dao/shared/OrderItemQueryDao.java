package com.cafe.dao.shared;

import com.cafe.model.OrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Các truy vấn ĐỌC danh sách món của {@code sales.OrderItem} — mỗi method phục vụ đúng một màn.
 *
 * <p>Tách khỏi {@link OrderItemDao} để một thay đổi ở đây đọc ra ngay là "đổi cái màn nào đó thấy",
 * không lẫn với các câu UPDATE đổi trạng thái. Hình dạng dòng vẫn dùng chung
 * {@link OrderItemDao#SELECT} và {@link OrderItemDao#map} — cột chỉ có một nguồn.
 *
 * <p>Tra cứu lẻ ({@code findById}, {@code findByOrder}) KHÔNG ở đây mà ở lõi, vì mọi luồng ghi đều
 * phải đọc lại món trước khi đổi trạng thái — bắt chúng giữ thêm DAO này chỉ để đọc một dòng là thừa.
 */
public class OrderItemQueryDao {

    /** Số liệu tổng hợp cho bảng điều khiển quầy pha chế, suy ra từ {@code sales.OrderItem}. */
    public record PreparationMetrics(
            int myMakingCups,
            int myCompletedCups,
            long myAveragePreparationSeconds,
            int branchWaitingCups,
            int branchMakingCups,
            int branchReadyCups,
            int branchBlockedCups) {
    }

    /**
     * Gộp 7 con số của bảng điều khiển vào MỘT truy vấn thay vì 7 lần quét bảng.
     * Không dùng {@link OrderItemDao#SELECT} vì đây là truy vấn tổng hợp, không trả dòng món.
     */
    public PreparationMetrics loadPreparationMetrics(Connection conn, int branchId, int userId,
                                                      java.time.LocalDateTime businessDayStartUtc)
            throws SQLException {
        final String sql = "SELECT "
            + "COALESCE(SUM(CASE WHEN oi.Status='MAKING' AND oi.BaristaId=? THEN oi.Quantity ELSE 0 END),0) AS MyMakingCups, "
            + "COALESCE(SUM(CASE WHEN oi.PreparedBy=? AND oi.DoneAt>=? THEN oi.Quantity ELSE 0 END),0) AS MyCompletedCups, "
            + "COALESCE(AVG(CASE WHEN oi.PreparedBy=? AND oi.DoneAt>=? AND oi.StartedAt IS NOT NULL "
            + "THEN CONVERT(BIGINT,DATEDIFF(SECOND, oi.StartedAt, oi.DoneAt)) END),0) AS MyAvgPrepSeconds, "
            + "COALESCE(SUM(CASE WHEN o.Status='ACTIVE' AND oi.Status='WAITING' THEN oi.Quantity ELSE 0 END),0) AS WaitingCups, "
            + "COALESCE(SUM(CASE WHEN o.Status='ACTIVE' AND oi.Status='MAKING' THEN oi.Quantity ELSE 0 END),0) AS MakingCups, "
            + "COALESCE(SUM(CASE WHEN o.Status='ACTIVE' AND oi.Status='READY' THEN oi.Quantity ELSE 0 END),0) AS ReadyCups, "
            + "COALESCE(SUM(CASE WHEN o.Status='ACTIVE' AND oi.Status='BLOCKED' THEN oi.Quantity ELSE 0 END),0) AS BlockedCups "
            + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
            + "WHERE o.BranchId=? AND o.CreatedAt>=? "
            + "AND oi.Status IN ('WAITING','MAKING','READY','PICKED_UP','SERVED','BLOCKED')";
        Timestamp from = Timestamp.valueOf(businessDayStartUtc);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setTimestamp(3, from);
            ps.setInt(4, userId);
            ps.setTimestamp(5, from);
            ps.setInt(6, branchId);
            ps.setTimestamp(7, from);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new PreparationMetrics(0, 0, 0, 0, 0, 0, 0);
                return new PreparationMetrics(
                    rs.getInt("MyMakingCups"),
                    rs.getInt("MyCompletedCups"),
                    rs.getLong("MyAvgPrepSeconds"),
                    rs.getInt("WaitingCups"),
                    rs.getInt("MakingCups"),
                    rs.getInt("ReadyCups"),
                    rs.getInt("BlockedCups"));
            }
        }
    }

    /**
     * Quầy pha chế. Mỗi phần tử là đúng một dòng món, không gom theo đơn.
     * BLOCKED nằm trong danh sách để món bị chặn vẫn hiện ở khu "Cần xử lý" —
     * bỏ ra thì món biến mất khỏi mọi màn barista và khách chờ mãi không ai biết.
     */
    public List<OrderItem> findBaristaWorkbench(Connection conn, int branchId) throws SQLException {
        return findBaristaWorkbench(conn, branchId, null);
    }

    /**
     * @param businessDayStartUtc mốc đầu ngày kinh doanh; món tạo TRƯỚC mốc này bị loại khỏi hàng
     *        chờ hiện tại, để đơn sót từ hôm trước không lẫn vào việc của ca đang chạy.
     *        Truyền null để lấy tất cả.
     *
     * <p>Thứ tự pha: món phải làm lại lên đầu (khách đã đợi trọn một lượt rồi mới phải đợi lại),
     * phần còn lại thuần FIFO theo giờ vào đơn — đặt trước thì pha trước. Cột {@code Priority}
     * CỐ Ý không tham gia sắp xếp: nó chỉ do {@link OrderItemIssueDao#finishRemake} ghi, mà món
     * làm lại đã được nhánh {@code RemakeCount} lo rồi.
     */
    public List<OrderItem> findBaristaWorkbench(Connection conn, int branchId,
                                                java.time.LocalDateTime businessDayStartUtc) throws SQLException {
        final String sql = OrderItemDao.SELECT +
            "WHERE o.BranchId=? AND o.Status='ACTIVE' AND oi.Status IN ('WAITING','MAKING','READY','BLOCKED') " +
            (businessDayStartUtc == null ? "" : "AND o.CreatedAt >= ? ") +
            "ORDER BY CASE WHEN oi.RemakeCount>0 THEN 0 ELSE 1 END, o.CreatedAt, oi.OrderItemId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            if (businessDayStartUtc != null) ps.setTimestamp(2, Timestamp.valueOf(businessDayStartUtc));
            return OrderItemDao.mapAll(ps);
        }
    }

    /** Bảng lấy món: READY của chi nhánh, đơn còn ACTIVE. Cũ nhất trước (tie-break theo Id cho ổn định). */
    public List<OrderItem> findReady(Connection conn, int branchId) throws SQLException {
        final String sql = OrderItemDao.SELECT +
            "WHERE o.BranchId=? AND o.Status='ACTIVE' AND oi.Status='READY' " +
            "ORDER BY oi.DoneAt, oi.OrderItemId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            return OrderItemDao.mapAll(ps);
        }
    }

    /** B2 · Món vừa giao gần đây (SERVED, ServedAt trong {@code minutes} phút) để hoàn tác giao nhầm. Mới giao trước. */
    public List<OrderItem> findRecentlyServed(Connection conn, int branchId, int minutes) throws SQLException {
        final String sql = OrderItemDao.SELECT +
            "WHERE o.BranchId=? AND oi.Status='SERVED' AND oi.ServedAt IS NOT NULL " +
            "  AND oi.ServedAt >= DATEADD(MINUTE, ?, SYSUTCDATETIME()) " +
            "ORDER BY oi.ServedAt DESC, oi.OrderItemId DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, -Math.abs(minutes));
            return OrderItemDao.mapAll(ps);
        }
    }

    /** Món nhân viên đã nhận khỏi quầy, đang mang giao khách. */
    public List<OrderItem> findPickedUp(Connection conn, int branchId) throws SQLException {
        final String sql = OrderItemDao.SELECT + "WHERE o.BranchId=? AND o.Status='ACTIVE' AND oi.Status='PICKED_UP' "
                + "ORDER BY oi.PickedUpAt,oi.OrderItemId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            return OrderItemDao.mapAll(ps);
        }
    }

    /** Các món đang mở (chưa SERVED/CANCELLED) của một danh sách đơn — gom 1 query cho màn Pickup (tránh N+1). */
    public List<OrderItem> findByOrders(Connection conn, List<Integer> orderIds) throws SQLException {
        if (orderIds == null || orderIds.isEmpty()) return new ArrayList<>();
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < orderIds.size(); i++) in.append(i == 0 ? "?" : ",?");
        final String sql = OrderItemDao.SELECT + "WHERE oi.OrderId IN (" + in + ") ORDER BY oi.OrderId, oi.OrderItemId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Integer id : orderIds) ps.setInt(idx++, id);
            return OrderItemDao.mapAll(ps);
        }
    }

    /** Các món của các đơn tại một bàn (cho khách theo dõi). */
    public List<OrderItem> findByTable(Connection conn, int tableId) throws SQLException {
        final String sql = OrderItemDao.SELECT + "WHERE o.DiningTableId=? AND EXISTS (" +
                "SELECT 1 FROM sales.OrderItem currentItem " +
                "LEFT JOIN payment.Bill currentBill ON currentBill.BillId=currentItem.BillId " +
                "WHERE currentItem.OrderId=o.OrderId AND currentItem.Status<>'CANCELLED' " +
                "AND (currentItem.BillId IS NULL OR currentBill.Status='UNPAID')) " +
                "ORDER BY oi.OrderItemId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            return OrderItemDao.mapAll(ps);
        }
    }
}
