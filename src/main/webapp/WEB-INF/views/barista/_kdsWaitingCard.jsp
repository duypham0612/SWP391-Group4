<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<article class="card kds-card kds-${cardItem.slaTier}" data-kds-item-id="${cardItem.orderItemId}" data-owner="unassigned" data-station="${cardItem.station}" data-order-type="${cardItem.orderType}" data-priority="${cardItem.priority}" data-sla-tier="${cardItem.slaTier}">
    <jsp:include page="_kdsCardHeader.jsp" />
    <c:if test="${onShift}">
        <div class="kds-item-actions">
            <form action="${ctx}/barista/kds" method="post" class="kds-primary-form">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="start"><input type="hidden" name="orderItemId" value="${cardItem.orderItemId}">
                <button type="submit" class="btn btn-primary btn-full">Nhận pha</button>
            </form>
            <button type="button" class="btn btn-ghost btn-sm btn-full js-issue" data-item-id="${cardItem.orderItemId}" data-product-id="${cardItem.productId}" data-name="${cardItem.quantity} × <c:out value='${cardItem.productName}' />">Báo sự cố</button>
        </div>
    </c:if>
</article>
