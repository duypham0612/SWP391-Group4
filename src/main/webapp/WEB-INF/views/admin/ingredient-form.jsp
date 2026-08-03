<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1><c:choose><c:when test="${ingredient.ingredientId > 0}">Sửa nguyên liệu</c:when><c:otherwise>Thêm nguyên liệu</c:otherwise></c:choose></h1><p>Khai báo đơn vị và loại nguyên liệu.</p></div>
    <a class="btn btn-ghost" href="${ctx}/admin/ingredient">← Quay lại</a>
</div>

<c:if test="${not empty errorMsg}"><div class="alert alert-error"><c:out value="${errorMsg}"/></div></c:if>
<c:if test="${not empty sessionScope.flashError}"><div class="alert alert-error"><c:out value="${sessionScope.flashError}"/></div><c:remove var="flashError" scope="session" /></c:if>
<c:if test="${not empty sessionScope.flashOk}"><div class="alert alert-success"><c:out value="${sessionScope.flashOk}"/></div><c:remove var="flashOk" scope="session" /></c:if>

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
            <label for="unit">Đơn vị tồn kho *</label>
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
        <div class="form-section" aria-labelledby="purchaseConversionTitle">
            <div class="form-section__head">
                <h3 id="purchaseConversionTitle">Quy cách nhập hàng</h3>
                <span class="badge">Không bắt buộc</span>
            </div>
            <p class="muted">Chỉ khai báo khi nguyên liệu được mua theo hộp, thùng, chai hoặc quy cách khác đơn vị tồn kho.</p>
            <div class="form-group">
                <label for="purchaseUnitName">Đơn vị nhập hàng</label>
                <input id="purchaseUnitName" type="text" name="purchaseUnitName" class="form-control"
                       maxlength="20" pattern="[\p{L}\p{M}\p{N}][\p{L}\p{M}\p{N}\s./()\-]*"
                       title="Từ 1 đến 20 ký tự; chỉ dùng chữ, số, khoảng trắng, dấu chấm, gạch nối hoặc dấu gạch chéo."
                       value="${fn:escapeXml(ingredient.purchaseUnitName)}"
                       placeholder="Ví dụ: hộp, thùng, chai">
            </div>
            <div class="form-group">
                <label id="purchaseFactorLabel" for="purchaseFactorToBase">Số đơn vị tồn kho trong mỗi đơn vị nhập hàng</label>
                <input id="purchaseFactorToBase" type="number" name="purchaseFactorToBase"
                       class="form-control" min="2" max="1000000" step="1"
                       inputmode="numeric" value="${view.plain(ingredient.purchaseFactorToBase)}">
            </div>
            <div id="purchaseConversionPreview" class="conversion-preview" role="status" aria-live="polite" hidden></div>
        </div>
        <div class="form-group">
            <label><input type="checkbox" name="active" value="1" <c:if test="${ingredient.active or ingredient.ingredientId == 0}">checked</c:if>> Đang hoạt động</label>
        </div>
        <button type="submit" class="btn btn-primary btn-lg">Lưu</button>
    </form>
</div>

<script>
  (function(){
    var type = document.getElementById('ingredientType');
    var group = document.getElementById('shelfLifeGroup');
    var input = document.getElementById('shelfLifeHours');
    var baseUnit = document.getElementById('unit');
    var purchaseUnit = document.getElementById('purchaseUnitName');
    var purchaseFactor = document.getElementById('purchaseFactorToBase');
    var purchaseFactorLabel = document.getElementById('purchaseFactorLabel');
    var purchasePreview = document.getElementById('purchaseConversionPreview');
    function sync(){
      var prepped = type.value === 'PREPPED';
      group.hidden = !prepped;
      input.required = prepped;
      input.disabled = !prepped;
    }
    function syncPurchaseConversion(){
      var base = baseUnit.value || 'đơn vị tồn kho';
      var purchase = purchaseUnit.value.trim();
      var factor = purchaseFactor.value.trim();
      purchaseFactorLabel.textContent = purchase
              ? 'Số ' + base + ' trong mỗi ' + purchase
              : 'Số đơn vị tồn kho trong mỗi đơn vị nhập hàng';
      purchaseUnit.required = factor !== '';
      purchaseFactor.required = purchase !== '';
      if (purchase && factor) {
        purchasePreview.textContent = 'Quy đổi: 1 ' + purchase + ' = ' + factor + ' ' + base;
        purchasePreview.hidden = false;
      } else {
        purchasePreview.textContent = '';
        purchasePreview.hidden = true;
      }
    }
    type.addEventListener('change', sync);
    baseUnit.addEventListener('change', syncPurchaseConversion);
    purchaseUnit.addEventListener('input', syncPurchaseConversion);
    purchaseFactor.addEventListener('input', syncPurchaseConversion);
    sync();
    syncPurchaseConversion();
  })();
</script>

<jsp:include page="../layout/footer.jsp" />
