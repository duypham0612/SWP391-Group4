package com.cafe.service.customer;

import com.cafe.common.EventType;
import com.cafe.config.DBConnection;
import com.cafe.config.Tx;
import com.cafe.dao.cashier.DiningTableDao;
import com.cafe.dao.shared.OutboxEventDao;
import com.cafe.model.DiningTable;
import com.cafe.model.Order;
import com.cafe.model.OrderItem;
import com.cafe.model.PosMenuItem;
import com.cafe.service.shared.CatalogReadService;
import com.cafe.model.CartLine;
import com.cafe.service.shared.OrderIssueService;
import com.cafe.service.shared.OrderPlacementService;
import com.cafe.service.shared.OrderQueryService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** App khách quét QR; HTTP session chỉ giữ tableId, không còn phiên bàn trong DB. */
public class QrOrderService {
    private final DiningTableDao tableDao;
    private final OutboxEventDao outboxDao;
    private final CatalogReadService catalogReadService;
    private final OrderPlacementService orderPlacement;
    private final OrderQueryService orderQuery;
    private final OrderIssueService orderIssues;

    public QrOrderService() {
        this(new DiningTableDao(), new OutboxEventDao(), new CatalogReadService(),
                new OrderPlacementService(), new OrderQueryService(), new OrderIssueService());
    }

    public QrOrderService(DiningTableDao tableDao, OutboxEventDao outboxDao,
                          CatalogReadService catalogReadService, OrderPlacementService orderPlacement,
                          OrderQueryService orderQuery, OrderIssueService orderIssues) {
        this.tableDao = java.util.Objects.requireNonNull(tableDao);
        this.outboxDao = java.util.Objects.requireNonNull(outboxDao);
        this.catalogReadService = java.util.Objects.requireNonNull(catalogReadService);
        this.orderPlacement = java.util.Objects.requireNonNull(orderPlacement);
        this.orderQuery = java.util.Objects.requireNonNull(orderQuery);
        this.orderIssues = java.util.Objects.requireNonNull(orderIssues);
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
        Tx.run(conn -> {
            DiningTable table = tableDao.findByIdForUpdate(conn, tableId);
            if (table == null || table.getBranchId() != branchId) {
                throw new IllegalArgumentException("Bàn không thuộc chi nhánh tương ứng.");
            }
            if ("EMPTY".equals(table.getStatus()) && !outboxDao.hasPendingOpenRequest(conn, tableId)) {
                outboxDao.insert(conn, EventType.TABLE_OPEN_REQUESTED, String.valueOf(tableId), branchId,
                        "{\"tableId\":" + tableId + ",\"tableNumber\":\"" + esc(tableNumber) + "\"}");
            }
        });
    }

    public DiningTable getTable(int tableId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return tableDao.findById(conn, tableId); }
    }

    public List<PosMenuItem> getMenu(int branchId) throws SQLException {
        return catalogReadService.getPosMenu(branchId);
    }

    public int placeCustomerOrder(int branchId, int tableId, List<CartLine> lines)
            throws SQLException {
        return orderPlacement.placeOrder(branchId, tableId, "QR", "DINE_IN", null, lines);
    }

    public List<OrderItem> getTableStatuses(int tableId) throws SQLException {
        return orderQuery.getTableItemStatuses(tableId);
    }

    public List<Order> getCancellableOrders(int tableId) throws SQLException {
        List<Order> out = new ArrayList<>();
        for (Order order : orderQuery.getTableOrders(tableId)) {
            if ("ACTIVE".equals(order.getStatus()) && order.isCancellable()) out.add(order);
        }
        return out;
    }

    public boolean cancelOrder(int tableId, int orderId) throws SQLException {
        Integer branchId = null;
        for (Order order : orderQuery.getTableOrders(tableId)) {
            if (order.getOrderId() == orderId) {
                branchId = order.getBranchId();
                break;
            }
        }
        return branchId != null && orderIssues.voidOrder(orderId, null, branchId);
    }

    public void callStaff(int tableId, int branchId) throws SQLException {
        publishTableSignal(EventType.SERVICE_CALL, tableId, branchId);
    }

    public void requestBill(int tableId, int branchId) throws SQLException {
        publishTableSignal(EventType.BILL_REQUESTED, tableId, branchId);
    }

    private void publishTableSignal(EventType type, int tableId, int branchId) throws SQLException {
        Tx.run(conn -> {
            // Khoá bàn tới hết transaction để không ghi tín hiệu sau khi payment vừa quyết định trả bàn.
            DiningTable table = tableDao.findByIdForUpdate(conn, tableId);
            if (table == null || table.getBranchId() != branchId
                    || !"OCCUPIED".equals(table.getStatus())
                    || !tableDao.hasUnpaidOrders(conn, tableId, branchId)) {
                throw new IllegalStateException("Bàn không còn đơn chưa thanh toán.");
            }
            outboxDao.insert(conn, type, String.valueOf(tableId), branchId,
                    "{\"tableId\":" + tableId + "}");
        });
    }

    private static String esc(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
