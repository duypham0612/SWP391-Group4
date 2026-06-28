<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1><c:choose><c:when test="${branch.branchId > 0}">Sửa chi nhánh</c:when><c:otherwise>Thêm chi nhánh</c:otherwise></c:choose></h1><p>org.Branch</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/branch">← Quay lại</a>
</div>

<c:if test="${not empty errorMsg}"><div class="alert alert-error">${errorMsg}</div></c:if>

<div class="card form-card">
    <form action="${ctx}/admin/branch" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="save">
        <input type="hidden" name="branchId" value="${branch.branchId}">

        <div class="form-group">
            <label for="code">Mã chi nhánh * (vd CN01)</label>
            <input id="code" type="text" name="code" class="form-control" maxlength="20" value="${branch.code}" required autofocus>
        </div>
        <div class="form-group">
            <label for="name">Tên chi nhánh *</label>
            <input id="name" type="text" name="name" class="form-control" maxlength="150" value="${branch.name}" required>
        </div>
        <div class="form-group">
            <label for="address">Địa chỉ</label>
            <input id="address" type="text" name="address" class="form-control" maxlength="255" value="${branch.address}">
        </div>
        <div class="form-group">
            <label for="phone">Số điện thoại</label>
            <input id="phone" type="text" name="phone" class="form-control" maxlength="20" value="${branch.phone}">
        </div>
        <div class="form-group">
            <label><input type="checkbox" name="active" value="1" <c:if test="${branch.active or branch.branchId == 0}">checked</c:if>> Đang hoạt động</label>
        </div>
        <button type="submit" class="btn btn-primary btn-lg">Lưu</button>
    </form>
</div>

<jsp:include page="../layout/footer.jsp" />
