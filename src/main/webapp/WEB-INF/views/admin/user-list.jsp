<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Nhân sự</h1><p>iam.[User]</p></div>
    <a class="btn btn-primary" href="${ctx}/admin/user?action=new">+ Thêm nhân sự</a>
</div>

<form method="get" action="${ctx}/admin/user" class="card" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap;padding:14px;margin-bottom:16px">
    <div class="form-group" style="margin:0">
        <label for="fRole">Vai trò</label>
        <select id="fRole" name="roleId" class="form-control" style="min-width:180px">
            <option value="">— Tất cả vai trò —</option>
            <c:forEach var="r" items="${roles}">
                <option value="${r.roleId}" <c:if test="${fRoleId == r.roleId}">selected</c:if>>${r.name}</option>
            </c:forEach>
        </select>
    </div>
    <div class="form-group" style="margin:0">
        <label for="fBranch">Chi nhánh</label>
        <select id="fBranch" name="branchId" class="form-control" style="min-width:180px">
            <option value="">— Tất cả chi nhánh —</option>
            <c:forEach var="b" items="${branches}">
                <option value="${b.branchId}" <c:if test="${fBranchId == b.branchId}">selected</c:if>>${b.name}</option>
            </c:forEach>
        </select>
    </div>
    <button type="submit" class="btn btn-ghost">Lọc</button>
    <c:if test="${not empty fRoleId or not empty fBranchId}">
        <a class="btn btn-ghost" href="${ctx}/admin/user">Xoá lọc</a>
    </c:if>
</form>

<c:choose>
    <c:when test="${empty staffList}">
        <div class="card empty-state"><div class="icon">📭</div><p>Chưa có nhân sự nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr>
                <th style="width:60px">#</th><th>Tên đăng nhập</th><th>Họ tên</th><th>Vai trò</th>
                <th>Chi nhánh</th><th style="width:110px">Trạng thái</th><th style="width:90px">Thao tác</th>
            </tr></thead>
            <tbody>
                <c:forEach var="s" items="${staffList}">
                    <tr>
                        <td>${s.userId}</td>
                        <td>${s.username}</td>
                        <td>${s.fullName}</td>
                        <td>${s.roleName}</td>
                        <td><c:choose><c:when test="${empty s.branchName}"><span class="muted">(toàn chuỗi)</span></c:when><c:otherwise>${s.branchName}</c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${s.status == 'ACTIVE'}"><span class="badge badge-ready">ACTIVE</span></c:when><c:otherwise><span class="badge badge-cancelled">LOCKED</span></c:otherwise></c:choose></td>
                        <td>
                            <a class="btn btn-ghost btn-sm" href="${ctx}/admin/user?action=edit&id=${s.userId}">Sửa</a>
                            <form action="${ctx}/admin/user" method="post" style="display:inline"
                                  onsubmit="return confirm('Đổi trạng thái tài khoản này?');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="toggleStatus">
                                <input type="hidden" name="id" value="${s.userId}">
                                <input type="hidden" name="current" value="${s.status}">
                                <button type="submit" class="btn btn-ghost btn-sm">
                                    <c:choose><c:when test="${s.status == 'ACTIVE'}">Khoá</c:when><c:otherwise>Mở khoá</c:otherwise></c:choose>
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
