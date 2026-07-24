<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<c:if test="${not empty sessionScope.flashOk}"><div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" /></c:if>
<c:if test="${not empty sessionScope.flashError}"><div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" /></c:if>

<div class="page-header">
    <div>
        <div class="eyebrow">Kho chi nhánh</div>
        <h1>Đối soát tồn và hao hụt</h1>
        <p>Kiểm kê chênh lệch, xử lý ngoại lệ và theo dõi hao hụt trong cùng một màn hình.</p>
    </div>
    <a class="btn btn-primary" href="${ctx}/manager/reconciliation?action=new">+ Tạo lần kiểm kê</a>
</div>

<section class="card" style="margin-bottom:22px">
    <div class="waste-card__head">
        <div><h3>Lịch sử kiểm kê và điều chỉnh tồn</h3><p>Các lần ghi nhận chênh lệch giữa số lượng trên hệ thống và số lượng kiểm đếm thực tế.</p></div>
        <strong>${fn:length(adjustments)} lần</strong>
    </div>
    <c:choose>
        <c:when test="${empty adjustments}">
            <div class="empty-state"><div class="icon">✓</div><p>Chưa phát sinh chênh lệch tồn kho.</p></div>
        </c:when>
        <c:otherwise>
            <div class="table-scroll"><table class="table">
                <thead><tr><th>Mã</th><th>Nguyên liệu</th><th>Tồn hệ thống</th><th>Tồn thực tế</th><th>Chênh lệch</th><th>Lý do</th><th>Người thực hiện</th></tr></thead>
                <tbody><c:forEach var="a" items="${adjustments}"><tr>
                    <td>#${a.stockAdjustmentId}</td><td>${a.ingredientName}</td>
                    <td>${a.systemQtyDisplay} ${a.displayUnit}</td><td>${a.actualQtyDisplay} ${a.displayUnit}</td>
                    <td><strong><c:if test="${a.diffQty.signum() > 0}">+</c:if>${a.diffQtyDisplay}</strong></td>
                    <td><c:out value="${a.reason}" /></td><td>${a.adjustedByName}</td>
                </tr></c:forEach></tbody>
            </table></div>
        </c:otherwise>
    </c:choose>
</section>

<div class="section-title">
    <h2 style="margin:0">Hao hụt và làm lại món</h2>
    <p class="muted" style="margin:4px 0 0">Số liệu trong khoảng ${range.label} (${range.dayCount} ngày).</p>
</div>

<c:url var="todayUrl" value="/manager/reconciliation">
    <c:param name="from" value="${todayDate}" />
    <c:param name="to" value="${todayDate}" />
</c:url>
<c:url var="last7Url" value="/manager/reconciliation">
    <c:param name="from" value="${last7FromDate}" />
    <c:param name="to" value="${todayDate}" />
</c:url>
<c:url var="last30Url" value="/manager/reconciliation">
    <c:param name="from" value="${last30FromDate}" />
    <c:param name="to" value="${todayDate}" />
</c:url>

<form class="table-toolbar" action="${ctx}/manager/reconciliation" method="get">
    <input type="hidden" name="page" value="1">
    <div class="form-group">
        <label for="fromDate">Từ ngày</label>
        <input id="fromDate" class="form-control" type="date" name="from" value="${range.fromDate}">
    </div>
    <div class="form-group">
        <label for="toDate">Đến ngày</label>
        <input id="toDate" class="form-control" type="date" name="to" value="${range.toDate}">
    </div>
    <div class="form-group">
        <label>&nbsp;</label>
        <button class="btn btn-primary" type="submit">Áp dụng</button>
    </div>
    <div class="form-group">
        <label>Khoảng nhanh</label>
        <div class="btn-row">
            <a class="btn btn-ghost btn-sm" href="${todayUrl}">Hôm nay</a>
            <a class="btn btn-ghost btn-sm" href="${last7Url}">7 ngày</a>
            <a class="btn btn-ghost btn-sm" href="${last30Url}">30 ngày</a>
        </div>
    </div>
</form>

<section class="waste-summary">
    <div class="card stat">
        <span class="label">Tổng chi phí</span>
        <span class="value"><fmt:formatNumber value="${summary.totalCost}" maxFractionDigits="0"/> ₫</span>
        <small>${summary.activeCount} dòng hiệu lực</small>
    </div>
    <div class="card stat">
        <span class="label">Hao hụt nguyên liệu</span>
        <span class="value">${summary.ingredientWasteCount}</span>
        <small><fmt:formatNumber value="${summary.ingredientWasteCost}" maxFractionDigits="0"/> ₫</small>
    </div>
    <div class="card stat">
        <span class="label">Làm lại món</span>
        <span class="value">${summary.remakeCount}</span>
        <small><fmt:formatNumber value="${summary.remakeCost}" maxFractionDigits="0"/> ₫</small>
    </div>
    <div class="card stat">
        <span class="label">Hao nhiều nhất</span>
        <span class="value waste-top-name">
            <c:choose>
                <c:when test="${summary.hasTopIngredient}">${summary.topIngredientName}</c:when>
                <c:otherwise>-</c:otherwise>
            </c:choose>
        </span>
        <small>
            <c:choose>
                <c:when test="${summary.hasTopIngredient}"><fmt:formatNumber value="${summary.topIngredientCost}" maxFractionDigits="0"/> ₫</c:when>
                <c:otherwise>Chưa đủ dữ liệu giá</c:otherwise>
            </c:choose>
        </small>
    </div>
</section>

<p class="muted">Bốn số trên tính cho toàn bộ khoảng ngày ${range.label}, không chịu ảnh hưởng của bộ lọc và phân trang bên dưới.</p>

<section class="card waste-review-card">
    <div class="waste-card__head"><div><h3>Ngoại lệ cần xử lý</h3><p>Kiểm tra các trường hợp tồn bị âm sau khi ghi nhận hao hụt, sau đó kiểm kê và điều chỉnh nếu cần.</p></div><strong>${fn:length(openReviews)} trường hợp</strong></div>
    <c:choose><c:when test="${empty openReviews}"><p class="muted">Không có ngoại lệ đang chờ xử lý.</p></c:when><c:otherwise>
        <div class="table-scroll"><table class="table"><thead><tr><th>Loại</th><th>Nguyên liệu</th><th>Tồn trước → sau</th><th>Ghi chú</th><th></th></tr></thead><tbody>
        <c:forEach var="r" items="${openReviews}"><tr><td><c:choose><c:when test="${r.reviewType == 'NEGATIVE_STOCK'}">Tồn kho âm</c:when><c:otherwise>Cần kiểm tra</c:otherwise></c:choose></td><td>${r.ingredientName}</td><td>${r.qtyBefore} → ${r.qtyAfter}</td><td><c:out value="${r.note}" /></td><td><form action="${ctx}/manager/waste" method="post"><input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="resolveReview"><input type="hidden" name="reviewId" value="${r.wasteReviewId}"><input class="form-control" name="note" maxlength="255" placeholder="Ghi chú cách xử lý" required><button class="btn btn-primary btn-sm" type="submit">Đánh dấu đã xử lý</button></form></td></tr></c:forEach>
        </tbody></table></div>
    </c:otherwise></c:choose>
</section>

<c:if test="${summary.missingCostCount > 0}">
    <div class="alert alert-info">Có ${summary.missingCostCount} dòng chưa có đơn giá nhập gần nhất, thành tiền đang để “Chưa có giá”.</div>
</c:if>

<h3 class="section-title">Nhật ký hao hụt &amp; làm lại</h3>
<form id="wasteLogFilters" class="table-toolbar" action="${ctx}/manager/reconciliation" method="get">
    <input type="hidden" name="page" value="1">
    <input type="hidden" name="from" value="${range.fromDate}">
    <input type="hidden" name="to" value="${range.toDate}">
    <div class="form-group table-search">
        <label for="wasteLogSearch">Tìm kiếm</label>
        <input id="wasteLogSearch" class="form-control" type="search" name="q" value="${fn:escapeXml(wasteLogQuery)}"
               placeholder="Tìm nguyên liệu, lý do hoặc người ghi" autocomplete="off">
    </div>
    <div class="form-group">
        <label for="wasteTypeFilter">Loại ghi nhận</label>
        <select id="wasteTypeFilter" name="wasteType" class="form-control tt-filter">
            <option value="">Tất cả</option>
            <option value="SPILL" ${wasteLogWasteType == 'SPILL' ? 'selected' : ''}>Đổ/rơi</option>
            <option value="EXPIRED" ${wasteLogWasteType == 'EXPIRED' ? 'selected' : ''}>Hết hạn</option>
            <option value="REMAKE" ${wasteLogWasteType == 'REMAKE' ? 'selected' : ''}>Làm lại món</option>
            <option value="OTHER" ${wasteLogWasteType == 'OTHER' ? 'selected' : ''}>Khác</option>
        </select>
    </div>
    <div class="form-group">
        <label for="wasteStatusFilter">Trạng thái</label>
        <select id="wasteStatusFilter" name="status" class="form-control tt-filter">
            <option value="">Tất cả</option>
            <option value="ACTIVE" ${wasteLogStatus == 'ACTIVE' ? 'selected' : ''}>Hiệu lực</option>
            <option value="VOIDED" ${wasteLogStatus == 'VOIDED' ? 'selected' : ''}>Đã huỷ</option>
        </select>
    </div>
</form>

<div class="table-scroll">
    <table class="table waste-table">
        <thead>
            <tr>
                <th style="width:110px">Thời gian</th>
                <th>Nguyên liệu</th>
                <th style="width:120px">Số lượng</th>
                <th style="width:120px">Loại</th>
                <th>Lý do</th>
                <th style="width:130px">Thành tiền</th>
                <th>Người ghi</th>
                <th style="width:100px">Trạng thái</th>
            </tr>
        </thead>
        <tbody>
            <c:choose>
                <c:when test="${empty logs}">
                    <tr class="tt-empty"><td colspan="8">Không tìm thấy nhật ký phù hợp.</td></tr>
                </c:when>
                <c:otherwise>
                    <c:forEach var="w" items="${logs}">
                        <tr class="${w.status == 'VOIDED' ? 'row-muted' : ''}">
                            <td>${w.loggedAtDisplay}</td>
                            <td>
                                <strong>${w.ingredientName}</strong>
                                <c:if test="${w.ingredientType == 'PREPPED'}"><span class="badge badge-making">Pha sẵn</span></c:if>
                            </td>
                            <td><strong>${w.quantityDisplay}</strong> ${w.ingredientUnit}</td>
                            <td>${w.wasteTypeLabel}</td>
                            <td>${w.reason}<c:if test="${not empty w.wasteEvent}"><small class="muted"><br>${w.wasteEvent.sourceLabel}<c:if test="${not empty w.wasteEvent.productName}"> · ${w.wasteEvent.cupQuantity} × ${w.wasteEvent.productName}</c:if></small></c:if></td>
                            <td><strong>${w.costDisplay}</strong><small class="muted"><br>${w.costBasisLabel}</small></td>
                            <td>${w.loggedByName}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${w.status == 'VOIDED'}"><span class="badge badge-cancelled">Đã huỷ</span></c:when>
                                    <c:otherwise><span class="badge badge-ready">Hiệu lực</span></c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </tbody>
    </table>
</div>

<div class="table-tools-foot">
    <span class="tt-summary" aria-live="polite">${wasteLogPage.startRow}-${wasteLogPage.endRow} / ${wasteLogPage.total}</span>
    <c:if test="${wasteLogPage.totalPages > 1}">
        <div class="pagination" aria-label="Phân trang nhật ký hao hụt">
            <c:url var="firstWasteLogPageUrl" value="/manager/reconciliation">
                <c:param name="from" value="${range.fromDate}" /><c:param name="to" value="${range.toDate}" />
                <c:param name="q" value="${wasteLogQuery}" /><c:param name="wasteType" value="${wasteLogWasteType}" />
                <c:param name="status" value="${wasteLogStatus}" /><c:param name="page" value="1" />
            </c:url>
            <c:url var="previousWasteLogPageUrl" value="/manager/reconciliation">
                <c:param name="from" value="${range.fromDate}" /><c:param name="to" value="${range.toDate}" />
                <c:param name="q" value="${wasteLogQuery}" /><c:param name="wasteType" value="${wasteLogWasteType}" />
                <c:param name="status" value="${wasteLogStatus}" /><c:param name="page" value="${wasteLogPage.page - 1}" />
            </c:url>
            <a class="page" href="${firstWasteLogPageUrl}" aria-disabled="${not wasteLogPage.hasPrevious}">«</a>
            <a class="page" href="${previousWasteLogPageUrl}" aria-disabled="${not wasteLogPage.hasPrevious}">‹</a>
            <c:forEach var="pageNumber" items="${wasteLogPage.visiblePages}">
                <c:url var="wasteLogPageUrl" value="/manager/reconciliation">
                    <c:param name="from" value="${range.fromDate}" /><c:param name="to" value="${range.toDate}" />
                    <c:param name="q" value="${wasteLogQuery}" /><c:param name="wasteType" value="${wasteLogWasteType}" />
                    <c:param name="status" value="${wasteLogStatus}" /><c:param name="page" value="${pageNumber}" />
                </c:url>
                <a class="page ${pageNumber == wasteLogPage.page ? 'is-active' : ''}" href="${wasteLogPageUrl}" aria-current="${pageNumber == wasteLogPage.page ? 'page' : 'false'}">${pageNumber}</a>
            </c:forEach>
            <c:url var="nextWasteLogPageUrl" value="/manager/reconciliation">
                <c:param name="from" value="${range.fromDate}" /><c:param name="to" value="${range.toDate}" />
                <c:param name="q" value="${wasteLogQuery}" /><c:param name="wasteType" value="${wasteLogWasteType}" />
                <c:param name="status" value="${wasteLogStatus}" /><c:param name="page" value="${wasteLogPage.page + 1}" />
            </c:url>
            <c:url var="lastWasteLogPageUrl" value="/manager/reconciliation">
                <c:param name="from" value="${range.fromDate}" /><c:param name="to" value="${range.toDate}" />
                <c:param name="q" value="${wasteLogQuery}" /><c:param name="wasteType" value="${wasteLogWasteType}" />
                <c:param name="status" value="${wasteLogStatus}" /><c:param name="page" value="${wasteLogPage.totalPages}" />
            </c:url>
            <a class="page" href="${nextWasteLogPageUrl}" aria-disabled="${not wasteLogPage.hasNext}">›</a>
            <a class="page" href="${lastWasteLogPageUrl}" aria-disabled="${not wasteLogPage.hasNext}">»</a>
        </div>
    </c:if>
</div>

<script>
(function(){
  var form = document.getElementById('wasteLogFilters');
  if (!form) return;
  form.querySelectorAll('.tt-filter').forEach(function(select){
    select.addEventListener('change', function(){ form.submit(); });
  });
})();
</script>

<jsp:include page="../layout/footer.jsp" />
