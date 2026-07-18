<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<div class="kds-card__top">
    <div class="kds-card__id">
        <div class="kds-card__idrow">
            <strong class="kds-table"><c:choose><c:when test="${not empty cardItem.tableNumber}"><c:out value="${cardItem.tableNumber}" /></c:when><c:otherwise>Nhận tại quầy</c:otherwise></c:choose></strong>
            <span class="badge badge-neutral">${cardItem.orderTypeLabel}</span>
        </div>
        <div class="muted">Đơn #${cardItem.orderId} · ${cardItem.quantity} món</div>
    </div>
    <span class="kds-sla kds-sla--${cardItem.slaTier}">${cardItem.slaLabel}</span>
</div>
<div class="kds-ticket-item__head">
    <strong class="kds-product">${cardItem.quantity} × <c:out value="${cardItem.productName}" /></strong>
    <jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="${cardItem.status}" /></jsp:include>
</div>
<div class="kds-meta-row"><span>Tạo lúc ${cardItem.createdDisplay}</span><span>Nên xong trong ${kdsCritMin} phút</span></div>
<c:if test="${cardItem.priority}"><div class="kds-priority">LÀM LẠI – ƯU TIÊN · lần ${cardItem.remakeCount}</div></c:if>
<c:if test="${cardItem.hasIssue}"><div class="kds-issue"><strong>⚠ Sự cố:</strong> <c:out value="${cardItem.issueReason}" /></div></c:if>
<c:if test="${not empty cardItem.modifiers}"><div class="kds-mods"><c:forEach var="om" items="${cardItem.modifiers}"><span class="chip"><c:out value="${om.optionName}" /></span></c:forEach></div></c:if>
<c:if test="${not empty cardItem.note}"><div class="kds-note"><span class="kds-note__tag">Ghi chú</span> <c:out value="${cardItem.note}" /></div></c:if>
<c:if test="${cardItem.recipeMissing}"><div class="kds-note kds-note--warn"><span class="kds-note__tag">⚠ Chưa có công thức</span> Không thể hoàn thành hoặc làm lại cho đến khi cấu hình công thức.</div></c:if>
