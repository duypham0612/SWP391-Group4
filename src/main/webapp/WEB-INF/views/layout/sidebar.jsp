<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="u" value="${sessionScope.authUser}" />
<aside class="sidebar">
    <div class="sidebar-brand">
        <span class="logo">C</span>
        <span class="brand-text">
            <span class="brand-name">Cà Phê Chain</span>
            <span class="brand-sub">Quản trị hệ thống</span>
        </span>
    </div>
    <ul class="nav">
        <li class="nav-section">Tổng quan</li>
        <li><a class="active" href="${ctx}/dashboard">Bảng điều khiển</a></li>

        <c:choose>
            <c:when test="${u.roleCode == 'ADMIN'}">
                <li class="nav-section">Tổ chức</li>
                <li><a href="${ctx}/admin/user">Nhân sự</a></li>
                <li><a href="${ctx}/admin/branch">Chi nhánh</a></li>
                <li class="nav-section">Thực đơn &amp; công thức</li>
                <li><a href="${ctx}/admin/category">Danh mục</a></li>
                <li><a href="${ctx}/admin/product">Sản phẩm</a></li>
                <li><a href="${ctx}/admin/ingredient">Nguyên liệu</a></li>
                <li><a href="${ctx}/admin/recipe">Công thức</a></li>
                <li><a href="${ctx}/admin/modifier">Tuỳ chọn (Modifier)</a></li>
                <li><a href="${ctx}/admin/branch-menu">Menu chi nhánh</a></li>
                <li class="nav-section">Khuyến mãi</li>
                <li><a href="${ctx}/admin/voucher">Voucher</a></li>
                <li class="nav-section">Báo cáo</li>
                <li><a href="${ctx}/admin/report">Doanh thu toàn chuỗi</a></li>
            </c:when>
            <c:when test="${u.roleCode == 'BRANCH_MANAGER'}">
                <li class="nav-section">Kho</li>
                <li><a href="${ctx}/manager/inventory">Tồn kho &amp; cảnh báo</a></li>
                <li><a href="${ctx}/manager/receipt">Nhập kho</a></li>
                <li><a href="${ctx}/manager/supplier">Nhà cung cấp</a></li>
                <li><a href="${ctx}/manager/reconciliation">Đối soát tồn</a></li>
                <li class="nav-section">Nhân sự</li>
                <li><a href="${ctx}/manager/shift">Ca làm</a></li>
                <li><a href="${ctx}/manager/attendance">Chấm công</a></li>
                <li><a href="${ctx}/manager/payroll">Bảng lương</a></li>
                <li class="nav-section">Thực đơn</li>
                <li><a href="${ctx}/manager/menu">Menu chi nhánh</a></li>
            </c:when>
            <c:when test="${u.roleCode == 'CASHIER'}">
                <li class="nav-section">Bán hàng</li>
                <li><a href="${ctx}/cashier/table">Sơ đồ bàn</a></li>
                <li><a href="${ctx}/cashier/pos">POS / Đặt món</a></li>
                <li class="nav-section">Thu ngân</li>
                <li><a href="${ctx}/cashier/shift">Ca thu ngân</a></li>
                <li><a href="${ctx}/cashier/checkout">Thanh toán</a></li>
                <li><a href="${ctx}/cashier/history">Lịch sử hoá đơn</a></li>
            </c:when>
            <c:when test="${u.roleCode == 'BARISTA'}">
                <li class="nav-section">Pha chế</li>
                <li><a href="${ctx}/barista/kds">Hàng chờ (KDS)</a></li>
                <li><a href="${ctx}/barista/pickup">Món sẵn lấy</a></li>
                <li><a href="${ctx}/barista/prep">Pha sẵn (Prep)</a></li>
                <li><a href="${ctx}/barista/waste">Hao hụt / Làm lại</a></li>
                <li><a href="${ctx}/barista/eightysix">Hết món (86)</a></li>
                <li><a href="${ctx}/barista/recipe">Tra cứu công thức</a></li>
                <li><a href="${ctx}/barista/handover">Bàn giao ca</a></li>
            </c:when>
        </c:choose>
    </ul>
    <div class="sidebar-foot">SWP391 · phiên bản 0.2</div>
</aside>
