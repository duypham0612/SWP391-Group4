package com.cafe.dao.sales;

import com.cafe.common.BusinessException;
import com.cafe.model.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    private static final int PICKUP_CODE_MAX_ATTEMPTS = 5;
    private static final int PICKUP_CODE_MAX_SEQUENCE = 9_999_999;

    private static final String SELECT =
        "SELECT o.OrderId, o.BranchId, o.DiningTableId, o.Source, o.OrderType, o.Status, " +
        "       o.CreatedBy, o.CreatedAt, o.BusinessDate, o.PickupCode, dt.TableNumber " +
        "FROM sales.SalesOrder o " +
        "LEFT JOIN sales.DiningTable dt ON dt.DiningTableId=o.DiningTableId ";

    public int insert(Connection conn, Order o) throws SQLException {
        String pickupPrefix = pickupPrefix(o.getPickupCode());
        for (int attempt = 1; attempt <= PICKUP_CODE_MAX_ATTEMPTS; attempt++) {
            try {
                return insertOnce(conn, o);
            } catch (SQLException e) {
                if (pickupPrefix == null || !isDuplicateKey(e)) throw e;
                if (attempt == PICKUP_CODE_MAX_ATTEMPTS) {
                    throw new BusinessException("Không thể cấp mã pickup do có quá nhiều đơn được tạo đồng thời. Vui lòng thử lại.");
                }
                int nextSequence = reservePickupSequence(conn, o.getBranchId(), o.getBusinessDate());
                o.setPickupCode(formatPickupCode(pickupPrefix, nextSequence));
            }
        }
        throw new BusinessException("Không thể cấp mã pickup. Vui lòng thử lại.");
    }

    private int insertOnce(Connection conn, Order o) throws SQLException {
        final String sql = "INSERT INTO sales.SalesOrder(BranchId,DiningTableId,Source,OrderType,Status,CreatedBy,BusinessDate,PickupCode) " +
                "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, o.getBranchId());
            if (o.getDiningTableId() == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, o.getDiningTableId());
            ps.setString(3, o.getSource());
            ps.setString(4, o.getOrderType() == null ? "DINE_IN" : o.getOrderType());
            ps.setString(5, o.getStatus() == null ? "ACTIVE" : o.getStatus());
            if (o.getCreatedBy() == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, o.getCreatedBy());
            ps.setDate(7, Date.valueOf(o.getBusinessDate()));
            ps.setString(8, o.getPickupCode());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public Order findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE o.OrderId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public List<Order> findByTable(Connection conn, int tableId) throws SQLException {
        List<Order> out = new ArrayList<>();
        final String sql = SELECT + "WHERE o.DiningTableId=? AND EXISTS (" +
                "SELECT 1 FROM sales.OrderItem oi LEFT JOIN payment.Bill b ON b.BillId=oi.BillId " +
                "WHERE oi.OrderId=o.OrderId AND oi.Status<>'CANCELLED' " +
                "AND (oi.BillId IS NULL OR b.Status='UNPAID')) ORDER BY o.CreatedAt";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /**
     * Đơn mang đi chưa thanh toán: chưa có bill hoặc đang có bill UNPAID.
     * Giữ đơn trong danh sách checkout cả khi Cashier đã mở bill rồi rời màn hình.
     */
    public List<Order> findTakeawayAwaitingPaymentByBranch(Connection conn, int branchId) throws SQLException {
        List<Order> out = new ArrayList<>();
        final String sql = SELECT +
                "WHERE o.BranchId=? AND o.OrderType='TAKEAWAY' AND o.Status<>'CANCELLED' " +
                "AND EXISTS (SELECT 1 FROM sales.OrderItem oi " +
                "LEFT JOIN payment.Bill b ON b.BillId=oi.BillId " +
                "WHERE oi.OrderId=o.OrderId AND oi.Status<>'CANCELLED' " +
                "AND (oi.BillId IS NULL OR b.Status='UNPAID')) " +
                "ORDER BY o.CreatedAt DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /**
     * Đơn đang xử lý (ACTIVE) của chi nhánh — cho Order Inbox (Cashier monitor).
     *
     * <p>Đơn TREO (tạo trước mốc đầu ngày kinh doanh) xếp lên ĐẦU, phần còn lại giữ mới-trước như cũ.
     * Sắp thuần "mới nhất trước" đẩy đơn càng cũ càng xuống đáy — đúng chỗ không ai nhìn, trong khi
     * đơn treo lại là loại duy nhất chỉ Thu ngân xử lý được (huỷ &amp; hoàn tiền cho khách đã về).
     *
     * @param businessDayStartUtc mốc đầu ngày kinh doanh; null = không tách nhóm treo.
     */
    public List<Order> findActiveByBranch(Connection conn, int branchId,
                                          java.time.LocalDateTime businessDayStartUtc) throws SQLException {
        List<Order> out = new ArrayList<>();
        String order = businessDayStartUtc == null
                ? "ORDER BY o.CreatedAt DESC"
                : "ORDER BY CASE WHEN o.CreatedAt < ? THEN 0 ELSE 1 END, o.CreatedAt DESC";
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE o.BranchId=? AND o.Status='ACTIVE' " + order)) {
            ps.setInt(1, branchId);
            if (businessDayStartUtc != null) ps.setTimestamp(2, Timestamp.valueOf(businessDayStartUtc));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /**
     * Tính số pickup kế tiếp từ các mã đã có của chi nhánh/ngày kinh doanh.
     * Tên method được giữ để tương thích caller; unique index và retry trong insert() là chốt chống race.
     */
    public int reservePickupSequence(Connection conn, int branchId, LocalDate businessDate)
            throws SQLException {
        final String sql = "SELECT ISNULL(MAX(TRY_CONVERT(int,SUBSTRING(PickupCode,2,7))),0)+1 "
                + "FROM sales.SalesOrder WHERE BranchId=? AND BusinessDate=? AND PickupCode IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(businessDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int nextSequence = rs.getInt(1);
                    if (nextSequence > PICKUP_CODE_MAX_SEQUENCE) {
                        throw new BusinessException("Đã hết dải mã pickup cho ngày kinh doanh này.");
                    }
                    return nextSequence;
                }
            }
        }
        throw new SQLException("Không thể tính mã pickup kế tiếp.");
    }

    private static String pickupPrefix(String pickupCode) {
        if (pickupCode == null || pickupCode.length() < 2) return null;
        int suffixStart = pickupCode.length();
        while (suffixStart > 0 && Character.isDigit(pickupCode.charAt(suffixStart - 1))) suffixStart--;
        return suffixStart == 0 || suffixStart == pickupCode.length() ? null : pickupCode.substring(0, suffixStart);
    }

    private static String formatPickupCode(String prefix, int sequence) {
        String pickupCode = prefix + sequence;
        if (pickupCode.length() > 8) {
            throw new BusinessException("Đã hết dải mã pickup cho ngày kinh doanh này.");
        }
        return pickupCode;
    }

    private static boolean isDuplicateKey(SQLException error) {
        for (SQLException current = error; current != null; current = current.getNextException()) {
            if (current.getErrorCode() == 2601 || current.getErrorCode() == 2627) return true;
        }
        return false;
    }

    public void updateStatus(Connection conn, int orderId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE sales.SalesOrder SET Status=? WHERE OrderId=?")) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    /**
     * ACTIVE → COMPLETED khi MỌI món của đơn đã kết thúc (SERVED/CANCELLED). NGUYÊN TỬ (1 câu UPDATE,
     * WHERE-guard NOT EXISTS chống race khi 2 barista giao món cuối song song). Trả số dòng đổi (0/1):
     * ==1 nghĩa là đơn vừa hoàn tất → caller publish order.status_changed. Không đổi nếu còn món chưa xong.
     */
    public int completeIfAllItemsFinal(Connection conn, int orderId) throws SQLException {
        final String sql =
            "UPDATE sales.SalesOrder SET Status='COMPLETED' " +
            "WHERE OrderId=? AND Status='ACTIVE' AND NOT EXISTS (" +
            "  SELECT 1 FROM sales.OrderItem oi WHERE oi.OrderId=? AND oi.Status NOT IN ('SERVED','CANCELLED'))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, orderId);
            return ps.executeUpdate();
        }
    }

    /** COMPLETED → ACTIVE khi hoàn tác giao (món SERVED quay lại READY). Trả số dòng đổi (0/1). */
    public int reopenIfCompleted(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sales.SalesOrder SET Status='ACTIVE' WHERE OrderId=? AND Status='COMPLETED'")) {
            ps.setInt(1, orderId);
            return ps.executeUpdate();
        }
    }

    private Order map(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getInt("OrderId"));
        o.setBranchId(rs.getInt("BranchId"));
        int tableId = rs.getInt("DiningTableId");
        if (!rs.wasNull()) o.setDiningTableId(tableId);
        o.setSource(rs.getString("Source"));
        o.setOrderType(rs.getString("OrderType"));
        o.setStatus(rs.getString("Status"));
        int cb = rs.getInt("CreatedBy");
        if (!rs.wasNull()) o.setCreatedBy(cb);
        Timestamp ca = rs.getTimestamp("CreatedAt");
        if (ca != null) o.setCreatedAt(ca.toLocalDateTime());
        Date businessDate = rs.getDate("BusinessDate");
        if (businessDate != null) o.setBusinessDate(businessDate.toLocalDate());
        o.setPickupCode(rs.getString("PickupCode"));
        o.setTableNumber(rs.getString("TableNumber"));
        return o;
    }
}
