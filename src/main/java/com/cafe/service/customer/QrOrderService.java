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
    private final TableSessionService tableSessionService = new TableSessionService();
    private final CatalogReadService catalogReadService = new CatalogReadService();
    private final OrderService orderService = new OrderService();

    /** Nhận diện bàn từ mã QR → mở/lấy phiên ẩn danh (OpenedBy=null). null nếu QR sai. */
    public TableSession identifyTable(String qrCode) throws SQLException {
        if (qrCode == null || qrCode.isBlank()) return null;
        DiningTable t;
        try (Connection c = DBConnection.getConnection()) {
            t = tableDao.findByQrCode(c, qrCode);
            if (t != null && t.isMerged()) t = tableDao.findById(c, t.getMergedIntoTableId());
        }
        if (t == null || !t.isVisible() || "CLEANING".equals(t.getStatus())) return null;
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
        requireOpenSession(sessionId, branchId);
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

    /** R5 · Khách huỷ đơn — uỷ thác OrderService.voidOrder (cùng guard: chỉ huỷ khi chưa pha). */
    public boolean cancelOrder(int sessionId, int orderId) throws SQLException {
        TableSession session = getSession(sessionId);
        if (session == null) return false;
        Order order = orderService.getOrder(orderId);
        if (order == null || order.getTableSessionId() == null
                || order.getTableSessionId() != sessionId || !"QR".equals(order.getSource())) return false;
        return orderService.voidOrder(orderId, null);
    }

    public void callStaff(int sessionId) throws SQLException {
        TableSession session = requireOpenSession(sessionId, null);
        publish(EventType.SERVICE_CALL, sessionId, session.getBranchId());
    }

    public void requestBill(int sessionId) throws SQLException {
        TableSession session = requireOpenSession(sessionId, null);
        publish(EventType.BILL_REQUESTED, sessionId, session.getBranchId());
    }

    private TableSession requireOpenSession(int sessionId, Integer expectedBranchId) throws SQLException {
        TableSession session = getSession(sessionId);
        if (session == null || !"OPEN".equals(session.getStatus())
                || expectedBranchId != null && session.getBranchId() != expectedBranchId) {
            throw new IllegalStateException("Phiên QR không còn hợp lệ. Vui lòng quét lại mã tại bàn.");
        }
        return session;
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
