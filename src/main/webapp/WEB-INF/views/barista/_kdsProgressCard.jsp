<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<article class="card kds-card kds-${peakMode ? 'ok' : cardItem.slaTier}" data-kds-item-id="${cardItem.orderItemId}" data-owner="${cardItem.baristaId}" data-station="${cardItem.station}" data-order-type="${cardItem.orderType}" data-priority="${cardItem.priority}" data-sla-tier="${cardItem.slaTier}">
    <jsp:include page="_kdsCardHeader.jsp" />
    <div class="kds-assignee"><strong>Pha bởi:</strong> <c:out value="${cardItem.baristaName}" /><span>· nhận lúc ${cardItem.startedDisplay}<c:if test="${cardItem.makingSeconds ne null}"> · đang pha ${cardItem.makingDisplay}</c:if></span></div>
    <c:if test="${onShift and cardItem.baristaId == currentUserId}">
        <div class="kds-item-actions">
            <form action="${ctx}/barista/kds" method="post" class="kds-primary-form">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="markReady"><input type="hidden" name="orderItemId" value="${cardItem.orderItemId}">
                <button type="submit" class="btn btn-primary btn-full" ${cardItem.recipeMissing ? 'disabled' : ''}>Đã pha xong ${cardItem.quantity} món</button>
            </form>
            <div class="kds-subactions">
                <button type="button" class="btn btn-ghost btn-sm js-issue" data-item-id="${cardItem.orderItemId}" data-product-id="${cardItem.productId}" data-name="${cardItem.quantity} × <c:out value='${cardItem.productName}' />">Báo sự cố</button>
                <form action="${ctx}/barista/kds" method="post" data-confirm="Trả món về hàng chờ để barista khác có thể nhận?">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="returnQueue"><input type="hidden" name="orderItemId" value="${cardItem.orderItemId}">
                    <button type="submit" class="btn btn-ghost btn-sm">Trả lại hàng chờ</button>
                </form>
            </div>
        </div>
    </c:if>
    <c:if test="${onShift and cardItem.baristaId != currentUserId}"><div class="kds-owned-note">Món đang được barista khác thực hiện.</div></c:if>
</article>
