<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<article class="card kds-card kds-ready-card" data-kds-item-id="${cardItem.orderItemId}" data-owner="ready" data-station="${cardItem.station}" data-order-type="${cardItem.orderType}" data-priority="${cardItem.priority}" data-sla-tier="${cardItem.slaTier}">
    <jsp:include page="_kdsCardHeader.jsp" />
    <div class="kds-ready-facts">
        <div><span>Pha bởi</span><strong><c:choose><c:when test="${not empty cardItem.preparedByName}"><c:out value="${cardItem.preparedByName}" /></c:when><c:otherwise>—</c:otherwise></c:choose></strong></div>
        <div><span>Hoàn thành</span><strong>${cardItem.doneDisplay}</strong></div>
        <div><span>Chờ nhận</span><strong>${cardItem.serveWaitDisplay}</strong></div>
    </div>
    <div class="kds-handover"><span class="kds-refresh__dot"></span><strong>Đang chờ nhận</strong><c:if test="${not empty cardItem.handoverLocation}"> · <c:out value="${cardItem.handoverLocation}" /></c:if></div>
    <c:if test="${onShift}">
        <button type="button" class="btn btn-ghost btn-sm btn-full js-remake" data-item-id="${cardItem.orderItemId}" data-name="Dòng món #${cardItem.orderItemId}" ${cardItem.recipeMissing ? 'disabled' : ''}>Làm lại món</button>
    </c:if>
</article>
