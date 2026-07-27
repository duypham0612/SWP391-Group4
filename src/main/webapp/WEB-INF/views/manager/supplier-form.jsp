<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1><c:choose><c:when test="${supplier.supplierId > 0}">Cập nhật nhà cung cấp</c:when><c:otherwise>Thêm nhà cung cấp</c:otherwise></c:choose></h1><p>Nhập đầy đủ tên và thông tin liên hệ của nhà cung cấp.</p></div>
    <a class="btn btn-ghost" href="${ctx}/manager/supplier">← Quay lại</a>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty errorMsg}"><div class="alert alert-error">${errorMsg}</div></c:if>

<div class="card form-card">
    <form action="${ctx}/manager/supplier" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="save">
        <input type="hidden" name="supplierId" value="${supplier.supplierId}">
        <div class="form-group"><label for="name">Tên nhà cung cấp *</label>
            <input id="name" type="text" name="name" class="form-control" maxlength="150" value="${supplier.name}" required autofocus></div>
        <div class="form-group"><label for="phone">Số điện thoại *</label>
            <input id="phone" type="tel" name="phone" class="form-control" maxlength="20"
                   inputmode="numeric" pattern="0[0-9]{9}" value="${supplier.phone}" required
                   oninvalid="this.setCustomValidity('Số điện thoại không hợp lệ. Vui lòng nhập đúng 10 chữ số và bắt đầu bằng 0.')"
                   oninput="this.setCustomValidity('');document.getElementById('phoneLengthError').style.display=this.value.length>10?'block':'none'">
            <div class="muted" style="font-size:.8rem">Gồm đúng 10 chữ số và bắt đầu bằng 0.</div>
            <div id="phoneLengthError" class="field-error" style="display:none;color:var(--st-cancelled);font-size:.8rem">
                Số điện thoại không hợp lệ: chỉ được nhập đúng 10 chữ số.
            </div></div>
        <div class="form-group"><label for="address">Địa chỉ *</label>
            <input id="address" type="text" name="address" class="form-control" maxlength="255" value="${supplier.address}" required></div>
        <div class="form-group"><label><input type="checkbox" name="active" value="1" <c:if test="${supplier.active or supplier.supplierId == 0}">checked</c:if>> Đang hoạt động</label></div>
        <button type="submit" class="btn btn-primary btn-lg">Lưu</button>
    </form>
</div>

<jsp:include page="../layout/footer.jsp" />
