<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <h1><c:choose><c:when test="${category.categoryId > 0}">Sửa danh mục</c:when><c:otherwise>Thêm danh mục</c:otherwise></c:choose></h1>
    </div>
    <a class="btn btn-ghost" href="${ctx}/admin/category">← Quay lại</a>
</div>

<c:if test="${not empty errorMsg}">
    <div class="alert alert-error">${errorMsg}</div>
</c:if>

<div class="card form-card">
    <form action="${ctx}/admin/category" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="save">
        <input type="hidden" name="categoryId" value="${category.categoryId}">

        <div class="form-group">
            <label for="name">Tên danh mục *</label>
            <input id="name" type="text" name="name" class="form-control" maxlength="100"
                   value="${category.name}" required autofocus>
        </div>
        <div class="form-group">
            <label for="sortOrder">Thứ tự hiển thị</label>
            <input id="sortOrder" type="number" name="sortOrder" class="form-control" min="0"
                   value="${category.sortOrder}">
        </div>
        <div class="form-group">
            <label>
                <input type="checkbox" name="active" value="1"
                       <c:if test="${category.active or category.categoryId == 0}">checked</c:if>>
                Hiển thị (đang hoạt động)
            </label>
        </div>

        <button type="submit" class="btn btn-primary btn-lg">Lưu</button>
    </form>
</div>

<jsp:include page="../layout/footer.jsp" />
