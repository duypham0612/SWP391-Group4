<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Thu ngân</div><h1>Đơn đến &amp; Bàn giao</h1></div>
    <a class="btn btn-ghost btn-sm" href="${ctx}/cashier/inbox#handoff">↻ Làm mới</a>
</div>

<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>

<c:if test="${not empty outOfStockItems}">
    <div class="alert alert-error">
        <strong>Món hiện không nhận đặt:</strong>
        <c:forEach var="m" items="${outOfStockItems}" varStatus="loop">
            <c:out value="${m.name}" /> (<c:out value="${m.stockMessage}" />)${loop.last ? '' : ' · '}
        </c:forEach>
        — nếu đơn đang xử lý có món bị chặn, hãy thông báo khách để đổi hoặc huỷ món.
    </div>
</c:if>
<c:if test="${not empty lowStockItems}">
    <div class="alert alert-info">
        <strong>Cảnh báo sắp hết — vẫn nhận đặt:</strong>
        <c:forEach var="m" items="${lowStockItems}" varStatus="loop">
            <c:out value="${m.name}" /> (<c:out value="${m.stockMessage}" />)${loop.last ? '' : ' · '}
        </c:forEach>
    </div>
</c:if>

<section id="handoff" style="scroll-margin-top:20px;margin-bottom:24px">
    <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:12px">
        <h2 style="margin:0">Món sẵn bàn giao</h2>
        <span class="badge badge-ready">${tickets.size() + pickedUpGroups.size()} nhóm đang chờ</span>
    </div>
    <jsp:include page="handoff/cards.jsp" />
</section>

<h2 id="orders" style="scroll-margin-top:20px">Đơn đang xử lý</h2>

<%-- Đơn treo từ ngày kinh doanh trước: quán đã đóng cửa nhiều giờ trước mốc cắt ngày nên khách của
     những đơn này đã về — quầy pha chế không nhận nữa, chỉ Thu ngân còn theo dõi và chốt được.
     Chúng đã được xếp lên đầu danh sách; dòng nhắc này để không phải đếm bằng mắt. --%>
<c:if test="${staleOrderCount > 0}">
    <div class="alert alert-error">
        <strong>${staleOrderCount} đơn treo từ ngày kinh doanh trước</strong> — nằm ở đầu danh sách.
        Huỷ món chưa pha, hoặc giao nốt món đã pha xong ở khu vực Bàn giao phía trên.
    </div>
</c:if>

<c:choose>
    <c:when test="${empty orders}">
        <div class="card empty-state"><div class="icon">📭</div><p>Không có đơn nào đang xử lý.</p></div>
    </c:when>
    <c:otherwise>
      <div class="kds-grid">
        <c:forEach var="o" items="${orders}">
            <article class="card kds-card ${o.stale ? 'kds-late' : 'kds-ok'}">
                <div class="kds-card__top">
                    <div>
                        <div>
                            <c:if test="${not empty o.pickupCode}"><span class="kds-code kds-code--lg"><c:out value="${o.pickupCode}" /></span> </c:if>
                            <strong class="kds-table">
                                <c:choose>
                                    <c:when test="${not empty o.tableNumber}"><c:out value="${o.tableNumber}" /></c:when>
                                    <c:otherwise>Nhận tại quầy</c:otherwise>
                                </c:choose>
                            </strong>
                        </div>
                        <div class="muted">Đơn #${o.orderId} · ${o.createdAtDisplay} · ${o.items.size()} dòng món</div>
                        <div class="muted">Tổng <strong><fmt:formatNumber value="${o.total}" type="number"/>đ</strong></div>
                    </div>
                    <div class="pickup-card__badges">
                        <c:if test="${o.stale}"><span class="badge badge-cancelled">Treo từ ngày trước</span></c:if>
                        <c:choose>
                            <c:when test="${o.source == 'QR'}"><span class="badge" style="background:var(--caramel);color:#fff">Đơn QR</span></c:when>
                            <c:otherwise><span class="badge" style="background:var(--coffee);color:#fff">Đơn quầy</span></c:otherwise>
                        </c:choose>
                        <c:choose>
                            <c:when test="${o.paymentStatus == 'PAID'}"><span class="badge badge-ready">Đã thanh toán</span></c:when>
                            <c:when test="${o.paymentStatus == 'ERROR'}"><span class="badge badge-cancelled">Lỗi thanh toán</span></c:when>
                            <c:otherwise><span class="badge badge-waiting">Đang thanh toán</span></c:otherwise>
                        </c:choose>
                    </div>
                </div>
                <div class="kds-ticket-items">
                    <c:forEach var="it" items="${o.items}">
                        <section class="kds-ticket-item ${it.status == 'BLOCKED' ? 'kds-issue' : ''}">
                            <div class="kds-ticket-item__head">
                                <strong>${it.quantity} × <c:out value="${it.productName}" /></strong>
                                <jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="${it.status}"/></jsp:include>
                            </div>
                            <c:if test="${it.hasIssue and not empty it.issueReason}">
                                <div class="kds-note" style="color:var(--st-cancelled)">⚠ <c:out value="${it.issueReason}" /></div>
                            </c:if>
                            <c:if test="${not empty it.note}"><div class="kds-note"><c:out value="${it.note}" /></div></c:if>
                            <c:if test="${it.status == 'BLOCKED'}">
                                <form action="${ctx}/cashier/inbox" method="post" onsubmit="return confirm('Huỷ món ${it.productName}? Món này đang bị chặn.');">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="cancelItem">
                                    <input type="hidden" name="orderItemId" value="${it.orderItemId}">
                                    <input type="hidden" name="reason" value="Huỷ món bị chặn từ Inbox">
                                    <button type="submit" class="btn btn-ghost btn-sm btn-full" style="color:var(--st-cancelled)">Huỷ món bị chặn</button>
                                </form>
                            </c:if>
                        </section>
                    </c:forEach>
                </div>
                <div class="kds-card__foot">
                    <c:if test="${o.orderType == 'TAKEAWAY' and o.paymentStatus != 'PAID'}">
                        <a class="btn btn-primary btn-sm" href="${ctx}/cashier/checkout?orderId=${o.orderId}">Thanh toán</a>
                    </c:if>
                    <c:choose>
                        <c:when test="${o.cancellable}">
                            <form action="${ctx}/cashier/inbox" method="post" onsubmit="return confirm('Huỷ đơn #${o.orderId}? Các món chưa pha sẽ bị huỷ.');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="void">
                                <input type="hidden" name="orderId" value="${o.orderId}">
                                <button type="submit" class="btn btn-ghost btn-sm" style="color:var(--st-cancelled)">Huỷ đơn</button>
                            </form>
                        </c:when>
                        <c:otherwise><small class="muted">Đang/đã pha — không thể huỷ đơn</small></c:otherwise>
                    </c:choose>
                </div>
            </article>
        </c:forEach>
      </div>
    </c:otherwise>
</c:choose>

<script>
document.querySelectorAll('#handoff form[data-confirm]').forEach(function(form) {
    form.addEventListener('submit', function(event) {
        if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    });
});
</script>

<jsp:include page="../layout/footer.jsp" />
