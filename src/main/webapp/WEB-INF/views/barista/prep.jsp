<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Chuẩn bị đầu ca</div>
        <h1>Pha sẵn nguyên liệu</h1>
        <p>Xem lượng cần chuẩn bị, pha theo công thức và xác nhận sản lượng thực tế.</p>
    </div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
</c:if>

<jsp:include page="/WEB-INF/views/layout/_baristaShiftBanner.jsp" />
<jsp:include page="/WEB-INF/views/layout/_handoverPendingAlert.jsp" />

<div class="${onShift ? '' : 'is-viewonly'}">
    <c:if test="${expiredBatchCount > 0}">
        <section class="card prep-expired" style="margin-bottom:var(--s4)">
            <div class="prep-expired__head">
                <div>
                    <h2 style="margin:0;font-size:var(--fs-xl)">Cần loại bỏ</h2>
                    <p class="muted" style="margin:4px 0 0">Mẻ đã quá hạn, kiểm tra thực tế trước khi xác nhận.</p>
                </div>
                <span class="badge badge-cancelled">${expiredBatchCount} mẻ</span>
            </div>
            <div class="prep-expired__list">
                <c:forEach var="batch" items="${expiredBatches}">
                    <div class="prep-expired__item">
                        <div class="prep-expired__main">
                            <strong>${batch.preppedIngredientName}</strong>
                            <span>Mẻ #${batch.prepBatchId} · hết hạn ${batch.expiresAtDisplay}</span>
                        </div>
                        <c:choose>
                            <c:when test="${batch.hasSuggestedWaste}">
                                <form action="${ctx}/barista/prep" method="post" class="prep-expired__form"
                                      onsubmit="return confirm('Xác nhận đã loại bỏ phần nguyên liệu quá hạn này?');">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="writeOffExpired">
                                    <input type="hidden" name="prepBatchId" value="${batch.prepBatchId}">
                                    <button type="submit" class="btn btn-ghost btn-sm">
                                        Xác nhận loại bỏ ${batch.suggestedWasteQuantity} ${batch.preppedIngredientUnit}
                                    </button>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <span class="muted">Không còn tồn để trừ; báo Manager kiểm kê.</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:forEach>
            </div>
        </section>
    </c:if>

    <c:set var="readyCount" value="0" />
    <c:set var="blockedCount" value="0" />
    <c:forEach var="item" items="${checklist}">
        <c:if test="${item.readyToPrep}"><c:set var="readyCount" value="${readyCount + 1}" /></c:if>
        <c:if test="${not item.readyToPrep and (item.needPrep or item.oversold or not item.hasTarget or not item.hasRecipe or not item.hasShelfLife)}">
            <c:set var="blockedCount" value="${blockedCount + 1}" />
        </c:if>
    </c:forEach>

    <section class="card prep-checklist" style="margin-bottom:var(--s4)">
        <div class="prep-checklist__head">
            <div>
                <h2 style="margin:0;font-size:var(--fs-xl)">Cần pha hôm nay</h2>
                <p class="muted" style="margin:4px 0 0">Hệ thống đề xuất khi tồn chạm ngưỡng của chi nhánh.</p>
            </div>
            <span class="badge badge-waiting">${readyCount} việc</span>
        </div>

        <c:choose>
            <c:when test="${readyCount == 0}">
                <p class="muted" style="margin:var(--s3) 0 0">Chưa có nguyên liệu nào cần pha ngay.</p>
            </c:when>
            <c:otherwise>
                <div class="prep-chips">
                    <c:forEach var="item" items="${checklist}">
                        <c:if test="${item.readyToPrep}">
                            <button type="button" class="prep-chip prep-task"
                                    data-id="${item.ingredientId}"
                                    data-name="${fn:escapeXml(item.name)}"
                                    data-unit="${fn:escapeXml(item.unit)}"
                                    data-suggested="${item.suggestedQty}">
                                <strong>${item.name}</strong>
                                <span>Còn ${item.onHandDisplay} · mục tiêu ${item.targetQtyDisplay} ${item.unit}</span>
                                <em style="color:var(--st-ready)">Nên pha ${item.suggestedQtyDisplay} ${item.unit} →</em>
                            </button>
                        </c:if>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>

        <c:if test="${blockedCount > 0}">
            <details style="margin-top:var(--s3)">
                <summary class="muted">${blockedCount} nguyên liệu chưa thể tạo việc pha</summary>
                <div class="prep-chips">
                    <c:forEach var="item" items="${checklist}">
                        <c:if test="${not item.readyToPrep and (item.needPrep or item.oversold or not item.hasTarget or not item.hasRecipe or not item.hasShelfLife)}">
                            <div class="prep-chip prep-chip--norecipe">
                                <strong>${item.name}</strong>
                                <span>Tồn ${item.onHandDisplay} ${item.unit}</span>
                                <em>${item.blockedReason}</em>
                            </div>
                        </c:if>
                    </c:forEach>
                </div>
            </details>
        </c:if>
    </section>

    <section id="prepConfirmCard" class="card prep-card" style="margin-bottom:var(--s4)" hidden>
        <h2 style="margin-top:0;font-size:var(--fs-xl)">Xác nhận đã pha xong</h2>
        <form id="prepConfirmForm" action="${ctx}/barista/prep" method="post">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="createBatch">
            <input type="hidden" name="clientRequestId" value="${clientRequestId}">
            <input id="prepIngredientId" type="hidden" name="preppedIngredientId">
            <div class="prep-row">
                <div class="form-group prep-row__grow" style="margin:0">
                    <label>Nguyên liệu</label>
                    <strong id="prepIngredientName" style="display:block;padding:9px 0"></strong>
                </div>
                <div class="form-group" style="margin:0">
                    <label for="prepQuantity">Sản lượng thực tế</label>
                    <div class="qty-stepper">
                        <input id="prepQuantity" type="number" name="quantityProduced"
                               class="form-control qty-input" min="0.001" step="0.001" required>
                        <span id="prepUnit" class="qty-unit"></span>
                    </div>
                </div>
                <div id="prepPreview" class="prep-row__preview prep-preview"></div>
            </div>
            <div id="prepStockWarn" class="prep-stock-warn" hidden></div>
            <div class="prep-form-foot">
                <button type="button" id="prepCancelChoice" class="btn btn-ghost">Chọn lại</button>
                <button type="submit" id="prepSubmit" class="btn btn-primary">Xác nhận đã pha xong</button>
            </div>
        </form>
    </section>
</div>

<section>
    <div class="prep-checklist__head">
        <div>
            <h2 style="margin:0;font-size:var(--fs-xl)">Mẻ vừa pha</h2>
            <p class="muted" style="margin:4px 0 0">Năm mẻ gần nhất tại chi nhánh. Sai số lượng hãy báo Manager.</p>
        </div>
    </div>
    <table class="table" style="margin-top:var(--s3)">
        <thead><tr><th>#</th><th>Nguyên liệu</th><th>Sản lượng</th><th>Người pha</th><th>Thời gian</th><th>Hạn dùng</th></tr></thead>
        <tbody>
            <c:choose>
                <c:when test="${empty recentBatches}">
                    <tr class="tt-empty"><td colspan="6">Chưa có mẻ pha nào.</td></tr>
                </c:when>
                <c:otherwise>
                    <c:forEach var="batch" items="${recentBatches}">
                        <tr class="${batch.status == 'CANCELLED' ? 'row-muted' : ''}">
                            <td>${batch.prepBatchId}</td>
                            <td>${batch.preppedIngredientName}</td>
                            <td><strong>${batch.quantityProduced}</strong> ${batch.preppedIngredientUnit}</td>
                            <td>${batch.madeByName}</td>
                            <td>${batch.madeAtDisplay}</td>
                            <td>${empty batch.expiresAtDisplay ? '—' : batch.expiresAtDisplay}</td>
                        </tr>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </tbody>
    </table>
</section>

<script>
  (function(){
    var recipes = ${recipeJson};
    var stock = ${empty rawOnHandJson ? '{}' : rawOnHandJson};
    var card = document.getElementById('prepConfirmCard');
    var form = document.getElementById('prepConfirmForm');
    var idInput = document.getElementById('prepIngredientId');
    var nameBox = document.getElementById('prepIngredientName');
    var qtyInput = document.getElementById('prepQuantity');
    var unitBox = document.getElementById('prepUnit');
    var preview = document.getElementById('prepPreview');
    var warning = document.getElementById('prepStockWarn');
    var submit = document.getElementById('prepSubmit');

    function fmt(value){ return Math.round(value * 1000) / 1000; }
    function refreshPreview(){
      var quantity = Number(qtyInput.value);
      var lines = recipes[idInput.value] || [];
      if (!(quantity > 0)){ preview.innerHTML = 'Nhập sản lượng thực tế để xem nguyên liệu cần dùng.'; return; }
      var shortages = [];
      var html = '<div class="prep-preview__title">Nguyên liệu thô sẽ trừ:</div><ul>';
      lines.forEach(function(line){
        var need = quantity / line.y * line.q;
        var have = Number(stock[line.r] || 0);
        if (need > have + 1e-9) shortages.push(line.n + ': cần ' + fmt(need) + ' / còn ' + fmt(Math.max(0, have)) + ' ' + line.u);
        html += '<li class="' + (need > have + 1e-9 ? 'prep-lack' : '') + '">'
             + line.n + ': <strong>' + fmt(need) + ' ' + line.u + '</strong></li>';
      });
      preview.innerHTML = html + '</ul><div class="muted">Hạn dùng được tính tự động theo cấu hình của Admin.</div>';
      warning.hidden = shortages.length === 0;
      warning.textContent = shortages.length ? 'Không đủ nguyên liệu thô: ' + shortages.join('; ') : '';
      submit.disabled = shortages.length > 0;
    }

    document.querySelectorAll('.prep-task').forEach(function(task){
      task.addEventListener('click', function(){
        idInput.value = task.dataset.id;
        nameBox.textContent = task.dataset.name;
        qtyInput.value = task.dataset.suggested;
        unitBox.textContent = task.dataset.unit;
        card.hidden = false;
        refreshPreview();
        card.scrollIntoView({behavior:'smooth', block:'center'});
        qtyInput.focus();
      });
    });
    qtyInput.addEventListener('input', refreshPreview);
    document.getElementById('prepCancelChoice').addEventListener('click', function(){
      card.hidden = true;
      idInput.value = '';
      qtyInput.value = '';
    });
    form.addEventListener('submit', function(){
      submit.disabled = true;
      submit.textContent = 'Đang xác nhận…';
    });
    window.addEventListener('pageshow', function(){
      submit.textContent = 'Xác nhận đã pha xong';
      refreshPreview();
    });
  })();
</script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
