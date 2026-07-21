<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Báo hết món</h1><p>Chỉ dùng cho <strong>sự cố</strong> (máy hỏng, lỗi chất lượng…). Hết nguyên liệu thì kho tự ẩn/hiện món; đồ hỏng thì ghi ở Hao hụt.</p></div>
</div>

<jsp:include page="../layout/_baristaShiftBanner.jsp" />
<nav class="seg" aria-label="Điều hướng báo hết món" style="margin-bottom:var(--s4)"><a class="seg__btn is-active" href="#menu-status">Trạng thái hiện tại</a><a class="seg__btn" href="#menu-history">Lịch sử</a></nav>

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
            Không cần báo tay. Nếu do <strong>nguyên liệu hỏng</strong>, hãy <a href="${ctx}/barista/waste">ghi ở Hao hụt &amp; Làm lại</a> để trừ khỏi sổ kho.
        </div>
    </div>
</c:if>

<div id="menu-status"><c:choose>
    <c:when test="${empty items}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chi nhánh chưa có món nào trên menu.</p></div>
    </c:when>
    <c:otherwise>
        <div class="list-toolbar">
            <input type="search" id="e86Search" class="form-control list-toolbar__search"
                   placeholder="Tìm món…" autocomplete="off" aria-label="Tìm món">
            <div class="seg" role="group" aria-label="Lọc trạng thái">
                <button type="button" class="seg__btn is-active" data-filter="all">Tất cả</button>
                <button type="button" class="seg__btn" data-filter="available">Còn bán</button>
                <button type="button" class="seg__btn" data-filter="out">Đã hết</button>
            </div>
            <span class="list-count"><strong id="e86Count">0</strong> món</span>
        </div>

        <table class="table">
            <thead><tr><th>Món</th><th style="width:190px">Trạng thái</th><th style="width:420px">Thao tác</th></tr></thead>
            <tbody id="e86Body">
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
                <tr id="e86NoResult" style="display:none">
                    <td colspan="3" style="text-align:center;padding:26px" class="muted">Không tìm thấy món phù hợp.</td>
                </tr>
            </tbody>
        </table>

        <div class="pager" id="e86Pager"></div>

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

            var PAGE_SIZE = 10;
            var body = document.getElementById('e86Body');
            if (!body) return;
            var search = document.getElementById('e86Search');
            var pager = document.getElementById('e86Pager');
            var countEl = document.getElementById('e86Count');
            var noRes = document.getElementById('e86NoResult');
            var segBtns = Array.prototype.slice.call(document.querySelectorAll('.seg__btn[data-filter]'));
            var rows = Array.prototype.slice.call(body.querySelectorAll('tr[data-name]'));
            var page = 1, filter = 'all', query = '';

            function norm(s){
              return (s || '').toLowerCase().normalize('NFD')
                     .replace(/[\u0300-\u036f]/g, '').replace(/\u0111/g, 'd');
            }
            rows.forEach(function(r){ r._n = norm(r.getAttribute('data-name')); });

            function matches(r){
              if (filter !== 'all' && r.getAttribute('data-state') !== filter) return false;
              if (query && r._n.indexOf(query) === -1) return false;
              return true;
            }

            function pageWindow(cur, pages){
              if (pages <= 7){ var a=[]; for (var i=1;i<=pages;i++) a.push(i); return a; }
              var out = [1];
              if (cur > 3) out.push('…');
              for (var p = Math.max(2, cur-1); p <= Math.min(pages-1, cur+1); p++) out.push(p);
              if (cur < pages-2) out.push('…');
              out.push(pages);
              return out;
            }

            function renderPager(pages){
              pager.innerHTML = '';
              if (pages <= 1) return;
              function btn(label, target, o){
                o = o || {};
                if (label === '…'){
                  var g = document.createElement('span'); g.className = 'pager__gap'; g.textContent = '…';
                  pager.appendChild(g); return;
                }
                var b = document.createElement('button');
                b.type = 'button';
                b.className = 'pager__btn' + (o.active ? ' is-active' : '');
                b.textContent = label;
                if (o.disabled) b.disabled = true;
                else b.addEventListener('click', function(){ page = target; render(); });
                pager.appendChild(b);
              }
              btn('‹', page - 1, {disabled: page === 1});
              pageWindow(page, pages).forEach(function(p){
                if (p === '…') btn('…');
                else btn(String(p), p, {active: p === page});
              });
              btn('›', page + 1, {disabled: page === pages});
            }

            function render(){
              var matched = rows.filter(matches);
              var total = matched.length;
              var pages = Math.max(1, Math.ceil(total / PAGE_SIZE));
              if (page > pages) page = pages;
              var start = (page - 1) * PAGE_SIZE, end = start + PAGE_SIZE;
              rows.forEach(function(r){ r.style.display = 'none'; });
              matched.forEach(function(r, i){ if (i >= start && i < end) r.style.display = ''; });
              if (noRes) noRes.style.display = total === 0 ? '' : 'none';
              if (countEl) countEl.textContent = total;
              renderPager(pages);
            }

            if (search) search.addEventListener('input', function(){
              query = norm(this.value.trim()); page = 1; render();
            });
            segBtns.forEach(function(b){
              b.addEventListener('click', function(){
                segBtns.forEach(function(x){ x.classList.remove('is-active'); });
                b.classList.add('is-active');
                filter = b.getAttribute('data-filter'); page = 1; render();
              });
            });

            render();
          })();
</script>
<section id="menu-history" class="card" style="margin-top:var(--s4)">
    <details>
        <summary><strong>Lịch sử báo hết món</strong> <span class="muted">(${fn:length(baristaHistory)} sự kiện gần nhất)</span></summary>
        <div class="table-scroll" style="margin-top:12px">
            <table class="table"><thead><tr><th>Thời gian</th><th>Hành động</th><th>Yêu cầu</th><th>Người thực hiện</th><th>Chi tiết</th></tr></thead>
            <tbody>
            <c:forEach var="h" items="${baristaHistory}"><c:if test="${h.entityType == 'MENU_86'}">
                <tr><td>${h.createdAt}</td><td>${h.actionLabel}</td><td>#${h.entityId}</td><td>${h.performedByName}</td>
                    <td><details><summary>Xem</summary><small>Trước: ${h.beforeJson}<br/>Sau: ${h.afterJson}<c:if test="${not empty h.reason}"><br/>Lý do: ${h.reason}</c:if></small></details></td></tr>
            </c:if></c:forEach>
            </tbody></table>
        </div>
    </details>
</section>
    </c:otherwise>
</c:choose></div>

<jsp:include page="../layout/footer.jsp" />
