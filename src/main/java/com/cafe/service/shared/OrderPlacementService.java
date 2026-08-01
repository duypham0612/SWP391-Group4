package com.cafe.service.shared;

import com.cafe.common.*;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.*;
import com.cafe.dao.shared.*;
import com.cafe.model.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Đặt đơn COUNTER/QR và chốt snapshot giá, tên, modifier trong một transaction. */
public final class OrderPlacementService {
    private final OrderRepository repository;
    public OrderPlacementService() { this(new OrderRepository()); }
    OrderPlacementService(OrderRepository repository) { this.repository = Objects.requireNonNull(repository); }

    /**
     * Đặt đơn (COUNTER hoặc QR) — tạo Order + OrderItem(WAITING) + OrderItemModifier, publish order.created.
     * Tất cả trong MỘT transaction. Giá tính server-side (giá menu chi nhánh + Σ priceDelta option).
     */
    public int placeOrder(int branchId, Integer tableId, String source, String orderType,
                          Integer createdBy, List<OrderService.CartLine> lines) throws SQLException {
        if ("COUNTER".equals(source) && createdBy == null)
            throw new BusinessException("Đơn tại quầy bắt buộc có nhân viên tạo.");
        if ("QR".equals(source) && createdBy != null)
            throw new BusinessException("Đơn QR không được gắn nhân viên tạo.");
        // Guard cuối dùng chung: cả POS, QR và mọi caller khác đều không thể vượt 20 món cùng loại.
        OrderQuantityValidator.validate(lines);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (tableId != null) {
                    DiningTable table = new DiningTableDao().findByIdForUpdate(conn, tableId);
                    if (table == null || table.getBranchId() != branchId
                            || !"OCCUPIED".equals(table.getStatus())) {
                        throw new BusinessException("Bàn không còn mở hoặc không thuộc chi nhánh hiện tại.");
                    }
                }
                Map<Integer, BigDecimal> priceByProduct = new HashMap<>();
                Map<Integer, Boolean> is86ByProduct = new HashMap<>();
                Map<Integer, String> nameByProduct = new HashMap<>();
                java.util.Set<Integer> unavailableProduct = new java.util.HashSet<>();
                for (BranchMenuItem bm : repository.branchMenuDao.listForBranch(conn, branchId)) {
                    nameByProduct.put(bm.getProductId(), bm.getProductName());   // giữ tên cho câu báo lỗi
                    // listForBranch LEFT JOIN nên trả về CẢ món chưa có trong menu chi nhánh (Published=0,
                    // giá = BasePrice). Không lọc thì món chưa publish vẫn đặt được bằng POST tự soạn.
                    if (!bm.isPublished()) continue;
                    priceByProduct.put(bm.getProductId(),
                            bm.getLocalPrice() != null ? bm.getLocalPrice() : bm.getBasePrice());
                    is86ByProduct.put(bm.getProductId(), bm.isTemporarilyUnavailable());
                    if (!bm.isListed()) unavailableProduct.add(bm.getProductId());
                }
                // Hết theo kho (bản chất 1): chặn đặt món có nguyên liệu tồn ≤ 0, độc lập với cờ 86 thủ công.
                Map<Integer, ProductStockStatus> stockByProduct =
                        repository.productRecipeDao.findProductStockStatuses(conn, branchId);

                Order o = new Order();
                o.setBranchId(branchId);
                o.setDiningTableId(tableId);
                o.setSource(source);
                o.setOrderType(orderType == null ? "DINE_IN" : orderType);
                o.setStatus("ACTIVE");
                o.setCreatedBy(createdBy);
                com.cafe.model.Branch branch = repository.branchDao.findById(conn, branchId);
                java.time.LocalDate businessDate = com.cafe.common.BusinessDay.businessDate(
                        branch == null ? null : branch.getOpenTime());
                int pickupSequence = repository.orderDao.reservePickupSequence(conn, branchId, businessDate);
                o.setBusinessDate(businessDate);
                o.setPickupCode(pickupPrefix(o.getSource(), o.getOrderType()) + pickupSequence);
                int orderId = repository.orderDao.insert(conn, o);

                for (OrderService.CartLine line : lines) {
                    if (line.quantity <= 0) continue;
                    BigDecimal base = priceByProduct.get(line.productId);
                    // Chưa có trong menu chi nhánh (hoặc product không còn active) — lỗi phía client gửi lên,
                    // ném IllegalArgumentException để PosServlet/QrMenuServlet trả 400 kèm lời nhắn, không phải 500.
                    if (base == null) {
                        String nm = nameByProduct.getOrDefault(line.productId, "#" + line.productId);
                        throw new IllegalArgumentException("Món \"" + nm + "\" không bán ở chi nhánh này — vui lòng bỏ khỏi đơn.");
                    }
                    // Món manager đã "Ngừng bán": getPosMenu chỉ lọc ở tầng hiển thị, tab POS mở sẵn từ
                    // trước đó vẫn giữ món trong giỏ — phải chặn lại ở tầng ghi giống cờ 86.
                    if (unavailableProduct.contains(line.productId)) {
                        String nm = nameByProduct.getOrDefault(line.productId, "#" + line.productId);
                        throw new ItemUnavailableException(line.productId, nm, "DISABLED",
                                "Món \"" + nm + "\" đã ngừng bán — vui lòng bỏ khỏi đơn.");
                    }
                    // Chặn oversell tại nguồn: món đang 86 (hết) thì không cho vào đơn (Contract #3 — cashier sở hữu order entry).
                    if (Boolean.TRUE.equals(is86ByProduct.get(line.productId))) {
                        String nm = nameByProduct.getOrDefault(line.productId, "#" + line.productId);
                        throw new ItemUnavailableException(line.productId, nm, "EIGHTY_SIX",
                                "Món \"" + nm + "\" đang tạm ngừng bán — vui lòng bỏ khỏi đơn.");
                    }
                    // Hết theo kho: nguyên liệu tồn ≤ 0 → chặn đặt, chờ nhập/pha lại (tự có lại khi tồn > 0).
                    ProductStockStatus stock = stockByProduct.get(line.productId);
                    if (stock != null && stock.isOut()) {
                        String nm = nameByProduct.getOrDefault(line.productId, "#" + line.productId);
                        throw new ItemUnavailableException(line.productId, nm, ProductStockStatus.OUT,
                                "Món \"" + nm + "\" đã hết nguyên liệu (" + stockReason(stock)
                                        + ") — vui lòng chọn món khác.");
                    }

                    List<Integer> optionIds = validateOptions(conn, line.productId, line.optionIds);

                    // gom priceDelta của các option đã chọn
                    Map<Integer, BigDecimal> deltaByOption = new HashMap<>();
                    Map<Integer, String> nameByOption = new HashMap<>();
                    for (Integer optId : optionIds) {
                        ModifierOption opt = repository.optionDao.findById(conn, optId);
                        deltaByOption.put(optId, opt.getPriceDelta());
                        nameByOption.put(optId, opt.getName());
                    }

                    OrderItem it = new OrderItem();
                    it.setOrderId(orderId);
                    it.setProductId(line.productId);
                    it.setQuantity(line.quantity);
                    it.setUnitPrice(base);
                    it.setNote(line.note);
                    it.setStatus("WAITING");
                    it.setProductNameAtOrder(nameByProduct.get(line.productId));
                    int itemId = repository.itemDao.insert(conn, it);

                    for (Integer optId : optionIds) {
                        repository.oimDao.insert(conn, itemId, optId,
                                deltaByOption.getOrDefault(optId, BigDecimal.ZERO),
                                nameByOption.get(optId));
                    }
                }

                repository.outboxEventDao.insert(conn, EventType.ORDER_CREATED, String.valueOf(orderId), branchId,
                        "{\"orderId\":" + orderId + ",\"source\":\"" + source + "\"}");
                conn.commit();
                return orderId;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally { conn.setAutoCommit(true); }
        }
    }

    private String pickupPrefix(String source, String orderType) {
        String prefix = "QR".equals(source) ? "G" : ("TAKEAWAY".equals(orderType) ? "T" : "D");
        return prefix;
    }

    private static String stockReason(ProductStockStatus stock) {
        return stock.getOutIngredients().isEmpty()
                ? "nguyên liệu chưa xác định"
                : String.join(", ", stock.getOutIngredients());
    }

    private List<Integer> validateOptions(Connection conn, int productId, List<Integer> optionIds) throws SQLException {
        List<Integer> selected = optionIds == null ? List.of() : optionIds;
        Map<Integer, ModifierGroup> groupsById = new HashMap<>();
        Map<Integer, Integer> optionToGroup = new HashMap<>();
        Map<Integer, Integer> selectedCountByGroup = new HashMap<>();

        for (ProductModifierGroup pmg : repository.pmgDao.findByProduct(conn, productId)) {
            ModifierGroup group = repository.groupDao.findById(conn, pmg.getModifierGroupId());
            if (group == null) continue;
            groupsById.put(group.getModifierGroupId(), group);
            for (ModifierOption option : repository.optionDao.findByGroup(conn, group.getModifierGroupId())) {
                if (option.isActive()) optionToGroup.put(option.getModifierOptionId(), group.getModifierGroupId());
            }
        }

        List<Integer> validOptionIds = new ArrayList<>();
        for (Integer optionId : selected) {
            if (optionId == null) continue;
            Integer groupId = optionToGroup.get(optionId);
            if (groupId == null) {
                throw new IllegalArgumentException("Tuỳ chọn không hợp lệ cho món này.");
            }
            selectedCountByGroup.merge(groupId, 1, Integer::sum);
            validOptionIds.add(optionId);
        }

        for (ModifierGroup group : groupsById.values()) {
            int count = selectedCountByGroup.getOrDefault(group.getModifierGroupId(), 0);
            String groupName = ModifierGroupNames.display(group.getName());
            if (group.isRequired() && count < group.getMinSelect()) {
                throw new IllegalArgumentException("Vui lòng chọn " + groupName + ".");
            }
            if (count < group.getMinSelect()) {
                throw new IllegalArgumentException("Vui lòng chọn tối thiểu " + group.getMinSelect()
                        + " tuỳ chọn cho " + groupName + ".");
            }
            if (group.getMaxSelect() > 0 && count > group.getMaxSelect()) {
                throw new IllegalArgumentException(groupName + " chỉ được chọn tối đa "
                        + group.getMaxSelect() + " tuỳ chọn.");
            }
        }
        return validOptionIds;
    }

    /**
     * Nạp modifier cho cả danh sách bằng MỘT query thay vì một query mỗi món (N+1).
     * Dùng chung cho mọi màn đọc dòng đơn: quầy pha chế, pickup, inbox, lịch sử, theo dõi QR.
     * Món không có modifier được gán list rỗng để JSP/EL không phải kiểm null.
     */

}
