<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Menu chi nhánh</h1><p>Chọn chi nhánh để cập nhật món đang bán.</p></div>
</div>

<c:choose>
    <c:when test="${empty branches}">
        <div class="card empty-state"><div class="icon">🏢</div><p>Chưa có chi nhánh hoạt động.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table admin-branch-menu-branches">
            <thead><tr><th style="width:100px">Mã</th><th>Chi nhánh</th><th style="width:160px">Menu</th></tr></thead>
            <tbody>
                <c:forEach var="b" items="${branches}">
                    <tr>
                        <td>${b.code}</td>
                        <td>${b.name}</td>
                        <td><a class="btn btn-ghost btn-sm" href="${ctx}/admin/branch-menu?branchId=${b.branchId}">Quản lý menu</a></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
