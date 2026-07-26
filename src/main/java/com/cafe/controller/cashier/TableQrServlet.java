package com.cafe.controller.cashier;

import com.cafe.common.QrLink;
import com.cafe.controller.manager.InventoryDashboardServlet;
import com.cafe.service.cashier.TableSessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import com.cafe.model.DiningTable;

/** Trang chỉ-GET để thu ngân in mã QR dán tại bàn. */
@WebServlet("/cashier/table-qr")
public class TableQrServlet extends HttpServlet {

    private final TableSessionService service = new TableSessionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            List<DiningTable> tables = service.getFloorMap(branchId);
            req.setAttribute("tables", tables);
            String baseUrl = QrLink.absoluteBase(req);
            req.setAttribute("baseUrl", baseUrl);
            Map<Integer, String> menuUrls = new LinkedHashMap<>();
            for (DiningTable table : tables) {
                if (table.getQrCode() != null && !table.getQrCode().isBlank()) {
                    menuUrls.put(table.getDiningTableId(), QrLink.menuUrl(baseUrl, table.getQrCode()));
                }
            }
            req.setAttribute("menuUrls", menuUrls);
            req.getRequestDispatcher("/WEB-INF/views/cashier/table-qr.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
