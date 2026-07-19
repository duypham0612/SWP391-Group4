<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- READY: barista không còn thao tác pha, chỉ theo dõi bàn giao (thu ngân mới là người nhận).
     Ba mốc bắt buộc hiển thị đủ: người pha, lúc bắt đầu, lúc hoàn thành. --%>
<article class="card kds-card kds-ready-card${cardItem.staleReady ? ' is-stale' : ''}" tabindex="0" data-kds-item-id="${cardItem.orderItemId}" data-cups="${cardItem.quantity}" data-owner="${cardItem.preparedBy}" data-station="${cardItem.station}" data-order-type="${cardItem.orderType}" data-priority="${cardItem.priority}" data-sla-tier="ready">
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

    <div class="kds-card__identity">
        <div class="kds-card__destination"><span>Bàn giao tại</span><strong><c:if test="${not empty cardItem.pickupCode}"><span class="kds-code"><c:out value="${cardItem.pickupCode}" /></span> · </c:if><c:choose><c:when test="${not empty cardItem.handoverLocation}"><c:out value="${cardItem.handoverLocation}" /></c:when><c:otherwise>Chưa chọn vị trí</c:otherwise></c:choose></strong></div>
        <div class="kds-meta-row"><span>Đang chờ thu ngân nhận</span><span><c:choose><c:when test="${not empty cardItem.tableNumber}"><c:out value="${cardItem.tableNumber}" /></c:when><c:otherwise>${cardItem.orderTypeLabel}</c:otherwise></c:choose> · Đơn #${cardItem.orderId}</span></div>
    </div>

    <c:if test="${onShift}">
        <details class="kds-card-menu"><summary>Thao tác khác</summary><div class="kds-card-menu__panel"><button type="button" class="btn btn-ghost btn-sm btn-full js-remake" data-item-id="${cardItem.orderItemId}" data-name="${cardItem.quantity} × <c:out value='${cardItem.productName}' />" ${cardItem.recipeMissing ? 'disabled' : ''}>Làm lại món</button></div></details>
    </c:if>
</article>
