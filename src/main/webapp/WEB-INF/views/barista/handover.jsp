<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Bàn giao ca</h1><p>Ghi lại tình hình quầy cho ca sau</p></div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
</c:if>

<div class="card form-card" style="margin-bottom:18px">
    <h3 style="margin-top:0">Ghi bàn giao</h3>
    <form action="${ctx}/barista/handover" method="post">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="create">
        <div class="form-group">
            <label>Nội dung bàn giao</label>
            <textarea name="note" class="form-control" rows="3" maxlength="1000" required
                      placeholder="VD: còn 2 mẻ cold brew, máy xay #2 kêu lạ, cần đặt thêm sữa oat..."></textarea>
        </div>
        <button type="submit" class="btn btn-primary">Lưu bàn giao</button>
    </form>
</div>

<%-- Ghi bàn giao xong thì tan ca luôn, khỏi phải sang màn Ca làm của tôi.
     Vẫn ghi được bàn giao khi đã tan ca — không khoá form theo trạng thái ca. --%>
<c:if test="${not empty clockStatus}">
    <div class="card handover-clock">
        <div class="handover-clock__text">
            <c:choose>
                <c:when test="${clockStatus.canClockOut}"><span class="badge badge-making">Đang làm</span></c:when>
                <c:otherwise><span class="badge badge-cancelled">Ngoài ca</span></c:otherwise>
            </c:choose>
            <span class="muted">${clockStatus.statusText}</span>
        </div>
        <c:choose>
            <c:when test="${clockStatus.canClockOut}">
                <form action="${clockPostUrl}" method="post">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="clockOut">
                    <button type="submit" class="btn btn-primary">Tan ca</button>
                </form>
            </c:when>
            <c:otherwise>
                <a class="btn btn-ghost" href="${ctx}/barista/shift">Ca làm của tôi →</a>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>

<h3>Lịch sử bàn giao</h3>
<c:choose>
    <c:when test="${empty handovers}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có ghi chú bàn giao nào.</p></div>
    </c:when>
    <c:otherwise>
        <table class="table">
            <thead><tr><th style="width:180px">Lúc</th><th style="width:160px">Người ghi</th><th>Nội dung</th></tr></thead>
            <tbody>
                <c:forEach var="h" items="${handovers}">
                    <tr>
                        <td>${h.createdDisplay}</td>
                        <td>${h.createdByName}</td>
                        <td>${h.note}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<div class="page-header" style="margin-top:var(--s6)">
    <div><h2 style="margin:0">Cả quán hôm nay</h2><p>Số liệu của toàn chi nhánh, không phải riêng bạn</p></div>
</div>

<div class="card-grid">
    <div class="card stat">
        <span class="label">Thời gian pha trung bình</span>
        <span class="value">${kpi.avgLeadDisplay}</span>
        <div class="muted" style="font-size:.8em;margin-top:4px">Chỉ tính món có mốc “bắt đầu pha”</div>
    </div>
    <div class="card stat">
        <span class="label">Số món đã pha xong</span>
        <span class="value">${kpi.cupCount}</span>
        <div class="muted" style="font-size:.8em;margin-top:4px">Gồm cả món pha nhanh (READY thẳng)</div>
    </div>
</div>

<h3>Ly cả quán đã pha hôm nay</h3>
<c:choose>
    <c:when test="${empty brewHistory}">
        <div class="card empty-state"><div class="icon">∅</div><p>Chưa có món nào hoàn tất hôm nay.</p></div>
    </c:when>
    <c:otherwise>
        <div class="card" style="margin-bottom:18px;padding:0;overflow:auto">
            <table class="table" style="margin:0;min-width:560px">
                <thead><tr><th style="width:180px">Giờ xong</th><th>Món</th><th style="width:100px">SL</th><th style="width:120px">Trạng thái</th></tr></thead>
                <tbody>
                    <c:forEach var="item" items="${brewHistory}">
                        <tr>
                            <td>${item.doneDisplay}</td>
                            <td>${item.productName}</td>
                            <td>${item.quantity}</td>
                            <td><jsp:include page="../layout/_statusBadge.jsp"><jsp:param name="status" value="${item.status}" /></jsp:include></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
