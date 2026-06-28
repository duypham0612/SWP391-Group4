<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="welcome">
    <h1>Xin chào, ${sessionScope.authUser.fullName}</h1>
    <p>Quản trị hệ thống · toàn chuỗi</p>
</div>

<div class="card-grid">
    <a class="card stat" href="${ctx}/admin/report"><span class="label">Doanh thu hôm nay</span><span class="value"><fmt:formatNumber value="${summary.todayRevenue}" maxFractionDigits="0"/></span></a>
    <a class="card stat" href="${ctx}/admin/report"><span class="label">Tổng doanh thu</span><span class="value"><fmt:formatNumber value="${summary.revenue}" maxFractionDigits="0"/></span></a>
    <a class="card stat" href="${ctx}/admin/report"><span class="label">Tổng hoá đơn</span><span class="value">${summary.paidBills}</span></a>
    <a class="card stat" href="${ctx}/admin/product"><span class="label">Thực đơn</span><span class="value">→</span></a>
</div>

<div class="alert alert-info" style="margin-top:24px">
    Phân quyền · thực đơn · kho · nhân sự · bán hàng · thanh toán · QR khách đang hoạt động.
    Xem <a href="${ctx}/admin/report">báo cáo doanh thu toàn chuỗi</a>.
</div>

<jsp:include page="../layout/footer.jsp" />
