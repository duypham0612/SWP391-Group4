package com.mycoffee.controller.admin;

import com.mycoffee.model.Product;
import com.mycoffee.service.ProductService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@WebServlet(name = "MenuManagementController", urlPatterns = {"/admin-menu"})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2MB Bộ nhớ đệm tạm
    maxFileSize = 1024 * 1024 * 10,       // Tối đa 10MB cho 1 file ảnh
    maxRequestSize = 1024 * 1024 * 50     // Tối đa tổng dung lượng request là 50MB
)
public class MenuManagementController extends HttpServlet {
    
    private final ProductService productService = new ProductService();
    // Thư mục lưu ảnh cố định không sợ mất ảnh
    private static final String FIXED_UPLOAD_DIR = "C:\\Users\\NTS\\Downloads\\coffee"; 

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        if (action == null) action = "list";

        switch (action) {
            case "delete":
                int id = Integer.parseInt(request.getParameter("id"));
                boolean isDeleted = productService.deleteProduct(id);
                
                if (isDeleted) {
                    request.getSession().setAttribute("successMessage", "Đã gỡ (ẩn) món ăn khỏi thực đơn thành công!");
                } else {
                    request.getSession().setAttribute("errorMessage", "Gỡ món ăn thất bại, vui lòng thử lại!");
                }
                response.sendRedirect(request.getContextPath() + "/admin-menu");
                break;

            case "restore":
                int restoreId = Integer.parseInt(request.getParameter("id"));
                boolean isRestored = productService.restoreProduct(restoreId);
                
                if (isRestored) {
                    request.getSession().setAttribute("successMessage", "Khôi phục sản phẩm quay lại menu kinh doanh thành công!");
                } else {
                    request.getSession().setAttribute("errorMessage", "Khôi phục món ăn thất bại, vui lòng thử lại!");
                }
                response.sendRedirect(request.getContextPath() + "/admin-menu?action=view-hidden");
                break;

            case "view-hidden":
                List<Product> hiddenList = productService.getHiddenProducts();
                request.setAttribute("productList", hiddenList);
                request.setAttribute("isArchiveView", true);
                request.getRequestDispatcher("/views/admin/menu_management.jsp").forward(request, response);
                break;
                
            case "list":
            default:
                List<Product> list = productService.getAllAvailableProducts();
                request.setAttribute("productList", list);
                request.setAttribute("isArchiveView", false);
                request.getRequestDispatcher("/views/admin/menu_management.jsp").forward(request, response);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        
        // Đọc dữ liệu văn bản từ Form
        String action = getValueFromPart(request.getPart("action"));
        String productName = getValueFromPart(request.getPart("productName"));
        double basePrice = Double.parseDouble(getValueFromPart(request.getPart("basePrice")));
        int categoryId = Integer.parseInt(getValueFromPart(request.getPart("categoryId")));
        String imageSource = getValueFromPart(request.getPart("imageSource"));
        
        // Đọc thêm trường Mô Tả từ form
        String description = getValueFromPart(request.getPart("productDescription")); 
        
        String finalImageUrl = "";

        // Kiểm tra nguồn ảnh
        if ("url".equals(imageSource)) {
            finalImageUrl = getValueFromPart(request.getPart("productImageUrl"));
        } else {
            Part filePart = request.getPart("productImageFile");
            String fileName = filePart.getSubmittedFileName();
            
            if (fileName != null && !fileName.isEmpty()) {
                File uploadFolder = new File(FIXED_UPLOAD_DIR);
                if (!uploadFolder.exists()) {
                    uploadFolder.mkdirs(); 
                }
                
                String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
                String finalServerPath = FIXED_UPLOAD_DIR + File.separator + uniqueFileName;
                
                try (InputStream input = filePart.getInputStream()) {
                    Files.copy(input, Paths.get(finalServerPath), StandardCopyOption.REPLACE_EXISTING);
                }
                
                // Trả về đường dẫn ảo cho Tomcat ánh xạ
                finalImageUrl = request.getContextPath() + "/external-images/" + uniqueFileName;
            } else if ("update".equals(action)) {
                finalImageUrl = getValueFromPart(request.getPart("productImageUrl"));
            }
        }

        if ("update".equals(action)) {
            int productId = Integer.parseInt(getValueFromPart(request.getPart("productId")));
            Product p = new Product(productId, productName, basePrice, categoryId, finalImageUrl);
            p.setDescription(description); // Set trường Mô Tả vào Model
            
            boolean isUpdated = productService.updateProduct(p);
            if (isUpdated) {
                request.getSession().setAttribute("successMessage", "Cập nhật thông tin món ăn thành công!");
            } else {
                request.getSession().setAttribute("errorMessage", "Cập nhật thất bại! Vui lòng kiểm tra lại dữ liệu đầu vào.");
            }
        } else {
            Product p = new Product(0, productName, basePrice, categoryId, finalImageUrl);
            p.setDescription(description); // Set trường Mô Tả vào Model
            
            boolean isAdded = productService.addProduct(p);
            if (isAdded) {
                request.getSession().setAttribute("successMessage", "Thêm món ăn mới vào thực đơn thành công!");
            } else {
                request.getSession().setAttribute("errorMessage", "Thêm mới thất bại! Vui lòng kiểm tra lại dữ liệu đầu vào.");
            }
        }
        
        response.sendRedirect(request.getContextPath() + "/admin-menu");
    }

    private String getValueFromPart(Part part) throws IOException {
        if (part == null) return "";
        try (InputStream is = part.getInputStream()) {
            return new String(is.readAllBytes(), "UTF-8").trim();
        }
    }
}

