package com.mycoffee.controller.cashier;

import com.mycoffee.dao.TableDAO;
import com.mycoffee.model.Table;
import com.mycoffee.model.User;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// 1. ĐỔI NAME THÀNH CashierTableController ĐỂ KHÔNG BỊ TRÙNG VỚI MANAGER
@WebServlet(name = "CashierTableController", urlPatterns = {"/pos-tables"})
public class TableController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        int roleId = (user != null) ? user.getRoleId() : 0;

        if (roleId != 1 && roleId != 2 && roleId != 3) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        int branchId = 1;
        TableDAO tableDAO = new TableDAO();
        List<Table> tableList = tableDAO.getTablesByBranch(branchId);

        // 2. ĐỔI THÀNH "tableList" ĐỂ KHỚP HOÀN TOÀN VỚI FILE JSP CỦA BẠN
        request.setAttribute("tableList", tableList); 
        
        // 3. THÊM DẤU GẠCH CHÉO / TRƯỚC table_layout.jsp ĐỂ AN TOÀN ĐƯỜNG DẪN
        request.getRequestDispatcher("/views/cashier/table_layout.jsp").forward(request, response);
    }
}