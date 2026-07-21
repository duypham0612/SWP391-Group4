<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1><c:choose><c:when test="${branch.branchId > 0}">Sửa chi nhánh</c:when><c:otherwise>Thêm chi nhánh</c:otherwise></c:choose></h1></div>
    <a class="btn btn-ghost" href="${ctx}/admin/branch">← Quay lại</a>
</div>

<c:if test="${not empty errorMsg}"><div class="alert alert-error">${errorMsg}</div></c:if>

<div class="card form-card">
    <form action="${ctx}/admin/branch" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="save">
        <input type="hidden" name="branchId" value="${branch.branchId}">

        <c:if test="${branch.branchId > 0}">
            <div class="form-group">
                <label>Mã chi nhánh</label>
                <div class="form-control" style="background:var(--surface-2);color:var(--ink-soft)">${branch.code}</div>
                <small class="muted">Mã chi nhánh không thể thay đổi sau khi tạo.</small>
            </div>
        </c:if>
        <div class="form-group">
            <label for="name">Tên chi nhánh *</label>
            <input id="name" type="text" name="name" class="form-control" maxlength="150" value="${branch.name}" required
                   <c:if test="${branch.branchId == 0}">autofocus</c:if>>
        </div>
        <div class="form-group">
            <label for="address">Địa chỉ *</label>
            <input id="address" type="text" name="address" class="form-control" maxlength="255" value="${branch.address}" required>
        </div>
        <div class="form-row" style="display:flex;gap:16px">
            <div class="form-group" style="flex:1">
                <label for="openTime">Giờ mở cửa</label>
                <input id="openTime" type="time" name="openTime" class="form-control" value="${branch.openTime}">
            </div>
            <div class="form-group" style="flex:1">
                <label for="closeTime">Giờ đóng cửa</label>
                <input id="closeTime" type="time" name="closeTime" class="form-control" value="${branch.closeTime}">
            </div>
        </div>
        <div class="form-group">
            <label for="managerUserId">Quản lý phụ trách</label>
            <select id="managerUserId" name="managerUserId" class="form-control">
                <option value="">— Chưa gán —</option>
                <c:forEach var="m" items="${managers}">
                    <option value="${m.userId}" <c:if test="${branch.managerUserId == m.userId}">selected</c:if>>${m.fullName} (${m.username})</option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group">
            <label><input type="checkbox" name="active" value="1" <c:if test="${branch.active or branch.branchId == 0}">checked</c:if>> Đang hoạt động</label>
        </div>
        <button type="submit" class="btn btn-primary btn-lg">Lưu</button>
    </form>
</div>

<script>
(function(){
    var openTime = document.getElementById('openTime');
    var closeTime = document.getElementById('closeTime');
    if (!openTime || !closeTime) return;

    function validateHours() {
        closeTime.min = openTime.value || '';
        if (openTime.value && closeTime.value && closeTime.value <= openTime.value) {
            closeTime.setCustomValidity('Giờ đóng cửa phải sau giờ mở cửa.');
        } else {
            closeTime.setCustomValidity('');
        }
    }

    openTime.addEventListener('input', validateHours);
    closeTime.addEventListener('input', validateHours);
    validateHours();
})();
</script>

<jsp:include page="../layout/footer.jsp" />
