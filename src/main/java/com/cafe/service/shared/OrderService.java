package com.cafe.service.shared;

import com.cafe.model.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Facade tương thích cho toàn bộ vòng đời đơn hàng — KHÔNG chứa logic nghiệp vụ.
 *
 * <p>Mọi method dưới đây chỉ chuyển tiếp sang một trong năm service chuyên trách:
 * <ul>
 *   <li>{@link OrderPlacementService} — đặt món (POS, QR)</li>
 *   <li>{@link OrderQueryService} — mọi truy vấn đọc</li>
 *   <li>{@link KdsOrderWorkflowService} — quầy pha chế: nhận pha, pha xong, thu hồi</li>
 *   <li>{@link OrderIssueService} — sự cố, chặn/bỏ chặn, làm lại, huỷ</li>
 *   <li>{@link OrderHandoffService} — giao nhận: nhân viên nhận, giao khách</li>
 * </ul>
 *
 * <p><b>Thêm use case mới thì viết thẳng vào service chuyên trách</b>, chỉ thêm vào đây khi thật sự
 * cần cho một caller đang giữ facade. Lớp này càng phình thì càng khó biết một màn thực sự đụng vào
 * phần nào của luồng đơn.
 *
 * <p>Tên tham số ở đây phải KHỚP với service đích. Đặc biệt {@code sessionBranchId} (chi nhánh của
 * phiên đăng nhập, dùng chặn thao tác chéo chi nhánh) khác {@code branchId} thường — đừng gộp tên.
 */
public final class OrderService {

    private final OrderPlacementService placement;
    private final OrderQueryService query;
    private final KdsOrderWorkflowService kds;
    private final OrderIssueService issues;
    private final OrderHandoffService handoff;

    public OrderService() {
        OrderRepository repository = new OrderRepository();
        this.placement = new OrderPlacementService(repository);
        this.query = new OrderQueryService(repository);
        this.kds = new KdsOrderWorkflowService(repository);
        this.issues = new OrderIssueService(repository);
        this.handoff = new OrderHandoffService(repository);
    }

    public OrderService(OrderPlacementService placement, OrderQueryService query,
                        KdsOrderWorkflowService kds, OrderIssueService issues,
                        OrderHandoffService handoff) {
        this.placement = Objects.requireNonNull(placement);
        this.query = Objects.requireNonNull(query);
        this.kds = Objects.requireNonNull(kds);
        this.issues = Objects.requireNonNull(issues);
        this.handoff = Objects.requireNonNull(handoff);
    }

    // ── Đặt món (placement) ──────────────────────────────────────────────────────────────

    public int placeOrder(int branchId, Integer tableId, String source, String orderType,
                          Integer createdBy, List<CartLine> lines) throws SQLException {
        return placement.placeOrder(branchId, tableId, source, orderType, createdBy, lines);
    }

    // ── Đọc (query) ──────────────────────────────────────────────────────────────────────

    public List<OrderItem> getBaristaWorkbench(int branchId, LocalDateTime businessDayStartUtc)
            throws SQLException {
        return query.getBaristaWorkbench(branchId, businessDayStartUtc);
    }

    public List<OrderItem> getRecentlyServed(int branchId, int minutes) throws SQLException {
        return query.getRecentlyServed(branchId, minutes);
    }

    public List<OrderItem> getPickedUpItems(int branchId) throws SQLException {
        return query.getPickedUpItems(branchId);
    }

    public List<OrderItem> getTableItemStatuses(int tableId) throws SQLException {
        return query.getTableItemStatuses(tableId);
    }

    public List<Recipe> getRecipeIngredients(int productId) throws SQLException {
        return query.getRecipeIngredients(productId);
    }

    public List<Recipe> getDepletedRecipeIngredients(int branchId, int productId) throws SQLException {
        return query.getDepletedRecipeIngredients(branchId, productId);
    }

    public List<PickupTicket> getPickupTickets(int branchId) throws SQLException {
        return query.getPickupTickets(branchId);
    }

    public List<Order> getIncomingOrders(int branchId) throws SQLException {
        return query.getIncomingOrders(branchId);
    }

    public List<Order> getTableOrders(int tableId) throws SQLException {
        return query.getTableOrders(tableId);
    }

    // ── Quầy pha chế (kds) ───────────────────────────────────────────────────────────────

    public boolean startItem(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        return kds.startItem(orderItemId, userId, sessionBranchId);
    }

    public boolean markItemReady(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        return kds.markItemReady(orderItemId, userId, sessionBranchId);
    }

    public int startAllInOrder(int orderId, Integer userId, int sessionBranchId) throws SQLException {
        return kds.startAllInOrder(orderId, userId, sessionBranchId);
    }

    public KdsOrderWorkflowService.BulkReadyResult markOrderReady(int orderId, Integer userId,
                                                                  int sessionBranchId) throws SQLException {
        return kds.markOrderReady(orderId, userId, sessionBranchId);
    }

    public int countMyMakingItems(int branchId, int userId) throws SQLException {
        return kds.countMyMakingItems(branchId, userId);
    }

    public boolean reclaimItem(int orderItemId, Integer actorUserId, int branchId, String actorName,
                               Set<Integer> onDutyUserIds) throws SQLException {
        return kds.reclaimItem(orderItemId, actorUserId, branchId, actorName, onDutyUserIds);
    }

    public boolean returnItemToQueue(int orderItemId, Integer userId, int branchId) throws SQLException {
        return kds.returnItemToQueue(orderItemId, userId, branchId);
    }

    // ── Sự cố / chặn món (issues) ────────────────────────────────────────────────────────

    public boolean reportItemIssue(int orderItemId, String reason, Integer userId, int branchId)
            throws SQLException {
        return issues.reportItemIssue(orderItemId, reason, userId, branchId);
    }

    public boolean blockItem(int orderItemId, String reason, Integer userId, int branchId)
            throws SQLException {
        return issues.blockItem(orderItemId, reason, userId, branchId);
    }

    public boolean blockItemForDepletedIngredients(int orderItemId, List<Integer> ingredientIds,
                                                   String reason, Integer userId, int branchId)
            throws SQLException {
        return issues.blockItemForDepletedIngredients(orderItemId, ingredientIds, reason, userId, branchId);
    }

    public boolean unblockItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        return issues.unblockItem(orderItemId, userId, branchId);
    }

    public OrderIssueService.UnblockResult unblockItem(int orderItemId, List<StockAdjustment> recounts,
                                                       Integer userId, int branchId) throws SQLException {
        return issues.unblockItem(orderItemId, recounts, userId, branchId);
    }

    public boolean remakeItem(int orderItemId, String reason, Integer userId, int branchId)
            throws SQLException {
        return issues.remakeItem(orderItemId, reason, userId, branchId);
    }

    public String cancelItem(int orderItemId, String reason, Integer userId, int sessionBranchId)
            throws SQLException {
        return issues.cancelItem(orderItemId, reason, userId, sessionBranchId);
    }

    public boolean voidOrder(int orderId, Integer userId, int branchId) throws SQLException {
        return issues.voidOrder(orderId, userId, branchId);
    }

    // ── Giao nhận (handoff) ──────────────────────────────────────────────────────────────

    public boolean markItemPickedUp(int orderItemId, Integer userId, int sessionBranchId)
            throws SQLException {
        return handoff.markItemPickedUp(orderItemId, userId, sessionBranchId);
    }

    public boolean markItemServed(int orderItemId, Integer userId, int sessionBranchId)
            throws SQLException {
        return handoff.markItemServed(orderItemId, userId, sessionBranchId);
    }

    public int pickUpAllReady(int orderId, Integer userId, int sessionBranchId) throws SQLException {
        return handoff.pickUpAllReady(orderId, userId, sessionBranchId);
    }

    /** {@code tableNumber} là SỐ BÀN hiển thị (chuỗi), không phải khoá chính của bàn. */
    public int serveAllPickedUp(List<Integer> orderIds, String tableNumber,
                                Integer userId, int sessionBranchId) throws SQLException {
        return handoff.serveAllPickedUp(orderIds, tableNumber, userId, sessionBranchId);
    }

    public boolean unserveItem(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        return handoff.unserveItem(orderItemId, userId, sessionBranchId);
    }
}
