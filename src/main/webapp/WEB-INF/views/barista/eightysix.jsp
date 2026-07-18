<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Báo hết món</h1><p>Tắt món tạm thời — khoá khỏi POS và menu QR của khách</p></div>
</div>

<jsp:include page="../layout/_baristaShiftBanner.jsp" />

<c:if test="${not empty suggest86}">
    <div class="alert alert-warn" style="display:block">
        <strong>Gợi ý báo hết — có nguyên liệu đã cạn:</strong>
        <div style="display:flex;flex-wrap:wrap;gap:8px;margin-top:8px">
            <c:forEach var="s" items="${suggest86}">
                <form action="${ctx}/barista/eightysix" method="post" style="display:flex;align-items:center;gap:6px;margin:0">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="toggle86">
                    <input type="hidden" name="productId" value="${s.productId}">
                    <input type="hidden" name="is86" value="true">
                    <span class="badge badge-waiting">${s.productName} · hết ${s.ingredientName}</span>
                    <button type="submit" class="btn btn-sm btn-ghost" ${onShift ? '' : 'disabled'}>Báo tạm hết</button>
                </form>
            </c:forEach>
        </div>
        <div class="muted" style="font-size:.82em;margin-top:8px">Chỉ là gợi ý — bạn tự quyết định báo hết. Hệ thống không tự khoá món.</div>
    </div>
</c:if>

<c:choose>
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
            <thead><tr><th>Món</th><th style="width:140px">Trạng thái</th><th style="width:160px"></th></tr></thead>
            <tbody id="e86Body">
                <c:forEach var="m" items="${items}">
                    <c:if test="${m.published}">
                        <c:set var="imgSrc" value="${empty m.imageUrl ? ctx.concat('/assets/img/products/_placeholder.svg') : (m.imageUrl.startsWith('http') ? m.imageUrl : ctx.concat(m.imageUrl))}" />
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
                                    </c:when>
                                    <c:otherwise><span class="badge badge-ready">Còn bán</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${m.is86}">
                                        <form action="${ctx}/barista/eightysix" method="post">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="action" value="toggle86">
                                            <input type="hidden" name="productId" value="${m.productId}">
                                            <input type="hidden" name="is86" value="false">
                                            <button type="submit" class="btn btn-sm btn-primary" ${onShift ? '' : 'disabled'}>Mở bán lại</button>
                                        </form>
                                    </c:when>
                                    <c:otherwise>
                                        <form action="${ctx}/barista/eightysix" method="post" style="display:flex;gap:6px;align-items:center;flex-wrap:wrap;margin:0">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="action" value="toggle86">
                                            <input type="hidden" name="productId" value="${m.productId}">
                                            <input type="hidden" name="is86" value="true">
                                            <input type="datetime-local" name="backInEta" class="form-control" style="width:185px" title="Dự kiến có lại (tuỳ chọn)" ${onShift ? '' : 'disabled'}>
                                            <button type="submit" class="btn btn-sm btn-ghost" ${onShift ? '' : 'disabled'}>Báo tạm hết</button>
                                        </form>
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

        <script>
          (function(){
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

            // Bỏ dấu tiếng Việt để tìm không phân biệt dấu ("ca phe" khớp "Cà Phê")
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
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
