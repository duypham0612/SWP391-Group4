<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Tạo phiếu nhập</h1><p>Tạo phiếu nháp, thêm dòng nguyên liệu, rồi xác nhận để cộng tồn</p></div>
    <a class="btn btn-ghost" href="${ctx}/manager/receipt">← Quay lại</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>

<div class="card form-card">
    <form action="${ctx}/manager/receipt" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="create">
        <div class="form-group"><label for="supplierId">Nhà cung cấp</label>
            <select id="supplierId" name="supplierId" class="form-control">
                <option value="">-- Không chọn --</option>
                <c:forEach var="s" items="${suppliers}"><option value="${s.supplierId}">${s.name}</option></c:forEach>
            </select></div>
        <div class="form-group"><label for="note">Ghi chú</label>
            <input id="note" type="text" name="note" class="form-control" maxlength="255"></div>
        <button type="submit" class="btn btn-primary btn-lg">Tạo phiếu nháp</button>
    </form>
</div>

<jsp:include page="../layout/footer.jsp" />
