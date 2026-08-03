<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <h1>Thay quản lý</h1>
        <p><c:out value="${branch.code}"/> · <c:out value="${branch.name}"/></p>
    </div>
    <a class="btn btn-ghost" href="${ctx}/admin/branch">← Quay lại</a>
</div>

<div class="card form-card">
    <div class="alert alert-info">
        <strong>Quản lý hiện tại:</strong> <c:out value="${branch.managerName}"/><br>
        Sau khi xác nhận, người mới sẽ nhận role Quản lý chi nhánh; tài khoản quản lý cũ
        sẽ bị khóa và các phiên đăng nhập của cả hai người sẽ kết thúc.
    </div>

    <c:choose>
        <c:when test="${empty candidates}">
            <div class="empty-state">
                <p>Chi nhánh chưa có nhân sự ACTIVE phù hợp để thay thế.</p>
                <a class="btn btn-primary" href="${ctx}/admin/user?action=new">Thêm nhân sự</a>
            </div>
        </c:when>
        <c:otherwise>
            <form action="${ctx}/admin/branch" method="post"
                  onsubmit="return confirm('Xác nhận thay quản lý? Quản lý cũ sẽ bị khóa và đăng xuất.');">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="replaceManager">
                <input type="hidden" name="branchId" value="${branch.branchId}">

                <div class="form-group">
                    <label for="replacementUserId">Người thay thế *</label>
                    <select id="replacementUserId" name="replacementUserId" class="form-control" required autofocus>
                        <option value="">-- Chọn nhân sự ACTIVE cùng chi nhánh --</option>
                        <c:forEach var="candidate" items="${candidates}">
                            <option value="${candidate.userId}"><c:out value="${candidate.fullName}"/> — <c:out value="${candidate.roleName}"/> (<c:out value="${candidate.username}"/>)</option>
                        </c:forEach>
                    </select>
                    <small class="muted">Không thể chọn nhân sự đang mở ca thu ngân hoặc đang chấm công.</small>
                </div>

                <button type="submit" class="btn btn-primary btn-lg">Xác nhận thay quản lý</button>
            </form>
        </c:otherwise>
    </c:choose>
</div>

<jsp:include page="../layout/footer.jsp" />
