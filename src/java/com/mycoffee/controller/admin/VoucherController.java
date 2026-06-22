package com.mycoffee.controller.admin;

import com.mycoffee.dao.VoucherDAO;
import com.mycoffee.model.Voucher;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "VoucherController", urlPatterns = {"/admin/vouchers"})
public class VoucherController extends HttpServlet {

    // LẤY DỮ LIỆU VÀ HIỂN THỊ GIAO DIỆN (Tích hợp Phân trang 20 dòng, Bộ lọc Trạng thái, Realtime Search và Check trùng Code)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        VoucherDAO dao = new VoucherDAO();
        String action = request.getParameter("action");

        // --- 1. API KIỂM TRA TRÙNG MÃ CODE REAL-TIME QUA AJAX ---
        if ("checkVoucherCode".equals(action)) {
            response.setContentType("application/json;charset=UTF-8");
            try {
                String code = request.getParameter("code");
                boolean exists = false;
                if (code != null && !code.trim().isEmpty()) {
                    // Gọi hàm kiểm tra từ tầng DAO (Cần đảm bảo trong VoucherDAO đã viết hàm checkExistCode)
                    exists = dao.checkExistCode(code.trim().toUpperCase());
                }
                // Trả về JSON kết quả cho phía Giao diện JSP nhận biết
                response.getWriter().write("{\"exists\":" + exists + "}");
            } catch (Exception e) {
                response.getWriter().write("{\"exists\":false}");
            }
            return;
        }

        // --- 2. API ĐỌC THÔNG TIN VOUCHER TRẢ VỀ JSON ĐỂ ĐIỀN VÀO FORM SỬA ---
        if ("getVoucher".equals(action)) {
            response.setContentType("application/json;charset=UTF-8");
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                Voucher v = dao.getVoucherById(id);
                if (v != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String json = String.format(
                            "{\"id\":%d,\"code\":\"%s\",\"discount\":%.1f,\"isPercentage\":%b,\"minOrder\":%.1f,\"start\":\"%s\",\"end\":\"%s\",\"isActive\":%b}",
                            v.getVoucherID(), v.getVoucherCode(), v.getDiscountValue(), v.isIsPercentage(),
                            v.getMinOrderValue(), dateFormat.format(v.getStartDate()), dateFormat.format(v.getEndDate()), v.isIsActive()
                    );
                    response.getWriter().write(json);
                }
            } catch (Exception e) {
                response.getWriter().write("{}");
            }
            return;
        }

        // --- 3. ĐỌC THAM SỐ BỘ LỌC, PHÂN TRANG VÀ TÌM KIẾM ---
        String status = request.getParameter("status"); // all, active, ended, draft
        if (status == null || status.trim().isEmpty()) {
            status = "all";
        }

        String pageStr = request.getParameter("page");
        int page = 1;
        try {
            if (pageStr != null && !pageStr.trim().isEmpty()) {
                page = Integer.parseInt(pageStr);
                if (page < 1) {
                    page = 1;
                }
                if (page > 999) {
                    page = 999;
                }
            }
        } catch (NumberFormatException e) {
            page = 1;
        }

        int size = 20; 
        int offset = (page - 1) * size;

        String searchKeyword = request.getParameter("search");
        List<Voucher> voucherList;

        // --- 4. TRUY VẤN DỮ LIỆU TỪ DAO ---
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            voucherList = dao.searchVouchersWithPagination(searchKeyword, status, offset, size);
        } else {
            voucherList = dao.getVouchersWithPagination(status, offset, size);
        }

        // --- 5. ĐOẠN XỬ LÝ AJAX REALTIME TỰ SINH HTML TẠI CHỖ ---
        String xRequestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();

            if (voucherList == null || voucherList.isEmpty()) {
                out.print("<tr><td colspan='6' class='p-8 text-center text-slate-400 font-medium'>Không tìm thấy dữ liệu chương trình khuyến mãi nào.</td></tr>");
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

                for (Voucher v : voucherList) {
                    String minOrderTxt = (v.getMinOrderValue() > 0)
                            ? "Áp dụng đơn từ " + String.format("%,.0f", v.getMinOrderValue()) + "đ"
                            : "Áp dụng cho mọi đơn hàng";

                    String discountTxt = v.isIsPercentage()
                            ? "Giảm " + String.format("%.0f", v.getDiscountValue()) + "%"
                            : "Giảm " + String.format("%,.0f", v.getDiscountValue()) + "đ";

                    String statusTxt = v.getStatusLabel();

                    String discountBadge = v.isIsPercentage()
                            ? "<span class='px-2.5 py-1 rounded-full text-[10px] font-bold bg-cyan-50 text-cyan-600 border border-cyan-100'>" + discountTxt + "</span>"
                            : "<span class='px-2.5 py-1 rounded-full text-[10px] font-bold bg-slate-100 text-slate-600 border border-slate-200'>" + discountTxt + "</span>";

                    String statusBadge = "";
                    if ("Bản nháp".equals(statusTxt)) {
                        statusBadge = "<span class='inline-flex items-center gap-1.5 text-amber-600 bg-amber-50 px-2 py-0.5 rounded-md text-[10px] font-bold'><span class='w-1.5 h-1.5 rounded-full bg-amber-500'></span> Bản nháp</span>";
                    } else if ("Sắp diễn ra".equals(statusTxt)) {
                        statusBadge = "<span class='inline-flex items-center gap-1.5 text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md text-[10px] font-bold'><span class='w-1.5 h-1.5 rounded-full bg-indigo-500'></span> Sắp diễn ra</span>";
                    } else if ("Đang chạy".equals(statusTxt)) {
                        statusBadge = "<span class='inline-flex items-center gap-1.5 text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-md text-[10px] font-bold'><span class='w-1.5 h-1.5 rounded-full bg-emerald-500'></span> Đang chạy</span>";
                    } else {
                        statusBadge = "<span class='inline-flex items-center gap-1.5 text-slate-400 bg-slate-50 px-2 py-0.5 rounded-md text-[10px] font-bold'><span class='w-1.5 h-1.5 rounded-full bg-slate-400'></span> Đã kết thúc</span>";
                    }

                    out.print("<tr class='hover:bg-slate-50/40 transition-colors'>");
                    out.print("  <td class='p-4 pl-6'><div class='flex items-center gap-3'><div class='w-8 h-8 rounded-lg bg-cyan-50 flex items-center justify-center text-cyan-700'><i class='fa-solid fa-gift'></i></div><div><h4 class='font-bold text-slate-900 text-xs'>" + v.getVoucherCode() + "</h4><p class='text-[10px] text-slate-400 mt-0.5'>" + minOrderTxt + "</p></div></div></td>");
                    out.print("  <td class='p-4'><span class='px-2 py-1 bg-slate-100 text-slate-600 text-[11px] font-bold rounded-md tracking-wide border border-slate-200/50'>" + v.getVoucherCode() + "</span></td>");
                    out.print("  <td class='p-4'>" + discountBadge + "</td>");
                    out.print("  <td class='p-4 text-slate-500 font-normal'><div class='flex flex-col text-[11px]'><span>" + (v.getStartDate() != null ? sdf.format(v.getStartDate()) : "") + "</span><span class='text-slate-300 text-[9px] -mt-0.5'>đến</span><span>" + (v.getEndDate() != null ? sdf.format(v.getEndDate()) : "") + "</span></div></td>");
                    out.print("  <td class='p-4'>" + statusBadge + "</td>");
                    out.print("  <td class='p-4 pr-6 text-center text-slate-400 text-sm'>");
                    out.print("      <button onclick='openEditModal(" + v.getVoucherID() + ")' class='hover:text-cyan-700 transition mr-2' title='Chỉnh sửa'><i class='fa-regular fa-pen-to-square'></i></button>");
                    if (v.isIsActive()) {
                        out.print("      <button onclick='confirmStop(" + v.getVoucherID() + ")' class='hover:text-amber-600 transition mr-2' title='Ngừng kích hoạt'><i class='fa-regular fa-circle-stop'></i></button>");
                    }
                    out.print("      <button onclick='confirmDelete(" + v.getVoucherID() + ")' class='hover:text-red-600 transition' title='Xóa bỏ'><i class='fa-regular fa-trash-can'></i></button>");
                    out.print("  </td>");
                    out.print("</tr>");
                }
            }
            out.flush();
            return;
        }

        // --- 6. GỬI DỮ LIỆU SANG JSP HIỂN THỊ BAN ĐẦU ---
        request.setAttribute("vouchers", voucherList);
        request.setAttribute("searchKeyword", searchKeyword);
        request.setAttribute("currentStatus", status);
        request.setAttribute("currentPage", page);

        double[] stats = dao.getVoucherStats();
        request.setAttribute("activeCount", (int) stats[0]);
        request.setAttribute("endedCount", (int) stats[1]);
        request.setAttribute("totalUses", (int) stats[2]);
        request.setAttribute("avgProfitRate", stats[3]);

        request.getRequestDispatcher("/admin/voucher-management.jsp").forward(request, response);
    }

    // TIẾP NHẬN FORM (Xử lý cả Thêm mới, Chỉnh sửa, Ngừng kích hoạt, Xóa)
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        VoucherDAO dao = new VoucherDAO();
        String action = request.getParameter("action");

        try {
            if ("stop".equals(action)) {
                int id = Integer.parseInt(request.getParameter("voucherID"));
                dao.changeStatus(id, false);
                response.sendRedirect(request.getContextPath() + "/admin/vouchers");
                return;
            }

            if ("delete".equals(action)) {
                int id = Integer.parseInt(request.getParameter("voucherID"));
                dao.deleteVoucher(id);
                response.sendRedirect(request.getContextPath() + "/admin/vouchers");
                return;
            }

            String voucherCode = request.getParameter("voucherCode");
            double discountValue = Double.parseDouble(request.getParameter("discountValue"));
            boolean isPercentage = Boolean.parseBoolean(request.getParameter("isPercentage"));

            String minOrderStr = request.getParameter("minOrderValue");
            double minOrderValue = (minOrderStr != null && !minOrderStr.isEmpty()) ? Double.parseDouble(minOrderStr) : 0;

            String startStr = request.getParameter("startDate") + " 00:00:00";
            String endStr = request.getParameter("endDate") + " 23:59:59";
            java.sql.Timestamp startDate = java.sql.Timestamp.valueOf(startStr);
            java.sql.Timestamp endDate = java.sql.Timestamp.valueOf(endStr);

            boolean isActive = request.getParameter("isActive") != null;

            boolean isValidData = true;

            // Kiểm tra tính hợp lệ về logic ngày tháng và khoảng giá trị giảm
            if (startDate.after(endDate)) {
                isValidData = false;
            }

            if (isPercentage) {
                if (discountValue < 10 || discountValue > 100) {
                    isValidData = false;
                }
            } else {
                if (discountValue < 10000 || discountValue > 500000) {
                    isValidData = false;
                }
            }

            // --- KIỂM TRA TRÙNG MÃ CODE TỪ BACKEND KHI THÊM MỚI ---
            String idStr = request.getParameter("voucherID");
            boolean isEditMode = (idStr != null && !idStr.trim().isEmpty() && "edit".equals(action));

            if (!isEditMode && voucherCode != null) {
                // Nếu đang THÊM MỚI, tiến hành gọi dao để check trùng
                if (dao.checkExistCode(voucherCode.trim().toUpperCase())) {
                    isValidData = false;
                    System.out.println(">>> [Hệ thống] Phát hiện trùng mã Code '" + voucherCode + "' từ Backend. Đã chặn hành động thêm!");
                }
            }

            if (isValidData) {
                Voucher voucher = new Voucher();
                voucher.setVoucherCode(voucherCode.trim().toUpperCase()); // Luôn viết hoa mã code khi lưu
                voucher.setDiscountValue(discountValue);
                voucher.setIsPercentage(isPercentage);
                voucher.setMinOrderValue(minOrderValue);
                voucher.setStartDate(startDate);
                voucher.setEndDate(endDate);
                voucher.setIsActive(isActive);

                if (isEditMode) {
                    voucher.setVoucherID(Integer.parseInt(idStr));
                    dao.updateVoucher(voucher);
                } else {
                    dao.addVoucher(voucher);
                }
            } else {
                System.out.println(">>> [Hệ thống] Dữ liệu Voucher không hợp lệ hoặc bị trùng mã Code! Đã từ chối thực hiện yêu cầu.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        response.sendRedirect(request.getContextPath() + "/admin/vouchers");
    }
}
