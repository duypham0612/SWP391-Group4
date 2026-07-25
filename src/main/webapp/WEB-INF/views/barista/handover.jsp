<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header"><div><div class="eyebrow">Pha chế</div><h1>Bàn giao ca</h1><p>Giao việc còn lại cho đúng ca tiếp theo và theo dõi đến khi hoàn tất.</p></div></div>
<c:if test="${not empty sessionScope.flashError}"><div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" /></c:if>
<c:if test="${not empty sessionScope.flashOk}"><div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" /></c:if>

<%-- Ca quá hạn chấm công vẫn lập được bàn giao, lúc đó banner "Ngoài ca — chỉ xem" nói ngược với
     form đang mở ngay bên dưới. Trường hợp đó để thẻ tạo bàn giao tự giải thích. --%>
<c:if test="${not canCreateHandover}"><jsp:include page="../layout/_baristaShiftBanner.jsp" /></c:if>

<%-- Ba con số mở đầu: việc của tôi trước, tình hình chi nhánh sau. --%>
<section class="card-grid">
    <div class="card stat">
        <span class="label">Chờ bạn xác nhận</span>
        <span class="value">${summary.pendingForMe}</span>
        <small class="muted">bàn giao gửi cho bạn chưa tiếp nhận</small>
    </div>
    <div class="card stat">
        <span class="label">Việc bạn cần xử lý</span>
        <span class="value">${summary.openTasksForMe}</span>
        <small class="muted">đầu việc đã nhận nhưng chưa xong</small>
    </div>
    <div class="card stat">
        <span class="label">Chi nhánh chờ ca nhận</span>
        <span class="value">${summary.waitingReceipt}</span>
        <small class="muted">bàn giao chưa ai xác nhận</small>
    </div>
</section>

<c:if test="${expiredPrepBatchCount > 0}"><div class="alert alert-warn">Có ${expiredPrepBatchCount} mẻ pha sẵn đã quá hạn. <a href="${ctx}/barista/prep">Kiểm tra Pha sẵn</a> trước khi bàn giao.</div></c:if>
<c:if test="${summary.pendingForMe > 0}"><div class="alert alert-warn"><strong>Bạn có ${summary.pendingForMe} bàn giao chưa xác nhận.</strong> Đọc nội dung và bấm “Đã nhận bàn giao” bên dưới trước khi xử lý việc.</div></c:if>

<%-- Đến từ nút tan ca: ca chưa bàn giao nên chưa được tan, nói rõ việc cần làm để tan ca. --%>
<c:if test="${handoverRequired}"><div class="alert alert-warn"><strong>Ca của bạn chưa được bàn giao nên chưa thể tan ca.</strong> Ghi việc cần bàn giao cho ca sau rồi bấm “Lưu bàn giao &amp; Tan ca”.</div></c:if>

<%-- Bàn giao mồ côi hoặc quá hạn chưa ai xác nhận: ai đang trực thì có thể nhận thay. Link bỏ
     bộ lọc vì bàn giao mồ côi không nằm trong phạm vi "Gửi cho tôi". --%>
<c:if test="${summary.claimable > 0}"><div class="alert alert-warn"><strong>Có ${summary.claimable} bàn giao ca trước để lại chưa ai tiếp nhận.</strong> <a href="${ctx}/barista/handover?pageSize=${handoverPage.pageSize}">Xem toàn bộ bàn giao chi nhánh</a> rồi bấm “Tiếp nhận” để nhận việc về mình.</div></c:if>

<c:choose>
  <c:when test="${canCreateHandover}">
    <div class="card form-card" style="margin-bottom:var(--s5)">
      <h3 style="margin-top:0">Tạo bàn giao cho ca sau</h3>
      <c:choose>
        <c:when test="${not empty receiverPreview}">
          <p class="alert alert-info">Người nhận: <strong><c:out value="${receiverPreview.label}" /></strong><c:if test="${receiverPreview.managerFallback}"> · chưa có ca barista kế tiếp nên quản lý sẽ nhận dự phòng</c:if><c:if test="${receiverPreview.unassigned}"> · chưa xếp ca sau và chi nhánh chưa có quản lý, bàn giao vẫn được lưu để người vào ca sau tiếp nhận</c:if></p>
        </c:when>
        <c:otherwise><p class="alert alert-error"><c:out value="${receiverPreviewError}" /></p></c:otherwise>
      </c:choose>
      <c:if test="${not empty receiverPreview}">
      <form action="${ctx}/barista/handover" method="post" id="handoverCreateForm">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">

        <%-- Việc ca trước giao mà chưa xong sẽ mất dấu nếu barista không tự gõ lại; tick là chuyển tiếp
             nguyên văn sang bàn giao mới (bản ghi cũ vẫn giữ nguyên để truy vết). --%>
        <c:if test="${not empty carryOverTasks}">
          <div class="form-group">
            <label>Việc còn tồn của bạn <span class="muted">(tick để chuyển tiếp cho ca sau)</span></label>
            <div class="handover-carry">
              <c:forEach var="ct" items="${carryOverTasks}">
                <label class="handover-carry__item">
                  <input type="checkbox" name="task" value="${fn:escapeXml(ct.content)}">
                  <span><c:out value="${ct.content}" /> <span class="badge ${ct.statusBadge}">${ct.statusLabel}</span><br><small class="muted">Từ bàn giao của <c:out value="${ct.sourceLabel}" /></small></span>
                </label>
              </c:forEach>
            </div>
          </div>
        </c:if>

        <%-- Gợi ý lấy thẳng từ hàng chờ quầy: lúc tan ca thì "còn mấy ly chưa pha" là thứ dễ quên
             nhất, mà quên thì ca sau nhận quầy không biết đang nợ gì. Tick sẵn vì đây là hiện trạng
             có thật, barista chỉ cần bỏ tick nếu thấy không cần bàn giao. --%>
        <c:if test="${not empty brewTasks}">
          <div class="form-group">
            <label>Hiện trạng quầy pha chế <span class="muted">(bỏ tick nếu không cần bàn giao)</span></label>
            <div class="handover-carry">
              <c:forEach var="bt" items="${brewTasks}">
                <label class="handover-carry__item">
                  <input type="checkbox" name="task" value="${fn:escapeXml(bt)}" checked>
                  <span><c:out value="${bt}" /></span>
                </label>
              </c:forEach>
            </div>
          </div>
        </c:if>

        <div class="form-group">
          <label>Việc cần bàn giao</label>
          <div id="handoverTasks"><input class="form-control" name="task" maxlength="500" placeholder="VD: Kiểm tra máy xay #2 kêu lạ"></div>
          <button type="button" class="btn btn-ghost btn-sm" id="addHandoverTask" style="margin-top:8px">+ Thêm việc</button>
          <div class="muted" style="margin-top:6px">Tối đa 10 việc (tính cả việc tồn đã tick). Mỗi việc sẽ được ca nhận theo dõi riêng.</div>
        </div>
        <div class="form-group"><label for="handoverNote">Ghi chú chung <span class="muted">(không bắt buộc)</span></label><textarea id="handoverNote" name="note" class="form-control" rows="3" maxlength="1000" placeholder="Bối cảnh chung cho ca nhận..."></textarea></div>
        <%-- Quá hạn chấm công thì chỉ còn lưu bàn giao: giờ tan ca phải do Quản lý chốt, nhưng việc
             tồn thì vẫn phải sang được ca sau ngay bây giờ. --%>
        <c:if test="${not canClockOutHandover}"><div class="alert alert-warn">Ca của bạn đã quá hạn bấm tan ca. Bạn vẫn lưu được bàn giao cho ca sau; giờ tan ca nhờ Quản lý chốt giúp.</div></c:if>
        <div style="display:flex;gap:8px;flex-wrap:wrap"><button type="submit" name="action" value="create" class="btn ${canClockOutHandover ? 'btn-ghost' : 'btn-primary'}">Lưu bàn giao</button><c:if test="${canClockOutHandover}"><button type="submit" name="action" value="createAndClockOut" class="btn btn-primary">Lưu bàn giao &amp; Tan ca</button></c:if></div>
      </form></c:if>
    </div>
  </c:when>
  <c:otherwise><div class="alert alert-info">Bạn cần <a href="${ctx}/barista/shift">vào ca</a> trước khi tạo bàn giao. Bạn vẫn có thể xem các bàn giao được gửi cho mình.</div></c:otherwise>
</c:choose>

<h3 class="section-title">Bàn giao cần xử lý và lịch sử</h3>
<form id="handoverFilters" class="table-toolbar" action="${ctx}/barista/handover" method="get">
    <input type="hidden" name="page" value="1">
    <div class="form-group table-search">
        <label for="handoverSearch">Tìm kiếm</label>
        <input id="handoverSearch" class="form-control" type="search" name="q" value="${fn:escapeXml(filterQuery)}"
               placeholder="Tìm nội dung việc, ghi chú hoặc người gửi" autocomplete="off">
    </div>
    <div class="form-group">
        <label for="handoverScope">Phạm vi</label>
        <select id="handoverScope" name="scope" class="form-control tt-filter">
            <option value="">Cả chi nhánh</option>
            <option value="MINE" ${filterScope == 'MINE' ? 'selected' : ''}>Gửi cho tôi</option>
            <option value="SENT" ${filterScope == 'SENT' ? 'selected' : ''}>Tôi đã gửi</option>
        </select>
    </div>
    <div class="form-group">
        <label for="handoverState">Trạng thái</label>
        <%-- "state" chứ không phải "status": bảng việc bên dưới đã dùng "status" cho từng đầu việc. --%>
        <select id="handoverState" name="state" class="form-control tt-filter">
            <option value="">Tất cả</option>
            <option value="WAITING_RECEIPT" ${filterStatus == 'WAITING_RECEIPT' ? 'selected' : ''}>Chờ ca nhận</option>
            <option value="IN_PROGRESS" ${filterStatus == 'IN_PROGRESS' ? 'selected' : ''}>Đang xử lý</option>
            <option value="COMPLETED" ${filterStatus == 'COMPLETED' ? 'selected' : ''}>Hoàn tất</option>
        </select>
    </div>
    <div class="form-group">
        <label for="handoverPageSize">Hiển thị</label>
        <select id="handoverPageSize" name="pageSize" class="form-control tt-size">
            <option value="5" ${handoverPage.pageSize == 5 ? 'selected' : ''}>5</option>
            <option value="10" ${handoverPage.pageSize == 10 ? 'selected' : ''}>10</option>
            <option value="20" ${handoverPage.pageSize == 20 ? 'selected' : ''}>20</option>
            <option value="50" ${handoverPage.pageSize == 50 ? 'selected' : ''}>50</option>
        </select>
    </div>
</form>

<%-- Bộ lọc + trang hiện tại đi kèm mọi POST để thao tác xong quay lại đúng chỗ đang xem. --%>
<c:set var="keepFilters">
    <input type="hidden" name="q" value="${fn:escapeXml(filterQuery)}">
    <input type="hidden" name="scope" value="${filterScope}">
    <input type="hidden" name="state" value="${filterStatus}">
    <input type="hidden" name="pageSize" value="${handoverPage.pageSize}">
    <input type="hidden" name="page" value="${handoverPage.page}">
</c:set>

<c:choose><c:when test="${empty handoverPage.items}"><div class="card empty-state"><div class="icon">∅</div><p>Không có bàn giao nào khớp bộ lọc.</p></div></c:when><c:otherwise>
  <c:forEach var="h" items="${handoverPage.items}">
    <article id="h${h.shiftHandoverId}" class="card handover-card ${h.canAcknowledge or h.claimable ? 'is-actionable' : ''}">
      <div class="handover-card__head">
        <div>
          <h3>Bàn giao bởi <c:out value="${h.createdByName}" /><c:if test="${h.currentUserCreator}"> <span class="badge badge-served">Bạn gửi</span></c:if></h3>
          <div class="muted">${h.createdDisplay} · ${h.ageDisplay}<c:if test="${not empty h.sourceShiftLabel}"> · <c:out value="${h.sourceShiftLabel}" /></c:if></div>
        </div>
        <span class="badge ${h.overallStatusBadge}">${h.overallStatusLabel}</span>
      </div>

      <c:if test="${h.taskCount > 0}">
        <div class="handover-progress">
          <div class="handover-progress__bar"><span style="width:${h.progressPercent}%"></span></div>
          <span class="handover-progress__text">${h.doneTaskCount}/${h.taskCount} việc đã xong<c:if test="${h.openTaskCount > 0}"> · còn ${h.openTaskCount}</c:if></span>
        </div>
      </c:if>

      <c:if test="${not empty h.note}"><p style="margin:var(--s3) 0"><strong>Ghi chú:</strong> <c:out value="${h.note}" /></p></c:if>

      <c:if test="${not empty h.recipients}">
        <div class="muted" style="margin:var(--s3) 0">
          <strong>Người nhận (${h.acknowledgedCount}/${h.recipientCount} đã nhận):</strong>
          <c:forEach var="r" items="${h.recipients}" varStatus="loop"><c:out value="${r.recipientName}" /> <span class="badge ${r.acknowledged ? 'badge-ready' : 'badge-waiting'}">${r.acknowledged ? 'Đã nhận' : 'Chưa nhận'}</span><c:if test="${not empty r.shiftLabel}"> · <c:out value="${r.shiftLabel}" /></c:if><c:if test="${not loop.last}">; </c:if></c:forEach>
        </div>
      </c:if>

      <%-- Bàn giao mồ côi hoặc quá hạn chưa ai xác nhận: người đang trực tự đứng ra nhận.
           Nhận là gánh việc luôn, nên nói rõ trước khi bấm thay vì chỉ hiện một cái nút. --%>
      <c:if test="${h.claimable}">
        <div class="${onShift ? '' : 'is-viewonly'}" style="margin:var(--s3) 0">
          <div class="muted" style="margin-bottom:6px">Bàn giao này chưa ai xác nhận và đang cần người trực nhận thay. Tiếp nhận để các việc bên dưới về danh sách của bạn.</div>
          <form action="${ctx}/barista/handover" method="post"><input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="claim"><input type="hidden" name="handoverId" value="${h.shiftHandoverId}">${keepFilters}<button class="btn btn-primary">Tiếp nhận bàn giao này</button></form>
        </div>
      </c:if>

      <%-- Ngoài ca thì server chặn ghi; khoá luôn ở đây để không mời bấm một nút chắc chắn báo lỗi. --%>
      <c:if test="${h.canAcknowledge}"><div class="${onShift ? '' : 'is-viewonly'}" style="margin:var(--s3) 0"><form action="${ctx}/barista/handover" method="post"><input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="acknowledge"><input type="hidden" name="handoverId" value="${h.shiftHandoverId}">${keepFilters}<button class="btn btn-primary">Đã nhận bàn giao</button></form></div></c:if>

      <c:if test="${not empty h.tasks}"><div class="table-scroll"><table class="table" style="min-width:620px"><thead><tr><th>Việc cần xử lý</th><th style="width:130px">Trạng thái</th><th style="width:180px">Cập nhật</th><c:if test="${h.canUpdateTasks}"><th style="width:150px">Đổi trạng thái</th></c:if></tr></thead><tbody><c:forEach var="t" items="${h.tasks}"><tr class="${t.status == 'DONE' ? 'row-muted' : ''}"><td><c:out value="${t.content}" /></td><td><span class="badge ${t.statusBadge}">${t.statusLabel}</span></td><td class="muted"><c:out value="${empty t.updatedByName ? '—' : t.updatedByName}" /><c:if test="${not empty t.updatedAt}"> · ${t.updatedDisplay}</c:if></td><c:if test="${h.canUpdateTasks}"><td><div class="${onShift ? '' : 'is-viewonly'}"><form action="${ctx}/barista/handover" method="post"><input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="updateTask"><input type="hidden" name="handoverId" value="${h.shiftHandoverId}"><input type="hidden" name="taskId" value="${t.shiftHandoverTaskId}">${keepFilters}<select name="status" class="form-control" onchange="this.form.submit()"><option value="NEW" ${t.status == 'NEW' ? 'selected' : ''}>Mới</option><option value="IN_PROGRESS" ${t.status == 'IN_PROGRESS' ? 'selected' : ''}>Đang xử lý</option><option value="DONE" ${t.status == 'DONE' ? 'selected' : ''}>Đã xử lý</option></select></form></div></td></c:if></tr></c:forEach></tbody></table></div></c:if>
    </article>
  </c:forEach>
</c:otherwise></c:choose>

<div class="table-tools-foot">
    <span class="tt-summary" aria-live="polite">
        <c:choose>
            <c:when test="${handoverPage.total == 0}">0 bàn giao</c:when>
            <c:otherwise>${handoverPage.startRow}-${handoverPage.endRow} / ${handoverPage.total} bàn giao · trang ${handoverPage.page}/${handoverPage.totalPages}</c:otherwise>
        </c:choose>
    </span>
    <c:if test="${handoverPage.totalPages > 1}">
        <div class="pagination" aria-label="Phân trang bàn giao">
            <c:url var="firstPageUrl" value="/barista/handover"><c:param name="q" value="${filterQuery}" /><c:param name="scope" value="${filterScope}" /><c:param name="state" value="${filterStatus}" /><c:param name="pageSize" value="${handoverPage.pageSize}" /><c:param name="page" value="1" /></c:url>
            <c:url var="prevPageUrl" value="/barista/handover"><c:param name="q" value="${filterQuery}" /><c:param name="scope" value="${filterScope}" /><c:param name="state" value="${filterStatus}" /><c:param name="pageSize" value="${handoverPage.pageSize}" /><c:param name="page" value="${handoverPage.page - 1}" /></c:url>
            <a class="page" href="${firstPageUrl}" aria-disabled="${not handoverPage.hasPrevious}" title="Trang đầu">«</a>
            <a class="page" href="${prevPageUrl}" aria-disabled="${not handoverPage.hasPrevious}" title="Trang trước">‹</a>
            <c:forEach var="pageNumber" items="${handoverPage.visiblePages}">
                <c:url var="pageUrl" value="/barista/handover"><c:param name="q" value="${filterQuery}" /><c:param name="scope" value="${filterScope}" /><c:param name="state" value="${filterStatus}" /><c:param name="pageSize" value="${handoverPage.pageSize}" /><c:param name="page" value="${pageNumber}" /></c:url>
                <a class="page ${pageNumber == handoverPage.page ? 'is-active' : ''}" href="${pageUrl}" aria-current="${pageNumber == handoverPage.page ? 'page' : 'false'}">${pageNumber}</a>
            </c:forEach>
            <c:url var="nextPageUrl" value="/barista/handover"><c:param name="q" value="${filterQuery}" /><c:param name="scope" value="${filterScope}" /><c:param name="state" value="${filterStatus}" /><c:param name="pageSize" value="${handoverPage.pageSize}" /><c:param name="page" value="${handoverPage.page + 1}" /></c:url>
            <c:url var="lastPageUrl" value="/barista/handover"><c:param name="q" value="${filterQuery}" /><c:param name="scope" value="${filterScope}" /><c:param name="state" value="${filterStatus}" /><c:param name="pageSize" value="${handoverPage.pageSize}" /><c:param name="page" value="${handoverPage.totalPages}" /></c:url>
            <a class="page" href="${nextPageUrl}" aria-disabled="${not handoverPage.hasNext}" title="Trang sau">›</a>
            <a class="page" href="${lastPageUrl}" aria-disabled="${not handoverPage.hasNext}" title="Trang cuối">»</a>
        </div>
    </c:if>
</div>

<script>
(function(){
  var add = document.getElementById('addHandoverTask'), list = document.getElementById('handoverTasks');
  var form = document.getElementById('handoverCreateForm');
  if (!form) return;

  // Trần 10 việc tính cả việc tồn đã tick — server từ chối phần vượt, chặn sớm ở đây cho đỡ mất công gõ.
  function selectedCount(){
    var count = 0;
    Array.prototype.forEach.call(form.querySelectorAll('[name="task"]'), function(el){
      if (el.type === 'checkbox') { if (el.checked) count++; }
      else if (el.value.trim()) count++;
    });
    return count;
  }

  if (add && list) add.addEventListener('click', function(){
    if (list.querySelectorAll('input').length >= 10) return;
    var input = document.createElement('input');
    input.className = 'form-control'; input.name = 'task'; input.maxLength = 500;
    input.placeholder = 'Việc cần bàn giao'; input.style.marginTop = '8px';
    list.appendChild(input); input.focus();
  });

  form.addEventListener('submit', function(e){
    var count = selectedCount();
    if (count === 0) { e.preventDefault(); alert('Cần có ít nhất một việc cần bàn giao.'); return; }
    if (count > 10) { e.preventDefault(); alert('Tối đa 10 việc trong một bàn giao.'); }
  });
})();
</script>

<script>
(function(){
  var form = document.getElementById('handoverFilters');
  if (!form) return;
  var search = document.getElementById('handoverSearch');
  var page = form.querySelector('input[name="page"]');
  var timer;

  function submitFromFirstPage(){
    if (page) page.value = '1';
    if (form.requestSubmit) form.requestSubmit(); else form.submit();
  }

  // Lọc chạy ở server nên mỗi lần gõ là một lần tải lại trang; ghi cờ để trả lại con trỏ cho ô tìm kiếm.
  var FOCUS_KEY = 'handoverSearchFocus';
  if (search) {
    try {
      if (window.sessionStorage && sessionStorage.getItem(FOCUS_KEY)) {
        sessionStorage.removeItem(FOCUS_KEY);
        search.focus(); search.setSelectionRange(search.value.length, search.value.length);
      }
    } catch (e) { /* storage bị chặn thì bỏ qua, tìm kiếm vẫn chạy bình thường */ }

    search.addEventListener('input', function(){
      window.clearTimeout(timer);
      timer = window.setTimeout(function(){
        try { if (window.sessionStorage) sessionStorage.setItem(FOCUS_KEY, '1'); } catch (e) { /* storage bị chặn */ }
        submitFromFirstPage();
      }, 350);
    });
  }

  Array.prototype.forEach.call(form.querySelectorAll('select'), function(control){
    control.addEventListener('change', submitFromFirstPage);
  });
})();
</script>
<jsp:include page="../layout/footer.jsp" />
