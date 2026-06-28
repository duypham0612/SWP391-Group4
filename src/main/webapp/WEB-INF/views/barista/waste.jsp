<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Hao hụt / Làm lại</h1><p>Ghi hao hụt — trừ tồn qua sổ cái (txn WASTE)</p></div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>

<div class="card form-card" style="margin-bottom:18px">
    <h3 style="margin-top:0">Ghi hao hụt</h3>
    <form action="${ctx}/barista/waste" method="post" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="create">
        <div class="form-group" style="margin:0;flex:1;min-width:200px"><label>Nguyên liệu</label>
            <select name="ingredientId" class="form-control" required>
                <option value="">-- Chọn --</option>
                <c:forEach var="i" items="${ingredients}"><option value="${i.ingredientId}">${i.name} (${i.unit})</option></c:forEach>
            </select></div>
        <div class="form-group" style="margin:0;width:130px"><label>Số lượng</label>
            <input type="number" name="quantity" class="form-control" min="0" step="0.001" required></div>
        <div class="form-group" style="margin:0;width:150px"><label>Loại</label>
            <select name="wasteType" class="form-control" required>
                <option value="SPILL">Đổ/rơi</option>
                <option value="EXPIRED">Hết hạn</option>
                <option value="REMAKE">Làm lại</option>
                <option value="OTHER">Khác</option>
            </select></div>
        <div class="form-group" style="margin:0;flex:1;min-width:160px"><label>Lý do</label>
            <input type="text" name="reason" class="form-control" maxlength="255"></div>
        <button type="submit" class="btn btn-primary">Ghi</button>
    </form>
</div>

<h3>Nhật ký hao hụt</h3>
<c:choose>
    <c:when test="${empty logs}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có ghi nhận hao hụt nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th>Nguyên liệu</th><th style="width:140px">Số lượng</th><th style="width:120px">Loại</th><th>Lý do</th><th>Người ghi</th><th>Lúc</th></tr></thead>
            <tbody>
                <c:forEach var="w" items="${logs}">
                    <tr>
                        <td>${w.ingredientName}</td>
                        <td><strong>${w.quantity}</strong> ${w.ingredientUnit}</td>
                        <td>
                            <c:choose>
                                <c:when test="${w.wasteType == 'SPILL'}">Đổ/rơi</c:when>
                                <c:when test="${w.wasteType == 'EXPIRED'}">Hết hạn</c:when>
                                <c:when test="${w.wasteType == 'REMAKE'}">Làm lại</c:when>
                                <c:otherwise>Khác</c:otherwise>
                            </c:choose>
                        </td>
                        <td>${w.reason}</td>
                        <td>${w.loggedByName}</td>
                        <td>${w.loggedAt}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
