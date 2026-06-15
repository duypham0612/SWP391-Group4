<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.mycoffee.model.Inventory"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Quản Lý Kho Nguyên Liệu - My Coffee House</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        body { background-color: #f8f9fa; }
        .card-header-coffee { background-color: #4e3629; color: white; }
        .warning-row { background-color: #fff3cd !important; color: #856404; font-weight: bold; }
        .badge-danger { background-color: #dc3545; color: white; }
        .badge-success { background-color: #198754; color: white; }
    </style>
</head>
<body>

<div class="container mt-5">
    <div class="card shadow-sm">
        <div class="card-header card-header-coffee d-flex justify-content-between align-items-center py-3">
            <h4 class="mb-0"><i class="fa-solid fa-warehouse me-2"></i> QUẢN LÝ TỒN KHO NGUYÊN LIỆU</h4>
            <span class="badge bg-light text-dark shadow-sm">Chi nhánh: Cầu Giấy (Mặc định)</span>
        </div>
        <div class="card-body p-4">
            
            <p class="text-muted mb-4">Danh sách chi tiết lượng tồn kho thực tế của các nguyên liệu pha chế tại chi nhánh hiện tại. Hệ thống sẽ tự động bật cảnh báo màu vàng đối với nguyên liệu sắp hết.</p>

            <div class="table-responsive">
                <table class="table table-bordered table-hover align-middle mb-0">
                    <thead class="table-dark text-center">
                        <tr>
                            <th>Mã NL</th>
                            <th class="text-start">Tên Nguyên Liệu</th>
                            <th>Số Lượng Tồn Kho</th>
                            <th>Đơn Vị Tính</th>
                            <th>Ngưỡng Tối Thiểu</th>
                            <th>Trạng Thái Cảnh Báo</th>
                            <th>Cập Nhật Lần Cuối</th>
                        </tr>
                    </thead>
                    <tbody>
                        <%
                            // Lấy danh sách khoHang được truyền từ Servlet sang
                            List<Inventory> list = (List<Inventory>) request.getAttribute("khoHang");
                            if (list == null || list.isEmpty()) {
                        %>
                            <tr>
                                <td colspan="7" class="text-center text-danger py-4">Không có dữ liệu nguyên liệu nào trong kho! Vui lòng kiểm tra lại SQL Server.</td>
                            </tr>
                        <%
                            } else {
                                for (Inventory item : list) {
                                    // Kiểm tra xem lượng tồn kho thực tế có nhỏ hơn ngưỡng yêu cầu tối thiểu không
                                    boolean isLowStock = item.getQuantity() < item.getMinRequired();
                                    String rowClass = isLowStock ? "warning-row" : "";
                        %>
                            <tr class="<%= rowClass %>">
                                <td class="text-center"><%= item.getIngredientId() %></td>
                                <td><strong><%= item.getIngredientName() %></strong></td>
                                <td class="text-center fs-5"><%= item.getQuantity() %></td>
                                <td class="text-center"><span class="badge bg-secondary py-2 px-3"><%= item.getUnit() %></span></td>
                                <td class="text-center text-muted"><%= item.getMinRequired() %></td>
                                <td class="text-center">
                                    <% if (isLowStock) { %>
                                        <span class="badge badge-danger p-2"><i class="fa-solid fa-triangle-exclamation me-1"></i> SẮP HẾT - CẦN NHẬP KHO</span>
                                    <% } else { %>
                                        <span class="badge badge-success p-2"><i class="fa-solid fa-circle-check me-1"></i> An toàn</span>
                                    <% } %>
                                </td>
                                <td class="text-center text-muted small"><%= item.getLastUpdated() %></td>
                            </tr>
                        <%
                                }
                            }
                        %>
                    </tbody>
                </table>
            </div>
            
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
