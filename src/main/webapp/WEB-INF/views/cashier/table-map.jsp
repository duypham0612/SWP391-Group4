<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Bán hàng</div><h1>Sơ đồ bàn</h1><p>sales.DiningTable · TableSession — mở bàn để bắt đầu đặt món</p></div>
    <a class="btn btn-ghost" href="${ctx}/cashier/pos">POS đem về (takeaway)</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>

<div class="table-toolbar">
    <div class="table-search">
        <input id="tableSearch" class="form-control" type="search" placeholder="Tìm bàn..." autocomplete="off">
    </div>
</div>

<div class="table-grid" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:16px">
    <c:forEach var="t" items="${tables}">
        <c:set var="tblClass" value="tbl-empty" />
        <c:set var="tblLabel" value="Trống" />
        <c:set var="tblBadge" value="badge-served" />
        <c:if test="${not empty t.activeSessionId}">
            <c:set var="tblClass" value="tbl-draft" />
            <c:set var="tblLabel" value="Nháp" />
            <c:set var="tblBadge" value="badge-waiting" />
        </c:if>
        <c:if test="${not empty t.activeSessionId and t.activeItemCount > 0}">
            <c:set var="tblClass" value="tbl-busy" />
            <c:set var="tblLabel" value="Đang phục vụ" />
            <c:set var="tblBadge" value="badge-ready" />
        </c:if>
        <c:choose>
            <c:when test="${not empty t.activeSessionId}">
                <a class="card table-card table-card-link ${tblClass}" data-name="${t.tableNumber}" href="${ctx}/cashier/pos?sessionId=${t.activeSessionId}">
                    <div style="display:flex;justify-content:space-between;align-items:center">
                        <strong style="font-size:1.1rem">${t.tableNumber}</strong>
                        <span class="badge ${tblBadge}">${tblLabel}</span>
                    </div>
                    <div class="muted">${t.activeItemCount} món · phiên #${t.activeSessionId}</div>
                </a>
            </c:when>
            <c:otherwise>
                <div class="card table-card ${tblClass}" data-name="${t.tableNumber}">
                    <div style="display:flex;justify-content:space-between;align-items:center">
                        <strong style="font-size:1.1rem">${t.tableNumber}</strong>
                        <span class="badge ${tblBadge}">${tblLabel}</span>
                    </div>
                    <div class="muted">Bàn trống</div>
                    <form action="${ctx}/cashier/table" method="post">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="openTable">
                        <input type="hidden" name="tableId" value="${t.diningTableId}">
                        <button type="submit" class="btn btn-primary btn-sm">Mở bàn</button>
                    </form>
                </div>
            </c:otherwise>
        </c:choose>
    </c:forEach>
</div>
<div id="tableNoMatch" class="card empty-state" style="display:none;margin-top:16px"><div class="icon">∅</div><p>Không tìm thấy bàn phù hợp.</p></div>

<c:if test="${empty tables}">
    <div class="card empty-state"><div class="icon">∅</div><p>Chi nhánh chưa có bàn nào.</p></div>
</c:if>

<script>
document.getElementById('tableSearch').addEventListener('input', function(){
  const q = this.value.trim().toLowerCase();
  let shown = 0;
  document.querySelectorAll('.table-card').forEach(card => {
    const ok = !q || (card.dataset.name || '').toLowerCase().includes(q);
    card.style.display = ok ? '' : 'none';
    if (ok) shown++;
  });
  document.getElementById('tableNoMatch').style.display = shown === 0 ? '' : 'none';
});
</script>

<jsp:include page="../layout/footer.jsp" />
