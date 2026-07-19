<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Quản lý</div><h1>Cài đặt chi nhánh</h1><p><c:out value="${branch.name}" /></p></div>
</div>

<c:if test="${not empty sessionScope.flashOk}"><div class="alert alert-success">${sessionScope.flashOk}</div><c:remove var="flashOk" scope="session" /></c:if>
<c:if test="${not empty sessionScope.flashError}"><div class="alert alert-error">${sessionScope.flashError}</div><c:remove var="flashError" scope="session" /></c:if>

<div class="card form-card">
    <form action="${ctx}/manager/branch-settings" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">

        <div class="form-group">
            <label for="openTime">Giờ mở cửa</label>
            <input id="openTime" type="time" name="openTime" class="form-control"
                   value="${empty branch.openTime ? '' : branch.openTime}">
            <small class="muted">Mốc "ngày kinh doanh" ở Quầy pha chế cắt theo giờ này. Để trống = cắt theo nửa đêm.</small>
        </div>
        <div class="form-group">
            <label for="closeTime">Giờ đóng cửa</label>
            <input id="closeTime" type="time" name="closeTime" class="form-control"
                   value="${empty branch.closeTime ? '' : branch.closeTime}">
            <small class="muted">Được phép đóng sau nửa đêm (vd mở 17:00, đóng 01:00).</small>
        </div>
        <div class="form-group">
            <label for="peakThresholdCups">Ngưỡng cao điểm (số ly đang chờ + đang pha)</label>
            <input id="peakThresholdCups" type="number" name="peakThresholdCups" class="form-control" min="0" step="1"
                   value="${branch.peakThresholdCups}">
            <small class="muted">Vượt ngưỡng, Quầy pha chế bỏ tô đỏ hàng loạt và chuyển sang xếp số thứ tự pha. Để 0 = dùng mặc định của hệ thống.</small>
        </div>

        <div class="alert alert-warn" style="margin-top:4px">
            Đổi giờ mở cửa giữa ca sẽ dịch mốc ngày kinh doanh: các món tạo trước mốc mới sẽ rời hàng chờ
            sang khu "Đơn treo". Nên đổi vào đầu/cuối ca.
        </div>

        <button type="submit" class="btn btn-primary btn-lg">Lưu cài đặt</button>
    </form>
</div>

<jsp:include page="../layout/footer.jsp" />
