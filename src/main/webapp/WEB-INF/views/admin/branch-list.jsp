<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Chi nhánh</h1><p>org.Branch</p></div>
    <a class="btn btn-primary" href="${ctx}/admin/branch?action=new">+ Thêm chi nhánh</a>
</div>

<c:choose>
    <c:when test="${empty branches}">
        <div class="card empty-state"><div class="icon">📭</div><p>Chưa có chi nhánh nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr>
                <th style="width:60px">#</th><th style="width:100px">Mã</th><th>Tên</th><th>Địa chỉ</th>
                <th style="width:120px">Giờ mở cửa</th><th>Quản lý</th>
                <th style="width:110px">Trạng thái</th><th style="width:170px">Thao tác</th>
            </tr></thead>
            <tbody>
                <c:forEach var="b" items="${branches}">
                    <tr>
                        <td>${b.branchId}</td>
                        <td>${b.code}</td>
                        <td>${b.name}</td>
                        <td>${b.address}</td>
                        <td><c:choose><c:when test="${not empty b.hoursText}">${b.hoursText}</c:when><c:otherwise><span class="muted">—</span></c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${not empty b.managerName}">${b.managerName}</c:when><c:otherwise><span class="muted">(chưa gán)</span></c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${b.active}"><span class="badge badge-ready">Hoạt động</span></c:when><c:otherwise><span class="badge badge-cancelled">Ngừng</span></c:otherwise></c:choose></td>
                        <td>
                            <a class="btn btn-ghost btn-sm" href="${ctx}/admin/branch?action=edit&id=${b.branchId}">Sửa</a>
                            <form action="${ctx}/admin/branch" method="post" style="display:inline" onsubmit="return confirm('Đổi trạng thái chi nhánh này?');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="toggleActive">
                                <input type="hidden" name="id" value="${b.branchId}">
                                <button type="submit" class="btn btn-ghost btn-sm">${b.active ? 'Ngừng' : 'Bật'}</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
