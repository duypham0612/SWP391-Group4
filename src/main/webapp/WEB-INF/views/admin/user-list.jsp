<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Nhân sự</h1><p>iam.[User]</p></div>
    <a class="btn btn-primary" href="${ctx}/admin/user?action=new">+ Thêm nhân sự</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" />
</c:if>

<form id="staffFilterForm" method="get" action="${ctx}/admin/user" class="table-toolbar">
    <div class="form-group" style="margin:0">
        <label for="fRole">Vai trò</label>
        <select id="fRole" name="roleId" class="form-control tt-filter">
            <option value="">— Tất cả vai trò —</option>
            <c:forEach var="r" items="${roles}">
                <option value="${r.roleId}" <c:if test="${fRoleId == r.roleId}">selected</c:if>>${r.name}</option>
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
    <a id="clearFilters" class="btn btn-ghost" href="${ctx}/admin/user" <c:if test="${empty fRoleId and empty fBranchId and empty q}">style="display:none"</c:if>>Xoá lọc</a>
</form>

<div id="staffResults">
    <c:choose>
        <c:when test="${empty staffList}">
            <div class="card empty-state"><div class="icon">📭</div><p>Chưa có nhân sự nào.</p></div>
        </c:when>
        <c:otherwise>
            <table class="table">
                <thead><tr>
                    <th style="width:60px">#</th><th>Tên đăng nhập</th><th>Họ tên</th><th>Vai trò</th>
                    <th>Chi nhánh</th><th style="width:110px">Trạng thái</th><th style="width:90px">Thao tác</th>
                </tr></thead>
                <tbody>
                    <c:forEach var="s" items="${staffList}" varStatus="st">
                        <tr>
                            <td>${rowStart + st.index + 1}</td>
                            <td>${s.username}</td>
                            <td>${s.fullName}</td>
                            <td>${s.roleName}</td>
                            <td><c:choose><c:when test="${empty s.branchName}"><span class="muted">(toàn chuỗi)</span></c:when><c:otherwise>${s.branchName}</c:otherwise></c:choose></td>
                            <td><c:choose><c:when test="${s.status == 'ACTIVE'}"><span class="badge badge-ready">ACTIVE</span></c:when><c:otherwise><span class="badge badge-cancelled">LOCKED</span></c:otherwise></c:choose></td>
                            <td>
                                <c:choose>
                                    <c:when test="${s.roleCode == 'ADMIN'}">
                                        <span class="muted" title="Tài khoản Admin hệ thống — không thể sửa/khoá">🔒 Admin hệ thống</span>
                                    </c:when>
                                    <c:otherwise>
                                        <a class="btn btn-ghost btn-sm" href="${ctx}/admin/user?action=edit&id=${s.userId}">Sửa</a>
                                        <form action="${ctx}/admin/user" method="post" style="display:inline"
                                              onsubmit="return confirm('Đổi trạng thái tài khoản này?');">
                                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="action" value="toggleStatus">
                                            <input type="hidden" name="id" value="${s.userId}">
                                            <input type="hidden" name="current" value="${s.status}">
                                            <button type="submit" class="btn btn-ghost btn-sm">
                                                <c:choose><c:when test="${s.status == 'ACTIVE'}">Khoá</c:when><c:otherwise>Mở khoá</c:otherwise></c:choose>
                                            </button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            <c:if test="${totalPages > 1}">
                <div class="pagination" style="margin-top:16px">
                    <c:if test="${page > 1}">
                        <c:url var="prevUrl" value="/admin/user">
                            <c:if test="${not empty fRoleId}"><c:param name="roleId" value="${fRoleId}" /></c:if>
                            <c:if test="${not empty fBranchId}"><c:param name="branchId" value="${fBranchId}" /></c:if>
                            <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
                            <c:param name="page" value="${page - 1}" />
                        </c:url>
                        <a class="page" href="${prevUrl}">‹</a>
                    </c:if>
                    <span class="muted" style="align-self:center">Trang ${page}/${totalPages} · ${total} tài khoản</span>
                    <c:if test="${page < totalPages}">
                        <c:url var="nextUrl" value="/admin/user">
                            <c:if test="${not empty fRoleId}"><c:param name="roleId" value="${fRoleId}" /></c:if>
                            <c:if test="${not empty fBranchId}"><c:param name="branchId" value="${fBranchId}" /></c:if>
                            <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
                            <c:param name="page" value="${page + 1}" />
                        </c:url>
                        <a class="page" href="${nextUrl}">›</a>
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
        if (role && role.value) params.set('roleId', role.value);
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
