<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="cssBundles" value="kds,list-controls" scope="request" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Nhân sự</h1><p>Quản lý tài khoản, vai trò và chi nhánh làm việc.</p></div>
    <a class="btn btn-primary" href="${ctx}/admin/user?action=new">+ Thêm nhân sự</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error"><c:out value="${sessionScope.flashError}"/></div><c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success"><c:out value="${sessionScope.flashOk}"/></div><c:remove var="flashOk" scope="session" />
</c:if>

<form id="staffFilterForm" method="get" action="${ctx}/admin/user" class="table-toolbar">
    <div class="form-group" style="margin:0">
        <label for="fRole">Vai trò</label>
        <select id="fRole" name="roleCode" class="form-control tt-filter">
            <option value="">— Tất cả vai trò —</option>
            <c:forEach var="r" items="${roles}">
                <option value="${r.code}" <c:if test="${fRoleCode == r.code}">selected</c:if>>${r.name}</option>
            </c:forEach>
        </select>
    </div>
    <div class="form-group" style="margin:0">
        <label for="fBranch">Chi nhánh</label>
        <select id="fBranch" name="branchId" class="form-control tt-filter">
            <option value="">— Tất cả chi nhánh —</option>
            <c:forEach var="b" items="${branches}">
                <option value="${b.branchId}" <c:if test="${fBranchId == b.branchId}">selected</c:if>>${b.name}</option>
            </c:forEach>
        </select>
    </div>
    <div class="form-group table-search" style="margin:0">
        <label for="q">Tìm kiếm</label>
        <input id="q" name="q" class="form-control"
               placeholder="Tên, tài khoản, email, SĐT..." value="${q}">
    </div>
    <button type="submit" class="btn btn-ghost">Lọc</button>
    <a id="clearFilters" class="btn btn-ghost" href="${ctx}/admin/user" <c:if test="${empty fRoleCode and empty fBranchId and empty q}">style="display:none"</c:if>>Xoá lọc</a>
</form>

<div id="staffResults">
    <c:choose>
        <c:when test="${empty staffList}">
            <div class="card empty-state"><div class="icon">📭</div><p>Chưa có nhân sự nào.</p></div>
        </c:when>
        <c:otherwise>
            <div class="table-scroll">
            <table class="table admin-user-table">
                <colgroup>
                    <col style="width:48px">
                    <col style="width:13%">
                    <col style="width:15%">
                    <col style="width:20%">
                    <col style="width:12%">
                    <col style="width:15%">
                    <col style="width:104px">
                    <col style="width:90px">
                </colgroup>
                <thead><tr>
                    <th>STT</th>
                    <th>Tên đăng nhập</th>
                    <th>Họ tên</th>
                    <th>Liên hệ</th>
                    <th>Vai trò</th>
                    <th>Chi nhánh</th>
                    <th>Trạng thái</th>
                    <th>Thao tác</th>
                </tr></thead>
                <tbody>
                    <c:forEach var="s" items="${staffList}" varStatus="st">
                        <tr>
                            <td>${rowStart + st.index + 1}</td>
                            <td>${s.username}</td>
                            <td>${s.fullName}</td>
                            <td>
                                <div class="staff-contact">
                                    <span title="${fn:escapeXml(s.email)}"><c:out value="${s.email}"/></span>
                                    <span title="${fn:escapeXml(s.phone)}"><c:out value="${s.phone}"/></span>
                                </div>
                            </td>
                            <td><span class="badge badge-served"><c:out value="${s.roleName}"/></span></td>
                            <td><c:choose><c:when test="${empty s.branchName}"><span class="muted">(toàn chuỗi)</span></c:when><c:otherwise><c:out value="${s.branchName}"/><c:if test="${s.branchActive == false}"> <span class="badge badge-cancelled">Ngừng</span></c:if></c:otherwise></c:choose></td>
                            <td><c:choose><c:when test="${s.status == 'ACTIVE'}"><span class="badge badge-ready">Hoạt động</span></c:when><c:otherwise><span class="badge badge-cancelled">Đã khóa</span></c:otherwise></c:choose></td>
                            <td>
                                <c:choose>
                                    <c:when test="${s.roleCode == 'ADMIN'}">
                                        <span class="badge badge-served" title="Tài khoản Admin hệ thống không thể sửa hoặc khoá">Admin</span>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="row-actions">
                                        <a class="icon-btn user-action-btn" href="${ctx}/admin/user?action=edit&id=${s.userId}"
                                           aria-label="Sửa tài khoản ${fn:escapeXml(s.fullName)}" title="Sửa tài khoản">
                                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                                <path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L8 18l-4 1 1-4Z"/>
                                            </svg>
                                        </a>
                                        <form action="${ctx}/admin/user" method="post" style="display:inline"
                                              onsubmit="return confirm('${s.status == 'ACTIVE' ? 'Khóa tài khoản này? Tài khoản sẽ bị đăng xuất và không thể đăng nhập.' : 'Mở khóa tài khoản này?'}');">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="action" value="toggleStatus">
                                            <input type="hidden" name="id" value="${s.userId}">
                                            <input type="hidden" name="current" value="${s.status}">
                                            <button type="submit" class="icon-btn user-action-btn user-action-btn--status"
                                                    <c:if test="${s.assignedBranchManager}">disabled</c:if>
                                                    aria-label="${s.status == 'ACTIVE' ? 'Khóa tài khoản' : 'Mở khóa tài khoản'} ${fn:escapeXml(s.fullName)}"
                                                    title="${s.assignedBranchManager ? 'Hãy thay quản lý phụ trách trước khi khóa' : (s.status == 'ACTIVE' ? 'Khóa tài khoản' : 'Mở khóa tài khoản')}">
                                                <c:choose>
                                                    <c:when test="${s.status == 'ACTIVE'}">
                                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                                            <rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>
                                                        </svg>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                                            <rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 7.5-2"/>
                                                        </svg>
                                                    </c:otherwise>
                                                </c:choose>
                                            </button>
                                        </form>
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            </div>
            <c:if test="${totalPages > 1}">
                <div class="pagination" style="margin-top:16px">
                    <c:if test="${page > 1}">
                        <c:url var="prevUrl" value="/admin/user">
                            <c:if test="${not empty fRoleCode}"><c:param name="roleCode" value="${fRoleCode}" /></c:if>
                            <c:if test="${not empty fBranchId}"><c:param name="branchId" value="${fBranchId}" /></c:if>
                            <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
                            <c:param name="page" value="${page - 1}" />
                        </c:url>
                        <a class="page" href="${prevUrl}" aria-label="Trang trước" title="Trang trước">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 18l-6-6 6-6"/></svg>
                        </a>
                    </c:if>
                    <span class="muted" style="align-self:center">Trang ${page}/${totalPages} · ${total} tài khoản</span>
                    <c:if test="${page < totalPages}">
                        <c:url var="nextUrl" value="/admin/user">
                            <c:if test="${not empty fRoleCode}"><c:param name="roleCode" value="${fRoleCode}" /></c:if>
                            <c:if test="${not empty fBranchId}"><c:param name="branchId" value="${fBranchId}" /></c:if>
                            <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
                            <c:param name="page" value="${page + 1}" />
                        </c:url>
                        <a class="page" href="${nextUrl}" aria-label="Trang sau" title="Trang sau">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18l6-6-6-6"/></svg>
                        </a>
                    </c:if>
                </div>
            </c:if>
        </c:otherwise>
    </c:choose>
</div>

<script>
(function(){
    var form = document.getElementById('staffFilterForm');
    var results = document.getElementById('staffResults');
    var q = document.getElementById('q');
    var role = document.getElementById('fRole');
    var branch = document.getElementById('fBranch');
    var clear = document.getElementById('clearFilters');
    var timer = null;
    var controller = null;

    function buildUrl(page) {
        var params = new URLSearchParams();
        if (role && role.value) params.set('roleCode', role.value);
        if (branch && branch.value) params.set('branchId', branch.value);
        if (q && q.value.trim()) params.set('q', q.value.trim());
        if (page && page > 1) params.set('page', page);
        var query = params.toString();
        return form.action + (query ? '?' + query : '');
    }

    function syncClear() {
        if (!clear) return;
        var hasFilter = (role && role.value) || (branch && branch.value) || (q && q.value.trim());
        clear.style.display = hasFilter ? '' : 'none';
    }

    function load(url, push) {
        if (controller) controller.abort();
        controller = new AbortController();
        results.style.opacity = '.55';
        fetch(url, {headers:{'X-Requested-With':'fetch'}, signal:controller.signal})
            .then(function(resp){ return resp.text(); })
            .then(function(html){
                var doc = new DOMParser().parseFromString(html, 'text/html');
                var nextResults = doc.getElementById('staffResults');
                if (!nextResults) {
                    window.location.href = url;
                    return;
                }
                results.innerHTML = nextResults.innerHTML;
                if (push) window.history.replaceState(null, '', url);
                syncClear();
            })
            .catch(function(err){
                if (err.name !== 'AbortError') window.location.href = url;
            })
            .finally(function(){ results.style.opacity = ''; });
    }

    function schedule() {
        clearTimeout(timer);
        timer = setTimeout(function(){ load(buildUrl(1), true); }, 300);
    }

    form.addEventListener('submit', function(e){
        e.preventDefault();
        load(buildUrl(1), true);
    });
    if (q) q.addEventListener('input', schedule);
    if (role) role.addEventListener('change', function(){ load(buildUrl(1), true); });
    if (branch) branch.addEventListener('change', function(){ load(buildUrl(1), true); });
    if (clear) clear.addEventListener('click', function(e){
        e.preventDefault();
        if (role) role.value = '';
        if (branch) branch.value = '';
        if (q) q.value = '';
        load(form.action, true);
    });
    results.addEventListener('click', function(e){
        var a = e.target.closest('a');
        if (!a || a.href.indexOf(form.action) !== 0 || a.href.indexOf('action=edit') !== -1) return;
        e.preventDefault();
        load(a.href, true);
    });
    syncClear();
})();
</script>

<jsp:include page="../layout/footer.jsp" />
