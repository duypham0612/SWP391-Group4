<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Option của nhóm: ${group.name}</h1><p>catalog.ModifierOption</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/modifier">← Quay lại danh sách nhóm</a>
</div>

<div class="card" style="margin-bottom:18px">
    <h3 style="margin-top:0">Thêm option</h3>
    <form action="${ctx}/admin/modifier" method="post" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="addOption">
        <input type="hidden" name="groupId" value="${group.modifierGroupId}">
        <div class="form-group" style="margin:0;flex:1;min-width:220px">
            <label for="name">Tên option (vd Size L, Oat milk, Extra shot)</label>
            <input id="name" type="text" name="name" class="form-control" maxlength="80" required>
        </div>
        <div class="form-group" style="margin:0;width:180px">
            <label for="priceDelta">Phụ thu (₫)</label>
            <input id="priceDelta" type="number" name="priceDelta" class="form-control" step="500" value="0">
        </div>
        <button type="submit" class="btn btn-primary">+ Thêm</button>
    </form>
</div>

<c:choose>
    <c:when test="${empty options}">
        <div class="card empty-state"><div class="icon">🧃</div><p>Nhóm chưa có option nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th>Sửa option (tên · phụ thu · bật)</th><th style="width:320px">Thao tác</th></tr></thead>
            <tbody>
                <c:forEach var="o" items="${options}">
                    <tr>
                        <td>
                            <form action="${ctx}/admin/modifier" method="post" style="display:inline-flex;gap:6px;align-items:center;flex-wrap:wrap">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="updateOption">
                                <input type="hidden" name="groupId" value="${group.modifierGroupId}">
                                <input type="hidden" name="optionId" value="${o.modifierOptionId}">
                                <input type="text" name="name" class="form-control" style="width:180px" value="${o.name}" maxlength="80" required>
                                <input type="number" name="priceDelta" class="form-control" style="width:120px" step="500" value="${o.priceDelta}">
                                <label style="display:inline-flex;gap:4px;align-items:center;margin:0"><input type="checkbox" name="active" ${o.active ? 'checked' : ''}> bật</label>
                                <button type="submit" class="btn btn-ghost btn-sm">Lưu</button>
                            </form>
                        </td>
                        <td>
                            <a class="btn btn-ghost btn-sm" href="${ctx}/admin/modifier?view=impacts&optionId=${o.modifierOptionId}">Định mức nguyên liệu</a>
                            <form action="${ctx}/admin/modifier" method="post" style="display:inline" onsubmit="return confirm('Xoá option này (và định mức của nó)?');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="deleteOption">
                                <input type="hidden" name="groupId" value="${group.modifierGroupId}">
                                <input type="hidden" name="optionId" value="${o.modifierOptionId}">
                                <button type="submit" class="btn btn-ghost btn-sm" style="color:var(--st-cancelled)">Xoá</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
