package com.cafe.controller.manager;

import com.cafe.common.BusinessException;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.manager.ManagerCashierShiftService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/** Màn phục hồi két thu ngân bị bỏ quên của đúng chi nhánh Manager. */
@WebServlet("/manager/cashier-shift")
public class ManagerCashierShiftServlet extends HttpServlet {

    private final ManagerCashierShiftService service = new ManagerCashierShiftService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("openShifts", service.getOpenShifts(branchId));
            req.setAttribute("pageTitle", "Két thu ngân");
            req.getRequestDispatcher("/WEB-INF/views/manager/cashier-shift.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF");
            return;
        }

        String redirect = req.getContextPath() + "/manager/cashier-shift";
        int branchId = InventoryDashboardServlet.branchId(req);
        User user = SessionUtil.currentUser(req);
        int managerId = user == null ? 0 : user.getUserId();

        try {
            if ("forceClose".equals(req.getParameter("action"))) {
                int shiftId = Integer.parseInt(req.getParameter("shiftId"));
                BigDecimal actualCash = parseMoney(req.getParameter("actualCash"));
                ManagerCashierShiftService.ForceCloseResult result = service.forceClose(
                        branchId, managerId, shiftId, actualCash, req.getParameter("reason"));
                req.getSession().setAttribute("flashOk",
                        "Đã kết ca #" + result.shiftId() + " và ghi audit. Chênh lệch tiền mặt: "
                                + result.variance().stripTrailingZeros().toPlainString() + " đ.");
            }
            resp.sendRedirect(redirect);
        } catch (BusinessException | IllegalArgumentException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(redirect);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tiền mặt thực đếm không được để trống.");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tiền mặt thực đếm không hợp lệ.");
        }
    }
}
