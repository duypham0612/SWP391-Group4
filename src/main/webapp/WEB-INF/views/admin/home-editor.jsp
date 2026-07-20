<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <div class="eyebrow">Trang công khai</div>
        <h1>Chỉnh sửa trang Home</h1>
        <p>Quản lý nội dung và món nổi bật trên trang Home.</p>
    </div>
    <a class="btn btn-ghost" href="${ctx}/home" target="_blank" rel="noopener">Xem trang Home ↗</a>
</div>

<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success"><c:out value="${sessionScope.flashOk}"/></div><c:remove var="flashOk" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error"><c:out value="${sessionScope.flashError}"/></div><c:remove var="flashError" scope="session" />
</c:if>

<%-- ===== Nội dung hero ===== --%>
<c:set var="heroImg" value="${empty setting.heroImageUrl ? ctx.concat('/assets/img/login-hero.svg') : (fn:startsWith(fn:toLowerCase(setting.heroImageUrl), 'http') ? setting.heroImageUrl : ctx.concat(setting.heroImageUrl))}" />
<div class="card form-card">
    <h2 style="margin-top:0">Nội dung phần đầu trang (hero)</h2>
    <div class="grid-2">
        <form action="${ctx}/admin/home" method="post">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="saveContent">
            <div class="form-group">
                <label for="heroEyebrow">Dòng nhỏ phía trên (eyebrow)</label>
                <input id="heroEyebrow" type="text" name="heroEyebrow" class="form-control" maxlength="150" value="${fn:escapeXml(setting.heroEyebrow)}">
            </div>
            <div class="form-group">
                <label for="heroTitle">Tiêu đề chính *</label>
                <input id="heroTitle" type="text" name="heroTitle" class="form-control" maxlength="200" value="${fn:escapeXml(setting.heroTitle)}" required>
            </div>
            <div class="form-group">
                <label for="heroSubtitle">Mô tả</label>
                <textarea id="heroSubtitle" name="heroSubtitle" class="form-control" rows="3" maxlength="500"><c:out value="${setting.heroSubtitle}"/></textarea>
            </div>
            <div class="form-group">
                <label for="heroImageUrl">Ảnh hero (URL hoặc /assets/...)</label>
                <input id="heroImageUrl" type="text" name="heroImageUrl" class="form-control" maxlength="500" value="${fn:escapeXml(setting.heroImageUrl)}"
                       placeholder="/assets/img/login-hero.svg hoặc https://...">
                <small class="muted">Để trống = dùng ảnh mặc định. Dán link ảnh trên mạng (https://…) hoặc đường dẫn nội bộ (/assets/…).</small>
            </div>
            <button type="submit" class="btn btn-primary btn-lg">Lưu nội dung</button>
        </form>

        <%-- Xem trước trực tiếp (cập nhật khi gõ) --%>
        <div>
            <label class="muted">Xem trước (cập nhật khi gõ)</label>
            <div class="hero-preview" id="heroPreview" style="margin-top:8px">
                <div class="img-preview" style="margin-bottom:10px">
                    <img id="pvImg" src="${heroImg}" alt="Xem trước hero" data-ctx="${ctx}"
                         data-default="${ctx}/assets/img/login-hero.svg"
                         onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'">
                </div>
                <div class="eyebrow" id="pvEyebrow">${fn:escapeXml(setting.heroEyebrow)}</div>
                <h3 id="pvTitle" style="margin:2px 0">${fn:escapeXml(setting.heroTitle)}</h3>
                <p class="muted" id="pvSubtitle" style="margin:0"><c:out value="${setting.heroSubtitle}"/></p>
            </div>
        </div>
    </div>
</div>

<%-- ===== Chọn món hiển thị + thứ tự ===== --%>
<div class="alert alert-info">
    Tick <strong>Hiện trên Home</strong> để khách nhìn thấy món; bỏ tick để ẩn (món vẫn bán bình thường ở POS/QR).
    <strong>Thứ tự</strong> nhỏ hơn hiển thị trước trong cùng danh mục. Sửa nhiều dòng rồi bấm <strong>Lưu tất cả</strong> một lần.
    Chỉ liệt kê món <strong>đang bán</strong> (IsActive).
</div>

<c:choose>
    <c:when test="${empty products}">
        <div class="card empty-state"><div class="icon">☕</div><p>Chưa có sản phẩm nào đang bán để hiển thị.</p></div>
    </c:when>
    <c:otherwise>
        <div class="list-toolbar">
            <input type="search" id="prodSearch" class="form-control list-toolbar__search"
                   placeholder="Tìm món theo tên..." autocomplete="off" aria-label="Tìm món">
            <div class="seg" role="group" aria-label="Lọc hiển thị Home">
                <button type="button" class="seg__btn is-active" data-filter="all">Tất cả</button>
                <button type="button" class="seg__btn" data-filter="shown">Đang hiện</button>
                <button type="button" class="seg__btn" data-filter="hidden">Đang ẩn</button>
            </div>
            <span class="list-count"><strong id="prodCount">0</strong> món</span>
        </div>

        <form action="${ctx}/admin/home" method="post">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="saveHomeProducts">
            <table class="table">
                <thead><tr>
                    <th>Sản phẩm</th>
                    <th style="width:280px">Hiển thị trên Home &amp; thứ tự</th>
                </tr></thead>
                <tbody id="prodBody">
                    <c:set var="lastCat" value="@@none@@" />
                    <c:forEach var="p" items="${products}">
                        <c:if test="${p.categoryName != lastCat}">
                            <tr data-cat-header><td colspan="2" style="background:var(--surface-2);font-weight:600"><c:out value="${p.categoryName}"/></td></tr>
                            <c:set var="lastCat" value="${p.categoryName}" />
                        </c:if>
                        <c:set var="imgSrc" value="${empty p.imageUrl ? ctx.concat('/assets/img/products/_placeholder.svg') : (fn:startsWith(fn:toLowerCase(p.imageUrl), 'http') ? p.imageUrl : ctx.concat(p.imageUrl))}" />
                        <tr data-pid="${p.productId}" data-state="${p.showOnHome ? 'shown' : 'hidden'}" data-name="${fn:escapeXml(fn:toLowerCase(p.name))}">
                            <td style="display:flex;align-items:center;gap:10px">
                                <img class="prod-thumb" src="${imgSrc}" alt="${fn:escapeXml(p.name)}" loading="lazy" onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'">
                                <span><c:out value="${p.name}"/>
                                    <c:if test="${not p.showOnHome}"><span class="badge badge-served" style="margin-left:6px" data-hidden-badge>Đang ẩn</span></c:if>
                                </span>
                                <span style="margin-left:auto"><fmt:formatNumber value="${p.basePrice}" maxFractionDigits="0"/> ₫</span>
                            </td>
                            <td>
                                <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
                                    <input type="hidden" name="pid" value="${p.productId}">
                                    <label style="margin:0;display:flex;align-items:center;gap:6px">
                                        <input type="checkbox" name="show_${p.productId}" value="1" <c:if test="${p.showOnHome}">checked</c:if>> Hiện trên Home
                                    </label>
                                    <input type="number" name="order_${p.productId}" class="form-control" style="width:90px" min="0" step="1"
                                           value="${p.homeSortOrder}" title="Thứ tự (nhỏ = trước)">
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    <tr id="prodNoResult" style="display:none"><td colspan="2" style="text-align:center;padding:24px" class="muted">Không tìm thấy món phù hợp.</td></tr>
                </tbody>
            </table>

            <div class="save-bar">
                <span class="muted">Thay đổi tick/thứ tự các món rồi bấm lưu — áp dụng cho toàn bộ danh sách.</span>
                <button type="submit" class="btn btn-primary btn-lg">Lưu tất cả</button>
            </div>
        </form>
    </c:otherwise>
</c:choose>

<style>
.hero-preview{border:1px solid var(--line);border-radius:var(--radius);padding:14px;background:var(--surface-2)}
.save-bar{display:flex;justify-content:space-between;align-items:center;gap:16px;flex-wrap:wrap;
    position:sticky;bottom:0;background:var(--surface);border-top:1px solid var(--line);padding:12px 4px;margin-top:8px}
</style>

<script>
(function(){
    // ----- Live preview hero -----
    var img = document.getElementById('pvImg');
    function bind(id, target, isImg) {
        var el = document.getElementById(id), out = document.getElementById(target);
        if (!el) return;
        el.addEventListener('input', function(){
            var v = el.value.trim();
            if (isImg) {
                if (!v) v = img.getAttribute('data-default');
                else if (!/^https?:\/\//i.test(v)) v = img.getAttribute('data-ctx') + v;
                out.src = v;
            } else {
                out.textContent = v;
            }
        });
    }
    bind('heroEyebrow', 'pvEyebrow', false);
    bind('heroTitle', 'pvTitle', false);
    bind('heroSubtitle', 'pvSubtitle', false);
    bind('heroImageUrl', 'pvImg', true);

    // ----- Live search + filter danh sách món -----
    var body = document.getElementById('prodBody');
    if (!body) return;
    var search = document.getElementById('prodSearch');
    var countEl = document.getElementById('prodCount');
    var noRes = document.getElementById('prodNoResult');
    var segBtns = Array.prototype.slice.call(document.querySelectorAll('.seg__btn[data-filter]'));
    var rows = Array.prototype.slice.call(body.querySelectorAll('tr[data-pid]'));
    var filter = 'all';
    var query = '';

    function norm(s) {
        return (s || '').toLowerCase().normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '').replace(/\u0111/g, 'd');
    }
    rows.forEach(function(r){ r._search = norm(r.getAttribute('data-name')); });

    function matchRow(r) {
        if (filter !== 'all' && r.getAttribute('data-state') !== filter) return false;
        if (query && r._search.indexOf(query) === -1) return false;
        return true;
    }

    function render() {
        var total = 0;
        // hiện/ẩn từng dòng món
        rows.forEach(function(r){
            var ok = matchRow(r);
            r.style.display = ok ? '' : 'none';
            if (ok) total++;
        });
        // ẩn tiêu đề danh mục nếu không còn món nào hiển thị dưới nó
        var children = Array.prototype.slice.call(body.children);
        var header = null, headerHasVisible = false;
        children.forEach(function(el){
            if (el.hasAttribute && el.hasAttribute('data-cat-header')) {
                if (header) header.style.display = headerHasVisible ? '' : 'none';
                header = el; headerHasVisible = false;
            } else if (el.hasAttribute && el.hasAttribute('data-pid')) {
                if (el.style.display !== 'none') headerHasVisible = true;
            }
        });
        if (header) header.style.display = headerHasVisible ? '' : 'none';

        if (countEl) countEl.textContent = total;
        if (noRes) noRes.style.display = total === 0 ? '' : 'none';
    }

    if (search) search.addEventListener('input', function(){ query = norm(this.value.trim()); render(); });
    segBtns.forEach(function(btn){
        btn.addEventListener('click', function(){
            segBtns.forEach(function(x){ x.classList.remove('is-active'); });
            btn.classList.add('is-active');
            filter = btn.getAttribute('data-filter');
            render();
        });
    });
    render();
})();
</script>

<jsp:include page="../layout/footer.jsp" />
