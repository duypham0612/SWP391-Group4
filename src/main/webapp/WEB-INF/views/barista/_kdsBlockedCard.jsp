<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<article class="card kds-card kds-blocked" data-kds-item-id="${cardItem.orderItemId}" data-owner="blocked" data-station="${cardItem.station}" data-order-type="${cardItem.orderType}" data-priority="${cardItem.priority}" data-sla-tier="${cardItem.slaTier}">
    <jsp:include page="_kdsCardHeader.jsp" />
    <div class="kds-handover"><strong>Đang chờ xử lý</strong> · Thu ngân có thể huỷ món này nếu khách không đổi ý</div>
    <c:if test="${onShift}">
        <form action="${ctx}/barista/kds" method="post" class="kds-primary-form"
              data-confirm="Nguyên liệu/thiết bị đã sẵn sàng trở lại? Món sẽ về hàng chờ pha.">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="unblock"><input type="hidden" name="orderItemId" value="${cardItem.orderItemId}">
            <button type="submit" class="btn btn-primary btn-full">Có lại rồi — trả về chờ pha</button>
        </form>
    </c:if>
</article>
