package com.mycoffee.controller.customer;

import com.mycoffee.dao.CustomerDAO;
import com.mycoffee.dao.TableDAO;
import com.mycoffee.model.Customer;
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

@WebServlet(name = "CustomerQrOrderController", urlPatterns = {"/customer-qr-order"})
public class CustomerQrOrderController extends HttpServlet {

    private static final int DEFAULT_BRANCH_ID = 1;
    private final TableDAO tableDAO = new TableDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user != null && user.getRoleId() == 5) {
            Customer customer = customerDAO.getCustomerById(user.getUserId());
            request.setAttribute("customerInfo", customer);
        }

        List<Table> tables = tableDAO.getTablesForCustomerCheckout(DEFAULT_BRANCH_ID);
        int tableIdFromQr = parseInt(request.getParameter("tableId"), 0);
        boolean qrScanned = tableIdFromQr > 0;
        int selectedTableId = tableIdFromQr;
        if (selectedTableId <= 0) {
            Object savedTableId = session.getAttribute("customerTableId");
            if (savedTableId instanceof Integer) {
                selectedTableId = (Integer) savedTableId;
            }
        }
        if (selectedTableId <= 0 && !tables.isEmpty()) {
            selectedTableId = tables.get(0).getTableID();
        }
        session.setAttribute("customerTableId", selectedTableId);

        String selectedTableName = "Chưa chọn bàn";
        for (Table table : tables) {
            if (table.getTableID() == selectedTableId) {
                selectedTableName = table.getTableName();
                break;
            }
        }

        request.setAttribute("tables", tables);
        request.setAttribute("selectedTableId", selectedTableId);
        request.setAttribute("selectedTableName", selectedTableName);
        request.setAttribute("qrScanned", qrScanned);
        request.getRequestDispatcher("customer_qr_order.jsp").forward(request, response);
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
