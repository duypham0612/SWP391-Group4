<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1><c:choose><c:when test="${product.productId > 0}">Sửa sản phẩm</c:when><c:otherwise>Thêm sản phẩm</c:otherwise></c:choose></h1><p>catalog.Product</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/product">← Quay lại</a>
</div>

<c:if test="${not empty errorMsg}"><div class="alert alert-error">${errorMsg}</div></c:if>

<div class="card form-card">
    <form action="${ctx}/admin/product" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="save">
        <input type="hidden" name="productId" value="${product.productId}">

        <div class="form-group">
            <label for="name">Tên sản phẩm *</label>
            <input id="name" type="text" name="name" class="form-control" maxlength="150" value="${product.name}" required autofocus>
        </div>
        <div class="form-group">
            <label for="categoryId">Danh mục *</label>
            <select id="categoryId" name="categoryId" class="form-control" required>
                <option value="">-- Chọn danh mục --</option>
                <c:forEach var="cat" items="${categories}">
                    <option value="${cat.categoryId}" <c:if test="${cat.categoryId == product.categoryId}">selected</c:if>>${cat.name}</option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group">
            <label for="basePrice">Giá gốc (₫) *</label>
            <input id="basePrice" type="number" name="basePrice" class="form-control" min="0" step="1000" value="${product.basePrice}" required>
        </div>
        <div class="form-group">
            <label for="imageUrl">Ảnh (URL)</label>
            <input id="imageUrl" type="text" name="imageUrl" class="form-control" maxlength="255" value="${product.imageUrl}">
        </div>
        <div class="form-group">
            <label><input type="checkbox" name="active" value="1" <c:if test="${product.active or product.productId == 0}">checked</c:if>> Đang hoạt động</label>
        </div>
        <button type="submit" class="btn btn-primary btn-lg">Lưu</button>
    </form>
</div>

<jsp:include page="../layout/footer.jsp" />
