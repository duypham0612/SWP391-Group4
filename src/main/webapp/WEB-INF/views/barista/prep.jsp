<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Pha chế</div>
        <h1>Pha sẵn nguyên liệu</h1>
        <p>Pha đồ nền (cold brew, syrup…) từ nguyên liệu thô. Tồn được cập nhật qua sổ cái — chặn nếu thiếu nguyên liệu.</p>
    </div>
</div>

<c:if test="${not empty requestScope.flashError}">
    <div class="alert alert-error">${requestScope.flashError}</div>
</c:if>
<c:if test="${empty requestScope.flashError and not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
</c:if>

<jsp:include page="../layout/_baristaShiftBanner.jsp" />

<div class="${onShift ? '' : 'is-viewonly'}">
<%-- ===== Checklist: cần pha hôm nay ===== --%>
<c:set var="needCount" value="0" />
<c:forEach var="r" items="${checklist}"><c:if test="${r.needPrep}"><c:set var="needCount" value="${needCount + 1}" /></c:if></c:forEach>
<div class="card prep-checklist" style="margin-bottom:var(--s4)">
    <div class="prep-checklist__head">
        <h3 style="margin:0">Cần pha hôm nay</h3>
        <span class="muted">${needCount} loại cần pha · ${prepBatchPage.total} mẻ hôm nay</span>
    </div>
    <c:choose>
        <c:when test="${needCount == 0}">
            <p class="muted" style="margin:8px 0 0">Tồn đồ pha sẵn đều trên ngưỡng — chưa cần pha thêm.</p>
        </c:when>
        <c:otherwise>
            <div class="prep-chips">
                <c:forEach var="r" items="${checklist}">
                    <c:if test="${r.needPrep}">
                        <button type="button" class="prep-chip ${r.hasRecipe ? '' : 'prep-chip--norecipe'}"
                                data-prepped-id="${r.ingredientId}" ${r.hasRecipe ? '' : 'disabled'}
                                title="${r.hasRecipe ? 'Bấm để pha ngay' : 'Chưa có công thức prep'}">
                            <strong>${r.name}</strong>
                            <span>tồn ${r.onHand} / ngưỡng ${r.threshold} ${r.unit}</span>
                            <c:if test="${not r.hasRecipe}"><em>⚠ chưa có công thức</em></c:if>
                        </button>
                    </c:if>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%-- ===== Tạo mẻ (nhiều dòng một lần) ===== --%>
<div class="card prep-card" style="margin-bottom:var(--s4)">
    <h3 style="margin-top:0">Tạo mẻ pha sẵn</h3>
    <p class="muted" style="margin-top:0">Chọn nhiều nguyên liệu cùng lúc — bấm “Thêm nguyên liệu” để pha nhiều mẻ trong một lần. Nếu một dòng thiếu nguyên liệu thô thì không mẻ nào được tạo.</p>
    <form id="prepForm" action="${ctx}/barista/prep" method="post" novalidate>
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="createBatch">
        <div id="prepRows" class="prep-rows"></div>
        <div id="prepStockWarn" class="prep-stock-warn" hidden></div>
        <div class="prep-form-foot">
            <div class="prep-form-foot__left">
                <button type="button" id="prepAddRow" class="btn btn-ghost btn-sm">+ Thêm nguyên liệu</button>
                <button type="button" id="prepRefreshStock" class="btn btn-ghost btn-sm" title="Cập nhật lại tồn kho hiện tại">↻ Làm mới tồn</button>
            </div>
            <button type="submit" id="prepSubmit" class="btn btn-primary">Tạo mẻ</button>
        </div>
    </form>
</div>

<%-- Mẫu một dòng (clone bằng JS) — select render sẵn server-side kèm data-unit --%>
<template id="prepRowTpl">
    <div class="prep-row">
        <div class="form-group prep-row__grow" style="margin:0">
            <label>Nguyên liệu pha sẵn</label>
            <select name="preppedIngredientId" class="form-control prep-row__select">
                <option value="">-- Chọn --</option>
                <c:forEach var="i" items="${preppedIngredients}">
                    <option value="${i.ingredientId}" data-unit="${i.unit}">${i.name} (${i.unit})</option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group" style="margin:0">
            <label>Sản lượng</label>
            <div class="qty-stepper">
                <button type="button" class="qty-step" data-dir="-1" tabindex="-1" aria-label="Giảm">−</button>
                <input type="number" name="quantityProduced" class="form-control qty-input" min="0.001" step="0.001" placeholder="0">
                <button type="button" class="qty-step" data-dir="1" tabindex="-1" aria-label="Tăng">+</button>
                <span class="qty-unit"></span>
            </div>
        </div>
        <div class="form-group" style="margin:0">
            <label>Hạn dùng (tuỳ chọn)</label>
            <input type="datetime-local" name="expiresAt" class="form-control">
        </div>
        <button type="button" class="prep-row__remove" title="Xoá dòng" tabindex="-1">✕</button>
        <div class="prep-row__preview prep-preview" hidden></div>
        <div class="prep-row__error" hidden></div>
    </div>
</template>

<%-- ===== Mẻ pha hôm nay ===== --%>
<h3>Mẻ pha hôm nay</h3>
<div>
    <form id="prepBatchFilters" class="table-toolbar" action="${ctx}/barista/prep" method="get">
        <input type="hidden" name="page" value="1">
        <div class="form-group table-search">
            <label for="prepBatchSearch">Tìm kiếm</label>
            <input id="prepBatchSearch" class="form-control" type="search" name="q" value="${fn:escapeXml(prepBatchQuery)}"
                   placeholder="Mã mẻ, nguyên liệu hoặc người pha" autocomplete="off">
        </div>
        <div class="form-group">
            <label for="prepBatchIngredientFilter">Nguyên liệu</label>
            <select id="prepBatchIngredientFilter" name="ingredientId" class="form-control tt-filter">
                <option value="">Tất cả</option>
                <c:forEach var="i" items="${preppedIngredients}">
                    <option value="${i.ingredientId}" ${prepBatchIngredientId == i.ingredientId ? 'selected' : ''}>${i.name}</option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group">
            <label for="prepBatchExpiryFilter">Hạn dùng</label>
            <select id="prepBatchExpiryFilter" name="expiry" class="form-control tt-filter">
                <option value="">Tất cả</option>
                <option value="expired" ${prepBatchExpiry == 'expired' ? 'selected' : ''}>Đã hết hạn</option>
                <option value="soon" ${prepBatchExpiry == 'soon' ? 'selected' : ''}>Sắp hết hạn</option>
                <option value="ok" ${prepBatchExpiry == 'ok' ? 'selected' : ''}>Còn hạn</option>
                <option value="none" ${prepBatchExpiry == 'none' ? 'selected' : ''}>Chưa đặt</option>
            </select>
        </div>
        <div class="form-group">
            <label for="prepBatchStatusFilter">Trạng thái</label>
            <select id="prepBatchStatusFilter" name="status" class="form-control tt-filter">
                <option value="">Tất cả</option>
                <option value="ACTIVE" ${prepBatchStatus == 'ACTIVE' ? 'selected' : ''}>Hiệu lực</option>
                <option value="CANCELLED" ${prepBatchStatus == 'CANCELLED' ? 'selected' : ''}>Đã huỷ</option>
            </select>
        </div>
        <div class="form-group">
            <label for="prepBatchPageSize">Hiển thị</label>
            <select id="prepBatchPageSize" name="pageSize" class="form-control tt-size">
                <option value="10" ${prepBatchPage.pageSize == 10 ? 'selected' : ''}>10</option>
                <option value="20" ${prepBatchPage.pageSize == 20 ? 'selected' : ''}>20</option>
                <option value="50" ${prepBatchPage.pageSize == 50 ? 'selected' : ''}>50</option>
            </select>
        </div>
    </form>
    <table class="table">
        <thead><tr>
            <th style="width:46px">#</th><th>Nguyên liệu</th><th style="width:140px">Sản lượng</th>
            <th style="width:150px">Hạn dùng</th><th>Người pha</th><th style="width:110px">Lúc</th>
            <th style="width:104px">Trạng thái</th><th style="width:240px">Thao tác</th>
        </tr></thead>
        <tbody>
            <c:choose>
                <c:when test="${empty batches}">
                    <tr class="tt-empty"><td colspan="8">Không tìm thấy mẻ pha phù hợp.</td></tr>
                </c:when>
                <c:otherwise>
                    <c:forEach var="b" items="${batches}">
                        <tr<c:if test="${b.status == 'CANCELLED'}"> class="row-muted"</c:if>>
                            <td>${b.prepBatchId}</td>
                            <td>${b.preppedIngredientName}</td>
                            <td><strong>${b.quantityProduced}</strong> ${b.preppedIngredientUnit}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${b.expiryTier == 'expired'}"><span class="badge badge-cancelled">Hết hạn · ${b.expiresAtDisplay}</span></c:when>
                                    <c:when test="${b.expiryTier == 'soon'}"><span class="badge badge-waiting">Sắp hết · ${b.expiresAtDisplay}</span></c:when>
                                    <c:when test="${b.expiryTier == 'ok'}">${b.expiresAtDisplay}</c:when>
                                    <c:otherwise><span class="muted">—</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>${b.madeByName}</td>
                            <td>${b.madeAtDisplay}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${b.status == 'CANCELLED'}"><span class="badge badge-cancelled">Đã huỷ</span></c:when>
                                    <c:otherwise><span class="badge badge-ready">Hiệu lực</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:if test="${b.status == 'ACTIVE'}">
                                    <form action="${ctx}/barista/prep" method="post" class="prep-row-form"
                                          onsubmit="return confirm('Cập nhật sản lượng mẻ này? Chênh lệch sẽ ghi vào sổ cái tồn kho.');">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="updateBatch">
                                        <input type="hidden" name="prepBatchId" value="${b.prepBatchId}">
                                        <input type="number" name="quantityProduced" class="form-control prep-row-qty" min="0.001" step="0.001" value="${b.quantityProduced}" required>
                                        <button type="submit" class="btn btn-ghost btn-sm">Sửa</button>
                                    </form>
                                    <form action="${ctx}/barista/prep" method="post" style="display:inline"
                                          onsubmit="return confirm('Huỷ mẻ này? Tồn kho sẽ được hoàn lại qua sổ cái.');">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="cancelBatch">
                                        <input type="hidden" name="prepBatchId" value="${b.prepBatchId}">
                                        <button type="submit" class="btn btn-ghost btn-sm prep-cancel-btn">Huỷ</button>
                                    </form>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </tbody>
    </table>
    <div class="table-tools-foot">
        <span class="tt-summary" aria-live="polite">${prepBatchPage.startRow}-${prepBatchPage.endRow} / ${prepBatchPage.total}</span>
        <c:if test="${prepBatchPage.totalPages > 1}">
            <div class="pagination" aria-label="Phân trang mẻ pha hôm nay">
                <c:url var="firstPrepBatchPageUrl" value="/barista/prep"><c:param name="q" value="${prepBatchQuery}" /><c:param name="ingredientId" value="${prepBatchIngredientId}" /><c:param name="expiry" value="${prepBatchExpiry}" /><c:param name="status" value="${prepBatchStatus}" /><c:param name="pageSize" value="${prepBatchPage.pageSize}" /><c:param name="page" value="1" /></c:url>
                <c:url var="previousPrepBatchPageUrl" value="/barista/prep"><c:param name="q" value="${prepBatchQuery}" /><c:param name="ingredientId" value="${prepBatchIngredientId}" /><c:param name="expiry" value="${prepBatchExpiry}" /><c:param name="status" value="${prepBatchStatus}" /><c:param name="pageSize" value="${prepBatchPage.pageSize}" /><c:param name="page" value="${prepBatchPage.page - 1}" /></c:url>
                <a class="page" href="${firstPrepBatchPageUrl}" aria-disabled="${not prepBatchPage.hasPrevious}">«</a>
                <a class="page" href="${previousPrepBatchPageUrl}" aria-disabled="${not prepBatchPage.hasPrevious}">‹</a>
                <c:forEach var="pageNumber" items="${prepBatchPage.visiblePages}">
                    <c:url var="prepBatchPageUrl" value="/barista/prep"><c:param name="q" value="${prepBatchQuery}" /><c:param name="ingredientId" value="${prepBatchIngredientId}" /><c:param name="expiry" value="${prepBatchExpiry}" /><c:param name="status" value="${prepBatchStatus}" /><c:param name="pageSize" value="${prepBatchPage.pageSize}" /><c:param name="page" value="${pageNumber}" /></c:url>
                    <a class="page ${pageNumber == prepBatchPage.page ? 'is-active' : ''}" href="${prepBatchPageUrl}" aria-current="${pageNumber == prepBatchPage.page ? 'page' : 'false'}">${pageNumber}</a>
                </c:forEach>
                <c:url var="nextPrepBatchPageUrl" value="/barista/prep"><c:param name="q" value="${prepBatchQuery}" /><c:param name="ingredientId" value="${prepBatchIngredientId}" /><c:param name="expiry" value="${prepBatchExpiry}" /><c:param name="status" value="${prepBatchStatus}" /><c:param name="pageSize" value="${prepBatchPage.pageSize}" /><c:param name="page" value="${prepBatchPage.page + 1}" /></c:url>
                <c:url var="lastPrepBatchPageUrl" value="/barista/prep"><c:param name="q" value="${prepBatchQuery}" /><c:param name="ingredientId" value="${prepBatchIngredientId}" /><c:param name="expiry" value="${prepBatchExpiry}" /><c:param name="status" value="${prepBatchStatus}" /><c:param name="pageSize" value="${prepBatchPage.pageSize}" /><c:param name="page" value="${prepBatchPage.totalPages}" /></c:url>
                <a class="page" href="${nextPrepBatchPageUrl}" aria-disabled="${not prepBatchPage.hasNext}">›</a>
                <a class="page" href="${lastPrepBatchPageUrl}" aria-disabled="${not prepBatchPage.hasNext}">»</a>
            </div>
        </c:if>
    </div>
</div>
</div><%-- /is-viewonly --%>

<script>
  (function(){
    var ctx = '${ctx}';
    var RECIPES = ${recipeJson};
    var RAW_ONHAND = ${empty rawOnHandJson ? '{}' : rawOnHandJson};   // {rawId: onHand} — ảnh chụp, làm mới được
    var INITIAL_ROWS = ${empty submittedPrepRowsJson ? '[]' : submittedPrepRowsJson};
    var rows = document.getElementById('prepRows');
    var tpl = document.getElementById('prepRowTpl');
    var addBtn = document.getElementById('prepAddRow');
    var refreshBtn = document.getElementById('prepRefreshStock');
    var stockWarn = document.getElementById('prepStockWarn');
    var submit = document.getElementById('prepSubmit');
    var form = document.getElementById('prepForm');

    function fmt(n){ return (Math.round(n * 1000) / 1000); }
    function blank(v){ return !v || !String(v).trim(); }
    function rawHave(id){ var v = RAW_ONHAND[id]; return (v == null) ? 0 : Number(v); }
    function haveText(have, unit){ return have < 0 ? 'còn 0 · đang âm kho' : ('còn ' + fmt(have) + ' ' + unit); }

    function addRow(seed){
      var data = (seed && typeof seed === 'object') ? seed : {preppedIngredientId: seed || ''};
      var node = tpl.content.firstElementChild.cloneNode(true);
      rows.appendChild(node);
      if (data.preppedIngredientId){ node.querySelector('.prep-row__select').value = data.preppedIngredientId; }
      if (data.quantityProduced){ node.querySelector('.qty-input').value = data.quantityProduced; }
      if (data.expiresAt){ node.querySelector('input[name="expiresAt"]').value = data.expiresAt; }
      refreshRow(node);
      return node;
    }

    function clearError(row){
      row.classList.remove('prep-row--error');
      row.querySelectorAll('[aria-invalid="true"]').forEach(function(el){ el.removeAttribute('aria-invalid'); });
      var box = row.querySelector('.prep-row__error');
      if (box){ box.hidden = true; box.textContent = ''; }
    }

    function setError(row, field, msg){
      row.classList.add('prep-row--error');
      if (field) field.setAttribute('aria-invalid', 'true');
      var box = row.querySelector('.prep-row__error');
      if (box){ box.textContent = msg; box.hidden = false; }
      return {valid:false, row:row, field:field};
    }

    function refreshRow(row){
      var sel = row.querySelector('.prep-row__select');
      var qty = row.querySelector('.qty-input');
      var unit = row.querySelector('.qty-unit');
      var preview = row.querySelector('.prep-row__preview');
      var id = sel.value;
      var opt = sel.options[sel.selectedIndex];
      unit.textContent = (id && opt) ? (opt.getAttribute('data-unit') || '') : '';

      if (!id){ preview.hidden = true; return; }
      var lines = RECIPES[id] || [];
      if (lines.length === 0){
        preview.hidden = false;
        preview.className = 'prep-row__preview prep-preview prep-preview--warn';
        preview.innerHTML = '⚠ Nguyên liệu này chưa có công thức prep — không thể tạo mẻ.';
        return;
      }
      var pUnit = unit.textContent || '';
      var q = parseFloat(qty.value);
      var maxQty = Infinity;
      var html = '';
      if (q > 0){
        html += '<div class="prep-preview__title">Sẽ trừ nguyên liệu thô:</div><ul>';
        lines.forEach(function(l){
          var c = q / l.y * l.q;
          var have = rawHave(l.r);
          var lack = c > have + 1e-9;
          html += '<li class="' + (lack ? 'prep-lack' : '') + '">' + l.n
                + ': cần <strong>' + fmt(c) + ' ' + l.u + '</strong> · ' + haveText(have, l.u) + '</li>';
        });
        html += '</ul>';
      }
      // Tối đa pha được theo raw eo hẹp nhất (với tồn hiện tại, chưa trừ các dòng khác)
      lines.forEach(function(l){
        var perUnit = l.q / l.y;
        if (perUnit > 0){ var cap = Math.max(0, rawHave(l.r)) / perUnit; if (cap < maxQty) maxQty = cap; }
      });
      if (isFinite(maxQty)) html += '<div class="prep-preview__max">Tối đa pha được ~ <strong>' + fmt(maxQty) + ' ' + pUnit + '</strong> với tồn hiện tại</div>';
      preview.hidden = false;
      preview.className = 'prep-row__preview prep-preview';
      preview.innerHTML = html;
    }

    /** Cộng dồn nhu cầu RAW trên TẤT CẢ dòng, so tồn → banner + chặn mềm nút Tạo mẻ. */
    function syncSubmit(){
      var demand = {}, meta = {}, hasNoRecipe = false;
      rows.querySelectorAll('.prep-row').forEach(function(row){
        var id = row.querySelector('.prep-row__select').value;
        if (!id) return;
        var lines = RECIPES[id] || [];
        if (lines.length === 0){ hasNoRecipe = true; return; }
        var q = parseFloat(row.querySelector('.qty-input').value);
        if (!(q > 0)) return;
        lines.forEach(function(l){
          demand[l.r] = (demand[l.r] || 0) + q / l.y * l.q;
          meta[l.r] = {n:l.n, u:l.u};
        });
      });
      var short = [];
      Object.keys(demand).forEach(function(rid){
        var have = rawHave(rid);
        if (demand[rid] > have + 1e-9)
          short.push(meta[rid].n + ': cần ' + fmt(demand[rid]) + ' / còn ' + fmt(Math.max(0, have)) + ' ' + meta[rid].u);
      });
      if (short.length){
        stockWarn.hidden = false;
        stockWarn.innerHTML = '⚠ Không đủ nguyên liệu thô: ' + short.join('; ') + '. Hãy giảm sản lượng hoặc nhập thêm kho.';
        submit.disabled = true;
      } else {
        stockWarn.hidden = true;
        submit.disabled = hasNoRecipe;
      }
    }

    function validateRow(row){
      clearError(row);
      var sel = row.querySelector('.prep-row__select');
      var qty = row.querySelector('.qty-input');
      var exp = row.querySelector('input[name="expiresAt"]');
      var id = sel.value.trim();
      var qRaw = qty.value.trim();
      var expRaw = exp.value.trim();
      if (!id && !qRaw && !expRaw) return {valid:false, empty:true};
      if (!id) return setError(row, sel, 'Chọn nguyên liệu pha sẵn cho dòng này.');
      if (!qRaw) return setError(row, qty, 'Nhập sản lượng lớn hơn 0.');
      var q = Number(qRaw);
      if (!Number.isFinite(q) || q <= 0) return setError(row, qty, 'Sản lượng phải lớn hơn 0.');
      if ((RECIPES[id] || []).length === 0) return setError(row, sel, 'Nguyên liệu này chưa có công thức prep.');
      if (expRaw){
        var expDate = new Date(expRaw);
        if (Number.isNaN(expDate.getTime())) return setError(row, exp, 'Hạn dùng không hợp lệ.');
        if (expDate.getTime() <= Date.now()) return setError(row, exp, 'Hạn dùng phải ở tương lai.');
      }
      return {valid:true};
    }

    function focusError(row, field){
      row.scrollIntoView({behavior:'smooth', block:'center'});
      window.setTimeout(function(){ (field || row.querySelector('.prep-row__select')).focus(); }, 150);
    }

    // Event delegation cho mọi dòng
    rows.addEventListener('change', function(e){
      if (e.target.classList.contains('prep-row__select')){
        var row = e.target.closest('.prep-row');
        clearError(row); refreshRow(row); syncSubmit();
      }
    });
    rows.addEventListener('input', function(e){
      if (e.target.classList.contains('qty-input') || e.target.name === 'expiresAt'){
        var row = e.target.closest('.prep-row');
        clearError(row); refreshRow(row); syncSubmit();
      }
    });
    rows.addEventListener('click', function(e){
      if (e.target.classList.contains('qty-step')){
        var row = e.target.closest('.prep-row');
        var input = e.target.parentNode.querySelector('.qty-input');
        var step = parseFloat(e.target.getAttribute('data-dir'));
        var v = parseFloat(input.value) || 0;
        input.value = Math.max(0.001, fmt(v + step));
        clearError(row); refreshRow(row); syncSubmit();
      } else if (e.target.classList.contains('prep-row__remove')){
        e.target.closest('.prep-row').remove();
        if (!rows.querySelector('.prep-row')) addRow();   // luôn còn ít nhất 1 dòng
        syncSubmit();
      }
    });

    addBtn.addEventListener('click', function(){ var r = addRow(); r.querySelector('.prep-row__select').focus(); });

    // Làm mới tồn RAW (chống chặn oan do tồn cũ) — không reload, giữ nguyên form
    refreshBtn.addEventListener('click', async function(){
      refreshBtn.disabled = true;
      try {
        var r = await fetch(ctx + '/barista/prep?stock=1', {credentials:'same-origin'});
        if (r.ok){
          RAW_ONHAND = await r.json();
          rows.querySelectorAll('.prep-row').forEach(refreshRow);
          syncSubmit();
        }
      } catch (err) { /* giữ tồn cũ nếu lỗi mạng */ }
      finally { refreshBtn.disabled = false; }
    });

    // "Pha ngay" từ checklist → thêm dòng đã chọn sẵn
    document.querySelectorAll('.prep-chip[data-prepped-id]').forEach(function(chip){
      chip.addEventListener('click', function(){
        var id = chip.getAttribute('data-prepped-id');
        // dùng dòng trống đầu tiên nếu có, không thì thêm mới
        var empty = null;
        rows.querySelectorAll('.prep-row__select').forEach(function(s){ if (!empty && !s.value) empty = s; });
        var row = empty ? empty.closest('.prep-row') : addRow();
        row.querySelector('.prep-row__select').value = id;
        clearError(row); refreshRow(row); syncSubmit();
        row.querySelector('.qty-input').focus();
        document.getElementById('prepForm').scrollIntoView({behavior:'smooth', block:'center'});
      });
    });

    form.addEventListener('submit', function(e){
      var firstBad = null;
      var validCount = 0;
      rows.querySelectorAll('.prep-row').forEach(function(row){
        var result = validateRow(row);
        if (result.valid) validCount++;
        else if (!result.empty && !firstBad) firstBad = result;
      });
      if (!firstBad && validCount === 0){
        var row = rows.querySelector('.prep-row') || addRow();
        firstBad = setError(row, row.querySelector('.prep-row__select'), 'Chọn ít nhất một nguyên liệu để tạo mẻ.');
      }
      if (firstBad){
        e.preventDefault();
        focusError(firstBad.row, firstBad.field);
      }
    });

    if (Array.isArray(INITIAL_ROWS) && INITIAL_ROWS.length){
      INITIAL_ROWS.forEach(function(row){ addRow(row); });
    } else {
      addRow();   // dòng khởi tạo
    }
    syncSubmit();
  })();
</script>
<script>
  (function(){
    var form = document.getElementById('prepBatchFilters');
    if (!form) return;
    var search = document.getElementById('prepBatchSearch');
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
