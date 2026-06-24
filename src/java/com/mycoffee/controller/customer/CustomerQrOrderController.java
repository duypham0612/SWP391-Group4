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

        if (user != null && user.getRoleId() == User.ROLE_CUSTOMER) {
            Customer customer = customerDAO.getCustomerById(user.getUserId());
            request.setAttribute("customerInfo", customer);
        }

        int tableIdFromQr = parseInt(request.getParameter("tableId"), 0);
        boolean scannedFromCamera = "1".equals(request.getParameter("scan"));
        boolean qrVerified = session.getAttribute("customerQrVerified") instanceof Boolean
                && (Boolean) session.getAttribute("customerQrVerified");
        String selectionMode = (String) session.getAttribute("customerTableSelectionMode");
        Object savedTableValue = session.getAttribute("customerTableId");
        int savedTableId = savedTableValue instanceof Integer ? (Integer) savedTableValue : 0;

        if (scannedFromCamera && "test".equals(selectionMode) && savedTableId > 0) {
            tableDAO.releaseCustomerSelectedTable(savedTableId);
            savedTableId = 0;
        }

        int tableToInclude = "test".equals(selectionMode) ? savedTableId : 0;
        List<Table> tables = tableDAO.getTablesForCustomerCheckout(DEFAULT_BRANCH_ID, tableToInclude);

        int selectedTableId = 0;
        if (scannedFromCamera) {
            selectedTableId = tableIdFromQr;
        } else if (qrVerified) {
            selectedTableId = savedTableId;
        } else if (tableIdFromQr > 0) {
            session.setAttribute("cartError", "Vui lòng dùng nút quét QR bằng camera để xác thực bàn trước khi gửi order.");
        }

        boolean selectedTableAvailable = isTableAvailable(tables, selectedTableId);
        if (selectedTableId > 0 && !selectedTableAvailable) {
            if ("test".equals(selectionMode)) {
                tableDAO.releaseCustomerSelectedTable(selectedTableId);
            }
            session.removeAttribute("customerTableId");
            session.removeAttribute("customerQrVerified");
            session.removeAttribute("customerTableSelectionMode");
            session.setAttribute("cartError", "Bàn này hiện không còn trống. Vui lòng quét hoặc chọn bàn khác.");
            selectedTableId = 0;
            qrVerified = false;
        }

        if (scannedFromCamera && selectedTableId > 0 && selectedTableAvailable) {
            session.setAttribute("customerTableId", selectedTableId);
            session.setAttribute("customerQrVerified", true);
            session.setAttribute("customerTableSelectionMode", "qr");
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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        int tableId = parseInt(request.getParameter("tableId"), 0);
        Object savedTableValue = session.getAttribute("customerTableId");
        int savedTableId = savedTableValue instanceof Integer ? (Integer) savedTableValue : 0;
        boolean alreadySelected = tableId > 0
                && tableId == savedTableId
                && "test".equals(session.getAttribute("customerTableSelectionMode"));
        boolean tableSelected = alreadySelected
                ? isTableAvailable(tableDAO.getTablesForCustomerCheckout(DEFAULT_BRANCH_ID, tableId), tableId)
                : tableDAO.selectTableForCustomer(DEFAULT_BRANCH_ID, tableId);

        if (tableId <= 0 || !tableSelected) {
            session.setAttribute("cartError", "Bàn đã chọn không còn trống. Vui lòng chọn bàn khác.");
            response.sendRedirect(request.getContextPath() + "/customer-qr-order");
            return;
        }

        if (savedTableId > 0 && savedTableId != tableId
                && "test".equals(session.getAttribute("customerTableSelectionMode"))) {
            tableDAO.releaseCustomerSelectedTable(savedTableId);
        }

        session.setAttribute("customerTableId", tableId);
        session.setAttribute("customerQrVerified", true);
        session.setAttribute("customerTableSelectionMode", "test");
        session.setAttribute("cartMessage", "Đã chọn bàn để test. Bạn có thể thêm món và gửi order ngay.");
        response.sendRedirect(request.getContextPath() + "/customer-qr-order");
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
