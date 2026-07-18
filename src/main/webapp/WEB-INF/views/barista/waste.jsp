<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Pha chế</div>
        <h1>Hao hụt & Làm lại</h1>
        <p>Ghi nguyên liệu bị hao (đổ, rơi, hết hạn) và món làm lại — để theo dõi & cắt giảm thất thoát.</p>
    </div>
    <div class="waste-scope">
        <strong>${scope.label}</strong>
        <span>${scope.windowDisplay}</span>
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

<jsp:include page="../layout/_baristaShiftBanner.jsp" />

<div class="${onShift ? '' : 'is-viewonly'}">
<section class="waste-summary">
    <div class="card stat">
        <span class="label">${scope.label}</span>
        <span class="value">${summary.activeCount}</span>
        <small>dòng hiệu lực · tổng <fmt:formatNumber value="${summary.totalCost}" maxFractionDigits="0"/> ₫</small>
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
<c:if test="${summary.missingCostCount > 0}">
    <div class="alert alert-info">Có ${summary.missingCostCount} dòng chưa có đơn giá nhập gần nhất, thành tiền đang để “Chưa có giá”.</div>
</c:if>

<section class="waste-mode-grid">
    <div class="card waste-card">
        <div class="waste-card__head">
            <div>
                <h3>Hao hụt nguyên liệu</h3>
                <p>Ghi nhanh nhiều nguyên liệu bị đổ, rơi, hết hạn hoặc thất thoát khác.</p>
            </div>
        </div>
        <form id="ingredientWasteForm" action="${ctx}/barista/waste" method="post" novalidate>
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="createIngredientWaste">
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
                            <input type="number" name="quantity" class="form-control" min="0.001" step="0.001" value="${row.quantity}">
                        </div>
                        <div class="form-group waste-row__type">
                            <label>Loại</label>
                            <select name="wasteType" class="form-control waste-type">
                                <option value="SPILL" ${row.wasteType == 'SPILL' ? 'selected' : ''}>Đổ/rơi</option>
                                <option value="EXPIRED" ${row.wasteType == 'EXPIRED' ? 'selected' : ''}>Hết hạn</option>
                                <option value="OTHER" ${row.wasteType == 'OTHER' ? 'selected' : ''}>Khác</option>
                            </select>
                        </div>
                        <div class="form-group waste-row__preset">
                            <label>Lý do</label>
                            <select name="reasonPreset" class="form-control waste-reason-preset">
                                <option value="">-- Gợi ý --</option>
                                <option data-type="SPILL" value="Đổ khi pha" ${row.reasonPreset == 'Đổ khi pha' ? 'selected' : ''}>Đổ khi pha</option>
                                <option data-type="SPILL" value="Rơi khi thao tác" ${row.reasonPreset == 'Rơi khi thao tác' ? 'selected' : ''}>Rơi khi thao tác</option>
                                <option data-type="SPILL" value="Sai định lượng" ${row.reasonPreset == 'Sai định lượng' ? 'selected' : ''}>Sai định lượng</option>
                                <option data-type="EXPIRED" value="Hết hạn" ${row.reasonPreset == 'Hết hạn' ? 'selected' : ''}>Hết hạn</option>
                                <option data-type="EXPIRED" value="Bảo quản lỗi" ${row.reasonPreset == 'Bảo quản lỗi' ? 'selected' : ''}>Bảo quản lỗi</option>
                                <option data-type="EXPIRED" value="Quá thời gian mở nắp" ${row.reasonPreset == 'Quá thời gian mở nắp' ? 'selected' : ''}>Quá thời gian mở nắp</option>
                                <option data-type="OTHER" value="Kiểm kê lệch" ${row.reasonPreset == 'Kiểm kê lệch' ? 'selected' : ''}>Kiểm kê lệch</option>
                                <option data-type="OTHER" value="Mẫu thử/QC" ${row.reasonPreset == 'Mẫu thử/QC' ? 'selected' : ''}>Mẫu thử/QC</option>
                                <option data-type="OTHER" value="Khác" ${row.reasonPreset == 'Khác' ? 'selected' : ''}>Khác</option>
                            </select>
                        </div>
                        <div class="form-group waste-row__note">
                            <label>Nhập thêm</label>
                            <input type="text" name="reasonDetail" class="form-control" maxlength="255" value="${row.reasonDetail}">
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
    </div>

    <div class="card waste-card">
        <div class="waste-card__head">
            <div>
                <h3>Làm lại món</h3>
                <p>Chọn món và số lượng, hệ thống tự bung công thức thành các dòng nguyên liệu REMAKE.</p>
            </div>
        </div>
        <form action="${ctx}/barista/waste" method="post" class="waste-remake-form">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="remakeProduct">
            <div class="form-group">
                <label>Món</label>
                <select name="productId" id="remakeProduct" class="form-control" required>
                    <option value="">-- Chọn món --</option>
                    <c:forEach var="p" items="${products}">
                        <option value="${p.productId}" ${submittedRemake.productId == p.productId ? 'selected' : ''}>${p.productName}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-group" id="remakeModifiersGroup" style="display:none">
                <label>Tuỳ chọn đã thêm (trừ nguyên liệu kèm)</label>
                <div id="remakeModifiers" class="remake-modifiers"></div>
                <small class="muted">Tick những tuỳ chọn của ly gốc (thêm shot, đổi sữa…) để trừ đúng nguyên liệu.</small>
            </div>
            <div class="form-group">
                <label>Số lượng</label>
                <input type="number" name="productQty" class="form-control" min="1" step="1" value="${submittedRemake.quantity}" required>
            </div>
            <div class="form-group">
                <label>Lý do</label>
                <select name="remakeReasonPreset" class="form-control">
                    <option value="">-- Gợi ý --</option>
                    <option value="Sai công thức" ${submittedRemake.reasonPreset == 'Sai công thức' ? 'selected' : ''}>Sai công thức</option>
                    <option value="Khách yêu cầu làm lại" ${submittedRemake.reasonPreset == 'Khách yêu cầu làm lại' ? 'selected' : ''}>Khách yêu cầu làm lại</option>
                    <option value="Đổ/rơi sau khi pha" ${submittedRemake.reasonPreset == 'Đổ/rơi sau khi pha' ? 'selected' : ''}>Đổ/rơi sau khi pha</option>
                    <option value="Lỗi chất lượng" ${submittedRemake.reasonPreset == 'Lỗi chất lượng' ? 'selected' : ''}>Lỗi chất lượng</option>
                </select>
            </div>
            <div class="form-group">
                <label>Nhập thêm</label>
                <input type="text" name="remakeReasonDetail" class="form-control" maxlength="255" value="${submittedRemake.reasonDetail}">
            </div>
            <button type="submit" class="btn btn-primary btn-full">Ghi làm lại món</button>
        </form>
        <script>
          (function(){
            var MODS = ${empty remakeModifiersJson ? '{}' : remakeModifiersJson};
            var sel = document.getElementById('remakeProduct');
            var group = document.getElementById('remakeModifiersGroup');
            var box = document.getElementById('remakeModifiers');
            if (!sel || !group || !box) return;
            function render(){
              var list = MODS[sel.value] || [];
              box.innerHTML = '';
              if (!list.length){ group.style.display = 'none'; return; }
              group.style.display = '';
              list.forEach(function(o){
                var id = 'rmk-opt-' + o.id;
                var lbl = document.createElement('label');
                lbl.className = 'remake-modifiers__item';
                lbl.setAttribute('for', id);
                var cb = document.createElement('input');
                cb.type = 'checkbox'; cb.name = 'remakeOptionId'; cb.value = o.id; cb.id = id;
                lbl.appendChild(cb);
                lbl.appendChild(document.createTextNode(' ' + o.name));
                box.appendChild(lbl);
              });
            }
            sel.addEventListener('change', render);
            render();
          })();
        </script>
    </div>
</section>

<c:if test="${not empty editLog}">
    <div id="editWaste" class="card waste-edit-card">
        <div>
            <h3>Sửa bản ghi hao hụt</h3>
            <p>${editLog.ingredientName} · ${editLog.loggedAtDisplay}</p>
        </div>
        <form action="${ctx}/barista/waste" method="post" class="waste-edit-form">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="wasteLogId" value="${editLog.wasteLogId}">
            <c:set var="editQty" value="${empty requestScope.editQuantity ? editLog.quantity : requestScope.editQuantity}" />
            <c:set var="editType" value="${empty requestScope.editWasteType ? editLog.wasteType : requestScope.editWasteType}" />
            <c:set var="editReasonValue" value="${empty requestScope.editReason ? editLog.reason : requestScope.editReason}" />
            <div class="form-group">
                <label>Số lượng</label>
                <input type="number" name="quantity" class="form-control" min="0.001" step="0.001" value="${editQty}" required>
            </div>
            <div class="form-group">
                <label>Loại</label>
                <select name="wasteType" class="form-control">
                    <option value="SPILL" ${editType == 'SPILL' ? 'selected' : ''}>Đổ/rơi</option>
                    <option value="EXPIRED" ${editType == 'EXPIRED' ? 'selected' : ''}>Hết hạn</option>
                    <option value="OTHER" ${editType == 'OTHER' ? 'selected' : ''}>Khác</option>
                </select>
            </div>
            <div class="form-group waste-edit-form__reason">
                <label>Lý do</label>
                <input type="text" name="reason" class="form-control" maxlength="255" value="${editReasonValue}">
            </div>
            <div class="waste-edit-form__actions">
                <button type="submit" class="btn btn-primary">Lưu sửa</button>
                <a class="btn btn-ghost" href="${ctx}/barista/waste">Huỷ sửa</a>
            </div>
        </form>
    </div>
</c:if>

<h3 class="section-title">Nhật ký trong phạm vi đang xem</h3>
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
                <div class="form-group">
                    <label for="wasteLogPageSize">Hiển thị</label>
                    <select id="wasteLogPageSize" name="pageSize" class="form-control tt-size">
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
                            <th style="width:130px">Thành tiền</th>
                            <th>Người ghi</th>
                            <th style="width:100px">Trạng thái</th>
                            <th style="width:150px">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty logs}">
                                <tr class="tt-empty"><td colspan="9">Không tìm thấy nhật ký phù hợp.</td></tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="w" items="${logs}">
                                    <tr class="${w.status == 'VOIDED' ? 'row-muted' : ''}">
                                        <td>${w.loggedAtDisplay}</td>
                                        <td>
                                            <strong>${w.ingredientName}</strong>
                                            <c:if test="${w.ingredientType == 'PREPPED'}"><span class="badge badge-making">Pha sẵn</span></c:if>
                                        </td>
                                        <td><strong>${w.quantity}</strong> ${w.ingredientUnit}</td>
                                        <td>${w.wasteTypeLabel}</td>
                                        <td>${w.reason}</td>
                                        <td><strong>${w.costDisplay}</strong></td>
                                        <td>${w.loggedByName}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${w.status == 'VOIDED'}"><span class="badge badge-cancelled">Đã huỷ</span></c:when>
                                                <c:otherwise><span class="badge badge-ready">Hiệu lực</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <div class="waste-actions">
                                                <c:if test="${w.editable}">
                                                    <a class="btn btn-ghost btn-sm" href="${ctx}/barista/waste?edit=${w.wasteLogId}#editWaste">Sửa</a>
                                                </c:if>
                                                <c:if test="${w.status == 'ACTIVE'}">
                                                    <form action="${ctx}/barista/waste" method="post" onsubmit="return confirm('Huỷ bản ghi này? Tồn kho sẽ được hoàn lại qua sổ cái.');">
                                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                        <input type="hidden" name="action" value="void">
                                                        <input type="hidden" name="wasteLogId" value="${w.wasteLogId}">
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
                <span class="tt-summary" aria-live="polite">${wasteLogPage.startRow}-${wasteLogPage.endRow} / ${wasteLogPage.total}</span>
                <c:if test="${wasteLogPage.totalPages > 1}">
                    <div class="pagination" aria-label="Phân trang nhật ký hao hụt">
                        <c:url var="firstWasteLogPageUrl" value="/barista/waste">
                            <c:param name="q" value="${wasteLogQuery}" /><c:param name="wasteType" value="${wasteLogWasteType}" />
                            <c:param name="status" value="${wasteLogStatus}" /><c:param name="pageSize" value="${wasteLogPage.pageSize}" /><c:param name="page" value="1" />
                        </c:url>
                        <c:url var="previousWasteLogPageUrl" value="/barista/waste">
                            <c:param name="q" value="${wasteLogQuery}" /><c:param name="wasteType" value="${wasteLogWasteType}" />
                            <c:param name="status" value="${wasteLogStatus}" /><c:param name="pageSize" value="${wasteLogPage.pageSize}" /><c:param name="page" value="${wasteLogPage.page - 1}" />
                        </c:url>
                        <a class="page" href="${firstWasteLogPageUrl}" aria-disabled="${not wasteLogPage.hasPrevious}">«</a>
                        <a class="page" href="${previousWasteLogPageUrl}" aria-disabled="${not wasteLogPage.hasPrevious}">‹</a>
                        <c:forEach var="pageNumber" items="${wasteLogPage.visiblePages}">
                            <c:url var="wasteLogPageUrl" value="/barista/waste">
                                <c:param name="q" value="${wasteLogQuery}" /><c:param name="wasteType" value="${wasteLogWasteType}" />
                                <c:param name="status" value="${wasteLogStatus}" /><c:param name="pageSize" value="${wasteLogPage.pageSize}" /><c:param name="page" value="${pageNumber}" />
                            </c:url>
                            <a class="page ${pageNumber == wasteLogPage.page ? 'is-active' : ''}" href="${wasteLogPageUrl}" aria-current="${pageNumber == wasteLogPage.page ? 'page' : 'false'}">${pageNumber}</a>
                        </c:forEach>
                        <c:url var="nextWasteLogPageUrl" value="/barista/waste">
                            <c:param name="q" value="${wasteLogQuery}" /><c:param name="wasteType" value="${wasteLogWasteType}" />
                            <c:param name="status" value="${wasteLogStatus}" /><c:param name="pageSize" value="${wasteLogPage.pageSize}" /><c:param name="page" value="${wasteLogPage.page + 1}" />
                        </c:url>
                        <c:url var="lastWasteLogPageUrl" value="/barista/waste">
                            <c:param name="q" value="${wasteLogQuery}" /><c:param name="wasteType" value="${wasteLogWasteType}" />
                            <c:param name="status" value="${wasteLogStatus}" /><c:param name="pageSize" value="${wasteLogPage.pageSize}" /><c:param name="page" value="${wasteLogPage.totalPages}" />
                        </c:url>
                        <a class="page" href="${nextWasteLogPageUrl}" aria-disabled="${not wasteLogPage.hasNext}">›</a>
                        <a class="page" href="${lastWasteLogPageUrl}" aria-disabled="${not wasteLogPage.hasNext}">»</a>
                    </div>
                </c:if>
            </div>
</div>
</div><%-- /is-viewonly --%>

<script>
(function(){
  var rows = document.getElementById('wasteRows');
  var add = document.getElementById('addWasteRow');
  if (!rows || !add) return;

  function syncPreset(row){
    var type = row.querySelector('.waste-type');
    var preset = row.querySelector('.waste-reason-preset');
    if (!type || !preset) return;
    var current = preset.value;
    Array.prototype.forEach.call(preset.options, function(opt){
      var optType = opt.getAttribute('data-type');
      var show = !optType || optType === type.value;
      opt.hidden = !show;
      opt.disabled = !show;
    });
    if (current && preset.selectedOptions[0] && preset.selectedOptions[0].disabled) preset.value = '';
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
