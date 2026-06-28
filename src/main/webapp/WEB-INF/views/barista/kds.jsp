<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Hàng chờ (KDS)</h1><p>Bấm "Xong" để trừ tồn tự động (modifier-aware) và chuyển READY</p></div>
    <a class="btn btn-ghost" href="${ctx}/barista/pickup">Món sẵn lấy →</a>
</div>

<c:choose>
    <c:when test="${empty queue}">
        <div class="card empty-state"><div class="icon">✓</div><p>Không có món nào đang chờ. Bếp trống.</p></div>
    </c:when>
    <c:otherwise>
        <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:14px">
            <c:forEach var="it" items="${queue}">
                <c:set var="barColor" value="${it.status == 'MAKING' ? '#2F6FB0' : '#E0A100'}" />
                <div class="card" style="display:flex;flex-direction:column;gap:8px;border-left:4px solid ${barColor}">
                    <div style="display:flex;justify-content:space-between;align-items:center">
                        <strong style="font-size:1.05rem">${it.quantity}× ${it.productName}</strong>
                        <c:choose>
                            <c:when test="${it.status == 'MAKING'}"><span class="badge badge-making">Đang pha</span></c:when>
                            <c:otherwise><span class="badge badge-waiting">Chờ</span></c:otherwise>
                        </c:choose>
                    </div>
                    <div class="muted">
                        <c:choose><c:when test="${not empty it.tableNumber}">${it.tableNumber}</c:when><c:otherwise>Đem về</c:otherwise></c:choose>
                        · đơn #${it.orderId}
                    </div>
                    <c:if test="${not empty it.modifiers}">
                        <div style="font-size:.88rem">
                            <c:forEach var="om" items="${it.modifiers}" varStatus="st">${om.optionName}<c:if test="${not st.last}">, </c:if></c:forEach>
                        </div>
                    </c:if>
                    <c:if test="${not empty it.note}"><div class="muted" style="font-style:italic">“${it.note}”</div></c:if>
                    <div style="display:flex;gap:8px;margin-top:6px">
                        <c:if test="${it.status == 'WAITING'}">
                            <form action="${ctx}/barista/kds" method="post" style="flex:1">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="start">
                                <input type="hidden" name="orderItemId" value="${it.orderItemId}">
                                <button type="submit" class="btn btn-ghost btn-sm" style="width:100%">Bắt đầu</button>
                            </form>
                        </c:if>
                        <form action="${ctx}/barista/kds" method="post" style="flex:1" onsubmit="return confirm('Hoàn thành món? Tồn sẽ bị trừ.');">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="markReady">
                            <input type="hidden" name="orderItemId" value="${it.orderItemId}">
                            <button type="submit" class="btn btn-primary btn-sm" style="width:100%">Xong (READY)</button>
                        </form>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<script>setTimeout(function(){ location.reload(); }, 5000);</script>
<jsp:include page="../layout/footer.jsp" />
