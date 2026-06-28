<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Pha sẵn (Prep)</h1><p>Nơi DUY NHẤT đổi RAW→PREPPED — trừ nguyên liệu thô, cộng đồ pha sẵn (qua sổ cái)</p></div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>

<div class="card form-card" style="margin-bottom:18px">
    <h3 style="margin-top:0">Tạo mẻ pha sẵn</h3>
    <form action="${ctx}/barista/prep" method="post" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="createBatch">
        <div class="form-group" style="margin:0;flex:1;min-width:220px"><label>Nguyên liệu pha sẵn (PREPPED)</label>
            <select name="preppedIngredientId" class="form-control" required>
                <option value="">-- Chọn --</option>
                <c:forEach var="i" items="${preppedIngredients}"><option value="${i.ingredientId}">${i.name} (${i.unit})</option></c:forEach>
            </select></div>
        <div class="form-group" style="margin:0;width:160px"><label>Sản lượng tạo ra</label>
            <input type="number" name="quantityProduced" class="form-control" min="0" step="0.001" required></div>
        <button type="submit" class="btn btn-primary">Tạo mẻ</button>
    </form>
</div>

<h3>Mẻ pha gần đây</h3>
<c:choose>
    <c:when test="${empty batches}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có mẻ pha nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th style="width:60px">#</th><th>Nguyên liệu</th><th style="width:160px">Sản lượng</th><th>Người pha</th><th>Lúc</th></tr></thead>
            <tbody>
                <c:forEach var="b" items="${batches}">
                    <tr>
                        <td>${b.prepBatchId}</td>
                        <td>${b.preppedIngredientName}</td>
                        <td><strong>${b.quantityProduced}</strong> ${b.preppedIngredientUnit}</td>
                        <td>${b.madeByName}</td>
                        <td>${b.madeAt}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
