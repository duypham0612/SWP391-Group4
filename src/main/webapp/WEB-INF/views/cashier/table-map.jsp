<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Bán hàng</div><h1>Sơ đồ bàn</h1><p>sales.DiningTable · TableSession — mở bàn để bắt đầu đặt món</p></div>
    <a class="btn btn-ghost" href="${ctx}/cashier/pos">POS đem về (takeaway)</a>
</div>

<div class="table-grid" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:16px">
    <c:forEach var="t" items="${tables}">
        <div class="card" style="display:flex;flex-direction:column;gap:10px">
            <div style="display:flex;justify-content:space-between;align-items:center">
                <strong style="font-size:1.1rem">${t.tableNumber}</strong>
                <c:choose>
                    <c:when test="${t.status == 'OCCUPIED' or t.occupied}"><span class="badge badge-making">Đang phục vụ</span></c:when>
                    <c:when test="${t.status == 'CLEANING'}"><span class="badge badge-served">Dọn bàn</span></c:when>
                    <c:otherwise><span class="badge badge-ready">Trống</span></c:otherwise>
                </c:choose>
            </div>
            <c:choose>
                <c:when test="${t.occupied}">
                    <div class="muted">${t.activeItemCount} món · phiên #${t.activeSessionId}</div>
                    <div style="display:flex;gap:8px;flex-wrap:wrap">
                        <a class="btn btn-primary btn-sm" href="${ctx}/cashier/pos?sessionId=${t.activeSessionId}">Đặt món</a>
                        <form action="${ctx}/cashier/table" method="post" onsubmit="return confirm('Đóng bàn này?');">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="closeTable">
                            <input type="hidden" name="sessionId" value="${t.activeSessionId}">
                            <button type="submit" class="btn btn-ghost btn-sm">Đóng bàn</button>
                        </form>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="muted">Bàn trống</div>
                    <form action="${ctx}/cashier/table" method="post">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="openTable">
                        <input type="hidden" name="tableId" value="${t.diningTableId}">
                        <button type="submit" class="btn btn-primary btn-sm">Mở bàn</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </div>
    </c:forEach>
</div>

<c:if test="${empty tables}">
    <div class="card empty-state"><div class="icon">∅</div><p>Chi nhánh chưa có bàn nào.</p></div>
</c:if>

<jsp:include page="../layout/footer.jsp" />
