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
        <span class="kds-stat__context"><c:choose><c:when test="${oldestQty gt 0}"><c:out value="${oldestLocation}" /> · ${oldestQty}× <c:out value="${oldestProduct}" /></c:when><c:otherwise>Quầy đang thông thoáng</c:otherwise></c:choose></span>
    </div>
</section>

<%-- Master–detail theo bàn: danh sách bàn bên trái, chi tiết một bàn bên phải. Render TẤT CẢ
     panel, CSS chỉ hiện panel active — cùng cơ chế "render all + toggle" của board lane cũ, để
     polling swap nguyên khối và JS chỉ cần đổi cờ active. --%>
<c:choose>
    <c:when test="${empty tableGroups}">
        <div class="kds-tables-empty"><span>✓</span> Quầy đang thông thoáng — chưa có bàn nào cần pha.</div>
    </c:when>
    <c:otherwise>
        <div class="kds-tables" id="kdsTables">
            <div class="kds-table-side">
            <div class="kds-side-head"><span>Bàn cần pha</span><strong>${tableGroups.size()}</strong></div>
            <div class="kds-table-search">
                <input type="search" id="kdsTableSearch" class="kds-table-search__input" placeholder="Tìm bàn…" autocomplete="off" aria-label="Tìm nhanh bàn">
            </div>
            <nav class="kds-table-list" role="tablist" aria-label="Danh sách bàn" aria-orientation="vertical">
                <c:forEach var="tg" items="${tableGroups}" varStatus="st">
                    <button type="button" role="tab" class="kds-table-tab kds-table-tab--${tg.badgeTier} ${st.first ? 'is-active' : ''} ${tg.done ? 'is-done' : ''}"
                            data-table-tab data-table-key="<c:out value='${tg.key}' />"
                            aria-selected="${st.first ? 'true' : 'false'}" tabindex="${st.first ? '0' : '-1'}">
                        <span class="kds-table-tab__name"><c:out value="${tg.label}" /><c:if test="${tg.orderCount gt 1}"> <span class="kds-table-tab__ordn">${tg.orderCount} đơn</span></c:if></span>
                        <span class="kds-table-tab__meta">
                            <c:choose>
                                <c:when test="${tg.openCups gt 0}"><span class="kds-table-tab__open">${tg.openCups} ly</span></c:when>
                                <c:otherwise><span class="kds-table-tab__isdone">✓ đã xong</span></c:otherwise>
                            </c:choose>
                            <c:if test="${tg.hasBlocked}"><span class="kds-table-tab__flag">Cần xử lý</span></c:if>
                        </span>
                        <span class="kds-table-tab__dots" aria-hidden="true">
                            <c:if test="${tg.waitingCups gt 0}"><span class="kds-dot kds-dot--waiting">${tg.waitingCups}</span></c:if>
                            <c:if test="${tg.makingCups gt 0}"><span class="kds-dot kds-dot--making">${tg.makingCups}</span></c:if>
                            <c:if test="${tg.readyCups gt 0}"><span class="kds-dot kds-dot--ready">${tg.readyCups}</span></c:if>
                        </span>
                    </button>
                </c:forEach>
            </nav>
            </div>

            <div class="kds-table-detail">
                <c:forEach var="tg" items="${tableGroups}" varStatus="st">
                    <section class="kds-table-panel ${st.first ? 'is-active' : ''}" role="tabpanel"
                             data-table-panel data-table-key="<c:out value='${tg.key}' />"
                             aria-label="Chi tiết ${tg.label}">
                        <div class="kds-table-panel__head">
                            <h2 class="kds-table-panel__name"><c:out value="${tg.label}" /></h2>
                            <span class="kds-table-panel__sub">
                                <c:if test="${tg.orderCount gt 1}">${tg.orderCount} đơn · </c:if>
                                <c:if test="${tg.orderCount eq 1 and not empty tg.pickupCode}"><span class="kds-code"><c:out value="${tg.pickupCode}" /></span> · </c:if>
                                <c:choose><c:when test="${tg.openCups gt 0}">${tg.openCups} ly đang mở</c:when><c:otherwise>Đã pha xong, chờ giao</c:otherwise></c:choose>
                            </span>
                        </div>

                        <c:if test="${not empty tg.blocked}">
                            <div class="kds-table-section kds-table-section--blocked">
                                <div class="kds-table-section__head"><h3>Tạm dừng — cần xử lý</h3><span class="kds-sec-count">${tg.blockedCups} ly</span></div>
                                <p class="kds-modal__hint">Khách vẫn đang đợi. Xử lý nguyên liệu hoặc thiết bị rồi trả món về hàng chờ; Thu ngân có thể huỷ nếu cần.</p>
                                <div class="kds-table-section__body">
                                    <c:forEach var="b" items="${tg.blockedBatches}"><div class="kds-batch"><div class="kds-batch__head"><span class="kds-batch__qty">${b.totalQty}×</span><span class="kds-batch__name"><c:out value="${b.productName}" /></span><c:if test="${b.orderCount gt 1}"><span class="kds-batch__n">${b.orderCount} đơn</span></c:if></div><c:forEach var="item" items="${b.items}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsBatchRow.jsp" /></c:forEach></div></c:forEach>
                                </div>
                            </div>
                        </c:if>
                        <c:if test="${not empty tg.waiting}">
                            <div class="kds-table-section kds-table-section--waiting">
                                <div class="kds-table-section__head"><h3>Chưa pha</h3><span class="kds-sec-count">${tg.waitingCups} ly</span></div>
                                <div class="kds-table-section__body">
                                    <c:forEach var="b" items="${tg.waitingBatches}"><div class="kds-batch"><div class="kds-batch__head"><span class="kds-batch__qty">${b.totalQty}×</span><span class="kds-batch__name"><c:out value="${b.productName}" /></span><c:if test="${b.orderCount gt 1}"><span class="kds-batch__n">${b.orderCount} đơn</span></c:if></div><c:forEach var="item" items="${b.items}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsBatchRow.jsp" /></c:forEach></div></c:forEach>
                                </div>
                            </div>
                        </c:if>
                        <c:if test="${not empty tg.making}">
                            <div class="kds-table-section kds-table-section--making">
                                <div class="kds-table-section__head"><h3>Đang pha</h3><span class="kds-sec-count">${tg.makingCups} ly</span></div>
                                <div class="kds-table-section__body">
                                    <c:forEach var="b" items="${tg.makingBatches}"><div class="kds-batch"><div class="kds-batch__head"><span class="kds-batch__qty">${b.totalQty}×</span><span class="kds-batch__name"><c:out value="${b.productName}" /></span><c:if test="${b.orderCount gt 1}"><span class="kds-batch__n">${b.orderCount} đơn</span></c:if></div><c:forEach var="item" items="${b.items}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsBatchRow.jsp" /></c:forEach></div></c:forEach>
                                </div>
                            </div>
                        </c:if>
                        <c:if test="${not empty tg.ready}">
                            <div class="kds-table-section kds-table-section--ready">
                                <div class="kds-table-section__head"><h3>Đã pha xong — chờ giao</h3><span class="kds-sec-count">${tg.readyCups} ly</span></div>
                                <div class="kds-table-section__body">
                                    <c:forEach var="b" items="${tg.readyBatches}"><div class="kds-batch"><div class="kds-batch__head"><span class="kds-batch__qty">${b.totalQty}×</span><span class="kds-batch__name"><c:out value="${b.productName}" /></span><c:if test="${b.orderCount gt 1}"><span class="kds-batch__n">${b.orderCount} đơn</span></c:if></div><c:forEach var="item" items="${b.items}"><c:set var="cardItem" value="${item}" scope="request" /><jsp:include page="_kdsBatchRow.jsp" /></c:forEach></div></c:forEach>
                                </div>
                            </div>
                        </c:if>
                        <div class="kds-filter-empty" hidden>Không có món phù hợp bộ lọc.</div>
                    </section>
                </c:forEach>
            </div>
        </div>
    </c:otherwise>
</c:choose>
