package com.cafe.controller.manager;

import com.cafe.common.BusinessException;
import com.cafe.web.support.CsrfUtil;
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

    private final SupplierService service;

    public SupplierServlet() { this(new SupplierService()); }
    SupplierServlet(SupplierService service) {
        this.service = java.util.Objects.requireNonNull(service);
    }

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
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Mã nhà cung cấp không hợp lệ.");
            resp.sendRedirect(req.getContextPath() + "/manager/supplier");
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        String ctx = req.getContextPath();
        String action = req.getParameter("action");
        Supplier submitted = null;
        try {
            if ("toggleActive".equals(action)) {
                service.toggleActive(Integer.parseInt(req.getParameter("id")));
                resp.sendRedirect(ctx + "/manager/supplier");
                return;
            }
            submitted = bind(req);
            if (submitted.getSupplierId() == 0) service.createSupplier(submitted);
            else service.updateSupplier(submitted);
            resp.sendRedirect(ctx + "/manager/supplier");
        } catch (BusinessException e) {
            if (submitted == null) {
                req.getSession().setAttribute("flashError", e.getMessage());
                resp.sendRedirect(ctx + "/manager/supplier");
                return;
            }
            req.setAttribute("supplier", submitted);
            req.setAttribute("errorMsg", e.getMessage());
            forwardForm(req, resp,
                    submitted.getSupplierId() == 0 ? "Thêm nhà cung cấp" : "Sửa nhà cung cấp");
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Mã nhà cung cấp không hợp lệ.");
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
