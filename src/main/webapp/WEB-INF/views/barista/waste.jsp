<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Pha chế</div>
        <h1>Hao hụt nguyên liệu</h1>
        <p>Ghi nguyên liệu bị đổ, rơi, hỏng hoặc hết hạn để trừ khỏi sổ kho — theo dõi & cắt giảm thất thoát.</p>
    </div>
    <div class="waste-scope">
        <strong>${view.scopeLabel(scope.kind)}</strong>
        <span>${view.scopeWindow(scope.kind, scope.fromUtc, scope.toUtc)}</span>
    </div>
</div>

<c:if test="${not empty requestScope.flashError}">
    <div class="alert alert-error">${requestScope.flashError}</div>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
</c:if>

<jsp:include page="/WEB-INF/views/layout/_baristaShiftBanner.jsp" />

<div class="${onShift ? '' : 'is-viewonly'}">
<%-- Bốn ô dưới đây tính cho trọn phạm vi đang xem, không đổi theo bộ lọc/phân trang của nhật ký. --%>
<section class="waste-summary">
    <div class="card stat">
        <span class="label">${view.scopeLabel(scope.kind)}</span>
        <span class="value">${summary.ingredientWasteCount}</span>
        <small>dòng hao hụt hiệu lực</small>
    </div>
    <div class="card stat">
        <span class="label">Đổ/rơi</span>
        <span class="value">${summary.spillCount}</span>
        <small>dòng</small>
    </div>
    <div class="card stat">
        <span class="label">Hỏng / hết hạn</span>
        <span class="value">${summary.expiredCount}</span>
        <small>dòng</small>
    </div>
    <div class="card stat">
        <span class="label">Khác</span>
        <span class="value">${summary.otherCount}</span>
        <small>dòng</small>
    </div>
</section>

<section class="card waste-card">
    <div class="waste-card__head">
        <div>
            <h3>Ghi hao hụt</h3>
            <p>Ghi nhanh nhiều nguyên liệu trong một lần. Món pha lỗi phải làm lại thì bấm ngay trên màn KDS — kho tự trừ, không ghi tay ở đây.</p>
        </div>
    </div>
    <form id="ingredientWasteForm"
          action="${ctx}/barista/waste"
          method="post"
          onsubmit="return confirm('Xác nhận ghi hao hụt? Nếu vượt tồn hệ thống, Quản lý sẽ nhận ngoại lệ để đối soát.');">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="clientRequestId" class="js-waste-request-id" value="${wasteClientRequestId}">
        <input type="hidden" name="action" value="createIngredientWaste">
        <%-- Bộ lọc + trang nhật ký đang xem: ghi xong redirect (PRG) quay lại đúng chỗ, lỗi validate cũng giữ nguyên.
        --%>
        <input type="hidden" name="q" value="${fn:escapeXml(wasteLogQuery)}">
        <input type="hidden" name="logType" value="${wasteLogWasteType}">
        <input type="hidden" name="status" value="${wasteLogStatus}">
        <input type="hidden" name="pageSize" value="${wasteLogPage.pageSize}">
        <input type="hidden" name="page" value="${wasteLogPage.page}">
        <div id="wasteRows" class="waste-rows">
            <c:forEach var="row" items="${submittedWasteRows}">
                <div class="waste-row">
                    <div class="form-group waste-row__ingredient">
                        <label>Nguyên liệu</label>
                        <select name="ingredientId" class="form-control">
                            <option value="">-- Chọn --</option>
                            <c:forEach var="i" items="${ingredients}">
                                <option value="${i.ingredientId}" ${row.ingredientId == i.ingredientId ? 'selected' : ''}>${i.name} (${i.unit})</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="form-group waste-row__qty">
                        <label>Số lượng</label>
                        <input type="number"
                               name="quantity"
                               class="form-control"
                               min="0.001"
                               max="999999999.999"
                               step="0.001"
                               value="${fn:escapeXml(row.quantity)}">
                    </div>
                    <div class="form-group waste-row__type">
                        <label>Loại</label>
                        <select name="wasteType" class="form-control waste-type">
                            <option value="SPILL" ${row.wasteType == 'SPILL' ? 'selected' : ''}>Đổ/rơi</option>
                            <option value="EXPIRED" ${row.wasteType == 'EXPIRED' ? 'selected' : ''}>Hỏng / hết hạn</option>
                            <option value="OTHER" ${row.wasteType == 'OTHER' ? 'selected' : ''}>Khác</option>
                        </select>
                    </div>
                    <div class="form-group waste-row__preset">
                        <label>Lý do</label>
                        <select name="reasonPreset" class="form-control waste-reason-preset" required>
                            <option value="">-- Gợi ý --</option>
                            <option data-type="SPILL"
                                    value="Đổ khi pha"
                                    ${row.reasonPreset == 'Đổ khi pha' ? 'selected' : ''}>Đổ khi pha</option>
                            <option data-type="SPILL"
                                    value="Rơi khi thao tác"
                                    ${row.reasonPreset == 'Rơi khi thao tác' ? 'selected' : ''}>Rơi khi thao tác</option>
                            <option data-type="SPILL"
                                    value="Sai định lượng"
                                    ${row.reasonPreset == 'Sai định lượng' ? 'selected' : ''}>Sai định lượng</option>
                            <option data-type="EXPIRED"
                                    value="Hết hạn"
                                    ${row.reasonPreset == 'Hết hạn' ? 'selected' : ''}>Hết hạn</option>
                            <option data-type="EXPIRED"
                                    value="Nguyên liệu hỏng"
                                    ${row.reasonPreset == 'Nguyên liệu hỏng' ? 'selected' : ''}>Nguyên liệu hỏng</option>
                            <option data-type="EXPIRED"
                                    value="Bảo quản lỗi"
                                    ${row.reasonPreset == 'Bảo quản lỗi' ? 'selected' : ''}>Bảo quản lỗi</option>
                            <option data-type="EXPIRED"
                                    value="Quá thời gian mở nắp"
                                    ${row.reasonPreset == 'Quá thời gian mở nắp' ? 'selected' : ''}>Quá thời gian mở nắp</option>
                            <option data-type="OTHER"
                                    value="Mẫu thử/QC"
                                    ${row.reasonPreset == 'Mẫu thử/QC' ? 'selected' : ''}>Mẫu thử/QC</option>
                            <option data-type="OTHER" value="Khác" ${row.reasonPreset == 'Khác' ? 'selected' : ''}>Khác</option>
                        </select>
                    </div>
                    <div class="form-group waste-row__note">
                        <label>Nhập thêm</label>
                        <input type="text"
                               name="reasonDetail"
                               class="form-control waste-reason-detail"
                               maxlength="255"
                               value="${fn:escapeXml(row.reasonDetail)}">
                    </div>
                    <button type="button" class="waste-row__remove" title="Xoá dòng">×</button>
                </div>
            </c:forEach>
        </div>
        <div class="waste-form-actions">
            <button type="button" id="addWasteRow" class="btn btn-ghost">Thêm dòng</button>
            <button type="submit" class="btn btn-primary">Ghi hao hụt</button>
        </div>
    </form>
</section>

<c:if test="${not empty editLog}">
    <div id="editWaste" class="card waste-edit-card">
        <div>
            <h3>Sửa bản ghi hao hụt</h3>
            <p>${editLog.ingredientName} · ${view.timeDateUtc(editLog.loggedAt)}</p>
        </div>
        <form action="${ctx}/barista/waste" method="post" class="waste-edit-form">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="wasteEntryId" value="${editLog.wasteEntryId}">
            <input type="hidden" name="q" value="${fn:escapeXml(wasteLogQuery)}">
            <input type="hidden" name="logType" value="${wasteLogWasteType}">
            <input type="hidden" name="status" value="${wasteLogStatus}">
            <input type="hidden" name="pageSize" value="${wasteLogPage.pageSize}">
            <input type="hidden" name="page" value="${wasteLogPage.page}">
            <c:set var="editQty" value="${empty requestScope.editQuantity ? view.plain(editLog.quantity) : requestScope.editQuantity}" />
            <c:set var="editType" value="${empty requestScope.editWasteType ? editLog.wasteType : requestScope.editWasteType}" />
            <c:set var="editReasonValue" value="${empty requestScope.editReason ? editLog.reason : requestScope.editReason}" />
            <div class="form-group">
                <label>Số lượng</label>
                <input type="number"
                       name="quantity"
                       class="form-control"
                       min="0.001"
                       max="999999999.999"
                       step="0.001"
                       value="${fn:escapeXml(editQty)}"
                       required>
            </div>
            <div class="form-group">
                <label>Loại</label>
                <select name="wasteType" class="form-control">
                    <option value="SPILL" ${editType == 'SPILL' ? 'selected' : ''}>Đổ/rơi</option>
                    <option value="EXPIRED" ${editType == 'EXPIRED' ? 'selected' : ''}>Hỏng / hết hạn</option>
                    <option value="OTHER" ${editType == 'OTHER' ? 'selected' : ''}>Khác</option>
                </select>
            </div>
            <div class="form-group waste-edit-form__reason">
                <label>Lý do</label>
                <input type="text"
                       name="reason"
                       class="form-control"
                       maxlength="255"
                       value="${fn:escapeXml(editReasonValue)}"
                       required>
            </div>
            <div class="waste-edit-form__actions">
                <button type="submit" class="btn btn-primary">Lưu sửa</button>
                <c:url var="cancelEditUrl" value="/barista/waste">
                    <c:param name="q" value="${wasteLogQuery}" /><c:param name="logType" value="${wasteLogWasteType}" />
                    <c:param name="status" value="${wasteLogStatus}" /><c:param name="pageSize" value="${wasteLogPage.pageSize}" />
                    <c:param name="page" value="${wasteLogPage.page}" />
                </c:url>
                <a class="btn btn-ghost" href="${cancelEditUrl}">Huỷ sửa</a>
            </div>
        </form>
    </div>
</c:if>
</div><%-- /is-viewonly: hết phần ghi dữ liệu --%>

<%-- Nhật ký chỉ để đọc/tra cứu nên vẫn tìm và lật trang được khi ngoài ca;
     riêng nút Sửa/Huỷ từng dòng vẫn bị khoá bên dưới. --%>
<h3 class="section-title">Nhật ký hao hụt · ${view.scopeLabel(scope.kind)}</h3>
<div>
            <form id="wasteLogFilters" class="table-toolbar" action="${ctx}/barista/waste" method="get">
                <input type="hidden" name="page" value="1">
                <div class="form-group table-search">
                    <label for="wasteLogSearch">Tìm kiếm</label>
                    <input id="wasteLogSearch" class="form-control" type="search" name="q" value="${fn:escapeXml(wasteLogQuery)}"
                           placeholder="Tìm nguyên liệu, lý do hoặc người ghi" autocomplete="off">
                </div>
                <div class="form-group">
                    <label for="wasteTypeFilter">Loại ghi nhận</label>
                    <%-- Tên "logType" chứ không phải "wasteType": form ghi hao hụt bên trên đã dùng
                         "wasteType" cho từng dòng, trùng tên là bộ lọc ăn nhầm giá trị của form. --%>
                    <select id="wasteTypeFilter" name="logType" class="form-control tt-filter">
                        <option value="">Tất cả</option>
                        <option value="SPILL" ${wasteLogWasteType == 'SPILL' ? 'selected' : ''}>Đổ/rơi</option>
                        <option value="EXPIRED" ${wasteLogWasteType == 'EXPIRED' ? 'selected' : ''}>Hỏng / hết hạn</option>
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
                <div class="form-group">
                    <label for="wasteLogPageSize">Hiển thị</label>
                    <select id="wasteLogPageSize" name="pageSize" class="form-control tt-size">
                        <option value="5" ${wasteLogPage.pageSize == 5 ? 'selected' : ''}>5</option>
                        <option value="10" ${wasteLogPage.pageSize == 10 ? 'selected' : ''}>10</option>
                        <option value="20" ${wasteLogPage.pageSize == 20 ? 'selected' : ''}>20</option>
                        <option value="50" ${wasteLogPage.pageSize == 50 ? 'selected' : ''}>50</option>
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
                            <th>Người ghi</th>
                            <th style="width:100px">Trạng thái</th>
                            <th style="width:150px">Thao tác</th>
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
                                        <td>${view.timeDateUtc(w.loggedAt)}</td>
                                        <td>
                                            <strong>${w.ingredientName}</strong>
                                            <c:if test="${w.ingredientType == 'PREPPED'}"><span class="badge badge-making">Pha sẵn</span></c:if>
                                        </td>
                                        <td><strong>${view.plain(w.quantity)}</strong> ${w.ingredientUnit}</td>
                                        <td>${view.wasteType(w.wasteType)}</td>
                                        <td>${fn:escapeXml(w.reason)}</td>
                                        <td>${w.loggedByName}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${w.status == 'VOIDED'}"><span class="badge badge-cancelled">Đã huỷ</span></c:when>
                                                <c:otherwise><span class="badge badge-ready">Hiệu lực</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <div class="waste-actions ${onShift ? '' : 'is-viewonly'}">
                                                <c:if test="${w.editable and w.loggedBy == currentUserId}">
                                                    <%-- Mang theo bộ lọc + trang đang xem để sửa xong không bị văng
                                                    về trang 1. --%>
                                                    <c:url var="editWasteUrl" value="/barista/waste">
                                                        <c:param name="q" value="${wasteLogQuery}" /><c:param
                                                            name="logType" value="${wasteLogWasteType}" />
                                                        <c:param name="status" value="${wasteLogStatus}" /><c:param
                                                            name="pageSize" value="${wasteLogPage.pageSize}" />
                                <c:param name="page" value="${wasteLogPage.page}" /><c:param name="edit"
                                    value="${w.wasteEntryId}" />
                                                    </c:url>
                                                    <a class="btn btn-ghost btn-sm" href="${editWasteUrl}#editWaste">Sửa</a>
                                                </c:if>
                                                <c:if test="${w.voidable and w.loggedBy == currentUserId}">
                                                    <form action="${ctx}/barista/waste"
                                                          method="post"
                                                          onsubmit="return confirm('Huỷ bản ghi này? Tồn kho sẽ được hoàn lại qua sổ cái.');">
                                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                        <input type="hidden" name="action" value="void">
                                <input type="hidden" name="wasteEntryId" value="${w.wasteEntryId}">
                                                        <input type="hidden" name="q" value="${fn:escapeXml(wasteLogQuery)}">
                                                        <input type="hidden" name="logType" value="${wasteLogWasteType}">
                                                        <input type="hidden" name="status" value="${wasteLogStatus}">
                                                        <input type="hidden" name="pageSize" value="${wasteLogPage.pageSize}">
                                                        <input type="hidden" name="page" value="${wasteLogPage.page}">
                                                        <button type="submit" class="btn btn-ghost btn-sm waste-void-btn">Huỷ</button>
                                                    </form>
                                                </c:if>
                                            </div>
                                        </td>
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
                        <c:when test="${wasteLogPage.total == 0}">0 dòng</c:when>
                        <c:otherwise>${wasteLogPage.startRow}-${wasteLogPage.endRow} / ${wasteLogPage.total} dòng · trang ${wasteLogPage.page}/${wasteLogPage.totalPages}</c:otherwise>
                    </c:choose>
                </span>
                <c:if test="${wasteLogPage.totalPages > 1}">
                    <div class="pagination" aria-label="Phân trang nhật ký hao hụt">
                        <c:url var="firstWasteLogPageUrl" value="/barista/waste">
                            <c:param name="q" value="${wasteLogQuery}" /><c:param name="logType" value="${wasteLogWasteType}" />
                            <c:param name="status" value="${wasteLogStatus}" /><c:param name="pageSize"
                                value="${wasteLogPage.pageSize}" />
                            <c:param name="page" value="1" />
                        </c:url>
                        <c:url var="previousWasteLogPageUrl" value="/barista/waste">
                            <c:param name="q" value="${wasteLogQuery}" /><c:param name="logType" value="${wasteLogWasteType}" />
                            <c:param name="status" value="${wasteLogStatus}" /><c:param name="pageSize"
                                value="${wasteLogPage.pageSize}" />
                            <c:param name="page" value="${wasteLogPage.page - 1}" />
                        </c:url>
                        <a class="page"
                           href="${firstWasteLogPageUrl}"
                           aria-disabled="${not wasteLogPage.hasPrevious}"
                           title="Trang đầu">«</a>
                        <a class="page"
                           href="${previousWasteLogPageUrl}"
                           aria-disabled="${not wasteLogPage.hasPrevious}"
                           title="Trang trước">‹</a>
                        <c:forEach var="pageNumber" items="${wasteLogPage.visiblePages}">
                            <c:url var="wasteLogPageUrl" value="/barista/waste">
                                <c:param name="q" value="${wasteLogQuery}" /><c:param name="logType" value="${wasteLogWasteType}" />
                                <c:param name="status" value="${wasteLogStatus}" /><c:param name="pageSize"
                                    value="${wasteLogPage.pageSize}" />
                                <c:param name="page" value="${pageNumber}" />
                            </c:url>
                            <a class="page ${pageNumber == wasteLogPage.page ? 'is-active' : ''}"
                               href="${wasteLogPageUrl}"
                               aria-current="${pageNumber == wasteLogPage.page ? 'page' : 'false'}">${pageNumber}</a>
                        </c:forEach>
                        <c:url var="nextWasteLogPageUrl" value="/barista/waste">
                            <c:param name="q" value="${wasteLogQuery}" /><c:param name="logType" value="${wasteLogWasteType}" />
                            <c:param name="status" value="${wasteLogStatus}" /><c:param name="pageSize"
                                value="${wasteLogPage.pageSize}" />
                            <c:param name="page" value="${wasteLogPage.page + 1}" />
                        </c:url>
                        <c:url var="lastWasteLogPageUrl" value="/barista/waste">
                            <c:param name="q" value="${wasteLogQuery}" /><c:param name="logType" value="${wasteLogWasteType}" />
                            <c:param name="status" value="${wasteLogStatus}" /><c:param name="pageSize"
                                value="${wasteLogPage.pageSize}" />
                            <c:param name="page" value="${wasteLogPage.totalPages}" />
                        </c:url>
                        <a class="page"
                           href="${nextWasteLogPageUrl}"
                           aria-disabled="${not wasteLogPage.hasNext}"
                           title="Trang sau">›</a>
                        <a class="page"
                           href="${lastWasteLogPageUrl}"
                           aria-disabled="${not wasteLogPage.hasNext}"
                           title="Trang cuối">»</a>
                    </div>
                </c:if>
            </div>
</div><%-- /nhật ký --%>

<script>
(function(){
  var rows = document.getElementById('wasteRows');
  var add = document.getElementById('addWasteRow');
  if (!rows || !add) return;

  function syncPreset(row){
    var type = row.querySelector('.waste-type');
    var preset = row.querySelector('.waste-reason-preset');
    var detail = row.querySelector('.waste-reason-detail');
    if (!type || !preset) return;
    var current = preset.value;
    Array.prototype.forEach.call(preset.options, function(opt){
      var optType = opt.getAttribute('data-type');
      var show = !optType || optType === type.value;
      opt.hidden = !show;
      opt.disabled = !show;
    });
    if (current && preset.selectedOptions[0] && preset.selectedOptions[0].disabled) preset.value = '';
    if (detail) {
      var isOther = preset.value === 'Khác';
      detail.required = isOther;
      detail.setAttribute('aria-required', isOther ? 'true' : 'false');
    }
  }

  function wire(row){
    syncPreset(row);
    var type = row.querySelector('.waste-type');
    if (type) type.addEventListener('change', function(){ syncPreset(row); });
    var remove = row.querySelector('.waste-row__remove');
    if (remove) remove.addEventListener('click', function(){
      if (rows.querySelectorAll('.waste-row').length > 1) row.remove();
      else {
        Array.prototype.forEach.call(row.querySelectorAll('input,select'), function(el){
          if (el.name === 'wasteType') el.value = 'SPILL';
          else el.value = '';
        });
        syncPreset(row);
      }
    });
  }

  Array.prototype.forEach.call(rows.querySelectorAll('.waste-row'), wire);
  add.addEventListener('click', function(){
    var base = rows.querySelector('.waste-row');
    if (!base) return;
    var clone = base.cloneNode(true);
    Array.prototype.forEach.call(clone.querySelectorAll('input,select'), function(el){
      if (el.name === 'wasteType') el.value = 'SPILL';
      else el.value = '';
    });
    rows.appendChild(clone);
    wire(clone);
    var first = clone.querySelector('select[name="ingredientId"]');
    if (first) first.focus();
  });
})();
</script>

<script>
(function(){
  document.querySelectorAll('form[action$="/barista/waste"]').forEach(function(form){
    form.addEventListener('submit', function(){
      var requestId = form.querySelector('.js-waste-request-id');
      if (requestId && !requestId.value) requestId.value = window.crypto && crypto.randomUUID ? crypto.randomUUID() : String(Date.now()) + '-' + Math.random().toString(36).slice(2);
      var button = form.querySelector('button[type="submit"]');
      if (!button || button.disabled) return;
      button.disabled = true; button.dataset.originalText = button.textContent; button.textContent = 'Đang ghi…';
    });
  });
})();
</script>

<script>
(function(){
  var form = document.getElementById('wasteLogFilters');
  if (!form) return;
  var search = document.getElementById('wasteLogSearch');
  var page = form.querySelector('input[name="page"]');
  var timer;

  function submitFromFirstPage(){
    if (page) page.value = '1';
    if (form.requestSubmit) form.requestSubmit();
    else form.submit();
  }

  // Lọc chạy ở server nên mỗi lần gõ là một lần tải lại trang; ghi cờ để trả lại con trỏ cho ô tìm kiếm.
  var FOCUS_KEY = 'wasteLogSearchFocus';
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
