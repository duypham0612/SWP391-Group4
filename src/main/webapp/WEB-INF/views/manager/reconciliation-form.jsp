<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Điều chỉnh tồn</h1><p>Nhập tồn thực tế sau kiểm kê — hệ thống tự ghi chênh lệch vào sổ cái</p></div>
    <a class="btn btn-ghost" href="${ctx}/manager/reconciliation">← Quay lại</a>
</div>

<div class="alert alert-info">Chênh lệch (thực tế − hệ thống) sẽ được ghi 1 dòng <code>ADJUST</code> vào sổ cái và cập nhật tồn.</div>

<div class="card form-card">
    <form action="${ctx}/manager/reconciliation" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <div class="form-group"><label for="ingredientId">Nguyên liệu *</label>
            <select id="ingredientId" name="ingredientId" class="form-control" required>
                <option value="">-- Chọn --</option>
                <c:forEach var="i" items="${ingredients}"><option value="${i.ingredientId}">${i.name} (${i.unit} · ${i.ingredientType})</option></c:forEach>
            </select></div>
        <div class="form-group"><label for="actualQty">Tồn thực tế *</label>
            <input id="actualQty" type="number" name="actualQty" class="form-control" step="0.001" required></div>
        <div class="form-group"><label for="reason">Lý do</label>
            <input id="reason" type="text" name="reason" class="form-control" maxlength="255" placeholder="Kiểm kê cuối ca..."></div>
        <button type="submit" class="btn btn-primary btn-lg">Ghi điều chỉnh</button>
    </form>
</div>

<jsp:include page="../layout/footer.jsp" />
