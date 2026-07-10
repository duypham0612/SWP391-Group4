<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Bán hàng</div><h1>Lịch sử hoá đơn</h1><p>Phạm vi: <strong>${scopeLabel}</strong></p></div>
    <c:if test="${hasOpenShift}">
        <span>
            <a class="btn btn-ghost btn-sm" href="${ctx}/cashier/history">Theo ca</a>
            <a class="btn btn-ghost btn-sm" href="${ctx}/cashier/history?scope=branch">Toàn chi nhánh</a>
        </span>
    </c:if>
</div>

<%-- R4 · Lọc theo hình thức thanh toán (chuyển khoản / tiền mặt) — giữ nguyên phạm vi đang xem --%>
<c:set var="sc" value="${empty param.scope ? '' : '&scope='}${param.scope}" />
<div style="margin:0 0 14px">
    <a class="btn btn-sm ${empty param.method ? 'btn-primary' : 'btn-ghost'}" href="${ctx}/cashier/history${empty param.scope ? '' : '?scope='}${param.scope}">Tất cả</a>
    <a class="btn btn-sm ${param.method == 'CASH' ? 'btn-primary' : 'btn-ghost'}" href="${ctx}/cashier/history?method=CASH${sc}">Tiền mặt</a>
    <a class="btn btn-sm ${param.method == 'TRANSFER' ? 'btn-primary' : 'btn-ghost'}" href="${ctx}/cashier/history?method=TRANSFER${sc}">Chuyển khoản</a>
    <a class="btn btn-sm ${param.method == 'QR_BANK' ? 'btn-primary' : 'btn-ghost'}" href="${ctx}/cashier/history?method=QR_BANK${sc}">QR ngân hàng</a>
</div>

<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>

<c:choose>
    <c:when test="${empty bills}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có hoá đơn nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th style="width:60px">#</th><th>Bàn</th><th style="width:150px">Tổng tiền</th><th style="width:120px">Hình thức</th><th style="width:120px">Trạng thái</th><th>Lúc</th><th style="width:90px"></th></tr></thead>
            <tbody>
                <c:forEach var="b" items="${bills}">
                    <tr>
                        <td>${b.billId}</td>
                        <td><c:choose><c:when test="${not empty b.tableNumber}">${b.tableNumber}</c:when><c:otherwise><span class="muted">Đem về</span></c:otherwise></c:choose></td>
                        <td><strong><fmt:formatNumber value="${b.totalAmount}" maxFractionDigits="0"/> ₫</strong></td>
                        <td>
                            <c:choose>
                                <c:when test="${b.paymentMethod == 'CASH'}">Tiền mặt</c:when>
                                <c:when test="${b.paymentMethod == 'TRANSFER'}">Chuyển khoản</c:when>
                                <c:when test="${b.paymentMethod == 'QR_BANK'}">QR ngân hàng</c:when>
                                <c:otherwise><span class="muted">—</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${b.status == 'PAID'}"><span class="badge badge-ready">Đã thu</span></c:when>
                                <c:when test="${b.status == 'VOID'}"><span class="badge badge-cancelled">Huỷ</span></c:when>
                                <c:when test="${b.status == 'REFUND'}"><span class="badge badge-cancelled">Đã hoàn</span></c:when>
                                <c:otherwise><span class="badge badge-waiting">Chưa thu</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>${b.paidAt != null ? b.paidAt : b.createdAt}</td>
                        <td><a class="btn btn-ghost btn-sm" href="${ctx}/cashier/history?action=view&billId=${b.billId}">Xem</a></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
