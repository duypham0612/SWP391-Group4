package com.mycoffee.controller;

import com.mycoffee.dao.CustomerDAO;
import com.mycoffee.dao.ProductDAO;
import com.mycoffee.dao.TableDAO;
import com.mycoffee.model.Customer;
import com.mycoffee.model.User;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "MenuController", urlPatterns = {"/menu", "/customer-menu"})
public class MenuController extends HttpServlet {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final TableDAO tableDAO = new TableDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // AuthFilter đã kiểm tra đăng nhập — chỉ cần lấy user từ session
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        // Nếu là Customer (RoleID=5), đẩy thêm thông tin điểm tích lũy lên JSP
        if (user != null && user.getRoleId() == 5) {
            Customer customer = customerDAO.getCustomerById(user.getUserId());
            request.setAttribute("customerInfo", customer);
        }

        request.setAttribute("products", productDAO.getAllAvailableProducts());
        request.setAttribute("tables", tableDAO.getTablesForCustomerCheckout(1));
        request.getRequestDispatcher("menu.jsp").forward(request, response);
    }
}
