package com.cafe.service.customer;
import com.cafe.dao.shared.OutboxEventDao;
import com.cafe.service.shared.OrderService;
import com.cafe.service.shared.CatalogReadService;

import com.cafe.common.EventPublisher;
import com.cafe.common.EventType;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.DiningTableDao;
import com.cafe.dao.cashier.TableSessionDao;
import com.cafe.model.DiningTable;
import com.cafe.model.Order;
import com.cafe.model.OrderItem;
import com.cafe.model.PosMenuItem;
import com.cafe.model.TableSession;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * C7/C8 · QrOrderService — app khách quét QR (ẩn danh). DÙNG LẠI OrderService (Contract #1, #3:
 * đơn QR & đơn quầy cùng một bảng/service). Khách không đăng nhập — session gắn token bàn.
 */
public class QrOrderService {

    private final DiningTableDao tableDao = new DiningTableDao();
    private final TableSessionDao sessionDao = new TableSessionDao();
    private final OutboxEventDao outboxDao = new OutboxEventDao();
    private final CatalogReadService catalogReadService = new CatalogReadService();
    private final OrderService orderService = new OrderService();

    /** Kết quả quét QR: bàn nhận ra được chưa, và phiên bàn đã được THU NGÂN mở chưa. */
    public static class ScanResult {
        public enum Status { INVALID_QR, TABLE_NOT_OPEN, OK }

        private final Status status;
        private final DiningTable table;
        private final TableSession session;

        private ScanResult(Status status, DiningTable table, TableSession session) {
            this.status = status; this.table = table; this.session = session;
        }
        static ScanResult invalid()                       { return new ScanResult(Status.INVALID_QR, null, null); }
        static ScanResult notOpen(DiningTable t)          { return new ScanResult(Status.TABLE_NOT_OPEN, t, null); }
        static ScanResult ok(DiningTable t, TableSession s){ return new ScanResult(Status.OK, t, s); }

        public Status getStatus()      { return status; }
        public DiningTable getTable()  { return table; }
        public TableSession getSession() { return session; }
        public boolean isOk()          { return status == Status.OK; }
    }

    /**
     * Nhận diện bàn từ mã QR và GẮN khách vào phiên bàn đang mở.
     *
     * Nghiệp vụ (Contract #3 — Cashier sở hữu order entry): khách quét QR KHÔNG mở được bàn.
     * Thu ngân phải mở bàn trước (/cashier/table → openTable) thì QR mới đặt món được; nếu không,
     * người đi ngang hoặc ảnh chụp mã QR cũng tạo được phiên và đẩy bàn sang OCCUPIED,
     * làm sơ đồ bàn của quầy sai và sinh phiên rác không ai chịu trách nhiệm.
     */
    public ScanResult scan(String qrCode) throws SQLException {
        if (qrCode == null || qrCode.isBlank()) return ScanResult.invalid();
        try (Connection c = DBConnection.getConnection()) {
            DiningTable t = tableDao.findByQrCode(c, qrCode);
            if (t == null) return ScanResult.invalid();
            TableSession open = sessionDao.findOpenByTable(c, t.getDiningTableId());
            return open == null ? ScanResult.notOpen(t) : ScanResult.ok(t, open);
        }
    }

    /**
     * Phiên còn nhận đơn của khách không. Chặn đặt món vào phiên thu ngân đã chốt bill/đóng bàn
     * trong lúc tab QR của khách vẫn mở.
     */
    public boolean isSessionOrderable(int sessionId) throws SQLException {
        TableSession s = getSession(sessionId);
        return s != null && "OPEN".equals(s.getStatus());
    }

    /**
     * Khách xin quầy mở bàn (bàn chưa có phiên). Ghi ops.OutboxEvent để hiện lên sơ đồ bàn thu ngân.
     * Idempotent: đã có yêu cầu treo cho bàn thì không ghi thêm.
     */
    public void requestTableOpen(int branchId, int tableId, String tableNumber) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (!outboxDao.hasPendingOpenRequest(c, tableId)) {
                    EventPublisher.publish(c, EventType.TABLE_OPEN_REQUESTED, String.valueOf(tableId), branchId,
                            "{\"tableId\":" + tableId + ",\"tableNumber\":\"" + esc(tableNumber) + "\"}");
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public TableSession getSession(int sessionId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return sessionDao.findById(c, sessionId); }
    }

    public List<PosMenuItem> getMenu(int branchId) throws SQLException {
        return catalogReadService.getPosMenu(branchId);
    }

    /** Đặt món QR — uỷ thác OrderService.placeOrder(source=QR), publish order.created. */
    public int placeCustomerOrder(int branchId, int sessionId, List<OrderService.CartLine> lines) throws SQLException {
        return orderService.placeOrder(branchId, sessionId, "QR", "DINE_IN", null, lines);
    }

    public List<OrderItem> getSessionStatuses(int sessionId) throws SQLException {
        return orderService.getSessionItemStatuses(sessionId);
    }

    /** R5 · Đơn của phiên còn huỷ được (mọi món WAITING — barista chưa pha). Để hiện nút huỷ cho khách. */
    public List<Order> getCancellableOrders(int sessionId) throws SQLException {
        List<Order> out = new ArrayList<>();
        for (Order o : orderService.getSessionOrders(sessionId)) {
            if ("ACTIVE".equals(o.getStatus()) && o.isCancellable()) out.add(o);
        }
        return out;
    }

    /**
     * R5 · Khách huỷ đơn — uỷ thác OrderService.voidOrder (cùng guard: chỉ huỷ khi chưa pha).
     * Bắt buộc truyền sessionId của chính khách: đơn phải thuộc phiên đó, nếu không khách bàn này
     * huỷ được đơn bàn khác chỉ bằng cách đổi orderId.
     */
    public boolean cancelOrder(int sessionId, int orderId) throws SQLException {
        boolean owned = false;
        for (Order o : orderService.getSessionOrders(sessionId)) {
            if (o.getOrderId() == orderId) { owned = true; break; }
        }
        return owned && orderService.voidOrder(orderId, null);
    }

    public void callStaff(int sessionId, int branchId) throws SQLException {
        publish(EventType.SERVICE_CALL, sessionId, branchId);
    }

    public void requestBill(int sessionId, int branchId) throws SQLException {
        publish(EventType.BILL_REQUESTED, sessionId, branchId);
    }

    private void publish(EventType type, int sessionId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                EventPublisher.publish(c, type, String.valueOf(sessionId), branchId,
                        "{\"sessionId\":" + sessionId + "}");
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }
}
