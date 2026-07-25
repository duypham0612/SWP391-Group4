<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Pha chế</div>
        <h1>Bảng điều khiển ca</h1>
        <p>${sessionScope.authUser.branchName}</p>
    </div>
    <div style="display:flex;gap:8px;flex-wrap:wrap">
        <a class="btn btn-ghost" href="${ctx}/barista/dashboard">↻ Làm mới</a>
        <a class="btn btn-primary" href="${ctx}/barista/kds">Mở quầy pha chế</a>
    </div>
</div>

<h3 class="section-title">Nhịp làm việc của bạn</h3>
<div class="card-grid card-grid--4">
    <a class="card stat" href="${ctx}/barista/kds?owner=mine">
        <span class="label">Bạn đang pha</span><span class="value">${baristaOps.myMakingCups}</span><span class="muted">ly đang giữ tại quầy</span>
    </a>
    <a class="card stat" href="${ctx}/barista/kds?owner=mine">
        <span class="label">Bạn đã hoàn thành</span><span class="value">${baristaOps.myCompletedCups}</span><span class="muted">ly trong ngày kinh doanh</span>
    </a>
    <a class="card stat" href="${ctx}/barista/waste">
        <span class="label">Làm lại / hao hụt</span><span class="value">${baristaOps.myRemakeCount} / ${baristaOps.myWasteCount}</span><span class="muted">sự kiện do bạn ghi</span>
    </a>
    <a class="card stat" href="${ctx}/barista/kds?owner=mine">
        <span class="label">Thời gian pha TB</span><span class="value" style="font-size:1.25rem">${baristaOps.myAveragePreparationDisplay}</span><span class="muted">các ly bạn đã hoàn thành</span>
    </a>
</div>

<h3 class="section-title">Tổng quan quầy pha chế</h3>
<div class="card-grid card-grid--4">
    <a class="card stat" href="${ctx}/barista/kds"><span class="label">Chờ / đang pha</span><span class="value">${baristaOps.branchWaitingCups} / ${baristaOps.branchMakingCups}</span><span class="muted">ly của toàn quầy</span></a>
    <a class="card stat" href="${ctx}/barista/kds"><span class="label">Sẵn sàng / bị chặn</span><span class="value">${baristaOps.branchReadyCups} / ${baristaOps.branchBlockedCups}</span><span class="muted">ly cần được tiếp nhận hoặc xử lý</span></a>
    <a class="card stat" href="${ctx}/barista/waste"><span class="label">Làm lại toàn quầy</span><span class="value">${baristaOps.branchRemakeCount}</span><span class="muted">lần trong ngày kinh doanh</span></a>
    <a class="card stat" href="${ctx}/barista/prep" style="${baristaOps.expiredPrepBatchCount gt 0 ? 'border-color:var(--st-waiting)' : ''}"><span class="label">Mẻ prep quá hạn</span><span class="value">${baristaOps.expiredPrepBatchCount}</span><span class="muted">cần ghi hao hụt trước khi bàn giao</span></a>
</div>

<jsp:include page="../layout/_handoverPendingAlert.jsp" />

<div class="card-grid card-grid--4">
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
                                <td>${l.quantityOnHandDisplay} ${l.ingredientUnit}</td>
                                <td>${l.minThresholdDisplay}</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<jsp:include page="../layout/footer.jsp" />
