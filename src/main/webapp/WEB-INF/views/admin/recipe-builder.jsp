<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Công thức: ${product.name}</h1><p>catalog.ProductRecipe · định mức cho 1 phần</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/recipe">← Chọn sản phẩm khác</a>
</div>

<c:if test="${not empty errorMsg}"><div class="alert alert-error">${errorMsg}</div></c:if>

<div class="alert alert-info">
    Định mức ở đây áp dụng khi Barista bấm "Xong" (Phase 4). Nguyên liệu PREPPED đã gộp RAW lúc PrepBatch —
    khai báo đúng nguyên liệu công thức tham chiếu, <strong>không khai trùng RAW + PREPPED</strong> cho cùng một thành phần.
</div>

<div class="card" style="margin-bottom:18px">
    <h3 style="margin-top:0">Thêm nguyên liệu vào công thức</h3>
    <form action="${ctx}/admin/recipe" method="post" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="addLine">
        <input type="hidden" name="productId" value="${product.productId}">
        <div class="form-group" style="margin:0;flex:1;min-width:240px">
            <label for="ingredientId">Nguyên liệu</label>
            <select id="ingredientId" name="ingredientId" class="form-control" required>
                <option value="">-- Chọn --</option>
                <c:forEach var="i" items="${ingredients}">
                    <option value="${i.ingredientId}">${i.name} (${i.unit} · ${i.ingredientType})</option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group" style="margin:0;width:160px">
            <label for="quantity">Số lượng</label>
            <input id="quantity" type="number" name="quantity" class="form-control" min="0" step="0.001" required>
        </div>
        <button type="submit" class="btn btn-primary">+ Thêm</button>
    </form>
</div>

<c:choose>
    <c:when test="${empty lines}">
        <div class="card empty-state"><div class="icon">🧪</div><p>Công thức chưa có nguyên liệu nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th>Nguyên liệu</th><th style="width:120px">Loại</th><th style="width:230px">Định mức</th><th style="width:90px">Xoá</th></tr></thead>
            <tbody>
                <c:forEach var="l" items="${lines}">
                    <tr>
                        <td>${l.ingredientName}</td>
                        <td><c:choose><c:when test="${l.ingredientType == 'RAW'}"><span class="badge badge-making">RAW</span></c:when><c:otherwise><span class="badge badge-ready">PREPPED</span></c:otherwise></c:choose></td>
                        <td>
                            <form action="${ctx}/admin/recipe" method="post" style="display:inline-flex;gap:4px;align-items:center">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="updateLine">
                                <input type="hidden" name="productId" value="${product.productId}">
                                <input type="hidden" name="lineId" value="${l.productRecipeId}">
                                <input type="number" name="quantity" class="form-control" style="width:100px" min="0.001" step="0.001" value="${l.quantity}" required>
                                <span class="muted">${l.ingredientUnit}</span>
                                <button type="submit" class="btn btn-ghost btn-sm">Lưu</button>
                            </form>
                        </td>
                        <td>
                            <form action="${ctx}/admin/recipe" method="post" style="display:inline" onsubmit="return confirm('Xoá dòng này?');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="deleteLine">
                                <input type="hidden" name="productId" value="${product.productId}">
                                <input type="hidden" name="lineId" value="${l.productRecipeId}">
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
