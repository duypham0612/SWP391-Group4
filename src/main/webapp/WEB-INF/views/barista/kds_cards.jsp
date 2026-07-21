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

<%-- Dải trạng thái gọn, đếm theo số ly; không dùng ước tính hay hạn chờ. --%>
<section class="kds-summary" aria-label="Trạng thái quầy pha chế">
    <div class="kds-stat"><span class="kds-stat__label">Chờ pha</span><strong class="kds-stat__num">${waitingCount}</strong><span class="kds-stat__unit">ly</span></div>
    <div class="kds-stat"><span class="kds-stat__label">Đang pha</span><strong class="kds-stat__num kds-stat__num--making">${makingCount}</strong><span class="kds-stat__unit">ly</span></div>
    <div class="kds-stat"><span class="kds-stat__label">Sẵn sàng</span><strong class="kds-stat__num kds-stat__num--ready">${readyCount}</strong><span class="kds-stat__unit">ly</span></div>
    <div class="kds-stat"><span class="kds-stat__label">Bàn đang chờ</span><strong class="kds-stat__num">${tableCount}</strong><span class="kds-stat__unit">nhóm</span></div>
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
                    <p class="kds-stale__hint">Không thuộc ca hiện tại. Quản lý cần xác nhận huỷ hoặc xử lý riêng.</p>
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

<div class="kds-groups">
    <c:choose>
        <c:when test="${empty brewGroups}"><div class="kds-col__empty"><span>✓</span> Không còn nhóm nào chờ pha</div></c:when>
        <c:otherwise>
            <c:forEach var="grp" items="${brewGroups}" varStatus="st">
                <section class="kds-group" data-group-key="${grp.key}">
                    <header class="kds-group__head">
                        <span class="kds-group__seq">${st.index + 1}</span>
                        <strong class="kds-group__where">
                            <c:choose>
                                <c:when test="${grp.dineIn and not empty grp.tableNumber}"><c:out value="${grp.tableNumber}" /></c:when>
                                <c:otherwise><c:if test="${not empty grp.pickupCode}"><span class="kds-code"><c:out value="${grp.pickupCode}" /></span> · </c:if>${grp.orderTypeLabel}</c:otherwise>
                            </c:choose>
                        </strong>
                        <span class="kds-group__count">${grp.cups} ly <small>(${grp.waitingCups} chờ · ${grp.makingCups} pha · ${grp.readyCups} xong)</small></span>
                    </header>
                    <div class="kds-group__body">
                        <c:forEach var="item" items="${grp.items}">
                            <c:set var="cardItem" value="${item}" scope="request" />
                            <c:choose>
                                <c:when test="${item.status eq 'WAITING'}"><jsp:include page="_kdsWaitingCard.jsp" /></c:when>
                                <c:when test="${item.status eq 'MAKING'}"><jsp:include page="_kdsProgressCard.jsp" /></c:when>
                                <c:otherwise><jsp:include page="_kdsReadyCard.jsp" /></c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </div>
                </section>
            </c:forEach>
        </c:otherwise>
    </c:choose>
    <div class="kds-filter-empty" hidden>Không có nhóm phù hợp bộ lọc.</div>
</div>
