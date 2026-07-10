<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Tuỳ chọn (Modifier)</h1><p>Nhóm tuỳ chọn → option → định mức nguyên liệu</p></div>
    <a class="btn btn-primary" href="${ctx}/admin/modifier?view=group">+ Thêm nhóm</a>
</div>

<c:if test="${not empty flashOk}"><div class="alert alert-success">${flashOk}</div></c:if>
<c:if test="${not empty flashError}"><div class="alert alert-error">${flashError}</div></c:if>

<c:choose>
    <c:when test="${empty groups}">
        <div class="card empty-state"><div class="icon">🎚️</div>
            <p>Chưa có nhóm tuỳ chọn nào (vd Size, Sữa, Đường, Topping).</p>
            <a class="btn btn-primary" href="${ctx}/admin/modifier?view=group">+ Tạo nhóm đầu tiên</a>
        </div>
    </c:when>
    <c:otherwise>
        <div class="list-toolbar">
            <input type="search" id="mgSearch" class="form-control list-toolbar__search"
                   placeholder="Tìm theo tên nhóm..." autocomplete="off" aria-label="Tìm nhóm tuỳ chọn">
            <div class="seg" role="group" aria-label="Lọc loại nhóm">
                <button type="button" class="seg__btn is-active" data-filter="all">Tất cả</button>
                <button type="button" class="seg__btn" data-filter="required">Bắt buộc</button>
                <button type="button" class="seg__btn" data-filter="optional">Tuỳ chọn</button>
            </div>
            <span class="list-count"><strong id="mgCount">0</strong> nhóm</span>
        </div>

        <table class="table">
            <thead><tr>
                <th style="width:56px">#</th><th>Tên nhóm</th><th style="width:230px">Quy tắc chọn</th>
                <th style="width:120px">Option</th><th style="width:130px">Sản phẩm dùng</th>
                <th style="width:150px">Thao tác</th>
            </tr></thead>
            <tbody id="mgBody">
                <c:forEach var="g" items="${groups}">
                    <tr data-state="${g.required ? 'required' : 'optional'}">
                        <td>${g.modifierGroupId}</td>
                        <td>
                            <a style="font-weight:600" href="${ctx}/admin/modifier?view=group&groupId=${g.modifierGroupId}">${g.name}</a>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${g.required}"><span class="badge badge-making">Bắt buộc</span></c:when>
                                <c:otherwise><span class="badge badge-served">Tuỳ chọn</span></c:otherwise>
                            </c:choose>
                            <span class="muted" style="margin-left:6px">
                                <c:choose>
                                    <c:when test="${g.minSelect == g.maxSelect}">Chọn đúng ${g.maxSelect}</c:when>
                                    <c:when test="${g.minSelect == 0}">Tối đa ${g.maxSelect}</c:when>
                                    <c:otherwise>Chọn ${g.minSelect}–${g.maxSelect}</c:otherwise>
                                </c:choose>
                            </span>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${g.optionCount > 0}"><span class="chip">${g.optionCount} option</span></c:when>
                                <c:otherwise><span class="muted">— chưa có —</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${g.productCount > 0}">${g.productCount} sản phẩm</c:when>
                                <c:otherwise><span class="muted">0</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <a class="btn btn-ghost btn-sm" href="${ctx}/admin/modifier?view=group&groupId=${g.modifierGroupId}">Mở</a>
                            <form action="${ctx}/admin/modifier" method="post" style="display:inline"
                                  onsubmit="return confirm('Xoá nhóm này? Mọi option + định mức + gán sản phẩm của nhóm sẽ bị xoá.');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="deleteGroup">
                                <input type="hidden" name="groupId" value="${g.modifierGroupId}">
                                <button type="submit" class="btn btn-ghost btn-sm" style="color:var(--st-cancelled)">Xoá</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <tr id="mgNoResult" style="display:none">
                    <td colspan="6" style="text-align:center;padding:26px" class="muted">Không tìm thấy nhóm phù hợp.</td>
                </tr>
            </tbody>
        </table>

        <nav class="pager" id="mgPager" aria-label="Phân trang nhóm modifier"></nav>

        <script>
        (function(){
            var PER_PAGE = 8;
            var body = document.getElementById('mgBody');
            if (!body) return;
            var search = document.getElementById('mgSearch');
            var countEl = document.getElementById('mgCount');
            var noRes = document.getElementById('mgNoResult');
            var pager = document.getElementById('mgPager');
            var segBtns = Array.prototype.slice.call(document.querySelectorAll('.seg__btn[data-filter]'));
            var rows = Array.prototype.slice.call(body.querySelectorAll('tr[data-state]'));
            var filter = 'all';
            var query = '';
            var page = 1;

            function norm(s) {
                return (s || '').toLowerCase().normalize('NFD')
                    .replace(/[\u0300-\u036f]/g, '').replace(/\u0111/g, 'd');
            }

            rows.forEach(function(row){
                var cells = row.children;
                row._search = norm((cells[1] ? cells[1].textContent : '') + ' ' + (cells[2] ? cells[2].textContent : ''));
            });

            function matches(row) {
                if (filter !== 'all' && row.getAttribute('data-state') !== filter) return false;
                if (query && row._search.indexOf(query) === -1) return false;
                return true;
            }

            function pagerBtn(label, target, opts) {
                opts = opts || {};
                var b = document.createElement('button');
                b.type = 'button';
                b.className = 'pager__btn' + (opts.active ? ' is-active' : '');
                b.textContent = label;
                if (opts.disabled) { b.disabled = true; }
                else if (!opts.active) { b.addEventListener('click', function(){ page = target; render(); }); }
                return b;
            }

            function renderPager(pageCount) {
                if (!pager) return;
                pager.innerHTML = '';
                if (pageCount <= 1) return;
                pager.appendChild(pagerBtn('‹', page - 1, { disabled: page === 1 }));
                for (var p = 1; p <= pageCount; p++) pager.appendChild(pagerBtn(String(p), p, { active: p === page }));
                pager.appendChild(pagerBtn('›', page + 1, { disabled: page === pageCount }));
            }

            function render() {
                var matched = rows.filter(matches);
                var total = matched.length;
                var pageCount = Math.max(1, Math.ceil(total / PER_PAGE));
                if (page > pageCount) page = pageCount;
                if (page < 1) page = 1;
                var start = (page - 1) * PER_PAGE;
                rows.forEach(function(row){ row.style.display = 'none'; });
                matched.slice(start, start + PER_PAGE).forEach(function(row){ row.style.display = ''; });
                if (countEl) countEl.textContent = total;
                if (noRes) noRes.style.display = total === 0 ? '' : 'none';
                renderPager(pageCount);
            }

            if (search) search.addEventListener('input', function(){ query = norm(this.value.trim()); page = 1; render(); });
            segBtns.forEach(function(btn){
                btn.addEventListener('click', function(){
                    segBtns.forEach(function(x){ x.classList.remove('is-active'); });
                    btn.classList.add('is-active');
                    filter = btn.getAttribute('data-filter');
                    page = 1;
                    render();
                });
            });
            render();
        })();
        </script>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
