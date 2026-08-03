<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1>Tạo phiếu nhập</h1><p>Tạo phiếu nháp, thêm dòng nguyên liệu, rồi xác nhận để cộng tồn</p></div>
    <a class="btn btn-ghost" href="${ctx}/manager/receipt">← Quay lại</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty errorMsg}">
    <div class="alert alert-error">${errorMsg}</div>
</c:if>

<div class="card form-card">
    <form action="${ctx}/manager/receipt" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="create">
        <div class="form-group"><label for="supplierId">Nhà cung cấp *</label>
            <select id="supplierId" name="supplierId" class="form-control" required
                    oninvalid="this.setCustomValidity('Vui lòng chọn nhà cung cấp trước khi tạo phiếu nhập kho.')"
                    onchange="this.setCustomValidity('')">
                <option value="">-- Chọn nhà cung cấp --</option>
                <c:forEach var="s" items="${suppliers}"><option value="${s.supplierId}" <c:if test="${param.supplierId == s.supplierId}">selected</c:if>>${s.name}</option></c:forEach>
            </select></div>
        <div class="form-group"><label for="note">Ghi chú</label>
            <input id="note" type="text" name="note" class="form-control" maxlength="255" value="${fn:escapeXml(param.note)}"></div>
        <div class="form-group"><label for="ingredientId">Nguyên liệu đầu tiên</label>
            <select id="ingredientId" name="ingredientId" class="form-control" required>
                <option value="">-- Chọn --</option>
                <c:forEach var="i" items="${ingredients}"><option value="${i.ingredientId}" <c:if test="${param.ingredientId == i.ingredientId}">selected</c:if>>${i.name}</option></c:forEach>
            </select></div>
        <div class="form-group"><label for="unitConversionId">Đơn vị nhập</label>
            <select id="unitConversionId" name="unitConversionId" class="form-control" required disabled>
                <option value="">-- Chọn --</option>
                <c:forEach var="i" items="${ingredients}">
                    <c:forEach var="unit" items="${unitChoicesByIngredient[i.ingredientId]}">
                        <option value="${unit.choiceCode}" data-ingredient="${i.ingredientId}" <c:if test="${param.unitConversionId == unit.choiceCode}">selected</c:if> hidden>${unit.unitName}</option>
                    </c:forEach>
                </c:forEach>
            </select></div>
        <div class="form-group"><label for="quantity">Số lượng</label>
            <input id="quantity" type="number" name="quantity" class="form-control" min="0.000001" step="0.000001" required value="${fn:escapeXml(param.quantity)}"></div>
        <div class="form-group"><label for="unitCost">Đơn giá</label>
            <input id="unitCost" type="number" name="unitCost" class="form-control" min="0.01" step="0.01" required value="${fn:escapeXml(param.unitCost)}"></div>
        <button type="submit" class="btn btn-primary btn-lg">Tạo phiếu nháp</button>
    </form>
</div>

<script>
(function(){
    var ingredient=document.getElementById('ingredientId');
    var unit=document.getElementById('unitConversionId');
    function syncUnits(){
        var current=unit.value;
        Array.prototype.forEach.call(unit.options,function(option,index){
            if(index===0)return;
            option.hidden=option.dataset.ingredient!==ingredient.value;
            option.disabled=option.hidden;
        });
        unit.disabled=!ingredient.value;
        var selected=Array.prototype.find.call(unit.options,function(option){
            return option.value===current&&!option.hidden;
        });
        var first=Array.prototype.find.call(unit.options,function(option){return option.value&&!option.hidden;});
        if(selected)unit.value=selected.value;
        else if(first)unit.value=first.value;
        else unit.value='';
    }
    ingredient.addEventListener('change',syncUnits);
    syncUnits();
})();
</script>

<jsp:include page="../layout/footer.jsp" />
