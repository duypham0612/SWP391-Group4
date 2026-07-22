<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
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

<c:if test="${pendingHandoverCount > 0}">
    <a class="alert alert-warn" href="${ctx}/barista/handover" style="display:block;margin-bottom:16px;text-decoration:none">
        <strong>${pendingHandoverCount} bàn giao ca đang chờ bạn xác nhận.</strong> Đọc và tiếp nhận các việc trước khi xử lý →
    </a>
</c:if>

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
    <a class="card stat" href="${ctx}/barista/waste" style="${wasteSummary.activeCount gt 0 ? 'border-color:var(--st-waiting)' : ''}">
        <span class="label">Hao hụt hôm nay</span>
        <span class="value">${wasteSummary.remakeCount}</span>
        <span class="muted">
            lần làm lại
            <c:choose>
                <c:when test="${wasteSummary.hasTopIngredient}">
                    · ${wasteSummary.ingredientWasteCount} lần bỏ nguyên liệu · Top: ${wasteSummary.topIngredientName}
                </c:when>
                <c:otherwise>
                    · ${wasteSummary.ingredientWasteCount} lần bỏ nguyên liệu
                </c:otherwise>
            </c:choose>
        </span>
    </a>
    <a class="card stat" href="${ctx}/barista/eightysix" style="${alertCount gt 0 ? 'border-color:var(--st-cancelled)' : ''}">
        <span class="label">Cảnh báo vận hành</span>
        <span class="value">${alertCount}</span>
        <span class="muted">${lowStockCount} tồn thấp<c:if test="${oversoldCount gt 0}"> (${oversoldCount} âm kho)</c:if> · ${eightySixCount} món tạm hết</span>
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
            <a class="btn btn-ghost btn-sm" href="${ctx}/barista/kds">Mở KDS</a>
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
