<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div>
        <h1><c:choose><c:when test="${voucher.voucherId > 0}">Sửa voucher</c:when><c:otherwise>Thêm voucher</c:otherwise></c:choose></h1>
        <p>Thiết lập mã giảm giá và phạm vi áp dụng.</p>
    </div>
    <a class="btn btn-ghost" href="${ctx}/admin/voucher">← Quay lại</a>
</div>

<c:if test="${not empty errorMsg}"><div class="alert alert-error"><c:out value="${errorMsg}"/></div></c:if>

<div class="card form-card">
    <form action="${ctx}/admin/voucher" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="save">
        <input type="hidden" name="voucherId" value="${voucher.voucherId}">

        <div class="form-group">
            <label for="code">Mã voucher *</label>
            <input id="code" type="text" name="code" class="form-control" maxlength="40" pattern="[A-Za-z0-9_-]+"
                   title="Chỉ dùng chữ cái không dấu, chữ số, dấu gạch ngang hoặc gạch dưới."
                   value="${voucher.code}" required
                   <c:choose><c:when test="${voucher.voucherId > 0}">readonly</c:when><c:otherwise>autofocus</c:otherwise></c:choose>>
            <c:choose>
                <c:when test="${voucher.voucherId > 0}">
                    <small class="muted">Mã voucher không thể thay đổi sau khi tạo.</small>
                </c:when>
                <c:otherwise>
                    <small class="muted">Tối đa 40 ký tự; chỉ dùng chữ cái không dấu, chữ số, dấu - hoặc _.</small>
                </c:otherwise>
            </c:choose>
        </div>
        <div class="form-group">
            <label for="discountType">Loại giảm *</label>
            <select id="discountType" name="discountType" class="form-control" required>
                <option value="PERCENT" <c:if test="${voucher.discountType == 'PERCENT'}">selected</c:if>>PERCENT (theo %)</option>
                <option value="FIXED" <c:if test="${voucher.discountType == 'FIXED'}">selected</c:if>>FIXED (số tiền)</option>
            </select>
        </div>
        <div class="form-group">
            <label for="discountValue">Giá trị giảm *</label>
            <input id="discountValue" type="text" name="discountValue" class="form-control" value="${voucher.discountValue}" data-money-input required>
        </div>
        <div class="form-group">
            <label for="minOrderAmount">Đơn tối thiểu (₫)</label>
            <input id="minOrderAmount" type="text" name="minOrderAmount" class="form-control" value="${voucher.minOrderAmount}" data-money-input>
        </div>
        <div class="form-group">
            <label for="scopeTarget">Phạm vi áp dụng *</label>
            <select id="scopeTarget" name="scopeTarget" class="form-control" required>
                <option value="CHAIN" <c:if test="${voucher.scope == 'CHAIN'}">selected</c:if>>Toàn chuỗi</option>
                <c:forEach var="b" items="${branches}">
                    <option value="BRANCH:${b.branchId}" <c:if test="${voucher.scope == 'BRANCH' and b.branchId == voucher.branchId}">selected</c:if>>
                        Chi nhánh: ${b.code} — ${b.name}
                    </option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group">
            <label for="startAtLocal">Bắt đầu (giờ Việt Nam)</label>
            <input id="startAtLocal" type="datetime-local" name="startAtLocal" class="form-control" value="${view.voucherInput(voucher.startAtUtc)}">
        </div>
        <div class="form-group">
            <label for="endAtLocal">Kết thúc (giờ Việt Nam, không bao gồm mốc này)</label>
            <input id="endAtLocal" type="datetime-local" name="endAtLocal" class="form-control" value="${view.voucherInput(voucher.endAtUtc)}">
        </div>
        <div class="form-group">
            <label for="usageLimit">Giới hạn lượt dùng <span class="muted">(trống = không giới hạn)</span></label>
            <input id="usageLimit" type="number" name="usageLimit" class="form-control" min="0" value="${voucher.usageLimit}">
        </div>
        <div class="form-group">
            <label><input type="checkbox" name="active" value="1" <c:if test="${voucher.active or voucher.voucherId == 0}">checked</c:if>> Đang bật</label>
        </div>
        <button type="submit" class="btn btn-primary btn-lg">Lưu</button>
    </form>
</div>

<jsp:include page="../layout/footer.jsp" />
