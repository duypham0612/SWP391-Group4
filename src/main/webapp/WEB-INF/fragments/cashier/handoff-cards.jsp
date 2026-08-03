<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<c:if test="${not embeddedHandoff and not empty sessionScope.flashOk}">
  <div class="alert alert-success"><c:out value="${sessionScope.flashOk}" /></div>
  <c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not embeddedHandoff and not empty sessionScope.flashError}">
  <div class="alert alert-error"><c:out value="${sessionScope.flashError}" /></div>
  <c:remove var="flashError" scope="session" />
</c:if>

<c:if test="${empty tickets and empty pickedUpGroups}">
  <div class="card empty-state"><div class="icon">✓</div><p>Chưa có món cần bàn giao.</p></div>
</c:if>

<c:if test="${not empty tickets}">
  <h3 class="pickup-recent__head">Đã pha xong · chờ nhân viên nhận</h3>
  <div class="kds-grid">
    <c:forEach var="t" items="${tickets}">
      <article class="card kds-card pickup-card">
        <div class="kds-card__top">
          <div>
            <div>
              <c:if test="${not empty t.pickupCode}">
                <span class="kds-code kds-code--lg"><c:out value="${t.pickupCode}" /></span>
              </c:if>
              <strong class="kds-table">
                <c:choose>
                  <c:when test="${not empty t.tableNumber}"><c:out value="${t.tableNumber}" /></c:when>
                  <c:otherwise>Nhận tại quầy</c:otherwise>
                </c:choose>
              </strong>
            </div>
            <div class="muted">Đơn #${t.orderId} · ${t.readyCupCount} món sẵn sàng</div>
          </div>
          <div class="pickup-card__badges">
            <span class="badge badge-ready">Chờ nhận</span>
            <c:if test="${t.pendingCupCount gt 0}">
              <span class="badge badge-making">${t.pendingCupCount} món chưa xong</span>
            </c:if>
          </div>
        </div>

        <div class="kds-ticket-items">
          <c:forEach var="it" items="${t.readyItems}">
            <section class="kds-ticket-item">
              <div class="kds-ticket-item__head">
                <strong>${it.quantity} × <c:out value="${it.productName}" /></strong>
                <span class="pickup-sla pickup-sla--${view.serveTier(it.serveWaitSeconds)}">Chờ nhận ${view.durationMinutes(it.serveWaitSeconds)}</span>
              </div>
              <c:if test="${not empty it.note}"><div class="kds-note"><c:out value="${it.note}" /></div></c:if>
              <form action="${ctx}/cashier/inbox" method="post">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="pickUp">
                <input type="hidden" name="orderItemId" value="${it.orderItemId}">
                <button class="btn btn-ghost btn-sm btn-full">Đã nhận ${it.quantity} món</button>
              </form>
            </section>
          </c:forEach>
        </div>

        <c:if test="${t.readyCount gt 1}">
          <div class="kds-card__foot">
            <form action="${ctx}/cashier/inbox" method="post" style="width:100%"
                  data-confirm="Xác nhận đã nhận tất cả ${t.readyCupCount} món sẵn sàng của đơn này?">
              <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
              <input type="hidden" name="action" value="pickUpAllReady">
              <input type="hidden" name="orderId" value="${t.orderId}">
              <button class="btn btn-primary btn-full">Đã nhận tất cả ${t.readyCupCount} món</button>
            </form>
          </div>
        </c:if>
      </article>
    </c:forEach>
  </div>
</c:if>

<c:if test="${not empty pickedUpGroups}">
  <h3 class="pickup-recent__head" style="margin-top:24px">Đã nhận · đang giao khách</h3>
  <div class="kds-grid">
    <c:forEach var="g" items="${pickedUpGroups}">
      <article class="card kds-card pickup-card--carrying">
        <div class="kds-card__top">
          <div>
            <div>
              <c:if test="${not empty g.pickupCodes}">
                <span class="kds-code kds-code--lg"><c:out value="${view.pickupCodes(g.pickupCodes)}" /></span>
              </c:if>
              <strong class="kds-table">
                <c:choose>
                  <c:when test="${not empty g.tableNumber}"><c:out value="${g.tableNumber}" /></c:when>
                  <c:otherwise>Nhận tại quầy</c:otherwise>
                </c:choose>
              </strong>
            </div>
            <div class="muted">${g.orderCount} đơn · ${g.cupCount} món đang giao</div>
          </div>
          <span class="badge badge-making">Đang giao</span>
        </div>

        <div class="kds-ticket-items">
          <c:forEach var="it" items="${g.items}">
            <section class="kds-ticket-item">
              <div class="kds-ticket-item__head">
                <strong>${it.quantity} × <c:out value="${it.productName}" /></strong>
                <span class="badge badge-ready">Đã nhận</span>
              </div>
              <c:if test="${not empty it.note}"><div class="kds-note"><c:out value="${it.note}" /></div></c:if>
              <form action="${ctx}/cashier/inbox" method="post">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="serve">
                <input type="hidden" name="orderItemId" value="${it.orderItemId}">
                <button class="btn btn-ghost btn-sm btn-full">Đã giao món này</button>
              </form>
            </section>
          </c:forEach>
        </div>

        <c:if test="${g.canServeAll}">
          <div class="kds-card__foot">
            <form action="${ctx}/cashier/inbox" method="post" style="width:100%"
                  data-confirm="Xác nhận đã giao tất cả ${g.cupCount} món cho ${g.tableNumber}?">
              <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
              <input type="hidden" name="action" value="serveAll">
              <input type="hidden" name="tableNumber" value="<c:out value='${g.tableNumber}'/>">
              <c:forEach var="orderId" items="${g.orderIds}">
                <input type="hidden" name="orderId" value="${orderId}">
              </c:forEach>
              <button class="btn btn-primary btn-full">Đã giao tất cả ${g.cupCount} món</button>
            </form>
          </div>
        </c:if>
      </article>
    </c:forEach>
  </div>
</c:if>
