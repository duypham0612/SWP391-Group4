<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Thứ tự ưu tiên thị giác: tên món + số lượng → ghi chú → thời gian → bàn/mã đơn.
     Barista đọc card ở ~1.2m, liếc 1 giây, tay đang bận. --%>

<div class="kds-card__top">
    <strong class="kds-product"><span class="kds-qty">${cardItem.quantity}×</span> <c:out value="${cardItem.productName}" /></strong>
    <%-- Cao điểm: thay nhãn trễ (lúc này ly nào cũng trễ) bằng số thứ tự pha để barista biết làm ly nào trước. --%>
    <c:choose>
        <c:when test="${peakMode and cardItem.seqNo > 0}"><span class="kds-sla kds-seq">Pha thứ ${cardItem.seqNo}</span></c:when>
        <c:otherwise><span class="kds-sla kds-sla--${cardItem.slaTier}">${cardItem.slaLabel}</span></c:otherwise>
    </c:choose>
</div>

<c:if test="${not empty cardItem.modifiers}">
    <div class="kds-mods"><c:forEach var="om" items="${cardItem.modifiers}"><span class="chip"><c:out value="${om.optionName}" /></span></c:forEach></div>
</c:if>
<c:if test="${not empty cardItem.note}">
    <div class="kds-note"><span class="kds-note__tag">Ghi chú</span><c:out value="${cardItem.note}" /></div>
</c:if>

<c:if test="${cardItem.priority}"><div class="kds-priority">LÀM LẠI – ƯU TIÊN · lần ${cardItem.remakeCount}</div></c:if>
<c:if test="${cardItem.hasIssue}"><div class="kds-issue"><strong>⚠ Sự cố:</strong> <c:out value="${cardItem.issueReason}" /></div></c:if>
<c:if test="${cardItem.recipeMissing}"><div class="kds-note kds-note--warn"><span class="kds-note__tag">⚠ Chưa có công thức</span> Không thể hoàn thành hoặc làm lại cho đến khi cấu hình công thức.</div></c:if>

<div class="kds-card__identity">
    <div class="kds-card__destination">
        <span>Giao tại</span>
        <strong><c:if test="${not empty cardItem.pickupCode}"><span class="kds-code"><c:out value="${cardItem.pickupCode}" /></span> · </c:if><c:choose><c:when test="${not empty cardItem.tableNumber}"><c:out value="${cardItem.tableNumber}" /></c:when><c:otherwise>${cardItem.orderTypeLabel}</c:otherwise></c:choose></strong>
    </div>
    <div class="kds-meta-row"><span>${cardItem.waitProgressLabel}</span><span>${cardItem.orderTypeLabel} · Đơn #${cardItem.orderId} · ${cardItem.createdDisplay}</span></div>
</div>
