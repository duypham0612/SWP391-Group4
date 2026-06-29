package com.cafe.controller.manager;

import com.cafe.common.CsrfUtil;
import com.cafe.model.Supplier;
import com.cafe.service.manager.SupplierService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** M6 · SupplierServlet → /manager/supplier. */
@WebServlet("/manager/supplier")
public class SupplierServlet extends HttpServlet {

    private final SupplierService service = new SupplierService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                req.setAttribute("supplier", new Supplier());
                forwardForm(req, resp, "Thêm nhà cung cấp");
            } else if ("edit".equals(action)) {
                Supplier s = service.getSupplier(Integer.parseInt(req.getParameter("id")));
                if (s == null) { resp.sendError(404); return; }
                req.setAttribute("supplier", s);
                forwardForm(req, resp, "Sửa nhà cung cấp");
            } else {
                req.setAttribute("suppliers", service.getSupplierList());
                req.setAttribute("pageTitle", "Nhà cung cấp");
                req.getRequestDispatcher("/WEB-INF/views/manager/supplier-list.jsp").forward(req, resp);
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        String ctx = req.getContextPath();
        String action = req.getParameter("action");
        try {
            if ("toggleActive".equals(action)) {
                service.toggleActive(Integer.parseInt(req.getParameter("id")));
                resp.sendRedirect(ctx + "/manager/supplier");
                return;
            }
            Supplier s = bind(req);
            if (s.getName() == null || s.getName().isBlank()) {
                req.setAttribute("supplier", s);
                req.setAttribute("errorMsg", "Tên nhà cung cấp không được để trống.");
                forwardForm(req, resp, s.getSupplierId() == 0 ? "Thêm nhà cung cấp" : "Sửa nhà cung cấp");
                return;
            }
            if (s.getSupplierId() == 0) service.createSupplier(s); else service.updateSupplier(s);
            resp.sendRedirect(ctx + "/manager/supplier");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private Supplier bind(HttpServletRequest req) {
        Supplier s = new Supplier();
        String id = req.getParameter("supplierId");
        if (id != null && !id.isBlank()) s.setSupplierId(Integer.parseInt(id));
        s.setName(trim(req.getParameter("name")));
        s.setPhone(trim(req.getParameter("phone")));
        s.setAddress(trim(req.getParameter("address")));
        s.setActive(req.getParameter("active") != null);
        return s;
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/manager/supplier-form.jsp").forward(req, resp);
    }
    private String trim(String s) { return s == null ? null : s.trim(); }
}
