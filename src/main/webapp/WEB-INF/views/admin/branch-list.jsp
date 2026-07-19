<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Chi nhánh</h1><p>org.Branch</p></div>
    <a class="btn btn-primary" href="${ctx}/admin/branch?action=new">+ Thêm chi nhánh</a>
</div>

<c:choose>
    <c:when test="${empty branches}">
        <div class="card empty-state"><div class="icon">📭</div><p>Chưa có chi nhánh nào.</p></div>
    </c:when>
    <c:otherwise>
        <div class="table-toolbar">
            <div class="form-group table-search">
                <label for="branchSearch">Tìm kiếm</label>
                <input type="search" id="branchSearch" class="form-control"
                       placeholder="Tìm mã, tên, địa chỉ, quản lý..." autocomplete="off" aria-label="Tìm chi nhánh">
            </div>
            <div class="seg" role="group" aria-label="Lọc trạng thái">
                <button type="button" class="seg__btn is-active" data-filter="all">Tất cả</button>
                <button type="button" class="seg__btn" data-filter="active">Hoạt động</button>
                <button type="button" class="seg__btn" data-filter="inactive">Ngừng</button>
            </div>
            <span class="tt-summary"><strong id="branchCount">0</strong> chi nhánh</span>
        </div>

        <table class="table">
            <thead><tr>
                <th style="width:60px">#</th><th style="width:100px">Mã</th><th>Tên</th><th>Địa chỉ</th>
                <th style="width:120px">Giờ mở cửa</th><th>Quản lý</th>
                <th style="width:110px">Trạng thái</th><th style="width:170px">Thao tác</th>
            </tr></thead>
            <tbody id="branchBody">
                <c:forEach var="b" items="${branches}">
                    <tr data-state="${b.active ? 'active' : 'inactive'}">
                        <td>${b.branchId}</td>
                        <td>${b.code}</td>
                        <td>${b.name}</td>
                        <td>${b.address}</td>
                        <td><c:choose><c:when test="${not empty b.hoursText}">${b.hoursText}</c:when><c:otherwise><span class="muted">—</span></c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${not empty b.managerName}">${b.managerName}</c:when><c:otherwise><span class="muted">(chưa gán)</span></c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${b.active}"><span class="badge badge-ready">Hoạt động</span></c:when><c:otherwise><span class="badge badge-cancelled">Ngừng</span></c:otherwise></c:choose></td>
                        <td>
                            <a class="btn btn-ghost btn-sm" href="${ctx}/admin/branch?action=edit&id=${b.branchId}">Sửa</a>
                            <form action="${ctx}/admin/branch" method="post" style="display:inline" onsubmit="return confirm('Đổi trạng thái chi nhánh này?');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="toggleActive">
                                <input type="hidden" name="id" value="${b.branchId}">
                                <button type="submit" class="btn btn-ghost btn-sm">${b.active ? 'Ngừng' : 'Bật'}</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <tr id="branchNoResult" style="display:none">
                    <td colspan="8" style="text-align:center;padding:26px" class="muted">Không tìm thấy chi nhánh phù hợp.</td>
                </tr>
            </tbody>
        </table>

        <nav class="pagination" id="branchPager" aria-label="Phân trang chi nhánh"></nav>

        <script>
        (function(){
            var PER_PAGE = 5;
            var body = document.getElementById('branchBody');
            if (!body) return;
            var search = document.getElementById('branchSearch');
            var countEl = document.getElementById('branchCount');
            var noRes = document.getElementById('branchNoResult');
            var pager = document.getElementById('branchPager');
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
                row._search = norm([
                    cells[1] ? cells[1].textContent : '',
                    cells[2] ? cells[2].textContent : '',
                    cells[3] ? cells[3].textContent : '',
                    cells[5] ? cells[5].textContent : ''
                ].join(' '));
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
                b.className = 'page' + (opts.active ? ' is-active' : '');
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
                for (var p = 1; p <= pageCount; p++) {
                    pager.appendChild(pagerBtn(String(p), p, { active: p === page }));
                }
                pager.appendChild(pagerBtn('›', page + 1, { disabled: page === pageCount }));
            }

            function render() {
                var matched = rows.filter(matches);
                var total = matched.length;
                var pageCount = Math.max(1, Math.ceil(total / PER_PAGE));
                if (page > pageCount) page = pageCount;
                if (page < 1) page = 1;
                var start = (page - 1) * PER_PAGE;
                var end = start + PER_PAGE;

                rows.forEach(function(row){ row.style.display = 'none'; });
                matched.slice(start, end).forEach(function(row, index){
                    row.style.display = '';
                    if (row.cells[0]) row.cells[0].textContent = String(start + index + 1);
                });

                if (countEl) countEl.textContent = total;
                if (noRes) noRes.style.display = total === 0 ? '' : 'none';
                renderPager(pageCount);
            }

            if (search) {
                search.addEventListener('input', function(){
                    query = norm(this.value.trim());
                    page = 1;
                    render();
                });
            }

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
