package com.cafe.controller.barista;

import com.cafe.controller.manager.InventoryDashboardServlet;
import com.cafe.common.SessionUtil;
import com.cafe.model.BranchInventory;
import com.cafe.model.BranchMenuItem;
import com.cafe.model.OrderItem;
import com.cafe.model.User;
import com.cafe.service.barista.HandoverService;
import com.cafe.service.barista.KdsService;
import com.cafe.service.barista.WasteService;
import com.cafe.service.shared.BranchMenuService;
import com.cafe.service.shared.InventoryService;
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
    private final HandoverService handoverService = new HandoverService();
    private final WasteService wasteService = new WasteService();
    private final InventoryService inventoryService = new InventoryService();
    private final BranchMenuService branchMenuService = new BranchMenuService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            User user = SessionUtil.currentUser(req);
            int userId = user != null ? user.getUserId() : 0;
            List<OrderItem> queue = kdsService.getQueue(branchId);
            List<OrderItem> readyItems = kdsService.getReadyItems(branchId);
            HandoverService.HandoverKpi kpi = handoverService.getKpi(branchId);
            HandoverService.HandoverKpi myKpi = userId > 0
                    ? handoverService.getMyKpi(branchId, userId)
                    : new HandoverService.HandoverKpi(-1, 0);
            WasteService.WasteSummary wasteSummary = wasteService.getTodayWasteSummary(branchId);
            List<BranchInventory> lowStock = inventoryService.getLowStock(branchId);
            List<BranchMenuItem> menuItems = branchMenuService.getMenuAvailability(branchId);

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
            req.setAttribute("kpi", kpi);
            req.setAttribute("myKpi", myKpi);
            req.setAttribute("wasteSummary", wasteSummary);
            req.setAttribute("queueCount", queue.size());
            req.setAttribute("readyCount", readyItems.size());
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
}
