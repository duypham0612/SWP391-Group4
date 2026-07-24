<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Công thức pha sẵn: ${prepped.name}</h1><p>Tạo mẻ pha sẵn từ nguyên liệu thô.</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/recipe">← Quay lại</a>
</div>

<c:if test="${not empty errorMsg}"><div class="alert alert-error">${errorMsg}</div></c:if>

<div class="card" style="margin-bottom:18px">
    <h3 style="margin-top:0">Thêm nguyên liệu thô vào công thức pha</h3>
    <form action="${ctx}/admin/recipe" method="post" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="addPrep">
        <input type="hidden" name="preppedId" value="${prepped.ingredientId}">
        <div class="form-group" style="margin:0;flex:1;min-width:220px">
            <label for="rawIngredientId">Nguyên liệu thô</label>
            <select id="rawIngredientId" name="rawIngredientId" class="form-control" required>
                <option value="">-- Chọn --</option>
                <c:forEach var="i" items="${rawIngredients}">
                    <option value="${i.ingredientId}">${i.name} (${i.unit})</option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group" style="margin:0;width:150px">
            <label for="quantity">Lượng nguyên liệu thô</label>
            <input id="quantity" type="number" name="quantity" class="form-control" min="0" step="0.001" required>
        </div>
        <div class="form-group" style="margin:0;width:150px">
            <label for="yieldQty">Sản lượng (yield)</label>
            <input id="yieldQty" type="number" name="yieldQty" class="form-control" min="0" step="0.001" required>
        </div>
        <button type="submit" class="btn btn-primary">+ Thêm</button>
    </form>
</div>

<c:choose>
    <c:when test="${empty prepLines}">
        <div class="card empty-state"><div class="icon">🧪</div><p>Chưa có công thức pha cho nguyên liệu này.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th>Nguyên liệu thô</th><th style="width:160px">Lượng dùng</th><th style="width:160px">Yield</th><th style="width:100px">Xoá</th></tr></thead>
            <tbody>
                <c:forEach var="pl" items="${prepLines}">
                    <fmt:formatNumber var="prepQty" value="${pl.quantity}" groupingUsed="false" maxFractionDigits="3" />
                    <fmt:formatNumber var="prepYield" value="${pl.yieldQty}" groupingUsed="false" maxFractionDigits="3" />
                    <tr>
                        <td>${pl.rawIngredientName}</td>
                        <td>${prepQty} ${pl.rawIngredientUnit}</td>
                        <td>${prepYield} ${prepped.unit}</td>
                        <td>
                            <form action="${ctx}/admin/recipe" method="post" style="display:inline" onsubmit="return confirm('Xoá dòng này?');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="deletePrep">
                                <input type="hidden" name="preppedId" value="${prepped.ingredientId}">
                                <input type="hidden" name="prepId" value="${pl.prepRecipeId}">
                                <button type="submit" class="btn btn-ghost btn-sm">Xoá</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
