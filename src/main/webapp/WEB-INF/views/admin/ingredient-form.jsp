<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1><c:choose><c:when test="${ingredient.ingredientId > 0}">Sửa nguyên liệu</c:when><c:otherwise>Thêm nguyên liệu</c:otherwise></c:choose></h1><p>Khai báo đơn vị và loại nguyên liệu.</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/ingredient">← Quay lại</a>
</div>

<c:if test="${not empty errorMsg}"><div class="alert alert-error">${errorMsg}</div></c:if>
<c:if test="${not empty sessionScope.flashError}"><div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" /></c:if>
<c:if test="${not empty sessionScope.flashOk}"><div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" /></c:if>

<div class="card form-card">
    <form action="${ctx}/admin/ingredient" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="save">
        <input type="hidden" name="ingredientId" value="${ingredient.ingredientId}">

        <div class="form-group">
            <label for="name">Tên nguyên liệu *</label>
            <input id="name" type="text" name="name" class="form-control" minlength="2" maxlength="120"
                   pattern="[\p{L}\p{M}\p{N}][\p{L}\p{M}\p{N}\s.,&amp;'()/%+\-]*"
                   title="Từ 2 đến 120 ký tự; chỉ dùng chữ, số và dấu câu thông dụng."
                   value="${fn:escapeXml(ingredient.name)}" required autofocus>
        </div>
        <div class="form-group">
            <label for="unit">Đơn vị *</label>
            <select id="unit" name="unit" class="form-control" required>
                <option value="">-- Chọn đơn vị --</option>
                <c:forEach var="unit" items="${supportedUnits}">
                    <option value="${unit}" <c:if test="${ingredient.unit == unit}">selected</c:if>>${unit}</option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group">
            <label for="ingredientType">Loại *</label>
            <select id="ingredientType" name="ingredientType" class="form-control" required>
                <option value="RAW"     <c:if test="${ingredient.ingredientType == 'RAW'}">selected</c:if>>Nguyên liệu thô (mua về)</option>
                <option value="PREPPED" <c:if test="${ingredient.ingredientType == 'PREPPED'}">selected</c:if>>Nguyên liệu pha sẵn</option>
            </select>
        </div>
        <div class="form-group" id="shelfLifeGroup">
            <label for="shelfLifeHours">Thời hạn bảo quản (giờ) *</label>
            <input id="shelfLifeHours" type="number" name="shelfLifeHours" class="form-control"
                   min="1" max="720" step="1" value="${view.shelfLifeHours(ingredient.shelfLifeMinutes)}">
            <small class="muted">Chỉ áp dụng cho nguyên liệu pha sẵn; hệ thống tự tính hạn dùng của mẻ.</small>
        </div>
        <div class="form-group">
            <label><input type="checkbox" name="active" value="1" <c:if test="${ingredient.active or ingredient.ingredientId == 0}">checked</c:if>> Đang hoạt động</label>
        </div>
        <button type="submit" class="btn btn-primary btn-lg">Lưu</button>
    </form>
</div>

<c:if test="${ingredient.ingredientId > 0}">
    <div class="card" style="margin-top:18px">
        <h3>Đơn vị đóng gói và quy đổi</h3>
        <p class="muted">Tồn kho luôn lưu theo <strong>${ingredient.unit}</strong>. Chứng từ đã tạo giữ nguyên hệ số snapshot dù hệ số dưới đây thay đổi.</p>
        <table class="table">
            <thead><tr><th>Đơn vị</th><th>Hệ số về ${ingredient.unit}</th><th>Trạng thái</th><th></th></tr></thead>
            <tbody>
                <c:forEach var="u" items="${unitConversions}">
                    <tr>
                        <c:choose>
                            <c:when test="${u.baseUnit}">
                                <td>${u.unitName} <span class="badge badge-ready">Đơn vị gốc</span></td>
                                <td>1</td><td>Đang dùng</td><td></td>
                            </c:when>
                            <c:otherwise>
                                <td colspan="4">
                                    <form action="${ctx}/admin/ingredient" method="post" style="display:grid;grid-template-columns:1fr 1fr auto auto;gap:10px;align-items:center">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="updateConversion">
                                        <input type="hidden" name="ingredientId" value="${ingredient.ingredientId}">
                                        <input type="hidden" name="conversionId" value="${u.ingredientUnitConversionId}">
                                        <input type="text" name="unitName" class="form-control" maxlength="20" value="${fn:escapeXml(u.unitName)}" required>
                                        <input type="number" name="factorToBase" class="form-control" min="0.000001" step="0.000001" value="${u.factorToBase}" required>
                                        <label><input type="checkbox" name="active" value="1" <c:if test="${u.active}">checked</c:if>> Hoạt động</label>
                                        <button class="btn btn-ghost btn-sm" type="submit">Lưu</button>
                                    </form>
                                </td>
                            </c:otherwise>
                        </c:choose>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        <form action="${ctx}/admin/ingredient" method="post" style="display:flex;gap:10px;align-items:flex-end;flex-wrap:wrap">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="addConversion">
            <input type="hidden" name="ingredientId" value="${ingredient.ingredientId}">
            <div class="form-group" style="margin:0"><label>Tên đơn vị mới</label><input type="text" name="unitName" class="form-control" maxlength="20" placeholder="Túi, thùng, chai..." required></div>
            <div class="form-group" style="margin:0"><label>1 đơn vị này = bao nhiêu ${ingredient.unit}</label><input type="number" name="factorToBase" class="form-control" min="0.000001" step="0.000001" required></div>
            <button class="btn btn-primary" type="submit">+ Thêm quy đổi</button>
        </form>
    </div>
</c:if>

<script>
  (function(){
    var type = document.getElementById('ingredientType');
    var group = document.getElementById('shelfLifeGroup');
    var input = document.getElementById('shelfLifeHours');
    function sync(){
      var prepped = type.value === 'PREPPED';
      group.hidden = !prepped;
      input.required = prepped;
      input.disabled = !prepped;
    }
    type.addEventListener('change', sync);
    sync();
  })();
</script>

<jsp:include page="../layout/footer.jsp" />
