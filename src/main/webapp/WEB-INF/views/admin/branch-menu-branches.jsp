<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Menu chi nhánh</h1></div>
</div>

<c:choose>
    <c:when test="${empty branches}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có chi nhánh hoạt động.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead>
                <tr>
                    <th style="width:110px">Mã</th>
                    <th>Chi nhánh</th>
                    <th style="width:150px;text-align:right">Thao tác</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="b" items="${branches}">
                    <tr>
                        <td><strong>${b.code}</strong></td>
                        <td>${b.name}</td>
                        <td style="text-align:right">
                            <a class="btn btn-primary btn-sm" href="${ctx}/admin/branch-menu?branchId=${b.branchId}">Mở menu</a>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
