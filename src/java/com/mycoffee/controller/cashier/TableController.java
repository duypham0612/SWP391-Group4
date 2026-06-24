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

@WebServlet(name = "TableController", urlPatterns = {"/pos-tables"})
public class TableController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. KIỂM TRA BẢO MẬT (Chỉ Admin, Manager hoặc Cashier mới được xem)
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null || (user.getRoleId() != 1 && user.getRoleId() != 2 && user.getRoleId() != 3)) {
            response.sendRedirect("login");
            return;
        }

        // 2. LẤY DATA
        // Tạm thời fix cứng Branch = 1. Sau này sẽ query BranchID dựa vào user.getUserId() từ bảng Employees.
        int branchId = 1;
        TableDAO tableDAO = new TableDAO();
        List<Table> tableList = tableDAO.getTablesByBranch(branchId);

        // 3. ĐẨY SANG VIEW
        request.setAttribute("danhSachBan", tableList);
        request.getRequestDispatcher("table_layout.jsp").forward(request, response);
    }
}