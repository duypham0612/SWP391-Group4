<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Nhóm Modifier</h1><p>catalog.ModifierGroup → Option → IngredientImpact</p></div>
    <a class="btn btn-primary" href="${ctx}/admin/modifier?view=groupForm">+ Thêm nhóm</a>
</div>

<c:choose>
    <c:when test="${empty groups}">
        <div class="card empty-state"><div class="icon">🎚️</div><p>Chưa có nhóm modifier nào (vd Size, Sữa, Topping).</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th style="width:60px">#</th><th>Tên nhóm</th><th style="width:100px">Bắt buộc</th><th style="width:140px">Chọn (min–max)</th><th style="width:220px">Thao tác</th></tr></thead>
            <tbody>
                <c:forEach var="g" items="${groups}">
                    <tr>
                        <td>${g.modifierGroupId}</td>
                        <td>${g.name}</td>
                        <td><c:choose><c:when test="${g.required}"><span class="badge badge-making">Bắt buộc</span></c:when><c:otherwise><span class="badge badge-served">Tuỳ chọn</span></c:otherwise></c:choose></td>
                        <td>${g.minSelect} – ${g.maxSelect}</td>
                        <td>
                            <a class="btn btn-ghost btn-sm" href="${ctx}/admin/modifier?view=options&groupId=${g.modifierGroupId}">Option</a>
                            <a class="btn btn-ghost btn-sm" href="${ctx}/admin/modifier?view=groupForm&groupId=${g.modifierGroupId}">Sửa</a>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
