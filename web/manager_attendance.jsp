<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="com.mycoffee.model.Attendance"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Lịch Sử Chấm Công Hôm Nay - My Coffee House</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        body { background-color: #f8f9fa; }
        .card-header-coffee { background-color: #2c3e50; color: white; }
    </style>
</head>
<body>

<div class="container mt-5">
    <div class="card shadow-sm">
        <div class="card-header card-header-coffee d-flex justify-content-between align-items-center py-3">
            <h4 class="mb-0"><i class="fa-solid fa-user-clock me-2"></i> LỊCH SỬ CHẤM CÔNG NGÀY HÔM NAY</h4>
            <span class="badge bg-light text-dark shadow-sm">Chi nhánh: Cầu Giấy</span>
        </div>
        <div class="card-body p-4">
            
            <p class="text-muted mb-4">Danh sách những nhân viên đã thực hiện Check-in vào ca làm việc ngày hôm nay tại chi nhánh.</p>

            <div class="table-responsive">
                <table class="table table-bordered table-hover align-middle mb-0">
                    <thead class="table-secondary text-center">
                        <tr>
                            <th>Mã Chấm Công</th>
                            <th>Mã Nhân Viên</th>
                            <th class="text-start">Tên Nhân Viên</th>
                            <th>Ca Làm Việc</th>
                            <th>Ngày Làm Việc</th>
                            <th>Giờ Vào Làm (Check-in)</th>
                            <th>Trạng Thái</th>
                        </tr>
                    </thead>
                    <tbody>
                        <%
                            List<Attendance> list = (List<Attendance>) request.getAttribute("danhSachChamCong");
                            if (list == null || list.isEmpty()) {
                        %>
                            <tr>
                                <td colspan="7" class="text-center text-muted py-4">Hôm nay chưa có nhân viên nào thực hiện check-in ca làm việc.</td>
                            </tr>
                        <%
                            } else {
                                for (Attendance item : list) {
                        %>
                            <tr>
                                <td class="text-center"><%= item.getAttendanceId() %></td>
                                <td class="text-center"><%= item.getEmployeeId() %></td>
                                <td><strong><%= item.getEmployeeName() %></strong></td>
                                <td class="text-center"><span class="badge bg-info text-dark"><%= item.getShiftName() %></span></td>
                                <td class="text-center"><%= item.getDate() %></td>
                                <td class="text-center text-primary fw-bold"><%= item.getCheckInTime() %></td>
                                <td class="text-center"><span class="badge bg-success"><i class="fa-solid fa-check me-1"></i> Đã đi làm</span></td>
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

</body>
</html>