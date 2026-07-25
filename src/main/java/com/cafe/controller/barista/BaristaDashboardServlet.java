package com.cafe.controller.barista;

import com.cafe.controller.manager.InventoryDashboardServlet;
import com.cafe.model.BranchInventory;
import com.cafe.model.BranchMenuItem;
import com.cafe.model.OrderItem;
import com.cafe.service.barista.KdsService;
import com.cafe.service.barista.WasteService;
import com.cafe.service.shared.BranchMenuService;
import com.cafe.service.shared.InventoryService;
import com.cafe.service.shared.WasteSummary;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** BaristaDashboardServlet -> /barista/dashboard. Bảng điều khiển ca read-only. */
@WebServlet("/barista/dashboard")
public class BaristaDashboardServlet extends HttpServlet {

    private final KdsService kdsService = new KdsService();
    private final WasteService wasteService = new WasteService();
    private final InventoryService inventoryService = new InventoryService();
    private final BranchMenuService branchMenuService = new BranchMenuService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            // Đếm theo ĐÚNG phạm vi của Quầy pha chế: ngày kinh doanh tính từ giờ mở cửa chi nhánh.
            // Trước đây màn này dùng hàng chờ không cắt ngày nên hai màn cho hai con số "đang chờ"
            // khác nhau ở cùng một thời điểm, không giải thích được cho người dùng.
            com.cafe.model.Branch branch = kdsService.getBranch(branchId);
            java.time.LocalDateTime dayStart = com.cafe.common.BusinessDay.startUtc(
                    branch == null ? null : branch.getOpenTime());
            List<OrderItem> workbench = kdsService.getWorkbenchQueue(branchId, dayStart);
            java.util.Map<String, List<OrderItem>> board = KdsService.splitWorkbench(workbench);
            // Giữ nguyên thứ tự pha của truy vấn (làm lại trước, rồi FIFO) để "Top món chờ lâu nhất"
            // đúng là 5 dòng đầu barista sẽ pha, khớp thứ tự trên màn Quầy pha chế.
            List<OrderItem> queue = new ArrayList<>();
            for (OrderItem it : workbench) {
                if ("WAITING".equals(it.getStatus()) || "MAKING".equals(it.getStatus())) queue.add(it);
            }
            List<OrderItem> readyItems = board.get("ready");
            List<OrderItem> blocked = board.get("blocked");
            WasteSummary wasteSummary = wasteService.getTodayWasteSummary(branchId);
            List<BranchInventory> lowStock = inventoryService.getLowStock(branchId);
            List<BranchMenuItem> menuItems = branchMenuService.getMenuAvailability(branchId);
            BaristaShift.expose(req, MyShiftServlet.PATH);

            int eightySixCount = 0;
            for (BranchMenuItem item : menuItems) {
                if (item.isPublished() && item.isIs86()) {
                    eightySixCount++;
                }
            }

            // Âm kho (oversold) = tập con của lowStock nhưng KHẨN hơn: bán/dùng quá tồn lý thuyết,
            // cần đối soát. Đếm riêng để làm nổi, KHÔNG cộng lại vào alertCount (tránh đếm đôi).
            int oversoldCount = 0;
            for (BranchInventory bi : lowStock) {
                if (bi.getQuantityOnHand() != null && bi.getQuantityOnHand().signum() < 0) {
                    oversoldCount++;
                }
            }

            req.setAttribute("queue", queue);
            req.setAttribute("readyItems", readyItems);
            req.setAttribute("topWaitingItems", firstItems(queue, 5));
            req.setAttribute("lowStock", lowStock);
            req.setAttribute("lowStockPreview", firstItems(lowStock, 5));
            req.setAttribute("wasteSummary", wasteSummary);
            req.setAttribute("queueCount", cupCount(queue));
            req.setAttribute("readyCount", cupCount(readyItems));
            req.setAttribute("blockedCount", cupCount(blocked));
            req.setAttribute("lowStockCount", lowStock.size());
            req.setAttribute("eightySixCount", eightySixCount);
            req.setAttribute("oversoldCount", oversoldCount);
            req.setAttribute("suggest86Count", branchMenuService.getSuggested86(branchId).size());
            req.setAttribute("alertCount", lowStock.size() + eightySixCount);
            req.setAttribute("pageTitle", "Bảng điều khiển ca");
            req.getRequestDispatcher("/WEB-INF/views/barista/dashboard.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private static <T> List<T> firstItems(List<T> source, int limit) {
        if (source == null || source.isEmpty() || limit <= 0) {
            return List.of();
        }
        int end = Math.min(source.size(), limit);
        return new ArrayList<>(source.subList(0, end));
    }

    private static int cupCount(List<OrderItem> items) {
        int total = 0;
        if (items != null) for (OrderItem item : items) total += item.getQuantity();
        return total;
    }
}
