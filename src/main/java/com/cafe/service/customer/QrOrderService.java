package com.cafe.service.customer;

import com.cafe.common.EventType;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.DiningTableDao;
import com.cafe.dao.shared.OutboxEventDao;
import com.cafe.model.DiningTable;
import com.cafe.model.Order;
import com.cafe.model.OrderItem;
import com.cafe.model.PosMenuItem;
import com.cafe.service.shared.CatalogReadService;
import com.cafe.service.shared.OrderService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** App khách quét QR; HTTP session chỉ giữ tableId, không còn phiên bàn trong DB. */
public class QrOrderService {
    private final DiningTableDao tableDao;
    private final OutboxEventDao outboxDao;
    private final CatalogReadService catalogReadService;
    private final OrderService orderService;

    public QrOrderService() {
        this(new DiningTableDao(), new OutboxEventDao(), new CatalogReadService(), new OrderService());
    }

    public QrOrderService(DiningTableDao tableDao, OutboxEventDao outboxDao,
                          CatalogReadService catalogReadService, OrderService orderService) {
        this.tableDao = java.util.Objects.requireNonNull(tableDao);
        this.outboxDao = java.util.Objects.requireNonNull(outboxDao);
        this.catalogReadService = java.util.Objects.requireNonNull(catalogReadService);
        this.orderService = java.util.Objects.requireNonNull(orderService);
    }

    public static class ScanResult {
        public enum Status { INVALID_QR, TABLE_NOT_OPEN, OK }
        private final Status status;
        private final DiningTable table;

        private ScanResult(Status status, DiningTable table) {
            this.status = status;
            this.table = table;
        }
        static ScanResult invalid() { return new ScanResult(Status.INVALID_QR, null); }
        static ScanResult notOpen(DiningTable table) { return new ScanResult(Status.TABLE_NOT_OPEN, table); }
        static ScanResult ok(DiningTable table) { return new ScanResult(Status.OK, table); }
        public Status getStatus() { return status; }
        public DiningTable getTable() { return table; }
        public boolean isOk() { return status == Status.OK; }
    }

    public ScanResult scan(String qrCode) throws SQLException {
        if (qrCode == null || qrCode.isBlank()) return ScanResult.invalid();
        try (Connection conn = DBConnection.getConnection()) {
            DiningTable table = tableDao.findByQrCode(conn, qrCode);
            if (table == null) return ScanResult.invalid();
            return "OCCUPIED".equals(table.getStatus())
                    ? ScanResult.ok(table) : ScanResult.notOpen(table);
        }
    }

    public boolean isTableOrderable(int tableId, int branchId) throws SQLException {
        DiningTable table = getTable(tableId);
        return table != null && table.getBranchId() == branchId
                && "OCCUPIED".equals(table.getStatus());
    }

    public void requestTableOpen(int branchId, int tableId, String tableNumber) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                DiningTable table = tableDao.findByIdForUpdate(conn, tableId);
                if (table == null || table.getBranchId() != branchId) {
                    throw new IllegalArgumentException("Bàn không thuộc chi nhánh tương ứng.");
                }
                if ("EMPTY".equals(table.getStatus()) && !outboxDao.hasPendingOpenRequest(conn, tableId)) {
                    outboxDao.insert(conn, EventType.TABLE_OPEN_REQUESTED, String.valueOf(tableId), branchId,
                            "{\"tableId\":" + tableId + ",\"tableNumber\":\"" + esc(tableNumber) + "\"}");
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public DiningTable getTable(int tableId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return tableDao.findById(conn, tableId); }
    }

    public List<PosMenuItem> getMenu(int branchId) throws SQLException {
        return catalogReadService.getPosMenu(branchId);
    }

    public int placeCustomerOrder(int branchId, int tableId, List<OrderService.CartLine> lines)
            throws SQLException {
        return orderService.placeOrder(branchId, tableId, "QR", "DINE_IN", null, lines);
    }

    public List<OrderItem> getTableStatuses(int tableId) throws SQLException {
        return orderService.getTableItemStatuses(tableId);
    }

    public List<Order> getCancellableOrders(int tableId) throws SQLException {
        List<Order> out = new ArrayList<>();
        for (Order order : orderService.getTableOrders(tableId)) {
            if ("ACTIVE".equals(order.getStatus()) && order.isCancellable()) out.add(order);
        }
        return out;
    }

    public boolean cancelOrder(int tableId, int orderId) throws SQLException {
        Integer branchId = null;
        for (Order order : orderService.getTableOrders(tableId)) {
            if (order.getOrderId() == orderId) {
                branchId = order.getBranchId();
                break;
            }
        }
        return branchId != null && orderService.voidOrder(orderId, null, branchId);
    }

    public void callStaff(int tableId, int branchId) throws SQLException {
        publishTableSignal(EventType.SERVICE_CALL, tableId, branchId);
    }

    public void requestBill(int tableId, int branchId) throws SQLException {
        publishTableSignal(EventType.BILL_REQUESTED, tableId, branchId);
    }

    private void publishTableSignal(EventType type, int tableId, int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Khoá bàn tới hết transaction để không ghi tín hiệu sau khi payment vừa quyết định trả bàn.
                DiningTable table = tableDao.findByIdForUpdate(conn, tableId);
                if (table == null || table.getBranchId() != branchId
                        || !"OCCUPIED".equals(table.getStatus())
                        || !tableDao.hasUnpaidOrders(conn, tableId, branchId)) {
                    throw new IllegalStateException("Bàn không còn đơn chưa thanh toán.");
                }
                outboxDao.insert(conn, type, String.valueOf(tableId), branchId,
                        "{\"tableId\":" + tableId + "}");
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static String esc(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
