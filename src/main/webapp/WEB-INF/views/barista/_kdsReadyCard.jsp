<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- READY: barista không còn thao tác pha, chỉ theo dõi bàn giao (thu ngân mới là người nhận).
     Ba mốc bắt buộc hiển thị đủ: người pha, lúc bắt đầu, lúc hoàn thành. --%>
<article class="card kds-card kds-ready-card${cardItem.staleReady ? ' is-stale' : ''}" data-kds-item-id="${cardItem.orderItemId}" data-owner="ready" data-station="${cardItem.station}" data-order-type="${cardItem.orderType}" data-priority="${cardItem.priority}" data-sla-tier="ready">
    <div class="kds-card__top">
        <strong class="kds-product"><span class="kds-qty">${cardItem.quantity}×</span> <c:out value="${cardItem.productName}" /></strong>
        <span class="kds-sla kds-sla--${cardItem.staleReady ? 'warn' : 'ok'}">Chờ nhận ${cardItem.serveWaitDisplay}</span>
    </div>

    <c:if test="${cardItem.staleReady}">
        <div class="kds-note"><span class="kds-note__tag">⚠ Để lâu</span>Món pha xong đã lâu — kiểm tra chất lượng trước khi giao, hoặc làm lại.</div>
    </c:if>

    <div class="kds-ready-facts">
        <div><span>Pha bởi</span><strong><c:choose><c:when test="${not empty cardItem.preparedByName}"><c:out value="${cardItem.preparedByName}" /></c:when><c:otherwise>—</c:otherwise></c:choose></strong></div>
        <div><span>Bắt đầu</span><strong><c:choose><c:when test="${not empty cardItem.startedDisplay}">${cardItem.startedDisplay}</c:when><c:otherwise>—</c:otherwise></c:choose></strong></div>
        <div><span>Hoàn thành</span><strong><c:choose><c:when test="${not empty cardItem.doneDisplay}">${cardItem.doneDisplay}</c:when><c:otherwise>—</c:otherwise></c:choose></strong></div>
    </div>

    <div class="kds-meta-row">
        <span>Đang chờ thu ngân nhận<c:if test="${not empty cardItem.handoverLocation}"> · <c:out value="${cardItem.handoverLocation}" /></c:if></span>
        <span><c:choose><c:when test="${not empty cardItem.tableNumber}"><c:out value="${cardItem.tableNumber}" /></c:when><c:otherwise>${cardItem.orderTypeLabel}</c:otherwise></c:choose> · #${cardItem.orderId}</span>
    </div>

    <c:if test="${onShift}">
        <button type="button" class="btn btn-ghost btn-sm btn-full js-remake" data-item-id="${cardItem.orderItemId}" data-name="${cardItem.quantity} × <c:out value='${cardItem.productName}' />" ${cardItem.recipeMissing ? 'disabled' : ''}>Làm lại món</button>
    </c:if>
</article>
