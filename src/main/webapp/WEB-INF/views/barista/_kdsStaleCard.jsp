<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Đơn treo từ ngày kinh doanh trước — chỉ đọc. Barista đang đứng pha không xử lý được
     (đơn cũ thường phải huỷ hoặc hoàn tiền), nên card này KHÔNG có nút thao tác. --%>
<article class="card kds-card kds-ok" data-kds-item-id="${cardItem.orderItemId}" data-owner="stale" data-station="${cardItem.station}" data-order-type="${cardItem.orderType}" data-priority="false" data-sla-tier="ok">
    <div class="kds-card__top">
        <strong class="kds-product"><span class="kds-qty">${cardItem.quantity}×</span> <c:out value="${cardItem.productName}" /></strong>
        <span class="kds-sla kds-sla--ok"><jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="${cardItem.status}" /></jsp:include></span>
    </div>
    <c:if test="${not empty cardItem.note}">
        <div class="kds-meta-row"><span>Ghi chú: <c:out value="${cardItem.note}" /></span></div>
    </c:if>
    <div class="kds-meta-row">
        <span>Tạo ${cardItem.createdDisplay}</span>
        <span><c:choose><c:when test="${not empty cardItem.tableNumber}"><c:out value="${cardItem.tableNumber}" /></c:when><c:otherwise>${cardItem.orderTypeLabel}</c:otherwise></c:choose> · #${cardItem.orderId}</span>
    </div>
</article>
