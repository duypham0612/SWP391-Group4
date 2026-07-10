<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<section class="kds-summary kds-summary--${oldestTier}">
    <div class="kds-stat">
        <span class="kds-stat__label">Chờ pha</span>
        <strong class="kds-stat__num">${waitingCount}</strong>
    </div>
    <div class="kds-stat">
        <span class="kds-stat__label">Đang pha</span>
        <strong class="kds-stat__num kds-stat__num--making">${makingCount}</strong>
    </div>
    <div class="kds-stat">
        <span class="kds-stat__label">Quá giờ</span>
        <strong class="kds-stat__num ${overdueCount gt 0 ? 'kds-stat__num--over' : ''}">${overdueCount}</strong>
    </div>
    <div class="kds-stat">
        <span class="kds-stat__label">Chờ lâu nhất</span>
        <strong class="kds-stat__num">${oldestDisplay}</strong>
        <span class="kds-stat__hint">nhắc từ ${kdsWarnMin}′ · trễ ${kdsCritMin}′</span>
    </div>
</section>

<c:choose>
    <c:when test="${empty waitingTickets and empty makingTickets}">
        <div class="card empty-state kds-empty">
            <div class="icon">✓</div>
            <p>Không có món nào đang chờ. Bếp trống.</p>
        </div>
    </c:when>
    <c:otherwise>
        <div class="kds-columns">

            <%-- ============ Cột CHỜ PHA (FIFO: đơn vào trước ở trên, #1 = tiếp theo) ============ --%>
            <section class="kds-col">
                <div class="kds-col__head"><h2>Chờ pha</h2><span class="kds-col__count">${waitingCount}</span></div>
                <div class="kds-col__body">
                    <c:choose>
                        <c:when test="${empty waitingTickets}">
                            <div class="kds-col__empty">Không có món chờ pha.</div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="ticket" items="${waitingTickets}" varStatus="st">
                                <article class="card kds-card kds-${ticket.ageTier} ${st.first ? 'kds-card--next' : ''}"
                                         data-kds-ticket-id="w${ticket.orderId}" tabindex="0">
                                    <div class="kds-card__top">
                                        <div class="kds-card__id">
                                            <div class="kds-card__idrow">
                                                <span class="kds-pos">${st.index + 1}</span>
                                                <div class="kds-table">
                                                    <c:choose>
                                                        <c:when test="${not empty ticket.tableNumber}">${ticket.tableNumber}</c:when>
                                                        <c:otherwise>Đem về</c:otherwise>
                                                    </c:choose>
                                                </div>
                                                <c:if test="${st.first}"><span class="kds-next">TIẾP THEO</span></c:if>
                                            </div>
                                            <div class="muted">Đơn #${ticket.orderId} · ${ticket.itemCount} món</div>
                                        </div>
                                        <c:choose>
                                            <c:when test="${ticket.ageTier == 'crit'}">
                                                <span class="kds-overdue">QUÁ GIỜ · ${ticket.waitedDisplay}</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="kds-clock">Chờ ${ticket.waitedDisplay}</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>

                                    <div class="kds-ticket-items">
                                        <c:forEach var="it" items="${ticket.items}">
                                            <section class="kds-ticket-item" data-order-item-id="${it.orderItemId}">
                                                <div class="kds-ticket-item__head">
                                                    <strong>${it.quantity}× ${it.productName}</strong>
                                                    <jsp:include page="../layout/_statusBadge.jsp">
                                                        <jsp:param name="status" value="${it.status}" />
                                                    </jsp:include>
                                                </div>
                                                <c:if test="${not empty it.modifiers}">
                                                    <div class="kds-mods">
                                                        <c:forEach var="om" items="${it.modifiers}"><span class="chip">${om.optionName}</span></c:forEach>
                                                    </div>
                                                </c:if>
                                                <c:if test="${not empty it.note}">
                                                    <div class="kds-note"><span class="kds-note__tag">Ghi chú</span> ${it.note}</div>
                                                </c:if>
                                                <div class="kds-item-actions">
                                                    <form action="${ctx}/barista/kds" method="post" class="kds-primary-form">
                                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                        <input type="hidden" name="action" value="markReady">
                                                        <input type="hidden" name="orderItemId" value="${it.orderItemId}">
                                                        <button type="submit" class="btn btn-primary btn-full kds-done">Xong</button>
                                                    </form>
                                                    <div class="kds-subactions">
                                                        <form action="${ctx}/barista/kds" method="post">
                                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                            <input type="hidden" name="action" value="start">
                                                            <input type="hidden" name="orderItemId" value="${it.orderItemId}">
                                                            <button type="submit" class="btn btn-ghost btn-sm">Bắt đầu</button>
                                                        </form>
                                                        <button type="button" class="btn btn-ghost btn-sm kds-cant js-cantmake"
                                                                data-item-id="${it.orderItemId}" data-product-id="${it.productId}"
                                                                data-name="${it.quantity}× ${it.productName}">Không pha được</button>
                                                    </div>
                                                </div>
                                            </section>
                                        </c:forEach>
                                    </div>

                                    <div class="kds-card__foot">
                                        <form action="${ctx}/barista/kds" method="post" title="Đẩy đơn này lên đầu hàng chờ">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="action" value="bump">
                                            <input type="hidden" name="orderItemId" value="${ticket.items[0].orderItemId}">
                                            <button type="submit" class="btn btn-ghost btn-sm">↑ Ưu tiên</button>
                                        </form>
                                        <c:if test="${ticket.itemCount > 1}">
                                            <form action="${ctx}/barista/kds" method="post"
                                                  data-confirm="Pha xong TẤT CẢ ${ticket.itemCount} món của đơn này? Nguyên liệu sẽ được trừ khỏi kho.">
                                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                <input type="hidden" name="action" value="markReadyAll">
                                                <input type="hidden" name="orderId" value="${ticket.orderId}">
                                                <button type="submit" class="btn btn-primary btn-sm">Xong cả đơn (${ticket.itemCount})</button>
                                            </form>
                                        </c:if>
                                    </div>
                                </article>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </section>

            <%-- ============ Cột ĐANG PHA (món đã bắt đầu, giữ vị trí FIFO) ============ --%>
            <section class="kds-col kds-col--making">
                <div class="kds-col__head"><h2>Đang pha</h2><span class="kds-col__count">${makingCount}</span></div>
                <div class="kds-col__body">
                    <c:choose>
                        <c:when test="${empty makingTickets}">
                            <div class="kds-col__empty">Chưa có món đang pha.</div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="ticket" items="${makingTickets}">
                                <article class="card kds-card kds-${ticket.ageTier}"
                                         data-kds-ticket-id="m${ticket.orderId}" tabindex="0">
                                    <div class="kds-card__top">
                                        <div class="kds-card__id">
                                            <div class="kds-table">
                                                <c:choose>
                                                    <c:when test="${not empty ticket.tableNumber}">${ticket.tableNumber}</c:when>
                                                    <c:otherwise>Đem về</c:otherwise>
                                                </c:choose>
                                            </div>
                                            <div class="muted">Đơn #${ticket.orderId} · ${ticket.itemCount} món</div>
                                        </div>
                                        <c:choose>
                                            <c:when test="${ticket.ageTier == 'crit'}">
                                                <span class="kds-overdue">QUÁ GIỜ · ${ticket.waitedDisplay}</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="kds-clock">Chờ ${ticket.waitedDisplay}</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>

                                    <div class="kds-ticket-items">
                                        <c:forEach var="it" items="${ticket.items}">
                                            <section class="kds-ticket-item" data-order-item-id="${it.orderItemId}">
                                                <div class="kds-ticket-item__head">
                                                    <strong>${it.quantity}× ${it.productName}</strong>
                                                    <jsp:include page="../layout/_statusBadge.jsp">
                                                        <jsp:param name="status" value="${it.status}" />
                                                    </jsp:include>
                                                </div>
                                                <c:if test="${it.makingSeconds ne null}">
                                                    <div class="kds-clock kds-clock--making">Đang pha ${it.makingDisplay}</div>
                                                </c:if>
                                                <c:if test="${not empty it.modifiers}">
                                                    <div class="kds-mods">
                                                        <c:forEach var="om" items="${it.modifiers}"><span class="chip">${om.optionName}</span></c:forEach>
                                                    </div>
                                                </c:if>
                                                <c:if test="${not empty it.note}">
                                                    <div class="kds-note"><span class="kds-note__tag">Ghi chú</span> ${it.note}</div>
                                                </c:if>
                                                <div class="kds-item-actions">
                                                    <form action="${ctx}/barista/kds" method="post" class="kds-primary-form">
                                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                        <input type="hidden" name="action" value="markReady">
                                                        <input type="hidden" name="orderItemId" value="${it.orderItemId}">
                                                        <button type="submit" class="btn btn-primary btn-full kds-done">Xong</button>
                                                    </form>
                                                    <div class="kds-subactions">
                                                        <button type="button" class="btn btn-ghost btn-sm kds-cant js-cantmake"
                                                                data-item-id="${it.orderItemId}" data-product-id="${it.productId}"
                                                                data-name="${it.quantity}× ${it.productName}">Không pha được</button>
                                                    </div>
                                                </div>
                                            </section>
                                        </c:forEach>
                                    </div>

                                    <c:if test="${ticket.itemCount > 1}">
                                        <div class="kds-card__foot kds-card__foot--end">
                                            <form action="${ctx}/barista/kds" method="post"
                                                  data-confirm="Pha xong TẤT CẢ ${ticket.itemCount} món đang pha của đơn này?">
                                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                <input type="hidden" name="action" value="markReadyAll">
                                                <input type="hidden" name="orderId" value="${ticket.orderId}">
                                                <button type="submit" class="btn btn-primary btn-sm">Xong cả đơn (${ticket.itemCount})</button>
                                            </form>
                                        </div>
                                    </c:if>
                                </article>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </section>

        </div>
    </c:otherwise>
</c:choose>
