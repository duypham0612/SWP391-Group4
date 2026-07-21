<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Tra cứu công thức</h1><p>Xem định mức nguyên liệu thô và nguyên liệu pha sẵn — chỉ đọc</p></div>
</div>

<div class="card form-card" style="margin-bottom:18px">
    <form id="recipeFilterForm" action="${ctx}/barista/recipe" method="get" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
        <input type="hidden" name="filter" value="1">
        <div class="form-group" style="margin:0;flex:1;min-width:220px"><label for="q">Tìm món</label>
            <input id="q" type="text" name="q" class="form-control" value="${q}" placeholder="Nhập tên món...">
        </div>
        <div class="form-group" style="margin:0;min-width:190px">
            <label for="fCategory">Nhóm món</label>
            <select id="fCategory" name="categoryId" class="form-control">
                <option value="">Tất cả nhóm</option>
                <c:forEach var="cat" items="${categories}">
                    <option value="${cat.categoryId}" <c:if test="${fCategoryId == cat.categoryId}">selected</c:if>>${cat.name}</option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group" style="margin:0;min-width:190px">
            <label for="fRecipeState">Công thức</label>
            <select id="fRecipeState" name="recipeState" class="form-control">
                <option value="">Tất cả</option>
                <option value="HAS" <c:if test="${fRecipeState == 'HAS'}">selected</c:if>>Đã có công thức</option>
                <option value="NONE" <c:if test="${fRecipeState == 'NONE'}">selected</c:if>>Chưa có công thức</option>
            </select>
        </div>
        <label style="display:flex;align-items:center;gap:8px;margin-bottom:10px;white-space:nowrap">
            <input id="fBranchOnly" type="checkbox" name="branchOnly" value="1" <c:if test="${fBranchOnly}">checked</c:if>>
            Chỉ món chi nhánh tôi
        </label>
        <noscript><button type="submit" class="btn btn-ghost">Lọc</button></noscript>
        <a id="clearFilters" class="btn btn-ghost" href="${ctx}/barista/recipe"
           <c:if test="${empty q and empty fCategoryId and empty fRecipeState and fBranchOnly}">style="display:none"</c:if>>Xoá lọc</a>
    </form>
</div>

<div class="recipe-layout">
    <div id="recipeResults" class="card recipe-results-card">
        <h3 style="margin-top:0">Món (${total})</h3>
        <c:choose>
            <c:when test="${empty products}">
                <div class="empty-state"><div class="icon">∅</div><p>Không có món phù hợp.</p></div>
            </c:when>
            <c:otherwise>
                <table class="table recipe-product-table">
                    <thead><tr><th>Món</th><th></th></tr></thead>
                    <tbody>
                        <c:forEach var="p" items="${products}">
                            <tr>
                                <td><span class="recipe-product-name">${p.name}</span><small>${p.categoryName}</small></td>
                                <td>
                                    <c:url var="viewUrl" value="/barista/recipe">
                                        <c:param name="productId" value="${p.productId}" />
                                        <c:if test="${not empty q or not empty fCategoryId or not empty fRecipeState or not fBranchOnly}">
                                            <c:param name="filter" value="1" />
                                        </c:if>
                                        <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
                                        <c:if test="${not empty fCategoryId}"><c:param name="categoryId" value="${fCategoryId}" /></c:if>
                                        <c:if test="${not empty fRecipeState}"><c:param name="recipeState" value="${fRecipeState}" /></c:if>
                                        <c:if test="${fBranchOnly}"><c:param name="branchOnly" value="1" /></c:if>
                                        <c:if test="${page > 1}"><c:param name="page" value="${page}" /></c:if>
                                    </c:url>
                                    <a class="btn btn-ghost btn-sm" href="${viewUrl}">Xem</a>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                <c:if test="${totalPages > 1}">
                    <div class="pagination" style="justify-content:center;margin-top:16px">
                        <c:if test="${page > 1}">
                            <c:url var="prevUrl" value="/barista/recipe">
                                <c:if test="${not empty q or not empty fCategoryId or not empty fRecipeState or not fBranchOnly}">
                                    <c:param name="filter" value="1" />
                                </c:if>
                                <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
                                <c:if test="${not empty fCategoryId}"><c:param name="categoryId" value="${fCategoryId}" /></c:if>
                                <c:if test="${not empty fRecipeState}"><c:param name="recipeState" value="${fRecipeState}" /></c:if>
                                <c:if test="${fBranchOnly}"><c:param name="branchOnly" value="1" /></c:if>
                                <c:param name="page" value="${page - 1}" />
                            </c:url>
                            <a class="page" href="${prevUrl}">Trước</a>
                        </c:if>
                        <c:forEach var="pnum" begin="1" end="${totalPages}">
                            <c:choose>
                                <c:when test="${pnum == page}">
                                    <span class="page is-active" aria-current="page">${pnum}</span>
                                </c:when>
                                <c:otherwise>
                                    <c:url var="pageUrl" value="/barista/recipe">
                                        <c:if test="${not empty q or not empty fCategoryId or not empty fRecipeState or not fBranchOnly}">
                                            <c:param name="filter" value="1" />
                                        </c:if>
                                        <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
                                        <c:if test="${not empty fCategoryId}"><c:param name="categoryId" value="${fCategoryId}" /></c:if>
                                        <c:if test="${not empty fRecipeState}"><c:param name="recipeState" value="${fRecipeState}" /></c:if>
                                        <c:if test="${fBranchOnly}"><c:param name="branchOnly" value="1" /></c:if>
                                        <c:if test="${pnum > 1}"><c:param name="page" value="${pnum}" /></c:if>
                                    </c:url>
                                    <a class="page" href="${pageUrl}">${pnum}</a>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                        <span class="muted" style="align-self:center">· ${total} món</span>
                        <c:if test="${page < totalPages}">
                            <c:url var="nextUrl" value="/barista/recipe">
                                <c:if test="${not empty q or not empty fCategoryId or not empty fRecipeState or not fBranchOnly}">
                                    <c:param name="filter" value="1" />
                                </c:if>
                                <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
                                <c:if test="${not empty fCategoryId}"><c:param name="categoryId" value="${fCategoryId}" /></c:if>
                                <c:if test="${not empty fRecipeState}"><c:param name="recipeState" value="${fRecipeState}" /></c:if>
                                <c:if test="${fBranchOnly}"><c:param name="branchOnly" value="1" /></c:if>
                                <c:param name="page" value="${page + 1}" />
                            </c:url>
                            <a class="page" href="${nextUrl}">Sau</a>
                        </c:if>
                    </div>
                </c:if>
            </c:otherwise>
        </c:choose>
    </div>

    <div id="recipeDetail" class="card recipe-detail-card">
        <c:choose>
            <c:when test="${empty selected}">
                <div class="empty-state"><div class="icon">☕</div><p>Chọn một món để xem công thức.</p></div>
            </c:when>
            <c:otherwise>
                <h3 style="margin-top:0">${selected.name}</h3>

                <h4>Định mức / 1 phần</h4>
                <c:choose>
                    <c:when test="${empty recipe}">
                        <p style="color:var(--muted)">Chưa khai báo công thức cho món này.</p>
                    </c:when>
                    <c:otherwise>
                        <table class="table">
                            <thead><tr><th>Nguyên liệu</th><th style="width:120px">Định mức</th><th style="width:110px">Loại</th></tr></thead>
                            <tbody>
                                <c:forEach var="r" items="${recipe}">
                                    <tr>
                                        <td>${r.ingredientName}</td>
                                        <td><strong>${r.quantity}</strong> ${r.ingredientUnit}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${r.ingredientType == 'PREPPED'}"><span class="badge" style="background:var(--latte)">Pha sẵn</span></c:when>
                                                <c:otherwise><span class="badge">Thô</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </c:otherwise>
                </c:choose>

                <c:if test="${not empty preps}">
                    <h4>Định mức pha sẵn</h4>
                    <c:forEach var="ps" items="${preps}">
                        <p style="margin:6px 0"><strong>${ps.name}</strong></p>
                        <table class="table">
                            <thead><tr><th>Nguyên liệu thô</th><th style="width:120px">Dùng</th><th style="width:120px">Sản lượng (yield)</th></tr></thead>
                            <tbody>
                                <c:forEach var="l" items="${ps.lines}">
                                    <tr><td>${l.rawIngredientName}</td><td>${l.quantity} ${l.rawIngredientUnit}</td><td>${l.yieldQty}</td></tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </c:forEach>
                </c:if>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<script>
(function(){
    var form = document.getElementById('recipeFilterForm');
    var results = document.getElementById('recipeResults');
    var detail = document.getElementById('recipeDetail');
    var q = document.getElementById('q');
    var category = document.getElementById('fCategory');
    var recipeState = document.getElementById('fRecipeState');
    var branchOnly = document.getElementById('fBranchOnly');
    var clear = document.getElementById('clearFilters');
    var timer = null;
    var controller = null;

    function hasActiveFilter() {
        return (q && q.value.trim()) ||
            (category && category.value) ||
            (recipeState && recipeState.value) ||
            (branchOnly && !branchOnly.checked);
    }

    function buildUrl(page) {
        var params = new URLSearchParams();
        if (hasActiveFilter() || (branchOnly && branchOnly.checked)) params.set('filter', '1');
        if (q && q.value.trim()) params.set('q', q.value.trim());
        if (category && category.value) params.set('categoryId', category.value);
        if (recipeState && recipeState.value) params.set('recipeState', recipeState.value);
        if (branchOnly && branchOnly.checked) params.set('branchOnly', '1');
        if (page && page > 1) params.set('page', page);
        var query = params.toString();
        return form.action + (query ? '?' + query : '');
    }

    function syncClear() {
        if (!clear) return;
        clear.style.display = hasActiveFilter() ? '' : 'none';
    }

    function load(url, push) {
        if (controller) controller.abort();
        controller = new AbortController();
        results.style.opacity = '.55';
        detail.style.opacity = '.55';
        fetch(url, {headers:{'X-Requested-With':'fetch'}, signal:controller.signal})
            .then(function(resp){ return resp.text(); })
            .then(function(html){
                var doc = new DOMParser().parseFromString(html, 'text/html');
                var nextResults = doc.getElementById('recipeResults');
                var nextDetail = doc.getElementById('recipeDetail');
                if (!nextResults || !nextDetail) {
                    window.location.href = url;
                    return;
                }
                results.innerHTML = nextResults.innerHTML;
                detail.innerHTML = nextDetail.innerHTML;
                if (push) window.history.replaceState(null, '', url);
                syncClear();
            })
            .catch(function(err){
                if (err.name !== 'AbortError') window.location.href = url;
            })
            .finally(function(){
                results.style.opacity = '';
                detail.style.opacity = '';
            });
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
    if (category) category.addEventListener('change', function(){ load(buildUrl(1), true); });
    if (recipeState) recipeState.addEventListener('change', function(){ load(buildUrl(1), true); });
    if (branchOnly) branchOnly.addEventListener('change', function(){ load(buildUrl(1), true); });
    if (clear) clear.addEventListener('click', function(e){
        e.preventDefault();
        if (q) q.value = '';
        if (category) category.value = '';
        if (recipeState) recipeState.value = '';
        if (branchOnly) branchOnly.checked = true;
        load(form.action, true);
    });
    results.addEventListener('click', function(e){
        var a = e.target.closest('a');
        if (!a || a.href.indexOf(form.action) !== 0) return;
        e.preventDefault();
        load(a.href, true);
    });
    syncClear();
})();
</script>

<jsp:include page="../layout/footer.jsp" />
