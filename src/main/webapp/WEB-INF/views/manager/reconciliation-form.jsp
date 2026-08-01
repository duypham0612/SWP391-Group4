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

<div class="alert alert-info">Tick các nguyên liệu cần kiểm kê, chọn đơn vị đóng gói và nhập <strong>tồn thực tế</strong>. Hệ thống tự quy đổi về đơn vị gốc trước khi tính chênh lệch và ghi <code>ADJUST</code>. Dòng được tick nhưng chưa nhập số lượng sẽ bỏ qua.</div>

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
                        <td><select name="unitConversionId_${i.ingredientId}" class="form-control" required>
                            <c:forEach var="u" items="${unitConversionsByIngredient[i.ingredientId]}">
                                <option value="${u.ingredientUnitConversionId}">${u.unitName}</option>
                            </c:forEach>
                        </select></td>
                        <td><input type="text" name="actual_${i.ingredientId}" class="form-control" placeholder="0" data-vi-number></td>
                        <td><input type="text" name="reason_${i.ingredientId}" class="form-control" maxlength="255" placeholder="Kiểm kê cuối ca..."></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        <button type="submit" class="btn btn-primary btn-lg">Ghi điều chỉnh các mục đã chọn</button>
    </form>
</div>

<jsp:include page="../layout/footer.jsp" />
