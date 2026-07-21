<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Pha chế</div>
        <h1>Bảng điều khiển ca</h1>
        <p>${sessionScope.authUser.branchName} · tự cập nhật mỗi <span id="baristaCountdown">5</span> giây</p>
    </div>
    <div style="display:flex;gap:8px;flex-wrap:wrap">
        <a class="btn btn-primary" href="${ctx}/barista/kds">Mở quầy pha chế</a>
    </div>
</div>

<div class="card-grid">
    <a class="card stat" href="${ctx}/barista/kds">
        <span class="label">Đang chờ pha</span>
        <span class="value">${queueCount}</span>
        <span class="muted">món · chờ pha + đang pha</span>
    </a>
    <a class="card stat" href="${ctx}/barista/kds" style="${readyCount gt 0 ? 'border-color:var(--st-ready)' : ''}">
        <span class="label">Đã pha xong</span>
        <span class="value">${readyCount}</span>
        <span class="muted">món · chờ mang ra</span>
    </a>
    <div class="card stat">
        <span class="label">Tốc độ pha cả ca hôm nay</span>
        <span class="value">${kpi.avgLeadDisplay}</span>
        <span class="muted">${kpi.cupCount} món đã xong · số của cả ca, không chấm điểm cá nhân</span>
    </div>
    <a class="card stat" href="${ctx}/barista/waste" style="${wasteSummary.totalCost gt 0 ? 'border-color:var(--st-waiting)' : ''}">
        <span class="label">Hao hụt hôm nay</span>
        <span class="value"><fmt:formatNumber value="${wasteSummary.totalCost}" maxFractionDigits="0"/> ₫</span>
        <span class="muted">
            <c:choose>
                <c:when test="${wasteSummary.hasTopIngredient}">
                    Top: ${wasteSummary.topIngredientName} · ${wasteSummary.remakeCount} làm lại
                </c:when>
                <c:otherwise>
                    Chưa ghi nhận hao hụt · ${wasteSummary.remakeCount} làm lại
                </c:otherwise>
            </c:choose>
        </span>
    </a>
    <a class="card stat" href="${ctx}/barista/eightysix" style="${alertCount gt 0 ? 'border-color:var(--st-cancelled)' : ''}">
        <span class="label">Cảnh báo vận hành</span>
        <span class="value">${alertCount}</span>
        <span class="muted">${lowStockCount} tồn thấp<c:if test="${oversoldCount gt 0}"> (${oversoldCount} âm kho)</c:if> · ${eightySixCount} món tạm hết</span>
    </a>
    <a class="card stat" href="${ctx}/barista/prep" style="${prepNeedCount gt 0 ? 'border-color:var(--st-waiting)' : ''}">
        <span class="label">Cần pha bán thành phẩm</span>
        <span class="value">${prepNeedCount}</span>
        <span class="muted">theo đơn đang mở + ngưỡng tồn</span>
    </a>
</div>

<c:if test="${suggest86Count gt 0}">
    <a class="alert alert-warn" href="${ctx}/barista/eightysix" style="display:block;margin-top:16px;text-decoration:none">
        <strong>${suggest86Count} món có nguyên liệu đã cạn</strong> — cân nhắc báo tạm hết. Bấm để xem &amp; xử lý →
    </a>
</c:if>

<div class="grid-2">
    <div class="card">
        <div style="display:flex;justify-content:space-between;gap:12px;align-items:flex-start;margin-bottom:12px">
            <div>
                <h3 style="margin-top:0">Top món chờ lâu nhất</h3>
                <p class="muted" style="margin:0">Ưu tiên theo thứ tự hàng chờ pha.</p>
            </div>
            <a class="btn btn-ghost btn-sm" href="${ctx}/barista/kds">Bump</a>
        </div>
        <c:choose>
            <c:when test="${empty topWaitingItems}">
                <p class="muted">Không có món nào đang chờ.</p>
            </c:when>
            <c:otherwise>
                <table class="table">
                    <thead><tr><th>Món</th><th>Bàn</th><th>Trạng thái</th></tr></thead>
                    <tbody>
                        <c:forEach var="it" items="${topWaitingItems}">
                            <tr>
                                <td><strong>${it.quantity}× ${it.productName}</strong><div class="muted">Đơn #${it.orderId}</div></td>
                                <td><c:choose><c:when test="${not empty it.tableNumber}">${it.tableNumber}</c:when><c:otherwise>Đem về</c:otherwise></c:choose></td>
                                <td><jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="${it.status}" /></jsp:include></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="card" id="low-stock">
        <div style="display:flex;justify-content:space-between;gap:12px;align-items:flex-start;margin-bottom:12px">
            <div>
                <h3 style="margin-top:0">Nguyên liệu sắp hết</h3>
                <p class="muted" style="margin:0">Theo ngưỡng tồn của chi nhánh.</p>
            </div>
            <a class="btn btn-ghost btn-sm" href="#low-stock">Tồn thấp</a>
        </div>
        <c:choose>
            <c:when test="${empty lowStockPreview}">
                <p class="muted">Tồn kho ổn định.</p>
            </c:when>
            <c:otherwise>
                <table class="table">
                    <thead><tr><th>Nguyên liệu</th><th>Tồn</th><th>Ngưỡng</th></tr></thead>
                    <tbody>
                        <c:forEach var="l" items="${lowStockPreview}">
                            <tr>
                                <td>${l.ingredientName}
                                    <c:choose>
                                        <c:when test="${l.quantityOnHand lt 0}"><span class="badge badge-cancelled" style="margin-left:6px">Âm kho</span></c:when>
                                        <c:otherwise><span class="badge badge-waiting" style="margin-left:6px">Thấp</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${l.quantityOnHand} ${l.ingredientUnit}</td>
                                <td>${l.minThreshold}</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<div class="card" style="margin-top:var(--s5)">
    <div style="display:flex;justify-content:space-between;align-items:center;gap:12px">
        <div><h3 style="margin:0">Lịch sử thao tác gần đây</h3><p class="muted" style="margin:4px 0 0">Audit trong ca/chi nhánh, không thay thế sổ cái tồn.</p></div>
        <a class="btn btn-ghost btn-sm" href="${ctx}/barista/waste">Mở nhật ký</a>
    </div>
    <div class="table-scroll" style="margin-top:12px">
        <table class="table"><thead><tr><th>Thời gian</th><th>Hành động</th><th>Đối tượng</th><th>Người thực hiện</th></tr></thead>
        <tbody><c:forEach var="h" items="${baristaHistory}"><tr><td>${h.createdAt}</td><td>${h.actionLabel}</td><td>${h.entityLabel} <c:if test="${not empty h.entityId}">#${h.entityId}</c:if></td><td>${h.performedByName}</td></tr></c:forEach></tbody></table>
    </div>
</div>

<div class="card" style="margin-top:24px">
    <h3 style="margin-top:0">Trạng thái món</h3>
    <p class="muted" style="margin-top:0">Trạng thái dùng chung cho Quầy pha chế, Thu ngân và tracking QR khách.</p>
    <div style="display:flex; gap:10px; flex-wrap:wrap; margin-top:6px">
        <jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="WAITING" /></jsp:include>
        <jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="MAKING" /></jsp:include>
        <jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="READY" /></jsp:include>
        <jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="SERVED" /></jsp:include>
        <jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="CANCELLED" /></jsp:include>
    </div>
</div>

<script>
  (function(){
    var n = 5, el = document.getElementById('baristaCountdown');
    setInterval(function(){
      if (document.visibilityState === 'hidden') return;
      n--; if (el) el.textContent = n;
      if (n <= 0) location.reload();
    }, 1000);
  })();
</script>
<jsp:include page="../layout/footer.jsp" />
