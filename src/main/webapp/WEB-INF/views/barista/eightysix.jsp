<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Báo hết món</h1><p>Chỉ dùng cho <strong>sự cố</strong> (máy hỏng, lỗi chất lượng…). Hết nguyên liệu thì kho tự ẩn/hiện món; đồ hỏng thì ghi ở Hao hụt.</p></div>
</div>

<jsp:include page="/WEB-INF/views/layout/_baristaShiftBanner.jsp" />

<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>

<c:if test="${not empty suggest86}">
    <div class="alert alert-warn" style="display:block">
        <strong>Tự hết theo kho — đã ẩn khỏi POS/QR:</strong>
        <div style="display:flex;flex-wrap:wrap;gap:8px;margin-top:8px">
            <c:forEach var="s" items="${suggest86}">
                <span class="badge badge-cancelled">${s.productName} · hết ${s.ingredientName}</span>
            </c:forEach>
        </div>
        <div class="muted" style="font-size:.82em;margin-top:8px">
            Những món này đã <strong>tự động ẩn</strong> vì hết nguyên liệu và sẽ <strong>tự hiện lại</strong> khi có tồn (nhập kho / pha mẻ mới).
            Không cần báo tay. Nếu do <strong>nguyên liệu hỏng</strong>, hãy <a href="${ctx}/barista/waste">ghi ở Hao hụt nguyên liệu</a> để trừ khỏi sổ kho.
        </div>
    </div>
</c:if>

<form action="${ctx}/barista/eightysix" method="get" class="list-toolbar" id="e86FilterForm">
    <input type="search" name="q" class="form-control list-toolbar__search"
           value="<c:out value='${filterQuery}'/>" placeholder="Tìm món…" autocomplete="off" aria-label="Tìm món">
    <div class="form-group" style="margin:0;min-width:150px">
        <label for="e86State">Trạng thái</label>
        <select id="e86State" name="state" class="form-control">
            <option value="" ${empty filterState ? 'selected' : ''}>Tất cả</option>
            <option value="available" ${filterState == 'available' ? 'selected' : ''}>Còn bán</option>
            <option value="out" ${filterState == 'out' ? 'selected' : ''}>Đã hết</option>
        </select>
    </div>
    <div class="form-group" style="margin:0;min-width:110px">
        <label for="e86PageSize">Hiển thị</label>
        <select id="e86PageSize" name="pageSize" class="form-control">
            <option value="10" ${menuPage.pageSize == 10 ? 'selected' : ''}>10</option>
            <option value="20" ${menuPage.pageSize == 20 ? 'selected' : ''}>20</option>
            <option value="50" ${menuPage.pageSize == 50 ? 'selected' : ''}>50</option>
        </select>
    </div>
    <button type="submit" class="btn btn-primary btn-sm">Lọc</button>
    <c:if test="${not empty filterQuery or not empty filterState or menuPage.pageSize != 10}">
        <a class="btn btn-ghost btn-sm" href="${ctx}/barista/eightysix">Xóa lọc</a>
    </c:if>
    <span class="list-count"><strong>${menuPage.total}</strong> món</span>
</form>

<c:choose>
    <c:when test="${empty items}">
        <div class="card empty-state"><div class="icon">∅</div><p>Không tìm thấy món phù hợp.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th>Món</th><th style="width:190px">Trạng thái</th><th style="width:420px">Thao tác</th></tr></thead>
            <tbody>
                <c:forEach var="m" items="${items}">
                    <c:if test="${m.published}">
                        <c:set var="imgSrc" value="${empty m.imageUrl ? ctx.concat('/assets/img/products/_placeholder.svg') : (m.imageUrl.startsWith('http') ? m.imageUrl : ctx.concat(m.imageUrl))}" />
                        <c:set var="openReq" value="${openRequests[m.productId]}" />
                        <tr data-name="${m.productName}" data-state="${m.is86 ? 'out' : 'available'}">
                            <td style="display:flex;align-items:center;gap:10px">
                                <img class="prod-thumb" src="${imgSrc}" alt="${m.productName}" loading="lazy" onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'">
                                ${m.productName}
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${m.is86}">
                                        <span class="badge badge-cancelled">Tạm hết</span>
                                        <c:if test="${not empty m.backInEtaText}">
                                            <div class="muted" style="font-size:.82em;margin-top:4px">Có lại: ${m.backInEtaText}</div>
                                        </c:if>
                                        <c:if test="${not empty openReq}">
                                            <div class="muted" style="font-size:.82em;margin-top:4px">${openReq.statusLabel}</div>
                                        </c:if>
                                    </c:when>
                                    <c:otherwise><span class="badge badge-ready">Còn bán</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${m.is86}">
                                        <c:choose>
                                            <c:when test="${not empty openReq && not empty openReq.reopenRequestedAt}">
                                                <div class="muted" style="font-size:.9em">Đã gửi yêu cầu mở bán — chờ quản lý</div>
                                                <button type="button" class="btn btn-sm btn-ghost" disabled>Đang chờ duyệt</button>
                                            </c:when>
                                            <c:otherwise>
                                                <form action="${ctx}/barista/eightysix" method="post" style="margin:0">
                                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                    <input type="hidden" name="action" value="askReopen">
                                                    <input type="hidden" name="productId" value="${m.productId}">
                                                    <input type="hidden" name="q" value="<c:out value='${filterQuery}'/>">
                                                    <input type="hidden" name="state" value="${filterState}">
                                                    <input type="hidden" name="page" value="${menuPage.page}">
                                                    <input type="hidden" name="pageSize" value="${menuPage.pageSize}">
                                                    <button type="submit" class="btn btn-sm btn-primary" ${onShift ? '' : 'disabled'}>Xin mở bán lại</button>
                                                </form>
                                            </c:otherwise>
                                        </c:choose>
                                    </c:when>
                                    <c:otherwise>
                                        <details class="report86" data-product-id="${m.productId}">
                                            <summary class="btn btn-sm btn-ghost">Báo tạm hết</summary>
                                            <form action="${ctx}/barista/eightysix" method="post" class="report86-form">
                                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                <input type="hidden" name="action" value="report86">
                                                <input type="hidden" name="productId" value="${m.productId}">
                                                <input type="hidden" name="q" value="<c:out value='${filterQuery}'/>">
                                                <input type="hidden" name="state" value="${filterState}">
                                                <input type="hidden" name="page" value="${menuPage.page}">
                                                <input type="hidden" name="pageSize" value="${menuPage.pageSize}">
                                                <select name="reasonCode" class="form-control reason-select" required ${onShift ? '' : 'disabled'}>
                                                    <option value="">Chọn lý do</option>
                                                    <c:forEach var="r" items="${reasons}">
                                                        <option value="${r.code}" data-quick-notes='${r.quickNotesJson}'>${r.label}</option>
                                                    </c:forEach>
                                                </select>
                                                <div class="chips quick-note-chips" style="display:flex;gap:6px;flex-wrap:wrap"></div>
                                                <input name="note" class="form-control note-input" maxlength="255" placeholder="Ghi chú" ${onShift ? '' : 'disabled'}>
                                                <label class="muted" style="font-size:.82em">Dự kiến có lại (nếu ước lượng được — sự cố bất định có thể bỏ trống)</label>
                                                <input type="datetime-local" name="backInEta" class="form-control"
                                                       min="${etaMin}" max="${etaMax}" ${onShift ? '' : 'disabled'}>
                                                <button type="submit" class="btn btn-sm btn-primary" ${onShift ? '' : 'disabled'}>Báo tạm hết</button>
                                            </form>
                                        </details>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:if>
                </c:forEach>
            </tbody>
        </table>

        <div class="table-tools-foot">
            <span class="tt-summary" aria-live="polite">
                ${menuPage.startRow}-${menuPage.endRow} / ${menuPage.total} món · trang ${menuPage.page}/${menuPage.totalPages}
            </span>
            <c:if test="${menuPage.totalPages > 1}">
                <div class="pagination" aria-label="Phân trang danh sách món">
                    <c:url var="firstE86PageUrl" value="/barista/eightysix">
                        <c:param name="q" value="${filterQuery}" /><c:param name="state" value="${filterState}" />
                        <c:param name="pageSize" value="${menuPage.pageSize}" /><c:param name="page" value="1" />
                    </c:url>
                    <c:url var="previousE86PageUrl" value="/barista/eightysix">
                        <c:param name="q" value="${filterQuery}" /><c:param name="state" value="${filterState}" />
                        <c:param name="pageSize" value="${menuPage.pageSize}" /><c:param name="page" value="${menuPage.page - 1}" />
                    </c:url>
                    <a class="page" href="${firstE86PageUrl}" aria-disabled="${not menuPage.hasPrevious}" title="Trang đầu">«</a>
                    <a class="page" href="${previousE86PageUrl}" aria-disabled="${not menuPage.hasPrevious}" title="Trang trước">‹</a>
                    <c:forEach var="pageNumber" items="${menuPage.visiblePages}">
                        <c:url var="e86PageUrl" value="/barista/eightysix">
                            <c:param name="q" value="${filterQuery}" /><c:param name="state" value="${filterState}" />
                            <c:param name="pageSize" value="${menuPage.pageSize}" /><c:param name="page" value="${pageNumber}" />
                        </c:url>
                        <a class="page ${pageNumber == menuPage.page ? 'is-active' : ''}" href="${e86PageUrl}"
                           aria-current="${pageNumber == menuPage.page ? 'page' : 'false'}">${pageNumber}</a>
                    </c:forEach>
                    <c:url var="nextE86PageUrl" value="/barista/eightysix">
                        <c:param name="q" value="${filterQuery}" /><c:param name="state" value="${filterState}" />
                        <c:param name="pageSize" value="${menuPage.pageSize}" /><c:param name="page" value="${menuPage.page + 1}" />
                    </c:url>
                    <c:url var="lastE86PageUrl" value="/barista/eightysix">
                        <c:param name="q" value="${filterQuery}" /><c:param name="state" value="${filterState}" />
                        <c:param name="pageSize" value="${menuPage.pageSize}" /><c:param name="page" value="${menuPage.totalPages}" />
                    </c:url>
                    <a class="page" href="${nextE86PageUrl}" aria-disabled="${not menuPage.hasNext}" title="Trang sau">›</a>
                    <a class="page" href="${lastE86PageUrl}" aria-disabled="${not menuPage.hasNext}" title="Trang cuối">»</a>
                </div>
            </c:if>
        </div>

        <style>
          .report86 summary{display:inline-flex;list-style:none;cursor:pointer}
          .report86 summary::-webkit-details-marker{display:none}
          .report86-form{margin-top:10px;display:grid;gap:8px;max-width:360px}
          .quick-note-chip.is-selected{background:var(--brand);color:#fff;border-color:var(--brand)}
        </style>
        <script>
          (function(){
            function renderChips(box){
              var select = box.querySelector('.reason-select');
              var chips = box.querySelector('.quick-note-chips');
              var note = box.querySelector('.note-input');
              if (!select || !chips || !note) return;
              var opt = select.options[select.selectedIndex];
              var notes = [];
              try { notes = JSON.parse(opt ? (opt.getAttribute('data-quick-notes') || '[]') : '[]'); } catch(e) {}
              chips.innerHTML = '';
              note.minLength = select.value === 'OTHER' ? 10 : 0;
              if (select.value === 'OTHER') {
                chips.style.display = 'none';
                note.focus();
                return;
              }
              chips.style.display = notes.length ? 'flex' : 'none';
              notes.forEach(function(text){
                var b = document.createElement('button');
                b.type = 'button';
                b.className = 'btn btn-sm btn-ghost quick-note-chip';
                b.textContent = text;
                b.addEventListener('click', function(){
                  b.classList.toggle('is-selected');
                  var selected = Array.prototype.slice.call(chips.querySelectorAll('.is-selected')).map(function(x){ return x.textContent; });
                  note.value = selected.join(' · ');
                });
                chips.appendChild(b);
              });
            }

            document.querySelectorAll('.report86').forEach(function(box){
              var select = box.querySelector('.reason-select');
              var note = box.querySelector('.note-input');
              if (select) select.addEventListener('change', function(){ renderChips(box); });
              if (note) note.addEventListener('input', function(){
                box.querySelectorAll('.quick-note-chip.is-selected').forEach(function(b){ b.classList.remove('is-selected'); });
              });
              renderChips(box);
            });

          })();
        </script>
    </c:otherwise>
</c:choose>

<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
