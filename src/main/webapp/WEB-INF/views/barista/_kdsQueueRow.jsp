<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Một ly = một dòng của hàng chờ pha chế, dạng danh sách MỘT CỘT xếp theo thứ tự pha.
     Cột "Bàn" nằm ngay trên dòng nên không cần danh sách bàn riêng ở bên trái nữa.
     Giữ nguyên bộ data-* của card cũ để bộ lọc, làm mới và modal thao tác chạy y như trước. --%>
<%-- Viền màu theo TRẠNG THÁI, không theo đồng hồ: món chặn cần xử lý, món đã xong làm mờ.
     Mức khẩn cấp đã nằm ở chính vị trí dòng (đặt trước thì ở trên). --%>
<article class="kds-qrow kds-qrow--${cardItem.status == 'READY' ? 'ready' : (cardItem.status == 'BLOCKED' ? 'blocked' : 'open')}" tabindex="0"
         data-kds-item-id="${cardItem.orderItemId}" data-cups="${cardItem.quantity}"
         data-owner="${cardItem.status == 'MAKING' ? cardItem.baristaId : (cardItem.status == 'READY' ? cardItem.preparedBy : (cardItem.status == 'BLOCKED' ? 'blocked' : 'unassigned'))}"
         data-station="${cardItem.station}" data-order-type="${cardItem.orderType}"
         data-priority="${cardItem.priority}">

    <%-- Số thứ tự pha. Món đã xong không đánh số — nó không còn là việc của quầy.
         seqNo = 0 nghĩa là dòng nằm ngoài hàng chờ hôm nay (khu "Đơn treo"): đánh dấu ⚠ thay vì
         in số 0, vốn đọc như "pha thứ 0". --%>
    <span class="kds-qrow__seq" aria-hidden="true">
        <c:choose>
            <c:when test="${cardItem.status == 'READY'}">✓</c:when>
            <c:when test="${cardItem.seqNo == 0}">⚠</c:when>
            <c:otherwise>${cardItem.seqNo}</c:otherwise>
        </c:choose>
    </span>

    <span class="kds-qrow__qty">${cardItem.quantity}×</span>

    <span class="kds-qrow__name">
        <c:out value="${cardItem.productName}" />
        <c:if test="${cardItem.priority}"><span class="kds-qrow__tag kds-qrow__tag--pri">⟳ Làm lại · lần ${cardItem.remakeCount}</span></c:if>
    </span>

    <%-- Cột BÀN: đơn tại bàn hiện số bàn, đơn mang đi/giao hiện loại đơn. --%>
    <span class="kds-qrow__table">
        <c:choose>
            <c:when test="${not empty cardItem.tableNumber}"><c:out value="${cardItem.tableNumber}" /></c:when>
            <c:otherwise><span class="kds-qrow__away">${cardItem.orderTypeLabel}</span></c:otherwise>
        </c:choose>
    </span>

    <span class="kds-qrow__code">
        <c:if test="${not empty cardItem.pickupCode}"><span class="kds-code"><c:out value="${cardItem.pickupCode}" /></span></c:if>
        <span class="kds-qrow__ord">#${cardItem.orderId}</span>
    </span>

    <span class="kds-qrow__state kds-qrow__state--${cardItem.status}">
        <c:choose>
            <c:when test="${cardItem.status == 'WAITING'}">Chờ pha</c:when>
            <c:when test="${cardItem.status == 'MAKING'}">Đang pha</c:when>
            <c:when test="${cardItem.status == 'READY'}">Đã pha xong</c:when>
            <c:otherwise>Tạm dừng</c:otherwise>
        </c:choose>
    </span>

    <c:if test="${not empty cardItem.note or not empty cardItem.modifiers or cardItem.hasIssue or cardItem.recipeMissing}">
        <span class="kds-qrow__tags">
            <c:forEach var="om" items="${cardItem.modifiers}"><span class="kds-qrow__tag"><c:out value="${om.optionName}" /></span></c:forEach>
            <c:if test="${not empty cardItem.note}"><span class="kds-qrow__tag kds-qrow__tag--note">Ghi chú: <c:out value="${cardItem.note}" /></span></c:if>
            <c:if test="${cardItem.hasIssue and cardItem.status == 'BLOCKED'}"><span class="kds-qrow__tag kds-qrow__tag--issue">⚠ <c:out value="${cardItem.issueReason}" /></span></c:if>
            <c:if test="${cardItem.recipeMissing}"><span class="kds-qrow__tag kds-qrow__tag--issue">⚠ Chưa có công thức</span></c:if>
        </span>
    </c:if>

    <span class="kds-qrow__act">
        <c:choose>
            <%-- CHỜ PHA --%>
            <c:when test="${cardItem.status == 'WAITING'}">
                <c:if test="${onShift}">
                    <form action="${ctx}/barista/kds" method="post">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="start"><input type="hidden" name="orderItemId" value="${cardItem.orderItemId}">
                        <button type="submit" class="btn btn-primary btn-sm">Nhận pha</button>
                    </form>
                    <details class="kds-card-menu"><summary>⋯</summary><div class="kds-card-menu__panel">
                        <button type="button" class="btn btn-ghost btn-sm btn-full js-issue" data-item-id="${cardItem.orderItemId}" data-product-id="${cardItem.productId}" data-name="${cardItem.quantity} × <c:out value='${cardItem.productName}' />">Báo sự cố</button>
                    </div></details>
                </c:if>
            </c:when>
            <%-- ĐANG PHA --%>
            <c:when test="${cardItem.status == 'MAKING'}">
                <c:if test="${onShift and cardItem.baristaId == currentUserId}">
                    <form action="${ctx}/barista/kds" method="post" class="kds-qrow__ready">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="markReady"><input type="hidden" name="orderItemId" value="${cardItem.orderItemId}">
                        <select name="handoverLocation" class="kds-qrow__sel"><option value="">— nơi đặt —</option><c:forEach var="loc" items="${handoverLocations}"><option value="${loc}">${loc}</option></c:forEach></select>
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
                </c:if>
                <c:if test="${onShift and cardItem.baristaId != currentUserId}"><span class="kds-qrow__by">Đang pha: <c:out value="${cardItem.baristaName}" /></span></c:if>
            </c:when>
            <%-- ĐÃ PHA XONG --%>
            <c:when test="${cardItem.status == 'READY'}">
                <span class="kds-qrow__by">✓ <c:choose><c:when test="${not empty cardItem.preparedByName}"><c:out value="${cardItem.preparedByName}" /></c:when><c:otherwise>đã pha</c:otherwise></c:choose><c:if test="${not empty cardItem.handoverLocation}"> · <c:out value="${cardItem.handoverLocation}" /></c:if></span>
                <c:if test="${onShift}">
                    <details class="kds-card-menu"><summary>⋯</summary><div class="kds-card-menu__panel">
                        <button type="button" class="btn btn-ghost btn-sm btn-full js-remake" data-item-id="${cardItem.orderItemId}" data-name="${cardItem.quantity} × <c:out value='${cardItem.productName}' />" ${cardItem.recipeMissing ? 'disabled' : ''}>Làm lại món</button>
                    </div></details>
                </c:if>
            </c:when>
            <%-- BỊ CHẶN --%>
            <c:when test="${cardItem.status == 'BLOCKED'}">
                <c:if test="${onShift}">
                    <form action="${ctx}/barista/kds" method="post" data-confirm="Nguyên liệu/thiết bị đã sẵn sàng lại? Món sẽ về hàng chờ pha.">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="unblock"><input type="hidden" name="orderItemId" value="${cardItem.orderItemId}">
                        <button type="submit" class="btn btn-primary btn-sm js-unblock" data-item-id="${cardItem.orderItemId}" data-product-id="${cardItem.productId}" data-name="${cardItem.quantity} × <c:out value='${cardItem.productName}' />">Trả về chờ pha</button>
                    </form>
                </c:if>
            </c:when>
        </c:choose>
    </span>
</article>
