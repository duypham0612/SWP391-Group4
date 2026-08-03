<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Công thức</h1><p>Thiết lập nguyên liệu và định mức cho món hoặc nguyên liệu pha sẵn.</p></div>
</div>

<h3>Công thức món</h3>
<c:choose>
    <c:when test="${empty products}">
        <div class="card empty-state"><div class="icon">📭</div><p>Chưa có sản phẩm. Hãy thêm sản phẩm trước.</p></div>
    </c:when>
    <c:otherwise>
        <div data-tabletools data-tt-page-size="8">
            <div class="table-toolbar">
                <div class="form-group table-search">
                    <label for="recipeProductSearch">Tìm kiếm</label>
                    <input id="recipeProductSearch" class="form-control" type="search" data-tt-search placeholder="Tìm theo tên sản phẩm">
                </div>
                <div class="form-group">
                    <label for="recipeProductCategoryFilter">Danh mục</label>
                    <select id="recipeProductCategoryFilter" class="form-control tt-filter" data-tt-filter data-tt-col="2" data-tt-autofill>
                        <option value="">Tất cả</option>
                    </select>
                </div>
            </div>
            <table class="table">
                <thead><tr><th style="width:70px" data-tt-nosearch>STT</th><th data-tt-search>Sản phẩm</th><th data-tt-nosearch>Danh mục</th><th style="width:180px" data-tt-nosearch>Công thức</th></tr></thead>
                <tbody>
                    <c:forEach var="p" items="${products}" varStatus="status">
                        <tr>
                            <td data-tt-index>${status.index + 1}</td>
                            <td>${p.name}</td>
                            <td>${p.categoryName}</td>
                            <td><a class="btn btn-ghost btn-sm" href="${ctx}/admin/recipe?productId=${p.productId}">Công thức món</a></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            <div class="table-tools-foot">
                <span class="tt-summary" data-tt-summary></span>
                <div class="pagination" data-tt-pager></div>
            </div>
        </div>
    </c:otherwise>
</c:choose>

<h3 style="margin-top:26px">Công thức pha sẵn</h3>
<c:choose>
    <c:when test="${empty preppedIngredients}">
        <div class="card empty-state"><p>Chưa có nguyên liệu pha sẵn.</p></div>
    </c:when>
    <c:otherwise>
        <div data-tabletools data-tt-page-size="6">
            <div class="table-toolbar">
                <div class="form-group table-search">
                    <label for="recipePreppedSearch">Tìm kiếm</label>
                    <input id="recipePreppedSearch" class="form-control" type="search"
                           data-tt-search placeholder="Tìm theo tên nguyên liệu">
                </div>
            </div>
            <table class="table">
                <thead>
                    <tr>
                        <th style="width:70px" data-tt-nosearch>STT</th>
                        <th data-tt-search>Nguyên liệu pha sẵn</th>
                        <th style="width:100px" data-tt-nosearch>Đơn vị</th>
                        <th style="width:180px" data-tt-nosearch>Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="i" items="${preppedIngredients}" varStatus="status">
                        <tr>
                            <td data-tt-index>${status.index + 1}</td>
                            <td>${i.name}</td>
                            <td>${i.unit}</td>
                            <td>
                                <a class="btn btn-ghost btn-sm"
                                   href="${ctx}/admin/recipe?preppedId=${i.ingredientId}">Công thức</a>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            <div class="table-tools-foot">
                <span class="tt-summary" data-tt-summary></span>
                <div class="pagination" data-tt-pager></div>
            </div>
        </div>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
