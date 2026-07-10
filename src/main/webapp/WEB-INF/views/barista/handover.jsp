<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<jsp:include page="../layout/header.jsp" />

<div class="page-header">
    <div><div class="eyebrow">Pha chế</div><h1>Ca làm & Bàn giao</h1><p>Ghi chú bàn giao + KPI hiệu suất pha chế hôm nay</p></div>
</div>

<c:if test="${not empty sessionScope.flashError}">
    <div class="alert alert-error">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashOk}">
    <div class="alert alert-success">${sessionScope.flashOk}</div>
    <c:remove var="flashOk" scope="session" />
</c:if>

<c:set var="clockPostUrl" value="${ctx}/barista/handover" />
<div class="shift-clock-dashboard">
    <jsp:include page="../layout/_shiftClockCard.jsp" />
    <div class="card stat">
        <div class="eyebrow">Lead time TB (hôm nay)</div>
        <span class="value">${kpi.avgLeadDisplay}</span>
        <div class="muted" style="font-size:.8em;margin-top:4px">Chỉ tính món có mốc “bắt đầu pha”</div>
    </div>
    <div class="card stat">
        <div class="eyebrow">Số ly đã pha xong (hôm nay)</div>
        <span class="value">${kpi.cupCount}</span>
        <div class="muted" style="font-size:.8em;margin-top:4px">Gồm cả món pha nhanh (READY thẳng)</div>
    </div>
</div>

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
                        <td>${h.createdAt}</td>
                        <td>${h.createdByName}</td>
                        <td>${h.note}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp" />
