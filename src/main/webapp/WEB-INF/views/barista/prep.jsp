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
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
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
            <thead><tr><th style="width:50px">#</th><th>Nguyên liệu</th><th style="width:150px">Sản lượng</th><th>Người pha</th><th>Lúc</th><th style="width:110px">Trạng thái</th><th style="width:280px">Thao tác</th></tr></thead>
            <tbody>
                <c:forEach var="b" items="${batches}">
                    <tr<c:if test="${b.status == 'CANCELLED'}"> style="opacity:.55"</c:if>>
                        <td>${b.prepBatchId}</td>
                        <td>${b.preppedIngredientName}</td>
                        <td><strong>${b.quantityProduced}</strong> ${b.preppedIngredientUnit}</td>
                        <td>${b.madeByName}</td>
                        <td>${b.madeAt}</td>
                        <td>
                            <c:choose>
                                <c:when test="${b.status == 'CANCELLED'}"><span class="badge" style="background:var(--st-cancelled);color:#fff">Đã huỷ</span></c:when>
                                <c:otherwise><span class="badge" style="background:var(--st-ready);color:#fff">Hiệu lực</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:if test="${b.status == 'ACTIVE'}">
                                <form action="${ctx}/barista/prep" method="post" style="display:inline-flex;gap:4px;align-items:center">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="updateBatch">
                                    <input type="hidden" name="prepBatchId" value="${b.prepBatchId}">
                                    <input type="number" name="quantityProduced" class="form-control" style="width:90px" min="0.001" step="0.001" value="${b.quantityProduced}" required>
                                    <button type="submit" class="btn btn-ghost btn-sm">Sửa</button>
                                </form>
                                <form action="${ctx}/barista/prep" method="post" style="display:inline" onsubmit="return confirm('Huỷ mẻ này? Tồn kho sẽ được hoàn lại qua sổ cái.');">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="cancelBatch">
                                    <input type="hidden" name="prepBatchId" value="${b.prepBatchId}">
                                    <button type="submit" class="btn btn-ghost btn-sm" style="color:var(--st-cancelled)">Huỷ</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
