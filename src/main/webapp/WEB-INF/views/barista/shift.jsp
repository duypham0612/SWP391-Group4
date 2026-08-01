<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Ca làm của tôi</h1><p>Chấm công và giờ làm của bạn</p></div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
</c:if>

<jsp:include page="/WEB-INF/views/layout/_shiftClockCard.jsp" />

<c:if test="${not empty monthSummary}">
    <div class="page-header" style="margin-top:var(--s6)">
        <div><h2 style="margin:0">Giờ làm tháng ${month}</h2><p>Lịch đi làm và giờ công đã ghi nhận</p></div>
        <div style="display:flex;gap:8px;align-items:center">
            <%-- Đổi tháng giữ nguyên từ khoá + bộ lọc, chỉ về trang 1 vì số dòng mỗi tháng khác nhau. --%>
            <c:url var="prevMonthUrl" value="/barista/shift">
                <c:param name="month" value="${prevMonth}" /><c:param name="q" value="${historyQuery}" />
                <c:param name="state" value="${historyState}" /><c:param name="pageSize" value="${historyPage.pageSize}" />
                <c:param name="page" value="1" />
            </c:url>
            <c:url var="nextMonthUrl" value="/barista/shift">
                <c:param name="month" value="${nextMonth}" /><c:param name="q" value="${historyQuery}" />
                <c:param name="state" value="${historyState}" /><c:param name="pageSize" value="${historyPage.pageSize}" />
                <c:param name="page" value="1" />
            </c:url>
            <a class="btn btn-ghost btn-sm" href="${prevMonthUrl}">← Tháng trước</a>
            <strong>${month}</strong>
            <a class="btn btn-ghost btn-sm" href="${nextMonthUrl}">Tháng sau →</a>
        </div>
    </div>

    <div class="card-grid">
        <div class="card stat">
            <span class="label">Giờ đã duyệt</span>
            <span class="value">${monthSummary.approvedHours}h</span>
            <div class="muted" style="font-size:.8em;margin-top:4px">quản lý đã duyệt</div>
        </div>
        <div class="card stat">
            <span class="label">Chờ duyệt</span>
            <span class="value">${monthSummary.pendingHours}h</span>
            <div class="muted" style="font-size:.8em;margin-top:4px">chưa tính vào lương</div>
        </div>
        <div class="card stat">
            <span class="label">Số ca đã làm</span>
            <span class="value">${monthSummary.shiftsWorked}</span>
            <div class="muted" style="font-size:.8em;margin-top:4px">trung bình ${monthSummary.avgHoursPerShift}h mỗi ca</div>
        </div>
    </div>

    <c:if test="${monthSummary.openCount > 0}">
        <div class="alert alert-warn">
            Có ${monthSummary.openCount} ca bạn quên bấm Tan ca — những ca này chưa được tính giờ.
            Báo quản lý để chỉnh giúp.
        </div>
    </c:if>

    <c:choose>
        <c:when test="${not hasMonthRows}">
            <div class="card empty-state"><div class="icon">∅</div><p>Tháng này bạn chưa được xếp ca nào.</p></div>
        </c:when>
        <c:otherwise>
            <%-- Tìm kiếm, lọc và phân trang đều chạy ở server: form GET tự gửi lại khi gõ/đổi lựa chọn. --%>
            <form id="shiftHistoryFilters" class="table-toolbar" action="${ctx}/barista/shift" method="get">
                <input type="hidden" name="month" value="${month}">
                <input type="hidden" name="page" value="1">
                <div class="form-group table-search">
                    <label for="shiftHistorySearch">Tìm kiếm</label>
                    <input id="shiftHistorySearch" class="form-control" type="search" name="q" value="${fn:escapeXml(historyQuery)}"
                           placeholder="Tìm theo tên ca hoặc ngày (vd 19/07)" autocomplete="off">
                </div>
                <div class="form-group">
                    <label for="shiftStateFilter">Trạng thái</label>
                    <select id="shiftStateFilter" name="state" class="form-control tt-filter">
                        <option value="">Tất cả</option>
                        <option value="APPROVED" ${historyState == 'APPROVED' ? 'selected' : ''}>Đã duyệt</option>
                        <option value="PENDING" ${historyState == 'PENDING' ? 'selected' : ''}>Chờ duyệt</option>
                        <option value="REJECTED" ${historyState == 'REJECTED' ? 'selected' : ''}>Bị từ chối</option>
                        <option value="OPEN" ${historyState == 'OPEN' ? 'selected' : ''}>Chưa tan ca</option>
                        <option value="ABSENT" ${historyState == 'ABSENT' ? 'selected' : ''}>Vắng</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="shiftHistoryPageSize">Hiển thị</label>
                    <select id="shiftHistoryPageSize" name="pageSize" class="form-control tt-size">
                        <option value="5" ${historyPage.pageSize == 5 ? 'selected' : ''}>5</option>
                        <option value="10" ${historyPage.pageSize == 10 ? 'selected' : ''}>10</option>
                        <option value="20" ${historyPage.pageSize == 20 ? 'selected' : ''}>20</option>
                        <option value="50" ${historyPage.pageSize == 50 ? 'selected' : ''}>50</option>
                    </select>
                </div>
            </form>

            <div class="table-scroll">
                <table class="table" style="min-width:640px">
                    <thead><tr>
                        <th style="width:80px">Ngày</th>
                        <th>Ca</th>
                        <th style="width:80px">Vào</th>
                        <th style="width:80px">Tan</th>
                        <th style="width:80px">Giờ</th>
                        <th style="width:120px">Trạng thái</th>
                    </tr></thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty historyPage.rows}">
                                <tr class="tt-empty"><td colspan="6">Không tìm thấy ca làm phù hợp.</td></tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="r" items="${historyPage.rows}">
                                    <tr>
                                        <td>${view.localDate(r.workDate)}</td>
                                <td>${fn:escapeXml(r.shiftName)} <span class="muted">${view.timeRange(r.shiftStart, r.shiftEnd)}</span></td>
                                        <td>${view.timeUtc(r.checkInAt)}</td>
                                        <td>${view.timeUtc(r.checkOutAt)}</td>
                                        <td>${view.attendanceHours(r)}</td>
                                        <td><span class="badge ${view.attendanceBadge(r)}">${view.attendanceState(r)}</span></td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>

            <div class="table-tools-foot">
                <span class="tt-summary" aria-live="polite">
                    <c:choose>
                        <c:when test="${historyPage.total == 0}">0 ca</c:when>
                        <c:otherwise>${historyPage.startRow}-${historyPage.endRow} / ${historyPage.total} ca · trang ${historyPage.page}/${historyPage.totalPages}</c:otherwise>
                    </c:choose>
                </span>
                <c:if test="${historyPage.totalPages > 1}">
                    <div class="pagination" aria-label="Phân trang lịch sử ca làm">
                        <c:url var="firstShiftPageUrl" value="/barista/shift">
                            <c:param name="month" value="${month}" /><c:param name="q" value="${historyQuery}" />
                            <c:param name="state" value="${historyState}" /><c:param name="pageSize" value="${historyPage.pageSize}" />
                            <c:param name="page" value="1" />
                        </c:url>
                        <c:url var="previousShiftPageUrl" value="/barista/shift">
                            <c:param name="month" value="${month}" /><c:param name="q" value="${historyQuery}" />
                            <c:param name="state" value="${historyState}" /><c:param name="pageSize" value="${historyPage.pageSize}" />
                            <c:param name="page" value="${historyPage.page - 1}" />
                        </c:url>
                        <a class="page" href="${firstShiftPageUrl}" aria-disabled="${not historyPage.hasPrevious}" title="Trang đầu">«</a>
                        <a class="page" href="${previousShiftPageUrl}" aria-disabled="${not historyPage.hasPrevious}" title="Trang trước">‹</a>
                        <c:forEach var="pageNumber" items="${historyPage.visiblePages}">
                            <c:url var="shiftPageUrl" value="/barista/shift">
                                <c:param name="month" value="${month}" /><c:param name="q" value="${historyQuery}" />
                                <c:param name="state" value="${historyState}" /><c:param name="pageSize" value="${historyPage.pageSize}" />
                                <c:param name="page" value="${pageNumber}" />
                            </c:url>
                            <a class="page ${pageNumber == historyPage.page ? 'is-active' : ''}" href="${shiftPageUrl}" aria-current="${pageNumber == historyPage.page ? 'page' : 'false'}">${pageNumber}</a>
                        </c:forEach>
                        <c:url var="nextShiftPageUrl" value="/barista/shift">
                            <c:param name="month" value="${month}" /><c:param name="q" value="${historyQuery}" />
                            <c:param name="state" value="${historyState}" /><c:param name="pageSize" value="${historyPage.pageSize}" />
                            <c:param name="page" value="${historyPage.page + 1}" />
                        </c:url>
                        <c:url var="lastShiftPageUrl" value="/barista/shift">
                            <c:param name="month" value="${month}" /><c:param name="q" value="${historyQuery}" />
                            <c:param name="state" value="${historyState}" /><c:param name="pageSize" value="${historyPage.pageSize}" />
                            <c:param name="page" value="${historyPage.totalPages}" />
                        </c:url>
                        <a class="page" href="${nextShiftPageUrl}" aria-disabled="${not historyPage.hasNext}" title="Trang sau">›</a>
                        <a class="page" href="${lastShiftPageUrl}" aria-disabled="${not historyPage.hasNext}" title="Trang cuối">»</a>
                    </div>
                </c:if>
            </div>
        </c:otherwise>
    </c:choose>
</c:if>

<script>
(function(){
  var form = document.getElementById('shiftHistoryFilters');
  if (!form) return;
  var search = document.getElementById('shiftHistorySearch');
  var page = form.querySelector('input[name="page"]');
  var timer;

  function submitFromFirstPage(){
    if (page) page.value = '1';
    if (form.requestSubmit) form.requestSubmit();
    else form.submit();
  }

  // Lọc chạy ở server nên mỗi lần gõ là một lần tải lại trang; ghi cờ để trả lại con trỏ cho ô tìm kiếm.
  var FOCUS_KEY = 'shiftHistorySearchFocus';
  function rememberFocus(){
    try { if (window.sessionStorage) sessionStorage.setItem(FOCUS_KEY, '1'); } catch (e) { /* storage bị chặn */ }
  }

  if (search) {
    try {
      if (window.sessionStorage && sessionStorage.getItem(FOCUS_KEY)) {
        sessionStorage.removeItem(FOCUS_KEY);
        search.focus();
        search.setSelectionRange(search.value.length, search.value.length);
      }
    } catch (e) { /* storage bị chặn thì bỏ qua, tìm kiếm vẫn chạy bình thường */ }

    search.addEventListener('input', function(){
      window.clearTimeout(timer);
      timer = window.setTimeout(function(){ rememberFocus(); submitFromFirstPage(); }, 350);
    });
  }

  Array.prototype.forEach.call(form.querySelectorAll('select'), function(control){
    control.addEventListener('change', submitFromFirstPage);
  });
})();
</script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
