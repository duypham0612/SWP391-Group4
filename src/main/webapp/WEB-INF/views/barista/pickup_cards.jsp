<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<c:choose>
    <c:when test="${empty tickets}">
        <div class="card empty-state">
            <div class="icon">✓</div>
            <p>Chưa có món nào chờ giao.</p>
        </div>
    </c:when>
    <c:otherwise>
        <div class="kds-grid">
            <c:forEach var="t" items="${tickets}">
                <article class="card kds-card pickup-card ${t.allReady ? 'pickup-card--ready' : 'pickup-card--partial'}"
                         data-kds-ticket-id="${t.orderId}" tabindex="0">
                    <div class="kds-card__top">
                        <div>
                            <div class="kds-table">
                                <c:choose>
                                    <c:when test="${not empty t.tableNumber}">${t.tableNumber}</c:when>
                                    <c:otherwise>Đem về</c:otherwise>
                                </c:choose>
                            </div>
                            <div class="muted">Đơn #${t.orderId} · ${t.readyCount} món chờ giao</div>
                        </div>
                        <c:choose>
                            <c:when test="${t.allReady}">
                                <span class="badge badge-ready">Đủ món</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge badge-making">Còn ${t.pendingCount} đang pha</span>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <%-- Checklist toàn bộ đơn để đối chiếu đủ/đúng trước khi giao --%>
                    <div class="kds-ticket-items">
                        <c:forEach var="it" items="${t.items}">
                            <section class="kds-ticket-item pickup-line ${it.status == 'READY' ? '' : 'pickup-line--pending'}"
                                     data-order-item-id="${it.orderItemId}">
                                <div class="kds-ticket-item__head">
                                    <strong>${it.quantity}× ${it.productName}</strong>
                                    <jsp:include page="../layout/_statusBadge.jsp">
                                        <jsp:param name="status" value="${it.status}" />
                                    </jsp:include>
                                </div>

                                <c:if test="${not empty it.modifiers}">
                                    <div class="kds-mods">
                                        <c:forEach var="om" items="${it.modifiers}">
                                            <span class="chip">${om.optionName}</span>
                                        </c:forEach>
                                    </div>
                                </c:if>

                                <c:if test="${not empty it.note}">
                                    <div class="kds-note">${it.note}</div>
                                </c:if>

                                <c:if test="${it.status == 'READY'}">
                                    <form action="${ctx}/barista/pickup" method="post" class="pickup-line-form">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="markServed">
                                        <input type="hidden" name="orderItemId" value="${it.orderItemId}">
                                        <button type="submit" class="btn btn-ghost btn-sm btn-full">Đã giao món này</button>
                                    </form>
                                </c:if>
                            </section>
                        </c:forEach>
                    </div>

                    <c:if test="${t.readyCount > 0}">
                        <div class="kds-actions">
                            <form action="${ctx}/barista/pickup" method="post" class="kds-action-form"
                                  onsubmit="return confirm('Đã giao tất cả món sẵn của bàn này cho khách?');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="serveAllReady">
                                <input type="hidden" name="orderId" value="${t.orderId}">
                                <button type="submit" class="btn btn-primary btn-sm btn-full">Giao các món sẵn (${t.readyCount})</button>
                            </form>
                        </div>
                    </c:if>
                </article>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>
