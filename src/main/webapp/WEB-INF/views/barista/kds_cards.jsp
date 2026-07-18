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

<%-- Thống kê đếm theo SỐ LY (khối lượng việc pha thật); số dòng món và số đơn là thông tin phụ. --%>
<section class="kds-summary kds-summary--${peakMode ? 'ok' : oldestTier}" aria-label="Thống kê quầy pha chế">
    <div class="kds-stat">
        <span class="kds-stat__label">Chờ pha</span><strong class="kds-stat__num">${waitingCount} ly</strong>
        <span class="kds-stat__hint">${waitingLines} dòng món</span>
    </div>
    <div class="kds-stat">
        <span class="kds-stat__label">Đang pha</span><strong class="kds-stat__num kds-stat__num--making">${makingCount} ly</strong>
        <span class="kds-stat__hint">${makingLines} dòng món</span>
    </div>
    <div class="kds-stat">
        <span class="kds-stat__label">Đã pha xong</span><strong class="kds-stat__num kds-stat__num--ready">${readyCount} ly</strong>
        <span class="kds-stat__hint">${readyLines} dòng món</span>
    </div>
    <div class="kds-stat">
        <span class="kds-stat__label">Trễ giờ</span><strong class="kds-stat__num ${overdueCount gt 0 ? 'kds-stat__num--over' : ''}">${overdueCount} ly</strong>
        <span class="kds-stat__hint">${openOrderCount} đơn đang mở</span>
    </div>
    <div class="kds-stat kds-stat--wide">
        <span class="kds-stat__label">Chờ lâu nhất</span>
        <strong class="kds-stat__num">${oldestDisplay}</strong>
        <span class="kds-stat__hint">
            <c:choose>
                <c:when test="${oldestQty gt 0}"><c:out value="${oldestLocation}" /> · ${oldestQty}× <c:out value="${oldestProduct}" /></c:when>
                <c:otherwise>Không có món nào đang chờ</c:otherwise>
            </c:choose>
        </span>
    </div>
</section>

<%-- Đơn của ngày kinh doanh trước: việc của quản lý, không trộn vào hàng chờ đang pha. --%>
<c:if test="${not empty staleItems}">
    <details class="kds-stale">
        <summary>Đơn treo cần xử lý · ${staleCount} ly từ ngày trước</summary>
        <p class="kds-stale__hint">Những đơn này không thuộc ca hiện tại và không tính vào thống kê trễ giờ. Quản lý cần xác nhận huỷ hoặc xử lý riêng.</p>
        <div class="kds-stale__body">
            <c:forEach var="item" items="${staleItems}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsStaleCard.jsp" /></c:forEach>
        </div>
    </details>
</c:if>

<c:if test="${not empty blockedItems}">
    <section class="kds-blocked-zone" aria-labelledby="blockedTitle">
        <div class="kds-col__head"><h2 id="blockedTitle">Cần xử lý</h2><span class="kds-col__count">${blockedCount} ly</span></div>
        <p class="kds-modal__hint">Những món này không pha được nên đã rời hàng chờ và không tính vào số ly trễ.
            Đồng hồ vẫn chạy vì khách vẫn đang đợi — xử lý sớm hoặc báo Thu ngân huỷ món.</p>
        <div class="kds-blocked-zone__body">
            <c:forEach var="item" items="${blockedItems}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsBlockedCard.jsp" /></c:forEach>
        </div>
    </section>
</c:if>

<div class="kds-columns kds-columns--three">
    <section class="kds-col" aria-labelledby="waitingTitle">
        <div class="kds-col__head"><h2 id="waitingTitle">Chờ pha</h2><span class="kds-col__count">${waitingCount} ly</span></div>
        <div class="kds-col__body">
            <c:choose>
                <c:when test="${empty waitingItems}"><div class="kds-col__empty"><span>✓</span> Không còn món chờ pha</div></c:when>
                <c:otherwise><c:forEach var="item" items="${waitingItems}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsWaitingCard.jsp" /></c:forEach></c:otherwise>
            </c:choose>
        </div>
    </section>

    <section class="kds-col kds-col--making" aria-labelledby="progressTitle">
        <div class="kds-col__head"><h2 id="progressTitle">Đang pha</h2><span class="kds-col__count">${makingCount} ly</span></div>
        <div class="kds-col__body">
            <c:choose>
                <c:when test="${empty inProgressItems}"><div class="kds-col__empty"><span>✓</span> Chưa có món đang pha</div></c:when>
                <c:otherwise><c:forEach var="item" items="${inProgressItems}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsProgressCard.jsp" /></c:forEach></c:otherwise>
            </c:choose>
        </div>
    </section>

    <section class="kds-col kds-col--ready" aria-labelledby="readyTitle">
        <div class="kds-col__head"><h2 id="readyTitle">Đã pha xong</h2><span class="kds-col__count">${readyCount} ly</span></div>
        <div class="kds-col__body">
            <c:choose>
                <c:when test="${empty readyItems}"><div class="kds-col__empty"><span>✓</span> Chưa có món chờ nhận</div></c:when>
                <c:otherwise><c:forEach var="item" items="${readyItems}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsReadyCard.jsp" /></c:forEach></c:otherwise>
            </c:choose>
        </div>
    </section>
</div>
