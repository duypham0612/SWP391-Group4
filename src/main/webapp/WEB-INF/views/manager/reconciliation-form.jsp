<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Ghi nhận kiểm kê</h1><p>Nhập số lượng kiểm đếm thực tế; hệ thống sẽ tự tính và lưu phần chênh lệch.</p></div>
    <a class="btn btn-ghost" href="${ctx}/manager/reconciliation">← Quay lại</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>

<div class="alert alert-info">Tick các nguyên liệu cần kiểm kê, nhập <strong>tồn thực tế</strong> (và đơn vị/lý do nếu cần). Chênh lệch (thực tế − hệ thống) sẽ được ghi 1 dòng <code>ADJUST</code> vào sổ cái và cập nhật tồn. Dòng được tick nhưng chưa nhập tồn thực tế sẽ bỏ qua.</div>

<div class="card">
    <form action="${ctx}/manager/reconciliation" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <table class="table">
            <thead><tr>
                <th style="width:40px"><input type="checkbox" onclick="document.querySelectorAll('.pickbox').forEach(c=>c.checked=this.checked)"></th>
                <th>Nguyên liệu</th>
                <th style="width:120px">Đơn vị</th>
                <th style="width:150px">Tồn thực tế</th>
                <th>Lý do</th>
            </tr></thead>
            <tbody>
                <c:forEach var="i" items="${ingredients}">
                    <tr>
                        <td><input class="pickbox" type="checkbox" name="pick" value="${i.ingredientId}"></td>
                        <td>${i.name} <span class="muted">· ${i.ingredientType == 'PREPPED' ? 'Đã sơ chế' : 'Nguyên liệu thô'}</span></td>
                        <td><input type="text" name="unit_${i.ingredientId}" class="form-control" maxlength="20" value="${i.unit}"></td>
                        <td><input type="number" name="actual_${i.ingredientId}" class="form-control" min="0" step="any" placeholder="0"></td>
                        <td><input type="text" name="reason_${i.ingredientId}" class="form-control" maxlength="255" placeholder="Kiểm kê cuối ca..."></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        <button type="submit" class="btn btn-primary btn-lg">Ghi điều chỉnh các mục đã chọn</button>
    </form>
</div>

<jsp:include page="../layout/footer.jsp" />
