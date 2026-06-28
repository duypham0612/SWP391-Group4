<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Công thức / Recipe</h1><p>BOM cho sản phẩm · Công thức pha sẵn (PrepRecipe) cho nguyên liệu PREPPED</p></div>
</div>

<h3>Công thức món (BOM) — chọn sản phẩm</h3>
<c:choose>
    <c:when test="${empty products}">
        <div class="card empty-state"><div class="icon">📭</div><p>Chưa có sản phẩm. Hãy thêm sản phẩm trước.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table" style="margin-bottom:26px">
            <thead><tr><th style="width:60px">#</th><th>Sản phẩm</th><th>Danh mục</th><th style="width:180px">Công thức</th></tr></thead>
            <tbody>
                <c:forEach var="p" items="${products}">
                    <tr>
                        <td>${p.productId}</td>
                        <td>${p.name}</td>
                        <td>${p.categoryName}</td>
                        <td><a class="btn btn-ghost btn-sm" href="${ctx}/admin/recipe?productId=${p.productId}">Khai báo BOM</a></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<h3>Công thức pha sẵn (RAW → PREPPED) — chọn nguyên liệu PREPPED</h3>
<c:choose>
    <c:when test="${empty preppedIngredients}">
        <div class="card empty-state"><div class="icon">🥤</div><p>Chưa có nguyên liệu PREPPED nào. Tạo ở mục Nguyên liệu (loại PREPPED).</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th style="width:60px">#</th><th>Nguyên liệu PREPPED</th><th style="width:100px">Đơn vị</th><th style="width:180px">Công thức pha</th></tr></thead>
            <tbody>
                <c:forEach var="i" items="${preppedIngredients}">
                    <tr>
                        <td>${i.ingredientId}</td>
                        <td>${i.name}</td>
                        <td>${i.unit}</td>
                        <td><a class="btn btn-ghost btn-sm" href="${ctx}/admin/recipe?preppedId=${i.ingredientId}">Khai báo PrepRecipe</a></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
