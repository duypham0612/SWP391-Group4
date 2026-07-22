<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Một ly ở dạng DÒNG GỌN cho chế độ gộp theo món. Mang đủ data-* như card lớn để bộ lọc,
     đổi mới (polling) và modal thao tác chạy y nguyên. Thao tác chính nằm inline, phụ nằm trong "⋯". --%>
<c:set var="rowTier" value="${cardItem.status == 'READY' ? 'ready' : (peakMode ? 'ok' : cardItem.slaTier)}" />
<article class="kds-brow kds-brow--${rowTier}" tabindex="0"
         data-kds-item-id="${cardItem.orderItemId}" data-cups="${cardItem.quantity}"
         data-owner="${cardItem.status == 'MAKING' ? cardItem.baristaId : (cardItem.status == 'READY' ? cardItem.preparedBy : (cardItem.status == 'BLOCKED' ? 'blocked' : 'unassigned'))}"
         data-station="${cardItem.station}" data-order-type="${cardItem.orderType}"
         data-priority="${cardItem.priority}" data-sla-tier="${cardItem.slaTier}">
    <div class="kds-brow__line">
        <span class="kds-brow__qty">${cardItem.quantity}×</span>
        <span class="kds-brow__where">
            <c:if test="${not empty cardItem.pickupCode}"><span class="kds-code"><c:out value="${cardItem.pickupCode}" /></span> </c:if>
            <c:choose><c:when test="${not empty cardItem.tableNumber}"><c:out value="${cardItem.tableNumber}" /></c:when><c:otherwise>${cardItem.orderTypeLabel} #${cardItem.orderId}</c:otherwise></c:choose>
        </span>
        <c:choose>
            <c:when test="${cardItem.status == 'READY'}"><span class="kds-brow__sla kds-sla--${cardItem.staleReady ? 'warn' : 'ok'}">chờ nhận ${cardItem.serveWaitDisplay}</span></c:when>
            <c:when test="${cardItem.status == 'BLOCKED'}"><span class="kds-brow__sla kds-sla--blocked">Cần xử lý</span></c:when>
            <c:when test="${peakMode and cardItem.seqNo > 0}"><span class="kds-brow__sla kds-seq">Pha thứ ${cardItem.seqNo}</span></c:when>
            <c:otherwise><span class="kds-brow__sla kds-sla--${cardItem.slaTier}">${cardItem.slaLabel}</span></c:otherwise>
        </c:choose>
    </div>

    <c:if test="${cardItem.priority or not empty cardItem.note or not empty cardItem.modifiers or cardItem.hasIssue or cardItem.recipeMissing}">
        <div class="kds-brow__tags">
            <c:if test="${cardItem.priority}"><span class="kds-brow__tag kds-brow__tag--pri">Làm lại · lần ${cardItem.remakeCount}</span></c:if>
            <c:forEach var="om" items="${cardItem.modifiers}"><span class="kds-brow__tag"><c:out value="${om.optionName}" /></span></c:forEach>
            <c:if test="${not empty cardItem.note}"><span class="kds-brow__tag kds-brow__tag--note">Ghi chú: <c:out value="${cardItem.note}" /></span></c:if>
            <c:if test="${cardItem.hasIssue and cardItem.status == 'BLOCKED'}"><span class="kds-brow__tag kds-brow__tag--issue">⚠ <c:out value="${cardItem.issueReason}" /></span></c:if>
            <c:if test="${cardItem.recipeMissing}"><span class="kds-brow__tag kds-brow__tag--issue">⚠ Chưa có công thức</span></c:if>
        </div>
    </c:if>

    <c:choose>
        <%-- CHỜ PHA --%>
        <c:when test="${cardItem.status == 'WAITING'}">
            <c:if test="${onShift}">
                <div class="kds-brow__act">
                    <form action="${ctx}/barista/kds" method="post">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="start"><input type="hidden" name="orderItemId" value="${cardItem.orderItemId}">
                        <button type="submit" class="btn btn-primary btn-sm">Nhận pha</button>
                    </form>
                    <details class="kds-card-menu"><summary>⋯</summary><div class="kds-card-menu__panel">
                        <button type="button" class="btn btn-ghost btn-sm btn-full js-issue" data-item-id="${cardItem.orderItemId}" data-product-id="${cardItem.productId}" data-name="${cardItem.quantity} × <c:out value='${cardItem.productName}' />">Báo sự cố</button>
                    </div></details>
                </div>
            </c:if>
        </c:when>
        <%-- ĐANG PHA --%>
        <c:when test="${cardItem.status == 'MAKING'}">
            <c:if test="${onShift and cardItem.baristaId == currentUserId}">
                <div class="kds-brow__act">
                    <form action="${ctx}/barista/kds" method="post" class="kds-brow__ready">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="markReady"><input type="hidden" name="orderItemId" value="${cardItem.orderItemId}">
                        <select name="handoverLocation" class="kds-brow__sel"><option value="">— nơi đặt —</option><c:forEach var="loc" items="${handoverLocations}"><option value="${loc}">${loc}</option></c:forEach></select>
                        <button type="submit" class="btn btn-primary btn-sm" ${cardItem.recipeMissing ? 'disabled' : ''}>Xong</button>
                    </form>
                    <details class="kds-card-menu"><summary>⋯</summary><div class="kds-card-menu__panel kds-subactions">
                        <button type="button" class="btn btn-ghost btn-sm js-issue" data-item-id="${cardItem.orderItemId}" data-product-id="${cardItem.productId}" data-name="${cardItem.quantity} × <c:out value='${cardItem.productName}' />">Báo sự cố</button>
                        <button type="button" class="btn btn-ghost btn-sm js-remake" data-item-id="${cardItem.orderItemId}" data-name="${cardItem.quantity} × <c:out value='${cardItem.productName}' />" ${cardItem.recipeMissing ? 'disabled' : ''}>Làm lại</button>
                        <form action="${ctx}/barista/kds" method="post" data-confirm="Trả món về hàng chờ để barista khác nhận?">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="returnQueue"><input type="hidden" name="orderItemId" value="${cardItem.orderItemId}">
                            <button type="submit" class="btn btn-ghost btn-sm">Trả lại chờ</button>
                        </form>
                    </div></details>
                </div>
            </c:if>
            <c:if test="${onShift and cardItem.baristaId != currentUserId}"><span class="kds-brow__by">Đang pha: <c:out value="${cardItem.baristaName}" /></span></c:if>
        </c:when>
        <%-- ĐÃ PHA XONG --%>
        <c:when test="${cardItem.status == 'READY'}">
            <span class="kds-brow__by">✓ <c:choose><c:when test="${not empty cardItem.preparedByName}"><c:out value="${cardItem.preparedByName}" /></c:when><c:otherwise>đã pha</c:otherwise></c:choose><c:if test="${not empty cardItem.handoverLocation}"> · <c:out value="${cardItem.handoverLocation}" /></c:if></span>
            <c:if test="${onShift}">
                <details class="kds-card-menu"><summary>⋯</summary><div class="kds-card-menu__panel">
                    <button type="button" class="btn btn-ghost btn-sm btn-full js-remake" data-item-id="${cardItem.orderItemId}" data-name="${cardItem.quantity} × <c:out value='${cardItem.productName}' />" ${cardItem.recipeMissing ? 'disabled' : ''}>Làm lại món</button>
                </div></details>
            </c:if>
        </c:when>
        <%-- BỊ CHẶN --%>
        <c:when test="${cardItem.status == 'BLOCKED'}">
            <c:if test="${onShift}">
                <div class="kds-brow__act">
                    <form action="${ctx}/barista/kds" method="post" data-confirm="Nguyên liệu/thiết bị đã sẵn sàng lại? Món sẽ về hàng chờ pha.">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="unblock"><input type="hidden" name="orderItemId" value="${cardItem.orderItemId}">
                        <button type="submit" class="btn btn-primary btn-sm js-unblock" data-item-id="${cardItem.orderItemId}" data-product-id="${cardItem.productId}" data-name="${cardItem.quantity} × <c:out value='${cardItem.productName}' />">Trả về chờ pha</button>
                    </form>
                </div>
            </c:if>
        </c:when>
    </c:choose>
</article>
