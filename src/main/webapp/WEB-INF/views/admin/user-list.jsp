<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Nhân sự</h1><p>iam.[User]</p></div>
    <a class="btn btn-primary" href="${ctx}/admin/user?action=new">+ Thêm nhân sự</a>
</div>

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
