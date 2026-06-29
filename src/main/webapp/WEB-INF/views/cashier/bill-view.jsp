<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<style media="print">
    .sidebar, .topbar, .page-header a, .no-print { display:none !important; }
    .app-main { margin:0 !important; } .app-content { padding:0 !important; }
</style>

<div class="page-header">
    <div><div class="eyebrow">Hoá đơn #${bill.billId}</div><h1>Chi tiết hoá đơn</h1>
        <p><c:if test="${not empty bill.tableNumber}">${bill.tableNumber} · </c:if>${bill.paidAt != null ? bill.paidAt : bill.createdAt}</p></div>
    <span>
        <button type="button" class="btn btn-ghost no-print" onclick="window.print()">In / Tái in</button>
        <a class="btn btn-ghost" href="${ctx}/cashier/history">← Lịch sử</a>
    </span>
</div>

<div class="card" style="max-width:520px">
    <table class="table">
        <thead><tr><th>Món</th><th style="width:70px">SL</th><th style="width:140px">Thành tiền</th></tr></thead>
        <tbody>
            <c:forEach var="bi" items="${bill.items}">
                <tr><td>${bi.productName}</td><td>${bi.quantity}</td><td><fmt:formatNumber value="${bi.amount}" maxFractionDigits="0"/> ₫</td></tr>
            </c:forEach>
        </tbody>
    </table>
    <div style="max-width:300px;margin-left:auto;font-size:.95rem;margin-top:10px">
        <div style="display:flex;justify-content:space-between"><span>Tạm tính</span><span><fmt:formatNumber value="${bill.subtotal}" maxFractionDigits="0"/> ₫</span></div>
        <c:if test="${bill.discountAmount > 0}"><div style="display:flex;justify-content:space-between;color:var(--st-ready)"><span>Giảm ${bill.voucherCode}</span><span>−<fmt:formatNumber value="${bill.discountAmount}" maxFractionDigits="0"/> ₫</span></div></c:if>
        <div style="display:flex;justify-content:space-between"><span>VAT 8%</span><span><fmt:formatNumber value="${bill.vatAmount}" maxFractionDigits="0"/> ₫</span></div>
        <div style="display:flex;justify-content:space-between;font-weight:700;border-top:1px solid var(--line);padding-top:6px;margin-top:6px"><span>Tổng cộng</span><span><fmt:formatNumber value="${bill.totalAmount}" maxFractionDigits="0"/> ₫</span></div>
    </div>
    <p style="margin-top:14px">Trạng thái:
        <c:choose>
            <c:when test="${bill.status == 'PAID'}"><span class="badge badge-ready">Đã thu (${bill.paymentMethod})</span></c:when>
            <c:when test="${bill.status == 'VOID'}"><span class="badge badge-cancelled">Huỷ</span></c:when>
            <c:when test="${bill.status == 'REFUND'}"><span class="badge badge-cancelled">Đã hoàn</span></c:when>
            <c:otherwise><span class="badge badge-waiting">Chưa thu</span></c:otherwise>
        </c:choose>
    </p>
    <c:if test="${bill.status == 'UNPAID'}">
        <form action="${ctx}/cashier/history" method="post" class="no-print" style="margin-top:10px;display:flex;gap:8px;align-items:flex-end;flex-wrap:wrap"
              onsubmit="return confirm('Huỷ hoá đơn này?');">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="void">
            <input type="hidden" name="billId" value="${bill.billId}">
            <div class="form-group" style="margin:0;flex:1;min-width:200px">
                <label>Lý do huỷ (bắt buộc)</label>
                <input type="text" name="reason" class="form-control" maxlength="255" required placeholder="VD: khách đổi món, nhập sai...">
            </div>
            <button type="submit" class="btn btn-ghost btn-sm" style="color:var(--st-cancelled)">Huỷ hoá đơn</button>
        </form>
    </c:if>
    <c:if test="${bill.status == 'PAID'}">
        <form action="${ctx}/cashier/history" method="post" class="no-print" style="margin-top:10px;display:flex;gap:8px;align-items:flex-end;flex-wrap:wrap"
              onsubmit="return confirm('Hoàn tiền hoá đơn ĐÃ thanh toán này?');">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="refund">
            <input type="hidden" name="billId" value="${bill.billId}">
            <div class="form-group" style="margin:0;flex:1;min-width:200px">
                <label>Lý do hoàn tiền (bắt buộc)</label>
                <input type="text" name="reason" class="form-control" maxlength="255" required placeholder="VD: khách trả món, tính nhầm...">
            </div>
            <button type="submit" class="btn btn-ghost btn-sm" style="color:var(--st-cancelled)">Hoàn hoá đơn</button>
        </form>
    </c:if>
</div>

<jsp:include page="../layout/footer.jsp" />
