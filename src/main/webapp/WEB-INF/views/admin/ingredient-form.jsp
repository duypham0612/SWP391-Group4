<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1><c:choose><c:when test="${ingredient.ingredientId > 0}">Sửa nguyên liệu</c:when><c:otherwise>Thêm nguyên liệu</c:otherwise></c:choose></h1><p>catalog.Ingredient</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/ingredient">← Quay lại</a>
</div>

<c:if test="${not empty errorMsg}"><div class="alert alert-error">${errorMsg}</div></c:if>

<div class="card form-card">
    <form action="${ctx}/admin/ingredient" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="save">
        <input type="hidden" name="ingredientId" value="${ingredient.ingredientId}">

        <div class="form-group">
            <label for="name">Tên nguyên liệu *</label>
            <input id="name" type="text" name="name" class="form-control" maxlength="120" value="${ingredient.name}" required autofocus>
        </div>
        <div class="form-group">
            <label for="unit">Đơn vị * (g, ml, kg, L, cái...)</label>
            <input id="unit" type="text" name="unit" class="form-control" maxlength="20" value="${ingredient.unit}" required>
        </div>
        <div class="form-group">
            <label for="ingredientType">Loại *</label>
            <select id="ingredientType" name="ingredientType" class="form-control" required>
                <option value="RAW"     <c:if test="${ingredient.ingredientType == 'RAW'}">selected</c:if>>RAW — nguyên liệu thô (mua về)</option>
                <option value="PREPPED" <c:if test="${ingredient.ingredientType == 'PREPPED'}">selected</c:if>>PREPPED — pha sẵn tại quán</option>
            </select>
        </div>
        <div class="form-group">
            <label><input type="checkbox" name="active" value="1" <c:if test="${ingredient.active or ingredient.ingredientId == 0}">checked</c:if>> Đang hoạt động</label>
        </div>
        <button type="submit" class="btn btn-primary btn-lg">Lưu</button>
    </form>
</div>

<jsp:include page="../layout/footer.jsp" />
