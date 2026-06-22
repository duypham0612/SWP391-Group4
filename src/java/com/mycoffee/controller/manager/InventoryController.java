package com.mycoffee.controller.manager;
import com.mycoffee.dao.InventoryDAO;
import com.mycoffee.model.Inventory;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@jakarta.servlet.annotation.WebServlet(name = "InventoryController", urlPatterns = {"/manager-inventory"})
public class InventoryController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Khởi tạo lớp DAO để giao tiếp với Database
        InventoryDAO dao = new InventoryDAO();

        // 2. Mặc định giả sử chúng ta đang xem kho của Chi nhánh có BranchID = 1 (Cầu Giấy)
        // Sau này khi làm chức năng Login, Duy có thể đổi thành lấy BranchID theo tài khoản của Manager đăng nhập.
        int branchId = 1;

        // 3. Gọi hàm lấy danh sách tồn kho từ Database thông qua DAO bạn đã viết
        List<Inventory> inventoryList = dao.getInventoryByBranch(branchId);

        // 4. Đính kèm danh sách này vào yêu cầu (request) với cái tên "khoHang" để chuyển tiếp sang trang JSP
        request.setAttribute("khoHang", inventoryList);

        // 5. Điều hướng (Forward) toàn bộ dữ liệu này sang file giao diện manager_inventory.jsp để hiển thị
        request.getRequestDispatcher("/views/manager/manager_inventory.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
