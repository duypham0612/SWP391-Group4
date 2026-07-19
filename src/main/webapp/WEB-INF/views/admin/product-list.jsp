<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Sản phẩm</h1><p>catalog.Product</p></div>
    <a class="btn btn-primary" href="${ctx}/admin/product?action=new">+ Thêm sản phẩm</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" />
</c:if>

<c:choose>
    <c:when test="${empty products}">
        <div class="card empty-state"><div class="icon">📭</div><p>Chưa có sản phẩm nào.</p></div>
    </c:when>
    <c:otherwise>
        <form id="publishManyForm" action="${ctx}/admin/product" method="post" data-tabletools>
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="publishManyToBranch">

            <div class="card" style="margin-bottom:14px">
                <div style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
                    <div class="form-group" style="margin:0;min-width:240px">
                        <label for="branchId">Chi nhánh</label>
                        <select id="branchId" name="branchId" class="form-control" required>
                            <option value="">-- Chọn chi nhánh --</option>
                            <c:forEach var="b" items="${branches}">
                                <option value="${b.branchId}">${b.code} · ${b.name}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <button type="submit" class="btn btn-primary">Thêm sản phẩm vào chi nhánh</button>
                </div>
            </div>

            <div class="table-toolbar">
                <div class="form-group table-search">
                    <label for="productSearch">Tìm kiếm</label>
                    <input id="productSearch" class="form-control" type="search" data-tt-search placeholder="Tìm theo tên hoặc danh mục">
                </div>
                <div class="form-group">
                    <label for="productCategoryFilter">Danh mục</label>
                    <select id="productCategoryFilter" class="form-control tt-filter" data-tt-filter data-tt-col="4" data-tt-autofill>
                        <option value="">Tất cả</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="productStatusFilter">Trạng thái</label>
                    <select id="productStatusFilter" class="form-control tt-filter" data-tt-filter data-tt-col="6">
                        <option value="">Tất cả</option>
                        <option value="true">Hiển thị</option>
                        <option value="false">Ẩn</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="productPageSize">Hiển thị</label>
                    <select id="productPageSize" class="form-control tt-size" data-tt-size>
                        <option value="10">10</option>
                        <option value="20">20</option>
                        <option value="50">50</option>
                    </select>
                </div>
            </div>

            <table class="table">
                <thead><tr>
                    <th style="width:44px" data-tt-nosearch><input id="checkAllProducts" type="checkbox" aria-label="Chọn tất cả sản phẩm"></th>
                    <th style="width:60px" data-tt-nosearch>#</th><th style="width:64px" data-tt-nosearch>Ảnh</th><th data-tt-search>Tên</th><th data-tt-search>Danh mục</th>
                    <th style="width:140px" data-tt-nosearch>Giá gốc</th><th style="width:110px" data-tt-nosearch>Trạng thái</th><th style="width:100px" data-tt-nosearch>Pha chuẩn</th><th style="width:170px" data-tt-nosearch>Thao tác</th>
                </tr></thead>
                <tbody>
                    <c:forEach var="p" items="${products}">
                        <c:set var="imgSrc" value="${empty p.imageUrl ? ctx.concat('/assets/img/products/_placeholder.svg') : (p.imageUrl.startsWith('http') ? p.imageUrl : ctx.concat(p.imageUrl))}" />
                        <tr>
                            <td><input class="product-pick" type="checkbox" name="productIds" value="${p.productId}" aria-label="Chọn ${p.name}"></td>
                            <td>${p.productId}</td>
                            <td><img class="prod-thumb" src="${imgSrc}" alt="${p.name}" loading="lazy" onerror="this.src='${ctx}/assets/img/products/_placeholder.svg'"></td>
                            <td>${p.name}</td>
                            <td>${p.categoryName}</td>
                            <td><fmt:formatNumber value="${p.basePrice}" type="number" maxFractionDigits="0"/> ₫</td>
                            <td data-tt-val="${p.active}"><c:choose><c:when test="${p.active}"><span class="badge badge-ready">Hiển thị</span></c:when><c:otherwise><span class="badge badge-cancelled">Ẩn</span></c:otherwise></c:choose></td>
                            <td>${p.prepMinutes} phút</td>
                            <td>
                                <a class="btn btn-ghost btn-sm" href="${ctx}/admin/product?action=edit&id=${p.productId}">Sửa</a>
                                <a class="btn btn-ghost btn-sm" href="${ctx}/admin/recipe?productId=${p.productId}">Công thức</a>
                                <button type="submit" form="toggleProduct${p.productId}" class="btn btn-ghost btn-sm" onclick="return confirm('Đổi trạng thái sản phẩm này?');">${p.active ? 'Ẩn' : 'Hiện'}</button>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            <div class="table-tools-foot">
                <span class="tt-summary" data-tt-summary></span>
                <div class="pagination" data-tt-pager></div>
            </div>
        </form>

        <c:forEach var="p" items="${products}">
            <form id="toggleProduct${p.productId}" action="${ctx}/admin/product" method="post" style="display:none">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="toggleActive">
                <input type="hidden" name="id" value="${p.productId}">
            </form>
        </c:forEach>

        <script>
            (function () {
                var all = document.getElementById('checkAllProducts');
                if (!all) return;
                var form = document.getElementById('publishManyForm');
                function visibleBoxes() {
                    return Array.prototype.slice.call(document.querySelectorAll('.product-pick')).filter(function (box) {
                        var row = box.closest('tr');
                        return row && !row.hidden;
                    });
                }
                function updateAllState() {
                    var boxes = visibleBoxes();
                    var checked = boxes.filter(function (box) { return box.checked; }).length;
                    all.checked = boxes.length > 0 && checked === boxes.length;
                    all.indeterminate = checked > 0 && checked < boxes.length;
                    all.disabled = boxes.length === 0;
                }
                all.addEventListener('change', function () {
                    visibleBoxes().forEach(function (box) {
                        box.checked = all.checked;
                    });
                    updateAllState();
                });
                document.querySelectorAll('.product-pick').forEach(function (box) {
                    box.addEventListener('change', updateAllState);
                });
                if (form) {
                    form.addEventListener('tabletools:updated', function () {
                        document.querySelectorAll('.product-pick').forEach(function (box) {
                            var row = box.closest('tr');
                            if (row && row.hidden) box.checked = false;
                        });
                        updateAllState();
                    });
                }
                updateAllState();
            })();
        </script>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
