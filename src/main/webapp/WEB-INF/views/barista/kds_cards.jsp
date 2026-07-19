<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" scope="request" />

<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success" role="status"><c:out value="${sessionScope.flashOk}" /></div>
    <c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error" role="alert"><c:out value="${sessionScope.flashError}" /></div>
    <c:remove var="flashError" scope="session" />
</c:if>

<%-- Cao điểm: gom cảnh báo về MỘT dòng thay vì tô đỏ từng card. Lúc đông, ly nào cũng "trễ"
     theo đồng hồ chờ song song — tô đỏ hết thì mất tác dụng phân loại; barista cần biết pha ly
     nào trước (số thứ tự trên card), quản lý cần biết khách cuối còn đợi bao lâu. --%>
<c:if test="${peakMode}">
    <div class="kds-peak" role="status">
        <strong>Cao điểm</strong>
        <span>${peakQueueCups} ly đang dồn · khách cuối đợi ~${peakEstLastMin} phút · pha theo số thứ tự trên card</span>
    </div>
</c:if>

<%-- Một dải trạng thái gọn để giữ món đầu tiên trong tầm mắt. Số đếm theo số ly. --%>
<section class="kds-summary kds-summary--${peakMode ? 'ok' : oldestTier}" aria-label="Trạng thái quầy pha chế">
    <div class="kds-stat"><span class="kds-stat__label">Chờ pha</span><strong class="kds-stat__num">${waitingCount}</strong><span class="kds-stat__unit">ly</span></div>
    <div class="kds-stat"><span class="kds-stat__label">Đang pha</span><strong class="kds-stat__num kds-stat__num--making">${makingCount}</strong><span class="kds-stat__unit">ly</span></div>
    <div class="kds-stat"><span class="kds-stat__label">Sẵn sàng</span><strong class="kds-stat__num kds-stat__num--ready">${readyCount}</strong><span class="kds-stat__unit">ly</span></div>
    <div class="kds-stat"><span class="kds-stat__label">Trễ giờ</span><strong class="kds-stat__num ${overdueCount gt 0 ? 'kds-stat__num--over' : ''}">${overdueCount}</strong><span class="kds-stat__unit">ly</span></div>
    <div class="kds-stat kds-stat--wide">
        <span class="kds-stat__label">Chờ lâu nhất</span><strong class="kds-stat__num">${oldestDisplay}</strong>
        <%-- Có đơn treo thì nói luôn ở đây: "thông thoáng" đứng cạnh banner đỏ 30 ly đọc như hệ thống tự mâu thuẫn. --%>
        <span class="kds-stat__context"><c:choose><c:when test="${oldestQty gt 0}"><c:out value="${oldestLocation}" /> · ${oldestQty}× <c:out value="${oldestProduct}" /></c:when><c:otherwise>Quầy đang thông thoáng</c:otherwise></c:choose><c:if test="${staleHasItems}"> · ${staleCount} ly tồn từ ngày trước</c:if></span>
    </div>
</section>

<%-- Ngoại lệ được gom vào một drawer, không đẩy ba lane vận hành xuống dưới. --%>
<c:if test="${staleHasItems or not empty blockedItems}">
    <details class="kds-alert-drawer" id="kdsAlertDrawer">
        <summary>
            <span class="kds-alert-drawer__icon" aria-hidden="true">!</span>
            <strong>Cảnh báo cần xử lý</strong>
            <c:if test="${not empty blockedItems}"><span class="kds-alert-count">${blockedCount} ly bị chặn</span></c:if>
            <c:if test="${staleHasItems}"><span class="kds-alert-count">${staleOrderCount} đơn treo · ${staleCount} ly</span></c:if>
            <span class="kds-alert-drawer__action">Xem chi tiết</span>
        </summary>
        <div class="kds-alert-drawer__body">
            <c:if test="${not empty blockedItems}">
                <section class="kds-blocked-zone" aria-labelledby="blockedTitle">
                    <div class="kds-alert-section__head"><h2 id="blockedTitle">Món bị chặn</h2><span>${blockedCount} ly</span></div>
                    <p class="kds-modal__hint">Khách vẫn đang đợi. Xử lý nguyên liệu hoặc thiết bị rồi trả món về hàng chờ; Thu ngân có thể huỷ nếu cần.</p>
                    <div class="kds-blocked-zone__body"><c:forEach var="item" items="${blockedItems}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsBlockedCard.jsp" /></c:forEach></div>
                </section>
            </c:if>
            <%-- Đơn treo gộp một dòng mỗi đơn: barista không thao tác được, chỉ cần tra cứu nhanh
                 khi khách hỏi. Card đầy đủ cho dữ liệu chỉ-đọc là lãng phí chỗ và che mất board. --%>
            <c:if test="${staleHasItems}">
                <section class="kds-stale" aria-labelledby="staleTitle">
                    <div class="kds-alert-section__head"><h2 id="staleTitle">Đơn treo từ ngày trước</h2><span>${staleOrderCount} đơn · ${staleCount} ly</span></div>
                    <p class="kds-stale__hint">Không thuộc ca hiện tại và không tính vào thống kê trễ giờ. Quản lý cần xác nhận huỷ hoặc xử lý riêng.</p>
                    <ul class="kds-stale-list">
                        <c:forEach var="g" items="${staleGroups}">
                            <li class="kds-stale-row">
                                <span class="kds-stale-row__order">#${g.orderId}</span>
                                <span class="kds-stale-row__where"><c:out value="${g.location}" /></span>
                                <span class="kds-stale-row__items" title="<c:out value='${g.productSummary}' />"><c:out value="${g.productSummary}" /></span>
                                <span class="kds-stale-row__cups">${g.cups} ly</span>
                                <span class="kds-stale-row__time">${g.createdDisplay}</span>
                            </li>
                        </c:forEach>
                    </ul>
                    <c:if test="${staleHiddenOrders gt 0}">
                        <p class="kds-stale__hint kds-stale__more">Còn ${staleHiddenOrders} đơn treo khác — xem đầy đủ ở màn Quản lý.</p>
                    </c:if>
                </section>
            </c:if>
        </div>
    </details>
</c:if>

<div class="kds-lane-tabs" role="tablist" aria-label="Trạng thái món">
    <button type="button" role="tab" class="kds-lane-tab is-active" id="waitingTab" aria-controls="waitingLane" aria-selected="true" data-lane-tab="waiting">Chờ pha <span>${waitingCount}</span></button>
    <button type="button" role="tab" class="kds-lane-tab" id="makingTab" aria-controls="makingLane" aria-selected="false" data-lane-tab="making">Đang pha <span>${makingCount}</span></button>
    <button type="button" role="tab" class="kds-lane-tab" id="readyTab" aria-controls="readyLane" aria-selected="false" data-lane-tab="ready">Sẵn sàng <span>${readyCount}</span></button>
</div>

<div class="kds-columns kds-columns--three">
    <section class="kds-col is-active" id="waitingLane" role="tabpanel" aria-labelledby="waitingTab" data-lane="waiting">
        <div class="kds-col__head"><h2 id="waitingTitle">Chờ pha</h2><span class="kds-col__count">${waitingCount} ly</span></div>
        <div class="kds-col__body">
            <c:choose>
                <c:when test="${empty waitingItems}"><div class="kds-col__empty"><span>✓</span> Không còn món chờ pha</div></c:when>
                <c:otherwise><c:forEach var="item" items="${waitingItems}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsWaitingCard.jsp" /></c:forEach></c:otherwise>
            </c:choose>
            <div class="kds-filter-empty" hidden>Không có món phù hợp bộ lọc.</div>
        </div>
    </section>

    <section class="kds-col kds-col--making" id="makingLane" role="tabpanel" aria-labelledby="makingTab" data-lane="making">
        <div class="kds-col__head"><h2 id="progressTitle">Đang pha</h2><span class="kds-col__count">${makingCount} ly</span></div>
        <div class="kds-col__body">
            <c:choose>
                <c:when test="${empty inProgressItems}"><div class="kds-col__empty"><span>✓</span> Chưa có món đang pha</div></c:when>
                <c:otherwise><c:forEach var="item" items="${inProgressItems}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsProgressCard.jsp" /></c:forEach></c:otherwise>
            </c:choose>
            <div class="kds-filter-empty" hidden>Không có món phù hợp bộ lọc.</div>
        </div>
    </section>

    <section class="kds-col kds-col--ready" id="readyLane" role="tabpanel" aria-labelledby="readyTab" data-lane="ready">
        <div class="kds-col__head"><h2 id="readyTitle">Đã pha xong</h2><span class="kds-col__count">${readyCount} ly</span></div>
        <div class="kds-col__body">
            <c:choose>
                <c:when test="${empty readyItems}"><div class="kds-col__empty"><span>✓</span> Chưa có món chờ nhận</div></c:when>
                <c:otherwise><c:forEach var="item" items="${readyItems}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsReadyCard.jsp" /></c:forEach></c:otherwise>
            </c:choose>
            <div class="kds-filter-empty" hidden>Không có món phù hợp bộ lọc.</div>
        </div>
    </section>
</div>
