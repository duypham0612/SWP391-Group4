package com.cafe.service.customer;
import com.cafe.service.cashier.TableSessionService;
import com.cafe.service.shared.OrderService;
import com.cafe.service.shared.CatalogReadService;

import com.cafe.common.EventPublisher;
import com.cafe.common.EventType;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.DiningTableDao;
import com.cafe.dao.cashier.TableSessionDao;
import com.cafe.model.DiningTable;
import com.cafe.model.OrderItem;
import com.cafe.model.PosMenuItem;
import com.cafe.model.TableSession;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * C7/C8 · QrOrderService — app khách quét QR (ẩn danh). DÙNG LẠI OrderService (Contract #1, #3:
 * đơn QR & đơn quầy cùng một bảng/service). Khách không đăng nhập — session gắn token bàn.
 */
public class QrOrderService {

    private final DiningTableDao tableDao = new DiningTableDao();
    private final TableSessionDao sessionDao = new TableSessionDao();
    private final TableSessionService tableSessionService = new TableSessionService();
    private final CatalogReadService catalogReadService = new CatalogReadService();
    private final OrderService orderService = new OrderService();

    /** Nhận diện bàn từ mã QR → mở/lấy phiên ẩn danh (OpenedBy=null). null nếu QR sai. */
    public TableSession identifyTable(String qrCode) throws SQLException {
        if (qrCode == null || qrCode.isBlank()) return null;
        DiningTable t;
        try (Connection c = DBConnection.getConnection()) {
            t = tableDao.findByQrCode(c, qrCode);
        }
        if (t == null) return null;
        int sessionId = tableSessionService.openSession(t.getBranchId(), t.getDiningTableId(), null);
        return getSession(sessionId);
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
