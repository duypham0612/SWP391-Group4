<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Thu ngân</div><h1>Đơn đến (Inbox)</h1><p>Giám sát đơn quầy + QR đang xử lý — huỷ đơn sai (không chặn luồng vào bếp)</p></div>
</div>

<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>

<c:choose>
    <c:when test="${empty orders}">
        <div class="card empty-state"><div class="icon">📭</div><p>Không có đơn nào đang xử lý.</p></div>
    </c:when>
    <c:otherwise>
        <c:forEach var="o" items="${orders}">
            <div class="card" style="margin-bottom:14px">
                <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:12px;flex-wrap:wrap">
                    <div>
                        <strong>Đơn #${o.orderId}</strong>
                        <c:choose>
                            <c:when test="${o.source == 'QR'}"><span class="badge" style="background:var(--caramel);color:#fff">QR</span></c:when>
                            <c:otherwise><span class="badge" style="background:var(--coffee);color:#fff">Quầy</span></c:otherwise>
                        </c:choose>
                        <c:if test="${not empty o.tableNumber}"><span class="badge" style="background:var(--latte)">Bàn ${o.tableNumber}</span></c:if>
                        <c:if test="${o.orderType == 'TAKEAWAY'}"><span class="badge" style="background:var(--latte)">Mang đi</span></c:if>
                        <c:choose>
                            <c:when test="${o.paymentStatus == 'PAID'}"><span class="badge badge-ready">Đã thanh toán</span></c:when>
                            <c:when test="${o.paymentStatus == 'ERROR'}"><span class="badge badge-cancelled">Lỗi thanh toán</span></c:when>
                            <c:otherwise><span class="badge badge-waiting">Đang thanh toán</span></c:otherwise>
                        </c:choose>
                        <br><small style="color:var(--muted)">${o.createdAt} · ${o.items.size()} món · Tổng <fmt:formatNumber value="${o.total}" type="number"/>đ</small>
                    </div>
                    <c:choose>
                        <c:when test="${o.cancellable}">
                            <form action="${ctx}/cashier/inbox" method="post" onsubmit="return confirm('Huỷ đơn #${o.orderId}? Các món chưa pha sẽ bị huỷ.');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="void">
                                <input type="hidden" name="orderId" value="${o.orderId}">
                                <button type="submit" class="btn btn-ghost btn-sm" style="color:var(--st-cancelled)">Huỷ đơn</button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <small style="color:var(--muted)">Đang/đã pha — không thể huỷ</small>
                        </c:otherwise>
                    </c:choose>
                </div>
                <table class="table" style="margin-top:10px">
                    <thead><tr><th>Món</th><th style="width:70px">SL</th><th style="width:120px">Trạng thái</th><th>Ghi chú</th></tr></thead>
                    <tbody>
                        <c:forEach var="it" items="${o.items}">
                            <tr>
                                <td>${it.productName}</td>
                                <td>${it.quantity}</td>
                                <td><jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="${it.status}"/></jsp:include></td>
                                <td>${it.note}</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:forEach>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
