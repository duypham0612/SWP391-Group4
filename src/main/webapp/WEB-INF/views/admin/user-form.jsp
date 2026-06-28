<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="editing" value="${staff.userId > 0}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1><c:choose><c:when test="${editing}">Sửa nhân sự</c:when><c:otherwise>Thêm nhân sự</c:otherwise></c:choose></h1><p>iam.[User]</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/user">← Quay lại</a>
</div>

<c:if test="${not empty errorMsg}"><div class="alert alert-error">${errorMsg}</div></c:if>

<div class="card form-card">
    <form action="${ctx}/admin/user" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="userId" value="${staff.userId}">

        <div class="form-group">
            <label for="username">Tên đăng nhập *</label>
            <input id="username" type="text" name="username" class="form-control" maxlength="60"
                   value="${staff.username}" required <c:if test="${editing}">readonly</c:if>>
        </div>
        <div class="form-group">
            <label for="fullName">Họ tên *</label>
            <input id="fullName" type="text" name="fullName" class="form-control" maxlength="120" value="${staff.fullName}" required>
        </div>
        <div class="form-group">
            <label for="email">Email</label>
            <input id="email" type="email" name="email" class="form-control" maxlength="120" value="${staff.email}">
        </div>
        <div class="form-group">
            <label for="phone">Số điện thoại</label>
            <input id="phone" type="text" name="phone" class="form-control" maxlength="20" value="${staff.phone}">
        </div>
        <div class="form-group">
            <label for="roleId">Vai trò *</label>
            <select id="roleId" name="roleId" class="form-control" required>
                <option value="">-- Chọn vai trò --</option>
                <c:forEach var="r" items="${roles}">
                    <option value="${r.roleId}" <c:if test="${r.roleId == staff.roleId}">selected</c:if>>${r.name} (${r.code})</option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group">
            <label for="branchId">Chi nhánh <span class="muted">(để trống nếu là Admin toàn chuỗi)</span></label>
            <select id="branchId" name="branchId" class="form-control">
                <option value="">-- Toàn chuỗi (Admin) --</option>
                <c:forEach var="b" items="${branches}">
                    <option value="${b.branchId}" <c:if test="${b.branchId == staff.branchId}">selected</c:if>>${b.code} — ${b.name}</option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group">
            <label for="status">Trạng thái</label>
            <select id="status" name="status" class="form-control">
                <option value="ACTIVE" <c:if test="${staff.status == 'ACTIVE'}">selected</c:if>>ACTIVE</option>
                <option value="LOCKED" <c:if test="${staff.status == 'LOCKED'}">selected</c:if>>LOCKED</option>
            </select>
        </div>
        <div class="form-group">
            <label for="password">
                <c:choose><c:when test="${editing}">Đổi mật khẩu (để trống nếu giữ nguyên)</c:when><c:otherwise>Mật khẩu * (≥ 6 ký tự)</c:otherwise></c:choose>
            </label>
            <input id="password" type="password" name="password" class="form-control" <c:if test="${not editing}">required</c:if>>
        </div>
        <button type="submit" class="btn btn-primary btn-lg">Lưu</button>
    </form>
</div>

<jsp:include page="../layout/footer.jsp" />
