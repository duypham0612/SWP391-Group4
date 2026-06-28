<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Bán hàng</div><h1>Ca thu ngân</h1><p>payment.CashierShift — mở quỹ đầu ca, đóng &amp; chốt cuối ca</p></div>
</div>

<c:choose>
    <%-- Báo cáo ca vừa đóng --%>
    <c:when test="${not empty shift and not shift.open}">
        <div class="card form-card">
            <h3 style="margin-top:0">Báo cáo ca #${shift.cashierShiftId}</h3>
            <table class="table">
                <tr><td>Thu ngân</td><td>${shift.cashierName}</td></tr>
                <tr><td>Quỹ đầu ca</td><td><fmt:formatNumber value="${shift.openingCash}" maxFractionDigits="0"/> ₫</td></tr>
                <tr><td>Số hoá đơn đã thu</td><td>${shift.billCount}</td></tr>
                <tr><td>Tổng tiền thu (PAID)</td><td><strong><fmt:formatNumber value="${shift.totalCollected}" maxFractionDigits="0"/> ₫</strong></td></tr>
                <tr><td>Quỹ cuối ca (đếm tay)</td><td><fmt:formatNumber value="${shift.closingCash}" maxFractionDigits="0"/> ₫</td></tr>
            </table>
            <a class="btn btn-primary" href="${ctx}/cashier/shift">Mở ca mới</a>
        </div>
    </c:when>
    <%-- Đang có ca mở --%>
    <c:when test="${not empty current}">
        <div class="card form-card">
            <h3 style="margin-top:0">Ca đang mở #${current.cashierShiftId}</h3>
            <p>Mở lúc ${current.openedAt} · Quỹ đầu ca <strong><fmt:formatNumber value="${current.openingCash}" maxFractionDigits="0"/> ₫</strong></p>
            <div style="display:flex;gap:10px">
                <a class="btn btn-primary" href="${ctx}/cashier/checkout">Tới thanh toán →</a>
            </div>
            <form action="${ctx}/cashier/shift" method="post" style="margin-top:18px;display:flex;gap:10px;align-items:flex-end">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="close">
                <input type="hidden" name="shiftId" value="${current.cashierShiftId}">
                <div class="form-group" style="margin:0;width:200px"><label>Quỹ cuối ca (đếm tay)</label>
                    <input type="number" name="closingCash" class="form-control" min="0" step="1000" value="0"></div>
                <button type="submit" class="btn btn-ghost" onclick="return confirm('Đóng ca này?');">Đóng ca</button>
            </form>
        </div>
    </c:when>
    <%-- Chưa có ca --%>
    <c:otherwise>
        <div class="card form-card">
            <h3 style="margin-top:0">Mở ca</h3>
            <form action="${ctx}/cashier/shift" method="post" style="display:flex;gap:10px;align-items:flex-end">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="open">
                <div class="form-group" style="margin:0;width:220px"><label>Quỹ đầu ca (tiền mặt)</label>
                    <input type="number" name="openingCash" class="form-control" min="0" step="1000" value="0"></div>
                <button type="submit" class="btn btn-primary">Mở ca</button>
            </form>
        </div>
    </c:otherwise>
</c:choose>

<h3 style="margin-top:24px">Các ca gần đây</h3>
<c:choose>
    <c:when test="${empty history}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có ca nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th>#</th><th>Thu ngân</th><th>Mở</th><th>Đóng</th><th style="width:120px">Trạng thái</th></tr></thead>
            <tbody>
                <c:forEach var="s" items="${history}">
                    <tr>
                        <td><a href="${ctx}/cashier/shift?action=report&shiftId=${s.cashierShiftId}">#${s.cashierShiftId}</a></td>
                        <td>${s.cashierName}</td>
                        <td>${s.openedAt}</td>
                        <td><c:choose><c:when test="${s.open}"><span class="badge badge-making">Đang mở</span></c:when><c:otherwise>${s.closedAt}</c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${s.open}"><span class="badge badge-waiting">OPEN</span></c:when><c:otherwise><span class="badge badge-served">Đã đóng</span></c:otherwise></c:choose></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
