<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Tra cứu công thức</h1><p>Xem định mức nguyên liệu thô / nguyên liệu pha sẵn &amp; tác động của modifier — chỉ đọc</p></div>
</div>

<div class="card form-card recipe-filter" style="margin-bottom:18px">
    <form id="recipeFilterForm" action="${ctx}/barista/recipe" method="get" class="recipe-filter__form">
        <input type="hidden" name="filter" value="1">
        <div class="form-group recipe-filter__search"><label for="q">Tìm món</label>
            <input id="q" type="search" name="q" class="form-control" value="<c:out value='${q}'/>" placeholder="Nhập tên món..." autocomplete="off" enterkeyhint="search" aria-describedby="recipeSearchHint">
            <small id="recipeSearchHint">Kết quả tự cập nhật khi bạn ngừng gõ.</small>
        </div>
        <div class="form-group recipe-filter__select">
            <label for="fCategory">Nhóm món</label>
            <select id="fCategory" name="categoryId" class="form-control">
                <option value="">Tất cả nhóm</option>
                <c:forEach var="cat" items="${categories}">
                    <option value="${cat.categoryId}" <c:if test="${fCategoryId == cat.categoryId}">selected</c:if>><c:out value="${cat.name}"/></option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group recipe-filter__select">
            <label for="fRecipeState">Công thức</label>
            <select id="fRecipeState" name="recipeState" class="form-control">
                <option value="">Tất cả</option>
                <option value="HAS" <c:if test="${fRecipeState == 'HAS'}">selected</c:if>>Đã có công thức</option>
                <option value="NONE" <c:if test="${fRecipeState == 'NONE'}">selected</c:if>>Chưa có công thức</option>
            </select>
        </div>
        <label class="recipe-filter__branch">
            <input id="fBranchOnly" type="checkbox" name="branchOnly" value="1" <c:if test="${fBranchOnly}">checked</c:if>>
            Chỉ món chi nhánh tôi
        </label>
        <a id="clearFilters" class="btn btn-ghost" href="${ctx}/barista/recipe"
           <c:if test="${empty q and empty fCategoryId and empty fRecipeState and fBranchOnly}">style="display:none"</c:if>>Xoá lọc</a>
    </form>
</div>

<p id="recipeLiveStatus" class="visually-hidden" role="status" aria-live="polite"></p>

<div class="recipe-layout">
    <section id="recipeResults" class="card recipe-results-card" aria-labelledby="recipeResultsTitle">
        <div class="recipe-card__head">
            <div><div class="eyebrow">Danh sách</div><h2 id="recipeResultsTitle">Món (${total})</h2></div>
            <span class="recipe-count"><c:out value="${total}"/> món</span>
        </div>
        <c:choose>
            <c:when test="${empty products}">
                <div class="empty-state"><div class="icon">∅</div><p>Không có món phù hợp.</p></div>
            </c:when>
            <c:otherwise>
                <div class="recipe-table-wrap">
                <table class="table recipe-results-table">
                    <thead><tr><th>Món</th><th style="width:90px"></th></tr></thead>
                    <tbody>
                        <c:forEach var="p" items="${products}">
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
                            <tr>
                                <td>
                                    <a class="recipe-product-link" href="${viewUrl}" data-recipe-select
                                       <c:if test="${selected.productId == p.productId}">aria-current="true"</c:if>>
                                        <strong class="recipe-product-name"><c:out value="${p.name}"/></strong><br>
                                        <small><c:out value="${p.categoryName}"/></small>
                                    </a>
                                </td>
                                <td>
                                    <a class="btn btn-ghost btn-sm" href="${viewUrl}" data-recipe-select aria-label="Xem công thức: <c:out value='${p.name}'/>">Xem</a>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                </div>
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
    </section>

    <article id="recipeDetail" class="card recipe-detail-card" aria-live="polite" aria-atomic="false">
        <c:choose>
            <c:when test="${empty selected}">
                <div class="empty-state"><div class="icon">☕</div><p><c:choose><c:when test="${not empty recipeLookupNotice}"><c:out value="${recipeLookupNotice}"/></c:when><c:otherwise>Chọn một món để xem công thức.</c:otherwise></c:choose></p></div>
            </c:when>
            <c:otherwise>
                <div class="recipe-detail-card__head">
                    <div class="eyebrow">Công thức chuẩn</div>
                    <h2><c:out value="${selected.name}"/></h2>
                    <p>Định mức cho 1 phần; đối chiếu modifier trước khi pha.</p>
                </div>

                <section class="recipe-section" aria-labelledby="baseRecipeTitle">
                    <h3 id="baseRecipeTitle">Định mức / 1 phần</h3>
                    <c:choose>
                        <c:when test="${empty recipe}">
                            <p class="recipe-muted">Chưa khai báo công thức cho món này.</p>
                        </c:when>
                        <c:otherwise>
                            <div class="recipe-table-wrap"><table class="table">
                                <thead><tr><th>Nguyên liệu</th><th style="width:120px">Định mức</th><th style="width:110px">Loại</th></tr></thead>
                                <tbody>
                                    <c:forEach var="r" items="${recipe}">
                                        <tr>
                                            <td><c:out value="${r.ingredientName}"/></td>
                                            <td class="recipe-qty"><strong><c:out value="${r.quantity}"/></strong> <c:out value="${r.ingredientUnit}"/></td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${r.ingredientType == 'PREPPED'}"><span class="badge recipe-badge-prepped">Pha sẵn</span></c:when>
                                                    <c:otherwise><span class="badge recipe-badge-raw">Thô</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table></div>
                        </c:otherwise>
                    </c:choose>
                </section>

                <c:if test="${not empty preps}">
                    <section class="recipe-section" aria-labelledby="prepRecipeTitle">
                        <h3 id="prepRecipeTitle">Định mức pha sẵn</h3>
                        <p class="recipe-section__hint">Nguyên liệu thô cần dùng để tạo đồ nền.</p>
                        <c:forEach var="ps" items="${preps}">
                            <div class="recipe-prep-card">
                                <div class="recipe-prep-card__head"><strong><c:out value="${ps.name}"/></strong><span class="recipe-prep-card__unit">Đơn vị thành phẩm: <c:out value="${ps.unit}"/></span></div>
                                <div class="recipe-table-wrap"><table class="table">
                                    <thead><tr><th>Nguyên liệu thô</th><th style="width:150px">Lượng dùng</th><th style="width:150px">Sản lượng</th></tr></thead>
                                    <tbody>
                                        <c:forEach var="l" items="${ps.lines}">
                                            <tr><td><c:out value="${l.rawIngredientName}"/></td><td class="recipe-qty"><c:out value="${l.quantity}"/> <c:out value="${l.rawIngredientUnit}"/></td><td class="recipe-qty"><strong><c:out value="${l.yieldDisplay}"/></strong> <c:out value="${ps.unit}"/></td></tr>
                                        </c:forEach>
                                    </tbody>
                                </table></div>
                            </div>
                        </c:forEach>
                    </section>
                </c:if>

                <section class="recipe-section" aria-labelledby="modifierImpactTitle">
                    <h3 id="modifierImpactTitle">Tác động của modifier</h3>
                    <p class="recipe-section__hint">Chỉ hiển thị lựa chọn đang hoạt động trên menu bán hàng.</p>
                    <c:choose>
                        <c:when test="${empty impacts}">
                            <p class="recipe-muted">Modifier không ảnh hưởng định mức (hoặc chưa khai báo).</p>
                        </c:when>
                        <c:otherwise>
                            <div class="recipe-table-wrap"><table class="table">
                                <thead><tr><th>Nhóm</th><th>Tuỳ chọn</th><th>Nguyên liệu</th><th style="width:130px">Thay đổi</th></tr></thead>
                                <tbody>
                                    <c:forEach var="im" items="${impacts}">
                                        <tr>
                                            <td><c:out value="${im.groupName}"/></td>
                                            <td><c:out value="${im.optionName}"/></td>
                                            <td><c:out value="${im.ingredientName}"/></td>
                                            <td class="recipe-impact-delta"><strong><c:if test="${im.qtyDelta > 0}">+</c:if><c:out value="${im.qtyDelta}"/></strong> <c:out value="${im.ingredientUnit}"/></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table></div>
                        </c:otherwise>
                    </c:choose>
                </section>
            </c:otherwise>
        </c:choose>
    </article>
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
    var liveStatus = document.getElementById('recipeLiveStatus');
    var timer = null;
    var controller = null;
    var isComposing = false;
    var LIVE_SEARCH_DELAY = 250;
    var cache = new Map();
    var requestedUrl = null;
    var CACHE_LIMIT = 24;
    var CACHE_TTL = 10000;

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

    function cacheResponse(url, html) {
        if (cache.has(url)) cache.delete(url);
        cache.set(url, {html: html, expiresAt: Date.now() + CACHE_TTL});
        if (cache.size > CACHE_LIMIT) cache.delete(cache.keys().next().value);
    }

    function updateContent(html, url, push) {
        var doc = new DOMParser().parseFromString(html, 'text/html');
        var nextResults = doc.getElementById('recipeResults');
        var nextDetail = doc.getElementById('recipeDetail');
        if (!nextResults || !nextDetail) {
            window.location.href = url;
            return false;
        }
        results.innerHTML = nextResults.innerHTML;
        detail.innerHTML = nextDetail.innerHTML;
        if (push) window.history.replaceState(null, '', url);
        syncClear();
        if (liveStatus) {
            var heading = nextResults.querySelector('h2');
            liveStatus.textContent = heading ? heading.textContent.trim() + ' đã được cập nhật.' : 'Kết quả tra cứu đã được cập nhật.';
        }
        return true;
    }

    function load(url, push) {
        if (requestedUrl === url) return;
        var cached = cache.get(url);
        if (cached && cached.expiresAt > Date.now()) {
            if (controller) controller.abort();
            controller = null;
            requestedUrl = null;
            form.classList.remove('is-live-searching');
            results.removeAttribute('aria-busy');
            detail.removeAttribute('aria-busy');
            updateContent(cached.html, url, push);
            return;
        }
        if (cached) cache.delete(url);
        if (controller) controller.abort();
        var requestController = new AbortController();
        controller = requestController;
        requestedUrl = url;
        form.classList.add('is-live-searching');
        results.setAttribute('aria-busy', 'true');
        detail.setAttribute('aria-busy', 'true');
        results.style.opacity = '.55';
        detail.style.opacity = '.55';
        fetch(url, {headers:{'X-Requested-With':'recipe-lookup'}, signal:requestController.signal})
            .then(function(resp){
                if (!resp.ok) throw new Error('Không tải được công thức.');
                return resp.text();
            })
            .then(function(html){
                // Request cũ có thể hoàn tất sau khi người dùng đã gõ tiếp.
                // Không cho phép nó ghi đè kết quả mới hơn.
                if (controller !== requestController) return;
                cacheResponse(url, html);
                updateContent(html, url, push);
            })
            .catch(function(err){
                if (err.name !== 'AbortError') window.location.href = url;
            })
            .finally(function(){
                if (controller !== requestController) return;
                form.classList.remove('is-live-searching');
                requestedUrl = null;
                results.removeAttribute('aria-busy');
                detail.removeAttribute('aria-busy');
                results.style.opacity = '';
                detail.style.opacity = '';
            });
    }

    function schedule() {
        if (isComposing) return;
        clearTimeout(timer);
        if (liveStatus) {
            var term = q && q.value.trim();
            liveStatus.textContent = term ? 'Đang tìm món ' + term + '…' : 'Đang tải lại danh sách món…';
        }
        timer = setTimeout(function(){ load(buildUrl(1), true); }, LIVE_SEARCH_DELAY);
    }

    form.addEventListener('submit', function(e){
        e.preventDefault();
        load(buildUrl(1), true);
    });
    if (q) {
        q.addEventListener('compositionstart', function(){ isComposing = true; clearTimeout(timer); });
        q.addEventListener('compositionend', function(){ isComposing = false; schedule(); });
        q.addEventListener('input', schedule);
        q.addEventListener('keydown', function(e){
            if (e.key === 'Escape' && q.value) {
                e.preventDefault();
                q.value = '';
                schedule();
            }
        });
    }
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
        // Xem công thức cần luôn dùng điều hướng chuẩn: thao tác này vẫn hoạt động
        // khi trình duyệt chặn hoặc huỷ một request tải nền trước đó.
        if (a.hasAttribute('data-recipe-select')) return;
        e.preventDefault();
        load(a.href, true);
    });
    syncClear();
})();
</script>

<jsp:include page="../layout/footer.jsp" />
