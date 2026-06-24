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
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user != null && user.getRoleId() == 5) {
            Customer customer = customerDAO.getCustomerById(user.getUserId());
            request.setAttribute("customerInfo", customer);
        }

        List<Table> tables = tableDAO.getTablesForCustomerCheckout(DEFAULT_BRANCH_ID);
        int tableIdFromQr = parseInt(request.getParameter("tableId"), 0);
        boolean scannedFromCamera = "1".equals(request.getParameter("scan"));
        boolean qrVerified = session.getAttribute("customerQrVerified") instanceof Boolean
                && (Boolean) session.getAttribute("customerQrVerified");

        int selectedTableId = 0;
        if (scannedFromCamera) {
            selectedTableId = tableIdFromQr;
        } else if (qrVerified) {
            Object savedTableId = session.getAttribute("customerTableId");
            if (savedTableId instanceof Integer) {
                selectedTableId = (Integer) savedTableId;
            }
        } else if (tableIdFromQr > 0) {
            session.setAttribute("cartError", "Vui lòng dùng nút quét QR bằng camera để xác thực bàn trước khi gửi order.");
        }

        boolean selectedTableAvailable = isTableAvailable(tables, selectedTableId);
        if (selectedTableId > 0 && !selectedTableAvailable) {
            session.removeAttribute("customerTableId");
            session.removeAttribute("customerQrVerified");
            session.setAttribute("cartError", "Bàn này hiện không còn trống. Vui lòng quét bàn khác.");
            selectedTableId = 0;
            qrVerified = false;
        }

        if (scannedFromCamera && selectedTableId > 0 && selectedTableAvailable) {
            session.setAttribute("customerTableId", selectedTableId);
            session.setAttribute("customerQrVerified", true);
            qrVerified = true;
        }

        String selectedTableName = "Chưa chọn bàn";
        for (Table table : tables) {
            if (table.getTableID() == selectedTableId) {
                selectedTableName = table.getTableName();
                break;
            }
        }

        request.setAttribute("tables", tables);
        request.setAttribute("selectedTableId", qrVerified ? selectedTableId : 0);
        request.setAttribute("selectedTableName", selectedTableName);
        request.setAttribute("qrScanned", qrVerified && selectedTableId > 0);
        request.getRequestDispatcher("customer_qr_order.jsp").forward(request, response);
    }

    private boolean isTableAvailable(List<Table> tables, int tableId) {
        for (Table table : tables) {
            if (table.getTableID() == tableId) {
                return true;
            }
        }
        return false;
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
