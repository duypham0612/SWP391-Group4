<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Gán Size / Đường / Đá: ${product.name}</h1><p>Chọn nhóm lựa chọn áp dụng cho sản phẩm</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/product">← Quay lại sản phẩm</a>
</div>

<div class="card" style="margin-bottom:18px">
    <h3 style="margin-top:0">Gán nhóm lựa chọn cho sản phẩm</h3>
    <form action="${ctx}/admin/modifier" method="post" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="assignGroup">
        <input type="hidden" name="productId" value="${product.productId}">
        <div class="form-group" style="margin:0;flex:1;min-width:240px">
            <label for="groupId">Nhóm lựa chọn</label>
            <select id="groupId" name="groupId" class="form-control" required>
                <option value="">-- Chọn nhóm --</option>
                <c:forEach var="g" items="${allGroups}">
                    <option value="${g.modifierGroupId}">${g.name}</option>
                </c:forEach>
            </select>
        </div>
        <button type="submit" class="btn btn-primary">+ Gán</button>
    </form>
</div>

<c:choose>
    <c:when test="${empty assigned}">
        <div class="card empty-state"><div class="icon">🎚️</div><p>Sản phẩm chưa gán nhóm Size / Đường / Đá nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th>Nhóm lựa chọn đã gán</th><th style="width:120px">Bỏ gán</th></tr></thead>
            <tbody>
                <c:forEach var="a" items="${assigned}">
                    <tr>
                        <td>${a.groupName}</td>
                        <td>
                            <form action="${ctx}/admin/modifier" method="post" style="display:inline" onsubmit="return confirm('Bỏ gán nhóm này?');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="unassignGroup">
                                <input type="hidden" name="productId" value="${product.productId}">
                                <input type="hidden" name="groupId" value="${a.modifierGroupId}">
                                <button type="submit" class="btn btn-ghost btn-sm">Bỏ gán</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
