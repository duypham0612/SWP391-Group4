package com.cafe.service.shared;

import com.cafe.dao.cashier.DiningTableDao;
import com.cafe.dao.shared.BranchInventoryDao;
import com.cafe.common.*;
import com.cafe.model.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Đặt đơn COUNTER/QR và chốt snapshot giá, tên, modifier trong một transaction. */
public final class OrderPlacementService {
    private final OrderRepository repository;
    private final BranchInventoryDao inventoryDao;
    public OrderPlacementService() { this(new OrderRepository(), new BranchInventoryDao()); }
    OrderPlacementService(OrderRepository repository) { this(repository, new BranchInventoryDao()); }
    OrderPlacementService(OrderRepository repository, BranchInventoryDao inventoryDao) {
        this.repository = Objects.requireNonNull(repository);
        this.inventoryDao = Objects.requireNonNull(inventoryDao);
    }

    /**
     * Đặt đơn (COUNTER hoặc QR) — tạo Order + OrderItem(WAITING) + OrderItemModifier, publish order.created.
     * Tất cả trong MỘT transaction. Giá tính server-side (giá menu chi nhánh + Σ priceDelta option).
     */
    public int placeOrder(int branchId, Integer tableId, String source, String orderType,
                          Integer createdBy, List<CartLine> lines) throws SQLException {
        if ("COUNTER".equals(source) && createdBy == null)
            throw new BusinessException("Đơn tại quầy bắt buộc có nhân viên tạo.");
        if ("QR".equals(source) && createdBy != null)
            throw new BusinessException("Đơn QR không được gắn nhân viên tạo.");
        // Guard cuối dùng chung: cả POS, QR và mọi caller khác đều không thể vượt 20 món cùng loại.
        OrderQuantityValidator.validate(lines);
        // Đi qua repository.tx để hưởng vòng chạy lại khi bị chọn làm nạn nhân deadlock (SQL 1205).
        // Cấp mã pickup ở đây quét dải SELECT MAX(...) rồi INSERT vào chính bảng đó, nên đây CHÍNH LÀ
        // đường sinh deadlock — trước kia nó tự quản transaction riêng nên nằm ngoài vùng thử lại.
        return repository.tx(conn -> {
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

                List<PreparedLine> preparedLines = new ArrayList<>();
                Map<Integer, BigDecimal> requiredByIngredient = new LinkedHashMap<>();
                Map<Integer, String> ingredientNameById = new HashMap<>();
                Map<Integer, String> ingredientUnitById = new HashMap<>();
                Map<Integer, Integer> productByIngredient = new HashMap<>();
                for (CartLine line : lines) {
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

                    List<Recipe> recipe = repository.productRecipeDao.findByProduct(conn, line.productId);
                    List<Recipe> impacts = new ArrayList<>();
                    for (Integer optId : optionIds) {
                        impacts.addAll(repository.productRecipeDao.findByOption(conn, optId));
                    }
                    for (Recipe ingredient : recipe) {
                        ingredientNameById.put(ingredient.getIngredientId(), ingredient.getIngredientName());
                        ingredientUnitById.put(ingredient.getIngredientId(), ingredient.getIngredientUnit());
                    }
                    for (Recipe ingredient : impacts) {
                        ingredientNameById.put(ingredient.getIngredientId(), ingredient.getIngredientName());
                        ingredientUnitById.put(ingredient.getIngredientId(), ingredient.getIngredientUnit());
                    }
                    for (Map.Entry<Integer, BigDecimal> requirement
                            : DeductionCalculator.computeRequired(recipe, impacts, line.quantity).entrySet()) {
                        requiredByIngredient.merge(requirement.getKey(), requirement.getValue(), BigDecimal::add);
                        productByIngredient.put(requirement.getKey(), line.productId);
                    }
                    preparedLines.add(new PreparedLine(line, base, nameByProduct.get(line.productId), optionIds,
                            deltaByOption, nameByOption));
                }

                // Khóa theo IngredientId cố định, đồng thời tính cả món đã nhận nhưng chưa hoàn thành,
                // để không nhận vượt số lượng có thể phục vụ. Việc ghi trừ tồn vẫn ở bước Barista hoàn thành.
                Map<Integer, BigDecimal> onHandByIngredient = new HashMap<>();
                for (Integer ingredientId : new TreeSet<>(requiredByIngredient.keySet())) {
                    onHandByIngredient.put(ingredientId,
                            inventoryDao.findQtyOnHandForUpdate(conn, branchId, ingredientId));
                }
                // Đặt lock trước khi đọc commitments để request thứ hai chỉ đọc sau khi request thứ nhất
                // đã insert đơn. Vì thế tồn còn lại không thể bị hai request cùng sử dụng.
                Map<Integer, BigDecimal> committedByIngredient = new HashMap<>();
                for (com.cafe.dao.shared.OrderItemDao.StockCommitment commitment
                        : repository.itemDao.findOutstandingStockCommitments(conn, branchId)) {
                    List<Recipe> committedRecipe = repository.productRecipeDao.findByProduct(conn,
                            commitment.productId());
                    List<Recipe> committedImpacts = new ArrayList<>();
                    for (Integer optionId : repository.oimDao.findOptionIds(conn, commitment.orderItemId())) {
                        committedImpacts.addAll(repository.productRecipeDao.findByOption(conn, optionId));
                    }
                    for (Map.Entry<Integer, BigDecimal> commitmentRequirement
                            : DeductionCalculator.computeRequired(committedRecipe, committedImpacts,
                                    commitment.quantity()).entrySet()) {
                        if (onHandByIngredient.containsKey(commitmentRequirement.getKey())) {
                            committedByIngredient.merge(commitmentRequirement.getKey(),
                                    commitmentRequirement.getValue(), BigDecimal::add);
                        }
                    }
                }
                for (Map.Entry<Integer, BigDecimal> commitment : committedByIngredient.entrySet()) {
                    onHandByIngredient.merge(commitment.getKey(), commitment.getValue().negate(), BigDecimal::add);
                }
                List<OrderStockValidator.Shortfall> shortfalls =
                        OrderStockValidator.findShortfalls(requiredByIngredient, onHandByIngredient);
                if (!shortfalls.isEmpty()) {
                    OrderStockValidator.Shortfall first = shortfalls.get(0);
                    int productId = productByIngredient.getOrDefault(first.ingredientId(), 0);
                    String productName = nameByProduct.getOrDefault(productId, "Món trong đơn");
                    List<String> details = new ArrayList<>();
                    for (OrderStockValidator.Shortfall shortfall : shortfalls) {
                        String ingredientName = ingredientNameById.getOrDefault(shortfall.ingredientId(),
                                "Nguyên liệu #" + shortfall.ingredientId());
                        String unit = ingredientUnitById.getOrDefault(shortfall.ingredientId(), "");
                        details.add(ingredientName + ": cần " + QuantityFormat.plain(shortfall.required())
                                + " / còn " + QuantityFormat.plain(shortfall.onHand())
                                + (unit.isBlank() ? "" : " " + unit));
                    }
                    throw new ItemUnavailableException(productId, productName, "INSUFFICIENT_STOCK",
                            "Không đủ tồn kho để đặt đơn: " + String.join("; ", details) + ".");
                }

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

                for (PreparedLine prepared : preparedLines) {
                    CartLine line = prepared.line();

                    OrderItem it = new OrderItem();
                    it.setOrderId(orderId);
                    it.setProductId(line.productId);
                    it.setQuantity(line.quantity);
                    it.setUnitPrice(prepared.basePrice());
                    it.setNote(line.note);
                    it.setStatus("WAITING");
                    it.setProductNameAtOrder(prepared.productName());
                    int itemId = repository.itemDao.insert(conn, it);

                    for (Integer optId : prepared.optionIds()) {
                        repository.oimDao.insert(conn, itemId, optId,
                                prepared.deltaByOption().getOrDefault(optId, BigDecimal.ZERO),
                                prepared.nameByOption().get(optId));
                    }
                }

                repository.outboxEventDao.insert(conn, EventType.ORDER_CREATED, String.valueOf(orderId), branchId,
                        "{\"orderId\":" + orderId + ",\"source\":\"" + source + "\"}");
            return orderId;
        });
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

    private record PreparedLine(CartLine line, BigDecimal basePrice, String productName,
                                List<Integer> optionIds, Map<Integer, BigDecimal> deltaByOption,
                                Map<Integer, String> nameByOption) { }

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
