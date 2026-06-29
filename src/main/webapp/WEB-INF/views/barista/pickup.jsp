<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Món sẵn lấy</h1><p>Trạng thái READY — bấm "Đã phục vụ" khi giao cho khách</p></div>
    <a class="btn btn-ghost" href="${ctx}/barista/kds">← Hàng chờ</a>
</div>

<c:choose>
    <c:when test="${empty readyItems}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có món nào sẵn lấy.</p></div>
    </c:when>
    <c:otherwise>
        <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:14px">
            <c:forEach var="it" items="${readyItems}">
                <div class="card" style="display:flex;flex-direction:column;gap:8px;border-left:4px solid var(--st-ready)">
                    <div style="display:flex;justify-content:space-between;align-items:center">
                        <strong>${it.quantity}× ${it.productName}</strong>
                        <span class="badge badge-ready">Sẵn lấy</span>
                    </div>
                    <div class="muted"><c:choose><c:when test="${not empty it.tableNumber}">${it.tableNumber}</c:when><c:otherwise>Đem về</c:otherwise></c:choose> · đơn #${it.orderId}</div>
                    <form action="${ctx}/barista/pickup" method="post">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="markServed">
                        <input type="hidden" name="orderItemId" value="${it.orderItemId}">
                        <button type="submit" class="btn btn-primary btn-sm" style="width:100%">Đã phục vụ</button>
                    </form>
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<div class="muted" style="text-align:right;font-size:.8rem;margin-top:12px">
    <span style="color:var(--st-ready)">●</span> Tự cập nhật mỗi <span id="puCountdown">5</span> giây
</div>
<script>
  // Pickup board realtime — auto-poll 5s; tab nền thì tạm dừng
  (function(){
    var n = 5, el = document.getElementById('puCountdown');
    setInterval(function(){
      if (document.visibilityState === 'hidden') return;
      n--; if (el) el.textContent = n;
      if (n <= 0) location.reload();
    }, 1000);
  })();
</script>
<jsp:include page="../layout/footer.jsp" />
