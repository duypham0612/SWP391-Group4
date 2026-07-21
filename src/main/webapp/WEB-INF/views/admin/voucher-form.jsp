<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><h1><c:choose><c:when test="${voucher.voucherId > 0}">Sửa voucher</c:when><c:otherwise>Thêm voucher</c:otherwise></c:choose></h1></div>
    <a class="btn btn-ghost" href="${ctx}/admin/voucher">← Quay lại</a>
</div>

<c:if test="${not empty errorMsg}"><div class="alert alert-error">${errorMsg}</div></c:if>

<div class="card form-card">
    <form action="${ctx}/admin/voucher" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="save">
        <input type="hidden" name="voucherId" value="${voucher.voucherId}">

        <div class="form-group">
            <label for="code">Mã voucher *</label>
            <input id="code" type="text" name="code" class="form-control" maxlength="40" value="${voucher.code}" required
                   <c:choose><c:when test="${voucher.voucherId > 0}">readonly</c:when><c:otherwise>autofocus</c:otherwise></c:choose>>
            <c:if test="${voucher.voucherId > 0}"><small class="muted">Mã voucher không thể thay đổi sau khi tạo.</small></c:if>
        </div>
        <div class="form-group">
            <label for="discountType">Loại giảm *</label>
            <select id="discountType" name="discountType" class="form-control" required>
                <option value="PERCENT" <c:if test="${voucher.discountType == 'PERCENT'}">selected</c:if>>PERCENT (theo %)</option>
                <option value="FIXED"   <c:if test="${voucher.discountType == 'FIXED'}">selected</c:if>>FIXED (số tiền)</option>
            </select>
        </div>
        <div class="form-group">
            <label for="discountValue">Giá trị giảm *</label>
            <input id="discountValue" type="number" name="discountValue" class="form-control" min="0" step="0.01" value="${voucher.discountValue}" required>
        </div>
        <div class="form-group">
            <label for="minOrderAmount">Đơn tối thiểu (₫)</label>
            <input id="minOrderAmount" type="number" name="minOrderAmount" class="form-control" min="0" step="1000" value="${voucher.minOrderAmount}">
        </div>
        <div class="form-group">
            <label for="scope">Phạm vi *</label>
            <select id="scope" name="scope" class="form-control" required>
                <option value="CHAIN"  <c:if test="${voucher.scope == 'CHAIN'}">selected</c:if>>CHAIN (toàn chuỗi)</option>
                <option value="BRANCH" <c:if test="${voucher.scope == 'BRANCH'}">selected</c:if>>BRANCH (1 chi nhánh)</option>
            </select>
        </div>
        <div class="form-group">
            <label for="branchId">Chi nhánh <span class="muted">(chỉ khi phạm vi BRANCH)</span></label>
            <select id="branchId" name="branchId" class="form-control">
                <option value="">--</option>
                <c:forEach var="b" items="${branches}">
                    <option value="${b.branchId}" <c:if test="${b.branchId == voucher.branchId}">selected</c:if>>${b.code} — ${b.name}</option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group">
            <label for="startDate">Bắt đầu</label>
            <input id="startDate" type="datetime-local" name="startDate" class="form-control" value="${voucher.startInput}">
        </div>
        <div class="form-group">
            <label for="endDate">Kết thúc</label>
            <input id="endDate" type="datetime-local" name="endDate" class="form-control" value="${voucher.endInput}">
            <small class="muted">Nếu voucher đang bật, thời điểm kết thúc phải lớn hơn hiện tại và lớn hơn thời điểm bắt đầu.</small>
        </div>
        <div class="form-group">
            <label for="usageLimit">Giới hạn lượt dùng <span class="muted">(trống = không giới hạn)</span></label>
            <input id="usageLimit" type="number" name="usageLimit" class="form-control" min="0" value="${voucher.usageLimit}">
        </div>
        <div class="form-group">
            <label><input id="activeInput" type="checkbox" name="active" value="1" <c:if test="${voucher.active or voucher.voucherId == 0}">checked</c:if>> Đang bật</label>
        </div>
        <button type="submit" class="btn btn-primary btn-lg">Lưu</button>
    </form>
</div>

<script>
(function(){
    var start = document.getElementById('startDate');
    var end = document.getElementById('endDate');
    var active = document.getElementById('activeInput');
    if (!start || !end) return;
    function syncEndMin(){
        var min = start.value || '';
        if (active && active.checked && (!min || '${nowInput}' > min)) min = '${nowInput}';
        end.min = min;
        if (end.value && start.value && end.value <= start.value) {
            end.setCustomValidity('Ngày kết thúc phải lớn hơn ngày bắt đầu.');
        } else if (end.value && active && active.checked && end.value <= '${nowInput}') {
            end.setCustomValidity('Voucher đang bật phải có ngày kết thúc lớn hơn hiện tại.');
        } else {
            end.setCustomValidity('');
        }
    }
    start.addEventListener('change', syncEndMin);
    end.addEventListener('input', syncEndMin);
    if (active) active.addEventListener('change', syncEndMin);
    syncEndMin();
})();
</script>

<jsp:include page="../layout/footer.jsp" />
