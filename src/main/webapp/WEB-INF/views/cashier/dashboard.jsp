<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="welcome">
    <h1>Xin chào, ${sessionScope.authUser.fullName}</h1>
    <p>Thu ngân · ${sessionScope.authUser.branchName}</p>
</div>

<%-- R2 · Số liệu hôm nay --%>
<div class="card-grid">
    <div class="card stat"><span class="label">Doanh thu hôm nay</span><span class="value"><fmt:formatNumber value="${todayRevenue}" maxFractionDigits="0"/> ₫</span></div>
    <div class="card stat"><span class="label">Số đơn đã thực hiện</span><span class="value">${todayBillCount}</span></div>
</div>

<h3 style="margin-top:24px">Lối tắt</h3>
<div class="card-grid">
    <a class="card stat" href="${ctx}/cashier/table"><span class="label">Sơ đồ bàn</span><span class="value">→</span></a>
    <a class="card stat" href="${ctx}/cashier/pos"><span class="label">POS / Đặt món</span><span class="value">→</span></a>
    <a class="card stat" href="${ctx}/cashier/inbox"><span class="label">Đơn đến (Inbox)</span><span class="value">→</span></a>
    <a class="card stat" href="${ctx}/cashier/checkout"><span class="label">Thanh toán</span><span class="value">→</span></a>
    <a class="card stat" href="${ctx}/cashier/shift"><span class="label">Ca thu ngân</span><span class="value">→</span></a>
    <a class="card stat" href="${ctx}/cashier/history"><span class="label">Lịch sử hoá đơn</span><span class="value">→</span></a>
</div>

<div class="alert alert-info" style="margin-top:24px">
    Sơ đồ bàn · POS · Ca thu ngân · Thanh toán (tách/gộp bill, voucher) đã mở.
</div>

<jsp:include page="../layout/footer.jsp" />
