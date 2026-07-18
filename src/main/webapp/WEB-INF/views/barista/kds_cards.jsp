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

<section class="kds-summary kds-summary--${oldestTier}" aria-label="Thống kê quầy pha chế">
    <div class="kds-stat"><span class="kds-stat__label">Chờ pha</span><strong class="kds-stat__num">${waitingCount} món</strong></div>
    <div class="kds-stat"><span class="kds-stat__label">Đang pha</span><strong class="kds-stat__num kds-stat__num--making">${makingCount} món</strong></div>
    <div class="kds-stat"><span class="kds-stat__label">Đã pha xong</span><strong class="kds-stat__num kds-stat__num--ready">${readyCount} món</strong></div>
    <div class="kds-stat"><span class="kds-stat__label">Trễ giờ</span><strong class="kds-stat__num ${overdueCount gt 0 ? 'kds-stat__num--over' : ''}">${overdueCount} món</strong></div>
    <div class="kds-stat kds-stat--wide"><span class="kds-stat__label">Chờ lâu nhất</span><strong class="kds-stat__num">${oldestDisplay}<c:if test="${not empty oldestLocation}"> · ${oldestLocation}</c:if></strong></div>
</section>

<c:if test="${not empty blockedItems}">
    <section class="kds-blocked-zone" aria-labelledby="blockedTitle">
        <div class="kds-col__head"><h2 id="blockedTitle">Cần xử lý</h2><span class="kds-col__count">${blockedCount} món</span></div>
        <p class="kds-modal__hint">Những món này không pha được nên đã rời hàng chờ và không tính vào số món trễ.
            Đồng hồ vẫn chạy vì khách vẫn đang đợi — xử lý sớm hoặc báo Thu ngân huỷ món.</p>
        <div class="kds-blocked-zone__body">
            <c:forEach var="item" items="${blockedItems}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsBlockedCard.jsp" /></c:forEach>
        </div>
    </section>
</c:if>

<div class="kds-columns kds-columns--three">
    <section class="kds-col" aria-labelledby="waitingTitle">
        <div class="kds-col__head"><h2 id="waitingTitle">Chờ pha</h2><span class="kds-col__count">${waitingCount} món</span></div>
        <div class="kds-col__body">
            <c:choose>
                <c:when test="${empty waitingItems}"><div class="kds-col__empty"><span>✓</span> Không có món chờ pha.</div></c:when>
                <c:otherwise><c:forEach var="item" items="${waitingItems}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsWaitingCard.jsp" /></c:forEach></c:otherwise>
            </c:choose>
        </div>
    </section>

    <section class="kds-col kds-col--making" aria-labelledby="progressTitle">
        <div class="kds-col__head"><h2 id="progressTitle">Đang pha</h2><span class="kds-col__count">${makingCount} món</span></div>
        <div class="kds-col__body">
            <c:choose>
                <c:when test="${empty inProgressItems}"><div class="kds-col__empty"><span>✓</span> Chưa có món đang pha.</div></c:when>
                <c:otherwise><c:forEach var="item" items="${inProgressItems}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsProgressCard.jsp" /></c:forEach></c:otherwise>
            </c:choose>
        </div>
    </section>

    <section class="kds-col kds-col--ready" aria-labelledby="readyTitle">
        <div class="kds-col__head"><h2 id="readyTitle">Đã pha xong</h2><span class="kds-col__count">${readyCount} món</span></div>
        <div class="kds-col__body">
            <c:choose>
                <c:when test="${empty readyItems}"><div class="kds-col__empty"><span>✓</span> Chưa có món chờ nhận.</div></c:when>
                <c:otherwise><c:forEach var="item" items="${readyItems}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsReadyCard.jsp" /></c:forEach></c:otherwise>
            </c:choose>
        </div>
    </section>
</div>
