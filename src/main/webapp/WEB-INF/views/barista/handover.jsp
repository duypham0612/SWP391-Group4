<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Bàn giao ca</h1><p>Ghi lại tình hình quầy cho ca sau</p></div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
</c:if>

<c:if test="${expiredPrepBatchCount > 0}">
    <div class="alert alert-warn handover-expiry">
        Có ${expiredPrepBatchCount} mẻ pha sẵn đã quá hạn. Kiểm tra tủ trước khi bàn giao ca.
        <a href="${ctx}/barista/prep">Mở màn Pha sẵn</a>
    </div>
</c:if>

<div class="card form-card" style="margin-bottom:18px">
    <h3 style="margin-top:0">Ghi bàn giao</h3>
    <form action="${ctx}/barista/handover" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="create">
        <div class="form-group">
            <label>Nội dung bàn giao</label>
            <textarea name="note" class="form-control" rows="3" maxlength="1000" required
                      placeholder="VD: còn 2 mẻ cold brew, máy xay #2 kêu lạ, cần đặt thêm sữa oat..."></textarea>
        </div>
        <button type="submit" class="btn btn-primary">Lưu bàn giao</button>
    </form>
</div>

<%-- Ghi bàn giao xong thì tan ca luôn, khỏi phải sang màn Ca làm của tôi.
     Vẫn ghi được bàn giao khi đã tan ca — không khoá form theo trạng thái ca. --%>
<c:if test="${not empty clockStatus}">
    <div class="card handover-clock">
        <div class="handover-clock__text">
            <c:choose>
                <c:when test="${clockStatus.canClockOut}"><span class="badge badge-making">Đang làm</span></c:when>
                <c:otherwise><span class="badge badge-cancelled">Ngoài ca</span></c:otherwise>
            </c:choose>
            <span class="muted">${clockStatus.statusText}</span>
        </div>
        <c:choose>
            <c:when test="${clockStatus.canClockOut}">
                <form action="${clockPostUrl}" method="post">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="clockOut">
                    <button type="submit" class="btn btn-primary">Tan ca</button>
                </form>
            </c:when>
            <c:otherwise>
                <a class="btn btn-ghost" href="${ctx}/barista/shift">Ca làm của tôi →</a>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>

<h3>Lịch sử bàn giao</h3>
<c:choose>
    <c:when test="${empty handovers}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có ghi chú bàn giao nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th style="width:180px">Lúc</th><th style="width:160px">Người ghi</th><th>Nội dung</th></tr></thead>
            <tbody>
                <c:forEach var="h" items="${handovers}">
                    <tr>
                        <td>${h.createdDisplay}</td>
                        <td>${h.createdByName}</td>
                        <td>${h.note}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<div class="page-header" style="margin-top:var(--s6)">
    <div><h2 style="margin:0">Cả quán hôm nay</h2><p>Số liệu của toàn chi nhánh, không phải riêng bạn</p></div>
</div>

<div class="card-grid">
    <div class="card stat">
        <span class="label">Thời gian pha trung bình</span>
        <span class="value">${kpi.avgLeadDisplay}</span>
        <div class="muted" style="font-size:.8em;margin-top:4px">Chỉ tính món có mốc “bắt đầu pha”</div>
    </div>
    <div class="card stat">
        <span class="label">Số món đã pha xong</span>
        <span class="value">${kpi.cupCount}</span>
        <div class="muted" style="font-size:.8em;margin-top:4px">Gồm cả món pha nhanh (READY thẳng)</div>
    </div>
</div>

<h3>Ly cả quán đã pha hôm nay</h3>
<div>
    <form id="brewHistoryFilters" class="table-toolbar" action="${ctx}/barista/handover" method="get">
        <input type="hidden" name="page" value="1">
        <div class="form-group table-search">
            <label for="brewHistorySearch">Tìm kiếm</label>
            <input id="brewHistorySearch" class="form-control" type="search" name="q" value="${fn:escapeXml(brewHistoryQuery)}"
                   placeholder="Mã món, mã đơn, món, người pha hoặc vị trí" autocomplete="off">
        </div>
        <div class="form-group">
            <label for="brewHistoryStatusFilter">Trạng thái</label>
            <select id="brewHistoryStatusFilter" name="status" class="form-control tt-filter">
                <option value="">Tất cả</option>
                <option value="READY" ${brewHistoryStatus == 'READY' ? 'selected' : ''}>Đã pha xong</option>
                <option value="PICKED_UP" ${brewHistoryStatus == 'PICKED_UP' ? 'selected' : ''}>Đã được nhận</option>
                <option value="SERVED" ${brewHistoryStatus == 'SERVED' ? 'selected' : ''}>Đã phục vụ</option>
            </select>
        </div>
        <div class="form-group">
            <label for="brewHistoryOrderTypeFilter">Loại đơn</label>
            <select id="brewHistoryOrderTypeFilter" name="orderType" class="form-control tt-filter">
                <option value="">Tất cả</option>
                <option value="DINE_IN" ${brewHistoryOrderType == 'DINE_IN' ? 'selected' : ''}>Tại bàn</option>
                <option value="TAKEAWAY" ${brewHistoryOrderType == 'TAKEAWAY' ? 'selected' : ''}>Mang đi</option>
                <option value="DELIVERY" ${brewHistoryOrderType == 'DELIVERY' ? 'selected' : ''}>Giao hàng</option>
            </select>
        </div>
        <div class="form-group">
            <label for="brewHistoryPageSize">Hiển thị</label>
            <select id="brewHistoryPageSize" name="pageSize" class="form-control tt-size">
                <option value="10" ${brewHistoryPage.pageSize == 10 ? 'selected' : ''}>10</option>
                <option value="20" ${brewHistoryPage.pageSize == 20 ? 'selected' : ''}>20</option>
                <option value="50" ${brewHistoryPage.pageSize == 50 ? 'selected' : ''}>50</option>
            </select>
        </div>
    </form>
    <div class="card" style="margin-bottom:18px;padding:0;overflow:auto">
        <table class="table" style="margin:0;min-width:780px">
            <thead>
                <tr>
                    <th style="width:100px">Mã</th>
                    <th style="width:150px">Giờ xong</th>
                    <th>Món</th>
                    <th style="width:70px">SL</th>
                    <th style="width:120px">Loại đơn</th>
                    <th style="width:150px">Người pha</th>
                    <th style="width:140px">Vị trí</th>
                    <th style="width:120px">Trạng thái</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${empty brewHistory}">
                        <tr class="tt-empty"><td colspan="8">Không tìm thấy ly phù hợp hôm nay.</td></tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="item" items="${brewHistory}">
                            <tr>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty item.pickupCode}"><strong>${item.pickupCode}</strong></c:when>
                                        <c:otherwise>#${item.orderItemId}</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${item.doneDisplay}</td>
                                <td>${item.productName}</td>
                                <td>${item.quantity}</td>
                                <td>${item.orderTypeLabel}</td>
                                <td><c:out value="${empty item.preparedByName ? '—' : item.preparedByName}" /></td>
                                <td><c:out value="${empty item.handoverLocation ? '—' : item.handoverLocation}" /></td>
                                <td><jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="${item.status}" /></jsp:include></td>
                            </tr>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
    <div class="table-tools-foot">
        <span class="tt-summary" aria-live="polite">${brewHistoryPage.startRow}-${brewHistoryPage.endRow} / ${brewHistoryPage.total}</span>
        <c:if test="${brewHistoryPage.totalPages > 1}">
            <div class="pagination" aria-label="Phân trang ly cả quán đã pha hôm nay">
                <c:url var="firstBrewHistoryPageUrl" value="/barista/handover"><c:param name="q" value="${brewHistoryQuery}" /><c:param name="status" value="${brewHistoryStatus}" /><c:param name="orderType" value="${brewHistoryOrderType}" /><c:param name="pageSize" value="${brewHistoryPage.pageSize}" /><c:param name="page" value="1" /></c:url>
                <c:url var="previousBrewHistoryPageUrl" value="/barista/handover"><c:param name="q" value="${brewHistoryQuery}" /><c:param name="status" value="${brewHistoryStatus}" /><c:param name="orderType" value="${brewHistoryOrderType}" /><c:param name="pageSize" value="${brewHistoryPage.pageSize}" /><c:param name="page" value="${brewHistoryPage.page - 1}" /></c:url>
                <a class="page" href="${firstBrewHistoryPageUrl}" aria-disabled="${not brewHistoryPage.hasPrevious}">«</a>
                <a class="page" href="${previousBrewHistoryPageUrl}" aria-disabled="${not brewHistoryPage.hasPrevious}">‹</a>
                <c:forEach var="pageNumber" items="${brewHistoryPage.visiblePages}">
                    <c:url var="brewHistoryPageUrl" value="/barista/handover"><c:param name="q" value="${brewHistoryQuery}" /><c:param name="status" value="${brewHistoryStatus}" /><c:param name="orderType" value="${brewHistoryOrderType}" /><c:param name="pageSize" value="${brewHistoryPage.pageSize}" /><c:param name="page" value="${pageNumber}" /></c:url>
                    <a class="page ${pageNumber == brewHistoryPage.page ? 'is-active' : ''}" href="${brewHistoryPageUrl}" aria-current="${pageNumber == brewHistoryPage.page ? 'page' : 'false'}">${pageNumber}</a>
                </c:forEach>
                <c:url var="nextBrewHistoryPageUrl" value="/barista/handover"><c:param name="q" value="${brewHistoryQuery}" /><c:param name="status" value="${brewHistoryStatus}" /><c:param name="orderType" value="${brewHistoryOrderType}" /><c:param name="pageSize" value="${brewHistoryPage.pageSize}" /><c:param name="page" value="${brewHistoryPage.page + 1}" /></c:url>
                <c:url var="lastBrewHistoryPageUrl" value="/barista/handover"><c:param name="q" value="${brewHistoryQuery}" /><c:param name="status" value="${brewHistoryStatus}" /><c:param name="orderType" value="${brewHistoryOrderType}" /><c:param name="pageSize" value="${brewHistoryPage.pageSize}" /><c:param name="page" value="${brewHistoryPage.totalPages}" /></c:url>
                <a class="page" href="${nextBrewHistoryPageUrl}" aria-disabled="${not brewHistoryPage.hasNext}">›</a>
                <a class="page" href="${lastBrewHistoryPageUrl}" aria-disabled="${not brewHistoryPage.hasNext}">»</a>
            </div>
        </c:if>
    </div>
</div>

<script>
  (function(){
    var form = document.getElementById('brewHistoryFilters');
    if (!form) return;
    var search = document.getElementById('brewHistorySearch');
    var page = form.querySelector('input[name="page"]');
    var timer;

    function submitFromFirstPage(){
      if (page) page.value = '1';
      if (form.requestSubmit) form.requestSubmit();
      else form.submit();
    }

    if (search) search.addEventListener('input', function(){
      window.clearTimeout(timer);
      timer = window.setTimeout(submitFromFirstPage, 350);
    });

    Array.prototype.forEach.call(form.querySelectorAll('select'), function(control){
      control.addEventListener('change', submitFromFirstPage);
    });
  })();
</script>

<jsp:include page="../layout/footer.jsp" />
