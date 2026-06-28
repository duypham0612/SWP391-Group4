<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1><c:choose><c:when test="${group.modifierGroupId > 0}">Sửa nhóm modifier</c:when><c:otherwise>Thêm nhóm modifier</c:otherwise></c:choose></h1><p>catalog.ModifierGroup</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/modifier">← Quay lại</a>
</div>

<c:if test="${not empty errorMsg}"><div class="alert alert-error">${errorMsg}</div></c:if>

<div class="card form-card">
    <form action="${ctx}/admin/modifier" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="saveGroup">
        <input type="hidden" name="modifierGroupId" value="${group.modifierGroupId}">

        <div class="form-group">
            <label for="name">Tên nhóm * (vd Size, Sữa, Topping)</label>
            <input id="name" type="text" name="name" class="form-control" maxlength="80" value="${group.name}" required autofocus>
        </div>
        <div class="form-group">
            <label><input type="checkbox" name="required" value="1" <c:if test="${group.required}">checked</c:if>> Bắt buộc chọn</label>
        </div>
        <div class="form-group">
            <label for="minSelect">Số lượng chọn tối thiểu</label>
            <input id="minSelect" type="number" name="minSelect" class="form-control" min="0" value="${group.minSelect}">
        </div>
        <div class="form-group">
            <label for="maxSelect">Số lượng chọn tối đa</label>
            <input id="maxSelect" type="number" name="maxSelect" class="form-control" min="1" value="${group.maxSelect}">
        </div>
        <button type="submit" class="btn btn-primary btn-lg">Lưu</button>
    </form>
</div>

<jsp:include page="../layout/footer.jsp" />
